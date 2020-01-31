package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.uno.RuntimeException;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Basic implementation of {@link WollMuxEvent}.
 */
public abstract class WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxEvent.class);

  /**
   * Process this event. Should only be called from the event processor. Shows a dialog for
   * {@link WollMuxFehlerException}s.
   */
  @SuppressWarnings("squid:S1181")
  public void process()
  {
    LOGGER.debug("Process WollMuxEvent {}", this);
    try
    {
      doit();
    } catch (WollMuxFehlerException e)
    {
      errorMessage(e);
    }
    catch (Throwable t)
    {
      LOGGER.error("", t);
    }
  }

  /**
   * Put this event on the event queue.
   */
  public void emit()
  {
    WollMuxEventHandler.getInstance().handle(this);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName();
  }

  /**
   * Logs an exception and show a dialog.
   *
   * @param t
   *          The exception.
   */
  protected void errorMessage(Throwable t)
  {
    LOGGER.error("", t);

    /*
     * No dialog if it's a RuntimeException because (1) user don't know what to do with the
     * information (2) they typically occur if the document is closed before processing finished.
     */
    if (t.getCause() instanceof RuntimeException)
    {
      return;
    }

    InfoDialog.showInfoModal(L.m("WollMux-Fehler"), "Leider ist ein Fehler aufgetreten. Die genaue Fehlerbeschreibung steht im Log. Bitte wenden Sie sich an ihren Administrator.");
  }

  /**
   * The task associated with this event should be implemented here. All exception which shouldn't
   * be visible to the user have to be handled by this method.
   *
   * @throws WollMuxFehlerException
   *           A user visible exception occured.
   */
  protected abstract void doit() throws WollMuxFehlerException;

  /**
   * Tries to stabilize the LibreOffice and WollMux by calling the garbage collector.
   *
   * Sometimes memory leaks after exhaustive use of UNO objects have been noticed.
   */
  protected void stabilize()
  {
    System.gc();
  }
}