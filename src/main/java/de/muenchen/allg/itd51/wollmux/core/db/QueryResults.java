package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Ergebnisse einer Datenbankanfrage.
 */
public interface QueryResults extends Iterable<Dataset>
{
  /**
   * Die Anzahl der Ergebnisse.
   */
  public int size();
  
  /**
   * Liefert true, falls es keine Ergebnisse gibt.
   */
  public boolean isEmpty();
}
