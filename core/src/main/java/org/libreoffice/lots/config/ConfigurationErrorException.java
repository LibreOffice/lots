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
package org.libreoffice.lots.config;

/**
 * Is thrown when a misconfiguration is detected (user error)
 */
public class ConfigurationErrorException extends RuntimeException
{
  private static final long serialVersionUID = -2457549809413613658L;
  public ConfigurationErrorException() {super();}
  public ConfigurationErrorException(String message) {super(message);}
  public ConfigurationErrorException(String message, Throwable cause) {super(message,cause);}
  public ConfigurationErrorException(Throwable cause) {super(cause);}
}
