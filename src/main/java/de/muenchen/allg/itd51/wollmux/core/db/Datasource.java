/*
 * Dateiname: Datasource.java
 * Projekt  : WollMux
 * Funktion : Interface für Datenquellen, die der DJ verwalten kann
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 27.10.2005 | BNK | Erstellung
 * 28.10.2005 | BNK | Erweiterung
 * 28.10.2005 | BNK | +getName()
 * 31.10.2005 | BNK | +find()
 * 03.11.2005 | BNK | besser kommentiert
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

/**
 * Interface für Datenquellen, die der DJ verwalten kann. ACHTUNG! Die Konstruktoren
 * dieser Klasse dürfen keine potentiell lange blockierenden Aktionen (zum Beispiel
 * Netzverbindung herstellen) ausführen. Sie dürfen auch nicht versagen, falls
 * irgendeine Rahmenbedingung nicht gegeben ist, die nur für Zugriffe auf die
 * Datensätze relevant ist (z.B. Verbindung zum LDAP-Server). Der Konstruktor darf
 * (und muss) nur dann versagen, wenn es nicht möglich ist, die Datenquelle in einen
 * Zustand zu bringen, in dem sie die Methoden ausführen kann, die unabhängig von den
 * Datensätzen sind. Am wichtigsten sind hier die Methoden zur Abfrage des Schemas.
 * Für die Methoden, die auf Datensätze zugreifen gilt, dass ihr Versagen aufgrund
 * von Rahmenbedingungen (z.B. kein Netz) nicht dazu führen darf, dass das
 * Datenquellen-Objekt in einen unbrauchbaren Zustand gerät. Wo immer sinnvoll sollte
 * es möglich sein, eine Operation zu einem späteren Zeitpunkt zu wiederholen, wenn
 * die Rahmenbedingungen sich geändert haben, und dann sollte die Operation gelingen.
 * Dies bedeutet insbesondere, dass Verbindungsaufbau zu Servern wo nötig jeweils neu
 * versucht wird und nicht nur einmalig im Konstruktor. In diesem Zusammenhang sei
 * darauf hingewiesen, dass Verbindungen explizit mit close() beendet werden sollten
 * (typischerweise in einem finally() Block, damit der Befehl auch im Ausnahmefall
 * ausgeführt wird), weil die Garbage Collection von Java dies evtl. sehr spät tut.
 * <br>
 * <br>
 * Argumente gegen Datasource-Typ "override": - (korrekte) Suche nur schwierig und
 * ineffizient zu implementieren - würde vermutlich dazu führen, dass Daten im LDAP
 * schlechter gepflegt werden, weil es einfacher ist, einen Override einzuführen
 */
public interface Datasource
{
  /**
   * Liefert eine Liste, die die Titel aller Spalten der Datenquelle enthält.
   */
  public List<String> getSchema();

  /**
   * Liefert alle Datensätze, deren Schlüssel in der Collection keys enthalten sind.
   * Man beachte, dass die Eindeutigkeit von Schlüsseln nur eine Empfehlung
   * darstellt. Die Anzahl der zurückgelieferten Datensätze kann also die Anzahl der
   * übergebenen Schlüssel übersteigen.
   * 
   * @param timeout
   *          die maximale Zeit in Millisekunden, die vergehen darf, bis die Funktion
   *          zurückkehrt.
   * @throws TimeoutException
   *           falls die Anfrage nicht rechtzeitig beendet werden konnte.
   */
  public QueryResults getDatasetsByKey(Collection<String> keys, long timeout)
      throws TimeoutException;

  /**
   * Liefert alle Datensätze, die alle Bedingungen von query (Liste von
   * {@link QueryPart}s) erfüllen. Ist query leer, werden keine Datensätze
   * zurückgeliefert. Enthält query Bedingungen über Spalten, die die Datenbank nicht
   * hat, werden keine Datensätze zurückgeliefert.
   * 
   * @param timeout
   *          die maximale Zeit in Millisekunden, die vergehen darf, bis die Funktion
   *          zurückkehrt.
   * @throws TimeoutException
   *           falls die Anfrage nicht rechtzeitig beendet werden konnte.
   */
  public QueryResults find(List<QueryPart> query, long timeout)
      throws TimeoutException;

  /**
   * Liefert eine implementierungsabhängige Teilmenge der Datensätze der Datenquelle.
   * Wenn möglich sollte die Datenquelle hier all ihre Datensätze zurückliefern oder
   * zumindest soviele wie möglich. Es ist jedoch auch erlaubt, dass hier gar keine
   * Datensätze zurückgeliefert werden. Wenn sinnvoll sollte anstatt des Werfens
   * einer TimeoutException ein Teil der Daten zurückgeliefert werden.
   * 
   * @throws TimeoutException
   *           falls ein Fehler auftritt oder die Anfrage nicht rechtzeitig beendet
   *           werden konnte. In letzterem Fall ist das Werfen dieser Exception
   *           jedoch nicht Pflicht und die Datenquelle kann stattdessen den Teil der
   *           Ergebnisse zurückliefern, die in der gegebenen Zeit gewonnen werden
   *           konnten.
   */
  public QueryResults getContents(long timeout) throws TimeoutException;

  /**
   * Liefert den Namen dieser Datenquelle.
   */
  public String getName();

  public default String parseConfig(ConfigThingy source, String key, Supplier<String> errorMessage)
  {
    return source.get(key, ConfigurationErrorException.class, errorMessage.get()).toString();
  }
}
