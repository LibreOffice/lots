/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.dispatch;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * A dispatch provider and interceptor for LibreOffice dispatches.
 */
public class DispatchProviderAndInterceptor implements XDispatchProvider,
    XDispatchProviderInterceptor
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DispatchProviderAndInterceptor.class);

  /**
   * Dispatch provider for global, not file based, dispatches.
   */
  public static final XDispatchProviderInterceptor globalWollMuxDispatches =
    new DispatchProviderAndInterceptor();

  /**
   * Collection of all registered file based interceptors. It is used to unregister the interceptors
   * when a document is closed and register only one per document.
   */
  private static final Set<DispatchProviderAndInterceptor> documentDispatchProviderAndInterceptors =
      new HashSet<>();

  private XDispatchProvider slave = null;

  private XDispatchProvider master = null;

  /**
   * If not null, this provider can handle file based dispatches.
   */
  private XFrame frame = null;

  /**
   * Close-Listener on the frame.
   */
  private XFrameActionListener frameActionListener = null;

  private static ServiceLoader<Dispatcher> dispatchers = ServiceLoader.load(Dispatcher.class,
      Dispatcher.class.getClassLoader());

  /**
   * Erzeugt einen {@link DispatchProviderAndInterceptor}, der nur globale URLs
   * behandeln kann.
   */
  /**
   * New dispatcher for global dispatches.
   */
  private DispatchProviderAndInterceptor()
  {
    // nothing to do
  }

  /**
   * New dispatcher for global and file based dispatches.
   *
   * @param frame
   *          The frame of the file.
   */
  private DispatchProviderAndInterceptor(XFrame frame)
  {
    this.frame = frame;
  }

  @Override
  public XDispatchProvider getSlaveDispatchProvider()
  {
    return slave;
  }

  @Override
  public void setSlaveDispatchProvider(XDispatchProvider slave)
  {
    this.slave = slave;
  }

  @Override
  public XDispatchProvider getMasterDispatchProvider()
  {
    return master;
  }

  @Override
  public void setMasterDispatchProvider(XDispatchProvider master)
  {
    this.master = master;
  }

  @Override
  public XDispatch queryDispatch(URL url, String frameName, int fsFlag)
  {
    for (Dispatcher dispatcher : dispatchers)
    {
      if (dispatcher.supports(url))
      {
	WollMuxDispatch dispatch = dispatcher
	    .create(getOrigDispatch(url, frameName, fsFlag), url, frame);
	if (dispatch.isGlobal() || (!dispatch.isGlobal() && frame != null))
	{
	  return dispatch;
	}
      }
    }

    return getOrigDispatch(url, frameName, fsFlag);
  }

  @Override
  public XDispatch[] queryDispatches(DispatchDescriptor[] seqDescripts)
  {
    int nCount = seqDescripts.length;
    XDispatch[] lDispatcher = new XDispatch[nCount];

    for (int i = 0; i < nCount; ++i)
    {
      lDispatcher[i] = queryDispatch(seqDescripts[i].FeatureURL, seqDescripts[i].FrameName,
          seqDescripts[i].SearchFlags);
    }
    return lDispatcher;
  }

  /**
   * Get the original dispatch of the registered slave dispatch provider (@see
   * {@link XDispatchProvider#queryDispatch(URL, String, int)})
   *
   * @param url
   *          The command URL.
   * @param frameName
   *          The target frame.
   * @param fsFlag
   *          Optional search parameter for finding the frame.
   * @return The original dispatch or null if there is no slave.
   */
  public XDispatch getOrigDispatch(URL url, String frameName,
      int fsFlag)
  {
    if (slave != null)
      return slave.queryDispatch(url, frameName, fsFlag);
    else
      return null;
  }

  /**
   * Register a dispatch provider on the frame if it is not already registered.
   *
   * @param frame
   *          The frame.
   */
  public static void registerDocumentDispatchInterceptor(XFrame frame)
  {
    if (frame == null || UNO.XDispatchProviderInterception(frame) == null
      || UNO.XDispatchProvider(frame) == null) return;

    // register interceptor if not already registered
    if (getRegisteredDPI(frame) == null)
    {
      DispatchProviderAndInterceptor dpi = new DispatchProviderAndInterceptor(frame);

      LOGGER.debug("Registriere DocumentDispatchInterceptor #{} für frame #{}",
          Integer.valueOf(dpi.hashCode()), Integer.valueOf(frame.hashCode()));

      UNO.XDispatchProviderInterception(frame).registerDispatchProviderInterceptor(
        dpi);
      registerDPI(dpi);

      dpi.frameActionListener = new DPIFrameActionListener();
      frame.addFrameActionListener(dpi.frameActionListener);
    }
    else
      LOGGER.debug(
          "Ignoriere doppelten Aufruf von registerDocumentDispatchInterceptor() für den selben Frame #{}",
          Integer.valueOf(frame.hashCode()));
  }

  /**
   * Listener on frames to get notified when they are closed, so that the interceptor can be
   * unregistered.
   */
  private static class DPIFrameActionListener implements XFrameActionListener
  {
    @Override
    public void disposing(EventObject e)
    {
      deregisterDPI(getRegisteredDPI(UNO.XFrame(e.Source)));
    }

    @Override
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

          LOGGER.debug("Deregistrierung von DocumentDispatchInterceptor #{} aus frame #{}",
              Integer.valueOf(dpi.hashCode()), Integer.valueOf(dpi.frame.hashCode()));
          UNO.XDispatchProviderInterception(dpi.frame).releaseDispatchProviderInterceptor(
            dpi);
          dpi.frame.contextChanged();
          deregisterDPI(dpi);
        }
      }
    }

    /**
     * Remove interceptor as it is not longer registered.
     *
     * @param dpi
     *          The interceptor to unregister.
     */
    private static void deregisterDPI(DispatchProviderAndInterceptor dpi)
    {
      if (dpi == null)
      {
        return;
      }
      synchronized (documentDispatchProviderAndInterceptors)
      {
        LOGGER.debug("Interne Freigabe des DocumentDispatchInterceptor #{}",
            Integer.valueOf(dpi.hashCode()));
        documentDispatchProviderAndInterceptors.remove(dpi);
      }
    }
  }

  /**
   * Add a new interceptor, which has been registered on a frame.
   *
   * @param dpi
   *          A new interceptor.
   */
  private static void registerDPI(DispatchProviderAndInterceptor dpi)
  {
    if (dpi == null)
    {
      return;
    }
    synchronized (documentDispatchProviderAndInterceptors)
    {
      documentDispatchProviderAndInterceptors.add(dpi);
    }
  }

  /**
   * Get an interceptor for a frame.
   *
   * @param frame
   *          The frame.
   * @return The interceptor created by WollMux or null if WollMux has no interceptor on this frame.
   */
  private static DispatchProviderAndInterceptor getRegisteredDPI(XFrame frame)
  {
    if (frame == null)
    {
      return null;
    }
    synchronized (documentDispatchProviderAndInterceptors)
    {
      for (DispatchProviderAndInterceptor dpi : documentDispatchProviderAndInterceptors)
      {
        if (dpi.frame != null && UnoRuntime.areSame(dpi.frame, frame))
        {
          return dpi;
        }
      }
    }
    return null;
  }
}
