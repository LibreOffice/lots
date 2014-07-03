package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.dispatch.DispatchProviderAndInterceptor;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

/**
 * Open a mail merge result as LibreOffice document.
 */
public class ToShowOdtFile extends PrintFunction
{

  /**
   * A {@link PrintFunction} with name "OOoMailMergeToShowOdtFile" and order 350.
   */
  public ToShowOdtFile()
  {
    super("OOoMailMergeToShowOdtFile", 350);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    XTextDocument result = UNO
        .XTextDocument(printModel.getProp(PrintFunction.PRINT_RESULT, printModel.getTextDocument()));
    if (result != null && result.getCurrentController() != null && result.getCurrentController().getFrame() != null
        && result.getCurrentController().getFrame().getContainerWindow() != null)
    {
      DocumentManager.getDocumentManager().addTextDocument(result);
      DispatchProviderAndInterceptor.registerDocumentDispatchInterceptor(result.getCurrentController().getFrame());
      result.getCurrentController().getFrame().getContainerWindow().setVisible(true);
      UNO.XTopWindow(result.getCurrentController().getFrame().getContainerWindow()).toFront();
    } else
    {
      InfoDialog.showInfoModal(L.m("WollMux-Seriendruck"),
          L.m("Das erzeugte Gesamtdokument kann leider nicht angezeigt werden."));
      printModel.cancel();
    }
  }
}
