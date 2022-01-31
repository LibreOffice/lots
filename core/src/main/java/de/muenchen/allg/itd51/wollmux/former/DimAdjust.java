/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.Dimension;

import javax.swing.JComponent;

/**
 * Statische Methoden zur Justierung der max.,pref.,min. Size einer JComponent.
 */
public class DimAdjust
{
  private DimAdjust()
  {
    // hide default public constructor
  }

  /**
   * Setzt die maximale Breite von compo auf unendlich und liefert compo zurück.
   */
  public static JComponent maxWidthUnlimited(JComponent compo)
  {
    Dimension dim = compo.getMaximumSize();
    dim.width = Integer.MAX_VALUE;
    compo.setMaximumSize(dim);
    return compo;
  }

  /**
   * Setzt die maximale Breite von compo auf unendlich, die maximale Höhe auf die
   * bevorzugte Höhe und liefert compo zurück.
   */
  public static JComponent maxHeightIsPrefMaxWidthUnlimited(JComponent compo)
  {
    Dimension dim = compo.getMaximumSize();
    dim.width = Integer.MAX_VALUE;
    dim.height = compo.getPreferredSize().height;
    compo.setMaximumSize(dim);
    return compo;
  }

  /**
   * Nagelt die preferred size von compo auf den aktuellen Wert fest.
   */
  public static JComponent fixedPreferredSize(JComponent compo)
  {
    Dimension dim = compo.getPreferredSize();
    compo.setPreferredSize(dim);
    return compo;
  }

  /**
   * Nagelt die preferred und die max size von compo auf den aktuellen preferred size
   * Wert fest.
   */
  public static JComponent fixedSize(JComponent compo)
  {
    Dimension dim = compo.getPreferredSize();
    compo.setPreferredSize(dim);
    compo.setMaximumSize(dim);
    return compo;
  }

  /**
   * Legt die maximale Größe von compo auf die preferred size zuzüglich eines
   * Zusatzplatzes addX und addY fest.
   */
  public static JComponent fixedMaxSize(JComponent compo, int addX, int addY)
  {
    Dimension dim = compo.getPreferredSize();
    dim.width += addX;
    dim.height += addY;
    compo.setMaximumSize(dim);
    return compo;
  }

  /**
   * Liefert das Maximum der beiden Werte w und der preferred-width von compo zurück.
   */
  public static int maxWidth(int w, JComponent compo)
  {
    int w2 = compo.getPreferredSize().width;
    if (w2 > w)
      return w2;
    else
      return w;
  }
}
