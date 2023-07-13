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

import java.awt.event.ActionListener;

import org.libreoffice.lots.WollMuxFehlerException;

/**
 * Event for notification that the form model as updated all fields.
 */
public class OnSetFormValueFinished extends WollMuxEvent
{
  private ActionListener listener;

  /**
   * Create this event.
   *
   * @param unlockActionListener
   *          The listener to notify.
   */
  public OnSetFormValueFinished(ActionListener unlockActionListener)
  {
    this.listener = unlockActionListener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (listener != null)
      listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}
