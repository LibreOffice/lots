package de.muenchen.allg.itd51.wollmux.sidebar.layout;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XWindow;

/**
 * Ein verticales Layout. Alle enthaltenen Layouts werden untereinander angeordnet.
 *
 * Ein Zwischenraum kann spezifiziert werden.
 *
 * @author daniel.sikeler
 */
public class VerticalLayout implements Layout
{

  /**
   * Container für die enthaltenen Layouts.
   */
  private Map<Layout, Integer> layouts = new LinkedHashMap<>();

  /**
   * Der Abstand oberhalb des ersten Layouts.
   */
  private int marginTop;

  /**
   * Der Abstand zwischen den Layouts und nach dem letzten Layout.
   */
  private int marginBetween;

  /**
   * Vertikales Layout mit keinen Abständen/Zwischenräumen.
   */
  public VerticalLayout()
  {
    this(0, 0);
  }

  /**
   * Ein neues verticales Layout.
   *
   * @param marginTop
   *          Der Abstand oberhalb des ersten Layouts.
   * @param marginBetween
   *          Der Abstand zwischen den Layouts und nach dem letzten Layout.
   */
  public VerticalLayout(int marginTop, int marginBetween)
  {
    this.marginTop = marginTop;
    this.marginBetween = marginBetween;
  }

  @Override
  public Pair<Integer, Integer> layout(Rectangle rect)
  {
    int yOffset = marginTop;

    for (Map.Entry<Layout, Integer> entry : layouts.entrySet())
    {
      Pair<Integer, Integer> size = entry.getKey()
          .layout(new Rectangle(rect.X, rect.Y + yOffset, rect.Width, rect.Height));
      yOffset += size.getLeft() + marginBetween;
    }

    yOffset -= marginBetween;

    return Pair.of(yOffset, rect.Width);
  }

  @Override
  public void addLayout(Layout layout, int weight)
  {
    layouts.put(layout, weight);
  }

  @Override
  public int getHeightForWidth(int width)
  {
    int h = layouts.keySet().stream().mapToInt(l -> l.getHeightForWidth(width)).sum();
    h += marginTop;
    h += (layouts.size() - 1) * marginBetween;

    return h;
  }

  @Override
  public int getMinimalWidth()
  {
    return layouts.keySet().stream().mapToInt(Layout::getMinimalWidth).max().orElse(0);
  }

  @Override
  public XWindow getControl()
  {
    throw new UnsupportedOperationException();
  }

}
