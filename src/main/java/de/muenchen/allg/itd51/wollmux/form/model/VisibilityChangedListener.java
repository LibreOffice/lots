package de.muenchen.allg.itd51.wollmux.form.model;

/**
 * Listener which is called when a visibility has changed.
 */
public interface VisibilityChangedListener
{
  /**
   * Called when a visibility has changed.
   *
   * @param id
   *          The ID of the visibility group.
   * @param visible
   *          True if the group is visible, false otherwise.
   */
  public void visibilityChanged(String id, boolean visible);
}
