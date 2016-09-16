/* Copyright (C) 2009 Matthias S. Benkmann
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
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XStorable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Klasse, die das Aufrufen von externen Applikationen erleichtert.
 * 
 */
public class OpenExt
{
  /**
   * Präfix für Verzeichnisnamen zum Herunterladen von URLs für ACTION "openExt".
   */
  private static final String WOLLMUX_DOWNLOAD_DIR_PREFIX =
    "wollmuxbar-temp-download-";

  /**
   * Der ext Parameter den der Konstruktor übergeben bekommt.
   */
  private String ext;

  /**
   * true, falls DOWNLOAD "true" bei der entsprechenden Anwendung spezifiziert wurde.
   * Sorgt dafür, dass die Datei vor dem Aufruf der externen Anwendung lokal
   * gespeichert und der externen Anwendung ein Dateipfad anstelle einer URL
   * übergeben wird.
   */
  private boolean download = false;

  /**
   * true, falls PIPE "true" bei der entsprechenden Anwendung spezifiziert wurde.
   * Sorgt dafür, dass stdout und stderr von einem Thread offen gehalten und geleert
   * werden. Manche Programme brauchen das.
   */
  private boolean pipe = false;

  /**
   * Die Liste der Programme, die bei
   * {@link #launch(de.muenchen.allg.itd51.wollmux.OpenExt.ExceptionHandler)} in der
   * Listenreihenfolge ausprobiert werden. Das erste gefundene Programm wird
   * genommen.
   */
  private List<String> programs = new Vector<String>();

  /**
   * Falls vorhanden, die FILTER-Angabe. Mögliche FILTER sind hier zu
   * finden:basis-link/share/registry/modules/org/openoffice/TypeDetection/Filter.
   * Zusätzliche Optionen des PDF Filters sind hier:
   * http://specs.openoffice.org/appwide/pdf_export/PDFExportDialog.odt
   */
  private String filter = null;

  /**
   * Eine Variante, die Quelle festzulegen ist {@link #setSource(URL)}. Dies setzt
   * diese Variable.
   */
  private URL url = null;

  /**
   * Eine Variante, die Quelle festzulegen ist {@link #setSource(XStorable)}. Dies
   * setzt diese Variable.
   */
  private XStorable doc = null;

  /**
   * Wird durch {@link #prepareTempFile(String)} generiert und durch
   * {@link #storeIfNecessary()} geschrieben.
   */
  private File destFile = null;

  /** true gdw {@link #storeIfNecessary()} bereits aufgerufen wurde. */
  private boolean haveStored = false;

  public static OpenExt getInstance(String ext, String url) throws MalformedURLException
  {
    final String USER_HOME = "${user.home}";
    int uhidx = url.indexOf(USER_HOME);
    if (uhidx >= 0)
    {
      String userHomeUrl =
        new File(System.getProperty("user.home")).toURI().toURL().toString();

      while ((uhidx = url.indexOf(USER_HOME)) >= 0)
        url =
          url.substring(0, uhidx) + userHomeUrl
            + url.substring(uhidx + USER_HOME.length());

      /**
       * Beim Einbau einer URL in eine bestehende URL kann es zu Doppelungen des
       * Protokollbezeichners file: kommen. In diesem Fall entfernen wir das erste
       * davon.
       */
      final Pattern DUPLICATE_FILE_PROTOCOL_PATTERN =
        Pattern.compile("file:/*(file:.*)");
      Matcher m = DUPLICATE_FILE_PROTOCOL_PATTERN.matcher(url);
      if (m.matches()) url = m.group(1);
    }

    URL srcUrl = WollMuxFiles.makeURL(url);
    final OpenExt openExt = new OpenExt(ext, WollMuxFiles.getWollmuxConf());
    openExt.setSource(srcUrl);
    return openExt;
  }
  
