package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class GroupBookmarkNode extends WollmuxBookmarkNode
{
  public GroupBookmarkNode(Bookmark bookmark, boolean isStart, ConfigThingy conf)
  {
    super(bookmark, isStart, conf);
  }
}