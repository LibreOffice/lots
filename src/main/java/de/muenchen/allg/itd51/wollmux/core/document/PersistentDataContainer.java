/*
 * Dateiname: PersistentDataContainer.java
 * Projekt  : WollMux
 * Funktion : Beschreibt einen Container in dem dokumentgebundene Metadaten
 *            des WollMux persistent abgelegt werden können.
 *
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.core.document;

/**
 * Beschreibt einen Container in dem dokumentgebundene Metadaten des WollMux
 * persistent abgelegt werden können.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public interface PersistentDataContainer
{
  /**
   * Attributname zur Einstellung des Speichermodus für persistente Daten
   */
  static final String PERSISTENT_DATA_MODE = "PERSISTENT_DATA_MODE";
  /**
   * Wert 'annotation' des Attributs PERSISTENT_DATA_MODE
   */
  static final String PERSISTENT_DATA_MODE_ANNOTATION = "annotation";
  /**
   * Wert 'transition' des Attributs PERSISTENT_DATA_MODE
   */
  static final String PERSISTENT_DATA_MODE_TRANSITION = "transition";
  /**
   * Wert 'rdf' des Attributs PERSISTENT_DATA_MODE
   */
  static final String PERSISTENT_DATA_MODE_RDF = "rdf";
  /**
   * Wert 'rdfReadLegacy' des Attributs PERSISTENT_DATA_MODE
   */
  static final String PERSISTENT_DATA_MODE_RDFREADLEGACY = "rdfReadLegacy";

  /**
   * Die Methode liefert die unter ID dataId gespeicherten Daten zurück oder null,
   * wenn keine vorhanden sind.
   */
  public String getData(DataID dataId);

  /**
   * Speichert dataValue mit der id dataId im zugehörigen ODF-Dokument. Falls bereits
   * Daten mit der selben dataId vorhanden sind, werden sie überschrieben. Die Aktion
   * wird erst garantiert nach Ausführung von flush() im Dokument persistiert.
   */
  public void setData(DataID dataId, String dataValue);

  /**
   * Entfernt die mit dataId bezeichneten Daten, falls vorhanden. Die Aktion wird
   * erst garantiert nach Ausführung von flush() im Dokument persistiert.
   */
  public void removeData(DataID dataId);

  /**
   * Garantiert, dass bereits getätigte Aufrufe von setData(...) bzw. removeData(...)
   * auch tatsächlich persistiert werden. Diese Optimierungs-Methode kann
   * rechenzeitaufwendige Anweisungen enthalten, die nicht mit jedem
   * setData(...)-Vorgang ausgeführt werden müssen.
   */
  public void flush();

  /**
   * Liste aller möglichen DataIDs des WollMux.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public enum DataID {
    /**
     * Die dataId unter der die WollMux-Formularbeschreibung gespeichert wird.
     */
    FORMULARBESCHREIBUNG("WollMuxFormularbeschreibung"),

    /**
     * Die dataId unter der die WollMux-Formularwerte gespeichert werden.
     */
    FORMULARWERTE("WollMuxFormularwerte"),

    /**
     * Die dataId unter der die Metadaten der Seriendruckfunktion gespeichert werden.
     */
    SERIENDRUCK("WollMuxSeriendruck"),

    /**
     * Die dataId unter der der Name der Druckfunktion gespeichert wird.
     */
    PRINTFUNCTION("PrintFunction"),

    /**
     * Die dataId unter der der Name der Druckfunktion gespeichert wird.
     */
    FILENAMEGENERATORFUNC("FilenameGeneratorFunction"),

    /**
     * Die dataId unter der der Typ des Dokuments gespeichert wird.
     */
    SETTYPE("SetType"),

    /**
     * Die dataId unter der die Version des letzten WollMux der das Dokument
     * angefasst hat (vor diesem gerade laufenden) gespeichert wird.
     */
    TOUCH_WOLLMUXVERSION("WollMuxVersion", true),

    /**
     * Die dataId unter der die Version des letzten OpenOffice,orgs das das Dokument
     * angefasst hat (vor diesem gerade laufenden) gespeichert wird.
     */
    TOUCH_OOOVERSION("OOoVersion", true);

    private String name;

    private boolean infodata;

    DataID(String name)
    {
      this.name = name;
      this.infodata = false;
    }

    DataID(String name, boolean infodata)
    {
      this.name = name;
      this.infodata = infodata;
    }

    /**
     * Liefert den Bezeichner der DataID zurück, unter dem die Daten tatsächlich im
     * Storage gespeichert werden.
     */
    public String getDescriptor()
    {
      return name;
    }

    /**
     * Gibt Auskunft darüber, ob es sich bei den unter dieser DataID abgelegten Daten
     * um reine, nicht durch den WollMux interpretierten Infodaten handelt oder
     * nicht.
     */
    public boolean isInfodata()
    {
      return infodata;
    }
  }
}
