package de.muenchen.allg.itd51.wollmux;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
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
   * Gibt an, wie die String-Repräsentation von PAL-Einträgen aussehen, die über die
   * XPALProvider-Methoden zurückgeliefert werden. Syntax mit %{Spalte} um
   * entsprechenden Wert des Datensatzes anzuzeigen, z.B. "%{Nachname}, %{Vorname}"
   * für die Anzeige in der Form "Meier, Hans" etc. Kann in der WollMux-Konfiguration
   * über SENDER_DISPLAYTEMPLATE gesetzt werden.
   */
  private String senderDisplayTemplate;
  /**
   * Der String, der in der String-Repräsentation von PAL-Einträgen, den Schlüssel
   * des PAL-Eintrags vom Rest des PAL-Eintrags abtrennt (siehe auch Dokumentation
   * der Methoden des {@link XPALProvider}-Interfaces.
   */
  public static final String SENDER_KEY_SEPARATOR = "§§%=%§§";
  /**
   * Default-Wert für {@link #senderDisplayTemplate}, wenn kein Wert in der
   * Konfiguration explizit angegeben ist.
   * 
   * An dieser Stelle einen Default-Wert hardzucodieren (der noch dazu LHM-spezifisch
   * ist!) ist sehr unschön und wurde nur gemacht um abwärtskompatibel zu alten
   * WollMux-Konfigurationen zu bleiben. Sobald sichergestellt ist, dass überall auf
   * eine neue WollMux-Konfiguration geupdatet wurde, sollte man diesen Fallback
   * wieder entfernen.
   */
  private static final String DEFAULT_SENDER_DISPLAYTEMPLATE =
    "%{Nachname}, %{Vorname} (%{Rolle})";
  
  private PersoenlicheAbsenderliste()
  {
    registeredPALChangeListener = new Vector<XPALChangeEventListener>();
    
    // Setzen von senderDisplayTemplate
    this.senderDisplayTemplate = DEFAULT_SENDER_DISPLAYTEMPLATE;
    try
    {
      this.senderDisplayTemplate =
        WollMuxFiles.getWollmuxConf().query("SENDER_DISPLAYTEMPLATE").getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      if ( ! WollMuxSingleton.getInstance().isNoConfig()) // nur wenn wir eine wollmux.conf haben
      {
        LOGGER.info(L.m(
          "Keine Einstellung für SENDER_DISPLAYTEMPLATE gefunden! Verwende Fallback: %1",
          DEFAULT_SENDER_DISPLAYTEMPLATE));

      }
      // SENDER_DISPLAYTEMPLATE sollte eigentlich verpflichtend sein und wir
      // sollten an dieser Stelle einen echten Error loggen bzw. eine
      // Meldung in der GUI ausgeben und evtl. sogar abbrechen. Wir tun
      // dies allerdings nicht, da das SENDER_DISPLAYTEMPLATE erst mit
      // WollMux 6.4.0 eingeführt wurde und wir abwärtskompatibel zu alten
      // WollMux-Konfigurationen bleiben müssen und Benutzer alter
      // Konfigurationen nicht mit Error-Meldungen irritieren wollen.
      // Dies ist allerdings nur eine Übergangslösung. Die obige Meldung
      // sollte nach ausreichend Zeit genauso wie DEFAULT_SENDER_DISPLAYTEMPLATE
      // entfernt werden (bzw. wie oben gesagt überarbeitet).
    }

  }

  public String getSenderDisplayTemplate()
  {
    return senderDisplayTemplate;
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
    QueryResults data = DatasourceJoiner.getDatasourceJoiner().getLOS();
  
    DJDatasetListElement[] elements = new DJDatasetListElement[data.size()];
    Iterator<Dataset> iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] =
        new DJDatasetListElement((DJDataset) iter.next(), senderDisplayTemplate);
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
      DJDataset selected = DatasourceJoiner.getDatasourceJoiner().getSelectedDataset();
      return new DJDatasetListElement(selected, senderDisplayTemplate).toString()
        + SENDER_KEY_SEPARATOR + selected.getKey();
    }
    catch (DatasetNotFoundException e)
    {
      return "";
    }
  }  
}
