/*
* Dateiname: GroupModel.java
* Projekt  : WollMux
* Funktion : Eine Sichtbarkeitsgruppe, zu der 0 bis mehrere setGroups-Bookmarks gehören können.
* 
* Copyright: Landeshauptstadt München
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Bookmark;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionAccess;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;

/*
 * '''2006-11-15''' BNK (2t): GroupModel, das 0 bis mehrere Bookmarks haben
 * kann, die zu der Gruppe gehören + * Implementierung der Seriendruckfunktion
 * des WollMux - *
 * 
 *  '''2006-11-17''' BNK (2t): OneFormControlGroupsView mit
 * Eingabefeld und Button zum Hinzufügen einer neuen Gruppe (d.h. einer die
 * bislang nicht existiert) + -
 *  
 * * '''2006-12-02''' BNK (2t): AllGroupNamesView
 * mit Markierung aller FormControls, die Mitglieder der Gruppe sind bei
 * Anklicken eines Gruppennamens + - *
 *  
 * '''2006-12-04''' BNK (5t): Unterstützung
 * für Feldreferenzen in FunctionSelectionAccessView. Das FormControlModel muss
 * Events broadcasten, wenn neue IDs hinzukommen oder alte IDs verschwinden.
 * ID-Änderungen werden ja schon gebroadcastet, aber noch nicht in der
 * FunctionSelectionAccessView ausgewertet.
 */

/**
 * Eine Sichtbarkeitsgruppe, zu der 0 bis mehrere setGroups-Bookmarks gehören können.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class GroupModel
{
  /**
   * 
   */
  private Map mapBookmarkNameToBookmark = new HashMap();
  
  /**
   * Der Name dieser Gruppe.
   */
  private String name;
  
  /**
   * Die Liste der setGroups-{@link Bookmark}s, die zu dieser Gruppe gehören.
   */
  private List bookmarks = new Vector(1);
  
  /**
   * Die Sichtbarkeitsbedingung für diese Gruppe.
   */
  private FunctionSelection condition;
  
  /**
   * Die {@link ModelChangeListener}, die über Änderungen dieses Models informiert werden wollen.
   */
  private List listeners = new Vector(1);
  
  /**
   * Der FormularMax4000 zu dem dieses Model gehört.
   */
  private FormularMax4000 formularMax4000;
  
  /**
   * Erzeugt eine neue Gruppe mit Name name.
   * @param condition wird direkt als Referenz übernommen und bestimmt die Sichtbarkeitsbedingung
   *        dieser Gruppe.
   * @param formularMax4000 der {@link FormularMax4000} zu dem diese Gruppe gehört.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public GroupModel(String name, FunctionSelection condition, FormularMax4000 formularMax4000)
  {
    this.name = name;
    this.condition = condition;
    this.formularMax4000 = formularMax4000;
    bookmarks.add(new Integer(0)); //Dummy-Statement, nur um die Warnung wegzukriegen, dass bookmarks derzeit nicht verwendet wird.
  }
  
  /**
   * Fügt das Bookmark bm zu dieser Gruppe hinzu, wenn es in dieser Gruppe noch kein Bookmark gleichen
   * Namens gibt. Falls es schon ein Bookmark gleichen Namens gibt, so wird dieses ersetzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addBookmark(Bookmark bm)
  {
    mapBookmarkNameToBookmark.put(bm.getName(), bm);
  }
  
  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden Container
   * aufgerufen werden, der das Model enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED */
  public void hasBeenRemoved()
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = (ModelChangeListener)iter.next();
      listener.modelRemoved(this);
    }
    formularMax4000.documentNeedsUpdating();
  }
  
  /**
   * listener wird über Änderungen des Models informiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }
  
  /**
   * Liefert ein Interface zum Zugriff auf die Sichtbarkeitsbedingung dieses Objekts.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public FunctionSelectionAccess getConditionAccess()
  {
    return new MyConditionAccess();
  }
  
  /**
   * Liefert ein ConfigThingy zurück, dessen Name der Name der Gruppe ist und dessen Inhalt die
   * Definition der Sichtbarkeitsfunktion der Gruppe ist. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy export()
  {
    return condition.export(name);
  }
  
  /**
   * Interface für Listener, die über Änderungen eines Models informiert
   * werden wollen. 
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich geändert hat. 
     * @param model das Model, das sich geändert hat.
     * @param attributeId eine der {@link GroupModel#CONDITION_ATTR Attribut-ID-Konstanten}.
     * @param newValue der neue Wert des Attributs. Numerische Attribute werden als Integer übergeben.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    //public void attributeChanged(GroupModel model, int attributeId, Object newValue);
    
    
    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit
     * in keiner View mehr angezeigt werden soll).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void modelRemoved(GroupModel model);
  }
  
  /**
   * Diese Klasse leitet Zugriffe weiter an das Objekt {@link GroupModel#condition}. Bei
   * ändernden Zugriffen wird auch noch der FormularMax4000 benachrichtigt, dass das Dokument
   * geupdatet werden muss. Im Prinzip müsste korrekterweise ein
   * ändernder Zugriff auch einen Event an die ModelChangeListener schicken.
   * Allerdings ist dies derzeit nicht implementiert,
   * weil es derzeit genau eine View gibt für die Condition, so dass konkurrierende Änderungen
   * gar nicht möglich sind.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyConditionAccess implements FunctionSelectionAccess
  {
    public boolean isReference() { return condition.isReference();}
    public boolean isExpert()    { return condition.isExpert(); }
    public boolean isNone()      { return condition.isNone(); }
    public String getFunctionName()      { return condition.getFunctionName();}
    public ConfigThingy getExpertFunction() { return condition.getExpertFunction(); }

    public void setParameterValues(Map mapNameToParamValue)
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
