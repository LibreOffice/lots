package de.muenchen.allg.itd51.wollmux.former.document;

/**
 * Wird von Nodes implementiert, die Formularsteuerelemente darstellen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormControl
{
  /**
   * Liefert die Art des Steuerelements, z,B, {@link DocumentTree#CHECKBOX_CONTROL}.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getType();

  /**
   * Liefert einen String, der das Steuerelement beschreibt. Bei Eingabefeldern ist
   * dies z.B. der "Hinweis"-Text.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getDescriptor();

  /**
   * Legt ein Bookmark mit gewünschtem Namen bmName um das Steuerelement und
   * liefert den Namen mit dem das Bookmark tatsächlich erzeugt wurde zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String surroundWithBookmark(String bmName);

  /**
   * Liefert den aktuell im Steuerelement eingestellten Wert zurück. Boolesche
   * Steuerelemente (Checkbox) liefern "true" oder "false".
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString();
}