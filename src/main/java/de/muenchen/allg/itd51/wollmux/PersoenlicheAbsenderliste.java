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
package de.muenchen.allg.itd51.wollmux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;

public class PersoenlicheAbsenderliste implements XPALProvider, Iterable<XPALChangeEventListener>
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PersoenlicheAbsenderliste.class);

  private static PersoenlicheAbsenderliste instance;

  public static PersoenlicheAbsenderliste getInstance()
  {
    if (instance == null)
      instance = new PersoenlicheAbsenderliste();

    return instance;
  }

  /**
   * Enthält alle registrierten SenderBox-Objekte.
   */
  private Vector<XPALChangeEventListener> registeredPALChangeListener;

  /**
   * Der String, der in der String-Repräsentation von PAL-Einträgen, den Schlüssel
   * des PAL-Eintrags vom Rest des PAL-Eintrags abtrennt (siehe auch Dokumentation
   * der Methoden des {@link XPALProvider}-Interfaces.
   */
  public static final String SENDER_KEY_SEPARATOR = "§§%=%§§";

  private PersoenlicheAbsenderliste()
  {
    registeredPALChangeListener = new Vector<>();
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empfängt
   * wenn sich die PAL ändert. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   *
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * PersoenlicheAbsenderliste auch nicht das XPALChangedBroadcaster-Interface.
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    LOGGER.trace("PersoenlicheAbsenderliste::addPALChangeEventListener()");

    if (listener == null) {
      return;
    }

    Iterator<XPALChangeEventListener> i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredPALChangeListener.add(listener);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war.
   *
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * PersoenlicheAbsenderliste auch nicht das XPALChangedBroadcaster-Interface.
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    LOGGER.trace("PersoenlicheAbsenderliste::removePALChangeEventListener()");
    Iterator<XPALChangeEventListener> i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }

  /**
   * Liefert einen Iterator auf alle registrierten SenderBox-Objekte.
   *
   * @return Iterator auf alle registrierten SenderBox-Objekte.
   */
  @Override
  public Iterator<XPALChangeEventListener> iterator()
  {
    return registeredPALChangeListener.iterator();
  }

  /**
   * Diese Methode liefert eine alphabethisch aufsteigend sortierte Liste mit
   * String-Repräsentationen aller Einträge der Persönlichen Absenderliste (PAL) in einem
   * String-Array. Jeder PAL-Eintrag enthält immer am Ende den String "§§%=%§§" gefolgt vom
   * Schlüssel des entsprechenden Eintrags!
   *
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getPALEntries()
   */
  @Override
  public String[] getPALEntries()
  {
    List<DJDataset> pal = getSortedPALEntries();
    String[] elements = new String[pal.size()];
    for (int i = 0; i < pal.size(); i++)
    {
      elements[i] =
          pal.get(i).toString() + SENDER_KEY_SEPARATOR + pal.get(i).getKey();
    }

    return elements;
  }

  /**
   * Diese Methode liefert alle DJDatasetListElemente der Persönlichen Absenderliste (PAL) in
   * alphabetisch aufsteigend sortierter Reihenfolge.
   *
   * Wichtig: Diese Methode ist nicht im XPALProvider-Interface enthalten.
   *
   * @return alle DJDatasetListElemente der Persönlichen Absenderliste (PAL) in alphabetisch
   *         aufsteigend sortierter Reihenfolge.
   */
  public List<DJDataset> getSortedPALEntries()
  {
    // Liste der entries aufbauen.
    QueryResults data = DatasourceJoinerFactory.getDatasourceJoiner().getLOS();

    List<DJDataset> listDataset = new ArrayList<>();
    Iterator<Dataset> iter = data.iterator();

    while (iter.hasNext())
    {
      listDataset.add((DJDataset) iter.next());
    }

    DatasourceJoinerFactory.getDatasourceJoiner();
    Collections.sort(listDataset, DatasourceJoiner.sortPAL);

    return listDataset;
  }

  /**
   * Diese Methode liefert eine String-Repräsentation des aktuell aus der persönlichen Absenderliste
   * (PAL) ausgewählten Absenders zurück. Die String-Repräsentation enthält auf jeden Fall immer am
   * Ende den String "§§%=%§§" gefolgt vom Schlüssel des aktuell ausgewählten Absenders. Ist die PAL
   * leer oder noch kein Absender ausgewählt, so liefert die Methode den Leerstring "" zurück.
   * Dieser Sonderfall sollte natürlich entsprechend durch die aufrufende Methode behandelt werden.
   *
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getCurrentSender()
   *
   * @return den aktuell aus der PAL ausgewählten Absender als String. Ist kein Absender ausgewählt
   *         wird der Leerstring "" zurückgegeben.
   */
  @Override
  public String getCurrentSender()
  {
    try
    {
      DJDataset selected = DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDataset();
      return selected.toString()
        + SENDER_KEY_SEPARATOR + selected.getKey();
    }
    catch (DatasetNotFoundException e)
    {
      return "";
    }
  }
}
