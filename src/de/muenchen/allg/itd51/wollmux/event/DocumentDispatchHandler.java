/*
 * Dateiname: DocumentDispatchHandler.java
 * Projekt  : WollMux
 * Funktion : Definiert einen DispatchHandler, der dokumentgebundene Dispatches behandelt, die immer einem TextDocumentModel model zugeordnet sein müssen.
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

import de.muenchen.allg.itd51.wollmux.TextDocumentModel;

  /**
   * Definiert einen DispatchHandler, der dokumentgebundene Dispatches behandelt, die
   * immer einem TextDocumentModel model zugeordnet sein müssen.
   * 
   * @author christoph.lutz
   */
  abstract class DocumentDispatchHandler extends BasicDispatchHandler
  {
    protected final TextDocumentModel model;

    public DocumentDispatchHandler(String urlStr, TextDocumentModel model)
    {
      super(urlStr);
      this.model = model;
    }

    /**
     * Callback-Methode, die vom DispatchProvider provider aufgerufen wird, bevor
     * dieser DispatchHandler über queryDispatch(...) zurückgegeben wird, über die es
     * möglich ist, das Original-Dispatch-Objekt beim DispatchProvider abzufragen.
     */
    public void requireOrigDispatch(DocumentDispatchInterceptor provider,
        com.sun.star.util.URL url, String frameName, int fsFlag)
    {}
  }
