package de.muenchen.allg.itd51.wollmux.sidebar.controls;

public class UIMenuItem extends UIButton
{
  private String parent;
  
  public UIMenuItem(String id, String label, String parent, UIElementAction action)
  {
    super(id, label, action);
    
    this.parent = parent;
  }

  public String getParent()
  {
    return parent;
  }
}
