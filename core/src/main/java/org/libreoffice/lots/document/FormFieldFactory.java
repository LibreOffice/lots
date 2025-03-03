/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.document;

import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XControlModel;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XControlShape;
import com.sun.star.frame.XController;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoCollection;
import org.libreoffice.ext.unohelper.common.UnoIterator;
import org.libreoffice.ext.unohelper.document.text.Bookmark;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.ext.unohelper.util.UnoService;
import org.libreoffice.lots.document.commands.DocumentCommand.InsertFormValue;
import org.libreoffice.lots.util.Utils;

/**
 * Factory for creating form fields from book marks with name WM(CMD 'insertFormValue' ...).
 */
public final class FormFieldFactory
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormFieldFactory.class);

  /**
   * Create a form field in the document at InsertFormValue-command. If there's already a form
   * element (textfield, dropdown or checkbox), this element is used, otherwise a
   * DynamicInputFormField is created. This element is replaced with an InputField as soon as it has
   * to be written.
   *
   * @param doc
   *          The document.
   * @param cmd
   *          The InsertFormValue-command.
   * @param bookmarkNameToFormField
   *          Mapping from book marks to form fields.
   * @return A form field.
   */
  public static FormField createFormField(XTextDocument doc, InsertFormValue cmd,
      Map<String, FormField> bookmarkNameToFormField)
  {
    String bookmarkName = cmd.getBookmarkName();
    FormField formField = bookmarkNameToFormField.get(bookmarkName);
    if (formField != null)
    {
      return formField;
    }

    XTextRange range = cmd.getTextCursor();

    if (range != null)
    {
      handleParagraphEnumeration(UnoCollection.getCollection(range, XTextRange.class), doc, bookmarkNameToFormField,
          cmd);
    }

    return bookmarkNameToFormField.get(bookmarkName);
  }

  /**
   * Create a form field in a document for a mail merge field.
   *
   * @param doc
   *          The document.
   * @param textfield
   *          The mail merge field.
   * @return field A form field.
   */
  public static FormField createDatabaseFormField(XTextDocument doc, XTextField textfield)
  {
    return new DatabaseFormField(doc, textfield);
  }

  /**
   * Create a form field in the document using a user variable.
   *
   * @param doc
   *          The document.
   * @param textfield
   *          The text field.
   * @param propSet
   *          The TextFieldMaster of the text field. The property {@code textfield.TextFieldMaster}
   *          can't be used because it can't be modified. The TextFieldMaster has to be taken form
   *          {@code doc.getTextFieldMasters()}. The text field and its master belong together if
   *          {@code textfield.Content.equals(master.Name)} is true.
   * @return A form field.
   */
  public static FormField createInputUserFormField(XTextDocument doc, XTextField textfield, XPropertySet propSet)
  {
    return new InputUserFormField(doc, textfield, propSet);
  }

  /**
   * Iterate through all paragraphs (no tables) of a document and create form fields for a
   * InsertFormValue-command if necessary.
   *
   * @param paragraph
   *          The paragraphs.
   * @param doc
   *          The document.
   * @param mapBookmarkNameToFormField
   *          Collection of InsertFormValue-commands.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleParagraphEnumeration(UnoCollection<XTextRange> paragraph, XTextDocument doc,
      Map<String, FormField> mapBookmarkNameToFormField, InsertFormValue cmd)
  {
    for (XTextRange para : paragraph)
    {
      UnoCollection<XTextRange> portions = UnoCollection.getCollection(para, XTextRange.class);
      if (portions != null) // seems to be a text paragraph
      {
        handleParagraph(portions, doc, mapBookmarkNameToFormField, cmd);
      }
    }
  }

  /**
   * Iterate through all text portions of a paragraph and create form fields for a
   * InsertFormValue-command if necessary.
   *
   * @param textportions
   *          The text portion.
   * @param doc
   *          The document.
   * @param mapBookmarkNameToFormField
   *          Collection of InsertFormValue-commands.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleParagraph(UnoCollection<XTextRange> textportions, XTextDocument doc,
      Map<String, FormField> mapBookmarkNameToFormField, InsertFormValue cmd)
  {
    boolean foundFormField = false;

    for (XTextRange textPortion : textportions)
    {
      String textPortionType = (String) Utils.getProperty(textPortion, UnoProperty.TEXT_PROTION_TYPE);
      if (textPortionType == null)
      {
        continue;
      }
      if ("Bookmark".equals(textPortionType))
      {
        foundFormField = handleBookmark(doc, mapBookmarkNameToFormField, textPortion, cmd, foundFormField);
      } else if ("TextField".equals(textPortionType))
      {
        handleTextField(doc, mapBookmarkNameToFormField, textPortion, cmd);
        foundFormField = false;
      } else if ("Frame".equals(textPortionType))
      {
        handleFrame(doc, mapBookmarkNameToFormField, textPortion, cmd);
        foundFormField = false;
      }
    }

    if (foundFormField)
    {
      handleNewInputField(mapBookmarkNameToFormField, doc, cmd);
    }

  }

  /**
   * Create a form field in a document for a book mark if necessary.
   *
   * @param doc
   *          The document.
   * @param mapBookmarkNameToFormField
   *          Mapping from book mark to form field. The new form field is added.
   * @param textPortion
   *          The text portion of the book mark.
   * @param cmd
   *          The InsertFormValue-command.
   * @param startedBookmark
   *          Is there a book mark that has been started.
   * @return True if a position for a form field was found.
   */
  private static boolean handleBookmark(XTextDocument doc,
      Map<String, FormField> mapBookmarkNameToFormField,
      XTextRange textPortion, InsertFormValue cmd, boolean startedBookmark)
  {
    XNamed bookmark = null;
    boolean isStart = false;
    boolean isCollapsed = false;
    try
    {
      isStart = ((Boolean) UnoProperty.getProperty(textPortion, UnoProperty.IS_START)).booleanValue();
      isCollapsed = ((Boolean) UnoProperty.getProperty(textPortion, UnoProperty.IS_COLLAPSED)).booleanValue();
      if (isCollapsed)
      {
        isStart = true;
      }
      bookmark = UNO.XNamed(UnoProperty.getProperty(textPortion, UnoProperty.BOOKMARK));
    } catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
      return startedBookmark;
    }
    if (bookmark == null)
    {
      return startedBookmark;
    }

    String name = bookmark.getName();
    if (!name.equals(cmd.getBookmarkName()))
    {
      return startedBookmark;
    }

    if (isStart)
    {
      LOGGER.debug("Found Bookmark-Start for {}", name);
      startedBookmark = true;
    }
    if (!isStart || isCollapsed)
    {
      LOGGER.debug("Found Bookmark-End or Collapsed-Bookmark for {}", name);
      if (startedBookmark)
      {
        handleNewInputField(mapBookmarkNameToFormField, doc, cmd);
        return false;
      }
    }
    return startedBookmark;
  }

  /**
   * Create a form field in a document for a frame if necessary.
   *
   * @param doc
   *          The document.
   * @param mapBookmarkNameToFormField
   *          Mapping from book mark to form field. The new form field is added.
   * @param textPortion
   *          The text portion of the frame.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleFrame(XTextDocument doc, Map<String, FormField> mapBookmarkNameToFormField,
      XTextRange textPortion, InsertFormValue cmd)
  {
    XControlModel model = null;
    try
    {
      UnoIterator<XControlShape> contentIter = UnoIterator.create(
          UNO.XContentEnumerationAccess(textPortion).createContentEnumeration("com.sun.star.text.TextPortion"),
          XControlShape.class);
      while (contentIter.hasNext())
      {
        XControlModel tempModel = contentIter.next().getControl();
        if (UnoService.supportsService(tempModel, UnoService.CSS_FORM_COMPONENT_CHECK_BOX))
        {
          model = tempModel;
        }
      }
    } catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
      return;
    }

    handleCheckbox(model, mapBookmarkNameToFormField, doc, cmd);
  }

  /**
   * Create a form field in a document for a text field if necessary.
   *
   * @param doc
   *          The document.
   * @param mapBookmarkNameToFormField
   *          Mapping from book mark to form field. The new form field is added.
   * @param textPortion
   *          The text portion of the text field.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleTextField(XTextDocument doc, Map<String, FormField> mapBookmarkNameToFormField,
      XTextRange textPortion, InsertFormValue cmd)
  {
    try
    {
      XDependentTextField textField = UNO
          .XDependentTextField(UnoProperty.getProperty(textPortion, UnoProperty.TEXT_FIELD));
      if (UnoService.supportsService(textField, UnoService.CSS_TEXT_TEXT_FIELD_DROP_DOWN))
      {
        handleDropdown(textField, mapBookmarkNameToFormField, doc, cmd);
      } else if (UnoService.supportsService(textField, UnoService.CSS_TEXT_TEXT_FIELD_INPUT))
      {
        handleInputField(textField, mapBookmarkNameToFormField, doc, cmd);
      } else
      {
        return;
      }
    } catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
      return;
    }
  }

  /**
   * Create a new form field in a document in a book mark.
   *
   * @param bookmarkName
   *          The name of the book mark.
   * @param mapBookmarkNameToFormField
   *          Mapping from book mark to form field. The new form field is added.
   * @param doc
   *          The document.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleNewInputField(Map<String, FormField> mapBookmarkNameToFormField, XTextDocument doc,
      InsertFormValue cmd)
  {
    FormField formField = new DynamicInputFormField(doc, cmd);
    mapBookmarkNameToFormField.put(cmd.getBookmarkName(), formField);
  }

  /**
   * Handle a form field in a document in a book mark.
   *
   * @param textfield
   *          The existing form field.
   * @param mapBookmarkNameToFormField
   *          Mapping from book mark to form field. The new form field is added.
   * @param doc
   *          The document.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleInputField(XDependentTextField textfield, Map<String, FormField> mapBookmarkNameToFormField,
      XTextDocument doc, InsertFormValue cmd)
  {
    if (textfield != null)
    {
      FormField formField = new InputFormField(doc, cmd, textfield);
      mapBookmarkNameToFormField.put(cmd.getBookmarkName(), formField);
    }
  }

  /**
   * Handle a form field drop down in a document in a book mark.
   *
   * @param textfield
   *          The existing form field drop down.
   * @param mapBookmarkNameToFormField
   *          Mapping from book mark to form field. The new form field is added.
   * @param doc
   *          The document.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleDropdown(XDependentTextField textfield, Map<String, FormField> mapBookmarkNameToFormField,
      XTextDocument doc, InsertFormValue cmd)
  {
    if (textfield != null)
    {
      FormField formField = new DropDownFormField(doc, cmd, textfield);
      mapBookmarkNameToFormField.put(cmd.getBookmarkName(), formField);
    }
  }

  /**
   * Handle a form field check box in a document in a book mark.
   *
   * @param textfield
   *          The existing form field check box.
   * @param mapBookmarkNameToFormField
   *          Mapping from book mark to form field. The new form field is added.
   * @param doc
   *          The document.
   * @param cmd
   *          The InsertFormValue-command.
   */
  private static void handleCheckbox(XControlModel checkbox, Map<String, FormField> mapBookmarkNameToFormField,
      XTextDocument doc, InsertFormValue cmd)
  {
    if (checkbox != null)
    {
      FormField formField = new CheckboxFormField(doc, cmd, checkbox);
      mapBookmarkNameToFormField.put(cmd.getBookmarkName(), formField);
    }
  }


  /**
   * A form field describes the properties of a form element of an InsertFromValue-command.
   */
  public interface FormField extends Comparable<FormField>
  {
    /**
     * Replace the ID of the InsertFormValue-command.
     *
     * @param oldFieldId
     *          The old ID.
     * @param newFieldId
     *          The new ID.
     * @return True if the field supports a 1-by-1 substitution of the ID, false otherwise. If false
     *         the ID hasn't been replaced.
     */
    public boolean substituteFieldID(String oldFieldId, String newFieldId);

    /**
     * Get the text range of the form field.
     *
     * @return The text range.
     */
    public XTextRange getAnchor();

    /**
     * Get the Id of an InsertFormValue-command.
     *
     * @return id The ID or null.
     */
    public String getId();

    /**
     * Get the name of the TRAFO function.
     *
     * @return The name of the TRAFO or null if there's no TRAFO.
     */
    public String getTrafoName();

    /**
     * Does the TRAFO function only use the value of the form field?
     *
     * @return True if only the form value can be used by the TRAFO, false otherwise.
     */
    public boolean singleParameterTrafo();

    /**
     * Set a new value in the form element of the document.
     *
     * @param value
     *          The new value.
     */
    public void setValue(String value);

    /**
     * Get the value of the form element of the document.
     *
     * @return The value of the form element or the empty string.
     */
    public String getValue();

    /**
     * Set the cursor to the position of the form element.
     */
    public void focus();

    /**
     * Delete the form element from the document.
     */
    public void dispose();

    /**
     * Get the type of the form element.
     *
     * @return The type.
     */
    public FormFieldType getType();
  }

  /**
   * Possible form element types.
   */
  public enum FormFieldType
  {
    /**
     * A text input field.
     */
    INPUT_FORM_FIELD,
    /**
     * A field which can be replaced by every other form field.
     */
    DYNAMIC_INPUT_FORM_FIELD,
    /**
     * A drop down.
     */
    DROPDOWN_FORM_FIELD,
    /**
     * A check box.
     */
    CHECKBOX_FORM_FIELD,
    /**
     * A mail merge field.
     */
    DATABASE_FORM_FIELD,
    /**
     * A text form field.
     */
    INPUT_USER_FORM_FIELD;
  }

  /**
   * Default implementation of form fields.
   */
  private abstract static class BasicFormField implements FormField
  {
    /**
     * The containing document.
     */
    protected XTextDocument doc;

    /**
     * The InsertFormValue-command.
     */
    protected InsertFormValue cmd;

    /**
     * Create a new form field in a document at the position of the InsertFormValue-command.
     *
     * @param doc
     *          The document.
     * @param cmd
     *          The InsertFormValue-command.
     */
    public BasicFormField(XTextDocument doc, InsertFormValue cmd)
    {
      this.doc = doc;
      this.cmd = cmd;
    }

    @Override
    public String getId()
    {
      if (cmd != null)
      {
        return cmd.getID();
      }
      return null;
    }

    @Override
    public String getTrafoName()
    {
      return cmd.getTrafoName();
    }

    @Override
    public void focus()
    {
      if (cmd == null)
      {
        return;
      }
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = cmd.getTextCursor();
        if (focusRange != null)
        {
          cursor.gotoRange(focusRange, false);
        }
      } catch (java.lang.Exception e)
      {
        LOGGER.trace("", e);
      }
    }

    /**
     * Compare the position of the document commands.
     *
     * @param other
     *          The other form field.
     * @return -1 if this is before other or they are in different text objects. 1 if this is after
     *         other. 0 if they overlap.
     */
    @Override
    public int compareTo(FormField other)
    {
      BasicFormField other2;
      try
      {
        other2 = (BasicFormField) other;
      } catch (Exception x)
      {
        LOGGER.trace("", x);
        return -1;
      }

      TreeRelation rel = new TreeRelation(cmd.getAnchor(), other2.cmd.getAnchor());
      if (rel.isAGreaterThanB())
        return 1;
      else if (rel.isALessThanB())
        return -1;
      else if (rel.isAEqualB())
      {
        return 0;
      }

      return -1;
    }

    @Override
    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      if (oldFieldId == null || newFieldId == null)
      {
        return false;
      }
      if (cmd.getID().equals(oldFieldId))
      {
        cmd.setID(newFieldId);
        return true;
      }
      return false;
    }

    @Override
    public XTextRange getAnchor()
    {
      return cmd.getAnchor();
    }

    @Override
    public void dispose()
    {
      cmd.markDone(true);
    }
  }

  /**
   * Puts the form value in an Input-Field.
   */
  private static class InputFormField extends BasicFormField
  {
    protected XTextField inputField;

    public InputFormField(XTextDocument doc, InsertFormValue cmd, XTextField inputField)
    {
      super(doc, cmd);
      this.inputField = inputField;
    }

    @Override
    public void setValue(String value)
    {
      if (inputField != null && doc != null)
      {
        Utils.setProperty(inputField, UnoProperty.CONTENT, value);
      }
    }

    @Override
    public String getValue()
    {
      if (inputField != null)
      {
        Object content = Utils.getProperty(inputField, UnoProperty.CONTENT);
        if (content != null)
        {
          return content.toString();
        }
      }
      return "";
    }

    @Override
    public boolean singleParameterTrafo()
    {
      return true;
    }

    @Override
    public FormFieldType getType()
    {
      return FormFieldType.INPUT_FORM_FIELD;
    }

    @Override
    public void dispose()
    {
      super.dispose();
      try
      {
        XTextContent xTextContent = UNO.XTextContent(inputField);
        xTextContent.getAnchor().getText().removeTextContent(xTextContent);
      } catch (NoSuchElementException e)
      {
        LOGGER.info("", e);
      }
    }
  }

  /**
   * A form field, which has no element at the beginning. An InputField is created as soon as the
   * field is focused or a value set.
   */
  private static class DynamicInputFormField extends InputFormField
  {

    public DynamicInputFormField(XTextDocument doc, InsertFormValue cmd)
    {
      super(doc, cmd, null);
      XTextCursor cursor = cmd.getTextCursor();

      if (cursor != null)
      {
        try
        {
          Bookmark bookmark = new Bookmark(cmd.getBookmarkName(), UNO.XBookmarksSupplier(doc));
          String textSurroundedByBookmark = cursor.getString();

          String trimmedText = textSurroundedByBookmark.trim();
          Pattern p = Pattern.compile("\\A[<\\[{].*[\\]>}]\\z");

          if (textSurroundedByBookmark.length() > 0 && !p.matcher(trimmedText).matches())
          {
            LOGGER.info("Kollabiere Textmarke \"{}\", die um den Text \"{}\" herum liegt.", cmd.getBookmarkName(),
                textSurroundedByBookmark);

            bookmark.collapseBookmark();
          }
        } catch (Exception x)
        {
          LOGGER.trace("", x);
        }
      }
    }

    @Override
    public void setValue(String value)
    {
      if (cmd == null)
      {
        return;
      }

      if (value.isEmpty())
      {
        if (inputField == null)
        {
          cmd.setTextRangeString("");
        }
      } else
      {
        if (inputField == null)
        {
          createInputField();
        }
      }
      super.setValue(value);
    }

    private void createInputField()
    {
      if (cmd == null)
      {
        return;
      }

      String bookmarkName = cmd.getBookmarkName();

      LOGGER.trace("Erzeuge neues Input-Field für Bookmark \"{}\"", bookmarkName);
      try
      {
        XTextField field = UNO.XTextField(UnoService.createService(UnoService.CSS_TEXT_TEXT_FIELD_INPUT, doc));

        if (field != null)
        {
          cmd.insertTextContentIntoBookmark(field, true);
          inputField = field;
        }
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
      }
    }

    @Override
    public FormFieldType getType()
    {
      return FormFieldType.DYNAMIC_INPUT_FORM_FIELD;
    }
  }

  /**
   * Puts the form value in a Drop-Down-Field.
   */
  private static class DropDownFormField extends BasicFormField
  {
    private XTextField dropdownField;

    private String[] origItemList = null;

    public DropDownFormField(XTextDocument doc, InsertFormValue cmd, XTextField dropdownField)
    {
      super(doc, cmd);
      this.dropdownField = dropdownField;

      if (dropdownField != null)
        origItemList = (String[]) Utils.getProperty(dropdownField, UnoProperty.ITEMS);

    }

    @Override
    public void setValue(String value)
    {
      // ISSUE: empty strings are permitted as drop down values.
      if (value.isEmpty())
      {
        value = " ";
      }

      if (dropdownField != null && doc != null)
      {
        extendItemsList(value);
        Utils.setProperty(dropdownField, UnoProperty.SELECTED_ITEM, value);
      }
    }

    /**
     * Add a new item if it isn't already there.
     *
     * @param value
     *          The new item.
     */
    private void extendItemsList(String value)
    {
      if (origItemList != null)
      {
        boolean found = false;
        for (int i = 0; i < origItemList.length; i++)
        {
          if (value.equals(origItemList[i]))
          {
            found = true;
            break;
          }
        }

        if (!found)
        {
          String[] extendedItems = new String[origItemList.length + 1];
          for (int i = 0; i < origItemList.length; i++)
          {
            extendedItems[i] = origItemList[i];
          }
          extendedItems[origItemList.length] = value;
          Utils.setProperty(dropdownField, UnoProperty.ITEMS, extendedItems);
        }
      }
    }

    @Override
    public String getValue()
    {
      if (dropdownField != null)
      {
        Object content = Utils.getProperty(dropdownField, UnoProperty.SELECTED_ITEM);
        if (content != null)
        {
          return content.toString();
        }
      }
      return "";
    }

    @Override
    public boolean singleParameterTrafo()
    {
      return true;
    }

    @Override
    public FormFieldType getType()
    {
      return FormFieldType.DROPDOWN_FORM_FIELD;
    }
  }

  /**
   * Puts the form value in a Check-Box-Field.
   */
  private static class CheckboxFormField extends BasicFormField
  {
    private Object checkbox;

    public CheckboxFormField(XTextDocument doc, InsertFormValue cmd, Object checkbox)
    {
      super(doc, cmd);

      this.checkbox = checkbox;
    }

    @Override
    public void setValue(String value)
    {
      Utils.setProperty(checkbox, UnoProperty.STATE,
          Boolean.parseBoolean(value) ? Short.valueOf((short) 1) : Short.valueOf((short) 0));
    }

    @Override
    public String getValue()
    {
      Object state = Utils.getProperty(checkbox, UnoProperty.STATE);
      if (state != null && state.equals(Short.valueOf((short) 1)))
      {
        return "true";
      }
      else
      {
        return "false";
      }
    }

    @Override
    public boolean singleParameterTrafo()
    {
      return true;
    }

    @Override
    public FormFieldType getType()
    {
      return FormFieldType.CHECKBOX_FORM_FIELD;
    }
  }

  /**
   * Puts the form value in a mail merge field. Mail merge fields doesn't support TRAFOs.
   */
  private static class DatabaseFormField implements FormField
  {
    private XTextField textfield;

    private XTextDocument doc;

    public DatabaseFormField(XTextDocument doc, XTextField textfield)
    {
      this.textfield = textfield;
      this.doc = doc;
    }

    @Override
    public String getTrafoName()
    {
      return null;
    }

    @Override
    public void setValue(String value)
    {
      if (value == null)
      {
        return;
      }
      Utils.setProperty(textfield, UnoProperty.CONTENT, value);
      Utils.setProperty(textfield, UnoProperty.CURRENT_PRESENTAITON, value);
    }

    @Override
    public String getValue()
    {
      String cont = (String) Utils.getProperty(textfield, UnoProperty.CONTENT);
      if (cont == null)
        cont = (String) Utils.getProperty(textfield, UnoProperty.CURRENT_PRESENTAITON);
      if (cont != null)
      {
        return cont;
      }
      return "";
    }

    @Override
    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = UNO.XTextContent(textfield).getAnchor();
        if (focusRange != null)
        {
          cursor.gotoRange(focusRange, false);
        }
      } catch (java.lang.Exception e)
      {
        LOGGER.trace("", e);
      }
    }

    @Override
    public int hashCode()
    {
      return UnoRuntime.generateOid(UNO.XInterface(textfield)).hashCode();
    }

    @Override
    public boolean equals(Object b)
    {
      return UnoRuntime.areSame(UNO.XInterface(textfield), UNO.XInterface(b));
    }

    @Override
    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      return false;
    }

    @Override
    public XTextRange getAnchor()
    {
      return textfield.getAnchor();
    }

    @Override
    public void dispose()
    {
      if (textfield != null)
      {
        textfield.dispose();
      }
    }

    @Override
    public int compareTo(FormField o)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean singleParameterTrafo()
    {
      return false;
    }

    @Override
    public FormFieldType getType()
    {
      return FormFieldType.DATABASE_FORM_FIELD;
    }

    @Override
    public String getId()
    {
      return null;
    }
  }

  /**
   * Puts the user variable in an Input-Field.
   */
  private static class InputUserFormField implements FormField
  {
    private XTextDocument doc;

    private XTextField textfield;

    private XPropertySet propSet;

    public InputUserFormField(XTextDocument doc, XTextField textfield, XPropertySet propSet)
    {
      this.doc = doc;
      this.textfield = textfield;
      this.propSet = propSet;
    }

    @Override
    public void setValue(final String value)
    {
      if (value != null && textfield != null && doc != null)
      {
        Utils.setProperty(propSet, UnoProperty.CONTENT, value);
      }
    }

    @Override
    public String getTrafoName()
    {
      return TextDocumentModel.getFunctionNameForUserFieldName("" + Utils.getProperty(textfield, UnoProperty.CONTENT));
    }

    @Override
    public String getValue()
    {
      if (propSet == null)
      {
        return "";
      }
      return "" + Utils.getProperty(propSet, UnoProperty.CONTENT);
    }

    @Override
    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = UNO.XTextContent(textfield).getAnchor();
        if (focusRange != null)
        {
          cursor.gotoRange(focusRange, false);
        }
      } catch (java.lang.Exception e)
      {
        LOGGER.trace("", e);
      }
    }

    @Override
    public int hashCode()
    {
      return UnoRuntime.generateOid(UNO.XInterface(textfield)).hashCode();
    }

    @Override
    public boolean equals(Object b)
    {
      return UnoRuntime.areSame(UNO.XInterface(textfield), UNO.XInterface(b));
    }

    @Override
    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      return false;
    }

    @Override
    public XTextRange getAnchor()
    {
      return textfield.getAnchor();
    }

    @Override
    public void dispose()
    {
      if (textfield != null)
      {
        textfield.dispose();
      }
    }

    @Override
    public int compareTo(FormField o)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean singleParameterTrafo()
    {
      return false;
    }

    @Override
    public FormFieldType getType()
    {
      return FormFieldType.INPUT_USER_FORM_FIELD;
    }

    @Override
    public String getId()
    {
      return null;
    }
  }
}
