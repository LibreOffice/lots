/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.comp;

import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.text.XTextDocument;

import org.libreoffice.ext.unohelper.common.UnoProps;
import org.libreoffice.lots.SyncActionListener;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.document.DocumentManager;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.document.TextDocumentModel;
import org.libreoffice.lots.document.WMCommandsFailedException;
import org.libreoffice.lots.document.commands.DocumentCommandInterpreter;
import org.libreoffice.lots.event.handlers.OnManagePrintFunction;
import org.libreoffice.lots.event.handlers.OnSetFormValue;
import org.libreoffice.lots.event.handlers.OnSetInsertValues;
import org.libreoffice.lots.form.model.Control;
import org.libreoffice.lots.form.model.FormModel;
import org.libreoffice.lots.form.model.FormModelException;

import de.muenchen.allg.itd51.wollmux.interfaces.XWollMuxDocument;

/**
 * A WollMux document.
 */
public class WollMuxDocument implements XWollMuxDocument
{
  private XTextDocument doc;

  private HashMap<String, String> mapDbSpalteToValue;

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxDocument.class);

  /**
   * A new WollMux document.
   *
   * @param doc
   *          The UNO document.
   */
  public WollMuxDocument(XTextDocument doc)
  {
    this.doc = doc;
    this.mapDbSpalteToValue = new HashMap<>();
  }

  @Override
  public void addPrintFunction(String functionName)
  {
    new OnManagePrintFunction(doc, functionName, false).emit();
  }

  @Override
  public void removePrintFunction(String functionName)
  {
    new OnManagePrintFunction(doc, functionName, true).emit();
  }

  @Override
  public void setFormValue(String id, String value)
  {
    SyncActionListener s = new SyncActionListener();
    new OnSetFormValue(doc, id, value, s).emit();
    s.synchronize();
  }

  @Override
  public void setInsertValue(String dbSpalte, String value)
  {
    mapDbSpalteToValue.put(dbSpalte, value);
  }

  /*
   * Triggers update form gui values, updates the document as well.
   * Is currently used by external Applications which use the wollmux instance only!
   *
   * @{link TextDocumentController.getFormModel()} triggers updating formGui values.
   * On first instance called by external Application form model will be null, an instance of form model will be created.
   * If not NULL, multiple listeners are already registered in @{link FormModel} which notifies UI and document.
   * External Applications set FormGUI-Values with @{link WollMuxDocument.setFormValue()},
   * setFormValue() writes both directly to rdf file and @{link FormModel}, so getFormModel()
   * will return *the* model as it was set by the last call of setFormValue().
   */
  @Override
  public void updateFormGUI()
  {
    TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);

    if (documentController == null)
    {
      LOGGER.error("documentController is NULL. updating form gui failed.");
      return;
    }

    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(
        documentController, WollMuxFiles.isDebugMode());

    try
    {
      dci.executeTemplateCommands();
      dci.scanGlobalDocumentCommands();
      dci.scanInsertFormValueCommands();

      documentController.getFormModel();

      Map<String,String> formFieldValues = documentController.getFormFieldValues();

      for (Map.Entry<String, String> entry: formFieldValues.entrySet())
      {
        documentController.updateDocumentFormFields(entry.getKey());
      }
    } catch (WMCommandsFailedException | FormModelException e)
    {
      LOGGER.error("", e);
    }
  }

  @Override
  public void updateInsertFields()
  {
    Map<String, String> m = new HashMap<>(mapDbSpalteToValue);
    mapDbSpalteToValue.clear();
    SyncActionListener s = new SyncActionListener();
    new OnSetInsertValues(doc, m, s).emit();
    s.synchronize();
  }

  @Override
  public PropertyValue[] getFormValues()
  {
    UnoProps p = new UnoProps();
    TextDocumentModel model = DocumentManager.getTextDocumentController(doc).getModel();
    Map<String, String> id2value = model.getFormFieldValuesMap();
    for (Entry<String, String> e : id2value.entrySet())
    {
      if (e.getValue() != null)
      {
        p.setPropertyValue(e.getKey(), e.getValue());
      }
    }
    return p.getProps();
  }
}
