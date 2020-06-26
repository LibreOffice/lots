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

import com.sun.star.text.XTextCursor;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.util.L;

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
    return executeAll(commands);
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
      XTextCursor cursor = cmd.getTextCursor();
      String text = cmd.getLeftSeparator() + value + cmd.getRightSeparator();
      cmd.getAnchor().getStart().setString(text);
      cursor.setString("");
    }

    cmd.markDone(false);
    return 0;
  }
}