/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.print;

import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyChangeListener;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.beans.XVetoableChangeListener;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Type;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoProps;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.GlobalFunctions;
import org.libreoffice.lots.SyncActionListener;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import org.libreoffice.lots.event.handlers.OnSetFormValue;
import org.libreoffice.lots.event.handlers.OnSetVisibleState;
import org.libreoffice.lots.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import org.libreoffice.lots.print.PageRange.PageRangeType;
import org.libreoffice.lots.slv.events.OnSetPrintBlocksPropsViaPrintModel;
import org.libreoffice.lots.util.L;

/**
 * This model maintains the call hierarchy of all print functions. The hierarchy is defined by the
 * ORDER value of the print functions.
 *
 * The {@link XPropertySet} interface is used to share data between the print functions.
 *
 * Each print function is executed with its own {@link SecondaryPrintModel} in a new thread. This class
 * has to do the synchronization.
 */
class PrimaryPrintModel implements XPrintModel
{
  /**
   * Name of the property to configure if a dialog for copy counts should be displayed.
   */
  private static final String PROP_FINAL_SHOW_COPIES_SPINNER = "FinalPF_ShowCopiesSpinner";

  /**
   * Name of the property to configure if a print parameter dialog should be displayed.
   */
  private static final String PROP_FINAL_NO_PARAMS_DIALOG = "FinalPF_NoParamsDialog";

  /**
   * Name of the property to store the print range for later calls to {@link #finalPrint()}.
   */
  private static final String PROP_FINAL_PAGE_RANGE = "FinalPF_PageRange";

  /**
   * Name of the property to store the print count for later calls to {@link #finalPrint()}.
   */
  private static final String PROP_FINAL_COPY_COUNT = "FinalPF_CopyCount";

  /**
   * Ordered set of print functions.
   */
  private SortedSet<PrintFunction> functions;

  /**
   * Properties of this print model.
   */
  private HashMap<String, Object> props;

  /**
   * Flag indicating cancellation of print.
   */
  private boolean[] isCanceled = new boolean[] { false };

  /**
   * Dialog for showing the print progress.
   */
  private PrintProgressBar printProgressBar = null;

  /**
   * Description of the current print function.
   */
  private String currentStage = L.m("Printing");

  private TextDocumentController documentController;

  /**
   * Create a primary print model for the document. The call hierarchy is empty. Use
   * {@link #usePrintFunction(String)} to add print functions to the call hierarchy.
   *
   * @param documentController
   *          The controller of the document.
   */
  PrimaryPrintModel(TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.props = new HashMap<>();
    this.functions = new TreeSet<>();
  }

  @Override
  public void usePrintFunction(String functionName) throws NoSuchMethodException
  {
    PrintFunction newFunc = GlobalFunctions.getInstance().getGlobalPrintFunctions().get(functionName);
    if (newFunc != null)
    {
      functions.add(newFunc);
    }
    else
      throw new NoSuchMethodException(L.m("Print function \"{0}\" is not defined.", functionName));
  }

  /**
   * Get a print function of the call hierarchy.
   *
   * @param idx
   *          The position in the hierarchy, starting with 0.
   * @return The print function or null if there's no function at this position.
   */
  protected PrintFunction getPrintFunction(int idx)
  {
    Object[] funcs = functions.toArray();
    if (idx >= 0 && idx < funcs.length)
      return (PrintFunction) funcs[idx];
    else
      return null;
  }

  @Override
  public XTextDocument getTextDocument()
  {
    return documentController.getModel().doc;
  }

  @Override
  public void print(short numberOfCopies)
  {
    for (int i = 0; i < numberOfCopies; ++i)
    {
      printWithProps();
    }
  }

  @Override
  public void printWithProps()
  {
    if (isCanceled())
      return;

    PrintFunction f = getPrintFunction(0);
    if (f != null)
    {
      XPrintModel pmod = new SecondaryPrintModel(this, 0);
      Thread t = f.printAsync(pmod);
      try
      {
        t.join();
      } catch (InterruptedException e)
      {
        PrintModels.LOGGER.error("", e);
        Thread.currentThread().interrupt();
      }
    } else
    {
      setPropertySynchronized(PROP_FINAL_SHOW_COPIES_SPINNER, Boolean.TRUE);
      finalPrint();
    }

    if (printProgressBar != null)
    {
      printProgressBar.dispose();
      printProgressBar = null;
    }
  }

