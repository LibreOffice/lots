package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.ArrayList;
import java.util.List;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Adds listener for notification about document processing.
 */
public class OnAddDocumentEventListener extends WollMuxEvent
{
  private XEventListener listener;

  /**
   * Create this event.
   *
   * @param listener
   *          The listener to register.
   */
  public OnAddDocumentEventListener(XEventListener listener)
  {
    this.listener = listener;
  }

  @Override
  protected void doit()
  {
    DocumentManager.getDocumentManager().addDocumentEventListener(listener);

    List<XComponent> processedDocuments = new ArrayList<>();
    DocumentManager.getDocumentManager()
        .getProcessedDocuments(processedDocuments);

    for (XComponent compo : processedDocuments)
    {
      new OnNotifyDocumentEventListener(listener,
          WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED, compo).emit();
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + listener.hashCode() + ")";
  }
}