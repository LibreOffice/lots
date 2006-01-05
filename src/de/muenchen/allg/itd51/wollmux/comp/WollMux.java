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

import java.net.URI;
import java.net.URISyntaxException;

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

/**
 * Diese Klasse stellt den zentralen UNO-Service WollMux dar. Der Service dient
 * als Einstiegspunkt des WollMux und initialisiert alle benötigten
 * Programmmodule. Sämtliche Felder und öffentliche Methoden des Services sind
 * static und ermöglichen den Zugriff aus anderen Programmmodulen.
 */
public class WollMux extends WeakBase implements XServiceInfo, XAsyncJob,
    XDispatch, XDispatchProvider
{

  private XComponentContext ctx;

  /**
   * Dieses Feld entält eine Liste aller Services, die dieser UNO-Service
   * implementiert.
   */
  public static final java.lang.String[] SERVICENAMES = {
                                                         "com.sun.star.task.AsyncJob",
                                                         "de.muenchen.allg.itd51.wollmux.comp.WollMux" };

  /*
   * Felder des Protocol-Handlers: Hier kommt die Definition der Befehlsnamen,
   * die über WollMux-Kommando-URLs abgesetzt werden können.
   */

  /**
   * Dieses Feld enthält den Namen des Protokolls der WollMux-Kommando-URLs
   */
  public static final String wollmuxProtocol = "wollmux";

  public static final String cmdAbsenderAuswaehlen = "AbsenderAuswaehlen";

  public static final String cmdOpenTemplate = "OpenTemplate";

  public static final String cmdSenderBox = "SenderBox";

  private static final String cmdMenu = "Menu";

  /**
   * TODO: überarbeiten. Der Konstruktor erzeugt einen neues WollMux-Service im
   * XComponentContext context. Wurde der WollMux bereits in einem anderen
   * Kontext erzeugt, so wird eine RuntimeException geworfen.
   * 
   * @param context
   */
  public WollMux(XComponentContext ctx)
  {
    this.ctx = ctx;
  }

  /**
   * Der AsyncJob wird mit dem Event OnFirstVisibleTask gestartet und besitzt
   * nur die Aufgabe, den WollMux über die Methode startupWollMux() zu starten.
   * 
   * @see com.sun.star.task.XAsyncJob#executeAsync(com.sun.star.beans.NamedValue[],
   *      com.sun.star.task.XJobListener)
   */
  public synchronized void executeAsync(com.sun.star.beans.NamedValue[] lArgs,
      com.sun.star.task.XJobListener xListener)
      throws com.sun.star.lang.IllegalArgumentException
  {
    if (xListener == null)
      throw new com.sun.star.lang.IllegalArgumentException("invalid listener");

    com.sun.star.beans.NamedValue[] lEnvironment = null;

    // Hole das Environment-Argument
    for (int i = 0; i < lArgs.length; ++i)
    {
      if (lArgs[i].Name.equals("Environment"))
      {
        lEnvironment = (com.sun.star.beans.NamedValue[]) com.sun.star.uno.AnyConverter
            .toArray(lArgs[i].Value);
      }
    }
    if (lEnvironment == null)
      throw new com.sun.star.lang.IllegalArgumentException("no environment");

    // Hole Event-Informationen
    String sEnvType = null;
    String sEventName = null;
    for (int i = 0; i < lEnvironment.length; ++i)
    {
      if (lEnvironment[i].Name.equals("EnvType"))
        sEnvType = com.sun.star.uno.AnyConverter
            .toString(lEnvironment[i].Value);
      else if (lEnvironment[i].Name.equals("EventName"))
        sEventName = com.sun.star.uno.AnyConverter
            .toString(lEnvironment[i].Value);
    }

    // Prüfe die property "EnvType":
    if ((sEnvType == null)
        || ((!sEnvType.equals("EXECUTOR")) && (!sEnvType.equals("DISPATCH"))))
    {
      java.lang.String sMessage = "\""
                                  + sEnvType
                                  + "\" isn't a valid value for EnvType";
      throw new com.sun.star.lang.IllegalArgumentException(sMessage);
    }

    /***************************************************************************
     * Starte den WollMux!
     */
    if (sEventName.equals("onFirstVisibleTask"))
    {
      WollMuxSingleton.getInstance().initialize(ctx);
    }
    /** *************************************************** */

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
    try
    {
      URI uri = new URI(aURL.Complete);
      Logger.debug2("queryDispatch: " + uri.toString());
      if (uri.getScheme().compareToIgnoreCase(wollmuxProtocol) == 0)
      {
        if (uri.getSchemeSpecificPart().compareToIgnoreCase(
            cmdAbsenderAuswaehlen) == 0) xRet = this;

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdOpenTemplate) == 0)
          xRet = this;

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdSenderBox) == 0)
          xRet = this;

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdMenu) == 0)
          xRet = this;
      }
    }
    catch (URISyntaxException e)
    {
      Logger.error(e);
    }
    return xRet;
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
    try
    {
      URI uri = new URI(aURL.Complete);
      WollMuxSingleton mux = WollMuxSingleton.getInstance();

      if (uri.getScheme().compareToIgnoreCase(wollmuxProtocol) == 0)
      {
        if (uri.getSchemeSpecificPart().compareToIgnoreCase(
            cmdAbsenderAuswaehlen) == 0)
        {
          Logger
              .debug2("Dispatch: Aufruf von WollMux:AbsenderdatenBearbeitenDialog");
          mux.getEventProcessor().addEvent(
              new Event(Event.ON_ABSENDER_AUSWAEHLEN));
        }

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdOpenTemplate) == 0)
        {
          Logger.debug2("Dispatch: Aufruf von WollMux:OpenFrag mit Frag:"
                        + uri.getFragment());
          mux.getEventProcessor().addEvent(
              new Event(Event.ON_OPENTEMPLATE, uri.getFragment()));
        }

        if (uri.getSchemeSpecificPart().compareToIgnoreCase(cmdMenu) == 0)
        {
          Logger.debug2("Dispatch: Aufruf von WollMux:menu mit menu:"
                        + uri.getFragment());
        }
      }
    }
    catch (URISyntaxException e)
    {
      Logger.error(e);
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

}
