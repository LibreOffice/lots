/*
 * Dateiname: VisibleTextFragmentList.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert die ausgewertete Liste aller in wollmux.conf definierten
 *            Textfragmente.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 13.10.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Die VisibleTextFragmentList repräsentiert die ausgewertete Liste aller in den
 * Konfigurationsdateien beschriebener "Textfragmente" Abschnitte. Sie kümmert
 * sich insbesondere um das Auswerten der Variablen in den URL-Attributen und um
 * die Beachtung der Vorrangregelung: Immer das zuletzt definierte Textfragment
 * oder die zuletzt definierte Variable gewinnt.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
public class VisibleTextFragmentList
{

  /**
   * Speichert die Wurzel des Konfigurationsbaumes, um später Variablen auflösen
   * zu können.
   */
  private ConfigThingy root;

  /**
   * Die fragmentMap (of ConfigThingy) enthält alle sichtbaren Textfragmente.
   */
  private Map fragmentMap;

  /**
   * Abbruchwert zur Vermeidung von Endlosloops bei Variablenersetzungen.
   */
  private static final int MAXCOUNT = 100;

  /**
   * Der Konstruktor erzeugt eine neue VisibleTextFragmentList aus einer
   * gegebenen Konfiguration.
   * 
   * @param root
   *          Wurzel des Konfigurationsbaumes der Konfigurationsdatei.
   * @throws NodeNotFoundException
   */
  public VisibleTextFragmentList(ConfigThingy root)
      throws NodeNotFoundException
  {
    this.root = root;
    this.fragmentMap = new HashMap();
    ConfigThingy tfrags;
    tfrags = root.get("Textfragmente").getByChild("FRAG_ID");
    Iterator s = tfrags.iterator();
    while (s.hasNext())
    {
      ConfigThingy frag = (ConfigThingy) s.next();
      fragmentMap.put(frag.get("FRAG_ID").toString(), frag);
    }
    Logger.debug2("VisibleTextFragmentList: "
                  + fragmentMap.size()
                  + " entries.");
  }

  private String expandVariable(ConfigThingy node, ConfigThingy root)
      throws EndlessLoopException
  {
    // Map der sichtbaren Variablen erzeugen:
    Map variables = new HashMap();
    Iterator i = ConfigThingy.getNodesVisibleAt(node, "VAR", root).iterator();
    while (i.hasNext())
    {
      ConfigThingy var = (ConfigThingy) i.next();
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
    Logger.debug2("Variablenset an Knoten "
                  + node.getName()
                  + " \""
                  + node.toString()
                  + "\":");
    Iterator keys = variables.keySet().iterator();
    while (keys.hasNext())
    {
      String key = (String) keys.next();
      String value = (String) variables.get(key);
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
        string = string.substring(0, m.start())
                 + (String) variables.get(key)
                 + string.substring(m.end());
        // string = m.replaceFirst((String) variables.get(key));
        Logger.debug2("  Ersetzen der Variable "
                      + m.group(0)
                      + " --> "
                      + string);
        // Nach jeder Ersetzung wieder von vorne anfangen.
        m = var.matcher(string);
      }
      else
      {
        // Die Variable kann nicht ersetzt werden und wird auch nicht
        // ersetzt. Eine Exception muss deswegen nicht geworfen werden, es ist
        // aber sinnvoll, die Fehlermeldung in einem Logger rauszuschreiben.
        Logger.error("Die Variable \""
                     + key
                     + "\" in der URL \""
                     + string
                     + "\" ist nicht definiert.");
      }
    }
    if (count == MAXCOUNT)
      throw new EndlessLoopException(
          "Endlosschleife ber der Ersetzung der Variablen in URL \""
              + node.toString()
              + "\".");
    return string;
  }

  /**
   * Gibt die URL des unter der frag_id definierten Textfragmente zurück. Falls
   * das Textfragment nicht existiert, wird eine TextfragmentNotDefinedException
   * geworfen.
   * 
   * @param frag_id
   *          Die ID des gesuchten Textfragments.
   * @return die URL des unter der frag_id definierten Textfragments. Falls das
   *         Textfragment nicht existiert oder die URL ungültig ist, wird null
   *         zurückgeliefert.
   * @throws NodeNotFoundException
   *           Das Attribut URL des Textfragments ist nicht definiert.
   * @throws TextFragmentNotDefinedException
   *           Das gesuchte Textfragment ist nicht definiert.
   * @throws EndlessLoopException
   *           Bei der Ersetzung der Variablen in der URL trat eine
   *           Endlosschleife auf.
   */
  public String getURLByID(String frag_id) throws NodeNotFoundException,
      TextFragmentNotDefinedException, EndlessLoopException
  {
    if (frag_id != null && fragmentMap.containsKey(frag_id))
    {
      ConfigThingy frag = (ConfigThingy) fragmentMap.get(frag_id);
      return expandVariable(frag.get("URL"), root);
    }
    else
      throw new TextFragmentNotDefinedException(frag_id);
  }

  /**
   * Diese Methode erzeugt ein String-Array der IDs aller erkannten
   * Textfragmente.
   * 
   * @return Ein String-Array der IDs aller erkannten Textfragmente.
   */
  public String[] getIDs()
  {
    Set keys = fragmentMap.keySet();
    return (String[]) keys.toArray(new String[keys.size()]);
  }

  /**
   * Testet die Funktionsweise der VisibleTextFragmentList. Eine in url
   * angegebene Konfigdatei wird eingelesen und die dazugehörige
   * VisibleTextFragmentList erstellt. Anschliessend wird die ausgegeben.
   * 
   * @param args
   *          url, dabei ist url die URL einer zu lesenden Config-Datei. Das
   *          Programm gibt die Liste der Textfragmente aus.
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws IOException
  {
    try
    {
      if (args.length < 1)
      {
        System.out.println("USAGE: <url>");
        System.exit(0);
      }
      Logger.init(Logger.DEBUG);

      File cwd = new File(".");

      args[0] = args[0].replaceAll("\\\\", "/");
      ConfigThingy conf = new ConfigThingy(args[0], new URL(cwd.toURL(),
          args[0]));

      VisibleTextFragmentList tfrags;
      tfrags = new VisibleTextFragmentList(conf);

      String[] ids = tfrags.getIDs();
      for (int i = 0; i < ids.length; i++)
      {
        try
        {
          Logger.debug("Textfragment: "
                       + ids[i]
                       + " --> "
                       + tfrags.getURLByID(ids[i]));
        }
        catch (EndlessLoopException e)
        {
          Logger.error(e);
        }
      }

    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    System.exit(0);
  }
}
