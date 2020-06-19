package de.muenchen.allg.itd51.wollmux.former;

/**
 * Änderung des
 * {@link de.muenchen.allg.itd51.wollmux.former.ViewVisibilityDescriptor}s.
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
