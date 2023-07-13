/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.former.group.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.SyntaxErrorException;
import org.libreoffice.lots.former.DuplicateIDException;
import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.function.FunctionSelection;
import org.libreoffice.lots.former.function.FunctionSelectionAccess;
import org.libreoffice.lots.former.function.ParamValue;
import org.libreoffice.lots.former.model.IdModel;
import org.libreoffice.lots.former.model.IdModel.IDChangeListener;
import org.libreoffice.lots.util.L;

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
  private IdModel id;

  /**
   * Die Sichtbarkeitsbedingung für diese Gruppe.
   */
  private FunctionSelection condition;

  /**
   * Die {@link ModelChangeListener}, die über Änderungen dieses Models informiert
   * werden wollen.
   */
  private List<ModelChangeListener> listeners = new ArrayList<>(1);

  /**
   * Der FormularMax4000 zu dem dieses Model gehört.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Listener der Änderungen an {@link #id} überwacht.
   */
  private MyIDChangeListener myIDChangeListener;

  /**
   * Erzeugt eine neue Gruppe mit Name/ID id. ACHTUNG! id muss activated sein!
   *
   * @param condition
   *          wird direkt als Referenz übernommen und bestimmt die Sichtbarkeitsbedingung dieser
   *          Gruppe.
   * @param formularMax4000
   *          der {@link FormularMax4kController} zu dem diese Gruppe gehört.
   */
  public GroupModel(IdModel id, FunctionSelection condition,
      FormularMax4kController formularMax4000)
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
   */
  public FormularMax4kController getFormularMax4000()
  {
    return formularMax4000;
  }

  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden
   * Container aufgerufen werden, der das Model enthält.
   */
  public void hasBeenRemoved()
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.modelRemoved(this);
    }
  }

  /**
   * listener wird über Änderungen des Models informiert.
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Liefert immer true. Diese Funktion existiert nur, damit der entsprechende Code
   * in AllGroupFuncViewsPanel analog zu den anderen Panels gehalten werden
   * kann.
   */
  public boolean hasFunc()
  {
    return true;
  }

  /**
   * Liefert ein Interface zum Zugriff auf die Sichtbarkeitsbedingung dieses Objekts.
   */
  public FunctionSelectionAccess getConditionAccess()
  {
    return new MyConditionAccess();
  }

  public IdModel getID()
  {
    return id;
  }

  /**
   * Setzt newID als neuen Namen für diese Sichtbarkeitsgruppe und benachrichtigt
   * alle mittels
   * {@link #addListener(org.libreoffice.lots.former.group.model.GroupModel.ModelChangeListener)}
   * registrierten Listener.
   *
   * @throws DuplicateIDException
   *           falls newID bereits von einer anderen Sichtbarkeitsgruppe verwendet
   *           wird.
   */
  public void setID(String newID) throws DuplicateIDException, SyntaxErrorException
  {
    if (!isLegalID(newID))
      throw new SyntaxErrorException(L.m(
        "\"{0}\" is not a syntactically correct ID for visibility groups", newID));
    id.setID(newID);
    /*
     * ID ruft MyIDChangeListener.idHasChanged() auf, was wiederum die Listener auf diesem
     * Model benachrichtigt.
     */
  }

  /**
   * Liefert true gdw, id auf {@link #ID_PATTERN} matcht.
   */
  private boolean isLegalID(String id)
  {
    return ID_PATTERN.matcher(id).matches();
  }

  /**
   * Liefert ein ConfigThingy zurück, dessen Name der Name der Gruppe ist und dessen
   * Inhalt die Definition der Sichtbarkeitsfunktion der Gruppe ist.
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
     *          der neue Wert des Attributs. Die ID wird als {@link IdModel}
     *          übergeben.
     */
    public void attributeChanged(GroupModel model, int attributeId, Object newValue);

    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit in
     * keiner View mehr angezeigt werden soll).
     */
    public void modelRemoved(GroupModel model);
  }

  private class MyIDChangeListener implements IDChangeListener
  {
    @Override
    public void idHasChanged(IdModel id)
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
    @Override
    public boolean isReference()
    {
      return condition.isReference();
    }

    @Override
    public boolean isExpert()
    {
      return condition.isExpert();
    }

    @Override
    public boolean isNone()
    {
      return condition.isNone();
    }

    @Override
    public String getFunctionName()
    {
      return condition.getFunctionName();
    }

    @Override
    public ConfigThingy getExpertFunction()
    {
      return condition.getExpertFunction();
    }

    @Override
    public void setParameterValues(Map<String, ParamValue> mapNameToParamValue)
    {
      condition.setParameterValues(mapNameToParamValue);
    }

    @Override
    public void setFunction(String functionName, String[] paramNames)
    {
      condition.setFunction(functionName, paramNames);
    }

    @Override
    public void setExpertFunction(ConfigThingy funConf)
    {
      condition.setExpertFunction(funConf);
    }

    @Override
    public void setParameterValue(String paramName, ParamValue paramValue)
    {
      condition.setParameterValue(paramName, paramValue);
    }

    @Override
    public String[] getParameterNames()
    {
      return condition.getParameterNames();
    }

    @Override
    public boolean hasSpecifiedParameters()
    {
      return condition.hasSpecifiedParameters();
    }

    @Override
    public ParamValue getParameterValue(String paramName)
    {
      return condition.getParameterValue(paramName);
    }

  }

}
