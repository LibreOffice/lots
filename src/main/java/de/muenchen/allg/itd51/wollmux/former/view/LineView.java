package de.muenchen.allg.itd51.wollmux.former.view;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;

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
  protected FormularMax4kController formularMax4000;

  @Override
  public JComponent getComponent()
  {
    return myPanel;
  }

  /**
   * Markiert diese View optisch als ausgewählt.
   */
  public void mark()
  {
    myPanel.setBackground(MARKED_BACKGROUND_COLOR);
  }

  /**
   * Entfernt die optische Markierung als ausgewählt von dieser View.
   */
  public void unmark()
  {
    myPanel.setBackground(unmarkedBackgroundColor);
  }

}
