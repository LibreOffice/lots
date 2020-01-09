package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JLabel;

import de.muenchen.allg.itd51.wollmux.core.form.model.Control;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;

/**
 * Abstrakte Basis-Klasse für UIElemente.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class UIElementBase implements UIElement
{

  protected UIElement.LabelPosition labelType = UIElement.LabelPosition.NONE;

  protected JLabel label = null;

  protected Object layoutConstraints = null;

  protected Object labelLayoutConstraints = null;

  protected Function constraints = null;

  protected String id = "";

  protected Control field = null;

  protected int tab = -1;

  @Override
  public int getTab()
  {
    return tab;
  }

  @Override
  public void setTab(int tab)
  {
    this.tab = tab;
  }

  @Override
  public void setBackground(Color bg)
  {
    this.getComponent().setBackground(bg);
  }

  @Override
  public boolean setEnabled(boolean enabled)
  {
    boolean oldState = getComponent().isEnabled();
    this.getComponent().setEnabled(enabled);
    return oldState != enabled;
  }

  @Override
  public UIElement.LabelPosition getLabelType()
  {
    return labelType;
  }

  @Override
  public Component getLabel()
  {
    return label;
  }

  @Override
  public Object getLayoutConstraints()
  {
    return layoutConstraints;
  }

  @Override
  public Object getLabelLayoutConstraints()
  {
    return labelLayoutConstraints;
  }

  @Override
  public Control getFormField()
  {
    return field;
  }

  @Override
  public void setFormField(Control field)
  {
    this.field = field;
  }

  @Override
  public boolean setVisible(boolean vis)
  {
    boolean oldState = getComponent().isVisible();
    if (getLabel() != null)
    {
      getLabel().setVisible(vis);
    }
    getComponent().setVisible(vis);
    /*
     * einige Komponenten (z.B. JTextField) tun dies nicht richtig siehe
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4403550
     */
    ((JComponent) getComponent().getParent()).revalidate();
    return oldState != vis;
  }

  @Override
  public String getId()
  {
    return id;
  }

  @Override
  public void setString(String str)
  {
  }

  @Override
  public boolean hasFocus()
  {
    return getComponent().isFocusOwner();
  }

  @Override
  public void takeFocus()
  {
    getComponent().requestFocusInWindow();
  }
}