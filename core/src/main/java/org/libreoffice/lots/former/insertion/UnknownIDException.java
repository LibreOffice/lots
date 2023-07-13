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
package org.libreoffice.lots.former.insertion;

/**
 * Wird geworfen, wenn versucht wird, eine ID zu verwenden, die dem System nicht bekannt ist.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UnknownIDException extends Exception
{
  /**
   * keine Ahnung, was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = -6185698424679725505L;

  public UnknownIDException()
  {
  }

  /**
   * New unknown ID exception.
   *
   * @param message
   *          The exception message.
   */
  public UnknownIDException(String message)
  {
    super(message);
  }
}
