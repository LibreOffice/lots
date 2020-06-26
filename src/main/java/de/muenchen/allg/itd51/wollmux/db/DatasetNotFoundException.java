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
package de.muenchen.allg.itd51.wollmux.db;

/**
 * Wird geworfen, falls kein passender Datensatz gefunden wurde und
 * die Möglichkeit, eine leere Ergebnisliste zurückzugeben nicht
 * existiert. 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasetNotFoundException extends Exception
{
  private static final long serialVersionUID = -2369645834768269537L;
  public DatasetNotFoundException() {super();}
  public DatasetNotFoundException(String message) {super(message);}
  public DatasetNotFoundException(String message, Throwable cause) {super(message,cause);}
  public DatasetNotFoundException(Throwable cause) {super(cause);}
}
