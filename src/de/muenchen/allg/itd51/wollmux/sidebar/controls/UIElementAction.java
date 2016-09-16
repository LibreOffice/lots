package de.muenchen.allg.itd51.wollmux.sidebar.controls;

import de.muenchen.allg.itd51.wollmux.dialog.UIElementEventHandler;

public class UIElementAction
{
  private UIElementEventHandler handler;

  private UIControl<?> control;

  private String eventType;

  private Object[] args;

  private boolean takeFocus;

  public UIElementAction(UIElementEventHandler handler,
      boolean takeFocus, String eventType, Object[] args)
  {
    this.handler = handler;
    this.takeFocus = takeFocus;
    this.eventType = eventType;
    this.args = args;
  }

  public UIControl<?> getControl()
  {
    return control;
  }

  public void setControl(UIControl<?> control)
  {
    this.control = control;
  }

  public void performAction()
  {
    if (takeFocus && !control.hasFocus()) control.takeFocus();
    handler.processUiElementEvent(null, eventType, args);
  }
}
