/*
 * Dateiname: OneSectionLineView.java
 * Projekt  : WollMux
 * Funktion : Eine einzeilige Sicht auf einen Bereich im Dokument.
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 24.03.2009 | BNK | Erstellung
 * 23.03.2010 | ERT | [R5721]Unterstützung für Shift-Klick
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.section;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.view.LineView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

/**
 * Eine einzeilige Sicht auf einen Bereich im Dokument.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class OneSectionLineView extends LineView
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(OneSectionLineView.class);

  /**
   * Das Model zu dieser View.
   */
  private SectionModel model;

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
  private JTextField nameBox;

  /**
   * Erzeugt eine neue View für model.
   */
  public OneSectionLineView(SectionModel model, ViewChangeListener bigDaddy,
      FormularMax4kController formularMax4000)
  {
    this.model = model;
    this.bigDaddy = bigDaddy;
    this.formularMax4000 = formularMax4000;
    myPanel = new JPanel();
    myPanel.setOpaque(true);
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
    myPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    myPanel.addMouseListener(myMouseListener);
    myPanel.add(makeNameView());
    myPanel.add(makeVisibilityView());
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
   * Liefert eine Komponente, die den Namen des Bereichs anzeigt und Änderungen an
   * das Model weitergibt.
   */
  private JComponent makeNameView()
  {
    nameBox = new JTextField(20);

    nameBox.setText(model.getNamePrefix());
    final JTextComponent tc = nameBox;
    final Document comboDoc = tc.getDocument();
    final Color defaultBackground = tc.getBackground();

    comboDoc.addDocumentListener(new DocumentListener()
    {
      public void update()
      {
        ignoreAttributeChanged = true;
        try
        {
          model.setNamePrefix(comboDoc.getText(0, comboDoc.getLength()));
          tc.setBackground(defaultBackground);
        }
        catch (BadLocationException x)
        {
          LOGGER.error("", x);
        }
        ignoreAttributeChanged = false;
      }

      @Override
      public void insertUpdate(DocumentEvent e)
      {
        update();
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        update();
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        update();
      }
    });

    nameBox.addMouseListener(myMouseListener);

    return nameBox;
  }

  private JComponent makeVisibilityView()
  {
    final JCheckBox visiBox = new JCheckBox();
    visiBox.setSelected(model.isVisible());
    visiBox.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        model.setVisible(visiBox.isSelected());
      }
    });
    return visiBox;
  }

  /**
   * Liefert das {@link SectionModel} das zu dieser View gehört.
   */
  public SectionModel getModel()
  {
    return model;
  }

  private void nameChangedDueToExternalReasons(String newName)
  {
    nameBox.setText(newName);
  }

  private class MyModelChangeListener implements SectionModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(SectionModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneSectionLineView.this);
    }

    @Override
    public void attributeChanged(SectionModel model, int attributeId, Object newValue)
    {
      if (ignoreAttributeChanged) return;
      switch (attributeId)
      {
        case SectionModel.NAME_ATTR:
          nameChangedDueToExternalReasons(newValue.toString());
          break;
      }
    }
  }

  /**
   * Wird auf alle Teilkomponenten der View registriert. Setzt MousePressed-Events um
   * in Broadcasts, die signalisieren, dass das entsprechende Model selektiert wurde.
   * Je nachdem ob CTRL gedrückt ist oder nicht wird die Selektion erweitert oder
   * ersetzt.
   */
  private class MyMouseListener implements MouseListener
  {
    @Override
    public void mouseClicked(MouseEvent e)
    {}

    @Override
    public void mousePressed(MouseEvent e)
    {
      int state = BroadcastObjectSelection.STATE_NORMAL_CLICK;
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
        state = BroadcastObjectSelection.STATE_CTRL_CLICK;
      else if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK)
        state = BroadcastObjectSelection.STATE_SHIFT_CLICK;

      formularMax4000.broadcast(new BroadcastObjectSelection(getModel(), state,
        state == BroadcastObjectSelection.STATE_NORMAL_CLICK)
      {
        @Override
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastSectionModelSelection(this);
        }
      });
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {}

    @Override
    public void mouseEntered(MouseEvent e)
    {}

    @Override
    public void mouseExited(MouseEvent e)
    {}
  }
}
