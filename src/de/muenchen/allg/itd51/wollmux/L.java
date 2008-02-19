/*
 * Dateiname: L.java
 * Projekt  : WollMux
 * Funktion : Funktionen zur Lokalisierung.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 19.02.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Funktionen zur Lokalisierung.
 * 
 * @author Matthias Benkmann (D-III-ITD D.10)
 */
public class L
{
  /**
   * Die URL der Daten und Übersetzungen zur Lokalisierung.
   */
  private static final URL LOCALIZE_DATA_URL = L.class.getClassLoader()
      .getResource("data/localization.conf");

  /**
   * Wird für die aktuelle Sprache initialisiert und bildet einen Originalstring
   * auf einen übersetzten String ab.
   */
  private static final Map mapMessageToTranslation = new HashMap();

  /**
   * Falls für original eine Übersetzung verfügbar ist, wird diese
   * zurückgeliefert, ansonsten der Originalstring.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10) TESTED
   */
  public static String m(String original)
  {
    String trans = (String) mapMessageToTranslation.get(original.trim());
    if (trans == null)
      return original;
    else
      return trans;
  }

  /**
   * Falls für original eine Übersetzung verfügbar ist, wird diese
   * zurückgeliefert, ansonsten der Originalstring. Dabei werden alle Vorkommen
   * von "%1" durch insertion1 ersetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10) TESTED
   */
  public static String m(String original, Object insertion1)
  {
    // nicht replaceAll verwenden, weil es \ und $ besonders
    // interpretiert
    return replace(m(original), "%1", "" + insertion1);
  }
  
  /**
   * Falls für original eine Übersetzung verfügbar ist, wird diese
   * zurückgeliefert, ansonsten der Originalstring. Dabei werden alle Vorkommen
   * von "%1" durch insertion1 und von "%2" durch insertion2 ersetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10) TESTED
   */
  public static String m(String original, Object insertion1, Object insertion2)
  {
    // nicht replaceAll verwenden, weil es \ und $ besonders
    // interpretiert
    return replace(m(original, insertion1), "%2", "" + insertion2);
  }

  /**
   * Ersetzt in where alle Vorkommen von what durch withWhat und liefert das
   * Ergebnis zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10) TESTED
   */
  private static String replace(String where, String what, String withWhat)
  {
    int i = where.indexOf(what);
    if (i < 0 || what.length() == 0) return where;

    StringBuilder buffy = new StringBuilder(where);
    while (i > 0)
    {
      buffy.replace(i, i + what.length(), withWhat);
      i = buffy.indexOf(what, i + withWhat.length());
    }

    return buffy.toString();
  }

  static
  {
    try
    {
      ConfigThingy l10n = new ConfigThingy("l10n", LOCALIZE_DATA_URL);

      String messageLanguage = Locale.getDefault().getCountry();
      String lcMessages = System.getenv("LC_MESSAGES");
      if (lcMessages != null && lcMessages.length() >= 2)
        messageLanguage = lcMessages.substring(0, 2);

      ConfigThingy messages = l10n.get("Messages", 2);
      Iterator iter = messages.iterator();
      String original = "foo";
      while (iter.hasNext())
      {
        ConfigThingy conf = (ConfigThingy) iter.next();
        if (conf.getName().equalsIgnoreCase("original"))
          original = conf.toString();

        if (conf.getName().equalsIgnoreCase(messageLanguage))
          mapMessageToTranslation.put(original, conf.toString());
      }
    }
    catch (Exception x)
    {
      x.printStackTrace();
    }

  }
}
