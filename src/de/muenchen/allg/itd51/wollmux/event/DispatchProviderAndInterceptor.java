/*
 * Dateiname: BasicWollMuxDispatchProvider.java
 * Projekt  : WollMux
 * Funktion : Liefert zu Dispatch-URLs, die der WollMux ohne ein zugehöriges TextDocumentModel behandeln kann XDispatch-Objekte.
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
 * 05.11.2009 | BNK | Auf Verwendung der neuen Dispatch und DocumentDispatch
 *                    Klassen umgeschrieben
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @author Matthias S. Benkmann (D-III-ITD-D101)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;

/**
 * Liefert zu Dispatch-URLs, die der WollMux ohne ein zugehöriges TextDocumentModel
 * behandeln kann XDispatch-Objekte.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DispatchProviderAndInterceptor implements XDispatchProvider,
    XDispatchProviderInterceptor
{
  /**
   * Enthält einen XDispatchProvider, der Dispatch-Objekte für alle globalen (d.h.
   * nicht dokumentgebundenen) Funktionalitäten des WollMux bereitstellt.
   */
  public static final XDispatchProviderInterceptor globalWollMuxDispatches =
    new DispatchProviderAndInterceptor();

  private XDispatchProvider slave = null;

  private XDispatchProvider master = null;

  /**
   * Falls ungleich null, so ist dieser {@link DispatchProviderAndInterceptor} in der
   * Lage für model-spezifische URLs {@link DocumentDispatch}-Objekte zu liefern.
   */
  private TextDocumentModel model = null;

  /**
   * Erzeugt einen {@link DispatchProviderAndInterceptor}, der nur globale URLs
   * behandeln kann.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public DispatchProviderAndInterceptor()
  {}

  /**
   * Erzeugt einen {@link DispatchProviderAndInterceptor}, der sowohl globale als
   * auch für model-spezifische URLs behandeln kann.
   * 
   * @param model
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public DispatchProviderAndInterceptor(TextDocumentModel model)
  {
    this.model = model;
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

  /**
   * Liefert die Methode methodName(String, PropertyValue[]) aus der Klasse c, oder
   * null falls so eine Methode nicht vorhanden ist.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  protected boolean hasMethod(Class<?> c, String methodName)
  {
    try
    {
      c.getDeclaredMethod(methodName, String.class, PropertyValue[].class);
      return true;
    }
    catch (Throwable x)
    {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
   *      java.lang.String, int)
   */
  public XDispatch queryDispatch(URL url, String frameName, int fsFlag)
  {
    String methodName = Dispatch.getMethodName(url);

    if (hasMethod(Dispatch.class, methodName))
      return new Dispatch();
    else
    {
      if (model != null)
      {
        if (hasMethod(DocumentDispatch.class, methodName))
          return new DocumentDispatch(getOrigDispatch(url, frameName, fsFlag), url,
            model);
      }
    }

    // ergibt return null, wenn kein Slave registriert
    return getOrigDispatch(url, frameName, fsFlag);
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
      XDispatchProviderInterceptor dpi = new DispatchProviderAndInterceptor(model);
      UNO.XDispatchProviderInterception(frame).registerDispatchProviderInterceptor(
        dpi);
    }
  }

}
