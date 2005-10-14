/*
 * Dateiname: WMCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Scannt alle Bookmarks eines Dokuments und interpretiert ggf. die 
 *            WM-Kommandos.
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
package de.muenchen.allg.itd51.wollmux;

import java.io.File;
import java.net.URL;

import com.sun.star.container.XNameAccess;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

public class WMCommandInterpreter {

	public WMCommandInterpreter(XTextDocument xDoc) {
		// Das ist noch eine Baustelle!
		UnoService doc = new UnoService(xDoc);
		XNameAccess bookmarkAccess = doc.xBookmarksSupplier().getBookmarks();
		String[] bookmarks = bookmarkAccess.getElementNames();
		for (int i = 0; i < bookmarks.length; i++) {
			Logger.debug2("Found Bookmark: " + bookmarks[i]);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				System.out.println("USAGE: <config_url> <document_url>");
				System.exit(0);
			}
			File cwd = new File(".");

			args[0] = args[0].replaceAll("\\\\", "/");
			args[1] = args[1].replaceAll("\\\\", "/");

			// Remote-Kontext herstellen
			UNO.init();

			// WollMux starten
			new WollMux(UNO.defaultContext);
			WollMux.initialize(System.err, new File(cwd, args[0]));
			WollMux.getInstance().startupWollMux();

			Logger.init(Logger.ALL);

			// Dokument zum Parsen Öffnen
			URL url = new URL(cwd.toURL(), args[1]);
			String urlStr = url.toExternalForm();
			if (url.getHost() == null || url.getHost().equals(""))
				urlStr = urlStr.replaceFirst(":/", ":///");
			UNO.loadComponentFromURL(urlStr, true, false);

			new WMCommandInterpreter(UNO.XTextDocument(UNO.compo));
		} catch (Exception e) {
			Logger.error(e);
		}
		System.exit(0);
	}
}
