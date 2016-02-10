package de.muenchen.allg.itd51.wollmux.sidebar.controls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementContext;
import de.muenchen.allg.itd51.wollmux.dialog.WollMuxBarConfig;
import de.muenchen.allg.itd51.wollmux.dialog.WollMuxBarEventHandler;

/**
 * Erzeugt {@link UIControl}s für Menüs und Buttons, die in der WollMux-Konfiguration
 * definiert sind. UIControls werden verwendet, um die Daten der Steuerelemente von
 * der Implementation zu trennen. Auf diese Weise können aus der selben Konfiguration
 * wahlweise z.B. Swing-Objekte oder XControls erzeugt werden. Über
 * {@link UIElementCreateListener} können konkrete Implementationen auf die Erzeugung
 * eines UIControls reagieren.
 * 
 * @author andor.ertsey
 * 
 */
public class UIFactory
{
  private static final String EDIT = "EDIT";

  private static final String READONLY = "READONLY";

  private static final String ACTION = "ACTION";

  private static final String HOTKEY = "HOTKEY";

  private static final String TYPE = "TYPE";

  private static final String ID = "ID";

  private static final String TIP = "TIP";

  private static final String LABEL = "LABEL";

  private static final String MENU = "MENU";

  private List<UIElementCreateListener> listeners;

  private WollMuxBarConfig config;

  private WollMuxSidebarUIElementEventHandler eventHandler;

  public static final Set<String> SUPPORTED_ACTIONS = new HashSet<String>();
  static
  {
    SUPPORTED_ACTIONS.add("openTemplate");
    SUPPORTED_ACTIONS.add("absenderAuswaehlen");
    SUPPORTED_ACTIONS.add("openDocument");
    SUPPORTED_ACTIONS.add("openExt");
    SUPPORTED_ACTIONS.add("open");
    SUPPORTED_ACTIONS.add("dumpInfo");
    SUPPORTED_ACTIONS.add("abort");
    SUPPORTED_ACTIONS.add("kill");
    SUPPORTED_ACTIONS.add("about");
    SUPPORTED_ACTIONS.add("menuManager");
    SUPPORTED_ACTIONS.add("options");
  }

  public UIFactory(WollMuxBarConfig config)
  {
    this.config = config;
    listeners = new ArrayList<UIElementCreateListener>();

    WollMuxBarEventHandler wmEventHandler = new WollMuxBarEventHandler(null);
    wmEventHandler.start();
    eventHandler = new WollMuxSidebarUIElementEventHandler(wmEventHandler);
  }

  /**
   * Die Funktion wird aufgerufen, um einen Bereich der Konfiguration zu parsen und
   * {@link UIControl}s zu erzeugen. Für jedes erzeugte Element werden alle Listener
   * aufgerufen.
   * 
   * @param context
   * @param menuConf
   * @param elementParent
   *          Die Konfiguration, die geparst werden soll. Muss eine Liste von Menüs
   *          oder Buttons enthalten.
   * @param isMenu
   *          Wenn true, werden button und menuitem in {@link UIMenuItem}
   *          umgewandelt, sonst in {@link UIButton}.
   */
  public void createUIElements(UIElementContext context, ConfigThingy menuConf,
      ConfigThingy elementParent, boolean isMenu)
  {
    for (ConfigThingy uiElementDesc : elementParent)
    {
      /*
       * Falls kein CONF_ID vorhanden ist, wird das Element angezeigt, ansonsten nur
       * dann wenn mindestens eine CONF_ID aktiv ist.
       */
      ConfigThingy conf_ids = uiElementDesc.query("CONF_ID");
      if (conf_ids.count() > 0)
      {
        boolean active = false;
        for (ConfigThingy conf_id_group : conf_ids)
          for (ConfigThingy conf_id : conf_id_group)
            if (config.isIDActive(conf_id.getName()))
            {
              active = true;
              break;
            }
        if (!active) continue;
      }

      // String type;
      // try
      // {
      // type = uiElementDesc.get(TYPE).toString();
      // }
      // catch (NodeNotFoundException e)
      // {
      // Logger.error(L.m("Ein User Interface Element ohne TYPE wurde entdeckt"));
      // continue;
      // }

      if (isMenu)
      {
        UIControl<?> control =
          createUIMenuElement(context, uiElementDesc, menuConf.getName());
        fireElementCreated(control);
      }
      else
      {
        UIControl<?> control = createUIElement(context, uiElementDesc);
        fireElementCreated(control);
      }
    }
  }

  private UIControl<?> createUIMenuElement(UIElementContext context,
      ConfigThingy conf, String parent)
  {
    Map<String, String> props = parseElementConf(conf);

    String type = props.get(TYPE);

    if (type.isEmpty())
      throw new ConfigurationErrorException(L.m(
        "TYPE-Angabe fehlt bei Element mit Label \"%1\"", props.get(LABEL)));

    UIControl<?> uiElement = null;

    if (type.equals("button") || type.equals("menuitem"))
    {
      UIElementAction action = null;
      if (props.get(ACTION) != null)
      {
        action = new UIElementAction(eventHandler, false, "action", new Object[] {
          props.get(ACTION), conf });
      }

      uiElement = new UIMenuItem(props.get(ID), props.get(LABEL), parent, action);

      if (action != null)
      {
        action.setControl(uiElement);
      }
    }

    return uiElement;
  }

  private UIControl<?> createUIElement(UIElementContext context, ConfigThingy conf)
  {
    Map<String, String> props = parseElementConf(conf);

    String type = props.get(TYPE);

    if (type.isEmpty())
      throw new ConfigurationErrorException(L.m(
        "TYPE-Angabe fehlt bei Element mit Label \"%1\"", props.get(LABEL)));

    UIControl<?> uiElement = null;

    if (type.equals("button") || type.equals("menuitem"))
    {
      UIElementAction action =
        new UIElementAction(eventHandler, false, "action", new Object[] {
          props.get(ACTION), conf });
      uiElement = new UIButton(props.get(ID), props.get(LABEL), action);
      action.setControl(uiElement);
    }
    else if (type.equals("menu"))
    {
      UIElementAction action = null;
      if (props.get(ACTION) != null)
      {
        action = new UIElementAction(eventHandler, false, "action", new Object[] {
          props.get(ACTION), conf });
      }

      uiElement = new UIMenu(props.get(MENU), props.get(LABEL), action);

      if (action != null)
      {
        action.setControl(uiElement);
      }
    }
    else if (type.equals("senderbox"))
    {
      String label = L.m("Bitte warten...");
      uiElement = new UISenderbox(props.get(ID), label, null);
    }
    else if (type.equals("searchbox"))
    {
      uiElement = new UISearchbox(props.get(ID), "", null);
    }

    return uiElement;
  }

  private Map<String, String> parseElementConf(ConfigThingy conf)
  {
    Map<String, String> props = new HashMap<String, String>();

    for (ConfigThingy node : conf)
    {
      String name = node.getName();
      String str = node.toString();

      if (name.equals(LABEL) || name.equals(TIP))
        props.put(name, L.m(str));
      else
        props.put(name, str);
    }

    return props;
  }

  public void addElementCreateListener(UIElementCreateListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  public void removeElementCreateListener(UIElementCreateListener listener)
  {
    if (listeners.contains(listener)) listeners.remove(listener);
  }

  protected void fireElementCreated(UIControl<?> control)
  {
    for (UIElementCreateListener listener : listeners)
    {
      listener.createControl(control);
    }
  }
}
