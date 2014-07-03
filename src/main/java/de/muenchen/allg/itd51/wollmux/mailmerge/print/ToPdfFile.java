package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.util.UnoProperty;

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
        UnoProps props = new UnoProps(UnoProperty.FILTER_NAME, "writer_pdf_Export");
        XStorable result = UNO.XStorable(printModel.getProp(PrintFunction.PRINT_RESULT, printModel.getTextDocument()));
        result.storeToURL(files[0], props.getProps());
        LOGGER.debug("Öffne erzeugtes Gesamtdokument {}", outputPath);
        Desktop.getDesktop().open(outputPath.toFile());
      } else
      {
        InfoDialog.showInfoModal("WollMux Seriendruck", "PDF Dokument konnte nicht angezeigt werden.");
      }
    } catch (IOException | java.io.IOException | URISyntaxException e)
    {
      LOGGER.error("", e);
    }
  }
}
