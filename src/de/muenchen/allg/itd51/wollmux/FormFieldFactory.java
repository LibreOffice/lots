/*
 * Dateiname: FormFieldFactory.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert eine Fabrik, die an der Stelle von 
 *            WM('insertFormValue'...)-Bookmark entsprechende FormField-Elemente
 *            erzeugt.
 * 
 * Copyright: Landeshauptstadt München
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
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.awt.XControlModel;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XControlShape;
import com.sun.star.frame.XController;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.table.XCell;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.TextRangeRelation.TreeRelation;

/**
 * Repräsentiert eine Fabrik, die an der Stelle von
 * WM('insertFormValue'...)-Bookmark entsprechende FormField-Elemente erzeugt.
 * 
 * @author lut
 */
public final class FormFieldFactory
{
  public static final Pattern INSERTFORMVALUE = Pattern
      .compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'((insertFormValue))'.*\\))\\s*\\d*\\z");

  /**
   * Erzeugt ein Formualfeld im Dokument doc an der Stelle des
   * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein
   * Formularelement (derzeit TextFeld vom Typ Input, DropDown oder eine
   * Checkbox) vorhanden, so wird dieses Feld als Formularelement für die
   * Darstellung des Wertes des Formularfeldes genutzt. Ist innerhalb des
   * Bookmarks noch kein Formularelement vorhanden, so wird ein
   * DynamicInputFormField an der Stelle des Bookmarks erzeugt, das erst dann
   * ein InputField-Textfeld im Dokument anlegt, wenn auf das Textfeld
   * schreibend zugegriffen wird.
   * 
   * @param doc
   *          das Dokument, zu dem das Formularfeld gehört.
   * @param cmd
   *          das zugehörige insertFormValue-Kommando.
   */
  public static FormField createFormField(XTextDocument doc,
      InsertFormValue cmd, Map bookmarkNameToFormField)
  {
    String bookmarkName = cmd.getBookmarkName();
    FormField formField = (FormField) bookmarkNameToFormField.get(bookmarkName);
    if (formField != null) return formField;

    /*
     * Falls die range in einer Tabellenzelle liegt, wird sie auf die ganze
     * Zelle ausgeweitet, damit die ganze Zelle gescannt wird (Workaround für
     * Bug http://qa.openoffice.org/issues/show_bug.cgi?id=68261)
     */
    XTextRange range = cmd.getAnchor();
    if (range != null)
    {
      range = range.getText();
      XCell cell = UNO.XCell(range);
      if (cell == null)
        range = null;
      else if (WollMuxFiles.isDebugMode())
      {
        String cellName = (String) UNO.getProperty(cell, "CellName");
        Logger.debug("Scanne Zelle " + cellName);
      }
    }

    if (range == null) range = cmd.getTextRange();

    if (range != null)
    {
      XEnumeration xenum = UNO.XEnumerationAccess(range).createEnumeration();
      handleParagraphEnumeration(xenum, doc, bookmarkNameToFormField);
    }

    return (FormField) bookmarkNameToFormField.get(bookmarkName);
  }

  /**
   * Erzeugt ein neues FormField für das Serienbrieffeld textfield vom Typ
   * c,s,s,text,textfield,Database, das im Dokument doc liegt. Die Methoden
   * {@link Object#equals(java.lang.Object)} und {@link Object#hashCode()}
   * beziehen sich auf das zugrundeliegende UNO-Objekt, wobei verschiedene
   * Proxies des selben Objekts als gleich behandelt werden.
   * 
   * @param doc
   *          das zugehörige Dokument doc
   * @param textfield
   *          ein Serienbrieffeld vom Typ css.text.textfield.Database.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static FormField createDatabaseFormField(XTextDocument doc,
      XTextField textfield)
  {
    return new DatabaseFormField(doc, textfield);
  }

  /**
   * Erzeugt ein neues FormField für ein Eingabefeld einer Benutzervariablen vom
   * Typ c,s,s,text,textfield,InputUser und den zugehörigen TextFieldMaster
   * master die im Dokument doc liegen. Die Methoden
   * {@link Object#equals(java.lang.Object)} und {@link Object#hashCode()}
   * beziehen sich auf das zugrundeliegende UNO-Objekt, wobei verschiedene
   * Proxies des selben Objekts als gleich behandelt werden.
   * 
   * @param doc
   *          das zugehörige Dokument doc
   * @param textfield
   *          das InputUser-Objekt.
   * @param master
   *          bei InputUser-Objekten kann auf den angezeigten Wert nicht direkt
   *          zugegriffen werden. Diese Zugriffe erfolgen über einen
   *          TextFieldMaster, der dem InputUser-Objekt zugeordnet ist.
   *          VORSICHT: Das Objekt textfield.TextFieldMaster ist dabei nicht als
   *          Master geeignet, da dieser Master keine direkte Möglichkeit zum
   *          Setzen der Anzeigewerte anbietet. Das statt dessen geeignete
   *          TextFieldMaster-Objekt muss über doc.getTextFieldMasters() bezogen
   *          werden, wobei textfield und master dann zusammen gehören, wenn
   *          textfield.Content.equals(master.Name) gilt.
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static FormField createInputUserFormField(XTextDocument doc,
      XTextField textfield, XPropertySet master)
  {
    return new InputUserFormField(doc, textfield, master);
  }

  /**
   * Geht die XEnumeration enu von Absätzen und TextTables durch und erzeugt für
   * alle in Absätzen (nicht TextTables) enthaltenen insertFormValue-Bookmarks
   * entsprechende Einträge in mapBookmarkNameToFormField. Falls nötig wird das
   * entsprechende FormField erzeugt.
   * 
   * @param doc
   *          das Dokument in dem sich die enumierten Objekte befinden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void handleParagraphEnumeration(XEnumeration enu,
      XTextDocument doc, Map mapBookmarkNameToFormField)
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
        continue;
      }
      enuAccess = UNO.XEnumerationAccess(ele);
      if (enuAccess != null) // ist wohl ein SwXParagraph
      {
        handleParagraph(enuAccess, doc, mapBookmarkNameToFormField);
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void handleParagraph(XEnumerationAccess paragraph,
      XTextDocument doc, Map mapBookmarkNameToFormField)
  {
    /*
     * Der Name des zuletzt gestarteten insertFormValue-Bookmarks.
     */
    String lastInsertFormValueStart = null;
    XNamed lastInsertFormValueBookmark = null;

    /*
     * enumeriere alle TextPortions des Paragraphs
     */
    XEnumeration textPortionEnu = paragraph.createEnumeration();
    while (textPortionEnu.hasMoreElements())
    {
      /*
       * Diese etwas seltsame Konstruktion beugt Exceptions durch Bugs wie 68261
       * vor.
       */
      Object textPortion;
      try
      {
        textPortion = textPortionEnu.nextElement();
      }
      catch (java.lang.Exception x)
      {
        continue;
      }
      ;

      String textPortionType = (String) UNO.getProperty(
          textPortion,
          "TextPortionType");
      if (textPortionType.equals("Bookmark"))
      {
        XNamed bookmark = null;
        boolean isStart = false;
        boolean isCollapsed = false;
        try
        {
          isStart = ((Boolean) UNO.getProperty(textPortion, "IsStart"))
              .booleanValue();
          isCollapsed = ((Boolean) UNO.getProperty(textPortion, "IsCollapsed"))
              .booleanValue();
          if (isCollapsed) isStart = true;
          bookmark = UNO.XNamed(UNO.getProperty(textPortion, "Bookmark"));
        }
        catch (java.lang.Exception x)
        {
          continue;
        }
        if (bookmark == null) continue;

        String name = bookmark.getName();
        Matcher m = INSERTFORMVALUE.matcher(name);
        if (m.matches())
        {
          if (isStart)
          {
            lastInsertFormValueStart = name;
            lastInsertFormValueBookmark = bookmark;
          }
          if (!isStart || isCollapsed)
          {
            if (name.equals(lastInsertFormValueStart))
            {
              handleNewInputField(
                  lastInsertFormValueStart,
                  bookmark,
                  mapBookmarkNameToFormField,
                  doc);
              lastInsertFormValueStart = null;
            }
          }
        }
      }
      else if (textPortionType.equals("TextField"))
      {
        XDependentTextField textField = null;
        int textfieldType = 0; // 0:input, 1:dropdown, 2: reference
        try
        {
          textField = UNO.XDependentTextField(UNO.getProperty(
              textPortion,
              "TextField"));
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
          continue;
        }

        switch (textfieldType)
        {
          case 0:
            handleInputField(
                textField,
                lastInsertFormValueStart,
                mapBookmarkNameToFormField,
                doc);
            break;
          case 1:
            handleDropdown(
                textField,
                lastInsertFormValueStart,
                mapBookmarkNameToFormField,
                doc);
            break;
        }
        lastInsertFormValueStart = null;
      }
      else if (textPortionType.equals("Frame"))
      {
        XControlModel model = null;
        try
        {
          XEnumeration contentEnum = UNO.XContentEnumerationAccess(textPortion)
              .createContentEnumeration("com.sun.star.text.TextPortion");
          while (contentEnum.hasMoreElements())
          {
            XControlShape tempShape = null;
            try
            {
              tempShape = UNO.XControlShape(contentEnum.nextElement());
            }
            catch (java.lang.Exception x)
            {
            }
            XControlModel tempModel = tempShape.getControl();
            XServiceInfo info = UNO.XServiceInfo(tempModel);
            if (info.supportsService("com.sun.star.form.component.CheckBox"))
            {
              model = tempModel;
            }
          }
        }
        catch (java.lang.Exception x)
        {
          continue;
        }

        handleCheckbox(
            model,
            lastInsertFormValueStart,
            mapBookmarkNameToFormField,
            doc);
        lastInsertFormValueStart = null;
      }
      else
        // sonstige TextPortion
        continue;
    }

    if (lastInsertFormValueStart != null)
      handleNewInputField(
          lastInsertFormValueStart,
          lastInsertFormValueBookmark,
          mapBookmarkNameToFormField,
          doc);

  }

  /**
   * Fügt ein neues Eingabefeld innerhalb des Bookmarks bookmark ein, erzeugt
   * ein dazugehöriges FormField und setzt ein passendes Mapping von
   * bookmarkName auf dieses FormField in mapBookmarkNameToFormField
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  private static void handleNewInputField(String bookmarkName, XNamed bookmark,
      Map mapBookmarkNameToFormField, XTextDocument doc)
  {
    FormField formField = new DynamicInputFormField(doc, null);
    mapBookmarkNameToFormField.put(bookmarkName, formField);
  }

  private static void handleInputField(XDependentTextField textfield,
      String bookmarkName, Map mapBookmarkNameToFormField, XTextDocument doc)
  {
    if (textfield != null)
    {
      FormField formField = new InputFormField(doc, null, textfield);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  private static void handleDropdown(XDependentTextField textfield,
      String bookmarkName, Map mapBookmarkNameToFormField, XTextDocument doc)
  {
    if (textfield != null)
    {
      FormField formField = new DropDownFormField(doc, null, textfield);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  private static void handleCheckbox(XControlModel checkbox,
      String bookmarkName, Map mapBookmarkNameToFormField, XTextDocument doc)
  {
    if (checkbox != null)
    {
      FormField formField = new CheckboxFormField(doc, null, checkbox);
      mapBookmarkNameToFormField.put(bookmarkName, formField);
    }
  }

  /**
   * Dieses Interface beschreibt die Eigenschaften eines Formularfeldes unter
   * einem WM(CMD'insertFormValue'...)-Kommando.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  interface FormField extends Comparable
  {
    /**
     * FIXME Unschöne Fixup-Funktion, die in FormScanner.executeCommand()
     * aufgerufen wird, da der Scan von Tabellenzellen nur die Bookmarks, aber
     * nicht die zugehörigen Commands kennt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public abstract void setCommand(InsertFormValue cmd);

    /**
     * Wenn das Feld die 1-zu-1 Ersetzung der Referenz auf die ID oldFieldId
     * durch eine neue ID newFieldId unterstützt, dann wird diese Ersetzung
     * vorgenommen und true zurückgeliefert, ansonsten false.
     */
    public abstract boolean substituteFieldID(String oldFieldId,
        String newFieldId);

    /**
     * Liefert die XTextRange, an der das Formularfeld verankert ist.
     */
    public XTextRange getAnchor();

    /**
     * Die Methode liefert den Namen der Trafo, die auf dieses Formularfeld
     * gesetzt ist oder null, wenn keine Trafo gesetzt ist.
     */
    public abstract String getTrafoName();

    /**
     * Diese Methode belegt den Wert des Formularfeldes im Dokument mit dem
     * neuen Inhalt value.
     * 
     * @param value
     */
    public abstract void setValue(String value);

    /**
     * Diese Methode liefert den aktuellen Wert des Formularfeldes als String
     * zurück oder null, falls der Wert nicht bestimmt werden kann.
     * 
     * @return der aktuelle Wert des Formularfeldes als String
     */
    public abstract String getValue();

    /**
     * Setzt den ViewCursor auf die Position des InputFields.
     * 
     * @param doc
     */
    public abstract void focus();

    /**
     * Löscht das Formularfeld aus dem Dokument
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public abstract void dispose();
  }

  private static abstract class BasicFormField implements FormField
  {
    protected XTextDocument doc;

    protected InsertFormValue cmd;

    public void setCommand(InsertFormValue cmd)
    {
      this.cmd = cmd;
    };

    /**
     * Erzeugt ein Formualfeld im Dokument doc an der Stelle des
     * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein
     * TextFeld vom Typ InputField vorhanden, so wird dieses Feld als inputField
     * für die Darstellung des Wertes des Formularfeldes genutzt. Ist innerhalb
     * des Bookmarks noch kein InputField vorhanden, so wird ein neues
     * InputField in den Bookmark eingefügt.
     * 
     * @param doc
     *          das Dokument, zu dem das Formularfeld gehört.
     * @param cmd
     *          das zugehörige insertFormValue-Kommando.
     * @param focusRange
     *          Beschreibt die range, auf die der ViewCursor beim Aufruf der
     *          focus()-methode gesetzt werden soll. Der Parameter ist
     *          erforderlich, da das Setzen des viewCursors auf die TextRanges
     *          des Kommandos cmd unter Linux nicht sauber funktioniert.
     */
    public BasicFormField(XTextDocument doc, InsertFormValue cmd)
    {
      this.doc = doc;
      this.cmd = cmd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#getTrafoName()
     */
    public String getTrafoName()
    {
      return cmd.getTrafoName();
    }

    /**
     * Diese Methode liest den Inhalt des internen Formularelements und liefert
     * den Wert als String zurück.
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
    public String getValue()
    {
      return getFormElementValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#focus()
     */
    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller)
            .getViewCursor();
        XTextRange focusRange = cmd.getTextRange();
        if (focusRange != null) cursor.gotoRange(focusRange, false);
      }
      catch (java.lang.Exception e)
      {
      }
    }

    /**
     * Vergleicht die Positionen der Dokumentkommandos der Formularfelder im
     * Dokument liefert -1 zurück, wenn this vor other liegt, 1, wenn this nach
     * other liegt und beide Formularfelder dem selben Text-Objekt zugeordnet
     * sind und 0, wenn sich die Dokumentkommandos überlappen; lässt sich die
     * Ordnung nicht bestimmen, da die Text-Objekte sich unterscheiden, dann
     * wird -1 geliefert.
     * 
     * @param other
     *          Das Vergleichsobjekt.
     * 
     * @return
     */
    public int compareTo(Object other)
    {
      TreeRelation rel = new TreeRelation(cmd.getAnchor(),
          ((BasicFormField) other).cmd.getAnchor());
      if (rel.isAGreaterThanB())
        return 1;
      else if (rel.isALessThanB())
        return -1;
      else if (rel.isAEqualB()) return 0;

      return -1;
    }

    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      if (oldFieldId == null || newFieldId == null) return false;
      if (cmd.getID().equals(oldFieldId))
      {
        cmd.setID(newFieldId);
        return true;
      }
      return false;
    }

    public XTextRange getAnchor()
    {
      return cmd.getAnchor();
    }

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

    public void setValue(String value)
    {
      if (inputField != null && UNO.XUpdatable(inputField) != null)
      {
        UNO.setProperty(inputField, "Content", value);
        UNO.XUpdatable(inputField).update();
      }
    }

    public String getFormElementValue()
    {
      if (inputField != null)
      {
        Object content = UNO.getProperty(inputField, "Content");
        if (content != null) return content.toString();
      }
      return "";
    }
  }

  /**
   * Repräsentiert ein FormField-Objekt, das zunächst kein Formularelement
   * enthält, aber eines vom Typ c,s,s,text,TextField,InputField erzeugt, wenn
   * mittels focus() oder setFormElementValue(...) darauf zugegriffen wird und
   * der zu setzende Wert nicht der Leerstring ist. Wird setFormElementValue()
   * der Leerstring übergeben, so werden der Inhalt des Bookmarks und das
   * Formularelement gelöscht.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  private static class DynamicInputFormField extends InputFormField
  {

    public DynamicInputFormField(XTextDocument doc, InsertFormValue cmd)
    {
      super(doc, cmd, null);
    }

    public void setValue(String value)
    {
      if (value.length() == 0)
      {
        // wenn kein inputField vorhanden ist, so wird der Inhalt des Bookmarks
        // gelöscht.
        if (inputField == null)
        {
          XTextRange range = cmd.createInsertCursor(false);
          if (range != null) range.setString("");
        }
      }
      else
      {
        // Erzeuge Formularelement wenn notwendig
        if (inputField == null) createInputField();
      }
      super.setValue(value);
    }

    public void focus()
    {
      if (inputField == null) createInputField();
      super.focus();
    }

    private void createInputField()
    {
      if (cmd == null) return;

      String bookmarkName = cmd.getBookmarkName();

      Logger.debug2("Erzeuge neues Input-Field für Bookmark \""
                    + bookmarkName
                    + "\"");
      try
      {
        XTextRange range = cmd.createInsertCursor(false);
        XText text = range.getText();
        XTextField field = UNO.XTextField(UNO.XMultiServiceFactory(doc)
            .createInstance("com.sun.star.text.TextField.Input"));
        XTextCursor cursor = text.createTextCursorByRange(range);

        if (cursor != null && field != null)
          text.insertTextContent(cursor, field, true);

        inputField = field;
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
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
        origItemList = (String[]) UNO.getProperty(dropdownField, "Items");

    }

    public void setValue(String value)
    {
      // DropDownFormFelder können in OOo nicht mit dem Leerstring belegt
      // werden. Die Verwendung des Leerstrings fürht dazu, dass ein anderes als
      // das ausgewählte Element angezeigt wird. Daher werden Leerstrings auf
      // ein Leerzeichen umgeschrieben. OOo-Issue: #70087
      if (value.equals("")) value = " ";

      if (dropdownField != null && UNO.XUpdatable(dropdownField) != null)
      {
        extendItemsList(value);
        UNO.setProperty(dropdownField, "SelectedItem", value);
        UNO.XUpdatable(dropdownField).update();
      }
    }

    /**
     * Die Methode prüft, ob der String value bereits in der zum Zeitpunkt des
     * Konstruktoraufrufs eingelesenen Liste oritItemList der erlaubten Einträge
     * der ComboBox vorhanden ist und erweitert die Liste um value, falls nicht.
     * 
     * @param value
     *          der Wert, der ggf. an in die Liste der erlaubten Einträge
     *          aufgenommen wird.
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
          UNO.setProperty(dropdownField, "Items", extendedItems);
        }
      }
    }

    public String getFormElementValue()
    {
      if (dropdownField != null)
      {
        Object content = UNO.getProperty(dropdownField, "SelectedItem");
        if (content != null) return content.toString();
      }
      return "";
    }
  }

  /**
   * Repräsentiert ein FormField, das den Formularwert in einer Checkbox
   * darstellt.
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
     *          Das Dokument in dem sich das Checkbox-Formularfeld-Kommando
     *          befindet
     * @param cmd
     *          das zum Formularfeld zugehörige insertFormValue-Kommando
     * @param checkbox
     *          Ein UNO-Service vom Typ von com.sun.star.form.component.CheckBox
     *          das den Zugriff auf das entsprechende FormControl-Element
     *          ermöglicht.
     * @param focusRange
     *          Beschreibt die range, auf die der ViewCursor beim Aufruf der
     *          focus()-methode gesetzt werden soll.
     */
    public CheckboxFormField(XTextDocument doc, InsertFormValue cmd,
        Object checkbox)
    {
      super(doc, cmd);

      this.checkbox = checkbox;
    }

    public void setValue(String value)
    {
      Boolean bv = new Boolean(value);

      UNO.setProperty(checkbox, "State", ((bv.booleanValue()) ? new Short(
          (short) 1) : new Short((short) 0)));
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormFieldFactory.BasicFormField#getFormElementValue()
     */
    public String getFormElementValue()
    {
      Object state = UNO.getProperty(checkbox, "State");
      if (state != null && state.equals(new Short((short) 1)))
        return "true";
      else
        return "false";
    }
  }

  /**
   * Kapselt ein Serienbrieffeld UNO-Objekt vom Typ
   * c,s,s,text,textfield,Database als FormField. In einem Serienbrieffeld kann
   * keine TRAFO-Funktion gesetzt werden - deshalb liefert die Methode
   * getTrafoName() immer null zurück. Die Objekte dieser Klasse betrachten zum
   * Zwecke von equals() und hashCode() die zugrundeliegenden UNO-Objekte.
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
    public void setCommand(InsertFormValue cmd)
    {
      // nicht verwendet
    }

    public String getTrafoName()
    {
      // diese Felder sind immer untransformiert
      return null;
    }

    public void setValue(String value)
    {
      if (value == null) return;
      UNO.setProperty(textfield, "Content", value);
      UNO.setProperty(textfield, "CurrentPresentation", value);
    }

    public String getValue()
    {
      String cont = (String) UNO.getProperty(textfield, "Content");
      if (cont == null)
        cont = (String) UNO.getProperty(textfield, "CurrentPresentation");
      if (cont != null) return cont;
      return "";
    }

    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller)
            .getViewCursor();
        XTextRange focusRange = UNO.XTextContent(textfield).getAnchor();
        if (focusRange != null) cursor.gotoRange(focusRange, false);
      }
      catch (java.lang.Exception e)
      {
      }
    }

    public int hashCode()
    {
      return UnoRuntime.generateOid(textfield).hashCode();
    }

    public boolean equals(Object b)
    {
      return UnoRuntime.areSame(textfield, UNO.XInterface(b));
    }

    public int compareTo(Object arg0)
    {
      // wird nicht aufgerufen und daher auch nicht implementiert.
      return -1;
    }

    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      return false;
    }

    public XTextRange getAnchor()
    {
      return textfield.getAnchor();
    }

    public void dispose()
    {
      if (textfield != null) textfield.dispose();
    }
  }

  /**
   * Kapselt ein Eingabefeld für eine Benutzervariable vom Typ
   * c,s,s,text,textfield,InputUser und den zugehörigen TextFieldMaster master
   * als FormField. Bei InputUser-Objekten kann auf den angezeigten Wert nicht
   * direkt zugegriffen werden. Diese Zugriffe erfolgen über einen
   * TextFieldMaster, der dem InputUser-Objekt zugeordnet ist. VORSICHT: Das
   * Objekt textfield.TextFieldMaster ist dabei nicht als Master geeignet, da
   * dieser Master keine direkte Möglichkeit zum Setzen der Anzeigewerte
   * anbietet. Das statt dessen geeignete TextFieldMaster-Objekt muss über
   * doc.getTextFieldMasters() bezogen werden, wobei textfield und master dann
   * zusammen gehören, wenn textfield.Content.equals(master.Name) gilt. Die
   * Objekte dieser Klasse betrachten zum Zwecke von equals() und hashCode() die
   * zugrundeliegenden UNO-Objekte.
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

    public void setCommand(InsertFormValue cmd)
    {
      // nicht notwendig
    }

    public void setValue(final String value)
    {
      if (value == null) return;
      UNO.setProperty(master, "Content", value);
      if (UNO.XUpdatable(textfield) != null)
        UNO.XUpdatable(textfield).update();
    }

    public String getTrafoName()
    {
      return TextDocumentModel
          .getFunctionNameForUserFieldName(""
                                           + UNO.getProperty(
                                               textfield,
                                               "Content"));
    }

    public String getValue()
    {
      if (master == null) return "";
      return "" + UNO.getProperty(master, "Content");
    }

    public void focus()
    {
      try
      {
        XController controller = UNO.XModel(doc).getCurrentController();
        XTextCursor cursor = UNO.XTextViewCursorSupplier(controller)
            .getViewCursor();
        XTextRange focusRange = UNO.XTextContent(textfield).getAnchor();
        if (focusRange != null) cursor.gotoRange(focusRange, false);
      }
      catch (java.lang.Exception e)
      {
      }
    }

    public int compareTo(Object arg0)
    {
      // wird nicht aufgerufen und daher auch nicht implementiert
      return -1;
    }

    public int hashCode()
    {
      return UnoRuntime.generateOid(UNO.XInterface(textfield)).hashCode();
    }

    public boolean equals(Object b)
    {
      return UnoRuntime.areSame(UNO.XInterface(textfield), UNO.XInterface(b));
    }

    public boolean substituteFieldID(String oldFieldId, String newFieldId)
    {
      return false;
    }

    public XTextRange getAnchor()
    {
      return textfield.getAnchor();
    }

    public void dispose()
    {
      if (textfield != null) textfield.dispose();
    }
  }
}
