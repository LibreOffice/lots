package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.core.document.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class InsertionBookmarkNode extends WollmuxBookmarkNode implements
    InsertionBookmark
{
  public InsertionBookmarkNode(Bookmark bookmark, boolean isStart,
      ConfigThingy conf)
  {
    super(bookmark, isStart, conf);
  }

  @Override
  public boolean visit(DocumentTreeVisitor visit)
  {
    return visit.insertionBookmark(this);
  }
}