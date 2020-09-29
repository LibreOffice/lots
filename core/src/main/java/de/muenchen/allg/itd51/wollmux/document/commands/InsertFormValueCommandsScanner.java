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
package de.muenchen.allg.itd51.wollmux.document.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.document.FormFieldFactory;
import de.muenchen.allg.itd51.wollmux.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.InsertFormValue;

/**
 * Scanner, der die InsertFormValue-Kommandos des Dokuments abarbeitet und ein
 * Mapping von IDs zu FormFields aufbaut, das dann dem TextDocumentModel zur
 * Verfügung gestellt werden kann.
 */
class InsertFormValueCommandsScanner extends AbstractExecutor
{
  /**
   * 
   */
  private final DocumentCommandInterpreter documentCommandInterpreter;

  /**
   * @param documentCommandInterpreter
   */
  InsertFormValueCommandsScanner(
      DocumentCommandInterpreter documentCommandInterpreter)
  {
    this.documentCommandInterpreter = documentCommandInterpreter;
  }

  /** Mapping von IDs zu FormFields */
  public HashMap<String, List<FormField>> idToFormFields =
    new HashMap<String, List<FormField>>();

  private Map<String, FormField> bookmarkNameToFormField =
    new HashMap<String, FormField>();

  public int execute(DocumentCommands commands)
  {
    return executeAll(commands);
  }

  @Override
  public int executeCommand(InsertFormValue cmd)
  {
    // idToFormFields aufbauen
    String id = cmd.getID();
    LinkedList<FormField> fields;
    if (idToFormFields.containsKey(id))
    {
      fields = (LinkedList<FormField>) idToFormFields.get(id);
    }
    else
    {
      fields = new LinkedList<FormField>();
      idToFormFields.put(id, fields);
    }
    FormField field =
      FormFieldFactory.createFormField(this.documentCommandInterpreter.getModel().doc, cmd, bookmarkNameToFormField);

    if (field != null)
    {
      // sortiertes Hinzufügen des neuen FormFields zur Liste:
      ListIterator<FormField> iter = fields.listIterator();
      while (iter.hasNext())
      {
        FormField fieldA = iter.next();
        if (field.compareTo(fieldA) < 0)
        {
          iter.previous();
          break;
        }
      }
      iter.add(field);
    }

    return 0;
  }

}