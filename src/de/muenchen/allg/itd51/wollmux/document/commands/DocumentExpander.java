package de.muenchen.allg.itd51.wollmux.document.commands;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.style.XStyleLoader;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.OverrideFragChainException;
import de.muenchen.allg.itd51.wollmux.core.document.VisibleTextFragmentList;
import de.muenchen.allg.itd51.wollmux.core.document.WMCommandsFailedException;
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
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Der DocumentExpander sorgt dafür, dass das Dokument nach Ausführung der
 * enthaltenen Kommandos komplett aufgebaut ist und alle Textfragmente eingefügt
 * wurden.
 * 
 * @author christoph.lutz
 * 
 */
class DocumentExpander extends AbstractExecutor
{
  /**
   * 
   */
  private final DocumentCommandInterpreter documentCommandInterpreter;

  private String[] fragUrls;

  private int fragUrlsCount = 0;

  // Markierung des ersten nicht ausgefüllten Platzhalter nach dem Einfügen
  // von Textbausteinen
  private boolean firstEmptyPlaceholder = false;

  /**
   * Erzeugt einen neuen DocumentExpander, mit der Liste fragUrls, die die URLs
   * beschreibt, von denen die Textfragmente für den insertContent Befehl bezogen
   * werden sollen.
   * 
   * @param fragUrls
   * @param documentCommandInterpreter TODO
   */
  public DocumentExpander(DocumentCommandInterpreter documentCommandInterpreter, String[] fragUrls)
  {
    this.documentCommandInterpreter = documentCommandInterpreter;
    this.fragUrls = fragUrls;
    this.fragUrlsCount = 0;
  }

  /**
   * Führt die Dokumentkommandos von commands aus, welche so lange aktualisiert
   * werden, bis das Dokument vollständig aufgebaut ist. Die Dokumentkommandos
   * OverrideFrags erhalten dabei eine Sonderrolle, da sie bereits vor den anderen
   * Dokumentkommandos (insertFrag/insertContent) abgefeiert werden.
   * 
   * @param tree
   * @return
   * @throws WMCommandsFailedException
   */
  public int execute(DocumentCommands commands) throws WMCommandsFailedException
  {
    int errors = 0;
    int i = 0;

    // so lange wiederholen, bis sich der Baum durch das Expandieren nicht
    // mehr ändert.
    do
    {
      i++;
      errors += executeOverrideFrags(commands);
      errors += executeAll(commands);
    } while (commands.update() && i < 50);

    return errors;
  }

  /**
   * führt alle OverrideFrag-Kommandos aus commands aus, wenn sie nicht den Status
   * DONE=true oder ERROR=true besitzen.
   * 
   * @param commands
   * @return Anzahl der bei der Ausführung aufgetretenen Fehler.
   */
  protected int executeOverrideFrags(DocumentCommands commands)
  {
    int errors = 0;

    // Alle DocumentCommands durchlaufen und mit execute aufrufen.
    for (Iterator<DocumentCommand> iter = commands.iterator(); iter.hasNext();)
    {
      DocumentCommand cmd = iter.next();
      if (!(cmd instanceof OverrideFrag)) continue;

      if (cmd.isDone() == false && cmd.hasError() == false)
      {
        // Kommando ausführen und Fehler zählen
        errors += cmd.execute(this);
      }
    }
    return errors;
  }

  /**
   * Wertet ein OverrideFrag-Kommandos aus, über das Fragmente umgemapped werden
   * können, und setzt das Kommando sofort auf DONE. Dies geschieht vor der
   * Bearbeitung der anderen Kommandos (insertFrag/insertContent), da das mapping
   * beim insertFrag/insertContent benötigt wird.
   */
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

