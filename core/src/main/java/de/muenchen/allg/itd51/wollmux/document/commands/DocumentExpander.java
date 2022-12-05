/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.io.IOException;
import com.sun.star.io.XInputStream;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.style.XStyleLoader;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentLoader;
import de.muenchen.allg.itd51.wollmux.document.VisibleTextFragmentList;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentModel.OverrideFragChainException;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.OverrideFrag;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToMark;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

/**
 * Builds the whole document by expanding each text fragment.
 */
class DocumentExpander extends AbstractExecutor
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DocumentExpander.class);

  private final DocumentCommandInterpreter documentCommandInterpreter;

  private String[] fragUrls;

  private int fragUrlsCount = 0;

  /**
   * Has document an unfilled placeholder after insertion of boilerplate?
   */
  private boolean firstEmptyPlaceholder = false;

  /**
   * Creates a new Document expander.
   *
   * @param fragUrls
   *          List of URLs to use for insertContent commands.
   * @param documentCommandInterpreter
   *          The command interpreter.
   */
  public DocumentExpander(DocumentCommandInterpreter documentCommandInterpreter, String[] fragUrls)
  {
    this.documentCommandInterpreter = documentCommandInterpreter;
    this.fragUrls = fragUrls;
    this.fragUrlsCount = 0;
  }

  /**
   * Execute the document commands as long as the document isn't fully constructed.
   *
   * OverrideFrag commands are executed first.
   *
   * @param commands
   *          Collection of document commands.
   * @return Number of command errors.
   */
  public int execute(DocumentCommands commands)
  {
    int errors = 0;
    int i = 0;

    // repeat as long as document changes.
    do
    {
      i++;
      errors += executeOverrideFrags(commands);
      errors += executeAll(commands);
    } while (commands.update() && i < 50);

    return errors;
  }

  /**
   * Execute all OverrideFrag commands, which haven't been executed already (DONE=true or
   * ERROR=true)
   *
   * @param commands
   *          Collection of document commands.
   * @return Number of command errors.
   */
  protected int executeOverrideFrags(DocumentCommands commands)
  {
    int errors = 0;

    for (DocumentCommand cmd : commands)
    {
      if (!(cmd instanceof OverrideFrag)) {
        continue;
      }

      if (!cmd.isDone() && !cmd.hasError())
      {
        errors += cmd.execute(this);
      }
    }
    return errors;
  }

  @Override
  public int executeCommand(OverrideFrag cmd)
  {
    try
    {
      this.documentCommandInterpreter.getModel().setOverrideFrag(cmd.getFragID(), cmd.getNewFragID());
      cmd.markDone(!this.documentCommandInterpreter.debugMode);
      return 0;
    }
    catch (OverrideFragChainException e)
    {
      AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, e);
      cmd.setErrorState(true);
      return 1;
    }
  }

  @Override
  public int executeCommand(InsertFrag cmd)
  {
    cmd.setErrorState(false);
    boolean found = false;
    StringBuilder errors = new StringBuilder();
    String fragId = "";

    try
    {
      fragId = this.documentCommandInterpreter.getModel().getOverrideFrag(cmd.getFragID());

      // If no fragId is present, delete text and mark as done.
      if (fragId.length() == 0)
      {
        clearTextRange(cmd);
        cmd.markDone(false);
        return 0;
      }

      List<String> urls = VisibleTextFragmentList.getURLsByID(WollMuxFiles.getWollmuxConf(), fragId);
      if (urls.isEmpty())
      {
        throw new ConfigurationErrorException(L.m(
          "The textfragment with the FRAG_ID '{0}' is not defined!",
          cmd.getFragID()));
      }

      Iterator<String> iter = urls.iterator();
      while (iter.hasNext() && !found)
      {
        String urlStr = iter.next();
        try
        {
          URL url = WollMuxFiles.makeURL(urlStr);
          LOGGER.debug("Insert Textfragment '{}' with URL '{}'.", cmd.getFragID(), url);
          if (cmd.importStylesOnly())
          {
            insertStylesFromURL(cmd, cmd.getStyles(), url);
          } else
          {
            insertDocumentFromURL(cmd, url);
          }

          found = true;
        }
        catch (java.lang.Exception e)
        {
          // Ignore exception and use next url
          errors.append(e.getLocalizedMessage());
          errors.append("\n\n");
          LOGGER.debug("", e);
          continue;
        }
      }

      if (!found)
      {
        throw new Exception(errors.toString());
      }

      fillPlaceholders(this.documentCommandInterpreter.getModel().doc,
          this.documentCommandInterpreter.getModel().getViewCursor(), cmd.getTextCursor(), cmd.getArgs());
    }
    catch (java.lang.Exception e)
    {
      if (cmd.isManualMode())
      {
        String msg =
          L.m(
            "The Textfragment with the identifier(FRAG_ID) '{0}' {1} could not be inserted:",
            cmd.getFragID(), (fragId.equals(cmd.getFragID()) ? "" : L.m(
              "(Override for fragment '{0}')", fragId)));

        LOGGER.error(msg, e);

        InfoDialog.showInfoModal(L.m("WollMux error"), msg);
      }
      else
      {
        AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, e);
      }
      cmd.setErrorState(true);
      return 1;
    }

    // mark command as done, it is deleted by the SurroundingGarbageCollector.
    cmd.markDone(false);
    return 0;
  }

  @Override
  public int executeCommand(InsertContent cmd)
  {
    cmd.setErrorState(false);
    if (fragUrls.length > fragUrlsCount)
    {
      String urlStr = fragUrls[fragUrlsCount++];

      try
      {
        LOGGER.debug("Inserting textfragment from URL '{}'.", urlStr);

        insertDocumentFromURL(cmd, WollMuxFiles.makeURL(urlStr));
      }
      catch (java.lang.Exception e)
      {
        AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, e);
        cmd.setErrorState(true);
        return 1;
      }
    }
    // mark command as done, it is deleted by the SurroundingGarbageCollector.
    cmd.markDone(false);
    return 0;
  }

  /**
   * Delete content below the command.
   *
   * @param cmd
   *          A document command.
   */
  private void clearTextRange(DocumentCommand cmd)
  {
    XTextCursor insCursor = cmd.getTextCursorWithinInsertMarks();
    insCursor.setString("");
  }

  /**
   * Replace content below the command with the content of the document provided by URL.
   *
   * @param cmd
   *          A document command.
   * @param url
   *          The URL of a document.
   */
  private void insertDocumentFromURL(DocumentCommand cmd, URL url)
  {

    String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

    // TODO: is this workaround still necessary?
    // Workaround: remember old paragraph style, see
    // http://qa.openoffice.org/issues/show_bug.cgi?id=60475
    String paraStyleName = null;
    XTextRange endCursor = null;
    XTextRange range = cmd.getTextCursor();
    if (range != null)
    {
      endCursor = range.getText().createTextCursorByRange(range.getEnd());
    }
    else
      LOGGER.error(
          "insertDocumentFromURL: TextRange des Dokumentkommandos '{}' ist null => Bookmark verschwunden?",
          cmd);

    if (endCursor != null)
    {
      paraStyleName = Utils.getProperty(endCursor, UnoProperty.PARA_STYLE_NAME).toString();
    }

    XTextCursor insCursor = cmd.getTextCursorWithinInsertMarks();
    if (UNO.XDocumentInsertable(insCursor) != null && urlStr != null)
    {
      DocumentLoader.getInstance().insertDocument(insCursor, urlStr);
    }

    // Workaround: reset paragraph style (see above)
    if (endCursor != null && paraStyleName != null)
    {
      Utils.setProperty(endCursor, UnoProperty.PARA_STYLE_NAME, paraStyleName);
    }
  }

  /**
   * Import styles from URL. Styles are replaced if they're already defined. The content below the
   * command is deleted.
   *
   * @param cmd
   *          The document command.
   * @param styles
   *          The styles to import.
   * @param url
   *          The URL of a document.
   * @throws java.io.IOException
   */
  private void insertStylesFromURL(DocumentCommand cmd, Set<String> styles, URL url)
      throws IOException
  {

    String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

    try
    {
      UnoProps props = new UnoProps();
      props.setPropertyValue(UnoProperty.OVERWRITE_STYLES, Boolean.TRUE);
      props.setPropertyValue(UnoProperty.LOAD_CELL_STYLES, Boolean.valueOf(styles.contains("cellstyles")));
      props.setPropertyValue(UnoProperty.LOAD_TEXT_STYLES, Boolean.valueOf(styles.contains("textstyles")));
      props.setPropertyValue(UnoProperty.LOAD_FRAME_STYLES, Boolean.valueOf(styles.contains("framestyles")));
      props.setPropertyValue(UnoProperty.LOAD_PAGE_STYLES, Boolean.valueOf(styles.contains("pagestyles")));
      props.setPropertyValue(UnoProperty.LOAD_NUMBERING_STYLES, Boolean.valueOf(styles.contains("numberingstyles")));
      XStyleFamiliesSupplier sfs = UNO.XStyleFamiliesSupplier(this.documentCommandInterpreter.getModel().doc);
      XStyleLoader loader = UNO.XStyleLoader(sfs.getStyleFamilies());
      XInputStream stream = DocumentLoader.getInstance().getDocumentStream(urlStr);
      props.setPropertyValue(UnoProperty.INPUT_STREAM, stream);
      loader.loadStylesFromURL("private:stream", props.getProps());
    }
    catch (NullPointerException | ExecutionException e)
    {
      LOGGER.error("", e);
    }

    cmd.setTextRangeString("");
  }

  /**
   * Fill placeholder of the boilerplate with the provided arguments.
   *
   * @param doc
   * @param viewCursor
   * @param range
   *          range of inserted boilerplate
   * @param args
   *          Contents to insert.
   */
  private void fillPlaceholders(XTextDocument doc, XTextCursor viewCursor,
      XTextRange range, List<String> args)
  {
    if (doc == null || viewCursor == null || range == null || args == null)
      return;

    List<XTextField> placeholders = new ArrayList<>();

    // Schleife über den Textbereich
    UnoCollection<XTextRange> paragraphs = UnoCollection.getCollection(range, XTextRange.class);
    for (XTextRange paragraph : paragraphs)
    {
      UnoCollection<XTextRange> portions = UnoCollection.getCollection(paragraph, XTextRange.class);
      if (portions != null) // ist wohl ein SwXParagraph
      {
        // Schleife über SwXParagraph und schauen ob es Platzhalterfelder gibt
        // diese werden dann im Vector placeholders gesammelt
        for (XTextRange textPortion : portions)
        {
          try
          {
            String textPortionType = (String) Utils.getProperty(textPortion, UnoProperty.TEXT_PROTION_TYPE);
            // Wenn es ein Textfeld ist
            if ("TextField".equals(textPortionType))
            {
              XTextField textField = UNO.XTextField(UnoProperty.getProperty(textPortion, UnoProperty.TEXT_FIELD));
              // Wenn es ein Platzhalterfeld ist, dem Vector placeholders
              // hinzufügen
              if (UnoService.supportsService(textField, UnoService.CSS_TEXT_TEXT_FIELD_JUMP_EDIT))
              {
                placeholders.add(textField);
              }
            }
          }
          catch (java.lang.Exception x)
          {
            continue;
          }
        }
      }
    }

    // Enumeration über den Vector placeholders mit Platzhalterfeldern die mit
    // den übergebenen Argumenten gefüllt werden
    for (int j = 0; j < args.size() && j < placeholders.size(); j++)
    {
      XTextField textField = UNO.XTextField(placeholders.get(j));
      XTextRange textFieldAnchor = textField.getAnchor();

      // bei einem Parameter ohne Inhalt bleibt die Einfügestelle und die
      // erste ist nach dem Einfügen markiert sonst wird
      // sie ersetzt
      if (!args.get(j).isEmpty())
      {
        textFieldAnchor.setString(args.get(j));
        // setzen des ViewCursor auf die erste nicht ausgefüllte Einfügestelle
        // nach dem Einfügen des Textbausteines
      }
      else if (!firstEmptyPlaceholder)
      {
        try
        {
          firstEmptyPlaceholder = true;
          viewCursor.gotoRange(textFieldAnchor, false);
        }
        catch (java.lang.Exception e)
        {
          LOGGER.trace("", e);
        }
      }
    }

    // wenn weniger Parameter als Einfügestellen angegeben wurden wird nach
    // dem Einfügen des Textbaustein und füllen der Argumente, die erste
    // unausgefüllte Einfügestelle markiert.
    if (placeholders.size() > args.size() && !firstEmptyPlaceholder)
    {
      XTextField textField = UNO.XTextField(placeholders.get(args.size()));
      XTextRange textFieldAnchor = textField.getAnchor();
      viewCursor.gotoRange(textFieldAnchor, false);
      firstEmptyPlaceholder = true;
    }

    // Wenn nach dem Einfügen keine Platzhalter vorhanden ist springt der
    // Cursor auf die definierte Marke setJumpMark (falls Vorhanden)
    if (placeholders.size() <= args.size())
    {
      new OnJumpToMark(doc, false).emit();
    }

    // Wenn mehr Platzhalter angegeben als Einfügestellen vorhanden, erscheint
    // ein Eintrag in der wollmux.log. Wenn in einer Conf Datei im Bereich
    // Textbausteine dort im Bereich Warnungen ein Eintrag mit
    // MSG_TOO_MANY_ARGS "true|on|1" ist, erscheint die Fehlermeldung in einem
    // Fenster im Writer.
    if (placeholders.size() < args.size())
    {

      String error =
        (L.m("There are more parameters specified than the number of placeholders"));

      LOGGER.error(error);

      ConfigThingy conf = WollMuxFiles.getWollmuxConf();
      ConfigThingy warnungenConf = conf.query("Textbausteine").query("Warnungen");

      String message = "";
      try
      {
        message = warnungenConf.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        LOGGER.trace("", e);
      }

      if ("true".equals(message) || "on".equals(message) || "1".equals(message))
      {
        InfoDialog.showInfoModal("WollMux", error);
      }
    }
  }
}
