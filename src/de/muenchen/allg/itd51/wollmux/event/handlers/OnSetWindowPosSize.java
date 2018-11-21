package de.muenchen.allg.itd51.wollmux.event.handlers;


import com.sun.star.document.XEventListener;
import com.sun.star.frame.XFrame;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager.Info;
import de.muenchen.allg.itd51.wollmux.document.FrameController;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Dieses Event wird vom FormModelImpl ausgelöst, wenn die Formular-GUI die
 * Position und die Ausmasse des Dokuments verändert. Ruft direkt setWindowsPosSize
 * der UNO-API auf.
 *
 * @author christoph.lutz
 */
public class OnSetWindowPosSize extends BasicEvent 
{
    private int docX, docY, docWidth, docHeight;

    private TextDocumentController documentController;

    public OnSetWindowPosSize(TextDocumentController documentController, int docX, int docY,
        int docWidth, int docHeight)
    {
      this.documentController = documentController;
      this.docX = docX;
      this.docY = docY;
      this.docWidth = docWidth;
      this.docHeight = docHeight;
    }

    @Override
    protected void doit()
    {

      documentController.getFrameController().setWindowPosSize(docX, docY, docWidth, docHeight);
      }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + docX + ", " + docY + ", "
        + docWidth + ", " + docHeight + ")";
    }
  }