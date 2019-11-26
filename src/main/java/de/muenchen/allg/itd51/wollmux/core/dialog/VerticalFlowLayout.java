/*
 * Dateiname: VerticalFlowLayout.java
 * Projekt  : WollMux
 * Funktion : Similar to FlowLayout but using vertical arrangement.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see 
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 19.06.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Similar to {@link FlowLayout} but using vertical arrangement. The height of the
 * layout will be the maximum height of the container.
 * 
 * @author Matthias Benkmann (D-III-ITD D.10)
 */
public class VerticalFlowLayout implements LayoutManager
{

  @Override
  public void addLayoutComponent(String name, Component comp)
  {
    // nothing to do
  }

  @Override
  public void removeLayoutComponent(Component comp)
  {
    // nothing to do
  }

  @Override
  public void layoutContainer(Container parent)
  {
    synchronized (parent.getTreeLock())
    {
      Insets insets = parent.getInsets();
      int maxheight = parent.getHeight() - insets.bottom - insets.top;

      int count = parent.getComponentCount();

      int x = insets.left;
      int y = insets.top;
      int height = 0;
      int columnWidth = 0;
      boolean firstInColumn = true;

      for (int i = 0; i < count; ++i)
      {
        Component compo = parent.getComponent(i);
        Dimension pref = compo.getPreferredSize();

        // Do not use setSize(Dimension), because it might store the object reference
        // directly
        compo.setSize(pref.width, pref.height);

        /*
         * Start new column if the component doesn't fit into the current column
         * anymore, but don't do this if it's the 1st component in the column,
         * because in that case starting a new column won't help either.
         */
        if (!firstInColumn && (height + pref.height > maxheight))
        {
          x += columnWidth;
          y = insets.top;
          columnWidth = 0;
          height = 0;
          firstInColumn = true;
        } else
        {
          firstInColumn = false;
        }

        compo.setLocation(x, y);
        if (pref.width > columnWidth)
        {
          columnWidth = pref.width;
        }
        height += pref.height;
        y += pref.height;
      }

    }
  }

  @Override
  public Dimension minimumLayoutSize(Container parent)
  {
    synchronized (parent.getTreeLock())
    {
      /*
       * Don't ask me why, but in the case of a JPopupMenu (see
       * JPotentiallyOverlongPopupMenuButton) the individual components don't always
       * start reporting their correct preferred sizes until after the last element's
       * preferred size has been accessed.
       * 
       * With JPotentially... this can be observed when the button is moved to the
       * bottom of the screen so that the menu will no longer fit under the button
       * because it's just a little too wide. In that case, rather than switch to
       * making the menu vertically large, the menu extends beyond the screen border
       * to the right. The following statement fixes this.
       * 
       * So just leave it in. It doesn't hurt.
       */
      if (parent.getComponentCount() > 0)
        parent.getComponent(parent.getComponentCount() - 1).getPreferredSize();

      Insets insets = parent.getInsets();
      int maxheight = parent.getMaximumSize().height - insets.bottom - insets.top;
      int containerHeight = 0;
      int containerWidth = 0;

      int count = parent.getComponentCount();

      int columnHeight = 0;
      int columnWidth = 0;
      boolean firstInColumn = true;

      for (int i = 0; i < count; ++i)
      {
        Component compo = parent.getComponent(i);
        Dimension pref = compo.getPreferredSize();
        /*
         * Start new column if the component doesn't fit into the current column
         * anymore, but don't do this if it's the 1st component in the column,
         * because in that case starting a new column won't help either.
         */
        if (!firstInColumn && (columnHeight + pref.height > maxheight))
        {
          containerWidth += columnWidth;
          if (containerHeight < columnHeight)
          {
            containerHeight = columnHeight;
          }
          columnWidth = 0;
          columnHeight = 0;
          firstInColumn = true;
        } else
        {
          firstInColumn = false;
        }

        if (pref.width > columnWidth)
        {
          columnWidth = pref.width;
        }
        columnHeight += pref.height;
      }

      containerWidth += columnWidth;
      if (containerHeight < columnHeight)
      {
        containerHeight = columnHeight;
      }
      return new Dimension(containerWidth + insets.left + insets.right,
        containerHeight + insets.top + insets.bottom);
    }
  }

  @Override
  public Dimension preferredLayoutSize(Container parent)
  {
    return minimumLayoutSize(parent);
  }

}
