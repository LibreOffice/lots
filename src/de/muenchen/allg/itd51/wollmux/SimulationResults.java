/*
 * Dateiname: SimulationResults.java
 * Projekt  : WollMux
 * Funktion : Enthält das Ergebnis eins Simulationslaufs im TextDocumentModel
 * 
 * Copyright (c) 2011-2015 Landeshauptstadt München
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
 * 08-08-2011 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */

package de.muenchen.allg.itd51.wollmux;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;

/**
 * Enthält nach einem Simulationslauf über das {@link #TextDocumentModel} (siehe
 * TextDocumentModel.startSimulation() das Simulationsergebnis. Die Simulation über
 * das TextDocumentModel wurde für die Anbindung des WollMux an den OOo-Seriendruck
 * eingeführt.
 * 
 * @author christoph.lutz
 */
public class SimulationResults
{
  /**
   * Enthält die Zuordnung von FormField-Objekten auf die in der Simulation gesetzten
   * FormField-Inhalte (üblicherweise können FormField-Objekte Trafos enthalten; Der
   * Inhalt ist aber dann schon der Wert, der nach der Berechnung der Trafo gesetzt
   * ist).
   */
  HashMap<FormField, String> mapFormFieldToContentValue;

  /**
   * Enthält die Zuordnung von FormIDs auf Formularwerte.
   */
  HashMap<String, String> mapFormFieldIDToValue;
  
  /**
   * Enthält eine Map mit den Namen aller (bisher gesetzter) Sichtbarkeitsgruppen auf
   * deren aktuellen Sichtbarkeitsstatus (sichtbar = true, unsichtbar = false)
   */
  private HashMap<String, Boolean> mapGroupIdToVisibilityState;
  
  /**
   * Erzeugt ein SimulationResults-Objekt zur Aufnahme von Simulationsergebnissen mit
   * (noch) leerem Inhalt.
   */
  public SimulationResults()
  {
    this.mapFormFieldToContentValue = new HashMap<FormField, String>();
    this.mapFormFieldIDToValue = new HashMap<String, String>();
    this.mapGroupIdToVisibilityState = new HashMap<String, Boolean>();
  }

  /**
   * Simuliert das Setzen eines FormField-Objekts field auf den Wert value.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void setFormFieldContent(FormField field, String value)
  {
    mapFormFieldToContentValue.put(field, value);
  }

  /**
   * Liefert das Set aller über {@link #setFormFieldContent(FormField, String)}
   * gesetzten FormField-Objekte.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public Set<FormField> getFormFields()
  {
    return mapFormFieldToContentValue.keySet();
  }

  /**
   * Liefert den in {@link #setFormFieldContent(FormField, String)} gesetzten Inhalt
   * zum FormField-Objekt field zurück, oder null, wenn es zu diesem Objekt kein
   * Element gesetzt wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public String getFormFieldContent(FormField field)
  {
    if (field == null) return null;
    return mapFormFieldToContentValue.get(field);
  }

  /**
   * Löscht alle bisher mit {@link #setFormFieldValue(String, String)} gesetzten
   * Werte und belegt die Formularwerte des Simulationslaufs mit mapIDToValue neu.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void setFormFieldValues(HashMap<String, String> mapIDToValue)
  {
    this.mapFormFieldIDToValue.clear();
    this.mapFormFieldIDToValue.putAll(mapIDToValue);
  }

  /**
   * Löscht alle bisher bekannten Zuständer der Sichtbarkeitsgruppen und belegt die
   * Gruppensichtbarkeiten des Simulationslaufs mit states neu.
   * 
   * @author Christoph Lutz (CIB software GmbH)
   */
  public void setGroupsVisibilityState(Map<String, Boolean> states)
  {
    mapGroupIdToVisibilityState.putAll(states);
  }

  /**
   * Liefert eine Map mit der Zuordnung aller im Simulationslauf gesetzter formIDs
   * auf die zugehörigen Werte.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public HashMap<String, String> getFormFieldValues()
  {
    return this.mapFormFieldIDToValue;
  }

  /**
   * Setzt den Formularwert von fieldId auf den Wert value oder löscht den Eintrag
   * aus der internen Map wenn value==null ist.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void setFormFieldValue(String fieldId, String value)
  {
    if (value != null)
      mapFormFieldIDToValue.put(fieldId, value);
    else
      mapFormFieldIDToValue.remove(fieldId);
  }
  
  /**
   * Liefert eine Map mit den Namen aller (bisher gesetzter) Sichtbarkeitsgruppen auf
   * deren aktuellen Sichtbarkeitsstatus (sichtbar = true, unsichtbar = false)
   * 
   * @author Christoph Lutz (CIB software GmbH)
   */
  public Map<String, Boolean> getGroupsVisibilityState()
  {
    return mapGroupIdToVisibilityState;
  }

  /**
   * Beschreibt eine Klasse mit einer Funktion zur Verarbeitung des
   * Simulationsergebnisses.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static interface SimulationResultsProcessor
  {
    void processSimulationResults(SimulationResults simRes);
  }
}
