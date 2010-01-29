/*
 * Dateiname: ParamValue.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert einen vom Benutzer konfigurierten Parameter für eine Funktion.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 25.09.2006 | BNK | Erstellung
 * 16.03.2007 | BNK | +setFieldReference()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.former.function;

import de.muenchen.allg.itd51.wollmux.former.IDManager;

/**
 * Repräsentiert einen vom Benutzer konfigurierten Parameter für eine Funktion.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ParamValue
{
  /**
   * Der Wert wurde vom Benutzer nicht spezifiziert.
   */
  public static final int UNSPECIFIED = 0;

  /**
   * Als Wert soll der Wert des Feldes mit ID {@link #fieldId} verwendet werden.
   */
  public static final int FIELD = 1;

  /**
   * Als Wert soll {@link #literal} als Literal verwendet werden.
   */
  public static final int LITERAL = 2;

  /**
   * Falls {@link #type} == {@link #LITERAL} enthält dies ein Literal.
   */
  private String literal;

  /**
   * Falls {@link #type} == {@link #FIELD}, dann speichert dies die ID.
   */
  private IDManager.ID fieldId = null;

  /**
   * Der Typ dieses Parameter-Wertes.
   */
  private int type;

  private ParamValue(int type, String str, IDManager.ID fieldId)
  {
    this.type = type;
    this.literal = str;
    this.fieldId = fieldId;
  }

  /**
   * Copy Constructor.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ParamValue(ParamValue orig)
  {
    this.literal = orig.literal;
    this.type = orig.type;
    this.fieldId = orig.fieldId;
  }

  /**
   * Liefert true gdw dieser Parameterwert unspezifiziert ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isUnspecified()
  {
    return type == UNSPECIFIED;
  }

  /**
   * Liefert true gdw dieser Parameterwert eine Referenz auf ein Feld mit ID
   * {@link #getString()} ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isFieldReference()
  {
    return type == FIELD;
  }

  /**
   * Liefert true gdw dieser Parameterwert der literale String {@link #getString()}
   * ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isLiteral()
  {
    return type == LITERAL;
  }

  /**
   * Liefert die Feld ID, wenn {@link #isFieldReference()}, den literalen String,
   * wenn {@link #isLiteral()} und ansonsten null.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString()
  {
    if (isUnspecified()) return null;
    if (isFieldReference()) return fieldId.toString();
    return literal;
  }

  /**
   * Liefert einen unspezifizierten ParamValue.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue unspecified()
  {
    return new ParamValue(UNSPECIFIED, "", null);
  }

  /**
   * Liefert einen ParamValue der eine Referenz auf das Feld mit ID id darstellt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue field(IDManager.ID id)
  {
    return new ParamValue(FIELD, "", id);
  }

  /**
   * Liefert einen ParamValue, der das String-Literal str darstellt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue literal(String str)
  {
    return new ParamValue(LITERAL, str, null);
  }
}
