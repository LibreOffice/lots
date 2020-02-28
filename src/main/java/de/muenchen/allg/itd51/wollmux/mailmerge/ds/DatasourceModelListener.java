package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

/**
 * Listener for changes in a data source.
 */
public interface DatasourceModelListener
{
  /**
   * Called when the data source has changed.
   */
  void datasourceChanged();
}
