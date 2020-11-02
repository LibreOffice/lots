/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.control;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.IDManager;

/**
 * Verwaltet eine Liste von FormControlModels.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModelList implements Iterable<FormControlModel>
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormControlModelList.class);

  /**
   * Die FormControlModelList erzwingt einen Tab nach spätestens sovielen
   * FormControlModels. Dies sorgt Problemen mit GridBagLayout vor.
   */
  public static final int MAX_MODELS_PER_TAB = 500;

  /**
   * Die Liste der {@link FormControlModel}s.
   */
  private List<FormControlModel> models = new ArrayList<>();

  /**
   * Liste aller {@link ItemListener}, die über Änderungen des Listeninhalts
   * informiert werden wollen.
   */
  private List<ItemListener> listeners = new ArrayList<>(1);

  /**
   * Der FormularMax4000 zu dem diese Liste gehört.
   */
  private FormularMax4kController formularMax4000;

  public FormControlModelList(FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
  }

  /**
   * Löscht alle bestehenden FormControlModels aus der Liste.
   */
  public void clear()
  {
    while (!models.isEmpty())
    {
      int index = models.size() - 1;
      FormControlModel model = models.remove(index);
      model.hasBeenRemoved();
    }
  }

  /**
   * Liefert die Anzahl der {@link FormControlModel}s in dieser Liste.
   */
  public int size()
  {
    return models.size();
  }

  /**
   * Liefert true gdw diese Liste keine Elemente enthält.
   */
  public boolean isEmpty()
  {
    return models.isEmpty();
  }

  /**
   * Liefert einen Iterator über alle {@link FormControlModel}s in dieser Liste.
   * ACHTUNG! Es dürfen keine Veränderungen über den Iterator (z.B.
   * {@link Iterator#remove()}) vorgenommen werden. Auch dürfen während der
   * Iteration keine Veränderungen an der InsertionModelList vorkommen, da der
   * Iterator direkt auf der internen Datenstruktur arbeitet und es daher zur
   * {@link java.util.ConcurrentModificationException} kommen würde.
   */
  @Override
  public Iterator<FormControlModel> iterator()
  {
    return models.iterator();
  }

  /**
   * Bittet die FormControlModelList darum, das Element model aus sich zu entfernen
   * (falls es in der Liste ist).
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
   * Macht aus str einen Identifier, der noch von keinem FormControlModel dieser
   * Liste verwendet wird und liefert diesen Identifier zurück. Falls str == "" wird
   * str zurückgeliefert.
   */
  public String makeUniqueId(String str)
  {
    if (str.equals("")) return str;
    Iterator<FormControlModel> iter = models.iterator();
    int count = 0;
    while (iter.hasNext())
    {
      FormControlModel model = iter.next();
      IDManager.ID id = model.getId();
      if (id != null && id.toString().startsWith(str))
      {
        if (count == 0) ++count;
        String suffix = id.toString().substring(str.length());
        try
        {
          int idx = Integer.parseInt(suffix);
          if (idx >= count) count = idx + 1;
        }
        catch (Exception x)
        {
          LOGGER.trace("", x);
        }
      }
    }

    if (count > 0)
      return str + count;
    else
      return str;
  }

  /**
   * Falls idx &gt;= 0 wird model an Index idx in die Liste eingefügt (das Element das sich vorher
   * an diesem Index befand hat danach Index idx+1); falls idx %lt; 0 wird model an das Ende der
   * Liste angehängt.
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
      if (models.get(i).isTab()) tabIdx = i;

      if (i - tabIdx >= MAX_MODELS_PER_TAB)
      {
        int idx = (i + tabIdx) / 2;
        String id = makeUniqueId(FormularMax4kController.STANDARD_TAB_NAME);
        this.add(FormControlModel.createTab(id, id, formularMax4000), idx);
        tabIdx = idx;
      }
    }
  }

  /**
   * Schiebt die ausgewählten FormControlModels in der Liste nach oben, d,h,
   * reduziert ihre Indizes um 1.
   *
   * @param iter
   *          iteriert über eine Menge von Integer-Objekten, die die Indizes der zu
   *          verschiebenden FormControlModels spezifizieren. Die Liste muss
   *          aufsteigend sortiert sein, sonst ist das Ergebnis unbestimmt. Ist das
   *          erste Element von indices die 0 oder 1, so wird nichts getan. Ansonsten
   *          werden die Indizes i aus indices der Reihe nach abgearbeitet und
   *          Element i wird mit Element i-1 vertauscht.
   */
  public void moveElementsUp(Iterator<Integer> iter)
  {
    boolean haveMovedTab = false;
    while (iter.hasNext())
    {
      int idx = iter.next().intValue();
      if (idx <= 1) return;
      FormControlModel model1 = models.get(idx - 1);
      FormControlModel model2 = models.get(idx);
      haveMovedTab = haveMovedTab || model1.isTab() || model2.isTab();
      models.set(idx-1, model2);
      models.set(idx, model1);
      notifyListeners(idx - 1, idx);
    }
    if (haveMovedTab) enforceMaxModelsPerTab();
  }

  /**
   * Schiebt die ausgewählten FormControlModels in der Liste nach unten, d,h, erhöht
   * ihre Indizes um 1.
   *
   * @param iter
   *          iteriert von hinten (d.h. startet hinter dem letzten Element) über eine
   *          Menge von Integer-Objekten, die die Indizes der zu verschiebenden
   *          FormControlModels spezifizieren. Die Liste muss aufsteigend sortiert
   *          sein (von vorne gesehen, d.h. iter startet hinter dem größten Wert),
   *          sonst ist das Ergebnis unbestimmt. Ist das letzte Element der Liste der
   *          höchste mögliche Index, so wird nichts getan. Ansonsten werden die
   *          Indizes i aus indices von hinten beginnend abgearbeitet und Element i
   *          wird mit Element i+1 vertauscht. Element 0 wird dabei niemals
   *          angefasst, auch wenn Index 0 in der Liste ist.
   */
  public void moveElementsDown(ListIterator<Integer> iter)
  {
    boolean haveMovedTab = false;
    while (iter.hasPrevious())
    {
      int idx = iter.previous().intValue();
      if (idx >= models.size() - 1) return;
      if (idx == 0) break;
      FormControlModel model1 = models.get(idx + 1);
      FormControlModel model2 = models.get(idx);
      haveMovedTab = haveMovedTab || model1.isTab() || model2.isTab();
      models.set(idx+1, model2);
      models.set(idx, model1);
      notifyListeners(idx, idx + 1);
    }
    if (haveMovedTab) enforceMaxModelsPerTab();
  }

  /**
   * Liefert ein ConfigThingy, dessen Wurzel ein "Fenster"-Knoten ist und alle
   * FormControlModels dieser Liste enthält.
   */
  public ConfigThingy export()
  {
    ConfigThingy export = new ConfigThingy("Fenster");
    ConfigThingy conf = export;
    ConfigThingy tabConf = export;

    int phase = 0; // 0: tab start, 1: Eingabefelder, 2: Buttons
    String id = makeUniqueId(FormularMax4kController.STANDARD_TAB_NAME);
    FormControlModel currentTab =
    FormControlModel.createTab(id, id, formularMax4000);

    Iterator<FormControlModel> iter = models.iterator();
    while (iter.hasNext())
    {
      FormControlModel model = iter.next();
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

        /*
         * wenn der Button ein trac#20601 Button ist, soll dieser nicht in einen eigenen Knoten
         * Button( ... ) exportiert werden da er sonst in der Fusszeile der FormularGUI auftauchen
         * würde in der auch die Standardbuttons gezeigt werden. der button wird dadurch ohne
         * expliziten Button-Knoten in den Knoten "Eingabefelder" exportiert.
         */
        if(model.getAction().equals("openExt") || model.getAction().equals("openTemplate")) {
          conf = tabConf.add("Eingabefelder");
        }
        else {
          conf = tabConf.add("Buttons");
        }

        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 1 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        if(!model.getAction().equals("openExt") && !model.getAction().equals("openTemplate"))
        {
          conf.addChild(makeGlue());
          conf = tabConf.add("Buttons");
        }

        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 2 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
	  if(!model.getAction().equals("openExt")
	      && !model.getAction().equals("openTemplate")
	      && tabConf.query("Buttons") != null
	      && tabConf.query("Buttons").count() < 1) {

	    conf = tabConf.add("Buttons");
	  }

	conf.addChild(model.export());
      }
      else if (phase == 2 && model.getType() != FormControlModel.BUTTON_TYPE
        && model.getType() != FormControlModel.GLUE_TYPE
        && model.getType() != FormControlModel.SEPARATOR_TYPE
        && model.getType().equals("tab"))
      {
        id = makeUniqueId(FormularMax4kController.STANDARD_TAB_NAME);
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
   */
  private ConfigThingy makeGlue()
  {
    ConfigThingy conf = new ConfigThingy("");
    conf.add("TYPE").add("glue");
    return conf;
  }

  /**
   * Erzeugt ein ConfigThingy für den Reiter tab, hängt es an conf an und liefert es
   * zurück. Das erzeugte ConfigThingy hat folgenden Aufbau: <br>
   * ReiterId(TITLE "title" CLOSEACTION "action" TIP "tip" HOTKEY "hotkey")
   */
  private ConfigThingy outputTab(FormControlModel tab, ConfigThingy conf)
  {
    conf = conf.add((tab.getId() == null) ? "Reiter" : tab.getId().toString());
    conf.add("TITLE").add(tab.getLabel());
    conf.add("CLOSEACTION").add(tab.getAction());
    conf.add("TIP").add(tab.getTooltip());
    char hotkey = tab.getHotkey();
    if (hotkey > 0) conf.add("HOTKEY").add("" + hotkey);

    return conf;
  }

  /**
   * listener wird über Änderungen der Liste informiert.
   */
  public void addListener(ItemListener listener)
  {
    if (!listeners.contains(listener))
    {
      listeners.add(listener);
    }
  }

  /**
   * Benachrichtigt alle ItemListener über das Hinzufügen von model zur Liste an
   * Index index.
   */
  private void notifyListeners(FormControlModel model, int index)
  {
    Iterator<ItemListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = iter.next();
      listener.itemAdded(model, index);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Benachrichtigt alle ItemListener über das Vertauschen der Models mit Indizes
   * index1 und index2.
   */
  private void notifyListeners(int index1, int index2)
  {
    Iterator<ItemListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ItemListener listener = iter.next();
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
     */
    public void itemAdded(FormControlModel model, int index);

    /**
     * Wird aufgerufen, nachdem Model mit Index index1 und Model mit index2
     * vertauscht wurden.
     */
    public void itemSwapped(int index1, int index2);
  }

}
