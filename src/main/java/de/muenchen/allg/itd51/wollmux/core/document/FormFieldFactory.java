/*
 * Dateiname: FormFieldFactory.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert eine Fabrik, die an der Stelle von
 *            WM('insertFormValue'...)-Bookmark entsprechende FormField-Elemente
 *            erzeugt.
 *
 * Copyright (c) 2011-2015 Landeshauptstadt München
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
 * 08.06.2006 | LUT | Erstellung als FormField
 * 14.06.2006 | LUT | Umbenennung in FormFieldFactory und Unterstützung
 *                    von Checkboxen.
 * 07.09.2006 | BNK | Rewrite
 * 12.09.2006 | BNK | Bugfix: Bookmarks ohne Ausdehnung wurden nicht gefunden.
 * 03.01.2007 | BNK | +TextFieldFormField
 *                  | +createFormField(doc, textfield)
 * 08.07.2009 | BED | Änderungen im Rahmen von R48539 (#2867)
 * 12.05.2011 | BED | Refresh von Textfeldern mit XRefreshable.refresh() [#6840]
 * 17.06.2011 | BED | Nachbesserungen für [#6840], überall XRefreshable.refresh()
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.core.document;

import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XControlModel;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XControlShape;
import com.sun.star.frame.XController;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

/**
 * Repräsentiert eine Fabrik, die an der Stelle von WM('insertFormValue'...)-Bookmark
 * entsprechende FormField-Elemente erzeugt.
 *
 * @author lut
 */
