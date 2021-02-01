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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;

/**
 * Show a document with OS default view for the MIME-Type.
 */
public class ShowDocument extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ShowDocument.class);

  /**
   * A {@link PrintFunction} with name "ShowDocument" and order 400.
   */
  public ShowDocument()
  {
    super("ShowDocument", 400);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    try
    {
      File file = (File) printModel.getProp(PrintFunction.PRINT_RESULT_FILE, null);
      if (file != null)
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
          Files.move(file.toPath(), outputPath, StandardCopyOption.REPLACE_EXISTING);
          LOGGER.debug("Öffne erzeugtes Gesamtdokument {}", outputPath);
          Desktop.getDesktop().open(outputPath.toFile());
        } else
        {
          InfoDialog.showInfoModal("WollMux", "Dokument konnte nicht angezeigt werden.");
        }
      } else
      {
        InfoDialog.showInfoModal("WollMux", "Dokument konnte nicht angezeigt werden.");
      }
    } catch (IOException | URISyntaxException e)
    {
      LOGGER.error("", e);
    }
  }
}
