package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.AbsenderAuswaehlen;

/**
 * Event for showing the dialog for selecting a sender.
 */
public class OnShowDialogAbsenderAuswaehlen extends WollMuxEvent
{

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    new AbsenderAuswaehlen(DatasourceJoinerFactory.getDatasourceJoiner());
  }
}