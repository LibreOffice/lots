package de.muenchen.allg.itd51.wollmux.former.document;

import java.util.Collection;

import de.muenchen.allg.itd51.wollmux.former.document.nodes.ContainerNode;
import de.muenchen.allg.itd51.wollmux.former.document.nodes.Node;

public class ParagraphNode extends ContainerNode
{
  public ParagraphNode(Collection<Node> children)
  {
    super(children);
  }

  public String toString()
  {
    return "PARAGRAPH";
  }

  public int getType()
  {
    return DocumentTree.PARAGRAPH_TYPE;
  }
}