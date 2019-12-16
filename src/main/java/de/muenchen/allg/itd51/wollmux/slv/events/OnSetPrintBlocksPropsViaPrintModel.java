package de.muenchen.allg.itd51.wollmux.slv.events;

import java.awt.event.ActionListener;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.handlers.BasicEvent;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockCommand;

/**
 * Creates a new event for setting properties of print blocks (e.g. allVersions).
 *
 * It's used by {@link XPrintModel}.
 */
public class OnSetPrintBlocksPropsViaPrintModel extends BasicEvent
{

  private TextDocumentController documentController;

  private String blockName;

  private boolean visible;

  private boolean showHighlightColor;

  private ActionListener listener;

  /**
   * Create this event.
   *
   * @param documentController
   *          The controller of the current document.
   * @param blockName
   *          The name of the block to modify.
   * @param visible
   *          True if the block should be visible, false otherwise.
   * @param showHighlightColor
   *          If true, the background color is set, false no background color is set.
   * @param listener
   *          The listener to call as soon as this event is completed.
   */
  public OnSetPrintBlocksPropsViaPrintModel(TextDocumentController documentController,
      String blockName, boolean visible,
      boolean showHighlightColor, ActionListener listener)
  {
    this.documentController = documentController;
    this.blockName = blockName;
    this.visible = visible;
    this.showHighlightColor = showHighlightColor;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentModel model = documentController.getModel();
    model.updateLastTouchedByVersionInfo();

    Set<PrintBlockCommand> commands = model.getDocumentCommands().printBlockCommands();
    commands.stream().filter(cmd -> blockName.equals(cmd.getName().getName())).forEach(cmd -> {
      cmd.setVisible(visible);
      cmd.showHighlightColor(showHighlightColor);
    });

    if (listener != null)
    {
      listener.actionPerformed(null);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().doc.hashCode()
        + ", '" + blockName + "', '" + visible + "', '" + showHighlightColor + "')";
  }
}