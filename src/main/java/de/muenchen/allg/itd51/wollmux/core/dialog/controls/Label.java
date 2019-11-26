package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Component;

import javax.swing.JLabel;

public class Label extends UIElementBase
{
  private JLabel component;

  public Label(String id, String label, Object layoutConstraints)
  {
    this.component = new JLabel(label);
    this.component.setFocusable(false);
    this.layoutConstraints = layoutConstraints;
    this.id = id;
  }

  @Override
  public Component getComponent()
  {
    return component;
  }

  @Override
  public String getString()
  {
    return "false";
  }

  @Override
  public boolean getBoolean()
  {
    return false;
  }

  @Override
  public boolean isStatic()
  {
    return true;
  }
}