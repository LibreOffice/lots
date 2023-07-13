/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.db;

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
