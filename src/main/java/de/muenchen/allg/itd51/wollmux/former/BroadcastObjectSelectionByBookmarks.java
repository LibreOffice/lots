package de.muenchen.allg.itd51.wollmux.former;

import java.util.Set;

public class BroadcastObjectSelectionByBookmarks implements Broadcast
{
  private Set<String> bookmarkNames;

  public BroadcastObjectSelectionByBookmarks(Set<String> bookmarkNames)
  {
    this.bookmarkNames = bookmarkNames;
  }

  public void sendTo(BroadcastListener listener)
  {
    listener.broadcastBookmarkSelection(bookmarkNames);
  }

}
