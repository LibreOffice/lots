package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;

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
    // Dialog modal starten:
    new PersoenlicheAbsenderlisteVerwalten(DatasourceJoinerFactory.getDatasourceJoiner(), null);
  }
}