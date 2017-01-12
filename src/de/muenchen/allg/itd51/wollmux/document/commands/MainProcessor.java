package de.muenchen.allg.itd51.wollmux.document.commands;

import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;

/**
 * Der Hauptverarbeitungsschritt, in dem vor allem die Textinhalte gefüllt werden.
 * 
 * @author christoph.lutz
 * 
 */
class MainProcessor extends AbstractExecutor
{
  /**
   * 
   */
  private final DocumentCommandInterpreter documentCommandInterpreter;

  /**
   * @param documentCommandInterpreter
   */
  MainProcessor(DocumentCommandInterpreter documentCommandInterpreter)
  {
    this.documentCommandInterpreter = documentCommandInterpreter;
  }

  /**
   * Hauptverarbeitungsschritt starten.
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
   * Besitzt das Dokument ein (inzwischen veraltetes) Form-Dokumentkommando, so
   * wird dieses in die persistenten Daten des Dokuments kopiert und die zugehörige
   * Notiz gelöscht, wenn nicht bereits eine Formularbeschreibung dort existiert.
   */
  @Override
  public int executeCommand(DocumentCommand.Form cmd)
  {
    cmd.setErrorState(false);
    try
    {
      this.documentCommandInterpreter.getDocumentController().addToCurrentFormDescription(cmd);
    }
    catch (ConfigurationErrorException e)
    {
      AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, e);
      cmd.setErrorState(true);
      return 1;
    }
    cmd.markDone(!this.documentCommandInterpreter.debugMode);
    return 0;
  }

  /**
   * Diese Methode bearbeitet ein InvalidCommand und fügt ein Fehlerfeld an der
   * Stelle des Dokumentkommandos ein.
   */
  @Override
  public int executeCommand(DocumentCommand.InvalidCommand cmd)
  {
    AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, cmd.getException());
    cmd.setErrorState(true);
    return 1;
  }

  /**
   * Diese Methode fügt einen Spaltenwert aus dem aktuellen Datensatz der
   * Absenderdaten ein. Im Fehlerfall wird die Fehlermeldung eingefügt.
   */
  @Override
  public int executeCommand(DocumentCommand.InsertValue cmd)
  {
    cmd.setErrorState(false);

    String spaltenname = cmd.getDBSpalte();
    String value = null;
    try
    {
      Dataset ds = DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDatasetTransformed();
      value = ds.get(spaltenname);
      if (value == null) value = "";

      // ggf. TRAFO durchführen
      value = this.documentCommandInterpreter.getDocumentController().getTransformedValue(cmd.getTrafoName(), value);
    }
    catch (DatasetNotFoundException e)
    {
      value = L.m("<FEHLER: Kein Absender ausgewählt!>");
    }
    catch (ColumnNotFoundException e)
    {
      AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, e);
      cmd.setErrorState(true);
      return 1;
    }

    if (value == null || value.equals(""))
    {
      cmd.setTextRangeString("");
    }
    else
    {
      cmd.setTextRangeString(cmd.getLeftSeparator() + value
        + cmd.getRightSeparator());
    }

    cmd.markDone(false);
    return 0;
  }
}