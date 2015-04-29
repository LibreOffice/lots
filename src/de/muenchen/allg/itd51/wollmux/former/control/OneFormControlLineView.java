/*
 * Dateiname: OneFormControlLineView.java
 * Projekt  : WollMux
 * Funktion : Eine einzeilige Sicht auf ein einzelnes Formularsteuerelement. 
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
 * 29.08.2006 | BNK | Erstellung
 * 19.07.2007 | BNK | [R5406]Teile der View können nach Benutzerwunsch ein- oder ausgeblendet werden
 * 23.03.2010 | ERT | [R5721]Unterstützung für Shift-Klick
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.control;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.ViewVisibilityDescriptor;
import de.muenchen.allg.itd51.wollmux.former.view.LineView;

/**
 * Eine einzeilige Sicht auf ein einzelnes Formularsteuerelement.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OneFormControlLineView extends LineView
{
  /**
   * Standardbreite des Textfelds, das das Label anzeigt.
   */
  private static final int LABEL_COLUMNS = 20;

  /**
   * Standardbreite des Textfelds, das die ID anzeigt.
   */
  private static final int ID_COLUMNS = 10;

  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private OneFormControlLineView.ViewChangeListener bigDaddy;

  /**
   * Wird vor dem Ändern eines Attributs des Models gesetzt, damit der rekursive
   * Aufruf des ChangeListeners nicht unnötigerweise das Feld updatet, das wir selbst
   * gerade gesetzt haben.
   */
  private boolean ignoreAttributeChanged = false;

  /**
   * Das Model zur View.
   */
  private FormControlModel model;

  /**
   * Das JTextField, das das LABEL anzeigt und ändern lässt.
   */
  private JTextField labelTextfield;

  /**
   * Das JTextField, das die ID anzeigt und ändern lässt.
   */
  private JTextField idTextfield;

  /**
   * Zusätzliche Elemente, die nur für die Textarea benötigt werden.
   */
  private JPanel textAreaAdditionalView;

  /**
   * Die Komponente, die das Bearbeiten des TYPE-Attributs erlaubt.
   */
  private JComboBox typeView;

  /**
   * Zusätzliche Elemente für FormControls mit TYPE "combobox".
   */
  private JPanel comboBoxAdditionalView;

  /**
   * Wird auf alle Teilkomponenten dieser View registriert.
   */
  private MyMouseListener myMouseListener = new MyMouseListener();

  /**
   * Normales Font für ein Textfield.
   */
  private Font normalFont;

  /**
   * Dickes Font für ein Textfield.
   */
  private Font boldFont;

  /**
   * Gibt an, welche Teile dieser View eingeblendet werden sollen. null bedeutet,
   * dass alle Teile angezeigt werden sollen.
   */
  private ViewVisibilityDescriptor viewVisibilityDescriptor = null;

  /**
   * Erzeugt eine View für model.
   * 
   * @param bigDaddy
   *          typischerweise ein Container, der die View enthält und daher über
   *          Änderungen auf dem Laufenden gehalten werden muss.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public OneFormControlLineView(FormControlModel model,
      OneFormControlLineView.ViewChangeListener bigDaddy,
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
    myPanel.add(makeIdView());
    myPanel.add(makeLabelView());
    myPanel.add(makeTypeView());
    myPanel.add(makeComboBoxAdditionalView());
    myPanel.add(makeTextAreaAdditionalView());
    normalFont = labelTextfield.getFont();
    boldFont = normalFont.deriveFont(Font.BOLD);
    unmarkedBackgroundColor = myPanel.getBackground();
    setViewVisibility();
    model.addListener(new MyModelChangeListener());
  }

  /**
   * Setzt den {@link ViewVisibilityDescriptor}, der bestimmt, welche Teile dieser
   * View angezeigt werden. ACHTUNG! Das Objekt wird als Referenz gemerkt (jedoch nie
   * durch diese Klasse geändert). Wird null übergeben, so wird für alles true
   * angenommen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setViewVisibilityDescriptor(ViewVisibilityDescriptor desc)
  {
    viewVisibilityDescriptor = desc;
    setViewVisibility();
  }

  private void setViewVisibility()
  {
    /*
     * ACHTUNG! viewVisibilityDescriptor kann null sein!! Dies wird als alles true
     * interpretiert.
     */

    idTextfield.setVisible(model.isTab() || viewVisibilityDescriptor == null
      || viewVisibilityDescriptor.formControlLineViewId);
    labelTextfield.setVisible(model.isTab() || viewVisibilityDescriptor == null
      || viewVisibilityDescriptor.formControlLineViewLabel);
    typeView.setVisible(!model.isTab()
      && (viewVisibilityDescriptor == null || viewVisibilityDescriptor.formControlLineViewType));
    comboBoxAdditionalView.setVisible(model.isCombo()
      && (viewVisibilityDescriptor == null || viewVisibilityDescriptor.formControlLineViewAdditional));
    textAreaAdditionalView.setVisible(model.isTextArea()
      && (viewVisibilityDescriptor == null || viewVisibilityDescriptor.formControlLineViewAdditional));
    /*
     * Wenn alle abgeschaltet sind, aktiviere zumindest das ID-Feld
     */
    if (viewVisibilityDescriptor != null
      && !viewVisibilityDescriptor.formControlLineViewAdditional
      && !viewVisibilityDescriptor.formControlLineViewId
      && !viewVisibilityDescriptor.formControlLineViewLabel
      && !viewVisibilityDescriptor.formControlLineViewType)
    {
      idTextfield.setVisible(true);
    }
    myPanel.validate();
  }

  /**
   * Liefert eine Komponente, die das LABEL des FormControlModels anzeigt und
   * Änderungen an das Model weitergibt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComponent makeLabelView()
  {
    labelTextfield = new JTextField(model.getLabel(), LABEL_COLUMNS);
    labelTextfield.setToolTipText("Label");
    Document tfdoc = labelTextfield.getDocument();
    tfdoc.addDocumentListener(new DocumentListener()
    {
      public void update()
      {
        ignoreAttributeChanged = true;
        model.setLabel(labelTextfield.getText());
        ignoreAttributeChanged = false;
        if (getModel().getType() == FormControlModel.TAB_TYPE)
          bigDaddy.tabTitleChanged(OneFormControlLineView.this);
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

    labelTextfield.setCaretPosition(0);
    labelTextfield.addMouseListener(myMouseListener);
    setTypeSpecificTraits(labelTextfield, model.getType());
    return labelTextfield;
  }

  /**
   * Liefert eine Komponente, die die ID des FormControlModels anzeigt und Änderungen
   * an das Model weitergibt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComponent makeIdView()
  {
    IDManager.ID id = model.getId();
    idTextfield = new JTextField((id == null) ? "" : id.toString(), ID_COLUMNS);
    idTextfield.setToolTipText("ID");
    final Color defaultBackground = idTextfield.getBackground();
    Document tfdoc = idTextfield.getDocument();
    tfdoc.addDocumentListener(new DocumentListener()
    {
      public void update()
      {
        ignoreAttributeChanged = true;
        try
        {
          model.setId(idTextfield.getText());
          idTextfield.setBackground(defaultBackground);
        }
        catch (Exception x)
        {
          idTextfield.setBackground(Color.RED);
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

    idTextfield.setCaretPosition(0);
    idTextfield.addMouseListener(myMouseListener);
    return idTextfield;
  }

  /**
   * Liefert eine Komponente, die den TYPE des FormControlModels anzeigt und
   * Änderungen an das Model weitergibt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComboBox makeTypeView()
  {
    typeView = new JComboBox();
    typeView.setToolTipText(L.m("Feldtyp"));
    typeView.setEditable(false);
    typeView.addItem(FormControlModel.COMBOBOX_TYPE);
    typeView.addItem(FormControlModel.TEXTFIELD_TYPE);
    typeView.addItem(FormControlModel.TEXTAREA_TYPE);
    typeView.addItem(FormControlModel.LABEL_TYPE);
    typeView.addItem(FormControlModel.SEPARATOR_TYPE);
    typeView.addItem(FormControlModel.GLUE_TYPE);
    typeView.addItem(FormControlModel.CHECKBOX_TYPE);
    typeView.addItem(FormControlModel.BUTTON_TYPE);

    typeView.setSelectedItem(model.getType());

    typeView.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent e)
      {
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
          if (!ignoreAttributeChanged)
            model.setType((String) typeView.getSelectedItem());
        }
      }
    });

    typeView.addMouseListener(myMouseListener);
    return typeView;
  }

  /**
   * Liefert ein JPanel zurück mit zusätzlichen Bedienelementen für das Bearbeiten
   * der Werteliste eines FormControls mit TYPE "combobox".
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JPanel makeComboBoxAdditionalView()
  {
    comboBoxAdditionalView = new JPanel();
    comboBoxAdditionalView.setLayout(new BoxLayout(comboBoxAdditionalView,
      BoxLayout.X_AXIS));

    final JComboBox combo = new JComboBox();
    combo.setToolTipText(L.m("Eingabeliste"));
    combo.setEditable(true);
    combo.setPrototypeDisplayValue("Sicherungsgeberin");
    List<String> items = model.getItems();
    Iterator<String> iter = items.iterator();
    while (iter.hasNext())
    {
      String item = iter.next();
      combo.addItem(item);
    }

    /*
     * Sicherstellen, dass der Anfang, nicht das Ende eines Items dargestellt wird,
     * wenn die ComboBox zu klein für den ganzen Text ist.
     */
    final JTextComponent tc =
      (JTextComponent) combo.getEditor().getEditorComponent();
    tc.setCaretPosition(0);
    combo.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent e)
      {
        tc.setCaretPosition(0);
      }
    });

    comboBoxAdditionalView.add(combo);

    tc.addMouseListener(myMouseListener);
    comboBoxAdditionalView.addMouseListener(myMouseListener);

    final JCheckBox editBox = new JCheckBox();
    editBox.setToolTipText(L.m("Erweiterbar"));
    editBox.setSelected(model.getEditable());
    comboBoxAdditionalView.add(editBox);
    editBox.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ignoreAttributeChanged = true;
        model.setEditable(editBox.isSelected());
        ignoreAttributeChanged = false;
      }
    });
    editBox.addMouseListener(myMouseListener);
    final JButton newButton = new JButton("N");
    newButton.setToolTipText(L.m("Neuer Wert"));
    Insets ins = newButton.getInsets();
    newButton.setMargin(new Insets(ins.top, 0, ins.bottom, 0));
    comboBoxAdditionalView.add(newButton);
    newButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        String sel = combo.getSelectedItem().toString();
        combo.addItem(sel);
        String[] items = new String[combo.getItemCount()];
        for (int i = 0; i < items.length; ++i)
          items[i] = combo.getItemAt(i).toString();
        ignoreAttributeChanged = true;
        model.setItems(items);
        ignoreAttributeChanged = false;
      }
    });
    newButton.addMouseListener(myMouseListener);
    JButton delButton = new JButton("X");
    delButton.setToolTipText(L.m("Wert löschen"));
    ins = delButton.getInsets();
    delButton.setMargin(new Insets(ins.top, 0, ins.bottom, 0));
    comboBoxAdditionalView.add(delButton);
    delButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        int idx = combo.getSelectedIndex();
        if (idx >= 0)
        {
          combo.removeItemAt(idx);
          String[] items = new String[combo.getItemCount()];
          for (int i = 0; i < items.length; ++i)
            items[i] = combo.getItemAt(i).toString();
          ignoreAttributeChanged = true;
          model.setItems(items);
          ignoreAttributeChanged = false;
        }
      }
    });
    delButton.addMouseListener(myMouseListener);

    return comboBoxAdditionalView;
  }

  /**
   * Liefert eine Komponente, die die ID des FormControlModels anzeigt und Änderungen
   * an das Model weitergibt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComponent makeTextAreaAdditionalView()
  {
    textAreaAdditionalView = new JPanel();
    textAreaAdditionalView.setLayout(new BoxLayout(textAreaAdditionalView,
      BoxLayout.X_AXIS));
    final JTextField linesTextfield = new JTextField("" + model.getLines(), 3);
    linesTextfield.setToolTipText(L.m("Anzahl der Zeilen"));
    Document tfdoc = linesTextfield.getDocument();
    tfdoc.addDocumentListener(new DocumentListener()
    {
      public void update()
      {
        int lines = -1;
        try
        {
          lines = Integer.parseInt(linesTextfield.getText());
          if (lines > 200 || lines < 1) lines = -1;
        }
        catch (NumberFormatException ex)
        {}
        if (lines > 0)
        {
          ignoreAttributeChanged = true;
          model.setLines(lines);
          ignoreAttributeChanged = false;
        }
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

    linesTextfield.setCaretPosition(0);
    linesTextfield.addMouseListener(myMouseListener);
    textAreaAdditionalView.add(linesTextfield);

    final JCheckBox wrapBox = new JCheckBox();
    wrapBox.setToolTipText(L.m("Automatischer Zeilenumbruch"));
    wrapBox.setSelected(model.getWrap());
    textAreaAdditionalView.add(wrapBox);
    wrapBox.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ignoreAttributeChanged = true;
        model.setWrap(wrapBox.isSelected());
        ignoreAttributeChanged = false;
      }
    });
    wrapBox.addMouseListener(myMouseListener);
    return textAreaAdditionalView;
  }

  /**
   * Setzt optische Aspekte wie Rand von compo abhängig von type.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setTypeSpecificTraits(JComponent compo, String type)
  {
    if (type.equals(FormControlModel.TAB_TYPE))
    {
      compo.setFont(boldFont);
      compo.setBackground(new Color(230, 235, 250));
      compo.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
    }
    else if (type.equals(FormControlModel.BUTTON_TYPE))
    {
      compo.setFont(normalFont);
      compo.setBackground(Color.LIGHT_GRAY);
      compo.setBorder(BorderFactory.createRaisedBevelBorder());
    }
    else if (type.equals(FormControlModel.LABEL_TYPE))
    {
      compo.setFont(boldFont);
      compo.setBackground(Color.WHITE);
      compo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }
    else
    {
      compo.setFont(normalFont);
      compo.setBackground(Color.WHITE);
      compo.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }
  }

  /**
   * Wird aufgerufen, wenn das LABEL des durch diese View dargestellten
   * {@link FormControlModel}s DURCH EINE ANDERE URSACHE ALS DIESE VIEW geändert
   * wurde.
   * 
   * @param newLabel
   *          das neue Label.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void labelChangedDueToExternalReasons(String newLabel)
  {
    labelTextfield.setText(newLabel);
    if (getModel().getType() == FormControlModel.TAB_TYPE)
      bigDaddy.tabTitleChanged(this);
  }

  /**
   * Wird aufgerufen, wenn das TYPE des durch diese View dargestellten
   * {@link FormControlModel}s geändert wurde (egal ob durch diese View selbst
   * verursacht oder durch etwas anderes).
   * 
   * @param newType
   *          der new TYPE-Wert.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void typeChanged(String newType)
  {
    boolean ign = ignoreAttributeChanged;
    ignoreAttributeChanged = true;
    typeView.setSelectedItem(newType);
    ignoreAttributeChanged = ign;
    setTypeSpecificTraits(labelTextfield, newType);
    setViewVisibility();
  }

  /**
   * Liefert das {@link FormControlModel} das zu dieser View gehört.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormControlModel getModel()
  {
    return model;
  }

  /**
   * Interface für Klassen, die an Änderungen dieser View interessiert sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ViewChangeListener extends
      de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener
  {
    /**
     * Wird aufgerufen, wenn sich das Label des Tabs, das das Model von view ist
     * geändert hat.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void tabTitleChanged(OneFormControlLineView view);
  }

  private class MyModelChangeListener implements
      FormControlModel.ModelChangeListener
  {
    public void attributeChanged(FormControlModel model, int attributeId,
        Object newValue)
    {
      if (ignoreAttributeChanged) return;
      switch (attributeId)
      {
        case FormControlModel.LABEL_ATTR:
          labelChangedDueToExternalReasons((String) newValue);
          break;
        case FormControlModel.TYPE_ATTR:
          typeChanged((String) newValue);
          break;
      }
    }

    public void modelRemoved(FormControlModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneFormControlLineView.this);
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
    /*
     * Beim Klicken in Feld "ID" oder "Label" wird der automatischen vorbefühllte Text gelöscht.
     */    
    public void mouseClicked(MouseEvent e)
    {
      if(e.getSource().equals(labelTextfield) && labelTextfield.getText().equals("Label"))
        labelTextfield.setText("");
      if(e.getSource().equals(idTextfield) && idTextfield.getText().matches("ID[0-9]*"))
        idTextfield.setText("");
    }

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

        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastFormControlModelSelection(this);
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
