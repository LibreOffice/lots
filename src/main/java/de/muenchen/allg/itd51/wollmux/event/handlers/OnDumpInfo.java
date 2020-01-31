package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;

/**
 * Event for dumping addintional information about WollMux.
 *
 * @see WollMuxFiles#dumpInfo()
 */
public class OnDumpInfo extends WollMuxEvent
{

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    final String title = L.m("Fehlerinfos erstellen");
    String name = WollMuxFiles.dumpInfo();

    if (name != null)
    {
      InfoDialog.showInfoModal(title, L.m(
          "Die Fehlerinformationen des WollMux wurden erfolgreich in die Datei '%1' geschrieben.",
          name));
    } else
    {
      InfoDialog.showInfoModal(title, L.m(
          "Die Fehlerinformationen des WollMux konnten nicht geschrieben werden. Details siehe Datei wollmux.log!"));
    }
  }
}