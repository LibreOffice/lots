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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.dialog.DialogLibrary;
import org.libreoffice.lots.func.FunctionFactory;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.util.L;

/**
 * Data source that enables columns calculated with WollMux functions.
 * CAUTION! This data source does not behave in search queries according to the normal behavior of a data source,
 * as it always searches the original data but returns transformed records.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class FunkyDatasource extends Datasource
{
  private Datasource source;

  private List<String> schema;

  private String name;

  private ColumnTransformer columnTransformer;

  /**
   * Creates a new FunkyDatasource.
   *
   * @param nameToDatasource
   *          Contains all data sources that were fully instantiated up to the point of defining this UnionDatasource.
   * @param sourceDesc
   *          The 'data source' node containing the description of this UnionDatasource.
   */
  public FunkyDatasource(Map<String, Datasource> nameToDatasource,
      ConfigThingy sourceDesc) throws ConfigurationErrorException
  {
    try
    {
      name = sourceDesc.get("NAME", 1).toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m("NAME of data source is missing"));
    }

    String sourceName;
    try
    {
      sourceName = sourceDesc.get("SOURCE", 1).toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(L.m(
        "SOURCE of data source \"{0}\" is missing", name));
    }

    source = nameToDatasource.get(sourceName);

    if (source == null)
      throw new ConfigurationErrorException(L.m("Error during initialization of datasource \"{0}\": "
          + "Referenced datasource \"{1}\" missing or defined incorrectly", name, sourceName));

    /*
     * TODO why not use global FunctionLibrary and global DialogLibrary?
     */
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    Map<Object, Object> context = new HashMap<>();
    columnTransformer =
      new ColumnTransformer(FunctionFactory.parseTrafos(sourceDesc, "ColumnTransformation", funcLib, dialogLib,
    	        context));

    schema = columnTransformer.getSchema();
    schema.addAll(source.getSchema());
  }

  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    return columnTransformer.transform(source.getDatasetsByKey(keys));
  }

  @Override
  public QueryResults getContents()
  {
    return columnTransformer.transform(source.getContents());
  }

  @Override
  public QueryResults find(List<QueryPart> query)
  {
    return columnTransformer.transform(source.find(query));
  }

  @Override
  public String getName()
  {
    return name;
  }

}
