//TODO L.m()
/*
* Dateiname: BroadcastObjectSelectionByBookmarks.java
* Projekt  : WollMux
* Funktion : Nachricht, dass eine Menge von Bookmarks selektiert wurde.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 31.10.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Set;

public class BroadcastObjectSelectionByBookmarks implements Broadcast
{
  private Set bookmarkNames;
  
  public BroadcastObjectSelectionByBookmarks(Set bookmarkNames)
  {
    this.bookmarkNames = bookmarkNames;
  }

  public void sendTo(BroadcastListener listener)
  {
    listener.broadcastBookmarkSelection(bookmarkNames);
  }

}
