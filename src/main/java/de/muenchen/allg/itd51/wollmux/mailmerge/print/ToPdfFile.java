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

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.io.IOException;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.TextDocument;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

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
      XFilePicker3 picker = FilePicker.createWithMode(UNO.defaultContext, TemplateDescription.FILESAVE_AUTOEXTENSION);
      String filterName = "PDF Dokument";
      picker.appendFilter(filterName, "*.pdf");
      picker.appendFilter("Alle Dateien", "*");
      picker.setCurrentFilter(filterName);
      short res = picker.execute();
      if (res == com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
      {
        String[] files = picker.getFiles();
        Path outputPath = Paths.get(new URI(files[0]));
        TextDocument doc = new TextDocument(
            UNO.XTextDocument(printModel.getProp(PrintFunction.PRINT_RESULT, printModel.getTextDocument())));
        File outputFile = doc.saveAsTemporaryPDF();
        Files.move(outputFile.toPath(), outputPath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.debug("Öffne erzeugtes Gesamtdokument {}", outputPath);
        Desktop.getDesktop().open(outputPath.toFile());
      } else
      {
        InfoDialog.showInfoModal("WollMux Seriendruck", "PDF Dokument konnte nicht angezeigt werden.");
      }
    } catch (IOException | java.io.IOException | URISyntaxException | UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }
}
