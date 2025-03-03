/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.form.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.form.config.FormConfig;
import org.libreoffice.lots.form.model.FormModelException;

public class TestFormConfig
{

  @Test
  public void testFormConfig() throws Exception
  {
    ConfigThingy conf = new ConfigThingy("Tab",
        "TITLE \"title\" PLAUSI_MARKER_COLOR \"#0000FF\" HOTKEY \"key\" CLOSEACTION \"action\"");
    ConfigThingy tabConf = new ConfigThingy("Fenster",
        "Tab(InputFields((TYPE \"LABEL\")) Buttons((TYPE \"LABEL\")))");
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
        "Tab(InputFields((TYPE \"LABEL\")) Buttons((TYPE \"LABEL\")))");
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
        "Tab(InputFields((TYPE \"LABEL\")) Buttons((TYPE \"LABEL\")))");
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
