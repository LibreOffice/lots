package de.muenchen.allg.itd51.wollmux.form.config;

import java.util.ArrayList;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.form.model.FormModelException;

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
    title = tabConf.getString("TITLE", "");
    closeAction = tabConf.getString("CLOSEACTION", "abort");
    tip = tabConf.getString("TIP", "");
    String hotkeyString = tabConf.getString("HOTKEY", "");
    hotkey = 0;
    if (!hotkeyString.isEmpty())
    {
      hotkey = hotkeyString.toUpperCase().charAt(0);
    }
    ConfigThingy inputFields = tabConf.query("Eingabefelder");
    for (ConfigThingy fields : inputFields)
    {
      for (ConfigThingy controlConf : fields)
      {
        controls.add(new UIElementConfig(controlConf));
      }
    }

    ConfigThingy buttonConf = tabConf.query("Buttons");
    for (ConfigThingy fields : buttonConf)
    {
      for (ConfigThingy controlConf : fields)
      {
        buttons.add(new UIElementConfig(controlConf));
      }
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
