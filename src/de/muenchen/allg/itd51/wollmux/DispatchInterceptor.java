/*
 * Dateiname: DispatchInterceptor.java
 * Projekt  : WollMux
 * Funktion : Dokumentgebundene Dispatch-Handler und Überschreiben von 
 *            OOo-Dispatches (wie z.B. .uno:Print.)
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 28.10.2006 | LUT | Erstellung als DispatchInterceptor
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.frame.XStatusListener;
import com.sun.star.text.XTextDocument;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;

/**
 * Der DispatchInterceptor registriert den WollMuxDispatchInterceptor, der alle
 * Dispatches behandeln kann, die dokumentgebunden sind. Er ermöglicht darüber
 * hinaus das Abfangen und Überschreiben von OOo-Dispatches (wie z.B.
 * .uno:Print).
 * 
 * @author christoph.lutz
 */
public class DispatchInterceptor
{
  /**
   * Url für den Redirect auf den ursprünglichen Dispatch von ".uno:Print"
   */
  public static final String DISP_UNO_PRINT_FORWARDER = "wollmux:DefaultUnoPrint";

  /**
   * Url für den Redirect auf den ursprünglichen Dispatch von
   * ".uno:PrintDefault"
   */
  public static final String DISP_UNO_PRINT_DEFAULT_FORWARDER = "wollmux:DefaultUnoPrintDefault";

  /**
   * Url für den Dispatch von ".uno:PrintDefault"
   */
  public static final String DISP_UNO_PRINT_DEFAULT = ".uno:PrintDefault";

  /**
   * Url für den Dispatch von ".uno:Print"
   */
  public static final String DISP_UNO_PRINT = ".uno:Print";

  /**
   * Url für den Dispatch von ".uno:PrinterSetup";
   */
  public static final String DISP_UNO_PRINTER_SETUP = ".uno:PrinterSetup";

