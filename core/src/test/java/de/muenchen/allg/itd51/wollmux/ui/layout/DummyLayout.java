/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.Rectangle;

public class DummyLayout implements Layout
{
  private boolean visible;
  private int x;
  private int y;
  private int width;
  private int height;

  public DummyLayout(boolean visible, int x, int y, int width, int height)
  {
    this.visible = visible;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  @Override
  public Pair<Integer, Integer> layout(Rectangle rect)
  {
    assertEquals(x, rect.X, "wrong X for child layout");
    assertEquals(y, rect.Y, "wrong Y for child layout");
    assertEquals(width, rect.Width, "wrong width for child layout");
    assertEquals(height, rect.Height, "wrong height for child layout");
    if (visible)
    {
      return Pair.of(10, 10);
    } else
    {
      return Pair.of(0, 0);
    }
  }

  @Override
  public void addLayout(Layout layout, int weight)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getHeightForWidth(int width)
  {
    if (visible)
    {
      return 10;
    } else
    {
      return 0;
    }
  }

  @Override
  public int getMinimalWidth(int maxWidth)
  {
    if (visible)
    {
      return 10;
    } else
    {
      return 0;
    }
  }

  @Override
  public boolean isVisible()
  {
    return visible;
  }

  @Override
  public int size()
  {
    return 0;
  }

}
