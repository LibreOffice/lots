/*
* Dateiname: LineView.java
* Projekt  : WollMux
* Funktion : Abstrakte Oberklasse für einzeilige Sichten.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 13.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;

public abstract class LineView implements View
{
  /**
   * Farbe für den Hintergrund, wenn die View markiert ist.
   */
  protected static final Color MARKED_BACKGROUND_COLOR = Color.BLUE;
  
  /**
   * Breite des Randes um die View.
   */
  protected static final int BORDER = 4;

  /**
   * Das Panel, das alle Komponenten dieser View enthält.
   */
  protected JPanel myPanel;
  
  /**
   * Die Hintergrundfarbe im unmarkierten Zustand.
   */
  protected Color unmarkedBackgroundColor;
  
  /**
   * Der FormularMax4000, zu dem diese View gehört.
   */
  protected FormularMax4000 formularMax4000;
  
  public JComponent JComponent()
  {
    return myPanel;
  }
  
  /**
   * Markiert diese View optisch als ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void mark()
  {
    myPanel.setBackground(MARKED_BACKGROUND_COLOR);
  }
  
  /**
   * Entfernt die optische Markierung als ausgewählt von dieser View.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void unmark()
  {
    myPanel.setBackground(unmarkedBackgroundColor);
  }
  


}
