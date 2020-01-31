package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.PersoenlicheAbsenderlisteVerwalten;

/**
 * Event for showing the personal sender list.
 */
public class OnShowDialogPersoenlicheAbsenderlisteVerwalten extends WollMuxEvent
{

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    new PersoenlicheAbsenderlisteVerwalten(DatasourceJoinerFactory.getDatasourceJoiner(), null);
  }
}