//TODO L.m()
/*
 * Dateiname: DJDatasetListElement.java
 * Projekt  : WollMux
 * Funktion : Wrapper um ein vom DJ gelieferten Datensatz für die Darstellung 
 *            in einer Liste.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 03.11.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.wollmux.Logger;

public class DJDatasetListElement implements Comparable
{
  /**
   * Gibt an, wie die Personen in den Listen angezeigt werden sollen. %{Spalte}
   * Syntax um entsprechenden Wert des Datensatzes einzufügen.
   */
  private final static String displayTemplate = "%{Nachname}, %{Vorname} (%{Rolle})";

  /**
   * Enthält das DJDataset-Element.
   */
  private DJDataset ds;

  /**
   * Erzeugt ein neues DJDatasetListElement für die Darstellung in einer Liste.
   * 
   * @param ds
   *          Das DJDatasetElement das über das DJDatasetListElement dargestellt
   *          werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public DJDatasetListElement(DJDataset ds)
  {
    this.ds = ds;
  }

  /**
   * Liefert den in der Listbox anzuzeigenden String.
   */
  public String toString()
  {
    return getDisplayString(ds);
  }

  /**
   * Liefert den DJDataset dieses DJDatasetListElements.
   * 
   * @return den DJDataset dieses DJDatasetListElements.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public DJDataset getDataset()
  {
    return ds;
  }

  /**
   * Liefert zu einem Datensatz den in einer Listbox anzuzeigenden String.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String getDisplayString(DJDataset ds)
  {
    return substituteVars(displayTemplate, ds);
  }

  /**
   * Ersetzt "%{SPALTENNAME}" in str durch den Wert der entsprechenden Spalte im
   * datensatz.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String substituteVars(String str, Dataset datensatz)
  {
    Pattern p = Pattern.compile("%\\{([a-zA-Z0-9]+)\\}");
    Matcher m = p.matcher(str);
    if (m.find()) do
    {
      String spalte = m.group(1);
      String wert = spalte;
      try
      {
        String wert2 = datensatz.get(spalte);
        if (wert2 != null) wert = wert2.replaceAll("%", "");
      }
      catch (ColumnNotFoundException e)
      {
        Logger.error(e);
      }
      str = str.substring(0, m.start()) + wert + str.substring(m.end());
      m = p.matcher(str);
    } while (m.find());
    return str;
  }

  /**
   * Vergleicht die String-Repräsentation (toString()) zweier Listenelemente
   * über die compareTo()-Methode der Klasse String.
   * 
   * @param o
   * @return Rückgabewert von this.toString().compareTo(o.toString())
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public int compareTo(Object o)
  {
    return this.toString().compareTo(o.toString());
  }

}
