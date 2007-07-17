/*
* Dateiname: FormControlModelList.java
* Projekt  : WollMux
* Funktion : Verwaltet eine Liste von FormControlModels.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 07.08.2006 | BNK | Erstellung
* 29.08.2006 | BNK | kommentiert.
* 10.09.2006 | BNK | [R3207]Maximale Anzahl von Steuerelementen pro Tab wird überwacht.
* 10.09.2006 | BNK | automatisch Tab einfügen, wenn nach Button ein in der Button-Zeile
*                    unsinniges Element auftaucht.
* 16.03.2007 | BNK | Für jedes hinzugekommene FormControlModel die ID broadcasten. 
* 12.07.2007 | BNK | Umgestellt auf Verwendung von IDManager.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.control;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.former.IDManager;

/**
 * Verwaltet eine Liste von FormControlModels.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModelList
{
  /**
   * Die FormControlModelList erzwingt einen Tab nach spätestens sovielen
   * FormControlModels. Dies sorgt Problemen mit GridBagLayout vor.
   */
  public static final int MAX_MODELS_PER_TAB = 500;
  
  /**
   * Die Liste der {@link FormControlModel}s.
   */
  private Vector models = new Vector();
  
  /**
   * Liste aller {@link ItemListener}, die über Änderungen des Listeninhalts informiert
   * werden wollen.
   */
  private List listeners = new Vector(1);
  
  /**
   * Der FormularMax4000 zu dem diese Liste gehört.
   */
  private FormularMax4000 formularMax4000;
  
  public FormControlModelList(FormularMax4000 formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
  }
  
  /**
   * Löscht alle bestehenden FormControlModels aus der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void clear()
  {
    while (!models.isEmpty())
    {
      int index = models.size() - 1;
      FormControlModel model = (FormControlModel)models.remove(index);
      model.hasBeenRemoved();
    }
  }
  
  /**
   * Liefert die Anzahl der {@link FormControlModel}s in dieser Liste.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int size()
  {
    return models.size();
  }
  
  /**
   * Liefert true gdw diese Liste keine Elemente enthält.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isEmpty()
  {
    return models.isEmpty();
  }
  
  /**
   * Bittet die FormControlModelList darum, das Element model aus sich zu entfernen
   * (falls es in der Liste ist).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void remove(FormControlModel model)
  {
    int index = models.indexOf(model);
    if (index < 0) return;
    boolean isTab = model.getType() == FormControlModel.TAB_TYPE;
    models.remove(model);
    model.hasBeenRemoved();
    if (isTab) enforceMaxModelsPerTab();
  }
  
  /**
   * Macht aus str einen Identifier, der noch von keinem FormControlModel dieser Liste
   * verwendet wird und liefert diesen Identifier zurück. Falls str == "" wird str zurückgeliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String makeUniqueId(String str)
  {
    if (str.equals("")) return str;
    Iterator iter = models.iterator();
    int count = 0;
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      IDManager.ID id = model.getId();
      if (id != null && id.toString().startsWith(str))
      {
        if (count == 0) ++count;
        String suffix = id.toString().substring(str.length());
        try{
          int idx = Integer.parseInt(suffix);
          if (idx >= count) count = idx + 1;
        }catch(Exception x){}
      }
    }
    
    if (count > 0) 
      return str + count;
    else
      return str;
  }
  
  /**
   * Falls idx >= 0 wird model an Index idx in die Liste eingefügt 
   * (das Element das sich vorher an diesem Index befand hat danach Index idx+1);
   * falls idx < 0 wird model an das Ende der Liste angehängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(final FormControlModel model, int idx)
  {
    if (idx < 0) idx = models.size();
    models.add(idx, model);
    model.hasBeenAdded();
    
    notifyListeners(model, idx);
    
    enforceMaxModelsPerTab();
  }

  /**
   * model wird an das Ende der Liste angehängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(FormControlModel model)
  {
    this.add(model, -1);
  }

  private void enforceMaxModelsPerTab()
  {
    if (models.size() < MAX_MODELS_PER_TAB) return;

    int tabIdx = 0;
    for (int i = 0; i < models.size(); ++i)
    {
      if (((FormControlModel)models.get(i)).isTab())
        tabIdx = i;
      
      if (i - tabIdx >= MAX_MODELS_PER_TAB)
      {
        int idx = (i + tabIdx)/2;
        String id = makeUniqueId(FormularMax4000.STANDARD_TAB_NAME);
        this.add(FormControlModel.createTab(id, id, formularMax4000), idx);
        tabIdx = idx;
      }
    }
  }
  
  /**
   * Schiebt die ausgewählten FormControlModels in der Liste nach oben, d,h, reduziert ihre
   * Indizes um 1.
   * @param iter iteriert über eine Menge von Integer-Objekten, die die Indizes der zu verschiebenden
   * FormControlModels spezifizieren. Die Liste muss aufsteigend sortiert sein, sonst ist
   * das Ergebnis unbestimmt. Ist das erste Element von indices die 0, so wird nichts 
   * getan. Ansonsten werden die Indizes i aus indices der Reihe nach abgearbeitet und Element
   * i wird mit Element i-1 vertauscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void moveElementsUp(Iterator iter)
  {
    boolean haveMovedTab = false;
    while (iter.hasNext())
    {
      int idx = ((Integer)iter.next()).intValue();
      if (idx <= 0) return;
      FormControlModel model1 = (FormControlModel)models.get(idx-1);
      FormControlModel model2 = (FormControlModel)models.get(idx);
      haveMovedTab = haveMovedTab || model1.isTab() || model2.isTab();
      models.setElementAt(model2, idx-1);
      models.setElementAt(model1, idx);
      notifyListeners(idx - 1 , idx);
    }
    if (haveMovedTab) enforceMaxModelsPerTab();
  }
  
  /**
   * Schiebt die ausgewählten FormControlModels in der Liste nach unten, d,h, erhöht ihre
   * Indizes um 1.
   * @param iter iteriert von hinten (d.h. startet hinter dem letzten Element) über eine Menge 
   * von Integer-Objekten, die die Indizes der zu verschiebenden
   * FormControlModels spezifizieren. Die Liste muss aufsteigend sortiert sein (von vorne gesehen,
   * d.h. iter startet hinter dem größten Wert), sonst ist
   * das Ergebnis unbestimmt. Ist das letzte Element der Liste der höchste mögliche Index, 
   * so wird nichts 
   * getan. Ansonsten werden die Indizes i aus indices von hinten beginnend abgearbeitet und 
   * Element i wird mit Element i+1 vertauscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void moveElementsDown(ListIterator iter)
  {
    boolean haveMovedTab = false;
    while (iter.hasPrevious())
    {
      int idx = ((Integer)iter.previous()).intValue();
      if (idx >= models.size() - 1) return;
      FormControlModel model1 = (FormControlModel)models.get(idx+1);
      FormControlModel model2 = (FormControlModel)models.get(idx);
      haveMovedTab = haveMovedTab || model1.isTab() || model2.isTab();
      models.setElementAt(model2, idx + 1);
      models.setElementAt(model1, idx);
      notifyListeners(idx, idx + 1);
    }
    if (haveMovedTab) enforceMaxModelsPerTab();
  }

  /**
   * Liefert ein ConfigThingy, dessen Wurzel ein "Fenster"-Knoten ist und alle FormControlModels
   * dieser Liste enthält.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy export()
  {
    ConfigThingy export = new ConfigThingy("Fenster");
    ConfigThingy conf = export;
    ConfigThingy tabConf = export;
    
    int phase = 0; //0: tab start, 1: Eingabefelder, 2: Buttons
    String id = makeUniqueId(FormularMax4000.STANDARD_TAB_NAME); 
    FormControlModel currentTab = FormControlModel.createTab(id, id, formularMax4000);
    Iterator iter = models.iterator();
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      if (phase == 0 && model.getType() == FormControlModel.TAB_TYPE)
        currentTab = model;
      else if (phase > 0 && model.getType() == FormControlModel.TAB_TYPE)
      {
        if (phase == 1) conf.addChild(makeGlue());
        currentTab = model;
        conf = export;
        phase = 0;
      }
      else if (phase == 0 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        tabConf = outputTab(currentTab, export);
        conf = tabConf.add("Buttons");
        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 1 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        conf.addChild(makeGlue());
        conf = tabConf.add("Buttons");
        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 2 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        conf.addChild(model.export());
      }
      else if  (phase == 2
          && model.getType() != FormControlModel.BUTTON_TYPE
          && model.getType() != FormControlModel.GLUE_TYPE 
          && model.getType() != FormControlModel.SEPARATOR_TYPE)
      {
        id = makeUniqueId(FormularMax4000.STANDARD_TAB_NAME); 
        currentTab = FormControlModel.createTab(id, id, formularMax4000);
        tabConf = outputTab(currentTab, export);
        conf = tabConf.add("Eingabefelder");
        conf.addChild(model.export());
        phase = 1;
      }
      else if (phase == 0)
      {
        tabConf = outputTab(currentTab, export);
        conf = tabConf.add("Eingabefelder");
        conf.addChild(model.export());
        phase = 1;
      } 
      else if (phase >= 1)
      {
        conf.addChild(model.export());
      }
    }
    if (phase == 1) conf.addChild(makeGlue());
  
    return export;
  }
  
  /**
   * Liefert (TYPE "glue") zurück.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ConfigThingy makeGlue()
  {
    ConfigThingy conf = new ConfigThingy("");
    conf.add("TYPE").add("glue");
    return conf;
  }
  
  /**
   * Erzeugt ein ConfigThingy für den Reiter tab, hängt es an conf an und liefert es zurück.
   * Das erzeugte ConfigThingy hat folgenden Aufbau: <br>
   * ReiterId(TITLE "title" CLOSEACTION "action" TIP "tip" HOTKEY "hotkey")
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ConfigThingy outputTab(FormControlModel tab, ConfigThingy conf)
  {
    conf = conf.add((tab.getId() == null)? "Reiter" : tab.getId().toString());
    conf.add("TITLE").add(tab.getLabel());
    conf.add("CLOSEACTION").add(tab.getAction());
    conf.add("TIP").add(tab.getTooltip());
    char hotkey = tab.getHotkey();
    if (hotkey > 0)
      conf.add("HOTKEY").add(""+hotkey);
    
    return conf;
  }

  /**
   * listener wird über Änderungen der Liste informiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ItemListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Benachrichtigt alle ItemListener über das Hinzufügen von model zur Liste an Index index.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void notifyListeners(FormControlModel model, int index)
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = (ItemListener)iter.next();
      listener.itemAdded(model, index);
    }
    formularMax4000.documentNeedsUpdating();
  }
  
  /**
   * Benachrichtigt alle ItemListener über das Vertauschen der Models mit Indizes index1 
   * und index2.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void notifyListeners(int index1, int index2)
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = (ItemListener)iter.next();
      listener.itemSwapped(index1, index2);
    }
    formularMax4000.documentNeedsUpdating();
  }
  
  /**
   * Interface für Klassen, die interessiert sind, zu erfahren, wenn sich die Liste
   * ändert.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ItemListener
  {
    /**
     * Wird aufgerufen nachdem model zur Liste hinzugefügt wurde (an Index index).
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemAdded(FormControlModel model, int index);
    
    /**
     * Wird aufgerufen, nachdem Model mit Index index1 und Model mit index2 vertauscht
     * wurden.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void itemSwapped(int index1, int index2);
  }
  

}
