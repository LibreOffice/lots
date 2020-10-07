/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.tab.XTabPageContainer;

import de.muenchen.allg.afid.UNO;

/**
 * A vertical layout. The layouts are shown in one column.
 */
public class TabLayout implements Layout
{

  /**
   * Container for layouts.
   */
  private List<Layout> layouts = new ArrayList<>();

  /**
   * The tab page container.
   */
  private XTabPageContainer tabPageContainer;

  /**
   * Vertical layout without margin.
   * 
   * @param tabPageContainer
   *          The container of the tab pages. Is also an {@link XWindow}.
   */
  public TabLayout(XTabPageContainer tabPageContainer)
  {
    this.tabPageContainer = tabPageContainer;
  }

  @Override
  public boolean isVisible()
  {
    return UNO.XWindow2(tabPageContainer).isVisible();
  }

  @Override
  public void addLayout(Layout layout, int weight)
  {
    layouts.add(layout);
  }

  @Override
  public Pair<Integer, Integer> layout(Rectangle rect)
  {
    Rectangle tabRect = getActivetTabPage().getPosSize();
    for (int i = 0; i < layouts.size(); i++)
    {
      UNO.XTabPageModel(UNO.XControl(tabPageContainer.getTabPageByID((short) (i + 1))).getModel())
          .setEnabled(layouts.get(i).isVisible());
    }
    Pair<Integer, Integer> size = getLayoutForActiveTabPageId().layout(
        new Rectangle(tabRect.X, rect.Y, rect.Width, getLayoutForActiveTabPageId().getHeightForWidth(rect.Width)));
    getActivetTabPage().setPosSize(tabRect.X, tabRect.Y, size.getRight(), size.getLeft() + tabRect.Y, PosSize.POSSIZE);
    UNO.XWindow(tabPageContainer).setPosSize(rect.X, rect.Y, size.getRight(), size.getLeft() + tabRect.Y,
        PosSize.POSSIZE);
    return Pair.of(size.getLeft() + tabRect.Y, size.getRight());
  }

  @Override
  public int getHeightForWidth(int width)
  {
    int height = getLayoutForActiveTabPageId().getHeightForWidth(width);
    height += getActivetTabPage().getPosSize().Y;
    return height;
  }

  @Override
  public int getMinimalWidth(int maxWidth)
  {
    return getLayoutForActiveTabPageId().getMinimalWidth(maxWidth);
  }

  @Override
  public int size()
  {
    return 1;
  }

  private Layout getLayoutForActiveTabPageId()
  {
    return layouts.get(getActiveTabPageId());
  }

  private XWindow getActivetTabPage()
  {
    return UNO.XWindow(tabPageContainer.getTabPage((short) getActiveTabPageId()));
  }

  private int getActiveTabPageId()
  {
    return tabPageContainer.getActiveTabPageID() - 1;
  }
}
