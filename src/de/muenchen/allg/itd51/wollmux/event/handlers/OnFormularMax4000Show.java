package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;

/**
 * Erzeugt ein neues WollMuxEvent, das den FormularMax4000 aufruft für das Dokument
 * doc.
 *
 * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
 * WollMuxEventHandler ausgelöst.
 */
public class OnFormularMax4000Show extends BasicEvent
{

    private TextDocumentController documentController;

    public OnFormularMax4000Show(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      // Bestehenden Max in den Vordergrund holen oder neuen Max erzeugen.
      FormularMax4kController max = DocumentManager.getDocumentManager().getCurrentFormularMax4000(documentController.getModel().doc);
      if (max != null)
      {
        max.toFront();
      }
      else
      {
        ActionListener l = actionEvent -> 
        {
          if (actionEvent.getSource() instanceof FormularMax4kController)
            WollMuxEventHandler.getInstance().handleFormularMax4000Returned(documentController);
        };

        // Der Konstruktor von FormularMax erwartet hier nur die globalen
        // Funktionsbibliotheken, nicht jedoch die neuen dokumentlokalen
        // Bibliotheken, die das model bereitstellt. Die dokumentlokalen
        // Bibliotheken kann der FM4000 selbst auflösen.
        max =
          new FormularMax4kController(documentController, l,
            GlobalFunctions.getInstance().getGlobalFunctions(),
            GlobalFunctions.getInstance().getGlobalPrintFunctions());
        DocumentManager.getDocumentManager().setCurrentFormularMax4000(documentController.getModel().doc, max);
        max.run();
      }
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
  }