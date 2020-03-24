package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Table;
import com.sun.star.util.XCloseBroadcaster;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.mailmerge.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;

/**
 * This model provides access to the records and meta data of a data source.
 */
public interface DatasourceModel extends XCloseBroadcaster
{
  /**
   * Add a listener to the model waiting for changes.
   *
   * @param listener
   *          The listener.
   */
  void addDatasourceListener(DatasourceModelListener listener);

  /**
   * Remove a listener.
   *
   * @param listener
   *          The listener.
   */
  void removeDatasourceListener(DatasourceModelListener listener);

  /**
   * Provide the name of this data source.
   *
   * @return The name.
   */
  String getName();

  /**
   * A data source can have several tables.
   *
   * @return A list of all tables.
   */
  List<String> getTableNames();

  /**
   * Activate a table.
   *
   * @param tableName
   *          The name of the table or sheet.
   * @throws NoTableSelectedException
   *           A table with this name doesn't exist.
   */
  void activateTable(String tableName) throws NoTableSelectedException;

  /**
   * Get the name of the currently selected table.
   *
   * @return The name of table.
   * @throws NoTableSelectedException
   *           A table has to be selected before this method can used.
   */
  String getActivatedTable() throws NoTableSelectedException;

  /**
   * Get the column names of a table.
   *
   * @return A list of column names in the same order as {@link #getRecord(int)} provides the data.
   * @throws NoTableSelectedException
   *           A table has to be selected before this method can used.
   */
  Set<String> getColumnNames() throws NoTableSelectedException;

  /**
   * Does this model support adding columns?
   *
   * @return True if columns can be added, false otherwise.
   */
  boolean supportsAddColumns();

  /**
   * Adds some columns to a table.
   *
   * @param mapIdToSubstitution
   *          For each entry a new column is added. The column name is the key and the values for
   *          each record are build from the substitutions.
   * @throws UnsupportedOperationException
   *           If {@link #supportsAddColumns()} returns false.
   * @throws NoTableSelectedException
   *           A table has to be selected before this method can used.
   */
  public void addColumns(Map<String, FieldSubstitution> mapIdToSubstitution)
      throws NoTableSelectedException;

  /**
   * The content of a table.
   *
   * @return The content.
   * @throws NoTableSelectedException
   *           A table has to be selected before this method can used.
   */
  Table<Integer, String, String> getData() throws NoTableSelectedException;

  /**
   * Get the number of records in a table.
   *
   * @return The number of records.
   * @throws NoTableSelectedException
   *           A table has to be selected before this method can used.
   */
  int getNumberOfRecords() throws NoTableSelectedException;

  /**
   * Get a record of a table.
   *
   * @param index
   *          The index of the record. Only visible rows are counted, invisible rows are ignored.
   * @return The data in the same order as {@link #getColumnNames()}. If the index is bigger than
   *         {@link #getNumberOfRecords()} a list of empty strings is returned. In case of an error
   *         an empty list is returned.
   * @throws NoTableSelectedException
   *           A table has to be selected before this method can used.
   */
  Map<String, String> getRecord(int index) throws NoTableSelectedException;

  /**
   * Opens a dialog to modify the data source.
   */
  void toFront();

  /**
   * Create a settings to be stored in the mail merge template.
   *
   * @return The settings.
   * @throws NoTableSelectedException
   *           A table has to be selected before this method can used.
   */
  ConfigThingy getSettings() throws NoTableSelectedException;

  /**
   * Close this model and all its dependencies.
   */
  void dispose();
}
