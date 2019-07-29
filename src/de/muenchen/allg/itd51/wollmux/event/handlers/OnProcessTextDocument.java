package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.beans.PropertyChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.document.WMCommandsFailedException;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;

/**
 * Dieses Event wird immer dann ausgelöst, wenn der GlobalEventBroadcaster von OOo
 * ein ON_NEW oder ein ON_LOAD-Event wirft. Das Event sorgt dafür, dass die
 * eigentliche Dokumentbearbeitung durch den WollMux angestossen wird.
 *
 * @author christoph.lutz
 */
public class OnProcessTextDocument extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnProcessTextDocument.class);

  TextDocumentController documentController;

  boolean visible;

  public OnProcessTextDocument(TextDocumentController documentController,
      boolean visible)
  {
    this.documentController = documentController;
    this.visible = visible;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (documentController == null)
      return;

    // Konfigurationsabschnitt Textdokument verarbeiten falls Dok sichtbar:
    if (visible)
      try
      {
        ConfigThingy tds = WollMuxFiles.getWollmuxConf().query("Fenster")
            .query("Textdokument").getLastChild();
        documentController.getFrameController().setWindowViewSettings(tds);
      } catch (NodeNotFoundException e)
      {
//        kein Fehler
      }

    // Mögliche Aktionen für das neu geöffnete Dokument:
    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(
        documentController, WollMuxFiles.isDebugMode());

    try
    {
      // Globale Dokumentkommandos wie z.B. setType, setPrintFunction, ...
      // auswerten.
      dci.scanGlobalDocumentCommands();

      int actions = documentController.evaluateDocumentActions(GlobalFunctions
          .getInstance().getDocumentActionFunctions().iterator());

      // Bei Vorlagen: Ausführung der Dokumentkommandos
      if ((actions < 0 && documentController.getModel().isTemplate())
          || (actions == Integer.MAX_VALUE))
      {
        dci.executeTemplateCommands();

        // manche Kommandos sind erst nach der Expansion verfügbar
        dci.scanGlobalDocumentCommands();
      }
      // insertFormValue-Kommandos auswerten
      dci.scanInsertFormValueCommands();

      // Bei Formularen:
      // Anmerkung: actions == allactions wird NICHT so interpretiert, dass auch
      // bei Dokumenten ohne Formularfunktionen der folgende Abschnitt ausgeführt
      // wird. Dies würde unsinnige Ergebnisse verursachen.
      if (actions != 0 && documentController.getModel().isFormDocument())
      {
        // Konfigurationsabschnitt Fenster/Formular verarbeiten falls Dok sichtbar
        if (visible)
          try
          {
            documentController.getFrameController().setDocumentZoom(
                WollMuxFiles.getWollmuxConf().query("Fenster").query(
                    "Formular").getLastChild().query("ZOOM"));
          } catch (java.lang.Exception e)
          {
            // kein Fehler
          }

        // FormGUI starten
        try
        {
          FormController formController = documentController.createFormController();
          DocumentManager.getDocumentManager().setFormModel(documentController.getModel().doc,
              formController);
          formController.startFormGUI();
          formController.initValues();
          formController.formControllerInitCompleted();

          formController.focusGained(formController.getFieldId());

          formController.addPropertyChangeListener((PropertyChangeEvent e) -> formController.focusGained( formController.getFieldId()));
        } catch (FormModelException e)
        {
          throw new WMCommandsFailedException(
              L.m("Die Vorlage bzw. das Formular enthält keine gültige Formularbeschreibung\n\nBitte kontaktieren Sie Ihre Systemadministration."));
        }
      }
    } catch (java.lang.Exception e)
    {
      throw new WollMuxFehlerException(
          L.m("Fehler bei der Dokumentbearbeitung."),
          e);
    }

    // Registrierte XEventListener (etwas später) informieren, dass die
    // Dokumentbearbeitung fertig ist.
    WollMuxEventHandler.getInstance().handleNotifyDocumentEventListener(null,
        WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED,
        documentController.getModel().doc);

    // ContextChanged auslösen, damit die Dispatches aktualisiert werden.
    try
    {
      documentController.getFrameController().getFrame().contextChanged();
    } catch (java.lang.Exception e)
    {
      LOGGER.debug("", e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.hashCode() + ")";
  }
}