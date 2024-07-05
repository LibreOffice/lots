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
package org.libreoffice.lots.document.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextRange;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoCollection;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.document.commands.DocumentCommand.UpdateFields;

/**
 * Dieser Executor hat die Aufgabe alle updateFields-Befehle zu verarbeiten.
 */
class TextFieldUpdater extends AbstractExecutor
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TextFieldUpdater.class);

  /**
   *
   */
  private final DocumentCommandInterpreter documentCommandInterpreter;

  /**
   * @param documentCommandInterpreter
   */
  TextFieldUpdater(DocumentCommandInterpreter documentCommandInterpreter)
  {
    this.documentCommandInterpreter = documentCommandInterpreter;
  }

  /**
   * Ausführung starten
   */
  int execute(DocumentCommands commands)
  {
    return executeAll(commands);
  }

  /**
   * Diese Methode updated alle TextFields, die das Kommando cmd umschließt.
   */
  @Override
  public int executeCommand(UpdateFields cmd)
  {
    XTextRange range = cmd.getTextCursor();
    if (range != null)
    {
      updateTextFieldsRecursive(range);
    }
    cmd.markDone(!this.documentCommandInterpreter.debugMode);
    return 0;
  }

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextFeldern und ruft deren Methode update() auf.
   *
   * @param element
   *          Das Element das geupdated werden soll.
   */
  private void updateTextFieldsRecursive(Object element)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    UnoCollection<Object> children = UnoCollection.getCollection(element, Object.class);
    if (children != null)
    {
      for (Object child : children)
      {
        try
        {
          updateTextFieldsRecursive(child);
        }
        catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
      }
    }

    // jetzt noch update selbst aufrufen (wenn verfügbar):
    try
    {
      Object textField = UnoProperty.getProperty(element, UnoProperty.TEXT_FIELD);
      if (textField != null && UNO.XUpdatable(textField) != null)
      {
        UNO.XUpdatable(textField).update();
      }
    }
    catch (UnoHelperException e)
    {
      LOGGER.trace("", e);
    }
  }
}
