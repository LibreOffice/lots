package de.muenchen.allg.itd51.wollmux.core.db;

/**
 * Interface für Datensätze einer Tabelle.
 */
public interface Dataset
{
  /**
   * Liefert den Wert des Datensatzes aus der Spalte columnName (null falls nicht belegt).
   * 
   * @throws ColumnNotFoundException
   *           falls die Spalte nicht existiert. Man beachte, dass dies eine Eigenschaft des
   *           Datenbankschemas ist und nichts damit zu tun hat, ob der Wert des Datensatzes in der
   *           entsprechenden Spalte gesetzt ist.
   */
  public String get(String columnName) throws ColumnNotFoundException;

  /**
   * Liefert den Schlüsselwert dieses Datensatzes. Dieser sollte den Datensatz in seiner Datenbank
   * eindeutig identifizieren muss es aber nicht.
   */
  public String getKey();
}
