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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextCursor;

import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoProperty;

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
          Utils.setProperty(cursor, UnoProperty.CHAR_BACK_COLOR, bgColor);
          cursor.collapseToEnd();
          UnoProperty.setPropertyToDefault(cursor, UnoProperty.CHAR_BACK_COLOR);
        } catch (NumberFormatException | UnoHelperException e)
        {
          LOGGER.error(L.m(
              "Fehler in Dokumentkommando '%1': Die Farbe HIGHLIGHT_COLOR mit dem Wert '%2' ist ungültig.",
              "" + this, highlightColor));
        }
      }
      else
      {
        try
        {
          UnoProperty.setPropertyToDefault(getTextCursor(), UnoProperty.CHAR_BACK_COLOR);
        } catch (UnoHelperException e)
        {
          LOGGER.error("Couldn't set background color.", e);
        }
      }
    }
  }
}
