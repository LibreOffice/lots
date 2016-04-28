/*
 * Dateiname: Logger.java
 * Projekt  : WollMux
 * Funktion : Logging-Mechanismus zum Schreiben von Nachrichten auf eine PrintStream.
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 14.10.2005 | BNK | Kommentar korrigiert: Standard ist LOG nicht NONE
 *                  | System.err als Standardausgabestrom
 * 14.10.2005 | LUT | critical(*) --> error(*)
 *                    + Anzeige des Datums bei allen Meldungen.
 * 27.10.2005 | BNK | Leerzeile nach jeder Logmeldung                  
 * 31.10.2005 | BNK | +error(msg, e)
 *                  | "critical" -> "error"
 * 02.11.2005 | BNK | LOG aus Default-Modus
 * 24.11.2005 | BNK | In init() das Logfile nicht löschen.
 * 05.12.2005 | BNK | line.separator statt \n
 * 06.12.2005 | BNK | bessere Separatoren, kein Test mehr in init, ob Logfile schreibbar
 * 20.04.2006 | BNK | bessere Datum/Zeitangabe, Angabe des Aufrufers
 * 24.04.2006 | BNK | korrekte Monatsangabe.
 * 15.05.2006 | BNK | Cause ausgeben in printException()
 * 16.05.2006 | BNK | println() und printException() vereinheitlicht
 * 30.05.2006 | BNK | bei init(PrintStream,...) den file zurücksetzen, damit
 *                  | die Zuweisung auch wirksam wird.
 * 18.06.2010 | BED | Sekundenausgabe hinzugefügt
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 */

package de.muenchen.allg.itd51.wollmux.core.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;

/**
 * <p>
 * Der Logger ist ein simpler Logging Mechanismus, der im Programmablauf auftretende
 * Nachrichten verschiedener Prioritäten entgegennimmt und die Nachrichten
 * entsprechend der Einstellung des Logging-Modus auf einem PrintStream ausgibt
 * (Standardeinstellung: System.err). Die Logging-Nachrichten werden über
 * unterschiedliche Methodenaufrufe entsprechend der Logging-Priorität abgesetzt.
 * Folgende Methoden stehen dafür zur Verfügung: error(), log(), debug(), debug2()
 * </p>
 * <p>
 * Der Logging-Modus kann über die init()-Methode initialisiert werden. Er
 * beschreibt, welche Nachrichten aus den Prioritätsstufen angezeigt werden und
 * welche nicht. Jeder Logging Modus zeigt die Nachrichten seiner Priorität und die
 * Nachrichten der höheren Prioritätsstufen an. Standardmässig ist der Modus
 * Logging.LOG voreingestellt.
 * </p>
 */
public class Logger
{

  /**
   * Der PrintStream, auf den die Nachrichten geschrieben werden.
   */
  private static PrintStream outputStream = System.err;

  /**
   * optional: Datei, aus der der PrintStream erzeugt wird.
   */
  private static File file = null;

  /**
   * Im Logging-Modus <code>NONE</code> werden keine Nachrichten ausgegeben.
   */
  public static final int NONE = 0;

  /**
   * Der Logging-Modus <code>ERROR</code> zeigt Nachrichten der höchsten
   * Prioritätsstufe "ERROR" an. ERROR enthält Nachrichten, die den Programmablauf
   * beeinflussen - z.B. Fehlermeldungen und Exceptions.
   */
  public static final int ERROR = 1;

  /**
   * Der Logging-Modus <code>LOG</code> ist der Standard Modus. Er zeigt Nachrichten
   * und wichtige Programminformationen an, die im täglichen Einsatz interessant
   * sind. Dieser Modus ist die Defaulteinstellung.
   */
  public static final int LOG = 3;

  /**
   * Der Logging-Modus <code>DEBUG</code> wird genutzt, um detaillierte Informationen
   * über den Programmablauf auszugeben. Er ist vor allem für DEBUG-Zwecke geeignet.
   */
  public static final int DEBUG = 5;

  /**
   * Der Logging-Modus <code>ALL</code> gibt uneingeschränkt alle Nachrichten aus. Er
   * enthält auch Nachrichten der Priorität debug2, die sehr detaillierte
   * Informationen ausgibt, die selbst für normale DEBUG-Zwecke zu genau sind.
   */
  public static final int ALL = 7;

  /**
   * Das Feld <code>mode</code> enthält den aktuellen Logging-Mode
   */
  private static int mode = LOG;

  /**
   * Wenn ignoreInit==true, wird der nächte init-Aufruf ignoriert.
   */
  private static boolean ignoreInit = false;
  
