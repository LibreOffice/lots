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