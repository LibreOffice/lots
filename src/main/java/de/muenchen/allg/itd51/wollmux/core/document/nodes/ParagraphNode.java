package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import java.util.Collection;

public class ParagraphNode extends ContainerNode
{
  public ParagraphNode(Collection<Node> children)
  {
    super(children);
  }

  @Override
  public String toString()
  {
    return "PARAGRAPH";
  }

  @Override
  public Container.Type getType()
  {
    return Container.Type.PARAGRAPH;
  }
}