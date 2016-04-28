/*
 * Dateiname: BasicWollMuxDispatchProvider.java
 * Projekt  : WollMux
 * Funktion : Liefert zu Dispatch-URLs, die der WollMux ohne ein zugehöriges TextDocumentModel behandeln kann XDispatch-Objekte.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
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

import java.util.HashSet;
import java.util.Set;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.FrameAction;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchProviderInterceptor;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrameActionListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Liefert zu Dispatch-URLs, die der WollMux behandeln kann XDispatch-Objekte.
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

  /**
   * Einthält eine Liste aller jemals registrierten dokumentgebundener
   * {@link DispatchProviderAndInterceptor}-Objekte; Die Liste wird benötigt um
   * festzustellen, ob der WollMux bereits einen
   * {@link DispatchProviderAndInterceptor} auf einem Frame registriert hat
   * (Vermeidung von Doppeleintragungen) und um
   * {@link DispatchProviderAndInterceptor}-Objekte deregistrieren zu können, wenn
   * das zugehörige Textdokument geschlossen wird.
   */
  private static final Set<DispatchProviderAndInterceptor> documentDispatchProviderAndInterceptors =
    new HashSet<DispatchProviderAndInterceptor>();

  private XDispatchProvider slave = null;

  private XDispatchProvider master = null;

  /**
   * Falls ungleich null, so ist dieser {@link DispatchProviderAndInterceptor} in der
   * Lage für model-spezifische URLs {@link DocumentDispatch}-Objekte zu liefern.
   */
  private XFrame frame = null;

  /**
   * Bei dokumentgebundenen {@link DispatchProviderAndInterceptor}-Objekten wird auf
   * dem Frame zusätzlich ein {@link XFrameActionListener} registriert um überwachen
   * zu können, wann der {@link DispatchProviderAndInterceptor} deregistriert werden
   * soll. Dieser {@link XFrameActionListener} ist hier hinterlegt.
   */
  private XFrameActionListener frameActionListener = null;

  /**
   * Erzeugt einen {@link DispatchProviderAndInterceptor}, der nur globale URLs
   * behandeln kann.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private DispatchProviderAndInterceptor()
  {}

  /**
   * Erzeugt einen {@link DispatchProviderAndInterceptor}, der sowohl globale als
   * auch für model-spezifische URLs behandeln kann.
   * 
   * @param model
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  private DispatchProviderAndInterceptor(XFrame frame)
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

  /**
   * Liefert true, wenn die Methode methodName(String, PropertyValue[]) in der Klasse
   * c vorhanden ist, andern falls false.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
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
   * java.lang.String, int)
   */
  public XDispatch queryDispatch(URL url, String frameName, int fsFlag)
  {
    String methodName = Dispatch.getMethodName(url);

    if (hasMethod(Dispatch.class, methodName))
      return new Dispatch();
    else
    {
      if (frame != null)
      {
        if (hasMethod(DocumentDispatch.class, methodName))
          return new DocumentDispatch(getOrigDispatch(url, frameName, fsFlag), url,
            frame);
      }
    }

    // ergibt return null, wenn kein Slave registriert
    return getOrigDispatch(url, frameName, fsFlag);
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.
   * DispatchDescriptor[])
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
   * 
   * @author Matthias Benkmann (D-III-ITD-D101), Christoph Lutz (D-III-ITD-D101)
   *         TESTED
   */
  public static void registerDocumentDispatchInterceptor(XFrame frame)
  {
    if (frame == null || UNO.XDispatchProviderInterception(frame) == null
      || UNO.XDispatchProvider(frame) == null) return;

    // DispatchInterceptor registrieren (wenn nicht bereits registriert):
    if (getRegisteredDPI(frame) == null)
    {
      DispatchProviderAndInterceptor dpi = new DispatchProviderAndInterceptor(frame);

      Logger.debug(L.m("Registriere DocumentDispatchInterceptor #%1 für frame #%2",
        Integer.valueOf(dpi.hashCode()), Integer.valueOf(frame.hashCode())));

      UNO.XDispatchProviderInterception(frame).registerDispatchProviderInterceptor(
        dpi);
      registerDPI(dpi);

      dpi.frameActionListener = new DPIFrameActionListener();
      frame.addFrameActionListener(dpi.frameActionListener);
    }
    else
      Logger.debug(L.m(
        "Ignoriere doppelten Aufruf von registerDocumentDispatchInterceptor() für den selben Frame #%1",
        Integer.valueOf(frame.hashCode())));
  }

  /**
   * Wird gleichzeitig mit der Registrierung eines
   * {@link DispatchProviderAndInterceptor}-Objekts auf einem Frame registriert und
   * dient zur Überwachung des Frames, um den {@link DispatchProviderAndInterceptor}
   * wieder freigeben zu können, wenn der Frame disposed wird (drücken auf den großen
   * "X"-Button) bzw. wenn der Frame nicht mehr an das XTextDocument gebunden ist
   * (z.B. beim Drücken des kleinen "X"-Buttons)
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static class DPIFrameActionListener implements XFrameActionListener
  {
    public void disposing(EventObject e)
    {
      deregisterDPI(getRegisteredDPI(UNO.XFrame(e.Source)));
    }

    public void frameAction(com.sun.star.frame.FrameActionEvent e)
    {
      if (e.Action == FrameAction.COMPONENT_REATTACHED)
      {
        DispatchProviderAndInterceptor dpi = getRegisteredDPI(UNO.XFrame(e.Source));
        if (dpi != null && dpi.frame != null
          && UNO.XTextDocument(dpi.frame.getController().getModel()) == null)
        {
          if (dpi.frameActionListener != null)
          {
            dpi.frame.removeFrameActionListener(dpi.frameActionListener);
            dpi.frameActionListener = null;
          }

          Logger.debug(L.m(
            "Deregistrierung von DocumentDispatchInterceptor #%1 aus frame #%2",
            Integer.valueOf(dpi.hashCode()), Integer.valueOf(dpi.frame.hashCode())));
          UNO.XDispatchProviderInterception(dpi.frame).releaseDispatchProviderInterceptor(
            dpi);
          dpi.frame.contextChanged();
          deregisterDPI(dpi);
        }
      }
    }
  }

  /**
   * Merkt sich den übergebenen dokumentgebundenen DispatchProviderAndInterceptor in
   * einem internen statischen Set; Der Zugriff auf dieses Set erfolgt
   * synchronisiert. Ist dpi==null wird nichts gemacht.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static void registerDPI(DispatchProviderAndInterceptor dpi)
  {
    if (dpi == null) return;
    synchronized (documentDispatchProviderAndInterceptors)
    {
      documentDispatchProviderAndInterceptors.add(dpi);
    }
  }

  /**
   * Entfernt den übergebenen dokumentgebundenen DispatchProviderAndInterceptor aus
   * einem internen statischen Set; Der Zugriff auf dieses Set erfolgt
   * synchronisiert. Ist dpi==null wird nichts gemacht.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static void deregisterDPI(DispatchProviderAndInterceptor dpi)
  {
    if (dpi == null) return;
    synchronized (documentDispatchProviderAndInterceptors)
    {
      Logger.debug(L.m("Interne Freigabe des DocumentDispatchInterceptor #%1",
        Integer.valueOf(dpi.hashCode())));
      documentDispatchProviderAndInterceptors.remove(dpi);
    }
  }

  /**
   * Liefert den für frame bereits vom WollMux registrierten
   * {@link DispatchProviderAndInterceptor} zurück, oder null, wenn der WollMux auf
   * diesen Frame noch keinen {@link DispatchProviderAndInterceptor} registriert hat;
   * Die Abfrage auf das interne Set der registrierten
   * {@link DispatchProviderAndInterceptor}-Objekten ist synchronisiert.
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private static DispatchProviderAndInterceptor getRegisteredDPI(XFrame frame)
  {
    if (frame == null) return null;
    synchronized (documentDispatchProviderAndInterceptors)
    {
      for (DispatchProviderAndInterceptor dpi : documentDispatchProviderAndInterceptors)
        if (dpi.frame != null && UnoRuntime.areSame(dpi.frame, frame)) return dpi;
    }
    return null;
  }
}
