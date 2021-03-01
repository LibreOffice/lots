/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.document;

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
