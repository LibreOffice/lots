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
package de.muenchen.allg.itd51.wollmux.document;

/**
 * Wird geworfen, wenn das verwendete OpenOffice.org das RDF-Metadaten-Interface
 * noch nicht unterstützt.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class RDFMetadataNotSupportedException extends Exception
{
  private static final long serialVersionUID = 2416952716636541797L;

  public RDFMetadataNotSupportedException()
  {
    super();
  }

  public RDFMetadataNotSupportedException(Throwable cause)
  {
    super(cause);
  }
}
