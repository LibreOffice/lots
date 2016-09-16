package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.wollmux.core.util.Logger;

public class Combobox extends UIElementBase
{
  private JComboBox<?> combo;

  public Combobox(String id, JComboBox<?> combo, Object layoutConstraints,
      Integer labelType, String label, Object labelLayoutConstraints)
  {
    this.combo = combo;
    this.layoutConstraints = layoutConstraints;
    this.labelLayoutConstraints = labelLayoutConstraints;
    this.label = new JLabel(label);
    this.labelType = labelType;
    this.id = id;
  }

  public void setBackground(Color bg)
  {
    super.setBackground(bg);
    combo.getEditor().getEditorComponent().setBackground(bg);
  }

  public Component getComponent()
  {
    return combo;
  }

  public String getString()
  {
    if (combo.isEditable())
    {
      Document comboDoc =
        ((JTextComponent) combo.getEditor().getEditorComponent()).getDocument();
      try
      {
        return comboDoc.getText(0, comboDoc.getLength());
      }
      catch (BadLocationException x)
      {
        Logger.error(x);
        return "";
      }
    }
    else
    {
      Object selected = combo.getSelectedItem();
      return selected == null ? "" : selected.toString();
    }
  }

  public boolean getBoolean()
  {
    return !getString().equals("");
  }

  public void setString(String str)
  {
    boolean edit = combo.isEditable();
    combo.setEditable(true);
    combo.setSelectedItem(str);
    combo.setEditable(edit);
  }

  public boolean isStatic()
  {
    return false;
  }
}