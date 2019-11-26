package de.muenchen.allg.itd51.wollmux.core.form.model;

import java.util.ArrayList;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

/**
 * Ein Tab im Formular-Model.
 * 
 * @author daniel.sikeler
 *
 */
public class Tab
{
  /**
   * Die Id.
   */
  private String id;

  /**
   * Der Titel.
   */
  private String title;

  /**
   * Die Aktion, die beim Schließen ausgeführt werden soll.
   */
  private String closeAction;

  /**
   * Ein Tipp, kann null sein.
   */
  private String tip;

  /**
   * Ein Hotkey, kann null sein.
   */
  private char hotkey;

  /**
   * Liste der enthaltenen Formularelemente.
   */
  private List<Control> controls = new ArrayList<>();

  /**
   * Liste der Elemente in der Button-Zeile.
   */
  private List<Control> buttons = new ArrayList<>();

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

  public List<Control> getControls()
  {
    return controls;
  }

  public List<Control> getButtons()
  {
    return buttons;
  }

  /**
   * Parst einen Tab samt Formularfelder.
   * 
   * @param tabConf
   *          Die Beschreibung der Tabs.
   * @param model
   *          Das Formular-Model.
   * @return Der neue Tab.
   * @throws FormModelException
   *           Unbekannte Type-Angabe für ein Formularfeld.
   */
  public static Tab create(ConfigThingy tabConf, FormModel model) throws FormModelException
  {
    Tab tab = new Tab();
    tab.id = tabConf.getName();
    tab.title = tabConf.getString("TITLE", "");
    tab.closeAction = tabConf.getString("CLOSEACTION", "abort");
    tab.tip = tabConf.getString("TIP", "");
    String hotkeyString = tabConf.getString("HOTKEY", "");
    tab.hotkey = 0;
    if (!hotkeyString.isEmpty())
    {
      tab.hotkey = hotkeyString.toUpperCase().charAt(0);
    }
    ConfigThingy inputFields = tabConf.query("Eingabefelder");
    for (ConfigThingy fields : inputFields)
    {
      for (ConfigThingy controlConf : fields)
      {
        Control control = Control.create(controlConf, model);
        tab.controls.add(control);
      }
    }

    ConfigThingy buttons = tabConf.query("Buttons");
    for (ConfigThingy fields : buttons)
    {
      for (ConfigThingy controlConf : fields)
      {
        tab.buttons.add(Control.create(controlConf, model));
      }
    }
    return tab;
  }
}
