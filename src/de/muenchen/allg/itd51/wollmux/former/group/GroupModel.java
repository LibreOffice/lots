/*
 * Dateiname: GroupModel.java
 * Projekt  : WollMux
 * Funktion : Eine Sichtbarkeitsgruppe.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * 15.11.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.DuplicateIDException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;
import de.muenchen.allg.itd51.wollmux.former.IDManager.IDChangeListener;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccess;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;

/**
 * Eine Sichtbarkeitsgruppe.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class GroupModel
{
  /**
   * {@link Pattern} für legale IDs.
   */
  private static Pattern ID_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z_0-9]*)");

  /**
   * Für {@link ModelChangeListener#attributeChanged(GroupModel, int, Object)}, gibt
   * an, dass die ID (der Name) der Gruppe sich geändert hat.
   */
  public static final int ID_ATTR = 0;

  /**
   * Die ID (=der Name) dieser Gruppe.
   */
  private IDManager.ID id;

  /**
   * Die Sichtbarkeitsbedingung für diese Gruppe.
   */
  private FunctionSelection condition;

  /**
   * Die {@link ModelChangeListener}, die über Änderungen dieses Models informiert
   * werden wollen.
   */
  private List<ModelChangeListener> listeners = new Vector<ModelChangeListener>(1);

  /**
   * Der FormularMax4000 zu dem dieses Model gehört.
   */
  private FormularMax4000 formularMax4000;

  /**
   * Listener der Änderungen an {@link #id} überwacht.
   */
  private MyIDChangeListener myIDChangeListener;

  /**
   * Erzeugt eine neue Gruppe mit Name/ID id. ACHTUNG! id muss activated sein!
   * 
   * @param condition
   *          wird direkt als Referenz übernommen und bestimmt die
   *          Sichtbarkeitsbedingung dieser Gruppe.
   * @param formularMax4000
   *          der {@link FormularMax4000} zu dem diese Gruppe gehört.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public GroupModel(IDManager.ID id, FunctionSelection condition,
      FormularMax4000 formularMax4000)
  {
    this.id = id;
    // Achtung! Wir müssen eine Referenz halten (siehe addIdChangeListener())
    myIDChangeListener = new MyIDChangeListener();
    id.addIDChangeListener(myIDChangeListener);
    this.condition = condition;
    this.formularMax4000 = formularMax4000;
  }

  /**
   * Liefert den FormularMax4000 zu dem dieses Model gehört.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4000 getFormularMax4000()
  {
    return formularMax4000;
  }

  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden
   * Container aufgerufen werden, der das Model enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void hasBeenRemoved()
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.modelRemoved(this);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * listener wird über Änderungen des Models informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Liefert immer true. Diese Funktion existiert nur, damit der entsprechende Code
   * in {@link AllGroupFuncViewsPanel} analog zu den anderen Panels gehalten werden
   * kann.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public boolean hasFunc()
  {
    return true;
  }

  /**
   * Liefert ein Interface zum Zugriff auf die Sichtbarkeitsbedingung dieses Objekts.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelectionAccess getConditionAccess()
  {
    return new MyConditionAccess();
  }

  /**
   * Liefert die {@link IDManager.ID} dieser Sichtbarkeitsgruppe zurück.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public IDManager.ID getID()
  {
    return id;
  }

  /**
   * Setzt newID als neuen Namen für diese Sichtbarkeitsgruppe und benachrichtigt
   * alle mittels
   * {@link #addListener(de.muenchen.allg.itd51.wollmux.former.group.GroupModel.ModelChangeListener)}
   * registrierten Listener.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * @throws DuplicateIDException
   *           falls newID bereits von einer anderen Sichtbarkeitsgruppe verwendet
   *           wird.
   * 
   * TESTED
   */
  public void setID(String newID) throws DuplicateIDException, SyntaxErrorException
  {
    if (!isLegalID(newID))
      throw new SyntaxErrorException(L.m(
        "'%1' ist keine syntaktisch korrekte ID für Sichtbarkeitsgruppen", newID));
    id.setID(newID);
    /**
     * IDManager.ID ruft MyIDChangeListener.idHasChanged() auf, was wiederum die
     * Listener auf diesem Model benachrichtigt.
     */
  }

  /**
   * Liefert true gdw, id auf {@link #ID_PATTERN} matcht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private boolean isLegalID(String id)
  {
    return ID_PATTERN.matcher(id).matches();
  }

  /**
   * Liefert ein ConfigThingy zurück, dessen Name der Name der Gruppe ist und dessen
   * Inhalt die Definition der Sichtbarkeitsfunktion der Gruppe ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy export()
  {
    return condition.export(id.toString());
  }

  /**
   * Ruft für jeden auf diesem Model registrierten {@link ModelChangeListener} die
   * Methode {@link ModelChangeListener#attributeChanged(GroupModel, int, Object)}
   * auf.
   */
  protected void notifyListeners(int attributeId, Object newValue)
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.attributeChanged(this, attributeId, newValue);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Interface für Listener, die über Änderungen eines Models informiert werden
   * wollen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich geändert hat.
     * 
     * @param model
     *          das Model, das sich geändert hat.
     * @param attributeId
     *          eine der {@link GroupModel#ID_ATTR Attribut-ID-Konstanten}.
     * @param newValue
     *          der neue Wert des Attributs. Die ID wird als {@link IDManager.ID}
     *          übergeben.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void attributeChanged(GroupModel model, int attributeId, Object newValue);

    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit in
     * keiner View mehr angezeigt werden soll).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void modelRemoved(GroupModel model);
  }

  private class MyIDChangeListener implements IDChangeListener
  {
    public void idHasChanged(ID id)
    {
      notifyListeners(ID_ATTR, id);
    }
  }

  /**
   * Diese Klasse leitet Zugriffe weiter an das Objekt {@link GroupModel#condition}.
   * Bei ändernden Zugriffen wird auch noch der FormularMax4000 benachrichtigt, dass
   * das Dokument geupdatet werden muss. Im Prinzip müsste korrekterweise ein
   * ändernder Zugriff auch einen Event an die ModelChangeListener schicken.
   * Allerdings ist dies derzeit nicht implementiert, weil es derzeit genau eine View
   * gibt für die Condition, so dass konkurrierende Änderungen gar nicht möglich
   * sind.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyConditionAccess implements FunctionSelectionAccess
  {
    public boolean isReference()
    {
      return condition.isReference();
    }

    public boolean isExpert()
    {
      return condition.isExpert();
    }

    public boolean isNone()
    {
      return condition.isNone();
    }

    public String getFunctionName()
    {
      return condition.getFunctionName();
    }

    public ConfigThingy getExpertFunction()
    {
      return condition.getExpertFunction();
    }

    public void setParameterValues(Map<String, ParamValue> mapNameToParamValue)
    {
      condition.setParameterValues(mapNameToParamValue);
      formularMax4000.documentNeedsUpdating();
    }

    public void setFunction(String functionName, String[] paramNames)
    {
      condition.setFunction(functionName, paramNames);
      formularMax4000.documentNeedsUpdating();
    }

    public void setExpertFunction(ConfigThingy funConf)
    {
      condition.setExpertFunction(funConf);
      formularMax4000.documentNeedsUpdating();
    }

    public void setParameterValue(String paramName, ParamValue paramValue)
    {
      condition.setParameterValue(paramName, paramValue);
      formularMax4000.documentNeedsUpdating();
    }

    public String[] getParameterNames()
    {
      return condition.getParameterNames();
    }

    public boolean hasSpecifiedParameters()
    {
      return condition.hasSpecifiedParameters();
    }

    public ParamValue getParameterValue(String paramName)
    {
      return condition.getParameterValue(paramName);
    }

  }

}
