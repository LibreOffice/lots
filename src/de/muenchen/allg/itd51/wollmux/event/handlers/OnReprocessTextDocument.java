package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;

public class OnReprocessTextDocument extends BasicEvent
{
    TextDocumentModel model;
    private TextDocumentController documentController;

    public OnReprocessTextDocument(TextDocumentController documentController)
    {
      this.documentController = documentController;
      this.model = documentController.getModel();
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      if (model == null) return;

      // Dokument mit neuen Dokumentkommandos über den
      // DocumentCommandInterpreter bearbeiten:
      model.getDocumentCommands().update();
      DocumentCommandInterpreter dci =
        new DocumentCommandInterpreter(documentController, WollMuxFiles.isDebugMode());
      try
      {
        dci.executeTemplateCommands();

        // manche Kommandos sind erst nach der Expansion verfügbar
        dci.scanGlobalDocumentCommands();
        dci.scanInsertFormValueCommands();
      }
      catch (java.lang.Exception e)
      {
        // Hier wird keine Exception erwartet, da Fehler (z.B. beim manuellen
        // Einfügen von Textbausteinen) bereits dort als Popup angezeigt werden
        // sollen, wo sie auftreten.
      }

      stabilize();
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + model + ")";
    }
  }