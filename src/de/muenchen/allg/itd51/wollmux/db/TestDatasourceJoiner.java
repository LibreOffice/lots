/*
 * Dateiname: TestDatasourceJoiner.java
 * Projekt  : WollMux
 * Funktion : Variante des DatasourceJoiners, die zum testen besser geeignet ist.
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
 * 19.10.2005 | BNK | Erstellung
 * 20.10.2005 | BNK | Fertig
 * 20.10.2005 | BNK | Fallback Rolle -> OrgaKurz
 * 24.10.2005 | BNK | Erweitert um die Features, die PAL Verwalten braucht
 * 31.10.2005 | BNK | TestDJ ist jetzt nur noch normaler DJ mit Default-
 *                    initialisierung und ohne speichern
 * 03.11.2005 | BNK | besser kommentiert
 * 26.05.2006 | BNK | Testfaelle für dj.find(Query)
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.db;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

/**
 * Variante des DatasourceJoiners, die zum testen besser geeignet ist.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TestDatasourceJoiner extends DatasourceJoiner
{
  private static final long TEST_QUERY_TIMEOUT = 500000;

  /**
   * TestDJ soll nichts (ungewollt) überschreiben, deshalb hier no-op (aber es gibt
   * reallySaveCacheAndLOS()).
   */
  public void saveCacheAndLOS(File cacheFile)
  {
  // TestDJ soll nichts (ungewollt) überschreiben
  }

  /**
   * Speichert den LOS und den Cache in der Datei cacheFile.
   * 
   * @throws IOException
   *           falls ein Fehler beim Speichern auftritt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void reallySaveCacheAndLOS(File cacheFile) throws IOException
  {
    super.saveCacheAndLOS(cacheFile);
  }

  /**
   * Erzeugt einen TestDatasourceJoiner (im wesentlichen ein DatasourceJoiner, der
   * mit bestimmten hartcodierten Vorgaben initialisiert wird).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public TestDatasourceJoiner() throws Exception
  { // TESTED
    File curDir = new File(System.getProperty("user.dir"));
    URL context = curDir.toURI().toURL();
    File losCache = new File(curDir, "testdata/cache.conf");
    String confFile = "testdata/testdjjoin.conf";
    URL confURL = new URL(context, confFile);
    ConfigThingy joinConf = new ConfigThingy("", confURL);
    init(joinConf, "Personal", losCache, context, TEST_QUERY_TIMEOUT);
  }

  protected long queryTimeout()
  {
    return TEST_QUERY_TIMEOUT;
  }

  /**
   * Gibt results aus.
   * 
   * @param query
   *          ein String der in die Überschrift der Ausgabe geschrieben wird, damit
   *          der Benutzer sieht, was er angezeigt bekommt.
   * @param schema
   *          bestimmt, welche Spalten angezeigt werden von den Datensätzen aus
   *          results.
   * @param results
   *          die Ergebnisse der Anfrage.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void printResults(String query, Set<String> schema,
      QueryResults results)
  {
    System.out.println("Results for query \"" + query + "\":");
    for (Dataset result : results)
    {
      try
      {
        System.out.print(result.get("Nachname") + ": ");
      }
      catch (Exception x)
      {}

      Iterator<String> spiter = schema.iterator();
      while (spiter.hasNext())
      {
        String spalte = spiter.next();
        String wert = "Spalte " + spalte + " nicht gefunden!";
        try
        {
          wert = result.get(spalte);
          if (wert == null)
            wert = "unbelegt";
          else
            wert = "\"" + wert + "\"";
        }
        catch (ColumnNotFoundException x)
        {}
        ;
        System.out.print(spalte + "=" + wert + (spiter.hasNext() ? ", " : ""));
      }
      System.out.println();
    }
    System.out.println();
  }
}
