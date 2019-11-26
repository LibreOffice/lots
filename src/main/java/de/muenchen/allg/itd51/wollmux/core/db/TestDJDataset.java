/*
 * Dateiname: TestDJDataset.java
 * Projekt  : WollMux
 * Funktion : Implementierung von DJDataset zu Testzwecken.
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
 * 14.10.2005 | BNK | Erstellung
 * 20.10.2005 | BNK | Stark erweitert 
 * 20.10.2005 | BNK | Unterstützung für Fallback
 * 03.11.2005 | BNK | besser kommentiert
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementierung von DJDataset zu Testzwecken. Eine zentrale Eigenschaft von
 * TestDJDataset ist, dass es für Spalten, für die kein Wert gesetzt ist den
 * Spaltennamen als Antwort auf get()-Anfragen liefern kann, so dass alle Spalten von
 * aussen betrachtet belegt sind.
 */
public class TestDJDataset extends DJDatasetBase
{
  /**
   * siehe Konstruktor.
   */
  private Map<String, String> fallback = null;

  /**
   * Erzeugt einen TestDJDataset, der jedes Schema unterstützt und als aus dem LOS
   * kommend betrachtet wird.
   */
  public TestDJDataset()
  {
    super(new HashMap<String, String>(), new HashMap<String, String>(), null);
  }

  /**
   * Erzeugt ein neues TestDJDataset.
   * 
   * @param backingStore
   *          mappt Spaltennamen auf den Spaltenwert des Datensatzes in der
   *          Hintergrunddatenbank. Es müssen nicht alle Spalten enthalten sein. Der
   *          Mechanismus zum automatischen Generieren von Spaltenwerten aus dem
   *          Spaltennamen existiert weiter.
   * @param schema
   *          falls nicht null übergeben wird, erzeugen Zugriffe auf Spalten mit
   *          Namen, die nicht in schema sind Exceptions.
   * @param isFromLOS
   *          legt fest, ob der Datensatz als aus dem LOS kommend betrachtet werden
   *          soll (also insbesondere ob er {@link #set(String, String)} unterstützen
   *          soll).
   * @param fallback
   *          falls fallback nicht null ist, so wird falls der Wert für eine Spalte
   *          nicht gesetzt ist (nicht zu verwechseln mit gesetzt auf den leeren
   *          String!) versucht, anhand dieser Map den Spaltennamen auf einen anderen
   *          Spaltennamen umzusetzen, dessen Wert dann geliefert wird.
   */
  public TestDJDataset(Map<String, String> backingStore, List<String> schema,
      boolean isFromLOS, Map<String, String> fallback)
  {
    super(backingStore, isFromLOS ? new HashMap<String, String>() : null, schema);
    this.fallback = fallback;
  }

  /**
   * Liefert die Map, die dem Konstruktor als backingStore Argument übergeben wurde.
   */
  @Override
  public Map<String, String> getBS()
  {
    return myBS;
  }

  @Override
  public String get(String spaltenName) throws ColumnNotFoundException
  {
    String str = super.get(spaltenName);
    if (str != null) {
      return str;
    }

    if (fallback != null && fallback.containsKey(spaltenName))
      return get(fallback.get(spaltenName));

    return spaltenName;
  }

  @Override
  public DJDataset copy()
  {
    return new TestDJDataset(hasBackingStore() ? new HashMap<>(myBS)
                                              : null, schema, true, fallback);
  }

  @Override
  public void remove()
  {
    // nothing to do
  }

  @Override
  public boolean isSelectedDataset()
  {
    return false;
  }

  @Override
  public void select()
  {
    if (!isFromLOS()) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public String getKey()
  {
    return Integer.toString(this.hashCode());
  }
}
