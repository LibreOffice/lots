/*
 * Dateiname: JPotentiallyOverlongPopupMenuButton.java
 * Projekt  : WollMux
 * Funktion : Stellt einen Button dar mit einem Popup-Menü, das darauf vorbereitet ist, sehr viele Elemente anzubieten.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 13.02.2008 | BNK | Erstellung
 * 19.06.2008 | BNK | Implemented handling of overlong menus.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Stellt einen Button dar mit einem Popup-Menü, das darauf vorbereitet ist, sehr
 * viele Elemente anzubieten.
 * 
 * @author Matthias Benkmann (D-III-ITD D.10)
 */
public class JPotentiallyOverlongPopupMenuButton extends JButton
{
  /**
   * Macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 3206786778925266706L;

  /**
   * Erzeugt einen Button mit Beschriftung label, bei dessen Betätigung eine
   * Popup-Menü erscheint, in dem alle Elemente aus actions enthalten sind. Wenn das
   * Popup-Menü zu lang wäre, um auf den Bildschirm zu passen, passiert etwas
   * intelligentes. Die Elemente von actions können {@link javax.swing.Action} oder
   * {@link java.awt.Component} Objekte sein. ACHTUNG! Bei jeder Betätigung des
   * Buttons wird das Menü neu aufgebaut, d.h. wenn sich die actions ändert, ändert
   * sich das Menü.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  public JPotentiallyOverlongPopupMenuButton(String label, final Iterable<?> actions)
  {
    super(label);

    this.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        JPopupMenu menu = new JPopupMenu();
        menu.setLayout(new MyVerticalFlowLayout());
        Iterator<?> iter = actions.iterator();
        while (iter.hasNext())
        {
          Object action = iter.next();
          if (action instanceof Action)
            menu.add((Action) action);
          else
            menu.add((Component) action);

          // menu.invalidate();
          // System.out.println(menu.getPreferredSize());
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Component compo = JPotentiallyOverlongPopupMenuButton.this;
        Point p =
          new Point(0, JPotentiallyOverlongPopupMenuButton.this.getSize().height);
        SwingUtilities.convertPointToScreen(p, compo);
        int maxheight = screenSize.height - p.y;
        if (maxheight < 1) maxheight = 1;
        menu.setMaximumSize(new Dimension(screenSize.width, maxheight));
        menu.invalidate();
        Dimension d = menu.getMinimumSize();
        if (d.width > screenSize.width)
        {
          menu.setMaximumSize(new Dimension(screenSize.width, screenSize.height));
          menu.invalidate();
        }

        menu.show(JPotentiallyOverlongPopupMenuButton.this, 0,
          JPotentiallyOverlongPopupMenuButton.this.getSize().height);
      }
    });
  }

  private static class MyVerticalFlowLayout extends VerticalFlowLayout
  {}

  public static void main(String[] args)
  {
    JFrame myFrame = new JFrame("JPotentiallyOverlongPopupMenuButtonTest");
    myFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    final Box myPanel = new Box(BoxLayout.X_AXIS);
    myFrame.setContentPane(myPanel);

    Vector<Action> actions = new Vector<Action>();
    for (int i = 1; i <= 100; ++i)
    {
      final Integer I = Integer.valueOf(i);
      actions.add(new AbstractAction("" + i)
      {

        public void actionPerformed(ActionEvent e)
        {
          JOptionPane.showMessageDialog(myPanel, "" + I);
        }
      });
    }
    JPotentiallyOverlongPopupMenuButton butt =
      new JPotentiallyOverlongPopupMenuButton(
        "JPotentiallyOverlongPopupMenuButtonTest", actions);

    myPanel.add(butt);
    myFrame.pack();
    myFrame.setVisible(true);
  }
}
