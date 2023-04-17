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
package de.muenchen.allg.itd51.wollmux.print;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyChangeListener;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.beans.XVetoableChangeListener;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * A print model for the print functions maintained by {@link PrimaryPrintModel}. Each print
 * functions gets its own model.
 */
class SecondaryPrintModel extends WeakBase implements XPrintModel
{
  private int idx;

  private PrimaryPrintModel primary;

  /**
   * Description of the print handled by this model.
   */
  private String stage;

  /**
   * Create a new print model in the call hierarchy maintained by the primary print model.
   *
   * @param primary
   *          The primary print model.
   * @param idx
   *          Position in the call hierarchy.
   */
  public SecondaryPrintModel(PrimaryPrintModel primary, int idx)
  {
    this.primary = primary;
    this.idx = idx;
  }

  @Override
  public XTextDocument getTextDocument()
  {
    return primary.getTextDocument();
  }

  @Override
  public void print(short numberOfCopies)
  {
    for (int i = 0; i < numberOfCopies && !isCanceled(); ++i)
      printWithProps();
  }

  /**
   * Call the next print function in the hierarchy.
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printWithProps()
   */
  @Override
  public void printWithProps()
  {
    if (isCanceled())
      return;

    PrintFunction f = primary.getPrintFunction(idx + 1);
    if (f != null)
    {
      XPrintModel pmod = new SecondaryPrintModel(primary, idx + 1);
      Thread t = f.printAsync(pmod);
      try
      {
        t.join();
      } catch (InterruptedException e)
      {
        PrintModels.LOGGER.error("", e);
        Thread.currentThread().interrupt();
      }
      primary.setPrintProgressMaxValue(pmod, (short) 0);
    } else
    {
      primary.finalPrint();
    }
    if (stage != null)
      primary.setStage(stage);
  }

  @Override
  public void setFormValue(String arg0, String arg1)
  {
    primary.setFormValue(arg0, arg1);
  }

  @Override
  public boolean getDocumentModified()
  {
    return primary.getDocumentModified();
  }

  @Override
  public void setDocumentModified(boolean arg0)
  {
    primary.setDocumentModified(arg0);
  }

  @Override
  public void collectNonWollMuxFormFields()
  {
    primary.collectNonWollMuxFormFields();
  }

  @Override
  public void setPrintBlocksProps(String arg0, boolean arg1, boolean arg2)
  {
    primary.setPrintBlocksProps(arg0, arg1, arg2);
  }

  @Override
  public XPropertySetInfo getPropertySetInfo()
  {
    return primary.getPropertySetInfo();
  }

  @Override
  public void setPropertyValue(String key, Object val)
      throws UnknownPropertyException, PropertyVetoException, WrappedTargetException
  {
    if (PrintModels.STAGE.equalsIgnoreCase(key) && val != null)
    {
      stage = val.toString();
      primary.setStage(stage);
    }
    primary.setPropertyValue(key, val);
  }

  @Override
  public Object getPropertyValue(String arg0) throws UnknownPropertyException, WrappedTargetException
  {
    return primary.getPropertyValue(arg0);
  }

  @Override
  public Object getProp(String arg0, Object arg1)
  {
    return primary.getProp(arg0, arg1);
  }

  @Override
  public void addPropertyChangeListener(String arg0, XPropertyChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    primary.addPropertyChangeListener(arg0, arg1);
  }

  @Override
  public void removePropertyChangeListener(String arg0, XPropertyChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    primary.removePropertyChangeListener(arg0, arg1);
  }

  @Override
  public void addVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    primary.addVetoableChangeListener(arg0, arg1);
  }

  @Override
  public void removeVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    primary.removeVetoableChangeListener(arg0, arg1);
  }

  /**
   * Only print functions with an ORDER-value that is higher than its own ORDER value are accepted.
   *
   * @see de.muenchen.allg.itd51.wollmux.XPrintModel#usePrintFunction(java.lang.String)
   */
  @Override
  public void usePrintFunction(String functionName) throws NoSuchMethodException
  {
    PrintFunction newFunc = GlobalFunctions.getInstance().getGlobalPrintFunctions().get(functionName);
    if (newFunc != null)
    {
      PrintFunction currentFunc = primary.getPrintFunction(idx);
      if (newFunc.compareTo(currentFunc) <= 0)
      {
        PrintModels.LOGGER.error("Druckfunktion '{}' muss einen höheren ORDER-Wert besitzen als die Druckfunktion '{}'",
            newFunc.getFunctionName(), currentFunc.getFunctionName());
      } else
      {
        primary.usePrintFunction(functionName);
      }
    } else
    {
      throw new NoSuchMethodException(L.m("Print function \"{0}\" is not defined.", functionName));
    }
  }

  @Override
  public void setGroupVisible(String arg0, boolean arg1)
  {
    primary.setGroupVisible(arg0, arg1);
  }

  @Override
  public boolean isCanceled()
  {
    return primary.isCanceled();
  }

  @Override
  public void cancel()
  {
    primary.cancel();
  }

  @Override
  public void setPrintProgressMaxValue(short maxValue)
  {
    primary.setPrintProgressMaxValue(this, maxValue);
  }

  @Override
  public void setPrintProgressValue(short value)
  {
    primary.setPrintProgressValue(this, value);
  }

  @Override
  public void setPrintMessage(String value)
  {
    primary.setPrintMessage(value);
  }
}
