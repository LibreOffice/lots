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
package de.muenchen.allg.itd51.wollmux.config.generator.xml;

/**
 * A Exception produced by the generator.
 * 
 * @author Daniel Sikeler
 */
public class XMLGeneratorException extends Exception
{

  /** Serial version ID. */
  private static final long serialVersionUID = 6474901851185961604L;

  /**
   * Constructor for generator exceptions.
   * 
   * @param message
   *          The description of the exception.
   */
  public XMLGeneratorException(final String message)
  {
    super(message);
  }

  /**
   * Constructor for generator exceptions.
   * 
   * @param message
   *          Description of the exception.
   * @param cause
   *          The exception which caused this one.
   */
  public XMLGeneratorException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

}
