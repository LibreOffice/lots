package de.muenchen.allg.itd51.wollmux.dialog.controls;

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

  public Component getComponent()
  {
    return box;
  }

  public String getString()
  {
    return getBoolean() ? "true" : "false";
  }

  public boolean getBoolean()
  {
    return box.isSelected();
  }

  public void setString(String str)
  {
    box.setSelected(str.equalsIgnoreCase("true"));
  }

  public boolean isStatic()
  {
    return false;
  }
}