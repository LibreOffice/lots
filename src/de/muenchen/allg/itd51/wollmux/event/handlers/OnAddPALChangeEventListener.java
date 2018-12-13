package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.form.binding.InvalidBindingStateException;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dieses Event wird ausgelöst, wenn sich ein externer PALChangeEventListener beim
 * WollMux-Service registriert. Es sorgt dafür, dass der PALChangeEventListener in
 * die Liste der registrierten PALChangeEventListener im WollMuxSingleton
 * aufgenommen wird.
 *
 * @author christoph.lutz
 */
public class OnAddPALChangeEventListener extends BasicEvent
{
  private XPALChangeEventListener listener;

  private Integer wollmuxConfHashCode;

  public OnAddPALChangeEventListener(XPALChangeEventListener listener,
      Integer wollmuxConfHashCode)
  {
    this.listener = listener;
    this.wollmuxConfHashCode = wollmuxConfHashCode;
  }

  @Override
  protected void doit()
  {
    PersoenlicheAbsenderliste.getInstance().addPALChangeEventListener(listener);

    WollMuxEventHandler.getInstance().handlePALChangedNotify();

    // Konsistenzprüfung: Stimmt WollMux-Konfiguration der entfernten
    // Komponente mit meiner Konfiguration überein? Ansonsten Fehlermeldung.
    if (wollmuxConfHashCode != null)
    {
      int myWmConfHash = WollMuxFiles.getWollmuxConf().stringRepresentation()
          .hashCode();
      if (myWmConfHash != wollmuxConfHashCode.intValue())
        errorMessage(new InvalidBindingStateException(
            L.m("Die Konfiguration des WollMux muss neu eingelesen werden.\n\nBitte beenden Sie den WollMux und OpenOffice.org und schießen Sie alle laufenden 'soffice.bin'-Prozesse über den Taskmanager ab.")));
    }

  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}