  /**
   * Diese Methode fügt das Textfragment frag_id in den gegebenen Bookmark
   * bookmarkName ein. Im Fehlerfall wird eine entsprechende Fehlermeldung
   * eingefügt.
   */
  @Override
  public int executeCommand(InsertFrag cmd)
  {
    cmd.setErrorState(false);
    boolean found = false;
    String errors = "";
    String fragId = "";

    try
    {
      fragId = this.documentCommandInterpreter.getModel().getOverrideFrag(cmd.getFragID());

      // Bei leeren FragIds wird der Text unter dem Dokumentkommando
      // gelöscht und das Dokumentkommando auf DONE gesetzt.
      if (fragId.length() == 0)
      {
        clearTextRange(cmd);
        cmd.markDone(false);
        return 0;
      }

      Vector<String> urls = VisibleTextFragmentList.getURLsByID(WollMuxFiles.getWollmuxConf(), fragId);
      if (urls.size() == 0)
      {
        throw new ConfigurationErrorException(L.m(
          "Das Textfragment mit der FRAG_ID '%1' ist nicht definiert!",
          cmd.getFragID()));
      }
      // Iterator über URLs
      Iterator<String> iter = urls.iterator();
      while (iter.hasNext() && found == false)
      {
        String urlStr = iter.next();
        try
        {
          URL url = WollMuxFiles.makeURL(urlStr);

          Logger.debug(L.m("Füge Textfragment '%1' von URL '%2' ein.",
            cmd.getFragID(), url.toExternalForm()));

          // styles bzw. fragment einfügen:
          if (cmd.importStylesOnly())
            insertStylesFromURL(cmd, cmd.getStyles(), url);
          else
            insertDocumentFromURL(cmd, url);

          found = true;
        }
        catch (java.lang.Exception e)
        {
          // Exception wird nicht beachtet. Wenn die aktuelle URL nicht
          // funktioniert wird die nächste URL ausgewertet
          errors += e.getLocalizedMessage() + "\n\n";
          Logger.debug(e);
          continue;
        }
      }

      if (!found)
      {
        throw new Exception(errors);
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
            cmd.getFragID(), ((fragId.equals(cmd.getFragID()) ? "" : L.m(
              "(Override für Fragment '%1')", fragId))))
            + "\n\n" + e.getMessage();

        Logger.error(msg);

        ModalDialogs.showInfoModal(L.m("WollMux-Fehler"), msg);
      }
      else
      {
        AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, e);
      }
      cmd.setErrorState(true);
      return 1;
    }

    // Kommando als Done markieren aber noch aufheben. Gelöscht wird das
    // Bookmark dann erst durch den SurroundingGarbageCollector.
    cmd.markDone(false);
    return 0;
  }

  /**
   * Diese Methode fügt das nächste Textfragment aus der dem WMCommandInterpreter
   * übergebenen frag_urls liste ein. Im Fehlerfall wird eine entsprechende
   * Fehlermeldung eingefügt.
   */
  @Override
  public int executeCommand(InsertContent cmd)
  {
    cmd.setErrorState(false);
    if (fragUrls.length > fragUrlsCount)
    {
      String urlStr = fragUrls[fragUrlsCount++];

      try
      {
        Logger.debug(L.m("Füge Textfragment von URL '%1' ein.", urlStr));

        insertDocumentFromURL(cmd, WollMuxFiles.makeURL(urlStr));
      }
      catch (java.lang.Exception e)
      {
        AbstractExecutor.insertErrorField(cmd, documentCommandInterpreter.getModel().doc, e);
        cmd.setErrorState(true);
        return 1;
      }
    }
    // Kommando als Done markieren aber noch aufheben. Gelöscht wird das
    // Bookmark dann erst durch den SurroundingGarbageCollector.
    cmd.markDone(false);
    return 0;
  }

  // Helper-Methoden:

  /**
   * Löscht den Inhalt der TextRange von cmd, wobei Workarounds für
   * OpenOffice-Probleme angewendet werden. Insbesondere werden InsertMarks um die
   * Stelle herumgelegt vor dem Löschen, wie dies auch bei
   * {@link #insertDocumentFromURL(DocumentCommand, URL)} geschieht.
   * 
   * @param cmd
   *          Einfügeposition
   */
  private void clearTextRange(DocumentCommand cmd)
  {
    // Leeren Text (mit Insert Marks) einfügen:
    XTextCursor insCursor = cmd.getTextCursorWithinInsertMarks();
    insCursor.setString("");
  }

  /**
   * Die Methode fügt das externe Dokument von der URL url an die Stelle von cmd
   * ein. Die Methode enthält desweiteren notwendige Workarounds für die Bugs des
   * insertDocumentFromURL der UNO-API.
   * 
   * @param cmd
   *          Einfügeposition
   * @param url
   *          die URL des einzufügenden Textfragments
   * @throws java.io.IOException
   * @throws IOException
   * @throws IllegalArgumentException
   * @throws java.io.IOException
   * @throws IOException
   */
  private void insertDocumentFromURL(DocumentCommand cmd, URL url)
      throws IllegalArgumentException, java.io.IOException, IOException
  {
    // Workaround: OOo friert ein, wenn ressource bei insertDocumentFromURL
    // nicht auflösbar. http://qa.openoffice.org/issues/show_bug.cgi?id=57049
    // Hier wird versucht, die URL über den java-Klasse url aufzulösen und bei
    // Fehlern abgebrochen.
    WollMuxSingleton.checkURL(url);

    // URL durch den URLTransformer von OOo jagen, damit die URL auch von OOo
    // verarbeitet werden kann.
    String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

    // Workaround: Alten Paragraphenstyle merken. Problembeschreibung siehe
    // http://qa.openoffice.org/issues/show_bug.cgi?id=60475
    String paraStyleName = null;
    UnoService endCursor = new UnoService(null);
    XTextRange range = cmd.getTextCursor();
    if (range != null)
    {
      endCursor =
        new UnoService(range.getText().createTextCursorByRange(range.getEnd()));
    }
    else
      Logger.error(L.m(
        "insertDocumentFromURL: TextRange des Dokumentkommandos '%1' ist null => Bookmark verschwunden?",
        cmd.toString()));
    try
    {
      if (endCursor.xPropertySet() != null)
        paraStyleName =
          endCursor.getPropertyValue("ParaStyleName").getObject().toString();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    // Liste aller TextFrames vor dem Einfügen zusammenstellen (benötigt für
    // das Updaten der enthaltenen TextFields später).
    HashSet<String> textFrames = new HashSet<String>();
    if (UNO.XTextFramesSupplier(this.documentCommandInterpreter.getModel().doc) != null)
    {
      String[] names =
        UNO.XTextFramesSupplier(this.documentCommandInterpreter.getModel().doc).getTextFrames().getElementNames();
      for (int i = 0; i < names.length; i++)
      {
        textFrames.add(names[i]);
      }
    }

    // Textfragment (mit Insert Marks) einfügen:
    XTextCursor insCursor = cmd.getTextCursorWithinInsertMarks();
    if (UNO.XDocumentInsertable(insCursor) != null && urlStr != null)
    {
      UNO.XDocumentInsertable(insCursor).insertDocumentFromURL(urlStr,
        new PropertyValue[] {});
    }

    // Workaround: ParagraphStyleName für den letzten eingefügten Paragraphen
    // wieder setzen (siehe oben).
    if (endCursor.xPropertySet() != null && paraStyleName != null)
    {
      try
      {
        endCursor.setPropertyValue("ParaStyleName", paraStyleName);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Diese Methode importiert alle in styles angegebenen Formatvorlagen aus dem
   * durch url beschriebenen Fragment definiert und ersetzt dabei auch die bereits
   * bestehenden Formatvorlagen des aktuellen Dokuments. Nach der erfolgreichen
   * Einfügung der Formatvorlagen wird der Inhalt des Dokumentkommandos gelöscht,
   * da ja mit dem Einfügen keine Textinhalte eingefügt werden.
   * 
   * @param cmd
   *          das Dokumentkommando dessen Inhalt nach dem erfolgreichen Einfügen
   *          gelöscht wird.
   * @param styles
   *          ein Set mit den in Kleinbuchstaben geschriebenen Namen der zu
   *          importierenden styles.
   * @param url
   *          die URL des einzufügenden Textfragments
   * @throws java.io.IOException
   * @throws IOException
   */
  private void insertStylesFromURL(DocumentCommand cmd, Set<String> styles, URL url)
      throws java.io.IOException, IOException
  {
    // Workaround für Einfrierfehler von OOo, wenn ressource nicht auflösbar
    // (ich habe nicht geprüft, ob das für insertStylesFromURL notwendig ist,
    // aber schaden kann es bestimmt nicht)
    WollMuxSingleton.checkURL(url);

    // URL durch den URLTransformer von OOo jagen, damit die URL auch von OOo
    // verarbeitet werden kann.
    String urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;

    // Styles einfügen:
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
      loader.loadStylesFromURL(urlStr, props.getProps());
    }
    catch (NullPointerException e)
    {
      Logger.error(e);
    }

    // Textinhalt löschen
    cmd.setTextRangeString("");
  }

  /**
   * Diese Methode füllt die Einfuegestellen(Platzhalter) aus dem eingefügten
   * Textbaustein mit den übergebenen Argumente args
   * 
   * @param range
   *          der Bereich des eingefügten Textbausteins
   * @param args
   *          Argumente die beim Aufruf zum Einfügen übergeben werden
   */
  private void fillPlaceholders(XTextDocument doc, XTextCursor viewCursor,
      XTextRange range, Vector<String> args)
  {
    if (doc == null || viewCursor == null || range == null || args == null)
      return;

    // Vector mit allen Platzhalterfelder
    Vector<XTextField> placeholders = new Vector<XTextField>();

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
        // diese diese werden dann im Vector placeholders gesammelt
        while (textPortionEnu.hasMoreElements())
        {
          Object textPortion;
          try
          {
            textPortion = textPortionEnu.nextElement();
          }
          catch (java.lang.Exception x)
          {
            continue;
          }
          String textPortionType =
            (String) UNO.getProperty(textPortion, "TextPortionType");
          // Wenn es ein Textfeld ist
          if (textPortionType.equals("TextField"))
          {
            XTextField textField = null;
            try
            {
              textField =
                UNO.XTextField(UNO.getProperty(textPortion, "TextField"));
              // Wenn es ein Platzhalterfeld ist, dem Vector placeholders
              // hinzufügen
              if (UNO.supportsService(textField,
                "com.sun.star.text.TextField.JumpEdit"))
              {
                placeholders.add(textField);
              }
            }
            catch (java.lang.Exception e)
            {
              continue;
            }
          }
        }
      }
    }

    // Enumeration über den Vector placeholders mit Platzhalterfeldern die mit
    // den übergebenen Argumenten gefüllt werden
    Enumeration<XTextField> enumPlaceholders = placeholders.elements();
    for (int j = 0; j < args.size() && j < placeholders.size(); j++)
    {
      Object placeholderObj = enumPlaceholders.nextElement();
      XTextField textField = UNO.XTextField(placeholderObj);
      XTextRange textFieldAnchor = textField.getAnchor();

      // bei einem Parameter ohne Inhalt bleibt die Einfügestelle und die
      // erste ist nach dem Einfügen markiert sonst wird
      // sie ersetzt
      if (!(args.elementAt(j).equals("")))
      {
        textFieldAnchor.setString(args.elementAt(j));
        // setzen des ViewCursor auf die erste nicht ausgefüllte Einfügestelle
        // nach dem Einfügen des Textbausteines
      }
      else if (firstEmptyPlaceholder != true)
      {
        try
        {
          firstEmptyPlaceholder = true;
          viewCursor.gotoRange(textFieldAnchor, false);
        }
        catch (java.lang.Exception e)
        {}
      }
    }

    // wenn weniger Parameter als Einfügestellen angegeben wurden wird nach
    // dem Einfügen des Textbaustein und füllen der Argumente, die erste
    // unausgefüllte Einfügestelle markiert.
    if (placeholders.size() > args.size())
    {
      if (firstEmptyPlaceholder == false)
      {
        XTextField textField = UNO.XTextField(placeholders.get(args.size()));
        XTextRange textFieldAnchor = textField.getAnchor();
        viewCursor.gotoRange(textFieldAnchor, false);
        firstEmptyPlaceholder = true;
      }
    }

    // Wenn nach dem Einfügen keine Platzhalter vorhanden ist springt der
    // Cursor auf die definierte Marke setJumpMark (falls Vorhanden)
    if (placeholders.size() <= args.size())
    {
      WollMuxEventHandler.handleJumpToMark(doc, false);
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

      Logger.error(error);

      ConfigThingy conf = WollMuxFiles.getWollmuxConf();
      ConfigThingy WarnungenConf = conf.query("Textbausteine").query("Warnungen");

      String message = "";
      try
      {
        message = WarnungenConf.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {}

      if (message.equals("true") || message.equals("on") || message.equals("1"))
      {
        ModalDialogs.showInfoModal("WollMux", error);
      }
    }
  }
}