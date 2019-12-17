package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;

/**
 * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
 * WollMuxEventHandler ausgelöst und sorgt dafür, dass der Dialog AbsenderAuswählen
 * gestartet wird.
 *
 * @author christoph.lutz
 */
public class OnShowDialogAbsenderAuswaehlen extends BasicEvent
{
  @Override
  protected void doit() throws WollMuxFehlerException
  {
    new AbsenderAuswaehlen(DatasourceJoinerFactory.getDatasourceJoiner());
  }
}