package de.muenchen.allg.itd51.wollmux.sidebar.controls;

public class UITextfield implements UIControl<String>
{
  private String id;
  private String label;
  private UIElementAction action;

  public UITextfield(String id, String label, UIElementAction action) {
    this.id = id;
    this.label = label;
    this.action = action;
  }
  
  @Override
  public String getId()
  {
    return id;
  }

  @Override
  public String getValue()
  {
    return null;
  }

  @Override
  public UIElementAction getAction()
  {
    return action;
  }

  @Override
  public void setValue(String value)
  {
  }

  @Override
  public boolean hasFocus()
  {
    return false;
  }

  @Override
  public void takeFocus()
  {
  }
}
