/*
 * Dateiname: InsertionModel4InputUser.java
 * Projekt  : n/a
 * Funktion : TODO Funktionsbeschreibung
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0.
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
 * 30.06.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;

public class InsertionModel4InputUser extends InsertionModel
{
  /**
   * Pattern zum Erkennen von InputUser-Feldern, die eine WollMux-Funktion
   * referenzieren (z.B. die Spezialfelder des WollMux-Seriendrucks).
   */
  public static final Pattern INPUT_USER_FUNCTION =
    Pattern.compile("\\A\\s*(WM\\s*\\(.*FUNCTION\\s*'[^']*'.*\\))\\s*\\d*\\z");

  /**
   * Name of the variable whose value is edited by the InputUser field. This is a
   * string matching {@link #INPUT_USER_FUNCTION} and contains the name of the
   * function used to compute the field by the WollMux.
   */
  private String name;

  /**
   * The InputUser textfield this insertion refers to.
   */
  private XTextContent textField;

  /**
   * The document that contains the insertion.
   */
  private XTextDocument doc;

  /**
   * Erzeugt ein neues InsertionModel für das InputUser Textfeld textField.
   * 
   * @param doc
   *          das Dokument in dem sich das InputUser-Feld befindet
   * @param funcSelections
   *          ein FunctionSelectionProvider, der für das Feld eine passende
   *          FunctionSelection liefern kann.
   * @param formularMax4000
   *          Der FormularMax4000 zu dem dieses InsertionModel gehört.
   * @throws SyntaxErrorException
   *           wenn das Content-Property von textField nicht korrekte
   *           ConfigThingy-Syntax hat oder keine korrekte Funktionsreferenz ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public InsertionModel4InputUser(Object textField, XTextDocument doc,
      FunctionSelectionProvider funcSelections, FormularMax4000 formularMax4000)
      throws SyntaxErrorException
  { // TESTED
    this.formularMax4000 = formularMax4000;
    this.doc = doc;
    try
    {
      this.textField = UNO.XTextContent(textField);
      this.name = UNO.getProperty(textField, "Content").toString();
    }
    catch (Exception x)
    {
      throw new SyntaxErrorException(x);
    }
    Matcher m = INPUT_USER_FUNCTION.matcher(name);

    if (!m.matches()) throw new SyntaxErrorException();
    String confStr = m.group(1);
    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("INSERT", confStr);
    }
    catch (IOException x)
    {
      throw new SyntaxErrorException(x);
    }

    ConfigThingy trafoConf = conf.query("FUNCTION");
    if (trafoConf.count() != 1)
      throw new SyntaxErrorException();
    else
    {
      String functionName = trafoConf.toString();
      this.trafo = funcSelections.getFunctionSelection(functionName);
    }
  }

  public String getName()
  {
    return name;
  }

  public void removeFromDocument()
  {
    try
    {
      textField.getAnchor().setString("");
    }
    catch (Exception x)
    {}
    try
    {
      getFieldMaster().dispose();
    }
    catch (Exception x)
    {}
  }

  private XComponent getFieldMaster() throws NoSuchElementException
  { // TESTED
    String fieldMasterName = "com.sun.star.text.fieldmaster.User." + name;
    try
    {
      return UNO.XComponent(UNO.XTextFieldsSupplier(doc).getTextFieldMasters().getByName(
        fieldMasterName));
    }
    catch (Exception x)
    {
      throw new NoSuchElementException(L.m("FieldMaster \"%1\" existiert nicht.",
        fieldMasterName));
    }
  }

  /**
   * Erzeugt einen neuen FieldMaster mit Namen newName und biegt dieses
   * InputUser-Feld darauf um.
   * 
   * @throws Exception
   *           falls was schief geht.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  private void rename(String newName) throws Exception
  {
    // The following is important to avoid nuking the field master
    if (name.equals(newName)) return;

    String content = "";
    try
    {
      content = UNO.getProperty(getFieldMaster(), "Content").toString();
    }
    catch (Exception x)
    {}

    XPropertySet master =
      UNO.XPropertySet(UNO.XMultiServiceFactory(doc).createInstance(
        "com.sun.star.text.FieldMaster.User"));
    UNO.setProperty(master, "Value", new Integer(0));
    UNO.setProperty(master, "Name", newName);
    UNO.setProperty(master, "Content", content);

    UNO.setProperty(textField, "Content", newName);
    try
    {
      getFieldMaster().dispose(); // clean up old field master
    }
    finally
    {
      name = newName;
    }
  }

  public void selectWithViewCursor()
  { // TESTED
    try
    {
      XTextRange anchor = textField.getAnchor();
      XTextRange cursor = anchor.getText().createTextCursorByRange(anchor);
      UNO.XTextViewCursorSupplier(UNO.XModel(doc).getCurrentController()).getViewCursor().gotoRange(
        cursor, false);
    }
    catch (java.lang.Exception x)
    {}
  }

  public boolean updateDocument(
      Map<String, ConfigThingy> mapFunctionNameToConfigThingy)
  { // TESTED
    ConfigThingy conf = new ConfigThingy("WM");

    if (trafo.isNone())
    {
      ConfigThingy expertConf = null;
      try
      {
        expertConf = new ConfigThingy("TRAFO", "\"\"");
      }
      catch (Exception x)
      {}
      trafo.setExpertFunction(expertConf);
    }

    // Falls eine externe Funktion referenziert wird, ohne dass irgendwelche
    // ihrer Parameter gebunden wurden, dann nehmen wir direkt den
    // Original-Funktionsnamen für das TRAFO-Attribut ...
    if (trafo.isReference() && !trafo.hasSpecifiedParameters())
    {
      conf.add("FUNCTION").add(trafo.getFunctionName());
    }
    else
    // ... ansonsten müssen wir eine neue Funktion machen.
    {
      int count = 1;
      String funcName;
      do
      {
        funcName =
          FM4000AUTO_GENERATED_TRAFO + (count++) + "_" + System.currentTimeMillis();
      } while (mapFunctionNameToConfigThingy.containsKey(funcName));

      conf.add("FUNCTION").add(funcName);
      mapFunctionNameToConfigThingy.put(funcName, trafo.export(funcName));
    }

    String newName = conf.stringRepresentation(false, '\'', false);
    try
    {
      rename(newName);
      return true;
    }
    catch (Exception x)
    {
      Logger.error(x);
      return false;
    }
  }

}
