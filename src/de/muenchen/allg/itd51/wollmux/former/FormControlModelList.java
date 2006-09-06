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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Verwaltet eine Liste von FormControlModels.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModelList
{
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
   * Bittet die FormControlModelList darum, das Element model aus sich zu entfernen
   * (falls es in der Liste ist).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public void remove(FormControlModel model)
  {
    int index = models.indexOf(model);
    if (index < 0) return;
    models.remove(model);
    model.hasBeenRemoved();
  }
  
  /**
   * Macht aus str einen Identifier, der noch von keinem FormControlModel dieser Liste
   * verwendet wird und liefert diesen Identifier zurück.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String makeUniqueId(String str)
  {
    Iterator iter = models.iterator();
    int count = 0;
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      String id = model.getId();
      if (id.startsWith(str))
      {
        if (count == 0) ++count;
        String suffix = id.substring(str.length());
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
  public void add(FormControlModel model, int idx)
  {
    if (idx < 0) idx = models.size();
    models.add(idx, model);
    
    notifyListeners(model, idx);
  }
  
  /**
   * model wird an das Ende der Liste angehängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(FormControlModel model)
  {
    models.add(model);
    notifyListeners(model, models.size() - 1);
  }
  
  /**
   * Schiebt die ausgewählten FormControlModels in der Liste nach oben, d,h, reduziert ihre
   * Indizes um 1.
   * @param indices eine Menge von Integer-Objekten, die die Indizes der zu verschiebenden
   * FormControlModels spezifizieren. Die Liste muss aufsteigend sortiert sein, sonst ist
   * das Ergebnis unbestimmt. Ist das erste Element von indices die 0, so wird nichts 
   * getan. Ansonsten werden die Indizes i aus indices der Reihe nach abgearbeitet und Element
   * i wird mit Element i-1 vertauscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void moveElementsUp(List indices)
  {
    Iterator iter = indices.iterator();
    while (iter.hasNext())
    {
      int idx = ((Integer)iter.next()).intValue();
      if (idx <= 0) return;
      Object temp = models.get(idx-1);
      models.setElementAt(models.get(idx), idx-1);
      models.setElementAt(temp, idx);
      notifyListeners(idx - 1 , idx);
    }
  }
  
  /**
   * Schiebt die ausgewählten FormControlModels in der Liste nach unten, d,h, erhöht ihre
   * Indizes um 1.
   * @param indices eine Menge von Integer-Objekten, die die Indizes der zu verschiebenden
   * FormControlModels spezifizieren. Die Liste muss aufsteigend sortiert sein, sonst ist
   * das Ergebnis unbestimmt. Ist das letzte Element von indices der höchste mögliche Index, 
   * so wird nichts 
   * getan. Ansonsten werden die Indizes i aus indices von hinten beginnend abgearbeitet und 
   * Element i wird mit Element i+1 vertauscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void moveElementsDown(List indices)
  {
    ListIterator iter = indices.listIterator(indices.size());
    while (iter.hasPrevious())
    {
      int idx = ((Integer)iter.previous()).intValue();
      if (idx >= models.size() - 1) return;
      Object temp = models.get(idx+1);
      models.setElementAt(models.get(idx), idx + 1);
      models.setElementAt(temp, idx);
      notifyListeners(idx, idx + 1);
    }
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
    FormControlModel currentTab = FormControlModel.createTab("Eingabe", "Reiter1");
    Iterator iter = models.iterator();
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      if (phase == 0 && model.getType() == FormControlModel.TAB_TYPE)
        currentTab = model;
      else if (phase > 0 && model.getType() == FormControlModel.TAB_TYPE)
      {
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
        conf = tabConf.add("Buttons");
        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 2 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        conf.addChild(model.export());
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
  
    return export;
  }
  
  /**
   * Erzeugt ein ConfigThingy für den Reiter tab, hängt es an conf an und liefert es zurück.
   * Das erzeugte ConfigThingy hat folgenden Aufbau: <br>
   * ReiterId(TITLE "title" CLOSEACTION "action" TIP "tip" HOTKEY "hotkey")
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ConfigThingy outputTab(FormControlModel tab, ConfigThingy conf)
  {
    conf = conf.add(tab.getId());
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
