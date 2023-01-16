/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.func.print;

/**
 * Exception for indicating any error during printing.
 */
public class PrintException extends Exception
{

  private static final long serialVersionUID = 8374980631449034838L;

  /**
   * Create a new exception with message and cause.
   *
   * @param message
   *          A human readable exception description.
   * @param cause
   *          The cause. (A null value is permitted, and indicates that the cause is nonexistent or
   *          unknown.
   */
  public PrintException(String message, Throwable cause)
  {
    super(message, cause);
  }

}