  /**
   * Über die Methode init wird der Logger mit einem PrintStream und einem
   * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
   * System.err im Modus LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder Logger.MODUS (z.
   *          B. Logger.DEBUG) angegeben werden.
   */
  public static void init(PrintStream outputPrintStream, int loggingMode)
  {
    if (ignoreInit) return;
    
    outputStream = outputPrintStream;
    file = null; // evtl. vorher erfolgte Zuweisung aufheben, damit outputStream
    // auch wirklich verwendet wird
    mode = loggingMode;
    Logger.debug2("========================== Logger::init(): LoggingMode = " + mode
      + " ========================");
  }

  /**
   * Über die Methode init wird der Logger mit einer Ausgabedatei und einem
   * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
   * System.err im Modus LOG.
   * 
   * @param outputFile
   *          Datei, in die die Ausgaben geschrieben werden.
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder Logger.MODUS (z.
   *          B. Logger.DEBUG) angegeben werden.
   * @throws FileNotFoundException
   */
  public static void init(File outputFile, int loggingMode)
  {
    if (ignoreInit) return;

    file = outputFile;
    mode = loggingMode;
    Logger.debug2("========================== Logger::init(): LoggingMode = " + mode
      + " ========================");
  }

  /**
   * Über die Methode init wird der Logger in dem Logging-Modus loggingMode
   * initialisiert. Ohne diese Methode schreibt der Logger auf System.err im Modus
   * LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder Logger.MODUS (z.
   *          B. Logger.DEBUG) angegeben werden.
   */
  public static void init(int loggingMode)
  {
    if (ignoreInit) return;

    mode = loggingMode;
    Logger.debug2("========================== Logger::init(): LoggingMode = " + mode
      + " ========================");
  }

  /**
   * Über die Methode init wird der Logger in dem Logging-Modus loggingMode
   * initialisiert, der in Form eines den obigen Konstanten-Namen übereinstimmenden
   * Strings vorliegt. Ohne diese Methode schreibt der Logger auf System.err im Modus
   * LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder Logger.MODUS (z.
   *          B. Logger.DEBUG) angegeben werden.
   */
  public static void init(String loggingMode)
  {
    if (ignoreInit) return;

    if (loggingMode.compareToIgnoreCase("NONE") == 0) init(NONE);
    if (loggingMode.compareToIgnoreCase("ERROR") == 0) init(ERROR);
    if (loggingMode.compareToIgnoreCase("LOG") == 0) init(LOG);
    if (loggingMode.compareToIgnoreCase("DEBUG") == 0) init(DEBUG);
    if (loggingMode.compareToIgnoreCase("ALL") == 0) init(ALL);
  }

  /**
   * Nach einem Aufruf dieser Methode mit ignoreInit==true werden alle folgenden
   * init-Aufrufe ignoriert.
   */
  public static void setIgnoreInit(boolean ignoreInit)
  {
    Logger.ignoreInit = ignoreInit;
  }

  /**
   * Nachricht der höchsten Priorität "error" absetzen. Als "error" sind nur
   * Ereignisse einzustufen, die den Programmablauf unvorhergesehen verändern oder
   * die weitere Ausführung unmöglich machen.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void error(String msg)
  {
    if (mode >= ERROR) printInfo("ERROR(" + getCaller(2) + "): ", msg, null);
  }

  /**
   * Wie {@link #error(String)}, nur dass statt dem String eine Exception ausgegeben
   * wird.
   * 
   * @param e
   */
  public static void error(Throwable e)
  {
    if (mode >= ERROR) printInfo("ERROR(" + getCaller(2) + "): ", null, e);
  }

  /**
   * Wie {@link #error(String)}, nur dass statt dem String eine Exception ausgegeben
   * wird.
   * 
   * @param e
   */
  public static void error(String msg, Exception e)
  {
    if (mode >= ERROR) printInfo("ERROR(" + getCaller(2) + "): ", msg, e);
  }

  /**
   * Nachricht der Priorität "log" absetzen. "log" enthält alle Nachrichten, die für
   * den täglichen Programmablauf beim Endanwender oder zur Auffindung der gängigsten
   * Bedienfehler interessant sind.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void log(String msg)
  {
    if (mode >= LOG) printInfo("LOG(" + getCaller(2) + "): ", msg, null);
  }

  /**
   * Wie {@link #log(String)}, nur dass statt dem String eine Exception ausgegeben
   * wird.
   * 
   * @param e
   */
  public static void log(Throwable e)
  {
    if (mode >= LOG) printInfo("LOG(" + getCaller(2) + "): ", null, e);
  }

