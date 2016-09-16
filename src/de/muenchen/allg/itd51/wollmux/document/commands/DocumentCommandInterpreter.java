/*
 * Dateiname: DocumentCommandInterpreter.java
 * Projekt  : WollMux
 * Funktion : Interpretiert die in einem Dokument enthaltenen Dokumentkommandos.
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
package de.muenchen.allg.itd51.wollmux.document.commands;

import java.util.HashSet;
import java.util.Iterator;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.WMCommandsFailedException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Diese Klasse repräsentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter
{

  private TextDocumentController documentController;

  /**
   * Enthält die Instanz auf das zentrale WollMuxSingleton.
   */
  boolean debugMode;

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
  public DocumentCommandInterpreter(TextDocumentController documentController, boolean debugMode)
  {
    this.documentController = documentController;
    this.debugMode = debugMode;
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
  public DocumentCommandInterpreter(TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.debugMode = false;
  }
  
  public TextDocumentController getDocumentController()
  {
    return documentController;
  }

  public TextDocumentModel getModel()
  {
    return getDocumentController().getModel();
  }

  /**
   * Diese Methode sollte vor {@link #executeTemplateCommands()} aufgerufen werden
   * und sorgt dafür, dass alle globalen Einstellungen des Dokuments (setType,
   * setPrintFunction) an das TextDocumentModel weitergereicht werden.
   */
  public void scanGlobalDocumentCommands()
  {
    Logger.debug("scanGlobalDocumentCommands");
    boolean modified = getDocumentController().getModel().isDocumentModified();
    
    try
    {
      getDocumentController().getModel().setDocumentModifiable(false);
      GlobalDocumentCommandsScanner s = new GlobalDocumentCommandsScanner(this);
      s.execute(getDocumentController().getModel().getDocumentCommands());
    }
    finally
    {
      getDocumentController().getModel().setDocumentModified(modified);
      getDocumentController().getModel().setDocumentModifiable(true);
    }
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
    boolean modified = getDocumentController().getModel().isDocumentModified();
    
    try
    {
      getDocumentController().getModel().setDocumentModifiable(false);
      InsertFormValueCommandsScanner s = new InsertFormValueCommandsScanner(this);
      s.execute(getDocumentController().getModel().getDocumentCommands());

      getDocumentController().getModel().setIDToFormFields(s.idToFormFields);
      getDocumentController().collectNonWollMuxFormFields();
    }
    finally
    {
      getDocumentController().getModel().setDocumentModified(modified);
      getDocumentController().getModel().setDocumentModifiable(true);
    }
  }

  /**
   * Über diese Methode wird die Ausführung der Kommandos gestartet, die für das
   * Expandieren und Befüllen von Dokumenten notwendig sind.
   * 
   * @throws WMCommandsFailedException
   */
  public void executeTemplateCommands() throws WMCommandsFailedException
  {
    // Zähler für aufgetretene Fehler bei der Bearbeitung der Kommandos.
    int errors = 0;
    boolean modified = getDocumentController().getModel().isDocumentModified();

    try
    {
      Logger.debug("executeTemplateCommands");
      getDocumentController().getModel().setDocumentModifiable(false);
      // Zuerst alle Kommandos bearbeiten, die irgendwie Kinder bekommen
      // können, damit der DocumentCommandTree vollständig aufgebaut werden
      // kann.
      errors +=
        new DocumentExpander(this, getDocumentController().getModel().getFragUrls()).execute(getDocumentController().getModel().getDocumentCommands());

      // Überträgt beim übergebenen XTextDocument doc die Eigenschaften der
      // Seitenvorlage Wollmuxseite auf die Seitenvorlage Standard, falls
      // Seitenvorlage Wollmuxseite vorhanden ist.
      pageStyleWollmuxseiteToStandard(getDocumentController().getModel().doc);

      // Ziffern-Anpassen der Sachleitenden Verfügungen aufrufen:
      SachleitendeVerfuegung.ziffernAnpassen(getDocumentController());

      // Jetzt können die TextFelder innerhalb der updateFields Kommandos
      // geupdatet werden. Durch die Auslagerung in einen extra Schritt wird die
      // Reihenfolge der Abarbeitung klar definiert (zuerst die updateFields
      // Kommandos, dann die anderen Kommandos). Dies ist wichtig, da
      // insbesondere das updateFields Kommando exakt mit einem anderen Kommando
      // übereinander liegen kann. Ausserdem liegt updateFields thematisch näher
      // am expandieren der Textfragmente, da updateFields im Prinzip nur dessen
      // Schwäche beseitigt.
      errors +=
        new TextFieldUpdater(this).execute(getDocumentController().getModel().getDocumentCommands());

      // Hauptverarbeitung: Jetzt alle noch übrigen DocumentCommands (z.B.
      // insertValues) in einem einzigen Durchlauf mit execute bearbeiten.
      errors +=
        new MainProcessor(this).execute(getDocumentController().getModel().getDocumentCommands());

      // Da keine neuen Elemente mehr eingefügt werden müssen, können
      // jetzt die INSERT_MARKS "<" und ">" der insertFrags und
      // InsertContent-Kommandos gelöscht werden.
      // errors += cleanInsertMarks(tree);

      // Erst nachdem die INSERT_MARKS entfernt wurden, lassen sich leere
      // Absätze zum Beginn und Ende der insertFrag bzw. insertContent-Kommandos
      // sauber erkennen und entfernen.
      // errors += new EmptyParagraphCleaner().execute(tree);
      SurroundingGarbageCollector collect = new SurroundingGarbageCollector(this);
      errors +=
        collect.execute(getDocumentController().getModel().getDocumentCommands());
      collect.removeGarbage();

      // da hier bookmarks entfernt werden, muss der Baum upgedatet werden
      getDocumentController().updateDocumentCommands();

      // Jetzt wird das Dokument als Formulardokument markiert, wenn mindestens ein
      // Formularfenster definiert ist.
      if (getDocumentController().getModel().hasFormGUIWindow())
        getDocumentController().markAsFormDocument();
    }
    finally
    {
      // Document-Modified auf false setzen, da nur wirkliche
      // Benutzerinteraktionen den Modified-Status beeinflussen sollen.
      getDocumentController().getModel().setDocumentModified(modified);
      getDocumentController().getModel().setDocumentModifiable(true);

      // ggf. eine WMCommandsFailedException werfen:
      if (errors != 0)
      {
        throw new WMCommandsFailedException(
          L.m(
            "Die verwendete Vorlage enthält %1 Fehler.\n\nBitte kontaktieren Sie Ihre Systemadministration.",
            ((errors == 1) ? "einen" : "" + errors)));
      }
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
  private static void pageStyleWollmuxseiteToStandard(XTextDocument doc)
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
}
