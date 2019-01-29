package de.muenchen.allg.itd51.wollmux;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;

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
   * String-Repräsentationen aller Einträge der Persönlichen Absenderliste (PAL) in
   * einem String-Array. Die genaue Form der String-Repräsentationen ist abhängig von
   * {@link #senderDisplayTemplate}, das in der WollMux-Konfiguration über den Wert
   * von SENDER_DISPLAYTEMPLATE gesetzt werden kann. Unabhängig von
   * {@link #senderDisplayTemplate} enthalten die über diese Methode
   * zurückgelieferten String-Repräsentationen der PAL-Einträge aber auf jeden Fall
   * immer am Ende den String "§§%=%§§" gefolgt vom Schlüssel des entsprechenden
   * Eintrags!
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getPALEntries()
   */
  @Override
  public String[] getPALEntries()
  {
    DJDatasetListElement[] pal = getSortedPALEntries();
    String[] elements = new String[pal.length];
    for (int i = 0; i < pal.length; i++)
    {
      elements[i] =
        pal[i].toString() + SENDER_KEY_SEPARATOR + pal[i].getDataset().getKey();
    }
    return elements;
  }

  /**
   * Diese Methode liefert alle DJDatasetListElemente der Persönlichen Absenderliste
   * (PAL) in alphabetisch aufsteigend sortierter Reihenfolge.
   * 
   * Wichtig: Diese Methode ist nicht im XPALProvider-Interface enthalten. Die
   * String-Repräsentation der zurückgelieferten DJDatasetListElements entsprechen
   * zwar {@link #senderDisplayTemplate}, aber sie enthalten im Gegensatz zu den
   * Strings, die man über {@link #getPALEntries()} erhält, NICHT zwangsläufig am
   * Ende die Schlüssel der Datensätze. Wenn man nicht direkt an die Dataset-Objekte
   * der PAL heran will, sollte man statt dieser Methode auf jeden Fall besser
   * {@link #getPALEntries()} verwenden!
   * 
   * @return alle DJDatasetListElemente der Persönlichen Absenderliste (PAL) in
   *         alphabetisch aufsteigend sortierter Reihenfolge.
   */
  public DJDatasetListElement[] getSortedPALEntries()
  {
    // Liste der entries aufbauen.
    QueryResults data = DatasourceJoinerFactory.getDatasourceJoiner().getLOS();
  
    DJDatasetListElement[] elements = new DJDatasetListElement[data.size()];
    Iterator<Dataset> iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] =
        new DJDatasetListElement((DJDataset) iter.next());
    Arrays.sort(elements);
  
    return elements;
  }

  /**
   * Diese Methode liefert eine String-Repräsentation des aktuell aus der
   * persönlichen Absenderliste (PAL) ausgewählten Absenders zurück. Die genaue Form
   * der String-Repräsentation ist abhängig von {@link #senderDisplayTemplate}, das
   * in der WollMux-Konfiguration über den Wert von SENDER_DISPLAYTEMPLATE gesetzt
   * werden kann. Unabhängig von {@link #senderDisplayTemplate} enthält die über
   * diese Methode zurückgelieferte String-Repräsentation aber auf jeden Fall immer
   * am Ende den String "§§%=%§§" gefolgt vom Schlüssel des aktuell ausgewählten
   * Absenders. Ist die PAL leer oder noch kein Absender ausgewählt, so liefert die
   * Methode den Leerstring "" zurück. Dieser Sonderfall sollte natürlich
   * entsprechend durch die aufrufende Methode behandelt werden.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getCurrentSender()
   * 
   * @return den aktuell aus der PAL ausgewählten Absender als String. Ist kein
   *         Absender ausgewählt wird der Leerstring "" zurückgegeben.
   */
  @Override
  public String getCurrentSender()
  {
    try
    {
      DJDataset selected = DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDataset();
      return new DJDatasetListElement(selected).toString()
        + SENDER_KEY_SEPARATOR + selected.getKey();
    }
    catch (DatasetNotFoundException e)
    {
      return "";
    }
  }  
}
