package de.muenchen.allg.itd51.wollmux.form.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class TestTabConfig
{

  @Test
  public void testTabConfig() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("Tab",
        "ID \"id\" TITLE \"title\" TIP \"tip\" HOTKEY \"key\" CLOSEACTION \"action\"");
    ConfigThingy inputConf = new ConfigThingy("Eingabefelder", "(TYPE \"LABEL\")");
    conf.addChild(inputConf);
    ConfigThingy buttonConf = new ConfigThingy("Buttons", "(TYPE \"LABEL\")");
    conf.addChild(buttonConf);
    TabConfig tab = new TabConfig(conf);
    assertEquals("Tab", tab.getId());
    assertEquals("tip", tab.getTip());
    assertEquals('K', tab.getHotkey());
    assertEquals("title", tab.getTitle());
    assertEquals("action", tab.getCloseAction());
    assertFalse(tab.getControls().isEmpty());
    assertFalse(tab.getButtons().isEmpty());
  }

}
