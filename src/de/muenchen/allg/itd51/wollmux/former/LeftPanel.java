/*
* Dateiname: LeftPanel.java
* Projekt  : WollMux
* Funktion : Der Übercontainer für die linke Hälfte des FormularMax 4000.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 22.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import de.muenchen.allg.itd51.wollmux.former.control.AllFormControlModelLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModelList;
import de.muenchen.allg.itd51.wollmux.former.insertion.AllInsertionModelLineViewsPanel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModelList;
import de.muenchen.allg.itd51.wollmux.former.view.View;

/**
 * Der Übercontainer für die linke Hälfte des FormularMax 4000.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class LeftPanel implements View
{
  /**
   * Hält in einem Panel FormControlModelLineViews für alle 
   * {@link FormControlModel}s. 
   */
  private AllFormControlModelLineViewsPanel allFormControlModelLineViewsPanel;
  
  /**
   * Hält in einem Panel InsertionModelLineViews für alle 
   * {@link InsertionModel}s. 
   */
  private AllInsertionModelLineViewsPanel allInsertionModelLineViewsPanel;
  
  /**
   * Enthält alle im linken Panel angezeigten Views.
   */
  private JTabbedPane myTabbedPane;
  
  public LeftPanel(InsertionModelList insertionModelList,
      FormControlModelList formControlModelList, FormularMax4000 formularMax4000)
  {
    allFormControlModelLineViewsPanel = new AllFormControlModelLineViewsPanel(formControlModelList, formularMax4000);
    allInsertionModelLineViewsPanel = new AllInsertionModelLineViewsPanel(insertionModelList, formularMax4000);
    
    myTabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    myTabbedPane.add("Formular-GUI", allFormControlModelLineViewsPanel.JComponent());
    myTabbedPane.add("Einfügungen", allInsertionModelLineViewsPanel.JComponent());
  }
  
  /**
   * Liefert den Index an dem Buttons auf dem aktuell sichtbaren Tab des
   * {@link AllFormControlModelLineViewsPanel} eingefügt
   * werden sollten oder -1, falls dort kein Tab ausgewählt ist. Der zurückgelieferte
   * Wert (falls nicht -1) entspricht dem Index des letzten sichtbaren
   * Elements + 1.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int getButtonInsertionIndex()
  {
    return allFormControlModelLineViewsPanel.getButtonInsertionIndex(); 
  }
  
  public JComponent JComponent()
  {
    return myTabbedPane;
  }

}
