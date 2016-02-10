package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Component;

import javax.swing.AbstractButton;

public class Button extends UIElementBase
{
  private AbstractButton button;

  public Button(String id, AbstractButton button, Object layoutConstraints)
  {
    this.button = button;
    this.layoutConstraints = layoutConstraints;
    this.id = id;
  }

  public Component getComponent()
  {
    return button;
  }

  public String getString()
  {
    return "false";
  }

  public boolean getBoolean()
  {
    return false;
  }

  public boolean isStatic()
  {
    return true;
  }
}