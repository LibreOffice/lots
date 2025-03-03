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

import org.libreoffice.lots.document.commands.DocumentCommand.SetPrintFunction;
import org.libreoffice.lots.document.commands.DocumentCommand.SetType;

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
