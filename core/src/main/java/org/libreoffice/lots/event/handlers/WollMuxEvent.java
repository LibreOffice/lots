/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.event.handlers;

import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.event.WollMuxEventHandler;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.uno.RuntimeException;

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
      LOGGER.error("", t);
      return;
    }

    InfoDialog.showInfoModal(L.m("WollMux error"), L.m("Unfortunately an error happened. "
        + "The exact error description can be found in the log file. Please contact your system administrator."));//TODO
  }

  /**
   * The task associated with this event should be implemented here. All exception which shouldn't
   * be visible to the user have to be handled by this method.
   *
   * @throws WollMuxFehlerException
   *           A user visible exception occured.
   */
  protected abstract void doit() throws WollMuxFehlerException;
}
