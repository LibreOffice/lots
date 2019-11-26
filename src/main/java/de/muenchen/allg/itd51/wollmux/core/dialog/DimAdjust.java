/*
 * Dateiname: DimAdjust.java
 * Projekt  : WollMux
 * Funktion : Statische Methoden zur Justierung der max.,pref.,min. Size einer JComponent.
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
 * 19.10.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.dialog;

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
