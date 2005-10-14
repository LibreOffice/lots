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
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 */

package de.muenchen.allg.itd51.wollmux;

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
 * Verfügung: critical(), log(), debug(), debug2()
 * </p>
 * <p>
 * Der Logging-Modus kann über die init()-Methode initialisiert werden. Er
 * beschreibt, welche Nachrichten aus den Prioritätsstufen angezeigt werden und
 * welche nicht. Jeder Logging Modus zeigt die Nachrichten seiner Priorität und
 * die Nachrichten der höheren Prioritätsstufen an. Standardmässig ist der Modus
 * Logging.ERROR voreingestellt.
 * </p>
 */
public class Logger {

	/**
	 * Der PrintStream, auf den die Nachrichten geschrieben werden.
	 */
	private static PrintStream out = System.err;

	/**
	 * Im Logging-Modus <code>NONE</code> werden keine Nachrichten ausgegeben.
	 * Dieser Modus ist die Defaulteinstellung.
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
	 * Nachrichten und wichtige Programminformationen an, die im täglichen
	 * Einsatz interessant sind.
	 */
	public static final int LOG = 3;

	/**
	 * Der Logging-Modus <code>DEBUG</code> wird genutzt, um detaillierte
	 * Informationen über den Programmablauf auszugeben. Er ist vor allem für
	 * DEBUG-Zwecke geeignet.
	 */
	public static final int DEBUG = 5;

	/**
	 * Der Logging-Modus <code>ALL</code> gibt uneingeschränkt alle
	 * Nachrichten aus. Er enthält auch Nachrichten der Priorität debug2, die
	 * sehr detaillierte Informationen ausgibt, die selbst für normale
	 * DEBUG-Zwecke zu genau sind.
	 */
	public static final int ALL = 7;

	/**
	 * Das Feld <code>mode</code> enthält den aktuellen Logging-Mode
	 */
	private static int mode = ERROR;

	/**
	 * Über die Methode init wird der Logger mit einem PrintStream und einem
	 * Logging-Modus initialisiert. Ohne diese Methode schreibt der Logger auf
	 * System.err im Modus LOG.
	 * 
	 * @param loggingMode
	 *            Der neue Logging-Modus kann über die statischen Felder
	 *            Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
	 */
	public static void init(PrintStream outputPrintStream, int loggingMode) {
		out = outputPrintStream;
		mode = loggingMode;
		Logger.debug2("Logger::init(): LoggingMode = " + mode);
	}

	/**
	 * Über die Methode init wird der Logger in dem Logging-Modus loggingMode
	 * initialisiert. Ohne diese Methode schreibt der Logger auf System.err im
	 * Modus LOG.
	 * 
	 * @param loggingMode
	 *            Der neue Logging-Modus kann über die statischen Felder
	 *            Logger.MODUS (z. B. Logger.DEBUG) angegeben werden.
	 */
	public static void init(int loggingMode) {
		mode = loggingMode;
		Logger.debug2("Logger::init(): LoggingMode = " + mode);
	}

	/**
	 * Nachricht der höchsten Priorität "critical" absetzen. Als "critical" sind
	 * nur Ereignisse einzustufen, die den Programmablauf unvorhergesehen
	 * verändern oder die weitere Ausführung unmöglich machen.
	 * 
	 * @param msg
	 *            Die Logging-Nachricht
	 */
	public static void error(String msg) {
		if (mode >= ERROR)
			println("ERROR: " + msg);
	}

	/**
	 * Wie {@link #critical(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void error(Exception e) {
		if (mode >= ERROR)
			printException("ERROR: ", e);
	}

	/**
	 * Nachricht der Priorität "log" absetzen. "log" enthält alle Nachrichten,
	 * die für den täglichen Programmablauf beim Endanwender oder zur Auffindung
	 * der gängigsten Bedienfehler interessant sind.
	 * 
	 * @param msg
	 *            Die Logging-Nachricht
	 */
	public static void log(String msg) {
		if (mode >= LOG)
			println("LOG: " + msg);
	}

	/**
	 * Wie {@link #log(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void log(Exception e) {
		if (mode >= LOG)
			printException("LOG: ", e);
	}

	/**
	 * Nachricht der Priorität "debug" absetzen. Die debug-Priorität dient zu
	 * debugging Zwecken. Sie enthält Informationen, die für Programmentwickler
	 * interessant sind.
	 * 
	 * @param msg
	 *            Die Logging-Nachricht
	 */
	public static void debug(String msg) {
		if (mode >= DEBUG)
			println("DEBUG: " + msg);
	}

	/**
	 * Wie {@link #debug(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void debug(Exception e) {
		if (mode >= DEBUG)
			printException("DEBUG: ", e);
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
	 *            Die Logging-Nachricht.
	 */
	public static void debug2(String msg) {
		if (mode >= ALL)
			println("DEBUG2: " + msg);
	}

	/**
	 * Wie {@link #debug2(String)}, nur dass statt dem String eine Exception
	 * ausgegeben wird.
	 * 
	 * @param e
	 */
	public static void debug2(Exception e) {
		if (mode >= ALL)
			printException("DEBUG2: ", e);
	}

	/**
	 * Gebe den String s auf dem PrintStream aus.
	 * 
	 * @param s
	 */
	private static void println(String s) {
		if (out != null) {
			out.println(new Date() + " " + s);
		}
	}

	/**
	 * Gebe die Exception e samt StackTrace auf dem PrintStream aus.
	 * 
	 * @param s
	 */
	private static void printException(String prefix, Exception e) {
		prefix = new Date() + " " + prefix;
		if (out != null) {
			out.println(prefix + e.toString());
			StackTraceElement[] se = e.getStackTrace();
			for (int i = 0; i < se.length; i++) {
				out.println(prefix + se[i].toString());
			}
		}
	}
}