package de.muenchen.allg.itd51.wollmux.former.document.nodes;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Bookmark;
import de.muenchen.allg.itd51.wollmux.former.document.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.former.document.Visitor;

public class InsertionBookmarkNode extends WollmuxBookmarkNode implements
    InsertionBookmark
{
  public InsertionBookmarkNode(Bookmark bookmark, boolean isStart,
      ConfigThingy conf)
  {
    super(bookmark, isStart, conf);
  }

  @Override
  public boolean visit(Visitor visit)
  {
    return visit.insertionBookmark(this);
  }
}