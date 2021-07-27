/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.comp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.star.beans.PropertyValue;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.SyncActionListener;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnManagePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValue;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetInsertValues;
import de.muenchen.allg.itd51.wollmux.interfaces.XWollMuxDocument;

/**
 * A WollMux document.
 */
public class WollMuxDocument implements XWollMuxDocument
{
  private XTextDocument doc;

  private HashMap<String, String> mapDbSpalteToValue;

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
    new OnSetFormValue(doc, id, value, null).emit();
  }

  @Override
  public void setInsertValue(String dbSpalte, String value)
  {
    mapDbSpalteToValue.put(dbSpalte, value);
  }

  /**
   * Does nothing.
   */
  @Override
  public void updateFormGUI()
  {
    // not used.
  }

  @Override
  public void updateInsertFields()
  {
    Map<String, String> m = new HashMap<>(mapDbSpalteToValue);
    mapDbSpalteToValue.clear();
    new OnSetInsertValues(doc, m, null).emit();
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
