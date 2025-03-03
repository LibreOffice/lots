/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.form.model;

/**
 * Listener which is called when a form value or form field state has changed.
 */
public interface FormValueChangedListener
{
  /**
   * Called if the return value of {@link Control#getValue()} has changed.
   *
   * @param id
   *          The ID of the control.
   * @param value
   *          The new value of the control.
   */
  public void valueChanged(String id, String value);

  /**
   * Called if the return value of {@link Control#isOkay()} has changed.
   *
   * @param id
   *          The ID of the control.
   * @param okay
   *          The new state of the control.
   */
  public void statusChanged(String id, boolean okay);
}
