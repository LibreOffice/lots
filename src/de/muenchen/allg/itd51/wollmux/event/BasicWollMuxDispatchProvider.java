/*
 * Dateiname: BasicWollMuxDispatchProvider.java
 * Projekt  : WollMux
 * Funktion : Enthält einen abstrakten DispatchProvider als Basis.
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
import java.util.Iterator;
import java.util.Set;

import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;


  /**
   * Enthält einen abstrakten DispatchProvider als Basis für die konkreten
   * DispatchProvider GlobalDispatchProvider und DocumentDispatchInterceptor und ist
   * in der Lage wollMuxURLs zu parsen.
   * 
   * @author christoph.lutz
   * 
   */
  abstract class BasicWollMuxDispatchProvider implements
      XDispatchProvider
  {
    private Set<BasicDispatchHandler> dispatchHandlers =
      new HashSet<BasicDispatchHandler>();

    /**
     * Teilt dem DispatchProvider mit, dass er in Zukunft alle in dispatchHandlers
     * enthaltenen DispatchHandler prüfen kann, wenn queryDispatch(...) aufgerufen
     * wird.
     * 
     * @param dispatchHandlers
     *          Set of BasicDispatchHandler
     */
    protected void setDispatchHandlers(Set<BasicDispatchHandler> dispatchHandlers)
    {
      this.dispatchHandlers = dispatchHandlers;
    }

    /**
     * Liefert einen DispatchHandler für die url urlStr zurück oder null, falls der
     * WollMux (in der aktuellen Situation) keinen dispatchHandler für urlStr
     * definiert.
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
      int idx = urlStr.indexOf('#');
      String part = idx < 0 ? urlStr : urlStr.substring(0, idx);

      Iterator<BasicDispatchHandler> iter = dispatchHandlers.iterator();
      while (iter.hasNext())
      {
        BasicDispatchHandler handler = iter.next();
        if (handler.providesUrl(part)) return handler;
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
        Logger.debug2(L.m("queryDispatch: verwende %1", myDisp));
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
        lDispatcher[i] =
          queryDispatch(seqDescripts[i].FeatureURL, seqDescripts[i].FrameName,
            seqDescripts[i].SearchFlags);

      return lDispatcher;
    }
  }
