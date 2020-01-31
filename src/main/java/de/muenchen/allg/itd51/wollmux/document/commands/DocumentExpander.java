package de.muenchen.allg.itd51.wollmux.document.commands;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.io.IOException;
import com.sun.star.io.XInputStream;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.style.XStyleLoader;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.OverrideFragChainException;
import de.muenchen.allg.itd51.wollmux.core.document.VisibleTextFragmentList;
import de.muenchen.allg.itd51.wollmux.core.document.commands.AbstractExecutor;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.OverrideFrag;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentLoader;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToMark;

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
          "Das Textfragment mit der FRAG_ID '%1' ist nicht definiert!",
          cmd.getFragID()));
      }

      Iterator<String> iter = urls.iterator();
      while (iter.hasNext() && !found)
      {
        String urlStr = iter.next();
        try
        {
          URL url = WollMuxFiles.makeURL(urlStr);
          LOGGER.debug("Füge Textfragment '{}' von URL '{}' ein.", cmd.getFragID(), url);
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

      fillPlaceholders(this.documentCommandInterpreter.getModel().doc, this.documentCommandInterpreter.getModel().getViewCursor(), cmd.getTextCursor(),
        cmd.getArgs());
    }
    catch (java.lang.Exception e)
    {
      if (cmd.isManualMode())
      {
        String msg =
          L.m(
            "Der Textbaustein mit der Bezeichnung (FRAG_ID) '%1' %2 konnte nicht eingefügt werden:",
            cmd.getFragID(), (fragId.equals(cmd.getFragID()) ? "" : L.m(
              "(Override für Fragment '%1')", fragId)));

        LOGGER.error(msg, e);

        InfoDialog.showInfoModal(L.m("WollMux-Fehler"), msg);
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
        LOGGER.debug("Füge Textfragment von URL '{}' ein.", urlStr);

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
   * @throws java.io.IOException
   *           If URL isn't readable.
   */
  private void insertDocumentFromURL(DocumentCommand cmd, URL url)
      throws java.io.IOException
  {
    // TODO: is this workaround still necessary?
    // Workaround: LO freezes if the resource given to insertDocumentFromURL is not available.
    // http://qa.openoffice.org/issues/show_bug.cgi?id=57049
    // Check with Java's URL class first and abort if necessary.

    String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

    if (!DocumentLoader.getInstance().hasDocument(urlStr))
    {
      WollMuxSingleton.checkURL(url);
    }

    // TODO: is this workaround still necessary?
    // Workaround: remember old paragraph style, see
    // http://qa.openoffice.org/issues/show_bug.cgi?id=60475
    String paraStyleName = null;
    XPropertySet endCursor = null;
    XTextRange range = cmd.getTextCursor();
    if (range != null)
    {
      endCursor =
         UNO.XPropertySet(range.getText().createTextCursorByRange(range.getEnd()));
    }
    else
      LOGGER.error(
          "insertDocumentFromURL: TextRange des Dokumentkommandos '{}' ist null => Bookmark verschwunden?",
          cmd);
    try
    {
      if (endCursor != null)
        paraStyleName =
          endCursor.getPropertyValue("ParaStyleName").toString();
    }
    catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }

    XTextCursor insCursor = cmd.getTextCursorWithinInsertMarks();
    if (UNO.XDocumentInsertable(insCursor) != null && urlStr != null)
    {
      DocumentLoader.getInstance().insertDocument(insCursor, urlStr);
    }

    // Workaround: reset paragraph style (see above)
    if (endCursor != null && paraStyleName != null)
    {
      try
      {
        endCursor.setPropertyValue("ParaStyleName", paraStyleName);
      }
      catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
      }
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
   * @throws IOException
   *           If URL isn't readable.
   */
  private void insertStylesFromURL(DocumentCommand cmd, Set<String> styles, URL url)
      throws java.io.IOException, IOException
  {
    // TODO: is this workaround still necessary?
    // Workaround: LO freezes if the resource given to insertDocumentFromURL is not available.
    // http://qa.openoffice.org/issues/show_bug.cgi?id=57049
    // Check with Java's URL class first and abort if necessary.

    String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

    if (!DocumentLoader.getInstance().hasDocument(urlStr))
    {
      WollMuxSingleton.checkURL(url);
    }

    try
    {
      UnoProps props = new UnoProps();
      props.setPropertyValue("OverwriteStyles", Boolean.TRUE);
      props.setPropertyValue("LoadCellStyles",
        Boolean.valueOf(styles.contains("cellstyles")));
      props.setPropertyValue("LoadTextStyles",
        Boolean.valueOf(styles.contains("textstyles")));
      props.setPropertyValue("LoadFrameStyles",
        Boolean.valueOf(styles.contains("framestyles")));
      props.setPropertyValue("LoadPageStyles",
        Boolean.valueOf(styles.contains("pagestyles")));
      props.setPropertyValue("LoadNumberingStyles",
        Boolean.valueOf(styles.contains("numberingstyles")));
      XStyleFamiliesSupplier sfs = UNO.XStyleFamiliesSupplier(this.documentCommandInterpreter.getModel().doc);
      XStyleLoader loader = UNO.XStyleLoader(sfs.getStyleFamilies());
      XInputStream stream = DocumentLoader.getInstance().getDocumentStream(urlStr);
      props.setPropertyValue("InputStream", stream);
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

    XEnumeration xEnum = UNO.XEnumerationAccess(range).createEnumeration();
    XEnumerationAccess enuAccess;
    // Schleife über den Textbereich
    while (xEnum.hasMoreElements())
    {
      Object ele = null;
      try
      {
        ele = xEnum.nextElement();
      }
      catch (Exception e)
      {
        continue;
      }
      enuAccess = UNO.XEnumerationAccess(ele);
      if (enuAccess != null) // ist wohl ein SwXParagraph
      {
        XEnumeration textPortionEnu = enuAccess.createEnumeration();
        // Schleife über SwXParagraph und schauen ob es Platzhalterfelder gibt
        // diese werden dann im Vector placeholders gesammelt
        while (textPortionEnu.hasMoreElements())
        {
          try
          {
            Object textPortion = textPortionEnu.nextElement();
            String textPortionType = (String) Utils.getProperty(textPortion, "TextPortionType");
            // Wenn es ein Textfeld ist
            if ("TextField".equals(textPortionType))
            {
              XTextField textField = UNO.XTextField(UNO.getProperty(textPortion, "TextField"));
              // Wenn es ein Platzhalterfeld ist, dem Vector placeholders
              // hinzufügen
              if (UNO.supportsService(textField, "com.sun.star.text.TextField.JumpEdit"))
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
        (L.m("Es sind mehr Parameter angegeben als Platzhalter vorhanden sind"));

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