/*
* Dateiname: ParamValue.java
* Projekt  : WollMux
* Funktion : Repräsentiert einen vom Benutzer konfigurierten Parameter für eine Funktion.
* 
* Copyright: Landeshauptstadt München
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
   * Als Wert soll der Wert des Feldes mit ID {@link #idStr} verwendet werden.
   */
  public static final int FIELD = 1;
  /**
   * Als Wert soll {@link #idStr} als Literal verwendet werden.
   */
  public static final int LITERAL = 2;
  
  /**
   * Je nach {@link #type} enthält dies ein Literal oder eine Feld-ID.
   */
  private String idStr;
  
  /**
   * Der Typ dieses Parameter-Wertes.
   */
  private int type;
  
  private ParamValue(int type, String str)
  {
    this.type = type;
    this.idStr = str;
  }
  
  /**
   * Copy Constructor.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ParamValue(ParamValue orig)
  {
    this.idStr = orig.idStr;
    this.type = orig.type;
  }
  
  /**
   * Liefert true gdw dieser Parameterwert unspezifiziert ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isUnspecified()
  {
    return type == UNSPECIFIED;
  }
  
  /**
   * Liefert true gdw dieser Parameterwert eine Referenz auf ein Feld mit ID
   * {@link #getString()} ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isFieldReference()
  {
    return type == FIELD; 
  }
  
  /**
   * Liefert true gdw dieser Parameterwert der literale String {@link #getString()} ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isLiteral()
  {
    return type == LITERAL;
  }
  
  /**
   * Liefert die Feld ID, wenn {@link #isFieldReference()}, den literalen String, wenn
   * {@link #isLiteral()} und ansonsten null.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString()
  {
    if (isUnspecified()) return null;
    return idStr;
  }
  
  /**
   * Macht diesen Parameter zu einer Referenz auf das Feld mit ID id.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setFieldReference(String id)
  {
    idStr = id;
    type = FIELD;
  }
  
  /**
   * Liefert einen unspezifizierten ParamValue.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue unspecified()
  {
    return new ParamValue(UNSPECIFIED,"");
  }
  
  /**
   * Liefert einen ParamValue der eine Referenz auf das Feld mit ID id darstellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue field(String id)
  {
    return new ParamValue(FIELD,id);
  }
  
  /**
   * Liefert einen ParamValue, der das String-Literal str darstellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static ParamValue literal(String str)
  {
    return new ParamValue(LITERAL,str);
  }
}
