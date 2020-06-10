package de.muenchen.allg.itd51.wollmux.sidebar.controls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementContext;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementType;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;

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

  private List<UIElementCreateListener> listeners;

  private WollMuxSidebarUIElementEventHandler eventHandler;

  public static final Set<String> SUPPORTED_ACTIONS = new HashSet<>();
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
    SUPPORTED_ACTIONS.add("options");
  }

  public UIFactory()
  {
    listeners = new ArrayList<>();

    eventHandler = new WollMuxSidebarUIElementEventHandler();
  }

  /**
   * Die Funktion wird aufgerufen, um einen Bereich der Konfiguration zu parsen und
   * {@link UIControl}s zu erzeugen. Für jedes erzeugte Element werden alle Listener aufgerufen.
   *
   * @param context
   * @param menuConf
   * @param elementParent
   *          Die Konfiguration, die geparst werden soll. Muss eine Liste von Menüs oder Buttons
   *          enthalten.
   * @param isMenu
   *          Wenn true, werden button und menuitem in {@link UIMenuItem} umgewandelt, sonst in
   *          {@link UIButton}.
   */
  public void createUIElements(UIElementContext context, ConfigThingy menuConf,
      ConfigThingy elementParent, boolean isMenu)
  {
    for (ConfigThingy uiElementDesc : elementParent)
    {
      UIElementConfig config = new UIElementConfig(uiElementDesc);

      if (!config.isSidebar())
      {
        continue;
      }

      if (isMenu)
      {
        UIControl<?> control =
            createUIMenuElement(context, config, menuConf.getName());
        fireElementCreated(control);
      }
      else
      {
        UIControl<?> control = createUIElement(context, config);
        fireElementCreated(control);
      }
    }
  }

  public UIControl<?> createUIMenuElement(UIElementContext context,
      UIElementConfig conf, String parent)
  {
    UIControl<?> uiElement = null;

    if (conf.getType() == UIElementType.BUTTON || conf.getType() == UIElementType.MENUITEM)
    {
      UIElementAction action = null;
      String actionName = conf.getAction();
      if (actionName != null)
      {
        action = new UIElementAction(eventHandler, false, "action", new Object[] {
            actionName, conf });
      }

      uiElement = new UIMenuItem(conf.getId(), conf.getLabel(), parent, action);

      if (action != null)
      {
        action.setControl(uiElement);
      }
    } else if (conf.getType() == UIElementType.MENU)
    {
      uiElement = new UIMenu(conf.getMenu(), conf.getLabel(), parent, null);
    }

    return uiElement;
  }

  private UIControl<?> createUIElement(UIElementContext context, UIElementConfig conf)
  {
    UIControl<?> uiElement = null;

    if (conf.getType() == UIElementType.BUTTON || conf.getType() == UIElementType.MENUITEM)
    {
      UIElementAction action =
        new UIElementAction(eventHandler, false, "action", new Object[] {
              conf.getAction(), conf });
      uiElement = new UIButton(conf.getId(), conf.getLabel(), action);
      action.setControl(uiElement);
    }
    else if (conf.getType() == UIElementType.MENU)
    {
      UIElementAction action = null;
      if (conf.getAction() != null)
      {
        action = new UIElementAction(eventHandler, false, "action", new Object[] {
            conf.getAction(), conf });
      }

      uiElement = new UIMenu(conf.getMenu(), conf.getLabel(), action);

      if (action != null)
      {
        action.setControl(uiElement);
      }
    }
    else if (conf.getType() == UIElementType.SENDERBOX)
    {
      String label = L.m("Bitte warten...");
      uiElement = new UISenderbox(conf.getId(), label, null);
    }
    else if (conf.getType() == UIElementType.SEARCHBOX)
    {
      uiElement = new UISearchbox(conf.getId(), "", null);
    }

    return uiElement;
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
