package de.muenchen.allg.itd51.wollmux.sidebar.controls;

public class UIButton implements UIControl<Boolean>
{
  private String id;
  private String label;
  private UIElementAction action;
  
  public UIButton(String id, String label, UIElementAction action) {
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
  public Boolean getValue()
  {
    return false;
  }

  @Override
  public void setValue(Boolean value)
  {
  }
  
  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label)
  {
    this.label = label;
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

  @Override
  public UIElementAction getAction()
  {
    return action;
  }
}