  /**
   * Show the final print dialog when called first time. Store the settings from the dialog and
   * reuse them for later calls.
   */
  protected void finalPrint()
  {
    Boolean b = (Boolean) getProperty(PROP_FINAL_SHOW_COPIES_SPINNER);
    boolean showCopiesSpinner = (b != null) && b.booleanValue();

    b = (Boolean) getProperty(PROP_FINAL_NO_PARAMS_DIALOG);
    boolean showParamsDialog = !((b != null) && b.booleanValue());

    if (showParamsDialog)
    {
      Pair<Short, PageRange> ppd = PrintParametersDialog.show(documentController.getModel(), showCopiesSpinner);

      if (ppd == null)
      {
        cancel();
        return;
      }

      setPropertySynchronized(PROP_FINAL_COPY_COUNT, ppd.getKey());
      setPropertySynchronized(PROP_FINAL_PAGE_RANGE, ppd.getValue());
      setPropertySynchronized(PROP_FINAL_NO_PARAMS_DIALOG, Boolean.TRUE);
    }

    Short copyCount = (Short) getProperty(PROP_FINAL_COPY_COUNT);
    if (copyCount == null)
      copyCount = Short.valueOf((short) 1);

    PageRange pageRange = (PageRange) getProperty(PROP_FINAL_PAGE_RANGE);
    if (pageRange == null)
      pageRange = new PageRange(PageRangeType.ALL, null);

    if (!print(pageRange, copyCount))
      cancel();
  }

  /**
   * Print a range of the document several times on the selected printer.
   *
   * @param pr
   *          The range to be printed.
   * @param copyCount
   *          The number of prints.
   * @return True if successful, false otherwise.
   */
  private boolean print(PageRange pr, Short copyCount)
  {
    UnoProps myProps = new UnoProps(UnoProperty.WAIT, Boolean.TRUE);

    myProps.setPropertyValue(UnoProperty.COPY_COUNT, copyCount);

    String prStr = "1";
    if (UNO.XPageCursor(documentController.getModel().getViewCursor()) != null)
      prStr = "" + UNO.XPageCursor(documentController.getModel().getViewCursor()).getPage();

    switch (pr.getPageRangeType())
    {
    case ALL:
      // default, so no pages property has to be set.
      break;

    case USER_DEFINED:
      myProps.setPropertyValue(UnoProperty.PAGES, pr.getPageRangeValue());
      break;

    case CURRENT_PAGE:
      myProps.setPropertyValue(UnoProperty.PAGES, prStr);
      break;

    case CURRENT_AND_FOLLOWING:
      myProps.setPropertyValue(UnoProperty.PAGES, prStr + "-" + documentController.getModel().getPageCount());
      break;
    }

    if (UNO.XPrintable(documentController.getModel().doc) != null)
    {
      try
      {
        UNO.XPrintable(documentController.getModel().doc).print(myProps.getProps());
        return true;
      } catch (IllegalArgumentException e)
      {
        PrintModels.LOGGER.error("", e);
      }
    }
    return false;
  }

  private void setPropertySynchronized(String prop, Object o)
  {
    synchronized (props)
    {
      props.put(prop, o);
    }
  }

  public void setStage(String stage)
  {
    currentStage = stage;
  }

  private Object getProperty(String prop)
  {
    synchronized (props)
    {
      return props.get(prop);
    }
  }

  @Override
  public void setFormValue(String id, String value)
  {
    SyncActionListener s = new SyncActionListener();
    new OnSetFormValue(documentController.getModel().doc, id, value, s).emit();
    s.synchronize();
  }

  @Override
  public boolean getDocumentModified()
  {
    return documentController.getModel().isDocumentModified();
  }

  @Override
  public void setDocumentModified(boolean modified)
  {
    documentController.getModel().setDocumentModified(modified);
  }

  @Override
  public void collectNonWollMuxFormFields()
  {
    SyncActionListener s = new SyncActionListener();
    new OnCollectNonWollMuxFormFieldsViaPrintModel(documentController, s).emit();
    s.synchronize();
  }

