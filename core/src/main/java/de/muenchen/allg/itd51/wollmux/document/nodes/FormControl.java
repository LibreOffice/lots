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
package de.muenchen.allg.itd51.wollmux.document.nodes;

import de.muenchen.allg.afid.UnoHelperException;

/**
 * Wird von Nodes implementiert, die Formularsteuerelemente darstellen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormControl
{
  /**
   * Types of {@link FormControl}.
   */
  public enum FormControlType
  {
    /**
     * Rückgabewert für {@link FormControl#getType()} im Falle einer Checkbox.
     */
    CHECKBOX_CONTROL,

    /**
     * Rückgabewert für {@link FormControl#getType()} im Falle einer Eingabeliste.
     */
    DROPDOWN_CONTROL,

    /**
     * Rückgabewert für {@link FormControl#getType()} im Falle eines Eingabefeldes.
     */
    INPUT_CONTROL;
  }


  /**
   * Liefert die Art des Steuerelements, z,B, {@link FormControlType#CHECKBOX_CONTROL}.
   *
   * @return Art des Steuerelements.
   */
  public FormControlType getType();

  /**
   * Liefert einen String, der das Steuerelement beschreibt. Bei Eingabefeldern ist
   * dies z.B. der "Hinweis"-Text.
   */
  public String getDescriptor();

  /**
   * Legt ein Bookmark mit gewünschtem Namen bmName um das Steuerelement und liefert den Namen mit
   * dem das Bookmark tatsächlich erzeugt wurde zurück.
   *
   * @param bmName
   *          The name of the book mark.
   * @return The name of the book mark.
   * 
   * @throws UnoHelperException
   *           Can't create a book mark.
   */
  public String surroundWithBookmark(String bmName) throws UnoHelperException;

  /**
   * Liefert den aktuell im Steuerelement eingestellten Wert zurück. Boolesche
   * Steuerelemente (Checkbox) liefern "true" oder "false".
   */
  public String getString();
}
