/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.db;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;

public class ThingyDatasource extends RAMDatasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ThingyDatasource.class);

  private static final Pattern SPALTENNAME = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

  /**
   * Erzeugt eine neue ThingyDatasource.
   *
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser
   *          ThingyDatasource bereits vollständig instanziierten Datenquellen.
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser
   *          ThingyDatasource enthält.
   * @param context
   *          der Kontext relativ zu dem URLs aufgelöst werden sollen.
   */
  public ThingyDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc, URL context)
      throws IOException
  {
    String name = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    String urlStr = parseConfig(sourceDesc, "URL", () -> L.m("URL of data source \"{0}\" is missing", name));

    try
    {
      URL url = new URL(context, ConfigThingy.urlEncode(urlStr));
      ConfigThingy conf = new ConfigThingy(name, url);

      ConfigThingy schemaDesc = conf.get("Schema");

      List<String> schema = new ArrayList<>();
      String[] schemaOrdered = new String[schemaDesc.count()];
      int i = 0;
      for (ConfigThingy spalteConfig : schemaDesc)
      {
        String spalte = spalteConfig.toString();
        if (!SPALTENNAME.matcher(spalte).matches())
        {
          throw new ConfigurationErrorException(L.m(
              "Error in definition of data source {0}: Column \"{1}\" does not comply with the syntax of identifiers",
              name, spalte));
        }
        if (schema.contains(spalte))
        {
          throw new ConfigurationErrorException(L.m(
              "Error in Defition of data source {0}: Column \"{1}\" was defined twice in schema",
              name, spalte));
        }
        schema.add(spalte);
        schemaOrdered[i++] = spalte;
      }

      String[] keyCols = parseKeys(sourceDesc, name, schema);
      List<Dataset> data = parseData(conf, name, schema, schemaOrdered,
          keyCols);

      init(name, schema, data);
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m(
          "Error in Conf-File of data source {0}: Section 'Schema' is missing",
          name), x);
    }
    catch (MalformedURLException e)
    {
      throw new ConfigurationErrorException(
          L.m("Error in Conf-file of data source {0}: Error in URL \"{1}\": ",
              name, urlStr),
          e);
    }
    catch (SyntaxErrorException e)
    {
      throw new ConfigurationErrorException(
          L.m("Error in Conf-file of data source {0}: ", name), e);
    }
  }

  private List<Dataset> parseData(ConfigThingy dataDesc, String name, List<String> schema,
      String[] schemaOrdered, String[] keyCols)
  {
    List<Dataset> data = new ArrayList<>();
    try
    {
      ConfigThingy daten = dataDesc.get("Daten");

      for (ConfigThingy dsDesc : daten)
      {
        data.add(createDataset(dsDesc, schema, schemaOrdered, keyCols));
      }
    }
    catch (ConfigurationErrorException x)
    {
      throw new ConfigurationErrorException(L.m("Error in Conf-file of data source {0}: ", name), x);
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(
          L.m("Error in Conf-file of data source {0}: Section 'Daten' is missing", name), x);
    }

    return data;
  }

  private String[] parseKeys(ConfigThingy sourceDesc, String name, List<String> schema)
  {
    List<String> keyCols = new ArrayList<>();
    try
    {
      ConfigThingy keys = sourceDesc.get("Schluessel");
      // Exception werfen, falls kein Schluessel angegeben
      keys.getFirstChild();
      for (ConfigThingy key : keys)
      {
        String spalte = key.toString();
        keyCols.add(spalte);
        if (!schema.contains(spalte))
        {
          throw new ConfigurationErrorException(L.m(
              "Error in Conf-file of data source {0}: Key column \"{1}\" is not defined in schema",
              name, spalte));
        }
      }
    } catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(
          L.m("Missing or incorrect key(...) specification for data source {0}", name), x);
    }
    return keyCols.toArray(new String[keyCols.size()]);
  }

  /**
   * Erzeugt ein neues MyDataset aus der Beschreibung dsDesc. Die Methode
   * erkennt automatisch, ob die Beschreibung in der Form ("Spaltenwert1",
   * "Spaltenwert2",...) oder der Form (Spalte1 "Wert1" Spalte2 "Wert2" ...)
   * ist.
   *
   * @param schema
   *          das Datenbankschema
   * @param schemaOrdered
   *          das Datenbankschema mit erhaltener Spaltenreihenfolge entsprechend
   *          Schema-Sektion.
   * @param keyCols
   *          die Schlüsselspalten
   * @throws ConfigurationErrorException
   *           im Falle von Verstössen gegen diverse Regeln.
   */
  private Dataset createDataset(ConfigThingy dsDesc, List<String> schema, String[] schemaOrdered,
      String[] keyCols)
  { // TESTED
    if (!dsDesc.getName().isEmpty())
      throw new ConfigurationErrorException(L.m("\"{0}\" awaits a preceding open bracket", dsDesc.getName()));
    if (dsDesc.count() == 0)
    {
      return new MyDataset(schema, keyCols);
    }
    try
    {
      if (dsDesc.getFirstChild().count() == 0)
        return createDatasetOrdered(dsDesc, schema, schemaOrdered, keyCols);
      else
        return createDatasetUnordered(dsDesc, schema, keyCols);
    } catch (NodeNotFoundException e)
    {
      LOGGER.error("", e);
    }
    return null;
  }

  /**
   * Erzeugt ein neues MyDataset aus der Beschreibung dsDesc. dsDesc muss in der
   * Form (Spalte1 "Spaltenwert1" Spalte2 "Spaltenwert2 ...) sein.
   *
   * @throws ConfigurationErrorException
   *           bei verstössen gegen diverse Regeln
   */
  private Dataset createDatasetUnordered(ConfigThingy dsDesc, List<String> schema, String[] keyCols)
  { // TESTED
    Map<String, String> data = new HashMap<>();
    Iterator<ConfigThingy> iter = dsDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy spaltenDaten = iter.next();
      String spalte = spaltenDaten.getName();
      if (!schema.contains(spalte))
      {
        throw new ConfigurationErrorException(
            L.m("Data set has column \"{0}\" which is not defined within the schema", spalte));
      }
      String wert = spaltenDaten.toString();
      data.put(spalte, wert);
    }
    return new MyDataset(schema, data, keyCols);
  }

  /**
   * Erzeugt ein neues MyDataset aus der Beschreibung dsDesc. dsDesc muss in der
   * Form ("Spaltenwert1" "Spaltenwert2 ...) sein.
   *
   * @throws ConfigurationErrorException
   *           bei verstössen gegen diverse Regeln
   */
  private Dataset createDatasetOrdered(ConfigThingy dsDesc, List<String> schema,
      String[] schemaOrdered, String[] keyCols)
  { // TESTED
    if (dsDesc.count() > schemaOrdered.length)
      throw new ConfigurationErrorException(L.m("Data set has more fields than the schema"));

    Map<String, String> data = new HashMap<>();
    int i = 0;
    Iterator<ConfigThingy> iter = dsDesc.iterator();
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

    private List<String> schema;

    public MyDataset(List<String> schema, String[] keyCols)
    {
      this.schema = schema;
      data = new HashMap<>();
      initKey(keyCols);
    }

    public MyDataset(List<String> schema, Map<String, String> data, String[] keyCols)
    { // TESTED
      this.schema = schema;
      this.data = data;
      initKey(keyCols);
    }

    /**
     * Setzt aus den Werten der Schlüsselspalten separiert durch KEY_SEPARATOR
     * den Schlüssel zusammen.
     *
     * @param keyCols
     *          die Namen der Schlüsselspalten
     */
    private void initKey(String[] keyCols)
    { // TESTED
      StringBuilder buffy = new StringBuilder();
      for (int i = 0; i < keyCols.length; ++i)
      {
        String str = data.get(keyCols[i]);
        if (str != null)
        {
          buffy.append(str);
        }
        if (i + 1 < keyCols.length)
        {
          buffy.append(KEY_SEPARATOR);
        }
      }
      key = buffy.toString();
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException(L.m("Column {0} does not exist!", columnName));
      return data.get(columnName);
    }

    @Override
    public String getKey()
    { // TESTED
      return key;
    }
  }
}