  /**
   * Erzeugt ein neues OpenExt Objekt für die Erweitertung ext, wobei Informationen
   * über die externe Applikation aus wollmuxConf,query("ExterneAnwendungen")
   * genommen werden.
   * 
   * ACHTUNG! Jedes OpenExt-Objekt kann nur einmal benutzt werden.
   * 
   * @throws ConfigurationErrorException
   *           falls für ext keine externe Anwendung (korrekt) definiert wurde.
   */
  public OpenExt(String ext, ConfigThingy wollmuxConf)
      throws ConfigurationErrorException
  {
    this.ext = ext;

    ConfigThingy conf = wollmuxConf.query("ExterneAnwendungen");
    Iterator<ConfigThingy> parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      ConfigThingy parentConf = parentIter.next();
      Iterator<ConfigThingy> iter = parentConf.iterator();
      while (iter.hasNext())
      {
        ConfigThingy appConf = iter.next();
        ConfigThingy extConf;
        boolean found = false;
        extConf = appConf.query("EXT");
        if (extConf.count() == 0)
        {
          Logger.error(L.m("Ein Eintrag im Abschnitt \"ExterneAnwendungen\" enthält keine gültige EXT-Angabe."));
          continue;
        }

        for (ConfigThingy oneExtConf : extConf)
          for (ConfigThingy singleExt : oneExtConf)
            if (ext.equals(singleExt.toString())) found = true;

        if (!found) continue;

        Vector<String> commands = new Vector<String>();
        try
        {
          ConfigThingy programConf = appConf.get("PROGRAM");
          programConf.getFirstChild(); // Testen, ob mindestens ein Kind vorhanden
          // ist, ansonsten Exception
          Iterator<ConfigThingy> progiter = programConf.iterator();
          while (progiter.hasNext())
          {
            String prog = progiter.next().toString();
            commands.add(prog);
          }
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(L.m("Ein Eintrag im Abschnitt \"ExterneAnwendungen\" enthält keine gültige PROGRAM-Angabe."));
          continue;
        }

        programs = commands;
        try
        {
          download = appConf.get("DOWNLOAD").toString().equalsIgnoreCase("true");
        }
        catch (Exception x)
        {}
        try
        {
          pipe = appConf.get("PIPE").toString().equalsIgnoreCase("true");
        }
        catch (Exception x)
        {}
        try
        {
          filter = appConf.get("FILTER").toString();
        }
        catch (Exception x)
        {}
      }
    }
  }

  /**
   * Liefert die Liste der Programme in der Reihenfolge in der
   * {@link #launch(de.muenchen.allg.itd51.wollmux.OpenExt.ExceptionHandler)}
   * versuchen wird, sie auszuführen. Die gelieferte Liste ist eine Referenz auf die
   * internen Daten. Es ist also möglich, sie vor dem Aufruf von launch zu verändern.
   * 
   * TESTED
   */
  public List<String> getPrograms()
  {
    return programs;
  }

  /**
   * Legt url als die Datei fest mit der die externe Anwendung gestartet werden soll.
   * Ob die URL selbst als Parameter an das Programm übergeben wird oder ob der
   * Inhalt der URL heruntergeladen und in eine Datei gespeichert wird, deren Pfad
   * als Parameter übergeben wird, wird durch die DOWNLOAD-Angabe in der Definition
   * der externen Anwendung in der wollmux.conf bestimmt.
   * 
   * @see #setSource(XStorable)
   * 
   * TESTED
   */
  public void setSource(URL url)
  {
    this.url = url;
    this.doc = null;
  }

  /**
   * Legt doc als die Datei fest mit der die externe Andwendung gestartet werden
   * soll. Die Datei wird immer zuerst in eine temporäre Datei exportiert bevor die
   * externe Anwendung aufgerufen wird. In welchem Format gespeichert wird, bestimmt
   * die FILTER-Angabe in der Definition der externen Anwendung. Ob die Datei als
   * Pfad oder als file: URL an die externe Anwendung übergeben wird bestimmt die
   * DOWNLOAD-Angabe. Als Dateierweiterung für die temporäre Datei wird das dem
   * Konstruktor übergebenene ext verwendet. Werden mehrere setSource() Funktionen
   * aufgerufen, so gewinnt die letzte.
   * 
   * @throws ConfigurationErrorException
   *           falls die FILTER-Angabe in der Definition der externen Anwendung
   *           fehlt.
   * 
   * @see #setSource(URL)
   * 
   * TESTED
   */
  public void setSource(XStorable doc) throws ConfigurationErrorException
  {
    if (filter == null)
      throw new ConfigurationErrorException(L.m(
        "FILTER-Angabe fehlt bei Anwendung für \"%1\"", ext));
    this.doc = doc;
    this.url = null;
  }

  /**
   * Speichert die Datei auf der Festplatte, falls download==true oder die Quelle ein
   * XStorable ist. In letzterem Fall wird ein Export gemacht, der die Quell-URL des
   * Dokuments nicht ändert.
   * 
   * @throws IOException
   *           falls beim Speichern ein Problem aufgetreten ist. *
   * @throws IllegalStateException
   *           falls diese Methode aufgerufen wurde ohne dass vorher mit setSource()
   *           eine Quelle festgelegt wurde.
   * 
   * 
   * TESTED
   */
  public void storeIfNecessary() throws IOException, IllegalStateException
  {
    if (haveStored) return;
    haveStored = true;

    testState();

    if (doc != null)
    {
      File destFile = prepareTempFile(null);

      PropertyValue[] storeProps = new PropertyValue[] { new PropertyValue() };
      storeProps[0].Name = "FilterName";
      storeProps[0].Value = filter;
      try
      {
        String parsedURL =
          UNO.getParsedUNOUrl(destFile.toURI().toURL().toString()).Complete;
        doc.storeToURL(parsedURL, storeProps);
      }
      catch (Exception x)
      {
        throw new IOException(L.m("Fehler beim Speichern der Datei: %1",
          x.getMessage()));
      }
    }
    else if (download)
    {
      String fileName = url.getPath();
      int idx1 = fileName.lastIndexOf('/');
      int idx2 = fileName.lastIndexOf('\\');
      if (idx2 > idx1) idx1 = idx2;
      if (idx1 >= 0) fileName = fileName.substring(idx1 + 1);

      File destFile = prepareTempFile(fileName);

      InputStream istream = url.openStream();
      if (!destFile.createNewFile())
        throw new IOException(L.m("Konnte temporäre Datei \"%1\" nicht anlegen",
          destFile.getPath()));
      FileOutputStream out = new FileOutputStream(destFile);
      byte[] buffy = new byte[4096];
      int len;
      while (0 <= (len = istream.read(buffy)))
        out.write(buffy, 0, len);
      out.close();
      istream.close();
    }
  }

  private void testState()
  {
    if (doc == null && url == null)
      throw new IllegalStateException(L.m("setSource() wurde nicht aufgerufen"));
  }

  /**
   * Generiert aus fileName (kann leerer String oder null sein) einen Dateinamen und
   * legt ein temporärers Verzeichnis an. Aus der Zusammensetzung von beidem wird
   * {@link #destFile} generiert.
   * 
   * TESTED
   */
  private File prepareTempFile(String fileName) throws IOException
  {
    if (fileName == null) fileName = "";
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    if (!tmpDir.isDirectory() || !tmpDir.canWrite())
      throw new IOException(
        L.m(
          "Temporäres Verzeichnis\n\"%1\"\nexistiert nicht oder kann nicht beschrieben werden!",
          tmpDir.getPath()));

    File downloadDir = null;
    for (int i = 0; i < 1000; ++i)
    {
      downloadDir = new File(tmpDir, WOLLMUX_DOWNLOAD_DIR_PREFIX + i);
      if (downloadDir.mkdir())
        break;
      else
        downloadDir = null;
    }

    if (downloadDir == null)
      throw new IOException(
        L.m("Konnte kein temporäres Verzeichnis für den Download der Datei anlegen!"));

    if (fileName.length() == 0) fileName = "temp";
    if (!fileName.endsWith("." + ext)) fileName += "." + ext;

    destFile = new File(downloadDir, fileName);
    return destFile;
  }

  public static interface ExceptionHandler
  {
    public void handle(Exception x);
  }

  /**
   * Startet die externe Anwendung nachdem falls nötig die Quelldatei heruntergeladen
   * bzw, exportiert wurde (d.h. falls noch nicht geschehen wird
   * {@link #storeIfNecessary()} aufgerufen). ACHTUNG! Der Aufruf erfolgt immer in
   * einem eigenen Thread. Diese Methode kehrt also sofort zurück.
   * 
   * Hinweis: Es macht einen Unterschied ob man {@link #storeIfNecessary()} vor dem
   * Aufruf dieser Methode explizit aufruft oder nicht. Ruft man
   * {@link #storeIfNecessary()} nicht explizit auf, erfolgt
   * {@link #storeIfNecessary()} ebenfalls im neuen Thread. Man kann Speicherprobleme
   * dann nicht mehr von Programmaufrufproblemen unterscheiden.
   * 
   * ACHTUNG! Im Fall, dass /loadComponentFromURL/ bei den zu versuchenden Programmen ({@link #getPrograms()})
   * dabei ist, geht diese Methode davon aus, dass eine funktionierende
   * OOo-Verbindung über {@link UNO} besteht.
   * 
   * @param handler
   *          wird im Falle einer Exception im von launch gestarteten Thread
   *          aufgerufen. Ausnahme ist die IllegalStateException im Falle dass
   *          setSource nicht aufgerufen wurde. Diese fliegt ganz normal aus der
   *          Methode raus.
   * 
   * @throws IllegalStateException
   *           falls diese Methode aufgerufen wurde ohne dass vorher mit setSource()
   *           eine Quelle festgelegt wurde.
   * 
   * TESTED
   */
  public void launch(final ExceptionHandler handler) throws IllegalStateException
  {
    testState();

    Thread t = new Thread()
    {
      @Override
      public void run()
      {
        try
        {
          storeIfNecessary();
        }
        catch (Exception x)
        {
          handler.handle(x);
          return;
        }

        String appArgument;
        if (download)
          appArgument = destFile.getAbsolutePath();
        else
        {
          if (url != null)
            appArgument = url.toString();
          else
            try
            {
              appArgument = destFile.toURI().toURL().toString();
            }
            catch (MalformedURLException x)
            {
              appArgument = "file:" + destFile.getAbsolutePath();
            }
        }

        StringBuilder errors = new StringBuilder();
        Iterator<String> iter = programs.iterator();
        while (iter.hasNext())
        {
          String command = iter.next();

          if (command.startsWith("/loadComponentFromURL/") && command.endsWith("/"))
          {
            if (loadComponentFromURL(command, appArgument, errors)) return;
          }
          else
          {
            if (runProgram(command, appArgument, pipe, errors)) return;
          }
        }

        handler.handle(new Exception(
          L.m(
            "Keines der für die Erweiterung \"%1\"konfigurierten Programme konnte gestartet werden!\n%2",
            ext, errors.toString())));
      }

    };

    t.setDaemon(false);
    t.start();
  }

  /**
   * Versucht, appArgument mittels
   * {@link UNO#loadComponentFromURL(String, boolean, short, boolean)} zu laden,
   * wobei die Parameter aus command extrahiert werden. Eine OOo-Verbindung mittels
   * {@link UNO} muss bereits bestehen. command hat folgende Form
   * 
   * <pre>
   *  /loadComponentFromURL/AsTemplate=true/MacroExecutionMode=3/Hidden=false/
   * </pre>
   * 
   * wobei die Reihenfolge der Parameter beliebig ist und nicht alle angegeben werden
   * müssen. Obiges Beispiel zeigt die Default-Werte. Das folgende command ist also
   * äquivalent
   * 
   * <pre>
   * /loadComponentFromURL/
   * </pre>
   * 
   * @param appArgument
   *          die URL der zu ladenden Datei
   * 
   * @param errors
   *          Fehler beim Laden der Datei werden hier angehängt.
   * 
   * @return true wenn die Datei geladen werden konnte.
   */
  private static boolean loadComponentFromURL(String command, String appArgument,
      StringBuilder errors)
  {
    boolean asTemplate = true;
    short macroExecutionMode = 3;
    boolean hidden = false;

    try
    {
      String[] param = command.split("/");
      if (param[0].length() > 0 || !param[1].equals("loadComponentFromURL"))
        throw new IllegalArgumentException();

      for (int i = 2; i < param.length; ++i)
      {
        String arg[] = param[i].split("=");
        if (arg.length != 2)
          throw new IllegalArgumentException(
            L.m("/loadComponentFromURL/ Parameter muss die Form \"Param=Wert\" haben"));

        if (arg[0].equals("AsTemplate"))
          asTemplate = arg[1].equalsIgnoreCase("true");
        else if (arg[0].equals("Hidden"))
          hidden = arg[1].equalsIgnoreCase("true");
        else if (arg[0].equals("MacroExecutionMode"))
          macroExecutionMode = Short.valueOf(arg[1]);
      }

      return (null != UNO.loadComponentFromURL(
        UNO.getParsedUNOUrl(appArgument).Complete, asTemplate, macroExecutionMode,
        hidden));
    }
    catch (Exception x)
    {
      errors.append(x.toString());
      errors.append('\n');
      return false;
    }
  }

  /**
   * Versucht, einen Prozess zu starten zur Ausführung von command mit
   * Kommandozeilenargument appArgument.
   * 
   * @param pipe
   *          falls true leert diese Methode in einer Endlosschleife stdout und
   *          stderr des gestarteten Prozesses. Falls false werden die beiden einfach
   *          geschlossen. stdin wird immer geschlossen.
   * 
   * @param errors
   *          Fehler beim Ausführen des Prozesses werden hier angehängt.
   * 
   * @return true wenn der Prozess gestartet werden konnte.
   */
  private static boolean runProgram(String command, String appArgument,
      boolean pipe, StringBuilder errors)
  {
    ProcessBuilder proc = new ProcessBuilder(new String[] {
      command, appArgument });
    proc.redirectErrorStream(true);
    try
    {
      Process process = proc.start();
      // Prozess daran hindern zu blocken durch Eingabe
      process.getOutputStream().close();

      /*
       * Wenn der gestartete Prozess Ein- oder Ausgabe tätigt, so wird er blocken,
       * wenn an der anderen Seite nichts hängt das schreibt oder liest. Am liebsten
       * würden wir natürlich nach /dev/null umleiten, aber das kann Java nicht (vor
       * allem nicht portabel). Für Stdin ist die Lösung einfach. Man schließt den
       * Strom. Damit muss jedes Programm zurecht kommen. Für Stdout/Stderr (oben
       * über redirectErrorStream zusammengelegt) kann man das zwar auch machen (und
       * das tut der unten stehende Code auch), aber das ist etwas böse, weil
       * Programme zumindest unter Unix für gewöhnlich nicht dafür ausgelegt sind,
       * kein Stdout+Stderr zu haben. Falls ein Programm damit Probleme hat, kann ein
       * einfaches Shell-Skript als Wrapper verwendet werden, das die Umleitung nach
       * /dev/null erledigt.
       * 
       * Eine alternative Lösung ist der durch pipe==true angetriggerte Code, der
       * einfach Stdout+Stderr ausliest. Unschön an dieser Lösung ist, dass der
       * Java-Thread weiterläuft solange wie das externe Programm läuft.
       */
      if (pipe == false)
      {
        process.getInputStream().close(); // böse
        process.getErrorStream().close(); // böse
      }
      else
      {
        InputStream istream = process.getInputStream();
        byte[] buffy = new byte[256];
        int count;
        while ((0 <= (count = istream.read(buffy))))
        {
          Logger.log(new String(buffy, 0, count));
        }
      }
    }
    catch (Exception x)
    {
      errors.append(x.toString());
      errors.append('\n');
      return false;
    }
    return true;
  }

}
