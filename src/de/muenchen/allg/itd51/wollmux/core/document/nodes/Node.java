package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import java.util.Iterator;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;

/**
 * Oberklasse für die Knoten des Dokumentbaums.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class Node
{
  protected Node() {}

  /**
   * Liefert einen Iterator über alle Kindknoten.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Iterator<Node> iterator()
  {
    return (new Vector<Node>(0)).iterator();
  }

  /**
   * Besucht den Knoten und falls es ein Container ist den ganzen Teilbaum mit
   * diesem Knoten als Wurzel. Es werden die entsprechenden Methoden des
   * {@link DocumentTreeVisitor}s visit aufgerufen.
   * 
   * @return false falls die entsprechende Methode von visit zurückliefert, dass
   *         keine weiteren Knoten mehr besucht werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean visit(DocumentTreeVisitor visit)
  {
    return true;
  }
}