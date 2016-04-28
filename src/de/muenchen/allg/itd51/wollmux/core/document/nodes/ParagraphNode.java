package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import java.util.Collection;

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
    return PARAGRAPH_TYPE;
  }
}