public final class FormFieldFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FormFieldFactory.class);

  public static final Pattern INSERTFORMVALUE =
    DocumentCommands.getPatternForCommand("insertFormValue");

  /**
   * Erzeugt ein Formularfeld im Dokument doc an der Stelle des
   * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein
   * Formularelement (derzeit TextFeld vom Typ Input, DropDown oder eine Checkbox)
   * vorhanden, so wird dieses Feld als Formularelement für die Darstellung des
   * Wertes des Formularfeldes genutzt. Ist innerhalb des Bookmarks noch kein
   * Formularelement vorhanden, so wird ein DynamicInputFormField an der Stelle des
   * Bookmarks erzeugt, das erst dann ein InputField-Textfeld im Dokument anlegt,
   * wenn auf das Textfeld schreibend zugegriffen wird.
   *
   * @param doc
   *          das Dokument, zu dem das Formularfeld gehört.
   * @param cmd
   *          das zugehörige insertFormValue-Kommando.
   */
  public static FormField createFormField(XTextDocument doc, InsertFormValue cmd,
      Map<String, FormField> bookmarkNameToFormField)
  {
    String bookmarkName = cmd.getBookmarkName();
    FormField formField = bookmarkNameToFormField.get(bookmarkName);
    if (formField != null) {
      return formField;
    }

    XTextRange range = cmd.getTextCursor();

    if (range != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(range).createEnumeration();
      handleParagraphEnumeration(xenum, doc, bookmarkNameToFormField, cmd.getBookmarkName());
    }

    return bookmarkNameToFormField.get(bookmarkName);
  }

  /**
   * Erzeugt ein neues FormField für das Serienbrieffeld textfield vom Typ
   * c,s,s,text,textfield,Database, das im Dokument doc liegt. Die Methoden
   * {@link Object#equals(java.lang.Object)} und {@link Object#hashCode()} beziehen
   * sich auf das zugrundeliegende UNO-Objekt, wobei verschiedene Proxies des selben
   * Objekts als gleich behandelt werden.
   *
   * @param doc
   *          das zugehörige Dokument doc
   * @param textfield
   *          ein Serienbrieffeld vom Typ css.text.textfield.Database.
   */
  public static FormField createDatabaseFormField(XTextDocument doc,
      XTextField textfield)
  {
    return new DatabaseFormField(doc, textfield);
  }

  /**
   * Erzeugt ein neues FormField für ein Eingabefeld einer Benutzervariablen vom Typ
   * c,s,s,text,textfield,InputUser und den zugehörigen TextFieldMaster master die im
   * Dokument doc liegen. Die Methoden {@link Object#equals(java.lang.Object)} und
   * {@link Object#hashCode()} beziehen sich auf das zugrundeliegende UNO-Objekt,
   * wobei verschiedene Proxies des selben Objekts als gleich behandelt werden.
   *
   * @param doc
   *          das zugehörige Dokument doc
   * @param textfield
   *          das InputUser-Objekt.
   * @param master
   *          bei InputUser-Objekten kann auf den angezeigten Wert nicht direkt
   *          zugegriffen werden. Diese Zugriffe erfolgen über einen TextFieldMaster,
   *          der dem InputUser-Objekt zugeordnet ist. VORSICHT: Das Objekt
   *          textfield.TextFieldMaster ist dabei nicht als Master geeignet, da
   *          dieser Master keine direkte Möglichkeit zum Setzen der Anzeigewerte
   *          anbietet. Das statt dessen geeignete TextFieldMaster-Objekt muss über
   *          doc.getTextFieldMasters() bezogen werden, wobei textfield und master
   *          dann zusammen gehören, wenn textfield.Content.equals(master.Name) gilt.
   * @return Das neue FormField.
   */
  public static FormField createInputUserFormField(XTextDocument doc,
      XTextField textfield, XPropertySet master)
  {
    return new InputUserFormField(doc, textfield, master);
  }

  /**
   * Geht die XEnumeration enu von Absätzen und TextTables durch und erzeugt für alle
   * in Absätzen (nicht TextTables) enthaltenen insertFormValue-Bookmarks
   * entsprechende Einträge in mapBookmarkNameToFormField. Falls nötig wird das
   * entsprechende FormField erzeugt.
   *
   * @param doc
   *          das Dokument in dem sich die enumierten Objekte befinden.
   */
  private static void handleParagraphEnumeration(XEnumeration enu,
      XTextDocument doc, Map<String, FormField> mapBookmarkNameToFormField, String bookmarkName)
  {
    XEnumerationAccess enuAccess;
    while (enu.hasMoreElements())
    {
      Object ele;
      try
      {
        ele = enu.nextElement();
      }
      catch (java.lang.Exception x)
      {
        LOGGER.trace("", x);
        continue;
      }
      enuAccess = UNO.XEnumerationAccess(ele);
      if (enuAccess != null) // ist wohl ein SwXParagraph
      {
        handleParagraph(enuAccess, doc, mapBookmarkNameToFormField, bookmarkName);
      }
    }
  }

  /**
   * Geht die XEnumeration enuAccess von TextPortions durch und erzeugt für alle
   * enthaltenen insertFormValue-Bookmarks entsprechende Einträge in
   * mapBookmarkNameToFormField. Falls nötig wird das entsprechende FormField
   * erzeugt.
   *
   * @param doc
   *          das Dokument in dem sich die enumierten Objekte befinden.
   */
  private static void handleParagraph(XEnumerationAccess paragraph,
      XTextDocument doc, Map<String, FormField> mapBookmarkNameToFormField, String bookmarkName)
  {
    /*
     * Der Name des zuletzt gestarteten insertFormValue-Bookmarks.
     */
    String lastInsertFormValueStart = null;

    /*
     * enumeriere alle TextPortions des Paragraphs
     */
    XEnumeration textPortionEnu = paragraph.createEnumeration();
    while (textPortionEnu.hasMoreElements())
    {
      /*
       * Diese etwas seltsame Konstruktion beugt Exceptions durch Bugs wie 68261 vor.
       */
      Object textPortion;
      try
      {
        textPortion = textPortionEnu.nextElement();
      }
      catch (java.lang.Exception x)
      {
        LOGGER.trace("", x);
        continue;
      }

      String textPortionType =
          (String) Utils.getProperty(textPortion, "TextPortionType");
      if (textPortionType == null)
      {
        continue;
      }
      if ("Bookmark".equals(textPortionType))
      {
        XNamed bookmark = null;
        boolean isStart = false;
        boolean isCollapsed = false;
        try
        {
          isStart =
            ((Boolean) UNO.getProperty(textPortion, "IsStart")).booleanValue();
          isCollapsed =
            ((Boolean) UNO.getProperty(textPortion, "IsCollapsed")).booleanValue();
          if (isCollapsed) {
            isStart = true;
          }
          bookmark = UNO.XNamed(UNO.getProperty(textPortion, "Bookmark"));
        }
        catch (java.lang.Exception x)
        {
          LOGGER.trace("", x);
          continue;
        }
        if (bookmark == null) {
          continue;
        }

        String name = bookmark.getName();
        if (!name.equals(bookmarkName))
        {
          continue;
        }

        if (isStart)
        {
          lastInsertFormValueStart = name;
          LOGGER.debug(L.m("Found Bookmark-Start for %1", name));
        }
        if (!isStart || isCollapsed)
        {
          LOGGER.debug(L.m("Found Bookmark-End or Collapsed-Bookmark for %1", name));
          if (name.equals(lastInsertFormValueStart))
          {
            handleNewInputField(lastInsertFormValueStart,
                mapBookmarkNameToFormField, doc);
            lastInsertFormValueStart = null;
          }
        }
      }
      else if ("TextField".equals(textPortionType))
      {
        XDependentTextField textField = null;
        int textfieldType = 0; // 0:input, 1:dropdown, 2: reference
        try
        {
          textField =
            UNO.XDependentTextField(UNO.getProperty(textPortion, "TextField"));
          XServiceInfo info = UNO.XServiceInfo(textField);
          if (info.supportsService("com.sun.star.text.TextField.DropDown"))
            textfieldType = 1;
          else if (info.supportsService("com.sun.star.text.TextField.Input"))
            textfieldType = 0;
          else
            continue; // sonstiges TextField
        }
        catch (java.lang.Exception x)
        {
          LOGGER.trace("", x);
          continue;
        }

        switch (textfieldType)
        {
        case 0:
          handleInputField(textField, lastInsertFormValueStart, mapBookmarkNameToFormField, doc);
          break;
        case 1:
            handleDropdown(textField, lastInsertFormValueStart,
              mapBookmarkNameToFormField, doc);
            break;
        }
        lastInsertFormValueStart = null;
      }
      else if ("Frame".equals(textPortionType))
      {
        XControlModel model = null;
        try
        {
          XEnumeration contentEnum =
            UNO.XContentEnumerationAccess(textPortion).createContentEnumeration(
              "com.sun.star.text.TextPortion");
          while (contentEnum.hasMoreElements())
          {
            XControlShape tempShape;
            try
            {
              tempShape = UNO.XControlShape(contentEnum.nextElement());
            }
            catch (Exception x)
            {
              // Wegen OOo Bugs kann nextElement() werfen auch wenn hasMoreElements()
              LOGGER.trace("", x);
              continue;
            }

            if (tempShape != null)
            {
              XControlModel tempModel = tempShape.getControl();
              XServiceInfo info = UNO.XServiceInfo(tempModel);
              if (info.supportsService("com.sun.star.form.component.CheckBox"))
              {
                model = tempModel;
              }
            }
          }
        }
        catch (java.lang.Exception x)
        {
          LOGGER.trace("", x);
          continue;
        }

        handleCheckbox(model, lastInsertFormValueStart, mapBookmarkNameToFormField,
          doc);
        lastInsertFormValueStart = null;
      }
      else
        // sonstige TextPortion
        continue;
    }

    if (lastInsertFormValueStart != null)
      handleNewInputField(lastInsertFormValueStart, mapBookmarkNameToFormField,
          doc);

  }

  /**
   * Fügt ein neues Eingabefeld innerhalb des Bookmarks bookmark ein, erzeugt ein
   * dazugehöriges FormField und setzt ein passendes Mapping von bookmarkName auf
   * dieses FormField in mapBookmarkNameToFormField.
   */
  private static void handleNewInputField(String bookmarkName,
      Map<String, FormField> mapBookmarkNameToFormField, XTextDocument doc)
  {
    FormField formField = new DynamicInputFormField(doc);
    mapBookmarkNameToFormField.put(bookmarkName, formField);
  }

  private static void handleInputField(XDependentTextField textfield,
      String bookmarkName, Map<String, FormField> mapBookmarkNameToFormField,
      XTextDocument doc)
  {
    if (textfield != null)
    {
      FormField formField = new InputFormField(doc, null, textfield);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  private static void handleDropdown(XDependentTextField textfield,
      String bookmarkName, Map<String, FormField> mapBookmarkNameToFormField,
      XTextDocument doc)
  {
    if (textfield != null)
    {
      FormField formField = new DropDownFormField(doc, null, textfield);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  private static void handleCheckbox(XControlModel checkbox, String bookmarkName,
      Map<String, FormField> mapBookmarkNameToFormField, XTextDocument doc)
  {
    if (checkbox != null)
    {
      FormField formField = new CheckboxFormField(doc, null, checkbox);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  /**
   * Dieses Interface beschreibt die Eigenschaften eines Formularfeldes unter einem
   * WM(CMD'insertFormValue'...)-Kommando.
   *
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public interface FormField extends Comparable<FormField>
  {
    /**
     * FIXME Unschöne Fixup-Funktion, die in FormScanner.executeCommand() aufgerufen
     * wird, da der Scan von Tabellenzellen nur die Bookmarks, aber nicht die
     * zugehörigen Commands kennt.
     */
    public abstract void setCommand(InsertFormValue cmd);

    /**
     * Wenn das Feld die 1-zu-1 Ersetzung der Referenz auf die ID oldFieldId durch
     * eine neue ID newFieldId unterstützt, dann wird diese Ersetzung vorgenommen und
     * true zurückgeliefert, ansonsten false.
     */
    public abstract boolean substituteFieldID(String oldFieldId, String newFieldId);

    /**
     * Liefert die XTextRange, an der das Formularfeld verankert ist.
     */
    public XTextRange getAnchor();

    /**
     * Liefert für alle aus insertFormValue-Bookmarks entstandenen FormField-Objekte
     * die im insertFormValue-Kommando angegebene ID, und andernfalls null.
     */
    public abstract String getId();

    /**
     * Die Methode liefert den Namen der Trafo, die auf dieses Formularfeld gesetzt
     * ist oder null, wenn keine Trafo gesetzt ist.
     */
    public abstract String getTrafoName();

    /**
     * Eine Rückgabe von true gibt an, dass die Trafo (falls definiert) nur einen
     * Parameter sinnvoll verarbeiten kann. Ein derartiges Verhalten ist für alle
     * Dokumentkommandos {@code WM(CMD'insertFormValue' ID'<id>' TRAFO '<trafo>')}
     * spezifiziert. In diesem Fall erwartet die Trafo für jeden in der Funktion
     * geforderten Parameter den Wert von {@code <id>}; Eine Rückgabe von false beschreibt,
     * dass die Trafo auch mehrere Parameter verarbeiten kann (wie z.B.
     * InputUserFields der Fall).
     */
    public abstract boolean singleParameterTrafo();

    /**
     * Diese Methode belegt den Wert des Formularfeldes im Dokument mit dem neuen
     * Inhalt value.
     *
     * @param value
     */
    public abstract void setValue(String value);

    /**
     * Diese Methode liefert den aktuellen Wert des Formularfeldes als String zurück
     * oder den Leerstring, falls der Wert nicht bestimmt werden kann.
     *
     * @return der aktuelle Wert des Formularfeldes als String
     */
    public abstract String getValue();

    /**
     * Setzt den ViewCursor auf die Position des InputFields.
     */
    public abstract void focus();

    /**
     * Löscht das Formularfeld aus dem Dokument
     */
    public abstract void dispose();

    /**
     * Liefert den Typ des FormField-Objekts zurück
     */
    public FormFieldType getType();
  }

  /**
   * Beschreibt mögliche Typen von FormField-Objekten
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public enum FormFieldType {
    INPUT_FORM_FIELD,
    DYNAMIC_INPUT_FORM_FIELD,
    DROPDOWN_FORM_FIELD,
    CHECKBOX_FORM_FIELD,
    DATABASE_FORM_FIELD,
    INPUT_USER_FORM_FIELD
  }

  private abstract static class BasicFormField implements FormField
  {
    protected XTextDocument doc;

    protected InsertFormValue cmd;

    /**
     * Erzeugt ein Formularfeld im Dokument doc an der Stelle des
     * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein TextFeld vom
     * Typ InputField vorhanden, so wird dieses Feld als inputField für die
     * Darstellung des Wertes des Formularfeldes genutzt. Ist innerhalb des Bookmarks
     * noch kein InputField vorhanden, so wird ein neues InputField in den Bookmark
     * eingefügt.
     *
     * @param doc
     *          das Dokument, zu dem das Formularfeld gehört.
     * @param cmd
     *          das zugehörige insertFormValue-Kommando.
     * @param focusRange
     *          Beschreibt die range, auf die der ViewCursor beim Aufruf der
     *          focus()-methode gesetzt werden soll. Der Parameter ist erforderlich,
     *          da das Setzen des viewCursors auf die TextRanges des Kommandos cmd
     *          unter Linux nicht sauber funktioniert.
     */
    public BasicFormField(XTextDocument doc, InsertFormValue cmd)
    {
      this.doc = doc;
      this.cmd = cmd;
    }

    @Override
    public void setCommand(InsertFormValue cmd)
    {
      this.cmd = cmd;
    }

    @Override
    public String getId()
    {
      if (cmd != null) {
        return cmd.getID();
      }
      return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.muenchen.allg.itd51.wollmux.FormField#getTrafoName()
     */
    @Override
    public String getTrafoName()
    {
      return cmd.getTrafoName();
    }

    /**
     * Diese Methode liest den Inhalt des internen Formularelements und liefert den
     * Wert als String zurück.
     *
     * @param value
     *          der neue Wert des Formularelements.
     */
    public abstract String getFormElementValue();

    /*
     * (non-Javadoc)
     *
     * @see de.muenchen.allg.itd51.wollmux.FormField#getValue()
     */
    @Override
    public String getValue()
    {
      return getFormElementValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.muenchen.allg.itd51.wollmux.FormField#focus()
     */
    @Override
    public void focus()
    {
      if (cmd == null) {
        return;
      }
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = cmd.getTextCursor();
        if (focusRange != null) {
          cursor.gotoRange(focusRange, false);
        }
      }
      catch (java.lang.Exception e)
      {
        LOGGER.trace("", e);
      }
    }

    /**
     * Vergleicht die Positionen der Dokumentkommandos der Formularfelder im Dokument
     * liefert -1 zurück, wenn this vor other liegt, 1, wenn this nach other liegt
     * und beide Formularfelder dem selben Text-Objekt zugeordnet sind und 0, wenn
     * sich die Dokumentkommandos überlappen; lässt sich die Ordnung nicht bestimmen,
     * da die Text-Objekte sich unterscheiden, dann wird -1 geliefert.
     *
     * @param other
     *          Das Vergleichsobjekt.
     *
     * @return
     */
    @Override
    public int compareTo(FormField other)
    {
      BasicFormField other2;
      try
      {
        other2 = (BasicFormField) other;
      }
      catch (Exception x)
      {
        LOGGER.trace("", x);
        return -1;
      }

      TreeRelation rel = new TreeRelation(cmd.getAnchor(), other2.cmd.getAnchor());
      if (rel.isAGreaterThanB())
        return 1;
      else if (rel.isALessThanB())
        return -1;
      else if (rel.isAEqualB()) {
        return 0;
      }

      return -1;
    }

    @Override
    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      if (oldFieldId == null || newFieldId == null) {
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
   * Repräsentiert ein FormField, das den Formularwert in einem Input-Field
   * darstellt.
   *
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class InputFormField extends BasicFormField
  {
    protected XTextField inputField;

    public InputFormField(XTextDocument doc, InsertFormValue cmd,
        XTextField inputField)
    {
      super(doc, cmd);
      this.inputField = inputField;
    }

    @Override
    public void setValue(String value)
    {
      if (inputField != null && doc != null)
      {
        // Neuen Inhalt in inputField schreiben
        Utils.setProperty(inputField, "Content", value);
      }
    }

    @Override
    public String getFormElementValue()
    {
      if (inputField != null)
      {
        Object content = Utils.getProperty(inputField, "Content");
        if (content != null) {
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
      }
      catch (NoSuchElementException e)
      {
        LOGGER.info("", e);
      }
    }
  }

  /**
   * Repräsentiert ein FormField-Objekt, das zunächst kein Formularelement enthält,
   * aber eines vom Typ c,s,s,text,TextField,InputField erzeugt, wenn mittels focus()
   * oder setFormElementValue(...) darauf zugegriffen wird und der zu setzende Wert
   * nicht der Leerstring ist.
   *
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class DynamicInputFormField extends InputFormField
  {

    public DynamicInputFormField(XTextDocument doc)
    {
      super(doc, null, null);
    }

    @Override
    public void setCommand(InsertFormValue cmd)
    {
      super.setCommand(cmd);
      // ursprünglich R43846 (#2439), überarbeitet nach R48539 (#2867)
      if (inputField == null)
      {
        XTextCursor cursor = cmd.getTextCursor();

        if (cursor != null)
        {
          try
          {
            Bookmark bookmark =
              new Bookmark(cmd.getBookmarkName(), UNO.XBookmarksSupplier(doc));
            String textSurroundedByBookmark = cursor.getString();

            String trimmedText = textSurroundedByBookmark.trim();
            Pattern p = Pattern.compile("\\A[<\\[{].*[\\]>}]\\z");
            // Für die Überprüfung des Patterns verwenden wir den getrimmten Text, da
            // wir auch die Fälle erwischen wollen, wo vielleicht aus Versehen ein
            // Leerzeichen am Anfang reingerutscht ist. Besser wäre es aber eventuell
            // das direkt ins Pattern einzubauen, statt hier zu trimmen.

            if (textSurroundedByBookmark.length() > 0
              && !p.matcher(trimmedText).matches())
            {
              LOGGER.info(L.m(
                "Kollabiere Textmarke \"%2\", die um den Text \"%1\" herum liegt.",
                textSurroundedByBookmark, cmd.getBookmarkName()));

              bookmark.collapseBookmark();
            }
          }
          catch (Exception x)
          {
            LOGGER.trace("", x);
          }
        }
      }
    }

    @Override
    public void setValue(String value)
    {
      if (cmd == null) {
        return;
      }

      if (value.length() == 0)
      {
        // wenn kein inputField vorhanden ist, so wird der Inhalt des Bookmarks
        // gelöscht
        if (inputField == null)
        {
          cmd.setTextRangeString("");
        }
      }
      else
      {
        // Erzeuge Formularelement wenn notwendig
        if (inputField == null) {
          createInputField();
        }
      }
      super.setValue(value);
    }

    private void createInputField()
    {
      if (cmd == null) {
        return;
      }

      String bookmarkName = cmd.getBookmarkName();

      LOGGER.trace(L.m("Erzeuge neues Input-Field für Bookmark \"%1\"",
        bookmarkName));
      try
      {
        XTextField field =
          UNO.XTextField(UNO.XMultiServiceFactory(doc).createInstance(
            "com.sun.star.text.TextField.Input"));

        if (field != null)
        {
          cmd.insertTextContentIntoBookmark(field, true);
          inputField = field;
        }
      }
      catch (java.lang.Exception e)
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
   * Repräsentiert ein FormField, das den Formularwert in einem DropDown-Field
   * darstellt.
   *
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class DropDownFormField extends BasicFormField
  {
    private XTextField dropdownField;

    private String[] origItemList = null;

    public DropDownFormField(XTextDocument doc, InsertFormValue cmd,
        XTextField dropdownField)
    {
      super(doc, cmd);
      this.dropdownField = dropdownField;

      if (dropdownField != null)
        origItemList = (String[]) Utils.getProperty(dropdownField, "Items");

    }

    @Override
    public void setValue(String value)
    {
      // DropDownFormFelder können in OOo nicht mit dem Leerstring belegt
      // werden. Die Verwendung des Leerstrings fürht dazu, dass ein anderes als
      // das ausgewählte Element angezeigt wird. Daher werden Leerstrings auf
      // ein Leerzeichen umgeschrieben. OOo-Issue: #70087
      if (value.isEmpty()) {
        value = " ";
      }

      if (dropdownField != null && doc != null)
      {
        extendItemsList(value);
        Utils.setProperty(dropdownField, "SelectedItem", value);
      }
    }

    /**
     * Die Methode prüft, ob der String value bereits in der zum Zeitpunkt des
     * Konstruktoraufrufs eingelesenen Liste oritItemList der erlaubten Einträge der
     * ComboBox vorhanden ist und erweitert die Liste um value, falls nicht.
     *
     * @param value
     *          der Wert, der ggf. an in die Liste der erlaubten Einträge aufgenommen
     *          wird.
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
            extendedItems[i] = origItemList[i];
          extendedItems[origItemList.length] = value;
          Utils.setProperty(dropdownField, "Items", extendedItems);
        }
      }
    }

    @Override
    public String getFormElementValue()
    {
      if (dropdownField != null)
      {
        Object content = Utils.getProperty(dropdownField, "SelectedItem");
        if (content != null) {
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
   * Repräsentiert ein FormField, das den Formularwert in einer Checkbox darstellt.
   *
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class CheckboxFormField extends BasicFormField
  {
    private Object checkbox;

    /**
     * Erzeugt eine neue CheckboxFormField, das eine bereits im Dokument doc
     * bestehende Checkbox checkbox vom Service-Typ
     * com.sun.star.form.component.CheckBox an der Stelle des Kommandos cmd
     * repräsentiert.
     *
     * @param doc
     *          Das Dokument in dem sich das Checkbox-Formularfeld-Kommando befindet
     * @param cmd
     *          das zum Formularfeld zugehörige insertFormValue-Kommando
     * @param checkbox
     *          Ein UNO-Service vom Typ von com.sun.star.form.component.CheckBox das
     *          den Zugriff auf das entsprechende FormControl-Element ermöglicht.
     * @param focusRange
     *          Beschreibt die range, auf die der ViewCursor beim Aufruf der
     *          focus()-methode gesetzt werden soll.
     */
    public CheckboxFormField(XTextDocument doc, InsertFormValue cmd, Object checkbox)
    {
      super(doc, cmd);

      this.checkbox = checkbox;
    }

    @Override
    public void setValue(String value)
    {
      Utils.setProperty(checkbox, "State",
        Boolean.parseBoolean(value) ? Short.valueOf((short) 1) : Short.valueOf((short) 0));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * de.muenchen.allg.itd51.wollmux.FormFieldFactory.BasicFormField#getFormElementValue
     * ()
     */
    @Override
    public String getFormElementValue()
    {
      Object state = Utils.getProperty(checkbox, "State");
      if (state != null && state.equals(Short.valueOf((short) 1)))
        return "true";
      else
        return "false";
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
   * Kapselt ein Serienbrieffeld UNO-Objekt vom Typ c,s,s,text,textfield,Database als
   * FormField. In einem Serienbrieffeld kann keine TRAFO-Funktion gesetzt werden -
   * deshalb liefert die Methode getTrafoName() immer null zurück. Die Objekte dieser
   * Klasse betrachten zum Zwecke von equals() und hashCode() die zugrundeliegenden
   * UNO-Objekte.
   *
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
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

    /**
     * Nicht verwendet.
     */
    @Override
    public void setCommand(InsertFormValue cmd)
    {
    // nicht verwendet
    }

    @Override
    public String getTrafoName()
    {
      // diese Felder sind immer untransformiert
      return null;
    }

    @Override
    public void setValue(String value)
    {
      if (value == null) {
        return;
      }
      Utils.setProperty(textfield, "Content", value);
      Utils.setProperty(textfield, "CurrentPresentation", value);
    }

    @Override
    public String getValue()
    {
      String cont = (String) Utils.getProperty(textfield, "Content");
      if (cont == null)
        cont = (String) Utils.getProperty(textfield, "CurrentPresentation");
      if (cont != null) {
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
        if (focusRange != null) {
          cursor.gotoRange(focusRange, false);
        }
      }
      catch (java.lang.Exception e)
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
      if (textfield != null) {
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
      // Der Rückgabewert spielt keine Rolle da diese Felder immer untransformiert
      // sind.
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
   * Kapselt ein Eingabefeld für eine Benutzervariable vom Typ
   * c,s,s,text,textfield,InputUser und den zugehörigen TextFieldMaster master als
   * FormField. Bei InputUser-Objekten kann auf den angezeigten Wert nicht direkt
   * zugegriffen werden. Diese Zugriffe erfolgen über einen TextFieldMaster, der dem
   * InputUser-Objekt zugeordnet ist. VORSICHT: Das Objekt textfield.TextFieldMaster
   * ist dabei nicht als Master geeignet, da dieser Master keine direkte Möglichkeit
   * zum Setzen der Anzeigewerte anbietet. Das statt dessen geeignete
   * TextFieldMaster-Objekt muss über doc.getTextFieldMasters() bezogen werden, wobei
   * textfield und master dann zusammen gehören, wenn
   * textfield.Content.equals(master.Name) gilt. Die Objekte dieser Klasse betrachten
   * zum Zwecke von equals() und hashCode() die zugrundeliegenden UNO-Objekte.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static class InputUserFormField implements FormField
  {
    private XTextDocument doc;

    private XTextField textfield;

    private XPropertySet master;

    public InputUserFormField(XTextDocument doc, XTextField textfield,
        XPropertySet master)
    {
      this.doc = doc;
      this.textfield = textfield;
      this.master = master;
    }

    @Override
    public void setCommand(InsertFormValue cmd)
    {
    // nicht notwendig
    }

    @Override
    public void setValue(final String value)
    {
      if (value != null && textfield != null && doc != null)
      {
        Utils.setProperty(master, "Content", value);
      }
    }

    @Override
    public String getTrafoName()
    {
      return TextDocumentModel.getFunctionNameForUserFieldName(""
          + Utils.getProperty(textfield, "Content"));
    }

    @Override
    public String getValue()
    {
      if (master == null) {
        return "";
      }
      return "" + Utils.getProperty(master, "Content");
    }

    @Override
    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller).getViewCursor();
        XTextRange focusRange = UNO.XTextContent(textfield).getAnchor();
        if (focusRange != null) {
          cursor.gotoRange(focusRange, false);
        }
      }
      catch (java.lang.Exception e)
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
      if (textfield != null) {
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
