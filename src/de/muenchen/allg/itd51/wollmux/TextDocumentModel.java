/*
 * Dateiname: TextDocumentModel.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert ein aktuell geöffnetes TextDokument.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 15.09.2006 | LUT | Erstellung als TextDocumentModel
 * 03.01.2007 | BNK | +collectNonWollMuxFormFields
 * 11.04.2007 | BNK | [R6176]+removeNonWMBookmarks()
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.Window;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XTopWindow;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
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
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.OptionalHighlightColorProvider;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.PrintModels.PrintModelProps;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.dialog.FormController;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.MailMergeNew;
import de.muenchen.allg.itd51.wollmux.func.Values.SimpleMap;

/**
 * Diese Klasse repräsentiert das Modell eines aktuell geöffneten TextDokuments
 * und ermöglicht den Zugriff auf alle interessanten Aspekte des TextDokuments.
 * 
 * @author christoph.lutz
 * 
 */
public class TextDocumentModel
{
  /**
   * Enthält die Referenz auf das XTextDocument-interface des eigentlichen
   * TextDocument-Services der zugehörigen UNO-Komponente.
   */
  public final XTextDocument doc;

  /**
   * Ist true, wenn PrintSettings-Dialog mindestens einmal aufgerufen wurde und
   * false, wenn der Dialog noch nicht aufgerufen wurde.
   */
  public boolean printSettingsDone;

  /**
   * Die dataId unter der die WollMux-Formularbeschreibung in
   * {@link #persistentData} gespeichert wird.
   */
  private static final String DATA_ID_FORMULARBESCHREIBUNG = "WollMuxFormularbeschreibung";

  /**
   * Die dataId unter der die WollMux-Formularwerte in {@link #persistentData}
   * gespeichert werden.
   */
  private static final String DATA_ID_FORMULARWERTE = "WollMuxFormularwerte";

  /**
   * Die dataId unter der die Metadaten der Seriendruckfunktion in
   * {@link #persistentData} gespeichert werden.
   */
  private static final String DATA_ID_SERIENDRUCK = "WollMuxSeriendruck";

  /**
   * Die dataId unter der der Name der Druckfunktion in {@link #persistentData}
   * gespeichert wird.
   */
  private static final String DATA_ID_PRINTFUNCTION = "PrintFunction";

  /**
   * Die dataId unter der der Name der Druckfunktion in {@link #persistentData}
   * gespeichert wird.
   */
  private static final String DATA_ID_SETTYPE = "SetType";

  /**
   * Pattern zum Erkennen der Bookmarks, die {@link #deForm()} entfernen soll.
   */
  private static final Pattern BOOKMARK_KILL_PATTERN = Pattern
      .compile("(\\A\\s*(WM\\s*\\(.*CMD\\s*'((form)|(setGroups)|(insertFormValue))'.*\\))\\s*\\d*\\z)"
               + "|(\\A\\s*(WM\\s*\\(.*CMD\\s*'(setType)'.*'formDocument'\\))\\s*\\d*\\z)"
               + "|(\\A\\s*(WM\\s*\\(.*'formDocument'.*CMD\\s*'(setType)'.*\\))\\s*\\d*\\z)");

  /**
   * Pattern zum Erkennen von WollMux-Bookmarks.
   */
  private static final Pattern WOLLMUX_BOOKMARK_PATTERN = Pattern
      .compile("(\\A\\s*(WM\\s*\\(.*\\))\\s*\\d*\\z)");

  /**
   * Prefix, mit dem die Namen aller automatisch generierten dokumentlokalen
   * Funktionen beginnen.
   */
  private static final String AUTOFUNCTION_PREFIX = "AUTOFUNCTION_";

  /**
   * Prefix "WM(FUNCTION '", mit dem die Namen von Benutzerfelder mit
   * WollMux-Funktionen beginnen.
   */
  public static final String USER_FIELD_NAME_PREFIX = "WM(FUNCTION '";

  /**
   * Ermöglicht den Zugriff auf eine Collection aller FormField-Objekte in
   * diesem TextDokument über den Namen der zugeordneten ID. Die in dieser Map
   * enthaltenen FormFields sind nicht in {@link #idToTextFieldFormFields}
   * enthalten und umgekehrt.
   */
  private HashMap idToFormFields;

  /**
   * Liefert zu einer ID eine {@link java.util.List} von FormField-Objekten, die
   * alle zu Textfeldern ohne ein umschließendes WollMux-Bookmark gehören, aber
   * trotzdem vom WollMux interpretiert werden. Die in dieser Map enthaltenen
   * FormFields sind nicht in {@link #idToFormFields} enthalten und umgekehrt.
   */
  private HashMap idToTextFieldFormFields;

  /**
   * Enthält alle Textfelder ohne ein umschließendes WollMux-Bookmark, die vom
   * WollMux interpretiert werden sollen, aber TRAFO-Funktionen verwenden, die
   * nur einen feste Werte zurückliefern (d.h. keine Parameter erwarten) Die in
   * dieser Map enthaltenen FormFields sind nicht in
   * {@link #idToTextFieldFormFields} enthalten, da sie keine ID besitzen der
   * sie zugeordnet werden können.
   */
  private List staticTextFieldFormFields;

  /**
   * Falls es sich bei dem Dokument um ein Formular handelt, wird das zugehörige
   * FormModel hier gespeichert und beim dispose() des TextDocumentModels mit
   * geschlossen.
   */
  private FormModel formModel;

  /**
   * In diesem Feld wird der CloseListener gespeichert, nachdem die Methode
   * registerCloseListener() aufgerufen wurde.
   */
  private XCloseListener closeListener;

  /**
   * Enthält die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * FormularMax4000.
   */
  private FormularMax4000 currentMax4000;

  /**
   * Enthält die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * MailMergeNew.
   */
  private MailMergeNew currentMM;

  /**
   * Dieses Feld stellt ein Zwischenspeicher für Fragment-Urls dar. Es wird dazu
   * benutzt, im Fall eines openTemplate-Befehls die urls der übergebenen
   * frag_id-Liste temporär zu speichern.
   */
  private String[] fragUrls;

  /**
   * Enthält den Typ des Dokuments oder null, falls keiner gesetzt ist.
   */
  private String type = null;

  /**
   * Enthält die Namen der aktuell gesetzten Druckfunktionen.
   */
  private HashSet printFunctions;

  /**
   * Enthält die Formularbeschreibung des Dokuments oder null, wenn die
   * Formularbeschreibung noch nicht eingelesen wurde.
   */
  private ConfigThingy formularConf;

  /**
   * Enthält die aktuellen Werte der Formularfelder als Zuordnung id -> Wert.
   */
  private HashMap formFieldValues;

  /**
   * Verantwortlich für das Speichern persistenter Daten.
   */
  private PersistentData persistentData;

  /**
   * Enthält die Kommandos dieses Dokuments.
   */
  private DocumentCommands documentCommands;

  /**
   * Enthält ein Setmit den Namen aller derzeit unsichtbar gestellter
   * Sichtbarkeitsgruppen.
   */
  private HashSet /* of String */invisibleGroups;

  /**
   * Der Vorschaumodus ist standardmäßig immer gesetzt - ist dieser Modus nicht
   * gesetzt, so werden in den Formularfeldern des Dokuments nur die Feldnamen
   * in spitzen Klammern angezeigt.
   */
  private boolean formFieldPreviewMode;

  /**
   * Kann über setPartOfMultiformDocument gesetzt werden und sollte dann true
   * enthalten, wenn das Dokument ein Teil eines Multiformdokuments ist.
   */
  private boolean partOfMultiform;

  /**
   * Enthält ein ein Mapping von alten FRAG_IDs fragId auf die jeweils neuen
   * FRAG_IDs newFragId, die über im Dokument enthaltene Dokumentkommando WM(CMD
   * 'overrideFrag' FRAG_ID 'fragId' NEW_FRAG_ID 'newFragId') entstanden sind.
   */
  private HashMap /* of String */overrideFragMap;

  /**
   * Enthält den Kontext für die Funktionsbibliotheken und Dialogbibliotheken
   * dieses Dokuments.
   */
  private HashMap functionContext;

  /**
   * Enthält die Dialogbibliothek mit den globalen und dokumentlokalen
   * Dialogfunktionen oder null, wenn die Dialogbibliothek noch nicht benötigt
   * wurde.
   */
  private DialogLibrary dialogLib;

  /**
   * Enthält die Funktionsbibliothek mit den globalen und dokumentlokalen
   * Funktionen oder null, wenn die Funktionsbilbiothek noch nicht benötigt
   * wurde.
   */
  private FunctionLibrary functionLib;

  /**
   * Enthält null oder ab dem ersten Aufruf von getMailmergeConf() die Metadaten
   * für den Seriendruck in einem ConfigThingy, das derzeit in der Form
   * "Seriendruck(Datenquelle(...))" aufgebaut ist.
   */
  private ConfigThingy mailmergeConf;

  /**
   * Enthält den Controller, der an das Dokumentfenster dieses Dokuments
   * angekoppelte Fenster überwacht und steuert.
   */
  private CoupledWindowController coupledWindowController = null;

