/*
 * Dateiname: AdjustFields.java
 * Projekt  : WollMux
 * Funktion : "Felder anpassen" Dialog der neuen erweiterten Serienbrief-Funktionalitäten
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
 * 11.10.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.core.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.ReferencedFieldID;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;

/**
 * "Felder anpassen" Dialog der neuen erweiterten Serienbrief-Funktionalitäten.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
class AdjustFields
{

  private AdjustFields() {}
  
  /**
   * Diese Methode zeigt den Dialog an, mit dem die Felder im Dokument an eine
   * Datenquelle angepasst werden können, die nicht die selben Spalten enthält wie
   * die Datenquelle, für die das Dokument gemacht wurde.
   * 
   * @param parent
   *          das Elternfenster des anzuzeigenden Dialogs
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  static void showAdjustFieldsDialog(JFrame parent, final TextDocumentController documentController,
      MailMergeDatasource ds)
  {
    ReferencedFieldID[] fieldIDs =
      documentController.getModel().getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(
        ds.getColumnNames()));
    ActionListener submitActionListener = e ->
    {
        HashMap<String, FieldSubstitution> mapIdToSubstitution =
          (HashMap<String, FieldSubstitution>) e.getSource();
        for (Map.Entry<String, FieldSubstitution> ent : mapIdToSubstitution.entrySet())
        {
          String fieldId = ent.getKey();
          FieldSubstitution subst = ent.getValue();
          documentController.applyFieldSubstitution(fieldId, subst);
          
          // Datenstrukturen aktualisieren
          documentController.updateDocumentCommands();
          DocumentCommandInterpreter dci = new DocumentCommandInterpreter(documentController);
          dci.scanGlobalDocumentCommands();
          // collectNonWollMuxFormFields() wird im folgenden scan auch noch erledigt
          dci.scanInsertFormValueCommands();

          // Alte Formularwerte aus den persistenten Daten entfernen
          documentController.setFormFieldValue(fieldId, null);

          // Ansicht der betroffenen Felder aktualisieren
          for (Iterator<FieldSubstitution.SubstElement> iter = subst.iterator(); iter.hasNext();)
          {
            FieldSubstitution.SubstElement ele = iter.next();
            if (ele.isField()) documentController.updateFormFields(ele.getValue());
          }

        }
    };
    showFieldMappingDialog(parent, fieldIDs, L.m("Felder anpassen"),
      L.m("Altes Feld"), L.m("Neue Belegung"), L.m("Felder anpassen"),
      ds.getColumnNames(), submitActionListener, false);
  }

  /**
   * Diese Methode zeigt den Dialog an, mit dem die Spalten der Tabelle ergänzt
   * werden können, wenn es zu Feldern im Dokument keine passenden Spalten in der
   * Tabelle gibt.
   * 
   * @param parent
   *          das Elternfenster des anzuzeigenden Dialogs
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  static void showAddMissingColumnsDialog(JFrame parent, TextDocumentController documentController,
      final MailMergeDatasource ds)
  {
    ReferencedFieldID[] fieldIDs =
        documentController.getModel().getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(
        ds.getColumnNames()));
    ActionListener submitActionListener = e ->
    {
      HashMap<String, FieldSubstitution> mapIdToSubstitution =
        (HashMap<String, FieldSubstitution>) e.getSource();
      ds.addColumns(mapIdToSubstitution);
    };
    showFieldMappingDialog(parent, fieldIDs, L.m("Tabellenspalten ergänzen"),
      L.m("Spalte"), L.m("Vorbelegung"), L.m("Spalten ergänzen"),
      ds.getColumnNames(), submitActionListener, true);

  }

  /**
   * Zeigt einen Dialog mit dem bestehende Felder fieldIDs über ein Textfeld neu
   * belegt werden können; für die neue Belegung stehen die neuen Felder der
   * aktuellen Datasource und Freitext zur Verfügung. Die Felder fieldIDs werden
   * dabei in der Reihenfolge angezeigt, in der sie in der Liste aufgeführt sind, ein
   * bereits aufgeführtes Feld wird aber nicht zweimal angezeigt. Ist bei einem Feld
   * die Eigenschaft isTransformed()==true und ignoreIsTransformed == false, dann
   * wird für dieses Feld nur die Eingabe einer 1-zu-1 Zuordnung von Feldern
   * akzeptiert, das andere Zuordnungen für transformierte Felder derzeit nicht
   * unterstützt werden.
   * 
   * @param parent
   *          Das Elternfenster dieses Dialogs.
   * @param fieldIDs
   *          Die field-IDs der alten, bereits im Dokument enthaltenen Felder, die in
   *          der gegebenen Reihenfolge angezeigt werden, Dupletten werden aber
   *          entfernt.
   * @param title
   *          Die Titelzeile des Dialogs
   * @param labelOldFields
   *          Die Spaltenüberschrift für die linke Spalte, in der die alten Felder
   *          angezeigt werden.
   * @param labelNewFields
   *          Die Spaltenüberschrift für die rechte Spalte, in dem die neue Zuordnung
   *          getroffen wird.
   * @param labelSubmitButton
   *          Die Beschriftung des Submit-Knopfes unten rechts, der die entsprechende
   *          Aktion auslöst.
   * @param fieldNames
   *          Die Namen aller Serienbrieffelder, die in dem Mapping verwendet werden
   *          können.
   * @param submitActionListener
   *          Nach Beendigung des Dialogs über den Submit-Knopf (unten rechts) wird
   *          die Methode submitActionListener.actionPerformed(actionEvent) in einem
   *          separaten Thread aufgerufen. Dort kann der Code stehen, der gewünschten
   *          Aktionen durchführt. Der ActionListener bekommt dabei in actionEvent
   *          eine HashMap übergeben, die eine Zuordnung von den alten fieldIDs auf
   *          den jeweiligen im Dialog gewählten Ersatzstring enthält.
   * @param ignoreIsTransformed
   *          falls true, werden Felder mit isTransformed()==true nicht speziell
   *          behandelt und es gibt keine Einschränkungen bzw. der
   *          Auswahlmöglichkeiten.
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static void showFieldMappingDialog(JFrame parent,
      ReferencedFieldID[] fieldIDs, String title, String labelOldFields,
      String labelNewFields, String labelSubmitButton,
      final List<String> fieldNames, final ActionListener submitActionListener,
      boolean ignoreIsTransformed)
  {
    //set JDialog to Modeless type so that it remains visible when changing focus between opened 
    //calc and writer document. Drawback: when this Dialog is open, the "Seriendruck" bar is 
    //active too.
    final JDialog dialog = new JDialog(parent, title, false);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final TextComponentTags[] currentField = new TextComponentTags[] { null };
    final HashMap<TextComponentTags, String> mapTextComponentTagsToFieldname = new HashMap<>();

    Box headers = Box.createHorizontalBox();
    final JButton insertFieldButton =
      new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
        new Iterable<Action>()
        {
          @Override
          public Iterator<Action> iterator()
          {
            List<Action> actions = new ArrayList<>();
            List<String> columnNames = new ArrayList<>(fieldNames);
            Collections.sort(columnNames);

            Iterator<String> iter = columnNames.iterator();
            while (iter.hasNext())
            {
              final String name = iter.next();
              Action button = new AbstractAction(name)
              {
                private static final long serialVersionUID = 3688585907102784521L;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                  if (currentField[0] != null) currentField[0].insertTag(name);
                }
              };
              actions.add(button);
            }

            return actions.iterator();
          }
        });
    insertFieldButton.setFocusable(false);
    headers.add(Box.createHorizontalGlue());
    headers.add(insertFieldButton);

    Box itemBox = Box.createVerticalBox();
    ArrayList<JLabel> labels = new ArrayList<>();
    int maxLabelWidth = 0;

    Box hbox = Box.createHorizontalBox();
    JLabel label = new JLabel(labelOldFields);
    labels.add(label);
    maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    hbox.add(label);
    label = new JLabel(labelNewFields);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(label);
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(200));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);
    itemBox.add(hbox);

    HashSet<String> addedFields = new HashSet<>();
    for (int i = 0; i < fieldIDs.length; i++)
    {
      String fieldId = fieldIDs[i].getFieldId();
      if (addedFields.contains(fieldId)) continue;
      final boolean isTransformed =
        fieldIDs[i].isTransformed() && !ignoreIsTransformed;
      addedFields.add(fieldId);

      hbox = Box.createHorizontalBox();

      label = new JLabel(fieldId);
      label.setFont(label.getFont().deriveFont(Font.PLAIN));
      labels.add(label);
      maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
      label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
      hbox.add(label);

      final TextComponentTags field = new TextComponentTags(new JTextField())
      {
        @Override
        public boolean isContentValid()
        {
          if (!isTransformed) return true;
          List<TextComponentTags.ContentElement> c = getContent();
          if (c.isEmpty()) return true;
          return c.size() == 1 && c.get(0).isTag();
        }
      };
      mapTextComponentTagsToFieldname.put(field, fieldId);
      Box fbox = Box.createHorizontalBox();
      hbox.add(fbox); // fbox für zeilenbündige Ausrichtung benötigt
      fbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
      field.getJTextComponent().addFocusListener(new FocusListener()
      {
        @Override
        public void focusLost(FocusEvent e)
        {}

        @Override
        public void focusGained(FocusEvent e)
        {
          currentField[0] = field;
        }
      });
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(field.getJTextComponent());
      fbox.add(field.getJTextComponent());

      itemBox.add(hbox);
    }

    // einheitliche Breite für alle Labels vergeben:
    for (Iterator<JLabel> iter = labels.iterator(); iter.hasNext();)
    {
      label = iter.next();

      Dimension d = label.getPreferredSize();
      d.width = maxLabelWidth + 10;
      label.setPreferredSize(d);
    }

    Box buttonBox = Box.createHorizontalBox();
    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(e -> dialog.dispose());
    buttonBox.add(button);

    buttonBox.add(Box.createHorizontalGlue());

    button = new JButton(labelSubmitButton);
    button.addActionListener(e ->
    {
        final HashMap<String, FieldSubstitution> result = new HashMap<>();

      for (TextComponentTags f : mapTextComponentTagsToFieldname.keySet())
        {
          if (!f.isContentValid()) continue;
          String fieldId = "" + mapTextComponentTagsToFieldname.get(f);
          FieldSubstitution subst = new TextDocumentModel.FieldSubstitution();
          for (TextComponentTags.ContentElement ce : f.getContent())
          {
            if (ce.isTag())
              subst.addField(ce.toString());
            else
              subst.addFixedText(ce.toString());
          }
          result.put(fieldId, subst);
        }

        dialog.dispose();

        if (submitActionListener != null) new Thread()
        {
          @Override
          public void run()
          {
            submitActionListener.actionPerformed(new ActionEvent(result, 0,
              "showSubstitutionDialogReturned"));
          }
        }.start();
    });
    buttonBox.add(button);

    JScrollPane spane =
      new JScrollPane(itemBox, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    spane.getVerticalScrollBar().setUnitIncrement(
      Common.getVerticalScrollbarUnitIncrement());

    spane.setBorder(BorderFactory.createEmptyBorder());
    dialog.add(spane);

    Box vbox = Box.createVerticalBox();
    vbox.add(headers);
    vbox.add(spane);
    vbox.add(Box.createVerticalGlue());
    vbox.add(buttonBox);
    vbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    dialog.add(vbox);

    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setVisible(true);
  }

}
