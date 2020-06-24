/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.document.XEventListener;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.HashableComponent;
import de.muenchen.allg.itd51.wollmux.core.document.AnnotationBasedPersistentDataContainer;
import de.muenchen.allg.itd51.wollmux.core.document.PersistentDataContainer;
import de.muenchen.allg.itd51.wollmux.core.document.RDFBasedPersistentDataContainer;
import de.muenchen.allg.itd51.wollmux.core.document.RDFMetadataNotSupportedException;
import de.muenchen.allg.itd51.wollmux.core.document.RDFReadLegacyModeDataContainer;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.TransitionModeDataContainer;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentControllerInitialized;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;

/**
 * Verwaltet Informationen zu allen offenen OOo-Dokumenten.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DocumentManager
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DocumentManager.class);

  /**
   * Verwaltet Informationen zu allen offenen OOo-Dokumenten.
   */
  private static DocumentManager docManager;

  private Map<HashableComponent, Info> info = new HashMap<>();
  private Map<XTextDocument, FormularMax4kController> fm4k = new HashMap<>();
  private Map<XTextDocument, FormController> controller = new HashMap<>();

  /**
   * Enthält alle registrierten XEventListener, die bei Statusänderungen der
   * Dokumentbearbeitung informiert werden.
   */
  private List<XEventListener> registeredDocumentEventListener;

  private DocumentManager() {
    registeredDocumentEventListener = new ArrayList<>();
  }

  /**
   * Fügt compo den gemanageten Objekten hinzu, wobei die für Textdokumente
   * relevanten Informationen hinterlegt werden.
   */
  public synchronized void addTextDocument(XTextDocument compo)
  {
    TextDocumentInfo docInfo = new TextDocumentInfo(compo);
    info.put(new HashableComponent(compo), docInfo);
    new OnTextDocumentControllerInitialized(docInfo.getTextDocumentController()).emit();
  }

  public Map<HashableComponent, Info> getTextDocumentList() {
    return info;
  }

  /**
   * Fügt compo den gemanageten Objekten hinzu, ohne weitere Informationen zu
   * hinterlegen. compo ist also ein Objekt, an dem für den WollMux nur interessant
   * ist, dass es existiert.
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
   */
  public synchronized Info remove(Object compo)
  {
    return info.remove(new HashableComponent(compo));
  }

  /**
   * Liefert die über dieses Objekt bekannten Informationen oder null, falls das
   * Objekt dem Manager nicht bekannt ist.
   */
  public synchronized Info getInfo(XComponent compo)
  {
    return info.get(new HashableComponent(compo));
  }

  /**
   * Fügt infoCollector alle Dokumente hinzu für die das
   * OnWollMuxProcessingFinished-Event bereits verschickt wurde.
   */
  public synchronized void getProcessedDocuments(Collection<XComponent> infoCollector)
  {
    for (Map.Entry<HashableComponent, Info> ent : info.entrySet())
    {
      if (ent.getValue().isProcessingFinished())
      {
        XComponent compo = UNO.XComponent(ent.getKey().getComponent());
        if (compo != null) {
          infoCollector.add(compo);
        }
      }
    }
  }

  /**
   * Setzt in den Informationen zu compo (falls dem DocumentManager bekannt) das Flag
   * das anzeigt, dass das OnWollMuxProcessingFinished-Event für diese Komponente
   * bereits verschickt wurde.
   *
   * @param compo
   */
  public synchronized void setProcessingFinished(XComponent compo)
  {
    Info nfo = info.get(new HashableComponent(compo));
    if (nfo != null) {
      nfo.setProcessingFinished();
    }
  }

  /**
   * Liefert einen Iterator auf alle registrierten XEventListener-Objekte, die über Änderungen am
   * Status der Dokumentverarbeitung informiert werden sollen.
   *
   * @return Liste aller registrierten XEventListener-Objekte.
   */
  public synchronized List<XEventListener> getDocumentEventListener()
  {
    return registeredDocumentEventListener;
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
  public synchronized void addDocumentEventListener(XEventListener listener)
  {
    LOGGER.trace("DocumentManager::addDocumentEventListener()");

    if (listener == null) {
      return;
    }

    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) {
        return;
      }
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
  public synchronized void removeDocumentEventListener(XEventListener listener)
  {
    LOGGER.trace("DocumentManager::removeDocumentEventListener()");
    Iterator<XEventListener> i = registeredDocumentEventListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener)) {
        i.remove();
      }
    }
  }

  /**
   * Liefert die zu diesem Dokument zugehörige FormularGUI, falls dem TextDocumentModel die Existent
   * einer FormGUI über setFormGUI(...) mitgeteilt wurde - andernfalls wird null zurück geliefert.
   *
   * @return Die FormularGUI des Formulardokuments oder null
   */
  public synchronized FormController getFormController(XTextDocument doc)
  {
    return controller.get(doc);
  }

  /**
   * Gibt dem TextDocumentModel die Existent der FormularGUI formGUI bekannt und wird vom
   * DocumentCommandInterpreter in der Methode processFormCommands() gestartet hat, falls das
   * Dokument ein Formulardokument ist.
   *
   * @param doc
   * @param formModel
   */
  public synchronized void setFormController(XTextDocument doc, FormController formModel)
  {
    this.controller.put(doc, formModel);
  }

  /**
   * Setzt die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * FormularMax4000.
   *
   * @param max
   */
  public synchronized void setCurrentFormularMax4000(XTextDocument doc, FormularMax4kController max)
  {
    fm4k.put(doc, max);
  }

  /**
   * Liefert die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden FormularMax4000
   * zurück, oder null, falls kein FormularMax gestartet wurde.
   *
   * @return Den FormularMax4000 oder null.
   */
  public synchronized FormularMax4kController getCurrentFormularMax4000(XTextDocument doc)
  {
    return fm4k.get(doc);
  }

  /**
   * Test whether a {@link TextDocumentController} has been created for the
   * given document.
   *
   * @param doc
   *          The document.
   * @return True if there's a {@link TextDocumentController}, false otherwise.
   */
  public static boolean hasTextDocumentController(XTextDocument doc)
  {
    return doc != null && getDocumentManager().getTextDocumentList()
        .containsKey(new HashableComponent(doc));
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
  public static TextDocumentController getTextDocumentController(XTextDocument doc)
  {
    Info info = getDocumentManager().getInfo(doc);
    if (info == null)
    {
      LOGGER.error(
        L.m("Irgendwer will hier ein TextDocumentModel für ein Objekt was der DocumentManager nicht kennt. Das sollte nicht passieren!"),
        new Exception());

      // Wir versuchen trotzdem sinnvoll weiterzumachen.
      getDocumentManager().addTextDocument(doc);
      info = getDocumentManager().getInfo(doc);
    }

    return info.getTextDocumentController();
  }

  /**
   * Get the controller of the document if the frame belongs to a {@link XTextDocument}.
   * {@link #getTextDocumentController(XTextDocument)}
   *
   * @param frame
   *          The frame.
   * @return The {@link TextDocumentController} of the frame or null.
   */
  public static TextDocumentController getTextDocumentController(XFrame frame)
  {
    if (frame != null)
    {
      XTextDocument doc = UNO.XTextDocument(frame.getController().getModel());
      if (doc != null)
      {
        return DocumentManager.getTextDocumentController(doc);
      }
    }
    return null;
  }

  /**
   * Liefert eine Referenz auf den von diesem WollMux verwendeten
   * {@link DocumentManager}.
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
     */
    public TextDocumentController getTextDocumentController()
    {
      return null;
    }

    /**
     * Liefert true gdw dieses Dokument ein TextDocumentModel zugeordnet haben kann
     * UND ein solches auch bereits angelegt wurde.
     */
    public boolean hasTextDocumentModel()
    {
      return false;
    }

    /**
     * Setzt das Flag, das mit {@link #isProcessingFinished()} abgefragt wird auf
     * true.
     */
    private void setProcessingFinished()
    {
      processingFinished = true;
    }
  }

  public static class TextDocumentInfo extends Info
  {
    private TextDocumentController documentController = null;

    private XTextDocument doc;

    public TextDocumentInfo(XTextDocument doc)
    {
      this.doc = doc;

      if (documentController == null)
      {
        documentController = new TextDocumentController(
            new TextDocumentModel(doc, createPersistentDataContainer(doc)),
            GlobalFunctions.getInstance().getGlobalFunctions(), GlobalFunctions.getInstance().getFunctionDialogs());
      }
    }

    /**
     * synchronized is required due WollMux Event Queue and LO's event handler. Two different
     * threads.
     */
    @Override
    public synchronized TextDocumentController getTextDocumentController()
    {
      return documentController;
    }

    @Override
    public boolean hasTextDocumentModel()
    {
      return getTextDocumentController().getModel() != null;
    }

    @Override
    public String toString()
    {
      return "TextDocumentInfo: controller: " + documentController + " doc: " + doc;
    }
  }

  /**
   * Ruft die Dispose-Methoden von allen aktiven, dem TextDocumentModel zugeordneten
   * Dialogen auf und gibt den Speicher des TextDocumentModels frei.
   */
  public synchronized void dispose(XTextDocument doc)
  {
    if (fm4k.containsKey(doc) && fm4k.get(doc) != null)
    {
      fm4k.get(doc).abort();
    }
    fm4k.remove(doc);

    controller.remove(doc);
  }

  /**
   * Liefert abhängig von der Konfigurationseinstellung PERSISTENT_DATA_MODE
   * (annotation|transition|rdfReadLegacy|rdf) den dazugehörigen PersistentDataContainer für das
   * Dokument doc.
   *
   * Die folgende Aufstellung zeigt das Verhalten der verschiedenen Einstellungen bezüglich der
   * möglichen Kombinationen von Metadaten in den Ausgangsdokumenten und der Aktualisierung der
   * Metadaten in den Ergebnisdokumenten. Ein "*" symbolisiert dabei, welcher Metadatencontainer
   * jeweils aktuell ist bzw. bei Dokumentänderungen aktualisiert wird.
   *
   * Ausgangsdokument -&gt; bearbeitet durch -&gt; Ergebnisdokument
   *
   * [N*] -&gt; annotation-Mode (WollMux-Alt) -&gt; [N*]
   *
   * [N*] -&gt; transition-Mode -&gt; [N*R*]
   *
   * [N*] -&gt; rdfReadLegacy-Mode -&gt; [R*]
   *
   * [N*] -&gt; rdf-Mode: NICHT UNTERSTÜTZT
   *
   * [N*R*] -&gt; annotation-Mode (WollMux-Alt) -&gt; [N*R]
   *
   * [N*R*] -&gt; transition-Mode -&gt; [N*R*]
   *
   * [N*R*] -&gt; rdfReadLegacy-Mode -&gt; [R*]
   *
   * [N*R*] -&gt; rdf-Mode -&gt; [NR*]
   *
   * [N*R] -&gt; annotation-Mode (WollMux-Alt) -&gt; [N*R]
   *
   * [N*R] -&gt; transition-Mode -&gt; [N*R*]
   *
   * [N*R] -&gt; rdfReadLegacy-Mode -&gt; [R*]
   *
   * [N*R] -&gt; rdf-Mode: NICHT UNTERSTÜTZT
   *
   * [NR*] -&gt; annotation-Mode (WollMux-Alt) : NICHT UNTERSTÜTZT
   *
   * [NR*] -&gt; transition-Mode: NICHT UNTERSTÜTZT
   *
   * [NR*] -&gt; rdfReadLegacy-Mode: NICHT UNTERSTÜTZT
   *
   * [NR*] -&gt; rdf -&gt; [NR*]
   *
   * [R*] -&gt; annotation-Mode (WollMux-Alt): NICHT UNTERSTÜTZT
   *
   * [R*] -&gt; transition-Mode -&gt; [N*R*]
   *
   * [R*] -&gt; rdfReadLegacy-Mode -&gt; [R*]
   *
   * [R*] -&gt; rdf-Mode -&gt; [R*]
   *
   * Agenda: [N]=Dokument mit Notizen; [R]=Dokument mit RDF-Metadaten; [NR]=Dokument mit Notizen und
   * RDF-Metadaten; *=N/R enthält aktuellen Stand;
   */
  public static PersistentDataContainer createPersistentDataContainer(
      XTextDocument doc)
  {
    ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
    String pdMode;
    try
    {
      pdMode = wmConf.query(PersistentDataContainer.PERSISTENT_DATA_MODE).getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      pdMode = PersistentDataContainer.PERSISTENT_DATA_MODE_RDFREADLEGACY;
      LOGGER.debug(L.m("Attribut %1 nicht gefunden. Verwende Voreinstellung '%2'.",
        PersistentDataContainer.PERSISTENT_DATA_MODE, pdMode));
    }

    try
    {
      if (PersistentDataContainer.PERSISTENT_DATA_MODE_TRANSITION.equalsIgnoreCase(pdMode))
      {
        return new TransitionModeDataContainer(doc);
      }
      else if (PersistentDataContainer.PERSISTENT_DATA_MODE_RDFREADLEGACY.equalsIgnoreCase(pdMode))
      {
        return new RDFReadLegacyModeDataContainer(doc);
      }
      else if (PersistentDataContainer.PERSISTENT_DATA_MODE_RDF.equalsIgnoreCase(pdMode))
      {
        return new RDFBasedPersistentDataContainer(doc);
      }
      else if (PersistentDataContainer.PERSISTENT_DATA_MODE_ANNOTATION.equalsIgnoreCase(pdMode))
      {
        return new AnnotationBasedPersistentDataContainer(doc);
      }
      else
      {
        LOGGER.error(L.m(
          "Ungültiger Wert '%1' für Attribut %2. Verwende Voreinstellung '%3' statt dessen.",
          pdMode, PersistentDataContainer.PERSISTENT_DATA_MODE, PersistentDataContainer.PERSISTENT_DATA_MODE_RDFREADLEGACY));
        return new RDFReadLegacyModeDataContainer(doc);
      }
    }
    catch (RDFMetadataNotSupportedException e)
    {
      LOGGER.info(L.m(
        "Die Einstellung '%1' für Attribut %2 ist mit dieser Office-Version nicht kompatibel. Verwende Einstellung '%3' statt dessen.",
        pdMode, PersistentDataContainer.PERSISTENT_DATA_MODE, PersistentDataContainer.PERSISTENT_DATA_MODE_ANNOTATION));
      return new AnnotationBasedPersistentDataContainer(doc);
    }
  }
}
