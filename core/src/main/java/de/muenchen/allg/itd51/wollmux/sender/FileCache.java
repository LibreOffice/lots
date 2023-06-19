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
package de.muenchen.allg.itd51.wollmux.sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.EventObject;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * A sender cache using a file as persistence. The file is in ConfigThingy format.
 */
public class FileCache implements SenderCache
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FileCache.class);

  private File cacheFile;

  private List<String> schema;

  private String selectedKey;
  private int selectedSameKeyIndex;

  private List<SenderConf> data;

  /**
   * Create a new cache based on a ConfigThingy file.
   *
   * @param cacheFile
   *          The file.
   * @param context
   *          The context for includes in the file.
   */
  public FileCache(File cacheFile, URL context)
  {
    this.cacheFile = cacheFile;
    try
    {
      ConfigThingy cacheData = readFromFile(cacheFile, context);
      List<String> newSchema = readSchema(cacheData);
      List<SenderConf> newData = readData(cacheData, newSchema);
      Pair<String, Integer> newSelected = readSelected(cacheData);
      schema = newSchema;
      data = newData;
      selectedKey = newSelected.getKey();
      selectedSameKeyIndex = newSelected.getValue();
    } catch (Exception e)
    {
      // If cache is corrupt, discard all data.
      schema = new ArrayList<>();
      data = Collections.emptyList();
      selectedKey = null;
      selectedSameKeyIndex = -1;
    }
  }

  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  @Override
  public int getSelectedSameKeyIndex()
  {
    return selectedSameKeyIndex;
  }

  @Override
  public String getSelectedKey()
  {
    return selectedKey;
  }

  @Override
  public List<SenderConf> getData()
  {
    return data;
  }

  private ConfigThingy readFromFile(File cacheFile, URL context) throws SenderException
  {
    try (Reader reader = new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8))
    {
      return new ConfigThingy(cacheFile.getPath(), context, reader);
    } catch (IOException | SyntaxErrorException e)
    {
      throw new SenderException(L.m("Cache-File {0} could not be read.", cacheFile.getPath()), e);
    }
  }

  private List<String> readSchema(ConfigThingy cacheData) throws SenderException
  {
    List<String> newSchema = new ArrayList<>();
    try
    {
      for (ConfigThingy column : cacheData.get("Schema"))
      {
        newSchema.add(column.toString());
      }
    } catch (NodeNotFoundException e)
    {
      throw new SenderException("Schema konnte nicht aus dem Cache gelesen werden");
    }
    return newSchema;
  }

  private List<SenderConf> readData(ConfigThingy cacheData, List<String> newSchema) throws SenderException
  {
    List<SenderConf> newData = new ArrayList<>();
    try
    {
      for (ConfigThingy dsconf : cacheData.get("Data"))
      {
        String key = dsconf.get("Key").toString();
        Map<String, String> cached = readCachedValues(dsconf, newSchema);
        Map<String, String> override = readOverriddenValues(dsconf, newSchema);
        SenderConf senderConf = new SenderConf(key, cached, override);
        newData.add(senderConf);
      }
    } catch (NodeNotFoundException e)
    {
      throw new SenderException("Daten konnten nicht aus dem Cache geladen werden");
    }
    return newData;
  }

  private Map<String, String> readCachedValues(ConfigThingy dsconf, List<String> schema)
      throws SenderException
  {
    Map<String, String> cached = new HashMap<>();
    try
    {
      ConfigThingy cacheColumns = dsconf.get("Cache");
      for (ConfigThingy dsNode : cacheColumns)
      {
        String spalte = dsNode.getName();
        if (!schema.contains(spalte))
        {
          throw new SenderException(
              "Cache enthält korrupten Datensatz (Spalte " + spalte + " nicht im Schema) => Cache wird ignoriert!");
        }

        cached.put(spalte, dsNode.toString());
      }
    } catch (NodeNotFoundException e)
    {
      // sender without cached values is a valid sender
    }
    return cached;
  }

  private Map<String, String> readOverriddenValues(ConfigThingy dsconf, List<String> schema) throws SenderException
  {
    Map<String, String> dsoverride = new HashMap<>();
    try
    {
      for (ConfigThingy dsNode : dsconf.get("Override"))
      {
        String spalte = dsNode.getName();
        if (!schema.contains(spalte))
        {
          throw new SenderException(
              "Cache enthält korrupten Datensatz (Spalte " + spalte + " nicht im Schema) => Cache wird ignoriert!");
        }

        dsoverride.put(spalte, dsNode.toString());
      }
    } catch (NodeNotFoundException e)
    {
      // sender without overridden values is a valid sender
    }
    return dsoverride;
  }

  private Pair<String, Integer> readSelected(ConfigThingy cacheData) throws SenderException
  {
    try
    {
      ConfigThingy ausgewaehlt = cacheData.get("Ausgewaehlt");
      return Pair.of(ausgewaehlt.getFirstChild().toString(), Integer.parseInt(ausgewaehlt.getLastChild().toString()));
    } catch (NodeNotFoundException | NumberFormatException e)
    {
      throw new SenderException("Ausgewählter Sender konnte nicht bestimmt werden.");
    }
  }

  ConfigThingy createCacheData(List<String> mainSchema, Sender selected, List<Sender> pal) throws SenderException
  {
    if (mainSchema == null)
    {
      throw new SenderException("Kann Cache nicht speichern, weil nicht initialisiert.");
    }

    ConfigThingy conf = new ConfigThingy(cacheFile.getPath());

    ConfigThingy schemaConf = conf.add("Schema");
    for (String column : mainSchema)
    {
      schemaConf.add(column);
    }

    ConfigThingy datenConf = dumpData(pal, mainSchema);
    conf.addChild(datenConf);

    ConfigThingy ausgewaehlt = conf.add("Ausgewaehlt");
    if (selected != null)
    {
      int index = 0;
      for (Sender s : pal)
      {
        if (selected.equals(s))
        {
          break;
        }
        if (selected.getKey().equals(s.getKey()))
        {
          index++;
        }
      }
      ausgewaehlt.add(selected.getKey());
      ausgewaehlt.add(Integer.toString(index));
    }
    return conf;
  }

  private ConfigThingy dumpData(List<Sender> senderList, List<String> mainSchema) throws SenderException
  {
    ConfigThingy conf = new ConfigThingy("Data");
    for (Sender ds : senderList)
    {
      ConfigThingy dsConf = conf.add("");
      dsConf.add("Key").add(ds.getKey());

      if (ds.isFromDatabase())
      {
        dumpCachedValues(mainSchema, ds, dsConf);
      }
      dumpOverriddenValues(mainSchema, ds, dsConf);
    }

    return conf;
  }

  private void dumpCachedValues(List<String> mainSchema, Sender ds, ConfigThingy dsConf) throws SenderException
  {
    ConfigThingy cacheConf = dsConf.add("Cache");
    for (String spalte : mainSchema)
    {
      try
      {
        String wert = ds.getDataset().get(spalte);
        if (wert != null)
        {
          cacheConf.add(spalte).add(wert);
        }
      } catch (ColumnNotFoundException e)
      {
        throw new SenderException("Unbekannte Spalte soll gespeichert werden", e);
      }
    }
  }

  private void dumpOverriddenValues(List<String> mainSchema, Sender ds, ConfigThingy dsConf)
  {
    ConfigThingy overrideConf = dsConf.add("Override");
    for (String spalte : mainSchema)
    {
      String wert = ds.getOverridenValues().get(spalte);
      if (wert != null)
      {
        overrideConf.add(spalte).add(wert);
      }
    }
  }

  /**
   * Write children of the configuration to the file.
   *
   * @param file
   *          The file to write into.
   * @param conf
   *          The configuration to write.
   * @throws IOException
   *           Can't write the file.
   */
  private void writeConfToFile(ConfigThingy conf) throws SenderException
  {
    if (cacheFile != null)
    {
      try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(cacheFile), StandardCharsets.UTF_8))
      {
        out.write("\uFEFF");
        out.write(conf.stringRepresentation(true, '"'));
      } catch (IOException e)
      {
        throw new SenderException("Konnte Cache nicht speichern", e);
      }
    } else {
      throw new SenderException("Keine Cache-Datei vorhanden.");
    }
  }

  @Override
  public void updateContent(EventObject event)
  {
    try
    {
      LOGGER.debug("Speichere Cache nach {}.", cacheFile);
      SenderService service = (SenderService) event.Source;
      ConfigThingy conf = createCacheData(service.getSchema(), service.selectedSender,
          service.getSenderListSorted(Sender.NACHNAME));
      writeConfToFile(conf);
    } catch (Exception e)
    {
      LOGGER.error("Cache konnte nicht gespeichert werden.", e);
    }
  }

  @Override
  public void disposing(EventObject event)
  {
    // nothing to do
  }

}
