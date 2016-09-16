package de.muenchen.allg.itd51.wollmux.former.document.nodes;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Bookmark;

public class GroupBookmarkNode extends WollmuxBookmarkNode
{
  public GroupBookmarkNode(Bookmark bookmark, boolean isStart, ConfigThingy conf)
  {
    super(bookmark, isStart, conf);
  }
}