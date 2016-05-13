package de.muenchen.allg.itd51.wollmux.document.commands;

import com.sun.star.container.XEnumeration;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Dieser Executor hat die Aufgabe alle updateFields-Befehle zu verarbeiten.
 */
class TextFieldUpdater extends AbstractExecutor
{
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
    try
    {
      this.documentCommandInterpreter.getDocumentController().setLockControllers(true);

      int errors = executeAll(commands);

      return errors;
    }
    finally
    {
      this.documentCommandInterpreter.getDocumentController().setLockControllers(false);
    }

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
      UnoService cursor = new UnoService(range);
      updateTextFieldsRecursive(cursor);
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
  private void updateTextFieldsRecursive(UnoService element)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    if (element.xEnumerationAccess() != null)
    {
      XEnumeration xEnum = element.xEnumerationAccess().createEnumeration();

      while (xEnum.hasMoreElements())
      {
        try
        {
          UnoService child = new UnoService(xEnum.nextElement());
          updateTextFieldsRecursive(child);
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    // jetzt noch update selbst aufrufen (wenn verfügbar):
    try
    {
      UnoService textField = element.getPropertyValue("TextField");
      if (textField != null && textField.xUpdatable() != null)
      {
        textField.xUpdatable().update();
      }
    }
    catch (Exception e)
    {}
  }
}