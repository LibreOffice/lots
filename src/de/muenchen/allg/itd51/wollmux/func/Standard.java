/*
 * Dateiname: Standard.java
 * Projekt  : WollMux
 * Funktion : Standardfunktionen für Plausibilitätschecks, Trafos,... in Formularen
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 26.01.2006 | BNK | Erstellung
 * 08.05.2006 | BNK | nach Standard umbenannt, da in Zukunft auch für Trafos etc.
 *                  | +anredeSuffix()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standardfunktionen für Plausibilitätschecks, Trafos,... in Formularen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Standard
{
  /**
   * Liefert immer true.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Boolean immerWahr()
  {
    return new Boolean(true);
  }

  /**
   * Liefert true genau dann wenn low, hi und zahl Integer-Zahlen sind und low<=zahl<=hi.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Boolean zahlenBereich(String low, String hi, String zahl)
  {
    try
    {
      long l = Long.parseLong(zahl);
      long lo = Long.parseLong(low);
      long high = Long.parseLong(hi);
      if (l < lo) return new Boolean(false);
      if (l > high) return new Boolean(false);
    }
    catch (Exception x)
    {
      return new Boolean(false);
    }
    return new Boolean(true);
  }

  /**
   * Liefert den String herrText zurück, falls lowcase(anrede) == "herr",
   * ansonsten wird frauText geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static String herrFrauText(String anrede, String frauText,
      String herrText)
  {
    if (anrede.equalsIgnoreCase("herr"))
      return herrText;
    else
      return frauText;
  }

  /**
   * Versucht, zu erkennen, ob datum ein korrektes Datum der Form Monat.Tag.Jahr
   * ist (wobei Jahr immer 4-stellig sein muss).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static boolean korrektesDatum(String datum)
  {
    try
    {
      String[] s = datum.split("\\.");
      if (s.length != 3) return false;
      int tag = Integer.parseInt(s[0]);
      int monat = Integer.parseInt(s[1]);
      int jahr = Integer.parseInt(s[2]);
      if (jahr < 1000 || jahr > 9999) return false;
      Calendar cal = Calendar.getInstance();
      cal.setLenient(false);
      cal.set(Calendar.DAY_OF_MONTH, tag);
      cal.set(Calendar.MONTH, monat - 1);
      cal.set(Calendar.YEAR, jahr);
      return (cal.get(Calendar.DAY_OF_MONTH) == tag
              && cal.get(Calendar.MONTH) == monat - 1 && cal.get(Calendar.YEAR) == jahr);
    }
    catch (Exception x)
    {
    }
    return false;
  }

  /**
   * Entfernt alle Zeichen die keine Ziffern sind aus dem übergebenen String tel
   * und transformiert die verbleibende Nummer in die Form einer internen
   * Telefonnummer mit einer 5-stelligen Durchwahl ("<Vorwahl> - <Durchwahl>").
   * Ist die nummer nicht gültig, wird eine Fehlermeldung zurückgeliefert. Eine
   * Nummer ist dann gültig, wenn sie mindestens 2 Ziffern für die Vorwahl und
   * exakt 5 Ziffern für die Durchwahl besitzt.
   * 
   * @param tel
   * @return die formatierte telefonnummer.
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public static String formatInternalTelefonNumber5(String tel)
  {
    String telNorm = tel.replaceAll("[^\\d]+", "");
    Pattern p = Pattern.compile("(\\d{2,})(\\d{5})");
    Matcher m = p.matcher(telNorm);

    if (m.matches())
    {
      String vorwahl = m.group(1);
      String durchwahl = m.group(2);
      return vorwahl + " - " + durchwahl;
    }
    else
      return "<Ungültige Telefonnummer (" + tel + ")>";
  }
}
