/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.document.XEventListener;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Event for importing content of formular.
 */
public class OnImportFormularinhalt extends WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnImportFormularinhalt.class);

  private TextDocumentController documentController;
  /**
   * Create this event.
   *
   * @param documentController
   *          The document associated with the FormularMax.
   */
  public OnImportFormularinhalt(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XFilePicker3 picker = FilePicker.createWithMode(UNO.defaultContext,
        TemplateDescription.FILEOPEN_SIMPLE);
    String filterName = "Tabellendokument";
    picker.appendFilter(filterName, "*.odt");
    picker.appendFilter("Alle Dateien", "*");
    picker.setCurrentFilter(filterName);
    picker.setMultiSelectionMode(false);

    short res = picker.execute();
    if (res != com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
      return;

    String[] files = picker.getFiles();
    try
    {
      XTextDocument importDoc = UNO.XTextDocument(UNO.loadComponentFromURL(files[0], false, false, true));

      XEventListener listener = new XEventListener()
      {
        @Override
        public void disposing(EventObject arg0)
        {
          // nothing to do
        }

        private void setFormValue(Entry<String, String> e)
        {
          if (e.getValue() == null)
            return;
          new OnSetFormValue(documentController.getModel().doc, e.getKey(), e.getValue(), null).emit();
        }

        private List<String> keysNotInList(Map<String, String> id2value, Map<String, List<FormField>> id2valueTo)
        {
          List<String> notFound = new ArrayList<>();

          for (Entry<String, String> e : id2value.entrySet())
          {
            if (id2valueTo.containsKey(e.getKey()))
            {
              setFormValue(e);
            } else
            {
              notFound.add(e.getKey());
            }
          }
          if (!notFound.isEmpty())
          {
            String msg = L.m("Die folgenden Schlüssel können nicht importiert werden:\n") + String.join("\n", notFound);

            InfoDialog.showInfoModal(L.m("WollMux"), msg);
          }

          return notFound;
        }

        @Override
        public void notifyEvent(com.sun.star.document.EventObject event)
        {
          if (importDoc!=null
              && UnoRuntime.areSame(importDoc, event.Source)
              && WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED.equals(event.EventName))
          {

            new OnRemoveDocumentEventListener(this).emit();
            try(ByteArrayOutputStream out = new ByteArrayOutputStream())
            {
              TextDocumentController importDocumentController =
                  DocumentManager.getTextDocumentController(importDoc);
              Map<String, String> id2value = importDocumentController.getModel().getFormFieldValuesMap();
              Map<String, List<FormField>> id2valueTo = documentController.getModel().getIdToFormFields();
              keysNotInList(id2value, id2valueTo);

            } catch(Exception e)
            {
              LOGGER.error("", e);
              InfoDialog.showInfoModal(L.m("WollMux-Fehler"), e.getMessage());
            }
          }
        }
      };

      new OnAddDocumentEventListener(listener).emit();
    } catch ( UnoHelperException  e )
    {
      LOGGER.error("", e);
    }
 }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
