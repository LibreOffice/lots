/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.form.config;

import java.util.ArrayList;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.former.FormMaxConstants;
import de.muenchen.allg.itd51.wollmux.ui.UIElementType;
import de.muenchen.allg.itd51.wollmux.ui.UIElementConfig;

/**
 * A tab of a form.
 */
public class TabConfig
{
  /**
   * The unique id.
   */
  private String id;

  /**
   * The title of the tab.
   */
  private String title;

  /**
   * The action on closing the tab.
   */
  private String closeAction;

  /**
   * A tip to be shown on hover.
   */
  private String tip;

  /**
   * A hot key, can be null.
   */
  private char hotkey;

  /**
   * List of input controls.
   */
  private List<UIElementConfig> controls = new ArrayList<>();

  /**
   * List of buttons below the input controls.
   */
  private List<UIElementConfig> buttons = new ArrayList<>();

  /**
   * A new tab configuration.
   *
   * @param tabConf
   *          The description of the tab.
   * @throws FormModelException
   *           Invalid control on the tab.
   */
  public TabConfig(ConfigThingy tabConf) throws FormModelException
  {
    id = tabConf.getName();
    title = tabConf.getString(FormMaxConstants.TITLE, "");
    closeAction = tabConf.getString("CLOSEACTION", "abort");
    tip = tabConf.getString("TIP", "");
    String hotkeyString = tabConf.getString("HOTKEY", "");
    hotkey = 0;
    if (!hotkeyString.isEmpty())
    {
      hotkey = hotkeyString.toUpperCase().charAt(0);
    }

    try
    {
      ConfigThingy inputFields = tabConf.query(FormMaxConstants.INPUT_FIELDS);
      for (ConfigThingy fields : inputFields)
      {
        for (ConfigThingy controlConf : fields)
        {
          controls.add(new UIElementConfig(controlConf));
        }
      }

      ConfigThingy buttonConf = tabConf.query(FormMaxConstants.BUTTONS);
      for (ConfigThingy fields : buttonConf)
      {
        for (ConfigThingy controlConf : fields)
        {
          buttons.add(new UIElementConfig(controlConf));
        }
      }
    } catch (ConfigurationErrorException e)
    {
      throw new FormModelException("Invalid tab", e);
    }
  }

  public String getId()
  {
    return id;
  }

  public String getTitle()
  {
    return title;
  }

  public String getCloseAction()
  {
    return closeAction;
  }

  public String getTip()
  {
    return tip;
  }

  public char getHotkey()
  {
    return hotkey;
  }

  public List<UIElementConfig> getControls()
  {
    return controls;
  }

  public List<UIElementConfig> getButtons()
  {
    return buttons;
  }
}
