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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.Rectangle;

/**
 * A vertical layout. The layouts are shown in one column. The first layout is treated specially. It
 * takes to rest of the space not needed by the other layouts.
 */
public class FixedVerticalLayout implements Layout
{

  /**
   * Container for layouts.
   */
  private List<Layout> layouts = new ArrayList<>();

  /**
   * The first control, which uses the height not needed by the following layouts.
   */
  private Layout firstLayout;

  /**
   * Margin above first layout.
   */
  private int marginTop;

  /**
   * Margin below last layout.
   */
  private int marginBottom;

  /**
   * Margin left.
   */
  private int marginLeft;

  /**
   * Margin right.
   */
  private int marginRight;

  /**
   * margin between layouts.
   */
  private int marginBetween;

  /**
   * Vertical layout without margin.
   *
   * @param firstLayout
   *          The first control which gets the rest of the space not used by the other layouts.
   */
  public FixedVerticalLayout(Layout firstLayout)
  {
    this(firstLayout, 0, 0, 0, 0, 0);
  }

  /**
   * Vertical layout with space between the elements.
   *
   * @param firstLayout
   *          The first control which gets the rest of the space not used by the other layouts.
   * @param marginTop
   *          Space before the first element.
   * @param marginBottom
   *          Space below the last element.
   * @param marginLeft
   *          Space to the left.
   * @param marginRight
   *          Space to the right.
   * @param marginBetween
   *          Space between the elements
   */
  public FixedVerticalLayout(Layout firstLayout, int marginTop, int marginBottom, int marginLeft, int marginRight,
      int marginBetween)
  {
    this.firstLayout = firstLayout;
    this.marginTop = marginTop;
    this.marginBottom = marginBottom;
    this.marginLeft = marginLeft;
    this.marginRight = marginRight;
    this.marginBetween = marginBetween;
  }

  @Override
  public boolean isVisible()
  {
    return layouts.stream().anyMatch(Layout::isVisible) || firstLayout.isVisible();
  }

  @Override
  public void addLayout(Layout layout, int weight)
  {
    layouts.add(layout);
  }

  @Override
  public Pair<Integer, Integer> layout(Rectangle rect)
  {
    int yOffset = marginTop;
    int layoutHeight = getHeightOfLayouts(rect.Width - marginLeft - marginRight);
    int fixedHeight = Integer.max(0, rect.Height - layoutHeight);
    firstLayout.layout(
        new Rectangle(rect.X + marginLeft, rect.Y + yOffset, rect.Width - marginLeft - marginRight, fixedHeight));
    yOffset += fixedHeight + marginBetween;
    for (Layout layout : layouts)
    {
      Pair<Integer, Integer> size = layout.layout(new Rectangle(rect.X + marginLeft, rect.Y + yOffset,
          rect.Width - marginLeft - marginRight, rect.Height - marginTop - marginBottom));
      if (size.getLeft() > 0)
      {
        yOffset += size.getLeft() + marginBetween;
      }
    }

    if (yOffset <= marginTop)
    {
      return Pair.of(0, 0);
    }
    yOffset -= marginBetween;
    yOffset += marginBottom;

    return Pair.of(yOffset, rect.Width);
  }

  @Override
  public int getHeightForWidth(int width)
  {
    int height = firstLayout.getHeightForWidth(width) + getHeightOfLayouts(width);
    if (height > 0)
    {
      height += marginTop + marginBottom;
    }
    return height;
  }

  private int getHeightOfLayouts(int width)
  {
    int layoutHeight = layouts.stream().mapToInt(l -> {
      int h = l.getHeightForWidth(width);
      return h > 0 ? h + marginBetween : 0;
    }).sum();
    if (layoutHeight > 0)
    {
      layoutHeight -= marginBetween;
    }
    return layoutHeight;
  }

  @Override
  public int getMinimalWidth(int maxWidth)
  {
    int width = layouts.stream().mapToInt(l -> l.getMinimalWidth(maxWidth - marginLeft - marginRight)).max().orElse(0);
    width = Integer.max(firstLayout.getMinimalWidth(maxWidth - marginLeft - marginRight), width);
    if (width > 0)
    {
      width += marginLeft + marginRight;
    }
    return width;
  }

  @Override
  public int size()
  {
    return layouts.size() + 1;
  }
}
