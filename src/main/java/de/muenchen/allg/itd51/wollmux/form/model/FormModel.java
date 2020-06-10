/*
 * Dateiname: FormController.java
 * Projekt  : WollMux
 * Funktion : Stellt UI bereit, um ein Formulardokument zu bearbeiten.
 *
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * 27.12.2005 | BNK | Erstellung
 * 27.01.2006 | BNK | JFrame-Verwaltung nach FormGUI ausgelagert.
 * 02.02.2006 | BNK | Ein/Ausblendungen begonnen
 * 05.05.2006 | BNK | Condition -> Function, kommentiert
 * 17.05.2006 | BNK | AUTOFILL, PLAUSI, Übergabe an FormModel
 * 18.05.2006 | BNK | Fokus-Änderungen an formModel kommunizieren
 *                  | TIP und HOTKEY bei Tabs unterstützen
 *                  | leere Tabs ausgrauen
 *                  | nextTab und prevTab implementiert
 * 29.05.2006 | BNK | Umstellung auf UIElementFactory.Context
 * 31.05.2006 | BNK | ACTION "funcDialog"
 * 19.06.2006 | BNK | Auch Werte für Felder, die nicht geautofilled sind an FormModel kommunizieren bei Startup
 * 10.09.2006 | BNK | [P1007]Abfangen von mehr als 512 Elementen auf einem Tab.
 * 10.09.2006 | BNK | Tabs scrollen, nicht hintereinander gruppieren.
 * 17.11.2006 | BNK | +setValue()
 * 08.01.2007 | BNK | intelligentere Behandlung der TAB-Taste
 * 28.03.2007 | BNK | Buttonanpassung verarbeiten bei mergeFormDescriptors().
 * 10.12.2007 | BNK | [R3582]Vertikale Scrollbar immer anzeigen
 * 28.04.2008 | BNK | [R19465]Fokus auf erstem Element, nicht auf Tab
 * 08.03.2010 | ERT | [R6331]Scrollfunktion in der wollmux formular gui
 * 02.06.2010 | BED | Unterstützung von ACTION "saveTempAndOpenExt"
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.form.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.functions.Values.SimpleMap;
import de.muenchen.allg.itd51.wollmux.form.config.FormConfig;
import de.muenchen.allg.itd51.wollmux.form.config.TabConfig;
import de.muenchen.allg.itd51.wollmux.form.config.VisibilityGroupConfig;

/**
 * Beschreibung eines Formulars. Enthält auch die Business-Logik.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormModel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormModel.class);

  /**
   * Die Funktionsbibliothek, die für das Interpretieren von Plausis etc, herangezogen wird.
   */
  private final FunctionLibrary funcLib;

  /**
   * Die Dialogbibliothek, die die Dialoge liefert, die für die automatische Befüllung von
   * Formularfeldern benötigt werden.
   */
  private final DialogLibrary dialogLib;

  /**
   * Der Kontext, in dem Funktionen geparst werden.
   */
  private final Map<Object, Object> functionContext;

  /**
   * Bildet GROUPS Bezeichner auf die entsprechenden Group-Instanzen ab. Key ist die ID der
   * Sichtbarkeitsgruppe.
   */
  private final Map<String, VisibilityGroup> visiblities = new HashMap<>();

  /**
   * Bildet Formularelemente auf die entsprechenden Control-Instanzen ab. Key ist die ID des
   * Formularelementes.
   */
  private final Map<String, Control> formControls = new LinkedHashMap<>();

  /**
   * Bildet Namen von Funktionsdialogen auf Lists von Controls ab, deren AUTOFILL von diesem
   * Funktionsdialog abhängen.
   */
  private Map<String, List<Control>> mapDialogNameToListOfControlsWithDependingAutofill = new HashMap<>();

  /**
   * Sammlung aller Listener, die informiert werden, wenn sich ein Formularfeld ändert (Wert oder
   * Status).
   */
  private List<FormValueChangedListener> listener = new ArrayList<>();

  /**
   * Sammlung aller Listener, die informiert werden, wenn sich eine Sichtbarkeit verändert.
   */
  private List<VisibilityChangedListener> vListener = new ArrayList<>(1);

  /**
   * Ein neues Formular.
   *
   * @param conf
   *          Die Beschreibung des Formulars.
   * @param frameTitle
   *          Der Title des LibreOffice Fensters.
   * @param functionContext
   *          der Kontext für Funktionen, die einen benötigen.
   * @param funcLib
   *          die Funktionsbibliothek, die zur Auswertung von Plausis etc. herangezogen werden soll.
   * @param dialogLib
   *          die Dialogbibliothek, die die Dialoge bereitstellt, die für automatisch zu befüllende
   *          Formularfelder benötigt werden.
   * @param presetValues
   *          Die gesetzten Werte im Dokument.
   * @throws FormModelException
   *           Fehlerhaftes Formular.
   */
  @SuppressWarnings("squid:S3776")
  public FormModel(FormConfig conf, Map<Object, Object> functionContext, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<String, String> presetValues) throws FormModelException
  {
    this.functionContext = functionContext;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;

    for (VisibilityGroupConfig config : conf.getVisibilities())
    {
      VisibilityGroup group = new VisibilityGroup(config, funcLib, dialogLib, functionContext);
      if (!visiblities.containsKey(config.getGroupId()))
      {
        visiblities.put(config.getGroupId(), group);
      }
    }

    for (TabConfig tab : conf.getTabs())
    {
      for (UIElementConfig config : tab.getControls())
      {
        Control control = new Control(config, funcLib, dialogLib, functionContext);
        config.getGroups().forEach(id -> control.addGroup(visiblities.get(id)));
        addFormField(control);
      }
      for (UIElementConfig config : tab.getButtons())
      {
        addFormField(new Control(config, funcLib, dialogLib, functionContext));
      }
    }
    for (Control control : formControls.values())
    {
      storeDepsForFormField(control);
    }

    // Gespeicherte Werte und/oder Autofill setzen
    SimpleMap values = idToValue();
    for (Control control : formControls.values())
    {
      String value = "";
      if (presetValues.containsKey(control.getId()))
      {
        value = presetValues.get(control.getId());
      } else
      {
        value = control.computeValue(values);
      }
      if (!value.equals(control.getValue()))
      {
        control.setValue(value);
        values.put(control.getId(), value);
      }
    }

    for (Control control : formControls.values())
    {
      control.setOkay(values);
    }
    for (VisibilityGroup group : visiblities.values())
    {
      storeDepsForVisibility(group);
    }
  }

  public DialogLibrary getDialogLib()
  {
    return dialogLib;
  }

  public Map<Object, Object> getFunctionContext()
  {
    return functionContext;
  }

  public FunctionLibrary getFuncLib()
  {
    return funcLib;
  }

  /**
   * Liefert eine Sichtbarkeitsgruppe mit der ID groupId.
   *
   * @param groupId
   *          Die ID der Gruppe.
   * @return Die Sichtbarkeitsgruppe mit ID groupId.
   */
  public VisibilityGroup getGroup(String groupId)
  {
    return visiblities.get(groupId);
  }

  /**
   * Get a control.
   * 
   * @param controlId
   *          The id of the control.
   * @return The cotnrol or null if there is no control with this id.
   */
  public Control getControl(String controlId)
  {
    return formControls.get(controlId);
  }

  /**
   * Setzt den Wert des Formularelementes mit ID id auf den Wert value und informiert die Listener
   * entsprechend.
   *
   * @param id
   *          Die ID des Formularelementes.
   * @param value
   *          Der neue Wert.
   */
  public void setValue(final String id, final String value)
  {
    if (formControls.containsKey(id) && !formControls.get(id).getValue().equals(value))
    {
      Control field = formControls.get(id);
      SimpleMap modified = new SimpleMap();

      // Abhängige Felder berechnen
      field.computeNewValues(value, idToValue(), modified);
      SimpleMap newValues = idToValue();
      newValues.putAll(modified);
      List<VisibilityGroup> modifiedGroups = new ArrayList<>();

      // Neue Werte übernehmen und Listener informieren
      for (Map.Entry<String, String> changedEntries : modified)
      {
        Control control = formControls.get(changedEntries.getKey());
        control.setValue(changedEntries.getValue());
        control.setOkay(newValues);
        for (FormValueChangedListener l : listener)
        {
          l.valueChanged(control.getId(), control.getValue());
          l.statusChanged(control.getId(), control.isOkay());
        }
        modifiedGroups.addAll(control.getDependingGroups());
      }
      modifiedGroups.forEach(g -> g.computeVisibility(newValues));

      for (VisibilityChangedListener l : vListener)
      {
        for (VisibilityGroup g : modifiedGroups)
        {
          l.visibilityChanged(g.getGroupId(), g.isVisible());
        }
      }
    }
  }

  /**
   * Liefert den Wert des Formularelementes mit der Id id.
   *
   * @param id
   *          Die Id des Formularelementes.
   * @return Der Wert des Formularelementes.
   * @throws FormModelException
   *           Ein Formularelement mit der Id id existiert nicht.
   */
  public String getValue(final String id) throws FormModelException
  {
    if (formControls.containsKey(id))
    {
      return formControls.get(id).getValue();
    }
    throw new FormModelException("Unbekanntes Formularelement " + id);
  }

  /**
   * Get collection of {@link Control} with equal group id.
   * 
   * @param groupId
   *          Group id.
   * @return Collection of {@link Control}.
   */
  public Collection<Control> getControlsByGroupId(String groupId)
  {
    List<Control> controls = new ArrayList<>();

    for (Map.Entry<String, Control> entry : formControls.entrySet())
    {
      for (VisibilityGroup vs : entry.getValue().getGroups())
      {
        if (vs.getGroupId().equals(groupId))
        {
          controls.add(entry.getValue());
        }
      }
    }

    return controls;
  }

  /**
   * Liefert den Status (Plausi) des Formularelementes mit der Id id.
   *
   * @param id
   *          Die Id des Formularelementes.
   * @return Der Status des Formularelementes.
   * @throws FormModelException
   *           Ein Formularelement mit der Id id existiert nicht.
   */
  public boolean getStatus(final String id) throws FormModelException
  {
    if (formControls.containsKey(id))
    {
      return formControls.get(id).isOkay();
    }
    throw new FormModelException("Unbekanntes Formularelement " + id);
  }

  /**
   * Setzt für die Controls, die vom Dialog dialogName abhängigen, die Werte entsprechend ihrer
   * Autofill-Funktion.
   *
   * @param dialogName
   *          Der Name des Dialogs.
   */
  public void setDialogAutofills(String dialogName)
  {
    for (Control c : mapDialogNameToListOfControlsWithDependingAutofill.get(dialogName))
    {
      c.getAutofill()
          .ifPresent(autofill -> setValue(c.getId(), autofill.getString(idToValue())));
    }
  }

  /**
   * Besitzt das Formular ein Feld mit der ID fieldId?
   *
   * @param fieldId
   *          Die ID des gesuchten Feldes.
   * @return True falls ein solches Feld existiert, sonst False.
   */
  public boolean hasFieldId(String fieldId)
  {
    return formControls.containsKey(fieldId);
  }

  /**
   * Erzeugt aus den Werten eine Map für die Funktionen.
   *
   * @return Ein Map mit Werten die Funktionen als Parameter übergeben werden können.
   */
  private SimpleMap idToValue()
  {
    SimpleMap values = new Values.SimpleMap();
    for (Map.Entry<String, Control> entry : formControls.entrySet())
    {
      values.put(entry.getKey(), entry.getValue().getValue());
    }
    return values;
  }

  /**
   * Falls das Control einen Autofill hat, wird das Control für alle Funktionsdialoge, die der
   * Autofill referenziert in die entsprechende Liste in
   * {@link #mapDialogNameToListOfControlsWithDependingAutofill} beingetragen.
   *
   * @param contro
   *          Das Control mit dem Autofill.
   */
  private void storeAutofillFunctionDialogDeps(Control control)
  {
    control.getAutofill().ifPresent(autofill -> {
      Set<String> funcDialogNames = new HashSet<>();
      autofill.getFunctionDialogReferences(funcDialogNames);
      for (String dialogName : funcDialogNames)
      {
        if (!mapDialogNameToListOfControlsWithDependingAutofill.containsKey(dialogName))
          mapDialogNameToListOfControlsWithDependingAutofill.put(dialogName,
              new ArrayList<Control>(1));

        List<Control> l = mapDialogNameToListOfControlsWithDependingAutofill.get(dialogName);
        l.add(control);
      }
    });
  }

  /**
   * Fügt diesem Model ein Formularelement hinzu und berechnet den initialen Wert so wie die
   * Abhängigkeiten.
   *
   * @param control
   *          Das neue Formularelement.
   */
  private void addFormField(Control control)
  {
    if (formControls.containsKey(control.getId()))
    {
      LOGGER.error("ID \"{}\" mehrfach vergeben", control.getId());
    }
    formControls.put(control.getId(), control);
  }

  /**
   * Speichert für ein Formularelement die Abhängigkeiten zu anderen Formularelementen. Sollte erst
   * aufgerufen werden, nachdem alle Formularelemente angelegt wruden.
   *
   * @param control
   *          Das Formularelement, für das die Abhängigkeiten in anderen Formularelementen
   *          eingetragen werden sollen.
   */
  private void storeDepsForFormField(Control control)
  {
    storeDeps(control);
    storeAutofillFunctionDialogDeps(control);
  }

  /**
   * Parst die Sichtbarkeitsinformationen und berechnet den aktuellen Wert (sichtbar, unsichtbar).
   *
   * @param visibilityDesc
   *          der Sichtbarkeit-Knoten der Formularbeschreibung oder ein leeres ConfigThingy falls
   *          der Knoten nicht existiert.
   */
  private void storeDepsForVisibility(VisibilityGroup group)
  {
    String[] deps = group.getCondition().parameters();
    for (int i = 0; i < deps.length; ++i)
    {
      String elementId = deps[i];
      if (formControls.containsKey(elementId))
      {
        formControls.get(elementId).addDependingGroup(group);
      }
    }

    // Sichtbarkeitsstatus setzen
    group.computeVisibility(idToValue());
  }

  /**
   * Falls ein Formularelement eine Plausi und/oder ein Autofill hat, werden entsprechende
   * Abhängigkeiten in den Maps erfasst.
   *
   * @param control
   *          Das zu betrachtende Formularelement.
   */
  private void storeDeps(Control control)
  {
    control.getAutofill().ifPresent(autofill -> Stream.of(autofill
        .parameters())
        .filter(id -> {
          if (!formControls.containsKey(id))
            LOGGER.warn("Unbekanntes Controlelement {} wird referenziert in {}", id,
                    control.getId());
          return formControls.containsKey(id);
        }).map(formControls::get).forEach(f -> f.addDependingAutoFillFormField(control)));
    Stream.of(control.getPlausi().parameters()).filter(id -> {
      if (!formControls.containsKey(id))
        LOGGER.warn("Unbekanntes Controlelement {} wird referenziert in {}", id,
                control.getId());
      return formControls.containsKey(id);
    }).map(formControls::get).forEach(f -> f.addDependingPlausiFormField(control));
    control.addDependingPlausiFormField(control);
  }

  /**
   * Fügt dem Modell einen weiteren Listener für Wert- oder Statusänderungen hinzu.
   *
   * @param l
   *          Der neue Listener.
   * @param notify
   *          Soll der Listener über den aktuellen Zustand des Modells informiert werden?
   */
  public void addFormModelChangedListener(FormValueChangedListener l, boolean notify)
  {
    this.listener.add(l);
    if (notify)
    {
      formControls.values().forEach(c -> {
        switch (c.getType())
        {
        case TEXTFIELD:
        case TEXTAREA:
        case COMBOBOX:
        case CHECKBOX:
        case LISTBOX:
          l.valueChanged(c.getId(), c.getValue());
          l.statusChanged(c.getId(), c.isOkay());
          break;
        default:
          break;
        }
      });
    }
  }

  /**
   * Fügt dem Modell einen weiteren Listener für Sichtbarkeitsänderungen hinzu.
   *
   * @param l
   *          Der neue Listener.
   * @param notify
   *          Soll der Listener über den aktuellen Zustand des Modells informiert werden?
   */
  public void addVisibilityChangedListener(VisibilityChangedListener l, boolean notify)
  {
    this.vListener.add(l);
    if (notify)
    {
      visiblities.values().forEach(g -> l.visibilityChanged(g.getGroupId(), g.isVisible()));
    }
  }
}
