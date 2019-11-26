package de.muenchen.allg.itd51.wollmux.core.form.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;

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

  @BeforeClass
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

  @Before
  public void setUp() throws FormModelException
  {
    Map<String, String> presetValues = new HashMap<>();
    presetValues.put("EmpfaengerZeile6", "zeile6");
    FunctionLibrary funcLib = new FunctionLibrary();
    Map<Object, Object> functionContext = new HashMap<>();
    DialogLibrary dialogLib = new DialogLibrary();
    dialogLib.add("Empfaengerauswahl", dialog);
    model = new FormModel(conf, frameTitle, functionContext, funcLib, dialogLib, presetValues);
    model.addFormModelChangedListener(valueListener, true);
    model.addVisibilityChangedListener(visibilityListener, true);
  }

  @Test
  public void testInit() throws FormModelException
  {
    assertEquals("Falscher Titel", frameTitle + " - Abtretungserklärung", model.getTitle());
    assertEquals("Falsche Plausi Frabe", Color.PINK, model.getPlausiMarkerColor());
    assertEquals("Falsche Anzahl an Tabs", 4, model.getTabs().size());
    assertEquals("Falscher gesetzter Wert für EmpfaengerZeile6", "zeile6",
        model.getValue("EmpfaengerZeile6"));
    assertTrue("Falscher Status für EmpfaengerZeile6", model.getStatus("EmpfaengerZeile6"));
    assertFalse("Falscher Status für SGVorname", model.getStatus("SGVorname"));
    assertTrue("AbtretungNotOK", model.getGroup("AbtretungNotOK").isVisible());
    assertFalse("AbtretungOK", model.getGroup("AbtretungOK").isVisible());
  }

  @Test
  public void testAutofill() throws FormModelException
  {
    assertTrue("Empfaengerauswahltab fehlt", model.getTabs().containsKey("Empfaengerauswahl"));
    Tab tab = model.getTabs().get("Empfaengerauswahl");
    assertEquals("Tab 1 falsche Anzahl an Controls", 11, tab.getControls().size());
    model.setDialogAutofills("Empfaengerauswahl");
    assertEquals("flascher Wert für EmpfaengerZeile1", "zeile1",
        model.getValue("EmpfaengerZeile1"));
    assertEquals("flascher Wert für EmpfaengerZeile2", "zeile2",
        model.getValue("EmpfaengerZeile2"));
    assertEquals("flascher Wert für EmpfaengerZeile3", "zeile3",
        model.getValue("EmpfaengerZeile3"));
    assertEquals("flascher Wert für EmpfaengerZeile4", "", model.getValue("EmpfaengerZeile4"));
    assertEquals("flascher Wert für EmpfaengerZeile5", "", model.getValue("EmpfaengerZeile5"));
    assertEquals("flascher Wert für EmpfaengerZeile6", "", model.getValue("EmpfaengerZeile6"));
  }

  @Test
  public void testPlausi() throws FormModelException
  {
    String field = "SGVorname";
    model.setValue(field, "test");
    assertTrue("Falscher Status für " + field, model.getStatus(field));
    assertEquals("Falscher Wert für " + field, "test", model.getValue(field));
    model.setValue(field, "");
    assertFalse("Falscher Status für " + field, model.getStatus(field));
    assertEquals("Falscher Wert für " + field, "", model.getValue(field));
  }

  @Test
  public void testVisibility() throws FormModelException
  {
    model.setValue("AbtLohn", "true");
    assertFalse("AbtretungNotOK", model.getGroup("AbtretungNotOK").isVisible());
    assertTrue("AbtretungOK", model.getGroup("AbtretungOK").isVisible());
    assertTrue("AbtLohn", model.getGroup("AbtLohn").isVisible());
    model.setValue("AbtLohn", "");
    assertTrue("AbtretungNotOK", model.getGroup("AbtretungNotOK").isVisible());
    assertFalse("AbtretungOK", model.getGroup("AbtretungOK").isVisible());
    assertFalse("AbtLohn", model.getGroup("AbtLohn").isVisible());
  }

}
