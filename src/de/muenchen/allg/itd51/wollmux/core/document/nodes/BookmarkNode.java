package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;

public class BookmarkNode extends Node
{
  private boolean isStart;

  private Bookmark bookmark;

  public BookmarkNode(Bookmark bookmark, boolean isStart)
  {
    super();
    this.bookmark = bookmark;
    this.isStart = isStart;
  }

  public String getName()
  {
    return bookmark.getName();
  }

  public boolean isStart()
  {
    return isStart;
  }

  @Override
  public String toString()
  {
    return "Bookmark '" + bookmark.getName() + (isStart ? "' Start" : "' End");
  }
}