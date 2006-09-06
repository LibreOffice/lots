/*
* Dateiname: AllFormControlModelLineViewsPanel.java
* Projekt  : WollMux
* Funktion : Hält in einem Panel FormControlModelLineViews für alle FormControlModels einer FormControlModelList.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 30.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.muenchen.allg.itd51.wollmux.former.FormControlModelList.ItemListener;

/**
 * Hält in einem Panel FormControlModelLineViews für alle 
 * {@link de.muenchen.allg.itd51.wollmux.former.FormControlModel} einer 
 * {@link de.muenchen.allg.itd51.wollmux.former.FormControlModelList}.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AllFormControlModelLineViewsPanel implements View, ItemListener, OneFormControlLineView.ViewChangeListener
{
  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet)
   * in Pixeln.
   */
  private final static int TF_BORDER = 4;
  
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;
  
  /**
   * Wird (mit wechselndem gridy Wert) verwendet als Constraints für das Hinzufügen von
   * Views zum Panel. 
   */
  private GridBagConstraints gbcLineView = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
  
  /**
   * Die {@link FormControlModelList}, deren Inhalt in dieser View angezeigt wird.
   */
  private FormControlModelList formControlModelList;
  
  /**
   * Das Panel, das alle Komponenten dieser View enthält.
   */
  private JPanel myPanel;
  
  /**
   * Das Panel, das die ganzen {@link OneFormControlLineView}s enthält.
   */
  private JPanel lineViewPanel;
  
  /**
   * Enthält die {@link OneFormControlLineView}s in der richtigen Reihenfolge.
   */
  private Vector views = new Vector();

  /**
   * Ein Vector von Integers, die die Indizes der selektierten Views angeben.
   */
  private Vector selection = new Vector();
  
  /**
   * Die Scrollpane in der sich das {@link #lineViewPanel} befindet.
   */
  private JScrollPane scrollPane;
  
  /**
   * Der FormularMax4000, zu dem diese View gehört.
   */
  private FormularMax4000 formularMax4000;
  
  /**
   * Erzeugt eine AllFormControlModelLineViewsPanel, die den Inhalt von
   * formControlModelList anzeigt. ACHTUNG! formControlModelList sollte leer sein,
   * da nur neu hinzugekommene Elemente in der View angezeigt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED*/
  public AllFormControlModelLineViewsPanel(FormControlModelList formControlModelList, FormularMax4000 formularMax4000)
  {
    this.formControlModelList = formControlModelList;
    this.formularMax4000 = formularMax4000;
    formControlModelList.addListener(this);
    formularMax4000.addBroadcastListener(new MyBroadcastListener());

    myPanel = new JPanel(new GridBagLayout());
    lineViewPanel = new JPanel(new GridBagLayout());
    
    scrollPane = new JScrollPane(lineViewPanel);
    scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
    
     //    int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcMainPanel = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
    gbcMainPanel.gridx = 0;
    gbcMainPanel.gridy = 0;
    
    myPanel.add(scrollPane, gbcMainPanel);
    
    JPanel buttonPanel = new JPanel(new GridBagLayout());
    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcButtonPanel = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    gbcButtonPanel.gridx = 0;
    gbcButtonPanel.gridy = 1;
    myPanel.add(buttonPanel,gbcButtonPanel);
    
    GridBagConstraints gbcButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    JButton hochButton = new JButton("Hoch");
    hochButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        moveSelectedElementsUp();
      }});
    buttonPanel.add(hochButton, gbcButton);
    
    ++gbcButton.gridx;
    JButton runterButton = new JButton("Runter");
    runterButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        moveSelectedElementsDown();
      }});
    buttonPanel.add(runterButton, gbcButton);
    
    ++gbcButton.gridx;
    JButton killButton = new JButton("Löschen");
    killButton.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        deleteSelectedElements();
      }});
    buttonPanel.add(killButton, gbcButton);
  }

  public JComponent JComponent()
  {
    return myPanel;
  }
  //TESTED
  public void itemAdded(FormControlModel model, int index)
  {
    OneFormControlLineView ofclView = new OneFormControlLineView(model, this, formularMax4000);
    for (int i = views.size() - 1; i >= index; --i)
    {
      gbcLineView.gridy = i + 1;
      lineViewPanel.add(((OneFormControlLineView)views.get(i)).JComponent(), gbcLineView);
    }
    
    views.add(index, ofclView);
    
    gbcLineView.gridy = index;
    lineViewPanel.add(ofclView.JComponent(), gbcLineView);
    lineViewPanel.validate();
    scrollPane.validate();
    
    fixupSelectionIndices(index, 1);
  }

  public void viewShouldBeRemoved(OneFormControlLineView view)
  {
    int index = views.indexOf(view);
    if (index < 0) return;
    
    views.remove(index);
    lineViewPanel.remove(view.JComponent());
    for (int i = views.size() - 1; i >= index; --i)
    {
      gbcLineView.gridy = i;
      lineViewPanel.add(((OneFormControlLineView)views.get(i)).JComponent(), gbcLineView);
    }
    lineViewPanel.validate();
    scrollPane.validate();
    
    removeSelectionIndex(index);
    fixupSelectionIndices(index, -1);
  }
  
  /**
   * Schiebt alle ausgewählten Elemente um einen Platz nach oben, d,h, in Richtung niedrigerer
   * Indizes. Falls Element 0 ausgewählt ist wird gar nichts getan.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void moveSelectedElementsUp()
  {
    if (((Integer)selection.firstElement()).intValue() > 0)
    {
      formControlModelList.moveElementsUp(selection);
      //Kein fixupSelectionIndices(0, -1) nötig, weil die itemSwapped() Events schon dafür sorgen
    }
  }
  
  /**
   * Schiebt alle ausgewählten Elemente um einen Platz nach unten, d,h, in Richtung niedrigerer
   * Indizes. Falls das letzte Element ausgewählt ist wird gar nichts getan.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void moveSelectedElementsDown()
  {
    if (((Integer)selection.lastElement()).intValue() < views.size() - 1)
    {
      formControlModelList.moveElementsDown(selection);
     //Kein fixupSelectionIndices(0, 1) nötig, weil die itemSwapped() Events schon dafür sorgen
    }
  }
  
  /**
   * Löscht alle ausgewählten Elemente.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void deleteSelectedElements()
  {
    Iterator iter = selection.iterator();
    while (iter.hasNext())
    {
      Integer I = (Integer)iter.next();
      OneFormControlLineView view = (OneFormControlLineView)views.get(I.intValue());
      formControlModelList.remove(view.getModel());
    }
  }

  public void itemSwapped(int index1, int index2)
  {
    OneFormControlLineView view1 = (OneFormControlLineView)views.get(index1);
    OneFormControlLineView view2 = (OneFormControlLineView)views.get(index2);
    views.setElementAt(view1, index2);
    views.setElementAt(view2, index1);
    gbcLineView.gridy = index2;
    lineViewPanel.add(view1.JComponent(), gbcLineView);
    gbcLineView.gridy = index1;
    lineViewPanel.add(view2.JComponent(), gbcLineView);
    lineViewPanel.validate();
    scrollPane.validate();
    
    swapSelectionIndices(index1, index2);
  }
  
  /**
   * Addiert auf alle Indizes in der {@link #selection} Liste größer gleich start den offset.
   * Indizes die dabei illegal werden werden aus der Liste gelöscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED*/
  private void fixupSelectionIndices(int start, int offset)
  {
    ListIterator iter = selection.listIterator();
    while (iter.hasNext())
    {
      Integer I = (Integer)iter.next();
      int i = I.intValue();
      if (i >= start)
      {
        i = i + offset;
        if (i < 0 || i > views.size()-1)
          iter.remove();
        else 
          iter.set(new Integer(i));
      }
    }
  }
  
  /**
   * Ersetzt in {@link #selection} index1 durch index2 und index2 durch index1.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void swapSelectionIndices(int index1, int index2)
  {
    ListIterator iter = selection.listIterator();
    while (iter.hasNext())
    {
      Integer I = (Integer)iter.next();
      int i = I.intValue();
      if (i == index1)
        iter.set(new Integer(index2));
      else if (i == index2)
        iter.set(new Integer(index1));
    }
    
    Collections.sort(selection);
  }
  
  /**
   * Hebt die Selektion aller Elemente auf.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED */
  private void clearSelection()
  {
    Iterator iter = selection.iterator();
    while (iter.hasNext())
    {
      Integer I = (Integer)iter.next();
      OneFormControlLineView view = (OneFormControlLineView)views.get(I.intValue());
      view.unmark();
    }
    selection.clear();
  }
  
  /**
   * Entfernt den Index i aus der {@link #selection} Liste falls er dort enthalten ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeSelectionIndex(int i)
  {
    int idx = selection.indexOf(new Integer(i));
    if (idx >= 0) selection.remove(idx);
  }

  private class MyBroadcastListener extends BroadcastListener
  {
    public void broadcastFormControlModelSelection(BroadcastFormControlModelSelection b) 
    { //TESTED
      if (b.getClearSelection()) clearSelection();
      FormControlModel model = b.getModel();
      Iterator iter = views.iterator();
      int index = 0;
      while (iter.hasNext())
      {
        OneFormControlLineView view = (OneFormControlLineView)iter.next();
        if (view.getModel() == model)
        {
          Integer I = new Integer(index);
          int state = b.getState();
          if (state == 0) //toggle
            state = selection.contains(I) ? -1 : 1;
            
          switch (state)
          {
            case -1: //abwählen
                     ((OneFormControlLineView)views.get(index)).unmark();
                     selection.remove(new Integer(index));
                     break;
            case 1: //auswählen
                     if (!selection.contains(I)) 
                     {
                       ((OneFormControlLineView)views.get(index)).mark();
                       selection.add(I);
                     }
                     break;
          }
        }
        
        ++index;
      }
      
      Collections.sort(selection);
    }
  }
    
}
