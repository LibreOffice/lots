package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dieses Event wird ausgelöst, wenn im WollMux-Service die methode setSender
 * aufgerufen wird. Es sort dafür, dass ein neuer Absender gesetzt wird.
 *
 * @author christoph.lutz
 */
public class OnSetSender extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnSetSender.class);

  private String senderName;

  private int idx;

  public OnSetSender(String senderName, int idx)
  {
    this.senderName = senderName;
    this.idx = idx;
  }

  @Override
  protected void doit()
  {
    String[] pal = PersoenlicheAbsenderliste.getInstance().getPALEntries();

    // nur den neuen Absender setzen, wenn index und sender übereinstimmen,
    // d.h.
    // die Absenderliste der entfernten WollMuxBar konsistent war.
    if (idx >= 0 && idx < pal.length && pal[idx].equals(senderName))
    {
      List<DJDataset> palDatasets = PersoenlicheAbsenderliste
          .getInstance().getSortedPALEntries();
      palDatasets.get(idx).select();
    } else
    {
      LOGGER.error(L.m(
          "Setzen des Senders '%1' schlug fehl, da der index '%2' nicht mit der PAL übereinstimmt (Inkonsistenzen?)",
          senderName, idx));
    }

    new OnPALChangedNotify().emit();
  }
}
