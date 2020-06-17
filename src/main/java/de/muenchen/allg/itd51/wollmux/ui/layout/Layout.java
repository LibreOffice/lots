package de.muenchen.allg.itd51.wollmux.ui.layout;

import org.apache.commons.lang3.tuple.Pair;

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
   * Reorder the controls
   *
   * @param rect
   *          Conditions for position and height.
   * @return A pair of height and width.
   */
  Pair<Integer, Integer> layout(Rectangle rect);

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
   * @param control
   *          {@link XControl} XControl which will be wrapped in an {@link ControlLayout}.
   */
  default void addControl(XControl control)
  {
    addControl(control, 1);
  }

  /**
   * {@link #addLayout(Layout, int)} mit {@link ControlLayout}.
   *
   * @see #addLayout(Layout, int)
   * @param control
   *          {@link XControl} XControl which will be wrapped in an {@link ControlLayout}.
   * @param weight
   *          The weight (Size) in relation to other layouts.
   */
  default void addControl(XControl control, int weight)
  {
    addLayout(new ControlLayout(control), weight);
  }

  /**
   * Die Höhe des Layouts.
   *
   * @param width
   *          The requested width.
   * @return Die Höhe.
   */
  int getHeightForWidth(int width);

  /**
   * The minimal width of the layout.
   *
   * @param maxWidth
   *          The maximal width.
   * @return The minimal width.
   */
  int getMinimalWidth(int maxWidth);

  boolean isVisible();

}
