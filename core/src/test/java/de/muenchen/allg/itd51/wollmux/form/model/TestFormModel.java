/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.form.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.form.config.FormConfig;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.test.OfficeTest;

/**
 * Test für das Formular-Model.
 *
 * @author Daniel Sikeler
 */
public class TestFormModel extends OfficeTest
{
  private final String frameTitle = "libreoffice";

  private static ConfigThingy conf;
  private static Dialog dialog;

  private XTextDocument doc;
  private FormModel model;

  @BeforeAll
  public static void beforeClass() throws MalformedURLException, IOException, SyntaxErrorException
  {
    conf = new ConfigThingy("WM",
        TestFormModel.class.getResource("abtretungserklaerung.conf"));
    dialog = new Dialog()
    {

      @Override
      public void show(ActionListener dialogEndListener, FunctionLibrary funcLib,
          DialogLibrary dialogLib)
      {
      }

      @Override
      public Dialog instanceFor(Map<Object, Object> context)
      {
        return this;
      }

      @Override
      public Collection<String> getSchema()
      {
        return Collections.emptyList();
      }

      @Override
      public Object getData(String id)
      {
        if ("EmpfaengerZeile1".equals(id))
          return "zeile1";
        if ("EmpfaengerZeile2".equals(id))
          return "zeile2";
        if ("EmpfaengerZeile3".equals(id))
          return "zeile3";
        if ("EmpfaengerZeile4".equals(id))
          return "";
        if ("EmpfaengerZeile5".equals(id))
          return "";
        if ("EmpfaengerZeile6".equals(id))
          return "";
        return null;
      }
    };
  }

  @BeforeEach
  public void setUp() throws FormModelException, UnoHelperException
  {
    Map<String, String> presetValues = new HashMap<>();
    presetValues.put("EmpfaengerZeile6", "zeile6");
    FunctionLibrary funcLib = new FunctionLibrary();
    Map<Object, Object> functionContext = new HashMap<>();
    DialogLibrary dialogLib = new DialogLibrary();
    dialogLib.add("Empfaengerauswahl", dialog);
    FormConfig config = new FormConfig(conf, frameTitle);
    URL file = TestFormModel.class.getResource("FormGuiTest.odt");
    doc = UNO.XTextDocument(loadComponent(file.toString(), false, true));
    TextDocumentController txtController = new TextDocumentController(
        new TextDocumentModel(doc, DocumentManager.createPersistentDataContainer(doc)),
        GlobalFunctions.getInstance().getGlobalFunctions(), GlobalFunctions.getInstance().getFunctionDialogs());
    model = new FormModel(config, functionContext, funcLib, dialogLib, presetValues, txtController);
  }

  @Test
  void testInit() throws FormModelException
  {
    assertEquals("zeile6", model.getValue("EmpfaengerZeile6"),
        "Falscher gesetzter Wert für EmpfaengerZeile6");
    assertTrue(model.getStatus("EmpfaengerZeile6"), "Falscher Status für EmpfaengerZeile6");
    assertEquals("Arbeitgeber", model.getValue("ArbeitgeberDienstherren"));
    assertFalse(model.getStatus("SGVorname"), "Falscher Status für SGVorname");
    assertTrue(model.getGroup("AbtretungNotOK").isVisible(), "AbtretungNotOK");
    assertFalse(model.getGroup("AbtretungOK").isVisible(), "AbtretungOK");
  }

  @Test
  void testAutofill() throws FormModelException
  {
    model.setDialogAutofills("Empfaengerauswahl");
    assertEquals("zeile1", model.getValue("EmpfaengerZeile1"),
        "flascher Wert für EmpfaengerZeile1");
    assertEquals("zeile2", model.getValue("EmpfaengerZeile2"),
        "flascher Wert für EmpfaengerZeile2");
    assertEquals("zeile3", model.getValue("EmpfaengerZeile3"),
        "flascher Wert für EmpfaengerZeile3");
    assertEquals("", model.getValue("EmpfaengerZeile4"), "flascher Wert für EmpfaengerZeile4");
    assertEquals("", model.getValue("EmpfaengerZeile5"), "flascher Wert für EmpfaengerZeile5");
    assertEquals("", model.getValue("EmpfaengerZeile6"), "flascher Wert für EmpfaengerZeile6");
    model.setValue("DarlBetrag", "10000");
    char decimalPoint = ((DecimalFormat) NumberFormat.getInstance()).getDecimalFormatSymbols()
        .getDecimalSeparator();
    assertEquals("12000" + decimalPoint + "00", model.getValue("DarlehenplusZusatzkosten"),
        "wrong autofill for DarlehenplusZusatzkosten");
  }

  @Test
  void testPlausi() throws FormModelException
  {
    String field = "SGVorname";
    model.setValue(field, "test");
    assertTrue(model.getStatus(field), "Falscher Status für " + field);
    assertEquals("test", model.getValue(field), "Falscher Wert für " + field);
    model.setValue(field, "");
    assertFalse(model.getStatus(field), "Falscher Status für " + field);
    assertEquals("", model.getValue(field), "Falscher Wert für " + field);
  }

  @Test
  void testVisibility() throws FormModelException
  {
    assertTrue(model.getGroup("AbtretungNotOK").isVisible(), "AbtretungNotOK");
    assertFalse(model.getGroup("AbtretungOK").isVisible(), "AbtretungOK");

    Control c = model.getControl("twoGroups");
    model.setValue("AbtLohn", "true");
    assertFalse(model.getGroup("AbtretungNotOK").isVisible(), "AbtretungNotOK");
    assertTrue(model.getGroup("AbtretungOK").isVisible(), "AbtretungOK");
    assertTrue(model.getGroup("AbtLohn").isVisible(), "AbtLohn");

    assertFalse(c.isVisible(), "Control is visible");
    model.setValue("AbtAnteile", "true");
    assertTrue(c.isVisible(), "Control is visible");

    model.setValue("AbtLohn", "");
    assertFalse(model.getGroup("AbtLohn").isVisible(), "AbtLohn");
    assertFalse(c.isVisible(), "Control is visible");
  }

  @AfterEach
  public void tearDown() throws Exception
  {
    UNO.XCloseable(doc).close(false);
  }

}
