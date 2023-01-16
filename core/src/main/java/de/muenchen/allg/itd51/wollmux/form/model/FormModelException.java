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
package de.muenchen.allg.itd51.wollmux.form.model;

/**
 * An invalid form model.
 */
public class FormModelException extends Exception
{

  private static final long serialVersionUID = 5470806103796601018L;

  /**
   * An invalid form model.
   *
   * @param message
   *          The description of the error.
   * @param cause
   *          The causing exception.
   */
  public FormModelException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * An invalid form model.
   *
   * @param message
   *          The description of the error.
   */
  public FormModelException(String message)
  {
    super(message);
  }

}
