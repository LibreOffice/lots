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
package de.muenchen.allg.itd51.wollmux.core.form.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.functions.Values.SimpleMap;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

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
   * Bildet Tabs auf die entsprechenden Tab-Instanzen ab. Key ist die ID des Tab.s
   */
  private Map<String, Tab> tabs = new LinkedHashMap<>();

  /**
   * Der Titel des Formulars.
   */
  private String title;

  /**
   * Die Farbe mit der Felder mit fehlerhafter Eingabe (PLAUSI) eingefärbt werden.
   */
  private Color plausiMarkerColor;

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
   * @param listener
   *          Der Listener für Änderungen an den Formularwerten.
   * @param vListener
   *          Der Listener für Änderungen von Sichtbarkeiten.
   * @throws FormModelException
   *           Fehlerhaftes Formular.
   */
  @SuppressWarnings("squid:S3776")
  public FormModel(ConfigThingy conf, String frameTitle,
      Map<Object, Object> functionContext, FunctionLibrary funcLib, DialogLibrary dialogLib,
      Map<String, String> presetValues) throws FormModelException
  {
    this.functionContext = functionContext;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;

    // Formulartitle auswerten
    String formTitle = conf.getString("TITLE", L.m("Unbenanntes Formular"));
    if (frameTitle != null)
    {
      title = frameTitle + " - " + formTitle;
    } else
    {
      title = formTitle;
    }

    // Tabs auswerten
    final ConfigThingy fensterDesc = conf.query("Fenster");
    try
    {
      tabs = new LinkedHashMap<>(fensterDesc.count());
      for (ConfigThingy tabConf : fensterDesc.getLastChild())
      {
        Tab tab = Tab.create(tabConf, this);
        tabs.put(tab.getId(), tab);
        for (Control control : tab.getControls())
        {
          addFormField(control);
        }
      }
      for (Control control : formControls.values())
      {
        storeDepsForFormField(control);
      }
    } catch (NodeNotFoundException e)
    {
      throw new FormModelException(L.m("Schlüssel 'Fenster' fehlt in %1", conf.getName()),
          e);
    }

    // Farbe für fehlerhafte Formularwerte auswerten
    try
    {
      plausiMarkerColor = Color
          .decode(conf.get("PLAUSI_MARKER_COLOR", 1).getLastChild().toString());
    } catch (Exception x)
    {
      plausiMarkerColor = Color.PINK;
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

    for (Map.Entry<String, Control> entry : formControls.entrySet())
    {
      entry.getValue().setOkay(values);
    }

    // Sichtbarkeiten auswerten
    try
    {
      ConfigThingy visibilityDesc = conf.query("Sichtbarkeit");
      if (visibilityDesc.count() > 0)
      {
        visibilityDesc = visibilityDesc.getLastChild();
      }
      setVisibility(visibilityDesc);
    } catch (NodeNotFoundException x)
    {
      LOGGER.error("", x);
    }
  }

  public String getTitle()
  {
    return title;
  }

  public Map<String, Tab> getTabs()
  {
    return tabs;
  }

  public Color getPlausiMarkerColor()
  {
    return plausiMarkerColor;
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
   * Liefert eine Sichtbarkeitsgruppe mit der ID groupId. Wenn die ID bisher nicht existiert, wird
   * eine neue Gruppe angelegt.
   *
   * @param groupId
   *          Die ID der Gruppe.
   * @return Die Sichtbarkeitsgruppe mit ID groupId.
   */
  public VisibilityGroup getGroup(String groupId)
  {
    if (!visiblities.containsKey(groupId))
    {
      visiblities.put(groupId, new VisibilityGroup(groupId));
    }

    return visiblities.get(groupId);
  }

  /**
   * Parst die Funktion im Kontext dieses Models.
   *
   * @param func
   *          Die Funktionsbeschreibung.
   * @return Die Funktion.
   */
  public Function createFunction(ConfigThingy func)
  {
    try
    {
      return FunctionFactory.parseGrandchildren(func, getFuncLib(), getDialogLib(),
        getFunctionContext());
    } catch (ConfigurationErrorException e)
    {
      LOGGER.info("Funktion konnte nicht geparst werden.", e);
      return null;
    }
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
    if (control.getType() == FormType.CHECKBOX || control.getType() == FormType.COMBOBOX
        || control.getType() == FormType.TEXTAREA || control.getType() == FormType.TEXTFIELD)
    {
      if (formControls.containsKey(control.getId()))
      {
        LOGGER.error(L.m("ID \"%1\" mehrfach vergeben", control.getId()));
      }
      formControls.put(control.getId(), control);
    }
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
  private void setVisibility(ConfigThingy visibilityDesc)
  {
    for (ConfigThingy visRule : visibilityDesc)
    {
      // Sichtbarkeitsfunktion parsen.
      String groupId = visRule.getName();
      Function cond;
      try
      {
        cond = FunctionFactory.parseChildren(visRule, getFuncLib(), getDialogLib(), getFunctionContext());
      } catch (ConfigurationErrorException x)
      {
        LOGGER.error("", x);
        continue;
      }

      // Falls keine Gruppe mit entsprechender Id existiert, dann legen wir einfach eine leere
      // Gruppe an.
      if (!visiblities.containsKey(groupId))
        visiblities.put(groupId, new VisibilityGroup(groupId));

      // Group mit der entsprechenden Bezeichnung heraussuchen und ihr condition-Feld setzten.
      // Fehler ausgeben wenn condition bereits gesetzt.
      VisibilityGroup group = visiblities.get(groupId);
      try
      {
        group.setCondition(Optional.ofNullable(cond));
      } catch (FormModelException e)
      {
        LOGGER.error(e.getMessage(), e);
      }

      // Für jeden Parameter der condition-Funktion eine Abhängigkeit im FormControl registrieren.
      String[] deps = cond.parameters();
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
    control.getAutofill().ifPresent(autofill -> Stream.of(autofill.parameters())
        .filter(id -> {
          if (!formControls.containsKey(id))
            LOGGER.warn("Unbekanntes Controlelement {} wird referenziert in {}", id,
                control.getId());
          return formControls.containsKey(id);
        }).map(formControls::get).forEach(f -> f.addDependingAutoFillFormField(control)));
    control.getPlausi().ifPresent(plausi -> Stream.of(plausi.parameters())
        .filter(id -> {
          if (!formControls.containsKey(id))
            LOGGER.warn("Unbekanntes Controlelement {} wird referenziert in {}", id,
                control.getId());
          return formControls.containsKey(id);
        }).map(formControls::get).forEach(f -> f.addDependingPlausiFormField(control)));
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
        l.valueChanged(c.getId(), c.getValue());
        l.statusChanged(c.getId(), c.isOkay());
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
