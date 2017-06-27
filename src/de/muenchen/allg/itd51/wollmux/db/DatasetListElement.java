/*
 * Dateiname: DatasetListElement.java
 * Projekt  : WollMux
 * Funktion : Wrapper um einen Datansatz des DJ für die Darstellung 
 *            in einer Liste.
 * 
 * Copyright (c) 2008-2017 Landeshauptstadt München
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
 * 03.11.2005 | LUT | Erstellung als DJDatasetListElement
 * 06.04.2010 | BED | +Icon
 * 07.04.2010 | BED | displayTemplate als Instanzvariable
 * 11.01.2011 | LUT | Verallgemeinert zu DatasetListElement
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import de.muenchen.allg.itd51.wollmux.core.util.Logger;

public class DatasetListElement implements Comparable<DatasetListElement>
{
  /**
   * Gibt an, wie die Personen in den Listen angezeigt werden sollen.
   * %{Spalte}-Syntax um entsprechenden Wert des Datensatzes einzufügen, z.B.
   * "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   */
  private String displayTemplate;

  /**
   * Enthält das Dataset-Element.
   */
  private Dataset ds;

  /**
   * Optionales Icon, das in der Liste angezeigt werden soll.
   */
  private Icon icon;

  /**
   * Erzeugt ein neues DatasetListElement für die Darstellung in einer Liste (ohne
   * Icon).
   * 
   * @param ds
   *          Das DatasetElement das über das DatasetListElement dargestellt werden
   *          soll.
   * @param displayTemplate
   *          gibt an, wie die Personen in den Listen angezeigt werden sollen.
   *          %{Spalte}-Syntax um entsprechenden Wert des Datensatzes einzufügen,
   *          z.B. "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public DatasetListElement(Dataset ds, String displayTemplate)
  {
    this(ds, displayTemplate, null);
  }

  /**
   * Erzeugt ein neues DatasetListElement für die Darstellung in einer Liste.
   * 
   * @param ds
   *          das DatasetElement das über das DatasetListElement dargestellt werden
   *          soll.
   * @param displayTemplate
   *          gibt an, wie die Personen in den Listen angezeigt werden sollen.
   *          %{Spalte}-Syntax um entsprechenden Wert des Datensatzes einzufügen,
   *          z.B. "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   * @param icon
   *          das Icon, das in der Liste für das Element verwendet werden soll. Falls
   *          kein Icon vorhanden ist, kann <code>null</code> übergeben werden.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public DatasetListElement(Dataset ds, String displayTemplate, Icon icon)
  {
    this.ds = ds;
    this.displayTemplate = displayTemplate;
    this.icon = icon;
  }

  /**
   * Liefert den in der Listbox anzuzeigenden String.
   */
  public String toString()
  {
    return getDisplayString(ds);
  }

  /**
   * Liefert den Dataset dieses DatasetListElements.
   * 
   * @return den Dataset dieses DatasetListElements.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public Dataset getDataset()
  {
    return ds;
  }

  /**
   * Liefert das Icon dieses DatasetListElements zurück.
   * 
   * @return das Icon dieses DatasetListElements. Falls kein Icon vorhanden ist, wird
   *         <code>null</code> zurückgeliefert.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public Icon getIcon()
  {
    return this.icon;
  }

  /**
   * Liefert zu einem Datensatz den in einer Listbox anzuzeigenden String.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String getDisplayString(Dataset ds)
  {
    return substituteVars(displayTemplate, ds);
  }

  /**
   * Ersetzt "%{SPALTENNAME}" in str durch den Wert der entsprechenden Spalte im
   * datensatz.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static String substituteVars(String str, Dataset datensatz)
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
   * Vergleicht die String-Repräsentation ({@link #toString()}) zweier Listenelemente
   * über die compareTo()-Methode der Klasse String.
   * 
   * @param o
   *          das DatasetListElement mit dem verglichen werden soll
   * @return Rückgabewert von this.toString().compareTo(o.toString())
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public int compareTo(DatasetListElement o)
  {
    return this.toString().compareTo(o.toString());
  }

  /**
   * Liefert <code>true</code> zurück, wenn die String-Repräsentation (
   * {@link #toString()}) des übergebenen DatasetListElements gleich (im Hinblick auf
   * {@link String#equals(Object)} ist zu der String-Repräsentation von this.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o instanceof DatasetListElement)
    {
      return this.toString().equals(o.toString());
    }
    return false;
  }

  public int hashCode()
  {
    return this.toString().hashCode();
  }
}