  /**
   * Nachricht der Priorität "debug" absetzen. Die debug-Priorität dient zu debugging
   * Zwecken. Sie enthält Informationen, die für Programmentwickler interessant sind.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void debug(String msg)
  {
    if (mode >= DEBUG) printInfo("DEBUG(" + getCaller(2) + "): ", msg, null);
  }

  /**
   * Wie {@link #debug(String)}, nur dass statt dem String eine Exception ausgegeben
   * wird.
   * 
   * @param e
   */
  public static void debug(Throwable e)
  {
    if (mode >= DEBUG) printInfo("DEBUG(" + getCaller(2) + "): ", null, e);
  }

  /**
   * Nachricht der geringsten Priorität "debug2" absetzen. Das sind Meldungen, die im
   * Normalfall selbst für debugging-Zwecke zu detailliert sind. Beispielsweise
   * Logging-Meldungen von privaten Unterfunktionen, die die Ausgabe nur unnötig
   * unübersichtlich machen, aber nicht zum schnellen Auffinden von Standard-Fehlern
   * geeignet sind. "debug2" ist geeignet, um ganz spezielle Fehler ausfindig zu
   * machen.
   * 
   * @param msg
   *          Die Logging-Nachricht.
   */
  public static void debug2(String msg)
  {
    if (mode >= ALL) printInfo("DEBUG2(" + getCaller(2) + "): ", msg, null);
  }

  /**
   * Wie {@link #debug2(String)}, nur dass statt dem String eine Exception ausgegeben
   * wird.
   * 
   * @param e
   */
  public static void debug2(Throwable e)
  {
    if (mode >= ALL) printInfo("DEBUG2(" + getCaller(2) + "): ", null, e);
  }

  /**
   * Gibt msg gefolgt vom Stacktrace von e aus, wobei jeder Zeile prefix
   * vorangestellt wird.
   */
  private static void printInfo(String prefix, String msg, Throwable e)
  {
    PrintStream out = null;
    PrintStream fileOut = null;
    try
    {
      if (file != null)
        try
        {
          fileOut = new PrintStream(new FileOutputStream(file, true));
          out = fileOut;
        }
        catch (FileNotFoundException x)
        {
          out = Logger.outputStream;
        }
      else
      {
        out = Logger.outputStream;
      }

      // Zeit und Datum holen und aufbereiten
      Calendar now = Calendar.getInstance();
      int day = now.get(Calendar.DAY_OF_MONTH);
      int month = now.get(Calendar.MONTH) + 1;
      int hour = now.get(Calendar.HOUR_OF_DAY);
      int minute = now.get(Calendar.MINUTE);
      int second = now.get(Calendar.SECOND);
      String dayStr = "" + day;
      String monthStr = "" + month;
      String hourStr = "" + hour;
      String minuteStr = "" + minute;
      String secondStr = "" + second;
      if (day < 10) dayStr = "0" + dayStr;
      if (month < 10) monthStr = "0" + monthStr;
      if (hour < 10) hourStr = "0" + hourStr;
      if (minute < 10) minuteStr = "0" + minuteStr;
      if (second < 10) secondStr = "0" + secondStr;
      prefix =
        "" + now.get(Calendar.YEAR) + "-" + monthStr + "-" + dayStr + " " + hourStr
          + ":" + minuteStr + ":" + secondStr + " " + prefix;

      // Ausgabe schreiben:
      if (msg != null)
      {
        out.print(prefix);
        out.println(msg);
      }

      while (e != null)
      {
        out.print(prefix);
        out.println(e.toString());
        StackTraceElement[] se = e.getStackTrace();
        for (int i = 0; i < se.length; i++)
        {
          out.print(prefix);
          out.println(se[i].toString());
        }

        e = e.getCause();
        if (e != null)
        {
          out.print(prefix);
          out.println("-------- CAUSED BY ------");
        }
      }
      out.println();
      out.flush();
    }
    finally
    {
      // Ein File wird nach der Ausgabe geschlossen:
      if (fileOut != null)
      {
        try
        {
          fileOut.close();
        }
        catch (Exception e1)
        {}
      }
    }
  }

  /**
   * Liefert Datei (ohne java Extension) und Zeilennummer des Elements level des
   * Stacks. Level 1 ist dabei die Funktion, die getCaller() aufruft.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static String getCaller(int level)
  {
    try
    {
      Throwable grosserWurf = new Throwable();
      grosserWurf.fillInStackTrace();
      StackTraceElement[] dickTracy = grosserWurf.getStackTrace();
      return dickTracy[level].getFileName().replaceAll("\\.java", "") + ":"
        + dickTracy[level].getLineNumber();
    }
    catch (Exception x)
    {
      return "Unknown:???";
    }
  }
}
