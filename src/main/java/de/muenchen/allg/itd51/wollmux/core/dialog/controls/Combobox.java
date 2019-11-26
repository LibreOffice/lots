package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Combobox extends UIElementBase
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Combobox.class);

  private JComboBox<?> combo;

  public Combobox(String id, JComboBox<?> combo, Object layoutConstraints, UIElement.LabelPosition labelType, String label,
      Object labelLayoutConstraints)
  {
    this.combo = combo;
    this.layoutConstraints = layoutConstraints;
    this.labelLayoutConstraints = labelLayoutConstraints;
    this.label = new JLabel(label);
    this.labelType = labelType;
    this.id = id;
  }

  @Override
  public void setBackground(Color bg)
  {
    super.setBackground(bg);
    combo.getEditor().getEditorComponent().setBackground(bg);
  }

  @Override
  public Component getComponent()
  {
    return combo;
  }

  @Override
  public String getString()
  {
    if (combo.isEditable())
    {
      Document comboDoc = ((JTextComponent) combo.getEditor().getEditorComponent()).getDocument();
      try
      {
        return comboDoc.getText(0, comboDoc.getLength());
      } catch (BadLocationException x)
      {
        LOGGER.error("", x);
        return "";
      }
    } else
    {
      Object selected = combo.getSelectedItem();
      return selected == null ? "" : selected.toString();
    }
  }

  @Override
  public boolean getBoolean()
  {
    return !getString().isEmpty();
  }

  @Override
  public void setString(String str)
  {
    boolean edit = combo.isEditable();
    combo.setEditable(true);
    combo.setSelectedItem(str);
    combo.setEditable(edit);
  }

  @Override
  public boolean isStatic()
  {
    return false;
  }
}