package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;

/**
 * Abstrakte Basis-Klasse für UIElemente.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class UIElementBase implements UIElement
{
  protected Integer labelType = LABEL_NONE;

  protected JLabel label = null;

  protected Object layoutConstraints = null;

  protected Object labelLayoutConstraints = null;

  protected Function constraints = null;

  protected String id = "";

  protected Object addData = null;

  public void setBackground(Color bg)
  {
    this.getComponent().setBackground(bg);
  }
  
  public void setEnabled(boolean enabled)
  {
    this.getComponent().setEnabled(enabled);
  }

  public Integer getLabelType()
  {
    return labelType;
  }

  public Component getLabel()
  {
    return label;
  }

  public abstract Component getComponent();

  public Object getLayoutConstraints()
  {
    return layoutConstraints;
  }

  public Object getLabelLayoutConstraints()
  {
    return labelLayoutConstraints;
  }

  public Object getAdditionalData()
  {
    return addData;
  }

  public void setAdditionalData(Object o)
  {
    addData = o;
  }

  public void setVisible(boolean vis)
  {
    if (getLabel() != null) getLabel().setVisible(vis);
    getComponent().setVisible(vis);
    /*
     * einige Komponenten (z.B. JTextField) tun dies nicht richtig siehe
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4403550
     */
    ((JComponent) getComponent().getParent()).revalidate();
  }

  public abstract String getString();

  public abstract boolean getBoolean();

  public String getId()
  {
    return id;
  }

  public void setString(String str)
  {};

  public boolean hasFocus()
  {
    return getComponent().isFocusOwner();
  }

  public void takeFocus()
  {
    getComponent().requestFocusInWindow();
  }
}