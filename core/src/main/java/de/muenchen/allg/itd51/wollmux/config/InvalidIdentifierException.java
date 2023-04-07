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
package de.muenchen.allg.itd51.wollmux.config;

import de.muenchen.allg.itd51.wollmux.util.L;

public class InvalidIdentifierException extends Exception
{
  private static final long serialVersionUID = 495666967644874471L;

  private final String invalidId;

  public InvalidIdentifierException(String invalidId)
  {
    this.invalidId = invalidId;
  }

  @Override
  public String getMessage()
  {
    return L.m("The identifier \"{0}\" is invalid, and can only contain the characters a-z, A-Z, _ and 0-9, "
        + "whereas the first character cannot be a number.", invalidId);
  }
}
