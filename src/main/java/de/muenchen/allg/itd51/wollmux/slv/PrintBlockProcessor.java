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
package de.muenchen.allg.itd51.wollmux.slv;

import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;

/**
 * Processing step for print block commands.
 */
public class PrintBlockProcessor extends AbstractExecutor
{

  /**
   * Create new processor of print block commands.
   */
  public PrintBlockProcessor()
  {
    // nothing to do
  }

  /**
   * Start processing
   *
   * @param commands
   *          Process these commands
   * @return The number of errors.
   */
  public int execute(DocumentCommands commands)
  {
    return executeAll(commands);
  }

  @Override
  public int executeCommand(PrintBlockCommand cmd)
  {
    cmd.showHighlightColor(true);
    cmd.markDone(false);
    cmd.setErrorState(false);
    return 0;
  }
}