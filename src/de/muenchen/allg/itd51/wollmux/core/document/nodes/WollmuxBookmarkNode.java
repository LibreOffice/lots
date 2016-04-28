package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class WollmuxBookmarkNode extends BookmarkNode
{
  protected ConfigThingy conf;

  public WollmuxBookmarkNode(Bookmark bookmark, boolean isStart, ConfigThingy conf)
  {
    super(bookmark, isStart);
    this.conf = conf;
  }
}