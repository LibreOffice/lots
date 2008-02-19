//TODO L.m()
/*
* Dateiname: DimAdjust.java
* Projekt  : WollMux
* Funktion : Statische Methoden zur Justierung der max.,pref.,min. Size einer JComponent.
* 
* Copyright: Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;

import javax.swing.JComponent;

/**
 * Statische Methoden zur Justierung der max.,pref.,min. Size einer JComponent.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DimAdjust
{
  
  /**
   * Setzt die maximale Breite von compo auf unendlich und liefert compo zurück.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static JComponent maxWidthUnlimited(JComponent compo)
  {
    Dimension dim = compo.getMaximumSize();
    dim.width = Integer.MAX_VALUE;
    compo.setMaximumSize(dim);
    return compo;
  }
  
  /**
   * Setzt die maximale Breite von compo auf unendlich, die maximale Höhe auf
   * die bevorzugte Höhe und liefert compo zurück.
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static JComponent fixedSize(JComponent compo)
  {
    Dimension dim = compo.getPreferredSize();
    compo.setPreferredSize(dim);
    return compo;
  }

  /**
   * Legt die maximale Größe von compo auf die preferred size zuzüglich eines
   * Zusatzplatzes addX und addY fest.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
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
   * Liefert das Maximum der beiden Werte w und der preferred-width von compo
   * zurück.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
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
