package de.muenchen.allg.itd51.wollmux.core.form.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.form.model.FormModelException;

public class TestFormConfig
{

  @Test
  public void testFormConfig() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("Tab",
        "TITLE \"title\" PLAUSI_MARKER_COLOR \"#0000FF\" HOTKEY \"key\" CLOSEACTION \"action\"");
    ConfigThingy tabConf = new ConfigThingy("Fenster",
        "Tab(Eingabefelder((TYPE \"LABEL\")) Buttons((TYPE \"LABEL\")))");
    conf.addChild(tabConf);
    ConfigThingy visConf = new ConfigThingy("Sichtbarkeit", "(Test \"true\")");
    conf.addChild(visConf);
    FormConfig form = new FormConfig(conf, "frame");
    assertEquals("frame - title", form.getTitle());
    assertEquals(Color.BLUE, form.getPlausiMarkerColor());
    assertEquals(1, form.getVisibilities().size());
    assertEquals(1, form.getTabs().size());
    assertEquals(2, form.getControls().count());
  }

  @Test
  public void testFormConfigWithoutFrame() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("Tab",
        "TITLE \"title\" PLAUSI_MARKER_COLOR \"#0000FF\" HOTKEY \"key\" CLOSEACTION \"action\"");
    ConfigThingy tabConf = new ConfigThingy("Fenster",
        "Tab(Eingabefelder((TYPE \"LABEL\")) Buttons((TYPE \"LABEL\")))");
    conf.addChild(tabConf);
    ConfigThingy visConf = new ConfigThingy("Sichtbarkeit", "(Test \"true\")");
    conf.addChild(visConf);
    FormConfig form = new FormConfig(conf, null);
    assertEquals("title", form.getTitle());
  }

  @Test
  public void testFormConfigWithoutVisibility() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("Tab",
        "TITLE \"title\" PLAUSI_MARKER_COLOR \"#0000FF\" HOTKEY \"key\" CLOSEACTION \"action\"");
    ConfigThingy tabConf = new ConfigThingy("Fenster",
        "Tab(Eingabefelder((TYPE \"LABEL\")) Buttons((TYPE \"LABEL\")))");
    conf.addChild(tabConf);
    FormConfig form = new FormConfig(conf, null);
    assertTrue(form.getVisibilities().isEmpty());
  }

  @Test
  public void testFormConfigInvalid() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("Tab",
        "TITLE \"title\" PLAUSI_MARKER_COLOR \"#0000FF\" HOTKEY \"key\" CLOSEACTION \"action\"");
    assertThrows(FormModelException.class, () -> new FormConfig(conf, null));
  }

}
