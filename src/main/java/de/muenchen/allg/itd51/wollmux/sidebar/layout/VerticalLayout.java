package de.muenchen.allg.itd51.wollmux.sidebar.layout;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.Rectangle;

/**
 * A vertical layout. The layouts are shown in one column.
 */
public class VerticalLayout implements Layout
{

  /**
   * Container for layouts.
   */
  private Map<Layout, Integer> layouts = new LinkedHashMap<>();

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
   */
  public VerticalLayout()
  {
    this(0, 0, 0, 0, 0);
  }

  /**
   * Vertical layout with space between the elements.
   *
   * @param marginTop
   *          Space before the first element.
   * @param marginBottom
   *          Space below the last element.
   * @param marginLeft
   *          Space before the first element.
   * @param marginRight
   *          Space before the first element.
   * @param marginBetween
   *          Space between the elements
   */
  public VerticalLayout(int marginTop, int marginBottom, int marginLeft, int marginRight, int marginBetween)
  {
    this.marginTop = marginTop;
    this.marginBottom = marginBottom;
    this.marginLeft = marginLeft;
    this.marginRight = marginRight;
    this.marginBetween = marginBetween;
  }

  @Override
  public boolean isVisible()
  {
    return layouts.keySet().stream().anyMatch(Layout::isVisible);
  }

  @Override
  public void addLayout(Layout layout, int weight)
  {
    layouts.put(layout, weight);
  }

  @Override
  public Pair<Integer, Integer> layout(Rectangle rect)
  {
    int yOffset = marginTop;
    for (Map.Entry<Layout, Integer> entry : layouts.entrySet())
    {
      Pair<Integer, Integer> size = entry.getKey().layout(new Rectangle(rect.X + marginLeft, rect.Y + yOffset,
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
    int height = layouts.keySet().stream().mapToInt(l -> {
      int h = l.getHeightForWidth(width);
      return h > 0 ? h + marginBetween : 0;
    }).sum();
    if (height > 0)
    {
      height += marginTop + marginBottom - marginBetween;
    }
    return height;
  }

  @Override
  public int getMinimalWidth(int maxWidth)
  {
    int width = layouts.keySet().stream().mapToInt(l -> l.getMinimalWidth(maxWidth - marginLeft - marginRight)).max()
        .orElse(0);
    if (width > 0)
    {
      width += marginLeft + marginRight;
    }
    return width;
  }
}
