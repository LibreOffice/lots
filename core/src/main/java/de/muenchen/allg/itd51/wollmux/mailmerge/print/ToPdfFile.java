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
package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.TextDocument;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;

/**
 * Open a mail merge result as PDF.
 */
public class ToPdfFile extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ToPdfFile.class);

  /**
   * A {@link PrintFunction} with name "OOoMailMergeToPdfFile" and order 210.
   */
  public ToPdfFile()
  {
    super("OOoMailMergeToPdfFile", 210);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    try
    {
      TextDocument doc = new TextDocument(
          UNO.XTextDocument(printModel.getProp(PrintFunction.PRINT_RESULT, printModel.getTextDocument())));
      File outputFile = doc.saveAsTemporaryPDF();
      printModel.setPropertyValue(PrintFunction.PRINT_RESULT_FILE, outputFile);
      printModel.printWithProps();
    } catch (IOException | java.io.IOException | UnoHelperException
        | IllegalArgumentException
        | UnknownPropertyException | PropertyVetoException | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
}
