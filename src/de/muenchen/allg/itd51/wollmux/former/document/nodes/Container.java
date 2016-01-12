package de.muenchen.allg.itd51.wollmux.former.document.nodes;

import de.muenchen.allg.itd51.wollmux.former.document.DocumentTree;

/**
 * Implementiert von Knoten, die Nachfahren haben können, z,B, Absätzen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Container
{
  /**
   * Liefert die Art des Knotens, z,B, {@link DocumentTree#PARAGRAPH_TYPE}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getType();
}