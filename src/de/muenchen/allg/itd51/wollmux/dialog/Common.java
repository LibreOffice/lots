/*
 * Dateiname: Common.java
 * Projekt  : WollMux
 * Funktion : Enthält von den Dialogen gemeinsam genutzten Code.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * 22.11.2005 | BNK | Erstellung
 * 26.06.2006 | BNK | +zoomFonts
 *                  | refak. von setLookAndFeel() zu setLookAndFeelOnce()
 * 27.07.2006 | BNK | "auto" Wert explizit parsen.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Font;
import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Enthält von den Dialogen gemeinsam genutzten Code.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Common
{
  /**
   * Spezialwert wenn eine Breite oder Höhe die maximal sinnvolle sein soll.
   */
  public static final int DIMENSION_MAX = -1;

  /**
   * Spezialwert, wenn eine Breite oder Höhe nicht angegeben wurde.
   */
  public static final int DIMENSION_UNSPECIFIED = -2;

  /**
   * Spezialwert, wenn eine X oder Y Koordinate so gesetzt werden soll, dass das
   * Fenster in der Mitte positioniert ist.
   */
  public static final int COORDINATE_CENTER = -1;

  /**
   * Spezialwert wenn eine X oder Y Koordinate die maximal sinnvolle sein soll.
   */
  public static final int COORDINATE_MAX = -2;

  /**
   * Spezialwert wenn eine X oder Y Koordinate die minimal sinnvolle sein soll.
   */
  public static final int COORDINATE_MIN = -3;

  /**
   * Spezialwert, wenn eine X oder Y Koordinate nicht angegeben wurde.
   */
  public static final int COORDINATE_UNSPECIFIED = -4;

  /**
   * Der Unit-Increment für vertikale Scrollbars, die Zeilen von Eingabefeldern
   * enthalten, wie z,B, die Steuerelemente-Ansicht des FM4000.
   */
  public static int VERTICAL_SCROLLBAR_UNIT_INCREMENT = 12;

  private static boolean lafSet = false;

  /**
   * Führt {@link #setLookAndFeel()} aus, aber nur, wenn es bisher noch nicht
   * ausgeführt wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void setLookAndFeelOnce()
  {
    if (!lafSet) setLookAndFeel();
  }

  /**
   * Setzt das Metal Look and Feel. Das plattformspezifische LAF wird nicht
   * verwendet, damit die Benutzer unter Windows und Linux eine einheitliche Optik
   * haben, so dass a) Schulungsvideos und Unterlagen für beide Plattformen anwendbar
   * sind und b) Benutzer sich bei der Umstellung von Windows auf Linux nicht noch
   * beim WollMux umgewöhnen müssen. Des weiteren hatte zumindest als wir angefangen
   * haben das GTK Look and Feel einige Bugs. Es ist also auch ein Problem, dass wir
   * nicht genug Ressourcen haben, um 2 Plattformen diesbzgl. zu testen und zu
   * debuggen.
   * 
   * alt: Setzt das System Look and Feel, falls es nicht MetalLookAndFeel ist.
   * Ansonsten setzt es GTKLookAndFeel falls möglich.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void setLookAndFeel()
  {
    String lafName = UIManager.getSystemLookAndFeelClassName();
    // if (lafName.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
    // lafName = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
    lafName = "javax.swing.plaf.metal.MetalLookAndFeel";
    try
    {
      UIManager.setLookAndFeel(lafName);
    }
    catch (Exception x)
    {}
    ;
    // JFrame.setDefaultLookAndFeelDecorated(true); seems to cause problems with
    // undecorated windows in Metal LAF
    lafSet = true;
  }

  /**
   * Multipliziert alle Font-Größen mit zoomFactor. ACHTUNG! Nach jedem Aufruf von
   * setLookAndFeel() kann diese Funktion genau einmal verwendet werden und hat in
   * folgenden Aufrufen keine Wirkung mehr, bis wieder setLookAndFeel() aufgerufen
   * wird (was den Zoom wieder zurücksetzt).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void zoomFonts(double zoomFactor)
  {
    Logger.debug("zoomFonts(" + zoomFactor + ")");
    UIDefaults def = UIManager.getLookAndFeelDefaults();
    Enumeration<Object> enu = def.keys();
    int changedFonts = 0;
    while (enu.hasMoreElements())
    {
      Object key = enu.nextElement();
      if (key.toString().endsWith(".font"))
      {
        try
        {
          FontUIResource res = (FontUIResource) def.get(key);
          Font fnt = res.deriveFont((float) (res.getSize() * zoomFactor));
          def.put(key, fnt);
          ++changedFonts;
        }
        catch (Exception x)
        {}
      }
    }
    Logger.debug(changedFonts + L.m(" Fontgrößen verändert!"));
  }

  /**
   * Parst WIDTH, HEIGHT, X und Y aus fensterConf und liefert ein entsprechendes
   * Rectangle. Spezialwerte wie {@link #COORDINATE_CENTER} und
   * {@link #DIMENSION_MAX} werden verwendet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Rectangle parseDimensions(ConfigThingy fensterConf)
  {
    Rectangle r = new Rectangle();
    r.x = COORDINATE_UNSPECIFIED;
    try
    {
      String xStr = fensterConf.get("X").toString();
      if (xStr.equalsIgnoreCase("center"))
        r.x = COORDINATE_CENTER;
      else if (xStr.equalsIgnoreCase("max"))
        r.x = COORDINATE_MAX;
      else if (xStr.equalsIgnoreCase("min"))
        r.x = COORDINATE_MIN;
      else if (xStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.x = Integer.parseInt(xStr);
        // Ja, das folgende ist eine Einschränkung, aber
        // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
        // obigen Festlegungen
        if (r.x < 0) r.x = 0;
      }
    }
    catch (Exception x)
    {}

    r.y = COORDINATE_UNSPECIFIED;
    try
    {
      String yStr = fensterConf.get("Y").toString();
      if (yStr.equalsIgnoreCase("center"))
        r.y = COORDINATE_CENTER;
      else if (yStr.equalsIgnoreCase("max"))
        r.y = COORDINATE_MAX;
      else if (yStr.equalsIgnoreCase("min"))
        r.y = COORDINATE_MIN;
      else if (yStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.y = Integer.parseInt(yStr);
        // Ja, das folgende ist eine Einschränkung, aber
        // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
        // obigen Festlegungen
        if (r.y < 0) r.y = 0;
      }
    }
    catch (Exception x)
    {}

    r.width = DIMENSION_UNSPECIFIED;
    try
    {
      String widthStr = fensterConf.get("WIDTH").toString();
      if (widthStr.equalsIgnoreCase("max"))
        r.width = DIMENSION_MAX;
      else if (widthStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.width = Integer.parseInt(widthStr);
        if (r.width < 0) r.width = DIMENSION_UNSPECIFIED;
      }
    }
    catch (Exception x)
    {}

    r.height = DIMENSION_UNSPECIFIED;
    try
    {
      String heightStr = fensterConf.get("HEIGHT").toString();
      if (heightStr.equalsIgnoreCase("max"))
        r.height = DIMENSION_MAX;
      else if (heightStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.height = Integer.parseInt(heightStr);
        if (r.height < 0) r.height = DIMENSION_UNSPECIFIED;
      }
    }
    catch (Exception x)
    {}

    return r;
  }
}
