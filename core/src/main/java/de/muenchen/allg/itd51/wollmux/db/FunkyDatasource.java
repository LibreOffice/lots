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
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Datasource, die mit WollMux-Funktionen berechnete Spalten ermöglicht. ACHTUNG!
 * Diese Datasource verhält sich bei Suchanfragen nicht entsprechend dem normalen
 * Verhalten einer Datasource, da sie immer auf den Originaldaten sucht, jedoch
 * transformierte Datensätze zurückliefert.
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
   * Erzeugt eine neue FunkyDatasource.
   *
   * @param nameToDatasource
   *          enthält alle bis zum Zeitpunkt der Definition dieser UnionDatasource
   *          bereits vollständig instanziierten Datenquellen.
   * @param sourceDesc
   *          der "Datenquelle"-Knoten, der die Beschreibung dieser UnionDatasource
   *          enthält.
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
      new ColumnTransformer(FunctionFactory.parseTrafos(sourceDesc, "ColumnTransformation", "Spaltenumsetzung",
          funcLib, dialogLib, context));

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
