package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class Textfield extends UIElementBase
{
  private JTextField textfield;

  public Textfield(String id, JTextField tf, Object layoutConstraints,
      Integer labelType, String label, Object labelLayoutConstraints)
  {
    this.textfield = tf;
    this.layoutConstraints = layoutConstraints;
    this.labelLayoutConstraints = labelLayoutConstraints;
    this.label = new JLabel(label);
    this.labelType = labelType;
    this.id = id;
  }

  public Component getComponent()
  {
    return textfield;
  }

  public String getString()
  {
    return textfield.getText();
  }

  public boolean getBoolean()
  {
    return !getString().equals("");
  }

  public void setString(String str)
  {
    textfield.setText(str);
  }

  public boolean isStatic()
  {
    return false;
  }
}