package de.muenchen.allg.itd51.wollmux.document.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.util.UnoProperty;

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