/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
import de.muenchen.allg.itd51.wollmux.func.print.PrintException;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * A print model for the print functions maintained by {@link MasterPrintModel}. Each print
 * functions gets its own model.
 */
class SlavePrintModel extends WeakBase implements XPrintModel
{
  private int idx;

  private MasterPrintModel master;

  /**
   * Description of the print handled by this model.
   */
  private String stage;

  /**
   * Create a new print model in the call hierarchy maintained by the master print model.
   *
   * @param master
   *          The master print model.
   * @param idx
   *          Position in the call hierarchy.
   */
  public SlavePrintModel(MasterPrintModel master, int idx)
  {
    this.master = master;
    this.idx = idx;
  }

  @Override
  public XTextDocument getTextDocument()
  {
    return master.getTextDocument();
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

    PrintFunction f = master.getPrintFunction(idx + 1);
    if (f != null)
    {
      XPrintModel pmod = new SlavePrintModel(master, idx + 1);
      try
      {
        f.print(pmod);
      } catch (PrintException e)
      {
        PrintModels.LOGGER.error("", e);
      }
      
      master.setPrintProgressMaxValue(pmod, (short) 0);
    } else
    {
      master.finalPrint();
    }
    if (stage != null)
      master.setStage(stage);
  }

  @Override
  public void setFormValue(String arg0, String arg1)
  {
    master.setFormValue(arg0, arg1);
  }

  @Override
  public boolean getDocumentModified()
  {
    return master.getDocumentModified();
  }

  @Override
  public void setDocumentModified(boolean arg0)
  {
    master.setDocumentModified(arg0);
  }

  @Override
  public void collectNonWollMuxFormFields()
  {
    master.collectNonWollMuxFormFields();
  }

  @Override
  public void setPrintBlocksProps(String arg0, boolean arg1, boolean arg2)
  {
    master.setPrintBlocksProps(arg0, arg1, arg2);
  }

  @Override
  public XPropertySetInfo getPropertySetInfo()
  {
    return master.getPropertySetInfo();
  }

  @Override
  public void setPropertyValue(String key, Object val)
      throws UnknownPropertyException, PropertyVetoException, WrappedTargetException
  {
    if (PrintModels.STAGE.equalsIgnoreCase(key) && val != null)
    {
      stage = val.toString();
      master.setStage(stage);
    }
    master.setPropertyValue(key, val);
  }

  @Override
  public Object getPropertyValue(String arg0) throws UnknownPropertyException, WrappedTargetException
  {
    return master.getPropertyValue(arg0);
  }

  @Override
  public Object getProp(String arg0, Object arg1)
  {
    return master.getProp(arg0, arg1);
  }

  @Override
  public void addPropertyChangeListener(String arg0, XPropertyChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    master.addPropertyChangeListener(arg0, arg1);
  }

  @Override
  public void removePropertyChangeListener(String arg0, XPropertyChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    master.removePropertyChangeListener(arg0, arg1);
  }

  @Override
  public void addVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    master.addVetoableChangeListener(arg0, arg1);
  }

  @Override
  public void removeVetoableChangeListener(String arg0, XVetoableChangeListener arg1)
      throws UnknownPropertyException, WrappedTargetException
  {
    master.removeVetoableChangeListener(arg0, arg1);
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
      PrintFunction currentFunc = master.getPrintFunction(idx);
      if (newFunc.compareTo(currentFunc) <= 0)
      {
        PrintModels.LOGGER.error("Druckfunktion '{}' muss einen höheren ORDER-Wert besitzen als die Druckfunktion '{}'",
            newFunc.getFunctionName(), currentFunc.getFunctionName());
      } else
      {
        master.usePrintFunction(functionName);
      }
    } else
    {
      throw new NoSuchMethodException(L.m("Druckfunktion '%1' nicht definiert.", functionName));
    }
  }

  @Override
  public void setGroupVisible(String arg0, boolean arg1)
  {
    master.setGroupVisible(arg0, arg1);
  }

  @Override
  public boolean isCanceled()
  {
    return master.isCanceled();
  }

  @Override
  public void cancel()
  {
    master.cancel();
  }

  @Override
  public void setPrintProgressMaxValue(short maxValue)
  {
    master.setPrintProgressMaxValue(this, maxValue);
  }

  @Override
  public void setPrintProgressValue(short value)
  {
    master.setPrintProgressValue(this, value);
  }

  @Override
  public void setPrintMessage(String value)
  {
    master.setPrintMessage(value);
  }
}
