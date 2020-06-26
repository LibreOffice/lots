/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.form.control;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.dispatch.PrintDispatch;
import de.muenchen.allg.itd51.wollmux.dispatch.SaveDispatch;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseAndOpenExt;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFocusFormField;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpenDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnResetDocumentState;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveTempAndOpenExt;
import de.muenchen.allg.itd51.wollmux.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.util.L;

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
   * Das Model, das diesem Controller zugeordnet ist.
   */
  private FormModel model;

  /**
   * Der LibreOffice-Controller für das Writer-Dokument.
   */
  private final TextDocumentController documentController;

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
  public FormController(final FormModel model, final ConfigThingy formFensterConf,
      final TextDocumentController documentController)
  {
    this.model = model;
    this.documentController = documentController;
  }

  /**
   * Versucht das Dokument zu schließen. Wurde das Dokument verändert (Modified-Status des
   * Dokuments==true), so erscheint der Dialog "Speichern"/"Verwerfen"/"Abbrechen" (über den ein
   * sofortiges Schließen des Dokuments durch den Benutzer verhindert werden kann)
   */
  public void close()
  {
    new OnCloseTextDocument(documentController).emit();
  }

  /**
   * Startet den Ausdruck unter Verwendung eventuell vorhandener Komfortdruckfunktionen.
   */
  public void print()
  {
    UNO.dispatch(documentController.getModel().doc, PrintDispatch.COMMAND);
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
    UNO.dispatch(documentController.getModel().doc, SaveDispatch.COMMAND_SAVE);
  }

  /**
   * Speichert das Dokument (Datei/Speichern unter...).
   */
  public void saveAs()
  {
    UNO.dispatch(documentController.getModel().doc, SaveDispatch.COMMAND_SAVE_AS);
  }

  /**
   * Öffnet durch ACTION-Event ein neues Dokument oder Template. Durch Angabe
   * der FragID wird die entsprechende Vorlage zugeordnet.
   *
   * @param fragIds
   *          Liste der zu öffnenden Vorlagen.
   * @param asTemplate
   *          Open the document as a template.
   */
  public void openTemplateOrDocument(List<String> fragIds, boolean asTemplate)
  {
    new OnOpenDocument(fragIds, asTemplate).emit();
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
    new OnCloseAndOpenExt(documentController, ext).emit();
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
    new OnSaveTempAndOpenExt(documentController, ext).emit();
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
    new OnFocusFormField(documentController, fieldId).emit();
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
    new OnResetDocumentState(documentController).emit();
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

  /**
   * Besitzt das Formular, welches von diesem Controller verwaltet wird, ein Feld mit der ID
   * fieldId?
   *
   * @param fieldId
   *          Die ID des gesuchten Feldes.
   * @return True falls ein solches Feld existiert, sonst False.
   */
  public boolean hasFieldId(String fieldId)
  {
    return model.hasFieldId(fieldId);
  }
}
