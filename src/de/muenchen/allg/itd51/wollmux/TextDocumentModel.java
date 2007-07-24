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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.dialog.FormController;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

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
      .compile("(\\A\\s*WM\\s*\\(.*\\)\\s*\\d*\\z)");

  /**
   * Ermöglicht den Zugriff auf eine Collection aller FormField-Objekte in
   * diesem TextDokument über den Namen der zugeordneten ID. Die in dieser Map
   * enthaltenen FormFields sind nicht in {@link #idToTextFieldFormFields}
   * enthalten und umgekehrt.
   */
  private HashMap idToFormFields;

  /**
   * Liefert zu einer ID eine {@link java.util.List} von FormField-Objekten, die
   * alle zu einfachen Textfeldern (derzeit nur MailMerge-Feldern) gehören ohne
   * ein umschließendes WollMux-Bookmark. Die in dieser Map enthaltenen
   * FormFields sind nicht in {@link #idToFormFields} enthalten und umgekehrt.
   */
  private HashMap idToTextFieldFormFields;

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
   * Enthält den Namen der aktuell gesetzten Druckfunktion oder null, wenn keine
   * Druckfunktion gesetzt ist.
   */
  private String printFunctionName;

  /**
   * Enthält den Namen der Druckfunktion, die vor dem letzten Aufruf der Methode
   * setPrintFunction(...) gesetzt war oder null wenn noch keine Druckfunktion
   * gesetzt war.
   */
  private String formerPrintFunctionName;

  /**
   * Enthält das zu diesem TextDocumentModel zugehörige XPrintModel.
   */
  private XPrintModel printModel;

  /**
   * Enthält die Gesamtbeschreibung alle Formular-Abschnitte, die in den
   * persistenten Daten bzw. in den mit add hinzugefügten Form-Kommandos
   * gefunden wurden.
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
   * Enthält die Menge aller Textbereiche des Dokuments, die über den
   * Namenszusatz "GROUPS (<Liste mit Gruppen>)" gekennzeichnet sind.
   */
  private Set /* of VisibleSection */visibleTextSections;

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
    this.fragUrls = new String[] {};
    this.currentMax4000 = null;
    this.closeListener = null;
    this.printFunctionName = null;
    this.printSettingsDone = false;
    this.formularConf = new ConfigThingy("WM");
    this.formFieldValues = new HashMap();
    this.invisibleGroups = new HashSet();
    this.overrideFragMap = new HashMap();
    this.formModel = null;
    this.printModel = new PrintModel(this);

    // Kommandobaum erzeugen (modified-Status dabei unberührt lassen):
    boolean modified = getDocumentModified();
    this.documentCommands = new DocumentCommands(UNO.XBookmarksSupplier(doc));
    documentCommands.update();
    setDocumentModified(modified);

    registerCloseListener();

    // Textbereiche mit Namenszusatz 'GROUPS (<Liste mit Gruppen>)' einlesen:
    visibleTextSections = TextSection.createVisibleSections(UNO
        .XTextSectionsSupplier(doc));

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
    this.printFunctionName = persistentData.getData(DATA_ID_PRINTFUNCTION);
    parseFormDescription(persistentData.getData(DATA_ID_FORMULARBESCHREIBUNG));
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
  public DocumentCommands getDocumentCommands()
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
  public Iterator visibleElementsIterator()
  {
    Vector visibleElements = new Vector();
    visibleElements.addAll(visibleTextSections);
    for (Iterator iter = documentCommands.setGroupsIterator(); iter.hasNext();)
      visibleElements.add(iter.next());
    return visibleElements.iterator();
  }

  /**
   * Wertet den String-Inhalt value der Formularbeschreibungsnotiz aus, die von
   * der Form "WM( Formular(...) )" sein muss und fügt diese der
   * Gesamtbeschreibung hinzu.
   * 
   * @param value
   *          darf null sein und wird in diesem Fall ignoriert, darf aber kein
   *          leerer String sein, sonst Fehler.
   */
  private void parseFormDescription(String value)
  {
    if (value == null) return;

    try
    {
      ConfigThingy conf = new ConfigThingy("", null, new StringReader(value));
      // Formular-Abschnitt auswerten:
      try
      {
        ConfigThingy formular = conf.get("WM").get("Formular");
        formularConf.addChild(formular);
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(new ConfigurationErrorException(
            "Die Formularbeschreibung enthält keinen Abschnitt 'Formular':\n"
                + e.getMessage()));
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(new ConfigurationErrorException(
          "Formularbeschreibung ist fehlerhaft:\n" + e.getMessage()));
      return;
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
      Logger.error(new ConfigurationErrorException(
          "Formularwerte-Abschnitt ist fehlerhaft:\n" + e.getMessage()));
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
  public void setIDToFormFields(HashMap idToFormFields)
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
   *         Vorbelegungen im Dokument.
   */
  public HashMap getIDToPresetValue()
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

          if (field.hasChangedPreviously()) allAreUnchanged = false;

          if (field.hasTrafo())
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
          value = (String) formFieldValues.get(id);
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
        value = (String) formFieldValues.get(id);
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
  public boolean hasMailMergeFields()
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
   * befüllt werden (derzeit c,s,s,t,textfield,Database-Felder).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void collectNonWollMuxFormFields()
  {
    idToTextFieldFormFields.clear();

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
          XPropertySet master = tf.getTextFieldMaster();
          String column = (String) UNO.getProperty(master, "DataColumnName");
          if (column != null && column.length() > 0)
          {
            if (!idToTextFieldFormFields.containsKey(column))
              idToTextFieldFormFields.put(column, new Vector());

            List formFields = (List) idToTextFieldFormFields.get(column);
            formFields.add(FormFieldFactory.createFormField(doc, tf));
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
  public String[] getFragUrls()
  {
    return fragUrls;
  }

  /**
   * Über diese Methode kann der openDocument-Eventhandler die Liste der mit
   * einem insertContent-Kommando zu öffnenden frag-urls speichern.
   */
  public void setFragUrls(String[] fragUrls)
  {
    this.fragUrls = fragUrls;
  }

  /**
   * Registriert das Überschreiben des Textfragments fragId auf den neuen Namen
   * newFragId im TextDocumentModel, wenn das Textfragment fragId nicht bereits
   * überschrieben wurde.
   */
  public void setOverrideFrag(String fragId, String newFragId)
  {
    if (!overrideFragMap.containsKey(fragId))
      overrideFragMap.put(fragId, newFragId);
  }

  /**
   * Liefert die neue FragId zurück, die anstelle der FRAG_ID fragId verwendet
   * werden soll und durch ein WM(CMD 'overrideFrag'...)-Kommando gesetzt wurde,
   * oder fragId (also sich selbst), wenn keine Überschreibung definiert ist.
   */
  public String getOverrideFrag(String fragId)
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
  public void setCurrentFormularMax4000(FormularMax4000 max)
  {
    currentMax4000 = max;
  }

  /**
   * Liefert die Instanz des aktuell geöffneten, zu diesem Dokument gehörenden
   * FormularMax4000 zurück, oder null, falls kein FormularMax gestartet wurde.
   * 
   * @return
   */
  public FormularMax4000 getCurrentFormularMax4000()
  {
    return currentMax4000;
  }

  /**
   * Liefert true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage
   * behandelt werden soll, ansonsten false.
   * 
   * @return true, wenn das Dokument eine Vorlage ist oder wie eine Vorlage
   *         behandelt werden soll, ansonsten false.
   */
  public boolean isTemplate()
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
  public boolean hasURL()
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
  public boolean isFormDocument()
  {
    return (type != null && type.equalsIgnoreCase("formDocument"));
  }

  /**
   * Liefert true, wenn das Dokument ein Teil eines Multiformdokuments ist.
   * 
   * @return Liefert true, wenn das Dokument Teil eines Multiformdokuments ist.
   */
  public boolean isPartOfMultiformDocument()
  {
    return partOfMultiform;
  }

  public void setPartOfMultiformDocument(boolean partOfMultiform)
  {
    this.partOfMultiform = partOfMultiform;
  }

  /**
   * Liefert true, wenn das Dokument ein Formular mit einer gültigen
   * Formularbeschreibung enthält und damit die Dokumentkommandos des
   * Formularsystems bearbeitet werden sollen.
   * 
   * @return true, wenn das Dokument ein Formular mit einer gültigen
   *         Formularbeschreibung ist, ansonsten false.
   */
  public boolean hasFormDescriptor()
  {
    return !(formularConf.count() == 0);
  }

  /**
   * Setzt den Typ des Dokuments auf type und speichert den Wert persistent im
   * Dokument ab.
   */
  public void setType(String type)
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
  public void setType(DocumentCommand.SetType cmd)
  {
    if (type == null) this.type = cmd.getType();
  }

  /**
   * Diese Methode setzt die diesem TextDocumentModel zugehörige Druckfunktion
   * auf den Wert functionName, der ein gültiger Funktionsbezeichner sein muss
   * oder setzt eine Druckfunktion auf den davor zuletzt gesetzten Wert zurück,
   * wenn functionName der Leerstring ist und früher bereits eine Druckfunktion
   * registriert war. War früher noch keine Druckfunktion registriert, so wird
   * das entsprechende setPrintFunction-Dokumentkommando aus dem Dokument
   * gelöscht.
   * 
   * @param newFunctionName
   *          der Name der Druckfunktion (zum setzen), der Leerstring "" (zum
   *          zurücksetzen auf die zuletzt gesetzte Druckfunktion) oder
   *          "default" zum Löschen der Druckfunktion. Der zu setzende Name muss
   *          ein gültiger Funktionsbezeichner sein und in einem Abschnitt
   *          "Druckfunktionen" in der wollmux.conf definiert sein.
   */
  public void setPrintFunction(String newFunctionName)
  {
    // nichts machen, wenn der Name bereits gesetzt ist.
    if (printFunctionName != null && printFunctionName.equals(newFunctionName))
      return;

    // Bei null oder Leerstring: Name der vorhergehenden Druckfunktion
    // verwenden.
    if (newFunctionName == null || newFunctionName.equals(""))
      newFunctionName = formerPrintFunctionName;
    else if (newFunctionName != null
             && newFunctionName.equalsIgnoreCase("default"))
      newFunctionName = null;

    // Neuen Funktionsnamen setzen und alten merken
    formerPrintFunctionName = printFunctionName;
    printFunctionName = newFunctionName;

    // Persistente Daten entsprechend anpassen
    if (printFunctionName != null)
    {
      persistentData.setData(DATA_ID_PRINTFUNCTION, printFunctionName);
    }
    else
    {
      persistentData.removeData(DATA_ID_PRINTFUNCTION);
    }

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
   * Setzt die Druckfunktion die Druckfunktion zurück, die gesetzt war, bevor
   * die Druckfunktion functionName gesetzt wurde. Die Druckfunktion wird nur
   * zurück gesetzt, wenn die aktuell gesetzte Druckfunktion functionName
   * entspricht.
   * 
   * Wird z.B. in den Sachleitenden Verfügungen verwendet, um auf die
   * ursprünglich gesetzte Druckfunktion zurück zu schalten, wenn keine
   * Verfügungspunkte vorhanden sind.
   * 
   * @param functionNameToReset
   *          der Name der Druckfunktion, die zurück gesetzt werden soll (falls
   *          sie aktuell gesetzt ist).
   */
  public void resetPrintFunction(String functionNameToReset)
  {
    if (printFunctionName != null
        && printFunctionName.equals(functionNameToReset)) setPrintFunction("");
  }

  /**
   * Wird vom DocumentCommandInterpreter beim parsen des Dokumentkommandobaumes
   * aufgerufen, wenn das Dokument ein setPrintFunction-Kommando enthält und
   * setzt die in cmd.getFunctionName() enthaltene Druckfunktion PERSISTENT im
   * Dokument, falls nicht bereits eine Druckfunktion definiert ist. Ansonsten
   * wird das Dokumentkommando ignoriert.
   * 
   * @param cmd
   *          Das gefundene setPrintFunction-Dokumentkommando.
   */
  public void setPrintFunction(SetPrintFunction cmd)
  {
    if (printFunctionName == null) setPrintFunction(cmd.getFunctionName());
  }

  /**
   * Liefert den Namen der aktuellen Druckfunktion zurück, oder null, wenn keine
   * Druckfunktion definiert ist.
   */
  public String getPrintFunctionName()
  {
    return printFunctionName;
  }

  /**
   * Liefert die zusätzlichen Konfigurationsdaten für die Druckfunktion zurück
   * (oder einen leerer String, falls nicht gesetzt).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getPrintFunctionConfig()
  {
    // TODO getPrintFunctionConfig() implementieren
    return "";
  }

  /**
   * Setzt die zusätzlichen Konfigurationsdaten für die Druckfunktion auf conf.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setPrintFunctionConfig(String conf)
  {
    // TODO setPrintFunctionConfig() implementieren
  }

  /**
   * Liefert ein HashSet mit den Namen (Strings) aller als unsichtbar markierten
   * Sichtbarkeitsgruppen.
   */
  public HashSet getInvisibleGroups()
  {
    return invisibleGroups;
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * NotInOrininal-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller
   *         NotInOrininal-Dokumentkommandos dieses Dokuments ermöglicht. Der
   *         Iterator kann auch keine Elemente enthalten.
   */
  public Iterator getNotInOrininalBlocksIterator()
  {
    return documentCommands.notInOriginalIterator();
  }

  /**
   * Liefert einen Iterator zurück, der die Iteration aller
   * DraftOnly-Dokumentkommandos dieses Dokuments ermöglicht.
   * 
   * @return ein Iterator, der die Iteration aller DraftOnly-Dokumentkommandos
   *         dieses Dokuments ermöglicht. Der Iterator kann auch keine Elemente
   *         enthalten.
   */
  public Iterator getDraftOnlyBlocksIterator()
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
  public Iterator getAllVersionsBlocksIterator()
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
  public SetJumpMark getFirstJumpMark()
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
  public XTextCursor getViewCursor()
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
  public void removeNonWMBookmarks()
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
  public void deForm()
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
   * Liefert die aktuelle Formularbeschreibung.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getFormDescription()
  {
    return formularConf;
  }

  /**
   * Ersetzt die Formularbeschreibung dieses Dokuments durch die aus conf.
   * ACHTUNG! conf wird nicht kopiert sondern als Referenz eingebunden.
   * 
   * @param conf
   *          ein WM-Knoten, der "Formular"-Kinder hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setFormDescription(ConfigThingy conf)
  {
    formularConf = conf;
    persistentData.setData(DATA_ID_FORMULARBESCHREIBUNG, formularConf
        .stringRepresentation());
    setDocumentModified(true);
  }

  /**
   * Setzt den Wert des WollMuxFormularfeldes fieldId auf value.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setFormFieldValue(String fieldId, String value)
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
   * Überträgt den aktuell gesetzten Wert des Formularfeldes mit der ID fieldID
   * in die Formularfelder im Dokument.
   * 
   * @param fieldId
   *          Die ID des Formularfeldes bzw. der Formularfelder, die im Dokument
   *          angepasst werden sollen.
   * @param funcLib
   *          Die Funktionsbibliothek, die zum Auflösen der TRAFO-Attribute der
   *          Formularfelder verwendet werden sollen.
   */
  public void updateFormFields(String fieldId, FunctionLibrary funcLib)
  {
    if (formFieldValues.containsKey(fieldId))
    {
      String value = formFieldValues.get(fieldId).toString();
      setFormFields(fieldId, funcLib, value);
    }
  }

  /**
   * Setzt den Inhalt aller Formularfelder mit ID fieldId auf value.
   * 
   * @param funcLib
   *          Funktionsbibliothek zum Berechnen von TRAFOs.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFormFields(String fieldId, FunctionLibrary funcLib,
      String value)
  {
    setFormFields((List) idToFormFields.get(fieldId), funcLib, value);
    setFormFields((List) idToTextFieldFormFields.get(fieldId), funcLib, value);
  }

  /**
   * Setzt den Inhalt aller Formularfelder aus der Liste formFields auf value.
   * formFields kann null sein, dann passiert nichts.
   * 
   * @param funcLib
   *          Funktionsbibliothek zum Berechnen von TRAFOs.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setFormFields(List formFields, FunctionLibrary funcLib,
      String value)
  {
    if (formFields == null) return;
    Iterator fields = formFields.iterator();
    while (fields.hasNext())
    {
      FormField field = (FormField) fields.next();
      try
      {
        field.setValue(value, funcLib);
      }
      catch (RuntimeException e)
      {
        // Absicherung gegen das manuelle Löschen von Dokumentinhalten.
      }
    }
  }

  /**
   * Überträgt den aktuell gesetzten Wert des Formularfeldes mit der ID fieldID
   * in die Formularfelder im Dokument, wobei zum Auflösen der TRAFO-Attribute
   * ausschließlich die globalen Funktionen verwendet werden.
   * 
   * @param fieldId
   *          Die ID des Formularfeldes bzw. des Formularfelder, die im Dokument
   *          angepasst werden sollen.
   */
  public void updateFormFields(String fieldId)
  {
    updateFormFields(fieldId, WollMuxSingleton.getInstance()
        .getGlobalFunctions());
  }

  /**
   * Setzt den ViewCursor auf das erste untransformierte Formularfeld, das den
   * Formularwert mit der ID fieldID darstellt. Falls kein untransformiertes
   * Formularfeld vorhanden ist, wird ein transformiertes gewählt.
   * 
   * @param fieldId
   *          Die ID des Formularfeldes, das angesprungen werden soll.
   */
  public void focusFormField(String fieldId)
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
      if (!f.hasTrafo()) return f;
    }
    return field;
  }

  /**
   * Liefert die zu diesem Dokument zugehörige FormularGUI, falls dem
   * TextDocumentModel die Existent einer FormGUI über setFormGUI(...)
   * mitgeteilt wurde - andernfalls wird null zurück geliefert.
   * 
   * @return Die FormularGUI des Formulardokuments oder null
   */
  public FormModel getFormModel()
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
  public void setFormModel(FormModel formModel)
  {
    this.formModel = formModel;
  }

  /**
   * Liefert den Frame zu diesem TextDocument oder null, wenn der Frame nicht
   * bestimmt werden kann.
   * 
   * @return
   */
  public XFrame getFrame()
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
  public int getPageCount()
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
  public void setWindowVisible(boolean visible)
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
  public boolean getDocumentModified()
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
  public void setDocumentModified(boolean state)
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
  public void setLockControllers(boolean lock)
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
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
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
  public void setWindowViewSettings(ConfigThingy settings)
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
  public void setDocumentZoom(ConfigThingy conf)
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
   * Die Methode fügt die Formularbeschreibung, die unterhalb der Notiz des
   * WM(CMD'Form')-Kommandos gefunden wird zur Gesamtformularbeschreibung hinzu.
   * 
   * @param formCmd
   *          Das formCmd, das die Notzi mit der hinzuzufügenden
   *          Formularbeschreibung enthält.
   * @throws ConfigurationErrorException
   *           Die Notiz der Formularbeschreibung ist nicht vorhanden, die
   *           Formularbeschreibung ist nicht vollständig oder kann nicht
   *           geparst werden.
   */
  public void addFormCommand(DocumentCommand.Form formCmd)
      throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextRange();

    Object annotationField = WollMuxSingleton
        .findAnnotationFieldRecursive(range);
    if (annotationField == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung fehlt.");

    Object content = UNO.getProperty(annotationField, "Content");
    if (content == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung kann nicht gelesen werden.");

    parseFormDescription(content.toString());
  }

  /**
   * Druckt das Dokument mit der Anzahl von Ausfertigungen numberOfCopies auf
   * dem aktuell eingestellten Drucker aus.
   * 
   * @param numberOfCopies
   * @throws PrintFailedException
   */
  public void print(short numberOfCopies) throws PrintFailedException
  {
    if (UNO.XPrintable(doc) != null)
    {
      PropertyValue[] args = new PropertyValue[] {
                                                  new PropertyValue(),
                                                  new PropertyValue() };
      args[0].Name = "CopyCount";
      args[0].Value = new Short(numberOfCopies);
      args[1].Name = "Wait";
      args[1].Value = Boolean.TRUE;

      try
      {
        UNO.XPrintable(doc).print(args);
      }
      catch (java.lang.Exception e)
      {
        throw new PrintFailedException(e);
      }
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
  public void close()
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
  public void dispose()
  {
    if (currentMax4000 != null) currentMax4000.dispose();
    currentMax4000 = null;

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
  public String getTitle()
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
  public String toString()
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
   * Liefert das zu diesem TextDocumentModel zugehörige XPrintModel.
   */
  public XPrintModel getPrintModel()
  {
    return printModel;
  }

  /**
   * Das XPrintModel ist Bestandteil der Komfortdruckfunktionen, wobei jede
   * Druckfunktion ein XPrintModel übergeben bekommt, das das Drucken aus der
   * Komfortdruckfunktion heraus erleichtern soll. Da die einzelnen
   * Druckfunktionen in eigenen Threads laufen, muss jede Druckfunktion sicher
   * stellen, dass die zu erledigenden Aktionen mit dem
   * WollMuxEventHandler-Thread synchronisiert werden. Dies geschieht über einen
   * lock-wait-callback-Mechanismus. Vor dem Einstellen des Action-Ereignisses
   * in den WollMuxEventHandler wird ein lock gesetzt. Nach dem Einstellen des
   * Ereignisses wird so lange gewartet, bis der WollMuxEventHandler die
   * übergebene Callback-Methode aufruft.
   * 
   * @author christoph.lutz
   */
  public static class PrintModel extends WeakBase implements XPrintModel
  {
    // TODO: Wenn beim Drucken (oder auch bei anderen Aktionen) ein Fehler
    // auftritt, soll das PrintModel alle weiteren Anfragen verweigern.
    /**
     * Das TextDocumentModel zu diesem PrintModel
     */
    private TextDocumentModel model;

    /**
     * Das lock-Flag, das vor dem Einstellen eines WollMuxEvents auf true
     * gesetzt werden muss und signalisiert, ob das WollMuxEvent erfolgreich
     * abgearbeitet wurde.
     */
    private boolean[] lock = new boolean[] { true };

    private PrintModel(TextDocumentModel model)
    {
      this.model = model;
    }

    /**
     * Liefert das XTextDocument mit dem die Druckfunktion aufgerufen wurde.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getTextDocument()
     */
    public XTextDocument getTextDocument()
    {
      return model.doc;
    }

    /**
     * Druckt das TextDocument mit numberOfCopies Ausfertigungen auf dem aktuell
     * eingestellten Drucker aus.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
     */
    public void print(short numberOfCopies)
    {
      setLock();
      WollMuxEventHandler.handlePrintViaPrintModel(
          model.doc,
          numberOfCopies,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Falls das TextDocument Sachleitende Verfügungen enthält, ist es mit
     * dieser Methode möglich, den Verfügungspunkt mit der Nummer verfPunkt
     * auszudrucken, wobei alle darauffolgenden Verfügungspunkte ausgeblendet
     * werden.
     * 
     * @param verfPunkt
     *          Die Nummer des auszuduruckenden Verfügungspunktes, wobei alle
     *          folgenden Verfügungspunkte ausgeblendet werden.
     * @param numberOfCopies
     *          Die Anzahl der Ausfertigungen, in der verfPunkt ausgedruckt
     *          werden soll.
     * @param isDraft
     *          wenn isDraft==true, werden alle draftOnly-Blöcke eingeblendet,
     *          ansonsten werden sie ausgeblendet.
     * @param isOriginal
     *          wenn isOriginal, wird die Ziffer des Verfügungspunktes I
     *          ausgeblendet und alle notInOriginal-Blöcke ebenso. Andernfalls
     *          sind Ziffer und notInOriginal-Blöcke eingeblendet.
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#printVerfuegungspunkt(short,
     *      short, boolean, boolean)
     */
    public void printVerfuegungspunkt(short verfPunkt, short numberOfCopies,
        boolean isDraft, boolean isOriginal)
    {
      setLock();
      WollMuxEventHandler.handlePrintVerfuegungspunkt(
          model.doc,
          verfPunkt,
          numberOfCopies,
          isDraft,
          isOriginal,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Zeigt den PrintSetupDialog an, über den der aktuelle Drucker ausgewählt
     * und geändert werden kann.
     * 
     * @param onlyOnce
     *          Gibt an, dass der Dialog nur beim ersten Aufruf (aus Sicht eines
     *          Dokuments) der Methode angezeigt wird. Wurde bereits vor dem
     *          Aufruf ein PrintSetup-Dialog gestartet, so öffnet sich der
     *          Dialog nicht und die Methode endet ohne Aktion.
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#showPrinterSetupDialog()
     */
    public void showPrinterSetupDialog(boolean onlyOnce)
    {
      setLock();
      WollMuxEventHandler.handleShowPrinterSetupDialog(
          model.doc,
          onlyOnce,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Falls es sich bei dem zugehörigen Dokument um ein Formulardokument (mit
     * einer Formularbeschreibung) handelt, wird das Formularfeld mit der ID id
     * auf den neuen Wert value gesetzt und alle von diesem Formularfeld
     * abhängigen Formularfelder entsprechend angepasst. Handelt es sich beim
     * zugehörigen Dokument um ein Dokument ohne Formularbeschreibung, so werden
     * nur alle insertFormValue-Kommandos dieses Dokuments angepasst, die die ID
     * id besitzen.
     * 
     * @param id
     *          Die ID des Formularfeldes, dessen Wert verändert werden soll.
     *          Ist die FormGUI aktiv, so werden auch alle von id abhängigen
     *          Formularwerte neu gesetzt.
     * @param value
     *          Der neue Wert des Formularfeldes id
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setFormValue(java.lang.String,
     *      java.lang.String)
     */
    public void setFormValue(String id, String value)
    {
      setLock();
      WollMuxEventHandler.handleSetFormValueViaPrintModel(
          model.doc,
          id,
          value,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Liefert true, wenn das Dokument als "modifiziert" markiert ist und damit
     * z.B. die "Speichern?" Abfrage vor dem Schließen erscheint.
     * 
     * Manche Druckfunktionen verändern u.U. den Inhalt von Dokumenten. Trotzdem
     * kann es sein, dass eine solche Druckfunktion den "Modifiziert"-Status des
     * Dokuments nicht verändern darf um ungewünschte "Speichern?"-Abfragen zu
     * verhindern. In diesem Fall kann der "Modifiziert"-Status mit folgendem
     * Konstrukt innerhalb der Druckfunktion unverändert gehalten werden:
     * 
     * boolean modified = pmod.getDocumentModified();
     * 
     * ...die eigentliche Druckfunktion, die das Dokument verändert...
     * 
     * pmod.setDocumentModified(modified);
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getDocumentModified()
     */
    public boolean getDocumentModified()
    {
      // Keine WollMuxEvent notwendig, da keine WollMux-Datenstrukturen
      // angefasst werden.
      return model.getDocumentModified();
    }

    /**
     * Diese Methode setzt den DocumentModified-Status auf modified.
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#setDocumentModified(boolean)
     */
    public void setDocumentModified(boolean modified)
    {
      // Keine WollMuxEvent notwendig, da keine WollMux-Datenstrukturen
      // angefasst werden.
      model.setDocumentModified(modified);
    }

    /**
     * Sammelt alle Formularfelder des Dokuments auf, die nicht von
     * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden
     * und befüllt werden (derzeit c,s,s,t,textfield,Database-Felder). So werden
     * z.B. Seriendruckfelder erkannt, die erst nach dem Öffnen des Dokuments
     * manuell hinzugefügt wurden.
     */
    public void collectNonWollMuxFormFields()
    {
      setLock();
      WollMuxEventHandler.handleCollectNonWollMuxFormFieldsViaPrintModel(
          model,
          unlockActionListener);
      waitForUnlock();
    }

    /**
     * Setzt einen lock, der in Verbindung mit setUnlock und der
     * waitForUnlock-Methode verwendet werden kann, um eine Synchronisierung mit
     * dem WollMuxEventHandler-Thread zu realisieren. setLock() sollte stets vor
     * dem Absetzen des WollMux-Events erfolgen, nach dem Absetzen des
     * WollMux-Events folgt der Aufruf der waitForUnlock()-Methode. Das
     * WollMuxEventHandler-Event erzeugt bei der Beendigung ein ActionEvent, das
     * dafür sorgt, dass setUnlock aufgerufen wird.
     */
    protected void setLock()
    {
      synchronized (lock)
      {
        lock[0] = true;
      }
    }

    /**
     * Macht einen mit setLock() gesetzten Lock rückgängig und bricht damit eine
     * evtl. wartende waitForUnlock()-Methode ab.
     */
    protected void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
    }

    /**
     * Wartet so lange, bis der vorher mit setLock() gesetzt lock mit der
     * Methode setUnlock() aufgehoben wird. So kann die Synchronisierung mit
     * Events aus dem WollMuxEventHandler-Thread realisiert werden. setLock()
     * sollte stets vor dem Aufruf des Events erfolgen, nach dem Aufruf des
     * Events folgt der Aufruf der waitForUnlock()-Methode. Das Event erzeugt
     * bei der Beendigung ein ActionEvent, das dafür sorgt, dass setUnlock
     * aufgerufen wird.
     */
    protected void waitForUnlock()
    {
      try
      {
        synchronized (lock)
        {
          while (lock[0] == true)
            lock.wait();
        }
      }
      catch (InterruptedException e)
      {
      }
    }

    /**
     * Dieser ActionListener kann WollMuxHandler-Events übergeben werden und
     * sorgt in Verbindung mit den Methoden setLock() und waitForUnlock() dafür,
     * dass eine Synchronisierung mit dem WollMuxEventHandler-Thread realisiert
     * werden kann.
     */
    protected UnlockActionListener unlockActionListener = new UnlockActionListener();

    protected class UnlockActionListener implements ActionListener
    {
      public ActionEvent actionEvent = null;

      public void actionPerformed(ActionEvent arg0)
      {
        setUnlock();
        actionEvent = arg0;
      }
    }
  }

}
