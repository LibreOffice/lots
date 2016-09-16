package de.muenchen.allg.itd51.wollmux.document.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;

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
      field.setCommand(cmd);

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