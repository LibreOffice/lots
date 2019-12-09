package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;

/**
 * Über dieses Event werden alle registrierten DocumentEventListener (falls
 * listener==null) oder ein bestimmter registrierter DocumentEventListener (falls
 * listener != null) (XEventListener-Objekte) über Statusänderungen der
 * Dokumentbearbeitung informiert
 *
 * @param listener
 *          der zu benachrichtigende XEventListener. Falls null werden alle
 *          registrierten Listener benachrichtig. listener wird auf jeden Fall nur
 *          benachrichtigt, wenn er zur Zeit der Abarbeitung des Events noch
 *          registriert ist.
 * @param eventName
 *          Name des Events
 * @param source
 *          Das von der Statusänderung betroffene Dokument (üblicherweise eine
 *          XComponent)
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class OnNotifyDocumentEventListener extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnNotifyDocumentEventListener.class);
  private String eventName;

  private Object source;

  private XEventListener listener;

  public OnNotifyDocumentEventListener(XEventListener listener,
      String eventName,
      Object source)
  {
    this.listener = listener;
    this.eventName = eventName;
    this.source = source;
  }

  @Override
  protected void doit()
  {
    final com.sun.star.document.EventObject eventObject = new com.sun.star.document.EventObject();
    eventObject.Source = source;
    eventObject.EventName = eventName;

    Iterator<XEventListener> i = DocumentManager.getDocumentManager()
        .documentEventListenerIterator();
    while (i.hasNext())
    {
      LOGGER.trace("notifying XEventListener (event '{}')", eventName);
      try
      {
        final XEventListener docListener = i.next();
        if (this.listener == null || this.listener == docListener)
        {
          docListener.notifyEvent(eventObject);
        }
      } catch (java.lang.Exception e)
      {
        i.remove();
      }
    }

    XComponent compo = UNO.XComponent(source);
    if (compo != null
        && eventName.equals(WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED))
    {
      DocumentManager.getDocumentManager().setProcessingFinished(
          compo);
      XTextDocument xTextDoc = UNO.XTextDocument(source);
      if (xTextDoc != null)
      {
        TextDocumentController documentController = DocumentManager
            .getTextDocumentController(xTextDoc);

        if (documentController.getModel().isFormDocument())
        {

          FormController formController = DocumentManager.getDocumentManager()
              .getFormModel(xTextDoc);
          formController.showFormGUI();

          setWindowPosSize(formController, documentController, xTextDoc);

          formController.addPropertyChangeListener(
            (PropertyChangeEvent e) -> setWindowPosSize(formController, documentController, xTextDoc)
          );
        }
      }
    }
  }

  /**
   * Setzt die Position des Fensters auf die übergebenen Koordinaten, wobei die
   * Nachteile der UNO-Methode setWindowPosSize greifen, bei der die Fensterposition
   * nicht mit dem äusseren Fensterrahmen beginnt, sondern mit der grauen Ecke links
   * über dem File-Menü.
   *
   * @param formController
   * @param documentController
   * @param xTextDoc
   */
  private void setWindowPosSize(FormController formController,
      TextDocumentController documentController, XTextDocument xTextDoc)
  {
    LOGGER.debug("setWindowPosSize(..)");
    // Seit KDE4 muss ein maximiertes Fenster vor dem Verschieben "demaximiert" werden
    // sonst wird die Positionierung ignoriert. Leider ist die dafür benötigte Klasse
    // erst seit OpenOffice.org 3.4 verfügbar - zur Abwärtskompatibilität erfolgt der
    // Aufruf daher über Reflection.
    try
    {
      Class<?> c = Class.forName("com.sun.star.awt.XTopWindow2");
      Object o = UnoRuntime.queryInterface(c,
          xTextDoc.getCurrentController().getFrame().getContainerWindow());
      Method getIsMaximized = c.getMethod("getIsMaximized", (Class[]) null);
      Method setIsMaximized = c.getMethod("setIsMaximized", (boolean.class));
      if ((Boolean) getIsMaximized.invoke(o, (Object[]) null))
      {
        setIsMaximized.invoke(o, false);
      }
    } catch (java.lang.Exception e)
    {
      LOGGER.debug("", e);
    }

    java.awt.Rectangle frameB = formController.getFrameBounds();
    java.awt.Rectangle maxWindowBounds = formController.getMaxWindowBounds();
    Insets windowInsets = formController.getWindowInsets();

    /*
     * Das Addieren von windowInsets.left und windowInsets.right ist eine Heuristik. Da sich
     * setWindowPosSize() unter Windows und Linux anders verhält, gibt es keine korrekte Methode
     * (die mir bekannt ist), um die richtige Ausrichtung zu berechnen.
     */
    int docX = frameB.width + frameB.x + windowInsets.left;
    int docWidth = maxWindowBounds.width - frameB.width - frameB.x
        - windowInsets.right;
    if (docWidth < 0)
    {
      docX = maxWindowBounds.x;
      docWidth = maxWindowBounds.width;
    }
    int docY = maxWindowBounds.y + windowInsets.top;
    /*
     * Das Subtrahieren von 2*windowInsets.bottom ist ebenfalls eine Heuristik. (siehe weiter oben)
     */
    int docHeight = maxWindowBounds.y + maxWindowBounds.height - docY
        - 2 * windowInsets.bottom;

    documentController.getFrameController().getFrame().getContainerWindow()
        .setPosSize(docX, docY, docWidth, docHeight, PosSize.SIZE);
    documentController.getFrameController().getFrame().getContainerWindow()
        .setPosSize(docX, docY, docWidth, docHeight, PosSize.POS);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "('" + eventName + "', "
        + ((source != null) ? "#" + source.hashCode() : "null") + ")";
  }
}