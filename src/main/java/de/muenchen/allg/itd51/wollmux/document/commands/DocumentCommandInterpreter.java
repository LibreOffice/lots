package de.muenchen.allg.itd51.wollmux.document.commands;

import java.util.HashSet;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.container.XNameContainer;
import com.sun.star.style.XStyle;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.document.text.StyleService;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.WMCommandsFailedException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockProcessor;

/**
 * Diese Klasse repräsentiert den Kommando-Interpreter zur Auswertung von
 * WollMux-Kommandos in einem gegebenen Textdokument.
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class DocumentCommandInterpreter
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DocumentCommandInterpreter.class);

  private TextDocumentController documentController;

  /**
   * Enthält die Instanz auf das zentrale WollMuxSingleton.
   */
  boolean debugMode;

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle Dokumentkommandos im
   * übergebenen Dokument xDoc scannen und interpretieren kann.
   *
   * @param documentController
   * @param debugMode
   */
  public DocumentCommandInterpreter(TextDocumentController documentController, boolean debugMode)
  {
    this.documentController = documentController;
    this.debugMode = debugMode;
  }

  /**
   * Der Konstruktor erzeugt einen neuen Kommandointerpreter, der alle Dokumentkommandos im
   * übergebenen Dokument xDoc scannen und interpretieren kann.
   *
   * @param documentController
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
    LOGGER.debug("scanGlobalDocumentCommands");
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
   * Diese Methode scannt alle insertFormValue-Kommandos des Dokuments, verarbeitet diese und reicht
   * das gefundene Mapping von IDs zu FormFields an das TextDocumentModel weiter. Zudem wird von
   * dieser Methode auch noch {@link TextDocumentController#collectNonWollMuxFormFields()}
   * aufgerufen, so dass auch alle Formularfelder aufgesammelt werden, die nicht von
   * WollMux-Kommandos umgeben sind, jedoch trotzdem vom WollMux verstanden und befüllt werden.
   *
   * Diese Methode wurde aus der Methode {@link #scanGlobalDocumentCommands()} ausgelagert, die
   * früher neben den globalen Dokumentkommandos auch die insertFormValue-Kommandos bearbeitet hat.
   * Die Auslagerung geschah hauptsächlich aus Performance-Optimierungsgründen, da so beim
   * OnProcessTextDocument-Event nur einmal die insertFormValue-Kommandos ausgewertet werden müssen.
   */
  public void scanInsertFormValueCommands()
  {
    LOGGER.debug("scanInsertFormValueCommands");
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
      LOGGER.debug("executeTemplateCommands");
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
      ContentBasedDirectiveModel.createModel(documentController).adoptNumbers();

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
      errors = new PrintBlockProcessor()
          .execute(getDocumentController().getModel().getDocumentCommands());

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
    XStyle styleWollmuxseite = null;
    XStyle styleStandard = null;
    // Holt die Seitenvorlage Wollmuxseite und Standard
    try
    {
      XNameContainer nameContainer = StyleService.getStyleContainer(doc, StyleService.PAGE_STYLES);
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
      HashSet<String> set = new HashSet<>();
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
          Object value = Utils.getProperty(styleWollmuxseite, element);
          Object checkset = Utils.setProperty(styleStandard, element, value);
          if (UnoRuntime.areSame(value, checkset))
          {
            iter.remove();
          }
        }
      } while (size != set.size());
    }
  }
}
