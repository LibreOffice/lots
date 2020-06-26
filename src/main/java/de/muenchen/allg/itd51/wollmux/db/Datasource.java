/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
import java.util.List;
import java.util.function.Supplier;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;

/**
 * Interface für Datenquellen, die der DJ verwalten kann. ACHTUNG! Die Konstruktoren dieser Klasse
 * dürfen keine potentiell lange blockierenden Aktionen (zum Beispiel Netzverbindung herstellen)
 * ausführen. Sie dürfen auch nicht versagen, falls irgendeine Rahmenbedingung nicht gegeben ist,
 * die nur für Zugriffe auf die Datensätze relevant ist (z.B. Verbindung zum LDAP-Server). Der
 * Konstruktor darf (und muss) nur dann versagen, wenn es nicht möglich ist, die Datenquelle in
 * einen Zustand zu bringen, in dem sie die Methoden ausführen kann, die unabhängig von den
 * Datensätzen sind. Am wichtigsten sind hier die Methoden zur Abfrage des Schemas. Für die
 * Methoden, die auf Datensätze zugreifen gilt, dass ihr Versagen aufgrund von Rahmenbedingungen
 * (z.B. kein Netz) nicht dazu führen darf, dass das Datenquellen-Objekt in einen unbrauchbaren
 * Zustand gerät. Wo immer sinnvoll sollte es möglich sein, eine Operation zu einem späteren
 * Zeitpunkt zu wiederholen, wenn die Rahmenbedingungen sich geändert haben, und dann sollte die
 * Operation gelingen. Dies bedeutet insbesondere, dass Verbindungsaufbau zu Servern wo nötig
 * jeweils neu versucht wird und nicht nur einmalig im Konstruktor. In diesem Zusammenhang sei
 * darauf hingewiesen, dass Verbindungen explizit mit close() beendet werden sollten (typischerweise
 * in einem finally() Block, damit der Befehl auch im Ausnahmefall ausgeführt wird), weil die
 * Garbage Collection von Java dies evtl. sehr spät tut. <br>
 * <br>
 * Argumente gegen Datasource-Typ "override": - (korrekte) Suche nur schwierig und ineffizient zu
 * implementieren - würde vermutlich dazu führen, dass Daten im LDAP schlechter gepflegt werden,
 * weil es einfacher ist, einen Override einzuführen
 */
public interface Datasource
{
  /**
   * Liefert eine Liste, die die Titel aller Spalten der Datenquelle enthält.
   */
  public List<String> getSchema();

  /**
   * Liefert alle Datensätze, deren Schlüssel in der Collection keys enthalten sind. Man beachte,
   * dass die Eindeutigkeit von Schlüsseln nur eine Empfehlung darstellt. Die Anzahl der
   * zurückgelieferten Datensätze kann also die Anzahl der übergebenen Schlüssel übersteigen.
   * 
   * @param keys
   *          Keys to search against.
   * @return Results as {@link QueryResults}
   */
  public QueryResults getDatasetsByKey(Collection<String> keys);

  /**
   * Liefert alle Datensätze, die alle Bedingungen von query (Liste von {@link QueryPart}s)
   * erfüllen. Ist query leer, werden keine Datensätze zurückgeliefert. Enthält query Bedingungen
   * über Spalten, die die Datenbank nicht hat, werden keine Datensätze zurückgeliefert.
   * 
   * @param query
   *          Query to search against the main datasource.
   * @return Results as {@link QueryResults}
   */
  public QueryResults find(List<QueryPart> query);

  /**
   * Liefert eine implementierungsabhängige Teilmenge der Datensätze der Datenquelle. Wenn möglich
   * sollte die Datenquelle hier all ihre Datensätze zurückliefern oder zumindest soviele wie
   * möglich. Es ist jedoch auch erlaubt, dass hier gar keine Datensätze zurückgeliefert werden.
   */
  public QueryResults getContents();

  /**
   * Liefert den Namen dieser Datenquelle.
   */
  public String getName();

  /**
   * Gets datasource value by given {@link ConfigThingy} and key.
   * 
   * @param source
   *          {@link ConfigThingy} ConfigThingy that should contain a configured datasource.
   * @param key
   *          Name of the datasource that should be found.
   * @param errorMessage
   *          ErrorMessage that is thrown if configuration could not be parsed successfully.
   * @return Value of the datasource, i.e. 'ldap://test.ip'
   */
  public default String parseConfig(ConfigThingy source, String key, Supplier<String> errorMessage)
  {
    return source.get(key, ConfigurationErrorException.class, errorMessage.get()).toString();
  }
}
