/*
 * Dateiname: WollMux.java
 * Projekt  : WollMux
 * Funktion : UNO-Service WollMux; Singleton und zentrale WollMux-Instanz.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.comp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.sun.star.beans.NamedValue;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XAsyncJob;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.VisibleTextFragmentList;

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service dient
 * als Einstiegspunkt des WollMux und initialisiert alle benötigten
 * Programmmodule. Der Service ist als Singleton implementiert über den der
 * Zugriff auf alle Programmmodule über entsprechende get-Methoden gewährleistet
 * wird.
 */
public class WollMux extends WeakBase implements XServiceInfo, XAsyncJob {

	/**
	 * Die Instanz des Singleton.
	 */
	private static WollMux myInstance;

	/**
	 * Enthält einen PrintStream in den die Log-Nachrichten geschrieben werden.
	 */
	private static PrintStream wollmuxLog;

	/**
	 * Enthält das File der Konfigurationsdatei wollmux.conf
	 */
	private static File wollmuxConfFile;

	/**
	 * Enthält den geparsten Konfigruationsbaum der wollmux.conf
	 */
	private static ConfigThingy wollmuxConf;

	/**
	 * Enthält die geparste Textfragmentliste, die in der wollmux.conf definiert
	 * wurde.
	 */
	private static VisibleTextFragmentList textFragmentList;

	/**
	 * Der XComponentContext in dem der WollMux läuft.
	 */
	private static XComponentContext xComponentContext;

	/**
	 * Dieses Feld entält eine Liste aller Services, die dieser UNO-Service
	 * implementiert.
	 */
	public static final java.lang.String[] SERVICENAMES = { "com.sun.star.task.AsyncJob" };

	/**
	 * Dieses Feld enthält den vollständigen Namen der Serviceimplementierung.
	 * Dieser ist üblicherweise WollMux.class.getName(), in den Beispieldateien
	 * aus dem DevGuide wird der Name jedoch immer ausgeschrieben. Ich vermute
	 * dass das schon seinen Grund hat...
	 */
	public static final java.lang.String IMPLEMENTATIONNAME = "de.muenchen.allg.itd51.wollmux.comp.WollMux";

	/**
	 * Diesen Konstruktor bitte nur zur Erzeugung der ersten Singleton-Instanz
	 * benutzen. Er kann leider nicht private gemacht werden, da er für die
	 * Erzeugung des Jobs notwendig ist. Die Singleton-Instanz wird mit der
	 * Methode getInstance zurückgeliefert. Der Konstruktor erzeugt ein neues
	 * WollMux-Objekt im XComponentContext context.
	 * 
	 * @param context
	 */
	public WollMux(XComponentContext context) {
		xComponentContext = context;

		// Das hier sollte die einzige Stelle sein, an der Pfade hart
		// verdrahtet sind...
		String userHome = System.getProperty("user.home");
		File wollmuxDir = new File(userHome, ".wollmux");
		if (!wollmuxDir.exists())
			wollmuxDir.mkdirs();
		File wollmuxLogFile = new File(wollmuxDir, "wollmux.log");
		wollmuxConfFile = new File(wollmuxDir, "wollmux.conf");
		try {
			wollmuxLog = new PrintStream(new FileOutputStream(wollmuxLogFile));
		} catch (FileNotFoundException e) {
			// Da kann ich nicht viel machen, wenn noch nicht mal das
			// Logfile funktioniert...
		}
		myInstance = this;
	}

	/**
	 * Über dies Methode können die wichtigsten Startwerte des WollMux manuell
	 * gesetzt werden. Dies ist vor allem zum Testen der Anwendung im
	 * Remote-Betrieb notwendig.
	 * 
	 * @param logStream
	 *            Den Ausgabestream, in den die Logging Nachrichten geschrieben
	 *            werden sollen.
	 * @param wollmuxConf
	 *            die Wollmux-Konfigruationsdatei.
	 */
	public static void initialize(PrintStream logStream, File wollmuxConf) {
		WollMux.wollmuxLog = logStream;
		WollMux.wollmuxConfFile = wollmuxConf;
	}

	/**
	 * Diese Methode übernimmt den eigentlichen Bootstrap des WollMux.
	 */
	public void startupWollMux() {
		try {

			// Logger initialisieren und erste Meldung ausgeben:
			if (wollmuxLog != null)
				Logger.init(wollmuxLog, Logger.ALL);
			Logger.log("StartupWollMux");
			Logger.debug("wollmuxConfFile = " + wollmuxConfFile.toString());

			// Parsen der Konfigurationsdatei
			ConfigThingy conf = new ConfigThingy("wollmux.conf",
					wollmuxConfFile.toURL());

			// VisibleTextFragmentList erzeugen
			textFragmentList = new VisibleTextFragmentList(conf);
		} catch (Exception e) {
			Logger.error(e);
		}

	}

