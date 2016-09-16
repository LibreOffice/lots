package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public abstract class Senderbox
{
  protected JComponent menu;

  public void removeAllItems()
  {
    menu.removeAll();
  }

  public void addItem(String item, ActionListener listen, String actionCommand,
      MouseListener mouseListen)
  {
    JMenuItem menuItem = new JMenuItem(item);
    menuItem.addActionListener(listen);
    menuItem.setActionCommand(actionCommand);
    menuItem.addMouseListener(mouseListen);
    menu.add(menuItem);
  }

  public void addSeparator()
  {
    menu.add(new JSeparator());
  }

  public abstract void setSelectedItem(String item);

  public static Senderbox create(JMenu menu)
  {
    return new JMenuSenderbox(menu);
  }

  public static Senderbox create(JPopupMenu menu, AbstractButton button)
  {
    return new JPopupMenuSenderbox(menu, button);
  }

  private static class JMenuSenderbox extends Senderbox
  {

    public JMenuSenderbox(JMenu menu)
    {
      this.menu = menu;
    }

    @Override
    public void setSelectedItem(String item)
    {
      ((JMenu) menu).setText(item);
    }
  }

  private static class JPopupMenuSenderbox extends Senderbox
  {
    private AbstractButton button;

    public JPopupMenuSenderbox(JPopupMenu menu, AbstractButton button)
    {
      this.menu = menu;
      this.button = button;
      this.button.putClientProperty("menu", menu);

      button.addAncestorListener(new AncestorListener()
      {

        @Override
        public void ancestorRemoved(AncestorEvent event)
        {
          if (event.getComponent().isVisible())
            ((JPopupMenu) event.getComponent().getClientProperty("menu")).setVisible(false);
        }

        @Override
        public void ancestorMoved(AncestorEvent event)
        {}

        @Override
        public void ancestorAdded(AncestorEvent event)
        {}
      });
    }

    @Override
    public void setSelectedItem(String item)
    {
      button.setText(item);
    }
  }
}