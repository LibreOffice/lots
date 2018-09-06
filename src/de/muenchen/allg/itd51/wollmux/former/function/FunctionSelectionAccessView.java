/*
 * Dateiname: FunctionSelectionAccessView.java
 * Projekt  : WollMux
 * Funktion : Eine Sicht, die das Bearbeiten von {@link FunctionSelectionAccess} Objekten erlaubt.
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
 * 27.09.2006 | BNK | Erstellung
 * 02.03.2007 | BNK | Wenn Feldreferenzen vorhanden sind, dann Funktion in Expertenansicht darstellen.
 * 16.03.2007 | BNK | [R5860]Unterstützung für Feldreferenzen
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.function;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.StringReader;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.view.View;

/**
 * Eine Sicht, die das Bearbeiten von {@link FunctionSelectionAccess} Objekten
 * erlaubt.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionSelectionAccessView implements View
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FunctionSelectionAccessView.class);

  /**
   * Eintrag für die Funktionsauswahl-ComboBox, wenn keine Funktion gewünscht ist.
   */
  private static final String NONE_ITEM = L.m("<keine>");

  /**
   * Eintrag für die Funktionsauswahl-ComboBox, wenn manuelle Eingabe gewünscht ist.
   */
  private static final String EXPERT_ITEM = L.m("<Code>");

  /**
   * Eintrag für die Funktionsauswahl-ComboBox, wenn manuelle Eingabe eines Strings
   * gewünscht ist.
   */
  private static final String STRING_ITEM = L.m("<Wert>");

  /**
   * Eintrag für die ComboBox zum Festlegen eines Parameters, der anzeigt, dass der
   * Parameter nicht vorbelegt sein soll.
   */
  private static final String UNSPECIFIED_ITEM = L.m("[nicht fest verdrahtet]");

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Das Panel, das alle Elemente dieser View enthält.
   */
  private JPanel myPanel;

  /**
   * Das {@link FunctionSelectionAccess} Objekt, das diese View anzeigt und
   * bearbeitet.
   */
  private FunctionSelectionAccess funcSel;

  /**
   * Die Funktionsbibliothek, deren Funktionen auswählbar sind.
   */
  private FunctionLibrary funcLib;

  /**
   * Die JComboBox, in der der Benutzer die Funktion auswählen kann.
   */
  private JComboBox<String> functionSelectorBox;

  /**
   * Der {@link IDManager}, dessen aktive IDs für Feldreferenzen auswählbar sind.
   */
  private IDManager idManager;

  /**
   * Der Namensraum aus dem die IDs von {@link #idManager} genommen werden.
   */
  private Object namespace;

  /**
   * Wurde die manuelle Eingabe eines Stringliterals als Funktion gewählt, so erfolgt
   * die Eingabe in dieses Eingabefeld.
   */
  private JTextArea literalValueArea;

  /**
   * Wurde die manuelle Experten-Eingabe einer Funktion gewählt, so wird diese über
   * diese Textarea abgewickelt.
   */
  private JTextArea complexFunctionArea;

  /**
   * Damit nicht bei jedem gedrückten Buchstaben ein neues ConfigThingy erzeugt wird,
   * wird dieser Timer verwendet, um das updaten verzögert und gesammelt anzustoßen.
   */
  private Timer updateExpertFunctionTimer;

  /**
   * Wird von {@link #updateExpertFunction()} ausgewertet um festzustellen, ob die
   * Funktion aus {@link #literalValueArea} (Fall "false") oder
   * {@link #complexFunctionArea} (fall "true") geparst werden muss.
   */
  private boolean expertFunctionIsComplex;

  /**
   * Erzeugt eine neue View über die funcSel angezeigt und bearbeitet werden kann.
   * 
   * @param funcLib
   *          die Funktionsbibliothek, deren Funktionen auswählbar sein sollen.
   * @param idManager
   *          Als Feldreferenzen sind alle aktiven IDs dieses {@link IDManager}s
   *          auswählbar.
   * @param namespace
   *          der Namensraum aus dem die IDs von idManager genommen werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelectionAccessView(FunctionSelectionAccess funcSel,
      FunctionLibrary funcLib, IDManager idManager, Object namespace)
  {
    this.funcSel = funcSel;
    this.funcLib = funcLib;
    this.idManager = idManager;
    this.namespace = namespace;
    myPanel = new JPanel(new GridBagLayout());

    updateExpertFunctionTimer = new Timer(250, new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        updateExpertFunction();
      }
    });
    updateExpertFunctionTimer.setCoalesce(true);
    updateExpertFunctionTimer.setRepeats(false);

    buildPanel();
  }

  /**
   * Baut {@link #myPanel} komplett neu auf für den momentanen Zustand des
   * FunctionSelectionAccess.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void buildPanel()
  {
    myPanel.removeAll();

    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcLabelLeft =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcHsep =
      new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL,
        new Insets(3 * TF_BORDER, 0, 2 * TF_BORDER, 0), 0, 0);
    GridBagConstraints gbcTextfield =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcTextarea =
      new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
        GridBagConstraints.BOTH, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcGlue =
      new GridBagConstraints(0, 0, 2, 1, 1.0, 0.01, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    int y = 0;
    JLabel label = new JLabel(L.m("Funktion"));
    gbcLabelLeft.gridx = 0;
    gbcLabelLeft.gridy = y;
    myPanel.add(label, gbcLabelLeft);

    if (functionSelectorBox == null) functionSelectorBox = buildFunctionSelector();
    gbcTextfield.gridx = 1;
    gbcTextfield.gridy = y++;
    myPanel.add(functionSelectorBox, gbcTextfield);

    JSeparator seppl = new JSeparator(SwingConstants.HORIZONTAL);
    gbcHsep.gridx = 0;
    gbcHsep.gridy = y++;
    myPanel.add(seppl, gbcHsep);

    if (funcSel.isExpert())
    {
      ConfigThingy conf = funcSel.getExpertFunction();

      if (functionSelectorBox.getSelectedItem().toString().equals(STRING_ITEM))
      {
        expertFunctionIsComplex = false;

        /*
         * Nur wenn es sich bei der Funktion um einen einfachen String handelt, wird
         * dieser als Vorbelegung genommen. Ist die aktuelle Funktion eine komplexere
         * Funktion, so wird nur der leere String als Vorbelegung genommen.
         */
        String literal = "";
        if (conf.count() == 1 && conf.iterator().next().count() == 0)
        {
          literal = conf.toString();
        }
        literalValueArea = new JTextArea(literal);
        gbcTextarea.gridx = 0;
        gbcTextarea.gridy = y++;
        JScrollPane scrollPane =
          new JScrollPane(literalValueArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        myPanel.add(scrollPane, gbcTextarea);

        literalValueArea.getDocument().addDocumentListener(
          new ExpertFunctionChangeListener());

      }
      else
      // komplexere Expertenfunktion
      {
        expertFunctionIsComplex = true;

        StringBuilder code = new StringBuilder();
        Iterator<ConfigThingy> iter = conf.iterator();
        while (iter.hasNext())
        {
          code.append(iter.next().stringRepresentation());
        }

        complexFunctionArea = new JTextArea(code.toString());
        gbcTextarea.gridx = 0;
        gbcTextarea.gridy = y++;
        JScrollPane scrollPane =
          new JScrollPane(complexFunctionArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        myPanel.add(scrollPane, gbcTextarea);

        complexFunctionArea.getDocument().addDocumentListener(
          new ExpertFunctionChangeListener());
      }

    }
    else if (funcSel.isReference())
    {
      String[] names = funcSel.getParameterNames();
      for (int i = 0; i < names.length; ++i)
      {
        label = new JLabel(names[i]);
        gbcLabelLeft.gridx = 0;
        gbcLabelLeft.gridy = y;
        myPanel.add(label, gbcLabelLeft);

        JComboBox<String> box =
          buildParameterBox(names[i], funcSel.getParameterValue(names[i]));
        gbcTextfield.gridx = 1;
        gbcTextfield.gridy = y++;
        myPanel.add(box, gbcTextfield);
      }
    }

    Component glue = Box.createGlue();
    gbcGlue.gridx = 0;
    gbcGlue.gridy = y++;
    myPanel.add(glue, gbcGlue);

    myPanel.validate();
  }

  /**
   * Liefert eine JComboBox zurück über die der Benutzer einen Parameter der Funktion
   * festlegen kann.
   * 
   * @param paramName
   *          der Name des Parameters.
   * @param startValue
   *          der Wert, den die ComboBox zu Beginn eingestellt haben soll.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComboBox<String> buildParameterBox(final String paramName, ParamValue startValue)
  {
    final JComboBox<String> combo = new JComboBox<String>();
    combo.setEditable(true);

    combo.addItem(UNSPECIFIED_ITEM);
    combo.setSelectedIndex(0);
    if (startValue.isLiteral())
    {
      String lit = startValue.getString();
      combo.addItem(lit);
      combo.setSelectedIndex(combo.getItemCount() - 1);
    }
    else if (startValue.isFieldReference())
    {
      String brackId = "[" + startValue.getString() + "]";
      combo.addItem(brackId);
      combo.setSelectedIndex(combo.getItemCount() - 1);
    }

    updateParameterBox(combo);

    combo.addPopupMenuListener(new PopupMenuListener()
    {
      public void popupMenuWillBecomeVisible(PopupMenuEvent e)
      {
        updateParameterBox(combo);
      }

      public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
      {}

      public void popupMenuCanceled(PopupMenuEvent e)
      {}
    });

    JTextComponent tc = ((JTextComponent) combo.getEditor().getEditorComponent());
    tc.getDocument().addDocumentListener(new DocumentListener()
    {
      private void update()
      {
        Document comboDoc =
          ((JTextComponent) combo.getEditor().getEditorComponent()).getDocument();
        try
        {
          String newValue = comboDoc.getText(0, comboDoc.getLength());
          if (newValue.equals(UNSPECIFIED_ITEM))
            funcSel.setParameterValue(paramName, ParamValue.unspecified());
          else
          {
            boolean isLiteral = true;

            // wenn newValue die Form [...] hat (also potentiell eine Feldreferenz
            // ist)
            if (newValue.length() > 2 && newValue.charAt(0) == '['
              && newValue.charAt(newValue.length() - 1) == ']')
            {
              // "[" und "]" entfernen
              String idStr = newValue.substring(1, newValue.length() - 1);
              IDManager.ID id = idManager.getExistingID(namespace, idStr);
              if (id != null && id.isActive())
              {
                funcSel.setParameterValue(paramName, ParamValue.field(id));
                isLiteral = false;
              }
            }

            if (isLiteral)
              funcSel.setParameterValue(paramName, ParamValue.literal(newValue));
          }
        }
        catch (BadLocationException x)
        {
          LOGGER.error("", x);
          return;
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

    return combo;
  }

  /**
   * Aktualisiert die Liste der Einträge in combo, so dass sie den aktiven IDs von
   * {@link #idManager} entspricht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void updateParameterBox(JComboBox<String> combo)
  {
    Document comboDoc =
      ((JTextComponent) combo.getEditor().getEditorComponent()).getDocument();
    String currentValue = "";
    try
    {
      currentValue = comboDoc.getText(0, comboDoc.getLength());
    }
    catch (BadLocationException e)
    {
      LOGGER.error("", e);
    }
    combo.removeAllItems();
    combo.addItem(UNSPECIFIED_ITEM);
    combo.setSelectedIndex(0);
    boolean found = currentValue.equals(UNSPECIFIED_ITEM);
    Iterator<IDManager.ID> iter = idManager.getAllIDs(namespace).iterator();
    while (iter.hasNext())
    {
      IDManager.ID id = iter.next();
      if (id.isActive())
      {
        String brackId = "[" + id.toString() + "]";
        combo.addItem(brackId);
        if (currentValue.equals(brackId))
        {
          combo.setSelectedIndex(combo.getItemCount() - 1);
          found = true;
        }
      }
    }

    if (!found)
    {
      combo.addItem(currentValue);
      combo.setSelectedIndex(combo.getItemCount() - 1);
    }
  }

  /**
   * Liefert eine JComboBox, die die Auswahl einer Funktion aus {@link #funcLib}
   * erlaubt.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComboBox<String> buildFunctionSelector()
  {
    functionSelectorBox = new JComboBox<String>();
    int selectedIndex = 0;
    int none_index = functionSelectorBox.getItemCount();
    functionSelectorBox.addItem(NONE_ITEM);
    int string_index = functionSelectorBox.getItemCount();
    functionSelectorBox.addItem(STRING_ITEM);
    Iterator<String> iter = funcLib.getFunctionNames().iterator();
    int i = functionSelectorBox.getItemCount();

    while (iter.hasNext())
    {
      String funcName = iter.next();

      if (funcName.equals(funcSel.getFunctionName())) selectedIndex = i;
      functionSelectorBox.addItem(funcName);
      ++i;
    }
    int expert_index = functionSelectorBox.getItemCount();
    functionSelectorBox.addItem(EXPERT_ITEM);

    if (funcSel.isNone())
      selectedIndex = none_index;
    else if (funcSel.isExpert())
    {
      selectedIndex = expert_index;
      try
      {
        ConfigThingy expertFun = funcSel.getExpertFunction();

        // falls die Expertenfunktion leer ist oder nur ein Kind hat und keine Enkel
        // (d.h. wenn die Funktion ein String-Literal ist), dann wird der
        // Spezialeintrag
        // STRING_ITEM gewählt anstatt EXPERT_ITEM.
        if (expertFun.count() == 0
          || (expertFun.count() == 1 && expertFun.getFirstChild().count() == 0))
          selectedIndex = string_index;
      }
      catch (NodeNotFoundException x)
      {}
    }

    functionSelectorBox.setSelectedIndex(selectedIndex);

    functionSelectorBox.addItemListener(new FunctionSelectionBoxItemListener());

    return functionSelectorBox;
  }

  /**
   * Schreibt die aktuelle manuelle Eingabe der Expertenfunktion in den
   * FunctionSelectionAccess zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void updateExpertFunction()
  {
    // Falls updateExpertFunction() außer der Reihe aufgerufen wurde muss
    // der Timer gestoppt werden, damit keine unnötigen (und im Falle, dass sich
    // Rahmenbedingungen zwischenzeitlich geändert haben fehlerhaften) Aufrufe
    // erfolgen.
    updateExpertFunctionTimer.stop();

    if (expertFunctionIsComplex)
    {
      try
      {
        ConfigThingy conf =
          new ConfigThingy("", null, new StringReader(complexFunctionArea.getText()));
        funcSel.setExpertFunction(conf);
        complexFunctionArea.setBackground(Color.WHITE);
      }
      catch (Exception e1)
      {
        complexFunctionArea.setBackground(Color.PINK);
      }

    }
    else
    {
      ConfigThingy conf = new ConfigThingy("EXPERT");
      conf.add(literalValueArea.getText());
      funcSel.setExpertFunction(conf);
    }
  }

  /**
   * Dieser Listener wird sowohl für
   * {@link FunctionSelectionAccessView#literalValueArea} als auch
   * {@link FunctionSelectionAccessView#complexFunctionArea} verwendet, um auf
   * Benutzereingaben zu reagieren.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class ExpertFunctionChangeListener implements DocumentListener
  {
    public void insertUpdate(DocumentEvent e)
    {
      updateExpertFunctionTimer.restart();
    }

    public void removeUpdate(DocumentEvent e)
    {
      updateExpertFunctionTimer.restart();
    }

    public void changedUpdate(DocumentEvent e)
    {
      updateExpertFunctionTimer.restart();
    }
  }

  /**
   * Wird auf die Funktionsauswahl-Kombobox registriert und reagiert darauf, dass der
   * Benutzer eine andere Funktion auswählt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class FunctionSelectionBoxItemListener implements ItemListener
  {
    /*
     * TESTED
     */
    public void itemStateChanged(ItemEvent e)
    {
      if (e.getStateChange() == ItemEvent.SELECTED)
      {
        if (updateExpertFunctionTimer.isRunning()) updateExpertFunction();

        String item = functionSelectorBox.getSelectedItem().toString();
        String functionName = item;
        String[] paramNames = null;
        if (item.equals(EXPERT_ITEM) || item.equals(STRING_ITEM))
          functionName = FunctionSelectionAccess.EXPERT_FUNCTION;
        else if (item.equals(NONE_ITEM))
          functionName = FunctionSelectionAccess.NO_FUNCTION;
        else
        {
          Function func = funcLib.get(functionName);
          if (func == null)
          {
            LOGGER.error(L.m("Funktion \"%1\"\" ist verschwunden ?!?", functionName));
          }
          paramNames = func.parameters();
        }

        funcSel.setFunction(functionName, paramNames);
        buildPanel();
      }
    }
  }

  public JComponent getComponent()
  {
    return myPanel;
  }

}
