/*
 * Dateiname: FormField.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert ein Formularfeld innerhalb des Dokuments unter
 *            einem WM('insertFormValue'...)-Bookmark.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 08.06.2006 | LUT | Erstellung als FormField
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
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values.SimpleMap;

/**
 * Diese Klasse repräsentiert ein Formularfeld im Dokument an der Stelle eines
 * insertFormValue-Kommandos.
 * 
 * @author lut
 */
public class FormField
{
  XTextDocument doc;

  InsertFormValue cmd;

  UnoService inputField;

  /**
   * Enthält die TextRange des viewCursors, bevor die focus()-Methode aufgerufen
   * wurde.
   */
  XTextRange oldFocusRange = null;

  /**
   * Erzeugt ein Formualfeld im Dokument doc an der Stelle des
   * InsertFormValue-Kommandos cmd. Ist unter dem bookmark bereits ein TextFeld
   * vom Typ InputField vorhanden, so wird dieses Feld als inputField für die
   * Darstellung des Wertes des Formularfeldes genutzt. Ist innerhalb des
   * Bookmarks noch kein InputField vorhanden, so wird ein neues InputField in
   * den Bookmark eingefügt.
   * 
   * @param doc
   *          das Dokument, zu dem das Formularfeld gehört.
   * @param cmd
   *          das zugehörige insertFormValue-Kommando.
   */
  public FormField(XTextDocument doc, InsertFormValue cmd)
  {
    this.doc = doc;
    this.cmd = cmd;
    this.inputField = new UnoService(null);

    // Textfeld suchen:
    XTextRange range = cmd.getTextRange();
    if (range != null)
    {
      UnoService cursor = new UnoService(range.getText()
          .createTextCursorByRange(range));
      inputField = new UnoService(findTextFieldRecursive(
          cursor,
          "com.sun.star.text.TextField.Input"));
    }

    // wenn kein Textfeld vorhanden ist, wird eines neu erstellt
    if (inputField.xTextField() == null)
    {
      Logger.debug2(cmd + ": Erzeuge neues Input-Field.");
      try
      {
        inputField = new UnoService(doc)
            .create("com.sun.star.text.TextField.Input");
        XTextCursor cursor = cmd.createInsertCursor();
        if (cursor != null)
        {
          cursor.getText().insertTextContent(
              cursor,
              inputField.xTextContent(),
              true);
        }
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Gibt an, ob der Inhalt des InputFields mit der zuletzt gesetzten MD5-Sum
   * des Feldes übereinstimmt oder ob der Wert seitdem geändert wurde (z.B.
   * durch manuelles Setzen des Feldinhalts durch den Benutzer). Ist noch kein
   * MD5-Vergleichswert vorhanden, so wird false zurückgeliefert.
   * 
   * @return true, wenn der Inhalt des InputFields nicht mehr mit der zuletzt
   *         gesetzten MD5-Sum übereinstimmt, ansonsten false
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

  /**
   * Die Methode liefert true, wenn das insertFormValue-Kommando eine
   * TRAFO-Funktion verwendet, um den gesetzten Wert zu transformieren,
   * ansonsten wird false zurückgegeben.
   */
  public boolean hasTrafo()
  {
    return (cmd.getTrafoName() != null);
  }

  /**
   * Diese Methode belegt den Wert des Formularfeldes im Dokument mit dem neuen
   * Inhalt value; ist das Formularfeld mit einer TRAFO belegt, so wird vor dem
   * setzen des neuen Inhaltes die TRAFO-Funktion ausgeführt und der neu
   * berechnete Wert stattdessen eingefügt.
   * 
   * @param value
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
        value = "<FEHLER: TRAFO '" + cmd.getTrafoName() + "' nicht definiert>";
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
    if (inputField.xTextField() != null && inputField.xUpdatable() != null) try
    {
      inputField.setPropertyValue("Content", value);
      inputField.xUpdatable().update();
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Diese Methode liefert den aktuellen Wert des Formularfeldes als String
   * zurück oder null, falls der Wert nicht bestimmt werden kann.
   * 
   * @return der aktuelle Wert des Formularfeldes als String
   */
  public String getValue()
  {
    try
    {
      return inputField.getPropertyValue("Content").getObject().toString();
    }
    catch (Exception e)
    {
      return null;
    }
  }

  /**
   * Liefert true, wenn das Dokumentkommando eine Checksumme (MD5) im Status
   * enthält, über die festgestellt werden kann, ob der Wert des Eingabefeldes
   * noch mit der zuletzt gesetzten Checksumme übereinstimmt (siehe
   * hasChangedPreviously())
   */
  public boolean hasChecksum()
  {
    return (cmd.getMD5() != null);
  }

  /**
   * Setzt den ViewCursor auf die Position des InputFields und merkt sich dabei
   * die letze ViewCursor-Position.
   * 
   * @param doc
   */
  public void focus()
  {
    try
    {
      UnoService document = new UnoService(doc);
      UnoService controller = new UnoService(document.xModel()
          .getCurrentController());
      XTextCursor cursor = controller.xTextViewCursorSupplier().getViewCursor();
      oldFocusRange = cursor.getText().createTextCursorByRange(cursor);
      XTextRange anchor = inputField.xTextField().getAnchor();
      cursor.gotoRange(anchor, false);
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Setzt nach den ViewCursur auf die Position im Dokument zurück, an der er
   * vor dem letzten Aufruf der focus()-Methode war.
   * 
   * @param doc
   */
  public void unfocus()
  {
    if (oldFocusRange != null)
      try
      {
        UnoService document = new UnoService(doc);
        UnoService controller = new UnoService(document.xModel()
            .getCurrentController());
        XTextCursor cursor = controller.xTextViewCursorSupplier()
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
  public static String getMD5HexRepresentation(String string)
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

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach InputFields und liefert das erste gefundene
   * InputField zurück.
   * 
   * @param element
   *          Das erste gefundene InputField.
   */
  private XTextField findTextFieldRecursive(UnoService element,
      String serviceName)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    if (element.xEnumerationAccess() != null)
    {
      XEnumeration xEnum = element.xEnumerationAccess().createEnumeration();

      while (xEnum.hasMoreElements())
      {
        try
        {
          UnoService child = new UnoService(xEnum.nextElement());
          XTextField found = findTextFieldRecursive(child, serviceName);
          // das erste gefundene Element zurückliefern.
          if (found != null) return found;
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    // jetzt noch schauen, ob es sich bei dem Element um ein InputField
    // handelt:
    if (element.xTextField() != null)
    {
      try
      {
        UnoService textField = element.getPropertyValue("TextField");
        if (textField.supportsService(serviceName))
        {
          return textField.xTextField();
        }
      }
      catch (Exception e)
      {
      }
    }

    return null;
  }
}