	/**
	 * @return Returns the textFragmentList.
	 */
	public static VisibleTextFragmentList getTextFragmentList() {
		return textFragmentList;
	}

	/**
	 * @return Returns the wollmuxConf.
	 */
	public static ConfigThingy getWollmuxConf() {
		return wollmuxConf;
	}

	/**
	 * @return Returns the xComponentContext.
	 */
	public static XComponentContext getXComponentContext() {
		return xComponentContext;
	}

	/*
	 * TODO: Diese Methode müsste vielleicht gar nicht unbedingt was tun. Man
	 * kann das startupWollMux ja auch in die Konstruktoren auslagern...
	 * vielleicht mache ich das noch einmal.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see com.sun.star.task.XAsyncJob#executeAsync(com.sun.star.beans.NamedValue[],
	 *      com.sun.star.task.XJobListener)
	 */
	public synchronized void executeAsync(
			com.sun.star.beans.NamedValue[] lArgs,
			com.sun.star.task.XJobListener xListener)
			throws com.sun.star.lang.IllegalArgumentException {
		if (xListener == null)
			throw new com.sun.star.lang.IllegalArgumentException(
					"invalid listener");

		com.sun.star.beans.NamedValue[] lEnvironment = null;

		// Hole das Environment-Argument
		for (int i = 0; i < lArgs.length; ++i) {
			if (lArgs[i].Name.equals("Environment")) {
				lEnvironment = (com.sun.star.beans.NamedValue[]) com.sun.star.uno.AnyConverter
						.toArray(lArgs[i].Value);
			}
		}
		if (lEnvironment == null)
			throw new com.sun.star.lang.IllegalArgumentException(
					"no environment");

		// Hole Event-Informationen
		String sEnvType = null;
		String sEventName = null;
		for (int i = 0; i < lEnvironment.length; ++i) {
			if (lEnvironment[i].Name.equals("EnvType"))
				sEnvType = com.sun.star.uno.AnyConverter
						.toString(lEnvironment[i].Value);
			else if (lEnvironment[i].Name.equals("EventName"))
				sEventName = com.sun.star.uno.AnyConverter
						.toString(lEnvironment[i].Value);
		}

		// Prüfe die property "EnvType":
		if ((sEnvType == null)
				|| ((!sEnvType.equals("EXECUTOR")) && (!sEnvType
						.equals("DISPATCH")))) {
			java.lang.String sMessage = "\"" + sEnvType
					+ "\" isn't a valid value for EnvType";
			throw new com.sun.star.lang.IllegalArgumentException(sMessage);
		}

		/***********************************************************************
		 * Starte den WollMux!
		 */
		if (sEventName.equals("onFirstVisibleTask"))
			startupWollMux();
		/** *************************************************** */

		xListener.jobFinished(this, new NamedValue[] {});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
	 */
	public String[] getSupportedServiceNames() {
		return SERVICENAMES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
	 */
	public boolean supportsService(String sService) {
		int len = SERVICENAMES.length;
		for (int i = 0; i < len; i++) {
			if (sService.equals(SERVICENAMES[i]))
				return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.star.lang.XServiceInfo#getImplementationName()
	 */
	public String getImplementationName() {
		return (WollMux.class.getName());
	}

	/**
	 * Diese Methode liefert eine Factory zurück, die in der Lage ist den
	 * UNO-Service zu erzeugen. Die Methode wird von UNO intern benötigt. Die
	 * Methoden __getComponentFactory und __writeRegistryServiceInfo stellen das
	 * Herzstück des UNO-Service dar.
	 * 
	 * @param sImplName
	 * @return
	 */
	public synchronized static XSingleComponentFactory __getComponentFactory(
			java.lang.String sImplName) {
		com.sun.star.lang.XSingleComponentFactory xFactory = null;
		if (sImplName.equals(WollMux.IMPLEMENTATIONNAME))
			xFactory = Factory.createComponentFactory(WollMux.class,
					SERVICENAMES);
		return xFactory;
	}

	/**
	 * Diese Methode registriert den UNO-Service. Sie wird z.B. beim unopkg-add
	 * im Hintergrund aufgerufen. Die Methoden __getComponentFactory und
	 * __writeRegistryServiceInfo stellen das Herzstück des UNO-Service dar.
	 * 
	 * @param xRegKey
	 * @return
	 */
	public synchronized static boolean __writeRegistryServiceInfo(
			XRegistryKey xRegKey) {
		return Factory.writeRegistryServiceInfo(WollMux.IMPLEMENTATIONNAME,
				WollMux.SERVICENAMES, xRegKey);
	}

	/**
	 * @return Returns the myInstance.
	 */
	public static WollMux getInstance() {
		return myInstance;
	}
}
