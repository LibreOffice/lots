package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Component;

public class Box extends UIElementBase
{
  private Component jackInTheBox;

  public Box(String id, Component jackInTheBox, Object layoutConstraints)
  {
    this.jackInTheBox = jackInTheBox;
    this.jackInTheBox.setFocusable(false);
    this.layoutConstraints = layoutConstraints;
    this.id = id;
  }

  @Override
  public Component getComponent()
  {
    return jackInTheBox;
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