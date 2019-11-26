package de.muenchen.allg.itd51.wollmux.core.document.nodes;

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