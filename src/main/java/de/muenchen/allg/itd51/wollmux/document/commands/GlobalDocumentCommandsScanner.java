package de.muenchen.allg.itd51.wollmux.document.commands;

import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;

/**
 * Hierbei handelt es sich um einen minimalen Scanner, der zu aller erst abläuft
 * und die globalen Einstellungen des Dokuments (setType, setPrintFunction)
 * ausliest und dem TextDocumentModel zur Verfügung stellt.
 * 
 * @author christoph.lutz
 */
class GlobalDocumentCommandsScanner extends AbstractExecutor
{
  /**
   * 
   */
  private final DocumentCommandInterpreter documentCommandInterpreter;

  /**
   * @param documentCommandInterpreter
   */
  GlobalDocumentCommandsScanner(
      DocumentCommandInterpreter documentCommandInterpreter)
  {
    this.documentCommandInterpreter = documentCommandInterpreter;
  }

  public int execute(DocumentCommands commands)
  {
    return executeAll(commands);
  }

  /**
   * Legacy-Methode: Stellt sicher, dass die im veralteten Dokumentkommando
   * SetPrintFunction gesetzten Werte in die persistenten Daten des Dokuments
   * übertragen werden und das Dokumentkommando danach gelöscht wird.
   */
  @Override
  public int executeCommand(SetPrintFunction cmd)
  {
    this.documentCommandInterpreter.getDocumentController().addPrintFunction(cmd.getFunctionName());
    cmd.markDone(true);
    return 0;
  }

  @Override
  public int executeCommand(SetType cmd)
  {
    this.documentCommandInterpreter.getModel().setType(cmd.getType());

    // Wenn eine Mischvorlage zum Bearbeiten geöffnet wurde soll der typ
    // "templateTemplate" NICHT gelöscht werden, ansonsten schon.
    if (!(this.documentCommandInterpreter.getModel().hasURL() && cmd.getType().equalsIgnoreCase("templateTemplate")))
      cmd.markDone(true);
    return 0;
  }

}