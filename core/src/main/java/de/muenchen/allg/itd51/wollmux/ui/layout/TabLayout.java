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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.Size;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XUnitConversion;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.MeasureUnit;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.ui.GuiFactory;
import de.muenchen.allg.util.UnoProperty;

/**
 * A tab layout. The tab container always uses the whole place. A single tag page is displayed.
 */
public class TabLayout implements Layout
{

  private static final Logger LOGGER = LoggerFactory.getLogger(TabLayout.class);

  /**
   * Container for layouts.
   */
  private List<Layout> layouts = new ArrayList<>();

  /**
   * The tab page container.
   */
  private XTabPageContainer tabPageContainer;

  /**
   * List of invisible labels. Each tab page contains one label. The labels are used to compute the
   * offset created by scrolling. Unfortunately LibreOffice can't return the properties ScrollTop
   * and ScrollWidth properly. We need those values to reposition our controls because the
   * coordinate (0,0) is always the first visible pixel in the window.
   */
  private List<XWindow> anchors;

  /**
   * Vertical layout without margin.
   * 
   * @param tabPageContainer
   *          The container of the tab pages. Is also an {@link XWindow}. All tab pages have to be
   *          added to the container before this layout should be created. Otherwise it may not work
   *          properly.
   * @param xMCF
   *          The {@link XMultiComponentFactory} used to create controls in the tab pages.
   * @param context
   *          The {@link XComponentContext} for the created controls.
   */
  public TabLayout(XTabPageContainer tabPageContainer, XMultiComponentFactory xMCF, XComponentContext context)
  {
    this.tabPageContainer = tabPageContainer;
    this.anchors = new ArrayList<>(tabPageContainer.getTabPageCount());
    for (short i = 0; i < tabPageContainer.getTabPageCount(); i++)
    {
      XTabPage page = tabPageContainer.getTabPage(i);
      XControl anchor = GuiFactory.createLabel(xMCF, context, "", new Rectangle(), null);
      UNO.XControlContainer(page).addControl(RandomStringUtils.random(10), anchor);
      anchors.add(i, UNO.XWindow(anchor));
    }
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
    // Properties use logical coordinates so we have to convert them.
    // XWindow.setPosSize use pixels.
    XUnitConversion converter = UNO.XUnitConversion(getActivetTabPage());
    for (int i = 0; i < layouts.size(); i++)
    {
      UNO.XTabPageModel(UNO.XControl(tabPageContainer.getTabPageByID((short) (i + 1))).getModel())
          .setEnabled(layouts.get(i).isVisible());
    }
    try
    {
      UNO.XWindow(tabPageContainer).setPosSize(rect.X, rect.Y, rect.Width, rect.Height, PosSize.POSSIZE);
      Rectangle anchorRect = anchors.get(tabPageContainer.getActiveTabPageID() - 1).getPosSize();
      Pair<Integer, Integer> size = getLayoutForActiveTabPageId().layout(new Rectangle(anchorRect.X,
          anchorRect.Y, rect.Width, getLayoutForActiveTabPageId().getHeightForWidth(rect.Width)));
      Size scrollableSize = converter.convertSizeToLogic(new Size(0, size.getLeft()), MeasureUnit.APPFONT);
      UnoProperty.setProperty(UNO.XControl(getActivetTabPage()).getModel(), UnoProperty.SCROLL_HEIGHT,
          scrollableSize.Height);
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
    return Pair.of(rect.Height, rect.Width);
  }

  @Override
  public int getHeightForWidth(int width)
  {
    int height = getLayoutForActiveTabPageId().getHeightForWidth(width);
    height += UNO.XWindow(getActivetTabPage()).getPosSize().Y;
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

  private XTabPage getActivetTabPage()
  {
    return tabPageContainer.getTabPage((short) getActiveTabPageId());
  }

  private int getActiveTabPageId()
  {
    return tabPageContainer.getActiveTabPageID() - 1;
  }
}
