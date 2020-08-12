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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.Rectangle;

public class VerticalLayoutTest
{

  @Test
  public void empty()
  {
    Layout layout = new VerticalLayout(10, 10, 10, 10, 10);
    assertFalse(layout.isVisible(), "should be invisible");
    assertEquals(0, layout.getMinimalWidth(100), "width");
    assertEquals(0, layout.getHeightForWidth(100), "height");
    Pair<Integer, Integer> size = layout.layout(new Rectangle(0, 0, 10, 10));
    assertEquals(0, size.getLeft(), "height");
    assertEquals(0, size.getRight(), "width");
  }

  @Test
  public void oneElement()
  {
    Layout layout = new VerticalLayout(10, 10, 10, 10, 10);
    layout.addLayout(new DummyLayout(true, 10, 10, 80, 80), 1);
    assertTrue(layout.isVisible(), "should be visible");
    assertEquals(30, layout.getMinimalWidth(100), "width");
    assertEquals(30, layout.getHeightForWidth(100), "height");
    Pair<Integer, Integer> size = layout.layout(new Rectangle(0, 0, 100, 100));
    assertEquals(30, size.getLeft(), "height");
    assertEquals(100, size.getRight(), "width");

    layout = new VerticalLayout();
    layout.addLayout(new DummyLayout(true, 0, 0, 100, 100), 1);
    assertTrue(layout.isVisible(), "should be visible");
    assertEquals(10, layout.getMinimalWidth(100), "width");
    assertEquals(10, layout.getHeightForWidth(100), "height");
    size = layout.layout(new Rectangle(0, 0, 100, 100));
    assertEquals(10, size.getLeft(), "height");
    assertEquals(100, size.getRight(), "width");
  }

  @Test
  public void twoElements()
  {
    Layout layout = new VerticalLayout(10, 10, 10, 10, 10);
    layout.addLayout(new DummyLayout(true, 10, 10, 80, 80), 1);
    layout.addLayout(new DummyLayout(true, 10, 30, 80, 80), 3);
    assertTrue(layout.isVisible(), "should be visible");
    assertEquals(30, layout.getMinimalWidth(100), "width");
    assertEquals(50, layout.getHeightForWidth(100), "height");
    Pair<Integer, Integer> size = layout.layout(new Rectangle(0, 0, 100, 100));
    assertEquals(50, size.getLeft(), "height");
    assertEquals(100, size.getRight(), "width");
  }

  @Test
  public void invisibleElement()
  {
    Layout layout = new VerticalLayout(10, 10, 10, 10, 10);
    layout.addLayout(new DummyLayout(false, 10, 10, 80, 80), 1);
    assertFalse(layout.isVisible(), "should be invisible");
    assertEquals(0, layout.getMinimalWidth(100), "width");
    assertEquals(0, layout.getHeightForWidth(100), "height");
    Pair<Integer, Integer> size = layout.layout(new Rectangle(0, 0, 100, 100));
    assertEquals(0, size.getLeft(), "height");
    assertEquals(0, size.getRight(), "width");
  }
}
