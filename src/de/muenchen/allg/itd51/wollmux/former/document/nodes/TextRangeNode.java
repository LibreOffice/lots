package de.muenchen.allg.itd51.wollmux.former.document.nodes;

import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.former.document.TextRange;
import de.muenchen.allg.itd51.wollmux.former.document.Visitor;

public class TextRangeNode extends Node implements TextRange
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
  public boolean visit(Visitor visit)
  {
    return visit.textRange(this);
  }

  @Override
  public String getString()
  {
    return range.getString();
  }
}