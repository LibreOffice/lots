//TODO L.m()
/*
* Dateiname: BroadcastViewVisibilitySettings.java
* Projekt  : WollMux
* Funktion : Änderung des ViewVisibilityDescriptors.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 19.07.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Änderung des {@link de.muenchen.allg.itd51.wollmux.former.ViewVisibilityDescriptor}s.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class BroadcastViewVisibilitySettings implements Broadcast
{
  private ViewVisibilityDescriptor desc;
  
  public BroadcastViewVisibilitySettings(ViewVisibilityDescriptor desc)
  {
    this.desc = desc;
  }
  public void sendTo(BroadcastListener listener)
  {
    listener.broadcastViewVisibilitySettings(desc);
  }
}
