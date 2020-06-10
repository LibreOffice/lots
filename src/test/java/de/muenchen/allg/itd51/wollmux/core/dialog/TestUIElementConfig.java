package de.muenchen.allg.itd51.wollmux.core.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class TestUIElementConfig
{

  @Test
  public void testControl() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("",
        "TYPE \"LABEL\" ID \"id\" LABEL \"label\" TIP \"tip\" HOTKEY \"key\" ACTION \"action\" DIALOG \"dialog\" "
            + "WINDOW \"window\" FRAG_ID \"id1\" FRAG_ID \"id2\" EXT \"ext\" URL \"url\" READONLY \"false\" EDIT \"false\" "
            + "LINES \"3\" WRAP \"false\" MINSIZE \"1\" PREFSIZE \"1\" MAXSIZE \"1\" VALUES(\"value1\" \"value2\") "
            + "GROUPS(\"group1\" \"group2\") MENU \"menu\" SIDEBAR \"false\"");
    ConfigThingy plausiConf = new ConfigThingy("PLAUSI", "\"plausi\"");
    conf.addChild(plausiConf);
    ConfigThingy autofillConf = new ConfigThingy("AUTOFILL", "\"autofill\"");
    conf.addChild(autofillConf);
    UIElementConfig control = new UIElementConfig(conf);
    assertEquals("label", control.getLabel());
    assertEquals("tip", control.getTip());
    assertEquals(List.of("value1", "value2"), control.getOptions());
    assertEquals(3, control.getLines());
    assertFalse(control.isWrap());
    assertFalse(control.isReadonly());
    assertFalse(control.isEditable());
    assertEquals(1, control.getMinsize());
    assertEquals('K', control.getHotkey());
    assertEquals("action", control.getAction());
    assertEquals("dialog", control.getDialog());
    assertEquals("ext", control.getExt());
    assertEquals(1, control.getPrefsize());
    assertEquals(1, control.getMaxsize());
    assertEquals("window", control.getWindow());
    assertEquals("id1&id2", control.getFragId());
    assertEquals("url", control.getUrl());
    assertEquals("id", control.getId());
    assertEquals(UIElementType.LABEL, control.getType());
    assertEquals(plausiConf, control.getPlausi().getFirstChild());
    assertEquals(autofillConf, control.getAutofill().getFirstChild());
    assertEquals(List.of("group1", "group2"), control.getGroups());
    assertEquals("menu", control.getMenu());
    assertFalse(control.isSidebar());
  }

}
