/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source that provides data from an existing data source with modified columns.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SchemaDatasource extends Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaDatasource.class);

  private static final Pattern SPALTENNAME = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

  private static final String EMPTY_COLUMN = "";

  private Datasource source;

  private String sourceName;

  private String name;

  private List<String> schema;

  private Map<String, String> mapNewToOld;

  /**
   * Creates a new SchemaDatasource.
   *
   * @param nameToDatasource
   *          contains all data sources that have already been fully instantiated 
   *          up to the point of defining this SchemaDatasource.
   * @param sourceDesc
   *          the "DataSource" node containing the description of this SchemaDatasource.
   * @param context
   *          the context relative to which URLs should be resolved (currently not used).
   */
  public SchemaDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc, URL context)
  {
    name = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    sourceName = parseConfig(sourceDesc, "SOURCE", () -> L.m("SOURCE of data source {0} is missing", name));

    source = nameToDatasource.get(sourceName);

    if (source == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + " Referenced datasource \"{1}\" missing or defined incorrectly", name, sourceName));

    schema = new ArrayList<>(source.getSchema());
    mapNewToOld = new HashMap<>();

    List<String> columnsToDrop = dropColumns(sourceDesc.query("DROP"));
    List<String> columnsToAdd = addColumns(sourceDesc.query("ADD"), columnsToDrop);
    renameColumn(sourceDesc.query("RENAME"), columnsToDrop, columnsToAdd);

    /**
     * For all added columns that do not exist in the original database
     * and are not mapped to a column in the original database through a RENAME,
     * add a pseudomapping to EMPTY_COLUMN so that RenameDataset.get() knows to return null for the column.
     */
    for (String spalte : columnsToAdd)
    {
      if (!schema.contains(spalte) && !mapNewToOld.containsKey(spalte))
        mapNewToOld.put(spalte, EMPTY_COLUMN);
    }

    schema.removeAll(columnsToDrop);
    schema.addAll(columnsToAdd);
  }

  private void renameColumn(ConfigThingy renamesDesc, List<String> columnsToDrop, List<String> columnsToAdd)
  {
    for (ConfigThingy renameDesc : renamesDesc)
    {
      if (renameDesc.count() != 2)
        throw new ConfigurationErrorException(L.m(
          "Incorrect RENAME specification in data source \"{0}\"", name));

      String spalte1 = "";
      String spalte2 = "";
      try
      {
        spalte1 = renameDesc.getFirstChild().toString();
        spalte2 = renameDesc.getLastChild().toString();
      }
      catch (NodeNotFoundException x)
      {
        LOGGER.trace("", x);
      }

      if (!schema.contains(spalte1))
        throw new ConfigurationErrorException(L.m(
          "Column \"{0}\" is not defined in schema", spalte1));

      if (!SPALTENNAME.matcher(spalte2).matches())
        throw new ConfigurationErrorException(L.m(
          "\"{1}\" is not a valid column name", spalte2));

      mapNewToOld.put(spalte2, spalte1);
      columnsToDrop.add(spalte1);
      columnsToDrop.remove(spalte2);
      columnsToAdd.add(spalte2);
    }
  }

  private List<String> addColumns(ConfigThingy adds, List<String> columnsToDrop)
  {
    List<String> columnsToAdd = new ArrayList<>();
    for (ConfigThingy add : adds)
    {
      for (ConfigThingy addColumn : add)
      {
        String spalte = addColumn.toString();
        if (!SPALTENNAME.matcher(spalte).matches())
          throw new ConfigurationErrorException(L.m(
            "\"{0}\" is not a valid column name", spalte));
        columnsToAdd.add(spalte);
        columnsToDrop.remove(spalte);
      }
    }
    return columnsToAdd;
  }

  private List<String> dropColumns(ConfigThingy drops)
  {
    List<String> columnsToDrop = new ArrayList<>();
    for (ConfigThingy drop : drops)
    {
      for (ConfigThingy dropColumn : drop)
      {
        String spalte = dropColumn.toString();
        if (!schema.contains(spalte))
          throw new ConfigurationErrorException(L.m(
            "Column \"{0}\" is not defined in schema", spalte));
        columnsToDrop.add(spalte);
      }
    }
    return columnsToDrop;
  }

  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return wrapDatasets(source.getDatasetsByKey(keys));
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<RenameDataset>(0));
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    List<QueryPart> translatedQuery = new ArrayList<>(query.size());
    Iterator<QueryPart> iter = query.iterator();
    while (iter.hasNext())
    {
      QueryPart p = iter.next();
      String spalte = p.getColumnName();

      if (!schema.contains(spalte)) // this test is not redundant because of DROPs
        return new QueryResultsList(new Vector<RenameDataset>(0));

      String alteSpalte = mapNewToOld.get(spalte);

      if (alteSpalte == /* not equals()!!!! */EMPTY_COLUMN)
        return new QueryResultsList(new Vector<RenameDataset>(0));

      if (alteSpalte != null)
        translatedQuery.add(new QueryPart(alteSpalte, p.getSearchString()));
      else
        translatedQuery.add(p);
    }
    return wrapDatasets(source.find(translatedQuery));
  }

  @Override
  public String getName()
  {
    return name;
  }

  private QueryResults wrapDatasets(QueryResults res)
  {
    List<RenameDataset> wrappedRes = new ArrayList<>(res.size());
    Iterator<Dataset> iter = res.iterator();
    while (iter.hasNext())
      wrappedRes.add(new RenameDataset(iter.next()));

    return new QueryResultsList(wrappedRes);
  }

  private class RenameDataset implements Dataset
  {
    private Dataset ds;

    public RenameDataset(Dataset ds)
    {
      this.ds = ds;
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      // this test is not redundant because of DROPs
      if (!schema.contains(columnName))
        throw new ColumnNotFoundException(L.m("Column \"{0}\" does not exist!",
          columnName));

      String alteSpalte = mapNewToOld.get(columnName);

      if (alteSpalte == /* not equals()!!!! */EMPTY_COLUMN) {
        return null;
      }

      if (alteSpalte != null)
        return ds.get(alteSpalte);
      else
        return ds.get(columnName);
    }

    @Override
    public String getKey()
    {
      return ds.getKey();
    }
  }

}
