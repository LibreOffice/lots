package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dieses Event wird als erstes WollMuxEvent bei der Initialisierung des WollMux im
 * WollMuxSingleton erzeugt und übernimmt alle benutzersichtbaren (interaktiven)
 * Initialisierungen wie z.B. das Darstellen des AbsenderAuswählen-Dialogs, falls
 * die PAL leer ist.
 *
 * @author christoph.lutz TESTED
 */
public class OnInitialize extends BasicEvent 
{
	  private static final Logger LOGGER = LoggerFactory
		      .getLogger(OnInitialize.class);
	  
    @Override
    protected void doit()
    {
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
      else
      {
        // Liste der nicht zuordnenbaren Datensätze erstellen und ausgeben:
        String names = "";
        List<String> lost = DatasourceJoinerFactory.getLostDatasetDisplayStrings();
        if (!lost.isEmpty())
        {
          for (String l : lost)
            names += "- " + l + "\n";
          String message =
            L.m("Die folgenden Datensätze konnten nicht "
              + "aus der Datenbank aktualisiert werden:\n\n" + "%1\n"
              + "Wenn dieses Problem nicht temporärer "
              + "Natur ist, sollten Sie diese Datensätze aus "
              + "ihrer Absenderliste löschen und neu hinzufügen!", names);
          ModalDialogs.showInfoModal(L.m("WollMux-Info"), message);
        }
      }
    }

    /**
     * Wertet den Konfigurationsabschnitt
     * PersoenlicheAbsenderlisteInitialisierung/Suchstrategie aus und versucht nach
     * der angegebenen Strategie (mindestens) einen Datensatz im DJ dsj zu finden,
     * der den aktuellen Benutzer repräsentiert. Fehlt der Konfigurationsabschnitt,
     * so wird die Defaultsuche BY_OOO_USER_PROFILE(Vorname "${givenname}" Nachname
     * "${sn}") gestartet. Liefert ein Element der Suchstrategie mindestens einen
     * Datensatz zurück, so werden die anderen Elemente der Suchstrategie nicht mehr
     * ausgewertet.
     *
     * @param dsj
     *          Der DatasourceJoiner, in dem nach dem aktuellen Benutzer gesucht
     *          wird.
     * @return liefert die Anzahl der Datensätze, die nach Durchlaufen der
     *         Suchstrategie gefunden wurden.
     */
    private int searchDefaultSender(DatasourceJoiner dsj)
    {
      // Auswertung des Abschnitts
      // PersoenlicheAbsenderlisteInitialisierung/Suchstrategie
      ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
      ConfigThingy strat = null;
      try
      {
        strat =
          wmConf.query("PersoenlicheAbsenderlisteInitialisierung").query(
            "Suchstrategie").getLastChild();
      }
      catch (NodeNotFoundException e)
      {}

      if (strat != null)
      {
        // Suche über Suchstrategie aus Konfiguration
        for (Iterator<ConfigThingy> iter = strat.iterator(); iter.hasNext();)
        {
          ConfigThingy element = iter.next();
          int found = 0;
          if (element.getName().equals("BY_JAVA_PROPERTY"))
          {
            found = new ByJavaPropertyFinder(dsj).find(element);
          }
          else if (element.getName().equals("BY_OOO_USER_PROFILE"))
          {
            found = new ByOOoUserProfileFinder(dsj).find(element);
          }
          else
          {
            LOGGER.error(L.m("Ungültiger Schlüssel in Suchstrategie: %1",
              element.stringRepresentation()));
          }
          if (found != 0) return found;
        }
      }
      else
      {
        // Standardsuche über das OOoUserProfile:
        return new ByOOoUserProfileFinder(dsj).find("Vorname", "${givenname}",
          "Nachname", "${sn}");
      }

      return 0;
    }

    /**
     * Ein DataFinder sucht Datensätze im übergebenen dsj, wobei in der Beschreibung
     * der gesuchten Werte Variablen in der Form "${varname}" verwendet werden
     * können, die vor der Suche in einer anderen Datenquelle aufgelöst werden. Die
     * Auflösung erledigt durch die konkrete Klasse.
     */
    private abstract static class DataFinder
    {
      private DatasourceJoiner dsj;

