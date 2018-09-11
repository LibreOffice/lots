/*
 * Dateiname: GlobalEventListener.java
 * Projekt  : WollMux
 * Funktion : Reagiert auf globale Ereignisse
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
 * 17.05.2010 | BED | Workaround für Issue #100374 bei OnSave/OnSaveAs-Events
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */

package de.muenchen.allg.itd51.wollmux.event;

import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;

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
  private static final String ON_SAVE_AS = "OnSaveAs";

  private static final String ON_SAVE = "OnSave";

  private static final String ON_UNLOAD = "OnUnload";

  private static final String ON_CREATE = "OnCreate";

  private static final String ON_VIEW_CREATED = "OnViewCreated";

  private DocumentManager docManager;

  public GlobalEventListener(DocumentManager docManager)
  {
    this.docManager = docManager;
  }

  /**
   * NICHT SYNCHRONIZED, weil es Deadlocks gibt zwischen getUrl() und der Zustellung
   * bestimmter Events (z.B. OnSave).
   */
  @Override
  public void notifyEvent(com.sun.star.document.EventObject docEvent)
  {
    // Der try-catch-Block verhindert, daß die Funktion und damit der
    // ganze Listener ohne Fehlermeldung abstürzt.
    try
    {
      // Zur Optimierung werden hier gemeinsame Code-Teile auf das Nötigste
      // reduziert. Es gibt viele Events, die den WollMux überhaupt nicht
      // interessieren, da sollte der WollMux nichts tun (auch ein UNO-Cast kann hier
      // schon unnötig Performance fressen)
      if (docEvent.Source == null) return;
      String event = docEvent.EventName;

      if (ON_CREATE.equals(event))
        onCreate(docEvent.Source);
      else if (ON_VIEW_CREATED.equals(event))
        onViewCreated(docEvent.Source);
      else if (ON_UNLOAD.equals(event))
        onUnload(docEvent.Source);
      else if (ON_SAVE.equals(event))
        onSaveOrSaveAs(docEvent.Source);
      else if (ON_SAVE_AS.equals(event)) onSaveOrSaveAs(docEvent.Source);
    }
    catch (Throwable t)
    {
      Logger.error(t);
    }
  }

  /**
   * OnCreate ist das erste Event das aufgerufen wird, wenn ein neues leeres Dokument
   * über eine Factory erzeugt wird wie z.B. mit loadComponentFromURL(...
   * "private:factory/swriter" ...) oder in OOo über Datei->Neu. Auch OOo erzeugt
   * manchmal im Hintergrund unsichtbare leere Dokumente über die Factory. Bekannt
   * sind folgende Fälle: Beim OOo-Seriendruck über den Seriendruck-Assistent (für
   * jeden Datensatz); Beim Einfügen von Autotexten (z.B. mit "bt<F3>" in OOo).
   * 
   * Das Event kommt nicht, wenn ein Dokument von einer Datei geladen oder von einer
   * Vorlage erzeugt wird. Das Event kommt auch dann nicht, wenn eine Vorlagendatei
   * als Standardvorlage für neue Dokumente definiert ist und Datei->Neu verwendet
   * wird.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private void onCreate(Object source)
  {
    XComponent compo = UNO.XComponent(source);
    if (compo != null)
    {
      WollMuxEventHandler.handleOnCreateDocument(compo);
    }
  }

  /**
   * OnViewCreated kommt, wenn ein Dokument seitens OOo vollständig aufgebaut ist.
   * Das Event kommt bei allen Dokumenten, egal ob sie neu erzeugt, geladen, sichtbar
   * oder unsichtbar sind.
   * 
   * Da das Event in allen möglichen Fällen kommt, und die Bearbeitung von
   * unsichtbaren Dokumenten durch den WollMux für eine andere stadtinterne Anwendung
   * (JavaComm) notwendig ist, wird in diesem Event die eigentliche Verarbeitung von
   * Dokumenten durch den WollMux angestoßen.
   * 
   * Ausgenommen von der Verarbeitung werden temporäre Dokumente des OOo-Seriendrucks
   * und alle gerade erzeugten, unsichtbaren Textdokumente.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private void onViewCreated(Object source)
  {
    XModel compo = UNO.XModel(source);
    if (compo != null)
    {
      WollMuxEventHandler.handleOnViewCreated(compo);
    }
  }

  /**
   * OnSave oder OnSaveAs-Events werden beim Speichern von Dokumenten aufgerufen.
   * 
   * Wir verwenden diese beiden Events um die persistenten Daten des WollMux sicher
   * zu persistieren (flush).
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private void onSaveOrSaveAs(Object source)
  {
    XTextDocument xTextDoc = UNO.XTextDocument(source);
    if (xTextDoc == null) return;
    DocumentManager.Info info = docManager.getInfo(xTextDoc);
    if (info != null && info.hasTextDocumentModel())
        info.getTextDocumentController().flushPersistentData();
  }

  /**
   * OnUnlaod kommt als letztes Event wenn ein Dokument geschlossen wurde. Wir nutzen
   * dieses Event um den docManager aufzuräumen und angeschlossene Listener zu
   * informieren.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private void onUnload(Object source)
  {
    DocumentManager.Info info = docManager.remove(source);
    // info kann auch null sein, wenn es sich z.B. um ein temporäres Dokument des
    // Seriendrucks handelt
    /**
     * ACHTUNG! ACHTUNG! Zu folgender Zeile unbedingt {@link
     * WollMuxEventHandler#handleTextDocumentClosed(DocumentManager.Info} lesen. Hier
     * darf AUF KEINEN FALL info.hasTextDocumentModel() getestet oder
     * info.getTextDocumentModel() aufgerufen werden!
     */
    if (info != null)
      WollMuxEventHandler.handleTextDocumentClosed(info);
  }

  @Override
  public void disposing(EventObject arg0)
  {
    // nothing to do
  }
}
