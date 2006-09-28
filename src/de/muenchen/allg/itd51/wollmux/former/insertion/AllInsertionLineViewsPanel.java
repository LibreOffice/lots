/*
* Dateiname: AllInsertionLineViewsPanel.java
* Projekt  : WollMux
* Funktion : Enthält alle OneInsertionLineViews.
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
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.view.View;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;


/**
 * Enthält alle OneInsertionLineViews.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllInsertionLineViewsPanel implements View
{
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * Der {@link FormularMax4000} zu dem diese View gehört.
   */
  private FormularMax4000 formularMax4000;
  
  /**
   * Wird auf alle {@link OneInsertionLineView}s registriert.
   */
  private ViewChangeListener myViewChangeListener;
  
  /**
   * Das JPanel, das die ganze View enthält.
   */
  private JPanel myPanel;
  
  /**
   * Das JPanel, das die {@link de.muenchen.allg.itd51.wollmux.former.insertion.OneInsertionLineView}s
   * enthält.
   */
  private JPanel mainPanel;
  
  /**
   * Die JScrollPane, die {@link #mainPanel} enthält.
   */
  private JScrollPane scrollPane;
  
  /**
   * Die Liste der {@link OneInsertionLineView}s in dieser View.
   */
  private List views = new Vector();
  
  /**
   * Erzeugt ein AllInsertionLineViewsPanel, die den Inhalt von
   * insertionModelList anzeigt. ACHTUNG! insertionModelList sollte leer sein,
   * da nur neu hinzugekommene Elemente in der View angezeigt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * 
   */
  public AllInsertionLineViewsPanel(InsertionModelList insertionModelList, FormularMax4000 formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    insertionModelList.addListener(new MyItemListener());
    formularMax4000.addBroadcastListener(new MyBroadcastListener());
    myViewChangeListener = new MyViewChangeListener();
  
    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    mainPanel.add(Box.createGlue());
    
    scrollPane = new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    
    JPanel buttonPanel = new JPanel(new GridBagLayout());
    
    myPanel = new JPanel(new BorderLayout());
    myPanel.add(scrollPane, BorderLayout.CENTER);
    myPanel.add(buttonPanel, BorderLayout.SOUTH);
    
    GridBagConstraints gbcButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    JButton button = new JButton("Löschen");
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        
      }});
    buttonPanel.add(button, gbcButton);
    
    ++gbcButton.gridx;
    button = new JButton("Button 2");
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
    
      }});
    buttonPanel.add(button, gbcButton);
    
    ++gbcButton.gridx;
  
  }
  
  /**
   * Fügt dieser View eine {@link OneInsertionLineView} für model hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void addItem(InsertionModel model)
  {
    OneInsertionLineView view = new OneInsertionLineView(model, myViewChangeListener, formularMax4000);
    views.add(view);
    
    /*
     * view vor dem letzten Element von mainPanel einfügen, weil das letzte
     * Element immer ein Glue sein soll.
     */
    mainPanel.add(view.JComponent(), mainPanel.getComponentCount() - 1);
    
    mainPanel.validate();
    scrollPane.validate();
  }
  
  /**
   * Entfernt viWie findet man seine geschlossenen Tickets wieder.ew aus diesem Container (falls dort vorhanden).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void removeItem(OneInsertionLineView view)
  {
    
  }
  
  public JComponent JComponent()
  {
    return myPanel;
  }
  
  
  
  private class MyItemListener implements InsertionModelList.ItemListener
  {

    public void itemAdded(InsertionModel model, int index)
    {
      addItem(model);
    }
    
  }
  
  private class MyViewChangeListener implements ViewChangeListener
  {

    public void viewShouldBeRemoved(View view)
    {
      removeItem((OneInsertionLineView)view);
    }
    
  }

  
  private class MyBroadcastListener extends BroadcastListener
  {
    
  }
    
}
