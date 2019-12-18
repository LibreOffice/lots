package de.muenchen.allg.itd51.wollmux.func.print;

import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;
import de.muenchen.allg.itd51.wollmux.print.OOoBasedMailMerge;
import de.muenchen.allg.itd51.wollmux.print.OOoBasedMailMerge.OutputType;

/**
 * Print function for mailmerge by LibreOffice mailmerge with type {@link OutputType#toShell}.
 */
public class ToOdtFile extends PrintFunction
{
  /**
   * A {@link PrintFunction} with name "OOoMailMergeToOdtFile" and order 200.
   */
  public ToOdtFile()
  {
    super("OOoMailMergeToOdtFile", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    // notification if to much data is selected
    Integer maxProcessableDatasets = Workarounds
        .workaroundForTDFIssue89783(printModel.getTextDocument());
    if (maxProcessableDatasets != null
        && MailMergeNew.mailMergeNewGetSelectionSize(printModel) > maxProcessableDatasets)
    {
      InfoDialog.showInfoModal(L.m("WollMux-Seriendruck Fehler"), L.m(
          "Bei diesem Seriendruck-Hauptdokument kann Ihre aktuelle Office-Version maximal %1 Datensätze verarbeiten. "
              + "Bitte schränken Sie die Anzahl der Datensätze im Druckdialog unter 'folgende Datensätze verwenden' entsprechend ein!",
          maxProcessableDatasets));
      printModel.cancel();
      return;
    }

    OOoBasedMailMerge.oooMailMerge(printModel, OOoBasedMailMerge.OutputType.toShell);
  }

}
