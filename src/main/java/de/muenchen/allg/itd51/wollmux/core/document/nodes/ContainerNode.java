package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import java.util.Collection;
import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;

/**
 * Oberklasse für Knoten, die Nachfahren haben können (z,B, Absätze).
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ContainerNode implements Container, Node
{
  private Collection<Node> children;

  public ContainerNode(Collection<Node> children)
  {
    super();
    this.children = children;
  }

  @Override
  public Iterator<Node> iterator()
  {
    return children.iterator();
  }

  @Override
  public String toString()
  {
    return "CONTAINER";
  }

  @Override
  public Container.Type getType()
  {
    return Container.Type.CONTAINER;
  }

  @Override
  public boolean visit(DocumentTreeVisitor visit)
  {
    if (!visit.container(this, 0)) {
      return false;
    }

    Iterator<Node> iter = iterator();
    while (iter.hasNext())
    {
      if (!iter.next().visit(visit)) {
        return false;
      }
    }
    if (!visit.container(this, 1)) {
      return false;
    }
    return true;
  }
}