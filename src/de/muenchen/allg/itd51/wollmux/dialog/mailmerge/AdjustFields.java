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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XScrollBar;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractAdjustmentListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(AdjustFields.class);

  /**
   * Präfix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die Zuordnung beginnt mit
   * einem zero width space (nicht sichtbar, aber zur Unterscheidung des Präfix von den
   * Benutzereingaben) und dem "<"-Zeichen.
   */
  private static final String TAG_PREFIX = "" + Character.toChars(0x200B)[0] + "<";

  /**
   * Suffix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die Zuordnung beginnt mit
   * einem zero width space (nicht sichtbar, aber zur Unterscheidung des Präfix von den
   * Benutzereingaben) und dem ">"-Zeichen.
   */
  private static final String TAG_SUFFIX = "" + Character.toChars(0x200B)[0] + ">";

  /**
   * Beschreibt einen regulären Ausdruck, mit dem nach Tags im Text gesucht werden kann. Ein Match
   * liefert in Gruppe 1 den Text des Tags.
   */
  private static final Pattern TAG_PATTERN = Pattern
      .compile("(" + TAG_PREFIX + "(.*?)" + TAG_SUFFIX + ")");

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
    ReferencedFieldID[] fieldIDs = documentController.getModel()
        .getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(ds.getColumnNames()));
    ActionListener submitActionListener = e -> {
      @SuppressWarnings("unchecked")
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
      }
      finishedListener
          .actionPerformed(new ActionEvent(new Object(), 0, "AdjustFieldsDialogFinished"));
    };
    showFieldMappingDialog("Felder anpassen", fieldIDs, L.m("Altes Feld"), L.m("Neue Belegung"),
        L.m("Felder anpassen"), ds.getColumnNames(), submitActionListener, false);
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
    ReferencedFieldID[] fieldIDs = documentController.getModel()
        .getReferencedFieldIDsThatAreNotInSchema(new HashSet<>(ds.getColumnNames()));
    ActionListener submitActionListener = e -> {
      @SuppressWarnings("unchecked")
      Map<String, FieldSubstitution> mapIdToSubstitution = (HashMap<String, FieldSubstitution>) e
          .getSource();
      ds.addColumns(mapIdToSubstitution);
      ds.updatePreviewFields(ds.getPreviewDatasetNumber());
      finishedListener
          .actionPerformed(new ActionEvent(new Object(), 0, "AddMissingDialogFinished"));
    };
    showFieldMappingDialog("Tabellenspalten ergänzen", fieldIDs, L.m("Spalte"), L.m("Vorbelegung"),
        L.m("Spalten ergänzen"), ds.getColumnNames(), submitActionListener, true);
  }

  /**
   * Zeigt einen Dialog mit dem bestehende Felder fieldIDs über ein Textfeld neu belegt werden
   * können; für die neue Belegung stehen die neuen Felder der aktuellen Datasource und Freitext zur
   * Verfügung. Die Felder fieldIDs werden dabei in der Reihenfolge angezeigt, in der sie in der
   * Liste aufgeführt sind, ein bereits aufgeführtes Feld wird aber nicht zweimal angezeigt. Ist bei
   * einem Feld die Eigenschaft isTransformed()==true und ignoreIsTransformed == false, dann wird
   * für dieses Feld nur die Eingabe einer 1-zu-1 Zuordnung von Feldern akzeptiert, das andere
   * Zuordnungen für transformierte Felder derzeit nicht unterstützt werden.
   *
   * @param fieldIDs
   *          Die field-IDs der alten, bereits im Dokument enthaltenen Felder, die in der gegebenen
   *          Reihenfolge angezeigt werden, Dupletten werden aber entfernt.
   * @param labelOldFields
   *          Die Spaltenüberschrift für die linke Spalte, in der die alten Felder angezeigt werden.
   * @param labelNewFields
   *          Die Spaltenüberschrift für die rechte Spalte, in dem die neue Zuordnung getroffen
   *          wird.
   * @param labelSubmitButton
   *          Die Beschriftung des Submit-Knopfes unten rechts, der die entsprechende Aktion
   *          auslöst.
   * @param fieldNames
   *          Die Namen aller Serienbrieffelder, die in dem Mapping verwendet werden können.
   * @param submitActionListener
   *          Nach Beendigung des Dialogs über den Submit-Knopf (unten rechts) wird die Methode
   *          submitActionListener.actionPerformed(actionEvent) in einem separaten Thread
   *          aufgerufen. Dort kann der Code stehen, der gewünschten Aktionen durchführt. Der
   *          ActionListener bekommt dabei in actionEvent eine HashMap übergeben, die eine Zuordnung
   *          von den alten fieldIDs auf den jeweiligen im Dialog gewählten Ersatzstring enthält.
   * @param ignoreIsTransformed
   *          falls true, werden Felder mit isTransformed()==true nicht speziell behandelt und es
   *          gibt keine Einschränkungen bzw. der Auswahlmöglichkeiten.
   */
  private static void showFieldMappingDialog(String title, ReferencedFieldID[] fieldIDs,
      String labelOldFields, String labelNewFields, String labelSubmitButton,
      final List<String> fieldNames, final ActionListener submitActionListener,
      boolean ignoreIsTransformed)
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.edit_table_columns?location=application", "", peer, null);
      XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
      XDialog dialog = UnoRuntime.queryInterface(XDialog.class, window);
      dialog.setTitle(title);

      XTextComponent[] currentField = new XTextComponent[] { null };
      Map<ReferencedFieldID, String> mapIdTwoNewValue = new HashMap<>();

      XComboBox mailmergeFields = UNO.XComboBox(container.getControl("mailmergeFields"));
      mailmergeFields.addItem("", (short) 0);
      Collections.sort(fieldNames);
      mailmergeFields.addItems(fieldNames.toArray(new String[fieldNames.size()]), (short) 1);
      mailmergeFields.addItemListener(new AbstractItemListener()
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
            String text = TAG_PREFIX + mailmergeFields.getItem((short) event.Selected) + TAG_SUFFIX;
            currentField[0].insertText(currentField[0].getSelection(), text);
          }
          UNO.XTextComponent(mailmergeFields).setText(mailmergeFields.getItem((short) 0));
        }
      });

      XFixedText leftColumn = UNO.XFixedText(container.getControl("column"));
      leftColumn.setText(labelOldFields);

      XFixedText rightColumn = UNO.XFixedText(container.getControl("values"));
      rightColumn.setText(labelNewFields);

      XScrollBar scrollBar = UnoRuntime.queryInterface(XScrollBar.class,
          container.getControl("ScrollBar"));
      scrollBar.setMaximum(fieldIDs.length);
      AbstractAdjustmentListener scrollListener = event -> update(event.Value, container, fieldIDs,
          mapIdTwoNewValue);
      scrollBar.addAdjustmentListener(scrollListener);

      for (int i = 0; i < 10; i++)
      {
        int index = i;
        XTextComponent field = UNO.XTextComponent(container.getControl("TextField" + index));
        field.addTextListener(new AbstractTextListener()
        {

          @Override
          public void textChanged(TextEvent event)
          {
            String labelText = UNO.XFixedText(container.getControl("Label" + index)).getText();
            Optional<ReferencedFieldID> optionalId = Arrays.stream(fieldIDs)
                .filter(id -> id.getFieldId().equals(labelText)).findFirst();
            optionalId.ifPresent(id -> mapIdTwoNewValue.put(id, field.getText()));
          }
        });
        UNO.XWindow(field).addFocusListener(new AbstractFocusListener()
        {
          @Override
          public void focusGained(com.sun.star.awt.FocusEvent event)
          {
            currentField[0] = field;
          }
        });
      }

      XButton submitXBtn = UNO.XButton(container.getControl("submit"));
      submitXBtn.setLabel(labelSubmitButton);
      submitXBtn.addActionListener(new AbstractActionListener()
      {

        @Override
        public void actionPerformed(com.sun.star.awt.ActionEvent arg0)
        {
          final HashMap<String, FieldSubstitution> result = new HashMap<>();

          for (Map.Entry<ReferencedFieldID, String> entry : mapIdTwoNewValue.entrySet())
          {
            if (!isContentValid(entry.getValue(),
                entry.getKey().isTransformed() && !ignoreIsTransformed))
            {
              continue;
            }
            FieldSubstitution subst = new TextDocumentModel.FieldSubstitution();
            for (ContentElement ce : getContent(entry.getValue()))
            {
              if (ce.isTag())
                subst.addField(ce.toString());
              else
                subst.addFixedText(ce.toString());
            }
            result.put(entry.getKey().getFieldId(), subst);
          }

          dialog.endExecute();
          if (submitActionListener != null)
            submitActionListener
                .actionPerformed(new ActionEvent(result, 0, "showSubstitutionDialogReturned"));
        }
      });

      XButton abortXBtn = UNO.XButton(container.getControl("abort"));
      abortXBtn.addActionListener(new AbstractActionListener()
      {
        @Override
        public void actionPerformed(com.sun.star.awt.ActionEvent event)
        {
          dialog.endExecute();
        }
      });

      update(0, container, fieldIDs, mapIdTwoNewValue);
      dialog.execute();
    } catch (Exception e)
    {
      LOGGER.error("Tabellenspalten-Bearbeiten-Dialog konnte nicht angezeigt werden.", e);
    }
  }

  /**
   * Aktualisiert die Steuerelemente für die Feldanpassungen an Hand der Scrollbar.
   *
   * @param value
   *          Der Wert der Scrollbar.
   * @param container
   *          Der ControlContainer mit allen Steuerelementen.
   * @param fieldIds
   *          Die field-IDs der alten, bereits im Dokument enthaltenen Felder, die in der gegebenen
   *          Reihenfolge angezeigt werden, Dupletten werden aber entfernt.
   * @param mapIdTwoNewValue
   *          Mapping von FieldIds auf deren neuen Wert.
   */
  private static void update(int value, XControlContainer container, ReferencedFieldID[] fieldIDs,
      Map<ReferencedFieldID, String> mapIdTwoNewValue)
  {
    for (int i = 0; i < 10; i++)
    {
      XFixedText label = UNO.XFixedText(container.getControl("Label" + i));
      XTextComponent field = UNO.XTextComponent(container.getControl("TextField" + i));
      // Nicht benötigte Textfelder ausblenden
      if (i >= fieldIDs.length)
      {
        UNO.XWindow(field).setVisible(false);
      } else
      {
        int index = value + i;
        String id = fieldIDs[index].getFieldId();
        label.setText(id);
        field.setText(mapIdTwoNewValue.getOrDefault(fieldIDs[index], ""));
      }
    }
  }

  private static boolean isContentValid(String text, boolean isTransformed)
  {
    if (!isTransformed)
    {
      return true;
    }
    List<ContentElement> c = getContent(text);
    if (c.isEmpty())
    {
      return true;
    }
    return c.size() == 1 && c.get(0).isTag();
  }

  /**
   * Liefert eine Liste von {@link ContentElement}-Objekten, die den aktuellen Inhalt der
   * JTextComponent repräsentiert und dabei enthaltenen Text und evtl. enthaltene Tags als eigene
   * Objekte kapselt.
   */
  private static List<ContentElement> getContent(String text)
  {
    List<ContentElement> list = new ArrayList<>();
    Matcher m = TAG_PATTERN.matcher(text);
    int lastEndPos = 0;
    int startPos = 0;
    while (m.find())
    {
      startPos = m.start();
      String tag = m.group(2);
      list.add(new ContentElement(text.substring(lastEndPos, startPos), false));
      if (tag.length() > 0)
      {
        list.add(new ContentElement(tag, true));
      }
      lastEndPos = m.end();
    }
    String t = text.substring(lastEndPos);
    if (t.length() > 0)
    {
      list.add(new ContentElement(t, false));
    }
    return list;
  }

  /**
   * Beschreibt ein Element des Inhalts dieser JTextComponent und kann entweder ein eingefügtes Tag
   * oder ein normaler String sein. Auskunft über den Typ des Elements erteilt die Methode isTag(),
   * auf den String-Wert kann über die toString()-Methode zugegriffen werden.
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
     * Liefert true, wenn dieses Element ein Tag ist oder false, wenn es sich um normalen Text
     * handelt.
     */
    public boolean isTag()
    {
      return isTag;
    }
  }

}
