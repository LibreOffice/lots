/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Exception if a dialog can't start.
 */
public class CantStartDialogException extends WollMuxFehlerException
{
  private static final long serialVersionUID = -1130975078605219254L;

  /**
   * New exception.
   *
   * @param e
   *          The original exception.
   */
  public CantStartDialogException(Exception e)
  {
    super(
        L.m("Der Dialog konnte nicht gestartet werden!\n\nBitte kontaktieren Sie Ihre Systemadministration."),
        e);
  }
}
