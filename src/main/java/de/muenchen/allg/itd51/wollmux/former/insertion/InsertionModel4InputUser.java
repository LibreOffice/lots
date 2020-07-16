/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

public class InsertionModel4InputUser extends InsertionModel
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(InsertionModel4InputUser.class);

  /**
   * Name of the variable whose value is edited by the InputUser field. This is a
   * string matching {@link TextDocumentModel#INPUT_USER_FUNCTION} and contains the name of the
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
   */
  public InsertionModel4InputUser(Object textField, XTextDocument doc,
      FunctionSelectionProvider funcSelections, FormularMax4kController formularMax4000)
      throws SyntaxErrorException
  { // TESTED
    this.formularMax4000 = formularMax4000;
    this.doc = doc;
    try
    {
      this.textField = UNO.XTextContent(textField);
      this.name = UnoProperty.getProperty(textField, UnoProperty.CONTENT).toString();
    }
    catch (Exception x)
    {
      throw new SyntaxErrorException(x);
    }
    Matcher m = TextDocumentModel.INPUT_USER_FUNCTION.matcher(name);

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

  @Override
  public String getName()
  {
    return name;
  }

  @Override
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
   */
  private void rename(String newName) throws Exception
  {
    // The following is important to avoid nuking the field master
    if (name.equals(newName)) return;

    String content = "";
    try
    {
      content = UnoProperty.getProperty(getFieldMaster(), UnoProperty.CONTENT).toString();
    }
    catch (Exception x)
    {}

    XPropertySet master = UNO.XPropertySet(UnoService.createService(UnoService.CSS_TEXT_FIELD_MASTER_USER, doc));
    UnoProperty.setProperty(master, UnoProperty.VALUE, Integer.valueOf(0));
    UnoProperty.setProperty(master, UnoProperty.NAME, newName);
    UnoProperty.setProperty(master, UnoProperty.CONTENT, content);

    UnoProperty.setProperty(textField, UnoProperty.CONTENT, newName);
    try
    {
      getFieldMaster().dispose(); // clean up old field master
    }
    finally
    {
      name = newName;
    }
  }

  @Override
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

  @Override
  public boolean updateDocument(
      Map<String, ConfigThingy> mapFunctionNameToConfigThingy)
  { // TESTED
    ConfigThingy conf = new ConfigThingy("WM");

    if (trafo.isNone())
    {
      try
      {
        trafo.setExpertFunction(new ConfigThingy("TRAFO", "\"\""));
      }
      catch (Exception x)
      {}
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
      LOGGER.error("", x);
      return false;
    }
  }

}
