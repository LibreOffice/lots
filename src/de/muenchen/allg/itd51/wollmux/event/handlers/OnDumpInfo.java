package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass eine Datei wollmux.dump
 * erzeugt wird, die viele für die Fehlersuche relevanten Informationen enthält wie
 * z.B. Versionsinfo, Inhalt der wollmux.conf, cache.conf, StringRepräsentation der
 * Konfiguration im Speicher und eine Kopie der Log-Datei.
 *
 * Das Event wird von der WollMuxBar geworfen, die (speziell für Admins, nicht für
 * Endbenutzer) einen entsprechenden Button besitzt.
 */
public class OnDumpInfo extends BasicEvent 
{

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      final String title = L.m("Fehlerinfos erstellen");

      String name = WollMuxFiles.dumpInfo();

      if (name != null)
        ModalDialogs.showInfoModal(
          title,
          L.m(
            "Die Fehlerinformationen des WollMux wurden erfolgreich in die Datei '%1' geschrieben.",
            name));
      else
        ModalDialogs.showInfoModal(
          title,
          L.m("Die Fehlerinformationen des WollMux konnten nicht geschrieben werden\n\nDetails siehe Datei wollmux.log!"));
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }