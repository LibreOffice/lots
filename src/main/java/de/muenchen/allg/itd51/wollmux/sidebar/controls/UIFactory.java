package de.muenchen.allg.itd51.wollmux.sidebar.controls;

import java.util.ArrayList;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementContext;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

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

  public UIFactory()
  {
    listeners = new ArrayList<>();
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
        fireElementCreated(config, isMenu, menuConf.getName());
      }
      else
      {
        fireElementCreated(config, isMenu, null);
      }
    }
  }

  public void addElementCreateListener(UIElementCreateListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  public void removeElementCreateListener(UIElementCreateListener listener)
  {
    if (listeners.contains(listener)) listeners.remove(listener);
  }

  protected void fireElementCreated(UIElementConfig control, boolean isMenu, String parentEntry)
  {
    for (UIElementCreateListener listener : listeners)
    {
      listener.createControl(control, isMenu, parentEntry);
    }
  }
}
