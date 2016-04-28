/*
 * Dateiname: VisibleTextFragmentList.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert die ausgewertete Liste aller in wollmux.conf definierten
 *            Textfragmente.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 13.10.2005 | LUT | Erstellung
 * 20.04.2006 | LUT | Testen ob FRAG_IDs Identifier sind
 * 21.04.2006 | LUT | + Keine Warnung wenn keine Textfragmente definiert - Eine
 *                      Konfiguration ohne Textfragmente kann durchaus gewünscht sein.
 *                    + getURLByID: ConfigurationErrorException statt NodeNotFoundException
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.document;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.InvalidIdentifierException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

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

  /**
   * Abbruchwert zur Vermeidung von Endlosloops bei Variablenersetzungen.
   */
  private static final int MAXCOUNT = 100;

  /**
   * Ersetzen der zu dem Block gehörende Variable VAR durch den Wert VALUE
   * 
   * @param node
   *          Knoten der die Url enthält die benötigt wird um die Variable zu
   *          bestimmen die von ihm aus sichtbar ist.
   * @param root
   *          das ConfigThingy das die gesamte Configuration enthält
   * @return
   * @throws EndlessLoopException
   */
  private static String expandVariable(ConfigThingy node, ConfigThingy root)
      throws EndlessLoopException
  {
    // Map der sichtbaren Variablen erzeugen:
    Map<String, String> variables = new HashMap<String, String>();
    Iterator<ConfigThingy> i =
      ConfigThingy.getNodesVisibleAt(node, "VAR", root).iterator();
    while (i.hasNext())
    {
      ConfigThingy var = i.next();
      try
      {
        variables.put(var.get("NAME").toString(), var.get("VALUE").toString());
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(e);
      }
    }

    // Debug-Ausgabe:
    Logger.debug2(L.m("Variablenset an Knoten %1 '%2':", node.getName(),
      node.toString()));

    for (Map.Entry<String, String> ent : variables.entrySet())
    {
      String key = ent.getKey();
      String value = ent.getValue();
      Logger.debug2("  " + key + "=\"" + value + "\"");
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
        string =
          string.substring(0, m.start()) + variables.get(key)
            + string.substring(m.end());
        // string = m.replaceFirst((String) variables.get(key));
        Logger.debug2(L.m("  Ersetzen der Variable %1 --> %2", m.group(0), string));
        // Nach jeder Ersetzung wieder von vorne anfangen.
        m = var.matcher(string);
      }
      else
      {
        // Die Variable kann nicht ersetzt werden und wird auch nicht
        // ersetzt. Eine Exception muss deswegen nicht geworfen werden, es ist
        // aber sinnvoll, die Fehlermeldung in einem Logger rauszuschreiben.
        Logger.error(L.m("Die Variable '%1' in der URL '%2' ist nicht definiert.",
          key, string));
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
   * @param frag_id
   *          Die ID des gesuchten Textfragments.
   * @return die URL des unter der frag_id definierten Textfragments.
   * @throws InvalidIdentifierException
   */
  public static Vector<String> getURLsByID(ConfigThingy conf, String frag_id)
      throws InvalidIdentifierException
  {
    ConfigThingy.checkIdentifier(frag_id);

    LinkedList<ConfigThingy> tfListe = new LinkedList<ConfigThingy>();
    ConfigThingy tfConf = conf.query("Textfragmente");
    Iterator<ConfigThingy> iter = tfConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy confTextfragmente = iter.next();
      tfListe.addFirst(confTextfragmente);
    }

    Iterator<ConfigThingy> iterTbListe = tfListe.iterator();
    Vector<String> urls = new Vector<String>();
    while (iterTbListe.hasNext())
    {
      ConfigThingy textfragmente = iterTbListe.next();

      ConfigThingy mappingsConf = textfragmente.queryByChild("FRAG_ID");
      Iterator<ConfigThingy> iterMappings = mappingsConf.iterator();

      while (iterMappings.hasNext())
      {
        ConfigThingy mappingConf = iterMappings.next();

        String frag_idConf = null;
        try
        {
          frag_idConf = mappingConf.get("FRAG_ID").toString();
          // Typischen Konfigurationsfehler korrigieren
          if (frag_idConf.equals(".*")) frag_idConf = ".+";
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(L.m("FRAG_ID Angabe fehlt in %1",
            mappingConf.stringRepresentation()));
          continue;
        }

        Iterator<ConfigThingy> URLIterator = null;
        try
        {
          URLIterator = mappingConf.get("URL").iterator();
        }
        catch (NodeNotFoundException e)
        {
          // kommt nicht vor, da obiger queryByChild immer URL liefert
          continue;
        }

        if (frag_id.matches(frag_idConf))
        {

          while (URLIterator.hasNext())
          {
            ConfigThingy url_next = URLIterator.next();
            try
            {
              String urlStr = expandVariable(url_next, conf);
              urlStr = frag_id.replaceAll(frag_idConf, urlStr);
              urls.add(urlStr);
            }
            catch (EndlessLoopException e)
            {
              Logger.error(
                L.m(
                  "Die URL zum Textfragment '%1' mit der FRAG_ID '%2' ist fehlerhaft.",
                  mappingConf.stringRepresentation(), frag_id), e);
            }
          }
        }
      }
    }
    return urls;
  }

}
