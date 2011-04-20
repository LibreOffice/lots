/*
 * Dateiname: PersistentDataContainer.java
 * Projekt  : WollMux
 * Funktion : Beschreibt einen Container in dem dokumentgebundene Metadaten 
 *            des WollMux persistent abgelegt werden können.
 * 
 * Copyright (c) 2010 Landeshauptstadt München
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
 * 19.04.2011 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

/**
 * Beschreibt einen Container in dem dokumentgebundene Metadaten des WollMux
 * persistent abgelegt werden können.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public interface PersistentDataContainer
{
  /**
   * Die Methode liefert die unter ID dataId gespeicherten Daten zurück oder null,
   * wenn keine vorhanden sind.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public String getData(String dataId);

  /**
   * Speichert dataValue mit der id dataId im zugehörigen ODF-Dokument. Falls bereits
   * Daten mit der selben dataId vorhanden sind, werden sie überschrieben. Die Aktion
   * wird erst garantiert nach Ausführung von flush() im Dokument persistiert.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void setData(String dataId, String dataValue);

  /**
   * Entfernt die mit dataId bezeichneten Daten, falls vorhanden. Die Aktion wird
   * erst garantiert nach Ausführung von flush() im Dokument persistiert.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void removeData(String dataId);

  /**
   * Garantiert, dass bereits getätigte Aufrufe von setData(...) bzw. removeData(...)
   * auch tatsächlich persistiert werden. Diese Optimierungs-Methode kann
   * rechenzeitaufwendige Anweisungen enthalten, die nicht mit jedem
   * setData(...)-Vorgang ausgeführt werden müssen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void flush();

}