  @Override
  public XPropertySetInfo getPropertySetInfo()
  {
    final HashSet<String> propsKeySet;
    synchronized (props)
    {
      propsKeySet = new HashSet<>(props.keySet());
    }

    return new XPropertySetInfo()
    {
      @Override
      public boolean hasPropertyByName(String arg0)
      {
        return propsKeySet.contains(arg0);
      }

      @Override
      public Property getPropertyByName(String arg0) throws UnknownPropertyException
      {
        if (hasPropertyByName(arg0))
          return new Property(arg0, -1, Type.ANY, PropertyAttribute.OPTIONAL);
        else
          throw new UnknownPropertyException(arg0);
      }

      @Override
      public Property[] getProperties()
      {
        Property[] ps = new Property[propsKeySet.size()];
        int i = 0;
        for (String name : propsKeySet)
        {
          try
          {
            ps[i++] = getPropertyByName(name);
          } catch (UnknownPropertyException e)
          {
            PrintModels.LOGGER.trace("", e);
          }
        }
        return ps;
      }
    };
  }

  @Override
  public void setPropertyValue(String arg0, Object arg1)
      throws UnknownPropertyException, PropertyVetoException, WrappedTargetException
  {
    setPropertySynchronized(arg0, arg1);
  }

  @Override
  public Object getPropertyValue(String arg0) throws UnknownPropertyException, WrappedTargetException
  {
    Object o = getProperty(arg0);
    if (o != null)
      return o;
    else
      throw new UnknownPropertyException(arg0);
  }

  @Override
  public Object getProp(String propertyName, Object defaultValue)
  {
    try
    {
      return getPropertyValue(propertyName);
    } catch (Exception e)
    {
      return defaultValue;
    }
  }

  @Override
  public void addPropertyChangeListener(String arg0, XPropertyChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    // NOT IMPLEMENTED
  }

  @Override
  public void removePropertyChangeListener(String arg0, XPropertyChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    // NOT IMPLEMENTED
  }

  @Override
  public void addVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    // NOT IMPLEMENTED
  }

  @Override
  public void removeVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    // NOT IMPLEMENTED
  }

  @Override
  public void setPrintBlocksProps(String blockName, boolean visible, boolean showHighlightColor)
  {
    SyncActionListener s = new SyncActionListener();
    new OnSetPrintBlocksPropsViaPrintModel(documentController, blockName, visible, showHighlightColor, s).emit();
    s.synchronize();
  }

  @Override
  public void setGroupVisible(String groupID, boolean visible)
  {
    SyncActionListener s = new SyncActionListener();
    new OnSetVisibleState(documentController, groupID, visible, s).emit();
    s.synchronize();
  }

  @Override
  public boolean isCanceled()
  {
    synchronized (isCanceled)
    {
      return isCanceled[0];
    }
  }

  @Override
  public void cancel()
  {
    synchronized (isCanceled)
    {
      isCanceled[0] = true;
    }
  }

  @Override
  public void setPrintProgressMaxValue(short maxValue)
  {
    // only useful with SecondaryPrintModels
  }

  @Override
  public void setPrintProgressValue(short value)
  {
    // only useful with SecondaryPrintModels
  }

  /**
   * Specify the maximum value of prints created by a print function. If the value is greater than 0
   * and it's the first print function which calls this method the dialog is created.
   *
   * @param key
   *          The print model of the print function.
   * @param maxValue
   *          The maximum number of prints created by the function.
   */
  void setPrintProgressMaxValue(Object key, short maxValue)
  {
    if (printProgressBar == null && maxValue > 0)
    {
      printProgressBar = new PrintProgressBar(currentStage, e -> cancel());
    }

    if (printProgressBar != null)
      printProgressBar.setMaxValue(key, maxValue);
  }

  /**
   * Update the progress information in the dialog if necessary.
   *
   * @param key
   *          The print model of the print function with the new progress.
   * @param value
   *          The number of prints created by the function.
   */
  void setPrintProgressValue(Object key, short value)
  {
    if (printProgressBar != null)
      printProgressBar.setValue(key, value);
  }

  @Override
  public void setPrintMessage(String value)
  {
    if (printProgressBar != null)
    {
      printProgressBar.setMessage(value);
    }
  }
}
