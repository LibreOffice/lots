package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.core.document.TextRange;

public class TextRangeNode implements TextRange, Node
{
  protected XTextRange range;

  public TextRangeNode(XTextRange range)
  {
    super();
    this.range = range;
  }

  @Override
  public String toString()
  {
    return "\"" + range.getString() + "\"";
  }

  @Override
  public boolean visit(DocumentTreeVisitor visit)
  {
    return visit.textRange(this);
  }

  @Override
  public String getString()
  {
    return range.getString();
  }
}