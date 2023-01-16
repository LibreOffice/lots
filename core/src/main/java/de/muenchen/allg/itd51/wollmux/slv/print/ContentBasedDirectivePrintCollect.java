/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv.print;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.TextDocument;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Print function for collecting all content based directive prints. Each content based directive is
 * saved as a PDF and the URL added to a property.
 */
public class ContentBasedDirectivePrintCollect extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentBasedDirectivePrintCollect.class);

  /**
   * A {@link PrintFunction} with name "SachleitendeVerfuegungCollect" and order 300.
   */
  public ContentBasedDirectivePrintCollect()
  {
    super("SachleitendeVerfuegungCollect", 300);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    try
    {
      @SuppressWarnings("unchecked")
      List<File> collection = (List<File>) printModel.getProp(ContentBasedDirectivePrint.PROP_SLV_COLLECT,
          new ArrayList<File>());
      TextDocument doc = new TextDocument(
          UNO.XTextDocument(printModel.getProp(PrintFunction.PRINT_RESULT, printModel.getTextDocument())));
      File outputFile = doc.saveAsTemporaryPDF();
      collection.add(outputFile);
      printModel.setPropertyValue(ContentBasedDirectivePrint.PROP_SLV_COLLECT, collection);
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException
        | IOException | com.sun.star.io.IOException | UnoHelperException e)
    {
      LOGGER.error(L.m("Could not collect documents for printing content based directive."), e);
      InfoDialog.showInfoModal(L.m("Error collecting documents"),
          L.m("Could not collect documents for printing content based directive."));
      printModel.cancel();
    }
  }

}
