package de.muenchen.allg.itd51.wollmux.dialog.formmodel;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XFrame;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeDatasource;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von den
 * dahinterstehenden OOo-Objekten. ACHTUNG! Der FormController ruft die Methoden dieser Klasse aus
 * dem Event Dispatching Thread auf. Dort dürfen sie aber meist nicht laufen. Deshalb müssen alle
 * entsprechenden Methoden über den WollMuxEventHandler ein Event Objekt erzeugen und in die
 * WollMux-Queue zur späteren Ausführung schieben. Es muss dafür gesorgt werden, dass das FormModel
 * Objekt auch funktioniert, wenn das zugrundeliegende Office-Dokument disposed wurde, da der
 * FormController evtl. im Moment des disposens darauf zugreifen möchte. Hoffentlich löst obiges
 * Umsetzen der Aufrufe in Event-Objekte dieses Problem schon weitgehend.
 */
public class SingleDocumentFormModel
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(SingleDocumentFormModel.class);

  private final TextDocumentController documentController;

  private final ConfigThingy formFensterConf;

  private final ConfigThingy formConf;

  private final Map<Object, Object> functionContext;

  private final FunctionLibrary funcLib;

  private final DialogLibrary dialogLib;

  private final boolean visible;

  private final String defaultWindowAttributes;

  private FormGUI formGUI = null;

  /**
   * vFormGUIs beinhaltet die Referenzen der z.Z. ausgeführten FormGUIs
   */
  public static List<FormGUI> vFormGUIs = new ArrayList<>();

  /**
   * vFrames beinhaltet die Referenzen der z.Z. ausgeführten Frames mit FormGUI
   */
  public static List<XFrame> vFrames = new ArrayList<>();

  /**
   * Konstruktor für ein SingleDocumentFormModel mit dem zugehörigen
   * TextDocumentModel doc.
   *
   * @param documentController
   *          Das zugeordnete TextDocumentModel.
   * @param formFensterConf
   *          Der Formular-Unterabschnitt des Fenster-Abschnitts von
   *          wollmux.conf (wird für createFormGUI() benötigt).
   * @param formConf
   *          der Formular-Knoten, der die Formularbeschreibung enthält (wird
   *          für createFormGUI() benötigt).
   * @param functionContext
   *          der Kontext für Funktionen, die einen benötigen (wird für
   *          createFormGUI() benötigt).
   * @param funcLib
   *          die Funktionsbibliothek, die zur Auswertung von Trafos, Plausis
   *          etc. herangezogen werden soll.
   * @param dialogLib
   *          die Dialogbibliothek, die die Dialoge bereitstellt, die für
   *          automatisch zu befüllende Formularfelder benötigt werden (wird für
   *          createFormGUI() benötigt).
   * @param visible
   *          false zeigt an, dass die FormGUI unsichtbar sein soll.
   */
  public SingleDocumentFormModel(
      final TextDocumentController documentController,
      final ConfigThingy formFensterConf, final ConfigThingy formConf,
      final Map<Object, Object> functionContext, final FunctionLibrary funcLib,
      final DialogLibrary dialogLib, boolean visible)
  {
    this.documentController = documentController;
    this.formFensterConf = formFensterConf;
    this.formConf = formConf;
    this.functionContext = functionContext;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;
    this.visible = visible;

    // Standard-Fensterattribute vor dem Start der Form-GUI sichern um nach
    // dem Schließen des Formulardokuments die Standard-Werte wieder
    // herstellen zu können. Die Standard-Attribute ändern sich (OOo-seitig)
    // immer dann, wenn ein Dokument (mitsamt Fenster) geschlossen wird. Dann
    // merkt sich OOo die Position und Größe des zuletzt geschlossenen
    // Fensters.
    if (visible)
      this.defaultWindowAttributes = getDefaultWindowAttributes();
    else
      this.defaultWindowAttributes = null;
  }

  /**
   * Versucht das Dokument zu schließen. Wurde das Dokument verändert (Modified-Status des
   * Dokuments==true), so erscheint der Dialog "Speichern"/"Verwerfen"/"Abbrechen" (über den ein
   * sofortiges Schließen des Dokuments durch den Benutzer verhindert werden kann)
   */
  public void close()
  {
    WollMuxEventHandler.handleCloseTextDocument(documentController);
  }

  /**
   * Speichert dieses Formular in eine temporäre Datei unter Verwendung des in in ExterneAnwendungen
   * für ext festgelegten FILTERs, startet dann die zugehörige externe Anwendung mit dieser Datei
   * und schließt das Formular.
   */
  public void closeAndOpenExt(String ext)
  {
    WollMuxEventHandler.handleCloseAndOpenExt(documentController, ext);
  }

  /**
   * Speichert dieses Formular in eine temporäre Datei unter Verwendung des in in ExterneAnwendungen
   * für ext festgelegten FILTERs und startet dann die zugehörige externe Anwendung mit dieser
   * Datei.
   */
  public void saveTempAndOpenExt(String ext)
  {
    WollMuxEventHandler.handleSaveTempAndOpenExt(documentController, ext);
  }

  /**
   * Setzt den Sichtbarkeitsstatus des Fensters des zugehörigen Dokuments auf vis (true=sichtbar,
   * false=unsichtbar).
   * 
   * @param vis
   *          true=sichtbar, false=unsichtbar
   */
  public void setWindowVisible(boolean vis)
  {
    /*
     * Einmal unsichtbar, immer unsichtbar. Weiß nicht, ob das sinnvoll ist,
     * aber die ganze Methode wird soweit ich sehen kann derzeit nicht
     * verwendet, also ist es egal. Falls es hier erlaubt wird, das Fenster
     * sichtbar zu schalten, dann müsste noch einiges anderes geändert werden,
     * z.B. müsste die FormGUI sichtbar werden.
     */
    if (visible)
      WollMuxEventHandler.handleSetWindowVisible(documentController, vis);
  }

  /**
   * Setzt die Position und Größe des Fensters des zugehörigen Dokuments auf die vorgegebenen Werte
   * setzt. ACHTUNG: Die Maßangaben beziehen sich auf die linke obere Ecke des Fensterinhalts OHNE
   * die Titelzeile und die Fensterdekoration des Rahmens. Um die linke obere Ecke des gesamten
   * Fensters richtig zu setzen, müssen die Größenangaben des Randes der Fensterdekoration und die
   * Höhe der Titelzeile VOR dem Aufruf der Methode entsprechend eingerechnet werden.
   * 
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position in Pixel, gezählt
   *          von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gezählt von links oben.
   * @param docWidth
   *          Die Größe des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Größe des Dokuments auf der Y-Achse in Pixel. Auch hier wird die Titelzeile des
   *          Rahmens nicht beachtet und muss vorher entsprechend eingerechnet werden.
   */
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
  {
    if (visible)
      WollMuxEventHandler.handleSetWindowPosSize(documentController, docX, docY,
          docWidth, docHeight);
  }

  /**
   * Setzt den Sichtbarkeitsstatus der Sichtbarkeitsgruppe mit der ID groupID auf visible.
   * 
   * @param groupId
   *          Die ID der Gruppe, die Sichtbar/unsichtbar geschalten werden soll.
   * @param visible
   *          true==sichtbar, false==unsichtbar
   */
  public void setVisibleState(String groupId, boolean visible)
  {
    WollMuxEventHandler.handleSetVisibleState(documentController, groupId,
        visible, null);
  }

  /**
   * Setzt den Wert aller Formularfelder im Dokument, die von fieldId abhängen auf den neuen Wert
   * newValue (bzw. auf das Ergebnis der zu diesem Formularelement hinterlegten Trafo-Funktion).
   * 
   * Es ist nicht garantiert, dass sich der Wert tatsächlich geändert hat. Die fieldId kann leer
   * sein (aber nie null).
   */
  public void valueChanged(String fieldId, String newValue)
  {
    if (fieldId.length() > 0)
      WollMuxEventHandler.handleFormValueChanged(documentController, fieldId,
          newValue);
  }

  /**
   * Das Formularfeld im Dokument mit der ID fieldId erhält den Fokus. Gibt es im Dokument mehrere
   * Formularfelder, die von der ID abhängen, so erhält immer das erste Formularfeld den Fokus -
   * bevorzugt werden dabei auch die nicht transformierten Formularfelder.
   * 
   * @param fieldId
   *          id des Formularfeldes, das den Fokus bekommen soll.
   */
  public void focusGained(String fieldId)
  {
    if (visible)
      WollMuxEventHandler.handleFocusFormField(documentController, fieldId);
  }

  /**
   * Not Yet Implemented: Nimmt dem Formularfeld mit der ID fieldId den Fokus wieder weg - ergibt
   * aber bisher keinen Sinn.
   * 
   * @param fieldId
   */
  public void focusLost(String fieldId)
  {
  }

  /**
   * Informiert das FormModel, dass das zugrundeliegende Dokument source geschlossen wird und das
   * FormModel entsprechend handeln soll um sicherzustellen, dass das Dokument in Zukunft nicht mehr
   * angesprochen wird.
   * 
   * Abhängig von der Implementierung des FormModels werden unterschiedliche Aktionen erledigt. Dazu
   * gehören z.B. das Beenden einer bereits gestarteten FormGUI oder das Wiederherstellen der
   * Fensterattribute des Dokumentfensters auf die Werte, die das Fenster vor dem Starten der
   * FormGUI hatte.
   * 
   * @param source
   *          Das Dokument das geschlossen wurde.
   */
  public void closing(Object sender)
  {
    if (documentController.getModel().doc.equals(sender))
    {
      if (formGUI != null)
      {
        formGUI.dispose();
        formGUI = null;
      }

      // Rücksetzen des defaultWindowAttributes auf den Wert vor dem Schließen
      // des Formulardokuments.
      if (defaultWindowAttributes != null)
        setDefaultWindowAttributes(defaultWindowAttributes);
    }
  }

  /**
   * Diese Hilfsmethode liest das Attribut ooSetupFactoryWindowAttributes aus
   * dem Konfigurationsknoten
   * "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument" der
   * OOo-Konfiguration, welches die Standard-FensterAttribute enthält, mit denen
   * neue Fenster für TextDokumente erzeugt werden.
   *
   * @return
   */
  private static String getDefaultWindowAttributes()
  {
    try
    {
      Object cp = UNO
          .createUNOService("com.sun.star.configuration.ConfigurationProvider");

      // creation arguments: nodepath
      com.sun.star.beans.PropertyValue aPathArgument = new com.sun.star.beans.PropertyValue();
      aPathArgument.Name = "nodepath";
      aPathArgument.Value = "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument";
      Object[] aArguments = new Object[1];
      aArguments[0] = aPathArgument;

      Object ca = UNO.XMultiServiceFactory(cp).createInstanceWithArguments(
          "com.sun.star.configuration.ConfigurationAccess", aArguments);

      return UNO.getProperty(ca, "ooSetupFactoryWindowAttributes").toString();
    } catch (java.lang.Exception e)
    {
    }
    return null;
  }

  /**
   * Diese Hilfsmethode setzt das Attribut ooSetupFactoryWindowAttributes aus
   * dem Konfigurationsknoten
   * "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument" der
   * OOo-Konfiguration auf den neuen Wert value, der (am besten) über einen
   * vorhergehenden Aufruf von getDefaultWindowAttributes() gewonnen wird.
   *
   * @param value
   */
  private static void setDefaultWindowAttributes(String value)
  {
    try
    {
      Object cp = UNO
          .createUNOService("com.sun.star.configuration.ConfigurationProvider");

      // creation arguments: nodepath
      com.sun.star.beans.PropertyValue aPathArgument = new com.sun.star.beans.PropertyValue();
      aPathArgument.Name = "nodepath";
      aPathArgument.Value = "/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument";
      Object[] aArguments = new Object[1];
      aArguments[0] = aPathArgument;

      Object ca = UNO.XMultiServiceFactory(cp).createInstanceWithArguments(
          "com.sun.star.configuration.ConfigurationUpdateAccess", aArguments);

      UNO.setProperty(ca, "ooSetupFactoryWindowAttributes", value);

      UNO.XChangesBatch(ca).commitChanges();
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Über diese Methode kann der FormController das FormModel informieren, dass er vollständig
   * initialisiert wurde und notwendige Aktionen wie z.B. das zurücksetzen des modified-Status des
   * Dokuments durchgeführt werden sollen.
   */
  public void formControllerInitCompleted()
  {
    WollMuxEventHandler.handleFormControllerInitCompleted(documentController);
  }

  /**
   * Startet den Ausdruck unter Verwendung eventuell vorhandener Komfortdruckfunktionen.
   */
  public void print()
  {
    UNO.dispatch(documentController.getModel().doc, Dispatch.DISP_unoPrint);
  }

  /**
   * Exportiert das Dokument als PDF. Bei Multi-Form wird die Aktion für alle Formulare der Reihe
   * nach aufgerufen.
   */
  public void pdf()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:ExportToPDF");
  }

  /**
   * Speichert das Dokument (Datei/Speichern). Bei Multi-Form wird die Aktion für alle Formulare der
   * Reihe nach aufgerufen.
   */
  public void save()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:Save");
  }

  /**
   * Speichert das Dokument (Datei/Speichern unter...). Bei Multi-Form wird die Aktion für alle
   * Formulare der Reihe nach aufgerufen.
   */
  public void saveAs()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:SaveAs");
  }

  /**
   * Teilt der FormGUI die zu diesem FormModel gehört mit, dass der Wert des Formularfeldes mit der
   * id fieldId auf den neuen Wert value gesetzt werden soll und ruft nach erfolgreicher aktion die
   * Methode actionPerformed(ActionEvent arg0) des Listeners listener.
   * 
   * @param fieldId
   *          die Id des Feldes das in der FormGUI auf den neuen Wert value gesetzt werden soll.
   * @param value
   *          der neue Wert value.
   * @param listener
   *          der Listener der informiert wird, nachdem der Wert erfolgreich gesetzt wurde.
   */
  public void setValue(String fieldId, String value, ActionListener listener)
  {
    if (formGUI != null)
      formGUI.getController().setValue(fieldId, value, listener);
  }

  /**
   * Erzeugt eine FormGUI zu diesem FormModel und startet diese.
   */
  public void startFormGUI()
  {
    boolean containsFrames = false;
    containsFrames = SingleDocumentFormModel.vFrames
        .contains(documentController.getFrameController().getFrame());

    // Schaue ob bereits eine Instanz genau dieses Formulars geöffnet ist, falls
    // ja wird das nun ungültige FormGUI beendet
    if (containsFrames)
    {
      // Hole Index des ungültigen FormGUI
      int frameIndex = SingleDocumentFormModel.vFrames
          .indexOf(documentController.getFrameController().getFrame());

      SingleDocumentFormModel.vFormGUIs.get(frameIndex).dispose();
      SingleDocumentFormModel.vFormGUIs.remove(frameIndex);
      SingleDocumentFormModel.vFrames.remove(frameIndex);
      LOGGER.debug(L.m("FormGUI an der Stelle %1 beendet.", frameIndex));
    }

    Map<String, String> idToPresetValue = documentController
        .getIDToPresetValue();
    formGUI = new FormGUI(formFensterConf, formConf, this, idToPresetValue,
        functionContext, funcLib, dialogLib, visible);

    // füge FormGUI Refenrenz und die dazugehörigen Frames zu den
    // Klassenvariable hinzu
    SingleDocumentFormModel.vFormGUIs.add(formGUI);
    SingleDocumentFormModel.vFrames
        .add(documentController.getFrameController().getFrame());
  }

  /**
   * Liefert den Titel des zum FormModel gehörenden Fensters oder null, falls kein Titel bekannt
   * oder nicht anwendbar (z.B. Multi-Form).
   */
  public String getWindowTitle()
  {
    try
    {
      XFrame frame = UNO.XModel(documentController.getModel().doc)
          .getCurrentController().getFrame();
      String frameTitle = (String) UNO.getProperty(frame, "Title");
      frameTitle = MailMergeDatasource
          .stripOpenOfficeFromWindowName(frameTitle);
      return frameTitle;
    } catch (Exception x)
    {
      return null;
    }
  }

  /**
   * Öffnet durch ACTION-Event ein neues Dokument oder Template. Durch Angabe der FragID wird die
   * entsprechende Vorlage zugeordnet.
   */
  public void openTemplateOrDocument(List<String> fragIds)
  {
    WollMuxEventHandler.handleOpenDocument(fragIds, false);
  }

  /**
   * Sendet Dokument als Anhang über Standardbuttons in FormularMax.
   */
  public void sendAsEmail()
  {
    UNO.dispatch(documentController.getModel().doc, ".uno:SendMail");
  }
}