package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
 * WollMuxEventHandler ausgelöst und sorgt dafür, dass der Dialog
 * PersönlicheAbsendeliste-Verwalten gestartet wird.
 *
 * @author christoph.lutz
 */
public class OnShowDialogPersoenlicheAbsenderlisteVerwalten extends BasicEvent
{
  @Override
  protected void doit() throws WollMuxFehlerException
  {
    ConfigThingy conf = WollMuxFiles.getWollmuxConf();

    try
    {
      // Konfiguration auslesen:
      ConfigThingy PALconf = WollMuxEventHandler.getInstance()
          .requireLastSection(conf, "PersoenlicheAbsenderliste");

      // Dialog modal starten:
      new PersoenlicheAbsenderlisteVerwalten(PALconf,
          DatasourceJoinerFactory.getDatasourceJoiner(), null);
    } catch (Exception e)
    {
      throw new CantStartDialogException(e);
    }
  }
}