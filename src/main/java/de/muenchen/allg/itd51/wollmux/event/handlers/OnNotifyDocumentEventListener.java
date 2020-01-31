package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.XTopWindow2;
import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;

/**
 * Event for notifying listeners registered on an object.
 */
public class OnNotifyDocumentEventListener extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnNotifyDocumentEventListener.class);
  private String eventName;

  private Object source;

  private XEventListener listener;

  /**
   * Create this event.
   *
   * @param listener
   *          If listener == null all registered listeners are notified, otherwise only the provided
   *          listener is notified.
   * @param eventName
   *          Name of the notification event.
   * @param source
   *          The source of the notification event.
   */
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

    for (XEventListener docListener : DocumentManager.getDocumentManager()
        .getDocumentEventListener())
    {
      if (this.listener == null || this.listener == docListener)
      {
        LOGGER.trace("notifying XEventListener (event '{}')", eventName);
        new Thread(() -> docListener.notifyEvent(eventObject)).start();
      }
    }

    XComponent compo = UNO.XComponent(source);
    if (compo != null && eventName.equals(WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED))
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

          setWindowPosSize(documentController);

          formController.addPropertyChangeListener(
              (PropertyChangeEvent e) -> setWindowPosSize(documentController));
        }
      }
    }
  }

  /**
   * Set the position of the window.
   *
   * Attention: LibreOffice window position starts with gray edge above the file menu and not with
   * the window border.
   *
   * @param documentController
   *          The controller of the document.
   */
  private void setWindowPosSize(TextDocumentController documentController)
  {
    LOGGER.debug("setWindowPosSize(..)");
    // Since KDE4 maximized windows can't be positioned
    XTopWindow2 window = UnoRuntime.queryInterface(XTopWindow2.class,
        documentController.getFrameController().getFrame().getContainerWindow());
    if (window != null && window.getIsMaximized())
    {
      window.setIsMaximized(false);
    }

    try
    {
      Rectangle frameB = documentController.getFormController().getFrameBounds();
      Rectangle maxWindowBounds = documentController.getFormController()
          .getMaxWindowBounds();
      Insets windowInsets = documentController.getFormController().getWindowInsets();

      /*
       * Addition of windowInsets.left and right is a heuristic, because setWindowPosSize() behaves
       * different on Windows and Unix.
       */
      int docX = frameB.width + frameB.x + windowInsets.left;
      int docWidth = maxWindowBounds.width - frameB.width - frameB.x - windowInsets.right;
      if (docWidth < 0)
      {
        docX = maxWindowBounds.x;
        docWidth = maxWindowBounds.width;
      }
      int docY = maxWindowBounds.y + windowInsets.top;
      /*
       * Subtraction is also a heuristic (see above)
       */
      int docHeight = maxWindowBounds.y + maxWindowBounds.height - docY - 2 * windowInsets.bottom;

      documentController.getFrameController().getFrame().getContainerWindow().setPosSize(docX, docY,
          docWidth, docHeight, PosSize.SIZE);
      documentController.getFrameController().getFrame().getContainerWindow().setPosSize(docX, docY,
          docWidth, docHeight, PosSize.POS);
    } catch (FormModelException ex)
    {
      LOGGER.debug("no form document", ex);
    }
  }
}