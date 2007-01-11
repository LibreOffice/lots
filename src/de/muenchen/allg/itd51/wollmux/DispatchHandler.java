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

import java.util.Vector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStatusListener;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;

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
   * Url für den Dispatch von ".uno:Print"
   */
  public static final String DISP_UNO_PRINT = ".uno:Print";

  /**
   * Url für den Dispatch von ".uno:PrinterSetup";
   */
  public static final String DISP_UNO_PRINTER_SETUP = ".uno:PrinterSetup";

  /**
   * Dieses Feld enthält den Namen des Protokolls der WollMux-Kommando-URLs
   */
  public static final String wollmuxProtocol = "wollmux";

  // *****************************************************************************
  // * Globale Dispatches
  // *****************************************************************************
  public static final String cmdAbsenderAuswaehlen = "AbsenderAuswaehlen";

  public static final String cmdPALVerwalten = "PALVerwalten";

  public static final String cmdOpenTemplate = "OpenTemplate";

  public static final String cmdOpenDocument = "OpenDocument";

  public static final String cmdKill = "Kill";

  public static final String cmdAbout = "About";

  public static final String cmdDumpInfo = "DumpInfo";

  /**
   * Enthält einen XDispatchProvider, der Dispatch-Objekte für alle globalen
   * (d.h. nicht dokumentgebundenen) Funktionalitäten des WollMux bereitstellt.
   */
  public static final XDispatchProvider globalWollMuxDispatches = new GlobalDispatchProvider();

  /**
   * Diese Klasse stellt alle globalen Dispatches des WollMux zur Verfügung.
   * 
   * @author christoph.lutz
   * 
   */
  private static class GlobalDispatchProvider extends
      BasicWollMuxDispatchProvider
  {
    /**
     * Liefert einen GlobalDispatchHandler für die url urlStr zurück oder null,
     * falls der WollMux (in der aktuellen Situation) keinen dispatchHandler für
     * urlStr definiert.
     * 
     * @param urlStr
     *          die url des gesuchten Dispatch
     * @return ein DocumentDispatchHandler oder null, falls für urlStr in der
     *         aktuellen Situation kein DispatchHandler verfügbar ist.
     */
    private GlobalDispatchHandler createGlobalDispatchHandler(String urlStr)
    {
      final Vector parsedURL = parseWollmuxURL(urlStr);
      if (parsedURL != null && parsedURL.size() >= 1)
      {
        String cmd = (String) parsedURL.remove(0);

        if (cmd.equalsIgnoreCase(cmdAbsenderAuswaehlen))
        {
          return new GlobalDispatchHandler()
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleShowDialogAbsenderAuswaehlen();
            }
          };
        }

        else if (cmd.equalsIgnoreCase(cmdPALVerwalten))
        {
          return new GlobalDispatchHandler()
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleShowDialogPersoenlicheAbsenderliste();
            }
          };
        }

        else if (cmd.equalsIgnoreCase(cmdOpenTemplate))
        {
          return new GlobalDispatchHandler()
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleOpenDocument(parsedURL, true);
            }
          };
        }

        else if (cmd.equalsIgnoreCase(cmdOpenDocument))
        {
          return new GlobalDispatchHandler()
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleOpenDocument(parsedURL, false);
            }
          };
        }

        else if (cmd.equalsIgnoreCase(cmdDumpInfo))
        {
          return new GlobalDispatchHandler()
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleDumpInfo();
            }
          };
        }

        else if (cmd.equalsIgnoreCase(cmdKill))
        {
          return new GlobalDispatchHandler()
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleKill();
            }
          };
        }

        else if (cmd.equalsIgnoreCase(cmdAbout))
        {
          return new GlobalDispatchHandler()
          {
            public void dispatch()
            {
              String wollMuxBarVersion = null;
              if (parsedURL.size() > 0)
                wollMuxBarVersion = parsedURL.get(0).toString();
              WollMuxEventHandler.handleAbout(wollMuxBarVersion);
            }
          };
        }
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

      // Eigenes Dispatch-Objekt zurück liefern oder Anfrage an den slave
      // XDispatchProvider weiterleiten.
      XDispatch myDisp = createGlobalDispatchHandler(urlStr);
      if (myDisp != null)
      {
        Logger.debug2("queryDispatch: verwende GlobalDispatchHandler für '"
                      + urlStr
                      + "'");
        return myDisp;
      }
      return null;
    }
  }

  /**
   * Definiert einen DispatchHandler, der einen globalen Dispatch behandelt.
   * 
   * @author christoph.lutz
   */
  public static abstract class GlobalDispatchHandler implements XDispatch
  {
    protected abstract void dispatch();

    public void dispatch(URL arg0, PropertyValue[] arg1)
    {
      Logger.debug2(this.getClass().getSimpleName()
                    + ".dispatch('"
                    + arg0.Complete
                    + "')");

      dispatch();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#addStatusListener(com.sun.star.frame.XStatusListener,
     *      com.sun.star.util.URL)
     */
    public void addStatusListener(XStatusListener arg0, URL arg1)
    {
      // Rückmeldung an aufrufenden Kontroller bisher nicht notwendig!
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.star.frame.XDispatch#removeStatusListener(com.sun.star.frame.XStatusListener,
     *      com.sun.star.util.URL)
     */
    public void removeStatusListener(XStatusListener arg0, URL arg1)
    {
      // Rückmeldung an aufrufenden Kontroller bisher nicht notwendig!
    }
  }

  // *****************************************************************************
  // * Dokumentgebundene Dispatches
  // *****************************************************************************
  public static final String cmdFunctionDialog = "FunctionDialog";

  public static final String cmdFormularMax4000 = "FormularMax4000";

  public static final String cmdZifferEinfuegen = "ZifferEinfuegen";

  public static final String cmdAbdruck = "Abdruck";

  public static final String cmdZuleitungszeile = "Zuleitungszeile";

  public static final String cmdMarkBlock = "MarkBlock";

  public static final String cmdTextbausteinEinfuegen = "TextbausteinEinfuegen";

  public static final String cmdPlatzhalterAnspringen = "PlatzhalterAnspringen";

  public static final String cmdTextbausteinVerweisEinfuegen = "TextbausteinVerweisEinfuegen";

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
    com.sun.star.util.URL url = UNO.getParsedUNOUrl(wollmuxProtocol
                                                    + ":"
                                                    + cmdAbdruck);
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

  /**
   * Diese Klasse stellt alle dokumentgebundenen WollMux-Dispatches zu Verfügung
   * und ermöglicht es darüber hinaus, dispatch-Kommandos wie z.B. .uno:Print
   * abzufangen und statt dessen eigene Aktionen durchzuführen.
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

    /**
     * Liefert einen DocumentDispatchHandler für die url urlStr zurück oder
     * null, falls der WollMux (in der aktuellen Situation) keinen
     * dispatchHandler für urlStr definiert.
     * 
     * @param urlStr
     *          die url des gesuchten Dispatch
     * @param model
     *          das TextDocumentModel, das über den Frame des
     *          DispatchInterceptors bestimmt wurde.
     * @return ein DocumentDispatchHandler oder null, falls für urlStr in der
     *         aktuellen Situation kein DispatchHandler verfügbar ist.
     */
    private DocumentDispatchHandler createDocumentDispatchHandler(
        String urlStr, TextDocumentModel model)
    {
      if (model == null) return null;

      if (urlStr.equalsIgnoreCase(DISP_UNO_PRINT) // ".uno:Print"
          && model.getPrintFunctionName() != null)
      {
        return new DocumentDispatchHandler(model)
        {
          public void dispatch()
          {
            WollMuxEventHandler.handleExecutePrintFunction(model);
          }
        };
      }

      if (urlStr.equalsIgnoreCase(".uno:PrintDefault")
          && model.getPrintFunctionName() != null)
      {
        return new DocumentDispatchHandler(model)
        {
          public void dispatch()
          {
            WollMuxEventHandler.handleExecutePrintFunction(model);
          }
        };
      }

      final Vector parsedURL = parseWollmuxURL(urlStr);
      if (parsedURL != null && parsedURL.size() >= 1)
      {
        String cmd = (String) parsedURL.remove(0);

        if (cmd.equalsIgnoreCase(cmdFunctionDialog))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              String dialogName = "";
              if (parsedURL.size() > 0)
                dialogName = parsedURL.get(0).toString();
              WollMuxEventHandler.handleFunctionDialog(model, dialogName);
            }
          };

        }

        if (cmd.equalsIgnoreCase(cmdFormularMax4000))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleFormularMax4000Show(model);
            }
          };
        }

        if (cmd.equalsIgnoreCase(cmdZifferEinfuegen))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleButtonZifferEinfuegenPressed(model);
            }
          };
        }

        if (cmd.equalsIgnoreCase(cmdAbdruck))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleButtonAbdruckPressed(model);
            }
          };
        }

        if (cmd.equalsIgnoreCase(cmdMarkBlock))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              String blockname = null;
              if (parsedURL.size() > 0)
                blockname = parsedURL.get(0).toString();
              WollMuxEventHandler.handleMarkBlock(model, blockname);
            }
          };
        }

        if (cmd.equalsIgnoreCase(cmdZuleitungszeile))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleButtonZuleitungszeilePressed(model);
            }
          };
        }

        if (cmd.equalsIgnoreCase(cmdTextbausteinEinfuegen))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleTextbausteinEinfuegen(model, true);
            }
          };
        }

        if (cmd.equalsIgnoreCase(cmdPlatzhalterAnspringen))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleJumpToPlaceholder(model);
            }
          };
        }

        if (cmd.equalsIgnoreCase(cmdTextbausteinVerweisEinfuegen))
        {
          return new DocumentDispatchHandler(model)
          {
            public void dispatch()
            {
              WollMuxEventHandler.handleTextbausteinEinfuegen(model, false);
            }
          };
        }
      }
      return null;
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
      String urlStr = url.Complete;

      TextDocumentModel model = WollMuxSingleton.getInstance()
          .getTextDocumentModelForFrame(frame);

      // Eigenes Dispatch-Objekt zurück liefern oder Anfrage an den slave
      // XDispatchProvider weiterleiten.
      XDispatch myDisp = createDocumentDispatchHandler(urlStr, model);
      if (myDisp != null)
      {
        Logger.debug2("queryDispatch: verwende DocumentDispatchHandler für '"
                      + urlStr
                      + "'");
        return myDisp;
      }
      else
        return slave.queryDispatch(url, frameName, fsFlag);
    }
  }

  /**
   * Definiert einen DispatchHandler, der dokumentgebundene Dispatches
   * behandelt, die immer einem TextDocumentModel model zugeordnet sein müssen.
   * 
   * @author christoph.lutz
   */
  public static abstract class DocumentDispatchHandler implements XDispatch
  {
    protected final TextDocumentModel model;

    public DocumentDispatchHandler(TextDocumentModel model)
    {
      this.model = model;
    }

    protected abstract void dispatch();

    public void dispatch(URL arg0, PropertyValue[] arg1)
    {
      Logger.debug2(this.getClass().getSimpleName()
                    + ".dispatch('"
                    + arg0.Complete
                    + "')");

      if (model != null) dispatch();
    }

    public void addStatusListener(XStatusListener listener, URL url)
    {
      // listener informieren, dass Feature zur Verfügung steht (und nicht
      // ausgegraut werden soll):
      FeatureStateEvent fse = new FeatureStateEvent();
      fse.FeatureURL = url;
      fse.IsEnabled = true;
      listener.statusChanged(fse);
      // Merken der XStatusListener bislang nicht erforderlich.
    }

    public void removeStatusListener(XStatusListener listener, URL url)
    {
      // Rückmeldung an aufrufenden Kontroller bisher nicht notwendig!
    }
  }

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
    /**
     * Diese Methode prüft, ob die in urlStr übergebene URL eine wollmux-URL ist
     * (also mit "wollmux:" beginnt) und zerlegt die URL in die Teile (Kommando,
     * Argument1, ..., ArgumentN), die sie in einem Vector zurückgibt. Ist die
     * übergebene URL keine wollmux-URL, so liefert die Methode null zurück.
     * Eine gültige WollMux-URL ist überlicherweise wie folgt aufgebaut:
     * "wollmux:Kommando#Argument1&Argument2", wobei die Argumente alle Zeichen
     * ausser dem '&'-Zeichen enthalten dürfen.
     * 
     * @param urlStr
     * @return
     */
    protected Vector parseWollmuxURL(String urlStr)
    {
      String[] parts = urlStr.split(":", 2);
      if (parts != null
          && parts.length == 2
          && parts[0].equalsIgnoreCase(wollmuxProtocol))
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

    public abstract XDispatch queryDispatch(URL url, String frameName,
        int fsFlags);

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
}
