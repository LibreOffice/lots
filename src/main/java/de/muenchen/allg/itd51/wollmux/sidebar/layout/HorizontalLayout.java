package de.muenchen.allg.itd51.wollmux.sidebar.layout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.Rectangle;

/**
 * A horizontal layout. The layouts are shown in one row.
 *
 * The width of each layout is computed based on their weight.
 */
public class HorizontalLayout implements Layout
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
   * Horizontal layout without margin.
   */
  public HorizontalLayout()
  {
    this(0, 0, 0, 0, 0);
  }

  /**
   * Horizontal layout with space between the elements.
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
  public HorizontalLayout(int marginTop, int marginBottom, int marginLeft, int marginRight, int marginBetween)
  {
    this.marginTop = marginTop;
    this.marginBottom = marginBottom;
    this.marginLeft = marginLeft;
    this.marginRight = marginRight;
    this.marginBetween = marginBetween;
  }

  @Override
  public void addLayout(Layout layout, int weight)
  {
    layouts.put(layout, weight);
  }

  @Override
  public boolean isVisible()
  {
    return layouts.keySet().stream().anyMatch(Layout::isVisible);
  }

  @Override
  public Pair<Integer, Integer> layout(Rectangle rect)
  {
    List<Map.Entry<Layout, Integer>> visible = getVisibleLayouts();
    if (visible.isEmpty())
    {
      return Pair.of(0, 0);
    }

    int controls = visible.size();
    int xOffset = marginLeft;
    int marginTotal = marginLeft + (controls - 1) * marginBetween + marginRight;
    int width = (rect.Width - marginTotal) / visible.stream().map(Map.Entry::getValue).reduce(0, Integer::sum);

    int height = 0;
    for (Map.Entry<Layout, Integer> entry : visible)
    {
      Pair<Integer, Integer> size = entry.getKey().layout(new Rectangle(rect.X + xOffset, rect.Y + marginTop,
          width * entry.getValue(), rect.Height - marginTop - marginBottom));
      height = Integer.max(height, size.getLeft());
      xOffset += size.getRight() + marginBetween;
    }

    height += marginTop + marginBottom;
    return Pair.of(height, rect.Width);
  }

  @Override
  public int getHeightForWidth(int width)
  {
    int height = layouts.keySet().stream().mapToInt(l -> l.getHeightForWidth(width)).max().orElse(0);
    if (height > 0)
    {
      height += marginTop + marginBottom;
    }
    return height;
  }

  @Override
  public int getMinimalWidth(int maxWidth)
  {
    List<Map.Entry<Layout, Integer>> visible = getVisibleLayouts();
    if (visible.isEmpty())
    {
      return 0;
    }

    int controls = visible.size();
    int marginTotal = marginLeft + (controls - 1) * marginBetween + marginRight;
    int width = (maxWidth - marginTotal) / visible.stream().map(Map.Entry::getValue).reduce(0, Integer::sum);

    int minWidth = marginTotal;
    for (Map.Entry<Layout, Integer> l : layouts.entrySet())
    {
      minWidth += l.getKey().getMinimalWidth(l.getValue() * width);
    }
    return minWidth;
  }

  private List<Map.Entry<Layout, Integer>> getVisibleLayouts()
  {
    return layouts.entrySet().stream().filter(e -> e.getKey().isVisible()).collect(Collectors.toList());
  }
}
