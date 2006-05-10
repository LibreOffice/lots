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

/**
 * Standardfunktionen für Plausibilitätschecks, Trafos,... in Formularen.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Standard
{
  /**
   * Liefert immer true.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Boolean immerWahr()
  {
    return new Boolean(true);
  }
  
  /**
   * Liefert true genau dann wenn low, hi und zahl Integer-Zahlen sind und low<=zahl<=hi.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Boolean zahlenBereich(String low, String hi, String zahl)
  {
    try{
      long l = Long.parseLong(zahl);
      long lo = Long.parseLong(low);
      long high = Long.parseLong(hi);
      if (l < lo) return new Boolean(false);
      if (l > high) return new Boolean(false);
    } catch(Exception x)
    {
      return new Boolean(false);
    }
    return new Boolean(true);
  }
  
  /**
   * Liefert den String herrText zurück, falls lowcase(anrede) == "herr", ansonsten
   * wird frauText geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static String herrFrauText(String anrede, String frauText, String herrText)
  {
    if (anrede.equalsIgnoreCase("herr")) 
      return herrText;
    else
      return frauText;
  }

}
