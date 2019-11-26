package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Component;

import javax.swing.JSeparator;

public class Separator extends UIElementBase
{
  private JSeparator seppl;

  public Separator(String id, JSeparator wurzelSepp, Object layoutConstraints)
  {
    this.layoutConstraints = layoutConstraints;
    this.seppl = wurzelSepp;
    this.seppl.setFocusable(false);
    this.id = id;
  }

  @Override
  public Component getComponent()
  {
    return seppl;
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