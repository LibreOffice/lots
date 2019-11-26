package de.muenchen.allg.itd51.wollmux.sidebar.controls;

public class UIMenu extends UIButton
{
  private String parent = null;
  
  public UIMenu(String id, String label, UIElementAction action)
  {
    super(id, label, action);
  }
  
  public UIMenu(String id, String label, String parent, UIElementAction action)
  {
    this(id, label, action);
    this.parent = parent;
  }

  public String getParent()
  {
    return parent;
  }
}
