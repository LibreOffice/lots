/*
 * Dateiname: GlobalEventListener.java
 * Projekt  : WollMux
 * Funktion : Reagiert auf globale Ereignisse
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux über wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n     
 * 13.04.2006 | BNK | .wollmux/ Handling ausgegliedert in WollMuxFiles.
 * 20.04.2006 | LUT | Überarbeitung Code-Kommentare  
 * 20.04.2006 | BNK | DEFAULT_CONTEXT ausgegliedert nach WollMuxFiles
 * 21.04.2006 | LUT | + Robusteres Verhalten bei Fehlern während dem Einlesen 
 *                    von Konfigurationsdateien; 
 *                    + wohldefinierte Datenstrukturen
 *                    + Flag für EventProcessor: acceptEvents
 * 08.05.2006 | LUT | + isDebugMode()
 * 10.05.2006 | BNK | +parseGlobalFunctions()
 *                  | +parseFunctionDialogs()
 * 26.05.2006 | BNK | DJ initialisierung ausgelagert nacht WollMuxFiles
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 * 19.12.2006 | BAB | + setzen von Shortcuts im Konstruktor
 * 29.12.2006 | BNK | +registerDatasources()
 * 27.03.2007 | BNK | Default-oooEinstellungen ausgelagert nach data/...
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.event;

import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.DocumentManager;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Der GlobalEventListener sorgt dafür, dass der WollMux alle wichtigen globalen
 * Ereignisse wie z.B. ein OnNew on OnLoad abfangen und darauf reagieren kann. In
 * diesem Fall wird die Methode notifyEvent aufgerufen. Wichtig ist dabei, dass der
 * Verarbeitungsstatus für alle Dokumenttypen (auch nicht-Textdokumente) erfasst
 * wird, damit der WollMux auch für diese Komponenten onWollMuxProcessingFinished
 * liefern kann.
 * 
 * @author christoph.lutz
 */
public class GlobalEventListener implements com.sun.star.document.XEventListener
{
  private DocumentManager docManager;

  public GlobalEventListener(DocumentManager docManager)
  {
    this.docManager = docManager;
  }

  private boolean seenAlready(XComponent compo)
  {
    if (compo == null) return false;
    return docManager.getInfo(compo) != null;
  }

  /**
   * NICHT SYNCHRONIZED, weil es Deadlocks gibt zwischen getUrl() und der Zustellung
   * bestimmter Events (z.B. OnSave).
   */
  public void notifyEvent(com.sun.star.document.EventObject docEvent)
  {
    // Der try-catch-Block verhindert, daß die Funktion und damit der
    // ganze Listener ohne Fehlermeldung abstürzt.
    try
    {
      /*
       * Extra Test ohne Cast nach XComponent, weil queryInterface im Seriendruckfall
       * schon signifikanten Overhead erzeugt.
       */
      if (docEvent.Source == null) return;

      /*
       * Workaround for #3091: Die unsichtbaren Dokumente, die beim OOo-Seriendruck
       * anfallen nicht bearbeiten.
       */
      String event = docEvent.EventName;
      Logger.debug2(event);

      String url = "";
      XModel modelSource = UNO.XModel(docEvent.Source);
      if (modelSource != null)
        url = modelSource.getURL();
      else
        return;

      Logger.debug2(url);
      int idx = url.lastIndexOf('/') - 4;
      if (url.startsWith(".tmp/sv", idx) && url.endsWith(".tmp")) return;
      // --------------

      XComponent compo = UNO.XComponent(docEvent.Source);
      if (compo == null) return;
      XTextDocument xTextDoc = UNO.XTextDocument(compo);

      // Die Events OnLoad und OnNew kommen nur bei sichtbar geöffneten Dokumenten.
      // Das macht die Events für unsichtbar geöffnete Dokumente unbrauchbar. Nur
      // falls das Event OnViewCreated nicht empfangen wurden (z.B. möglicherweise
      // bei alten OOo-Verisonen), greift als Fallback die Dokumentbearbeitung über
      // OnNew bzw. OnLoad.
      // Im Gegensatz zu OnLoad oder OnNew wird das Event OnViewCreated auch bei
      // unsichtbar geöffneten Dokumenten erzeugt. Daher wird nun hauptsächlich
      // dieses Event zur Initiierung der Dokumentbearbeitung verwendet.
      // Es gibt Situationen (vorsynchronized allem beim Anlegen und Öffnen von
      // .odbs), wo
      // OnCreate oder OnLoadFinished kommt, ohne dass einer der anderen Events
      // kommt.
      if ((event.equals("OnViewCreated") || event.equals("OnLoad")
        || event.equals("OnNew") || event.equals("OnCreate") || event.equals("OnLoadFinished"))
        && !seenAlready(compo))
      {
        if (xTextDoc != null)
        {
          docManager.addTextDocument(xTextDoc);
          WollMuxEventHandler.handleProcessTextDocument(xTextDoc);
        }
        else
        {
          docManager.add(compo);
          WollMuxEventHandler.handleNotifyDocumentEventListener(null,
            WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED, compo);
        }
      }
      else if ((event.equals("OnUnload") && compo != null))
      {
        DocumentManager.Info info = docManager.remove(compo);
        /**
         * ACHTUNG! ACHTUNG! Zu folgender Zeile unbedingt
         * {@link WollMuxEventHandler#handleTextDocumentClosed(DocumentManager.Info}
         * lesen. Hier darf AUF KEINEN FALL info.hasTextDocumentModel() getestet oder
         * info.getTextDocumentModel() aufgerufen werden!
         */
        WollMuxEventHandler.handleTextDocumentClosed(info);
      }
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  public void disposing(EventObject arg0)
  {
  // nothing to do
  }
}
