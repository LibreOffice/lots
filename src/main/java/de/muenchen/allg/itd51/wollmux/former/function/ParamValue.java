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
    return type == UNSPECIFIED;
  }

  /**
   * Liefert true gdw dieser Parameterwert eine Referenz auf ein Feld mit ID
   * {@link #getString()} ist.
   */
  public boolean isFieldReference()
  {
    return type == FIELD;
  }

  /**
   * Liefert true gdw dieser Parameterwert der literale String {@link #getString()}
   * ist.
   */
  public boolean isLiteral()
  {
    return type == LITERAL;
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
    return new ParamValue(UNSPECIFIED, "", null);
  }

  /**
   * Liefert einen ParamValue der eine Referenz auf das Feld mit ID id darstellt.
   */
  public static ParamValue field(IDManager.ID id)
  {
    return new ParamValue(FIELD, "", id);
  }

  /**
   * Liefert einen ParamValue, der das String-Literal str darstellt.
   */
  public static ParamValue literal(String str)
  {
    return new ParamValue(LITERAL, str, null);
  }
}
