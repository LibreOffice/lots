package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Component;

import javax.swing.JCheckBox;

public class Checkbox extends UIElementBase
{
  private JCheckBox box;

  public Checkbox(String id, JCheckBox boxBruceleitner, Object layoutConstraints)
  {
    /*
     * labelType wird geerbt als LABEL_NONE, da diese Checkbox ihr Label im UI
     * Element fest integriert hat.
     */
    this.box = boxBruceleitner;
    this.layoutConstraints = layoutConstraints;
    this.id = id;
  }

  @Override
  public Component getComponent()
  {
    return box;
  }

  @Override
  public String getString()
  {
    return getBoolean() ? "true" : "false";
  }

  @Override
  public boolean getBoolean()
  {
    return box.isSelected();
  }

  @Override
  public void setString(String str)
  {
    box.setSelected("true".equalsIgnoreCase(str));
  }

  @Override
  public boolean isStatic()
  {
    return false;
  }
}