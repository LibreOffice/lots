package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTextArea;

public class Textarea extends UIElementBase
{
  private JTextArea textarea;

  private Component textAreaComponent;

  public Textarea(String id, JTextArea textarea, Component textAreaComponent,
      Object layoutConstraints, Integer labelType, String label,
      Object labelLayoutConstraints)
  {
    this.textarea = textarea;
    this.textAreaComponent = textAreaComponent;
    this.layoutConstraints = layoutConstraints;
    this.labelLayoutConstraints = labelLayoutConstraints;
    this.label = new JLabel(label);
    this.labelType = labelType;
    this.id = id;
  }

  public Component getComponent()
  {
    return textAreaComponent;
  }

  /**
   * Da getComponent das Panel zurückliefert, in dem sich die Textarea befindet,
   * gibt diese Funktion das eigentliche JTextArea-Objekt zurück.
   * 
   * @return
   * @author Andor Ertsey (D-III-ITD-D101)
   */
  public JTextArea getTextArea()
  {
    return textarea;
  }

  public String getString()
  {
    return textarea.getText();
  }

  public boolean getBoolean()
  {
    return !getString().equals("");
  }

  public void setString(String str)
  {
    textarea.setText(str);
  }

  public void setBackground(Color bg)
  {
    textarea.setBackground(bg);
  }

  public boolean isStatic()
  {
    return false;
  }
}