/*
 * Dateiname: TextDocumentModel.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert ein aktuell geöffnetes TextDokument.
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 15.09.2006 | LUT | Erstellung als TextDocumentModel
 * 03.01.2007 | BNK | +collectNonWollMuxFormFields
 * 11.04.2007 | BNK | [R6176]+removeNonWMBookmarks()
 * 08.04.2007 | BNK | [R18119]Druckfunktion inkompatibel mit Versionen <3.11.1
 * 08.07.2009 | BED | addToCurrentFormDescription(...) löscht jetzt nicht nur Notiz
 *                  | sondern kompletten Bookmark-Inhalt und Bookmark wird kollabiert
 * 17.05.2010 | BED | +rewritePersistantData() (für Workaround für Issue #100374)
 * 07.05.2012 | ERT | TextDocumentModel.setWindowPosSize: Größe und Position des Fensters
 *                  | werden jetzt nacheinander gesetzt. Funktioniert besser unter Gnome
 * 11.04.2014 | Loi | Die vorhergehende Änderungen reicht nicht mehr unter KDE4, 
 *                  | deswegen wird das Fenster noch demaximiert.            
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.document;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.DocumentManager;
import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.core.document.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.OptionalHighlightColorProvider;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.functions.Values.SimpleMap;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.dialog.DialogFactory;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommandInterpreter;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

/**
 * Diese Klasse repräsentiert das Modell eines aktuell geöffneten TextDokuments und
 * ermöglicht den Zugriff auf alle interessanten Aspekte des TextDokuments.
 * 
 * @author christoph.lutz
 * 
 */
public class TextDocumentModel
{
  public static final String OVERRIDE_FRAG_DB_SPALTE = "OVERRIDE_FRAG_DB_SPALTE";
  
  /**
   * Verwendet für {@link #lastTouchedByOOoVersion} und
   * {@link #lastTouchedByWollMuxVersion}.
   */
  public static final String VERSION_UNKNOWN = "unknown";

  /**
   * Enthält die Referenz auf das XTextDocument-interface des eigentlichen
   * TextDocument-Services der zugehörigen UNO-Komponente.
   */
  public final XTextDocument doc;

  /**
   * Pattern zum Erkennen der Bookmarks, die {@link #deForm()} entfernen soll.
   */
  private static final Pattern BOOKMARK_KILL_PATTERN =
    Pattern.compile("(\\A\\s*(WM\\s*\\(.*CMD\\s*'((form)|(setGroups)|(insertFormValue))'.*\\))\\s*\\d*\\z)"
      + "|(\\A\\s*(WM\\s*\\(.*CMD\\s*'(setType)'.*'formDocument'\\))\\s*\\d*\\z)"
      + "|(\\A\\s*(WM\\s*\\(.*'formDocument'.*CMD\\s*'(setType)'.*\\))\\s*\\d*\\z)");

  /**
   * Pattern zum Erkennen von WollMux-Bookmarks.
   */
  private static final Pattern WOLLMUX_BOOKMARK_PATTERN =
    Pattern.compile("(\\A\\s*(WM\\s*\\(.*\\))\\s*\\d*\\z)");

  /**
   * Prefix, mit dem die Namen aller automatisch generierten dokumentlokalen
   * Funktionen beginnen.
   */
  private static final String AUTOFUNCTION_PREFIX = "AUTOFUNCTION_";

  /**
   * Ermöglicht den Zugriff auf eine Collection aller FormField-Objekte in diesem
   * TextDokument über den Namen der zugeordneten ID. Die in dieser Map enthaltenen
   * FormFields sind nicht in {@link #idToTextFieldFormFields} enthalten und
   * umgekehrt.
   */
  private HashMap<String, List<FormField>> idToFormFields;

  /**
   * Liefert zu einer ID eine {@link java.util.List} von FormField-Objekten, die alle
   * zu Textfeldern ohne ein umschließendes WollMux-Bookmark gehören, aber trotzdem
   * vom WollMux interpretiert werden. Die in dieser Map enthaltenen FormFields sind
   * nicht in {@link #idToFormFields} enthalten und umgekehrt.
   */
  private HashMap<String, List<FormField>> idToTextFieldFormFields;

  /**
   * Enthält alle Textfelder ohne ein umschließendes WollMux-Bookmark, die vom
   * WollMux interpretiert werden sollen, aber TRAFO-Funktionen verwenden, die nur
   * einen feste Werte zurückliefern (d.h. keine Parameter erwarten) Die in dieser
   * Map enthaltenen FormFields sind nicht in {@link #idToTextFieldFormFields}
   * enthalten, da sie keine ID besitzen der sie zugeordnet werden können.
   */
  private List<FormField> staticTextFieldFormFields;

  /**
   * Dieses Feld stellt ein Zwischenspeicher für Fragment-Urls dar. Es wird dazu
   * benutzt, im Fall eines openTemplate-Befehls die urls der übergebenen
   * frag_id-Liste temporär zu speichern.
   */
  private String[] fragUrls;

  /**
   * Gibt an, ob das Dokument ein Template ist oder wie ein Template zu behandeln
   * ist.
   */
  private boolean isTemplate;

  /**
   * Gibt an, ob das Dokument wie ein Dokument mit WollMux-Formularfunktion zu
   * behandeln ist.
   */
  private boolean isFormDocument;

  /**
   * Enthält die Namen der aktuell gesetzten Druckfunktionen.
   */
  private HashSet<String> printFunctions;

  /**
   * Enthält die Formularbeschreibung des Dokuments oder null, wenn die
   * Formularbeschreibung noch nicht eingelesen wurde.
   */
  private ConfigThingy formularConf;

  /**
   * Enthält die aktuellen Werte der Formularfelder als Zuordnung id -> Wert.
   */
  private HashMap<String, String> formFieldValues;

  /**
   * Verantwortlich für das Speichern persistenter Daten.
   */
  private PersistentDataContainer persistentData;

  /**
   * Enthält die Kommandos dieses Dokuments.
   */
  private DocumentCommands documentCommands;

  /**
   * Enthält eine Map mit den Namen aller (bisher gesetzter) Sichtbarkeitsgruppen auf
   * deren aktuellen Sichtbarkeitsstatus (sichtbar = true, unsichtbar = false)
   */
  private HashMap<String, Boolean> mapGroupIdToVisibilityState;

  /**
   * Der Vorschaumodus ist standardmäßig immer gesetzt - ist dieser Modus nicht
   * gesetzt, so werden in den Formularfeldern des Dokuments nur die Feldnamen in
   * spitzen Klammern angezeigt.
   */
  private boolean formFieldPreviewMode;

  /**
   * Kann über setPartOfMultiformDocument gesetzt werden und sollte dann true
   * enthalten, wenn das Dokument ein Teil eines Multiformdokuments ist.
   */
  private boolean partOfMultiform;

  /**
   * Enthält ein ein Mapping von alten FRAG_IDs fragId auf die jeweils neuen FRAG_IDs
   * newFragId, die über im Dokument enthaltene Dokumentkommando WM(CMD
   * 'overrideFrag' FRAG_ID 'fragId' NEW_FRAG_ID 'newFragId') entstanden sind.
   */
  private HashMap /* of String */<String, String> overrideFragMap;

  /**
   * Enthält den Kontext für die Funktionsbibliotheken und Dialogbibliotheken dieses
   * Dokuments.
   */
  private HashMap<Object, Object> functionContext;

  /**
   * Enthält die Dialogbibliothek mit den globalen und dokumentlokalen
   * Dialogfunktionen oder null, wenn die Dialogbibliothek noch nicht benötigt wurde.
   */
  private DialogLibrary dialogLib;

  /**
   * Enthält die Funktionsbibliothek mit den globalen und dokumentlokalen Funktionen
   * oder null, wenn die Funktionsbilbiothek noch nicht benötigt wurde.
   */
  private FunctionLibrary functionLib;

  /**
   * Enthält null oder ab dem ersten Aufruf von getMailmergeConf() die Metadaten für
   * den Seriendruck in einem ConfigThingy, das derzeit in der Form
   * "Seriendruck(Datenquelle(...))" aufgebaut ist.
   */
  private ConfigThingy mailmergeConf;

  /**
   * Enthält die Versionsnummer (nicht Revision, da diese zwischen git und svn
   * unterschiedlich ist) des WollMux, der das Dokument zuletzt angefasst hat (vor
   * diesem gerade laufenden).
   */
  private String lastTouchedByWollMuxVersion;

  /**
   * Enthält die Versionsnummer des OpenOffice,org, das das Dokument zuletzt
   * angefasst hat (vor diesem gerade laufenden).
   */
  private String lastTouchedByOOoVersion;

  /**
   * Wird auf true gesetzt, wenn das erste mal
   * {@link #updateLastTouchedByVersionInfo() aufgerufen wurde.
   */
  private boolean haveUpdatedLastTouchedByVersionInfo = false;

  /**
   * Wird ausschließlich im Konstruktor gesetzt und gibt an, ob das Dokument bereits
   * beim Öffnen (also vor der Bearbeitung durch den WollMux) Merkmale eines
   * WollMux-Formulardokuments enthielt.
   */
  private boolean alreadyTouchedAsFormDocument;

  /**
   * Das TextDocumentModel kann in einem Simulationsmodus betrieben werden, in dem
   * Änderungen an Formularelementen (WollMux- und NON-WollMux-Felder) nur simuliert
   * und nicht tatsächlich durchgeführt werden. Benötigt wird dieser Modus für den
   * Seriendruck über den OOo-Seriendruck, bei dem die Änderungen nicht auf dem
   * gerade offenen TextDocument durchgeführt werden, sondern auf einer durch den
   * OOo-Seriendruckmechanismus verwalteten Kopie des Dokuments. Der Simulationsmodus
   * ist dann aktiviert, wenn {@link #simulationResult} != null ist.
   */
  private SimulationResults simulationResult = null;
  
  /**
   * Der Wert von {@link #OVERRIDE_FRAG_DB_SPALTE}, d,h, der Name der Spalte, die die
   * persönliche OverrideFrag-Liste enthält. "" falls nicht definiert.
   */
  private String overrideFragDbSpalte;

  /**
   * Wird and FormGUI und FormController in mapIdToPresetValue übergeben, wenn der
   * Wert des entsprechenden Feldes nicht korrekt widerhergestellt werden kann.
   * ACHTUNG! Diese Konstante muss als Objekt übergeben werden, da sie == verglichen
   * wird.
   */
  public final static String FISHY = L.m("!!!PRÜFEN!!!");

  /**
   * Pattern zum Erkennen von InputUser-Feldern, die eine WollMux-Funktion
   * referenzieren (z.B. die Spezialfelder des WollMux-Seriendrucks).
   */
  public static final Pattern INPUT_USER_FUNCTION =
    Pattern.compile("\\A\\s*(WM\\s*\\(.*FUNCTION\\s*'[^']*'.*\\))\\s*\\d*\\z");


  /**
   * Erzeugt ein neues TextDocumentModel zum XTextDocument doc und sollte nie 
   * direkt aufgerufen werden, da neue TextDocumentModels über 
   * {@link DocumentManager#getTextDocumentModel(XTextDocument)}  erzeugt und 
   * verwaltet werden.
   * 
   * @param doc
   */
  public TextDocumentModel(XTextDocument doc)
  {
    this.doc = doc;
    this.idToFormFields = new HashMap<String, List<FormField>>();
    this.idToTextFieldFormFields = new HashMap<String, List<FormField>>();
    this.staticTextFieldFormFields = new Vector<FormField>();
    this.fragUrls = new String[] {};
    this.printFunctions = new HashSet<String>();
    this.formularConf = null;
    this.formFieldValues = new HashMap<String, String>();
    this.mapGroupIdToVisibilityState = new HashMap<String, Boolean>();
    this.overrideFragMap = new HashMap<String, String>();
    parseInitialOverrideFragMap(getInitialOverrideFragMap());
    this.functionContext = new HashMap<Object, Object>();
    this.formFieldPreviewMode = true;

    // Kommandobaum erzeugen (modified-Status dabei unberührt lassen):
    boolean modified = getDocumentModified();
    this.documentCommands = new DocumentCommands(UNO.XBookmarksSupplier(doc));
    documentCommands.update();
    setDocumentModified(modified);

    // Auslesen der Persistenten Daten:
    this.persistentData = TextDocumentModel.createPersistentDataContainer(doc);
    String setTypeData = persistentData.getData(DataID.SETTYPE);
    alreadyTouchedAsFormDocument = "formDocument".equals(setTypeData);
    parsePrintFunctions(persistentData.getData(DataID.PRINTFUNCTION));
    parseFormValues(persistentData.getData(DataID.FORMULARWERTE));
    lastTouchedByWollMuxVersion =
      persistentData.getData(DataID.TOUCH_WOLLMUXVERSION);
    if (lastTouchedByWollMuxVersion == null)
      lastTouchedByWollMuxVersion = VERSION_UNKNOWN;
    lastTouchedByOOoVersion = persistentData.getData(DataID.TOUCH_OOOVERSION);
    if (lastTouchedByOOoVersion == null) lastTouchedByOOoVersion = VERSION_UNKNOWN;

    // Type auswerten
    this.isTemplate = !hasURL();
    this.isFormDocument = false;
    setType(setTypeData);
  }

  /**
   * Liefert die Version des letzten WollMux der dieses Dokument angefasst hat (vor
   * dem aktuell laufenden) oder {@link #VERSION_UNKNOWN} falls unbekannt.
   * 
   * Achtung! Es kann günstiger sein, hier im TextDocumentModel an zentraler Stelle
   * Funktionen einzubauen zum Vergleich des Versionsstrings mit bestimmten anderen
   * Versionen, als das Parsen/Vergleichen von Versionsstrings an mehreren Stellen zu
   * replizieren.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public String getLastTouchedByWollMuxVersion()
  {
    return lastTouchedByWollMuxVersion;
  }

  /**
   * Liefert die Version des letzten OpenOffice,org das dieses Dokument angefasst hat
   * (vor dem aktuell laufenden) oder {@link #VERSION_UNKNOWN} falls unbekannt.
   * 
   * Achtung! Es kann günstiger sein, hier im TextDocumentModel an zentraler Stelle
   * Funktionen einzubauen zum Vergleich des Versionsstrings mit bestimmten anderen
   * Versionen, als das Parsen/Vergleichen von Versionsstrings an mehreren Stellen zu
   * replizieren.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public String getLastTouchedByOOoVersion()
  {
    return lastTouchedByOOoVersion;
  }

  /**
   * Schreibt die WollMux und OOo-Version in {@link PersistentDataContainer}. Die
   * Rückgabewerte von {@link #getLastTouchedByOOoVersion()} und
   * {@link #getLastTouchedByWollMuxVersion()} sind davon NICHT betroffen, da diese
   * immer den Zustand beim Öffnen repräsentieren. Der modified-Zustand des Dokuments
   * wird durch diese Funktion nicht verändert.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public void updateLastTouchedByVersionInfo()
  {
    if (!haveUpdatedLastTouchedByVersionInfo)
    {
      // Logger.error(new Exception()); //um einen Stacktrace zu kriegen
      haveUpdatedLastTouchedByVersionInfo = true;
      boolean modified = getDocumentModified();
      persistentData.setData(DataID.TOUCH_WOLLMUXVERSION,
        WollMuxSingleton.getVersion());
      persistentData.setData(DataID.TOUCH_OOOVERSION, Workarounds.getOOoVersion());
      setDocumentModified(modified);
    }
  }

  /**
   * Nimmt ein ConfigThingy von folgender Form
   * 
   * <pre>
   * overrideFrag(
   *   (FRAG_ID 'A' NEW_FRAG_ID 'B')
   *   (FRAG_ID 'C' NEW_FRAG_ID 'D')
   *   ...
   * )
   * </pre>
   * 
   * parst es und initialisiert damit {@link #overrideFragMap}. NEW_FRAG_ID ist
   * optional und wird als leerer String behandelt wenn es fehlt.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private void parseInitialOverrideFragMap(ConfigThingy initialOverrideFragMap)
  {
    for (ConfigThingy conf : initialOverrideFragMap)
    {
      String oldFragId;
      try
      {
        oldFragId = conf.get("FRAG_ID").toString();
      }
      catch (NodeNotFoundException x)
      {
        Logger.error(L.m(
          "FRAG_ID Angabe fehlt in einem Eintrag der %1: %2\nVielleicht haben Sie die Klammern um (FRAG_ID 'A' NEW_FRAG_ID 'B') vergessen?",
          OVERRIDE_FRAG_DB_SPALTE, conf.stringRepresentation()));
        continue;
      }

      String newFragId = "";
      try
      {
        newFragId = conf.get("NEW_FRAG_ID").toString();
      }
      catch (NodeNotFoundException x)
      {
        // NEW_FRAG_ID ist optional
      }

      try
      {
        setOverrideFrag(oldFragId, newFragId);
      }
      catch (OverrideFragChainException x)
      {
        Logger.error(L.m("Fehlerhafte Angabe in %1: %2",
          OVERRIDE_FRAG_DB_SPALTE, conf.stringRepresentation()), x);
      }
    }
  }

  /**
   * Liefert den Dokument-Kommandobaum dieses Dokuments.
   * 
   * @return der Dokument-Kommandobaum dieses Dokuments.
   */
  synchronized public DocumentCommands getDocumentCommands()
  {
    return documentCommands;
  }

  /**
   * Erzeugt einen Iterator über alle Sichtbarkeitselemente (Dokumentkommandos und
   * Textbereiche mit dem Namenszusatz 'GROUPS ...'), die in diesem Dokument
   * enthalten sind. Der Iterator liefert dabei zuerst alle Textbereiche (mit
   * GROUPS-Erweiterung) und dann alle Dokumentkommandos des Kommandobaumes in der
   * Reihenfolge, die DocumentCommandTree.depthFirstIterator(false) liefert.
   */
  synchronized public Iterator<VisibilityElement> visibleElementsIterator()
  {
    Vector<VisibilityElement> visibleElements = new Vector<VisibilityElement>();
    for (Iterator<VisibilityElement> iter = documentCommands.setGroupsIterator(); iter.hasNext();)
      visibleElements.add(iter.next());
    return visibleElements.iterator();
  }

  /**
   * Diese Methode wertet den im Dokument enthaltenen PrintFunction-Abschnitt aus
   * (siehe storePrintFunctions()).
   * 
   * Anmerkungen:
   * 
   * o Das Einlesen von ARG Argumenten ist noch nicht implementiert
   * 
   * o WollMux-Versionen zwischen 2188 (3.10.1) und 2544 (4.4.0) (beides inklusive)
   * schreiben fehlerhafterweise immer ConfigThingy-Syntax. Aus dem Vorhandensein
   * eines ConfigThingys kann also NICHT darauf geschlossen werden, dass tatsächlich
   * Argumente oder mehr als eine Druckfunktion vorhanden sind.
   * 
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void parsePrintFunctions(String data)
  {
    if (data == null || data.length() == 0) return;

    final String errmsg =
      L.m("Fehler beim Einlesen des Druckfunktionen-Abschnitts '%1':", data);

    ConfigThingy conf = new ConfigThingy("dummy");
    try
    {
      conf = new ConfigThingy("dummy", data);
    }
    catch (IOException e)
    {
      Logger.error(errmsg, e);
    }
    catch (SyntaxErrorException e)
    {
      try
      {
        // Abwärtskompatibilität mit älteren PrintFunction-Blöcken, in denen nur
        // der Funktionsname steht:
        ConfigThingy.checkIdentifier(data);
        conf =
          new ConfigThingy("dummy", "WM(Druckfunktionen((FUNCTION '" + data + "')))");
      }
      catch (java.lang.Exception forgetMe)
      {
        // Fehlermeldung des SyntaxFehlers ausgeben
        Logger.error(errmsg, e);
      }
    }

    ConfigThingy functions =
      conf.query("WM").query("Druckfunktionen").queryByChild("FUNCTION");
    for (Iterator<ConfigThingy> iter = functions.iterator(); iter.hasNext();)
    {
      ConfigThingy func = iter.next();
      String name;
      try
      {
        name = func.get("FUNCTION").toString();
      }
      catch (NodeNotFoundException e)
      {
        // kann nicht vorkommen wg. obigem Query
        continue;
      }

      printFunctions.add(name);
    }
  }

  /**
   * Parst den String value als ConfigThingy und überträgt alle in diesem enthaltenen
   * Formular-Abschnitte in die übergebene Formularbeschreibung formularConf.
   * 
   * @param formDesc
   *          Wurzelknoten WM einer Formularbeschreibung dem die neuen
   *          Formular-Abschnitte hinzugefügt werden soll.
   * @param value
   *          darf null oder leer sein und wird in diesem Fall ignoriert; value muss
   *          sich fehlerfrei als ConfigThingy parsen lassen, sonst gibt's eine
   *          Fehlermeldung und es wird nichts hinzugefügt.
   */
  private static void addToFormDescription(ConfigThingy formDesc, String value)
  {
    if (value == null || value.length() == 0) return;

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("", null, new StringReader(value));
    }
    catch (java.lang.Exception e)
    {
      Logger.error(L.m("Die Formularbeschreibung ist fehlerhaft"), e);
      return;
    }

