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
package org.libreoffice.lots.document.nodes;

/**
 * Implementiert von Knoten, die Nachfahren haben können, z,B, Absätzen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Container
{
  public enum Type
  {
    /**
     * Rückgabewert für {@link #getType()} falls die Art des Containers nicht
     * näher bestimmt ist.
     */
    CONTAINER,

    /**
     * Rückgabewert für {@link #getType()} falls der Container ein Absatz ist.
     */
    PARAGRAPH;
  }

  /**
   * Liefert die Art des Knotens, z,B, {@link Type#PARAGRAPH}.
   */
  public Type getType();
}
