package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import java.util.Collections;
import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;

/**
 * Oberklasse für die Knoten des Dokumentbaums.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Node extends Iterable<Node>
{
  /**
   * Liefert einen Iterator über alle Kindknoten.
   */
  @Override
  public default Iterator<Node> iterator()
  {
    return Collections.emptyIterator();
  }

  /**
   * Besucht den Knoten und falls es ein Container ist den ganzen Teilbaum mit
   * diesem Knoten als Wurzel. Es werden die entsprechenden Methoden des
   * {@link DocumentTreeVisitor}s visit aufgerufen.
   *
   * @return false falls die entsprechende Methode von visit zurückliefert, dass
   *         keine weiteren Knoten mehr besucht werden sollen.
   */
  public default boolean visit(DocumentTreeVisitor visit)
  {
    return true;
  }
}