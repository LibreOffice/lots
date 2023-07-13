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
package org.libreoffice.lots.slv.print;

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

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.document.text.TextDocument;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import org.libreoffice.lots.util.L;

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
      LOGGER.error("Could not collect documents for printing content based directive.", e);
      InfoDialog.showInfoModal(L.m("Error collecting documents"),
          L.m("Could not collect documents for printing content based directive."));
      printModel.cancel();
    }
  }

}
