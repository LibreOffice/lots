/*
 * Dateiname: DocumentManager.java
 * Projekt  : WollMux
 * Funktion : Verwaltet Informationen zu allen offenen OOo-Dokumenten
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 27.10.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;

/**
 * Verwaltet Informationen zu allen offenen OOo-Dokumenten.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DocumentManager
{
  /**
   * Verwaltet Informationen zu allen offenen OOo-Dokumenten.
   */
  private static DocumentManager docManager;
  
  private HashMap<HashableComponent, Info> info =
    new HashMap<HashableComponent, Info>();
  
  /**
   * Enthält alle registrierten XEventListener, die bei Statusänderungen der
   * Dokumentbearbeitung informiert werden.
   */
  private Vector<XEventListener> registeredDocumentEventListener;
  
  private DocumentManager() {
    registeredDocumentEventListener = new Vector<XEventListener>();
  }

  /**
   * Fügt compo den gemanageten Objekten hinzu, wobei die für Textdokumente
   * relevanten Informationen hinterlegt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public synchronized void addTextDocument(XTextDocument compo)
  {
    info.put(new HashableComponent(compo), new TextDocumentInfo(compo));
  }

  /**
   * Fügt compo den gemanageten Objekten hinzu, ohne weitere Informationen zu
   * hinterlegen. compo ist also ein Objekt, an dem für den WollMux nur interessant
   * ist, dass es existiert.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public synchronized void add(XComponent compo)
  {
    info.put(new HashableComponent(compo), new Info());
  }

  /**
   * Entfernt alle Informationen über compo (falls vorhanden) aus diesem Manager.
   * 
   * compo wird hier zur Optimierung nur als Object erwartet (spart einen
   * vorangehenden UNO-Cast auf XComponent).
   * 
   * @return die entfernten Informationen oder null falls keine vorhanden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public synchronized Info remove(Object compo)
  {
    return info.remove(new HashableComponent(compo));
  }

  /**
   * Liefert die über dieses Objekt bekannten Informationen oder null, falls das
   * Objekt dem Manager nicht bekannt ist.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public synchronized Info getInfo(XComponent compo)
  {
    return info.get(new HashableComponent(compo));
  }

  /**
   * Fügt infoCollector alle Dokumente hinzu für die das
   * OnWollMuxProcessingFinished-Event bereits verschickt wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  public synchronized void getProcessedDocuments(Collection<XComponent> infoCollector)
  {
    for (Map.Entry<HashableComponent, Info> ent : info.entrySet())
    {
      if (ent.getValue().isProcessingFinished())
      {
        XComponent compo = UNO.XComponent(ent.getKey().getComponent());
        if (compo != null) infoCollector.add(compo);
      }
    }
  }

  /**
   * Setzt in den Informationen zu compo (falls dem DocumentManager bekannt) das Flag
   * das anzeigt, dass das OnWollMuxProcessingFinished-Event für diese Komponente
   * bereits verschickt wurde.
   * 
   * @param compo
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  public synchronized void setProcessingFinished(XComponent compo)
  {
    Info nfo = info.get(new HashableComponent(compo));
    if (nfo != null) nfo.setProcessingFinished();
  }

  /**
   * Liefert einen Iterator auf alle registrierten XEventListener-Objekte, die über
   * Änderungen am Status der Dokumentverarbeitung informiert werden sollen.
   * 
   * @return Iterator auf alle registrierten XEventListener-Objekte.
   */
  public Iterator<XEventListener> documentEventListenerIterator()
  {
    return registeredDocumentEventListener.iterator();
  }

  /**
   * Diese Methode registriert einen XEventListener, der Nachrichten empfängt wenn
   * sich der Status der Dokumentbearbeitung ändert (z.B. wenn ein Dokument
   * vollständig bearbeitet/expandiert wurde). Die Methode ignoriert alle
   * XEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void addDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("DocumentManager::addDocumentEventListener()");

    if (listener == null) return;

    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) return;
    }
    registeredDocumentEventListener.add(listener);
  }

  /**
   * Diese Methode deregistriert einen XEventListener wenn er bereits registriert
   * war.
   * 
   * Achtung: Die Methode darf nicht direkt von einem UNO-Service aufgerufen werden,
   * sondern jeder Aufruf muss über den EventHandler laufen. Deswegen exportiert
   * WollMuxSingleton auch nicht das XEventBroadcaster-Interface.
   */
  public void removeDocumentEventListener(XEventListener listener)
  {
    Logger.debug2("DocumentManager::removeDocumentEventListener()");
    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) i.remove();
    }
  }
  
  /**
   * Liefert das aktuelle TextDocumentModel zum übergebenen XTextDocument doc;
   * existiert zu doc noch kein TextDocumentModel, so wird hier eines erzeugt und das
   * neu erzeugte zurück geliefert.
   * 
   * @param doc
   *          Das XTextDocument, zu dem das zugehörige TextDocumentModel
   *          zurückgeliefert werden soll.
   * @return Das zu doc zugehörige TextDocumentModel.
   */
  public static TextDocumentModel getTextDocumentModel(XTextDocument doc)
  {
    Info info = getDocumentManager().getInfo(doc);
    if (info == null)
    {
      Logger.error(
        L.m("Irgendwer will hier ein TextDocumentModel für ein Objekt was der DocumentManager nicht kennt. Das sollte nicht passieren!"),
        new Exception());
  
      // Wir versuchen trotzdem sinnvoll weiterzumachen.
      getDocumentManager().addTextDocument(doc);
      info = getDocumentManager().getInfo(doc);
    }
  
    return info.getTextDocumentModel();
  }

  /**
   * Liefert eine Referenz auf den von diesem WollMux verwendeten
   * {@link DocumentManager}.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static DocumentManager getDocumentManager()
  {
    if (docManager == null)
      docManager = new DocumentManager();
    
    return docManager;
  }

  public static class Info
  {
    /**
     * Gibt an ob für das Dokument bereits ein OnWollMuxProcessingFinished-Event an
     * die Listener verschickt wurde.
     */
    private boolean processingFinished = false;

    /**
     * Liefert true gdw für das Dokument bereits ein
     * OnWollMuxProcessingFinished-Event an die Listener verschickt wurde.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public boolean isProcessingFinished()
    {
      return processingFinished;
    }

    /**
     * Liefert das zu diesem Dokument gehörige TextDocumentModel. Falls es noch nicht
     * angelegt wurde, wird es angelegt.
     * 
     * @throws UnsupportedOperationException
     *           falls das Dokument kein TextDocument ist.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public TextDocumentModel getTextDocumentModel()
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Liefert true gdw dieses Dokument ein TextDocumentModel zugeordnet haben kann
     * UND ein solches auch bereits angelegt wurde.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public boolean hasTextDocumentModel()
    {
      return false;
    }

    /**
     * Setzt das Flag, das mit {@link #isProcessingFinished()} abgefragt wird auf
     * true.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    private void setProcessingFinished()
    {
      processingFinished = true;
    }
  }

  public static class TextDocumentInfo extends Info
  {
    private TextDocumentModel model = null;

    private XTextDocument doc;

    public TextDocumentInfo(XTextDocument doc)
    {
      this.doc = doc;
    }

    /**
     * Auf die Methoden getTextDocumentModel() und hasTextDocumentModel() wird
     * möglicherweise aus verschiedenen Threads zugegriffen (WollMux Event Queue und
     * Event Handler im Singleton), daher ist synchronized notwendig.
     */
    @Override
    public synchronized TextDocumentModel getTextDocumentModel()
    {
      if (model == null) model = new TextDocumentModel(doc);
      return model;
    }

    @Override
    public boolean hasTextDocumentModel()
    {
      return model != null;
    }

    @Override
    public String toString()
    {
      return "TextDocumentInfo - model=" + model;
    }
  }
}