      public DataFinder(DatasourceJoiner dsj)
      {
        this.dsj = dsj;
      }

      /**
       * Erwartet ein ConfigThingy, das ein oder zwei Schlüssel-/Wertpaare enthält
       * (in der Form "<KNOTEN>(<dbSpalte1> 'wert1' [<dbSpalte2> 'wert2'])" nach
       * denen in der Datenquelle gesucht werden soll. Die Beiden Wertpaare werden
       * dabei UND verknüpft. Die Werte wert1 und wert2 können über die Syntax
       * "${name}" Variablen referenzieren, die vor der Suche aufgelöst werden.
       *
       * @param conf
       *          Das ConfigThingy, das die Suchabfrage beschreibt.
       * @return Die Anzahl der gefundenen Datensätze.
       */
      public int find(ConfigThingy conf)
      {
        int count = 0;
        String id1 = "";
        String id2 = "";
        String value1 = "";
        String value2 = "";
        for (Iterator<ConfigThingy> iter = conf.iterator(); iter.hasNext();)
        {
          ConfigThingy element = iter.next();
          if (count == 0)
          {
            id1 = element.getName();
            value1 = element.toString();
            count++;
          }
          else if (count == 1)
          {
            id2 = element.getName();
            value2 = element.toString();
            count++;
          }
          else
          {
            LOGGER.error(L.m("Nur max zwei Schlüssel/Wert-Paare werden als Argumente für Suchanfragen akzeptiert!"));
          }
        }

        if (count == 1)
        {
          return find(id1, value1);
        }
        else if (count == 2)
        {
          return find(id1, value1, id2, value2);
        }
        return 0;
      }

      /**
       * Sucht in der Datenquelle nach Datensätzen deren Feld dbSpalte den
       * evaluierten Wert von value enthält und überträgt die gefundenen Werte in die
       * PAL.
       *
       * @param dbSpalte
       *          der Feldname über den nach dem evaluierten Wert von value gesucht
       *          wird.
       * @param value
       *          value wird vor der Suche mittels evaluate() evaluiert (d.h. evtl.
       *          vorhandene Variablen durch die entsprechenden Inhalte ersetzt
       *          ersetzt).
       * @return die Anzahl der gefundenen Datensätze
       */
      protected int find(String dbSpalte, String value)
      {
        LOGGER.trace(this.getClass().getSimpleName() + ".tryToFind(" + dbSpalte
          + " '" + value + "')");
        try
        {
          String v = evaluate(value);
          if (v.length() == 0) return 0;
          QueryResults r = dsj.find(dbSpalte, v);
          return addToPAL(r);
        }
        catch (TimeoutException e)
        {
          LOGGER.error("", e);
        }
        catch (IllegalArgumentException e)
        {
          LOGGER.debug("", e);
        }
        return 0;
      }

      /**
       * Sucht in der Datenquelle nach Datensätzen wobei die beiden Suchbedingungen
       * (dbSpalte1==evaluate(value1) und dbSpalte2==evaluate(value2)) mit UND
       * verknüpft sind - die gefundenen Werte werden danach in die PAL kopiert.
       *
       * @param dbSpalte1
       *          der Feldname über den nach dem evaluierten Wert von value gesucht
       *          wird.
       * @param value1
       *          value wird vor der Suche mittels evaluate() evaluiert (d.h. evtl.
       *          vorhandene Variablen durch die entsprechenden Inhalte ersetzt
       *          ersetzt).
       * @param dbSpalte2
       *          der Feldname über den nach dem evaluierten Wert von value gesucht
       *          wird.
       * @param value2
       *          value wird vor der Suche mittels evaluate() evaluiert (d.h. evtl.
       *          vorhandene Variablen durch die entsprechenden Inhalte ersetzt
       *          ersetzt).
       * @return die Anzahl der gefundenen Datensätze
       */
      protected int find(String dbSpalte1, String value1, String dbSpalte2,
          String value2)
      {
        LOGGER.trace(this.getClass().getSimpleName() + ".tryToFind(" + dbSpalte1
          + " '" + value1 + "' " + dbSpalte2 + " '" + value2 + "')");
        try
        {
          String v1 = evaluate(value1);
          String v2 = evaluate(value2);
          if (v1.length() == 0 || v2.length() == 0) return 0;
          QueryResults r = dsj.find(dbSpalte1, v1, dbSpalte2, v2);
          return addToPAL(r);
        }
        catch (TimeoutException e)
        {
          LOGGER.error("", e);
        }
        catch (IllegalArgumentException e)
        {
          LOGGER.debug("", e);
        }
        return 0;
      }

