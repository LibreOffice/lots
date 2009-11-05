/*
 * Dateiname: DocumentDispatchInterceptor.java
 * Projekt  : WollMux
 * Funktion : Liefert zu Dispatch-URLs, die der WollMux TextDocumentModel-spezifisch behandelt XDispatch-Objekte.
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

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;

/**
 * Liefert zu Dispatch-URLs, die der WollMux TextDocumentModel-spezifisch behandelt
 * XDispatch-Objekte.
 * 
 * @author christoph.lutz
 */
public class DocumentDispatchInterceptor extends BasicWollMuxDispatchProvider
    implements XDispatchProviderInterceptor
{
  private XDispatchProvider slave = null;

  private XDispatchProvider master = null;

  private TextDocumentModel model;

  public DocumentDispatchInterceptor(TextDocumentModel model)
  {
    this.model = model;
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
    XDispatch myDisp = null;
    myDisp = super.queryDispatch(url, frameName, fsFlag);

    if (myDisp == null)
    {
      String methodName = Dispatch.getMethodName(url);

      if (getMethod(DocumentDispatch.class, methodName) != null)
        return new DocumentDispatch(getOrigDispatch(url, frameName, fsFlag), url,
          model);
    }

    return getOrigDispatch(url, frameName, fsFlag);
  }

  /**
   * Liefert das OriginalDispatch-Objekt, das der registrierte slave-DispatchProvider
   * liefert oder null falls kein slave.
   * 
   */
  public XDispatch getOrigDispatch(com.sun.star.util.URL url, String frameName,
      int fsFlag)
  {
    if (slave != null)
      return slave.queryDispatch(url, frameName, fsFlag);
    else
      return null;
  }

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
    com.sun.star.util.URL url = UNO.getParsedUNOUrl(Dispatch.DISP_wmAbdruck);
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
