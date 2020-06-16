package de.muenchen.allg.itd51.wollmux.form.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class TestVisibilityGroupConfig
{

  @Test
  public void testVisibilityConfig() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("Test", "\"true\"");
    VisibilityGroupConfig group = new VisibilityGroupConfig(conf);
    assertEquals("Test", group.getGroupId());
    assertEquals(conf, group.getCondition());
  }

}
