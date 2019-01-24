/* 
 * Dateiname: UnionDatasource.java
 * Projekt  : WollMux
 * Funktion : Datasource, die die Vereinigung 2er Datasources darstellt
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 07.11.2005 | BNK | Erstellung
 * 10.11.2005 | BNK | getestet und debuggt
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Datasource, die die Vereinigung 2er Datasources darstellt
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UnionDatasource implements Datasource
{
  private Datasource source1;

  private Datasource source2;

  private String source1Name;

  private String source2Name;

  private Set<String> schema;

  private String name;

  /**
   * Erzeugt eine neue UnionDatasource.
   * 
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser UnionDatasource
   *          bereits vollständig instanziierten Datenquellen.
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser UnionDatasource
   *          enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen (zur Zeit nicht
   *          verwendet).
   */
  public UnionDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, URL context) throws ConfigurationErrorException
  {
    try
    {
      name = sourceDesc.get("NAME").toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m("NAME der Datenquelle fehlt"));
    }

    try
    {
      source1Name = sourceDesc.get("SOURCE1").toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m(
        "SOURCE1 der Datenquelle \"%1\" fehlt", name));
    }

    try
    {
      source2Name = sourceDesc.get("SOURCE2").toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m(
        "SOURCE2 der Datenquelle \"%1\" fehlt", name));
    }

    source1 = nameToDatasource.get(source1Name);
    source2 = nameToDatasource.get(source2Name);

    if (source1 == null)
      throw new ConfigurationErrorException(
        L.m(
          "Fehler bei Initialisierung von Datenquelle \"%1\": Referenzierte Datenquelle \"%2\" nicht (oder fehlerhaft) definiert",
          name, source1Name));

    if (source2 == null)
      throw new ConfigurationErrorException(
        L.m(
          "Fehler bei Initialisierung von Datenquelle \"%1\": Referenzierte Datenquelle \"%2\" nicht (oder fehlerhaft) definiert",
          name, source2Name));

    /*
     * Anmerkung: Die folgende Bedingung ist "unnötig" streng, aber um sie
     * aufzuweichen (z.B. Gesamtschema ist Vereinigung der Schemata) wäre es
     * erforderlich, einen Dataset-Wrapper zu implementieren, der dafür sorgt, dass
     * alle Datasets, die in QueryResults zurück- geliefert werden das selbe Schema
     * haben. Solange dafür keine Notwendigkeit ersichtlich ist, spare ich mir diesen
     * Aufwand.
     */
    Set<String> schema1 = source1.getSchema();
    Set<String> schema2 = source2.getSchema();
    if (!schema1.containsAll(schema2) || !schema2.containsAll(schema1))
    {
      Set<String> difference1 = new HashSet<String>(schema1);
      difference1.removeAll(schema2);
      Set<String> difference2 = new HashSet<String>(schema2);
      difference2.removeAll(schema1);
      StringBuffer buf1 = new StringBuffer();
      Iterator<String> iter = difference1.iterator();
      while (iter.hasNext())
      {
        buf1.append(iter.next());
        if (iter.hasNext()) buf1.append(", ");
      }
      StringBuffer buf2 = new StringBuffer();
      iter = difference2.iterator();
      while (iter.hasNext())
      {
        buf2.append(iter.next());
        if (iter.hasNext()) buf2.append(", ");
      }
      throw new ConfigurationErrorException(
        L.m(
          "Datenquelle \"%1\" fehlen die Spalten: %2 und Datenquelle \"%3\" fehlen die Spalten: %4",
          source1Name, buf2, source2Name, buf1));
    }

    schema = new HashSet<String>(schema1);
  }

  public Set<String> getSchema()
  {
    return schema;
  }

  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
      throws TimeoutException
  {
    long time = new Date().getTime();
    QueryResults res1 = source1.getDatasetsByKey(keys, timeout);
    time = (new Date().getTime()) - time;
    timeout -= time;
    if (timeout <= 0)
      throw new TimeoutException(
        L.m(
          "Datenquelle \"%1\" konnte Anfrage getDatasetsByKey() nicht schnell genug beantworten",
          source1Name));
    QueryResults res2 = source2.getDatasetsByKey(keys, timeout);
    return new QueryResultsUnion(res1, res2);
  }

  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  public QueryResults find(List<QueryPart> query, long timeout)
      throws TimeoutException
  {
    long time = new Date().getTime();
    QueryResults res1 = source1.find(query, timeout);
    time = (new Date().getTime()) - time;
    timeout -= time;
    if (timeout <= 0)
      throw new TimeoutException(L.m(
        "Datenquelle \"%1\" konnte Anfrage find() nicht schnell genug beantworten",
        source1Name));
    QueryResults res2 = source2.find(query, timeout);
    return new QueryResultsUnion(res1, res2);
  }

  public String getName()
  {
    return name;
  }

}
