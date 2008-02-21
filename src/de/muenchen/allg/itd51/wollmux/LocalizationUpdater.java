/*
 * Dateiname: LocalizationUpdater.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse liest alle zu lokalisierenden Strings des WollMux 
 *            aus dem Source-Code und aktualisiert die Datei localization.conf.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 21.02.2008 | LUT | Erstellung als LocalizationUpdater
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Diese Klasse liest alle zu lokalisierenden Strings des WollMux aus dem
 * Source-Code und aktualisiert die Datei localization.conf. Dabei wird wie
 * folgt vorgegangen: Bereits bestehende Einträge bleiben in der Reihenfolge, in
 * der sie bestehen. Durch den Update neu hinzukommende Einträge werden an das
 * Ende der Liste angehängt. Bereits bestehende Einträge, zu denen es im Code
 * keine zugehörigen Original-Strings mehr gibt, werden auskommentiert und an
 * das Ende der Liste verschoben. Sind unter den auskommentierten, verschobenen
 * Zeilen auch Einträge dabei, die tatsächlich eine Übersetzung in anderen
 * Sprachen besitzen, so wird nach dem Update eine Warnung ausgegeben, die
 * darauf hinweist, dass hier bereits übersetzte Strings mit dem nächsten
 * Update-Lauf entfernt werden.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class LocalizationUpdater
{
  /**
   * Enthält den Pfad zur Konfigurationsdatei localization.conf aus Sicht des
   * Projekt-Hauptverzeichnisses.
   */
  private static File localizationConfFile = new File("./src/data/localization.conf");

  /**
   * Enthält das Wurzelverzeichnis der Source-Dateien aus Sicht des
   * Projekt-Hauptverzeichnises.
   */
  private static File sourcesDir = new File("./src/");

  /**
   * Enthält das Pattern, mit dem nach L.m-Strings gesucht wird. In Gruppe 1 ist
   * der String dieser zu lokalisierenden Message enthalten.
   */
  private static Pattern L_m_Pattern = Pattern.compile("L.m\\(\\s*\"((?:\\\\\"|[^\"])*)\"");

  /**
   * Muss aus dem Hauptverzeichnis des WollMux-Projekts ausgeführt werden und
   * aktualisiert die Datei localization.conf.
   * 
   * @param args
   *          die args werden nicht ausgewertet
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void main(String[] args)
  {
    updateLocalizationConf();
  }

  /**
   * Macht die eigentliche Arbeit.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static void updateLocalizationConf()
  {
    System.out.println(L.m("Aktualisiere die Datei localization.conf"));

    // localization.conf einlesen und knownOriginals sammeln.
    HashSet knownOriginals = new HashSet();
    HashSet currentOriginals = new HashSet();
    ConfigThingy localizationConf = new ConfigThingy("localization");
    ConfigThingy messages;
    try
    {
      localizationConf = new ConfigThingy("localization",
        localizationConfFile.toURL());
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    try
    {
      messages = localizationConf.query("L10n").query("Messages").getLastChild();
      for (Iterator iter = messages.iterator(); iter.hasNext();)
      {
        ConfigThingy element = (ConfigThingy) iter.next();
        if (element.getName().equalsIgnoreCase("original"))
          knownOriginals.add(element.toString());
      }
    }
    catch (NodeNotFoundException e)
    {
      messages = new ConfigThingy("Messages");
    }

    // Alle .java Files aus scanForSourcesDir iterieren und L.m()s rausziehen
    int countNew = 0;
    List sources = new ArrayList();
    addJavaFilesRecursive(sources, sourcesDir);
    int count = 0;
    int lastProgress = 0;
    for (Iterator iter = sources.iterator(); iter.hasNext();)
    {
      File file = (File) iter.next();

      String str = readSourceFile(file);

      Matcher m = L_m_Pattern.matcher(str);
      while (m.find())
      {
        String original = evalString(m.group(1));
        currentOriginals.add(original);
        if (!knownOriginals.contains(original))
        {
          messages.add("original").add(original);
          knownOriginals.add(original);
          countNew++;
        }
      }

      // Fortschrittsanzeige in
      count++;
      int progress = (int) ((1.0 * count / sources.size()) * 100);
      if (progress / 10 != lastProgress / 10)
      {
        System.out.println(L.m("Fortschritt: %1 %", new Integer(progress)));
        lastProgress = progress;
      }
    }

    // Neue localization.conf erzeugen (die StringRepresentation von
    // ConfigThingy macht das nicht schön genug, deshalb hier eine eigene
    // Ausgaberoutine.
    String str = "L10n(\n  Messages(\n";
    String removed = "";
    int countRemoved = 0;
    HashMap countTranslations = new HashMap();
    boolean valid = false;
    boolean removedTranslatedMessagesWarning = false;
    for (Iterator iter = messages.iterator(); iter.hasNext();)
    {
      ConfigThingy element = (ConfigThingy) iter.next();
      String elementStr = element.stringRepresentation();
      elementStr.replaceAll("\\n", "%n");

      if (element.getName().equalsIgnoreCase("original"))
      {
        valid = currentOriginals.contains(element.toString());
        if (valid)
        {
          str += "\n    " + elementStr + "\n";
        }
        else
        {
          removed += "\n# removed:\n#    " + elementStr + "\n";
          countRemoved++;
        }
      }
      else
      {
        if (valid)
        {
          str += "       " + elementStr + "\n";
          String language = element.getName().toLowerCase();
          Integer ct = (Integer) countTranslations.get(language);
          int cti = (ct != null) ? ct.intValue() : 0;
          countTranslations.put(language, new Integer(cti + 1));
        }
        else
        {
          removedTranslatedMessagesWarning = true;
          removed += "#       " + elementStr + "\n";
        }
      }
    }
    str += removed;
    str += "\n  )\n)";
    try
    {
      FileWriter writer = new FileWriter(localizationConfFile);
      writer.write(str);
      writer.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    // Statistik und Warnung ausgeben:
    System.out.println("");
    System.out.println(L.m("Neue original-Strings: %1", new Integer(countNew)));
    System.out.println(L.m("Auskommentierte original-Strings: %1", new Integer(
      countRemoved)));
    System.out.println(L.m("Gesamtzahl aktuelle original-Strings: %1", new Integer(
      currentOriginals.size())));
    for (Iterator iter = countTranslations.keySet().iterator(); iter.hasNext();)
    {
      String language = (String) iter.next();
      int ct = ((Integer) countTranslations.get(language)).intValue();
      System.out.println(L.m("Davon nicht übersetzt in Sprache %1: %2", language,
        new Integer(currentOriginals.size() - ct)));
    }

    if (removedTranslatedMessagesWarning)
      System.err.println("\n"
                         + L.m("ACHTUNG: Bitte überprüfen Sie den Inhalt Ihrer Datei localization.conf,\nda bereits übersetzte aber nicht mehr benötigte Einträge auskommentiert\nwurden und mit der nächsten Aktualisierung endgültig entfernt werden."));
  }

  /**
   * Liefert den kompletten Inhalt der ISO-8859-1 encodierten Quelldatei file
   * als String zurück.
   * 
   * @param file
   * @throws FileNotFoundException
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static String readSourceFile(File file)
  {
    String str = "";
    try
    {
      InputStreamReader r = new InputStreamReader(new FileInputStream(file),
        "ISO-8859-1");
      char[] buff = new char[1024];
      int count;
      while ((count = r.read(buff)) > 0)
      {
        str += new String(buff, 0, count);
      }
      r.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return str;
  }

  /**
   * Fügt alle Dateien, die mit .java enden aus diesem Verzeichnis fileOrDir und
   * aus allen Unterverzeichnissen zur Liste l hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static void addJavaFilesRecursive(List l, File fileOrDir)
  {
    if (fileOrDir.isFile() && fileOrDir.getName().endsWith(".java"))
    {
      l.add(fileOrDir);
      return;
    }

    if (fileOrDir.isDirectory())
    {
      File[] files = fileOrDir.listFiles();
      for (int i = 0; i < files.length; i++)
      {
        addJavaFilesRecursive(l, files[i]);
      }
    }
  }

  /**
   * Diese Methode evaluiert einen String aus dem SourceCode, der auch
   * Character-Escape-Sequenzen enthalten kann, in der Form, wie ihn der
   * Java-Compiler interpretieren würde und liefert den Java-String zurück.
   * 
   * Derzeit werden folgende Escape-Sequenzen aus
   * http://java.sun.com/docs/books/tutorial/java/data/characters.html
   * umgesetzt: \t, \b, \n, \r, \f, \', \", \\
   * 
   * @param str
   *          Ein String aus dem SourceCode, der zu übersetzen ist.
   * @return den evaluierten String
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static String evalString(String str)
  {
    String evalStr = new String(str);
    evalStr = evalStr.replaceAll("\\\\t", "\t");
    evalStr = evalStr.replaceAll("\\\\b", "\b");
    evalStr = evalStr.replaceAll("\\\\n", "\n");
    evalStr = evalStr.replaceAll("\\\\r", "\r");
    evalStr = evalStr.replaceAll("\\\\f", "\f");
    evalStr = evalStr.replaceAll("\\\\'", "\'");
    evalStr = evalStr.replaceAll("\\\\\"", "\"");
    evalStr = evalStr.replaceAll("\\\\\\\\", "\\\\");
    return evalStr;
  }
}
