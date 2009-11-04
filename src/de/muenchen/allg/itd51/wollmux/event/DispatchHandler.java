/*
 * Dateiname: DispatchInterceptor.java
 * Projekt  : WollMux
 * Funktion : Behandelt alle Dispatches des WollMux
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * 28.10.2006 | LUT | Erstellung als DispatchInterceptor
 * 10.01.2007 | LUT | Umbenennung in DispatchHandler: Behandelt jetzt
 *                    auch globale WollMux Dispatches
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Der DispatchHandler behandelt alle globalen Dispatches des WollMux und registriert
 * den DocumentDispatchInterceptor, der alle dokumentgebundenen Dispatches behandeln
 * kann. Er ermöglicht darüber hinaus das Abfangen und Überschreiben von
 * OOo-Dispatches (wie z.B. .uno:Print).
 * 
 * @author christoph.lutz
 */
public class DispatchHandler
{
  /**
   * Enthält einen XDispatchProvider, der Dispatch-Objekte für alle globalen (d.h.
   * nicht dokumentgebundenen) Funktionalitäten des WollMux bereitstellt.
   */
  public static final XDispatchProviderInterceptor globalWollMuxDispatches =
    new GlobalDispatchProvider();

  // *****************************************************************************
  // * Definition aller möglichen Dispatch-URLs
  // *****************************************************************************

  public static final String DISP_unoPrint = ".uno:Print";

  public static final String DISP_unoPrintDefault = ".uno:PrintDefault";

  public static final String DISP_unoPrinterSetup = ".uno:PrinterSetup";

  public static final String DISP_wmAbsenderAuswaehlen =
    "wollmux:AbsenderAuswaehlen";

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

  public static final String DISP_wmTextbausteinEinfuegen =
    "wollmux:TextbausteinEinfuegen";

  public static final String DISP_wmPlatzhalterAnspringen =
    "wollmux:PlatzhalterAnspringen";

  public static final String DISP_wmTextbausteinVerweisEinfuegen =
    "wollmux:TextbausteinVerweisEinfuegen";

  public static final String DISP_wmSeriendruck = "wollmux:Seriendruck";

  public static final String DISP_wmTest = "wollmux:Test";

  /**********************************************************************************
   * Erzeugt alle globalen DispatchHandler
   *********************************************************************************/
  static Set<BasicDispatchHandler> createGlobalDispatchHandlers()
  {
    Set<BasicDispatchHandler> handler = new HashSet<BasicDispatchHandler>();

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
        Vector<String> fragIds = new Vector<String>();
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
        Vector<String> fragIds = new Vector<String>();
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
        WollMuxEventHandler.handleOpen(arg);
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
        String wollMuxBarVersion = null;
        if (arg.length() > 0) wollMuxBarVersion = arg;
        WollMuxEventHandler.handleAbout(wollMuxBarVersion);
      }
    });

    return handler;
  }

  /**********************************************************************************
   * Erzeugt alle dokumentgebundenen Dispatchhandler
   *********************************************************************************/
  static Set<BasicDispatchHandler> createDocumentDispatchHandler(
      TextDocumentModel model)
  {
    Set<BasicDispatchHandler> handler = new HashSet<BasicDispatchHandler>();
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
        // FIXME: Achtung: potentielle Fehlerquelle, da immer das selbe Objekt
        // in verschiedenen Threads modifiziert werden kann.
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
        // FIXME: Achtung: potentielle Fehlerquelle, da immer das selbe Objekt
        // in verschiedenen Threads modifiziert werden kann.
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

    handler.add(new DocumentDispatchHandler(DISP_wmTextbausteinEinfuegen, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleTextbausteinEinfuegen(model, true);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmPlatzhalterAnspringen, model)
    {
      public void dispatch(String arg, PropertyValue[] props)
      {
        WollMuxEventHandler.handleJumpToPlaceholder(model);
      }
    });

    handler.add(new DocumentDispatchHandler(DISP_wmTextbausteinVerweisEinfuegen,
      model)
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
        WollMuxEventHandler.handleSeriendruck(model, false);
      }
    });

    if (WollMuxFiles.installQATestHandler())
    {
      handler.add(new DocumentDispatchHandler(DISP_wmTest, model)
      {
        public void dispatch(String arg, PropertyValue[] props)
        {
          TestHandler.doTest(model, arg);
        }
      });
    }

    return handler;
  }

  // *****************************************************************************
  // * DispatchHandler
  // *****************************************************************************

  /**
   * Registriert einen DocumentDispatchProvider im Frame frame (nur dann, wenn er
   * nicht bereits registriert wurde).
   */
  public static void registerDocumentDispatchInterceptor(XFrame frame,
      TextDocumentModel model)
  {
    if (frame == null || UNO.XDispatchProviderInterception(frame) == null
      || UNO.XDispatchProvider(frame) == null) return;

    Logger.debug(L.m("Registriere DocumentDispatchInterceptor für frame #%1",
      Integer.valueOf(frame.hashCode())));

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
    XDispatch disp =
      UNO.XDispatchProvider(frame).queryDispatch(url, "_self",
        com.sun.star.frame.FrameSearchFlag.SELF);
    boolean alreadyRegistered = disp != null;

    if (alreadyRegistered)
      Logger.error(L.m("Doppelter Aufruf von registerDocumentDispatchInterceptor() für den selben Frame???"));

    // DispatchInterceptor registrieren (wenn nicht bereits registriert):
    if (!alreadyRegistered)
    {
      XDispatchProviderInterceptor dpi = new DocumentDispatchInterceptor(model);
      UNO.XDispatchProviderInterception(frame).registerDispatchProviderInterceptor(
        dpi);
    }
  }
}
