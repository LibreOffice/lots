package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Component;

import javax.swing.JSeparator;

public class Separator extends UIElementBase
{
  private JSeparator seppl;

  public Separator(String id, JSeparator wurzelSepp, Object layoutConstraints)
  {
    this.layoutConstraints = layoutConstraints;
    this.seppl = wurzelSepp;
    this.id = id;
  }

  public Component getComponent()
  {
    return seppl;
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