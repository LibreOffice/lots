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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNamed;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
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
import com.sun.star.util.XModifiable2;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.core.document.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

/**
 * Diese Klasse repräsentiert das Modell eines aktuell geöffneten TextDokuments und
 * ermöglicht den Zugriff auf alle interessanten Aspekte des TextDokuments.
 *
 * @author christoph.lutz
 *
 */
public class TextDocumentModel
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TextDocumentModel.class);

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
   * Pattern zum Erkennen der Bookmarks, die entfernt werden sollen.
   */
  public static final Pattern BOOKMARK_KILL_PATTERN = Pattern.compile(
      "(\\A\\s*(WM\\s*\\(.*CMD\\s*'((form)|(setGroups)|(insertFormValue))'.*\\))\\s*\\d*\\z)"
          + "|(\\A\\s*(WM\\s*\\(.*CMD\\s*'(setType)'.*'formDocument'\\))\\s*\\d*\\z)"
          + "|(\\A\\s*(WM\\s*\\(.*'formDocument'.*CMD\\s*'(setType)'.*\\))\\s*\\d*\\z)");

  /**
   * Pattern zum Erkennen von WollMux-Bookmarks.
   */
  public static final Pattern WOLLMUX_BOOKMARK_PATTERN = Pattern
      .compile("(\\A\\s*(WM\\s*\\(.*\\))\\s*\\d*\\z)");

  /**
   * Prefix, mit dem die Namen aller automatisch generierten dokumentlokalen
   * Funktionen beginnen.
   */
  public static final String AUTOFUNCTION_PREFIX = "AUTOFUNCTION_";

  /**
   * Ermöglicht den Zugriff auf eine Collection aller FormField-Objekte in diesem
   * TextDokument über den Namen der zugeordneten ID. Die in dieser Map enthaltenen
   * FormFields sind nicht in {@link #idToTextFieldFormFields} enthalten und
   * umgekehrt.
   */
  private Map<String, List<FormField>> idToFormFields;

  /**
   * Liefert zu einer ID eine {@link java.util.List} von FormField-Objekten, die alle
   * zu Textfeldern ohne ein umschließendes WollMux-Bookmark gehören, aber trotzdem
   * vom WollMux interpretiert werden. Die in dieser Map enthaltenen FormFields sind
   * nicht in {@link #idToFormFields} enthalten und umgekehrt.
   */
  private Map<String, List<FormField>> idToTextFieldFormFields;

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
  private volatile String[] fragUrls;

  /**
   * Gibt an, ob das Dokument ein Template ist oder wie ein Template zu behandeln
   * ist.
   */
  private volatile boolean isTemplate;

  /**
   * Gibt an, ob das Dokument wie ein Dokument mit WollMux-Formularfunktion zu
   * behandeln ist.
   */
  private volatile boolean isFormDocument;

  /**
   * Enthält die Namen der aktuell gesetzten Druckfunktionen.
   */
  private volatile Set<String> printFunctions;

  /**
   * Enthält die Formularbeschreibung des Dokuments oder null, wenn die
   * Formularbeschreibung noch nicht eingelesen wurde.
   */
  private volatile ConfigThingy formularConf;

  /**
   * Enthält die aktuellen Werte der Formularfelder als Zuordnung id -> Wert.
   */
  private Map<String, String> formFieldValues;

  /**
   * Verantwortlich für das Speichern persistenter Daten.
   */
  private PersistentDataContainer persistentData;

  /**
   * Enthält die Kommandos dieses Dokuments.
   */
  private volatile DocumentCommands documentCommands;

  /**
   * Enthält eine Map mit den Namen aller (bisher gesetzter) Sichtbarkeitsgruppen auf
   * deren aktuellen Sichtbarkeitsstatus (sichtbar = true, unsichtbar = false)
   */
  private Map<String, Boolean> mapGroupIdToVisibilityState;

  /**
   * Enthält ein ein Mapping von alten FRAG_IDs fragId auf die jeweils neuen FRAG_IDs
   * newFragId, die über im Dokument enthaltene Dokumentkommando WM(CMD
   * 'overrideFrag' FRAG_ID 'fragId' NEW_FRAG_ID 'newFragId') entstanden sind.
   */
  private Map<String, String> overrideFragMap;

  /**
   * Enthält null oder ab dem ersten Aufruf von getMailmergeConf() die Metadaten für
   * den Seriendruck in einem ConfigThingy, das derzeit in der Form
   * "Seriendruck(Datenquelle(...))" aufgebaut ist.
   */
  private volatile ConfigThingy mailmergeConf;

  /**
   * Enthält die Versionsnummer (nicht Revision, da diese zwischen git und svn
   * unterschiedlich ist) des WollMux, der das Dokument zuletzt angefasst hat (vor
   * diesem gerade laufenden).
   */
  private volatile String lastTouchedByWollMuxVersion;

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
   * Wird and FormGUI und FormController in mapIdToPresetValue übergeben, wenn der
   * Wert des entsprechenden Feldes nicht korrekt widerhergestellt werden kann.
   * ACHTUNG! Diese Konstante muss als Objekt übergeben werden, da sie == verglichen
   * wird.
   */
  public static final String FISHY = L.m("!!!PRÜFEN!!!");

  /**
   * Pattern zum Erkennen von InputUser-Feldern, die eine WollMux-Funktion
   * referenzieren (z.B. die Spezialfelder des WollMux-Seriendrucks).
   */
  public static final Pattern INPUT_USER_FUNCTION = Pattern
      .compile("\\A\\s*(WM\\s*\\(.*FUNCTION\\s*'[^']*'.*\\))\\s*\\d*\\z");

  private String wollmuxVersion;

  private String oooVersion;

  /**
   * Erzeugt ein neues TextDocumentModel zum XTextDocument doc und sollte nie
   * direkt aufgerufen werden, da neue TextDocumentModels über
   * die Methode getTextDocumentController(XTextDocument) des DocumentManager erzeugt und
   * verwaltet werden.
   *
   * @param doc
   */
  public TextDocumentModel(XTextDocument doc,
      PersistentDataContainer persistentDataContainer, String wollmuxVersion,
      String oooVersion)
  {
    this.doc = doc;
    this.wollmuxVersion = wollmuxVersion;
    this.oooVersion = oooVersion;
    this.idToFormFields = new HashMap<>();
    idToTextFieldFormFields = new HashMap<>();
    staticTextFieldFormFields = new Vector<>();
    this.fragUrls = new String[] {};
    this.printFunctions = new HashSet<>();
    this.formularConf = null;
    this.formFieldValues = new HashMap<>();
    this.mapGroupIdToVisibilityState = new HashMap<>();
    this.overrideFragMap = new HashMap<>();

    // Kommandobaum erzeugen (modified-Status dabei unberührt lassen):
    boolean modified = isDocumentModified();
    this.documentCommands = new DocumentCommands(UNO.XBookmarksSupplier(doc));
    documentCommands.update();
    setDocumentModified(modified);

    // Auslesen der Persistenten Daten:
    this.persistentData = persistentDataContainer;
    String setTypeData = persistentData.getData(DataID.SETTYPE);
    parsePrintFunctions(persistentData.getData(DataID.PRINTFUNCTION));
    parseFormValues(persistentData.getData(DataID.FORMULARWERTE));
    lastTouchedByWollMuxVersion = persistentData
        .getData(DataID.TOUCH_WOLLMUXVERSION);
    if (lastTouchedByWollMuxVersion == null)
      lastTouchedByWollMuxVersion = VERSION_UNKNOWN;
    lastTouchedByOOoVersion = persistentData.getData(DataID.TOUCH_OOOVERSION);
    if (lastTouchedByOOoVersion == null)
    {
      lastTouchedByOOoVersion = VERSION_UNKNOWN;
    }

    // Type auswerten
    this.isTemplate = !hasURL();
    this.isFormDocument = false;

    setType(setTypeData);
  }

  public Map<String, List<FormField>> getIdToTextFieldFormFields()
  {
    return idToTextFieldFormFields;
  }

  public List<FormField> getStaticTextFieldFormFields()
  {
    return staticTextFieldFormFields;
  }

  public Map<String, List<FormField>> getIdToFormFields()
  {
    return idToFormFields;
  }

  public Map<String, Boolean> getMapGroupIdToVisibilityState()
  {
    return mapGroupIdToVisibilityState;
  }

  public void setMapGroupIdToVisibilityState(
      Map<String, Boolean> mapGroupIdToVisibilityState)
  {
    this.mapGroupIdToVisibilityState = mapGroupIdToVisibilityState;
  }

  /**
   * Liefert die Version des letzten WollMux der dieses Dokument angefasst hat (vor
   * dem aktuell laufenden) oder {@link #VERSION_UNKNOWN} falls unbekannt.
   *
   * Achtung! Es kann günstiger sein, hier im TextDocumentModel an zentraler Stelle
   * Funktionen einzubauen zum Vergleich des Versionsstrings mit bestimmten anderen
   * Versionen, als das Parsen/Vergleichen von Versionsstrings an mehreren Stellen zu
   * replizieren.
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
   */
  public String getLastTouchedByOOoVersion()
  {
    return lastTouchedByOOoVersion;
  }

  public PersistentDataContainer getPersistentData()
  {
    return persistentData;
  }

  public void setPersistentData(PersistentDataContainer persistentData)
  {
    this.persistentData = persistentData;
  }

  public ConfigThingy getFormularConf()
  {
    return formularConf;
  }

  public void setFormularConf(ConfigThingy formularConf)
  {
    this.formularConf = formularConf;
  }

  public ConfigThingy getMailmergeConf()
  {
    return mailmergeConf;
  }

  public void setMailmergeConf(ConfigThingy mailmergeConf)
  {
    this.mailmergeConf = mailmergeConf;
  }

  /**
   * Schreibt die WollMux und OOo-Version in {@link PersistentDataContainer}. Die
   * Rückgabewerte von {@link #getLastTouchedByOOoVersion()} und
   * {@link #getLastTouchedByWollMuxVersion()} sind davon NICHT betroffen, da diese
   * immer den Zustand beim Öffnen repräsentieren. Der modified-Zustand des Dokuments
   * wird durch diese Funktion nicht verändert.
   */
  public synchronized void updateLastTouchedByVersionInfo()
  {
    if (!haveUpdatedLastTouchedByVersionInfo)
    {
      // Logger.error(new Exception()); //um einen Stacktrace zu kriegen
      haveUpdatedLastTouchedByVersionInfo = true;
      boolean modified = isDocumentModified();
      persistentData.setData(DataID.TOUCH_WOLLMUXVERSION,
          wollmuxVersion);
      persistentData.setData(DataID.TOUCH_OOOVERSION, oooVersion);
      setDocumentModified(modified);
    }
  }

  /**
   * Liefert den Dokument-Kommandobaum dieses Dokuments.
   *
   * @return der Dokument-Kommandobaum dieses Dokuments.
   */
  public synchronized DocumentCommands getDocumentCommands()
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
  public synchronized Iterator<VisibilityElement> visibleElementsIterator()
  {
    ArrayList<VisibilityElement> visibleElements = new ArrayList<>();
    for (VisibilityElement ve : documentCommands.getSetGroups())
    {
      visibleElements.add(ve);
    }
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
   */
  private void parsePrintFunctions(String data)
  {
    if (data == null || data.length() == 0)
    {
      return;
    }

    final String errmsg = L
        .m("Fehler beim Einlesen des Druckfunktionen-Abschnitts '%1':", data);

    ConfigThingy conf = new ConfigThingy("dummy");
    try
    {
      conf = new ConfigThingy("dummy", data);
    }
    catch (IOException e)
    {
      LOGGER.error(errmsg, e);
    }
    catch (SyntaxErrorException e)
    {
      try
      {
        // Abwärtskompatibilität mit älteren PrintFunction-Blöcken, in denen nur
        // der Funktionsname steht:
        ConfigThingy.checkIdentifier(data);
        conf = new ConfigThingy("dummy",
            "WM(Druckfunktionen((FUNCTION '" + data + "')))");
      }
      catch (java.lang.Exception forgetMe)
      {
        // Fehlermeldung des SyntaxFehlers ausgeben
        LOGGER.error(errmsg, e);
      }
    }

    ConfigThingy functions = conf.query("WM").query("Druckfunktionen")
        .queryByChild("FUNCTION");
    for (Iterator<ConfigThingy> iter = functions.iterator(); iter.hasNext();)
    {
      ConfigThingy func = iter.next();
      String name = func.getString("FUNCTION");

      if (name != null && !name.isEmpty())
      {
        printFunctions.add(name);
      }
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
  public static void addToFormDescription(ConfigThingy formDesc, String value)
  {
    if (value == null || value.length() == 0)
    {
      return;
    }

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("", null, new StringReader(value));
    }
    catch (java.lang.Exception e)
    {
      LOGGER.error(L.m("Die Formularbeschreibung ist fehlerhaft"), e);
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

  public void exportFormValues(OutputStream out)
  {
    String data = persistentData.getData(DataID.FORMULARWERTE);
    try (PrintWriter pw = new PrintWriter(out))
    {
      pw.print(data);
    }
  }

  public void importFormValues(File f) throws IOException
  {
    String data = new String(Files.readAllBytes(f.toPath()));
    parseFormValues(data);
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
    if (werteStr == null)
    {
      return;
    }

    // Werte-Abschnitt holen:
    ConfigThingy werte;
    try
    {
      ConfigThingy conf = new ConfigThingy("", null,
          new StringReader(werteStr));
      werte = conf.get("WM").get("Formularwerte");
    }
    catch (NodeNotFoundException | IOException | SyntaxErrorException e)
    {
      LOGGER.error(L.m("Formularwerte-Abschnitt ist fehlerhaft"), e);
      return;
    }

    // "Formularwerte"-Abschnitt auswerten.
    for (ConfigThingy element : werte)
    {
      String id = element.getString("ID");
      String value = element.getString("VALUE");

      if (id != null && value != null)
      {
        formFieldValues.put(id, value);
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
  public synchronized void setIDToFormFields(
      Map<String, List<FormFieldFactory.FormField>> idToFormFields)
  {
    this.idToFormFields = idToFormFields;
  }

  /**
   * Diese Methode iteriert fields und liefert den Wert des ersten gefundenen
   * untransformierten Formularfeldes zurück, oder null, wenn kein untransformiertes
   * Formularfeld gefunden wurde.
   */
  public String getFirstUntransformedValue(List<FormField> fields)
  {
    for (FormField field : fields)
    {
      if (field.getTrafoName() == null)
      {
        return field.getValue();
      }
    }
    return null;
  }

  /**
   * Der DocumentCommandInterpreter liest sich die Liste der setFragUrls()
   * gespeicherten Fragment-URLs hier aus, wenn die Dokumentkommandos insertContent
   * ausgeführt werden.
   *
   * @return Zwischenspeicher der Fragment-Urls.
   */
  public String[] getFragUrls()
  {
    return fragUrls;
  }

  /**
   * Über diese Methode kann der openDocument-Eventhandler die Liste der mit einem
   * insertContent-Kommando zu öffnenden frag-urls speichern.
   */
  public void setFragUrls(String[] fragUrls)
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
   * @throws OverrideFragChainException
   *           Wenn eine fragId oder newFragId bereits Ziel/Quelle einer anderen
   *           Ersetzungsregel sind, dann entsteht eine Ersetzungskette, die nicht
   *           zugelassen ist.
   */
  public synchronized void setOverrideFrag(String fragId, String newFragId)
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

    private final String fragId;

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
  public synchronized String getOverrideFrag(String fragId)
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
  public boolean isTemplate()
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
  public synchronized boolean hasURL()
  {
    return doc.getURL() != null && !doc.getURL().isEmpty();
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
  public boolean isFormDocument()
  {
    return isFormDocument;
  }

  /**
   * Liefert true, wenn das Dokument eine nicht-leere Formularbeschreibung mit einem
   * nicht-leeren Fenster-Abschnitt enthält. In diesem Fall soll die FormGUI
   * gestartet werden.
   */
  public synchronized boolean hasFormGUIWindow()
  {
    try
    {
      ConfigThingy windows = getFormDescription().query("Formular").query("Fenster");
      if (windows.count() > 0)
      {
        return windows.getLastChild().count() != 0;
      }
      return false;
    }
    catch (NodeNotFoundException e)
    {
      LOGGER.trace("", e);
      return false;
    }
  }

  /**
   * Makiert dieses Dokument als Formulardokument (siehe {@link #isFormDocument()})
   */
  public void setFormDocument()
  {
    isFormDocument = true;
  }

  /**
   * Setzt abhängig von typeStr die NICHT PRESISTENTEN Zustände {@link #isTemplate()}
   * und {@link #isFormDocument()}, wenn es sich um einen der Dokumenttypen
   * normalTemplate, templateTemplate oder formDocument handelt.
   */
  public void setType(String typeStr)
  {
    if (typeStr == null)
      return;
    else if ("normalTemplate".equalsIgnoreCase(typeStr))
      isTemplate = true;
    else if ("templateTemplate".equalsIgnoreCase(typeStr))
      isTemplate = false;
    else if ("formDocument".equalsIgnoreCase(typeStr))
    {
      isFormDocument = true;
    }
  }

  public void addPrintFunction(String functionName)
  {
    printFunctions.add(functionName);
    storePrintFunctions();
  }

  public void removePrintFunction(String functionName)
  {
    if (!printFunctions.remove(functionName))
    {
      return;
    }
    storePrintFunctions();
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
   */
  private void storePrintFunctions()
  {
    if (printFunctions.isEmpty())
    {
      persistentData.removeData(DataID.PRINTFUNCTION);
    }
    else
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
      boolean needConfigThingy = printFunctions.size() > 1;

      // Elemente nach Namen sortieren (definierte Reihenfolge bei der Ausgabe)
      ArrayList<String> names = new ArrayList<>(printFunctions);
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
  public Set<String> getPrintFunctions()
  {
    return printFunctions;
  }

  /**
   * Diese Methode setzt den ViewCursor auf den Anfang des Ankers des
   * Sichtbarkeitselements.
   *
   * @param visibleElement
   *          Das Sichtbarkeitselement, auf dessen Anfang des Ankers der ViewCursor
   *          gesetzt werden soll.
   */
  public synchronized void focusRangeStart(VisibilityElement visibleElement)
  {
    try
    {
      getViewCursor().gotoRange(visibleElement.getAnchor().getStart(), false);
    }
    catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
    }
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
  public synchronized SetJumpMark getFirstJumpMark()
  {
    return documentCommands.getFirstJumpMark();
  }

  /**
   * Diese Methode liefert die FeldIDs aller im Dokument enthaltenen Felder.
   */
  public synchronized Set<String> getAllFieldIDs()
  {
    HashSet<String> ids = new HashSet<>();
    ids.addAll(idToFormFields.keySet());
    ids.addAll(getIdToTextFieldFormFields().keySet());
    return ids;
  }

  public synchronized Map<String, String> getFormFieldValues()
  {
    return formFieldValues;
  }

  /**
   * Liefert die zum aktuellen Stand gesetzten Formularwerte in einer Map mit ID als
   * Schlüssel. Änderungen an der zurückgelieferten Map zeigen keine Wirkung im
   * TextDocumentModel (da nur eine Kopie der internen Map zurückgegeben wird).
   */
  public synchronized Map<String, String> getFormFieldValuesMap()
  {
    return new HashMap<>(formFieldValues);
  }

  public synchronized void clearFormFieldValues()
  {
    for (String key : formFieldValues.keySet())
    {
      formFieldValues.put(key, "");
    }
  }

  /**
   * Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein Controller
   * (oder auch kein ViewCursor) für das Dokument verfügbar ist.
   *
   * @return Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein
   *         Controller (oder auch kein ViewCursor) für das Dokument verfügbar ist.
   */
  public synchronized XTextViewCursor getViewCursor()
  {
    if (UNO.XModel(doc) == null)
    {
      return null;
    }
    XTextViewCursorSupplier suppl = UNO
        .XTextViewCursorSupplier(UNO.XModel(doc).getCurrentController());
    if (suppl != null)
    {
      return suppl.getViewCursor();
    }
    return null;
  }

  /**
   * Diese Methode liefert true, wenn der viewCursor im Dokument aktuell nicht
   * kollabiert ist und damit einen markierten Bereich aufspannt, andernfalls false.
   */
  public synchronized boolean hasSelection()
  {
    XTextViewCursor vc = getViewCursor();
    if (vc != null)
    {
      return !vc.isCollapsed();
    }
    return false;
  }

  /**
   * Liefert die aktuelle Formularbeschreibung des Dokuments; Wurde die
   * Formularbeschreibung bis jetzt noch nicht eingelesen, so wird sie spätestens
   * jetzt eingelesen.
   */
  public synchronized ConfigThingy getFormDescription()
  {
    if (formularConf == null)
    {
      LOGGER.debug(L.m("Einlesen der Formularbeschreibung von %1", this));
      formularConf = new ConfigThingy("WM");
      addToFormDescription(formularConf,
          persistentData.getData(DataID.FORMULARBESCHREIBUNG));

      ConfigThingy title = formularConf.query("TITLE");
      if (title.count() > 0)
        LOGGER.debug(
            L.m("Formular %1 eingelesen.", title.stringRepresentation(true,
                '\'')));
    }

    return formularConf;
  }

  /**
   * Liefert den Seriendruck-Knoten der im Dokument gespeicherten
   * Seriendruck-Metadaten zurück. Die Metadaten liegen im Dokument beispielsweise in
   * der Form "WM(Seriendruck(Datenquelle(...)))" vor - diese Methode liefert aber
   * nur der Knoten "Seriendruck" zurück. Enthält das Dokument keine
   * Seriendruck-Metadaten, so liefert diese Methode einen leeren
   * "Seriendruck"-Knoten zurück.
   */
  public synchronized ConfigThingy getMailmergeConfig()
  {
    if (mailmergeConf == null)
    {
      String data = persistentData.getData(DataID.SERIENDRUCK);
      mailmergeConf = new ConfigThingy("Seriendruck");
      if (data != null)
        try
        {
          mailmergeConf = new ConfigThingy("", data).query("WM")
              .query("Seriendruck").getLastChild();
        }
        catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
    }
    return mailmergeConf;
  }

  /**
   * Liefert einen Funktionen-Abschnitt der Formularbeschreibung, in dem die lokalen
   * Auto-Funktionen abgelegt werden können. Besitzt die Formularbeschreibung keinen
   * Funktionen-Abschnitt, so wird der Funktionen-Abschnitt und ggf. auch ein
   * übergeordneter Formular-Abschnitt neu erzeugt.
   */
  public ConfigThingy getFunktionenConf()
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
   * Setzt den ViewCursor auf das erste untransformierte Formularfeld, das den
   * Formularwert mit der ID fieldID darstellt. Falls kein untransformiertes
   * Formularfeld vorhanden ist, wird ein transformiertes gewählt.
   *
   * @param fieldId
   *          Die ID des Formularfeldes, das angesprungen werden soll.
   */
  public synchronized void focusFormField(String fieldId)
  {
    FormField field;
    List<FormField> formFields = getIdToTextFieldFormFields().get(fieldId);
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
      if (field != null)
      {
        field.focus();
      }
    }
    catch (RuntimeException e)
    {
      // Absicherung gegen das manuelle Löschen von Dokumentinhalten.
      LOGGER.trace("", e);
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
  protected static FormField preferUntransformedFormField(
      List<FormField> formFields)
  {
    if (formFields == null)
    {
      return null;
    }
    Iterator<FormField> iter = formFields.iterator();
    FormField field = null;
    while (iter.hasNext())
    {
      FormField f = iter.next();
      if (field == null)
      {
        field = f;
      }
      if (f.getTrafoName() == null)
      {
        return f;
      }
    }
    return field;
  }

  /**
   * Liefert die Gesamtseitenzahl des Dokuments oder 0, wenn die Seitenzahl nicht
   * bestimmt werden kann.
   *
   * @return Liefert die Gesamtseitenzahl des Dokuments oder 0, wenn die Seitenzahl
   *         nicht bestimmt werden kann.
   */
  public synchronized int getPageCount()
  {
    try
    {
      return (int) AnyConverter
          .toLong(UNO.getProperty(doc.getCurrentController(),
              "PageCount"));
    }
    catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
      return 0;
    }
  }

  /**
   * Liefert true, wenn das Dokument als "modifiziert" markiert ist und damit z.B.
   * die "Speichern?" Abfrage vor dem Schließen erscheint.
   */
  public synchronized boolean isDocumentModified()
  {
    try
    {
      return UNO.XModifiable(doc).isModified();
    }
    catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
      return false;
    }
  }

  /**
   * Diese Methode setzt den DocumentModified-Status auf state.
   *
   * @param state
   */
  public synchronized void setDocumentModified(boolean state)
  {
    try
    {
      UNO.XModifiable(doc).setModified(state);
    }
    catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
    }
  }

  /**
   * Wenn true übergeben wird, wird der Status des Dokuments nie auf
   * modified gesetzt.
   *
   * @param state
   */
  public synchronized void setDocumentModifiable(boolean state)
  {
    try
    {
      XModifiable2 mod2 = UnoRuntime.queryInterface(XModifiable2.class, doc);
      if (state)
      {
        mod2.enableSetModified();
      }
      else
      {
        mod2.disableSetModified();
      }
    }
    catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
    }
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
  public synchronized void close()
  {
    // Damit OOo vor dem Schließen eines veränderten Dokuments den
    // save/dismiss-Dialog anzeigt, muss die suspend()-Methode aller
    // XController gestartet werden, die das Model der Komponente enthalten.
    // Man bekommt alle XController über die Frames, die der Desktop liefert.
    boolean closeOk = true;
    if (UNO.XFramesSupplier(UNO.desktop) != null)
    {
      XFrame[] frames = UNO.XFramesSupplier(UNO.desktop).getFrames()
          .queryFrames(FrameSearchFlag.ALL);
      for (int i = 0; i < frames.length; i++)
      {
        XController c = frames[i].getController();
        if (c != null && UnoRuntime.areSame(c.getModel(), doc)
            && !c.suspend(true))
        {
          // closeOk wird auf false gesetzt, wenn im save/dismiss-Dialog auf die
          // Schaltflächen "Speichern" und "Abbrechen" gedrückt wird. Bei
          // "Verwerfen" bleibt closeOK unverändert (also true).
          closeOk = false;
        }
      }
    }

    // Wurde das Dokument erfolgreich gespeichert, so merkt dies der Test
    // getDocumentModified() == false. Wurde der save/dismiss-Dialog mit
    // "Verwerfen" beendet, so ist closeOK==true und es wird beendet. Wurde der
    // save/dismiss Dialog abgebrochen, so soll das Dokument nicht geschlossen
    // werden.
    if (closeOk || !isDocumentModified())
    {

      // Hier das eigentliche Schließen:
      try
      {
        if (UNO.XCloseable(doc) != null)
        {
          UNO.XCloseable(doc).close(true);
        }
      }
      catch (CloseVetoException e)
      {
        LOGGER.trace("", e);
      }

    }
    else if (UNO.XFramesSupplier(UNO.desktop) != null)
    {

      // Tritt in Kraft, wenn "Abbrechen" betätigt wurde. In diesem Fall werden
      // die Controllers mit suspend(FALSE) wieder reaktiviert.
      XFrame[] frames = UNO.XFramesSupplier(UNO.desktop).getFrames()
          .queryFrames(FrameSearchFlag.ALL);
      for (int i = 0; i < frames.length; i++)
      {
        XController c = frames[i].getController();
        if (c != null && UnoRuntime.areSame(c.getModel(), doc))
        {
          c.suspend(false);
        }
      }

    }
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
   */
  public synchronized void addNewDocumentCommand(XTextRange r, String cmdStr)
  {
    documentCommands.addNewDocumentCommand(r, cmdStr);
  }

  /**
   * Wenn das Benutzerfeld mit dem Namen userFieldName vom WollMux interpretiert wird
   * (weil der Name in der Form {@code WM(FUNCTION '<name>')} aufgebaut ist), dann liefert
   * diese Funktion den Namen {@code <name>} der Funktion zurück; in allen anderen Fällen
   * liefert die Methode null zurück.
   *
   * @param userFieldName
   *          Name des Benutzerfeldes
   * @return den Namen der in diesem Benutzerfeld verwendeten Funktion oder null,
   *         wenn das Benutzerfeld nicht vom WollMux interpretiert wird.
   */
  public static String getFunctionNameForUserFieldName(String userFieldName)
  {
    if (userFieldName == null)
    {
      return null;
    }

    Matcher m = TextDocumentModel.INPUT_USER_FUNCTION.matcher(userFieldName);

    if (!m.matches())
    {
      return null;
    }
    String confStr = m.group(1);

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("INSERT", confStr);
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
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
   *          Ein Kommandostring eines Dokumentkommandos in der Form {@code WM(CMD
   *          '<command>' ...)}
   * @return Name der Trafofunktion oder null.
   */
  public static String getFunctionNameForDocumentCommand(String cmdStr)
  {
    ConfigThingy wm = new ConfigThingy("");
    try
    {
      wm = new ConfigThingy("", cmdStr).get("WM");
    }
    catch (NodeNotFoundException | IOException | SyntaxErrorException e)
    {
      LOGGER.trace("", e);
    }

    String cmd = wm.getString("CMD", "");

    if ("insertFormValue".equalsIgnoreCase(cmd))
    {
      return wm.getString("TRAFO");
    }

    return null;
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
   *         unter der die TRAFO modifiziert werden kann.
   */
  public synchronized ConfigThingy getFormFieldTrafoFromSelection()
  {
    XTextCursor vc = getViewCursor();
    if (vc == null)
    {
      return null;
    }

    Map<String, Integer> collectedTrafos = collectTrafosFromEnumeration(vc);

    // Auswertung von collectedTrafos
    HashSet<String> completeFields = new HashSet<>();
    HashSet<String> startedFields = new HashSet<>();
    HashSet<String> finishedFields = new HashSet<>();

    for (Map.Entry<String, Integer> ent : collectedTrafos.entrySet())
    {
      String trafo = ent.getKey();
      int complete = ent.getValue().intValue();
      if (complete == 1)
      {
        startedFields.add(trafo);
      }
      if (complete == 2)
      {
        finishedFields.add(trafo);
      }
      if (complete == 3)
      {
        completeFields.add(trafo);
      }
    }

    // Das Feld ist eindeutig bestimmbar, wenn genau ein vollständiges Feld oder
    // als Fallback genau eine Startmarke gefunden wurde.
    String trafoName = null;
    if (completeFields.size() > 1 || startedFields.size() > 1)
      return null; // nicht eindeutige Felder
    else if (completeFields.size() == 1)
      trafoName = completeFields.iterator().next();
    else if (startedFields.size() == 1)
    {
      trafoName = startedFields.iterator().next();
    }

    // zugehöriges ConfigThingy aus der Formularbeschreibung zurückliefern.
    if (trafoName != null)
      try
      {
        return getFormDescription().query("Formular").query("Funktionen").query(
            trafoName, 2).getLastChild();
      }
      catch (NodeNotFoundException e)
      {
        LOGGER.trace("", e);
      }

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
   */
  private static Map<String, Integer> collectTrafosFromEnumeration(
      XTextRange textRange)
  {
    HashMap<String, Integer> collectedTrafos = new HashMap<>();

    if (textRange == null)
    {
      return collectedTrafos;
    }
    XEnumerationAccess parEnumAcc = UNO.XEnumerationAccess(
        textRange.getText().createTextCursorByRange(textRange));
    if (parEnumAcc == null)
    {
      return collectedTrafos;
    }

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
        LOGGER.error("", e);
      }
      if (porEnumAcc == null)
      {
        continue;
      }

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
          LOGGER.error("", e);
        }

        // InputUser-Textfelder verarbeiten
        XTextField tf = UNO.XTextField(Utils.getProperty(portion, "TextField"));
        if (tf != null
            && UNO.supportsService(tf, "com.sun.star.text.TextField.InputUser"))
        {
          String varName = "" + Utils.getProperty(tf, "Content");
          String t = getFunctionNameForUserFieldName(varName);
          if (t != null)
          {
            collectedTrafos.put(t, Integer.valueOf(3));
          }
        }

        // Dokumentkommandos (derzeit insertFormValue) verarbeiten
        XNamed bm = UNO.XNamed(Utils.getProperty(portion, "Bookmark"));
        if (bm != null)
        {
          String name = "" + bm.getName();

          boolean isStart = false;
          boolean isEnd = false;
          try
          {
            boolean isCollapsed = AnyConverter
                .toBoolean(Utils.getProperty(portion, "IsCollapsed"));
            isStart = AnyConverter
                .toBoolean(Utils.getProperty(portion, "IsStart"))
                || isCollapsed;
            isEnd = !isStart || isCollapsed;
          }
          catch (IllegalArgumentException e)
          {
            LOGGER.trace("", e);
          }

          String docCmd = getDocumentCommandByBookmarkName(name);
          if (docCmd != null)
          {
            String t = getFunctionNameForDocumentCommand(docCmd);
            if (t != null)
            {
              Integer s = collectedTrafos.get(t);
              if (s == null)
              {
                s = Integer.valueOf(0);
              }
              if (isStart)
              {
                s = Integer.valueOf(s.intValue() | 1);
              }
              if (isEnd)
              {
                s = Integer.valueOf(s.intValue() | 2);
              }
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
   */
  public static String getDocumentCommandByBookmarkName(String bookmarkName)
  {
    Matcher m = WOLLMUX_BOOKMARK_PATTERN.matcher(bookmarkName);
    if (m.matches())
    {
      return m.group(1);
    }
    return null;
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
   */
  public synchronized ReferencedFieldID[] getReferencedFieldIDsThatAreNotInSchema(
      Set<String> schema)
  {
    ArrayList<ReferencedFieldID> list = new ArrayList<>();

    // Alle ReferencedFieldIDs des Dokuments alphabetisch sortiert
    // zurückliefern.
    List<String> sortedIDs = new ArrayList<>(getAllFieldIDs());
    Collections.sort(sortedIDs);
    for (Iterator<String> iter = sortedIDs.iterator(); iter.hasNext();)
    {
      String id = iter.next();
      if (schema.contains(id))
      {
        continue;
      }
      List<FormField> fields = new ArrayList<>();
      if (idToFormFields.containsKey(id))
      {
        fields.addAll(idToFormFields.get(id));
      }
      if (getIdToTextFieldFormFields().containsKey(id))
        fields.addAll(getIdToTextFieldFormFields().get(id));
      boolean hasTrafo = false;
      for (Iterator<FormField> fieldIter = fields.iterator(); fieldIter
          .hasNext();)
      {
        FormField field = fieldIter.next();
        if (field.getTrafoName() != null)
        {
          hasTrafo = true;
        }
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
     */
    public String getFieldId()
    {
      return fieldId;
    }

    /**
     * Ist eine TRAFO gesetzt?
     *
     * @return true, wenn auf dem Feld eine TRAFO gesetzt ist, sonst false.
     */
    public boolean isTransformed()
    {
      return isTransformed;
    }
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
    private List<SubstElement> list = new ArrayList<>();

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
        buffy.append(
            ele.isField() ? "<" + ele.getValue() + ">" : ele.getValue());
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
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zurück, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   *
   * @param element
   *          Das erste gefundene AnnotationField oder null, wenn keines gefunden
   *          wurde.
   */
  public static XTextField findAnnotationFieldRecursive(Object element)
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
          if (found != null)
          {
            return found;
          }
        }
        catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    }

    Object textField = Utils.getProperty(element, "TextField");
    if (textField != null
        && UNO.supportsService(textField,
            "com.sun.star.text.TextField.Annotation"))
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
   *          {@code <query results>} Knoten mit mehreren Buttonanpassung-Abschnitten
   *          übergeben wird. Es ist nur wichtig, dass alle ...Tab-Abschnitte auf der
   *          selben Höhe im Baum sind.
   */
  public static ConfigThingy mergeFormDescriptors(List<ConfigThingy> desc,
      ConfigThingy buttonAnpassung, String newTitle)
  {
    if (buttonAnpassung == null)
      buttonAnpassung = new ConfigThingy("Buttonanpassung");
    String plausiColor = null;
    Map<String, ConfigThingy> mapFensterIdToConfigThingy = new HashMap<>();
    Map<String, ConfigThingy> mapSichtbarkeitIdToConfigThingy = new HashMap<>();
    Map<String, ConfigThingy> mapFunktionenIdToConfigThingy = new HashMap<>();
    Map<String, ConfigThingy> mapFunktionsdialogeIdToConfigThingy = new HashMap<>();
    List<String> tabNames = new ArrayList<>();
    for (ConfigThingy conf : desc)
    {
      try
      {
        plausiColor = conf.get("PLAUSI_MARKER_COLOR", 1).toString();
      }
      catch (Exception x)
      {
        LOGGER.trace("", x);
      }

      mergeSection(conf, "Fenster", mapFensterIdToConfigThingy, tabNames, true);
      mergeSection(conf, "Sichtbarkeit", mapSichtbarkeitIdToConfigThingy, null,
          false);
      mergeSection(conf, "Funktionen", mapFunktionenIdToConfigThingy, null,
          false);
      mergeSection(conf, "Funktionsdialoge",
          mapFunktionsdialogeIdToConfigThingy,
          null, false);
    }

    ConfigThingy conf = new ConfigThingy("Formular");
    conf.add("TITLE").add(newTitle);
    if (plausiColor != null)
    {
      conf.add("PLAUSI_MARKER_COLOR").add(plausiColor);
    }

    ConfigThingy subConf = conf.add("Fenster");
    int tabNum = -1;
    if (tabNames.size() > 1)
    {
      tabNum = 0;
    }
    Iterator<String> iter = tabNames.iterator();
    while (iter.hasNext())
    {
      ConfigThingy tabConf = mapFensterIdToConfigThingy.get(iter.next());
      buttonAnpassung(tabNum, tabConf, buttonAnpassung);
      if (++tabNum == tabNames.size() - 1)
      {
        tabNum = Integer.MAX_VALUE;
      }
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
   */
  private static void mergeSection(ConfigThingy conf, String sectionName,
      Map<String, ConfigThingy> mapFensterIdToConfigThingy,
      List<String> tabNames,
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
        if (tabNames != null && !tabNames.contains(name))
        {
          tabNames.add(name);
        }
        if (!duplicatesAllowed && mapFensterIdToConfigThingy.containsKey(name))
          LOGGER.error(L.m(
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
    Set<String> neverActions = new HashSet<>();
    List<ActionUIElementPair> alwaysActions = new ArrayList<>(); // of
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
        if ("NEVER".equals(neverOrAlwaysConf.getName()))
        {
          Iterator<ConfigThingy> neverActionIter = neverOrAlwaysConf.iterator();
          while (neverActionIter.hasNext())
            neverActions.add(neverActionIter.next().toString());
        }
        else if ("ALWAYS".equals(neverOrAlwaysConf.getName()))
        {
          try
          {
            String action = neverOrAlwaysConf.get("ACTION").toString();
            neverOrAlwaysConf.setName("");
            alwaysActions
                .add(new ActionUIElementPair(action, neverOrAlwaysConf));
          }
          catch (NodeNotFoundException x)
          {
            LOGGER.error(
                L.m("Fehlerhafter ALWAYS-Angabe in Buttonanpassung-Abschnitt"),
                x);
          }
        }
      }
    }

    /*
     * Existierende Buttons-Abschnitte durchgehen, ihre Elemente aufsammeln (außer
     * denen, die durch NEVER verboten sind)
     */
    List<ActionUIElementPair> existingUIElements = new ArrayList<>(); // of
    // ActionUIElementPair
    ConfigThingy buttonsConf = tabConf.query("Buttons");

    // durchläuft die Buttons-Abschnitte
    for (ConfigThingy buttonsInner : buttonsConf)
    {
      // durchläuft die Eingabeelemente im Buttons-Abschnitt
      for (ConfigThingy buttonConf : buttonsInner)
      {
        String action = buttonConf.getString("ACTION");

        if (action == null || !neverActions.contains(action))
          existingUIElements.add(new ActionUIElementPair(action, buttonConf));
      }
    }

    // den Buttons-Abschnitt löschen (weil nachher ein neuer generiert wird)
    Iterator<ConfigThingy> iter = tabConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy possiblyButtonsConf = iter.next();
      if ("Buttons".equals(possiblyButtonsConf.getName()))
      {
        iter.remove();
      }
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
    ListIterator<ActionUIElementPair> liter = existingUIElements
        .listIterator(existingUIElements.size());
    while (liter.hasPrevious())
    {
      ActionUIElementPair act = liter.previous();
      String type = act.uiElementDesc.getString("TYPE");

      if (type != null && "glue".equals(type))
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
    public final String action;

    public final ConfigThingy uiElementDesc;

    public ActionUIElementPair(String action, ConfigThingy uiElementDesc)
    {
      this.action = action;
      this.uiElementDesc = uiElementDesc;
    }
  }
}