  /**
   * Registriert einen WollMuxDispatchProvider im Frame frame (nur dann, wenn er
   * nicht bereits registriert wurde).
   */
  public static void registerWollMuxDispatchInterceptor(XFrame frame)
  {
    if (frame == null
        || UNO.XDispatchProviderInterception(frame) == null
        || UNO.XDispatchProvider(frame) == null) return;

    Logger.debug("Register WollMuxDispatchInterceptor for frame #"
                 + frame.hashCode());

    // Hier möchte ich wissen, ob der WollMuxDispatchInterceptor bereits im
    // Frame registriert ist. Ist das der Fall, so darf der
    // WollMuxDispatchInterceptor nicht noch einmal registriert werden, weil
    // es sonst zu Endlosschleifen kommt, da sich die Dispatches des
    // WollMuxDispatchInterceptor gegenseitig aufrufen würden.
    //
    // Leider gibt es keine Methode um aus dem Frame direkt abzulesen, ob der
    // WollMuxDispatchInterceptor bereits registriert ist. Dieser Hack
    // übernimmt das: Er sucht per queryDispatch nach einer Dispatch-URL, die
    // der WollMux (weiter unten) selbst definiert. Kommt dabei ein Objekt vom
    // Typ RedirectDispatch zurück, so ist der frame bereits registriert,
    // ansonsten nicht.
    com.sun.star.util.URL url = UNO.getParsedUNOUrl(DISP_UNO_PRINT_FORWARDER);
    XDispatch disp = UNO.XDispatchProvider(frame).queryDispatch(
        url,
        "_self",
        com.sun.star.frame.FrameSearchFlag.SELF);
    boolean alreadyRegistered = disp instanceof RedirectDispatch;

    // DispatchInterceptor registrieren (wenn nicht bereits registriert):
    if (!alreadyRegistered)
    {
      XDispatchProviderInterceptor dpi = new WollMuxDispatchInterceptor(frame);
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
  private static class WollMuxDispatchInterceptor implements
      XDispatchProviderInterceptor
  {
    private XDispatchProvider slave = null;

    private XDispatchProvider master = null;

    private final XFrame frame;

    public WollMuxDispatchInterceptor(XFrame frame)
    {
      this.frame = frame;
    }

    public XDispatchProvider getSlaveDispatchProvider()
    {
      return slave;
    }

    public void setSlaveDispatchProvider(XDispatchProvider slave)
    {
      this.slave = slave;
    }

    public XDispatchProvider getMasterDispatchProvider()
    {
      return master;
    }

    public void setMasterDispatchProvider(XDispatchProvider master)
    {
      this.master = master;
    }

    public XDispatch queryDispatch(com.sun.star.util.URL url, String frameName,
        int fsFlag)
    {
      String urlStr = url.Complete;

      // Ab hier kommen Dispatches, die die URL in irgend einer Form
      // umschreiben (z.B. wird wollmux:defaultUnoPrint auf den alten .uno:Print
      // abgebildet).

      // -------------- Dispatch für wollmux:defaultUNOPrint --------------
      if (urlStr.equals(DISP_UNO_PRINT_FORWARDER))
      {
        Logger.debug2("queryDispatch: '" + urlStr + "'");
        return createRedirectDispatch(slave, DISP_UNO_PRINT, frameName, fsFlag);
      }

      // ----------- Dispatch für wollmux:defaultUNOPrintDefault -----------
      if (urlStr.equals(DISP_UNO_PRINT_DEFAULT_FORWARDER))
      {
        Logger.debug2("queryDispatch: '" + urlStr + "'");
        return createRedirectDispatch(
            slave,
            DISP_UNO_PRINT_DEFAULT,
            frameName,
            fsFlag);
      }

      // Ab hier kommen Dispatches, die die URL unverändert übernehmen:
      final XDispatch origDisp = slave.queryDispatch(url, frameName, fsFlag);
      if (origDisp == null) return null;

      // ------------------ Dispatch für .uno:Print -------------------
      if (urlStr.equals(DISP_UNO_PRINT))
      {
        Logger.debug2("queryDispatch: '" + urlStr + "'");

        return new XDispatch()
        {
          public void dispatch(com.sun.star.util.URL url, PropertyValue[] arg1)
          {
            Logger.debug2("XDispatch.dispatch('" + url.Complete + "')");
            XTextDocument doc = UNO.XTextDocument(frame.getController()
                .getModel());
            if (doc != null)
              WollMuxEventHandler.handlePrintButtonPressed(
                  doc,
                  DISP_UNO_PRINT_FORWARDER);
          }

          public void removeStatusListener(XStatusListener arg0,
              com.sun.star.util.URL arg1)
          {
            Logger.debug2("XDispatch.removeStatusListener(#"
                          + arg0.hashCode()
                          + ")");
            if (origDisp != null) origDisp.removeStatusListener(arg0, arg1);
          }

          public void addStatusListener(XStatusListener arg0,
              com.sun.star.util.URL arg1)
          {
            Logger.debug2("XDispatch.addStatusListener(#"
                          + arg0.hashCode()
                          + ")");
            if (origDisp != null) origDisp.addStatusListener(arg0, arg1);
          }
        };
      }

      // ------------------ Dispatch für .uno:PrintDefault -------------------
      if (urlStr.equals(DISP_UNO_PRINT_DEFAULT))
      {
        Logger.debug2("queryDispatch: '" + urlStr + "'");

        return new XDispatch()
        {
          public void dispatch(com.sun.star.util.URL url, PropertyValue[] arg1)
          {
            Logger.debug2("XDispatch.dispatch('" + url.Complete + "')");
            XTextDocument doc = UNO.XTextDocument(frame.getController()
                .getModel());
            if (doc != null)
              WollMuxEventHandler.handlePrintButtonPressed(
                  doc,
                  DISP_UNO_PRINT_DEFAULT_FORWARDER);
          }

          public void removeStatusListener(XStatusListener arg0,
              com.sun.star.util.URL arg1)
          {
            Logger.debug2("XDispatch.removeStatusListener(#"
                          + arg0.hashCode()
                          + ")");
            if (origDisp != null) origDisp.removeStatusListener(arg0, arg1);
          }

          public void addStatusListener(XStatusListener arg0,
              com.sun.star.util.URL arg1)
          {
            Logger.debug2("XDispatch.addStatusListener(#"
                          + arg0.hashCode()
                          + ")");
            if (origDisp != null) origDisp.addStatusListener(arg0, arg1);
          }
        };
      }

      // Anfrage an das ursprüngliche DispatchObjekt weiterleiten.
      return origDisp;
    }

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
   * Erzeugt einen neuen RedirectDispatch zu urlStr oder null, falls kein
   * Dispatch verfügbar ist.
   * 
   * @param urlStr
   * @return
   */
  private static RedirectDispatch createRedirectDispatch(
      XDispatchProvider provider, String urlStr, String frameName,
      int frameSearchFlags)
  {
    final URL unoUrl = UNO.getParsedUNOUrl(urlStr);
    final XDispatch origDisp = provider.queryDispatch(
        unoUrl,
        frameName,
        frameSearchFlags);

    if (UNO.XNotifyingDispatch(origDisp) != null)
    {
      return new RedirectNotifyingDispatch(UNO.XNotifyingDispatch(origDisp),
          unoUrl);
    }
    else if (origDisp != null)
    {
      return new RedirectDispatch(origDisp, unoUrl);
    }
    else
      return null;
  }

  /**
   * Der RedirectDispatch ist ein Dispatch-Handler, der Anfragen an den
   * ursprünglichen XDispatch-Handler weiter reicht, nachdem er die der
   * jeweiligen Anfrage übergebenen URL angepasst hat.
   * 
   * @author christoph.lutz
   */
  private static class RedirectDispatch implements XDispatch
  {
    protected XDispatch orig;

    protected URL url;

    public RedirectDispatch(XDispatch orig, URL url)
    {
      this.orig = orig;
      this.url = url;
    }

    public void dispatch(URL x, PropertyValue[] args)
    {
      Logger.debug2(RedirectDispatch.class.getName()
                    + ".dispatch('"
                    + url.Complete
                    + "')");

      if (orig != null) orig.dispatch(url, args);
    }

    public void addStatusListener(XStatusListener listener, URL x)
    {
      Logger.debug2(RedirectDispatch.class.getName()
                    + ".addStatusListener('"
                    + listener.hashCode()
                    + "')");

      if (orig != null) orig.addStatusListener(listener, url);
    }

    public void removeStatusListener(XStatusListener listener, URL x)
    {
      Logger.debug2(RedirectDispatch.class.getName()
                    + ".removeStatusListener('"
                    + listener.hashCode()
                    + "')");

      if (orig != null) orig.removeStatusListener(listener, url);
    }
  }

  /**
   * Der RedirectNotifyingDispatch ist ein Dispatch-Handler, der Anfragen an den
   * ursprünglichen XNotifyingDispatch-Handler weiter reicht, nachdem er die der
   * jeweiligen Anfrage übergebenen URL angepasst hat.
   * 
   * @author christoph.lutz
   */
  private static class RedirectNotifyingDispatch extends RedirectDispatch
      implements XNotifyingDispatch
  {
    public RedirectNotifyingDispatch(XNotifyingDispatch disp, URL url)
    {
      super(disp, url);
    }

    public void dispatchWithNotification(URL x, PropertyValue[] args,
        XDispatchResultListener listener)
    {
      Logger.debug2(RedirectDispatch.class.getName()
                    + ".dispatchWithNotification('"
                    + url
                    + "')");

      if (UNO.XNotifyingDispatch(orig) != null)
        UNO.XNotifyingDispatch(orig).dispatchWithNotification(
            url,
            args,
            listener);
    }
  }
}
