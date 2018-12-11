package de.muenchen.allg.itd51.wollmux.event.handlers;


import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Dieses Event wird vom FormModelImpl ausgelöst, wenn die Formular-GUI die
 * Position und die Ausmasse des Dokuments verändert. Ruft direkt setWindowsPosSize
 * der UNO-API auf.
 *
 * @author christoph.lutz
 */
public class OnSetWindowPosSize extends BasicEvent 
{
    private int docX;
    private int docY;
    private int docWidth;
    private int docHeight;

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