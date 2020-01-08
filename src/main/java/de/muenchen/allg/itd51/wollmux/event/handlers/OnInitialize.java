package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.ByJavaPropertyFinder;
import de.muenchen.allg.itd51.wollmux.db.ByOOoUserProfileFinder;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dieses Event wird als erstes WollMuxEvent bei der Initialisierung des WollMux im WollMuxSingleton
 * erzeugt und übernimmt alle benutzersichtbaren (interaktiven) Initialisierungen wie z.B. das
 * Darstellen des AbsenderAuswählen-Dialogs, falls die PAL leer ist.
 *
 * @author christoph.lutz
 */
public class OnInitialize extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnInitialize.class);

  @Override
  protected void doit()
  {
    WollMuxEventHandler.getInstance().unregisterInitEventListener();
    DatasourceJoiner dsj = DatasourceJoinerFactory.getDatasourceJoiner();

    if (dsj.getLOS().size() == 0)
    {
      // falls es keine Datensätze im LOS gibt:
      // Die initialen Daten nach Heuristik versuchen zu finden:
      int found = searchDefaultSender(dsj);

      // Absender Auswählen Dialog starten:
      // wurde genau ein Datensatz gefunden, kann davon ausgegangen werden,
      // dass dieser OK ist - der Dialog muss dann nicht erscheinen.
      if (found != 1)
        WollMuxEventHandler.getInstance().handleShowDialogAbsenderAuswaehlen();
      else
        WollMuxEventHandler.getInstance().handlePALChangedNotify();
    }
  }

  /**
   * Wertet den Konfigurationsabschnitt PersoenlicheAbsenderlisteInitialisierung/Suchstrategie aus
   * und versucht nach der angegebenen Strategie (mindestens) einen Datensatz im DJ dsj zu finden,
   * der den aktuellen Benutzer repräsentiert. Fehlt der Konfigurationsabschnitt, so wird die
   * Defaultsuche BY_OOO_USER_PROFILE(Vorname "${givenname}" Nachname "${sn}") gestartet. Liefert
   * ein Element der Suchstrategie mindestens einen Datensatz zurück, so werden die anderen Elemente
   * der Suchstrategie nicht mehr ausgewertet.
   *
   * @param dsj
   *          Der DatasourceJoiner, in dem nach dem aktuellen Benutzer gesucht wird.
   * @return liefert die Anzahl der Datensätze, die nach Durchlaufen der Suchstrategie gefunden
   *         wurden.
   */
  private int searchDefaultSender(DatasourceJoiner dsj)
  {
    // Auswertung des Abschnitts
    // PersoenlicheAbsenderlisteInitialisierung/Suchstrategie
    ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
    ConfigThingy strat = null;
    try
    {
      strat = wmConf.query("PersoenlicheAbsenderlisteInitialisierung").query("Suchstrategie")
          .getLastChild();
    } catch (NodeNotFoundException e)
    {
      LOGGER.error("", e);
    }

    QueryResults results = null;
    if (strat != null)
    {
      // Suche über Suchstrategie aus Konfiguration
      for (Iterator<ConfigThingy> iter = strat.iterator(); iter.hasNext();)
      {
        ConfigThingy element = iter.next();

        if (element.getName().equals("BY_JAVA_PROPERTY"))
        {
          results = new ByJavaPropertyFinder(dsj).find(element);
        } else if (element.getName().equals("BY_OOO_USER_PROFILE"))
        {
          results = new ByOOoUserProfileFinder(dsj).find(element);
        } else
        {
          LOGGER.error(
              L.m("Ungültiger Schlüssel in Suchstrategie: {}", element.stringRepresentation()));
        }
      }
    } else
    {
      // Standardsuche über das OOoUserProfile:
      List<Pair<String, String>> query = new ArrayList<>();
      query.add(Pair.with("Vorname", "${givenname}"));
      query.add(Pair.with("Nachname", "${sn}"));

      results = new ByOOoUserProfileFinder(dsj).find(query);
    }

    return dsj.addToPAL(results);
  }
}