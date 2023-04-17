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
package de.muenchen.allg.itd51.wollmux.func;

/**
 * A value that is available as different data types
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Value
{
  /**
   * The current value as a string. If it is a boolean value,
   * the string "true" or "false" is returned.
   */
  public String getString();

  /**
   * The current value as boolean. If the value is a string by nature,
   * so the result depends on the specific implementation.
   */
  public boolean getBoolean();
}
