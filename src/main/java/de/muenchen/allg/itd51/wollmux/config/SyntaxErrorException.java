/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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

/**
 * Signalisiert einen Fehler in einer zu parsenden Zeichenfolge
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SyntaxErrorException extends Exception
{
  /**
   * keine Ahnung was das soll, aber es macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 7215084024054862356L;
  public SyntaxErrorException() {super();}
  public SyntaxErrorException(String message) {super(message);}
  public SyntaxErrorException(String message, Throwable cause) {super(message,cause);}
  public SyntaxErrorException(Throwable cause) {super(cause);}
}
