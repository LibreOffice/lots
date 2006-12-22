/*
 * Dateiname: Shortcuts.java
 * Projekt  : WollMux
 * Funktion : Ersetzen bzw. setzen von Shortcuts in OOo.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 19.12.2006 | BAB | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Bettina Bauer (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.KeyFunction;
import com.sun.star.awt.KeyModifier;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.ui.XAcceleratorConfiguration;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

public class Shortcuts
{
  /**
   * Liest alle Attribute SHORTCUT und URL aus tastenkombinationenConf aus,
   * löscht alle bisher vorhandenen Tastenkombinationen die diese URLs verwenden
   * und setzt neue Tastenkombination in OOo-Writer.
   * 
   * @param tastenkombinationenConf
   *          .conf Abschnitt Tastenkuerzel mit allen Knoten
   */
  public static void createShortcuts(ConfigThingy tastenkombinationenConf)
  {
    XAcceleratorConfiguration shortcutManager = UNO
        .getShortcutManager("com.sun.star.text.TextDocument");
    if (shortcutManager == null) return;

    // lesen des Knoten SHORTCUT
    ConfigThingy shortcutConf = tastenkombinationenConf
        .queryByChild("SHORTCUT");

    // Iterieren über die Knoten SHORTCUT
    Iterator iterShortcut = shortcutConf.iterator();
    while (iterShortcut.hasNext())
    {

      ConfigThingy tastenkombination = (ConfigThingy) iterShortcut.next();

      String shortcut = null;
      // lesen der Knoten SHORTCUT
      try
      {
        shortcut = tastenkombination.get("SHORTCUT").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error("SHORTCUT Angabe fehlt in "
                     + tastenkombination.stringRepresentation());
        continue;
      }

      String url = null;
      // lesen der Knoten URL
      try
      {
        url = tastenkombination.get("URL").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error("URL Angabe fehlt in "
                     + tastenkombination.stringRepresentation());
        continue;
      }

      removeComandFromAllKeyEvents(shortcutManager, url);

      KeyEvent keyEvent = createKeyEvent(shortcut);

      // setzen der Tastenkombination mit KeyEvent und WollMux-Url
      try
      {
        shortcutManager.setKeyEvent(keyEvent, url);
      }
      catch (IllegalArgumentException e)
      {
        Logger.error(e);
      }
    }

    // Änderung Persistent machen
    try
    {
      if (UNO.XUIConfigurationPersistence(shortcutManager) != null)
      {
        UNO.XUIConfigurationPersistence(shortcutManager).store();
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Wenn es Tastenkuerzel mit einem Kommando url gibt, werden diese gelöscht.
   * Workaround wegen nicht funktionierendem
   * xAcceleratorConfiguration.removeCommandFromAllKeyEvents(). OOo Issue #72558
   * 
   * @param xAcceleratorConfiguration
   *          AcceleratorConfiguration (muß danach noch mit store() persistent
   *          gemacht werden)
   * @param url
   *          zu löschendes Kommando mit Tastenkombination
   */
  private static void removeComandFromAllKeyEvents(
      XAcceleratorConfiguration xAcceleratorConfiguration, String url)
  {
    // lesen aller gesetzten Tastenkombinationen
    KeyEvent[] keys = xAcceleratorConfiguration.getAllKeyEvents();

    // Wenn es Tastenkombinationen TextbausteinEinfuegen gibt,
    // werden diese gelöscht Workaround wegen nicht funktionierendem
    // xAcceleratorConfiguration.removeCommandFromAllKeyEvents().
    for (int i = 0; i < keys.length; i++)
    {
      try
      {
        // wenn es die URL als Tastenkombination gibt
        if (xAcceleratorConfiguration.getCommandByKeyEvent(keys[i]).equals(url))
        {
          // löschen der Tastenkombination
          xAcceleratorConfiguration.removeKeyEvent(keys[i]);
        }
      }
      catch (NoSuchElementException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Gibt alle KeyEvents mit Modifier, KeyCode und Command aus
   * 
   * @param xac
   *          AcceleratorConfigurator
   */
  public static void showKeyset(XAcceleratorConfiguration xac)
  {
    if (xac == null) return;

    KeyEvent[] keys = xac.getAllKeyEvents();

    try
    {
      for (int i = 0; i < keys.length; i++)
      {
        System.out.println("Modifiers: "
                           + keys[i].Modifiers
                           + " KeyCode: "
                           + keys[i].KeyCode
                           + " --> "
                           + xac.getCommandByKeyEvent(keys[i]));

      }
    }
    catch (Exception e)
    {
      System.err.println(e);
    }

  }

  /**
   * Erzeugt ein Object KeyEvent
   * 
   * @param shortcutWithSeparator
   *          Tastenkombination mit "+" als Separator
   * @return gibt ein KeyEvent zurück
   */
  private static KeyEvent createKeyEvent(String shortcutWithSeparator)
  {

    if (shortcutWithSeparator == null) return null;

    String[] shortcuts = shortcutWithSeparator.split("\\+");

    KeyEvent key = new KeyEvent();
    key.Modifiers = 0;
    key.KeyFunc = KeyFunction.DONTKNOW;

    for (int i = 0; i < shortcuts.length; i++)
    {
      String shortcut = shortcuts[i].replaceAll("\\s", "");
      Short keyCode = returnKeyCode(shortcut);
      if (keyCode != null)
      {
        key.KeyCode = keyCode.shortValue();
      }
      Short keyModifier = returnKeyModifier(shortcut);
      if (keyModifier != null)
      {
        key.Modifiers |= keyModifier.shortValue();
      }
    }
    return key;
  }

  /**
   * Gibt die Konstante com.sun.star.awt.Key für die entsprechende Taste zurück
   * 
   * @param shortcut
   *          Taste
   * @return Key der entsprechenden Taste
   */
  private static Short returnKeyCode(String shortcut)
  {

    final Map myMap = new HashMap();

    // Zahlen 0-9
    myMap.put("0", new Short(Key.NUM0));
    myMap.put("1", new Short(Key.NUM1));
    myMap.put("2", new Short(Key.NUM2));
    myMap.put("3", new Short(Key.NUM3));
    myMap.put("4", new Short(Key.NUM4));
    myMap.put("5", new Short(Key.NUM5));
    myMap.put("6", new Short(Key.NUM6));
    myMap.put("7", new Short(Key.NUM7));
    myMap.put("8", new Short(Key.NUM8));
    myMap.put("9", new Short(Key.NUM9));

    // Buchstaben A-Z
    myMap.put("A", new Short(Key.A));
    myMap.put("B", new Short(Key.B));
    myMap.put("C", new Short(Key.C));
    myMap.put("D", new Short(Key.D));
    myMap.put("E", new Short(Key.E));
    myMap.put("F", new Short(Key.F));
    myMap.put("G", new Short(Key.G));
    myMap.put("H", new Short(Key.H));
    myMap.put("I", new Short(Key.I));
    myMap.put("J", new Short(Key.J));
    myMap.put("K", new Short(Key.K));
    myMap.put("L", new Short(Key.L));
    myMap.put("M", new Short(Key.M));
    myMap.put("N", new Short(Key.N));
    myMap.put("O", new Short(Key.O));
    myMap.put("P", new Short(Key.P));
    myMap.put("Q", new Short(Key.Q));
    myMap.put("R", new Short(Key.R));
    myMap.put("S", new Short(Key.S));
    myMap.put("T", new Short(Key.T));
    myMap.put("U", new Short(Key.U));
    myMap.put("V", new Short(Key.V));
    myMap.put("W", new Short(Key.W));
    myMap.put("X", new Short(Key.X));
    myMap.put("Y", new Short(Key.Y));
    myMap.put("Z", new Short(Key.Z));

    // F1 - F26
    myMap.put("F1", new Short(Key.F1));
    myMap.put("F2", new Short(Key.F2));
    myMap.put("F3", new Short(Key.F3));
    myMap.put("F4", new Short(Key.F4));
    myMap.put("F5", new Short(Key.F5));
    myMap.put("F6", new Short(Key.F6));
    myMap.put("F7", new Short(Key.F7));
    myMap.put("F8", new Short(Key.F8));
    myMap.put("F9", new Short(Key.F9));
    myMap.put("F10", new Short(Key.F10));
    myMap.put("F11", new Short(Key.F11));
    myMap.put("F12", new Short(Key.F12));
    myMap.put("F13", new Short(Key.F13));
    myMap.put("F14", new Short(Key.F14));
    myMap.put("F15", new Short(Key.F15));
    myMap.put("F16", new Short(Key.F16));
    myMap.put("F17", new Short(Key.F17));
    myMap.put("F18", new Short(Key.F18));
    myMap.put("F19", new Short(Key.F19));
    myMap.put("F20", new Short(Key.F20));
    myMap.put("F21", new Short(Key.F21));
    myMap.put("F22", new Short(Key.F22));
    myMap.put("F23", new Short(Key.F23));
    myMap.put("F24", new Short(Key.F24));
    myMap.put("F25", new Short(Key.F25));
    myMap.put("F26", new Short(Key.F26));

    myMap.put("DOWN", new Short(Key.DOWN));
    myMap.put("UNTEN", new Short(Key.DOWN));
    myMap.put("UP", new Short(Key.UP));
    myMap.put("OBEN", new Short(Key.UP));
    myMap.put("LEFT", new Short(Key.LEFT));
    myMap.put("LINKS", new Short(Key.LEFT));
    myMap.put("RIGHT", new Short(Key.RIGHT));
    myMap.put("RECHTS", new Short(Key.RIGHT));
    myMap.put("HOME", new Short(Key.HOME));
    myMap.put("POS1", new Short(Key.HOME));
    myMap.put("END", new Short(Key.END));
    myMap.put("ENDE", new Short(Key.END));
    myMap.put("PAGEUP", new Short(Key.PAGEUP));
    myMap.put("BILDAUF", new Short(Key.PAGEUP));
    myMap.put("RETURN", new Short(Key.RETURN));
    myMap.put("EINGABE", new Short(Key.RETURN));
    myMap.put("ESCAPE", new Short(Key.ESCAPE));
    myMap.put("ESC", new Short(Key.ESCAPE));
    myMap.put("TAB", new Short(Key.TAB));
    myMap.put("TABULATOR", new Short(Key.TAB));
    myMap.put("BACKSPACE", new Short(Key.BACKSPACE));
    myMap.put("RUECKSCHRITT", new Short(Key.BACKSPACE));
    myMap.put("RÜCKSCHRITT", new Short(Key.BACKSPACE));
    myMap.put("SPACE", new Short(Key.SPACE));
    myMap.put("LEERTASTE", new Short(Key.SPACE));
    myMap.put("INSERT", new Short(Key.INSERT));
    myMap.put("EINFG", new Short(Key.INSERT));
    myMap.put("DELETE", new Short(Key.DELETE));
    myMap.put("ENTF", new Short(Key.DELETE));
    myMap.put("ADD", new Short(Key.ADD));
    myMap.put("PLUS", new Short(Key.ADD));
    myMap.put("SUBTRACT", new Short(Key.SUBTRACT));
    myMap.put("-", new Short(Key.SUBTRACT));
    myMap.put("MULTIPLY", new Short(Key.MULTIPLY));
    myMap.put("*", new Short(Key.MULTIPLY));
    myMap.put("DIVIDE", new Short(Key.DIVIDE));
    myMap.put("/", new Short(Key.DIVIDE));
    myMap.put("POINT", new Short(Key.POINT));
    myMap.put(".", new Short(Key.POINT));
    myMap.put("COMMA", new Short(Key.COMMA));
    myMap.put(",", new Short(Key.COMMA));
    myMap.put("LESS", new Short(Key.LESS));
    myMap.put("<", new Short(Key.LESS));
    myMap.put("GREATER", new Short(Key.GREATER));
    myMap.put(">", new Short(Key.GREATER));
    myMap.put("EQUAL", new Short(Key.EQUAL));
    myMap.put("=", new Short(Key.EQUAL));

    final Short keyCode = (Short) myMap.get(shortcut.toUpperCase());

    return keyCode;
  }

  /**
   * Gibt die Konstante com.sun.star.awt.KeyModifier für die entsprechende Taste
   * zurück
   * 
   * @param shortcut
   *          Taste
   * @return KeyModifier der entsprechenden Taste
   */
  private static Short returnKeyModifier(String shortcut)
  {

    final Map myMap = new HashMap();

    // SHIFT, CTRL und ALT
    myMap.put("SHIFT", new Short(KeyModifier.SHIFT));
    myMap.put("UMSCHALT", new Short(KeyModifier.SHIFT));
    myMap.put("CTRL", new Short(KeyModifier.MOD1));
    myMap.put("STRG", new Short(KeyModifier.MOD1));
    myMap.put("ALT", new Short(KeyModifier.MOD2));

    final Short keyModifier = (Short) myMap.get(shortcut.toUpperCase());

    return keyModifier;
  }

  public static void main(String[] args)
  {

    try
    {
      UNO.init();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    // lesen der conf --> fällt nachher weg
    ConfigThingy conf = null;
    String confFile = "./Tastenkombination.conf";

    try
    {
      conf = new ConfigThingy("", new URL(new File(".").toURL(), confFile));
    }
    catch (java.lang.Exception e)
    {
      System.out.println("Fehler ConfigThingy " + e);
    }

    // ConfigThingy conf = WollMuxSingleton.getInstance().getWollmuxConf();

    // lesen des .conf Abschnitt "Tastenkuerzel"
    ConfigThingy tastenkombinationenConf = conf.query("Tastenkuerzel");

    createShortcuts(tastenkombinationenConf);

    System.exit(0);
  }
}
