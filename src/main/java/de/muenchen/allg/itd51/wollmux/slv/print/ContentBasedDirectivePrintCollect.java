package de.muenchen.allg.itd51.wollmux.slv.print;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.util.UnoProperty;

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
      File outputFile = Files.createTempFile("WollMux_SLV_", ".pdf").toFile();
      UnoProps props = new UnoProps(UnoProperty.FILTER_NAME, "writer_pdf_Export");
      XStorable doc = UNO.XStorable(printModel.getProp(PrintFunction.PRINT_RESULT, printModel.getTextDocument()));
      doc.storeToURL(UNO.convertFilePathToURL(outputFile.getAbsolutePath()), props.getProps());
      collection.add(outputFile);
      printModel.setPropertyValue(ContentBasedDirectivePrint.PROP_SLV_COLLECT, collection);
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException
        | IOException | com.sun.star.io.IOException | UnoHelperException e)
    {
      LOGGER.error(L.m("Konnte die Dokumente für den Druck der Sachleitenden Verfügung nicht aufsammeln."), e);
      InfoDialog.showInfoModal("Sachleitende Verfügungen drucken", "Die Dokumente konnten nicht gesammelt werden.");
      printModel.cancel();
    }
  }

}
