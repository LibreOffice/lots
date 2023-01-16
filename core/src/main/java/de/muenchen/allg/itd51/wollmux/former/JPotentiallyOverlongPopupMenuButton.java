/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.former;

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
