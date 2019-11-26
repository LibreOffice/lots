package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class Textfield extends UIElementBase
{
  private JTextField tf;

  public Textfield(String id, JTextField tf, Object layoutConstraints,
      UIElement.LabelPosition labelType, String label, Object labelLayoutConstraints)
  {
    this.tf = tf;
    this.layoutConstraints = layoutConstraints;
    this.labelLayoutConstraints = labelLayoutConstraints;
    this.label = new JLabel(label);
    this.labelType = labelType;
    this.id = id;
  }

  @Override
  public Component getComponent()
  {
    return tf;
  }

  @Override
  public String getString()
  {
    return tf.getText();
  }

  @Override
  public boolean getBoolean()
  {
    return !getString().isEmpty();
  }

  @Override
  public void setString(String str)
  {
    tf.setText(str);
  }

  @Override
  public boolean isStatic()
  {
    return false;
  }
}