/*
 * Dateiname: AdjustFields.java
 * Projekt  : WollMux
 * Funktion : "Felder anpassen" Dialog der neuen erweiterten Serienbrief-Funktionalitäten
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
 * 11.10.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Dock;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.ReferencedFieldID;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;

/**
 * "Felder anpassen" Dialog der neuen erweiterten Serienbrief-Funktionalitäten.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AdjustFields
{
  /**
   * Präfix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die
   * Zuordnung beginnt mit einem zero width space (nicht sichtbar, aber zur
   * Unterscheidung des Präfix von den Benutzereingaben) und dem "<"-Zeichen.
   */
  private static final String TAG_PREFIX = "" + Character.toChars(0x200B)[0] + "<";

  /**
   * Suffix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die
   * Zuordnung beginnt mit einem zero width space (nicht sichtbar, aber zur
   * Unterscheidung des Präfix von den Benutzereingaben) und dem ">"-Zeichen.
   */
  private static final String TAG_SUFFIX = "" + Character.toChars(0x200B)[0] + ">";

  /**
   * Beschreibt einen regulären Ausdruck, mit dem nach Tags im Text gesucht
   * werden kann. Ein Match liefert in Gruppe 1 den Text des Tags.
   */
  private static final Pattern TAG_PATTERN = Pattern.compile("(" + TAG_PREFIX + "(.*?)" + TAG_SUFFIX + ")");

  private AdjustFields()
  {
    // nothing to initialize
  }

  /**
   * Diese Methode zeigt den Dialog an, mit dem die Felder im Dokument an eine Datenquelle angepasst
   * werden können, die nicht die selben Spalten enthält wie die Datenquelle, für die das Dokument
   * gemacht wurde.
   *
   * @param documentController
   *          Der Controller des Dokumentes.
   * @param ds
   *          Die Datenquelle.
   * @param finishedListener
   *          Der Listener wird aufgerufen, sobald der Dialog erfolgreich beendet wird.
   */
  public static void showAdjustFieldsDialog(final TextDocumentController documentController,
      MailMergeDatasource ds, ActionListener finishedListener)
  {
    ReferencedFieldID[] fieldIDs =
      documentController.getModel().getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(
        ds.getColumnNames()));
    ActionListener submitActionListener = e ->
    {
      Map<String, FieldSubstitution> mapIdToSubstitution = (HashMap<String, FieldSubstitution>) e
          .getSource();
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
          if (ele.isField())
            documentController.updateFormFields(ele.getValue());
        }
        finishedListener
            .actionPerformed(new ActionEvent(new Object(), 0, "AdjustFieldsDialogFinished"));
      }
    };
    showFieldMappingDialog(fieldIDs, L.m("Altes Feld"), L.m("Neue Belegung"),
        L.m("Felder anpassen"),
      ds.getColumnNames(), submitActionListener, false);
  }

  /**
   * Diese Methode zeigt den Dialog an, mit dem die Spalten der Tabelle ergänzt werden können, wenn
   * es zu Feldern im Dokument keine passenden Spalten in der Tabelle gibt.
   *
   * @param documentController
   *          Der Controller des Dokumentes.
   * @param ds
   *          Die Datenquelle.
   * @param finishedListener
   *          Der Listener wird aufgerufen, sobald der Dialog erfolgreich beendet wird.
   */
  public static void showAddMissingColumnsDialog(TextDocumentController documentController,
      final MailMergeDatasource ds, ActionListener finishedListener)
  {
    ReferencedFieldID[] fieldIDs =
        documentController.getModel().getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(
        ds.getColumnNames()));
    ActionListener submitActionListener = e ->
    {
      Map<String, FieldSubstitution> mapIdToSubstitution =
        (HashMap<String, FieldSubstitution>) e.getSource();
      ds.addColumns(mapIdToSubstitution);
      finishedListener
          .actionPerformed(new ActionEvent(new Object(), 0, "AddMissingDialogFinished"));
    };
    showFieldMappingDialog(fieldIDs, L.m("Spalte"), L.m("Vorbelegung"), L.m("Spalten ergänzen"),
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
   */
  private static void showFieldMappingDialog(
      ReferencedFieldID[] fieldIDs, String labelOldFields,
      String labelNewFields, String labelSubmitButton,
      final List<String> fieldNames, final ActionListener submitActionListener,
      boolean ignoreIsTransformed)
  {
    final XTextComponent[] currentField = new XTextComponent[] { null };
    final Map<XTextComponent, ReferencedFieldID> mapXTextComponentToFieldname = new HashMap<>();
    UNODialogFactory dialogFactory = new UNODialogFactory();
    XWindow dialogWindow = dialogFactory.createDialog(780, 700, 0xF2F2F2);
    dialogFactory.showDialog();

    SimpleDialogLayout layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    ControlProperties mailmergeLabel = new ControlProperties(ControlType.LABEL, "mailmergeLabel");
    mailmergeLabel.setControlPercentSize(40, 20);
    mailmergeLabel.setLabel("Serienbrieffeld");
    ControlProperties mailmerge = new ControlProperties(ControlType.COMBOBOX, "mailmerge");
    mailmerge.setControlPercentSize(60, 20);
    mailmerge.setComboBoxDropDown(true);
    XComboBox mailmergeButton = UNO.XComboBox(mailmerge.getXControl());
    mailmergeButton.addItem("", (short) 0);
    List<String> columnNames = new ArrayList<>(fieldNames);
    Collections.sort(columnNames);
    mailmergeButton.addItems(columnNames.toArray(new String[0]), (short) 1);
    UNO.XTextComponent(mailmergeButton).setText(mailmergeButton.getItem((short) 0));
    mailmergeButton.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        if (event.Selected == 0)
        {
          return;
        }
        if (currentField[0] != null)
        {
          String text = TAG_PREFIX + mailmergeButton.getItem((short) event.Selected) + TAG_SUFFIX;
          currentField[0].insertText(currentField[0].getSelection(), text);
        }
        UNO.XTextComponent(mailmergeButton).setText(mailmergeButton.getItem((short) 0));
      }
    });
    List<ControlProperties> controls = new ArrayList<>();
    controls.add(mailmergeLabel);
    controls.add(mailmerge);
    layout.addControlsToList(new ControlModel(Orientation.HORIZONTAL, Align.NONE, controls, Optional.of(Dock.TOP)));

    ControlProperties column = new ControlProperties(ControlType.LABEL, "column");
    column.setControlPercentSize(40, 20);
    column.setLabel(labelOldFields);
    ControlProperties preset = new ControlProperties(ControlType.LABEL, "preset");
    preset.setControlPercentSize(40, 20);
    preset.setLabel(labelNewFields);
    controls = new ArrayList<>();
    controls.add(column);
    controls.add(preset);
    layout.addControlsToList(new ControlModel(Orientation.HORIZONTAL, Align.NONE, controls, Optional.of(Dock.TOP)));

    HashSet<ReferencedFieldID> addedFields = new HashSet<>();
    for (ReferencedFieldID fieldId : fieldIDs)
    {
      if (addedFields.contains(fieldId))
      {
        continue;
      }
      addedFields.add(fieldId);
      ControlProperties label = new ControlProperties(ControlType.LABEL, "label_" + fieldId.getFieldId());
      label.setControlPercentSize(40, 20);
      label.setLabel(fieldId.getFieldId());
      ControlProperties edit = new ControlProperties(ControlType.EDIT, "edit_" + fieldId.getFieldId());
      edit.setControlPercentSize(60, 20);
      XTextComponent editField = UNO.XTextComponent(edit.getXControl());
      UNO.XWindow(edit.getXControl()).addFocusListener(new AbstractFocusListener()
      {
        @Override
        public void focusGained(com.sun.star.awt.FocusEvent event)
        {
          currentField[0] = editField;
        }
      });
      mapXTextComponentToFieldname.put(editField, fieldId);
      controls = new ArrayList<>();
      controls.add(label);
      controls.add(edit);
      layout.addControlsToList(new ControlModel(Orientation.HORIZONTAL, Align.NONE, controls, Optional.of(Dock.TOP)));
    }

    ControlProperties abort = new ControlProperties(ControlType.BUTTON, "abort");
    abort.setControlPercentSize(50, 40);
    abort.setLabel("Abbrechen");
    XButton abortXBtn = UNO.XButton(abort.getXControl());
    abortXBtn.addActionListener(new AbstractActionListener()
    {
      @Override
      public void actionPerformed(com.sun.star.awt.ActionEvent event)
      {
        dialogFactory.closeDialog();
      }
    });

    ControlProperties edit = new ControlProperties(ControlType.BUTTON, "add");
    edit.setControlPercentSize(50, 40);
    edit.setLabel(labelSubmitButton);
    XButton editXBtn = UNO.XButton(edit.getXControl());
    editXBtn.addActionListener(new AbstractActionListener()
    {

      @Override
      public void actionPerformed(com.sun.star.awt.ActionEvent arg0)
      {
        final HashMap<String, FieldSubstitution> result = new HashMap<>();

        for (Map.Entry<XTextComponent, ReferencedFieldID> entry : mapXTextComponentToFieldname.entrySet())
        {
          if (!isContentValid(entry.getKey(), entry.getValue().isTransformed() && !ignoreIsTransformed))
            continue;
          FieldSubstitution subst = new TextDocumentModel.FieldSubstitution();
          for (ContentElement ce : getContent(entry.getKey()))
          {
            if (ce.isTag())
              subst.addField(ce.toString());
            else
              subst.addFixedText(ce.toString());
          }
          result.put(entry.getValue().getFieldId(), subst);
        }

        dialogFactory.closeDialog();
        if (submitActionListener != null)
          submitActionListener
            .actionPerformed(new ActionEvent(result, 0, "showSubstitutionDialogReturned"));
      }
    });


    controls = new ArrayList<>();
    controls.add(abort);
    controls.add(edit);
    layout.addControlsToList(new ControlModel(Orientation.HORIZONTAL, Align.NONE, controls, Optional.of(Dock.BOTTOM)));

    layout.draw();
  }

  private static boolean isContentValid(XTextComponent compo, boolean isTransformed)
  {
    if (!isTransformed)
    {
      return true;
    }
    List<ContentElement> c = getContent(compo);
    if (c.isEmpty())
    {
      return true;
    }
    return c.size() == 1 && c.get(0).isTag();
  }

  /**
   * Liefert eine Liste von {@link ContentElement}-Objekten, die den aktuellen
   * Inhalt der JTextComponent repräsentiert und dabei enthaltenen Text und
   * evtl. enthaltene Tags als eigene Objekte kapselt.
   */
  private static List<ContentElement> getContent(XTextComponent compo)
  {
    List<ContentElement> list = new ArrayList<>();
    String t = compo.getText();
    Matcher m = TAG_PATTERN.matcher(t);
    int lastEndPos = 0;
    int startPos = 0;
    while (m.find())
    {
      startPos = m.start();
      String tag = m.group(2);
      list.add(new ContentElement(t.substring(lastEndPos, startPos), false));
      if (tag.length() > 0)
      {
        list.add(new ContentElement(tag, true));
      }
      lastEndPos = m.end();
    }
    String text = t.substring(lastEndPos);
    if (text.length() > 0)
    {
      list.add(new ContentElement(text, false));
    }
    return list;
  }

  /**
   * Beschreibt ein Element des Inhalts dieser JTextComponent und kann entweder
   * ein eingefügtes Tag oder ein normaler String sein. Auskunft über den Typ
   * des Elements erteilt die Methode isTag(), auf den String-Wert kann über die
   * toString()-Methode zugegriffen werden.
   */
  public static class ContentElement
  {
    private String value;

    private boolean isTag;

    private ContentElement(String value, boolean isTag)
    {
      this.value = value;
      this.isTag = isTag;
    }

    @Override
    public String toString()
    {
      return value;
    }

    /**
     * Liefert true, wenn dieses Element ein Tag ist oder false, wenn es sich um
     * normalen Text handelt.
     */
    public boolean isTag()
    {
      return isTag;
    }
  }

}
