/*
 * Dateiname: Logger.java
 * Projekt  : WollMux
 * Funktion : Logging-Mechanismus zum Schreiben von Nachrichten auf eine PrintStream.
 * 
 * Copyright: Landeshauptstadt München
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
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

/**
 * <p>
 * Der Logger ist ein simpler Logging Mechanismus, der im Programmablauf
 * auftretende Nachrichten verschiedener Prioritäten entgegennimmt und die
 * Nachrichten entsprechend der Einstellung des Logging-Modus auf einem
 * PrintStream ausgibt (Standardeinstellung: System.err). Die
 * Logging-Nachrichten werden über unterschiedliche Methodenaufrufe entsprechend
 * der Logging-Priorität abgesetzt. Folgende Methoden stehen dafür zur
 * Verfügung: error(), log(), debug(), debug2()
 * </p>
 * <p>
 * Der Logging-Modus kann über die init()-Methode initialisiert werden. Er
 * beschreibt, welche Nachrichten aus den Prioritätsstufen angezeigt werden und
 * welche nicht. Jeder Logging Modus zeigt die Nachrichten seiner Priorität und
 * die Nachrichten der höheren Prioritätsstufen an. Standardmässig ist der Modus
 * Logging.LOG voreingestellt.
 * </p>
 */
public class Logger
{

  /**
   * Der PrintStream, auf den die Nachrichten geschrieben werden.
   */
  private static PrintStream defaultOutputStream = System.err;

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
   * Prioritätsstufe "ERROR" an. ERROR enthält Nachrichten, die den
   * Programmablauf beeinflussen - z.B. Fehlermeldungen und Exceptions.
   */
  public static final int ERROR = 1;

  /**
   * Der Logging-Modus <code>LOG</code> ist der Standard Modus. Er zeigt
   * Nachrichten und wichtige Programminformationen an, die im täglichen Einsatz
   * interessant sind. Dieser Modus ist die Defaulteinstellung.
   */
  public static final int LOG = 3;

  /**
   * Der Logging-Modus <code>DEBUG</code> wird genutzt, um detaillierte
   * Informationen über den Programmablauf auszugeben. Er ist vor allem für
   * DEBUG-Zwecke geeignet.
   */
  public static final int DEBUG = 5;

  /**
   * Der Logging-Modus <code>ALL</code> gibt uneingeschränkt alle Nachrichten
   * aus. Er enthält auch Nachrichten der Priorität debug2, die sehr
   * detaillierte Informationen ausgibt, die selbst für normale DEBUG-Zwecke zu
   * genau sind.
   */
  public static final int ALL = 7;

  /**
   * Das Feld <code>mode</code> enthält den aktuellen Logging-Mode
   */
  private static int mode = LOG;

  /**
   * Über die Methode init wird der Logger mit einem PrintStream und einem
   * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
   * System.err im Modus LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   */
  public static void init(PrintStream outputPrintStream, int loggingMode)
  {
    defaultOutputStream = outputPrintStream;
    mode = loggingMode;
    Logger.debug2("Logger::init(): LoggingMode = " + mode);
  }

  /**
   * Über die Methode init wird der Logger mit einer Ausgabedatei und einem
   * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
   * System.err im Modus LOG.
   * 
   * @param outputFile
   *          Datei, in die die Ausgaben geschrieben werden.
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   * @throws FileNotFoundException
   */
  public static void init(File outputFile, int loggingMode)
      throws FileNotFoundException
  {
    file = outputFile;
    mode = loggingMode;
    Logger.debug2("Logger::init(): LoggingMode = " + mode);
    // prüfen, ob File geschrieben werden kann:
    new FileOutputStream(outputFile,true);
  }

  /**
   * Über die Methode init wird der Logger in dem Logging-Modus loggingMode
   * initialisiert. Ohne diese Methode schreibt der Logger auf System.err im
   * Modus LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   */
  public static void init(int loggingMode)
  {
    mode = loggingMode;
    Logger.debug2("Logger::init(): LoggingMode = " + mode);
  }

  /**
   * Über die Methode init wird der Logger in dem Logging-Modus loggingMode
   * initialisiert, der in Form eines den obigen Konstanten-Namen
   * übereinstimmenden Strings vorliegt. Ohne diese Methode schreibt der Logger
   * auf System.err im Modus LOG.
   * 
   * @param loggingMode
   *          Der neue Logging-Modus kann über die statischen Felder
   *          Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
   */
  public static void init(String loggingMode)
  {
    if (loggingMode.compareToIgnoreCase("NONE") == 0) init(NONE);
    if (loggingMode.compareToIgnoreCase("ERROR") == 0) init(ERROR);
    if (loggingMode.compareToIgnoreCase("LOG") == 0) init(LOG);
    if (loggingMode.compareToIgnoreCase("DEBUG") == 0) init(DEBUG);
    if (loggingMode.compareToIgnoreCase("ALL") == 0) init(ALL);
  }

