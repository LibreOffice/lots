package de.muenchen.allg.itd51.wollmux.mailmerge;

/**
 * Listener for changes of {@link ConnectionModel}.
 */
public interface ConnectionModelListener
{
  /**
   * A data source connection has changed.
   */
  void connectionsChanged();
}
