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
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.sun.star.container.XEnumeration;
import com.sun.star.drawing.XControlShape;
import com.sun.star.frame.XController;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values.SimpleMap;

/**
 * Repräsentiert eine Fabrik, die an der Stelle von
 * WM('insertFormValue'...)-Bookmark entsprechende FormField-Elemente erzeugt.
 * 
 * @author lut
 */
public final class FormFieldFactory
{
  /**
   * Erzeugt ein Formualfeld im Dokument doc an der Stelle des
   * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein
   * Formularelement (derzeit TextFeld vom Typ InputField oder eine Checkbox)
   * vorhanden, so wird dieses Feld als Formularelement für die Darstellung des
   * Wertes des Formularfeldes genutzt. Ist innerhalb des Bookmarks noch kein
   * Formularelement vorhanden, so wird ein neues InputField in den Bookmark
   * eingefügt.
   * 
   * @param doc
   *          das Dokument, zu dem das Formularfeld gehört.
   * @param cmd
   *          das zugehörige insertFormValue-Kommando.
   */
  public static FormField createFormField(XTextDocument doc, InsertFormValue cmd)
  {
    XTextRange range = cmd.getTextRange();

    // FormControl vom Typ Checkbox suchen:
    if (range != null)
    {
      XControlShape shape = findControlShape(
          range,
          "com.sun.star.form.component.CheckBox");
      if (shape != null && UNO.XTextContent(shape) != null)
        return new CheckboxFormField(doc, cmd, shape.getControl(), UNO
            .XTextContent(shape).getAnchor());
    }

    // Textfeld vom Typ com.sun.star.text.TextField.Input suchen:
    XTextField field = null;
    if (range != null)
      field = findTextField(range, "com.sun.star.text.TextField.Input");

    // wenn kein Formularfeld gefunden wurde, wird ein TextField.Input neu
    // erstellt.
    if (field == null)
    {
      Logger.debug2(cmd + ": Erzeuge neues Input-Field.");
      try
      {
        field = UNO.XTextField(UNO.XMultiServiceFactory(doc).createInstance(
            "com.sun.star.text.TextField.Input"));
        XTextCursor cursor = cmd.createInsertCursor();
        if (cursor != null && field != null)
        {
          cursor.getText().insertTextContent(cursor, field, true);
        }
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    if (field != null) return new InputFormField(doc, cmd, field);

    return null;
  }

  /**
   * Diese Methode sucht in der übergebenen Range range nach einer Instanz von
   * XControlShape, dessen getControl-Methode ein Objekt vom Service-Typ
   * serviceName implementiert, und liefert das erste gefundene, Element zurück
   * oder null, falls kein entsprechendes XControlShape-Element vorhanden ist.
   * 
   * @param range
   *          Die range in der nach FormControl-Elementen gesucht werden soll.
   * @param serviceName
   *          Der Service name des gesuchten FormControl-Elements
   * @return Die XControlShape-Instanz, die das gesuchte FormControl-Element zur
   *         Verfügung stellt, oder null, falls kein entsprechendes Element
   *         vorhanden ist.
   */
  private static XControlShape findControlShape(XTextRange range,
      String serviceName)
  {
    if (UNO.XContentEnumerationAccess(range) != null)
    {
      XEnumeration cursEnum = UNO.XContentEnumerationAccess(range)
          .createContentEnumeration("com.sun.star.text.TextContent");
      while (cursEnum.hasMoreElements())
      {
        Object element = null;
        try
        {
          element = cursEnum.nextElement();
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
        if (UNO.XControlShape(element) != null)
        {
          Object control = UNO.XControlShape(element).getControl();
          if (UNO.supportsService(control, serviceName))
            return UNO.XControlShape(element);
        }
      }
    }
    return null;
  }

  /**
   * Dieses Interface beschreibt die Eigenschaften eines Formularfeldes unter
   * einem WM(CMD'insertFormValue'...)-Kommando.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  interface FormField
  {
    /**
     * Gibt an, ob der Inhalt des Formularelements mit der zuletzt gesetzten
     * MD5-Sum des Feldes übereinstimmt oder ob der Wert seitdem geändert wurde
     * (z.B. durch manuelles Setzen des Feldinhalts durch den Benutzer). Ist
     * noch kein MD5-Vergleichswert vorhanden, so wird false zurückgeliefert.
     * 
     * @return true, wenn der Inhalt des Formularelements nicht mehr mit der
     *         zuletzt gesetzten MD5-Sum übereinstimmt, ansonsten false
     */
    public abstract boolean hasChangedPreviously();

    /**
     * Die Methode liefert true, wenn das insertFormValue-Kommando eine
     * TRAFO-Funktion verwendet, um den gesetzten Wert zu transformieren,
     * ansonsten wird false zurückgegeben.
     */
    public abstract boolean hasTrafo();

    /**
     * Diese Methode belegt den Wert des Formularfeldes im Dokument mit dem
     * neuen Inhalt value; ist das Formularfeld mit einer TRAFO belegt, so wird
     * vor dem setzen des neuen Inhaltes die TRAFO-Funktion ausgeführt und der
     * neu berechnete Wert stattdessen eingefügt.
     * 
     * @param value
     */
    public abstract void setValue(String value, FunctionLibrary funcLib);

    /**
     * Diese Methode liefert den aktuellen Wert des Formularfeldes als String
     * zurück oder null, falls der Wert nicht bestimmt werden kann.
     * 
     * @return der aktuelle Wert des Formularfeldes als String
     */
    public abstract String getValue();

    /**
     * Liefert true, wenn das Dokumentkommando eine Checksumme (MD5) im Status
     * enthält, über die festgestellt werden kann, ob der Wert des Eingabefeldes
     * noch mit der zuletzt gesetzten Checksumme übereinstimmt (siehe
     * hasChangedPreviously())
     */
    public abstract boolean hasChecksum();

    /**
     * Setzt den ViewCursor auf die Position des InputFields und merkt sich
     * dabei die letze ViewCursor-Position.
     * 
     * @param doc
     */
    public abstract void focus();

    /**
     * Setzt nach den ViewCursur auf die Position im Dokument zurück, an der er
     * vor dem letzten Aufruf der focus()-Methode war.
     * 
     * @param doc
     */
    public abstract void unfocus();
  }

  private static abstract class BasicFormField implements FormField
  {
    private XTextDocument doc;

    private InsertFormValue cmd;

    private final XTextRange focusRange;

    /**
     * Enthält die TextRange des viewCursors, bevor die focus()-Methode
     * aufgerufen wurde.
     */
    private XTextRange oldFocusRange = null;

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
    public BasicFormField(XTextDocument doc, InsertFormValue cmd,
        XTextRange focusRange)
    {
      this.doc = doc;
      this.cmd = cmd;
      this.focusRange = focusRange;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#hasChangedPreviously()
     */
    public boolean hasChangedPreviously()
    {
      String value = getValue();
      String lastSetMD5 = cmd.getMD5();
      if (value != null && lastSetMD5 != null)
      {
        String md5 = getMD5HexRepresentation(value);
        return !(md5.equals(lastSetMD5));
      }
      return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#hasTrafo()
     */
    public boolean hasTrafo()
    {
      return (cmd.getTrafoName() != null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#setValue(java.lang.String,
     *      de.muenchen.allg.itd51.wollmux.func.FunctionLibrary)
     */
    public void setValue(String value, FunctionLibrary funcLib)
    {
      if (cmd.getTrafoName() != null)
      {
        Function func = funcLib.get(cmd.getTrafoName());
        if (func != null)
        {
          SimpleMap args = new SimpleMap();
          args.put("VALUE", value);
          value = func.getString(args);
        }
        else
        {
          value = "<FEHLER: TRAFO '"
                  + cmd.getTrafoName()
                  + "' nicht definiert>";
          Logger.error("Die in Kommando '"
                       + cmd
                       + " verwendete TRAFO '"
                       + cmd.getTrafoName()
                       + "' ist nicht definiert.");
        }
      }

      // md5-Wert bestimmen und setzen:
      String md5 = getMD5HexRepresentation(value);
      cmd.setMD5(md5);
      cmd.updateBookmark(false);

      // Inhalt des Textfeldes setzen:
      setFormElementValue(value);

    }

    /**
     * Diese Methode setzt den Inhalt des internen Formularelements auf den
     * neuen Wert value.
     * 
     * @param value
     *          der neue Wert des Formularelements.
     */
    public abstract void setFormElementValue(String value);

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
     * @see de.muenchen.allg.itd51.wollmux.FormField#hasChecksum()
     */
    public boolean hasChecksum()
    {
      return (cmd.getMD5() != null);
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
        oldFocusRange = cursor.getText().createTextCursorByRange(cursor);
        if (focusRange != null) cursor.gotoRange(focusRange, false);
      }
      catch (java.lang.Exception e)
      {
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormField#unfocus()
     */
    public void unfocus()
    {
      if (oldFocusRange != null)
        try
        {
          XController controller = UNO.XModel(doc).getCurrentController();
          XTextCursor cursor = UNO.XTextViewCursorSupplier(controller)
              .getViewCursor();
          cursor.gotoRange(oldFocusRange, false);
          oldFocusRange = null;
        }
        catch (java.lang.Exception e)
        {
        }
    }

    // Helper-Methoden:

    /**
     * Liefert den HEX-Wert des MD5-Hashes von string als String-Repräsentation.
     * 
     * @param string
     * @return Liefert den HEX-Wert des MD5-Hashes von string als
     *         String-Repräsentation.
     */
    private String getMD5HexRepresentation(String string)
    {
      String md5 = "";
      try
      {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] md5bytes = md.digest(string.getBytes("UTF-8"));
        for (int k = 0; k < md5bytes.length; k++)
        {
          byte b = md5bytes[k];
          String str = Integer.toHexString(b + 512);
          md5 += str.substring(1);
        }
      }
      catch (NoSuchAlgorithmException e)
      {
        Logger.error(e);
      }
      catch (UnsupportedEncodingException e)
      {
        Logger.error(e);
      }
      return md5;
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
    private XTextField inputField;

    public InputFormField(XTextDocument doc, InsertFormValue cmd,
        XTextField inputField)
    {
      super(doc, cmd, UNO.XTextContent(inputField).getAnchor());
      this.inputField = inputField;
    }

    public void setFormElementValue(String value)
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
        Object checkbox, XTextRange focusRange)
    {
      super(doc, cmd, focusRange);

      this.checkbox = checkbox;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.FormFieldFactory.BasicFormField#setFormElementValue(java.lang.String)
     */
    public void setFormElementValue(String value)
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
   * Diese Methode durchsucht das Element element dessen Kinder-Elemente, falls
   * es sich bei element um einen Container handelt, nach einem Element das den
   * Service serviceName implementiert und liefert das erste gefundene Element
   * zurück oder null, falls kein entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          das Element, das ein finales Element oder ein Container
   *          (implementiert XEnumerationAccess) sein kann.
   * @param serviceName
   *          Der Service-Name des gesuchten Elements.
   * @return Das erste gefundene Element, das serviceName implementiert.
   */
  private static XTextField findTextField(Object element, String serviceName)
  {
    // Ende, wenn es das Element selbst schon ist.
    if (UNO.XTextField(element) != null
        && UNO.supportsService(element, serviceName))
      return UNO.XTextField(element);

    // Kinder-Elemente des Containers XEnumerationAccess durchsuchen
    if (UNO.XEnumerationAccess(element) != null)
    {
      XEnumeration xEnum = UNO.XEnumerationAccess(element).createEnumeration();

      while (xEnum.hasMoreElements())
      {
        try
        {
          XTextField found = findTextField(xEnum.nextElement(), serviceName);
          // das erste gefundene Element zurückliefern.
          if (found != null) return found;
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    // Kind-Element des Property "TextField" durchsuchen:
    if (UNO.XTextField(element) != null)
    {
      Object textField = UNO.getProperty(element, "TextField");
      XTextField found = findTextField(textField, serviceName);
      if (found != null) return found;
    }

    return null;
  }
}
