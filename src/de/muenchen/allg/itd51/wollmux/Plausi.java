/*
* Dateiname: Plausi.java
* Projekt  : WollMux
* Funktion : Standardfunktionen für Plausibilitätschecks in Formularen
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 26.01.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

/**
 * Standardfunktionen für Plausibilitätschecks in Formularen
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Plausi
{
  public static Boolean immerWahr()
  {
    return new Boolean(true);
  }
  
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

}
