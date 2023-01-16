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
package de.muenchen.allg.itd51.wollmux.mailmerge;

import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;

/**
 * Exception for accessing a {@link DatasourceModel} without selection of a table.
 */
public class NoTableSelectedException extends Exception
{
  private static final long serialVersionUID = 495666967644874471L;

  @Override
  public String getMessage()
  {
    return "Es wurde keine Tabelle ausgewählt.";
  }
}