    // enthaltene Formular-Abschnitte übertragen:
    ConfigThingy formulare = conf.query("Formular");
    for (Iterator<ConfigThingy> iter = formulare.iterator(); iter.hasNext();)
    {
      ConfigThingy formular = iter.next();
      formDesc.addChild(formular);
    }
  }

  /**
   * Wertet werteStr aus (das von der Form "WM(FormularWerte(...))" sein muss und
   * überträgt die gefundenen Werte nach formFieldValues.
   * 
   * @param werteStr
   *          darf null sein (und wird dann ignoriert) aber nicht der leere String.
   */
  private void parseFormValues(String werteStr)
  {
    if (werteStr == null) return;

    // Werte-Abschnitt holen:
    ConfigThingy werte;
    try
    {
      ConfigThingy conf = new ConfigThingy("", null, new StringReader(werteStr));
      werte = conf.get("WM").get("Formularwerte");
    }
    catch (java.lang.Exception e)
    {
      Logger.error(L.m("Formularwerte-Abschnitt ist fehlerhaft"), e);
      return;
    }

    // "Formularwerte"-Abschnitt auswerten.
    Iterator<ConfigThingy> iter = werte.iterator();
    while (iter.hasNext())
    {
      ConfigThingy element = iter.next();
      try
      {
        String id = element.get("ID").toString();
        String value = element.get("VALUE").toString();
        formFieldValues.put(id, value);
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Wird derzeit vom DocumentCommandInterpreter aufgerufen und gibt dem
   * TextDocumentModel alle FormFields bekannt, die beim Durchlauf des FormScanners
   * gefunden wurden.
   * 
   * @param idToFormFields
   */
  synchronized public void setIDToFormFields(
      HashMap<String, List<FormFieldFactory.FormField>> idToFormFields)
  {
    this.idToFormFields = idToFormFields;
  }

  /**
   * Diese Methode bestimmt die Vorbelegung der Formularfelder des Formulars und
   * liefert eine HashMap zurück, die die id eines Formularfeldes auf den bestimmten
   * Wert abbildet. Der Wert ist nur dann klar definiert, wenn alle FormFields zu
   * einer ID unverändert geblieben sind, oder wenn nur untransformierte Felder
   * vorhanden sind, die alle den selben Wert enthalten. Gibt es zu einer ID kein
   * FormField-Objekt, so wird der zuletzt abgespeicherte Wert zu dieser ID aus dem
   * FormDescriptor verwendet. Die Methode sollte erst aufgerufen werden, nachdem dem
   * Model mit setIDToFormFields die verfügbaren Formularfelder bekanntgegeben
   * wurden.
   * 
   * @return eine vollständige Zuordnung von Feld IDs zu den aktuellen Vorbelegungen
   *         im Dokument. TESTED
   */
  synchronized public HashMap<String, String> getIDToPresetValue()
  {
    HashMap<String, String> idToPresetValue = new HashMap<String, String>();
    Set<String> ids = new HashSet<String>(formFieldValues.keySet());

    // mapIdToPresetValue vorbelegen: Gibt es zu id mindestens ein untransformiertes
    // Feld, so wird der Wert dieses Feldes genommen. Gibt es kein untransformiertes
    // Feld, so wird der zuletzt im Formularwerte abgespeicherte Wert genommen.
    for (String id : ids)
    {
      List<FormField> fields = new ArrayList<FormField>();
      if (idToFormFields.get(id) != null) fields.addAll(idToFormFields.get(id));
      if (idToTextFieldFormFields.get(id) != null)
        fields.addAll(idToTextFieldFormFields.get(id));

      String value = getFirstUntransformedValue(fields);
      if (value == null) value = formFieldValues.get(id);
      if (value != null) idToPresetValue.put(id, value);
    }

    // Alle id's herauslöschen, deren Felder-Werte nicht konsistent sind.
    for (String id : ids)
    {
      String value = idToPresetValue.get(id);
      if (value != null)
      {
        boolean fieldValuesConsistent =
          fieldValuesConsistent(idToFormFields.get(id), idToPresetValue, value)
            && fieldValuesConsistent(idToTextFieldFormFields.get(id),
              idToPresetValue, value);
        if (!fieldValuesConsistent) idToPresetValue.remove(id);
      }
    }

    // IDs, zu denen keine gültige Vorbelegung vorhanden ist auf FISHY setzen. Das
    // Setzen von FISHY darf erst am Ende der Methode erfolgen, damit FISHY nicht
    // bereits als Wert bei der Transformation berücksichtigt wird.
    for (String id : ids)
    {
      if (!idToPresetValue.containsKey(id))
        idToPresetValue.put(id, TextDocumentModel.FISHY);
    }
    return idToPresetValue;
  }

  /**
   * Diese Methode prüft ob die Formularwerte der in fields enthaltenen
   * Formularfelder konsistent aus den in mapIdToValue enthaltenen Werten abgeleitet
   * werden können; Der Wert value beschreibt dabei den Wert der für
   * FormField-Objekte anzuwenden ist, die untransformiert sind oder deren Methode
   * field.singleParameterTrafo()==true zurück liefert. Ist in fields auch nur ein
   * Formularfeld enthalten, dessen Inhalt nicht konsistent aus diesen Werten
   * abgeleitet werden kann, so liefert die Methode false zurück. Die Methode liefert
   * true zurück, wenn die Konsistenzprüfung für alle Formularfelder erfolgreich
   * durchlaufen wurde.
   * 
   * @param fields
   *          Enthält die Liste der zu prüfenden Felder.
   * @param mapIdToValues
   *          enthält die für evtl. gesetzte Trafofunktionen zu verwendenden
   *          Parameter
   * @param value
   *          enthält den Wert der für untransformierte Felder oder für Felder, deren
   *          Trafofunktion nur einen einheitlichen Wert für sämtliche Parameter
   *          erwartet, verwendet werden soll.
   * @return true, wenn alle Felder konsistent aus den Werten mapIdToValue und value
   *         abgeleitet werden können oder false, falls nicht.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private boolean fieldValuesConsistent(List<FormField> fields,
      HashMap<String, String> mapIdToValues, String value)
  {
    if (fields == null) fields = new ArrayList<FormField>();
    if (mapIdToValues == null) mapIdToValues = new HashMap<String, String>();

    for (FormField field : fields)
    {
      // Soll-Wert refValue bestimmen
      String refValue = value;
      String trafoName = field.getTrafoName();
      if (trafoName != null)
      {
        if (field.singleParameterTrafo())
        {
          refValue = getTransformedValue(trafoName, value);
        }
        else
        {
          // Abbruch, wenn die Parameter für diese Funktion unvollständig sind.
          Function func = getFunctionLibrary().get(trafoName);
          if (func != null) for (String par : func.parameters())
            if (mapIdToValues.get(par) == null) return false;

          refValue = getTransformedValue(trafoName, mapIdToValues);
        }
      }

      // Ist-Wert mit Soll-Wert vergleichen:
      if (!field.getValue().equals(refValue)) return false;
    }
    return true;
  }

  /**
   * Diese Methode iteriert fields und liefert den Wert des ersten gefundenen
   * untransformierten Formularfeldes zurück, oder null, wenn kein untransformiertes
   * Formularfeld gefunden wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private String getFirstUntransformedValue(List<FormField> fields)
  {
    for (FormField field : fields)
    {
      if (field.getTrafoName() == null) return field.getValue();
    }
    return null;
  }

  /**
   * Sammelt alle Formularfelder des Dokuments auf, die nicht von WollMux-Kommandos
   * umgeben sind, jedoch trotzdem vom WollMux verstanden und befüllt werden (derzeit
   * c,s,s,t,textfield,Database-Felder und manche
   * c,s,s,t,textfield,InputUser-Felder).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  synchronized public void collectNonWollMuxFormFields()
  {
    idToTextFieldFormFields.clear();
    staticTextFieldFormFields.clear();

    try
    {
      XEnumeration xenu =
        UNO.XTextFieldsSupplier(doc).getTextFields().createEnumeration();
      while (xenu.hasMoreElements())
      {
        try
        {
          XDependentTextField tf = UNO.XDependentTextField(xenu.nextElement());
          if (tf == null) continue;

          if (UNO.supportsService(tf, "com.sun.star.text.TextField.InputUser"))
          {
            String varName = "" + UNO.getProperty(tf, "Content");
            String funcName = getFunctionNameForUserFieldName(varName);
            if (funcName == null) continue;
            XPropertySet master = getUserFieldMaster(varName);
            FormField f = FormFieldFactory.createInputUserFormField(doc, tf, master);
            Function func = getFunctionLibrary().get(funcName);
            if (func == null)
            {
              Logger.error(L.m(
                "Die im Formularfeld verwendete Funktion '%1' ist nicht definiert.",
                funcName));
              continue;
            }
            String[] pars = func.parameters();
            if (pars.length == 0) staticTextFieldFormFields.add(f);
            for (int i = 0; i < pars.length; i++)
            {
              String id = pars[i];
              if (id != null && id.length() > 0)
              {
                if (!idToTextFieldFormFields.containsKey(id))
                  idToTextFieldFormFields.put(id, new Vector<FormField>());

                List<FormField> formFields = idToTextFieldFormFields.get(id);
                formFields.add(f);
              }
            }
          }

          if (UNO.supportsService(tf, "com.sun.star.text.TextField.Database"))
          {
            XPropertySet master = tf.getTextFieldMaster();
            String id = (String) UNO.getProperty(master, "DataColumnName");
            if (id != null && id.length() > 0)
            {
              if (!idToTextFieldFormFields.containsKey(id))
                idToTextFieldFormFields.put(id, new Vector<FormField>());

              List<FormField> formFields = idToTextFieldFormFields.get(id);
              formFields.add(FormFieldFactory.createDatabaseFormField(doc, tf));
            }
          }
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Der DocumentCommandInterpreter liest sich die Liste der setFragUrls()
   * gespeicherten Fragment-URLs hier aus, wenn die Dokumentkommandos insertContent
   * ausgeführt werden.
   * 
   * @return
   */
  synchronized public String[] getFragUrls()
  {
    return fragUrls;
  }

  /**
   * Über diese Methode kann der openDocument-Eventhandler die Liste der mit einem
   * insertContent-Kommando zu öffnenden frag-urls speichern.
   */
  synchronized public void setFragUrls(String[] fragUrls)
  {
    this.fragUrls = fragUrls;
  }

  /**
   * Registriert das Überschreiben des Textfragments fragId auf den neuen Namen
   * newFragId im TextDocumentModel, wenn das Textfragment fragId nicht bereits
   * überschrieben wurde.
   * 
   * @param fragId
   *          Die alte FRAG_ID, die durch newFragId überschrieben werden soll.
   * @param newFragId
   *          die neue FRAG_ID, die die alte FRAG_ID ersetzt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @throws OverrideFragChainException
   *           Wenn eine fragId oder newFragId bereits Ziel/Quelle einer anderen
   *           Ersetzungsregel sind, dann entsteht eine Ersetzungskette, die nicht
   *           zugelassen ist.
   */
  synchronized public void setOverrideFrag(String fragId, String newFragId)
      throws OverrideFragChainException
  {
    if (overrideFragMap.containsKey(newFragId))
      throw new OverrideFragChainException(newFragId);
    if (overrideFragMap.containsValue(fragId))
      throw new OverrideFragChainException(fragId);
    if (!overrideFragMap.containsKey(fragId))
      overrideFragMap.put(fragId, newFragId);
  }

  public static class OverrideFragChainException extends Exception
  {
    private static final long serialVersionUID = 6792199728784265252L;

    private String fragId;

    public OverrideFragChainException(String fragId)
    {
      this.fragId = fragId;
    }

    @Override
    public String getMessage()
    {
      return L.m(
        "Mit overrideFrag können keine Ersetzungsketten definiert werden, das Fragment '%1' taucht jedoch bereits in einem anderen overrideFrag-Kommando auf.",
        fragId);
    }

  }

  /**
   * Liefert die neue FragId zurück, die anstelle der FRAG_ID fragId verwendet werden
   * soll und durch ein WM(CMD 'overrideFrag'...)-Kommando gesetzt wurde, oder fragId
   * (also sich selbst), wenn keine Überschreibung definiert ist.
   */
  synchronized public String getOverrideFrag(String fragId)
  {
    if (overrideFragMap.containsKey(fragId))
      return overrideFragMap.get(fragId);
    else
      return fragId;
  }

  /**
   * Liefert true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage behandelt
   * werden soll, ansonsten false.
   * 
   * Nicht davon verwirren lassen, dass z.B. in einem Dokument "Unbenannt x", das aus
   * einer Vorlage erstellt wurde, true zurückgeliefert wird, obwohl man ja meinen
   * könnte, dass man eigentlich ein normales Dokument und kein Template bearbeitet.
   * Entscheidend ist in diesem Fall, dass das Dokument aus einem Template erzeugt
   * wurde und nicht einfach eine odt-Datei ist, die man bearbeitet. Dies entspricht
   * letztlich dem Parameter asTemplate der Methode loadComponentFromURL.
   * 
   * Bei WollMux-Mischvorlagen, die mittels setType-Kommando ihren Typ auf
   * "templateTemplate" gesetzt haben, wird entsprechend false zurückgeliefert.
   * 
   * @return true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage behandelt
   *         werden soll, ansonsten false.
   */
  synchronized public boolean isTemplate()
  {
    return isTemplate;
  }

  /**
   * liefert true, wenn das Dokument eine URL besitzt, die die Quelle des Dokuments
   * beschreibt und es sich damit um ein in OOo im "Bearbeiten"-Modus geöffnetes
   * Dokument handelt oder false, wenn das Dokument keine URL besitzt und es sich
   * damit um eine Vorlage handelt.
   * 
   * @return liefert true, wenn das Dokument eine URL besitzt, die die Quelle des
   *         Dokuments beschreibt und es sich damit um ein in OOo im
   *         "Bearbeiten"-Modus geöffnetes Dokument handelt oder false, wenn das
   *         Dokument keine URL besitzt und es sich damit um eine Vorlage handelt.
   */
  synchronized public boolean hasURL()
  {
    return doc.getURL() != null && !doc.getURL().equals("");
  }

  /**
   * Liefert true, wenn das Dokument vom Typ formDocument ist ansonsten false.
   * ACHTUNG: Ein Dokument könnte theoretisch mit einem WM(CMD'setType'
   * TYPE'formDocument') Kommandos als Formulardokument markiert seine, OHNE eine
   * gültige Formularbeschreibung zu besitzen. Dies kann mit der Methode
   * hasFormDescriptor() geprüft werden.
   * 
   * @return Liefert true, wenn das Dokument vom Typ formDocument ist ansonsten
   *         false.
   */
  synchronized public boolean isFormDocument()
  {
    return isFormDocument;
  }

  /**
   * Liefert true, wenn das Dokument ein Teil eines Multiformdokuments ist.
   * 
   * @return Liefert true, wenn das Dokument Teil eines Multiformdokuments ist.
   */
  synchronized public boolean isPartOfMultiformDocument()
  {
    return partOfMultiform;
  }

  synchronized public void setPartOfMultiformDocument(boolean partOfMultiform)
  {
    this.partOfMultiform = partOfMultiform;
  }

  /**
   * Liefert true, wenn das Dokument eine nicht-leere Formularbeschreibung mit einem
   * nicht-leeren Fenster-Abschnitt enthält. In diesem Fall soll die FormGUI
   * gestartet werden.
   */
  synchronized public boolean hasFormGUIWindow()
  {
    try
    {
      return getFormDescription().query("Formular").query("Fenster").getLastChild().count() != 0;
    }
    catch (NodeNotFoundException e)
    {
      return false;
    }
  }

  /**
   * Makiert dieses Dokument als Formulardokument (siehe {@link #isFormDocument()})
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  synchronized public void setFormDocument()
  {
    this.isFormDocument = true;
  }

  /**
   * Setzt abhängig von typeStr die NICHT PRESISTENTEN Zustände {@link #isTemplate()}
   * und {@link #isFormDocument()}, wenn es sich um einen der Dokumenttypen
   * normalTemplate, templateTemplate oder formDocument handelt.
   */
  synchronized public void setType(String typeStr)
  {
    if ("normalTemplate".equalsIgnoreCase(typeStr))
      isTemplate = true;
    else if ("templateTemplate".equalsIgnoreCase(typeStr))
      isTemplate = false;
    else if ("formDocument".equalsIgnoreCase(typeStr)) isFormDocument = true;
  }

  /**
   * Markiert das Dokument als Formulardokument - damit liefert
   * {@link #isFormDocument()} zukünftig true und der Typ "formDocument" wird
   * persistent im Dokument hinterlegt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  synchronized public void markAsFormDocument()
  {
    updateLastTouchedByVersionInfo();
    setType("formDocument");
    persistentData.setData(DataID.SETTYPE, "formDocument");
  }

  /**
   * Diese Methode fügt die Druckfunktion functionName der Menge der dem Dokument
   * zugeordneten Druckfunktionen hinzu. FunctionName muss dabei ein gültiger
   * Funktionsbezeichner sein.
   * 
   * @param functionName
   *          der Name der Druckfunktion, der ein gültiger Funktionsbezeichner sein
   *          und in einem Abschnitt "Druckfunktionen" in der wollmux.conf definiert
   *          sein muss.
   */
  synchronized public void addPrintFunction(String functionName)
  {
    printFunctions.add(functionName);
    storePrintFunctions();

    // Frame veranlassen, die dispatches neu einzulesen - z.B. damit File->Print
    // auch auf die neue Druckfunktion reagiert.
    try
    {
      getFrame().contextChanged();
    }
    catch (java.lang.Exception e)
    {}
  }

  /**
   * Löscht die Druckfunktion functionName aus der Menge der dem Dokument
   * zugeordneten Druckfunktionen.
   * 
   * Wird z.B. in den Sachleitenden Verfügungen verwendet, um auf die ursprünglich
   * gesetzte Druckfunktion zurück zu schalten, wenn keine Verfügungspunkte vorhanden
   * sind.
   * 
   * @param functionName
   *          der Name der Druckfunktion, die aus der Menge gelöscht werden soll.
   */
  synchronized public void removePrintFunction(String functionName)
  {
    if (!printFunctions.remove(functionName)) return;
    storePrintFunctions();

    // Frame veranlassen, die dispatches neu einzulesen - z.B. damit File->Print
    // auch auf gelöschte Druckfunktion reagiert.
    try
    {
      getFrame().contextChanged();
    }
    catch (java.lang.Exception e)
    {}
  }

  /**
   * Schreibt den neuen Zustand der internen HashMap printFunctions in die persistent
   * Data oder löscht den Datenblock, wenn keine Druckfunktion gesetzt ist. Für die
   * Druckfunktionen gibt es 2 Syntaxvarianten. Ist nur eine einzige Druckfunktion
   * gesetzt ohne Parameter, so enthält der Abschnitt nur den Namen der
   * Druckfunktion. Ist entweder mindestens ein Parameter oder mehrere
   * Druckfunktionen gesetzt, so wird stattdessen ein ConfigThingy geschrieben, mit
   * dem Aufbau
   * 
   * <pre>
   * WM(
   *   Druckfunktionen( 
   *     (FUNCTION 'name' ARG 'arg') 
   *          ...
   *     )
   *   )
   * </pre>
   * 
   * . Das Argument ARG ist dabei optional und wird nur gesetzt, wenn ARG nicht leer
   * ist.
   * 
   * Anmerkungen:
   * 
   * o Das Schreiben von ARG Argumenten ist noch nicht implementiert
   * 
   * o WollMux-Versionen zwischen 2188 (3.10.1) und 2544 (4.4.0) (beides inklusive)
   * schreiben fehlerhafterweise immer ConfigThingy-Syntax.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @author Matthias Benkmann (D-III-ITD D.10)
   * 
   */
  private void storePrintFunctions()
  {
    updateLastTouchedByVersionInfo();
    if (printFunctions.isEmpty())
    {
      persistentData.removeData(DataID.PRINTFUNCTION);
    }
    else
    // if (printFunctions.size() > 0)
    {
      /*
       * Momentan ist es noch unnötig umständlich, die Bedingung
       * printFunctions.size() > 1 über eine Variable nach unten zu tunneln. Außerdem
       * ist es derzeit noch so, dass man im Fall printFunctions.size() == 1 erst gar
       * kein ConfigThingy zusammenbauen müsste. Aber wenn einmal Argumente
       * implementiert sind, dann gibt es für deren Vorhandensein vielleicht keinen
       * griffigen Test. In dem Fall ist es am einfachsten unten in der Schleife
       * einfach sobald man auf ein ARG stößt diese Variable hier auf true zu setzen.
       */
      boolean needConfigThingy = (printFunctions.size() > 1);

      // Elemente nach Namen sortieren (definierte Reihenfolge bei der Ausgabe)
      ArrayList<String> names = new ArrayList<String>(printFunctions);
      Collections.sort(names);

      ConfigThingy wm = new ConfigThingy("WM");
      ConfigThingy druckfunktionen = new ConfigThingy("Druckfunktionen");
      wm.addChild(druckfunktionen);
      for (Iterator<String> iter = names.iterator(); iter.hasNext();)
      {
        String name = iter.next();
        ConfigThingy list = new ConfigThingy("");
        ConfigThingy nameConf = new ConfigThingy("FUNCTION");
        nameConf.addChild(new ConfigThingy(name));
        list.addChild(nameConf);
        druckfunktionen.addChild(list);
        // if (Argument vorhanden) needConfigThingy = true;
      }

      if (needConfigThingy)
        persistentData.setData(DataID.PRINTFUNCTION, wm.stringRepresentation());
      else
        persistentData.setData(DataID.PRINTFUNCTION,
          printFunctions.iterator().next());
    }
  }

  /**
   * Liefert eine Menge mit den Namen der aktuell gesetzten Druckfunktionen.
   */
  synchronized public Set<String> getPrintFunctions()
  {
    return printFunctions;
  }

  /**
   * Setzt die FilenameGeneratorFunction, die verwendet wird für die Generierung des
   * Namensvorschlags beim Speichern neuer Dokumente persistent auf die durch
   * ConfigThingy c repräsentierte Funktion oder löscht diese Funktion, wenn c ==
   * null ist.
   */
  synchronized public void setFilenameGeneratorFunc(ConfigThingy c)
  {
    updateLastTouchedByVersionInfo();
    if (c == null)
      persistentData.removeData(DataID.FILENAMEGENERATORFUNC);
    else
      persistentData.setData(DataID.FILENAMEGENERATORFUNC, c.stringRepresentation());
  }

  /**
   * Liefert die aktuell im Dokument gesetzte FilenameGeneratorFunction in Form eines
   * ConfigThingy-Objekts, oder null, wenn keine gültige FilenameGeneratorFunction
   * gesetzt ist.
   */
  synchronized public ConfigThingy getFilenameGeneratorFunc()
  {
    String func = persistentData.getData(DataID.FILENAMEGENERATORFUNC);
    if (func == null) return null;
    try
    {
      return new ConfigThingy("func", func).getFirstChild();
    }
    catch (Exception e)
    {
      return null;
    }
  }

  /**
   * Diese Methode setzt die Eigenschaften "Sichtbar" (visible) und die Anzeige der
   * Hintergrundfarbe (showHighlightColor) für alle Druckblöcke eines bestimmten
   * Blocktyps blockName (z.B. allVersions).
   * 
   * @param blockName
   *          Der Blocktyp dessen Druckblöcke behandelt werden sollen.
   * @param visible
   *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
   *          visible==false.
   * @param showHighlightColor
   *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur, wenn
   *          zu einem betroffenen Druckblock auch eine Hintergrundfarbe angegeben
   *          ist).
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void setPrintBlocksProps(String blockName, boolean visible,
      boolean showHighlightColor)
  {
    updateLastTouchedByVersionInfo();

    Iterator<DocumentCommand> iter = new HashSet<DocumentCommand>().iterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_ALL_VERSIONS.equals(blockName))
      iter = documentCommands.allVersionsIterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_DRAFT_ONLY.equals(blockName))
      iter = documentCommands.draftOnlyIterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_NOT_IN_ORIGINAL.equals(blockName))
      iter = documentCommands.notInOriginalIterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_ORIGINAL_ONLY.equals(blockName))
      iter = documentCommands.originalOnlyIterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_COPY_ONLY.equals(blockName))
      iter = documentCommands.copyOnlyIterator();

    while (iter.hasNext())
    {
      DocumentCommand cmd = iter.next();
      cmd.setVisible(visible);
      String highlightColor =
        ((OptionalHighlightColorProvider) cmd).getHighlightColor();

      if (highlightColor != null)
      {
        if (showHighlightColor)
          try
          {
            Integer bgColor = Integer.valueOf(Integer.parseInt(highlightColor, 16));
            UNO.setProperty(cmd.getTextCursor(), "CharBackColor", bgColor);
          }
          catch (NumberFormatException e)
          {
            Logger.error(L.m(
              "Fehler in Dokumentkommando '%1': Die Farbe HIGHLIGHT_COLOR mit dem Wert '%2' ist ungültig.",
              "" + cmd, highlightColor));
          }
        else
        {
          UNO.setPropertyToDefault(cmd.getTextCursor(), "CharBackColor");
        }
      }

    }
  }

  synchronized public void setVisibleState(String groupId, boolean visible)
  {
    try
    {
      Map<String, Boolean> groupState = mapGroupIdToVisibilityState;
      if (simulationResult != null)
        groupState = simulationResult.getGroupsVisibilityState();

      groupState.put(groupId, visible);

      VisibilityElement firstChangedElement = null;

      // Sichtbarkeitselemente durchlaufen und alle ggf. updaten:
      Iterator<VisibilityElement> iter = documentCommands.setGroupsIterator();
      while (iter.hasNext())
      {
        VisibilityElement visibleElement = iter.next();
        Set<String> groups = visibleElement.getGroups();
        if (!groups.contains(groupId)) continue;

        // Visibility-Status neu bestimmen:
        boolean setVisible = true;
        for (String gid : groups)
        {
          if (groupState.get(gid).equals(Boolean.FALSE)) setVisible = false;
        }

        // Element merken, dessen Sichtbarkeitsstatus sich zuerst ändert und
        // den focus (ViewCursor) auf den Start des Bereichs setzen. Da das
        // Setzen eines ViewCursors in einen unsichtbaren Bereich nicht
        // funktioniert, wird die Methode focusRangeStart zwei mal aufgerufen,
        // je nach dem, ob der Bereich vor oder nach dem Setzen des neuen
        // Sichtbarkeitsstatus sichtbar ist.
        if (setVisible != visibleElement.isVisible() && firstChangedElement == null)
        {
          firstChangedElement = visibleElement;
          if (firstChangedElement.isVisible()) focusRangeStart(visibleElement);
        }

        // neuen Sichtbarkeitsstatus setzen:
        try
        {
          visibleElement.setVisible(setVisible);
        }
        catch (RuntimeException e)
        {
          // Absicherung gegen das manuelle Löschen von Dokumentinhalten
        }
      }

      // Den Cursor (nochmal) auf den Anfang des Ankers des Elements setzen,
      // dessen Sichtbarkeitsstatus sich zuerst geändert hat (siehe Begründung
      // oben).
      if (firstChangedElement != null && firstChangedElement.isVisible())
        focusRangeStart(firstChangedElement);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Diese Methode setzt den ViewCursor auf den Anfang des Ankers des
   * Sichtbarkeitselements.
   * 
   * @param visibleElement
   *          Das Sichtbarkeitselement, auf dessen Anfang des Ankers der ViewCursor
   *          gesetzt werden soll.
   */
  private void focusRangeStart(VisibilityElement visibleElement)
  {
    try
    {
      getViewCursor().gotoRange(visibleElement.getAnchor().getStart(), false);
    }
    catch (java.lang.Exception e)
    {}
  }

  /**
   * Liefert eine SetJumpMark zurück, der das erste setJumpMark-Dokumentkommandos
   * dieses Dokuments enthält oder null falls kein solches Dokumentkommando vorhanden
   * ist.
   * 
   * @return Liefert eine SetJumpMark zurück, der das erste
   *         setJumpMark-Dokumentkommandos dieses Dokuments enthält oder null falls
   *         kein solches Dokumentkommando vorhanden ist.
   */
  synchronized public SetJumpMark getFirstJumpMark()
  {
    return documentCommands.getFirstJumpMark();
  }

  /**
   * Diese Methode liefert die FeldIDs aller im Dokument enthaltenen Felder.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public Set<String> getAllFieldIDs()
  {
    HashSet<String> ids = new HashSet<String>();
    ids.addAll(idToFormFields.keySet());
    ids.addAll(idToTextFieldFormFields.keySet());
    return ids;
  }

  /**
   * Liefert die zum aktuellen Stand gesetzten Formularwerte in einer Map mit ID als
   * Schlüssel. Änderungen an der zurückgelieferten Map zeigen keine Wirkung im
   * TextDocumentModel (da nur eine Kopie der internen Map zurückgegeben wird).
   * 
   * Befindet sich das TextDocumentModel in einem über {@link #startSimulation()}
   * gesetzten Simulationslauf, so werden die im Simulationslauf gesetzten Werte
   * zurück geliefert, die nicht zwangsweise mit den reell gesetzten Werten
   * übereinstimmen müssen.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public Map<String, String> getFormFieldValues()
  {
    if (simulationResult == null)
      return new HashMap<String, String>(formFieldValues);
    else
      return new HashMap<String, String>(simulationResult.getFormFieldValues());
  }

  /**
   * Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein Controller
   * (oder auch kein ViewCursor) für das Dokument verfügbar ist.
   * 
   * @return Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein
   *         Controller (oder auch kein ViewCursor) für das Dokument verfügbar ist.
   */
  synchronized public XTextViewCursor getViewCursor()
  {
    if (UNO.XModel(doc) == null) return null;
    XTextViewCursorSupplier suppl =
      UNO.XTextViewCursorSupplier(UNO.XModel(doc).getCurrentController());
    if (suppl != null) return suppl.getViewCursor();
    return null;
  }

  /**
   * Diese Methode liefert true, wenn der viewCursor im Dokument aktuell nicht
   * kollabiert ist und damit einen markierten Bereich aufspannt, andernfalls false.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public boolean hasSelection()
  {
    XTextViewCursor vc = getViewCursor();
    if (vc != null)
    {
      return !vc.isCollapsed();
    }
    return false;
  }

  /**
   * Entfernt alle Bookmarks, die keine WollMux-Bookmarks sind aus dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  synchronized public void removeNonWMBookmarks()
  {
    updateLastTouchedByVersionInfo();

    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc);
    XNameAccess bookmarks = bmSupp.getBookmarks();
    String[] names = bookmarks.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try
      {
        String bookmark = names[i];
        if (!WOLLMUX_BOOKMARK_PATTERN.matcher(bookmark).matches())
        {
          XTextContent bm = UNO.XTextContent(bookmarks.getByName(bookmark));
          bm.getAnchor().getText().removeTextContent(bm);
        }

      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
  }

  /**
   * Entfernt die WollMux-Kommandos "insertFormValue", "setGroups", "setType
   * formDocument" und "form", sowie die WollMux-Formularbeschreibung und Daten aus
   * dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  synchronized public void deForm()
  {
    updateLastTouchedByVersionInfo();

    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(doc);
    XNameAccess bookmarks = bmSupp.getBookmarks();
    String[] names = bookmarks.getElementNames();
    for (int i = 0; i < names.length; ++i)
    {
      try
      {
        String bookmark = names[i];
        if (BOOKMARK_KILL_PATTERN.matcher(bookmark).matches())
        {
          XTextContent bm = UNO.XTextContent(bookmarks.getByName(bookmark));
          bm.getAnchor().getText().removeTextContent(bm);
        }

      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    persistentData.removeData(DataID.FORMULARBESCHREIBUNG);
    persistentData.removeData(DataID.FORMULARWERTE);
  }

  /**
   * Fügt an Stelle der aktuellen Selektion ein Serienbrieffeld ein, das auf die
   * Spalte fieldId zugreift und mit dem Wert "" vorbelegt ist, falls noch kein Wert
   * für fieldId gesetzt wurde. Das Serienbrieffeld wird im WollMux registriert und
   * kann damit sofort verwendet werden.
   */
  synchronized public void insertMailMergeFieldAtCursorPosition(String fieldId)
  {
    updateLastTouchedByVersionInfo();
    insertMailMergeField(fieldId, getViewCursor());
  }

  /**
   * Fügt an Stelle der aktuellen Selektion ein "Nächster Datensatz"-Feld für den
   * OOo-basierten Seriendruck ein.
   */
  synchronized public void insertNextDatasetFieldAtCursorPosition()
  {
    updateLastTouchedByVersionInfo();
    insertNextDatasetField(getViewCursor());
  }

  /**
   * Stellt sicher, dass persistente Daten dieses Dokuments auch tatsächlich
   * persistiert werden.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  synchronized public void flushPersistentData()
  {
    persistentData.flush();
  }

  /**
   * Fügt an Stelle range ein Serienbrieffeld ein, das auf die Spalte fieldId
   * zugreift und mit dem Wert "" vorbelegt ist, falls noch kein Wert für fieldId
   * gesetzt wurde. Das Serienbrieffeld wird im WollMux registriert und kann damit
   * sofort verwendet werden.
   */
  private void insertMailMergeField(String fieldId, XTextRange range)
  {
    updateLastTouchedByVersionInfo();

    if (fieldId == null || fieldId.length() == 0 || range == null) return;
    try
    {
      // Feld einfügen
      XMultiServiceFactory factory = UNO.XMultiServiceFactory(doc);
      XDependentTextField field =
        UNO.XDependentTextField(factory.createInstance("com.sun.star.text.TextField.Database"));
      XPropertySet master =
        UNO.XPropertySet(factory.createInstance("com.sun.star.text.FieldMaster.Database"));
      UNO.setProperty(master, "DataBaseName", "DataBase");
      UNO.setProperty(master, "DataTableName", "Table");
      UNO.setProperty(master, "DataColumnName", fieldId);
      if (!formFieldPreviewMode)
        UNO.setProperty(field, "Content", "<" + fieldId + ">");
      field.attachTextFieldMaster(master);

      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      cursor.getText().insertTextContent(cursor, field, true);
      cursor.collapseToEnd();

      // Feldwert mit leerem Inhalt vorbelegen
      if (!formFieldValues.containsKey(fieldId)) setFormFieldValue(fieldId, "");

      // Formularfeld bekanntmachen, damit es vom WollMux verwendet wird.
      if (!idToTextFieldFormFields.containsKey(fieldId))
        idToTextFieldFormFields.put(fieldId, new Vector<FormField>());
      List<FormField> formFields = idToTextFieldFormFields.get(fieldId);
      formFields.add(FormFieldFactory.createDatabaseFormField(doc, field));

      // Ansicht des Formularfeldes aktualisieren:
      updateFormFields(fieldId);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Fügt an Stelle range ein "Nächster Datensatz"-Feld für den OOo-basierten
   * Seriendruck ein.
   */
  private void insertNextDatasetField(XTextRange range)
  {
    updateLastTouchedByVersionInfo();

    try
    {
      // Feld einfügen
      XMultiServiceFactory factory = UNO.XMultiServiceFactory(doc);
      XDependentTextField field =
        UNO.XDependentTextField(factory.createInstance("com.sun.star.text.TextField.DatabaseNextSet"));
      UNO.setProperty(field, "DataBaseName", "DataBaseName");
      UNO.setProperty(field, "DataTableName", "DataTableName");
      UNO.setProperty(field, "DataCommandType", com.sun.star.sdb.CommandType.TABLE);
      UNO.setProperty(field, "Condition", "true");

      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      cursor.getText().insertTextContent(cursor, field, true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Liefert die aktuelle Formularbeschreibung des Dokuments; Wurde die
   * Formularbeschreibung bis jetzt noch nicht eingelesen, so wird sie spätestens
   * jetzt eingelesen.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  public synchronized ConfigThingy getFormDescription()
  {
    if (formularConf == null)
    {
      Logger.debug(L.m("Einlesen der Formularbeschreibung von %1", this));
      formularConf = new ConfigThingy("WM");
      addToFormDescription(formularConf,
        persistentData.getData(DataID.FORMULARBESCHREIBUNG));
      formularConf = applyFormularanpassung(formularConf);

      ConfigThingy title = formularConf.query("TITLE");
      if (title.count() > 0)
        Logger.debug(L.m("Formular %1 eingelesen.", title.stringRepresentation(true,
          '\'')));
    }

    return formularConf;
  }

  /**
   * Wendet alle matchenden "Formularanpassung"-Abschnitte in der Reihenfolge ihres
   * auftretends in der wollmux,conf auf formularConf an und liefert das Ergebnis
   * zurück. Achtung! Das zurückgelieferte Objekt kann das selbe Objekt sein wie das
   * übergebene.
   * 
   * @param formularConf
   *          ein "WM" Knoten unterhalb dessen sich eine normale Formularbeschreibung
   *          befindet ("Formular" Knoten).
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private ConfigThingy applyFormularanpassung(ConfigThingy formularConf)
  {
    ConfigThingy anpassungen =
        WollMuxFiles.getWollmuxConf().query("Formularanpassung", 1);
    if (anpassungen.count() == 0) return formularConf;

    try
    {
      ConfigThingy formularConfOld = formularConf;
      formularConf = formularConf.getFirstChild(); // Formular-Knoten
      if (!formularConf.getName().equals("Formular")) return formularConfOld;
    }
    catch (NodeNotFoundException x)
    {
      return formularConf;
    }

    process_anpassung: for (ConfigThingy conf : anpassungen)
    {
      ConfigThingy matches = conf.query("Match", 1);
      for (ConfigThingy matchConf : matches)
      {
        for (ConfigThingy subMatchConf : matchConf)
        {
          if (!matches(formularConf, subMatchConf)) continue process_anpassung;
        }
      }

      ConfigThingy formularAnpassung = conf.query("Formular", 1);
      List<ConfigThingy> mergeForms = new ArrayList<ConfigThingy>(2);
      mergeForms.add(formularConf);
      String title = "";
      try
      {
        title = formularConf.get("TITLE", 1).toString();
      }
      catch (Exception x)
      {}
      try
      {
        mergeForms.add(formularAnpassung.getFirstChild());
      }
      catch (NodeNotFoundException x)
      {}
      ConfigThingy buttonAnpassung = conf.query("Buttonanpassung");
      if (buttonAnpassung.count() == 0) buttonAnpassung = null;
      formularConf =
        TextDocumentModel.mergeFormDescriptors(mergeForms, buttonAnpassung, title);
    }

    ConfigThingy formularConfWithWM = new ConfigThingy("WM");
    formularConfWithWM.addChild(formularConf);
    return formularConfWithWM;
  }

  /**
   * Liefert true, wenn der Baum, der durch conf dargestellt wird sich durch
   * Herauslöschen von Knoten in den durch matchConf dargestellten Baum überführen
   * lässt. Herauslöschen bedeutet in diesem Fall bei einem inneren Knoten, dass
   * seine Kinder seinen Platz einnehmen.
   * 
   * Anmerkung: Die derzeitige Implementierung setzt die obige Spezifikation nicht
   * korrekt um, da {@link ConfigThingy#query(String)} nur die Ergebnisse auf einer
   * Ebene zurückliefert. In der Praxis sollten jedoch keine Fälle auftreten wo dies
   * zum Problem wird.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private boolean matches(ConfigThingy conf, ConfigThingy matchConf)
  {
    ConfigThingy resConf = conf.query(matchConf.getName());
    if (resConf.count() == 0) return false;
    testMatch: for (ConfigThingy subConf : resConf)
    {
      for (ConfigThingy subMatchConf : matchConf)
      {
        if (!matches(subConf, subMatchConf)) continue testMatch;
      }

      return true;
    }
    return false;
  }

  /**
   * Liefert den Seriendruck-Knoten der im Dokument gespeicherten
   * Seriendruck-Metadaten zurück. Die Metadaten liegen im Dokument beispielsweise in
   * der Form "WM(Seriendruck(Datenquelle(...)))" vor - diese Methode liefert aber
   * nur der Knoten "Seriendruck" zurück. Enthält das Dokument keine
   * Seriendruck-Metadaten, so liefert diese Methode einen leeren
   * "Seriendruck"-Knoten zurück.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  synchronized public ConfigThingy getMailmergeConfig()
  {
    if (mailmergeConf == null)
    {
      String data = persistentData.getData(DataID.SERIENDRUCK);
      mailmergeConf = new ConfigThingy("Seriendruck");
      if (data != null)
        try
        {
          mailmergeConf =
            new ConfigThingy("", data).query("WM").query("Seriendruck").getLastChild();
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
    }
    return mailmergeConf;
  }

  /**
   * Diese Methode speichert die als Kinder von conf übergebenen Metadaten für den
   * Seriendruck persistent im Dokument oder löscht die Metadaten aus dem Dokument,
   * wenn conf keine Kinder besitzt. conf kann dabei ein beliebig benannter Konten
   * sein, dessen Kinder müssen aber gültige Schlüssel des Abschnitts
   * WM(Seriendruck(...) darstellen. So ist z.B. "Datenquelle" ein gültiger
   * Kindknoten von conf.
   * 
   * @param conf
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  synchronized public void setMailmergeConfig(ConfigThingy conf)
  {
    updateLastTouchedByVersionInfo();

    mailmergeConf = new ConfigThingy("Seriendruck");
    for (Iterator<ConfigThingy> iter = conf.iterator(); iter.hasNext();)
    {
      ConfigThingy c = new ConfigThingy(iter.next());
      mailmergeConf.addChild(c);
    }
    ConfigThingy wm = new ConfigThingy("WM");
    wm.addChild(mailmergeConf);
    if (mailmergeConf.count() > 0)
      persistentData.setData(DataID.SERIENDRUCK, wm.stringRepresentation());
    else
      persistentData.removeData(DataID.SERIENDRUCK);
  }

  /**
   * Liefert einen Funktionen-Abschnitt der Formularbeschreibung, in dem die lokalen
   * Auto-Funktionen abgelegt werden können. Besitzt die Formularbeschreibung keinen
   * Funktionen-Abschnitt, so wird der Funktionen-Abschnitt und ggf. auch ein
   * übergeordneter Formular-Abschnitt neu erzeugt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private ConfigThingy getFunktionenConf()
  {
    ConfigThingy formDesc = getFormDescription();
    try
    {
      return formDesc.query("Formular").query("Funktionen").getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      ConfigThingy funktionen = new ConfigThingy("Funktionen");
      ConfigThingy formular;
      try
      {
        formular = formDesc.query("Formular").getLastChild();
      }
      catch (NodeNotFoundException e1)
      {
        formular = new ConfigThingy("Formular");
        formDesc.addChild(formular);
      }
      formular.addChild(funktionen);
      return funktionen;
    }
  }

  /**
   * Speichert die aktuelle Formularbeschreibung in den persistenten Daten des
   * Dokuments oder löscht den entsprechenden Abschnitt aus den persistenten Daten,
   * wenn die Formularbeschreibung nur aus einer leeren Struktur ohne eigentlichen
   * Formularinhalt besteht.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void storeCurrentFormDescription()
  {
    updateLastTouchedByVersionInfo();

    ConfigThingy conf = getFormDescription();
    try
    {
      if ((conf.query("Fenster").count() > 0 && conf.get("Fenster").count() > 0)
        || (conf.query("Sichtbarkeit").count() > 0 && conf.get("Sichtbarkeit").count() > 0)
        || (conf.query("Funktionen").count() > 0 && conf.get("Funktionen").count() > 0))
        persistentData.setData(DataID.FORMULARBESCHREIBUNG,
          conf.stringRepresentation());
      else
        persistentData.removeData(DataID.FORMULARBESCHREIBUNG);
    }
    catch (NodeNotFoundException e)
    {
      Logger.error(L.m("Dies kann nicht passieren."), e);
    }
  }

  /**
   * Ersetzt die Formularbeschreibung dieses Dokuments durch die aus conf. Falls conf
   * == null, so wird die Formularbeschreibung gelöscht. ACHTUNG! conf wird nicht
   * kopiert sondern als Referenz eingebunden.
   * 
   * @param conf
   *          ein WM-Knoten, der "Formular"-Kinder hat. Falls conf == null, so wird
   *          die Formularbeschreibungsnotiz gelöscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  synchronized public void setFormDescription(ConfigThingy conf)
  {
    if (conf != null)
      formularConf = conf;
    else
      formularConf = new ConfigThingy("WM");
    storeCurrentFormDescription();
    setDocumentModified(true);
  }

  /**
   * Speichert den neuen Wert value zum Formularfeld fieldId im
   * Formularwerte-Abschnitt in den persistenten Daten oder löscht den Eintrag für
   * fieldId aus den persistenten Daten, wenn value==null ist. ACHTUNG! Damit der
   * neue Wert angezeigt wird, ist ein Aufruf von {@link #updateFormFields(String)}
   * erforderlich.
   * 
   * Befindet sich das TextDocumentModel in einem über {@link #startSimulation()}
   * gestarteten Simulationslauf, so werden die persistenten Daten nicht verändert
   * und der neue Wert nur in einem internen Objekt des Simulationslaufs gespeichert
   * anstatt im Dokument.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  synchronized public void setFormFieldValue(String fieldId, String value)
  {
    if (simulationResult == null)
    {
      updateLastTouchedByVersionInfo();
      if (value == null)
        formFieldValues.remove(fieldId);
      else
        formFieldValues.put(fieldId, value);
      persistentData.setData(DataID.FORMULARWERTE, getFormFieldValuesString());
    }
    else
      simulationResult.setFormFieldValue(fieldId, value);
  }

  /**
   * Serialisiert die aktuellen Werte aller Fomularfelder.
   */
  private String getFormFieldValuesString()
  {
    // Neues ConfigThingy für "Formularwerte"-Abschnitt erzeugen:
    ConfigThingy werte = new ConfigThingy("WM");
    ConfigThingy formwerte = new ConfigThingy("Formularwerte");
    werte.addChild(formwerte);
    for (Map.Entry<String, String> ent : formFieldValues.entrySet())
    {
      String key = ent.getKey();
      String value = ent.getValue();
      if (key != null && value != null)
      {
        ConfigThingy entry = new ConfigThingy("");
        ConfigThingy cfID = new ConfigThingy("ID");
        cfID.add(key);
        ConfigThingy cfVALUE = new ConfigThingy("VALUE");
        cfVALUE.add(value);
        entry.addChild(cfID);
        entry.addChild(cfVALUE);
        formwerte.addChild(entry);
      }
    }

    return werte.stringRepresentation();
  }

  /**
   * Liefert den Kontext mit dem die dokumentlokalen Dokumentfunktionen beim Aufruf
   * von getFunctionLibrary() und getDialogLibrary() erzeugt werden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public Map<Object, Object> getFunctionContext()
  {
    return functionContext;
  }

  /**
   * Liefert die Funktionsbibliothek mit den globalen Funktionen des WollMux und den
   * lokalen Funktionen dieses Dokuments.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized FunctionLibrary getFunctionLibrary()
  {
    if (functionLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = getFormDescription().get("Formular");
      }
      catch (NodeNotFoundException e)
      {}
      functionLib =
        FunctionFactory.parseFunctions(formConf, getDialogLibrary(), functionContext,
          GlobalFunctions.getInstance().getGlobalFunctions());
    }
    return functionLib;
  }

  /**
   * Liefert die eine Bibliothek mit den globalen Dialogfunktionen des WollMux und
   * den lokalen Dialogfunktionen dieses Dokuments.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized DialogLibrary getDialogLibrary()
  {
    if (dialogLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = getFormDescription().get("Formular");
      }
      catch (NodeNotFoundException e)
      {}
      dialogLib =
        DialogFactory.parseFunctionDialogs(formConf,
          GlobalFunctions.getInstance().getFunctionDialogs(), functionContext);
    }
    return dialogLib;
  }

  /**
   * Erzeugt in der Funktionsbeschreibung eine neue Funktion mit einem automatisch
   * generierten Namen, registriert sie in der Funktionsbibliothek, so dass diese
   * sofort z.B. als TRAFO-Funktion genutzt werden kann und liefert den neuen
   * generierten Funktionsnamen zurück oder null, wenn funcConf fehlerhaft ist.
   * 
   * Der automatisch generierte Name ist, nach dem Prinzip
   * PRAEFIX_aktuelleZeitinMillisekunden_zahl aufgebaut. Es wird aber in jedem Fall
   * garantiert, dass der neue Name eindeutig ist und nicht bereits in der
   * Funktionsbibliothek vorkommt.
   * 
   * @param funcConf
   *          Ein ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION )",
   *          wobei Bezeichner ein beliebiger Bezeichner ist und FUNKTIONSDEFINITION
   *          ein erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   *          , d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine erlaubter
   *          Funktionsname, z.B. "AND" sein. Der Bezeichner wird NICHT als Name der
   *          TRAFO verwendet. Stattdessen wird ein neuer eindeutiger TRAFO-Name
   *          generiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private String addLocalAutofunction(ConfigThingy funcConf)
  {
    FunctionLibrary funcLib = getFunctionLibrary();
    DialogLibrary dialogLib = getDialogLibrary();
    Map<Object, Object> context = getFunctionContext();

    // eindeutigen Namen für die neue Autofunktion erzeugen:
    Set<String> currentFunctionNames = funcLib.getFunctionNames();
    String name = null;
    for (int i = 0; name == null || currentFunctionNames.contains(name); ++i)
      name = AUTOFUNCTION_PREFIX + System.currentTimeMillis() + "_" + i;

    try
    {
      funcLib.add(name, FunctionFactory.parseChildren(funcConf, funcLib, dialogLib,
        context));

      // Funktion zur Formularbeschreibung hinzufügen:
      ConfigThingy betterNameFunc = new ConfigThingy(name);
      for (Iterator<ConfigThingy> iter = funcConf.iterator(); iter.hasNext();)
      {
        ConfigThingy func = iter.next();
        betterNameFunc.addChild(func);
      }
      getFunktionenConf().addChild(betterNameFunc);

      storeCurrentFormDescription();
      return name;
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(e);
      return null;
    }
  }

  /**
   * Im Vorschaumodus überträgt diese Methode den Formularwert zum Feldes fieldId aus
   * dem persistenten Formularwerte-Abschnitt in die zugehörigen Formularfelder im
   * Dokument; Ist der Vorschaumodus nicht aktiv, so werden jeweils nur die
   * Spaltennamen in spitzen Klammern angezeigt; Für die Auflösung der TRAFOs wird
   * dabei die Funktionsbibliothek funcLib verwendet. Außerdem wird der
   * Modified-Status des Dokuments gesetzt.
   * 
   * Befindet sich das TextDocumentModel in einem über {@link #startSimulation()}
   * gestarteten Simulationslauf, so wird der Update der von fieldId abhängigen
   * Formularelemente nur simuliert und es der Modified-Status des Dokuments wird
   * nicht gesetzt.
   * 
   * @param fieldId
   *          Die ID des Formularfeldes bzw. der Formularfelder, die im Dokument
   *          angepasst werden sollen.
   */
  synchronized public void updateFormFields(String fieldId)
  {
    if (formFieldPreviewMode)
    {
      String value = formFieldValues.get(fieldId);
      if (simulationResult != null)
        value = simulationResult.getFormFieldValues().get(fieldId);
      if (value == null) value = "";
      setFormFields(fieldId, value, true);
    }
    else
    {
      setFormFields(fieldId, "<" + fieldId + ">", false);
    }
    if (simulationResult == null) setDocumentModified(true);
  }

  /**
   * Im Vorschaumodus überträgt diese Methode alle Formularwerte aus dem
   * Formularwerte-Abschnitt der persistenten Daten in die zugehörigen Formularfelder
   * im Dokument, wobei evtl. gesetzte Trafo-Funktionen ausgeführt werden; Ist der
   * Vorschaumodus nicht aktiv, so werden jeweils nur die Spaltennamen in spitzen
   * Klammern angezeigt.
   */
  private void updateAllFormFields()
  {
    for (Iterator<String> iter = getAllFieldIDs().iterator(); iter.hasNext();)
    {
      String fieldId = iter.next();
      updateFormFields(fieldId);
    }
  }

  /**
   * Setzt den Inhalt aller Formularfelder mit ID fieldId auf value.
   * 
   * @param applyTrafo
   *          gibt an, ob eine evtl. vorhandene TRAFO-Funktion angewendet werden soll
   *          (true) oder nicht (false).
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  private void setFormFields(String fieldId, String value, boolean applyTrafo)
  {
    setFormFields(idToFormFields.get(fieldId), value, applyTrafo, false);
    setFormFields(idToTextFieldFormFields.get(fieldId), value, applyTrafo, true);
    setFormFields(staticTextFieldFormFields, value, applyTrafo, true);
  }

  /**
   * Setzt den Inhalt aller Formularfelder aus der Liste formFields auf value und
   * wendet dabei ggf; (abhängig von applyTrafo und useKnownFormValues) die für die
   * Formularfelder korrekte Transformation an; Wenn simulateResult != null ist, so
   * werden die Werte nicht tatsächlich gesetzt, sondern das Setzen in die HashMap
   * simulateResult simuliert. formFields kann null sein, dann passiert nichts.
   * 
   * @param applyTrafo
   *          gibt an ob eine evtl. vorhandenen Trafofunktion verwendet werden soll.
   * @param useKnownFormValues
   *          gibt an, ob die Trafofunktion mit den bekannten Formularwerten (true)
   *          als Parameter, oder ob alle erwarteten Parameter mit dem Wert value
   *          (false) versorgt werden - wird aus Gründen der Abwärtskompatiblität zu
   *          den bisherigen insertFormValue-Kommandos benötigt.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  private void setFormFields(List<FormField> formFields, String value,
      boolean applyTrafo, boolean useKnownFormValues)
  {
    if (formFields == null) return;

    if (simulationResult == null) updateLastTouchedByVersionInfo();

    for (FormField field : formFields)
      try
      {
        String result;
        String trafoName = field.getTrafoName();
        if (trafoName != null && applyTrafo)
        {
          if (useKnownFormValues)
            result = getTransformedValue(trafoName);
          else
            result = getTransformedValue(trafoName, value);
        }
        else
          result = value;

        if (simulationResult == null)
          field.setValue(result);
        else
          simulationResult.setFormFieldContent(field, result);
      }
      catch (RuntimeException e)
      {
        // Absicherung gegen das manuelle Löschen von Dokumentinhalten.
      }
  }

  /**
   * Schaltet den Vorschaumodus für Formularfelder an oder aus - ist der
   * Vorschaumodus aktiviert, so werden alle Formularfelder mit den zuvor gesetzten
   * Formularwerten angezeigt, ist der Preview-Modus nicht aktiv, so werden nur die
   * Spaltennamen in spitzen Klammern angezeigt.
   * 
   * @param previewMode
   *          true schaltet den Modus an, false schaltet auf den Vorschaumodus zurück
   *          in dem die aktuell gesetzten Werte wieder angezeigt werden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void setFormFieldsPreviewMode(boolean previewMode)
  {
    this.formFieldPreviewMode = previewMode;
    updateAllFormFields();
    cleanupGarbageOfUnreferencedAutofunctions();
  }

  /**
   * Setzt den ViewCursor auf das erste untransformierte Formularfeld, das den
   * Formularwert mit der ID fieldID darstellt. Falls kein untransformiertes
   * Formularfeld vorhanden ist, wird ein transformiertes gewählt.
   * 
   * @param fieldId
   *          Die ID des Formularfeldes, das angesprungen werden soll.
   */
  synchronized public void focusFormField(String fieldId)
  {
    FormField field = null;
    List<FormField> formFields = idToTextFieldFormFields.get(fieldId);
    if (formFields != null)
    {
      field = formFields.get(0);
    }
    else
    {
      formFields = idToFormFields.get(fieldId);
      field = preferUntransformedFormField(formFields);
    }

    try
    {
      if (field != null) field.focus();
    }
    catch (RuntimeException e)
    {
      // Absicherung gegen das manuelle Löschen von Dokumentinhalten.
    }
  }

  /**
   * Wenn in der übergebenen {@link List} mit FormField-Elementen ein
   * nicht-transformiertes Feld vorhanden ist, so wird das erste nicht-transformierte
   * Feld zurückgegeben, ansonsten wird das erste transformierte Feld zurückgegeben,
   * oder null, falls die Liste keine Elemente enthält bzw. null ist.
   * 
   * @param formFields
   *          Liste mit FormField-Elementen
   * @return Ein FormField Element, wobei untransformierte Felder bevorzugt werden.
   */
  protected static FormField preferUntransformedFormField(List<FormField> formFields)
  {
    if (formFields == null) return null;
    Iterator<FormField> iter = formFields.iterator();
    FormField field = null;
    while (iter.hasNext())
    {
      FormField f = iter.next();
      if (field == null) field = f;
      if (f.getTrafoName() == null) return f;
    }
    return field;
  }

  /**
   * Diese Methode führt die Trafofunktion trafoName aus, wobei die Werte der
   * erwarteten Parameter aus mapIdToValues gewonnen werden, und liefert das
   * transformierte Ergebnis zurück. Die Trafofunktion trafoName darf nicht null sein
   * und muss global oder dokumentlokal definiert sein; Ist die Transfofunktion nicht
   * in der globalen oder dokumentlokalen Funktionsbibliothek enthalten, so wird ein
   * Fehlerstring zurückgeliefert und eine weitere Fehlermeldung in die Log-Datei
   * geschrieben.
   * 
   * @param trafoName
   *          Der Name der Trafofunktion, der nicht null sein darf.
   * @param mapIdToValues
   *          eine Zuordnung von ids auf die zugehörigen Werte, aus der die Werte für
   *          die von der Trafofunktion erwarteten Parameter bestimmt werden.
   * @return Der transformierte Wert falls die Trafo definiert ist oder ein
   *         Fehlerstring, falls die Trafo nicht definiert ist.
   */
  private String getTransformedValue(String trafoName,
      Map<String, String> mapIdToValues)
  {
    Function func = getFunctionLibrary().get(trafoName);
    if (func != null)
    {
      SimpleMap args = new SimpleMap();
      String[] pars = func.parameters();
      for (int i = 0; i < pars.length; i++)
        args.put(pars[i], mapIdToValues.get(pars[i]));
      return func.getString(args);
    }
    else
    {
      Logger.error(L.m("Die TRAFO '%1' ist nicht definiert.", trafoName));
      return L.m("<FEHLER: TRAFO '%1' nicht definiert>", trafoName);
    }
  }

  /**
   * Diese Methode führt die Trafofunktion trafoName aus, übergibt ihr dabei die
   * aktuell dem TextDocumentModel bekannten Formularwerte als Parameter und liefert
   * das transformierte Ergebnis zurück; Die Trafofunktion trafoName darf nicht null
   * sein und muss global oder dokumentlokal definiert sein; Ist die Transfofunktion
   * nicht in der globalen oder dokumentlokalen Funktionsbibliothek enthalten, so
   * wird ein Fehlerstring zurückgeliefert und eine weitere Fehlermeldung in die
   * Log-Datei geschrieben.
   * 
   * Befindet sich das TextDocumentModel in einem über {@link #startSimulation()}
   * gesetzten Simulationslauf, so wird die Trafo mit den im Simulationslauf
   * gesetzten Formularwerten berechnet und zurück geliefert.
   * 
   * @param trafoName
   *          Der Name der Trafofunktion, der nicht null sein darf.
   * @return Der transformierte Wert falls die Trafo definiert ist oder ein
   *         Fehlerstring, falls die Trafo nicht definiert ist.
   */
  public String getTransformedValue(String trafoName)
  {
    if (simulationResult == null)
      return getTransformedValue(trafoName, formFieldValues);
    else
      return getTransformedValue(trafoName, simulationResult.getFormFieldValues());
  }

  /**
   * Diese Methode berechnet die Transformation des Wertes value mit der
   * Trafofunktion trafoName, die global oder dokumentlokal definiert sein muss;
   * Dabei wird für alle von der Trafofunktion erwarteten Parameter der Wert value
   * übergeben - eine Praxis, die für insertFormValue- und für insertValue-Befehle
   * üblich war und mit Einführung der UserFieldFormFields geändert wurde (siehe
   * {@link #getTransformedValue(String)}. Ist trafoName==null, so wird value
   * zurückgegeben. Ist die Transformationsionfunktion nicht in der globalen oder
   * dokumentlokalen Funktionsbibliothek enthalten, so wird ein Fehlerstring
   * zurückgeliefert und eine weitere Fehlermeldung in die Log-Datei geschrieben.
   * 
   * @param value
   *          Der zu transformierende Wert.
   * @param trafoName
   *          Der Name der Trafofunktion, der auch null sein darf.
   * @return Der transformierte Wert falls das trafoName gesetzt ist und die Trafo
   *         korrekt definiert ist. Ist trafoName==null, so wird value unverändert
   *         zurückgeliefert. Ist die Funktion trafoName nicht definiert, wird ein
   *         Fehlerstring zurückgeliefert.
   */
  public String getTransformedValue(String trafoName, String value)
  {
    String transformed = value;
    if (trafoName != null)
    {
      Function func = getFunctionLibrary().get(trafoName);
      if (func != null)
      {
        SimpleMap args = new SimpleMap();
        String[] pars = func.parameters();
        for (int i = 0; i < pars.length; i++)
          args.put(pars[i], value);
        transformed = func.getString(args);
      }
      else
      {
        transformed = L.m("<FEHLER: TRAFO '%1' nicht definiert>", trafoName);
        Logger.error(L.m("Die TRAFO '%1' ist nicht definiert.", trafoName));
      }
    }
    return transformed;
  }

  /**
   * Liefert den Frame zu diesem TextDocument oder null, wenn der Frame nicht
   * bestimmt werden kann.
   * 
   * @return
   */
  synchronized public XFrame getFrame()
  {
    try
    {
      return doc.getCurrentController().getFrame();
    }
    catch (java.lang.Exception e)
    {
      return null;
    }
  }

  /**
   * Liefert die Gesamtseitenzahl des Dokuments oder 0, wenn die Seitenzahl nicht
   * bestimmt werden kann.
   * 
   * @return Liefert die Gesamtseitenzahl des Dokuments oder 0, wenn die Seitenzahl
   *         nicht bestimmt werden kann.
   */
  synchronized public int getPageCount()
  {
    try
    {
      return (int) AnyConverter.toLong(UNO.getProperty(doc.getCurrentController(),
        "PageCount"));
    }
    catch (java.lang.Exception e)
    {
      return 0;
    }
  }

  /**
   * Setzt das Fensters des TextDokuments auf Sichtbar (visible==true) oder
   * unsichtbar (visible == false).
   * 
   * @param visible
   */
  synchronized public void setWindowVisible(boolean visible)
  {
    XFrame frame = getFrame();
    if (frame != null)
    {
      frame.getContainerWindow().setVisible(visible);
    }
  }

  /**
   * Liefert true, wenn das Dokument als "modifiziert" markiert ist und damit z.B.
   * die "Speichern?" Abfrage vor dem Schließen erscheint.
   * 
   * @param state
   */
  synchronized public boolean getDocumentModified()
  {
    try
    {
      return UNO.XModifiable(doc).isModified();
    }
    catch (java.lang.Exception x)
    {
      return false;
    }
  }

  /**
   * Diese Methode setzt den DocumentModified-Status auf state.
   * 
   * @param state
   */
  synchronized public void setDocumentModified(boolean state)
  {
    try
    {
      UNO.XModifiable(doc).setModified(state);
    }
    catch (java.lang.Exception x)
    {}
  }

  /**
   * Diese Methode blockt/unblocked die Contoller, die für das Rendering der
   * Darstellung in den Dokumenten zuständig sind, jedoch nur, wenn nicht der
   * debug-modus gesetzt ist.
   * 
   * @param state
   */
  synchronized public void setLockControllers(boolean lock)
  {
    try
    {
      if (WollMuxFiles.isDebugMode() == false
        && UNO.XModel(doc) != null)
      {
        if (lock)
          UNO.XModel(doc).lockControllers();
        else
          UNO.XModel(doc).unlockControllers();
      }
    }
    catch (java.lang.Exception e)
    {}
  }

  /**
   * Setzt die Position des Fensters auf die übergebenen Koordinaten, wobei die
   * Nachteile der UNO-Methode setWindowPosSize greifen, bei der die Fensterposition
   * nicht mit dem äusseren Fensterrahmen beginnt, sondern mit der grauen Ecke links
   * über dem File-Menü.
   * 
   * @param docX
   * @param docY
   * @param docWidth
   * @param docHeight
   */
  synchronized public void setWindowPosSize(final int docX, final int docY,
      final int docWidth, final int docHeight)
  {
    try
    {
      // Seit KDE4 muss ein maximiertes Fenster vor dem Verschieben "demaximiert" werden 
      // sonst wird die Positionierung ignoriert. Leider ist die dafür benötigte Klasse
      // erst seit OpenOffice.org 3.4 verfügbar - zur Abwärtskompatibilität erfolgt der
      // Aufruf daher über Reflection.
      try
      {
        Class<?> c = Class.forName("com.sun.star.awt.XTopWindow2");
        Object o = UnoRuntime.queryInterface(c, getFrame().getContainerWindow());
        Method getIsMaximized = c.getMethod("getIsMaximized", (Class[])null);
        Method setIsMaximized = c.getMethod("setIsMaximized", (boolean.class));
        if ((Boolean)getIsMaximized.invoke(o, (Object[])null))
        {
          setIsMaximized.invoke(o, false);
        }
      }
      catch (java.lang.Exception e)
      {}

      getFrame().getContainerWindow().setPosSize(docX, docY, docWidth, docHeight,
        PosSize.SIZE);
      getFrame().getContainerWindow().setPosSize(docX, docY, docWidth, docHeight,
        PosSize.POS);

    }
    catch (java.lang.Exception e)
    { 
      Logger.debug(e);
    }
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht des
   * Dokuments auf den neuen Wert zoom, der entwender eine ganzzahliger Prozentwert
   * (ohne "%"-Zeichen") oder einer der Werte "Optimal", "PageWidth",
   * "PageWidthExact" oder "EntirePage" ist.
   * 
   * @param zoom
   * @throws ConfigurationErrorException
   */
  private void setDocumentZoom(String zoom) throws ConfigurationErrorException
  {
    Short zoomType = null;
    Short zoomValue = null;

    if (zoom != null)
    {
      // ZOOM-Argument auswerten:
      if (zoom.equalsIgnoreCase("Optimal"))
        zoomType = Short.valueOf(DocumentZoomType.OPTIMAL);

      if (zoom.equalsIgnoreCase("PageWidth"))
        zoomType = Short.valueOf(DocumentZoomType.PAGE_WIDTH);

      if (zoom.equalsIgnoreCase("PageWidthExact"))
        zoomType = Short.valueOf(DocumentZoomType.PAGE_WIDTH_EXACT);

      if (zoom.equalsIgnoreCase("EntirePage"))
        zoomType = Short.valueOf(DocumentZoomType.ENTIRE_PAGE);

      if (zoomType == null)
      {
        try
        {
          zoomValue = Short.valueOf(zoom);
        }
        catch (NumberFormatException e)
        {}
      }
    }

    // ZoomType bzw ZoomValue setzen:
    Object viewSettings = null;
    try
    {
      viewSettings =
        UNO.XViewSettingsSupplier(doc.getCurrentController()).getViewSettings();
    }
    catch (java.lang.Exception e)
    {}
    if (zoomType != null)
      UNO.setProperty(viewSettings, "ZoomType", zoomType);
    else if (zoomValue != null)
      UNO.setProperty(viewSettings, "ZoomValue", zoomValue);
    else
      throw new ConfigurationErrorException(L.m("Ungültiger ZOOM-Wert '%1'", zoom));
  }

  /**
   * Diese Methode liest die (optionalen) Attribute X, Y, WIDTH, HEIGHT und ZOOM aus
   * dem übergebenen Konfigurations-Abschnitt settings und setzt die
   * Fenstereinstellungen des Dokuments entsprechend um. Bei den Pärchen X/Y bzw.
   * SIZE/WIDTH müssen jeweils beide Komponenten im Konfigurationsabschnitt angegeben
   * sein.
   * 
   * @param settings
   *          der Konfigurationsabschnitt, der X, Y, WIDHT, HEIGHT und ZOOM als
   *          direkte Kinder enthält.
   */
  synchronized public void setWindowViewSettings(ConfigThingy settings)
  {
    // Fenster holen (zum setzen der Fensterposition und des Zooms)
    XWindow window = null;
    try
    {
      window = getFrame().getContainerWindow();
    }
    catch (java.lang.Exception e)
    {}

    // Insets bestimmen (Rahmenmaße des Windows)
    int insetLeft = 0, insetTop = 0, insetRight = 0, insetButtom = 0;
    if (UNO.XDevice(window) != null)
    {
      DeviceInfo di = UNO.XDevice(window).getInfo();
      insetButtom = di.BottomInset;
      insetTop = di.TopInset;
      insetRight = di.RightInset;
      insetLeft = di.LeftInset;
    }

    // Position setzen:
    try
    {
      int xPos = Integer.parseInt(settings.get("X").toString());
      int yPos = Integer.parseInt(settings.get("Y").toString());
      if (window != null)
      {
        window.setPosSize(xPos + insetLeft, yPos + insetTop, 0, 0, PosSize.POS);
      }
    }
    catch (java.lang.Exception e)
    {}
    // Dimensions setzen:
    try
    {
      int width = Integer.parseInt(settings.get("WIDTH").toString());
      int height = Integer.parseInt(settings.get("HEIGHT").toString());
      if (window != null)
        window.setPosSize(0, 0, width - insetLeft - insetRight, height - insetTop
          - insetButtom, PosSize.SIZE);
    }
    catch (java.lang.Exception e)
    {}

    // Zoom setzen:
    setDocumentZoom(settings);
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht des
   * Dokuments auf den neuen Wert den das ConfigThingy conf im Knoten ZOOM angibt,
   * der entwender eine ganzzahliger Prozentwert (ohne "%"-Zeichen") oder einer der
   * Werte "Optimal", "PageWidth", "PageWidthExact" oder "EntirePage" ist.
   * 
   * @param zoom
   * @throws ConfigurationErrorException
   */
  synchronized public void setDocumentZoom(ConfigThingy conf)
  {
    try
    {
      setDocumentZoom(conf.get("ZOOM").toString());
    }
    catch (NodeNotFoundException e)
    {
      // ZOOM ist optional
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(e);
    }
  }

  /**
   * Die Methode fügt die Formular-Abschnitte aus der Formularbeschreibung der Notiz
   * von formCmd zur aktuellen Formularbeschreibung des Dokuments in den persistenten
   * Daten hinzu und löscht die Notiz (sowie den übrigen Inhalt von formCmd).
   * 
   * @param formCmd
   *          Das formCmd, das die Notiz mit den hinzuzufügenden Formular-Abschnitten
   *          einer Formularbeschreibung enthält.
   * @throws ConfigurationErrorException
   *           Die Notiz der Formularbeschreibung ist nicht vorhanden, die
   *           Formularbeschreibung ist nicht vollständig oder kann nicht geparst
   *           werden.
   */
  synchronized public void addToCurrentFormDescription(DocumentCommand.Form formCmd)
      throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextCursor();

    XTextContent annotationField =
      UNO.XTextContent(findAnnotationFieldRecursive(range));
    if (annotationField == null)
      throw new ConfigurationErrorException(
        L.m("Die zugehörige Notiz mit der Formularbeschreibung fehlt."));

    Object content = UNO.getProperty(annotationField, "Content");
    if (content == null)
      throw new ConfigurationErrorException(
        L.m("Die zugehörige Notiz mit der Formularbeschreibung kann nicht gelesen werden."));

    // Formularbeschreibung übernehmen und persistent speichern:
    addToFormDescription(getFormDescription(), content.toString());
    storeCurrentFormDescription();

    // Notiz (sowie anderen Inhalt des Bookmarks) löschen
    formCmd.setTextRangeString("");
  }

  /**
   * Versucht das Dokument zu schließen, wurde das Dokument jedoch verändert
   * (Modified-Status des Dokuments==true), so erscheint der Dialog
   * "Speichern"/"Verwerfen"/"Abbrechen" über den ein sofortiges Schließen des
   * Dokuments durch den Benutzer verhindert werden kann. Ist der closeListener
   * registriert (was WollMuxSingleton bereits bei der Erstellung des
   * TextDocumentModels standardmäßig macht), so wird nach dem close() auch
   * automatisch die dispose()-Methode aufgerufen.
   */
  synchronized public void close()
  {
    // Damit OOo vor dem Schließen eines veränderten Dokuments den
    // save/dismiss-Dialog anzeigt, muss die suspend()-Methode aller
    // XController gestartet werden, die das Model der Komponente enthalten.
    // Man bekommt alle XController über die Frames, die der Desktop liefert.
    boolean closeOk = true;
    if (UNO.XFramesSupplier(UNO.desktop) != null)
    {
      XFrame[] frames =
        UNO.XFramesSupplier(UNO.desktop).getFrames().queryFrames(FrameSearchFlag.ALL);
      for (int i = 0; i < frames.length; i++)
      {
        XController c = frames[i].getController();
        if (c != null && UnoRuntime.areSame(c.getModel(), doc))
        {
          // closeOk wird auf false gesetzt, wenn im save/dismiss-Dialog auf die
          // Schaltflächen "Speichern" und "Abbrechen" gedrückt wird. Bei
          // "Verwerfen" bleibt closeOK unverändert (also true).
          if (c.suspend(true) == false) closeOk = false;
        }
      }
    }

    // Wurde das Dokument erfolgreich gespeichert, so merkt dies der Test
    // getDocumentModified() == false. Wurde der save/dismiss-Dialog mit
    // "Verwerfen" beendet, so ist closeOK==true und es wird beendet. Wurde der
    // save/dismiss Dialog abgebrochen, so soll das Dokument nicht geschlossen
    // werden.
    if (closeOk || getDocumentModified() == false)
    {

      // Hier das eigentliche Schließen:
      try
      {
        if (UNO.XCloseable(doc) != null) UNO.XCloseable(doc).close(true);
      }
      catch (CloseVetoException e)
      {}

    }
    else if (UNO.XFramesSupplier(UNO.desktop) != null)
    {

      // Tritt in Kraft, wenn "Abbrechen" betätigt wurde. In diesem Fall werden
      // die Controllers mit suspend(FALSE) wieder reaktiviert.
      XFrame[] frames =
        UNO.XFramesSupplier(UNO.desktop).getFrames().queryFrames(FrameSearchFlag.ALL);
      for (int i = 0; i < frames.length; i++)
      {
        XController c = frames[i].getController();
        if (c != null && UnoRuntime.areSame(c.getModel(), doc)) c.suspend(false);
      }

    }
  }

  /**
   * Liefert den Titel des Dokuments, wie er im Fenster des Dokuments angezeigt wird,
   * ohne den Zusatz " - OpenOffice.org Writer" oder "NoTitle", wenn der Titel nicht
   * bestimmt werden kann. TextDocumentModel('<title>')
   */
  synchronized public String getTitle()
  {
    String title = "NoTitle";
    try
    {
      title = UNO.getProperty(getFrame(), "Title").toString();
      // "Untitled1 - OpenOffice.org Writer" -> cut " - OpenOffice.org Writer"
      int i = title.lastIndexOf(" - ");
      if (i >= 0) title = title.substring(0, i);
    }
    catch (java.lang.Exception e)
    {}
    return title;
  }

  /**
   * Liefert eine Stringrepräsentation des TextDocumentModels - Derzeit in der Form
   * 'doc(<title>)'.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  synchronized public String toString()
  {
    return "doc('" + getTitle() + "')";
  }

  /**
   * Fügt ein neues Dokumentkommando mit dem Kommandostring cmdStr, der in der Form
   * "WM(...)" erwartet wird, in das Dokument an der TextRange r ein. Dabei wird ein
   * neues Bookmark erstellt und dieses als Dokumenkommando registriert. Dieses
   * Bookmark wird genau über r gelegt, so dass abhängig vom Dokumentkommando der
   * Inhalt der TextRange r durch eine eventuelle spätere Ausführung des
   * Dokumentkommandos überschrieben wird (wenn r keine Ausdehnung hat, wird ein
   * kollabiertes Bookmark erzeugt und es kann logischerweise auch nichts
   * überschrieben werden). cmdStr muss nur das gewünschte Kommando enthalten ohne
   * eine abschließende Zahl, die zur Herstellung eindeutiger Bookmarks benötigt wird
   * - diese Zahl wird bei Bedarf automatisch an den Bookmarknamen angehängt.
   * 
   * @param r
   *          Die TextRange, an der das neue Bookmark mit diesem Dokumentkommando
   *          eingefügt werden soll. r darf auch null sein und wird in diesem Fall
   *          ignoriert.
   * @param cmdStr
   *          Das Kommando als String der Form "WM(...)".
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public synchronized void addNewDocumentCommand(XTextRange r, String cmdStr)
  {
    documentCommands.addNewDocumentCommand(r, cmdStr);
  }

  /**
   * Fügt an der Stelle r ein neues Textelement vom Typ css.text.TextField.InputUser
   * ein, und verknüpft das Feld so, dass die Trafo trafo verwendet wird, um den
   * angezeigten Feldwert zu berechnen.
   * 
   * @param r
   *          die Textrange, an der das Feld eingefügt werden soll
   * @param trafoName
   *          der Name der zu verwendenden Trafofunktion
   * @param hint
   *          Ein Hinweistext, der im Feld angezeigt werden soll, wenn man mit der
   *          Maus drüber fährt - kann auch null sein, dann wird der Hint nicht
   *          gesetzt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void addNewInputUserField(XTextRange r, String trafoName,
      String hint)
  {
    updateLastTouchedByVersionInfo();

    try
    {
      ConfigThingy conf = new ConfigThingy("WM");
      conf.add("FUNCTION").add(trafoName);
      String userFieldName = conf.stringRepresentation(false, '\'', false);

      // master erzeugen
      XPropertySet master = getUserFieldMaster(userFieldName);
      if (master == null)
      {
        master =
          UNO.XPropertySet(UNO.XMultiServiceFactory(doc).createInstance(
            "com.sun.star.text.FieldMaster.User"));
        UNO.setProperty(master, "Value", Integer.valueOf(0));
        UNO.setProperty(master, "Name", userFieldName);
      }

      // textField erzeugen
      XTextContent f =
        UNO.XTextContent(UNO.XMultiServiceFactory(doc).createInstance(
          "com.sun.star.text.TextField.InputUser"));
      UNO.setProperty(f, "Content", userFieldName);
      if (hint != null) UNO.setProperty(f, "Hint", hint);
      r.getText().insertTextContent(r, f, true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Diese Methode entfernt alle Reste, die von nicht mehr referenzierten
   * AUTOFUNCTIONS übrig bleiben: AUTOFUNCTIONS-Definitionen aus der
   * Funktionsbibliothek, der Formularbeschreibung in den persistenten Daten und
   * nicht mehr benötigte TextFieldMaster von ehemaligen InputUser-Textfeldern -
   * Durch die Aufräumaktion ändert sich der DocumentModified-Status des Dokuments
   * nicht.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  private void cleanupGarbageOfUnreferencedAutofunctions()
  {
    boolean modified = getDocumentModified();

    // Liste aller derzeit eingesetzten Trafos aufbauen:
    HashSet<String> usedFunctions = new HashSet<String>();
    for (Map.Entry<String, List<FormField>> ent : idToFormFields.entrySet())
    {

      List<FormField> l = ent.getValue();
      for (Iterator<FormField> iterator = l.iterator(); iterator.hasNext();)
      {
        FormField f = iterator.next();
        String trafoName = f.getTrafoName();
        if (trafoName != null) usedFunctions.add(trafoName);
      }
    }

    for (Map.Entry<String, List<FormField>> ent : idToTextFieldFormFields.entrySet())
    {

      List<FormField> l = ent.getValue();
      for (Iterator<FormField> iterator = l.iterator(); iterator.hasNext();)
      {
        FormField f = iterator.next();
        String trafoName = f.getTrafoName();
        if (trafoName != null) usedFunctions.add(trafoName);
      }
    }
    for (Iterator<FormField> iterator = staticTextFieldFormFields.iterator(); iterator.hasNext();)
    {
      FormField f = iterator.next();
      String trafoName = f.getTrafoName();
      if (trafoName != null) usedFunctions.add(trafoName);
    }

    // Nicht mehr benötigte Autofunctions aus der Funktionsbibliothek löschen:
    FunctionLibrary funcLib = getFunctionLibrary();
    for (Iterator<String> iter = funcLib.getFunctionNames().iterator(); iter.hasNext();)
    {
      String name = iter.next();
      if (name == null || !name.startsWith(AUTOFUNCTION_PREFIX)
        || usedFunctions.contains(name)) continue;
      funcLib.remove(name);
    }

    // Nicht mehr benötigte Autofunctions aus der Formularbeschreibung der
    // persistenten Daten löschen.
    ConfigThingy functions =
      getFormDescription().query("Formular").query("Funktionen");
    for (Iterator<ConfigThingy> iter = functions.iterator(); iter.hasNext();)
    {
      ConfigThingy funcs = iter.next();
      for (Iterator<ConfigThingy> iterator = funcs.iterator(); iterator.hasNext();)
      {
        String name = iterator.next().getName();
        if (name == null || !name.startsWith(AUTOFUNCTION_PREFIX)
          || usedFunctions.contains(name)) continue;
        iterator.remove();
      }
    }
    storeCurrentFormDescription();

    // Nicht mehr benötigte TextFieldMaster von ehemaligen InputUser-Textfeldern
    // löschen:
    XNameAccess masters = UNO.XTextFieldsSupplier(doc).getTextFieldMasters();
    String prefix = "com.sun.star.text.FieldMaster.User.";
    String[] masterNames = masters.getElementNames();
    for (int i = 0; i < masterNames.length; i++)
    {
      String masterName = masterNames[i];
      if (masterName == null || !masterName.startsWith(prefix)) continue;
      String varName = masterName.substring(prefix.length());
      String trafoName = getFunctionNameForUserFieldName(varName);
      if (trafoName != null && !usedFunctions.contains(trafoName))
      {
        try
        {
          XComponent m = UNO.XComponent(masters.getByName(masterName));
          m.dispose();
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    setDocumentModified(modified);
  }

  /**
   * Führt alle Funktionen aus funcs der Reihe nach aus, solange bis eine davon einen
   * nicht-leeren String zurückliefert und interpretiert diesen als Angabe, welche
   * Aktionen für das Dokument auszuführen sind. Derzeit werden nur "noaction" und
   * "allactions" unterstützt. Den Funktionen werden als {@link Values} diverse Daten
   * zur Verfügung gestellt. Derzeit sind dies
   * <ul>
   * <li>"User/<Name>" Werte von Benutzervariablen (vgl.
   * {@link #getUserFieldMaster(String)}</li>
   * </ul>
   * 
   * @return 0 => noaction, Integer.MAX_VALUE => allactions, -1 => WollMux-Default
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  public int evaluateDocumentActions(Iterator<Function> funcs)
  {
    Values values = new MyValues();
    while (funcs.hasNext())
    {
      Function f = funcs.next();
      String res = f.getString(values);
      if (res.length() > 0)
      {
        if (res.equals("noaction")) return 0;
        if (res.equals("allactions")) return Integer.MAX_VALUE;
        Logger.error(L.m(
          "Unbekannter Rückgabewert \"%1\" von Dokumentaktionen-Funktion", res));
      }
    }
    return -1;
  }

  /**
   * Stellt diverse Daten zur Verfügung in der Syntax "Namensraum/Name". Derzeit
   * unterstützte Namensräume sind
   * <ul>
   * <li>"User/" Werte von Benutzervariablen (vgl.
   * {@link #getUserFieldMaster(String)}</li>
   * </ul>
   * 
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private class MyValues implements Values
  {
    public static final int MYVALUES_NAMESPACE_UNKNOWN = 0;

    public static final int MYVALUES_NAMESPACE_USER = 1;

    @Override
    public String getString(String id)
    {
      switch (namespace(id))
      {
        case MYVALUES_NAMESPACE_USER:
          return getString_User(id);
        default:
          return "";
      }
    }

    @Override
    public boolean getBoolean(String id)
    {
      switch (namespace(id))
      {
        case MYVALUES_NAMESPACE_USER:
          return getBoolean_User(id);
        default:
          return false;
      }
    }

    @Override
    public boolean hasValue(String id)
    {
      switch (namespace(id))
      {
        case MYVALUES_NAMESPACE_USER:
          return hasValue_User(id);
        default:
          return false;
      }
    }

    private int namespace(String id)
    {
      if (id.startsWith("User/")) return MYVALUES_NAMESPACE_USER;
      return MYVALUES_NAMESPACE_UNKNOWN;
    }

    private String getString_User(String id)
    {
      try
      {
        id = id.substring(id.indexOf('/') + 1);
        return getUserFieldMaster(id).getPropertyValue("Content").toString();
      }
      catch (Exception x)
      {
        return "";
      }
    }

    private boolean getBoolean_User(String id)
    {
      return getString_User(id).equalsIgnoreCase("true");
    }

    private boolean hasValue_User(String id)
    {
      try
      {
        id = id.substring(id.indexOf('/') + 1);
        return getUserFieldMaster(id) != null;
      }
      catch (Exception x)
      {
        return false;
      }
    }
  }

  /**
   * Diese Methode liefert den TextFieldMaster, der für Zugriffe auf das Benutzerfeld
   * mit den Namen userFieldName zuständig ist.
   * 
   * @param userFieldName
   * @return den TextFieldMaster oder null, wenn das Benutzerfeld userFieldName nicht
   *         existiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private XPropertySet getUserFieldMaster(String userFieldName)
  {
    XNameAccess masters = UNO.XTextFieldsSupplier(doc).getTextFieldMasters();
    String elementName = "com.sun.star.text.FieldMaster.User." + userFieldName;
    if (masters.hasByName(elementName))
    {
      try
      {
        return UNO.XPropertySet(masters.getByName(elementName));
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
    }
    return null;
  }

  /**
   * Wenn das Benutzerfeld mit dem Namen userFieldName vom WollMux interpretiert wird
   * (weil der Name in der Form "WM(FUNCTION '<name>')" aufgebaut ist), dann liefert
   * diese Funktion den Namen <name> der Funktion zurück; in allen anderen Fällen
   * liefert die Methode null zurück.
   * 
   * @param userFieldName
   *          Name des Benutzerfeldes
   * @return den Namen der in diesem Benutzerfeld verwendeten Funktion oder null,
   *         wenn das Benutzerfeld nicht vom WollMux interpretiert wird.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static String getFunctionNameForUserFieldName(String userFieldName)
  {
    if (userFieldName == null) return null;

    Matcher m = TextDocumentModel.INPUT_USER_FUNCTION.matcher(userFieldName);

    if (!m.matches()) return null;
    String confStr = m.group(1);

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("INSERT", confStr);
    }
    catch (Exception x)
    {
      return null;
    }

    ConfigThingy trafoConf = conf.query("FUNCTION");
    if (trafoConf.count() != 1)
      return null;
    else
      return trafoConf.toString();
  }

  /**
   * Wenn das als Kommandostring cmdStr übergebene Dokumentkommando (derzeit nur
   * insertFormValue) eine Trafofunktion gesetzt hat, so wird der Name dieser
   * Funktion zurückgeliefert; Bildet cmdStr kein gültiges Dokumentkommando ab oder
   * verwendet dieses Dokumentkommando keine Funktion, so wird null zurück geliefert.
   * 
   * @param cmdStr
   *          Ein Kommandostring eines Dokumentkommandos in der Form "WM(CMD
   *          '<command>' ...)"
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static String getFunctionNameForDocumentCommand(String cmdStr)
  {
    ConfigThingy wm = new ConfigThingy("");
    try
    {
      wm = new ConfigThingy("", cmdStr).get("WM");
    }
    catch (java.lang.Exception e)
    {}

    String cmd = "";
    try
    {
      cmd = wm.get("CMD").toString();
    }
    catch (NodeNotFoundException e)
    {}

    if (cmd.equalsIgnoreCase("insertFormValue")) try
    {
      return wm.get("TRAFO").toString();
    }
    catch (NodeNotFoundException e)
    {}
    return null;
  }

  /**
   * Ersetzt die aktuelle Selektion (falls vorhanden) durch ein WollMux-Formularfeld
   * mit ID id, dem Hinweistext hint und der durch trafoConf definierten TRAFO. Das
   * Formularfeld ist direkt einsetzbar, d.h. sobald diese Methode zurückkehrt, kann
   * über setFormFieldValue(id,...) das Feld befüllt werden. Ist keine Selektion
   * vorhanden, so tut die Funktion nichts.
   * 
   * @param trafoConf
   *          darf null sein, dann wird keine TRAFO gesetzt. Ansonsten ein
   *          ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION )", wobei
   *          Bezeichner ein beliebiger Bezeichner ist und FUNKTIONSDEFINITION ein
   *          erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   *          , d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine erlaubter
   *          Funktionsname, z.B. "AND" sein. Der Bezeichner wird NICHT als Name der
   *          TRAFO verwendet. Stattdessen wird ein neuer eindeutiger TRAFO-Name
   *          generiert.
   * @param hint
   *          Ein Hinweistext der als Tooltip des neuen Formularfeldes angezeigt
   *          werden soll. hint kann null sein, dann wird kein Hinweistext angezeigt.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  synchronized public void replaceSelectionWithTrafoField(ConfigThingy trafoConf,
      String hint)
  {
    String trafoName = addLocalAutofunction(trafoConf);

    if (trafoName != null) try
    {
      // Neues UserField an der Cursorposition einfügen
      addNewInputUserField(getViewCursor(), trafoName, hint);

      // Datenstrukturen aktualisieren
      collectNonWollMuxFormFields();

      // Formularwerte-Abschnitt für alle referenzierten fieldIDs vorbelegen
      // wenn noch kein Wert gesetzt ist und Anzeige aktualisieren.
      Function f = getFunctionLibrary().get(trafoName);
      String[] fieldIds = new String[] {};
      if (f != null) fieldIds = f.parameters();
      for (int i = 0; i < fieldIds.length; i++)
      {
        String fieldId = fieldIds[i];
        // Feldwert mit leerem Inhalt vorbelegen, wenn noch kein Wert gesetzt
        // ist.
        if (!formFieldValues.containsKey(fieldId)) setFormFieldValue(fieldId, "");
        updateFormFields(fieldId);
      }

      // Nicht referenzierte Autofunktionen/InputUser-TextFieldMaster löschen
      cleanupGarbageOfUnreferencedAutofunctions();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Falls die aktuelle Selektion genau ein Formularfeld enthält (die Selektion muss
   * nicht bündig mit den Grenzen dieses Feldes sein, aber es darf kein zweites
   * Formularfeld in der Selektion enthalten sein) und dieses eine TRAFO gesetzt hat,
   * so wird die Definition dieser TRAFO als ConfigThingy zurückgeliefert, ansonsten
   * null. Wird eine TRAFO gefunden, die in einem globalen Konfigurationsabschnitt
   * definiert ist (also nicht dokumentlokal) und damit auch nicht verändert werden
   * kann, so wird ebenfalls null zurück geliefert.
   * 
   * @return null oder die Definition der TRAFO in der Form
   *         "TrafoName(FUNKTIONSDEFINITION)", wobei TrafoName die Bezeichnung ist,
   *         unter der die TRAFO mittels {@link #setTrafo(String, ConfigThingy)}
   *         modifiziert werden kann.
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  synchronized public ConfigThingy getFormFieldTrafoFromSelection()
  {
    XTextCursor vc = getViewCursor();
    if (vc == null) return null;

    HashMap<String, Integer> collectedTrafos = collectTrafosFromEnumeration(vc);

    // Auswertung von collectedTrafos
    HashSet<String> completeFields = new HashSet<String>();
    HashSet<String> startedFields = new HashSet<String>();
    HashSet<String> finishedFields = new HashSet<String>();

    for (Map.Entry<String, Integer> ent : collectedTrafos.entrySet())
    {
      String trafo = ent.getKey();
      int complete = ent.getValue().intValue();
      if (complete == 1) startedFields.add(trafo);
      if (complete == 2) finishedFields.add(trafo);
      if (complete == 3) completeFields.add(trafo);
    }

    // Das Feld ist eindeutig bestimmbar, wenn genau ein vollständiges Feld oder
    // als Fallback genau eine Startmarke gefunden wurde.
    String trafoName = null;
    if (completeFields.size() > 1)
      return null; // nicht eindeutige Felder
    else if (completeFields.size() == 1)
      trafoName = completeFields.iterator().next();
    else if (startedFields.size() > 1)
      return null; // nicht eindeutige Felder
    else if (startedFields.size() == 1) trafoName = startedFields.iterator().next();

    // zugehöriges ConfigThingy aus der Formularbeschreibung zurückliefern.
    if (trafoName != null)
      try
      {
        return getFormDescription().query("Formular").query("Funktionen").query(
          trafoName, 2).getLastChild();
      }
      catch (NodeNotFoundException e)
      {}

    return null;
  }

  /**
   * Gibt die Namen aller in der XTextRange gefunden Trafos als Schlüssel einer
   * HashMap zurück. Die zusätzlichen Integer-Werte in der HashMap geben an, ob (1)
   * nur die Startmarke, (2) nur die Endemarke oder (3) ein vollständiges
   * Bookmark/Feld gefunden wurde (Bei atomaren Feldern wird gleich 3 als Wert
   * gesetzt).
   * 
   * @param textRange
   *          die XTextRange an der gesucht werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static HashMap<String, Integer> collectTrafosFromEnumeration(
      XTextRange textRange)
  {
    HashMap<String, Integer> collectedTrafos = new HashMap<String, Integer>();

    if (textRange == null) return collectedTrafos;
    XEnumerationAccess parEnumAcc =
      UNO.XEnumerationAccess(textRange.getText().createTextCursorByRange(textRange));
    if (parEnumAcc == null) return collectedTrafos;

    XEnumeration parEnum = parEnumAcc.createEnumeration();
    while (parEnum.hasMoreElements())
    {
      XEnumerationAccess porEnumAcc = null;
      try
      {
        porEnumAcc = UNO.XEnumerationAccess(parEnum.nextElement());
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
      if (porEnumAcc == null) continue;

      XEnumeration porEnum = porEnumAcc.createEnumeration();
      while (porEnum.hasMoreElements())
      {
        Object portion = null;
        try
        {
          portion = porEnum.nextElement();
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }

        // InputUser-Textfelder verarbeiten
        XTextField tf = UNO.XTextField(UNO.getProperty(portion, "TextField"));
        if (tf != null
          && UNO.supportsService(tf, "com.sun.star.text.TextField.InputUser"))
        {
          String varName = "" + UNO.getProperty(tf, "Content");
          String t = getFunctionNameForUserFieldName(varName);
          if (t != null) collectedTrafos.put(t, Integer.valueOf(3));
        }

        // Dokumentkommandos (derzeit insertFormValue) verarbeiten
        XNamed bm = UNO.XNamed(UNO.getProperty(portion, "Bookmark"));
        if (bm != null)
        {
          String name = "" + bm.getName();

          boolean isStart = false;
          boolean isEnd = false;
          try
          {
            boolean isCollapsed =
              AnyConverter.toBoolean(UNO.getProperty(portion, "IsCollapsed"));
            isStart =
              AnyConverter.toBoolean(UNO.getProperty(portion, "IsStart"))
                || isCollapsed;
            isEnd = !isStart || isCollapsed;
          }
          catch (IllegalArgumentException e)
          {}

          String docCmd = getDocumentCommandByBookmarkName(name);
          if (docCmd != null)
          {
            String t = getFunctionNameForDocumentCommand(docCmd);
            if (t != null)
            {
              Integer s = collectedTrafos.get(t);
              if (s == null) s = Integer.valueOf(0);
              if (isStart) s = Integer.valueOf(s.intValue() | 1);
              if (isEnd) s = Integer.valueOf(s.intValue() | 2);
              collectedTrafos.put(t, s);
            }
          }
        }
      }
    }
    return collectedTrafos;
  }

  /**
   * Prüft ob es sich bei dem mit bookmarkName bezeichneten Bookmark um ein
   * Dokumentkommando des WollMux handelt und liefert in diesem Fall das
   * Dokumentkommando (also den Bookmark-Namen bereinigt um die abschließende Ziffer)
   * zurück, oder null, wenn es sich um kein Dokumentkommando des WollMux handelt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static String getDocumentCommandByBookmarkName(String bookmarkName)
  {
    Matcher m = WOLLMUX_BOOKMARK_PATTERN.matcher(bookmarkName);
    if (m.matches()) return m.group(1);
    return null;
  }

  /**
   * Ändert die Definition der TRAFO mit Name trafoName auf trafoConf. Die neue
   * Definition wirkt sich sofort auf folgende
   * {@link #setFormFieldValue(String, String)} Aufrufe aus.
   * 
   * @param trafoConf
   *          ein ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION )",
   *          wobei Bezeichner ein beliebiger Bezeichner ist und FUNKTIONSDEFINITION
   *          ein erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
   *          , d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine erlaubter
   *          Funktionsname, z.B. "AND" sein. Der Bezeichner wird NICHT verwendet.
   *          Der Name der TRAFO wird ausschließlich durch trafoName festgelegt.
   * @throws UnavailableException
   *           wird geworfen, wenn die Trafo trafoName nicht schreibend verändert
   *           werden kann, weil sie z.B. nicht existiert oder in einer globalen
   *           Funktionsbeschreibung definiert ist.
   * @throws ConfigurationErrorException
   *           beim Parsen der Funktion trafoConf trat ein Fehler auf.
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  synchronized public void setTrafo(String trafoName, ConfigThingy trafoConf)
      throws UnavailableException, ConfigurationErrorException
  {
    // Funktionsknoten aus Formularbeschreibung zum Anpassen holen
    ConfigThingy func;
    try
    {
      func =
        getFormDescription().query("Formular").query("Funktionen").query(trafoName,
          2).getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      throw new UnavailableException(e);
    }

    // Funktion parsen und in Funktionsbibliothek setzen:
    FunctionLibrary funcLib = getFunctionLibrary();
    Function function =
      FunctionFactory.parseChildren(trafoConf, funcLib, getDialogLibrary(),
        getFunctionContext());
    funcLib.add(trafoName, function);

    // Kinder von func löschen, damit sie später neu gesetzt werden können
    for (Iterator<ConfigThingy> iter = func.iterator(); iter.hasNext();)
    {
      iter.next();
      iter.remove();
    }

    // Kinder von trafoConf auf func übertragen
    for (Iterator<ConfigThingy> iter = trafoConf.iterator(); iter.hasNext();)
    {
      ConfigThingy f = iter.next();
      func.addChild(new ConfigThingy(f));
    }

    // neue Formularbeschreibung sichern
    storeCurrentFormDescription();

    // Die neue Funktion kann von anderen IDs abhängen als die bisherige
    // Funktion. Hier muss dafür gesorgt werden, dass in idToTextFieldFormFields
    // veraltete ID-Zuordnungen gelöscht und neue ID-Zuordungen eingetragen
    // werden. Am einfachsten macht dies vermutlich ein
    // collectNonWollMuxFormFields(). InsertFormValue-Dokumentkommandos haben
    // eine feste ID-Zuordnung und kommen aus dieser auch nicht aus. D.h.
    // InsertFormValue-Bookmarks müssen nicht aktualisiert werden.
    collectNonWollMuxFormFields();

    // Felder updaten:
    updateAllFormFields();
  }

  /**
   * Diese Methode liefert ein Array von FieldInfo-Objekten, das Informationen über
   * FeldIDs enthält, die im gesamten Dokument in insertFormValue-Kommandos,
   * Serienbrieffelder, Benutzerfelder und evtl. gesetzten Trafos referenziert werden
   * und nicht im Schema schema aufgeführt sind. Ist eine Selektion vorhanden, so ist
   * die Liste in der Reihenfolge aufgebaut, in der die IDs im Dokument angesprochen
   * werden. Ist keine Selektion vorhanden, so werden die Felder in alphabetisch
   * sortierter Reihenfolge zurückgeliefert.
   * 
   * @return Eine Liste aller Referenzierten FeldIDs, des Dokuments oder der
   *         aktuellen Selektion, die nicht in schema enthalten sind.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public ReferencedFieldID[] getReferencedFieldIDsThatAreNotInSchema(
      Set<String> schema)
  {
    ArrayList<ReferencedFieldID> list = new ArrayList<ReferencedFieldID>();

    // Alle ReferencedFieldIDs des Dokuments alphabetisch sortiert
    // zurückliefern.
    List<String> sortedIDs = new ArrayList<String>(getAllFieldIDs());
    Collections.sort(sortedIDs);
    for (Iterator<String> iter = sortedIDs.iterator(); iter.hasNext();)
    {
      String id = iter.next();
      if (schema.contains(id)) continue;
      List<FormField> fields = new ArrayList<FormField>();
      if (idToFormFields.containsKey(id)) fields.addAll(idToFormFields.get(id));
      if (idToTextFieldFormFields.containsKey(id))
        fields.addAll(idToTextFieldFormFields.get(id));
      boolean hasTrafo = false;
      for (Iterator<FormField> fieldIter = fields.iterator(); fieldIter.hasNext();)
      {
        FormField field = fieldIter.next();
        if (field.getTrafoName() != null) hasTrafo = true;
      }
      list.add(new ReferencedFieldID(id, hasTrafo));
    }

    // Array FieldInfo erstellen
    ReferencedFieldID[] fieldInfos = new ReferencedFieldID[list.size()];
    int i = 0;
    for (Iterator<ReferencedFieldID> iter = list.iterator(); iter.hasNext();)
    {
      ReferencedFieldID fieldInfo = iter.next();
      fieldInfos[i++] = fieldInfo;
    }
    return fieldInfos;
  }

  /**
   * Enthält Informationen über die in der Selektion oder im gesamten Dokument in
   * Feldern referenzierten FeldIDs.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class ReferencedFieldID
  {
    private final String fieldId;

    private final boolean isTransformed;

    public ReferencedFieldID(String fieldId, boolean isTransformed)
    {
      this.fieldId = fieldId;
      this.isTransformed = isTransformed;
    }

    /**
     * Liefert die FieldID als String.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public String getFieldId()
    {
      return fieldId;
    }

    /**
     * Liefert true, wenn auf dem Feld eine TRAFO gesetzt ist.
     * 
     * @return
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public boolean isTransformed()
    {
      return isTransformed;
    }
  }

  /**
   * Diese Methode ersetzt die Referenzen der ID fieldId im gesamten Dokument durch
   * neue IDs, die in der Ersetzungsregel subst spezifiziert sind. Die
   * Ersetzungsregel ist vom Typ FieldSubstitution und kann mehrere Elemente (fester
   * Text oder Felder) enthalten, die an Stelle eines alten Feldes gesetzt werden
   * sollen. Damit kann eine Ersetzungsregel auch dafür sorgen, dass aus einem früher
   * atomaren Feld in Zukunft mehrere Felder entstehen. Folgender Abschnitt
   * beschreibt, wie sich die Ersetzung auf verschiedene Elemente auswirkt.
   * 
   * 1) Ersetzungsregel "&lt;neueID&gt;" - Einfache Ersetzung mit genau einem neuen
   * Serienbrieffeld (z.B. "&lt;Vorname&gt;"): bei insertFormValue-Kommandos wird
   * WM(CMD'insertFormValue' ID '&lt;alteID&gt;' [TRAFO...]) ersetzt durch WM(CMD
   * 'insertFormValue' ID '&lt;neueID&gt;' [TRAFO...]). Bei Serienbrieffeldern wird
   * die ID ebenfalls direkt ersetzt durch &lt;neueID&gt;. Bei
   * WollMux-Benutzerfeldern, die ja immer eine Trafo hinterlegt haben, wird jede
   * vorkommende Funktion VALUE 'alteID' ersetzt durch VALUE 'neueID'.
   * 
   * 2) Ersetzungsregel "&lt;A&gt; &lt;B&gt;" - Komplexe Ersetzung mit mehreren neuen
   * IDs und Text: Diese Ersetzung ist bei transformierten Feldern grundsätzlich
   * nicht zugelassen. Ein bestehendes insertFormValue-Kommando ohne Trafo wird wie
   * folgt manipuliert: anstelle des alten Bookmarks WM(CMD'insertFormValue' ID
   * 'alteId') wird der entsprechende Freitext und entsprechende neue
   * WM(CMD'insertFormValue' ID 'neueIDn') Bookmarks in den Text eingefügt. Ein
   * Serienbrieffeld wird ersetzt durch entsprechende neue Serienbrieffelder, die
   * durch den entsprechenden Freitext getrennt sind.
   * 
   * 3) Leere Ersetzungsregel - in diesem Fall wird keine Ersetzung vorgenommen und
   * die Methode kehrt sofort zurück.
   * 
   * In allen Fällen gilt, dass die Änderung nach Ausführung dieser Methode sofort
   * aktiv sind und der Aufruf von setFormFieldValue(...) bzw. updateFormFields(...)
   * mit den neuen IDs direkt in den veränderten Feldern Wirkung zeigt. Ebenso werden
   * aus dem Formularwerte-Abschnitt in den persistenten Daten die alten Werte der
   * ersetzten IDs gelöscht.
   * 
   * @param fieldId
   *          Feld, das mit Hilfe der Ersetzungsregel subst ersetzt werden soll.
   * @param subst
   *          die Ersetzungsregel, die beschreibt, welche Inhalte an Stelle des alten
   *          Feldes eingesetzt werden sollen.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  synchronized public void applyFieldSubstitution(String fieldId,
      FieldSubstitution subst)
  {
    // keine Ersetzung, wenn subst leer ist.
    if (!subst.iterator().hasNext()) return;

    // enthält später die neue FieldId, wenn eine 1-zu-1-Zuordnung vorliegt
    String newFieldId = null;

    // Neuen Text zusammenbauen, Felder sind darin mit <feldname> gekennzeichnet
    String substStr = "";
    int count = 0;
    for (Iterator<FieldSubstitution.SubstElement> substIter = subst.iterator(); substIter.hasNext();)
    {
      FieldSubstitution.SubstElement ele = substIter.next();
      if (ele.isFixedText())
      {
        substStr += ele.getValue();
      }
      else if (ele.isField())
      {
        substStr += "<" + ele.getValue() + ">";
        newFieldId = ele.getValue();
      }
      count++;
    }
    if (count != 1) newFieldId = null;

    // Alle InsertFormValue-Felder anpassen:
    List<FormField> c = idToFormFields.get(fieldId);
    if (c != null)
    {
      for (Iterator<FormField> iter = c.iterator(); iter.hasNext();)
      {
        FormField f = iter.next();
        if (f.getTrafoName() != null)
        {
          // Transformierte Felder soweit möglich behandeln
          if (newFieldId != null)
            // 1-zu-1 Zuordnung: Hier kann substitueFieldID verwendet werden
            f.substituteFieldID(fieldId, newFieldId);
          else
            Logger.error(L.m("Kann transformiertes Feld nur durch eine 1-zu-1 Zuordnung ersetzen."));
        }
        else
        {
          // Untransformierte Felder durch neue Felder ersetzen
          XTextRange anchor = f.getAnchor();
          if (f.getAnchor() != null)
          {
            // Cursor erzeugen, Formularfeld löschen und neuen String setzen
            XTextCursor cursor = anchor.getText().createTextCursorByRange(anchor);
            f.dispose();
            cursor.setString(substStr);

            // Neue Bookmarks passend zum Text platzieren
            cursor.collapseToStart();
            for (Iterator<FieldSubstitution.SubstElement> substIter =
              subst.iterator(); substIter.hasNext();)
            {
              FieldSubstitution.SubstElement ele = substIter.next();
              if (ele.isFixedText())
              {
                cursor.goRight((short) ele.getValue().length(), false);
              }
              else if (ele.isField())
              {
                cursor.goRight((short) (1 + ele.getValue().length() + 1), true);
                new Bookmark(
                  "WM(CMD 'insertFormValue' ID '" + ele.getValue() + "')", doc,
                  cursor);
                cursor.collapseToEnd();
              }
            }
          }
        }
      }
    }

    // Alle Datenbank- und Benutzerfelder anpassen:
    c = idToTextFieldFormFields.get(fieldId);
    if (c != null)
    {
      for (Iterator<FormField> iter = c.iterator(); iter.hasNext();)
      {
        FormField f = iter.next();
        if (f.getTrafoName() != null)
        {
          // Transformierte Felder soweit möglich behandeln
          if (newFieldId != null)
            // 1-zu-1 Zuordnung: hier kann f.substitueFieldId nicht verwendet
            // werden, dafür kann aber die Trafo angepasst werden.
            substituteFieldIdInTrafo(f.getTrafoName(), fieldId, newFieldId);
          else
            Logger.error(L.m("Kann transformiertes Feld nur durch eine 1-zu-1 Zuordnung ersetzen."));
        }
        else
        {
          // Untransformierte Felder durch neue Felder ersetzen
          XTextRange anchor = f.getAnchor();
          if (f.getAnchor() != null)
          {
            // Cursor über den Anker erzeugen und Formularfeld löschen
            XTextCursor cursor = anchor.getText().createTextCursorByRange(anchor);
            f.dispose();
            cursor.setString(substStr);

            // Neue Datenbankfelder passend zum Text einfügen
            cursor.collapseToStart();
            for (Iterator<FieldSubstitution.SubstElement> substIter =
              subst.iterator(); substIter.hasNext();)
            {
              FieldSubstitution.SubstElement ele = substIter.next();
              if (ele.isFixedText())
              {
                cursor.goRight((short) ele.getValue().length(), false);
              }
              else if (ele.isField())
              {
                cursor.goRight((short) (1 + ele.getValue().length() + 1), true);
                insertMailMergeField(ele.getValue(), cursor);
                cursor.collapseToEnd();
              }
            }
          }
        }
      }
    }

    // Datenstrukturen aktualisieren
    getDocumentCommands().update();
    DocumentCommandInterpreter dci = new DocumentCommandInterpreter(this);
    dci.scanGlobalDocumentCommands();
    // collectNonWollMuxFormFields() wird im folgenden scan auch noch erledigt
    dci.scanInsertFormValueCommands();

    // Alte Formularwerte aus den persistenten Daten entfernen
    setFormFieldValue(fieldId, null);

    // Ansicht der betroffenen Felder aktualisieren
    for (Iterator<FieldSubstitution.SubstElement> iter = subst.iterator(); iter.hasNext();)
    {
      FieldSubstitution.SubstElement ele = iter.next();
      if (ele.isField()) updateFormFields(ele.getValue());
    }
  }

  /**
   * Diese Methode ersetzt jedes Vorkommen von VALUE "oldFieldId" in der
   * dokumentlokalen Trafo-Funktion trafoName durch VALUE "newFieldId", speichert die
   * neue Formularbeschreibung persistent im Dokument ab und passt die aktuelle
   * Funktionsbibliothek entsprechend an. Ist einer der Werte trafoName, oldFieldId
   * oder newFieldId null, dann macht diese Methode nichts.
   * 
   * @param trafoName
   *          Die Funktion, in der die Ersetzung vorgenommen werden soll.
   * @param oldFieldId
   *          Die alte Feld-ID, die durch newFieldId ersetzt werden soll.
   * @param newFieldId
   *          die neue Feld-ID, die oldFieldId ersetzt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  private void substituteFieldIdInTrafo(String trafoName, String oldFieldId,
      String newFieldId)
  {
    if (trafoName == null || oldFieldId == null || newFieldId == null) return;
    try
    {
      ConfigThingy trafoConf =
        getFormDescription().query("Formular").query("Funktionen").query(trafoName,
          2).getLastChild();
      substituteValueRecursive(trafoConf, oldFieldId, newFieldId);

      // neue Formularbeschreibung persistent machen
      storeCurrentFormDescription();

      // Funktion neu parsen und Funktionsbibliothek anpassen
      FunctionLibrary funcLib = getFunctionLibrary();
      try
      {
        Function func =
          FunctionFactory.parseChildren(trafoConf, funcLib, dialogLib,
            getFunctionContext());
        getFunctionLibrary().add(trafoName, func);
      }
      catch (ConfigurationErrorException e)
      {
        // sollte eigentlich nicht auftreten, da die alte Trafo ja auch schon
        // einmal erfolgreich geparsed werden konnte.
        Logger.error(e);
      }
    }
    catch (NodeNotFoundException e)
    {
      Logger.error(L.m(
        "Die trafo '%1' ist nicht in diesem Dokument definiert und kann daher nicht verändert werden.",
        trafoName));
    }
  }

  /**
   * Durchsucht das ConfigThingy conf rekursiv und ersetzt alle VALUE-Knoten, die
   * genau ein Kind besitzen durch VALUE-Knoten mit dem neuen Kind newId.
   * 
   * @param conf
   *          Das ConfigThingy, in dem rekursiv ersetzt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void substituteValueRecursive(ConfigThingy conf, String oldFieldId,
      String newFieldId)
  {
    if (conf == null) return;

    if (conf.getName().equals("VALUE") && conf.count() == 1
      && conf.toString().equals(oldFieldId))
    {
      try
      {
        conf.getLastChild().setName(newFieldId);
      }
      catch (NodeNotFoundException e)
      {
        // kann wg. der obigen Prüfung nicht auftreten.
      }
      return;
    }

    for (Iterator<ConfigThingy> iter = conf.iterator(); iter.hasNext();)
    {
      ConfigThingy child = iter.next();
      substituteValueRecursive(child, oldFieldId, newFieldId);
    }
  }

  /**
   * Liefert die persönliche OverrideFrag-Liste des aktuell gewählten Absenders.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private ConfigThingy getInitialOverrideFragMap()
  {
    ConfigThingy overrideFragConf = new ConfigThingy("overrideFrag");
    if (overrideFragDbSpalte == null)
    {
      ConfigThingy overrideFragDbSpalteConf =
        WollMuxFiles.getWollmuxConf().query(OVERRIDE_FRAG_DB_SPALTE, 1);
      try
      {
        overrideFragDbSpalte = overrideFragDbSpalteConf.getLastChild().toString();
      }
      catch (NodeNotFoundException x)
      {
        // keine OVERRIDE_FRAG_DB_SPALTE Direktive gefunden
        overrideFragDbSpalte = "";
      }
    }

    if (overrideFragDbSpalte.length() > 0)
    {
      try
      {
        Dataset ds = DatasourceJoiner.getDatasourceJoiner().getSelectedDatasetTransformed();
        String value = ds.get(overrideFragDbSpalte);
        if (value == null) value = "";
        overrideFragConf = new ConfigThingy("overrideFrag", value);
      }
      catch (DatasetNotFoundException e)
      {
        Logger.log(L.m("Kein Absender ausgewählt => %1 bleibt wirkungslos",
          OVERRIDE_FRAG_DB_SPALTE));
      }
      catch (ColumnNotFoundException e)
      {
        Logger.error(L.m("%2 spezifiziert Spalte '%1', die nicht vorhanden ist",
          overrideFragDbSpalte, OVERRIDE_FRAG_DB_SPALTE), e);
      }
      catch (IOException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          OVERRIDE_FRAG_DB_SPALTE), x);
      }
      catch (SyntaxErrorException x)
      {
        Logger.error(L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte,
          OVERRIDE_FRAG_DB_SPALTE), x);
      }
    }

    return overrideFragConf;
  }
  
  /**
   * Diese Klasse beschreibt die Ersetzung eines bestehendes Formularfeldes durch
   * neue Felder oder konstante Textinhalte. Sie liefert einen Iterator, über den die
   * einzelnen Elemente (Felder bzw. fester Text) vom Typ SubstElement iteriert
   * werden können.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class FieldSubstitution implements
      Iterable<FieldSubstitution.SubstElement>
  {
    private List<SubstElement> list = new ArrayList<SubstElement>();

    public void addField(String fieldname)
    {
      list.add(new SubstElement(SubstElement.FIELD, fieldname));
    }

    public void addFixedText(String text)
    {
      list.add(new SubstElement(SubstElement.FIXED_TEXT, text));
    }

    @Override
    public Iterator<SubstElement> iterator()
    {
      return list.iterator();
    }

    @Override
    public String toString()
    {
      StringBuilder buffy = new StringBuilder();
      for (SubstElement ele : this)
        buffy.append(ele.isField() ? "<" + ele.getValue() + ">" : ele.getValue());
      return buffy.toString();
    }

    public static class SubstElement
    {
      private static final int FIXED_TEXT = 0;

      private static final int FIELD = 1;

      private int type;

      private String value;

      public SubstElement(int type, String value)
      {
        this.value = value;
        this.type = type;
      }

      public String getValue()
      {
        return value;
      }

      /**
       * Liefert true gdw das SubstElement ein Feld darstellt. In diesem Fall liefert
       * {@link #getValue()} die ID des Feldes.
       */
      public boolean isField()
      {
        return type == FIELD;
      }

      /**
       * Liefert true gdw das SubstElement einen einfachen Text darstellt. In diesem
       * Fall liefert {@link #getValue()} diesen Text.
       */
      public boolean isFixedText()
      {
        return type == FIXED_TEXT;
      }

      @Override
      public String toString()
      {
        return (isField() ? "FIELD" : "FIXED_TEXT") + " \"" + value + "\"";
      }
    }
  }

  /**
   * Startet den Simulationsmodus, in dem Änderungen an Formularelementen (WollMux-
   * und NON-WollMux-Felder) nur simuliert und nicht tatsächlich durchgeführt werden.
   * Benötigt wird dieser Modus für den Seriendruck über den OOo-Seriendruck, bei dem
   * die Änderungen nicht auf dem gerade offenen TextDocument durchgeführt werden,
   * sondern auf einer durch den OOo-Seriendruckmechanismus verwalteten Kopie des
   * Dokuments.
   */
  synchronized public void startSimulation()
  {
    simulationResult = new SimulationResults();
    simulationResult.setFormFieldValues(formFieldValues);
    simulationResult.setGroupsVisibilityState(mapGroupIdToVisibilityState);

    // Aktuell gesetzte FormField-Inhalte auslesen und simulationResults bekannt
    // machen.
    HashSet<FormField> ffs = new HashSet<FormField>();
    for (List<FormField> l : idToFormFields.values())
      for (FormField ff : l)
        ffs.add(ff);
    for (List<FormField> l : idToTextFieldFormFields.values())
      for (FormField ff : l)
        ffs.add(ff);
    ffs.addAll(staticTextFieldFormFields);
    for (FormField ff : ffs)
      simulationResult.setFormFieldContent(ff, ff.getValue());
  }

  /**
   * Beendet den mit {@link #startSimulation()} gestarteten Simulationsmodus und
   * liefert das Simulationsergebnis in SimulationResults zurück oder null, wenn der
   * Simulationsmodus vorher nicht gestartet wurde.
   */
  synchronized public SimulationResults stopSimulation()
  {
    SimulationResults r = simulationResult;
    simulationResult = null;
    return r;
  }

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zurück, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          Das erste gefundene AnnotationField oder null, wenn keines gefunden
   *          wurde.
   */
  private XTextField findAnnotationFieldRecursive(Object element)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    if (UNO.XEnumerationAccess(element) != null)
    {
      XEnumeration xEnum = UNO.XEnumerationAccess(element).createEnumeration();
  
      while (xEnum.hasMoreElements())
      {
        try
        {
          Object child = xEnum.nextElement();
          XTextField found = findAnnotationFieldRecursive(child);
          // das erste gefundene Element zurückliefern.
          if (found != null) return found;
        }
        catch (Exception e)
        {
          Logger.error(e);
        }
      }
    }
  
    Object textField = UNO.getProperty(element, "TextField");
    if (textField != null
      && UNO.supportsService(textField, "com.sun.star.text.TextField.Annotation"))
    {
      return UNO.XTextField(textField);
    }
  
    return null;
  }

  /**
   * Nimmt eine Liste von ConfigThingys, von denen jedes ein "Formular"-Knoten mit
   * enthaltener Formularbeschreibung ist, und liefert ein neues ConfigThingy zurück,
   * das eine gemergte Formularbeschreibung enthält. Beim Merge wird von Reitern mit
   * gleicher ID nur der letzte übernommen. Für die Reihenfolge wird die Reihenfolge
   * des ersten Auftretens herangezogen. Der TITLE wird zu newTitle. Die Funktionen-
   * und Funktionsdialoge-Abschnitte werden verschmolzen, wobei mehrfach auftretende
   * Funktionen eine Fehlermeldung im Log produzieren (und die letzte Definition
   * gewinnt). Selbiges gilt auch für die Sichtbarkeit-Abschnitte
   * 
   * @param buttonAnpassung
   *          ein ButtonAnpassung-Abschnitt wie bei wollmux:Open dokumentiert, oder
   *          null, wenn keine Anpassung erforderlich. Der oberste Knoten muss nicht
   *          Buttonanpassung sein. In der Tat ist es auch erlaubt, dass ein
   *          &lt;query results> Knoten mit mehreren Buttonanpassung-Abschnitten
   *          übergeben wird. Es ist nur wichtig, dass alle ...Tab-Abschnitte auf der
   *          selben Höhe im Baum sind.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static ConfigThingy mergeFormDescriptors(List<ConfigThingy> desc,
      ConfigThingy buttonAnpassung, String newTitle)
  {
    if (buttonAnpassung == null)
      buttonAnpassung = new ConfigThingy("Buttonanpassung");
    String plausiColor = null;
    Map<String, ConfigThingy> mapFensterIdToConfigThingy =
      new HashMap<String, ConfigThingy>();
    Map<String, ConfigThingy> mapSichtbarkeitIdToConfigThingy =
      new HashMap<String, ConfigThingy>();
    Map<String, ConfigThingy> mapFunktionenIdToConfigThingy =
      new HashMap<String, ConfigThingy>();
    Map<String, ConfigThingy> mapFunktionsdialogeIdToConfigThingy =
      new HashMap<String, ConfigThingy>();
    List<String> tabNames = new Vector<String>();
    for (ConfigThingy conf : desc)
    {
      try
      {
        plausiColor = conf.get("PLAUSI_MARKER_COLOR", 1).toString();
      }
      catch (Exception x)
      {}
  
      mergeSection(conf, "Fenster", mapFensterIdToConfigThingy, tabNames, true);
      mergeSection(conf, "Sichtbarkeit", mapSichtbarkeitIdToConfigThingy, null,
        false);
      mergeSection(conf, "Funktionen", mapFunktionenIdToConfigThingy, null, false);
      mergeSection(conf, "Funktionsdialoge", mapFunktionsdialogeIdToConfigThingy,
        null, false);
    }
  
    ConfigThingy conf = new ConfigThingy("Formular");
    conf.add("TITLE").add(newTitle);
    if (plausiColor != null) conf.add("PLAUSI_MARKER_COLOR").add(plausiColor);
  
    ConfigThingy subConf = conf.add("Fenster");
    int tabNum = -1;
    if (tabNames.size() > 1) tabNum = 0;
    Iterator<String> iter = tabNames.iterator();
    while (iter.hasNext())
    {
      ConfigThingy tabConf = mapFensterIdToConfigThingy.get(iter.next());
      buttonAnpassung(tabNum, tabConf, buttonAnpassung);
      if (++tabNum == tabNames.size() - 1) tabNum = Integer.MAX_VALUE;
      subConf.addChild(tabConf);
    }
  
    if (!mapSichtbarkeitIdToConfigThingy.isEmpty())
    {
      subConf = conf.add("Sichtbarkeit");
      for (ConfigThingy conf2 : mapSichtbarkeitIdToConfigThingy.values())
        subConf.addChild(conf2);
    }
  
    if (!mapFunktionenIdToConfigThingy.isEmpty())
    {
      subConf = conf.add("Funktionen");
      for (ConfigThingy conf2 : mapFunktionenIdToConfigThingy.values())
        subConf.addChild(conf2);
    }
  
    if (!mapFunktionsdialogeIdToConfigThingy.isEmpty())
    {
      subConf = conf.add("Funktionsdialoge");
      for (ConfigThingy conf2 : mapFunktionsdialogeIdToConfigThingy.values())
        subConf.addChild(conf2);
    }
  
    return conf;
  }

  /**
   * Geht die Enkelkinder von conf,query(sectionName) durch und trägt für jedes ein
   * Mapping von seinem Namen auf eine Kopie seiner selbst in die Map sectionMap ein.
   * Dabei wird ein vorher vorhandenes Mapping ersetzt. Falls
   * duplicatesAllowed==false, so wird eine Fehlermeldung geloggt, wenn eine
   * Ersetzung eines Mappings für einen Bezeichner durch ein neues Mapping
   * stattfindet.
   * 
   * @param tabNames
   *          falls nicht null, so werden alle Namen von Enkeln, die noch nicht in
   *          idList enthalten sind dieser in der Reihenfolge ihres Auftretens
   *          hinzugefügt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void mergeSection(ConfigThingy conf, String sectionName,
      Map<String, ConfigThingy> mapFensterIdToConfigThingy, List<String> tabNames,
      boolean duplicatesAllowed)
  {
    Iterator<ConfigThingy> parentIter = conf.query(sectionName).iterator();
    while (parentIter.hasNext())
    {
      Iterator<ConfigThingy> iter = parentIter.next().iterator();
      while (iter.hasNext())
      {
        ConfigThingy node = iter.next();
        String name = node.getName();
        if (tabNames != null && !tabNames.contains(name)) tabNames.add(name);
        if (!duplicatesAllowed && mapFensterIdToConfigThingy.containsKey(name))
          Logger.error(L.m(
            "Fehler beim Zusammenfassen mehrerer Formulare zum gemeinsamen Ausfüllen: Mehrere \"%1\" Abschnitte enthalten \"%2\"",
            sectionName, name));
  
        mapFensterIdToConfigThingy.put(name, new ConfigThingy(node));
      }
    }
  }

  /**
   * Passt die in tabConf gespeicherte Beschreibung eines Reiters einer FormularGUI
   * an entsprechend dem Buttonanpassung-Abschnitt in buttonAnpassung. Der oberste
   * Knoten muss nicht Buttonanpassung sein. In der Tat ist es auch erlaubt, dass ein
   * &lt;query results> Knoten mit mehreren Buttonanpassung-Abschnitten übergeben
   * wird. Es ist nur wichtig, dass alle ...Tab-Abschnitte auf der selben Höhe im
   * Baum sind.
   * 
   * @param tabNum
   *          0: tabConf beschreibt den ersten Tab, Integer.MAX_VALUE: tabConf
   *          beschreibt den letzten Tab, -1: tabConf beschreibt den einzigen Tab.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void buttonAnpassung(int tabNum, ConfigThingy tabConf,
      ConfigThingy buttonAnpassung)
  {
    ConfigThingy anpassung;
    switch (tabNum)
    {
      case -1:
        anpassung = buttonAnpassung.query("EinzigerTab");
        break;
      case 0:
        anpassung = buttonAnpassung.query("ErsterTab");
        break;
      case Integer.MAX_VALUE:
        anpassung = buttonAnpassung.query("LetzterTab");
        break;
      default:
        anpassung = buttonAnpassung.query("MittlererTab");
        break;
    }
  
    /*
     * Kopie machen, da wir evtl. Veränderungen vornehmen (z.B. "ALWAYS" entfernen)
     */
    anpassung = new ConfigThingy(anpassung);
  
    /*
     * NEVER und ALWAYS Angaben aufsammeln
     */
    Set<String> neverActions = new HashSet<String>();
    List<ActionUIElementPair> alwaysActions = new Vector<ActionUIElementPair>(); // of
    // ActionUIElementPair
    Iterator<ConfigThingy> anpOuterIter = anpassung.iterator(); // durchläuft die
    // *Tab Abschnitte
    while (anpOuterIter.hasNext())
    {
      // durchläuft die NEVER und ALWAYS Angaben
      Iterator<ConfigThingy> anpInnerIter = anpOuterIter.next().iterator();
      while (anpInnerIter.hasNext())
      {
        ConfigThingy neverOrAlwaysConf = anpInnerIter.next();
        if (neverOrAlwaysConf.getName().equals("NEVER"))
        {
          Iterator<ConfigThingy> neverActionIter = neverOrAlwaysConf.iterator();
          while (neverActionIter.hasNext())
            neverActions.add(neverActionIter.next().toString());
        }
        else if (neverOrAlwaysConf.getName().equals("ALWAYS"))
        {
          try
          {
            String action = neverOrAlwaysConf.get("ACTION").toString();
            neverOrAlwaysConf.setName("");
            alwaysActions.add(new ActionUIElementPair(action, neverOrAlwaysConf));
          }
          catch (Exception x)
          {
            Logger.error(
              L.m("Fehlerhafter ALWAYS-Angabe in Buttonanpassung-Abschnitt"), x);
          }
        }
      }
    }
  
    /*
     * Existierende Buttons-Abschnitte durchgehen, ihre Elemente aufsammeln (außer
     * denen, die durch NEVER verboten sind)
     */
    List<ActionUIElementPair> existingUIElements = new Vector<ActionUIElementPair>(); // of
    // ActionUIElementPair
    ConfigThingy buttonsConf = tabConf.query("Buttons");
    Iterator<ConfigThingy> buttonsOuterIter = buttonsConf.iterator(); // durchläuft
    // die
    // Buttons-Abschnitte
    while (buttonsOuterIter.hasNext())
    {
      Iterator<ConfigThingy> buttonsInnerIter = buttonsOuterIter.next().iterator(); // durchläuft
      // die
      // Eingabeelemente
      // im
      // Buttons-Abschnitt
      while (buttonsInnerIter.hasNext())
      {
        ConfigThingy buttonConf = buttonsInnerIter.next();
        String action = null;
        try
        {
          action = buttonConf.get("ACTION").toString();
        }
        catch (Exception x)
        {}
        if (action == null || !neverActions.contains(action))
          existingUIElements.add(new ActionUIElementPair(action, buttonConf));
      }
    }
  
    /*
     * den Buttons-Abschnitt löschen (weil nachher ein neuer generiert wird)
     */
    Iterator<ConfigThingy> iter = tabConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy possiblyButtonsConf = iter.next();
      if (possiblyButtonsConf.getName().equals("Buttons")) iter.remove();
    }
  
    /*
     * alwaysActions Liste in existingUIElements hineinmergen.
     */
    integrateAlwaysButtons: for (int i = 0; i < alwaysActions.size(); ++i)
    {
      ActionUIElementPair act = alwaysActions.get(i);
      /*
       * zuerst schauen, ob schon ein Button entsprechender ACTION vorhanden ist und
       * falls ja, dann mit dem nächsten Element aus alwaysActions weitermachen.
       */
      for (ActionUIElementPair act2 : existingUIElements)
      {
        if (act2.action != null && act2.action.equals(act.action))
          continue integrateAlwaysButtons;
      }
  
      /*
       * Okay, das Element gibt's noch nicht. Wir versuchen zuerst, eine
       * Einfügestelle zu finden hinter einem Button mit selber ACTION wie der
       * Vorgänger in alwaysActions (falls es einen gibt).
       */
      if (i > 0)
      {
        String predecessorAction = alwaysActions.get(i - 1).action;
        if (predecessorAction != null)
        {
          for (int k = 0; k < existingUIElements.size(); ++k)
          {
            ActionUIElementPair act2 = existingUIElements.get(k);
            if (act2.action != null && act2.action.equals(predecessorAction))
            {
              existingUIElements.add(k + 1, act);
              continue integrateAlwaysButtons;
            }
          }
        }
      }
  
      /*
       * Wenn wir keine passende Einfügestelle finden konnten, versuchen wir eine
       * Stelle zu finden vor einem Button mit selber ACTION wie der Nachfolger in
       * alwaysActions (falls es einen gibt).
       */
      if (i + 1 < alwaysActions.size())
      {
        String successorAction = alwaysActions.get(i + 1).action;
        if (successorAction != null)
        {
          for (int k = 0; k < existingUIElements.size(); ++k)
          {
            ActionUIElementPair act2 = existingUIElements.get(k);
            if (act2.action != null && act2.action.equals(successorAction))
            {
              existingUIElements.add(k, act);
              continue integrateAlwaysButtons;
            }
          }
        }
      }
  
      /*
       * Keine Einfügestelle gefunden? Dann hängen wir den Button einfach ans Ende.
       */
      existingUIElements.add(act);
    }
  
    /*
     * "glue" Elemente am Ende der Buttonliste löschen, da diese dort normalerweise
     * nicht erwünscht sind.
     */
    ListIterator<ActionUIElementPair> liter =
      existingUIElements.listIterator(existingUIElements.size());
    while (liter.hasPrevious())
    {
      ActionUIElementPair act = liter.previous();
      String type = null;
      try
      {
        type = act.uiElementDesc.get("TYPE").toString();
      }
      catch (Exception x)
      {}
      if (type != null && type.equals("glue"))
        liter.remove();
      else
        break;
    }
  
    ConfigThingy newButtons = new ConfigThingy("Buttons");
    for (ActionUIElementPair act : existingUIElements)
    {
      newButtons.addChild(act.uiElementDesc);
    }
    tabConf.addChild(newButtons);
  }

  private static class ActionUIElementPair
  {
    public String action;
  
    public ConfigThingy uiElementDesc;
  
    public ActionUIElementPair(String action, ConfigThingy uiElementDesc)
    {
      this.action = action;
      this.uiElementDesc = uiElementDesc;
    }
  }

  /**
   * Liefert abhängig von der Konfigurationseinstellung PERSISTENT_DATA_MODE
   * (annotation|transition|rdfReadLegacy|rdf) den dazugehörigen
   * PersistentDataContainer für das Dokument doc.
   * 
   * Die folgende Aufstellung zeigt das Verhalten der verschiedenen Einstellungen
   * bezüglich der möglichen Kombinationen von Metadaten in den Ausgangsdokumenten
   * und der Aktualisierung der Metadaten in den Ergebnisdokumenten. Ein "*"
   * symbolisiert dabei, welcher Metadatencontainer jeweils aktuell ist bzw. bei
   * Dokumentänderungen aktualisiert wird.
   * 
   * Ausgangsdokument -> bearbeitet durch -> Ergebnisdokument
   * 
   * [N*] -> annotation-Mode (WollMux-Alt) -> [N*]
   * 
   * [N*] -> transition-Mode -> [N*R*]
   * 
   * [N*] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [N*] -> rdf-Mode: NICHT UNTERSTÜTZT
   * 
   * [N*R*] -> annotation-Mode (WollMux-Alt) -> [N*R]
   * 
   * [N*R*] -> transition-Mode -> [N*R*]
   * 
   * [N*R*] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [N*R*] -> rdf-Mode -> [NR*]
   * 
   * [N*R] -> annotation-Mode (WollMux-Alt) -> [N*R]
   * 
   * [N*R] -> transition-Mode -> [N*R*]
   * 
   * [N*R] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [N*R] -> rdf-Mode: NICHT UNTERSTÜTZT
   * 
   * [NR*] -> annotation-Mode (WollMux-Alt) : NICHT UNTERSTÜTZT
   * 
   * [NR*] -> transition-Mode: NICHT UNTERSTÜTZT
   * 
   * [NR*] -> rdfReadLegacy-Mode: NICHT UNTERSTÜTZT
   * 
   * [NR*] -> rdf -> [NR*]
   * 
   * [R*] -> annotation-Mode (WollMux-Alt): NICHT UNTERSTÜTZT
   * 
   * [R*] -> transition-Mode -> [N*R*]
   * 
   * [R*] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [R*] -> rdf-Mode -> [R*]
   * 
   * Agenda: [N]=Dokument mit Notizen; [R]=Dokument mit RDF-Metadaten; [NR]=Dokument
   * mit Notizen und RDF-Metadaten; *=N/R enthält aktuellen Stand;
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  public static PersistentDataContainer createPersistentDataContainer(
      XTextDocument doc)
  {
    ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
    String pdMode;
    try
    {
      pdMode = wmConf.query(PersistentDataContainer.PERSISTENT_DATA_MODE).getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      pdMode = PersistentDataContainer.PERSISTENT_DATA_MODE_TRANSITION;
      Logger.debug(L.m("Attribut %1 nicht gefunden. Verwende Voreinstellung '%2'.",
        PersistentDataContainer.PERSISTENT_DATA_MODE, pdMode));
    }
  
    try
    {
      if (PersistentDataContainer.PERSISTENT_DATA_MODE_TRANSITION.equalsIgnoreCase(pdMode))
      {
        return new TransitionModeDataContainer(doc);
      }
      else if (PersistentDataContainer.PERSISTENT_DATA_MODE_RDFREADLEGACY.equalsIgnoreCase(pdMode))
      {
        return new RDFReadLegacyModeDataContainer(doc);
      }
      else if (PersistentDataContainer.PERSISTENT_DATA_MODE_RDF.equalsIgnoreCase(pdMode))
      {
        return new RDFBasedPersistentDataContainer(doc);
      }
      else if (PersistentDataContainer.PERSISTENT_DATA_MODE_ANNOTATION.equalsIgnoreCase(pdMode))
      {
        return new AnnotationBasedPersistentDataContainer(doc);
      }
      else
      {
        Logger.error(L.m(
          "Ungültiger Wert '%1' für Attribut %2. Verwende Voreinstellung '%3' statt dessen.",
          pdMode, PersistentDataContainer.PERSISTENT_DATA_MODE, PersistentDataContainer.PERSISTENT_DATA_MODE_TRANSITION));
        return new TransitionModeDataContainer(doc);
      }
    }
    catch (RDFMetadataNotSupportedException e)
    {
      Logger.log(L.m(
        "Die Einstellung '%1' für Attribut %2 ist mit dieser OpenOffice.org-Version nicht kompatibel. Verwende Einstellung '%3' statt dessen.",
        pdMode, PersistentDataContainer.PERSISTENT_DATA_MODE, PersistentDataContainer.PERSISTENT_DATA_MODE_ANNOTATION));
      return new AnnotationBasedPersistentDataContainer(doc);
    }
  }
}
