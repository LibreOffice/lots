/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.NoSuchMethodException;

import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.util.UnoProperty;

/**
 * Factory for {@link XPrintModel}s.
 */
public class PrintModels
{

  static final Logger LOGGER = LoggerFactory.getLogger(PrintModels.class);

  /**
   * Property for describing the print ({@link #setStage(XPrintModel, String)}).
   */
  static final String STAGE = "STAGE";

  private PrintModels()
  {
    // nothing to do
  }

  /**
   * Create a new print model for the document model of the provided controller. For each print
   * there's a new print model. If a print consists of several steps, the steps are chained.
   *
   * @param documentController
   *          The controller of the document to be printed.
   * @return The print model.
   */
  public static XPrintModel createPrintModel(TextDocumentController documentController)
  {
    return new MasterPrintModel(documentController);
  }

  /**
   * Create a new print model for the document model of the provided controller. For each print
   * there's a new print model. If a print consists of several steps, the steps are chained.
   *
   * @param documentController
   *          The controller of the document to be printed.
   * @param useDocPrintFunctions
   *          If true print functions associated with the document are loaded by calling
   *          {@link XPrintModel#usePrintFunction(String)}.
   * @return The print model.
   */
  public static XPrintModel createPrintModel(TextDocumentController documentController, boolean useDocPrintFunctions)
  {
    XPrintModel pmod = PrintModels.createPrintModel(documentController);
    if (useDocPrintFunctions)
    {
      for (String name : documentController.getModel().getPrintFunctions())
      {
        try
        {
          pmod.usePrintFunction(name);
        } catch (NoSuchMethodException e)
        {
          LOGGER.error("", e);
        }
      }
    }
    return pmod;
  }

  /**
   * Update the stage description of the print model.
   * 
   * @param pmod
   *          The print model.
   * @param stage
   *          The stage description.
   */
  public static void setStage(XPrintModel pmod, String stage)
  {
    try
    {
      UnoProperty.setProperty(pmod, STAGE, stage);
    } catch (UnoHelperException e)
    {
      LOGGER.error(L.m("Kann Stage nicht auf '%1' setzen", stage), e);
    }
  }
}
