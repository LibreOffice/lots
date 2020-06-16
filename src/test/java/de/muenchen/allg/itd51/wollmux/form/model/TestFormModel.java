package de.muenchen.allg.itd51.wollmux.form.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.form.config.FormConfig;

/**
 * Test für das Formular-Model.
 *
 * @author Daniel Sikeler
 */
public class TestFormModel
{
  private final String frameTitle = "libreoffice";

  private static ConfigThingy conf;
  private static Dialog dialog;
  private FormValueChangedListener valueListener = new FormValueChangedListener()
  {
    @Override
    public void valueChanged(String id, String value)
    {
    }

    @Override
    public void statusChanged(String id, boolean okay)
    {
    }
  };
  private VisibilityChangedListener visibilityListener = new VisibilityChangedListener()
  {
    @Override
    public void visibilityChanged(String id, boolean visible)
    {
    }
  };

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
  public void setUp() throws FormModelException
  {
    Map<String, String> presetValues = new HashMap<>();
    presetValues.put("EmpfaengerZeile6", "zeile6");
    FunctionLibrary funcLib = new FunctionLibrary();
    Map<Object, Object> functionContext = new HashMap<>();
    DialogLibrary dialogLib = new DialogLibrary();
    dialogLib.add("Empfaengerauswahl", dialog);
    FormConfig config = new FormConfig(conf, frameTitle);
    model = new FormModel(config, functionContext, funcLib, dialogLib, presetValues);
    model.addFormModelChangedListener(valueListener, true);
    model.addVisibilityChangedListener(visibilityListener, true);
  }

  @Test
  public void testInit() throws FormModelException
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
  public void testAutofill() throws FormModelException
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
  public void testPlausi() throws FormModelException
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
  public void testVisibility() throws FormModelException
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

}
