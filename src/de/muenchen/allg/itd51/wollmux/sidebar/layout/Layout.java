package de.muenchen.allg.itd51.wollmux.sidebar.layout;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;

/**
 * Ein LayoutManager für die Sidebar.
 *
 * @author daniel.sikeler
 */
public interface Layout
{
  /**
   * Ordnet die Controls neu an.
   *
   * @param rect
   *          Legt die Rahmenbedingungen für die Position und Größe der Controls fest.
   * @return Die Höhe der Controls.
   */
  int layout(Rectangle rect);

  /**
   * Fügt den Layout ein weiteres Sub-Layout hinzu.
   *
   * @param layout
   *          Das neue Sub-Layout.
   * @param weight
   *          Die Gewichtung (Größe) im Vergleich zu den anderen Layouts.
   */
  void addLayout(Layout layout, int weight);

  /**
   * {@link #addLayout(Layout, int)} mit {@link ControlLayout} und Gewicht 1.
   *
   * @see #addLayout(Layout, int)
   */
  default void addControl(XControl control)
  {
    addControl(control, 1);
  }

  /**
   * {@link #addLayout(Layout, int) mit {@link ControlLayout}.
   *
   * @see #addLayout(Layout, int)
   */
  default void addControl(XControl control, int weight)
  {
    addLayout(new ControlLayout(control), weight);
  }

  /**
   * Die Höhe des Layouts.
   * 
   * @return Die Höhe.
   */
  int getHeight();
}
