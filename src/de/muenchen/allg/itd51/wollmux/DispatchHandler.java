/*
 * Dateiname: DispatchInterceptor.java
 * Projekt  : WollMux
 * Funktion : Behandelt alle Dispatches des WollMux
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 28.10.2006 | LUT | Erstellung als DispatchInterceptor
 * 10.01.2007 | LUT | Umbenennung in DispatchHandler: Behandelt jetzt
 *                    auch globale WollMux Dispatches
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStatusListener;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Der DispatchHandler behandelt alle globalen Dispatches des WollMux und
 * registriert den DocumentDispatchInterceptor, der alle dokumentgebundenen
 * Dispatches behandeln kann. Er ermöglicht darüber hinaus das Abfangen und
 * Überschreiben von OOo-Dispatches (wie z.B. .uno:Print).
 * 
 * @author christoph.lutz
 */
public class DispatchHandler
{
  /**
   * Enthält einen XDispatchProvider, der Dispatch-Objekte für alle globalen
   * (d.h. nicht dokumentgebundenen) Funktionalitäten des WollMux bereitstellt.
   */
  public static final XDispatchProvider globalWollMuxDispatches = new GlobalDispatchProvider();

  // *****************************************************************************
  // * Definition aller möglichen Dispatch-URLs
  // *****************************************************************************

  public static final String DISP_unoPrint = ".uno:Print";

  public static final String DISP_unoPrintDefault = ".uno:PrintDefault";

  public static final String DISP_unoPrinterSetup = ".uno:PrinterSetup";

  public static final String DISP_wmAbsenderAuswaehlen = "wollmux:AbsenderAuswaehlen";

  public static final String DISP_wmPALVerwalten = "wollmux:PALVerwalten";

  public static final String DISP_wmOpenTemplate = "wollmux:OpenTemplate";

  public static final String DISP_wmOpen = "wollmux:Open";

  public static final String DISP_wmOpenDocument = "wollmux:OpenDocument";

  public static final String DISP_wmKill = "wollmux:Kill";

  public static final String DISP_wmAbout = "wollmux:About";

  public static final String DISP_wmDumpInfo = "wollmux:DumpInfo";

  public static final String DISP_wmFunctionDialog = "wollmux:FunctionDialog";

  public static final String DISP_wmFormularMax4000 = "wollmux:FormularMax4000";

  public static final String DISP_wmZifferEinfuegen = "wollmux:ZifferEinfuegen";

  public static final String DISP_wmAbdruck = "wollmux:Abdruck";

  public static final String DISP_wmZuleitungszeile = "wollmux:Zuleitungszeile";

  public static final String DISP_wmMarkBlock = "wollmux:MarkBlock";

  public static final String DISP_wmTextbausteinEinfuegen = "wollmux:TextbausteinEinfuegen";

  public static final String DISP_wmPlatzhalterAnspringen = "wollmux:PlatzhalterAnspringen";

  public static final String DISP_wmTextbausteinVerweisEinfuegen = "wollmux:TextbausteinVerweisEinfuegen";

  public static final String DISP_wmSeriendruck = "wollmux:Seriendruck";

