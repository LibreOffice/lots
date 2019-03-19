/*
 * Dateiname: WollMux.java
 * Projekt  : WollMux
 * Funktion : zentraler UNO-Service WollMux 
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux über wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n                 |  
 * 06.06.2006 | LUT | + Ablösung der Event-Klasse durch saubere Objektstruktur
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.comp;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.loader.FactoryHelper;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XIndexContainer;
import com.sun.star.document.XEventListener;
import com.sun.star.form.FormButtonType;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XWollMux;
import de.muenchen.allg.itd51.wollmux.XWollMuxDocument;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;
import de.muenchen.allg.itd51.wollmux.event.DispatchProviderAndInterceptor;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.sidebar.SeriendruckSidebarFactory;
import de.muenchen.allg.itd51.wollmux.sidebar.WollMuxSidebarFactory;


/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service hat
 * folgende Funktionen: Als XDispatchProvider und XDispatch behandelt er alle
 * "wollmux:kommando..." URLs und als XWollMux stellt er die Schnittstelle für
 * externe UNO-Komponenten dar. Der Service wird beim Starten von Office
 * automatisch (mehrfach) instanziiert, wenn OOo einen dispatchprovider für die in
 * der Datei Addons.xcu enthaltenen wollmux:... dispatches besorgen möchte (dies
 * geschieht auch bei unsichtbar geöffneten Dokumenten). Als Folge wird das
 * WollMux-Singleton bei OOo-Start (einmalig) initialisiert.
 */