  /**
   * Nachricht der höchsten Priorität "error" absetzen. Als "error" sind nur
   * Ereignisse einzustufen, die den Programmablauf unvorhergesehen verändern
   * oder die weitere Ausführung unmöglich machen.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void error(String msg)
  {
    if (mode >= ERROR) println("ERROR: " + msg + System.getProperty("line.separator"));
  }

  /**
   * Wie {@link #error(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void error(Throwable e)
  {
    if (mode >= ERROR) printException("ERROR: ", e);
  }

  /**
   * Wie {@link #error(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void error(String msg, Exception e)
  {
    if (mode >= ERROR)
    {
      println("ERROR: " + msg);
      printException("ERROR: ", e);
    }
  }

  /**
   * Nachricht der Priorität "log" absetzen. "log" enthält alle Nachrichten, die
   * für den täglichen Programmablauf beim Endanwender oder zur Auffindung der
   * gängigsten Bedienfehler interessant sind.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void log(String msg)
  {
    if (mode >= LOG) println("LOG: " + msg + System.getProperty("line.separator"));
  }

  /**
   * Wie {@link #log(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void log(Throwable e)
  {
    if (mode >= LOG) printException("LOG: ", e);
  }

  /**
   * Nachricht der Priorität "debug" absetzen. Die debug-Priorität dient zu
   * debugging Zwecken. Sie enthält Informationen, die für Programmentwickler
   * interessant sind.
   * 
   * @param msg
   *          Die Logging-Nachricht
   */
  public static void debug(String msg)
  {
    if (mode >= DEBUG) println("DEBUG: " + msg + System.getProperty("line.separator"));
  }

  /**
   * Wie {@link #debug(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void debug(Throwable e)
  {
    if (mode >= DEBUG) printException("DEBUG: ", e);
  }

  /**
   * Nachricht der geringsten Priorität "debug2" absetzen. Das sind Meldungen,
   * die im Normalfall selbst für debugging-Zwecke zu detailliert sind.
   * Beispielsweise Logging-Meldungen von privaten Unterfunktionen, die die
   * Ausgabe nur unnötig unübersichtlich machen, aber nicht zum schnellen
   * Auffinden von Standard-Fehlern geeignet sind. "debug2" ist geeignet, um
   * ganz spezielle Fehler ausfindig zu machen.
   * 
   * @param msg
   *          Die Logging-Nachricht.
   */
  public static void debug2(String msg)
  {
    if (mode >= ALL) println("DEBUG2: " + msg + System.getProperty("line.separator"));
  }

  /**
   * Wie {@link #debug2(String)}, nur dass statt dem String eine Exception
   * ausgegeben wird.
   * 
   * @param e
   */
  public static void debug2(Throwable e)
  {
    if (mode >= ALL) printException("DEBUG2: ", e);
  }

  /**
   * Gebe den String s auf dem PrintStream aus.
   * 
   * @param s
   */
  private static void println(String s)
  {
    // Ausgabestream oeffnen bzw. festlegen:
    PrintStream out;
    FileOutputStream fileOut = null;
    if (file != null)
      try
      {
        fileOut = new FileOutputStream(file, true);
        out = new PrintStream(fileOut);
      }
      catch (FileNotFoundException x)
      {
        out = Logger.defaultOutputStream;
      }
    else
    {
      out = Logger.defaultOutputStream;
    }

    // Ausgabe schreiben:
    out.println(new Date() + " " + s);
    out.flush();

    // Ein File wird nach dem Schreiben geschlossen.
    if (fileOut != null)
    {
      try
      {
        out.close();
        fileOut.close();
        out = null;
        fileOut = null;
        System.gc();
      }
      catch (IOException e)
      {
      }
    }
  }

  /**
   * Gebe die Exception e samt StackTrace auf dem PrintStream aus.
   * 
   * @param s
   */
  private static void printException(String prefix, Throwable e)
  {
    prefix = new Date() + " " + prefix;

    // Ausgabestream oeffnen bzw. festlegen:
    PrintStream out;
    FileOutputStream fileOut = null;
    if (file != null)
      try
      {
        fileOut = new FileOutputStream(file, true);
        out = new PrintStream(fileOut);
      }
      catch (FileNotFoundException x)
      {
        out = Logger.defaultOutputStream;
      }
    else
    {
      out = Logger.defaultOutputStream;
    }

    // Ausgabe schreiben:
    out.println(prefix + e.toString());
    StackTraceElement[] se = e.getStackTrace();
    for (int i = 0; i < se.length; i++)
    {
      out.println(prefix + se[i].toString());
    }
    out.println();
    out.flush();

    // Ein File wird nach der Ausgabe geschlossen:
    if (fileOut != null)
    {
      try
      {
        out.close();
        fileOut.close();
        out = null;
        fileOut = null;
        System.gc();
      }
      catch (IOException e1)
      {
      }
    }
  }
}
