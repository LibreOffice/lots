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
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseListener;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.DocumentCommand.SetPrintFunction;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4000;

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
   * Ermöglicht den Zugriff auf einen Vector aller FormField-Objekte in diesem
   * TextDokument über den Namen der zugeordneten ID.
   */
  private HashMap idToFormFields;

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
   * Enthält das SetPrintFunction-Dokumentkommando falls in diesem Dokument eine
   * Druckfunktion definiert ist oder null, wenn keine Druckfunktion definiert
   * ist.
   */
  private DocumentCommand.SetPrintFunction printFunction;

  /**
   * Enthält das zu diesem TextDocumentModel zugehörige XPrintModel.
   */
  private XPrintModel printModel = new PrintModel();

  /**
   * Baum aller im Dokument enthaltenen Dokumentkommandos.
   */
  private DocumentCommandTree docCmdTree;

  /**
   * Enthält die Formularbeschreibung falls es sich bei dem Dokument um ein
   * Formular handelt und wird jedoch erst mit
   * DocumentCommandInterpreter.executeNormalCommands() gesetzt.
   */
  private FormDescriptor formDescriptor = null;

  /**
   * Enthält das setType-Dokumentkommando dieses Dokuments falls eines vorhanden
   * ist.
   */
  private DocumentCommand.SetType setTypeCommand = null;

  /**
   * Erzeugt ein neues TextDocumentModel zum XTextDocument doc und sollte nie
   * direkt aufgerufen werden, da neue TextDocumentModels über das
   * WollMuxSingletonie (WollMuxSingleton.getTextDocumentModel()) erzeugt und
   * verwaltet werden.
   * 
   * @param doc
   */
  public TextDocumentModel(XTextDocument doc)
  {
    this.doc = doc;
    this.idToFormFields = new HashMap();
    this.fragUrls = new String[] {};
    this.currentMax4000 = null;
    this.closeListener = null;
    this.printFunction = null;
    this.docCmdTree = new DocumentCommandTree(UNO.XBookmarksSupplier(doc));
  }

  /**
   * TextDocumentModels forwarden die hashCode-Methode an das referenzierte
   * XTextDocument weiter - so kann ein TextDocumentModel über eine HashMap
   * verwaltet werden.
   */
  public int hashCode()
  {
    if (doc != null) return doc.hashCode();
    return 0;
  }

  /**
   * Zwei TextDocumentModels sind dann gleich, wenn sie das selbe XTextDocument
   * doc referenzieren - so kann ein TextDocumentModel über eine HashMap
   * verwaltet werden. ACHTUNG: Die Gleichheit zweier TextDocumentModes
   * beschreibt nur die Gleichheit des verknüpften XTextDocuments, nicht jedoch
   * die Gleichheit aller Felder des TextDocumentModels! Zwei Instanzen, die
   * equals==true zurückliefern müssen also nicht unbedingt wirklich identisch
   * sein.
   */
  public boolean equals(Object b)
  {
    if (b != null && b instanceof TextDocumentModel)
    {
      TextDocumentModel other = (TextDocumentModel) b;
      return UnoRuntime.areSame(this.doc, other.doc);
    }
    return false;
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
   * Ermöglicht den Zugriff auf einen Vector aller FormField-Objekte in diesem
   * TextDokument über den Namen der zugeordneten ID.
   * 
   * @return Eine HashMap, die unter dem Schlüssel ID einen Vector mit den
   *         entsprechenden FormFields bereithält.
   */
  public HashMap getIDToFormFields()
  {
    return idToFormFields;
  }

  /**
   * Speichert das FormModel formModel im TextDocumentModel und wird derzeit vom
   * DocumentCommandInterpreter aufgerufen, wenn er ein FormModel erzeugt.
   * 
   * @param formModel
   */
  public void setFormModel(FormModel formModel)
  {
    this.formModel = formModel;
  }

  /**
   * Liefert das zuletzt per setFormModel() gesetzte FormModel zurück.
   * 
   * @return
   */
  public FormModel getFormModel()
  {
    return this.formModel;
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
   * Liefert den Baum der Dokumentkommandos zu diesem Dokuments in einem nicht
   * zwangsweise aktualisierten Zustand. Der Zustand kann über die
   * update()-Methode des zurückgegebenen DocumentCommandTrees aktualisiert
   * werden.
   * 
   * @return Baum der Dokumentkommandos dieses Dokuments.
   */
  public DocumentCommandTree getDocumentCommandTree()
  {
    return docCmdTree;
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
    if (setTypeCommand != null)
    {
      if (setTypeCommand.getType().equalsIgnoreCase("normalTemplate"))
        return true;
      else if (setTypeCommand.getType().equalsIgnoreCase("templateTemplate"))
        return false;
      else if (setTypeCommand.getType().equalsIgnoreCase("formDocument"))
        return false;
    }

    // Das Dokument ist automatisch eine Vorlage, wenn es keine zugehörige URL
    // gibt (dann steht ja in der Fensterüberschrift auch "Unbenannt1" statt
    // einem konkreten Dokumentnamen).
    return (doc.getURL() == null || doc.getURL().equals(""));
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
    return (setTypeCommand != null && setTypeCommand.getType()
        .equalsIgnoreCase("formDocument"));
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
    if (formDescriptor != null) return !formDescriptor.isEmpty();

    return false;
  }

  /**
   * Setzt das setTypeCommand dieses Dokuments, falls ein solches existiert.
   * 
   * @param setTypeCommand
   */
  public void setTypeCommand(DocumentCommand.SetType setTypeCommand)
  {
    this.setTypeCommand = setTypeCommand;
  }

  /**
   * Setzt die zu diesem Dokument zugehörige Formularbeschreibung auf
   * formDescriptor.
   * 
   * @param formDescriptor
   */
  public void setFormDescriptor(FormDescriptor formDescriptor)
  {
    this.formDescriptor = formDescriptor;
  }

  /**
   * Diese Methode setzt die diesem TextDocumentModel zugehörige Druckfunktion
   * auf den Wert functionName, der ein gültiger Funktionsbezeichner sein muss
   * oder löscht eine bereits gesetzte Druckfunktion, wenn functionName der
   * Leerstring ist.
   * 
   * TESTED
   * 
   * @param printFunctionName
   *          der Name der Druckfunktion (zum setzen) oder der Leerstring (zum
   *          löschen). Der zu setzende Name muss ein gültiger
   *          Funktionsbezeichner sein und in einem Abschnitt "Druckfunktionen"
   *          in der wollmux.conf definiert sein.
   */
  public void setPrintFunctionName(String printFunctionName)
  {
    if (printFunctionName == null || printFunctionName.equals(""))
    {
      if (printFunction != null)
      {
        // Bestehende Druckfunktion löschen wenn Funktionsname null oder leer.
        printFunction.setDoneState(true);
        printFunction.updateBookmark(false);
        printFunction = null;
      }
    }
    else if (printFunction == null)
    {
      // Neues Dokumentkommando anlegen wenn noch nicht definiert.
      printFunction = new SetPrintFunction(doc, printFunctionName);
    }
    else
    {
      // ansonsten den Namen auf printFunctionName ändern.
      printFunction.setFunctionName(printFunctionName);
    }
  }

  /**
   * Liefert den Namen der aktuellen Druckfunktion, falls das Dokument ein
   * entsprechendes Dokumentkomando enthält oder eine Druckfunktion mit
   * setPrintFunctionName()-Methode gesetz wurde; ist keine Druckfunktion
   * definiert, so wird null zurück geliefert.
   */
  public String getPrintFunctionName()
  {
    if (printFunction != null) return printFunction.getFunctionName();
    return null;
  }

  /**
   * Wird vom DocumentCommandInterpreter beim parsen des Dokumentkommandobaumes
   * aufgerufen, wenn das Dokument ein setPrintFunction-Kommando enthält - das
   * entsprechende Kommando cmd wird im Model abgespeichert und die relevante
   * Information kann später über getPrintFunctionName() erfragt werden.
   * 
   * @param cmd
   *          Das gefundene setPrintFunction-Dokumentkommando.
   */
  public void setPrintFunction(SetPrintFunction cmd)
  {
    printFunction = cmd;
  }

  /**
   * Setzt das Fensters des TextDokuments auf Sichtbar (visible==true) oder
   * unsichtbar (visible == false).
   * 
   * @param visible
   */
  public void setWindowVisible(boolean visible)
  {
    XModel xModel = UNO.XModel(doc);
    if (xModel != null)
    {
      XFrame frame = xModel.getCurrentController().getFrame();
      if (frame != null)
      {
        frame.getContainerWindow().setVisible(visible);
      }
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
      UNO.XModel(doc).getCurrentController().getFrame().getContainerWindow()
          .setPosSize(docX, docY, docWidth, docHeight, PosSize.POSSIZE);
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
      window = UNO.XModel(doc).getCurrentController().getFrame()
          .getContainerWindow();
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
   * Schliesst das XTextDocument, das diesem Model zugeordnet ist. Ist der
   * closeListener registriert (was WollMuxSingleton bereits bei der Erstellung
   * des TextDocumentModels standardmäig macht), so wird nach dem close() auch
   * automatisch die dispose()-Methode aufgerufen.
   */
  public void close()
  {
    // Damit OOo vor dem Schließen eines veränderten Dokuments den
    // save/dismiss-Dialog anzeigt, muss die suspend()-Methode aller
    // XController gestartet werden, die das Model der Komponente enthalten.
    // Man bekommt alle XController über die Frames, die der Desktop liefert.
    Object desktop = UNO.createUNOService("com.sun.star.frame.Desktop");
    if (UNO.XFramesSupplier(desktop) != null)
    {
      XFrame[] frames = UNO.XFramesSupplier(desktop).getFrames().queryFrames(
          FrameSearchFlag.ALL);
      for (int i = 0; i < frames.length; i++)
      {
        XController c = frames[i].getController();
        if (c != null && UnoRuntime.areSame(c.getModel(), doc))
          c.suspend(true);
      }
    }

    // Hier das eigentliche Schließen:
    try
    {
      if (UNO.XCloseable(doc) != null) UNO.XCloseable(doc).close(true);
    }
    catch (CloseVetoException e)
    {
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

    if (formModel != null) formModel.dispose();
    formModel = null;

    // Löscht das TextDocumentModel aus dem WollMux-Singleton.
    WollMuxSingleton.getInstance().disposedTextDocumentModel(this);
  }

  /**
   * Registriert genau einen XCloseListener in der Komponente des
   * XTextDocuments, so dass beim Schließen des Dokuments die entsprechenden
   * WollMuxEvents ausgeführt werden - ist in diesem TextDocumentModel bereits
   * ein XCloseListener registriert, so wird nichts getan.
   */
  public void registerCloseListener()
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
  public class PrintModel implements XPrintModel
  {
    /**
     * Das lock-Flag, das vor dem Einstellen eines WollMuxEvents auf true
     * gesetzt werden muss und signalisiert, ob das WollMuxEvent erfolgreich
     * abgearbeitet wurde.
     */
    private boolean[] lock = new boolean[] { true };

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#print(short)
     */
    public void print(short numberOfCopies)
    {
      setLock();
      WollMuxEventHandler.handleDoPrint(
          doc,
          numberOfCopies,
          unlockActionListener);
      waitForUnlock();
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.XPrintModel#getTextDocument()
     */
    public XTextDocument getTextDocument()
    {
      return doc;
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
      lock[0] = true;
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
