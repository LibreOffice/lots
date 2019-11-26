/*
* Dateiname: Query.java
* Projekt  : WollMux
* Funktion : Speichert den Namen einer Datenquelle sowie eine Suchanfrage darauf.
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
* 24.05.2006 | BNK | Erstellung
* 26.05.2006 | BNK | +getQueryParts()
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Speichert den Namen einer Datenquelle sowie eine Suchanfrage darauf.
 */
public class Query implements Iterable<QueryPart>
{
  /**
   * Liste von QueryParts, die die eigentliche Query beschreiben.
   */
  private List<QueryPart> listOfQueryParts;
  
  /**
   * Der Name der Datenquelle auf der die Suche erfolgen soll.
   */
  private String dbName;
  
  /**
   * Erzeugt eine neue Query.
   * @param dbName der Name die Datenquelle auf der die Suche erfolgen soll.
   * @param listOfQueryParts Liste von QueryParts, die die eigentliche Query 
   *        beschreiben. ACHTUNG! Wird als Referenz eingebunden.
   */
  public Query(String dbName, List<QueryPart> listOfQueryParts)
  {
    this.listOfQueryParts = listOfQueryParts;
    this.dbName = dbName;
  }
  
  /**
   * Liefert den Namen der Datenquelle auf der die Suche erfolgen soll.
   */
  public String getDatasourceName() {return dbName;}
  
  /**
   * Iteriert über die QueryParts, die diese Suchanfrage ausmachen.
   */
  @Override
  public Iterator<QueryPart> iterator() {return listOfQueryParts.iterator();}
  
  /**
   * Liefert die Liste der QueryParts die diese Suchanfrage definieren.
   */
  public List<QueryPart> getQueryParts() {return new Vector<>(listOfQueryParts);}
  
  public int numberOfQueryParts() { return listOfQueryParts.size();}
}
