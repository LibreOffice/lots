/*
 * Dateiname: DispatchInterceptor.java
 * Projekt  : WollMux
 * Funktion : Erweitert BasicWollMuxDispatchProvider um das XDispatchProviderInterceptor Interface. 
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

/**
 * Erweitert {@link BasicWollMuxDispatchProvider} um das XDispatchProviderInterceptor
 * Interface.
 * 
 * @author christoph.lutz
 */
public class GlobalDispatchProvider extends BasicWollMuxDispatchProvider implements
    XDispatchProviderInterceptor
{
  private XDispatchProvider slave = null;

  private XDispatchProvider master = null;

  /**
   * Enthält einen XDispatchProvider, der Dispatch-Objekte für alle globalen (d.h.
   * nicht dokumentgebundenen) Funktionalitäten des WollMux bereitstellt.
   */
  public static final XDispatchProviderInterceptor globalWollMuxDispatches =
    new GlobalDispatchProvider();

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
    XDispatch myDisp = null;
    myDisp = super.queryDispatch(url, frameName, fsFlag);

    if (myDisp != null)
    {
      return myDisp;
    }
    else
    {
      return getOrigDispatch(url, frameName, fsFlag);
    }
  }

  /**
   * Liefert das OriginalDispatch-Objekt, das der registrierte slave-DispatchProvider
   * liefert oder null falls kein slave.
   */
  public XDispatch getOrigDispatch(com.sun.star.util.URL url, String frameName,
      int fsFlag)
  {
    if (slave != null)
      return slave.queryDispatch(url, frameName, fsFlag);
    else
      return null;
  }
}
