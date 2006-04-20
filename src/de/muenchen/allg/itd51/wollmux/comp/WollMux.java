/*
 * Dateiname: WollMux.java
 * Projekt  : WollMux
 * Funktion : UNO-Service WollMux; Singleton und zentrale WollMux-Instanz.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 14.10.2005 | LUT | Erstellung
 * 09.11.2005 | LUT | + Logfile wird jetzt erweitert (append-modus)
 *                    + verwenden des Konfigurationsparameters SENDER_SOURCE
 *                    + Erster Start des wollmux über wm_configured feststellen.
 * 05.12.2005 | BNK | line.separator statt \n                 |  
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.comp;

import java.util.Iterator;
import java.util.Vector;

import com.sun.star.beans.NamedValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.task.XAsyncJob;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.itd51.wollmux.Event;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XWollMux;

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service dient
 * als Einstiegspunkt des WollMux und initialisiert alle benötigten
 * Programmmodule. Sämtliche Felder und öffentliche Methoden des Services sind
 * static und ermöglichen den Zugriff aus anderen Programmmodulen.
 */
public class WollMux extends WeakBase implements XServiceInfo, XAsyncJob,
    XDispatch, XDispatchProvider, XWollMux
{

  /**
   * Dieses Feld entält eine Liste aller Services, die dieser UNO-Service
   * implementiert.
   */
  public static final java.lang.String[] SERVICENAMES = {
                                                         "com.sun.star.task.AsyncJob",
                                                         "de.muenchen.allg.itd51.wollmux.WollMux" };

  /*
   * Felder des Protocol-Handlers: Hier kommt die Definition der Befehlsnamen,
   * die über WollMux-Kommando-URLs abgesetzt werden können.
   */

  /**
   * Dieses Feld enthält den Namen des Protokolls der WollMux-Kommando-URLs
   */
  public static final String wollmuxProtocol = "wollmux";

  public static final String cmdAbsenderAuswaehlen = "AbsenderAuswaehlen";

  public static final String cmdPALVerwalten = "PALVerwalten";

  public static final String cmdOpenTemplate = "OpenTemplate";

  public static final String cmdOpenDocument = "OpenDocument";

  public static final String cmdSenderBox = "SenderBox";

  public static final String cmdMenu = "Menu";

  /**
   * TODO: überarbeiten. Der Konstruktor erzeugt einen neues WollMux-Service im
   * XComponentContext context. Wurde der WollMux bereits in einem anderen
   * Kontext erzeugt, so wird eine RuntimeException geworfen.
   * 
   * @param context
   */
  public WollMux(XComponentContext ctx)
  {
    WollMuxSingleton.initialize(ctx);
  }

  /**
   * Der AsyncJob wird mit dem Event OnFirstVisibleTask gestartet. Die Methode
   * selbst beendet sich sofort wieder, bevor die Methode jedoch ausgeführt
   * wird, wird im Konstruktor das WollMuxSingleton initialisiert.
   * 
   * @see com.sun.star.task.XAsyncJob#executeAsync(com.sun.star.beans.NamedValue[],
   *      com.sun.star.task.XJobListener)
   */
  public synchronized void executeAsync(com.sun.star.beans.NamedValue[] lArgs,
      com.sun.star.task.XJobListener xListener)
      throws com.sun.star.lang.IllegalArgumentException
  {
    xListener.jobFinished(this, new NamedValue[] {});
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#getSupportedServiceNames()
   */
  public String[] getSupportedServiceNames()
  {
    return SERVICENAMES;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.lang.XServiceInfo#supportsService(java.lang.String)
   */
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
  public String getImplementationName()
  {
    return (WollMux.class.getName());
  }

  /**
   * Diese Methode liefert eine Factory zurück, die in der Lage ist den
   * UNO-Service zu erzeugen. Die Methode wird von UNO intern benötigt. Die
   * Methoden __getComponentFactory und __writeRegistryServiceInfo stellen das
   * Herzstück des UNO-Service dar.
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
  public synchronized static boolean __writeRegistryServiceInfo(
      XRegistryKey xRegKey)
  {
    return Factory.writeRegistryServiceInfo(
        WollMux.class.getName(),
        WollMux.SERVICENAMES,
        xRegKey);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
   *      java.lang.String, int)
   */
  public XDispatch queryDispatch( /* IN */com.sun.star.util.URL aURL,
  /* IN */String sTargetFrameName,
  /* IN */int iSearchFlags)
  {
    XDispatch xRet = null;
    Logger.debug2("queryDispatch: " + aURL.Complete);

    Vector parsedURL = parseWollmuxURL(aURL.Complete);
    if (parsedURL != null && parsedURL.size() >= 1)
    {
      String cmd = (String) parsedURL.remove(0);

      if (cmd.compareToIgnoreCase(cmdAbsenderAuswaehlen) == 0) xRet = this;

      if (cmd.compareToIgnoreCase(cmdOpenTemplate) == 0) xRet = this;

      if (cmd.compareToIgnoreCase(cmdOpenDocument) == 0) xRet = this;

      if (cmd.compareToIgnoreCase(cmdSenderBox) == 0) xRet = this;

      if (cmd.compareToIgnoreCase(cmdMenu) == 0) xRet = this;

      if (cmd.compareToIgnoreCase(cmdPALVerwalten) == 0) xRet = this;
    }
    return xRet;
  }

  /**
   * Diese Methode prüft, ob die in urlStr übergebene URL eine wollmux-URL ist
   * (also mit "wollmux:" beginnt) und zerlegt die URL in die Teile (Kommando,
   * Argument1, ..., ArgumentN), die sie in einem Vector zurückgibt. Ist die
   * übergebene URL keine wollmux-URL, so liefert die Methode null zurück. Eine
   * gültige WollMux-URL ist überlicherweise wie folgt aufgebaut:
   * "wollmux:Kommando#Argument1&Argument2", wobei die Argumente nur Bezeichner
   * sein dürfen.
   * 
   * @param urlStr
   * @return
   */
  private Vector parseWollmuxURL(String urlStr)
  {
    String[] parts = urlStr.split(":", 2);
    if (parts != null
        && parts.length == 2
        && parts[0].compareToIgnoreCase(wollmuxProtocol) == 0)
    {
      Vector result = new Vector();
      String cmdAndArgs = parts[1];
      parts = cmdAndArgs.split("#", 2);
      String cmd = parts[0];
      result.add(cmd);
      if (parts.length == 2)
      {
        String argStr = parts[1];
        String[] args = argStr.split("&");
        for (int i = 0; i < args.length; i++)
        {
          result.add(args[i]);
        }
      }
      return result;
    }
    else
      return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.DispatchDescriptor[])
   */
  public XDispatch[] queryDispatches( /* IN */DispatchDescriptor[] seqDescripts)
  {
    int nCount = seqDescripts.length;
    XDispatch[] lDispatcher = new XDispatch[nCount];

    for (int i = 0; i < nCount; ++i)
      lDispatcher[i] = queryDispatch(
          seqDescripts[i].FeatureURL,
          seqDescripts[i].FrameName,
          seqDescripts[i].SearchFlags);

    return lDispatcher;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#addStatusListener(com.sun.star.frame.XStatusListener,
   *      com.sun.star.util.URL)
   */
  public void addStatusListener(XStatusListener arg0, com.sun.star.util.URL arg1)
  {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#dispatch(com.sun.star.util.URL,
   *      com.sun.star.beans.PropertyValue[])
   */
  public void dispatch( /* IN */com.sun.star.util.URL aURL,
  /* IN */com.sun.star.beans.PropertyValue[] aArguments)
  {
    WollMuxSingleton mux = WollMuxSingleton.getInstance();

    Vector parsedURL = parseWollmuxURL(aURL.Complete);
    if (parsedURL != null && parsedURL.size() >= 1)
    {
      String cmd = (String) parsedURL.remove(0);

      // argStr zusammenbauen (wird nur für debug-Meldung benötigt)
      String argStr = "";
      Iterator i = parsedURL.iterator();
      while (i.hasNext())
      {
        argStr += "" + i.next();
        if (i.hasNext()) argStr += ", ";
      }

      if (cmd.compareToIgnoreCase(cmdAbsenderAuswaehlen) == 0)
      {
        Logger
            .debug2("Dispatch: Aufruf von WollMux:AbsenderdatenBearbeitenDialog");
        mux.getEventProcessor().addEvent(
            new Event(Event.ON_ABSENDER_AUSWAEHLEN));
      }

      if (cmd.compareToIgnoreCase(cmdPALVerwalten) == 0)
      {
        Logger.debug2("Dispatch: Aufruf von WollMux:PALVerwalten");
        mux.getEventProcessor().addEvent(
            new Event(Event.ON_PERSOENLICHE_ABSENDERLISTE));
      }

      if (cmd.compareToIgnoreCase(cmdOpenTemplate) == 0)
      {
        Logger.debug2("Dispatch: Aufruf von WollMux:OpenTemplate mit Args:"
                      + argStr);
        mux.getEventProcessor().addEvent(
            new Event(Event.ON_OPENTEMPLATE, parsedURL));
      }

      if (cmd.compareToIgnoreCase(cmdOpenDocument) == 0)
      {
        Logger.debug2("Dispatch: Aufruf von WollMux:OpenDocument mit Args:"
                      + argStr);
        mux.getEventProcessor().addEvent(
            new Event(Event.ON_OPENDOCUMENT, parsedURL));
      }

      if (cmd.compareToIgnoreCase(cmdMenu) == 0)
      {
        Logger.debug2("Dispatch: Aufruf von WollMux:menu mit Arg:" + parsedURL);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#removeStatusListener(com.sun.star.frame.XStatusListener,
   *      com.sun.star.util.URL)
   */
  public void removeStatusListener(XStatusListener arg0,
      com.sun.star.util.URL arg1)
  {
  }

  /*****************************************************************************
   * XWollMux-Implementierung:
   ****************************************************************************/

  /*
   * Hier wird auch gleich ein update getriggered! (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.XPALChangeEventBroadcaster#addPALChangeEventListener(de.muenchen.allg.itd51.wollmux.XPALChangeEventListener)
   */
  public void addPALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxSingleton.getInstance().addPALChangeEventListener(l);
    WollMuxSingleton.getInstance().getEventProcessor().addEvent(
        new Event(Event.ON_SELECTION_CHANGED));
  }

  public void removePALChangeEventListener(XPALChangeEventListener l)
  {
    WollMuxSingleton.getInstance().removePALChangeEventListener(l);
  }

  public void setCurrentSender(String sender, short idx)
  {
    WollMuxSingleton.getInstance().setCurrentSender(sender, idx);
  }

  public String getWollmuxConfAsString()
  {
    return WollMuxSingleton.getInstance().getWollmuxConf()
        .stringRepresentation();
  }

}
