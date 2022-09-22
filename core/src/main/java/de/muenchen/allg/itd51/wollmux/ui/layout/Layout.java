/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.ui.layout;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;

/**
 * A LayoutManager for the sidebar.
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
   * Adds another sub-layout to the layout.
   *
   * @param layout
   *          The new sub-layout.
   * @param weight
   *          The weighting (size) compared to the other layouts.
   */
  void addLayout(Layout layout, int weight);

  /**
   * {@link #addLayout(Layout, int)} with {@link ControlLayout} and weight 1.
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
   * The height of the layout.
   *
   * @param width
   *          The requested width.
   * @return The height
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

  /**
   * Number of elements managed by the layout.
   *
   * @return Number of elements.
   */
  int size();

}
