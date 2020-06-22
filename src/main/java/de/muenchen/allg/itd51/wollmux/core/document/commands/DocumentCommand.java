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
package de.muenchen.allg.itd51.wollmux.core.document.commands;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockCommand;

/**
 * A document command with its properties like visibility, execution state and groups.
 */
public abstract class DocumentCommand
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentCommand.class);

  private static final String STATE = "STATE";

  private static final String ERROR_STRING = "ERROR";

  private static final String INSERT_MARK_OPEN = "<";

  private static final String INSERT_MARK_CLOSE = ">";

  /**
   * Init value of state {@link #done}.
   */
  private static final Boolean STATE_DEFAULT_DONE = Boolean.FALSE;

  /**
   * Init value of state {@link #error}.
   */
  private static final Boolean STATE_DEFAULT_ERROR = Boolean.FALSE;

  /**
   * The configuration of the book mark.
   */
  protected ConfigThingy wmCmd;

  /**
   * The book mark in the document.
   */
  private Bookmark bookmark;

  /**
   * The done state of the command or null if unmodified.
   */
  private Boolean done;

  /**
   * The error state of the command or null if unmodified.
   */
  private Boolean error;

  /**
   * Is the text content of the command visible? It's not persisted in the book mar.
   */
  private boolean visible = true;

  /**
   * Has a cursor to put the content.
   */
  private boolean hasInsertMarks;

  /**
   * New command.
   *
   * @param wmCmd
   *          The configuration of the command.
   * @param bookmark
   *          The book mark of the command.
   */
  protected DocumentCommand(ConfigThingy wmCmd, Bookmark bookmark)
  {
    this.wmCmd = wmCmd;
    this.bookmark = bookmark;
    this.hasInsertMarks = false;

    // Warn if groups is available although it's unsupported
    if (wmCmd.query("GROUPS").count() > 0 && !canHaveGroupsAttribute())
    {
      LOGGER.error("Das Dokumentkommando '{}' darf kein GROUPS-Attribut besitzen.", getBookmarkName());
    }
  }

  /**
   * Does this command support "GROUPS"-property?
   *
   * @return True if it supports GROUPS, false otherwise. Default is false.
   */
  protected boolean canHaveGroupsAttribute()
  {
    return false;
  }

  /**
   * Does this command insert content?
   *
   * @return True if it inserts content, false otherwise. Default is false.
   */
  protected boolean insertsTextContent()
  {
    return false;
  }

  /**
   * Get the name of the book mark.
   *
   * @return The name of the book mark.
   */
  public String getBookmarkName()
  {
    return bookmark.getName();
  }

  @Override
  public String toString()
  {
    return "" + this.getClass().getSimpleName() + "[" + (isRetired() ? "RETIRED:" : "") + (isDone() ? "DONE:" : "")
        + getBookmarkName() + "]";
  }

  /**
   * Executes the command. (callback)
   *
   * @param executor
   *          The caller of this method.
   * @return Number of errors during execution.
   */
  public abstract int execute(DocumentCommand.Executor executor);

  /**
   * Get the TextCursor of the book mark. It's the best way to iterate through a book mark
   * (OOo-Issue #67869). If you like to change the content use {@link #setTextRangeString(String)},
   * {@link #insertTextContentIntoBookmark(XTextContent, boolean)} or
   * {@link #getTextCursorWithinInsertMarks()}.
   *
   * @return The TextCursor of the book mark or null if the book mark doesn't exist.
   */
  public XTextCursor getTextCursor()
  {
    try
    {
      XTextCursor cursor = bookmark.getTextCursor();
      if (cursor == null)
      {
        LOGGER.debug(
            "Kann keinen Textcursor erstellen für Dokumentkommando '{}'\nIst das Bookmark vielleicht verschwunden?",
            this);
      }
      return cursor;
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return null;
    }
  }

  /**
   * Get the TextRange of the book mark. If you like to change the content use
   * {@link #setTextRangeString(String)},
   * {@link #insertTextContentIntoBookmark(XTextContent, boolean)} or
   * {@link #getTextCursorWithinInsertMarks()}.
   *
   * @return The TextRange of the book mark or null if the book mark doesn't exist.
   *
   * @see de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement#getAnchor()
   */
  public XTextRange getAnchor()
  {
    try
    {
      return bookmark.getAnchor();
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return null;
    }
  }

  /**
   * Get the TextCursor of the book mark without expansion. The book mark is decolapsed and its
   * content is replaced with {@link #INSERT_MARK_OPEN} and {@link #INSERT_MARK_CLOSE}. The cursor
   * is between the two marks.
   *
   * @return The TextCursor of the book mark or null if the book mark doesn't exist.
   */
  public XTextCursor getTextCursorWithinInsertMarks()
  {
    // add insert marks
    this.setTextRangeString(INSERT_MARK_OPEN + INSERT_MARK_CLOSE);
    hasInsertMarks = true;

    XTextCursor cursor = this.getTextCursor();
    if (cursor != null)
    {
      cursor.goRight(getStartMarkLength(), false);
      cursor.collapseToStart();
    }

    return cursor;
  }

  /**
   * Replace to content of the TextRange and collapse or decollapse the book mark.
   *
   * @param text
   *          The content. If null or empty the book mark is collapsed.
   */
  public void setTextRangeString(String text)
  {
    try
    {
      if (text != null && text.length() > 0)
      {
        bookmark.decollapseBookmark();
      }

      XTextRange range = bookmark.getAnchor();
      if (range != null)
      {
        range.setString(text);
      }

      if (text == null || text.length() == 0)
      {
        bookmark.collapseBookmark();
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }

  /**
   * Add or replace to content of the TextRange and decollapse the book mark if necessary.
   *
   * @param textContent
   *          The content.
   * @param replace
   *          If True, the content is replaced, otherwise it is appended at the end.
   */
  public void insertTextContentIntoBookmark(XTextContent textContent, boolean replace)
  {
    try
    {
      if (textContent != null)
      {
        bookmark.decollapseBookmark();
        XTextCursor cursor = bookmark.getTextCursor();
        if (cursor != null)
        {
          XText text = cursor.getText();
          text.insertTextContent(cursor, textContent, replace);
        }
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
    }
  }

  /**
   * Get the length of {@link #INSERT_MARK_CLOSE}.
   *
   * @return The length.
   */
  public short getEndMarkLength()
  {
    return (short) INSERT_MARK_CLOSE.length();
  }

  /**
   * Get the length of {@link #INSERT_MARK_OPEN}.
   *
   * @return The length.
   */
  public short getStartMarkLength()
  {
    return (short) INSERT_MARK_OPEN.length();
  }

  /**
   * Get the marks left and right of {@link #INSERT_MARK_OPEN}.
   *
   * @return An array with two cursors if the command has insert marks. Otherwise an empty array.
   */
  public XParagraphCursor[] getStartMark()
  {
    try
    {
      XTextRange range = bookmark.getTextCursor();
      if (range == null || !hasInsertMarks)
      {
        return new XParagraphCursor[] {};
      }
      XParagraphCursor[] cursor = new XParagraphCursor[2];
      XText text = range.getText();
      cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range.getStart()));
      cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
      cursor[1].goRight(getStartMarkLength(), false);
      return cursor;
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return new XParagraphCursor[] {};
    }
  }

  /**
   * Get the marks left and right of {@link #INSERT_MARK_CLOSE}.
   *
   * @return An array with two cursors if the command has insert marks. Otherwise an empty array.
   */
  public XParagraphCursor[] getEndMark()
  {
    try
    {
      XTextRange range = bookmark.getTextCursor();
      if (range == null || !hasInsertMarks)
      {
        return new XParagraphCursor[] {};
      }
      XParagraphCursor[] cursor = new XParagraphCursor[2];
      XText text = range.getText();
      cursor[0] = UNO.XParagraphCursor(text.createTextCursorByRange(range.getEnd()));
      cursor[1] = UNO.XParagraphCursor(text.createTextCursorByRange(cursor[0]));
      cursor[0].goLeft(getStartMarkLength(), false);
      return cursor;
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return new XParagraphCursor[] {};
    }
  }

  /**
   * Has the command insert marks created with {@link #getTextCursorWithinInsertMarks()}.
   *
   * @return True if it has insert marks, false otherwise.
   */
  public boolean hasInsertMarks()
  {
    return hasInsertMarks;
  }

  /**
   * Reset information about insert marks.
   */
  public void unsetHasInsertMarks()
  {
    this.hasInsertMarks = false;
  }

  /**
   * Does the book mark of the command still exist?
   *
   * @return True if the book mark doesn't exist anymore, false otherwise.
   */
  public boolean isRetired()
  {
    try
    {
      if (bookmark != null)
      {
        return bookmark.getAnchor() == null;
      }
      return false;
    } catch (UnoHelperException e)
    {
      return true;
    }
  }

  /**
   * Has the command been executed?
   *
   * @return True if the command was executed, false otherwise.
   */
  public boolean isDone()
  {
    if (done != null)
    {
      return done.booleanValue();
    }
    else if (isDefinedState("DONE"))
    {
      try
      {
        String doneStr = getState("DONE").toString();
        return doneStr.compareToIgnoreCase("true") == 0;
      } catch (NodeNotFoundException e)
      {
        return false;
      }
    } else
    {
      return STATE_DEFAULT_DONE.booleanValue();
    }
  }

  /**
   * Mark this command as executed and delete the book mark if necessary.
   *
   * @param removeBookmark
   *          True if it should be executed.
   */
  public void markDone(boolean removeBookmark)
  {
    this.done = Boolean.TRUE;
    flushToBookmark(removeBookmark);
  }

  /**
   * Were there any errors during command execution.
   *
   * @return True if there have been errors, false otherwise.
   */
  public boolean hasError()
  {
    if (error != null)
      return error.booleanValue();
    else if (isDefinedState(ERROR_STRING))
    {
      try
      {
        return Boolean.parseBoolean(getState(ERROR_STRING).toString());
      } catch (NodeNotFoundException e)
      {
        return false;
      }
    } else
    {
      return STATE_DEFAULT_ERROR.booleanValue();
    }
  }

  /**
   * Set the error state.
   *
   * @param error
   *          True if there were errors, false otherwise.
   */
  public void setErrorState(boolean error)
  {
    this.error = Boolean.valueOf(error);
  }

  /**
   * Create a ConfigThingy describing the current state of the command.
   *
   * @return The description.
   */
  protected ConfigThingy toConfigThingy()
  {
    if (isDefinedState("DONE") && done != null)
    {
      setOrCreate("DONE", done.toString());
    } else if (isDone() != STATE_DEFAULT_DONE.booleanValue())
    {
      setOrCreate("DONE", Boolean.toString(isDone()));
    }

    if (isDefinedState(ERROR_STRING) && error != null)
    {
      setOrCreate(ERROR_STRING, error.toString());
    } else if (hasError() != STATE_DEFAULT_ERROR.booleanValue())
    {
      setOrCreate("ERRORS", Boolean.toString(hasError()));
    }

    return wmCmd;
  }

  /**
   * Create a string of a ConfigThingy without newlines, commas and other chars, which aren't
   * supported in book mark names.
   *
   * @param conf
   *          The ConfigThingy.
   * @return String representation of the ConfigThingy.
   */
  public static String getCommandString(ConfigThingy conf)
  {
    String wmCmdString = conf.stringRepresentation(true, '\'', true);
    wmCmdString = wmCmdString.replaceAll(",", " ");
    wmCmdString = wmCmdString.replaceAll("[\r\n]+", " ");
    while (wmCmdString.endsWith(" "))
    {
      wmCmdString = wmCmdString.substring(0, wmCmdString.length() - 1);
    }
    return wmCmdString;
  }

  /**
   * Update a book mark or delete it.
   *
   * @param removeIfDone
   *          If true and the command is executed ({@link #isDone()}) the book mark is removed.
   *          Otherwise its name is updated with the state.
   * @return The name of the book mark or null if it has been removed.
   */
  protected String flushToBookmark(boolean removeIfDone)
  {
    try
    {
      if (isDone() && removeIfDone)
      {
        bookmark.remove();
        return null;
      } else
      {
        String wmCmdString = getCommandString(toConfigThingy());

        String name = bookmark.getName();
        name = name.replaceFirst("\\s*\\d+\\s*$", "");
        if (!wmCmdString.equals(name))
        {
          bookmark.rename(wmCmdString);
        }

        return bookmark.getName();
      }
    } catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return null;
    }
  }

  /**
   * Has the command the state.
   *
   * @param key
   *          The name of state ({@link #ERROR_STRING}, or "DONE").
   * @return True if the state is defined, false otherwise.
   */
  protected boolean isDefinedState(String key)
  {
    try
    {
      return getState(key) != null;
    } catch (NodeNotFoundException e)
    {
      return false;
    }
  }

  /**
   * Get the value of a state.
   *
   * @param key
   *          The name of state ({@link #ERROR_STRING}, or "DONE").
   * @return The value.
   * @throws NodeNotFoundException
   *           The command doesn't have the state.
   */
  protected ConfigThingy getState(String key) throws NodeNotFoundException
  {
    return wmCmd.get(STATE).get(key);
  }

  /**
   * Update state information of the command.
   *
   * @param key
   *          The name of state ({@link #ERROR_STRING}, or "DONE").
   * @param value
   *          The value of the state.
   */
  protected void setOrCreate(String key, String value)
  {
    ConfigThingy state;
    try
    {
      state = wmCmd.get(STATE);
    } catch (NodeNotFoundException e1)
    {
      state = wmCmd.add(STATE);
    }

    ConfigThingy ctKey;
    try
    {
      ctKey = state.get(key);
    } catch (NodeNotFoundException e)
    {
      ctKey = state.add(key);
    }

    try
    {
      ctKey.getFirstChild().setName(value);
    } catch (NodeNotFoundException e)
    {
      ctKey.add(value);
    }
  }

  /**
   * Is the content of the command visible?
   *
   * @return True if it's visible, false otherwise.
   * @see de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement#isVisible()
   */
  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Update the visibility of the content of the book mark.
   *
   * @param visible
   *          True if the content should be visible, false otherwise.
   * @see de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement#setVisible(boolean)
   */
  public void setVisible(boolean visible)
  {
    this.visible = visible;
    XTextCursor cursor = getTextCursor();
    if (cursor != null)
    {
      try
      {
        UNO.hideTextRange(cursor, !visible);
      } catch (UnoHelperException e)
      {
        LOGGER.error("Sichtbarkeit konnte nicht geändert werden.", e);
      }
    }
  }

  /**
   * Executor of commands. (Visitor design pattern)
   */
  public static interface Executor
  {
    /**
     * Replace the content of command with a text fragment or an error information.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.InsertFrag cmd);

    /**
     * Replace the content of the command with a database value.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.InsertValue cmd);

    /**
     * Replace the content of the command with a text fragment.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.InsertContent cmd);

    /**
     * Add the content to the form description.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.Form cmd);

    /**
     * Replace the content of the command with an error message.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.InvalidCommand cmd);

    /**
     * Update all text fields in the book mark.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.UpdateFields cmd);

    /**
     * Set the type of the document.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.SetType cmd);

    /**
     * Replace the content of the command with a form value.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.InsertFormValue cmd);

    /**
     * Mark the content of the book mark as part of one or more visibility groups.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.SetGroups cmd);

    /**
     * Add a print function to the document.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.SetPrintFunction cmd);

    /**
     * Highlights the text that it is only printed under special circumstances.
     *
     * @param cmd
     *          The command.
     * @return The number of errors.
     */
    public int executeCommand(PrintBlockCommand cmd);

    /**
     * Mark a place in the document to jump to if there aren't any placeholder.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.SetJumpMark cmd);

    /**
     * Change the mapping of a text fragment id.
     *
     * @param cmd
     *          The command to execute.
     * @return The number of errors.
     */
    public int executeCommand(DocumentCommand.OverrideFrag cmd);
  }

  /**
   * A document command which can transform the content before insertion.
   */
  public static interface OptionalTrafoProvider
  {
    /**
     * Get the name of the transformation.
     *
     * @return The name of the TRAFO.
     */
    public String getTrafoName();
  }

  /**
   * Exception for invalid commands.
   */
  public static class InvalidCommandException extends com.sun.star.uno.Exception
  {
    private static final long serialVersionUID = -3960668930339529734L;

    /**
     * New InvalidCommandException with cause.
     * 
     * @param message
     *          The message.
     */
    public InvalidCommandException(String message)
    {
      super(message);
    }

    /**
     * New InvalidCommandException with message and cause.
     * 
     * @param message
     *          The message.
     * @param cause
     *          The cause.
     */
    public InvalidCommandException(String message, Throwable cause)
    {
      super(message, cause);
    }
  }

  /**
   * Invalid command, which should be displayed in the document.
   */
  public static class InvalidCommand extends DocumentCommand
  {
    private java.lang.Exception exception;

    /**
     * Create a new invalid command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     * @param exception
     *          The cause of invalidity.
     */
    public InvalidCommand(ConfigThingy wmCmd, Bookmark bookmark, InvalidCommandException exception)
    {
      super(wmCmd, bookmark);
      this.exception = exception;
    }

    /**
     * Create a new invalid command with empty configuration.
     *
     * @param bookmark
     *          The book mark.
     * @param exception
     *          The cause of invalidity.
     */
    public InvalidCommand(Bookmark bookmark, SyntaxErrorException exception)
    {
      super(new ConfigThingy("WM"), bookmark);
      this.exception = exception;
    }

    public java.lang.Exception getException()
    {
      return exception;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  /**
   * A not yet implemented command, which is no error.
   */
  public static class NotYetImplemented extends DocumentCommand
  {
    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     */
    public NotYetImplemented(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
      markDone(false);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return 0;
    }
  }

  /**
   * A form command, which provides access to the form description of the document. The form
   * description is stored in a note under the book mark.
   */
  public static class Form extends DocumentCommand
  {
    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     */
    public Form(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  /**
   * Replaces the content of the command with a text fragment or an error information.
   */
  public static class InsertFrag extends DocumentCommand
  {
    private String fragID;

    private List<String> args = null;

    private boolean manualMode = false;

    private Set<String> styles = null;

    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     * @throws InvalidCommandException
     *           The configuraiton is invalid.
     */
    public InsertFrag(ConfigThingy wmCmd, Bookmark bookmark) throws InvalidCommandException
    {
      super(wmCmd, bookmark);

      ConfigThingy wm = wmCmd.query("WM");

      try
      {
        fragID = wm.get("FRAG_ID").toString();
      } catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FRAG_ID", e));
      }

      args = new Vector<>();
      try
      {
        ConfigThingy argsConf = wm.get("ARGS");
        for (ConfigThingy arg : argsConf)
        {
          args.add(arg.getName());
        }
      } catch (NodeNotFoundException e)
      {
        // ARGS are optional
      }

      String mode = wm.getString("MODE", "");
      manualMode = "manual".equalsIgnoreCase(mode);

      styles = new HashSet<>();
      try
      {
        ConfigThingy stylesConf = wm.get("STYLES");
        for (ConfigThingy style : stylesConf)
        {
          String s = style.toString();
          if ("all".equalsIgnoreCase(s))
          {
            styles.add("textstyles");
            styles.add("pagestyles");
            styles.add("numberingstyles");
          } else if ("textStyles".equalsIgnoreCase(s)
              || "pageStyles".equalsIgnoreCase(s)
              || "numberingStyles".equalsIgnoreCase(s))
          {
            styles.add(s.toLowerCase());
          } else
          {
            throw new InvalidCommandException(L.m("STYLE '%1' ist unbekannt.", s));
          }
        }
      } catch (NodeNotFoundException e)
      {
        // STYLES are optional
      }
    }

    public String getFragID()
    {
      return fragID;
    }

    public List<String> getArgs()
    {
      return args;
    }

    public boolean isManualMode()
    {
      return manualMode;
    }

    /**
     * Should the command only import styles.
     *
     * @return True if any style is given, false otherwise.
     */
    public boolean importStylesOnly()
    {
      return !styles.isEmpty();
    }

    public Set<String> getStyles()
    {
      return styles;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  /**
   * Replace the content of the command with a text fragment. The text fragment is defined in the
   * WollMuxBar configuration.
   */
  public static class InsertContent extends DocumentCommand
  {
    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     */
    public InsertContent(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  /**
   * Replace the content of the command with a database value.
   */
  public static class InsertValue extends DocumentCommand implements OptionalTrafoProvider
  {
    private String dbSpalte;

    private String leftSeparator = "";

    private String rightSeparator = "";

    private String trafo = null;

    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     * @throws InvalidCommandException
     *           The configuration is invalid.
     */
    public InsertValue(ConfigThingy wmCmd, Bookmark bookmark) throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      try
      {
        dbSpalte = wmCmd.get("WM").get("DB_SPALTE").toString();
      } catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut DB_SPALTE"));
      }

      Iterator<ConfigThingy> autoseps = wmCmd.query("AUTOSEP").iterator();
      Iterator<ConfigThingy> seps = wmCmd.query("SEPARATOR").iterator();
      String currentSep = " "; // initialize with default separator

      while (autoseps.hasNext())
      {
        ConfigThingy as = autoseps.next();
        String sep = currentSep;
        if (seps.hasNext())
        {
          sep = seps.next().toString();
        }

        if (as.toString().compareToIgnoreCase("left") == 0)
        {
          leftSeparator = sep;
        } else if (as.toString().compareToIgnoreCase("right") == 0)
        {
          rightSeparator = sep;
        } else if (as.toString().compareToIgnoreCase("both") == 0)
        {
          leftSeparator = sep;
          rightSeparator = sep;
        } else
        {
          throw new InvalidCommandException(
              L.m("Unbekannter AUTOSEP-Typ \"%1\". Erwarte \"left\", \"right\" oder \"both\".", as.toString()));
        }
        currentSep = sep;
      }

      try
      {
        trafo = wmCmd.get("WM").get("TRAFO").toString();
      } catch (NodeNotFoundException e)
      {
        // TRAFO is optional
      }
    }

    public String getDBSpalte()
    {
      return dbSpalte;
    }

    public String getLeftSeparator()
    {
      return leftSeparator;
    }

    public String getRightSeparator()
    {
      return rightSeparator;
    }

    @Override
    public String getTrafoName()
    {
      return trafo;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  /**
   * Replace the content of the command with a form value.
   */
  public static class InsertFormValue extends DocumentCommand implements OptionalTrafoProvider
  {
    private String id = null;

    private String trafo = null;

    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     * @throws InvalidCommandException
     *           The configuration is invalid.
     */
    public InsertFormValue(ConfigThingy wmCmd, Bookmark bookmark) throws InvalidCommandException
    {
      super(wmCmd, bookmark);

      try
      {
        id = wmCmd.get("WM").get("ID").toString();
      } catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut ID"));
      }

      try
      {
        trafo = wmCmd.get("WM").get("TRAFO").toString();
      } catch (NodeNotFoundException e)
      {
        // TRAFO is optional
      }
    }

    public String getID()
    {
      return id;
    }

    /**
     * Update the id of this command.
     * 
     * @param id
     *          The new id.
     */
    public void setID(String id)
    {
      this.id = id;
      try
      {
        ConfigThingy idConf = wmCmd.query("WM").query("ID").getLastChild();
        for (Iterator<ConfigThingy> iter = idConf.iterator(); iter.hasNext();)
        {
          iter.next();
          iter.remove();
        }
        idConf.addChild(new ConfigThingy(id));
      } catch (NodeNotFoundException e)
      {
        LOGGER.error("", e);
      }
      flushToBookmark(false);
    }

    @Override
    public String getTrafoName()
    {
      return trafo;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    protected boolean insertsTextContent()
    {
      return true;
    }
  }

  /**
   * Update all text fields in the book mark.
   */
  public static class UpdateFields extends DocumentCommand
  {
    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     */
    public UpdateFields(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  /**
   * Set the type of the document.
   */
  public static class SetType extends DocumentCommand
  {
    private String type;

    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     * @throws InvalidCommandException
     *           The configuration is invalid.
     */
    public SetType(ConfigThingy wmCmd, Bookmark bookmark) throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      type = "";
      try
      {
        type = wmCmd.get("WM").get("TYPE").toString();
      } catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut TYPE"));
      }
      if (type.compareToIgnoreCase("templateTemplate") != 0 && type.compareToIgnoreCase("normalTemplate") != 0
          && type.compareToIgnoreCase("formDocument") != 0)
      {
        throw new InvalidCommandException(L.m("Angegebener TYPE ist ungültig oder falsch geschrieben. "
            + "Erwarte \"templateTemplate\", \"normalTemplate\" oder \"formDocument\"!"));
      }
    }

    public String getType()
    {
      return type;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  /**
   * Change the mapping of a text fragment id.
   */
  public static class OverrideFrag extends DocumentCommand
  {
    private String fragId;

    private String newFragId = null;

    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     * @throws InvalidCommandException
     *           The configuration is invalid.
     */
    public OverrideFrag(ConfigThingy wmCmd, Bookmark bookmark) throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      fragId = "";
      try
      {
        fragId = wmCmd.get("WM").get("FRAG_ID").toString();
      } catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FRAG_ID"));
      }
      try
      {
        newFragId = wmCmd.get("WM").get("NEW_FRAG_ID").toString();
      } catch (NodeNotFoundException e)
      {
        // NEW_FRAG_ID is optional
      }
    }

    public String getFragID()
    {
      return fragId;
    }

    /**
     * Get the new FRAG_ID.
     * 
     * @return The new FRAG_ID or the empty String if no FRAG_ID was given.
     */
    public String getNewFragID()
    {
      if (newFragId == null)
      {
        return "";
      }
      return newFragId;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  /**
   * Add a print function to the document.
   */
  public static class SetPrintFunction extends DocumentCommand
  {
    private String funcName;

    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     * @throws InvalidCommandException
     *           The configuration is invalid.
     */
    public SetPrintFunction(ConfigThingy wmCmd, Bookmark bookmark) throws InvalidCommandException
    {
      super(wmCmd, bookmark);
      funcName = "";
      try
      {
        funcName = wmCmd.get("WM").get("FUNCTION").toString();
      } catch (NodeNotFoundException e)
      {
        throw new InvalidCommandException(L.m("Fehlendes Attribut FUNCTION"));
      }
    }

    public String getFunctionName()
    {
      return funcName;
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }

  /**
   * Mark the content of the book mark as part of one or more visibility groups.
   */
  public static class SetGroups extends DocumentCommand implements VisibilityElement
  {
    private Set<String> groupsSet;

    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     */
    public SetGroups(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
      groupsSet = new HashSet<>();
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }

    @Override
    public Set<String> getGroups()
    {
      ConfigThingy groups = new ConfigThingy("");
      try
      {
        groups = wmCmd.get("GROUPS");
      } catch (NodeNotFoundException e)
      {
        LOGGER.trace("", e);
      }

      for (ConfigThingy group : groups)
      {
        String groupId = group.toString();
        groupsSet.add(groupId);
      }

      return groupsSet;
    }

    @Override
    public void addGroups(Set<String> groups)
    {
      groupsSet.addAll(groups);
    }

    @Override
    protected boolean canHaveGroupsAttribute()
    {
      return true;
    }

    @Override
    public String toString()
    {
      return "" + this.getClass().getSimpleName() + "[" + (isRetired() ? "RETIRED:" : "") + (isDone() ? "DONE:" : "")
          + "GROUPS:" + groupsSet.toString() + getBookmarkName() + "]";
    }
  }

  /**
   * Mark a place in the document to jump to if there aren't any placeholder.
   */
  public static class SetJumpMark extends DocumentCommand
  {
    /**
     * Create a new command.
     *
     * @param wmCmd
     *          The configuration of the command.
     * @param bookmark
     *          The book mark.
     */
    public SetJumpMark(ConfigThingy wmCmd, Bookmark bookmark)
    {
      super(wmCmd, bookmark);
    }

    @Override
    public int execute(DocumentCommand.Executor visitable)
    {
      return visitable.executeCommand(this);
    }
  }
}
