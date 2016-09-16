package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Component;

public class Box extends UIElementBase
{
  private Component jackInTheBox;

  public Box(String id, Component jackInTheBox, Object layoutConstraints)
  {
    this.jackInTheBox = jackInTheBox;
    this.layoutConstraints = layoutConstraints;
    this.id = id;
  }

  public Component getComponent()
  {
    return jackInTheBox;
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