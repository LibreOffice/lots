package de.muenchen.allg.itd51.wollmux.sidebar.layout;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.star.awt.Rectangle;

/**
 * Ein horizontales Layout. Alle enthaltenen Layouts werden in einer Reihe angezeigt.
 *
 * Die Breite wird dynamisch an Hand der Gewichtung berechnet.
 *
 * @author daniel.sikeler
 */
public class HorizontalLayout implements Layout
{
  /**
   * Container für die enthaltenen Layouts.
   */
  private Map<Layout, Integer> layouts = new LinkedHashMap<>();

  @Override
  public int layout(Rectangle rect)
  {
    int xOffset = 0;
    int width = rect.Width / layouts.values().stream().reduce(0, Integer::sum);
    int height = 0;

    for (Map.Entry<Layout, Integer> entry : layouts.entrySet())
    {
      height = Integer.max(height, entry.getKey()
          .layout(new Rectangle(rect.X + xOffset, rect.Y, width * entry.getValue(), rect.Height)));
      xOffset += width * entry.getValue();
    }

    return height;
  }

  @Override
  public void addLayout(Layout layout, int width)
  {
    layouts.put(layout, width);
  }

  @Override
  public int getHeight()
  {
    return layouts.keySet().stream().mapToInt(Layout::getHeight).max().orElse(0);
  }

}
