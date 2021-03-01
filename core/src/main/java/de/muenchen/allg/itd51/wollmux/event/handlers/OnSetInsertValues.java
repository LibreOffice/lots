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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand;

/**
 * Updates all insertValue commands, even already executed commands.
 */
public class OnSetInsertValues extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnSetInsertValues.class);

  private XTextDocument doc;

  private Map<String, String> mapDbSpalteToValue;

  private ActionListener listener;

  /**
   * Create this event
   *
   * @param doc
   *          The document.
   * @param mapDbSpalteToValue
   *          Mapping of columns to values. These are the values, which are inserted in the
   *          document. If there is a transformation, it is executed.
   * @param unlockActionListener
   *          Listener to notify after completion.
   */
  public OnSetInsertValues(XTextDocument doc,
      Map<String, String> mapDbSpalteToValue,
      ActionListener unlockActionListener)
  {
    this.doc = doc;
    this.mapDbSpalteToValue = mapDbSpalteToValue;
    this.listener = unlockActionListener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController = DocumentManager
        .getTextDocumentController(doc);

    for (DocumentCommand cmd : documentController.getModel().getDocumentCommands())
    {
      try
      {
        if (cmd instanceof DocumentCommand.InsertValue)
        {
          DocumentCommand.InsertValue insVal = (DocumentCommand.InsertValue) cmd;
          String value = mapDbSpalteToValue.get(insVal.getDBSpalte());
          if (value != null)
          {
            value = documentController.getTransformedValue(insVal.getTrafoName(), value);
            if ("".equals(value))
            {
              cmd.setTextRangeString("");
            } else
            {
              cmd.setTextRangeString(
                  insVal.getLeftSeparator() + value + insVal.getRightSeparator());
            }
          }
        }
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
      }
    }

    if (listener != null)
    {
      listener.actionPerformed(null);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode()
        + ", Nr.Values=" + mapDbSpalteToValue.size() + ")";
  }
}
