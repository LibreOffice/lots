/*
 * Dateiname: JPotentiallyOverlongPopupMenuButton.java
 * Projekt  : WollMux
 * Funktion : Stellt einen Button dar mit einem Popup-Menü, das darauf vorbereitet ist, sehr viele Elemente anzubieten.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Stellt einen Button dar mit einem Popup-Menü, das darauf vorbereitet ist,
 * sehr viele Elemente anzubieten.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class JPotentiallyOverlongPopupMenuButton extends JButton
{
  /**
   * Macht Eclipse glücklich.
   */
  private static final long serialVersionUID = 3206786778925266706L;

  /**
   * Erzeugt einen Button mit Beschriftung label, bei dessen Betätigung eine
   * Popup-Menü erscheint, in dem alle Elemente aus actions enthalten sind. Wenn
   * das Popup-Menü zu lang wäre, um auf den Bildschirm zu passen, passiert
   * etwas intelligentes. Die Elemente von actions können
   * {@link javax.swing.Action} oder {@link java.awt.Component} Objekte sein.
   * ACHTUNG! Bei jeder Betätigung des Buttons wird das Menü neu aufgebaut, d.h.
   * wenn sich die actions ändert, ändert sich das Menü.
   */
  public JPotentiallyOverlongPopupMenuButton(String label, final Iterable<?> actions)
  {
    super(label);

    this.addActionListener(event -> {
      JPopupMenu menu = new JPopupMenu();
      menu.setLayout(new VerticalFlowLayout());
      Iterator<?> iter = actions.iterator();
      while (iter.hasNext())
      {
        Object action = iter.next();
        if (action instanceof Action)
          menu.add((Action) action);
        else
          menu.add((Component) action);
      }

      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Component compo = JPotentiallyOverlongPopupMenuButton.this;
      Point p = new Point(0, JPotentiallyOverlongPopupMenuButton.this.getSize().height);
      SwingUtilities.convertPointToScreen(p, compo);
      int maxheight = screenSize.height - p.y;
      if (maxheight < 1)
      {
        maxheight = 1;
      }
      menu.setMaximumSize(new Dimension(screenSize.width, maxheight));
      menu.invalidate();
      Dimension d = menu.getMinimumSize();
      if (d.width > screenSize.width)
      {
        menu.setMaximumSize(new Dimension(screenSize.width, screenSize.height));
        menu.invalidate();
      }

      menu.show(JPotentiallyOverlongPopupMenuButton.this, 0, JPotentiallyOverlongPopupMenuButton.this.getSize().height);
    });
  }
}