  /**
   * Erzeugt ein neues TextDocumentModel zum XTextDocument doc und sollte nie
   * direkt aufgerufen werden, da neue TextDocumentModels über das
   * WollMuxSingleton (siehe WollMuxSingleton.getTextDocumentModel()) erzeugt
   * und verwaltet werden.
   * 
   * @param doc
   */
  public TextDocumentModel(XTextDocument doc)
  {
    this.doc = doc;
    this.idToFormFields = new HashMap();
    this.idToTextFieldFormFields = new HashMap();
    this.staticTextFieldFormFields = new Vector();
    this.fragUrls = new String[] {};
    this.currentMax4000 = null;
    this.closeListener = null;
    this.printFunctions = new HashSet();
    this.printSettingsDone = false;
    this.formularConf = null;
    this.formFieldValues = new HashMap();
    this.invisibleGroups = new HashSet();
    this.overrideFragMap = new HashMap();
    this.functionContext = new HashMap();
    this.formModel = null;
    this.formFieldPreviewMode = true;

    // Kommandobaum erzeugen (modified-Status dabei unberührt lassen):
    boolean modified = getDocumentModified();
    this.documentCommands = new DocumentCommands(UNO.XBookmarksSupplier(doc));
    documentCommands.update();
    setDocumentModified(modified);

    registerCloseListener();

    // WollMuxDispatchInterceptor registrieren
    try
    {
      DispatchHandler.registerDocumentDispatchInterceptor(getFrame());
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Kann DispatchInterceptor nicht registrieren:", e);
    }

    // Auslesen der Persistenten Daten:
    this.persistentData = new PersistentData(doc);
    this.type = persistentData.getData(DATA_ID_SETTYPE);
    parsePrintFunctions(persistentData.getData(DATA_ID_PRINTFUNCTION));
    parseFormValues(persistentData.getData(DATA_ID_FORMULARWERTE));

    // Sicherstellen, dass die Schaltflächen der Symbolleisten aktiviert werden:
    try
    {
      getFrame().contextChanged();
    }
    catch (java.lang.Exception e)
    {
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
   * Erzeugt einen Iterator über alle Sichtbarkeitselemente (Dokumentkommandos
   * und Textbereiche mit dem Namenszusatz 'GROUPS ...'), die in diesem Dokument
   * enthalten sind. Der Iterator liefert dabei zuerst alle Textbereiche (mit
   * GROUPS-Erweiterung) und dann alle Dokumentkommandos des Kommandobaumes in
   * der Reihenfolge, die DocumentCommandTree.depthFirstIterator(false) liefert.
   */
  synchronized public Iterator visibleElementsIterator()
  {
    Vector visibleElements = new Vector();
    for (Iterator iter = documentCommands.setGroupsIterator(); iter.hasNext();)
      visibleElements.add(iter.next());
    return visibleElements.iterator();
  }

  /**
   * Diese Methode wertet den im Dokument enthaltenen PrintFunction-Abschnitt
   * aus, der entweder im ConfigThingy-Format (siehe storePrintFunctions()) oder
   * in dem legacy-Format vorliegen kann, in dem früher der Name genau einer
   * Druckfunktion abgelegt war.
   * 
   * @param data
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void parsePrintFunctions(String data)
  {
    if (data == null || data.length() == 0) return;

    final String errmsg = "Fehler beim Einlesen des Druckfunktionen-Abschnitts '"
                          + data
                          + "':";

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
        WollMuxSingleton.checkIdentifier(data);
        conf = new ConfigThingy("dummy", "WM(Druckfunktionen((FUNCTION '"
                                         + data
                                         + "')))");
      }
      catch (java.lang.Exception forgetMe)
      {
        // Fehlermeldung des SyntaxFehlers ausgeben
        Logger.error(errmsg, e);
      }
    }

    ConfigThingy functions = conf.query("WM").query("Druckfunktionen")
        .queryByChild("FUNCTION");
    for (Iterator iter = functions.iterator(); iter.hasNext();)
    {
      ConfigThingy func = (ConfigThingy) iter.next();
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
   * Parst den String value als ConfigThingy und überträgt alle in diesem
   * enthaltenen Formular-Abschnitte in die übergebene Formularbeschreibung
   * formularConf.
   * 
   * @param formDesc
   *          Wurzelknoten WM einer Formularbeschreibung dem die neuen
   *          Formular-Abschnitte hinzugefügt werden soll.
   * @param value
   *          darf null oder leer sein und wird in diesem Fall ignoriert; value
   *          muss sich fehlerfrei als ConfigThingy parsen lassen, sonst gibt's
   *          eine Fehlermeldung und es wird nichts hinzugefügt.
   */
  private static void addToFormDescription(ConfigThingy formDesc, String value)
  {
    if (value == null || value.length() == 0) return;

    ConfigThingy conf = new ConfigThingy("");
    try
    {
      conf = new ConfigThingy("", null, new StringReader(value));
    }
    catch (java.lang.Exception e)
    {
      Logger.error("Die Formularbeschreibung ist fehlerhaft", e);
      return;
    }

    // enthaltene Formular-Abschnitte übertragen:
    ConfigThingy formulare = conf.query("Formular");
    for (Iterator iter = formulare.iterator(); iter.hasNext();)
    {
      ConfigThingy formular = (ConfigThingy) iter.next();
      formDesc.addChild(formular);
    }
  }

  /**
   * Wertet werteStr aus (das von der Form "WM(FormularWerte(...))" sein muss
   * und überträgt die gefundenen Werte nach formFieldValues.
   * 
   * @param werteStr
   *          darf null sein (und wird dann ignoriert) aber nicht der leere
   *          String.
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
      Logger.error("Formularwerte-Abschnitt ist fehlerhaft", e);
      return;
    }

    // "Formularwerte"-Abschnitt auswerten.
    Iterator iter = werte.iterator();
    while (iter.hasNext())
    {
      ConfigThingy element = (ConfigThingy) iter.next();
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
   * TextDocumentModel alle FormFields bekannt, die beim Durchlauf des
   * FormScanners gefunden wurden.
   * 
   * @param idToFormFields
   */
  synchronized public void setIDToFormFields(HashMap idToFormFields)
  {
    this.idToFormFields = idToFormFields;
  }

  /**
   * Diese Methode bestimmt die Vorbelegung der Formularfelder des Formulars und
   * liefert eine HashMap zurück, die die id eines Formularfeldes auf den
   * bestimmten Wert abbildet. Der Wert ist nur dann klar definiert, wenn alle
   * FormFields zu einer ID unverändert geblieben sind, oder wenn nur
   * untransformierte Felder vorhanden sind, die alle den selben Wert enthalten.
   * Gibt es zu einer ID kein FormField-Objekt, so wird der zuletzt
   * abgespeicherte Wert zu dieser ID aus dem FormDescriptor verwendet. Die
   * Methode sollte erst aufgerufen werden, nachdem dem Model mit
   * setIDToFormFields die verfügbaren Formularfelder bekanntgegeben wurden.
   * 
   * @return eine vollständige Zuordnung von Feld IDs zu den aktuellen
   *         Vorbelegungen im Dokument. TESTED
   */
  synchronized public HashMap getIDToPresetValue()
  {
    HashMap idToPresetValue = new HashMap();

    // durch alle Werte, die im FormDescriptor abgelegt sind gehen, und
    // vergleichen, ob sie mit den Inhalten der Formularfelder im Dokument
    // übereinstimmen.
    Iterator idIter = formFieldValues.keySet().iterator();
    while (idIter.hasNext())
    {
      String id = (String) idIter.next();
      String value;
      String lastStoredValue = (String) formFieldValues.get(id);

      List fields = (List) idToFormFields.get(id);
      if (fields != null && fields.size() > 0)
      {
        boolean allAreUnchanged = true;
        boolean allAreUntransformed = true;
        boolean allUntransformedHaveSameValues = true;

        String refValue = null;

        Iterator j = fields.iterator();
        while (j.hasNext())
        {
          FormField field = (FormField) j.next();
          String thisValue = field.getValue();

          // Wurde der Wert des Feldes gegenüber dem zusetzt gespeicherten Wert
          // verändert?
          String transformedLastStoredValue = getTranformedValue(
              lastStoredValue,
              field.getTrafoName(),
              true);
          if (!thisValue.equals(transformedLastStoredValue))
            allAreUnchanged = false;

          if (field.getTrafoName() != null)
            allAreUntransformed = false;
          else
          {
            // Referenzwert bestimmen
            if (refValue == null) refValue = thisValue;

            if (thisValue == null || !thisValue.equals(refValue))
              allUntransformedHaveSameValues = false;
          }
        }

        // neuen Formularwert bestimmen. Regeln:
        // 1) Wenn sich kein Formularfeld geändert hat, wird der zuletzt
        // gesetzte Formularwert verwendet.
        // 2) Wenn sich mindestens ein Formularfeld geandert hat, jedoch alle
        // untransformiert sind und den selben Wert enhtalten, so wird dieser
        // gleiche Wert als neuer Formularwert übernommen.
        // 3) in allen anderen Fällen wird FISHY übergeben.
        if (allAreUnchanged)
          value = lastStoredValue;
        else
        {
          if (allAreUntransformed
              && allUntransformedHaveSameValues
              && refValue != null)
            value = refValue;
          else
            value = FormController.FISHY;
        }
      }
      else
      {
        // wenn kein Formularfeld vorhanden ist wird der zuletzt gesetzte
        // Formularwert übernommen.
        value = lastStoredValue;
      }

      // neuen Wert übernehmen:
      idToPresetValue.put(id, value);
      Logger.debug2("Add IDToPresetValue: ID=\""
                    + id
                    + "\" --> Wert=\""
                    + value
                    + "\"");

    }
    return idToPresetValue;
  }

  /**
   * Liefert true, wenn das Dokument Serienbrieffelder enthält, ansonsten false.
   */
  synchronized public boolean hasMailMergeFields()
  {
    try
    {
      XEnumeration xenu = UNO.XTextFieldsSupplier(doc).getTextFields()
          .createEnumeration();
      while (xenu.hasMoreElements())
      {
        try
        {
          XDependentTextField tf = UNO.XDependentTextField(xenu.nextElement());

          // Dieser Code wurde früher verwendet um zu erkennen, ob es sich um
          // ein Datenbankfeld handelt. In P1243 ist jedoch ein Crash
          // aufgeführt, der reproduzierbar von diesen Zeilen getriggert wurde:
          // if (tf == null) continue;
          // XPropertySet master = tf.getTextFieldMaster();
          // String column = (String) UNO.getProperty(master, "DataColumnName");
          // if (column != null && column.length() > 0) return true;

          // und hier der Workaround: Wenn es sich um ein Datenbankfeld handelt
          // (das den Service c.s.s.t.TextField.Database implementiert), dann
          // ist auch die Property DataBaseFormat definiert. Es reicht also aus
          // zu testen, ob diese Property definiert ist.
          Object o = UNO.getProperty(tf, "DataBaseFormat");
          if (o != null) return true;
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
    return false;
  }

  /**
   * Sammelt alle Formularfelder des Dokuments auf, die nicht von
   * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden und
   * befüllt werden (derzeit c,s,s,t,textfield,Database-Felder und manche
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
      XEnumeration xenu = UNO.XTextFieldsSupplier(doc).getTextFields()
          .createEnumeration();
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
            FormField f = FormFieldFactory.createInputUserFormField(
                doc,
                tf,
                master);
            Function func = getFunctionLibrary().get(funcName);
            if (func == null)
            {
              Logger.error("Die im Formularfeld verwendete Funktion '"
                           + funcName
                           + "' ist nicht definiert.");
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
                  idToTextFieldFormFields.put(id, new Vector());

                List formFields = (List) idToTextFieldFormFields.get(id);
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
                idToTextFieldFormFields.put(id, new Vector());

              List formFields = (List) idToTextFieldFormFields.get(id);
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
   * gespeicherten Fragment-URLs hier aus, wenn die Dokumentkommandos
   * insertContent ausgeführt werden.
   * 
   * @return
   */
  synchronized public String[] getFragUrls()
  {
    return fragUrls;
  }

  /**
   * Über diese Methode kann der openDocument-Eventhandler die Liste der mit
   * einem insertContent-Kommando zu öffnenden frag-urls speichern.
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
   *           Ersetzungsregel sind, dann entsteht eine Ersetzungskette, die
   *           nicht zugelassen ist.
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

    public String getMessage()
    {
      return "Mit overrideFrag können keine Ersetzungsketten definiert werden, das Fragment '"
             + fragId
             + "' taucht jedoch bereits in einem anderen overrideFrag-Kommando auf.";
    }

  }

  /**
   * Liefert die neue FragId zurück, die anstelle der FRAG_ID fragId verwendet
   * werden soll und durch ein WM(CMD 'overrideFrag'...)-Kommando gesetzt wurde,
   * oder fragId (also sich selbst), wenn keine Überschreibung definiert ist.
   */
  synchronized public String getOverrideFrag(String fragId)
  {
    if (overrideFragMap.containsKey(fragId))
      return overrideFragMap.get(fragId).toString();
    else
      return fragId;
  }

  /**
   * Setzt die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * FormularMax4000.
   * 
   * @param max
   */
  synchronized public void setCurrentFormularMax4000(FormularMax4000 max)
  {
    currentMax4000 = max;
  }

  /**
   * Liefert die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * FormularMax4000 zurück, oder null, falls kein FormularMax gestartet wurde.
   * 
   * @return
   */
  synchronized public FormularMax4000 getCurrentFormularMax4000()
  {
    return currentMax4000;
  }

  /**
   * Setzt die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * MailMergeNew.
   * 
   * @param max
   */
  synchronized public void setCurrentMailMergeNew(MailMergeNew max)
  {
    currentMM = max;
  }

  /**
   * Liefert die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * MailMergeNew zurück, oder null, falls kein FormularMax gestartet wurde.
   * 
   * @return
   */
  synchronized public MailMergeNew getCurrentMailMergeNew()
  {
    return currentMM;
  }

  /**
   * Liefert true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage
   * behandelt werden soll, ansonsten false.
   * 
   * @return true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage
   *         behandelt werden soll, ansonsten false.
   */
  synchronized public boolean isTemplate()
  {
    if (type != null)
    {
      if (type.equalsIgnoreCase("normalTemplate"))
        return true;
      else if (type.equalsIgnoreCase("templateTemplate"))
        return false;
      else if (type.equalsIgnoreCase("formDocument")) return false;
    }

    // Das Dokument ist automatisch eine Vorlage, wenn es keine zugehörige URL
    // gibt (dann steht ja in der Fensterüberschrift auch "Unbenannt1" statt
    // einem konkreten Dokumentnamen).
    return !hasURL();
  }

  /**
   * liefert true, wenn das Dokument eine URL besitzt, die die Quelle des
   * Dokuments beschreibt und es sich damit um ein in OOo im "Bearbeiten"-Modus
   * geöffnetes Dokument handelt oder false, wenn das Dokument keine URL besitzt
   * und es sich damit um eine Vorlage handelt.
   * 
   * @return liefert true, wenn das Dokument eine URL besitzt, die die Quelle
   *         des Dokuments beschreibt und es sich damit um ein in OOo im
   *         "Bearbeiten"-Modus geöffnetes Dokument handelt oder false, wenn das
   *         Dokument keine URL besitzt und es sich damit um eine Vorlage
   *         handelt.
   */
  synchronized public boolean hasURL()
  {
    return doc.getURL() != null && !doc.getURL().equals("");
  }

  /**
   * Liefert true, wenn das Dokument vom Typ formDocument ist ansonsten false.
   * ACHTUNG: Ein Dokument könnte theoretisch mit einem WM(CMD'setType'
   * TYPE'formDocument') Kommandos als Formulardokument markiert seine, OHNE
   * eine gültige Formularbeschreibung zu besitzen. Dies kann mit der Methode
   * hasFormDescriptor() geprüft werden.
   * 
   * @return Liefert true, wenn das Dokument vom Typ formDocument ist ansonsten
   *         false.
   */
  synchronized public boolean isFormDocument()
  {
    return (type != null && type.equalsIgnoreCase("formDocument"));
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
   * Liefert true, wenn das Dokument eine nicht leere Formularbeschreibung mit
   * einem Fenster-Abschnitt enthält. In diesem Fall soll das die FormGUI
   * gestartet werden.
   */
  synchronized public boolean hasFormGUIWindow()
  {
    return getFormDescription().query("Formular").query("Fenster").count() != 0;
  }

  /**
   * Setzt den Typ des Dokuments auf type und speichert den Wert persistent im
   * Dokument ab.
   */
  synchronized public void setType(String type)
  {
    this.type = type;

    // Persistente Daten entsprechend anpassen
    if (type != null)
    {
      persistentData.setData(DATA_ID_SETTYPE, type);
    }
    else
    {
      persistentData.removeData(DATA_ID_SETTYPE);
    }
  }

  /**
   * Wird vom {@link DocumentCommandInterpreter} beim Scannen der
   * Dokumentkommandos aufgerufen wenn ein setType-Dokumentkommando bearbeitet
   * werden muss und setzt den Typ des Dokuments NICHT PERSISTENT auf
   * cmd.getType(), wenn nicht bereits ein type gesetzt ist. Ansonsten wird das
   * Kommando ignoriert.
   */
  synchronized public void setType(DocumentCommand.SetType cmd)
  {
    if (type == null) this.type = cmd.getType();
  }

  /**
   * Diese Methode fügt die Druckfunktion functionName der Menge der dem
   * Dokument zugeordneten Druckfunktionen hinzu. FunctionName muss dabei ein
   * gültiger Funktionsbezeichner sein.
   * 
   * @param functionName
   *          der Name der Druckfunktion, der ein gültiger Funktionsbezeichner
   *          sein und in einem Abschnitt "Druckfunktionen" in der wollmux.conf
   *          definiert sein muss.
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
    {
    }
  }

  /**
   * Löscht die Druckfunktion functionName aus der Menge der dem Dokument
   * zugeordneten Druckfunktionen.
   * 
   * Wird z.B. in den Sachleitenden Verfügungen verwendet, um auf die
   * ursprünglich gesetzte Druckfunktion zurück zu schalten, wenn keine
   * Verfügungspunkte vorhanden sind.
   * 
   * @param functionName
   *          der Name der Druckfunktion, die aus der Menge gelöscht werden
   *          soll.
   */
  synchronized public void removePrintFunction(String functionName)
  {
    printFunctions.remove(functionName);
    storePrintFunctions();

    // Frame veranlassen, die dispatches neu einzulesen - z.B. damit File->Print
    // auch auf gelöschte Druckfunktion reagiert.
    try
    {
      getFrame().contextChanged();
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Schreibt den neuen Zustand der internen HashMap printFunctions in die
   * persistent Data oder löscht den Datenblock, wenn keine Druckfunktion
   * gesetzt ist. Die Druckfunktionen werden in ConfigThingy-Syntax abgelegt und
   * haben den Aufbau WM(Druckfunktionen( ... (FUNCTION 'name' ARG 'arg') ...)).
   * Das Argument ARG ist dabei optional und wird nur gesetzt, wenn ARG nicht
   * leer ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void storePrintFunctions()
  {
    // Elemente nach Namen sortieren (definierte Reihenfolge bei der Ausgabe)
    ArrayList names = new ArrayList(printFunctions);
    Collections.sort(names);

    ConfigThingy wm = new ConfigThingy("WM");
    ConfigThingy druckfunktionen = new ConfigThingy("Druckfunktionen");
    wm.addChild(druckfunktionen);
    for (Iterator iter = names.iterator(); iter.hasNext();)
    {
      String name = (String) iter.next();
      ConfigThingy list = new ConfigThingy("");
      ConfigThingy nameConf = new ConfigThingy("FUNCTION");
      nameConf.addChild(new ConfigThingy(name));
      list.addChild(nameConf);
      druckfunktionen.addChild(list);
    }

    // Persistente Daten entsprechend anpassen
    if (printFunctions.size() > 0)
    {
      persistentData.setData(DATA_ID_PRINTFUNCTION, wm.stringRepresentation());
    }
    else
    {
      persistentData.removeData(DATA_ID_PRINTFUNCTION);
    }
  }

  /**
   * Liefert eine Menge mit den Namen der aktuell gesetzten Druckfunktionen.
   */
  synchronized public Set getPrintFunctions()
  {
    return printFunctions;
  }

  /**
   * Liefert ein HashSet mit den Namen (Strings) aller als unsichtbar markierten
   * Sichtbarkeitsgruppen.
   */
  synchronized public HashSet getInvisibleGroups()
  {
    return invisibleGroups;
  }

  /**
   * Diese Methode setzt die Eigenschaften "Sichtbar" (visible) und die Anzeige
   * der Hintergrundfarbe (showHighlightColor) für alle Druckblöcke eines
   * bestimmten Blocktyps blockName (z.B. allVersions).
   * 
   * @param blockName
   *          Der Blocktyp dessen Druckblöcke behandelt werden sollen.
   * @param visible
   *          Der Block wird sichtbar, wenn visible==true und unsichtbar, wenn
   *          visible==false.
   * @param showHighlightColor
   *          gibt an ob die Hintergrundfarbe angezeigt werden soll (gilt nur,
   *          wenn zu einem betroffenen Druckblock auch eine Hintergrundfarbe
   *          angegeben ist).
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void setPrintBlocksProps(String blockName,
      boolean visible, boolean showHighlightColor)
  {
    Iterator iter = new HashSet().iterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_ALL_VERSIONS.equals(blockName))
      iter = documentCommands.allVersionsIterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_DRAFT_ONLY.equals(blockName))
      iter = documentCommands.draftOnlyIterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_NOT_IN_ORIGINAL.equals(blockName))
      iter = documentCommands.notInOriginalIterator();
    if (SachleitendeVerfuegung.BLOCKNAME_SLV_ORIGINAL_ONLY.equals(blockName))
      iter = documentCommands.originalOnlyIterator();

    while (iter.hasNext())
    {
      DocumentCommand cmd = (DocumentCommand) iter.next();
      cmd.setVisible(visible);
      String highlightColor = ((OptionalHighlightColorProvider) cmd)
          .getHighlightColor();

      if (highlightColor != null)
      {
        if (showHighlightColor)
          try
          {
            Integer bgColor = new Integer(Integer.parseInt(highlightColor, 16));
            UNO.setProperty(cmd.getTextRange(), "CharBackColor", bgColor);
          }
          catch (NumberFormatException e)
          {
            Logger.error("Fehler in Dokumentkommando '"
                         + cmd
                         + "': Die Farbe HIGHLIGHT_COLOR mit dem Wert '"
                         + highlightColor
                         + "' ist ungültig.");
          }
        else
        {
          UNO.setPropertyToDefault(cmd.getTextRange(), "CharBackColor");
        }
      }

    }
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * DraftOnly-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller DraftOnly-Dokumentkommandos
   *         dieses Dokuments ermöglicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   */
  synchronized public Iterator getDraftOnlyBlocksIterator()
  {
    return documentCommands.draftOnlyIterator();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * All-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller All-Dokumentkommandos dieses
   *         Dokuments ermöglicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   */
  synchronized public Iterator getAllVersionsBlocksIterator()
  {
    return documentCommands.allVersionsIterator();
  }

  /**
   * Liefert eine SetJumpMark zurück, der das erste
   * setJumpMark-Dokumentkommandos dieses Dokuments enthält oder null falls kein
   * solches Dokumentkommando vorhanden ist.
   * 
   * @return Liefert eine SetJumpMark zurück, der das erste
   *         setJumpMark-Dokumentkommandos dieses Dokuments enthält oder null
   *         falls kein solches Dokumentkommando vorhanden ist.
   */
  synchronized public SetJumpMark getFirstJumpMark()
  {
    return documentCommands.getFirstJumpMark();
  }

  /**
   * Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein
   * Controller (oder auch kein ViewCursor) für das Dokument verfügbar ist.
   * 
   * @return Liefert den ViewCursor des aktuellen Dokuments oder null, wenn kein
   *         Controller (oder auch kein ViewCursor) für das Dokument verfügbar
   *         ist.
   */
  synchronized public XTextCursor getViewCursor()
  {
    if (UNO.XModel(doc) == null) return null;
    XTextViewCursorSupplier suppl = UNO.XTextViewCursorSupplier(UNO.XModel(doc)
        .getCurrentController());
    if (suppl != null) return suppl.getViewCursor();
    return null;
  }

  /**
   * Entfernt alle Bookmarks, die keine WollMux-Bookmarks sind aus dem Dokument
   * doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  synchronized public void removeNonWMBookmarks()
  {
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
   * formDocument" und "form", sowie die WollMux-Formularbeschreibung und Daten
   * aus dem Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  synchronized public void deForm()
  {
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

    persistentData.removeData(DATA_ID_FORMULARBESCHREIBUNG);
    persistentData.removeData(DATA_ID_FORMULARWERTE);
  }

  /**
   * Fügt an Stelle der aktuellen Selektion ein Serienbrieffeld ein, das auf die
   * Spalte fieldId zugreift und mit dem Wert "" vorbelegt ist, falls noch kein
   * Wert für fieldId gesetzt wurde. Das Serienbrieffeld wird im WollMux
   * registriert und kann damit sofort verwendet werden.
   */
  synchronized public void insertMailMergeFieldAtCursorPosition(String fieldId)
  {
    if (fieldId.length() > 0)
      try
      {
        // Feld einfügen
        XMultiServiceFactory factory = UNO.XMultiServiceFactory(doc);
        XDependentTextField field = UNO.XDependentTextField(factory
            .createInstance("com.sun.star.text.TextField.Database"));
        XPropertySet master = UNO.XPropertySet(factory
            .createInstance("com.sun.star.text.FieldMaster.Database"));
        UNO.setProperty(master, "DataBaseName", "DataBase");
        UNO.setProperty(master, "DataTableName", "Table");
        UNO.setProperty(master, "DataColumnName", fieldId);
        if (!formFieldPreviewMode)
          UNO.setProperty(field, "Content", "<" + fieldId + ">");
        field.attachTextFieldMaster(master);

        XTextCursor vc = getViewCursor();
        vc.getText().insertTextContent(vc, field, true);
        vc.collapseToEnd();

        // Feldwert mit leerem Inhalt vorbelegen
        if (!formFieldValues.containsKey(fieldId))
          setFormFieldValue(fieldId, "");

        // Formularfeld bekanntmachen, damit es vom WollMux verwendet wird.
        if (!idToTextFieldFormFields.containsKey(fieldId))
          idToTextFieldFormFields.put(fieldId, new Vector());
        List formFields = (List) idToTextFieldFormFields.get(fieldId);
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
   * Liefert die aktuelle Formularbeschreibung des Dokuments; Wurde die
   * Formularbeschreibung bis jetzt noch nicht eingelesen, so wird sie
   * spätestens jetzt eingelesen.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  synchronized public ConfigThingy getFormDescription()
  {
    if (formularConf == null)
    {
      Logger.debug("Einlesen der Formularbeschreibung von " + this);
      formularConf = new ConfigThingy("WM");
      addToFormDescription(formularConf, persistentData
          .getData(DATA_ID_FORMULARBESCHREIBUNG));
    }

    return formularConf;
  }

  /**
   * Liefert den Seriendruck-Knoten der im Dokument gespeicherten
   * Seriendruck-Metadaten zurück. Die Metadaten liegen im Dokument
   * beispielsweise in der Form "WM(Seriendruck(Datenquelle(...)))" vor - diese
   * Methode liefert aber nur der Knoten "Seriendruck" zurück. Enthält das
   * Dokument keine Seriendruck-Metadaten, so liefert diese Methode einen leeren
   * "Seriendruck"-Knoten zurück.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  synchronized public ConfigThingy getMailmergeConfig()
  {
    if (mailmergeConf == null)
    {
      String data = persistentData.getData(DATA_ID_SERIENDRUCK);
      mailmergeConf = new ConfigThingy("Seriendruck");
      if (data != null)
        try
        {
          mailmergeConf = new ConfigThingy("", data).query("WM").query(
              "Seriendruck").getLastChild();
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
    }
    return mailmergeConf;
  }

  /**
   * Diese Methode speichert die als Kinder von conf übergebenen Metadaten für
   * den Seriendruck persistent im Dokument oder löscht die Metadaten aus dem
   * Dokument, wenn conf keine Kinder besitzt. conf kann dabei ein beliebig
   * benannter Konten sein, dessen Kinder müssen aber gültige Schlüssel des
   * Abschnitts WM(Seriendruck(...) darstellen. So ist z.B. "Datenquelle" ein
   * gültiger Kindknoten von conf.
   * 
   * @param conf
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  synchronized public void setMailmergeConfig(ConfigThingy conf)
  {
    mailmergeConf = new ConfigThingy("Seriendruck");
    for (Iterator iter = conf.iterator(); iter.hasNext();)
    {
      ConfigThingy c = new ConfigThingy((ConfigThingy) iter.next());
      mailmergeConf.addChild(c);
    }
    ConfigThingy wm = new ConfigThingy("WM");
    wm.addChild(mailmergeConf);
    if (mailmergeConf.count() > 0)
      persistentData.setData(DATA_ID_SERIENDRUCK, wm.stringRepresentation());
    else
      persistentData.removeData(DATA_ID_SERIENDRUCK);
  }

  /**
   * Liefert einen Funktionen-Abschnitt der Formularbeschreibung, in dem die
   * lokalen Auto-Funktionen abgelegt werden können. Besitzt die
   * Formularbeschreibung keinen Funktionen-Abschnitt, so wird der
   * Funktionen-Abschnitt und ggf. auch ein übergeordneter Formular-Abschnitt
   * neu erzeugt.
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
   * Dokuments oder löscht den entsprechenden Abschnitt aus den persistenten
   * Daten, wenn die Formularbeschreibung nur aus einer leeren Struktur ohne
   * eigentlichen Formularinhalt besteht.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void storeCurrentFormDescription()
  {
    ConfigThingy conf = getFormDescription();
    try
    {
      if ((conf.query("Fenster").count() > 0 && conf.get("Fenster").count() > 0)
          || (conf.query("Sichtbarkeit").count() > 0 && conf
              .get("Sichtbarkeit").count() > 0)
          || (conf.query("Funktionen").count() > 0 && conf.get("Funktionen")
              .count() > 0))
        persistentData.setData(DATA_ID_FORMULARBESCHREIBUNG, conf
            .stringRepresentation());
      else
        persistentData.removeData(DATA_ID_FORMULARBESCHREIBUNG);
    }
    catch (NodeNotFoundException e)
    {
      Logger.error("Dies kann nicht passieren.", e);
    }
  }

  /**
   * Ersetzt die Formularbeschreibung dieses Dokuments durch die aus conf. Falls
   * conf == null, so wird die Formularbeschreibung gelöscht. ACHTUNG! conf wird
   * nicht kopiert sondern als Referenz eingebunden.
   * 
   * @param conf
   *          ein WM-Knoten, der "Formular"-Kinder hat. Falls conf == null, so
   *          wird die Formularbeschreibungsnotiz gelöscht.
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
   * Formularwerte-Abschnitt in den persistenten Daten.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  synchronized public void setFormFieldValue(String fieldId, String value)
  {
    formFieldValues.put(fieldId, value);
    persistentData.setData(DATA_ID_FORMULARWERTE, getFormFieldValues());
  }

  /**
   * Serialisiert die aktuellen Werte aller Fomularfelder.
   */
  private String getFormFieldValues()
  {
    // Neues ConfigThingy für "Formularwerte"-Abschnitt erzeugen:
    ConfigThingy werte = new ConfigThingy("WM");
    ConfigThingy formwerte = new ConfigThingy("Formularwerte");
    werte.addChild(formwerte);
    Iterator iter = formFieldValues.keySet().iterator();
    while (iter.hasNext())
    {
      String key = (String) iter.next();
      String value = (String) formFieldValues.get(key);
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
   * Liefert den Kontext mit dem die dokumentlokalen Dokumentfunktionen beim
   * Aufruf von getFunctionLibrary() und getDialogLibrary() erzeugt werden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public Map getFunctionContext()
  {
    return functionContext;
  }

  /**
   * Liefert die Funktionsbibliothek mit den globalen Funktionen des WollMux und
   * den lokalen Funktionen dieses Dokuments.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public FunctionLibrary getFunctionLibrary()
  {
    if (functionLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = getFormDescription().get("Formular");
      }
      catch (NodeNotFoundException e)
      {
      }
      functionLib = WollMuxFiles.parseFunctions(
          formConf,
          getDialogLibrary(),
          functionContext,
          WollMuxSingleton.getInstance().getGlobalFunctions());
    }
    return functionLib;
  }

  /**
   * Liefert die eine Bibliothek mit den globalen Dialogfunktionen des WollMux
   * und den lokalen Dialogfunktionen dieses Dokuments.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public DialogLibrary getDialogLibrary()
  {
    if (dialogLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = getFormDescription().get("Formular");
      }
      catch (NodeNotFoundException e)
      {
      }
      dialogLib = WollMuxFiles.parseFunctionDialogs(formConf, WollMuxSingleton
          .getInstance().getFunctionDialogs(), functionContext);
    }
    return dialogLib;
  }

  /**
   * Erzeugt in der Funktionsbeschreibung eine neue Funktion mit einem
   * automatisch generierten Namen, registriert sie in der Funktionsbibliothek,
   * so dass diese sofort z.B. als TRAFO-Funktion genutzt werden kann und
   * liefert den neuen generierten Funktionsnamen zurück oder null, wenn
   * funcConf fehlerhaft ist.
   * 
   * Der automatisch generierte Name ist, nach dem Prinzip
   * PRAEFIX_aktuelleZeitinMillisekunden_zahl aufgebaut. Es wird aber in jedem
   * Fall garantiert, dass der neue Name eindeutig ist und nicht bereits in der
   * Funktionsbibliothek vorkommt.
   * 
   * @param funcConf
   *          Ein ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION
   *          )", wobei Bezeichner ein beliebiger Bezeichner ist und
   *          FUNKTIONSDEFINITION ein erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)},
   *          d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine
   *          erlaubter Funktionsname, z.B. "AND" sein. Der Bezeichner wird
   *          NICHT als Name der TRAFO verwendet. Stattdessen wird ein neuer
   *          eindeutiger TRAFO-Name generiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private String addLocalAutofunction(ConfigThingy funcConf)
  {
    FunctionLibrary funcLib = getFunctionLibrary();
    DialogLibrary dialogLib = getDialogLibrary();
    Map context = getFunctionContext();

    // eindeutigen Namen für die neue Autofunktion erzeugen:
    Set currentFunctionNames = funcLib.getFunctionNames();
    String name = null;
    for (int i = 0; name == null || currentFunctionNames.contains(name); ++i)
      name = AUTOFUNCTION_PREFIX + System.currentTimeMillis() + "_" + i;

    try
    {
      funcLib.add(name, FunctionFactory.parseChildren(
          funcConf,
          funcLib,
          dialogLib,
          context));

      // Funktion zur Formularbeschreibung hinzufügen:
      ConfigThingy betterNameFunc = new ConfigThingy(name);
      for (Iterator iter = funcConf.iterator(); iter.hasNext();)
      {
        ConfigThingy func = (ConfigThingy) iter.next();
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
   * Im Vorschaumodus überträgt diese Methode den Formularwert zum Feldes
   * fieldId aus dem persistenten Formularwerte-Abschnitt in die zugehörigen
   * Formularfelder im Dokument; Ist der Vorschaumodus nicht aktiv, so werden
   * jeweils nur die Spaltennamen in spitzen Klammern angezeigt; Für die
   * Auflösung der TRAFOs wird dabei die Funktionsbibliothek funcLib verwendet.
   * 
   * @param fieldId
   *          Die ID des Formularfeldes bzw. der Formularfelder, die im Dokument
   *          angepasst werden sollen.
   */
  synchronized public void updateFormFields(String fieldId)
  {
    if (formFieldPreviewMode)
    {
      String value = (String) formFieldValues.get(fieldId);
      if (value != null) setFormFields(fieldId, value, true);
    }
    else
    {
      setFormFields(fieldId, "<" + fieldId + ">", false);
    }
  }

  /**
   * Im Vorschaumodus überträgt diese Methode alle Formularwerte aus dem
   * Formularwerte-Abschnitt der persistenten Daten in die zugehörigen
   * Formularfelder im Dokument, wobei evtl. gesetzte Trafo-Funktionen
   * ausgeführt werden; Ist der Vorschaumodus nicht aktiv, so werden jeweils nur
   * die Spaltennamen in spitzen Klammern angezeigt.
   */
  private void updateAllFormFields()
  {
    for (Iterator iter = formFieldValues.keySet().iterator(); iter.hasNext();)
    {
      String fieldId = (String) iter.next();
      updateFormFields(fieldId);
    }
  }

  /**
   * Macht das selbe wie updateAllFormFields, allerdings werden nur die
   * Formularfelder aktualisiert, die die Trafo trafoName gesetzt haben.
   */
  private void updateAllFormFieldsWithTrafo(String trafoName)
  {
    updateAllFormFields();
    // TODO: Implementieren der Funktion zur Optimierung falls
    // Performance-Probleme auftreten.
  }

  /**
   * Setzt den Inhalt aller Formularfelder mit ID fieldId auf value.
   * 
   * @param applyTrafo
   *          gibt an, ob eine evtl. vorhandene TRAFO-Funktion angewendet werden
   *          soll (true) oder nicht (false).
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  private void setFormFields(String fieldId, String value, boolean applyTrafo)
  {
    setFormFields((List) idToFormFields.get(fieldId), value, applyTrafo, false);
    setFormFields(
        (List) idToTextFieldFormFields.get(fieldId),
        value,
        applyTrafo,
        true);
    setFormFields(staticTextFieldFormFields, value, applyTrafo, true);
  }

  /**
   * Setzt den Inhalt aller Formularfelder aus der Liste formFields auf value.
   * formFields kann null sein, dann passiert nichts.
   * 
   * @param funcLib
   *          Funktionsbibliothek zum Berechnen von TRAFOs. funcLib darf null
   *          sein, dann werden die Formularwerte in jedem Fall untransformiert
   *          gesetzt.
   * @param applyTrafo
   *          gibt an ob eine evtl. vorhandenen Trafofunktion verwendet werden
   *          soll.
   * @param useKnownFormValues
   *          gibt an, ob die Trafofunktion mit den bekannten Formularwerten
   *          (true) als Parameter, oder ob alle erwarteten Parameter mit dem
   *          Wert value (false) versorgt werden - wird aus Gründen der
   *          Abwärtskompatiblität zu den bisherigen insertFormValue-Kommandos
   *          benötigt.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1)
   */
  private void setFormFields(List formFields, String value, boolean applyTrafo,
      boolean useKnownFormValues)
  {
    if (formFields == null) return;
    Iterator fields = formFields.iterator();
    while (fields.hasNext())
    {
      FormField field = (FormField) fields.next();
      try
      {
        if (applyTrafo)
        {
          String trafoName = field.getTrafoName();
          field.setValue(getTranformedValue(
              value,
              trafoName,
              useKnownFormValues));
        }
        else
          field.setValue(value);
      }
      catch (RuntimeException e)
      {
        // Absicherung gegen das manuelle Löschen von Dokumentinhalten.
      }
    }
  }

  /**
   * Schaltet den Vorschaumodus für Formularfelder an oder aus - ist der
   * Vorschaumodus aktiviert, so werden alle Formularfelder mit den zuvor
   * gesetzten Formularwerten angezeigt, ist der Preview-Modus nicht aktiv, so
   * werden nur die Spaltennamen in spitzen Klammern angezeigt.
   * 
   * @param previewMode
   *          true schaltet den Modus an, false schaltet auf den Vorschaumodus
   *          zurück in dem die aktuell gesetzten Werte wieder angezeigt werden.
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
    List formFields = (List) idToTextFieldFormFields.get(fieldId);
    if (formFields != null)
    {
      field = (FormField) formFields.get(0);
    }
    else
    {
      formFields = (List) idToFormFields.get(fieldId);
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
   * nicht-transformiertes Feld vorhanden ist, so wird das erste
   * nicht-transformierte Feld zurückgegeben, ansonsten wird das erste
   * transformierte Feld zurückgegeben, oder null, falls die Liste keine
   * Elemente enthält bzw. null ist.
   * 
   * @param formFields
   *          Liste mit FormField-Elementen
   * @return Ein FormField Element, wobei untransformierte Felder bevorzugt
   *         werden.
   */
  protected static FormField preferUntransformedFormField(List formFields)
  {
    if (formFields == null) return null;
    Iterator iter = formFields.iterator();
    FormField field = null;
    while (iter.hasNext())
    {
      FormField f = (FormField) iter.next();
      if (field == null) field = f;
      if (f.getTrafoName() == null) return f;
    }
    return field;
  }

  /**
   * Diese Methode berechnet die Transformation des Wertes value mit der
   * Trafofunktion trafoName, die global oder dokumentlokal definiert sein muss;
   * dabei steuert useKnownFormValues, ob der Trafo die Menge aller bekannten
   * Formularwerte als parameter übergeben wird, oder ob aus Gründen der
   * Abwärtskompatiblilität jeder durch die Trafofunktion gelesene Parameter den
   * selben Wert value übergeben bekommt. Ist trafoName==null, so wird value
   * zurückgegeben. Ist die Transformationsionfunktion nicht in der globalen
   * oder dokumentlokalen Funktionsbibliothek enthalten, so wird eine
   * Fehlermeldung zurückgeliefert und eine weitere Fehlermeldung in die
   * Log-Datei geschrieben.
   * 
   * @param value
   *          Der zu transformierende Wert.
   * @param trafoName
   *          Der Name der Trafofunktion, der auch null sein darf.
   * @param useKnownFormValues
   *          steuert, ob der Trafo die Menge aller bekannten Formularwerte als
   *          parameter übergeben wird, oder ob aus Gründen der
   *          Abwärtskompatiblilität jeder durch die Trafofunktion gelesene
   *          Parameter den selben Wert value übergeben bekommt.
   * @return Der transformierte Wert falls das trafoName gesetzt ist und die
   *         Trafo korrekt definiert ist. Ist trafoName==null, so wird value
   *         unverändert zurückgeliefert. Ist die Funktion trafoName nicht
   *         definiert, wird eine Fehlermeldung zurückgeliefert. TESTED
   */
  public String getTranformedValue(String value, String trafoName,
      boolean useKnownFormValues)
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
        {
          if (useKnownFormValues)
            args.put(pars[i], (String) formFieldValues.get(pars[i]));
          else
            args.put(pars[i], value);
        }
        transformed = func.getString(args);
      }
      else
      {
        transformed = "<FEHLER: TRAFO '" + trafoName + "' nicht definiert>";
        Logger.error("Die TRAFO '" + trafoName + "' ist nicht definiert.");
      }
    }

    return transformed;
  }

  /**
   * Liefert die zu diesem Dokument zugehörige FormularGUI, falls dem
   * TextDocumentModel die Existent einer FormGUI über setFormGUI(...)
   * mitgeteilt wurde - andernfalls wird null zurück geliefert.
   * 
   * @return Die FormularGUI des Formulardokuments oder null
   */
  synchronized public FormModel getFormModel()
  {
    return formModel;
  }

  /**
   * Gibt dem TextDocumentModel die Existent der FormularGUI formGUI bekannt und
   * wird vom DocumentCommandInterpreter in der Methode processFormCommands()
   * gestartet hat, falls das Dokument ein Formulardokument ist.
   * 
   * @param formGUI
   *          Die zu diesem Dokument zugehörige formGUI
   */
  synchronized public void setFormModel(FormModel formModel)
  {
    this.formModel = formModel;
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
   * Liefert die Gesamtseitenzahl des Dokuments oder 0, wenn die Seitenzahl
   * nicht bestimmt werden kann.
   * 
   * @return Liefert die Gesamtseitenzahl des Dokuments oder 0, wenn die
   *         Seitenzahl nicht bestimmt werden kann.
   */
  synchronized public int getPageCount()
  {
    try
    {
      return (int) AnyConverter.toLong(UNO.getProperty(doc
          .getCurrentController(), "PageCount"));
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
   * Liefert true, wenn das Dokument als "modifiziert" markiert ist und damit
   * z.B. die "Speichern?" Abfrage vor dem Schließen erscheint.
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
    {
    }
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
      if (WollMuxSingleton.getInstance().isDebugMode() == false
          && UNO.XModel(doc) != null)
      {
        if (lock)
          UNO.XModel(doc).lockControllers();
        else
          UNO.XModel(doc).unlockControllers();
      }
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Setzt die Position des Fensters auf die übergebenen Koordinaten, wobei die
   * Nachteile der UNO-Methode setWindowPosSize greifen, bei der die
   * Fensterposition nicht mit dem äusseren Fensterrahmen beginnt, sondern mit
   * der grauen Ecke links über dem File-Menü.
   * 
   * @param docX
   * @param docY
   * @param docWidth
   * @param docHeight
   */
  synchronized public void setWindowPosSize(int docX, int docY, int docWidth,
      int docHeight)
  {
    try
    {
      getFrame().getContainerWindow().setPosSize(
          docX,
          docY,
          docWidth,
          docHeight,
          PosSize.POSSIZE);
    }
    catch (java.lang.Exception e)
    {
    }
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht
   * des Dokuments auf den neuen Wert zoom, der entwender eine ganzzahliger
   * Prozentwert (ohne "%"-Zeichen") oder einer der Werte "Optimal",
   * "PageWidth", "PageWidthExact" oder "EntirePage" ist.
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
        zoomType = new Short(DocumentZoomType.OPTIMAL);

      if (zoom.equalsIgnoreCase("PageWidth"))
        zoomType = new Short(DocumentZoomType.PAGE_WIDTH);

      if (zoom.equalsIgnoreCase("PageWidthExact"))
        zoomType = new Short(DocumentZoomType.PAGE_WIDTH_EXACT);

      if (zoom.equalsIgnoreCase("EntirePage"))
        zoomType = new Short(DocumentZoomType.ENTIRE_PAGE);

      if (zoomType == null)
      {
        try
        {
          zoomValue = new Short(zoom);
        }
        catch (NumberFormatException e)
        {
        }
      }
    }

    // ZoomType bzw ZoomValue setzen:
    Object viewSettings = null;
    try
    {
      viewSettings = UNO.XViewSettingsSupplier(doc.getCurrentController())
          .getViewSettings();
    }
    catch (java.lang.Exception e)
    {
    }
    if (zoomType != null)
      UNO.setProperty(viewSettings, "ZoomType", zoomType);
    else if (zoomValue != null)
      UNO.setProperty(viewSettings, "ZoomValue", zoomValue);
    else
      throw new ConfigurationErrorException("Ungültiger ZOOM-Wert '"
                                            + zoom
                                            + "'");
  }

  /**
   * Diese Methode liest die (optionalen) Attribute X, Y, WIDTH, HEIGHT und ZOOM
   * aus dem übergebenen Konfigurations-Abschnitt settings und setzt die
   * Fenstereinstellungen des Dokuments entsprechend um. Bei den Pärchen X/Y
   * bzw. SIZE/WIDTH müssen jeweils beide Komponenten im Konfigurationsabschnitt
   * angegeben sein.
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
    {
    }

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
      int xPos = new Integer(settings.get("X").toString()).intValue();
      int yPos = new Integer(settings.get("Y").toString()).intValue();
      if (window != null)
      {
        window.setPosSize(xPos + insetLeft, yPos + insetTop, 0, 0, PosSize.POS);
      }
    }
    catch (java.lang.Exception e)
    {
    }
    // Dimensions setzen:
    try
    {
      int width = new Integer(settings.get("WIDTH").toString()).intValue();
      int height = new Integer(settings.get("HEIGHT").toString()).intValue();
      if (window != null)
        window.setPosSize(
            0,
            0,
            width - insetLeft - insetRight,
            height - insetTop - insetButtom,
            PosSize.SIZE);
    }
    catch (java.lang.Exception e)
    {
    }

    // Zoom setzen:
    setDocumentZoom(settings);
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht
   * des Dokuments auf den neuen Wert den das ConfigThingy conf im Knoten ZOOM
   * angibt, der entwender eine ganzzahliger Prozentwert (ohne "%"-Zeichen")
   * oder einer der Werte "Optimal", "PageWidth", "PageWidthExact" oder
   * "EntirePage" ist.
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
   * Die Methode fügt die Formular-Abschnitte aus der Formularbeschreibung der
   * Notiz von formCmd zur aktuellen Formularbeschreibung des Dokuments in den
   * persistenten Daten hinzu und löscht die Notiz.
   * 
   * @param formCmd
   *          Das formCmd, das die Notzi mit den hinzuzufügenden
   *          Formular-Abschnitten einer Formularbeschreibung enthält.
   * @throws ConfigurationErrorException
   *           Die Notiz der Formularbeschreibung ist nicht vorhanden, die
   *           Formularbeschreibung ist nicht vollständig oder kann nicht
   *           geparst werden.
   */
  synchronized public void addToCurrentFormDescription(
      DocumentCommand.Form formCmd) throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextRange();

    XTextContent annotationField = UNO.XTextContent(WollMuxSingleton
        .findAnnotationFieldRecursive(range));
    if (annotationField == null)
      throw new ConfigurationErrorException(
          "Die zugehörige Notiz mit der Formularbeschreibung fehlt.");

    Object content = UNO.getProperty(annotationField, "Content");
    if (content == null)
      throw new ConfigurationErrorException(
          "Die zugehörige Notiz mit der Formularbeschreibung kann nicht gelesen werden.");

    // Formularbeschreibung übernehmen und persistent speichern:
    addToFormDescription(getFormDescription(), content.toString());
    storeCurrentFormDescription();

    // Notiz löschen
    try
    {
      range.getText().removeTextContent(annotationField);
    }
    catch (NoSuchElementException e)
    {
      Logger.error(e);
    }
  }

  /**
   * Druckt den über pageRangeType/pageRangeValue spezifizierten Bereich des
   * Dokuments in der Anzahl numberOfCopies auf dem aktuell eingestellten
   * Drucker aus.
   * 
   * @param numberOfCopies
   *          Bestimmt die Anzahl der Kopien
   * @param pageRangeType
   *          Legt den Typ des Druckbereichs fest und enthält einen der Werte
   *          PrintModels.PrintModelProps.PAGE_RANGE_TYPE_*.
   * @param pageRangeValue
   *          wird in Verbindung mit dem pageRangeType PAGE_RANGE_TYPE_MANUAL
   *          zwingend benötigt und enthält den zu druckenden Bereich als
   *          String.
   * @throws PrintFailedException
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void printWithPageRange(short numberOfCopies,
      short pageRangeType, String pageRangeValue) throws PrintFailedException
  {
    HashMap props = new HashMap();
    props.put(PrintModelProps.PROP_PAGE_RANGE_TYPE, new Short(pageRangeType));
    props.put(PrintModelProps.PROP_PAGE_RANGE_VALUE, pageRangeValue);
    props.put(PrintModelProps.PROP_COPY_COUNT, new Short(numberOfCopies));
    printWithProps(props);
  }

  /**
   * Druckt das Dokument auf dem aktuell eingestellten Drucker aus, wobei die in
   * props übergebenen Properties CopyCount, Pages, PageRangeType und
   * PageRangeValue ausgewertet werden, wenn sie vorhanden sind.
   * 
   * @param props
   *          HashMap mit Properties aus
   *          {@see de.muenchen.allg.itd51.wollmux.PrintModels.PrintModelProps},
   *          die ausgewertet werden sollen.
   * @throws PrintFailedException
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void printWithProps(HashMap props)
      throws PrintFailedException
  {
    try
    {
      if (props == null) props = new HashMap();
      UnoProps myProps = new UnoProps("Wait", Boolean.TRUE);

      // Property "CopyCount" bestimmen:
      if (props.containsKey(PrintModelProps.PROP_COPY_COUNT))
        myProps.setPropertyValue("CopyCount", props
            .get(PrintModelProps.PROP_COPY_COUNT));

      // Property "Pages" bestimmen:
      if (props.containsKey(PrintModelProps.PROP_PAGES))
        myProps
            .setPropertyValue("Pages", props.get(PrintModelProps.PROP_PAGES));
      else if (props.containsKey(PrintModelProps.PROP_PAGE_RANGE_TYPE))
      {
        // pr mit aktueller Seite vorbelegen (oder 1 als fallback)
        String pr = "1";
        if (UNO.XPageCursor(getViewCursor()) != null)
          pr = "" + UNO.XPageCursor(getViewCursor()).getPage();

        short pageRangeType = ((Short) props
            .get(PrintModelProps.PROP_PAGE_RANGE_TYPE)).shortValue();
        String pageRangeValue = null;
        if (props.containsKey(PrintModelProps.PROP_PAGE_RANGE_VALUE))
          pageRangeValue = ""
                           + props.get(PrintModelProps.PROP_PAGE_RANGE_VALUE);

        if (pageRangeType == PrintModelProps.PAGE_RANGE_TYPE_CURRENT)
          myProps.setPropertyValue("Pages", pr);
        else if (pageRangeType == PrintModelProps.PAGE_RANGE_TYPE_CURRENTFF)
          myProps.setPropertyValue("Pages", pr + "-" + getPageCount());
        else if (pageRangeType == PrintModelProps.PAGE_RANGE_TYPE_MANUAL
                 && pageRangeValue != null)
          myProps.setPropertyValue("Pages", pageRangeValue);
      }

      // Drucken:
      if (UNO.XPrintable(doc) != null)
        UNO.XPrintable(doc).print(myProps.getProps());
    }
    catch (java.lang.Exception e)
    {
      throw new PrintFailedException(e);
    }
  }

  /**
   * Das Drucken des Dokuments hat aus irgend einem Grund nicht funktioniert.
   * 
   * @author christoph.lutz
   */
  public static class PrintFailedException extends Exception
  {
    private static final long serialVersionUID = 1L;

    PrintFailedException(Exception e)
    {
      super("Das Drucken des Dokuments schlug fehl: ", e);
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
  synchronized public void close()
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
      {
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
          c.suspend(false);
      }

    }
  }

  /**
   * Ruft die Dispose-Methoden von allen aktiven, dem TextDocumentModel
   * zugeordneten Dialogen auf und gibt den Speicher des TextDocumentModels
   * frei.
   */
  synchronized public void dispose()
  {
    if (currentMax4000 != null) currentMax4000.dispose();
    currentMax4000 = null;

    if (currentMM != null) currentMM.dispose();
    currentMM = null;

    if (formModel != null) formModel.disposing(this);
    formModel = null;

    // Löscht das TextDocumentModel von doc aus dem WollMux-Singleton.
    WollMuxSingleton.getInstance().disposedTextDocument(doc);
  }

  /**
   * Liefert den Titel des Dokuments, wie er im Fenster des Dokuments angezeigt
   * wird, ohne den Zusatz " - OpenOffice.org Writer" oder "NoTitle", wenn der
   * Titel nicht bestimmt werden kann. TextDocumentModel('<title>')
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
    {
    }
    return title;
  }

  /**
   * Liefert eine Stringrepräsentation des TextDocumentModels - Derzeit in der
   * Form 'doc(<title>)'.
   * 
   * @see java.lang.Object#toString()
   */
  synchronized public String toString()
  {
    return "doc('" + getTitle() + "')";
  }

  /**
   * Registriert genau einen XCloseListener in der Komponente des
   * XTextDocuments, so dass beim Schließen des Dokuments die entsprechenden
   * WollMuxEvents ausgeführt werden - ist in diesem TextDocumentModel bereits
   * ein XCloseListener registriert, so wird nichts getan.
   */
  private void registerCloseListener()
  {
    if (closeListener == null && UNO.XCloseable(doc) != null)
    {
      closeListener = new XCloseListener()
      {
        public void disposing(EventObject arg0)
        {
          WollMuxEventHandler.handleTextDocumentClosed(doc);
        }

        public void notifyClosing(EventObject arg0)
        {
          WollMuxEventHandler.handleTextDocumentClosed(doc);
        }

        public void queryClosing(EventObject arg0, boolean arg1)
            throws CloseVetoException
        {
        }
      };
      UNO.XCloseable(doc).addCloseListener(closeListener);
    }
  }

  /**
   * Liefert ein neues zu diesem TextDocumentModel zugehörige XPrintModel für
   * einen Druckvorgang; ist useDocumentPrintFunctions==true, so werden bereits
   * alle im Dokument gesetzten Druckfunktionen per
   * XPrintModel.usePrintFunctionWithArgument(...) hinzugeladen.
   * 
   * @param useDocPrintFunctions
   *          steuert ob das PrintModel mit den im Dokument gesetzten
   *          Druckfunktionen vorbelegt sein soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public XPrintModel createPrintModel(boolean useDocPrintFunctions)
  {
    XPrintModel pmod = PrintModels.createPrintModel(this);
    if (useDocPrintFunctions)
    {
      for (Iterator iter = printFunctions.iterator(); iter.hasNext();)
      {
        String name = (String) iter.next();
        pmod.usePrintFunction(name);
      }
    }
    return pmod;
  }

  /**
   * TODO: comment TextDocumentModel.addCoupledWindow
   * 
   * @param w
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TODO: TESTEN
   */
  synchronized public void addCoupledWindow(Window window)
  {
    if (window == null) return;
    if (coupledWindowController == null)
    {
      coupledWindowController = new CoupledWindowController();
      XFrame f = getFrame();
      XTopWindow w = null;
      if (f != null) w = UNO.XTopWindow(f.getContainerWindow());
      if (w != null) coupledWindowController.setTopWindow(w);
    }

    coupledWindowController.addCoupledWindow(window);
  }

  /**
   * TODO: comment TextDocumentModel.removeCoupledWindow
   * 
   * @param w
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TODO: TESTEN
   */
  synchronized public void removeCoupledWindow(Window window)
  {
    if (window == null || coupledWindowController == null) return;

    coupledWindowController.removeCoupledWindow(window);
    
    if (!coupledWindowController.hasCoupledWindows())
    {
      // deregistriert den windowListener.
      XFrame f = getFrame();
      XTopWindow w = null;
      if (f != null) w = UNO.XTopWindow(f.getContainerWindow());
      if (w != null) coupledWindowController.unsetTopWindow(w);
      coupledWindowController = null;
    }
  }

  /**
   * Fügt ein neues Dokumentkommando mit dem Kommandostring cmdStr, der in der
   * Form "WM(...)" erwartet wird, in das Dokument an der TextRange r ein. Dabei
   * wird ein neues Bookmark erstellt und dieses als Dokumenkommando
   * registriert. Dieses Bookmark wird genau über r gelegt, so dass abhängig vom
   * Dokumentkommando der Inhalt der TextRange r durch eine eventuelle spätere
   * Ausführung des Dokumentkommandos überschrieben wird. cmdStr muss nur das
   * gewünschte Kommando enthalten ohne eine abschließende Zahl, die zur
   * Herstellung eindeutiger Bookmarks benötigt wird - diese Zahl wird bei
   * Bedarf automatisch an den Bookmarknamen angehängt.
   * 
   * @param r
   *          Die TextRange, an der das neue Bookmark mit diesem
   *          Dokumentkommando eingefügt werden soll. r darf auch null sein und
   *          wird in diesem Fall ignoriert.
   * @param cmdStr
   *          Das Kommando als String der Form "WM(...)".
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void addNewDocumentCommand(XTextRange r, String cmdStr)
  {
    documentCommands.addNewDocumentCommand(r, cmdStr);
  }

  /**
   * Fügt an der Stelle r ein neues Textelement vom Typ
   * css.text.TextField.InputUser ein, und verknüpft das Feld so, dass die Trafo
   * trafo verwendet wird, um den angezeigten Feldwert zu berechnen.
   * 
   * @param r
   *          die Textrange, an der das Feld eingefügt werden soll
   * @param trafoName
   *          der Name der zu verwendenden Trafofunktion
   * @param hint
   *          Ein Hinweistext, der im Feld angezeigt werden soll, wenn man mit
   *          der Maus drüber fährt - kann auch null sein, dann wird der Hint
   *          nicht gesetzt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  synchronized public void addNewInputUserField(XTextRange r, String trafoName,
      String hint)
  {
    try
    {
      String userFieldName = USER_FIELD_NAME_PREFIX + trafoName + "')";

      // master erzeugen
      XPropertySet master = getUserFieldMaster(userFieldName);
      if (master == null)
      {
        master = UNO.XPropertySet(UNO.XMultiServiceFactory(doc).createInstance(
            "com.sun.star.text.FieldMaster.User"));
        UNO.setProperty(master, "Value", new Integer(0));
        UNO.setProperty(master, "Name", userFieldName);
      }

      // textField erzeugen
      XTextContent f = UNO.XTextContent(UNO.XMultiServiceFactory(doc)
          .createInstance("com.sun.star.text.TextField.InputUser"));
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
   * Durch die Aufräumaktion ändert sich der DocumentModified-Status des
   * Dokuments nicht.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1) TESTED
   */
  private void cleanupGarbageOfUnreferencedAutofunctions()
  {
    boolean modified = getDocumentModified();

    // Liste aller derzeit eingesetzten Trafos aufbauen:
    HashSet usedFunctions = new HashSet();
    for (Iterator iter = idToFormFields.keySet().iterator(); iter.hasNext();)
    {
      String id = (String) iter.next();
      List l = (List) idToFormFields.get(id);
      for (Iterator iterator = l.iterator(); iterator.hasNext();)
      {
        FormField f = (FormField) iterator.next();
        String trafoName = f.getTrafoName();
        if (trafoName != null) usedFunctions.add(trafoName);
      }
    }
    for (Iterator iter = idToTextFieldFormFields.keySet().iterator(); iter
        .hasNext();)
    {
      String id = (String) iter.next();
      List l = (List) idToTextFieldFormFields.get(id);
      for (Iterator iterator = l.iterator(); iterator.hasNext();)
      {
        FormField f = (FormField) iterator.next();
        String trafoName = f.getTrafoName();
        if (trafoName != null) usedFunctions.add(trafoName);
      }
    }
    for (Iterator iterator = staticTextFieldFormFields.iterator(); iterator
        .hasNext();)
    {
      FormField f = (FormField) iterator.next();
      String trafoName = f.getTrafoName();
      if (trafoName != null) usedFunctions.add(trafoName);
    }

    // Nicht mehr benötigte Autofunctions aus der Funktionsbibliothek löschen:
    FunctionLibrary funcLib = getFunctionLibrary();
    for (Iterator iter = funcLib.getFunctionNames().iterator(); iter.hasNext();)
    {
      String name = (String) iter.next();
      if (name == null
          || !name.startsWith(AUTOFUNCTION_PREFIX)
          || usedFunctions.contains(name)) continue;
      funcLib.remove(name);
    }

    // Nicht mehr benötigte Autofunctions aus der Formularbeschreibung der
    // persistenten Daten löschen.
    ConfigThingy functions = getFormDescription().query("Formular").query(
        "Funktionen");
    for (Iterator iter = functions.iterator(); iter.hasNext();)
    {
      ConfigThingy funcs = (ConfigThingy) iter.next();
      for (Iterator iterator = funcs.iterator(); iterator.hasNext();)
      {
        String name = ((ConfigThingy) iterator.next()).getName();
        if (name == null
            || !name.startsWith(AUTOFUNCTION_PREFIX)
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
   * Diese Methode liefert den TextFieldMaster, der für Zugriffe auf das
   * Benutzerfeld mit den Namen userFieldName zuständig ist.
   * 
   * @param userFieldName
   * @return den TextFieldMaster oder null, wenn das Benutzerfeld userFieldName
   *         nicht existiert.
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
   * Wenn das Benutzerfeld mit dem Namen userFieldName vom WollMux interpretiert
   * wird (weil der Name in der Form "WM(FUNCTION '<name>')" aufgebaut ist),
   * dann liefert diese Funktion den Namen <name> der Funktion zurück; in allen
   * anderen Fällen liefert die Methode null zurück.
   * 
   * @param userFieldName
   *          Name des Benutzerfeldes
   * @return den Namen der in diesem Benutzerfeld verwendeten Funktion oder
   *         null, wenn das Benutzerfeld nicht vom WollMux interpretiert wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static String getFunctionNameForUserFieldName(String userFieldName)
  {
    if (userFieldName == null) return null;
    if (userFieldName.startsWith(TextDocumentModel.USER_FIELD_NAME_PREFIX))
      return userFieldName.substring(TextDocumentModel.USER_FIELD_NAME_PREFIX
          .length(), userFieldName.length() - 2);
    return null;
  }

  /**
   * Wenn das als Kommandostring cmdStr übergebene Dokumentkommando (derzeit nur
   * insertFormValue) eine Trafofunktion gesetzt hat, so wird der Name dieser
   * Funktion zurückgeliefert; Bildet cmdStr kein gültiges Dokumentkommando ab
   * oder verwendet dieses Dokumentkommando keine Funktion, so wird null zurück
   * geliefert.
   * 
   * @param cmdStr
   *          Ein Kommandostring eines Dokumentkommandos in der Form "WM(CMD '<command>'
   *          ...)"
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
    {
    }

    String cmd = "";
    try
    {
      cmd = wm.get("CMD").toString();
    }
    catch (NodeNotFoundException e)
    {
    }

    if (cmd.equalsIgnoreCase("insertFormValue")) try
    {
      return wm.get("TRAFO").toString();
    }
    catch (NodeNotFoundException e)
    {
    }
    return null;
  }

  /**
   * Ersetzt die aktuelle Selektion (falls vorhanden) durch ein
   * WollMux-Formularfeld mit ID id, dem Hinweistext hint und der durch
   * trafoConf definierten TRAFO. Das Formularfeld ist direkt einsetzbar, d.h.
   * sobald diese Methode zurückkehrt, kann über setFormFieldValue(id,...) das
   * Feld befüllt werden. Ist keine Selektion vorhanden, so tut die Funktion
   * nichts.
   * 
   * @param hint
   *          Ein Hinweistext der als Tooltip des neuen Formularfeldes angezeigt
   *          werden soll. hint kann null sein, dann wird kein Hinweistext
   *          angezeigt.
   * @param trafoConf
   *          darf null sein, dann wird keine TRAFO gesetzt. Ansonsten ein
   *          ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION )",
   *          wobei Bezeichner ein beliebiger Bezeichner ist und
   *          FUNKTIONSDEFINITION ein erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)},
   *          d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine
   *          erlaubter Funktionsname, z.B. "AND" sein. Der Bezeichner wird
   *          NICHT als Name der TRAFO verwendet. Stattdessen wird ein neuer
   *          eindeutiger TRAFO-Name generiert.
   * @param id
   *          die ID über die das Feld mit
   *          {@link #setFormFieldValue(String, String)} befüllt werden kann.
   * 
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  synchronized public void replaceSelectionWithFormField(String fieldId,
      String hint, ConfigThingy trafoConf)
  {
    String trafoName = addLocalAutofunction(trafoConf);

    try
    {
      // Neues UserField an der Cursorposition einfügen
      addNewInputUserField(getViewCursor(), trafoName, hint);

      // Feldwert mit leerem Inhalt vorbelegen, wenn noch kein Wert gesetzt ist.
      if (!formFieldValues.containsKey(fieldId))
        setFormFieldValue(fieldId, "");

      // Ansicht des Formularfeldes aktualisieren:
      collectNonWollMuxFormFields();
      updateFormFields(fieldId);

      // Nicht referenzierte Autofunktionen/InputUser-TextFieldMaster löschen
      cleanupGarbageOfUnreferencedAutofunctions();
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Falls die aktuelle Selektion genau ein Formularfeld enthält (die Selektion
   * muss nicht bündig mit den Grenzen dieses Feldes sein, aber es darf kein
   * zweites Formularfeld in der Selektion enthalten sein) und dieses eine TRAFO
   * gesetzt hat, so wird die Definition dieser TRAFO als ConfigThingy
   * zurückgeliefert, ansonsten null. Wird eine TRAFO gefunden, die in einem
   * globalen Konfigurationsabschnitt definiert ist (also nicht dokumentlokal)
   * und damit auch nicht verändert werden kann, so wird ebenfalls null zurück
   * geliefert.
   * 
   * @return null oder die Definition der TRAFO in der Form
   *         "TrafoName(FUNKTIONSDEFINITION)", wobei TrafoName die Bezeichnung
   *         ist, unter der die TRAFO mittels
   *         {@link #setTrafo(String, ConfigThingy)} modifiziert werden kann.
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  synchronized public ConfigThingy getFormFieldTrafoFromSelection()
  {
    XTextCursor vc = getViewCursor();
    if (vc == null) return null;

    HashMap collectedTrafos = collectTrafosFromEnumeration(vc);

    // Auswertung von collectedTrafos
    HashSet completeFields = new HashSet();
    HashSet startedFields = new HashSet();
    HashSet finishedFields = new HashSet();
    for (Iterator iter = collectedTrafos.keySet().iterator(); iter.hasNext();)
    {
      String trafo = (String) iter.next();
      int complete = ((Integer) collectedTrafos.get(trafo)).intValue();
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
      trafoName = completeFields.iterator().next().toString();
    else if (startedFields.size() > 1)
      return null; // nicht eindeutige Felder
    else if (startedFields.size() == 1)
      trafoName = startedFields.iterator().next().toString();

    // zugehöriges ConfigThingy aus der Formularbeschreibung zurückliefern.
    if (trafoName != null)
      try
      {
        return getFormDescription().query("Formular").query("Funktionen")
            .query(trafoName, 2).getLastChild();
      }
      catch (NodeNotFoundException e)
      {
      }

    return null;
  }

  /**
   * Gibt die Namen aller in der XTextRange gefunden Trafos als Schlüssel einer
   * HashMap zurück. Die zusätzlichen Integer-Werte in der HashMap geben an, ob
   * (1) nur die Startmarke, (2) nur die Endemarke oder (3) ein vollständiges
   * Bookmark/Feld gefunden wurde (Bei atomaren Feldern wird gleich 3 als Wert
   * gesetzt).
   * 
   * @param textRange
   *          die XTextRange an der gesucht werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static HashMap collectTrafosFromEnumeration(XTextRange textRange)
  {
    HashMap collectedTrafos = new HashMap();

    if (textRange == null) return collectedTrafos;
    XEnumerationAccess parEnumAcc = UNO.XEnumerationAccess(textRange.getText()
        .createTextCursorByRange(textRange));
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
          if (t != null) collectedTrafos.put(t, new Integer(3));
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
            boolean isCollapsed = AnyConverter.toBoolean(UNO.getProperty(
                portion,
                "IsCollapsed"));
            isStart = AnyConverter.toBoolean(UNO
                .getProperty(portion, "IsStart"))
                      || isCollapsed;
            isEnd = !isStart || isCollapsed;
          }
          catch (IllegalArgumentException e)
          {
          }

          Matcher m = WOLLMUX_BOOKMARK_PATTERN.matcher(name);
          if (m.matches())
          {
            String t = getFunctionNameForDocumentCommand(m.group(1));
            if (t != null)
            {
              Integer s = (Integer) collectedTrafos.get(t);
              if (s == null) s = new Integer(0);
              if (isStart) s = new Integer(s.intValue() | 1);
              if (isEnd) s = new Integer(s.intValue() | 2);
              collectedTrafos.put(t, s);
            }
          }
        }
      }
    }
    return collectedTrafos;
  }

  /**
   * Ändert die Definition der TRAFO mit Name trafoName auf trafoConf. Die neue
   * Definition wirkt sich sofort auf folgende
   * {@link #setFormFieldValue(String, String)} Aufrufe aus.
   * 
   * @param trafoConf
   *          ein ConfigThingy mit dem Aufbau "Bezeichner( FUNKTIONSDEFINITION
   *          )", wobei Bezeichner ein beliebiger Bezeichner ist und
   *          FUNKTIONSDEFINITION ein erlaubter Parameter für
   *          {@link de.muenchen.allg.itd51.wollmux.func.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)},
   *          d.h. der oberste Knoten von FUNKTIONSDEFINITION muss eine
   *          erlaubter Funktionsname, z.B. "AND" sein. Der Bezeichner wird
   *          NICHT verwendet. Der Name der TRAFO wird ausschließlich durch
   *          trafoName festgelegt.
   * @throws UnavailableException
   *           wird geworfen, wenn die Trafo trafoName nicht schreibend
   *           verändert werden kann, weil sie z.B. nicht existiert oder in
   *           einer globalen Funktionsbeschreibung definiert ist.
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
      func = getFormDescription().query("Formular").query("Funktionen").query(
          trafoName,
          2).getLastChild();
    }
    catch (NodeNotFoundException e)
    {
      throw new UnavailableException(e);
    }

    // Funktion parsen und in Funktionsbibliothek setzen:
    FunctionLibrary funcLib = getFunctionLibrary();
    Function function = FunctionFactory.parseChildren(
        trafoConf,
        funcLib,
        getDialogLibrary(),
        getFunctionContext());
    funcLib.add(trafoName, function);

    // Kinder von func löschen, damit sie später neu gesetzt werden können
    for (Iterator iter = func.iterator(); iter.hasNext();)
    {
      iter.next();
      iter.remove();
    }

    // Kinder von trafoConf auf func übertragen
    for (Iterator iter = trafoConf.iterator(); iter.hasNext();)
    {
      ConfigThingy f = (ConfigThingy) iter.next();
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

    // alle Felder updaten, die die Trafo trafoName verwenden:
    updateAllFormFieldsWithTrafo(trafoName);
  }
}
