/*
 * Dateiname: Shortcuts.java
 * Projekt  : WollMux
 * Funktion : Ersetzen bzw. setzen von Shortcuts in OOo.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
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
import com.sun.star.ui.XAcceleratorConfiguration;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

public class Shortcuts
{
  /**
   * Liest alle Attribute SHORTCUT und URL aus tastenkombinationenConf aus, löscht
   * alle bisher vorhandenen Tastenkombinationen deren URL mit "wollmux:" beginnt und
   * setzt neue Tastenkombination in OOo-Writer.
   * 
   * @param tastenkombinationenConf
   *          .conf Abschnitt Tastenkuerzel mit allen Knoten
   */
  public static void createShortcuts(ConfigThingy tastenkombinationenConf)
  {
    XAcceleratorConfiguration shortcutManager =
      UNO.getShortcutManager("com.sun.star.text.TextDocument");
    if (shortcutManager == null) return;

    // löschen aller KeyEvents die mit "wollmux:" beginnen
    removeComandFromAllKeyEvents(shortcutManager);

    // lesen des Knoten SHORTCUT
    ConfigThingy shortcutConf = tastenkombinationenConf.queryByChild("SHORTCUT");

    // Iterieren über die Knoten SHORTCUT
    Iterator<ConfigThingy> iterShortcut = shortcutConf.iterator();
    while (iterShortcut.hasNext())
    {

      ConfigThingy tastenkombination = iterShortcut.next();

      String shortcut = null;
      // lesen der Knoten SHORTCUT
      try
      {
        shortcut = tastenkombination.get("SHORTCUT").toString();
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(L.m("SHORTCUT Angabe fehlt in '%1%'",
          tastenkombination.stringRepresentation()));
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
        Logger.error(L.m("URL Angabe fehlt in '%1'",
          tastenkombination.stringRepresentation()));
        continue;
      }

      KeyEvent keyEvent = createKeyEvent(shortcut);
      if (keyEvent != null)
      {
        // setzen der Tastenkombination mit KeyEvent und WollMux-Url
        try
        {
          shortcutManager.setKeyEvent(keyEvent, url);
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      }
      else
      {
        Logger.error(L.m(
          "Ungültige Tastenkombination '%1' im .conf Abschnitt Tastenkuerzel",
          shortcut));
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
   * Wenn es Tastenkuerzel mit einer UNO-url beginnent mit "wollmux:" gibt, werden
   * diese gelöscht. Workaround wegen nicht funktionierendem
   * xAcceleratorConfiguration.removeCommandFromAllKeyEvents(). OOo Issue #72558
   * 
   * @param xAcceleratorConfiguration
   *          AcceleratorConfiguration (muß danach noch mit store() persistent
   *          gemacht werden)
   */
  private static void removeComandFromAllKeyEvents(
      XAcceleratorConfiguration xAcceleratorConfiguration)
  {
    // lesen aller gesetzten Tastenkombinationen
    KeyEvent[] keys = xAcceleratorConfiguration.getAllKeyEvents();

    // Wenn es Tastenkombinationen mit der UNO-url beginnent mit "wollmux:"
    // gibt, werden diese gelöscht. Workaround wegen nicht funktionierendem
    // xAcceleratorConfiguration.removeCommandFromAllKeyEvents().
    for (int i = 0; i < keys.length; i++)
    {
      try
      {
        String event = xAcceleratorConfiguration.getCommandByKeyEvent(keys[i]);
        // wenn die UNO-url mit "wollmux:" beginnt, wird sie gelöscht
        if (event.startsWith("wollmux:"))
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
        Logger.debug2("Modifiers: " + keys[i].Modifiers + " KeyCode: "
          + keys[i].KeyCode + " --> " + xac.getCommandByKeyEvent(keys[i]));

      }
    }
    catch (Exception e)
    {
      Logger.debug2(e);
    }

  }

  /**
   * Erzeugt ein Object KeyEvent
   * 
   * @param shortcutWithSeparator
   *          Tastenkombination mit "+" als Separator
   * @return gibt ein KeyEvent zurück oder null wenn kein keyCode sondern nur
   *         keyModifier verwendet werden
   */
  private static KeyEvent createKeyEvent(String shortcutWithSeparator)
  {

    if (shortcutWithSeparator == null) return null;

    String[] shortcuts = shortcutWithSeparator.split("\\+");
    Short keyCode = null;

    KeyEvent key = new KeyEvent();
    key.Modifiers = 0;
    key.KeyFunc = KeyFunction.DONTKNOW;

    for (int i = 0; i < shortcuts.length; i++)
    {
      String shortcut = shortcuts[i].replaceAll("\\s", "");
      keyCode = returnKeyCode(shortcut);
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

    if (keyCode == null)
    {
      return null;
    }
    else
    {
      return key;
    }
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

    final Map<String, Short> myMap = new HashMap<String, Short>();

    // Zahlen 0-9
    myMap.put("0", Short.valueOf(Key.NUM0));
    myMap.put("1", Short.valueOf(Key.NUM1));
    myMap.put("2", Short.valueOf(Key.NUM2));
    myMap.put("3", Short.valueOf(Key.NUM3));
    myMap.put("4", Short.valueOf(Key.NUM4));
    myMap.put("5", Short.valueOf(Key.NUM5));
    myMap.put("6", Short.valueOf(Key.NUM6));
    myMap.put("7", Short.valueOf(Key.NUM7));
    myMap.put("8", Short.valueOf(Key.NUM8));
    myMap.put("9", Short.valueOf(Key.NUM9));

    // Buchstaben A-Z
    myMap.put("A", Short.valueOf(Key.A));
    myMap.put("B", Short.valueOf(Key.B));
    myMap.put("C", Short.valueOf(Key.C));
    myMap.put("D", Short.valueOf(Key.D));
    myMap.put("E", Short.valueOf(Key.E));
    myMap.put("F", Short.valueOf(Key.F));
    myMap.put("G", Short.valueOf(Key.G));
    myMap.put("H", Short.valueOf(Key.H));
    myMap.put("I", Short.valueOf(Key.I));
    myMap.put("J", Short.valueOf(Key.J));
    myMap.put("K", Short.valueOf(Key.K));
    myMap.put("L", Short.valueOf(Key.L));
    myMap.put("M", Short.valueOf(Key.M));
    myMap.put("N", Short.valueOf(Key.N));
    myMap.put("O", Short.valueOf(Key.O));
    myMap.put("P", Short.valueOf(Key.P));
    myMap.put("Q", Short.valueOf(Key.Q));
    myMap.put("R", Short.valueOf(Key.R));
    myMap.put("S", Short.valueOf(Key.S));
    myMap.put("T", Short.valueOf(Key.T));
    myMap.put("U", Short.valueOf(Key.U));
    myMap.put("V", Short.valueOf(Key.V));
    myMap.put("W", Short.valueOf(Key.W));
    myMap.put("X", Short.valueOf(Key.X));
    myMap.put("Y", Short.valueOf(Key.Y));
    myMap.put("Z", Short.valueOf(Key.Z));

    // F1 - F26
    myMap.put("F1", Short.valueOf(Key.F1));
    myMap.put("F2", Short.valueOf(Key.F2));
    myMap.put("F3", Short.valueOf(Key.F3));
    myMap.put("F4", Short.valueOf(Key.F4));
    myMap.put("F5", Short.valueOf(Key.F5));
    myMap.put("F6", Short.valueOf(Key.F6));
    myMap.put("F7", Short.valueOf(Key.F7));
    myMap.put("F8", Short.valueOf(Key.F8));
    myMap.put("F9", Short.valueOf(Key.F9));
    myMap.put("F10", Short.valueOf(Key.F10));
    myMap.put("F11", Short.valueOf(Key.F11));
    myMap.put("F12", Short.valueOf(Key.F12));
    myMap.put("F13", Short.valueOf(Key.F13));
    myMap.put("F14", Short.valueOf(Key.F14));
    myMap.put("F15", Short.valueOf(Key.F15));
    myMap.put("F16", Short.valueOf(Key.F16));
    myMap.put("F17", Short.valueOf(Key.F17));
    myMap.put("F18", Short.valueOf(Key.F18));
    myMap.put("F19", Short.valueOf(Key.F19));
    myMap.put("F20", Short.valueOf(Key.F20));
    myMap.put("F21", Short.valueOf(Key.F21));
    myMap.put("F22", Short.valueOf(Key.F22));
    myMap.put("F23", Short.valueOf(Key.F23));
    myMap.put("F24", Short.valueOf(Key.F24));
    myMap.put("F25", Short.valueOf(Key.F25));
    myMap.put("F26", Short.valueOf(Key.F26));

    myMap.put("DOWN", Short.valueOf(Key.DOWN));
    myMap.put("UNTEN", Short.valueOf(Key.DOWN));
    myMap.put("UP", Short.valueOf(Key.UP));
    myMap.put("OBEN", Short.valueOf(Key.UP));
    myMap.put("LEFT", Short.valueOf(Key.LEFT));
    myMap.put("LINKS", Short.valueOf(Key.LEFT));
    myMap.put("RIGHT", Short.valueOf(Key.RIGHT));
    myMap.put("RECHTS", Short.valueOf(Key.RIGHT));
    myMap.put("HOME", Short.valueOf(Key.HOME));
    myMap.put("POS1", Short.valueOf(Key.HOME));
    myMap.put("END", Short.valueOf(Key.END));
    myMap.put("ENDE", Short.valueOf(Key.END));
    myMap.put("PAGEUP", Short.valueOf(Key.PAGEUP));
    myMap.put("BILDAUF", Short.valueOf(Key.PAGEUP));
    myMap.put("RETURN", Short.valueOf(Key.RETURN));
    myMap.put("EINGABE", Short.valueOf(Key.RETURN));
    myMap.put("ESCAPE", Short.valueOf(Key.ESCAPE));
    myMap.put("ESC", Short.valueOf(Key.ESCAPE));
    myMap.put("TAB", Short.valueOf(Key.TAB));
    myMap.put("TABULATOR", Short.valueOf(Key.TAB));
    myMap.put("BACKSPACE", Short.valueOf(Key.BACKSPACE));
    myMap.put("RUECKSCHRITT", Short.valueOf(Key.BACKSPACE));
    myMap.put("RÜCKSCHRITT", Short.valueOf(Key.BACKSPACE));
    myMap.put("SPACE", Short.valueOf(Key.SPACE));
    myMap.put("LEERTASTE", Short.valueOf(Key.SPACE));
    myMap.put("INSERT", Short.valueOf(Key.INSERT));
    myMap.put("EINFG", Short.valueOf(Key.INSERT));
    myMap.put("DELETE", Short.valueOf(Key.DELETE));
    myMap.put("ENTF", Short.valueOf(Key.DELETE));
    myMap.put("ADD", Short.valueOf(Key.ADD));
    myMap.put("PLUS", Short.valueOf(Key.ADD));
    myMap.put("SUBTRACT", Short.valueOf(Key.SUBTRACT));
    myMap.put("-", Short.valueOf(Key.SUBTRACT));
    myMap.put("MULTIPLY", Short.valueOf(Key.MULTIPLY));
    myMap.put("*", Short.valueOf(Key.MULTIPLY));
    myMap.put("DIVIDE", Short.valueOf(Key.DIVIDE));
    myMap.put("/", Short.valueOf(Key.DIVIDE));
    myMap.put("POINT", Short.valueOf(Key.POINT));
    myMap.put(".", Short.valueOf(Key.POINT));
    myMap.put("COMMA", Short.valueOf(Key.COMMA));
    myMap.put(",", Short.valueOf(Key.COMMA));
    myMap.put("LESS", Short.valueOf(Key.LESS));
    myMap.put("<", Short.valueOf(Key.LESS));
    myMap.put("GREATER", Short.valueOf(Key.GREATER));
    myMap.put(">", Short.valueOf(Key.GREATER));
    myMap.put("EQUAL", Short.valueOf(Key.EQUAL));
    myMap.put("=", Short.valueOf(Key.EQUAL));

    final Short keyCode = myMap.get(shortcut.toUpperCase());

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

    final Map<String, Short> myMap = new HashMap<String, Short>();

    // SHIFT, CTRL und ALT
    myMap.put("SHIFT", Short.valueOf(KeyModifier.SHIFT));
    myMap.put("UMSCHALT", Short.valueOf(KeyModifier.SHIFT));
    myMap.put("CTRL", Short.valueOf(KeyModifier.MOD1));
    myMap.put("STRG", Short.valueOf(KeyModifier.MOD1));
    myMap.put("ALT", Short.valueOf(KeyModifier.MOD2));

    final Short keyModifier = myMap.get(shortcut.toUpperCase());

    return keyModifier;
  }

  public static void main(String[] args) throws Exception
  {

    UNO.init();

    // lesen der conf --> fällt nachher weg
    ConfigThingy conf = null;
    String confFile = "../../.wollmux/wollmux.conf";

    conf = new ConfigThingy("", new URL(new File(".").toURI().toURL(), confFile));

    // ConfigThingy conf = WollMuxSingleton.getInstance().getWollmuxConf();

    // lesen des .conf Abschnitt "Tastenkuerzel"
    ConfigThingy tastenkombinationenConf = conf.query("Tastenkuerzel");

    createShortcuts(tastenkombinationenConf);

    System.exit(0);
  }
}
