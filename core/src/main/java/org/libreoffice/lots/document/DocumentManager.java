/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.document;

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

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.lots.GlobalFunctions;
import org.libreoffice.lots.HashableComponent;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.event.handlers.OnTextDocumentControllerInitialized;
import org.libreoffice.lots.form.control.FormController;
import org.libreoffice.lots.former.FormularMax4kController;

/**
 * Manages information about all open OOo documents.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DocumentManager
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentManager.class);

  /**
   * Attribute name for setting the storage mode for persistent data
   */
  private static final String PERSISTENT_DATA_MODE = "PERSISTENT_DATA_MODE";
  /**
   * Wert 'annotation' does Attribute PERSISTENT DATA MODE
   */
  private static final String PERSISTENT_DATA_MODE_ANNOTATION = "annotation";
  /**
   * Wert 'transition' of PERSISTENT_DATA_MODE Attributes
   */
  private static final String PERSISTENT_DATA_MODE_TRANSITION = "transition";
  /**
   * Wert 'rdf' of PERSISTENT_DATA_MODE Attributes
   */
  private static final String PERSISTENT_DATA_MODE_RDF = "rdf";
  /**
   * Wert 'rdfReadLegacy' des Attributes PERSISTENT DATA MODE
   */
  private static final String PERSISTENT_DATA_MODE_RDFREADLEGACY = "rdfReadLegacy";

  /**
   * Manages information about all open OOo documents.
   */
  private static DocumentManager docManager;

  private Map<HashableComponent, Info> info = new HashMap<>();
  private Map<XTextDocument, FormularMax4kController> fm4k = new HashMap<>();
  private Map<XTextDocument, FormController> controller = new HashMap<>();

  /**
   * Contains all registered XEventListeners that are activated when the status changes
   * Document processing to be informed.
   */
  private List<XEventListener> registeredDocumentEventListener;

  private DocumentManager() {
    registeredDocumentEventListener = new ArrayList<>();
  }

  /**
   * Adds compo to managed objects, with those for text documents
   * Relevant information is stored.
   */
  public void addTextDocument(XTextDocument compo)
  {
    TextDocumentInfo docInfo = new TextDocumentInfo(compo);
    info.put(new HashableComponent(compo), docInfo);

    new OnTextDocumentControllerInitialized(docInfo.getTextDocumentController()).emit();
  }

  public Map<HashableComponent, Info> getTextDocumentList() {
    return info;
  }

  /**
   * Adds compo to the managed objects without further information
   * deposit. So compo is an object that is only interesting for WollMux
   * is that it existst.
   */
  public synchronized void add(XComponent compo)
  {
    info.put(new HashableComponent(compo), new Info());
  }

  /**
   * Removes all information about compo (if any) from this manager.
   *
   * compo is only expected as an object here for optimization (saves a
   * previous UNO cast on XComponent).
   *
   * @return the information removed or null if none exists.
   */
  public synchronized Info remove(Object compo)
  {
    return info.remove(new HashableComponent(compo));
  }

  /**
   * Returns the information known about this object, or null if that
   * Object unknown to manager.
   */
  public synchronized Info getInfo(XComponent compo)
  {
    return info.get(new HashableComponent(compo));
  }

  /**
   * Adds all documents to infoCollector for the das
   * OnWollMuxProcessingFinished event has already been sent.
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
   * Sets the flag in the information about compo (if known to the DocumentManager).
   * indicating that the OnWollMuxProcessingFinished event for this component
   * has already been sent.
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
   * Returns an iterator to all registered XEventListener objects that are to
   * be informed about changes to the document processing status.
   *
   * @return List of all registered XEventListener objects.
   */
  public synchronized List<XEventListener> getDocumentEventListener()
  {
    return registeredDocumentEventListener;
  }

  /**
   * This method registers an XEventListener that receives messages 
   * when the document processing status changes (e.g., when a document has been fully edited/expanded). 
   * The method ignores all XEventListener instances that have already been registered. 
   * Therefore, it is not possible to register the same instance multiple times.
   *
   * Caution: This method should not be called directly from a UNO service; instead,
   * every call must go through the EventHandler. 
   * That's why the WollMuxSingleton does not export the XEventBroadcaster interface.
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
   * This method deregisters an XEventListener if it was already registered.
   *
   * Caution: This method should not be called directly from a UNO service; 
   * instead, every call must go through the EventHandler. 
   * That's why the WollMuxSingleton does not export the XEventBroadcaster interface.
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
   * Returns the associated FormGUI for this document, if the TextDocumentModel 
   * has been informed of the existence of a FormGUI through setFormGUI(...); otherwise, null is returned.
   *
   * @return The FormGUI of the form document, or null
   */
  public synchronized FormController getFormController(XTextDocument doc)
  {
    return controller.get(doc);
  }

  /**
   * Announces the existence of the FormGUI 'formGUI' to the TextDocumentModel 
   * and is invoked by the DocumentCommandInterpreter in the method processFormCommands() 
   * if the document is a form documentt.
   *
   * @param doc
   * @param formModel
   */
  public synchronized void setFormController(XTextDocument doc, FormController formModel)
  {
    this.controller.put(doc, formModel);
  }

  /**
   * Sets the instance of the currently open FormularMax4000 associated with this document.
   *
   * @param max
   */
  public synchronized void setCurrentFormularMax4000(XTextDocument doc, FormularMax4kController max)
  {
    fm4k.put(doc, max);
  }

  /**
   * Returns the instance of the currently open FormularMax4000 associated with this document, 
   * or null if no FormularMax has been started.
   *
   * @return The FormularMax4000 or null.
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
   * Returns the current TextDocumentModel for the given XTextDocument 'doc'; 
   * if there is no existing TextDocumentModel for 'doc', 
   * one is created and the newly created one is returned.
   *
   * @param doc
   *          The XTextDocument for which the associated TextDocumentModel should be returned.
   * @return The TextDocumentModel associated with 'doc'.
   */
  public static TextDocumentController getTextDocumentController(XTextDocument doc)
  {
    Info info = getDocumentManager().getInfo(doc);
    if (info == null)
    {
      LOGGER.error("Irgendwer will hier ein TextDocumentModel für ein Objekt was der DocumentManager nicht kennt. "
          + "Das sollte nicht passieren!", new Exception());

      // We are still trying to proceed sensibly.
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
   * Returns a reference to the one used by this WollMux.
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
     * Indicates whether an OnWollMuxProcessingFinished event has already been sent to the listeners for the document.
     */
    private boolean processingFinished = false;

    /**
     * Returns true if an OnWollMuxProcessingFinished event has already been sent to the listeners for the document.
     */
    public boolean isProcessingFinished()
    {
      return processingFinished;
    }

    /**
     * Returns the TextDocumentModel associated with this document. If it has not been created yet, it will be created.
     *
     * @throws UnsupportedOperationException
     *           If the document is not a TextDocument.
     */
    public TextDocumentController getTextDocumentController()
    {
      return null;
    }

    /**
     * Returns true if this document can be associated with a TextDocumentModel and one has already been created.
     */
    public boolean hasTextDocumentModel()
    {
      return false;
    }

    /**
     * Sets the flag queried with {@link #isProcessingFinished()}
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
   * Invokes the Dispose methods of all active dialogs associated with the 
   * TextDocumentModel and releases the memory of the TextDocumentModel.
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
   * Depending on the configuration setting PERSISTENT_DATA_MODE (annotation|transition|rdfReadLegacy|rdf), 
   * it returns the corresponding PersistentDataContainer for the document 'doc'.
   *
   * The following table illustrates the behavior of different settings regarding the possible 
   * combinations of metadata in the source documents and the update of metadata in the result documents. 
   * An "*" symbolizes which metadata container is currently up-to-date or updated when document changes occur.
   *
   * source document -&gt; Edited by -&gt; Results document
   *
   * [N*] -&gt; annotation-Mode (WollMux-Alt) -&gt; [N*]
   *
   * [N*] -&gt; transition-Mode -&gt; [N*R*]
   *
   * [N*] -&gt; rdfReadLegacy-Mode -&gt; [R*]
   *
   * [N*] -&gt; rdf-Mode: NOT SUPPORTED
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
   * [N*R] -&gt; rdf-Mode: NOT SUPPORTED
   *
   * [NR*] -&gt; annotation-Mode (WollMux-Alt) : NOT SUPPORTED
   *
   * [NR*] -&gt; transition-Mode: NOT SUPPORTED
   *
   * [NR*] -&gt; rdfReadLegacy-Mode: NOT SUPPORTED
   *
   * [NR*] -&gt; rdf -&gt; [NR*]
   *
   * [R*] -&gt; annotation-Mode (WollMux-Alt): NOT SUPPORTED
   *
   * [R*] -&gt; transition-Mode -&gt; [N*R*]
   *
   * [R*] -&gt; rdfReadLegacy-Mode -&gt; [R*]
   *
   * [R*] -&gt; rdf-Mode -&gt; [R*]
   *
   * Agenda: [N] = Document with notes; [R] = Document with RDF metadata; 
   * [NR] = Document with notes and RDF metadata; * = N/R contains current status;
   */
  public static PersistentDataContainer createPersistentDataContainer(
      XTextDocument doc)
  {
    ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
    String pdMode;
    try
    {
      pdMode = wmConf.query(PERSISTENT_DATA_MODE).getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      pdMode = PERSISTENT_DATA_MODE_RDFREADLEGACY;
      LOGGER.debug("Attribut {} nicht gefunden. Verwende Voreinstellung '{}'.", PERSISTENT_DATA_MODE, pdMode);
    }

    try
    {
      if (PERSISTENT_DATA_MODE_TRANSITION.equalsIgnoreCase(pdMode))
      {
        return new TransitionModeDataContainer(doc);
      }
      else if (PERSISTENT_DATA_MODE_RDFREADLEGACY.equalsIgnoreCase(pdMode))
      {
        return new RDFReadLegacyModeDataContainer(doc);
      }
      else if (PERSISTENT_DATA_MODE_RDF.equalsIgnoreCase(pdMode))
      {
        return new RDFBasedPersistentDataContainer(doc);
      }
      else if (PERSISTENT_DATA_MODE_ANNOTATION.equalsIgnoreCase(pdMode))
      {
        return new AnnotationBasedPersistentDataContainer(doc);
      }
      else
      {
        LOGGER.error("Ungültiger Wert '{}' für Attribut {}. Verwende Voreinstellung '{}' statt dessen.", pdMode,
            PERSISTENT_DATA_MODE, PERSISTENT_DATA_MODE_RDFREADLEGACY);
        return new RDFReadLegacyModeDataContainer(doc);
      }
    }
    catch (RDFMetadataNotSupportedException e)
    {
      LOGGER.info("Die Einstellung '{}' für Attribut {} ist mit dieser Office-Version nicht kompatibel. "
          + "Verwende Einstellung '{}' statt dessen.", pdMode, PERSISTENT_DATA_MODE, PERSISTENT_DATA_MODE_ANNOTATION);
      return new AnnotationBasedPersistentDataContainer(doc);
    }
  }
}
