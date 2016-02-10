package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Component;

import javax.swing.JLabel;

public class Label extends UIElementBase
{
  private JLabel component;

  public Label(String id, String label, Object layoutConstraints)
  {
    this.component = new JLabel(label);
    this.layoutConstraints = layoutConstraints;
    this.id = id;
  }

  public Component getComponent()
  {
    return component;
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