package de.muenchen.allg.itd51.wollmux.sidebar.layout;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.star.awt.Rectangle;

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
  public int layout(Rectangle rect)
  {
    int yOffset = marginTop;

    for (Map.Entry<Layout, Integer> entry : layouts.entrySet())
    {
      yOffset += entry.getKey()
          .layout(new Rectangle(rect.X, rect.Y + yOffset, rect.Width, rect.Height)) + marginBetween;
    }
    yOffset -= marginBetween;

    return yOffset;
  }

  @Override
  public void addLayout(Layout layout, int weight)
  {
    layouts.put(layout, weight);
  }

  @Override
  public int getHeight()
  {
    int h = layouts.keySet().stream().mapToInt(Layout::getHeight).sum();
    h += marginTop;
    h += layouts.size() * marginBetween;
    return h;
  }

}
