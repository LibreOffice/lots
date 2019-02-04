package de.muenchen.allg.itd51.wollmux.form.control;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.form.dialog.GUI;

/**
 * Der Controller für die FormularGUI.
 * 
 * Alle Änderungen in der GUI werden über diesen Controller auf das Model übertragen. Außerdem
 * werden entsprechende Aktionen an den LibreOffice-Controller mittels Events weitergereicht.
 * 
 * Der Controller beinhaltet keine Business-Logik (Sichtbarkeiten, Plausis, ...)
 * 
 * @author daniel.sikeler
 *
 */
public class FormController
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormController.class);

  /**
   * Die FormularGUI, die diesem Controller zugeordnet ist.
   */
  private GUI gui;

  /**
   * Das Model, das diesem Controller zugeordnet ist.
   */
  private FormModel model;

  /**
   * Der LibreOffice-Controller für das Writer-Dokument.
   */
  private final TextDocumentController documentController;

  /**
   * Die default Einstellung des LibreOffice-Fensters bevor die GUI geöffnet wird.
   */
  private String defaultWindowAttributes;

  /**
   * Erzeugt einen neuen Controller. Hierin wird auch die GUI initialisiert, aber noch nicht
   * aufgebaut und angezeigt.
   * 
   * @param model
   *          Das Model.
   * @param formFensterConf
   *          Die Fenstereinstellungen.
   * @param documentController
   *          Der LibreOffice-Controller.
   */
  public FormController(final FormModel model, final ConfigThingy formFensterConf, final TextDocumentController documentController)
  {
    gui = new GUI(this, formFensterConf);
    this.model = model;
    this.documentController = documentController;
    // Standard-Fensterattribute vor dem Start der Form-GUI sichern um nach
    // dem Schließen des Formulardokuments die Standard-Werte wieder
    // herstellen zu können. Die Standard-Attribute ändern sich (OOo-seitig)
    // immer dann, wenn ein Dokument (mitsamt Fenster) geschlossen wird. Dann
    // merkt sich OOo die Position und Größe des zuletzt geschlossenen
    // Fensters.
    defaultWindowAttributes = this.documentController.getDefaultWindowAttributes();
  }

  /**
   * Versucht das Dokument zu schließen. Wurde das Dokument verändert (Modified-Status des
   * Dokuments==true), so erscheint der Dialog "Speichern"/"Verwerfen"/"Abbrechen" (über den ein
   * sofortiges Schließen des Dokuments durch den Benutzer verhindert werden kann)
   */
  public void close()
  {
    WollMuxEventHandler.getInstance().handleCloseTextDocument(documentController);
  }

  /**
   * Startet den Ausdruck unter Verwendung eventuell vorhandener Komfortdruckfunktionen.
   */
  public void print()
  {
    UNO.dispatch(documentController.getModel().doc, Dispatch.DISP_unoPrint);
  }

  /**
   * Exportiert das Dokument als PDF.
   */
  public void pdf()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:ExportToPDF");
  }

  /**
   * Speichert das Dokument (Datei/Speichern).
   */
  public void save()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:Save");
  }

  /**
   * Speichert das Dokument (Datei/Speichern unter...).
   */
  public void saveAs()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:SaveAs");
  }

  /**
   * Öffnet durch ACTION-Event ein neues Dokument oder Template. Durch Angabe der FragID wird die
   * entsprechende Vorlage zugeordnet.
   * 
   * @param fragIds
   *          Liste der zu öffnenden Vorlagen.
   */
  public void openTemplateOrDocument(List<String> fragIds)
  {
    WollMuxEventHandler.getInstance().handleOpenDocument(fragIds, false);
  }

  /**
   * Sendet das Dokument als Anhang.
   */
  public void sendAsEmail()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:SendMail");
  }

  /**
   * Speichert dieses Formular in eine temporäre Datei unter Verwendung des in ExterneAnwendungen
   * für ext festgelegten FILTERs, startet dann die zugehörige externe Anwendung mit dieser Datei
   * und schließt das Formular.
   * 
   * @param ext
   *          Der Filter.
   */
  public void closeAndOpenExt(String ext)
  {
    WollMuxEventHandler.getInstance().handleCloseAndOpenExt(documentController, ext);
  }

  /**
   * Speichert dieses Formular in eine temporäre Datei unter Verwendung des in ExterneAnwendungen
   * für ext festgelegten FILTERs und startet dann die zugehörige externe Anwendung mit dieser
   * Datei.
   * 
   * @param ext
   *          Der Filter.
   */
  public void saveTempAndOpenExt(String ext)
  {
    WollMuxEventHandler.getInstance().handleSaveTempAndOpenExt(documentController, ext);
  }

  /**
   * Das Formularfeld im Dokument mit der ID fieldId erhält den Fokus. Gibt es im Dokument mehrere
   * Formularfelder, die von der ID abhängen, so erhält immer das erste Formularfeld den Fokus -
   * bevorzugt werden dabei auch die nicht transformierten Formularfelder.
   * 
   * @param fieldId
   *          id des Formularfeldes, das den Fokus bekommen soll.
   */
  public void focusGained(String fieldId)
  {
    WollMuxEventHandler.getInstance().handleFocusFormField(documentController, fieldId);
  }

  /**
   * Not Yet Implemented: Nimmt dem Formularfeld mit der ID fieldId den Fokus wieder weg - ergibt
   * aber bisher keinen Sinn.
   * 
   * @param fieldId
   *          id des Formularfeldes, das den Fokus verlieren soll.
   */
  public void focusLost(String fieldId)
  {
    // wird bisher nicht benötigt.
  }

  /**
   * Informiert das FormModel, dass das zugrundeliegende Dokument source geschlossen wird und das
   * FormModel entsprechend handeln soll um sicherzustellen, dass das Dokument in Zukunft nicht mehr
   * angesprochen wird.
   * 
   * Abhängig von der Implementierung des FormModels werden unterschiedliche Aktionen erledigt. Dazu
   * gehören z.B. das Beenden einer bereits gestarteten FormGUI oder das Wiederherstellen der
   * Fensterattribute des Dokumentfensters auf die Werte, die das Fenster vor dem Starten der
   * FormGUI hatte.
   * 
   * @param source
   *          Das Dokument das geschlossen wurde.
   */
  public void closing(Object sender)
  {
    if (documentController.getModel().doc.equals(sender))
    {
      if (gui != null)
      {
        gui.dispose();
        gui = null;
      }

      // Rücksetzen des defaultWindowAttributes auf den Wert vor dem Schließen
      // des Formulardokuments.
      if (defaultWindowAttributes != null)
        documentController.setDefaultWindowAttributes(defaultWindowAttributes);
    }
  }

  /**
   * Setzt die Position und Größe des Fensters des zugehörigen Dokuments auf die vorgegebenen Werte
   * ACHTUNG: Die Maßangaben beziehen sich auf die linke obere Ecke des Fensterinhalts OHNE die
   * Titelzeile und die Fensterdekoration des Rahmens. Um die linke obere Ecke des gesamten Fensters
   * richtig zu setzen, müssen die Größenangaben des Randes der Fensterdekoration und die Höhe der
   * Titelzeile VOR dem Aufruf der Methode entsprechend eingerechnet werden.
   * 
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position in Pixel, gezählt
   *          von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gezählt von links oben.
   * @param docWidth
   *          Die Größe des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Größe des Dokuments auf der Y-Achse in Pixel. Auch hier wird die Titelzeile des
   *          Rahmens nicht beachtet und muss vorher entsprechend eingerechnet werden.
   */
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
  {
    WollMuxEventHandler.getInstance().handleSetWindowPosSize(documentController, docX, docY,
        docWidth,
          docHeight);
  }

  /**
   * Baut die GUI zusammen und zeigt diese an. Sobald, die GUI angezeigt wurde wird die Methode
   * {@link #formControllerInitCompleted()} aufgerufen.
   */
  public void startFormGUI()
  {
    Runnable runner = () -> {
      try
      {
        gui.create(model, true);
        formControllerInitCompleted();
      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    };
    gui.createGUI(runner);
  }

  /**
   * Setzt den Wert für das Formularfeld mit der ID fieldId im Model auf value.
   * 
   * @param fieldId
   *          Die ID des Formularfeldes.
   * @param value
   *          Der neue Wert für das Formularfeld.
   * @param listener
   *          Ein Listener, der ausgeführt wird, sobald der neue Wert gesetzt wurde.
   */
  public void setValue(String fieldId, String value, ActionListener listener)
  {
    model.setValue(fieldId, value);
    if (listener != null)
    {
      listener.actionPerformed(null);
    }
  }

  /**
   * Öffnet einen Funktionsdialog.
   * 
   * @param dialogName
   *          Der Name des Dialogs.
   */
  public void openDialog(String dialogName)
  {
    Dialog dlg = model.getDialogLib().get(dialogName);
    if (dlg == null)
    {
      LOGGER.error(L.m("Funktionsdialog \"%1\" ist nicht definiert", dialogName));
    }
    else
    {
      dlg.instanceFor(model.getFunctionContext()).show(e -> {
        if ("select".equals(e.getActionCommand()))
        {
          model.setDialogAutofills(dialogName);
        }
      }, model.getFuncLib(), model.getDialogLib());
    }
  }

  /**
   * Über diese Methode kann der FormController das FormModel informieren, dass er vollständig
   * initialisiert wurde und notwendige Aktionen wie z.B. das zurücksetzen des modified-Status des
   * Dokuments durchgeführt werden sollen.
   */
  public void formControllerInitCompleted()
  {
    WollMuxEventHandler.getInstance().handleFormControllerInitCompleted(documentController);
  }

  public void exportFormValues(File f) throws IOException
  {
    try (FileOutputStream out = new FileOutputStream(f))
    {
      documentController.getModel().exportFormValues(out);
    }
  }

  public void importFormValues(File f) throws IOException
  {
    documentController.getModel().importFormValues(f);
  }
}