  /*****************************************************************************
   * Erzeugt alle globalen DispatchHandler
   ****************************************************************************/
  private static Set createGlobalDispatchHandlers()
  {
    Set handler = new HashSet();

    handler.add(new BasicDispatchHandler(DISP_wmAbsenderAuswaehlen)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleShowDialogAbsenderAuswaehlen();
      }
    });

    handler.add(new BasicDispatchHandler(DISP_wmPALVerwalten)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleShowDialogPersoenlicheAbsenderliste();
      }
    });

    handler.add(new BasicDispatchHandler(DISP_wmOpenTemplate)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        Vector fragIds = new Vector();
        String[] parts = arg.split("&");
        for (int i = 0; i < parts.length; i++)
          fragIds.add(parts[i]);
        WollMuxEventHandler.handleOpenDocument(fragIds, true);
      }
    });

    handler.add(new BasicDispatchHandler(DISP_wmOpenDocument)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        Vector fragIds = new Vector();
        String[] parts = arg.split("&");
        for (int i = 0; i < parts.length; i++)
          fragIds.add(parts[i]);
        WollMuxEventHandler.handleOpenDocument(fragIds, false);
      }
    });

    handler.add(new BasicDispatchHandler(DISP_wmOpen)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        boolean asTemplate = true;
        boolean merged = false;
        ConfigThingy conf;
        ConfigThingy fragConf;
        try
        {
          arg = URLDecoder.decode(arg, ConfigThingy.CHARSET);
          conf = new ConfigThingy("OPEN", null, new StringReader(arg));
          fragConf = conf.get("Fragmente");
        }
        catch (Exception e)
        {
          Logger.error("Fehlerhaftes \"wollmux:Open\" Kommando");
          return;
        }

        try
        {
          asTemplate = conf.get("AS_TEMPLATE", 1).toString().equalsIgnoreCase(
              "true");
        }
        catch (Exception x)
        {
        }

        try
        {
          merged = conf.get("FORMGUIS", 1).toString()
              .equalsIgnoreCase("merged");

          if (merged)
            Logger
                .error("FORMGUIS \"merged\" noch nicht implementiert => Fallback auf \"independent\"");
        }
        catch (Exception x)
        {
        }

        Iterator iter = fragConf.iterator();
        while (iter.hasNext())
        {
          ConfigThingy fragListConf = (ConfigThingy) iter.next();
          List fragIds = new Vector();
          Iterator fragIter = fragListConf.iterator();
          while (fragIter.hasNext())
          {
            fragIds.add(fragIter.next().toString());
          }
          if (!fragIds.isEmpty())
            WollMuxEventHandler.handleOpenDocument(fragIds, asTemplate);
        }
      }
    });

    handler.add(new BasicDispatchHandler(DISP_wmDumpInfo)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleDumpInfo();
      }
    });

    handler.add(new BasicDispatchHandler(DISP_wmKill)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleKill();
      }
    });

    handler.add(new BasicDispatchHandler(DISP_wmAbout)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        try
        {
          arg = URLDecoder.decode(arg, ConfigThingy.CHARSET);
        }
        catch (UnsupportedEncodingException e)
        {
          Logger.error(e);
        }
        String wollMuxBarVersion = null;
        if (arg.length() > 0) wollMuxBarVersion = arg;
        WollMuxEventHandler.handleAbout(wollMuxBarVersion);
      }
    });

    return handler;
  }

  /*****************************************************************************
   * Erzeugt alle dokumentgebundenen Dispatchhandler
   ****************************************************************************/
  private static Set createDocumentDispatchHandler(TextDocumentModel model)
  {
    Set handler = new HashSet();
    if (model == null) return handler;

    handler.add(new DocumentDispatchHandler(DISP_unoPrint, model)
    {
      private XDispatch origDisp = null;

      private com.sun.star.util.URL origUrl = null;

      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handlePrint(model, origDisp, origUrl, props);
      }

      public void requireOrigDispatch(DocumentDispatchInterceptor provider,
          com.sun.star.util.URL url, String frameName, int fsFlag)
      {
        origUrl = url;
        origDisp = provider.getOrigDispatch(url, frameName, fsFlag);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_unoPrintDefault, model)
    {
      private XDispatch origDisp = null;

      private com.sun.star.util.URL origUrl = null;

      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handlePrint(model, origDisp, origUrl, props);
      }

      public void requireOrigDispatch(DocumentDispatchInterceptor provider,
          com.sun.star.util.URL url, String frameName, int fsFlag)
      {
        origUrl = url;
        origDisp = provider.getOrigDispatch(url, frameName, fsFlag);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmFunctionDialog, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleFunctionDialog(model, arg);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmFormularMax4000, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleFormularMax4000Show(model);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmZifferEinfuegen, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleButtonZifferEinfuegenPressed(model);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmAbdruck, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleButtonAbdruckPressed(model);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmMarkBlock, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleMarkBlock(model, arg);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmZuleitungszeile, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleButtonZuleitungszeilePressed(model);
      }
    });

    handler
        .add(new DocumentDispatchHandler(DISP_wmTextbausteinEinfuegen, model)
        {
          public void dispatch(String arg, PropertyValue[] props)
          {
            WollMuxEventHandler.handleTextbausteinEinfuegen(model, true);
          }
        });

    handler
        .add(new DocumentDispatchHandler(DISP_wmPlatzhalterAnspringen, model)
        {
          public void dispatch(String arg, PropertyValue[] props)
          {
            WollMuxEventHandler.handleJumpToPlaceholder(model);
          }
        });

    handler.add(new DocumentDispatchHandler(
        DISP_wmTextbausteinVerweisEinfuegen, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleTextbausteinEinfuegen(model, false);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmSeriendruck, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleSeriendruck(model);
      }
    });

    return handler;
  }

  // *****************************************************************************
  // * DispatchHandler
  // *****************************************************************************

  /**
   * Dient als Basisklasse für DispatchHandler, die XDispatch implementieren und
   * für die Bearbeitung von Dispatch-Aufrufen zu GENAU EINER URL zuständig
   * sind.
   * 
   * @author christoph.lutz
   */
  private static abstract class BasicDispatchHandler implements XDispatch
  {
    /**
     * Enthält die url, auf die dieser DispatchHandler reagiert.
     */
    protected final String urlStr;

    /**
     * Enthält alle aktuell registrierten StatusListener
     */
    protected final Vector statusListener = new Vector();

    /**
     * Erzeugt einen DispatchHandler, der Dispatches mit der url urlStr
     * bearbeiten kann.
     * 
     * @param urlStr
     */
    public BasicDispatchHandler(String urlStr)
    {
      this.urlStr = urlStr;
    }

    /**
     * Führt den Dispatch selbst durch, wobei arg das Argument der URL enthält
     * (z.B. "internerBriefkopf", wenn
     * url="wollmux:openTemplate#internerBriefkopf" war) und props das
     * PropertyValues[], das auch schon der ursprünglichen dispatch Methode
     * mitgeliefert wurde. Es kann davon ausgegangen werden, dass arg nicht null
     * ist und falls es nicht vorhanden ist den Leerstring enthält.
     * 
     * @param arg
     *          Das Argument das mit der URL mitgeliefert wurde.
     * @param props
     */
    protected abstract void dispatch(String arg, PropertyValue[] props);

    /**
     * Prüft ob der DiapatchHandler zum aktuellen Zeitpunkt in der Lage ist, den
     * Dispatch abzuhandeln und liefert false zurück, wenn der DispatchHandler
     * nicht verwendet werden soll.
     * 
     * @param url
     *          die zu prüfende URL ohne Argumente (z.B. "wollmux:openTemplate",
     *          wenn die urprüngliche URL
     *          "wollmux:openTemplate#internerBriefkopf" enthielt).
     * @return true, wenn der DispatchHandler verwendet werden soll, andernfalls
     *         false.
     */
    public boolean providesUrl(String url)
    {
      return urlStr.equalsIgnoreCase(url);
    }

    /**
     * Benachrichtigt den übergebenen XStatusListener listener mittels
     * listener.statusChanged() über den aktuellen Zustand des DispatchHandlers
     * und setzt z.B. den Zustände IsEnabled (Standardmäßig wird IsEnabled=true
     * übermittelt).
     * 
     * @param listener
     * @param url
     */
    protected void notifyStatusListener(XStatusListener listener, URL url)
    {
      FeatureStateEvent fse = new FeatureStateEvent();
      fse.FeatureURL = url;
      fse.IsEnabled = true;
      listener.statusChanged(fse);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#dispatch(com.sun.star.util.URL,
     *      com.sun.star.beans.PropertyValue[])
     */
    public void dispatch(URL url, PropertyValue[] props)
    {
      Logger.debug2(this.getClass().getSimpleName()
                    + ".dispatch('"
                    + url.Complete
                    + "')");

      // z.B. "wollmux:OpenTemplate#internerBriefkopf"
      // =====> {"wollmux:OpenTemplate", "internerBriefkopf"}
      String arg = "";
      String[] parts = url.Complete.split("#", 2);
      if (parts.length == 2) arg = parts[1];

      dispatch(arg, props);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#addStatusListener(com.sun.star.frame.XStatusListener,
     *      com.sun.star.util.URL)
     */
    public void addStatusListener(XStatusListener listener, URL url)
    {
      boolean alreadyRegistered = false;
      Iterator iter = statusListener.iterator();
      while (iter.hasNext())
        if (UnoRuntime.areSame(iter.next(), listener))
          alreadyRegistered = true;

      if (!alreadyRegistered) statusListener.add(listener);

      notifyStatusListener(listener, url);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#removeStatusListener(com.sun.star.frame.XStatusListener,
     *      com.sun.star.util.URL)
     */
    public void removeStatusListener(XStatusListener listener, URL x)
    {
      Iterator iter = statusListener.iterator();
      while (iter.hasNext())
        if (UnoRuntime.areSame(iter.next(), listener)) iter.remove();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
      return this.getClass().getSimpleName() + "('" + urlStr + "')";
    }
  }

  /**
   * Definiert einen DispatchHandler, der dokumentgebundene Dispatches
   * behandelt, die immer einem TextDocumentModel model zugeordnet sein müssen.
   * 
   * @author christoph.lutz
   */
  public static abstract class DocumentDispatchHandler extends
      BasicDispatchHandler
  {
    protected final TextDocumentModel model;

    public DocumentDispatchHandler(String urlStr, TextDocumentModel model)
    {
      super(urlStr);
      this.model = model;
    }

    /**
     * Callback-Methode, die vom DispatchProvider provider aufgerufen wird,
     * bevor dieser DispatchHandler über queryDispatch(...) zurückgegeben wird,
     * über die es möglich ist, das Original-Dispatch-Objekt beim
     * DispatchProvider abzufragen.
     */
    public void requireOrigDispatch(DocumentDispatchInterceptor provider,
        com.sun.star.util.URL url, String frameName, int fsFlag)
    {
    }
  }

  // *****************************************************************************
  // * DispatchProvider
  // *****************************************************************************

  /**
   * Enthält einen abstrakten DispatchProvider als Basis für die konkreten
   * DispatchProvider GlobalDispatchProvider und DocumentDispatchInterceptor und
   * ist in der Lage wollMuxURLs zu parsen.
   * 
   * @author christoph.lutz
   * 
   */
  private static abstract class BasicWollMuxDispatchProvider implements
      XDispatchProvider
  {
    private Set dispatchHandlers = new HashSet();

    /**
     * Teilt dem DispatchProvider mit, dass er in Zukunft alle in
     * dispatchHandlers enthaltenen DispatchHandler prüfen kann, wenn
     * queryDispatch(...) aufgerufen wird.
     * 
     * @param dispatchHandlers
     *          Set of BasicDispatchHandler
     */
    protected void setDispatchHandlers(Set dispatchHandlers)
    {
      this.dispatchHandlers = dispatchHandlers;
    }

    /**
     * Liefert einen DispatchHandler für die url urlStr zurück oder null, falls
     * der WollMux (in der aktuellen Situation) keinen dispatchHandler für
     * urlStr definiert.
     * 
     * @param urlStr
     *          die url des gesuchten Dispatch
     * @return ein DocumentDispatchHandler oder null, falls für urlStr in der
     *         aktuellen Situation kein DispatchHandler verfügbar ist.
     */
    private BasicDispatchHandler getDispatchHandlerForUrl(String urlStr)
    {
      // z.B. "wollmux:OpenTemplate#internerBriefkopf"
      // =====> {"wollmux:OpenTemplate", "internerBriefkopf"}
      String[] parts = urlStr.split("#", 2);

      Iterator iter = dispatchHandlers.iterator();
      while (iter.hasNext())
      {
        BasicDispatchHandler handler = (BasicDispatchHandler) iter.next();
        if (handler.providesUrl(parts[0])) return handler;
      }
      return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
     *      java.lang.String, int)
     */
    public XDispatch queryDispatch(URL url, String frameName, int fsFlag)
    {
      String urlStr = url.Complete;

      // Eigenes Dispatch-Objekt zurück liefern.
      BasicDispatchHandler myDisp = getDispatchHandlerForUrl(urlStr);
      if (myDisp != null)
      {
        Logger.debug2("queryDispatch: verwende " + myDisp);
        return myDisp;
      }
      return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.DispatchDescriptor[])
     */
    public XDispatch[] queryDispatches(DispatchDescriptor[] seqDescripts)
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
  }

  /**
   * Diese Klasse stellt alle globalen Dispatches des WollMux zur Verfügung.
   * 
   * @author christoph.lutz
   */
  private static class GlobalDispatchProvider extends
      BasicWollMuxDispatchProvider
  {
    public GlobalDispatchProvider()
    {
      setDispatchHandlers(createGlobalDispatchHandlers());
    }
  }

  /**
   * Diese Klasse stellt alle dokumentgebundenen WollMux-Dispatches zu
   * Verfügung.
   * 
   * @author christoph.lutz
   */
  private static class DocumentDispatchInterceptor extends
      BasicWollMuxDispatchProvider implements XDispatchProviderInterceptor
  {
    private XDispatchProvider slave = null;

    private XDispatchProvider master = null;

    private final XFrame frame;

    public DocumentDispatchInterceptor(XFrame frame)
    {
      this.frame = frame;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProviderInterceptor#getSlaveDispatchProvider()
     */
    public XDispatchProvider getSlaveDispatchProvider()
    {
      return slave;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProviderInterceptor#setSlaveDispatchProvider(com.sun.star.frame.XDispatchProvider)
     */
    public void setSlaveDispatchProvider(XDispatchProvider slave)
    {
      this.slave = slave;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProviderInterceptor#getMasterDispatchProvider()
     */
    public XDispatchProvider getMasterDispatchProvider()
    {
      return master;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProviderInterceptor#setMasterDispatchProvider(com.sun.star.frame.XDispatchProvider)
     */
    public void setMasterDispatchProvider(XDispatchProvider master)
    {
      this.master = master;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
     *      java.lang.String, int)
     */
    public XDispatch queryDispatch(com.sun.star.util.URL url, String frameName,
        int fsFlag)
    {
      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModelForFrame(frame);
      setDispatchHandlers(createDocumentDispatchHandler(model));

      XDispatch myDisp = null;
      myDisp = super.queryDispatch(url, frameName, fsFlag);

      if (myDisp != null)
      {
        if (myDisp instanceof DocumentDispatchHandler)
          ((DocumentDispatchHandler) myDisp).requireOrigDispatch(
              this,
              url,
              frameName,
              fsFlag);
        return myDisp;
      }
      else
      {
        return getOrigDispatch(url, frameName, fsFlag);
      }
    }

    /**
     * Liefert das OriginalDispatch-Objekt, das der registrierte
     * slave-DispatchProvider liefert.
     * 
     * @param url
     * @param frameName
     * @param fsFlag
     * @return
     */
    public XDispatch getOrigDispatch(com.sun.star.util.URL url,
        String frameName, int fsFlag)
    {
      return slave.queryDispatch(url, frameName, fsFlag);
    }
  }

  /**
   * Registriert einen DocumentDispatchProvider im Frame frame (nur dann, wenn
   * er nicht bereits registriert wurde).
   */
  public static void registerDocumentDispatchInterceptor(XFrame frame)
  {
    if (frame == null
        || UNO.XDispatchProviderInterception(frame) == null
        || UNO.XDispatchProvider(frame) == null) return;

    Logger.debug("Register DocumentDispatchInterceptor for frame #"
                 + frame.hashCode());

    // Hier möchte ich wissen, ob der DocumentDispatchInterceptor bereits im
    // Frame registriert ist. Ist das der Fall, so darf der
    // DocumentDispatchInterceptor nicht noch einmal registriert werden, weil
    // es sonst zu Endlosschleifen kommt, da sich die Dispatches des
    // DocumentDispatchInterceptor gegenseitig aufrufen würden.
    //
    // Leider gibt es keine Methode um aus dem Frame direkt abzulesen, ob der
    // DocumentDispatchInterceptor bereits registriert ist. Dieser Hack
    // übernimmt das: Er sucht per queryDispatch nach einer Dispatch-URL, die
    // ausschließlich der WollMux (weiter unten) definiert. Kommt dabei ein
    // Objekt =! null zurück, so ist der frame bereits registriert, ansonsten
    // nicht.
    com.sun.star.util.URL url = UNO.getParsedUNOUrl(DISP_wmAbdruck);
    XDispatch disp = UNO.XDispatchProvider(frame).queryDispatch(
        url,
        "_self",
        com.sun.star.frame.FrameSearchFlag.SELF);
    boolean alreadyRegistered = disp != null;

    // DispatchInterceptor registrieren (wenn nicht bereits registriert):
    if (!alreadyRegistered)
    {
      XDispatchProviderInterceptor dpi = new DocumentDispatchInterceptor(frame);
      UNO.XDispatchProviderInterception(frame)
          .registerDispatchProviderInterceptor(dpi);
    }
  }
}
