//TODO L.m()
/*
* Dateiname: ThingyDatasource.java
* Projekt  : WollMux
* Funktion : Datasource, die ihre Daten aus einer ConfigThingy-Datei bezieht.
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
* 27.10.2005 | BNK | Erstellung
* 03.11.2005 | BNK | besser kommentiert
* 10.11.2005 | BNK | Fehlermeldung, wenn in Daten() ein benannter Datensatz auftaucht
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;

public class ThingyDatasource extends RAMDatasource
{
  private static final Pattern SPALTENNAME = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  
  /**
   * Erzeugt eine neue ThingyDatasource.
   * @param nameToDatasource enthält alle bis zum Zeitpunkt der Definition
   *        dieser ThingyDatasource bereits vollständig instanziierten
   *        Datenquellen.
   * @param sourceDesc der "Datenquelle"-Knoten, der die Beschreibung
   *        dieser ThingyDatasource enthält.
   * @param context der Kontext relativ zu dem URLs aufgelöst werden sollen.
   */
  public ThingyDatasource(Map nameToDatasource, ConfigThingy sourceDesc, URL context)
  throws ConfigurationErrorException, IOException
  {
    String name;
    String urlStr;
    String[] keyCols;
    try{ name = sourceDesc.get("NAME").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("NAME der Datenquelle fehlt");
    }
    
    try{ urlStr = sourceDesc.get("URL").toString();} 
    catch(NodeNotFoundException x) {
      throw new ConfigurationErrorException("URL der Datenquelle "+name+" fehlt");
    }
    
    try
    {
      URL url = new URL(context, ConfigThingy.urlEncode(urlStr));
      ConfigThingy conf = new ConfigThingy(name, url);
      
      ConfigThingy schemaDesc;
      try{ schemaDesc = conf.get("Schema"); }
      catch(NodeNotFoundException x) {
        throw new ConfigurationErrorException("Fehler in Conf-Datei von Datenquelle "+name+": Abschnitt 'Schema' fehlt"); 
      }
      
      Set<String> schema = new HashSet<String>();
      String[] schemaOrdered = new String[schemaDesc.count()];
      Iterator iter = schemaDesc.iterator();
      int i = 0;
      while (iter.hasNext()) 
      {
        String spalte = iter.next().toString();
        if (!SPALTENNAME.matcher(spalte).matches())
          throw new ConfigurationErrorException("Fehler in Definition von Datenquelle "+name+": Spalte \""+spalte+"\" entspricht nicht der Syntax eines Bezeichners");
        if (schema.contains(spalte))
          throw new ConfigurationErrorException("Fehler in Definition von Datenquelle "+name+": Spalte \""+spalte+"\" doppelt aufgeführt im Schema");
        schema.add(spalte);
        schemaOrdered[i++] = spalte;
      }
     
      try{ 
        ConfigThingy keys = sourceDesc.get("Schluessel");
        keyCols = new String[keys.count()];
        keys.getFirstChild(); //Exception werfen, falls kein Schluessel angegeben
        iter = keys.iterator();
        i = 0;
        while (iter.hasNext()) 
        {
          String spalte = iter.next().toString();
          keyCols[i++] = spalte; 
          if (!schema.contains(spalte))
            throw new ConfigurationErrorException("Fehler in Definition von Datenquelle "+name+": Schluessel-Spalte \""+spalte+"\" ist nicht im Schema aufgeführt"); 
        }
      } 
      catch(NodeNotFoundException x) {
        throw new ConfigurationErrorException("Fehlende oder fehlerhafte Schluessel(...) Spezifikation für Datenquelle "+name);
      }
      
      ConfigThingy daten;
      try{ daten = conf.get("Daten"); }
      catch(NodeNotFoundException x) {
        throw new ConfigurationErrorException("Fehler in Conf-Datei von Datenquelle "+name+": Abschnitt 'Daten' fehlt"); 
      }
      
      List<Dataset> data = new Vector<Dataset>(daten.count());
      
      iter = daten.iterator();
      while (iter.hasNext())
      {
        ConfigThingy dsDesc = ((ConfigThingy)iter.next());
        try{
          data.add(createDataset(dsDesc, schema, schemaOrdered, keyCols));
        } catch(ConfigurationErrorException x)
        {
          throw new ConfigurationErrorException("Fehler in Conf-Datei von Datenquelle "+name+": "+x.getMessage());
        }
      }
      
      init(name, schema, data);
      
    }
    catch (MalformedURLException e)
    {
      throw new ConfigurationErrorException("Fehler in Definition von Datenquelle "+name+": Fehler in URL \""+urlStr+"\": "+e.getMessage());
    }
    catch (SyntaxErrorException e)
    {
      throw new ConfigurationErrorException("Fehler in Conf-Datei von Datenquelle "+name+": "+e.getMessage());
    }
  }

  /**
   * Erzeugt ein neues MyDataset aus der Beschreibung dsDesc.
   * Die Methode erkennt automatisch, ob die Beschreibung in der Form
   *    ("Spaltenwert1", "Spaltenwert2",...)
   * oder der Form
   *    (Spalte1 "Wert1" Spalte2 "Wert2" ...)
   * ist.
   * @param schema das Datenbankschema
   * @param schemaOrdered das Datenbankschema mit erhaltener Spaltenreihenfolge
   *        entsprechend Schema-Sektion.
   * @param keyCols die Schlüsselspalten
   * @throws ConfigurationErrorException im Falle von Verstössen gegen diverse
   * Regeln.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private Dataset createDataset(ConfigThingy dsDesc, Set<String> schema, String[] schemaOrdered, String[] keyCols) throws ConfigurationErrorException
  { //TESTED
    if (!dsDesc.getName().equals("")) throw new ConfigurationErrorException("Öffnende Klammer erwartet vor \""+dsDesc.getName()+"\"");
    if (dsDesc.count() == 0) return new MyDataset(schema, keyCols);
    try
    {
      if (dsDesc.getFirstChild().count() == 0) 
        return createDatasetOrdered(dsDesc, schema, schemaOrdered, keyCols);
      else
        return createDatasetUnordered(dsDesc, schema, keyCols);
    }
    catch (NodeNotFoundException e) { Logger.error(e);}
    return null;
  }

  /**
   * Erzeugt ein neues MyDataset aus der Beschreibung dsDesc. dsDesc muss in der
   * Form (Spalte1 "Spaltenwert1" Spalte2 "Spaltenwert2 ...) sein.
   * @throws ConfigurationErrorException bei verstössen gegen diverse Regeln
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private Dataset createDatasetUnordered(ConfigThingy dsDesc, Set<String> schema, String[] keyCols) throws ConfigurationErrorException
  { //TESTED
    Map<String, String> data = new HashMap<String, String>();
    Iterator iter = dsDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy spaltenDaten = (ConfigThingy)iter.next();
      String spalte = spaltenDaten.getName();
      if (!schema.contains(spalte))
        throw new ConfigurationErrorException("Datensatz hat Spalte \""+spalte+"\", die nicht im Schema aufgeführt ist");
      String wert = spaltenDaten.toString();
      data.put(spalte, wert);
    }
    return new MyDataset(schema, data, keyCols);
  }

  /**
   * Erzeugt ein neues MyDataset aus der Beschreibung dsDesc. dsDesc muss in der
   * Form ("Spaltenwert1" "Spaltenwert2 ...) sein.
   * @throws ConfigurationErrorException bei verstössen gegen diverse Regeln
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private Dataset createDatasetOrdered(ConfigThingy dsDesc, Set<String> schema, String[] schemaOrdered, String[] keyCols) throws ConfigurationErrorException
  { //TESTED
    if (dsDesc.count() > schemaOrdered.length)
      throw new ConfigurationErrorException("Datensatz hat mehr Felder als das Schema");

    Map<String, String> data = new HashMap<String, String>();
    int i = 0;
    Iterator iter = dsDesc.iterator();
    while (iter.hasNext())
    {
      String spalte = schemaOrdered[i];
      String wert = iter.next().toString();
      data.put(spalte, wert);
      ++i;
    }
    return new MyDataset(schema, data, keyCols);
  }

  private static class MyDataset implements Dataset
  {
    private static final String KEY_SEPARATOR = "£#%&|";
    private Map<String, String> data;
    private String key;
    private Set<String> schema;
    
    public MyDataset(Set<String> schema, String[] keyCols)
    {
      this.schema = schema;
      data = new HashMap<String, String>();
      initKey(keyCols);
    }
    
    public MyDataset(Set<String> schema, Map<String, String> data, String[] keyCols) 
    { //TESTED
      this.schema = schema;
      this.data = data;
      initKey(keyCols);
    }
    
    /**
     * Setzt aus den Werten der Schlüsselspalten separiert durch KEY_SEPARATOR
     * den Schlüssel zusammen.
     * @param keyCols die Namen der Schlüsselspalten
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void initKey(String[] keyCols)
    { //TESTED
      StringBuffer buffy = new StringBuffer();
      for (int i = 0; i < keyCols.length; ++i)
      {
        String str = data.get(keyCols[i]);
        if (str != null) buffy.append(str);
        if (i + 1 < keyCols.length) buffy.append(KEY_SEPARATOR);
      }
      key = buffy.toString();
    }
    
    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName)) throw new ColumnNotFoundException("Spalte "+columnName+" existiert nicht!");
      return data.get(columnName);
    }
    
    public String getKey()
    { //TESTED
      return key;
    }
  }
}
