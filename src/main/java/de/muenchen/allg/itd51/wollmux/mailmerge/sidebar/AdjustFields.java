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
package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
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
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractAdjustmentListener;
import de.muenchen.allg.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.ReferencedFieldID;
import de.muenchen.allg.itd51.wollmux.mailmerge.FieldSubstitution;

/**
 * Dialogs for manipulating the mapping of fields to data source columns.
 */
public class AdjustFields
{

  private static final Logger LOGGER = LoggerFactory.getLogger(AdjustFields.class);

  /**
   * Suffix of a tag.
   */
  private static final String TAG_PREFIX = "" + Character.toChars(0x200B)[0] + "<";

  /**
   * Suffix of a tag.
   */
  private static final String TAG_SUFFIX = "" + Character.toChars(0x200B)[0] + ">";

  /**
   * Regular expression for searching tags. The first group contains the text of the tag.
   */
  private static final Pattern TAG_PATTERN = Pattern
      .compile("(" + TAG_PREFIX + "(.*?)" + TAG_SUFFIX + ")");

  private AdjustFields()
  {
    // nothing to initialize
  }

  /**
   * Show a dialog to override existing fields with free text or other fields.
   * 
   * @param title
   *          The title of the dialog.
   * @param labelOldFields
   *          The name of the column with old fields.
   * @param labelNewFields
   *          The name of the column with new fields.
   * @param labelSubmitButton
   *          The text of the submit button.
   * @param fieldIDs
   *          The ids of the existing fields. They are presented in the given order, but duplicates
   *          are ignored.
   * @param fieldNames
   *          The new fields, which can be used.
   * @param ignoreIsTransformed
   *          If false, fields with {@link ReferencedFieldID#isTransformed()}==true are treated
   *          special. Than only 1-to-1 substitutions are allowed.
   * @param submitActionListener
   *          The listener, which is called as soon as the dialog is closed. As argument it gets a
   *          {@link Map} of {@link String}s to {@link FieldSubstitution}.
   */
  @SuppressWarnings("java:S107")
  public static void showFieldMappingDialog(String title, String labelOldFields,
      String labelNewFields, String labelSubmitButton, ReferencedFieldID[] fieldIDs,
      final List<String> fieldNames, boolean ignoreIsTransformed,
      final ActionListener submitActionListener)
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.edit_table_columns?location=application", "", peer, null);
      XControlContainer container = UNO.XControlContainer(window);
      XDialog dialog = UNO.XDialog(window);
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
            Arrays.stream(fieldIDs).filter(id -> id.getFieldId().equals(labelText)).findFirst()
                .ifPresent(id -> mapIdTwoNewValue.put(id, field.getText()));
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
          submit(submitActionListener, ignoreIsTransformed, dialog, mapIdTwoNewValue);
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
   * Collect the mappings and call the listener.
   * 
   * @param submitActionListener
   *          The listener.
   * @param ignoreIsTransformed
   * @param dialog
   *          The dialog.
   * @param mapIdTwoNewValue
   *          The mapping of ids to new values.
   */
  private static void submit(final ActionListener submitActionListener, boolean ignoreIsTransformed,
      XDialog dialog, Map<ReferencedFieldID, String> mapIdTwoNewValue)
  {
    final HashMap<String, FieldSubstitution> result = new HashMap<>();

    for (Map.Entry<ReferencedFieldID, String> entry : mapIdTwoNewValue.entrySet())
    {
      if (!isContentValid(entry.getValue(), entry.getKey().isTransformed() && !ignoreIsTransformed))
      {
        continue;
      }
      FieldSubstitution subst = new FieldSubstitution();
      for (Pair<String, Boolean> ce : getContent(entry.getValue()))
      {
        if (ce.getValue())
        {
          subst.addField(ce.getKey());
        } else
        {
          subst.addFixedText(ce.getKey());
        }
      }
      result.put(entry.getKey().getFieldId(), subst);
    }

    dialog.endExecute();
    if (submitActionListener != null)
    {
      submitActionListener
          .actionPerformed(new ActionEvent(result, 0, "showSubstitutionDialogReturned"));
    }
  }

  /**
   * Update the controls according to the scroll bar.
   *
   * @param value
   *          The value of the scroll bar.
   * @param container
   *          The control container.
   * @param fieldIds
   *          The ids of the old (already in the document) fields, which are presented in the order
   *          of the list. Duplicates are ignored.
   * @param mapIdTwoNewValue
   *          Mapping of ids to new values.
   */
  private static void update(int value, XControlContainer container, ReferencedFieldID[] fieldIDs,
      Map<ReferencedFieldID, String> mapIdTwoNewValue)
  {
    for (int i = 0; i < 10; i++)
    {
      XFixedText label = UNO.XFixedText(container.getControl("Label" + i));
      XTextComponent field = UNO.XTextComponent(container.getControl("TextField" + i));
      // hide unused text fields
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

  /**
   * Check whether a string is valid.
   * 
   * @param text
   *          The string.
   * @param isTransformed
   *          If true only a 1-to-1 substitution is allowed.
   * @return True if text consists of one part, which is a tag, false otherwise.
   */
  private static boolean isContentValid(String text, boolean isTransformed)
  {
    if (!isTransformed)
    {
      return true;
    }
    List<Pair<String, Boolean>> c = getContent(text);
    if (c.isEmpty())
    {
      return true;
    }
    return c.size() == 1 && c.get(0).getValue();
  }

  /**
   * Divide the text in several parts by tags. Each tag is also a part.
   * 
   * @param text
   *          The text.
   * @return A list of parts.
   */
  private static List<Pair<String, Boolean>> getContent(String text)
  {
    List<Pair<String, Boolean>> list = new ArrayList<>();
    Matcher m = TAG_PATTERN.matcher(text);
    int lastEndPos = 0;
    int startPos = 0;
    while (m.find())
    {
      startPos = m.start();
      String tag = m.group(2);
      list.add(Pair.of(text.substring(lastEndPos, startPos), false));
      if (tag.length() > 0)
      {
        list.add(Pair.of(tag, true));
      }
      lastEndPos = m.end();
    }
    String t = text.substring(lastEndPos);
    if (t.length() > 0)
    {
      list.add(Pair.of(t, false));
    }
    return list;
  }
}
