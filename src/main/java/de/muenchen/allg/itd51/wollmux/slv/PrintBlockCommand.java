package de.muenchen.allg.itd51.wollmux.slv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextCursor;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.PropertyName;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

/**
 * Implementation of print block commands {@link PrintBlockSignature}.
 */
public class PrintBlockCommand extends DocumentCommand
{

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintBlockCommand.class);

  /**
   * The background color given by the command.
   */
  private String highlightColor = null;

  /**
   * The command.
   */
  private PrintBlockSignature name;

  /**
   * Create a new command.
   *
   * @param wmCmd
   *          The definition of the command.
   * @param bookmark
   *          The bookmark on which the command operates.
   * @throws InvalidCommandException
   *           The command wasn't a print blocj command.
   */
  public PrintBlockCommand(ConfigThingy wmCmd, Bookmark bookmark) throws InvalidCommandException
  {
    super(wmCmd, bookmark);
    try
    {
      name = PrintBlockSignature.valueOfIgnoreCase(wmCmd.getString("CMD", ""));
    } catch (IllegalArgumentException ex)
    {
      throw new InvalidCommandException("Unknown command.", ex);
    }

    try
    {
      highlightColor = wmCmd.get("WM").get("HIGHLIGHT_COLOR").toString();
    } catch (NodeNotFoundException e)
    {
      // HIGHLIGHT_COLOR is optional
    }
  }

  public String getHighlightColor()
  {
    return highlightColor;
  }

  public PrintBlockSignature getName()
  {
    return name;
  }

  @Override
  public int execute(DocumentCommand.Executor visitable)
  {
    return visitable.executeCommand(this);
  }

  /**
   * If there is a {@link #highlightColor}, sets the background color.
   *
   * @param showHighlightColor
   *          If true use the value of {@link #highlightColor}, otherwise set the default value.
   */
  public void showHighlightColor(boolean showHighlightColor)
  {
    if (highlightColor != null)
    {
      if (showHighlightColor)
      {
        try
        {
          Integer bgColor = Integer.valueOf(Integer.parseInt(highlightColor, 16));
          XTextCursor cursor = getTextCursor();
          Utils.setProperty(cursor, PropertyName.CHAR_BACK_COLOR, bgColor);
          cursor.collapseToEnd();
          UNO.setPropertyToDefault(cursor, PropertyName.CHAR_BACK_COLOR);
        } catch (NumberFormatException e)
        {
          LOGGER.error(L.m(
              "Fehler in Dokumentkommando '%1': Die Farbe HIGHLIGHT_COLOR mit dem Wert '%2' ist ungültig.",
              "" + this, highlightColor));
        }
      }
      else
      {
        UNO.setPropertyToDefault(getTextCursor(), PropertyName.CHAR_BACK_COLOR);
      }
    }
  }
}
