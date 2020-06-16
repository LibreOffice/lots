package de.muenchen.allg.itd51.wollmux.form.model;

/**
 * Listener which is called when a form value or form field state has changed.
 */
public interface FormValueChangedListener
{
  /**
   * Called if the return value of {@link Control#getValue()} has changed.
   *
   * @param id
   *          The ID of the control.
   * @param value
   *          The new value of the control.
   */
  public void valueChanged(String id, String value);

  /**
   * Called if the return value of {@link Control#isOkay()} has changed.
   *
   * @param id
   *          The ID of the control.
   * @param okay
   *          The new state of the control.
   */
  public void statusChanged(String id, boolean okay);
}