public class WollMux extends WeakBase implements XServiceInfo, XDispatchProvider,
    XWollMux
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMux.class);

  /**
   * Dieses Feld entält eine Liste aller Services, die dieser UNO-Service
   * implementiert.
   */
  private static final java.lang.String[] SERVICENAMES =
    { "de.muenchen.allg.itd51.wollmux.WollMux" };

  /**
   * Der Konstruktor initialisiert das WollMuxSingleton und startet damit den
   * eigentlichen WollMux. Der Konstuktor wird aufgerufen, bevor Office die
   * Methode executeAsync() aufrufen kann, die bei einem ON_FIRST_VISIBLE_TASK-Event
   * über den Job-Mechanismus ausgeführt wird.
   * 
   * @param context
   */
  public WollMux(XComponentContext ctx)
  {
    WollMuxSingleton.initialize(ctx);
    
    if (!WollMuxSingleton.getInstance().isMenusCreated())
    {
      createMenuItems();
      WollMuxSingleton.getInstance().setMenusCreated(true);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
   */
  @Override
  public String[] getSupportedServiceNames()
  {
    return SERVICENAMES;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
   */
  @Override
  public boolean supportsService(String sService)
  {
    int len = SERVICENAMES.length;
    for (int i = 0; i < len; i++)
    {
      if (sService.equals(SERVICENAMES[i])) return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getImplementationName()
   */
  @Override
  public String getImplementationName()
  {
    return (WollMux.class.getName());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
   * java.lang.String, int)
   */
  @Override
  public XDispatch queryDispatch( /* IN */com.sun.star.util.URL aURL,
  /* IN */String sTargetFrameName,
  /* IN */int iSearchFlags)
  {
    return DispatchProviderAndInterceptor.globalWollMuxDispatches.queryDispatch(
      aURL, sTargetFrameName, iSearchFlags);
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.
   * DispatchDescriptor[])
   */
  @Override
  public XDispatch[] queryDispatches( /* IN */DispatchDescriptor[] seqDescripts)
  {
    return DispatchProviderAndInterceptor.globalWollMuxDispatches.queryDispatches(seqDescripts);
  }

  /**
   * Diese Methode liefert eine Factory zurück, die in der Lage ist den UNO-Service
   * zu erzeugen. Die Methode wird von UNO intern benötigt. Die Methoden
   * __getComponentFactory und __writeRegistryServiceInfo stellen das Herzstück des
   * UNO-Service dar.
   * 
   * @param sImplName
   * @return
   */
  public synchronized static XSingleComponentFactory __getComponentFactory(
      java.lang.String sImplName)
  {
    com.sun.star.lang.XSingleComponentFactory xFactory = null;
    if (sImplName.equals(WollMux.class.getName()))
      xFactory = Factory.createComponentFactory(WollMux.class, SERVICENAMES);

    if (sImplName.equals(WollMuxSidebarFactory.class.getName()))
      xFactory =
        Factory.createComponentFactory(WollMuxSidebarFactory.class,
          new String[] { WollMuxSidebarFactory.__serviceName });

    if (sImplName.equals(SeriendruckSidebarFactory.class.getName()))
      xFactory = Factory.createComponentFactory(SeriendruckSidebarFactory.class,
          new String[] { SeriendruckSidebarFactory.__serviceName });

    return xFactory;
  }

  /**
   * Diese Methode registriert den UNO-Service. Sie wird z.B. beim unopkg-add im
   * Hintergrund aufgerufen. Die Methoden __getComponentFactory und
   * __writeRegistryServiceInfo stellen das Herzstück des UNO-Service dar.
   * 
   * @param xRegKey
   * @return
   */
  public synchronized static boolean __writeRegistryServiceInfo(XRegistryKey xRegKey)
  {
    try
    {
      FactoryHelper.writeRegistryServiceInfo(WollMuxSidebarFactory.class.getName(),
        WollMuxSidebarFactory.__serviceName, xRegKey);

      FactoryHelper.writeRegistryServiceInfo(SeriendruckSidebarFactory.class.getName(),
          SeriendruckSidebarFactory.__serviceName, xRegKey);

      return Factory.writeRegistryServiceInfo(WollMux.class.getName(),
        WollMux.SERVICENAMES, xRegKey);
    }
    catch (Throwable t)
    {
      // Es ist besser hier alles zu fangen was fliegt und es auf stderr auszugeben.
      // So kann man z.B. mit "unopkg add <paketname>" immer gleich sehen, warum sich
      // die Extension nicht installieren lässt. Fängt man hier nicht, erzeugt
      // "unopkg add" eine unverständliche Fehlerausgabe und man sucht lange nach der
      // Ursache. Wir hatten bei java-Extensions vor allem schon Probleme mit
      // verschiedenen OOo/LO-Versionen, die wir erst finden konnten, als wir die
      // Exception ausgegeben haben. Die Logger-Klasse möchte ich hier für die
      // Ausgabe nicht verwenden weil dies ein Problem während der Installation und
      // nicht während der Laufzeit ist.
      t.printStackTrace();
      return false;
    }
  }
  
  public synchronized static XSingleServiceFactory __getServiceFactory(
      final String sImplementationName,
      final XMultiServiceFactory xFactory,
      final XRegistryKey xKey)
  {
      XSingleServiceFactory xResult = null;
      if (sImplementationName.equals(WollMuxSidebarFactory.class.getName()))
      {
        xResult = FactoryHelper.getServiceFactory(
                WollMuxSidebarFactory.class,
                WollMuxSidebarFactory.__serviceName,
                xFactory,
                xKey);
    }

    if (sImplementationName.equals(SeriendruckSidebarFactory.class.getName()))
    {
      xResult = FactoryHelper.getServiceFactory(SeriendruckSidebarFactory.class,
          SeriendruckSidebarFactory.__serviceName, xFactory, xKey);
      }
      
      return xResult;
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empfängt
   * wenn sich die PAL ändert. Nach dem Registrieren wird sofort ein
   * ON_SELECTION_CHANGED Ereignis ausgelöst, welches dafür sort, dass sofort ein
   * erster update aller Listener ausgeführt wird. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#addPALChangeEventListener(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener)
   */
  @Override
  public void addPALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxEventHandler.getInstance().handleAddPALChangeEventListener(l, null);
  }

  /**
   * Diese Methode registriert einen XPALChangeEventListener, der updates empfängt
   * wenn sich die PAL ändert; nach der Registrierung wird geprüft, ob der WollMux
   * und der XPALChangeEventListener die selbe WollMux-Konfiguration verwenden, wozu
   * der Listener den HashCode wollmuxConfHashCode der aktuellen
   * WollMux-Konfiguration übermittelt. Stimmt wollmuxConfHashCode nicht mit dem
   * HashCode der WollMux-Konfiguration des WollMux überein, so erscheint ein Dialog,
   * der vor möglichen Fehlern warnt. Nach dem Registrieren wird sofort ein
   * ON_SELECTION_CHANGED Ereignis ausgelöst, welches dafür sort, dass sofort ein
   * erster update aller Listener ausgeführt wird. Die Methode ignoriert alle
   * XPALChangeEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * @param l
   *          Der zu registrierende XPALChangeEventListener
   * @param wollmuxConfHashCode
   *          Der HashCode der WollMux-Config der zur Konsistenzprüfung herangezogen
   *          wird und über
   *          WollMuxFiles.getWollMuxConf().getStringRepresentation().hashCode()
   *          erzeugt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#addPALChangeEventListenerWithConsistencyCheck(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener,
   *      int)
   */
  @Override
  public void addPALChangeEventListenerWithConsistencyCheck(
      XPALChangeEventListener l, int wollmuxConfHashCode)
  {
    WollMuxEventHandler.getInstance().handleAddPALChangeEventListener(l,
      Integer.valueOf(wollmuxConfHashCode));
  }

  /**
   * Diese Methode registriert einen Listener im WollMux, über den der WollMux über
   * den Status der Dokumentbearbeitung informiert (z.B. wenn ein Dokument
   * vollständig bearbeitet/expandiert wurde). Die Methode ignoriert alle
   * XEventListenener-Instanzen, die bereits registriert wurden.
   * Mehrfachregistrierung der selben Instanz ist also nicht möglich.
   * 
   * Tritt ein entstprechendes Ereignis ein, so erfolgt der Aufruf der entsprechenden
   * Methoden XEventListener.notifyEvent(...) immer gleichzeitig (d.h. für jeden
   * Listener in einem eigenen Thread).
   * 
   * Der WollMux liefert derzeit folgende Events:
   * 
   * OnWollMuxProcessingFinished: Dieses Event wird erzeugt, wenn ein Textdokument
   * nach dem Öffnen vollständig vom WollMux bearbeitet und expandiert wurde oder bei
   * allen anderen Dokumenttypen direkt nach dem Öffnen. D.h. für jedes in OOo
   * geöffnete Dokument erfolgt früher oder später ein solches Event.
   * 
   * @param l
   *          Der XEventListener, der bei Statusänderungen der Dokumentbearbeitung
   *          informiert werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see com.sun.star.document.XEventBroadcaster#addEventListener(com.sun.star.document.XEventListener)
   */
  @Override
  public void addEventListener(XEventListener l)
  {
    WollMuxEventHandler.getInstance().handleAddDocumentEventListener(l);
  }

  /**
   * Diese Methode deregistriert einen XPALChangeEventListener wenn er bereits
   * registriert war.
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#removePALChangeEventListener(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener)
   */
  @Override
  public void removePALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxEventHandler.getInstance().handleRemovePALChangeEventListener(l);
  }

  /**
   * Diese Methode deregistriert einen mit registerEventListener(XEventListener l)
   * registrierten XEventListener.
   * 
   * @param l
   *          der XEventListener, der deregistriert werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @see com.sun.star.document.XEventBroadcaster#removeEventListener(com.sun.star.document.XEventListener)
   */
  @Override
  public void removeEventListener(XEventListener l)
  {
    WollMuxEventHandler.getInstance().handleRemoveDocumentEventListener(l);
  }

  /**
   * Diese Methode setzt den aktuellen Absender der Persönlichen Absenderliste (PAL)
   * auf den Absender sender. Der Absender wird nur gesetzt, wenn die Parameter
   * sender und idx in der alphabetisch sortierten Absenderliste des WollMux
   * übereinstimmen - d.h. die Absenderliste der veranlassenden SenderBox zum
   * Zeitpunkt der Auswahl konsistent zur PAL des WollMux war. Die Methode erwartet
   * für sender das selben Format wie es von {@link XPALProvider.getCurrentSender()}
   * bzw. {@link XPALProvider.getPALEntries()} geliefert wird.
   */
  @Override
  public void setCurrentSender(String sender, short idx)
  {
    LOGGER.trace("WollMux.setCurrentSender(\"" + sender + "\", " + idx + ")");
    WollMuxEventHandler.getInstance().handleSetSender(sender, idx);
  }

  /**
   * Liefert die zum aktuellen Zeitpunkt im WollMux ausgewählten Absenderdaten (die
   * über das Dokumentkommandos WM(CMD'insertValue' DB_SPALTE'<dbSpalte>') in ein
   * Dokument eingefügt würden) in einem Array von {@link PropertyValue}-Objekten
   * zurück. Dabei repräsentieren die Attribute {@link PropertyValue.Name} die
   * verfügbaren DB_SPALTEn und die Attribute {@link PropertyValue.Value} die zu
   * DB_SPALTE zugehörigen Absenderdaten.
   * 
   * Jeder Aufruf erzeugt ein komplett neues und unabhängiges Objekt mit allen
   * Einträgen die zu dem Zeitpunkt gültig sind. Eine Änderung der Werte des
   * Rückgabeobjekts hat daher keine Auswirkung auf den WollMux.
   * 
   * @return Array von PropertyValue-Objekten mit den aktuell im WollMux gesetzten
   *         Absenderdaten. Gibt es keine Absenderdaten, so ist das Array leer (aber
   *         != null).
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  @Override
  public PropertyValue[] getInsertValues()
  {
    // Diese Methode nimmt keine Synchronisierung über den WollMuxEventHandler vor,
    // da das reine Auslesen der Datenstrukturen unkritisch ist.
    DatasourceJoiner dj = DatasourceJoinerFactory.getDatasourceJoiner();
    UnoProps p = new UnoProps();
    try
    {
      DJDataset ds = dj.getSelectedDataset();
      for (String key : dj.getMainDatasourceSchema())
      {
        String val;
        try
        {
          val = ds.get(key);
          if (val != null) p.setPropertyValue(key, val);
        }
        catch (ColumnNotFoundException x1)
        {}
      }
    }
    catch (DatasetNotFoundException x)
    {}
    return p.getProps();
  }

  /**
   * Diese Methode liefert den Wert der Absenderdaten zur Datenbankspalte dbSpalte,
   * der dem Wert entspricht, den das Dokumentkommando WM(CMD'insertValue'
   * DB_SPALTE'<dbSpalte>') in das Dokument einfügen würde, oder den Leerstring ""
   * wenn dieser Wert nicht bestimmt werden kann (z.B. wenn ein ungültiger
   * Spaltennamen dbSpalte übergeben wurde).
   * 
   * Anmerkung: Diese Methode wird durch die Methode getInsertValues() ergänzt die
   * alle Spaltennamen und Spaltenwerte zurück liefern kann.
   * 
   * @param dbSpalte
   *          Name der Datenbankspalte deren Wert zurückgeliefert werden soll.
   * @return Der Wert der Datenbankspalte dbSpalte des aktuell ausgewählten Absenders
   *         oder "", wenn der Wert nicht bestimmt werden kann.
   */
  @Override
  public String getValue(String dbSpalte)
  {
    /*
     * Diese Methode nimmt keine Synchronisierung über den WollMuxEventHandler vor,
     * da das reine Auslesen der Datenstrukturen unkritisch ist.
     * 
     * Im Test hatte ich einmal eine über den WollMuxEventHandler synchronisierte
     * Variante der Funktion getestet und beim Aufruf der Funktion über ein
     * Basic-Makro kam es zu einem Deadlock (Das Basic-Makro bekommt exclusiven
     * Zugriff auf OOo-Objekte; bereits laufende Events des WollMux, die evtl. mit
     * OOo-Objekten arbeiten müssen warten; WollMux-Eventqueue wird nicht weiter
     * abgearbeitet; Deadlock).
     */
    try
    {
      String value =
          DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDatasetTransformed().get(
          dbSpalte);
      if (value == null) value = "";
      return value;
    }
    catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
      return "";
    }
  }

  /**
   * Macht das selbe wie XWollMuxDocument wdoc = getWollMuxDocument(doc); if (wdoc !=
   * null) wdoc.addPrintFunction(functionName) und sollte durch diese Anweisungen
   * entsprechend ersetzt werden.
   * 
   * @param doc
   *          Das Dokument, dem die Druckfunktion functionName hinzugefügt werden
   *          soll.
   * @param functionName
   *          der Name einer Druckfunktion, die im Abschnitt "Druckfunktionen" der
   *          WollMux-Konfiguration definiert sein muss.
   * @deprecated since 2009-09-18
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  @Deprecated
  @Override
  public void addPrintFunction(XTextDocument doc, String functionName)
  {
    XWollMuxDocument wdoc = getWollMuxDocument(doc);
    if (wdoc != null) wdoc.removePrintFunction(functionName);
  }

  /**
   * Macht das selbe wie XWollMuxDocument wdoc = getWollMuxDocument(doc); if (wdoc !=
   * null) wdoc.removePrintFunction(functionName) und sollte durch diese Anweisungen
   * entsprechend ersetzt werden.
   * 
   * @param doc
   *          Das Dokument, dem die Druckfunktion functionName genommen werden soll.
   * @param functionName
   *          der Name einer Druckfunktion, die im Dokument gesetzt ist.
   * @deprecated since 2009-09-18
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  @Deprecated
  @Override
  public void removePrintFunction(XTextDocument doc, String functionName)
  {
    XWollMuxDocument wdoc = getWollMuxDocument(doc);
    if (wdoc != null) wdoc.removePrintFunction(functionName);
  }

  /**
   * Ermöglicht den Zugriff auf WollMux-Funktionen, die spezifisch für das Dokument
   * doc sind. Derzeit ist als doc nur ein c.s.s.t.TextDocument möglich. Wird ein
   * Dokument übergeben, für das der WollMux keine Funktionen anbietet (derzeit zum
   * Beispiel ein Calc-Dokument), so wird null zurückgeliefert. Dass diese Funktion
   * ein nicht-null Objekt zurückliefert bedeutet jedoch nicht zwangsweise, dass der
   * WollMux für das Dokument sinnvolle Funktionen bereitstellt. Es ist möglich, dass
   * Aufrufe der entsprechenden Funktionen des XWollMuxDocument-Interfaces nichts
   * tun.
   * 
   * Hinweis zur Synchronisation: Aufrufe der Funktionen von XWollMuxDocument können
   * ohne weitere Synchronisation sofort erfolgen. Jedoch ersetzt
   * getWollMuxDocument() keinesfalls die Synchronisation mit dem WollMux.
   * Insbesondere ist es möglich, dass getWollMuxDocument() zurückkehrt BEVOR der
   * WollMux das Dokument doc bearbeitet hat. Vergleiche hierzu die Beschreibung von
   * XWollMuxDocument.
   * 
   * @param doc
   *          Ein Office-Dokument, in dem dokumentspezifische Funktionen des
   *          WollMux aufgerufen werden sollen.
   * @return Liefert null, falls doc durch den WollMux nicht bearbeitet wird und eine
   *         Instanz von XWollMuxDocument, falls es sich bei doc prinzipiell um ein
   *         WollMux-Dokument handelt.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101), Christoph Lutz (D-III-ITD-D101)
   */
  @Override
  public XWollMuxDocument getWollMuxDocument(XComponent doc)
  {
    XTextDocument tdoc = UNO.XTextDocument(doc);
    if (tdoc != null) return new WollMuxDocument(tdoc);
    return null;
  }
  
  private void createMenuItems()
  {
    // "Extras->Seriendruck (WollMux)" erzeugen:
    List<String> removeButtonsFor = new ArrayList<>();
    removeButtonsFor.add(Dispatch.DISP_wmSeriendruck);
    WollMux.createMenuButton(Dispatch.DISP_wmSeriendruck, L.m("Seriendruck (WollMux)"),
      ".uno:ToolsMenu", ".uno:MailMergeWizard", removeButtonsFor);

    // "Help->Info über WollMux" erzeugen:
    removeButtonsFor.clear();
    removeButtonsFor.add(Dispatch.DISP_wmAbout);
    WollMux.createMenuButton(Dispatch.DISP_wmAbout,
      L.m("Info über Vorlagen und Formulare (WollMux)"), ".uno:HelpMenu",
      ".uno:About", removeButtonsFor);
  }

  /**
   * Erzeugt einen persistenten Menüeintrag mit der KommandoUrl cmdUrl und dem Label
   * label in dem durch mit insertIntoMenuUrl beschriebenen Toplevelmenü des Writers
   * und ordnet ihn direkt oberhalb des bereits bestehenden Menüpunktes mit der URL
   * insertBeforeElementUrl an. Alle Buttons, deren Url in der Liste removeCmdUrls
   * aufgeführt sind werden dabei vorher gelöscht (v.a. sollte cmdUrl aufgeführt
   * sein, damit nicht der selbe Button doppelt erscheint).
   */
  private static void createMenuButton(String cmdUrl, String label,
      String insertIntoMenuUrl, String insertBeforeElementUrl,
      List<String> removeCmdUrls)
  {
    final String settingsUrl = "private:resource/menubar/menubar";
  
    try
    {
      // Menüleiste aus des Moduls com.sun.star.text.TextDocument holen:
      XModuleUIConfigurationManagerSupplier suppl =
        UNO.XModuleUIConfigurationManagerSupplier(UNO.createUNOService("com.sun.star.ui.ModuleUIConfigurationManagerSupplier"));
      XUIConfigurationManager cfgMgr =
        UNO.XUIConfigurationManager(suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
      XIndexAccess menubar = UNO.XIndexAccess(cfgMgr.getSettings(settingsUrl, true));
  
      int idx = findElementWithCmdURL(menubar, insertIntoMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps((PropertyValue[]) menubar.getByIndex(idx));
        // Elemente des .uno:ToolsMenu besorgen:
        XIndexContainer toolsMenu =
          UNO.XIndexContainer(desc.getPropertyValue("ItemDescriptorContainer"));
  
        // Seriendruck-Button löschen, wenn er bereits vorhanden ist.
        for (String rCmdUrl : removeCmdUrls)
        {
          idx = findElementWithCmdURL(toolsMenu, rCmdUrl);
          if (idx >= 0) toolsMenu.removeByIndex(idx);
        }
  
        // SeriendruckAssistent suchen
        idx = findElementWithCmdURL(toolsMenu, insertBeforeElementUrl);
        if (idx >= 0)
        {
          UnoProps newDesc = new UnoProps();
          newDesc.setPropertyValue("CommandURL", cmdUrl);
          newDesc.setPropertyValue("Type", FormButtonType.PUSH);
          newDesc.setPropertyValue("Label", label);
          toolsMenu.insertByIndex(idx, newDesc.getProps());
          cfgMgr.replaceSettings(settingsUrl, menubar);
          UNO.XUIConfigurationPersistence(cfgMgr).store();
        }
      }
    }
    catch (Exception e)
    {}
  }
  
  /**
   * Liefert den Index des ersten Menüelements aus dem Menü menu zurück, dessen
   * CommandURL mit cmdUrl identisch ist oder -1, falls kein solches Element gefunden
   * wurde.
   * 
   * @return Liefert den Index des ersten Menüelements mit CommandURL cmdUrl oder -1.
   */
  private static int findElementWithCmdURL(XIndexAccess menu, String cmdUrl)
  {
    try
    {
      for (int i = 0; i < menu.getCount(); ++i)
      {
        PropertyValue[] desc = (PropertyValue[]) menu.getByIndex(i);
        for (int j = 0; j < desc.length; j++)
        {
          if ("CommandURL".equals(desc[j].Name) && cmdUrl.equals(desc[j].Value))
            return i;
        }
      }
    }
    catch (Exception e)
    {}
    return -1;
  }
  
}