      /**
       * Kopiert alle matches von QueryResults in die PAL.
       */
      private int addToPAL(QueryResults r)
      {
        for (Iterator<Dataset> iter = r.iterator(); iter.hasNext();)
        {
          DJDataset element = (DJDataset) iter.next();
          element.copy();
        }
        return r.size();
      }

      /**
       * Ersetzt die Variablen in exp durch deren evaluierten Inhalt, wobei die
       * Evaluierung über getValueForKey() erfolgt, die von jeder konkreten Klasse
       * implementiert wird. Evaluate() stellt auch sicher, dass die von
       * getValueForKey() zurückgelieferten Werte nicht selbst Variablen enthalten
       * können (indem die Variablenbegrenzer "${" und "}" durch "<" bzw. ">" ersetzt
       * werden.
       *
       * @param exp
       *          der zu evaluierende Ausdruck
       * @return
       */
      protected String evaluate(String exp)
      {
        final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^\\}]*)\\}");
        while (true)
        {
          Matcher m = VAR_PATTERN.matcher(exp);
          if (!m.find()) break;
          String key = m.group(1);
          String value = getValueForKey(key);
          // keine Variablenbegrenzer "${" und "}" in value zulassen:
          value = value.replaceAll("\\$\\{", "<");
          value = value.replaceAll("\\}", ">");
          exp = m.replaceFirst(value);
        }
        return exp;
      }

      /**
       * Liefert den Wert zu einer Variable namens key und muss von jeder konkreten
       * Finder-Klasse implementiert werden.
       *
       * @param key
       *          Der Schlüssel, zu dem der Wert zurückgeliefert werden soll.
       * @return der zugehörige Wert zum Schlüssel key.
       */
      protected abstract String getValueForKey(String key);
    }

    /**
     * Ein konkreter DataFinder, der für die Auflösung der Variable in getValueForKey
     * im Benutzerprofil der OOo Registry nachschaut (das selbe wie
     * Extras->Optionen->OpenOffice.org->Benutzerdaten).
     *
     * @author christoph.lutz
     */
    private static class ByOOoUserProfileFinder extends DataFinder
    {
      public ByOOoUserProfileFinder(DatasourceJoiner dsj)
      {
        super(dsj);
      }

      @Override
      protected String getValueForKey(String key)
      {
        try
        {
        	XComponentContext ctx = WollMuxSingleton.getInstance()
				    .getXComponentContext();
			Object confProvider = ctx.getServiceManager()
				    .createInstanceWithContext(
				        "com.sun.star.configuration.ConfigurationProvider", ctx);
			Object confView =
				    UNO.XMultiServiceFactory(confProvider)
				        .createInstanceWithArguments(
				            "com.sun.star.configuration.ConfigurationAccess",
				            new UnoProps("nodepath",
				                "/org.openoffice.UserProfile/Data").getProps());
				return UNO.XNameAccess(confView).getByName(key).toString();
        }
        catch (Exception e)
        {
          LOGGER.error(
            L.m(
              "Konnte den Wert zum Schlüssel '%1' des OOoUserProfils nicht bestimmen:",
              key), e);
        }
        return "";
      }
    }

    /**
     * Ein konkreter DataFinder, der für die Auflösung der Variable in getValueForKey
     * die Methode System.getProperty(key) verwendet.
     *
     * @author christoph.lutz
     */
    private static class ByJavaPropertyFinder extends DataFinder
    {
      public ByJavaPropertyFinder(DatasourceJoiner dsj)
      {
        super(dsj);
      }

      @Override
      protected String getValueForKey(String key)
      {
        try
        {
          return System.getProperty(key);
        }
        catch (java.lang.Exception e)
        {
          LOGGER.error(
            L.m("Konnte den Wert der JavaProperty '%1' nicht bestimmen:", key), e);
        }
        return "";
      }
    }
  }