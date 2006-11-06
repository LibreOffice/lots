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
 * 26.07.2006 | BNK | formatInternalTelefonNumber5 -> formatiereTelefonnummerDIN5008
 * 26.10.2006 | BNK | +gender()
 * 30.10.2006 | BNK | Kommentare verbessert.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.util.Calendar;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
   * Liefert herrText zurück, falls lowcase(anrede) == "herr" oder "herrn",
   * liefert frauText zurück, falls lowcase(anrede) == "frau",
   * liefert sonstText zurück, falls keiner der obigen Fälle zutrifft.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static String gender(String herrText, String frauText, String sonstText, String anrede)
  {
    if (anrede.equalsIgnoreCase("herr") || anrede.equalsIgnoreCase("herrn"))
      return herrText;
    else if (anrede.equalsIgnoreCase("frau"))
      return frauText;
    else
      return sonstText;
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
   * Formatiert tel gemäss DIN 5008 und setzt dann die Vorwahl "089" davor.
   * 
   * @author Bettina Bauer (D-III-ITD 5.1)
   */  
  public static String formatiereTelefonnummerDIN5008(String tel)
  {
    String vorwahlExtern = "089";
    String formatierteTelExtern = formatiereTelefonnummer(tel, vorwahlExtern);
    return formatierteTelExtern;
  }

  /**
   * Formatiert tel gemäss DIN 5008 und setzt dann die Vorwahl "0" davor, getrennt durch
   * Space. Dadurch entsteht eine Nummer, die von jedem internen Telefon aus funktioniert.
   * 
   * @author Bettina Bauer (D-III-ITD 5.1)
   */
  public static String formatiereTelefonnummerDIN5008Intern(String tel)
  {
    String vorwahlIntern = "0";
    String formatierteTelIntern = formatiereTelefonnummer(tel, vorwahlIntern);
    return formatierteTelIntern;
  }

  /**
   * Formatiert tel gemäss DIN 5008 und setzt dann die Vorwahl vorwahl davor, getrennt durch
   * Space.
   * 
   * @author Bettina Bauer (D-III-ITD 5.1)
   */
  private static String formatiereTelefonnummer(String tel, String vorwahl)
  {
    if (tel == null || tel.length() == 0)
    {
      return tel;
    }
    else
    {
      // alle Leerzeichen
      tel = tel.replaceAll("\\p{Space}", "");
      // alle anderen Zeichen bis auf "-" und "/"
      tel = tel.replaceAll("[^0-9-/]", "");
      // beginnt mit "233"
      if (tel.startsWith("233"))
      {
        tel = tel.replaceAll("\\p{Punct}", "");
        if(tel.startsWith("233989")){
          tel = tel.replaceFirst("233989", vorwahl + " 233-989 ");
        }else {
          tel = tel.replaceFirst("233", vorwahl + " 233-");
        }
      }
      // kein "/" enthalten und Startet mit "0"
      else if ((!tel.contains("/")) && tel.startsWith("0"))
      {
        if (tel.startsWith("0-"))
        {
          tel = tel.replaceFirst("0-", "");
        }
        else
        {
          tel = tel.replaceFirst("0", "");
        }
        tel = vorwahl + " " + tel;
        // kein "/"
      }
      else if (!tel.contains("/"))
      {
        tel = vorwahl + " " + tel;

      }
      else
      {
        for (int i = 0; i < tel.length(); i++)
        {
          if (tel.charAt(i) == '/')
          {
            tel = tel.replaceAll("\\p{Punct}", "");
            tel = tel.substring(0, i) + " " + tel.substring(i, tel.length());
          }
        }

      }
    }
    return tel;
  }
  
  /**
   * Liefert true gdw regex eingabe vollständig matcht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static boolean regex(String regex, String eingabe)
  {
    try{
      Pattern pattern = Pattern.compile(regex);
      return pattern.matcher(eingabe).matches();
    }catch(PatternSyntaxException x)
    {
      return false;
    }
  }
}
