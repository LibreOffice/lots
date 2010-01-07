/*
 * Dateiname: OneGroupLineView.java
 * Projekt  : WollMux
 * Funktion : Eine einzeilige Sicht auf eine Sichtbarkeitsgruppe.
 * 
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0.
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
 * 13.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.view.LineView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

public class OneGroupLineView extends LineView
{

  /**
   * Das Model zu dieser View.
   */
  private GroupModel model;

  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;

  /**
   * Wird auf alle Teilkomponenten dieser View registriert.
   */
  private MyMouseListener myMouseListener = new MyMouseListener();

  /**
   * Wird vor dem Ändern eines Attributs des Models gesetzt, damit der rekursive
   * Aufruf des ChangeListeners nicht unnötigerweise das Feld updatet, das wir selbst
   * gerade gesetzt haben.
   */
  private boolean ignoreAttributeChanged = false;

  /**
   * Das TextField, das die ID der Gruppe anzeigt und ändern lässt.
   */
  private JTextField idBox;

  /**
   * Erzeugt eine neue View für model.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public OneGroupLineView(GroupModel model, ViewChangeListener bigDaddy,
      FormularMax4000 formularMax4000)
  {
    this.model = model;
    this.bigDaddy = bigDaddy;
    this.formularMax4000 = formularMax4000;
    myPanel = new JPanel();
    myPanel.setOpaque(true);
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
    myPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    myPanel.addMouseListener(myMouseListener);
    myPanel.add(makeIDView());
    unmarkedBackgroundColor = myPanel.getBackground();
    model.addListener(new MyModelChangeListener());
    myPanel.validate();
    // Notwendig, um BoxLayout beim Aufziehen daran zu hindern, die Textfelder
    // hässlich
    // hoch zu machen.
    myPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
      myPanel.getPreferredSize().height));
  }

  /**
   * Liefert eine Komponente, die die ID anzeigt und Änderungen an das Model
   * weitergibt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComponent makeIDView()
  {
    idBox = new JTextField(20);

    idBox.setText(model.getID().toString());
    final JTextComponent tc = idBox;
    final Document comboDoc = tc.getDocument();
    final Color defaultBackground = tc.getBackground();

    comboDoc.addDocumentListener(new DocumentListener()
    {
      public void update()
      {
        ignoreAttributeChanged = true;
        try
        {
          model.setID(comboDoc.getText(0, comboDoc.getLength()));
          tc.setBackground(defaultBackground);
        }
        catch (BadLocationException x)
        {
          Logger.error(x);
        }
        catch (Exception x)
        {
          tc.setBackground(Color.RED);
        }
        ignoreAttributeChanged = false;
      }

      public void insertUpdate(DocumentEvent e)
      {
        update();
      }

      public void removeUpdate(DocumentEvent e)
      {
        update();
      }

      public void changedUpdate(DocumentEvent e)
      {
        update();
      }
    });

    idBox.addMouseListener(myMouseListener);

    return idBox;
  }

  /**
   * Liefert das {@link GroupModel} das zu dieser View gehört.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public GroupModel getModel()
  {
    return model;
  }

  private void idChangedDueToExternalReasons(IDManager.ID newId)
  {
    idBox.setText(newId.toString());
  }

  private class MyModelChangeListener implements GroupModel.ModelChangeListener
  {
    public void modelRemoved(GroupModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneGroupLineView.this);
    }

    public void attributeChanged(GroupModel model, int attributeId, Object newValue)
    {
      if (ignoreAttributeChanged) return;
      switch (attributeId)
      {
        case GroupModel.ID_ATTR:
          idChangedDueToExternalReasons((IDManager.ID) newValue);
          break;
      }
    }
  }

  /**
   * Wird auf alle Teilkomponenten der View registriert. Setzt MousePressed-Events um
   * in Broadcasts, die signalisieren, dass das entsprechende Model selektiert wurde.
   * Je nachdem ob CTRL gedrückt ist oder nicht wird die Selektion erweitert oder
   * ersetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyMouseListener implements MouseListener
  {
    public void mouseClicked(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {
      int state = 1;
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
        state = 0;
      formularMax4000.broadcast(new BroadcastObjectSelection(getModel(), state,
        state != 0)
      {
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastGroupModelSelection(this);
        }
      });
    }

    public void mouseReleased(MouseEvent e)
    {}

    public void mouseEntered(MouseEvent e)
    {}

    public void mouseExited(MouseEvent e)
    {}
  }
}
