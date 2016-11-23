/*
 * Dateiname: DocumentCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Interpretiert die in einem Dokument enthaltenen Dokumentkommandos.
 * 
 * Copyright (c) 2009-2016 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung als WMCommandInterpreter
 * 24.10.2005 | LUT | + Sauberes umschliessen von Bookmarks in 
 *                      executeInsertFrag.
 *                    + Abschalten der lock-Controllers  
 * 02.05.2006 | LUT | Komplett-Überarbeitung und Umbenennung in
 *                    DocumentCommandInterpreter.
 * 05.05.2006 | BNK | Dummy-Argument zum Aufruf des FormGUI Konstruktors hinzugefügt.
 * 17.05.2006 | LUT | Doku überarbeitet.
 * 22.08.2006 | BNK | cleanInsertMarks() und EmptyParagraphCleaner verschmolzen zu
 *                  | SurroundingGarbageCollector und dabei komplettes Rewrite.
 * 23.08.2006 | BNK | nochmal Rewrite. Ich glaube dieser Code hält den Rekord im WollMux
 *                  | was rewrites angeht.
 * 08.07.2009 | BED | Anpassung an die Änderungen in DocumentCommand (R48539)
 * 16.12.2009 | ERT | Cast XTextField-Interface entfernt
 * 08.03.2010 | ERT | [R33088]Bessere Fehlermeldungen im Zusammenhang mit overrideFrag
 * 29.05.2013 | JuB | execute() auf 50 begrenzt, damit potentielle endlos-Loops beim Einfügen 
 *                    von Fragmenten abgefangen werden.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.style.XStyleLoader;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertContent;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFormValue;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.InsertFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.OverrideFrag;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetType;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.UpdateFields;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.OverrideFragChainException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.ooo.TextDocument;

/**
 * Diese Klasse repräsentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter
{

  private TextDocumentModel model;

  /**
   * Enthält die Instanz auf das zentrale WollMuxSingleton.
   */
  private WollMuxSingleton mux;

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle
   * Dokumentkommandos im übergebenen Dokument xDoc scannen und interpretieren kann.
   * 
   * @param xDoc
   *          Das Dokument, dessen Kommandos ausgeführt werden sollen.
   * @param mux
   *          Die Instanz des zentralen WollMux-Singletons
   * @param frag_urls
   *          Eine Liste mit fragment-urls, die für das Kommando insertContent
   *          benötigt wird.
   */
  public DocumentCommandInterpreter(TextDocumentModel model, WollMuxSingleton mux)
  {
    this.model = model;
    this.mux = mux;
  }

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle
   * Dokumentkommandos im übergebenen Dokument xDoc scannen und interpretieren kann.
   * 
   * @param xDoc
   *          Das Dokument, dessen Kommandos ausgeführt werden sollen.
   * @param frag_urls
   *          Eine Liste mit fragment-urls, die für das Kommando insertContent
   *          benötigt wird.
   */
  public DocumentCommandInterpreter(TextDocumentModel model)
  {
    this.model = model;
    this.mux = WollMuxSingleton.getInstance();
  }

  /**
   * Diese Methode sollte vor {@link #executeTemplateCommands()} aufgerufen werden
   * und sorgt dafür, dass alle globalen Einstellungen des Dokuments (setType,
   * setPrintFunction) an das TextDocumentModel weitergereicht werden.
   */
  public void scanGlobalDocumentCommands()
  {
    Logger.debug("scanGlobalDocumentCommands");
    boolean modified = model.getDocumentModified();

    GlobalDocumentCommandsScanner s = new GlobalDocumentCommandsScanner();
    s.execute(model.getDocumentCommands());

    model.setDocumentModified(modified);
  }

  /**
   * Diese Methode scannt alle insertFormValue-Kommandos des Dokuments, verarbeitet
   * diese und reicht das gefundene Mapping von IDs zu FormFields an das
   * TextDocumentModel weiter. Zudem wird von dieser Methode auch noch
   * {@link TextDocumentModel#collectNonWollMuxFormFields()} aufgerufen, so dass auch
   * alle Formularfelder aufgesammelt werden, die nicht von WollMux-Kommandos umgeben
   * sind, jedoch trotzdem vom WollMux verstanden und befüllt werden.
   * 
   * Diese Methode wurde aus der Methode {@link #scanGlobalDocumentCommands()}
   * ausgelagert, die früher neben den globalen Dokumentkommandos auch die
   * insertFormValue-Kommandos bearbeitet hat. Die Auslagerung geschah hauptsächlich
   * aus Performance-Optimierungsgründen, da so beim OnProcessTextDocument-Event nur
   * einmal die insertFormValue-Kommandos ausgewertet werden müssen.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void scanInsertFormValueCommands()
  {
    Logger.debug("scanInsertFormValueCommands");
    boolean modified = model.getDocumentModified();

    InsertFormValueCommandsScanner s = new InsertFormValueCommandsScanner();
    s.execute(model.getDocumentCommands());

    model.setIDToFormFields(s.idToFormFields);
    model.collectNonWollMuxFormFields();

    model.setDocumentModified(modified);
  }

  /**
   * Über diese Methode wird die Ausführung der Kommandos gestartet, die für das
   * Expandieren und Befüllen von Dokumenten notwendig sind.
   * 
   * @throws WMCommandsFailedException
   */
  public void executeTemplateCommands() throws WMCommandsFailedException
  {
    Logger.debug("executeTemplateCommands");
    boolean modified = model.getDocumentModified();

    // Zähler für aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errors = 0;

    // Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen
    // können, damit der DocumentCommandTree vollständig aufgebaut werden
    // kann.
    errors +=
      new DocumentExpander(model.getFragUrls()).execute(model.getDocumentCommands());

    // Überträgt beim übergebenen XTextDocument doc die Eigenschaften der
    // Seitenvorlage Wollmuxseite auf die Seitenvorlage Standard, falls
    // Seitenvorlage Wollmuxseite vorhanden ist.
    pageStyleWollmuxseiteToStandard(model.doc);

    // Ziffern-Anpassen der Sachleitenden Verfügungen aufrufen:
    SachleitendeVerfuegung.ziffernAnpassen(model);

    // Jetzt können die TextFelder innerhalb der updateFields Kommandos
    // geupdatet werden. Durch die Auslagerung in einen extra Schritt wird die
    // Reihenfolge der Abarbeitung klar definiert (zuerst die updateFields
    // Kommandos, dann die anderen Kommandos). Dies ist wichtig, da
    // insbesondere das updateFields Kommando exakt mit einem anderen Kommando
    // übereinander liegen kann. Ausserdem liegt updateFields thematisch näher
    // am expandieren der Textfragmente, da updateFields im Prinzip nur dessen
    // Schwäche beseitigt.
    errors += new TextFieldUpdater().execute(model.getDocumentCommands());

    // Hauptverarbeitung: Jetzt alle noch übrigen DocumentCommands (z.B.
    // insertValues) in einem einzigen Durchlauf mit execute bearbeiten.
    errors += new MainProcessor().execute(model.getDocumentCommands());

    // Da keine neuen Elemente mehr eingefügt werden müssen, können
    // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
    // InsertContent-Kommandos gelöscht werden.
    // errors += cleanInsertMarks(tree);

    // Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
    // Absätze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
    // sauber erkennen und entfernen.
    // errors += new EmptyParagraphCleaner().execute(tree);
    SurroundingGarbageCollector collect = new SurroundingGarbageCollector();
    errors += collect.execute(model.getDocumentCommands());
    collect.removeGarbage();

    // da hier bookmarks entfernt werden, muss der Baum upgedatet werden
    model.getDocumentCommands().update();

    // Jetzt wird das Dokument als Formulardokument markiert, wenn mindestens ein
    // Formularfenster definiert ist.
    if (model.hasFormGUIWindow()) model.markAsFormDocument();

    // Document-Modified auf false setzen, da nur wirkliche
    // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
    model.setDocumentModified(modified);

    // ggf. eine WMCommandsFailedException werfen:
    if (errors != 0)
    {
      throw new WMCommandsFailedException(
        L.m(
          "Die verwendete Vorlage enthält %1 Fehler.\n\nBitte kontaktieren Sie Ihre Systemadministration.",
          ((errors == 1) ? "einen" : "" + errors)));
    }
  }

  /**
   * Überträgt beim übergebenen XTextDocument doc die Eigenschaften der Seitenvorlage
   * Wollmuxseite auf die Seitenvorlage Standard, falls Seitenvorlage Wollmuxseite
   * vorhanden ist.
   * 
   * @param doc
   *          Das dessen Seitenvorlage Standard an die Seitenvorlage Wollmuxseite
   *          angepasst werden soll, falls Wollmuxseite vorhanden ist.
   */
  public static void pageStyleWollmuxseiteToStandard(XTextDocument doc)
  {

    XStyleFamiliesSupplier styleFamiliesSupplier = UNO.XStyleFamiliesSupplier(doc);

    XNameContainer nameContainer = null;
    // Holt die Seitenvorlagen
    try
    {
      XNameAccess nameAccess = styleFamiliesSupplier.getStyleFamilies();
      nameContainer = UNO.XNameContainer(nameAccess.getByName("PageStyles"));
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }

    XStyle styleWollmuxseite = null;
    XStyle styleStandard = null;
    // Holt die Seitenvorlage Wollmuxseite und Standard
    try
    {
      styleWollmuxseite = UNO.XStyle(nameContainer.getByName("Wollmuxseite"));
      styleStandard = UNO.XStyle(nameContainer.getByName("Standard"));
    }
    catch (java.lang.Exception e)
    {}

    // Falls eine Seitenvorlage Wollmuxseite vorhanden ist, werden deren
    // Properties ausgelesen und alle, die nicht READONLY sind, auf die
    // Seitenvorlage Standard übertragen.
    if (styleWollmuxseite != null)
    {
      XMultiPropertySet multiPropertySetWollmuxseite =
        UNO.XMultiPropertySet(styleWollmuxseite);
      XPropertySetInfo propertySetInfo =
        multiPropertySetWollmuxseite.getPropertySetInfo();
      Property[] propertys = propertySetInfo.getProperties();
      HashSet<String> set = new HashSet<String>();
      // Schleife über die Properties der Wollmuxseite. Wenn die Properties
      // nicht read-only sind werden sie in einem HashSet set
      // zwischengespeichert.
      for (int i = 0; i < propertys.length; i++)
      {
        String name = propertys[i].Name;
        boolean readonly =
          (propertys[i].Attributes & PropertyAttribute.READONLY) != 0;
        if (!readonly)
        {
          set.add(name);
        }
      }
      // In der Property "HeaderText" und "FooterText" befindet sich der Text
      // aus der Kopf-/Fußzeile, würde dieser mit der Seitenvorlage
      // "Wollmuxseite" überschrieben geht der Text verloren.
      set.remove("HeaderText");
      set.remove("FooterText");
      // Die Property "FollowStyle" wird nicht übertragen da sie in der
      // Seitenvorlage "Wollmuxseite" Wollmuxseite ist und so die Folgeseiten
      // ein Seitenformat "Wollmuxseite" bekommen würden.
      set.remove("FollowStyle");
      int size;
      // Schleife wird so lange durchlaufen bis sich die Größe des HashSet set
      // nicht mehr ändert. Es gibt Properties die auf das erste Mal nicht
      // gesetzt werden können, weil es Abhängigkeiten zu anderen Properties
      // gibt die zuerst gesetzt werden müssen. z.B muß die Property für
      // Kopfzeile "HeaderIsOn" oder für Fußzeile "FooterIsOn" "true"
      // sein, damit anderen Properties der Kopf-/Fußzeile verändert werden
      // können. Die Property "UserDefinesAttributes" wird nie gesetzt und
      // "TextColumns" wird nicht als geändert erkannt.
      do
      {
        size = set.size();
        // Schleife über die HashSet set. Über den Property-Name (aus dem
        // HashSet) wird die entsprechende Property aus der Seitenvorlage
        // "Wollmux" geholt und dann in die Seitenvorlage "Standard" übertragen
        // und anschließend wenn dies funktioniert hat, wird der Property-Name
        // aus dem Iterator gelöscht.
        for (Iterator<String> iter = set.iterator(); iter.hasNext();)
        {
          String element = iter.next();
          Object value = UNO.getProperty(styleWollmuxseite, element);
          Object checkset = UNO.setProperty(styleStandard, element, value);
          if (UnoRuntime.areSame(value, checkset))
          {
            iter.remove();
          }
        }
      } while (size != set.size());
    }
  }

  /**
   * Hierbei handelt es sich um einen minimalen Scanner, der zu aller erst abläuft
   * und die globalen Einstellungen des Dokuments (setType, setPrintFunction)
   * ausliest und dem TextDocumentModel zur Verfügung stellt.
   * 
   * @author christoph.lutz
   */
  private class GlobalDocumentCommandsScanner extends DocumentCommands.Executor
  {
    public int execute(DocumentCommands commands)
    {
      return executeAll(commands);
    }

    /**
     * Legacy-Methode: Stellt sicher, dass die im veralteten Dokumentkommando
     * SetPrintFunction gesetzten Werte in die persistenten Daten des Dokuments
     * übertragen werden und das Dokumentkommando danach gelöscht wird.
     */
    public int executeCommand(SetPrintFunction cmd)
    {
      model.addPrintFunction(cmd.getFunctionName());
      cmd.markDone(true);
      return 0;
    }

    public int executeCommand(SetType cmd)
    {
      model.setType(cmd.getType());

      // Wenn eine Mischvorlage zum Bearbeiten geöffnet wurde soll der typ
      // "templateTemplate" NICHT gelöscht werden, ansonsten schon.
      if (!(model.hasURL() && cmd.getType().equalsIgnoreCase("templateTemplate")))
        cmd.markDone(true);
      return 0;
    }

  }

  /**
   * Scanner, der die InsertFormValue-Kommandos des Dokuments abarbeitet und ein
   * Mapping von IDs zu FormFields aufbaut, das dann dem TextDocumentModel zur
   * Verfügung gestellt werden kann.
   */
  private class InsertFormValueCommandsScanner extends DocumentCommands.Executor
  {
    /** Mapping von IDs zu FormFields */
    public HashMap<String, List<FormField>> idToFormFields =
      new HashMap<String, List<FormField>>();

    private Map<String, FormField> bookmarkNameToFormField =
      new HashMap<String, FormField>();

    public int execute(DocumentCommands commands)
    {
      return executeAll(commands);
    }

    public int executeCommand(InsertFormValue cmd)
    {
      // idToFormFields aufbauen
      String id = cmd.getID();
      LinkedList<FormField> fields;
      if (idToFormFields.containsKey(id))
      {
        fields = (LinkedList<FormField>) idToFormFields.get(id);
      }
      else
      {
        fields = new LinkedList<FormField>();
        idToFormFields.put(id, fields);
      }
      FormField field =
        FormFieldFactory.createFormField(model.doc, cmd, bookmarkNameToFormField);

      if (field != null)
      {
        field.setCommand(cmd);

        // sortiertes Hinzufügen des neuen FormFields zur Liste:
        ListIterator<FormField> iter = fields.listIterator();
        while (iter.hasNext())
        {
          FormField fieldA = iter.next();
          if (field.compareTo(fieldA) < 0)
          {
            iter.previous();
            break;
          }
        }
        iter.add(field);
      }

      return 0;
    }

  }

  /**
   * Der SurroundingGarbageCollector erfasst leere Absätze und Einfügemarker um
   * Dokumentkommandos herum.
   */
  private class SurroundingGarbageCollector extends DocumentCommands.Executor
  {
    /**
     * Speichert Muellmann-Objekte, die zu löschenden Müll entfernen.
     */
    private List<Muellmann> muellmaenner = new Vector<Muellmann>();

    private abstract class Muellmann
    {
      protected XTextRange range;

      public Muellmann(XTextRange range)
      {
        this.range = range;
      }

      public abstract void tueDeinePflicht();
    }

    private class RangeMuellmann extends Muellmann
    {
      public RangeMuellmann(XTextRange range)
      {
        super(range);
      }

      public void tueDeinePflicht()
      {
        Bookmark.removeTextFromInside(model.doc, range);
      }
    }

    private class ParagraphMuellmann extends Muellmann
    {
      public ParagraphMuellmann(XTextRange range)
      {
        super(range);
      }

      public void tueDeinePflicht()
      {
        TextDocument.deleteParagraph(range);
      }
    }

    /**
     * Diese Methode erfasst leere Absätze und Einfügemarker, die sich um die im
     * Kommandobaum tree enthaltenen Dokumentkommandos befinden.
     */
    private int execute(DocumentCommands commands)
    {
      int errors = 0;
      Iterator<DocumentCommand> iter = commands.iterator();
      while (iter.hasNext())
      {
        DocumentCommand cmd = iter.next();
        errors += cmd.execute(this);
      }

      return errors;
    }

    /**
     * Löscht die vorher als Müll identifizierten Inhalte. type filter text
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void removeGarbage()
    {
      try
      {
        model.setLockControllers(true);
        Iterator<Muellmann> iter = muellmaenner.iterator();
        while (iter.hasNext())
        {
          Muellmann muellmann = iter.next();
          muellmann.tueDeinePflicht();
        }
      }
      finally
      {
        model.setLockControllers(false);
      }
    }

    public int executeCommand(InsertFrag cmd)
    {
      if (cmd.hasInsertMarks())
      {
        // ist der ManualMode gesetzt, so darf ein leerer Paragraph am Ende des
        // Dokuments nicht gelöscht werden, da sonst der ViewCursor auf den
        // Start des Textbereiches zurück gesetzt wird. Im Falle der
        // automatischen Einfügung soll aber ein leerer Paragraph am Ende
        // gelöscht werden.
        collectSurroundingGarbageForCommand(cmd, cmd.isManualMode());
      }
      cmd.unsetHasInsertMarks();

      // Kommando löschen wenn der WollMux nicht im debugModus betrieben wird.
      cmd.markDone(!mux.isDebugMode());

      return 0;
    }

    public int executeCommand(InsertContent cmd)
    {
      if (cmd.hasInsertMarks())
      {
        collectSurroundingGarbageForCommand(cmd, false);
      }
      cmd.unsetHasInsertMarks();

      // Kommando löschen wenn der WollMux nicht im debugModus betrieben wird.
      cmd.markDone(!mux.isDebugMode());

      return 0;
    }

    // Helper-Methoden:

    /**
     * Diese Methode erfasst Einfügemarken und leere Absätze zum Beginn und zum Ende
     * des übergebenen Dokumentkommandos cmd, wobei über removeAnLastEmptyParagraph
     * gesteuert werden kann, ob ein Absatz am Ende eines Textes gelöscht werden soll
     * (bei true) oder nicht (bei false).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void collectSurroundingGarbageForCommand(DocumentCommand cmd,
        boolean removeAnLastEmptyParagraph)
    {
      /*
       * Im folgenden steht eine 0 in der ersten Stelle dafür, dass vor dem
       * Einfügemarker kein Text mehr steht (der Marker also am Anfang des Absatzes
       * ist). Eine 0 an der zweiten Stelle steht dafür, dass hinter dem
       * Einfügemarker kein Text mehr folgt (der Einfügemarker also am Ende des
       * Absatzes steht). Ein "T" an dritter Stelle gibt an, dass hinter dem Absatz
       * des Einfügemarkers eine Tabelle folgt. Ein "E" an dritter Stelle gibt an,
       * dass hinter dem Cursor das Dokument aufhört und kein weiterer Absatz kommt.
       * 
       * Startmarke: Grundsätzlich gibt es die folgenden Fälle zu unterscheiden.
       * 
       * 00: Einfügemarker und Zeilenumbruch DAHINTER löschen
       * 
       * 01: nur Einfügemarker löschen
       * 
       * 10: nur Einfügemarker löschen
       * 
       * 11: nur Einfügemarker löschen
       * 
       * 00T: Einfügemarker und Zeilenumbruch DAVOR löschen
       * 
       * Die Fälle 01T, 10T und 11T werden nicht unterstützt.
       * 
       * Endmarke: Es gibt die folgenden Fälle:
       * 
       * 00: Einfügemarker und Zeilenumbruch DAHINTER löschen
       * 
       * 00E: Einfügemarker und Zeilenumbruch DAVOR löschen
       * 
       * 01, 10, 11: Einfügemarker löschen
       * 
       * DO NOT TOUCH THIS CODE ! Dieser Code ist komplex und fehleranfällig.
       * Kleinste Änderungen können dafür sorgen, dass irgendeine der 1000e von
       * Vorlagen plötzlich anders dargestellt wird. Das gewünschte Verhalten dieses
       * Codes ist in diesem Kommentar vollständig dokumentiert und Änderungen
       * sollten nur erfolgen, falls obiger Kommentar nicht korrekt umgesetzt wurde.
       * Um neue Anforderungen umzusetzen sollten unbedingt alle anderen
       * Möglichkeiten in Betracht gezogen werden bevor hier eine Änderung erfolgt.
       * Sollte eine Änderung unumgehbar sein, so ist sie VOR der Implementierung im
       * Wiki und in obigem Kommentar zu dokumentieren. Dabei ist darauf zu achten,
       * dass ein neuer Fall sich mit keinem der anderen Fälle überschneidet.
       */
      XParagraphCursor[] start = cmd.getStartMark();
      XParagraphCursor[] end = cmd.getEndMark();
      if (start == null || end == null) return;

      // Startmarke auswerten:
      if (start[0].isStartOfParagraph() && start[1].isEndOfParagraph())
      {
        muellmaenner.add(new ParagraphMuellmann(start[1]));
      }
      else
      // if start mark is not the only text in the paragraph
      {
        start[1].goLeft((short) 1, true);
        muellmaenner.add(new RangeMuellmann(start[1]));
      }

      // Endemarke auswerten:

      // Prüfen ob der Cursor am Ende des Dokuments steht. Anmerkung: hier kann
      // nicht der bereits vorhandene cursor end[1] zum Testen verwendet werden,
      // weil dieser durch den goRight verändert würde. Man könnte ihn zwar mit
      // goLeft nachträglich wieder zurück schieben, aber das funzt nicht wenn
      // danach eine Tabelle kommt.
      XParagraphCursor docEndTest = cmd.getEndMark()[1];
      boolean isEndOfDocument = !docEndTest.goRight((short) 1, false);

      if (removeAnLastEmptyParagraph == false) isEndOfDocument = false;

      if (end[0].isStartOfParagraph() && end[1].isEndOfParagraph()
        && !isEndOfDocument)
      {
        muellmaenner.add(new ParagraphMuellmann(end[1]));
      }
      else
      {
        end[0].goRight(cmd.getEndMarkLength(), true);
        muellmaenner.add(new RangeMuellmann(end[0]));
      }
    }
  }

  /**
   * Der DocumentExpander sorgt dafür, dass das Dokument nach Ausführung der
   * enthaltenen Kommandos komplett aufgebaut ist und alle Textfragmente eingefügt
   * wurden.
   * 
   * @author christoph.lutz
   * 
   */
  private class DocumentExpander extends DocumentCommands.Executor
  {
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
     */
    public DocumentExpander(String[] fragUrls)
    {
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
    public int executeCommand(OverrideFrag cmd)
    {
      try
      {
        model.setOverrideFrag(cmd.getFragID(), cmd.getNewFragID());
        cmd.markDone(!mux.isDebugMode());
        return 0;
      }
      catch (OverrideFragChainException e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
    }

    /**
     * Diese Methode fügt das Textfragment frag_id in den gegebenen Bookmark
     * bookmarkName ein. Im Fehlerfall wird eine entsprechende Fehlermeldung
     * eingefügt.
     */
    public int executeCommand(InsertFrag cmd)
    {
      cmd.setErrorState(false);
      boolean found = false;
      String errors = "";
      String fragId = "";

      try
      {
        fragId = model.getOverrideFrag(cmd.getFragID());

        // Bei leeren FragIds wird der Text unter dem Dokumentkommando
        // gelöscht und das Dokumentkommando auf DONE gesetzt.
        if (fragId.length() == 0)
        {
          clearTextRange(cmd);
          cmd.markDone(false);
          return 0;
        }

        Vector<String> urls = VisibleTextFragmentList.getURLsByID(fragId);
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

        fillPlaceholders(model.doc, model.getViewCursor(), cmd.getTextCursor(),
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

          WollMuxSingleton.showInfoModal(L.m("WollMux-Fehler"), msg);
        }
        else
        {
          insertErrorField(cmd, e);
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
          insertErrorField(cmd, e);
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
      if (UNO.XTextFramesSupplier(model.doc) != null)
      {
        String[] names =
          UNO.XTextFramesSupplier(model.doc).getTextFrames().getElementNames();
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
        XStyleFamiliesSupplier sfs = UNO.XStyleFamiliesSupplier(model.doc);
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

        ConfigThingy conf = mux.getWollmuxConf();
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
          WollMuxSingleton.showInfoModal("WollMux", error);
        }
      }
    }
  }

  /**
   * Der Hauptverarbeitungsschritt, in dem vor allem die Textinhalte gefüllt werden.
   * 
   * @author christoph.lutz
   * 
   */
  private class MainProcessor extends DocumentCommands.Executor
  {
    /**
     * Hauptverarbeitungsschritt starten.
     */
    private int execute(DocumentCommands commands)
    {
      try
      {
        model.setLockControllers(true);

        int errors = executeAll(commands);

        return errors;
      }
      finally
      {
        model.setLockControllers(false);
      }
    }

    /**
     * Besitzt das Dokument ein (inzwischen veraltetes) Form-Dokumentkommando, so
     * wird dieses in die persistenten Daten des Dokuments kopiert und die zugehörige
     * Notiz gelöscht, wenn nicht bereits eine Formularbeschreibung dort existiert.
     */
    public int executeCommand(DocumentCommand.Form cmd)
    {
      cmd.setErrorState(false);
      try
      {
        model.addToCurrentFormDescription(cmd);
      }
      catch (ConfigurationErrorException e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }
      cmd.markDone(!mux.isDebugMode());
      return 0;
    }

    /**
     * Diese Methode bearbeitet ein InvalidCommand und fügt ein Fehlerfeld an der
     * Stelle des Dokumentkommandos ein.
     */
    public int executeCommand(DocumentCommand.InvalidCommand cmd)
    {
      insertErrorField(cmd, cmd.getException());
      cmd.setErrorState(true);
      return 1;
    }

    /**
     * Diese Methode fügt einen Spaltenwert aus dem aktuellen Datensatz der
     * Absenderdaten ein. Im Fehlerfall wird die Fehlermeldung eingefügt.
     */
    public int executeCommand(DocumentCommand.InsertValue cmd)
    {
      cmd.setErrorState(false);

      String spaltenname = cmd.getDBSpalte();
      String value = null;
      try
      {
        Dataset ds = mux.getDatasourceJoiner().getSelectedDatasetTransformed();
        value = ds.get(spaltenname);
        if (value == null) value = "";

        // ggf. TRAFO durchführen
        value = model.getTransformedValue(cmd.getTrafoName(), value);
      }
      catch (DatasetNotFoundException e)
      {
        value = L.m("<FEHLER: Kein Absender ausgewählt!>");
      }
      catch (ColumnNotFoundException e)
      {
        insertErrorField(cmd, e);
        cmd.setErrorState(true);
        return 1;
      }

      if (value == null || value.equals(""))
      {
        cmd.setTextRangeString("");
      }
      else
      {
        cmd.setTextRangeString(cmd.getLeftSeparator() + value
          + cmd.getRightSeparator());
      }

      cmd.markDone(false);
      return 0;
    }
  }

  /**
   * Dieser Executor hat die Aufgabe alle updateFields-Befehle zu verarbeiten.
   */
  private class TextFieldUpdater extends DocumentCommands.Executor
  {
    /**
     * Ausführung starten
     */
    private int execute(DocumentCommands commands)
    {
      try
      {
        model.setLockControllers(true);

        int errors = executeAll(commands);

        return errors;
      }
      finally
      {
        model.setLockControllers(false);
      }

    }

    /**
     * Diese Methode updated alle TextFields, die das Kommando cmd umschließt.
     */
    public int executeCommand(UpdateFields cmd)
    {
      XTextRange range = cmd.getTextCursor();
      if (range != null)
      {
        UnoService cursor = new UnoService(range);
        updateTextFieldsRecursive(cursor);
      }
      cmd.markDone(!mux.isDebugMode());
      return 0;
    }

    /**
     * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
     * Interface rekursiv nach TextFeldern und ruft deren Methode update() auf.
     * 
     * @param element
     *          Das Element das geupdated werden soll.
     */
    private void updateTextFieldsRecursive(UnoService element)
    {
      // zuerst die Kinder durchsuchen (falls vorhanden):
      if (element.xEnumerationAccess() != null)
      {
        XEnumeration xEnum = element.xEnumerationAccess().createEnumeration();

        while (xEnum.hasMoreElements())
        {
          try
          {
            UnoService child = new UnoService(xEnum.nextElement());
            updateTextFieldsRecursive(child);
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }
      }

      // jetzt noch update selbst aufrufen (wenn verfügbar):
      try
      {
        UnoService textField = element.getPropertyValue("TextField");
        if (textField != null && textField.xUpdatable() != null)
        {
          textField.xUpdatable().update();
        }
      }
      catch (Exception e)
      {}
    }
  }

  // Übergreifende Helper-Methoden:

  /**
   * Diese Methode fügt ein Fehler-Feld an die Stelle des Dokumentkommandos ein.
   */
  private void insertErrorField(DocumentCommand cmd, java.lang.Exception e)
  {
    String msg = L.m("Fehler in Dokumentkommando '%1'", cmd.getBookmarkName());

    // Meldung auch auf dem Logger ausgeben
    if (e != null)
    {
      Logger.error(msg, e);
    }
    else
    {
      Logger.error(msg);
    }

    cmd.setTextRangeString(L.m("<FEHLER:  >"));

    // Cursor erst NACH Aufruf von cmd.setTextRangeString(...) holen, da Bookmark
    // eventuell dekollabiert wird!
    XTextCursor insCursor = cmd.getTextCursor();
    if (insCursor == null)
    {
      Logger.error(L.m("Kann Fehler-Feld nicht einfügen, da kein InsertCursor erzeugt werden konnte."));
      return;
      // Anmerkung: Aufruf von cmd.setTextRangeString() oben macht nichts, falls kein
      // InsertCursor erzeugt werden kann, daher kein Problem, dass die Abfrage nach
      // insCursor == null erst danach geschieht
    }

    // Text fett und rot machen:
    UNO.setProperty(insCursor, "CharColor", Integer.valueOf(0xff0000));
    UNO.setProperty(insCursor, "CharWeight", Float.valueOf(FontWeight.BOLD));

    // Ein Annotation-Textfield erzeugen und einfügen:
    try
    {
      XTextRange range = insCursor.getEnd();
      XTextCursor c = range.getText().createTextCursorByRange(range);
      c.goLeft((short) 2, false);
      XTextContent note =
        UNO.XTextContent(UNO.XMultiServiceFactory(model.doc).createInstance(
          "com.sun.star.text.TextField.Annotation"));
      UNO.setProperty(note, "Content", msg + ":\n\n" + e.getMessage());
      c.getText().insertTextContent(c, note, false);
    }
    catch (java.lang.Exception x)
    {
      Logger.error(x);
    }
  }
}
