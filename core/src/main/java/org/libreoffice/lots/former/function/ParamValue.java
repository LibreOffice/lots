/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.former.function;

import org.libreoffice.lots.former.model.IdModel;

/**
 * Repräsentiert einen vom Benutzer konfigurierten Parameter für eine Funktion.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ParamValue
{
  private enum ParamValueType
  {
    /**
     * Der Wert wurde vom Benutzer nicht spezifiziert.
     */
    UNSPECIFIED,

    /**
     * Als Wert soll der Wert des Feldes mit ID {@link #fieldId} verwendet werden.
     */
    FIELD,

    /**
     * Als Wert soll {@link #literal} als Literal verwendet werden.
     */
    LITERAL;
  }

  /**
   * Falls {@link #type} == {@link #LITERAL} enthält dies ein Literal.
   */
  private String literal;

  /**
   * Falls {@link #type} == {@link #FIELD}, dann speichert dies die ID.
   */
  private IdModel fieldId = null;

  /**
   * Der Typ dieses Parameter-Wertes.
   */
  private ParamValueType type;

  private ParamValue(ParamValueType type, String str, IdModel fieldId)
  {
    this.type = type;
    this.literal = str;
    this.fieldId = fieldId;
  }

  /**
   * Copy Constructor.
   *
   * @param orig
   *          The original {@link ParamValue},
   */
  public ParamValue(ParamValue orig)
  {
    this.literal = orig.literal;
    this.type = orig.type;
    this.fieldId = orig.fieldId;
  }

  /**
   * Liefert true gdw dieser Parameterwert unspezifiziert ist.
   */
  public boolean isUnspecified()
  {
    return type == ParamValueType.UNSPECIFIED;
  }

  /**
   * Liefert true gdw dieser Parameterwert eine Referenz auf ein Feld mit ID
   * {@link #getString()} ist.
   */
  public boolean isFieldReference()
  {
    return type == ParamValueType.FIELD;
  }

  /**
   * Liefert true gdw dieser Parameterwert der literale String {@link #getString()}
   * ist.
   */
  public boolean isLiteral()
  {
    return type == ParamValueType.LITERAL;
  }

  /**
   * Liefert die Feld ID, wenn {@link #isFieldReference()}, den literalen String,
   * wenn {@link #isLiteral()} und ansonsten null.
   */
  public String getString()
  {
    if (isUnspecified()) return null;
    if (isFieldReference()) return fieldId.toString();
    return literal;
  }

  /**
   * Liefert einen unspezifizierten ParamValue.
   */
  public static ParamValue unspecified()
  {
    return new ParamValue(ParamValueType.UNSPECIFIED, "", null);
  }

  /**
   * Liefert einen ParamValue der eine Referenz auf das Feld mit ID id darstellt.
   */
  public static ParamValue field(IdModel id)
  {
    return new ParamValue(ParamValueType.FIELD, "", id);
  }

  /**
   * Liefert einen ParamValue, der das String-Literal str darstellt.
   */
  public static ParamValue literal(String str)
  {
    return new ParamValue(ParamValueType.LITERAL, str, null);
  }
}
