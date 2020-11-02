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
package de.muenchen.allg.itd51.wollmux.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.InvalidIdentifierException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * TODO: überarbeiten! Die VisibleTextFragmentList repräsentiert die ausgewertete
 * Liste aller in den Konfigurationsdateien beschriebener "Textfragmente" Abschnitte.
 * Sie kümmert sich insbesondere um das Auswerten der Variablen in den URL-Attributen
 * und um die Beachtung der Vorrangregelung: Immer das zuletzt definierte
 * Textfragment oder die zuletzt definierte Variable gewinnt.
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 *
 */
public class VisibleTextFragmentList
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(VisibleTextFragmentList.class);

  /**
   * Abbruchwert zur Vermeidung von Endlosloops bei Variablenersetzungen.
   */
  private static final int MAXCOUNT = 100;

  private VisibleTextFragmentList() {}

  /**
   * Ersetzen der zu dem Block gehörende Variable VAR durch den Wert VALUE
   *
   * @param node
   *          Knoten der die Url enthält die benötigt wird um die Variable zu
   *          bestimmen die von ihm aus sichtbar ist.
   * @param root
   *          das ConfigThingy das die gesamte Configuration enthält
   * @return Das ConfigThingy als String ohne Variablen.
   * @throws EndlessLoopException
   */
  private static String expandVariable(ConfigThingy node, ConfigThingy root)
      throws EndlessLoopException
  {
    // Map der sichtbaren Variablen erzeugen:
    Map<String, String> variables = new HashMap<>();

    for (ConfigThingy var : ConfigThingy.getNodesVisibleAt(node, "VAR", root))
    {
      String name = var.getString("NAME");
      String value = var.getString("VALUE");

      if (name != null && value != null)
      {
        variables.put(name, value);
      }
    }

    LOGGER.trace("Variablenset an Knoten {} '{}':", node.getName(), node);
    for (Map.Entry<String, String> ent : variables.entrySet())
    {
      LOGGER.trace("  {}=\"{}\"", ent.getKey(), ent.getValue());
    }

    // Matcher zum Finden der Variablen ersetzen:
    String string = node.toString();
    Pattern var = Pattern.compile("\\$\\{([^\\}]*)\\}");
    Matcher m = var.matcher(string);

    // Variablen so lange ersetzen, bis keine Variable mehr gefunden wird.
    // Vermeidung von möglichen Endlosloops durch Abbruch nach MAXCOUNT
    // Ersetzungen.
    int count = 0;
    while (m.find() && MAXCOUNT > ++count)
    {
      String key = m.group(1);
      if (variables.containsKey(key))
      {
        string = string.substring(0, m.start()) + variables.get(key) + string.substring(m.end());
        if (LOGGER.isTraceEnabled())
        {
          LOGGER.trace("  Ersetzen der Variable {} --> {}", m.group(0), string);
        }
        // Nach jeder Ersetzung wieder von vorne anfangen.
        m = var.matcher(string);
      }
      else
      {
        // Die Variable kann nicht ersetzt werden und wird auch nicht
        // ersetzt. Eine Exception muss deswegen nicht geworfen werden, es ist
        // aber sinnvoll, die Fehlermeldung in einem Logger rauszuschreiben.
        LOGGER.error("Die Variable '{}' in der URL '{}' ist nicht definiert.", key, string);
      }
    }
    if (count == MAXCOUNT)
      throw new EndlessLoopException(L.m(
        "Endlosschleife ber der Ersetzung der Variablen in URL '%1'.",
        node.toString()));
    return string;
  }

  /**
   * Gibt die URLs des unter der frag_id definierten Textfragmente zurück.
   *
   * @param fragId
   *          Die ID des gesuchten Textfragments.
   * @return die URL des unter der frag_id definierten Textfragments.
   * @throws InvalidIdentifierException
   */
  public static List<String> getURLsByID(ConfigThingy conf, String fragId)
      throws InvalidIdentifierException
  {
    ConfigThingy.checkIdentifier(fragId);

    LinkedList<ConfigThingy> tfListe = new LinkedList<>();
    ConfigThingy tfConf = conf.query("Textfragmente");
    Iterator<ConfigThingy> iter = tfConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy confTextfragmente = iter.next();
      tfListe.addFirst(confTextfragmente);
    }

    Iterator<ConfigThingy> iterTbListe = tfListe.iterator();
    List<String> urls = new ArrayList<>();
    while (iterTbListe.hasNext())
    {
      ConfigThingy textfragmente = iterTbListe.next();

      ConfigThingy mappingsConf = textfragmente.queryByChild("FRAG_ID");

      for (ConfigThingy mappingConf : mappingsConf)
      {

        String fragIdConf = null;
        try
        {
          fragIdConf = mappingConf.get("FRAG_ID").toString();
          // Typischen Konfigurationsfehler korrigieren
          if (".*".equals(fragIdConf)) {
            fragIdConf = ".+";
          }
        }
        catch (NodeNotFoundException e)
        {
          LOGGER.error(L.m("FRAG_ID Angabe fehlt in %1",
            mappingConf.stringRepresentation()), e);
          continue;
        }

        ConfigThingy url = null;
        try
        {
          url = mappingConf.get("URL");
        }
        catch (NodeNotFoundException e)
        {
          LOGGER.trace("", e);
          // kommt nicht vor, da obiger queryByChild immer URL liefert
          continue;
        }

        if (fragId.matches(fragIdConf))
        {
          for (ConfigThingy urlNext : url)
          {
            try
            {
              String urlStr = expandVariable(urlNext, conf);
              urlStr = fragId.replaceAll(fragIdConf, urlStr);
              urls.add(urlStr);
            }
            catch (EndlessLoopException e)
            {
              LOGGER.error(
                L.m(
                  "Die URL zum Textfragment '%1' mit der FRAG_ID '%2' ist fehlerhaft.",
                  mappingConf.stringRepresentation(), fragId), e);
            }
          }
        }
      }
    }
    return urls;
  }

}
