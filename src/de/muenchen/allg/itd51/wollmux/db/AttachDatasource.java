//TODO L.m()
/* 
* Dateiname: AttachDatasource.java
* Projekt  : WollMux
* Funktion : Eine Datenquelle, die eine andere Datenquelle um Spalten ergänzt.
* 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330/5980
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 08.11.2005 | BNK | Erstellung
* 09.11.2005 | BNK | find() auf Spalten der ATTACH-Datenquelle unterstützt
* 10.11.2005 | BNK | korrekte Filterung nach Spalten der ATTACH-Datenquelle
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

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;
import de.muenchen.allg.itd51.wollmux.db.checker.DatasetChecker;
import de.muenchen.allg.itd51.wollmux.db.checker.MatchAllDatasetChecker;

/**
 * Eine Datenquelle, die eine andere Datenquelle um Spalten ergänzt.
 * Zur Erstellung der Menge der Ergebnisdatensätze
 * wird jeder Datensatz aus SOURCE1 genau einmal verwendet und 
 * jeder Datensatz aus SOURCE2 beliebig oft (auch keinmal).
 * Unterschiede zu einem richtigen Join:<br><br>
 *  a) Verhindert, dass eine Person 2 mal auftaucht, nur weil es 2 
 *     Einträge mit Verkehrsverbindungen für ihre Adresse gibt<br>
 *  b) Verhindert, dass eine Person rausfliegt, weil es zu ihrer 
 *     Adresse keine
 *     Verkehrsverbindung gibt<br>
 * c) Die Schlüssel der Ergebnisdatensätze bleiben die aus SOURCE1
*    und werden nicht kombiniert aus SOURCE1 und SOURCE2. Das 
*    verhindert, dass ein Datensatz bei einer Änderung der 
*    Adresse aus der lokalen Absenderliste fliegt, weil er beim 
*    Cache-Refresh nicht mehr gefunden wird.
*<br><br>
* In der Ergebnisdatenquelle sind alle Spalten von SOURCE1 unter
* ihrem ursprünglichen Namen, alle Spalten von SOURCE2 unter dem
* Namen von SOURCE2 konkateniert mit "." konkateniert mit dem
* Spaltennamen zu finden.
*<br><br>
* Argument gegen automatische Umbenennung/Aliase für Spalten aus
* SOURCE2, deren Name sich nicht mit einer Spalte aus SOURCE1 stört:<br><br>
* - Der Alias würde verschwinden, wenn die Quelle SOURCE1 später einmal
*   um eine Spalte mit dem entsprechenden Namen erweitert wird.
*   Definitionen, die den Alias verwendet haben verwenden ab da 
*   stillschweigend die Spalte aus SOURCE1, was schwierig zu findende
*   Fehler nach sich ziehen kann.
* @author Matthias Benkmann (D-III-ITD 5.1)
*/
public class AttachDatasource implements Datasource
{
  private static final String CONCAT_SEPARATOR = "__";
  private String name;
  private String source1Name;
  private String source2Name;
  private Datasource source1;
  private Datasource source2;
  private Set<String> schema;
  private String[] match1;
  private String[] match2;
  private String source2Prefix;
  
