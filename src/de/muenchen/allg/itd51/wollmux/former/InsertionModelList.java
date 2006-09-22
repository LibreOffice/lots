/*
* Dateiname: InsertionModelList.java
* Projekt  : WollMux
* Funktion : Verwaltet eine Liste von InsertionModels.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 06.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * Verwaltet eine Liste von InsertionModels
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class InsertionModelList
{
  /**
   * Die Liste der {@link InsertionModel}s. 
   */
  private List models = new LinkedList();
  
  /**
   * Liste aller {@link ItemListener}, die über Änderungen des Listeninhalts informiert
   * werden wollen.
   */
  private List listeners = new Vector(1);
  
  /**
   * Fügt model dieser Liste hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(InsertionModel model)
  {
    int idx = models.size();
    models.add(idx, model);
    notifyListeners(model, idx);
  }
  
  /**
   * Löscht alle bestehenden InsertionModels aus der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void clear()
  {
    while (!models.isEmpty())
    {
      int index = models.size() - 1;
      InsertionModel model = (InsertionModel)models.remove(index);
      model.hasBeenRemoved();
    }
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
  private void notifyListeners(InsertionModel model, int index)
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = (ItemListener)iter.next();
      listener.itemAdded(model, index);
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
    public void itemAdded(InsertionModel model, int index);
  }

}

