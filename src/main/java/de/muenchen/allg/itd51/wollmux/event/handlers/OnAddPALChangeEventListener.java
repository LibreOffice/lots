package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.form.binding.InvalidBindingStateException;

import de.muenchen.allg.itd51.wollmux.PersoenlicheAbsenderliste;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Add listener for changes in the personal sender list.
 */
public class OnAddPALChangeEventListener extends WollMuxEvent
{
  private XPALChangeEventListener listener;

  private Integer wollmuxConfHashCode;

  /**
   * Create this event.
   *
   * @param listener
   *          The listener to register.
   * @param wollmuxConfHashCode
   *          HashCode of the configuration used by the caller.
   */
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

    // Check if calling and my configuration are the same
    if (wollmuxConfHashCode != null)
    {
      int myWmConfHash = WollMuxFiles.getWollmuxConf().stringRepresentation().hashCode();
      if (myWmConfHash != wollmuxConfHashCode.intValue())
      {
        errorMessage(new InvalidBindingStateException(L.m(
            "Die Konfiguration des WollMux muss neu eingelesen werden.\n\nBitte beenden Sie den WollMux und Office und schießen Sie alle laufenden 'soffice.bin'-Prozesse über den Taskmanager ab.")));
      }
    }

  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}