  /**
   * Erzeugt eine neue AttachDatasource.
   * @param nameToDatasource enthält alle bis zum Zeitpunkt der Definition
   *        dieser AttachDatasource bereits vollständig instanziierten
   *        Datenquellen.
   * @param sourceDesc der "Datenquelle"-Knoten, der die Beschreibung
   *        dieser AttachDatasource enthält.
   * @param context der Kontext relativ zu dem URLs aufgelöst werden sollen
   *        (zur Zeit nicht verwendet).
   */
  public AttachDatasource(Map nameToDatasource, ConfigThingy sourceDesc, URL context)
  throws ConfigurationErrorException
  {
    try{ name = sourceDesc.get("NAME").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("NAME der Datenquelle fehlt");
    }
    
    try{ source1Name = sourceDesc.get("SOURCE").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("SOURCE der Datenquelle "+name+" fehlt");
    }
    
    try{ source2Name = sourceDesc.get("ATTACH").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("ATTACH-Angabe der Datenquelle "+name+" fehlt");
    }
    
    source1 = (Datasource)nameToDatasource.get(source1Name);  
    source2 = (Datasource)nameToDatasource.get(source2Name);
    
    if (source1 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source1Name+"\" nicht (oder fehlerhaft) definiert");
    
    if (source2 == null)
      throw new ConfigurationErrorException("Fehler bei Initialisierung von Datenquelle \""+name+"\": Referenzierte Datenquelle \""+source2Name+"\" nicht (oder fehlerhaft) definiert");

    Set<String> schema1 = source1.getSchema();
    Set schema2 = source2.getSchema();
    
    source2Prefix = source2Name + CONCAT_SEPARATOR;
    
    schema = new HashSet<String>(schema1);
    Iterator iter = schema2.iterator();
    while (iter.hasNext())
    {
      String spalte = (String)iter.next();
      spalte = source2Prefix+spalte;
      if (schema1.contains(spalte))
        throw new ConfigurationErrorException("Kollision mit Spalte \""+spalte+"\" aus Datenquelle \""+source1Name+"\"");
      schema.add(spalte);
    }
    
    ConfigThingy matchesDesc = sourceDesc.query("MATCH");
    int numMatches = matchesDesc.count();
    if (numMatches == 0)
      throw new ConfigurationErrorException("Mindestens eine MATCH-Angabe muss bei Datenquelle \""+name+"\" gemacht werden");
    
    match1 = new String[numMatches];
    match2 = new String[numMatches];
    
    iter = matchesDesc.iterator();
    for (int i = 0; i < numMatches; ++i)
    {
      ConfigThingy matchDesc = (ConfigThingy)iter.next();
      if (matchDesc.count() != 2)
        throw new ConfigurationErrorException("Fehlerhafte MATCH Angabe in Datenquelle \""+name+"\"");
      
      String spalte1 = "";
      String spalte2 = "";
      try{
        spalte1 = matchDesc.getFirstChild().toString();
        spalte2 = matchDesc.getLastChild().toString();
      }catch(NodeNotFoundException x){}
      
      if (!schema1.contains(spalte1))
        throw new ConfigurationErrorException("Spalte \""+spalte1+"\" ist nicht im Schema");
      
      if (!schema2.contains(spalte2))
        throw new ConfigurationErrorException("Spalte \""+spalte2+"\" ist nicht im Schema");
      
      match1[i] = spalte1;
      match2[i] = spalte2;
    }
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getSchema()
   */
  public Set<String> getSchema()
  {
    return schema;
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getDatasetsByKey(java.util.Collection, long)
   */
  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
      throws TimeoutException
  {
    long time = new Date().getTime();
    QueryResults results = source1.getDatasetsByKey(keys, timeout);
    time = (new Date().getTime()) - time;
    timeout -= time;
    if (timeout <= 0) throw new TimeoutException("Datenquelle "+source1Name+" konnte Anfrage getDatasetsByKey() nicht schnell genug beantworten");
    return attachColumns(results, timeout, new MatchAllDatasetChecker());
  }

  public QueryResults getContents(long timeout) throws TimeoutException
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#find(java.util.List, long)
   */
  public QueryResults find(List<QueryPart> query, long timeout) throws TimeoutException
  {
    long time = new Date().getTime();
    List<QueryPart> query1 = new Vector<QueryPart>(query.size()/2);
    List<QueryPart> query2 = new Vector<QueryPart>(query.size()/2);
    List<QueryPart> query2WithPrefix = new Vector<QueryPart>(query.size()/2);
    Iterator<QueryPart> iter = query.iterator();
    while (iter.hasNext())
    {
      QueryPart p = iter.next();
      if (p.getColumnName().startsWith(source2Prefix))
      {
        query2.add(new QueryPart(p.getColumnName().substring(source2Prefix.length()),p.getSearchString()));
        query2WithPrefix.add(p);
      }
      else
        query1.add(p);
    }
    
    /*
     * Die ATTACH-Datenquelle ist normalerweise nur untergeordnet und
     * Spaltenbedingungen dafür schränken die Suchergebnisse wenig ein.
     * Deshalb werten wir falls wir mindestens eine Bedingung an die
     * Hauptdatenquelle haben, die Anfrage auf dieser Datenquelle aus. 
     */
    if (query1.size() > 0)
    {
      QueryResults results = source1.find(query1, timeout);
      time = (new Date().getTime()) - time;
      timeout -= time;
      if (timeout <= 0) throw new TimeoutException("Datenquelle "+source1Name+" konnte Anfrage find() nicht schnell genug beantworten");
      
      DatasetChecker filter = DatasetChecker.makeChecker(query2WithPrefix);
      
      return attachColumns(results, timeout, filter);
    }
    else
    {
      QueryResults results = source2.find(query2, timeout);
      time = (new Date().getTime()) - time;
      timeout -= time;
      if (timeout <= 0) throw new TimeoutException("Datenquelle "+source2Name+" konnte Anfrage find() nicht schnell genug beantworten");
      return attachColumnsReversed(results, timeout);
    }
  }
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Datasource#getName()
   */
  public String getName()
  {
    return name;
  }
  
  private QueryResults attachColumns(QueryResults results, long timeout, DatasetChecker filter) throws TimeoutException
  {
    long endTime = new Date().getTime() + timeout;
    
    List<Dataset> resultsWithAttachments = new Vector<Dataset>(results.size());
    
    Iterator iter = results.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
     
      List<QueryPart> query = new Vector<QueryPart>(match1.length);
      for (int i = 0; i < match1.length; ++i)
      {
        try{
          query.add(new QueryPart(match2[i], ds.get(match1[i])));
        }catch(ColumnNotFoundException x) { Logger.error(x); }
      }
      
      timeout = endTime - (new Date().getTime());
      if (timeout <= 0) throw new TimeoutException();
      QueryResults appendix = source2.find(query, timeout);
      
      Dataset newDataset;
      
      if (appendix.size() == 0)
      {
        newDataset = new ConcatDataset(ds,null);
        if (filter.matches(newDataset)) resultsWithAttachments.add(newDataset);
      }
      else
      {
        Iterator appendixIter = appendix.iterator();
        while (appendixIter.hasNext())
        {
          newDataset = new ConcatDataset(ds,(Dataset)appendixIter.next());
          if (filter.matches(newDataset)) 
          {
            resultsWithAttachments.add(newDataset);
            break;
          }
        }
      }
    }
    
    return new QueryResultsList(resultsWithAttachments);
  }
  
  private QueryResults attachColumnsReversed(QueryResults results, long timeout) throws TimeoutException
  {
    long endTime = new Date().getTime() + timeout;
    
    List<ConcatDataset> resultsWithAttachments = new Vector<ConcatDataset>(results.size());
    
    Iterator iter = results.iterator();
    while (iter.hasNext())
    {
      Dataset ds = (Dataset)iter.next();
      List<QueryPart> query = new Vector<QueryPart>(match1.length);
      for (int i = 0; i < match1.length; ++i)
      {
        try{
          query.add(new QueryPart(match1[i], ds.get(match2[i])));
        }catch(ColumnNotFoundException x) { Logger.error(x); }
      }
      
      timeout = endTime - (new Date().getTime());
      if (timeout <= 0) throw new TimeoutException();
      QueryResults prependix = source1.find(query, timeout);
      
      if (prependix.size() > 0)
      {
        Iterator iter2 = prependix.iterator();
        while (iter2.hasNext())
           resultsWithAttachments.add(new ConcatDataset((Dataset)iter2.next(),ds));
      }
    }
    
    return new QueryResultsList(resultsWithAttachments);
  }


  private class ConcatDataset implements Dataset
  {
    private Dataset ds1;
    private Dataset ds2; //kann null sein!
    
    public ConcatDataset(Dataset ds1, Dataset ds2)
    {
      this.ds1 = ds1;
      this.ds2 = ds2; //kann null sein!
    }
    
    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException("Spalte \""+columnName+"\" ist nicht im Schema");

      if (columnName.startsWith(source2Prefix))
      {
        if (ds2 == null) return null;
        return (ds2.get(columnName.substring(source2Prefix.length())));
      }
      else
        return ds1.get(columnName);
    }

    public String getKey()
    {
      return ds1.getKey();
    }
  }


  
}
