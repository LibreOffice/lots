/*
* Dateiname: Datasource.java
* Projekt  : WollMux
* Funktion : Interface für Datenquellen, die der DJ verwalten kann
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 27.10.2005 | BNK | Erstellung
* 28.10.2005 | BNK | Erweiterung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.TimeoutException;

/**
 * ACHTUNG! Die Konstruktoren dieser Klasse dürfen keine potentiell
 * lange blockierenden Aktionen (zum Beispiel Netzverbindung herstellen) 
 * ausführen. Sie dürfen auch nicht versagen, falls irgendeine Rahmenbedingung
 * nicht gegeben ist, die nur für Zugriffe auf
 * die Datensätze relevant ist (z.B. Verbindung zum LDAP-Server). 
 * Der Konstruktor darf (und muss) nur dann versagen, wenn es nicht möglich 
 * ist, die Datenquelle in einen Zustand zu bringen, in dem sie die
 * Methoden ausführen kann, die unabhängig von den Datensätzen sind.
 * Am wichtigsten sind hier die Methoden zur Abfrage des Schemas.
 * Für die Methoden, die auf Datensätze zugreifen gilt, dass ihr Versagen
 * aufgrund von Rahmenbedingungen (z.B. kein Netz) nicht dazu führen darf,
 * dass das Datenquellen-Objekt in einen unbrauchbaren Zustand gerät.
 * Woimmer sinnvoll sollte es möglich sein, eine Operation zu einem
 * späteren Zeitpunkt zu wiederholen, wenn die Rahmenbedingungen sich
 * geändert haben, und dann sollte die Operation gelingen. Dies bedeutet
 * insbesondere, dass Verbindungsaufbau zu Servern wo nötig jeweils neu
 * versucht wird und nicht nur einmalig im Konstruktor.
 * 
 * Argumente gegen Datasource-Typ "override":
 * - (korrekte) Suche nur schwierig und ineffizient zu implementieren
 * - würde vermutlich dazu führen, dass Daten im LDAP schlechter gepflegt
 *   werden, weil es einfacher ist, einen Override einzuführen
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Datasource
{
  /**
   * Liefert ein Set, das die Titel aller Spalten der Datenquelle enthält.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Set getSchema();
  
  /**
   * Liefert alle Datensätze, deren Schlüssel in der Collection keys
   * enthalten sind.
   * @param timeout die maximale Zeit in Millisekunden, die vergehen darf, bis die
   * Funktion zurückkehrt.
   * @throws TimeoutException, falls die Anfrage nicht rechtzeitig beendet
   * werden konnte.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public QueryResults getDatasetsByKey(Collection keys, long timeout) 
    throws TimeoutException;
  
}
