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
package de.muenchen.allg.itd51.wollmux.config.scanner;

import java.io.IOException;

/**
 * Exception class for the scanner module.
 *
 * @author Daniel Sikeler
 */
public class ScannerException extends IOException
{

  /** Serial Version ID. */
  private static final long serialVersionUID = 6441716168137433335L;

  /**
   * Constructor for scanner exceptions.
   *
   * @param message
   *          Description of the error.
   */
  public ScannerException(final String message)
  {
    super(message);
  }

  /**
   * Constructor for scanner exceptions.
   *
   * @param cause
   *          The error, which preceeds this exception.
   */
  public ScannerException(final Throwable cause)
  {
    super(cause);
  }

  /**
   * Constructor for scanner exceptions.
   *
   * @param message
   *          Description of the error.
   * @param cause
   *          An other exception, which caused this one.
   */
  public ScannerException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

}
