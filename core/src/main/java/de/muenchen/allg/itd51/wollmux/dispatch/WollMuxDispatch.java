/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XStatusListener;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoProperty;

/**
 * A dispatch executed by WollMux.
 */
public abstract class WollMuxDispatch implements XDispatch
{
  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxDispatch.class);

  /**
   * The original dispatch of the command.
   */
  protected XDispatch origDisp;

  /**
   * The original command URL.
   */
  protected URL origUrl;

  /**
   * The frame on which the command should be executed.
   */
  protected XFrame frame;

  /**
   * Creates a new dispatch executed by WollMux.
   *
   * @param origDisp
   *          The original dispatch.
   * @param origUrl
   *          The original command URL.
   * @param frame
   *          The frame of the command.
   */
  public WollMuxDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    this.origDisp = origDisp;
    this.origUrl = origUrl;
    this.frame = frame;
  }

  @Override
  public void addStatusListener(XStatusListener listener, URL url)
  {
    if (origDisp != null)
    {
      origDisp.addStatusListener(listener, url);
    } else
    {
      FeatureStateEvent fse = new FeatureStateEvent();
      fse.FeatureURL = url;
      fse.IsEnabled = status();
      listener.statusChanged(fse);
    }
  }

  @Override
  public void removeStatusListener(XStatusListener listener, URL url)
  {
    if (origDisp != null)
    {
      origDisp.removeStatusListener(listener, url);
    }
  }

  /**
   * Can this dispatch be executed?
   *
   * If not overwritten calls {@link #isElementInPrintPreview()}.
   *
   * @return True if the dispatch can be executed, false otherwise.
   */
  public boolean status()
  {
    return isElementInPrintPreview();
  }

  /**
   * Does this dispatch need a frame.
   *
   * @return True, if it needs none. False otherwise.
   */
  public boolean isGlobal()
  {
    return false;
  }

  /**
   * Check if the frame is in print preview mode.
   *
   * @return True if print preview is active, false otherwise.
   */
  protected boolean isElementInPrintPreview()
  {
    boolean flag = false;
    XLayoutManager layout = UNO
        .XLayoutManager(Utils.getProperty(frame, UnoProperty.LAYOUT_MANAGER));
    if (layout != null)
    {
      flag = !layout
          .isElementVisible("private:resource/toolbar/previewobjectbar");
    }
    return flag;
  }

  /**
   * Get the arguments of the dispatch. (for the command
   * {@code wollmux:OpenTemplate#internerBriefkopf} the argument is {@code internerBriefkopf}.
   *
   * @return The argument of te dispatch.
   */
  protected String getMethodArgument()
  {
    String arg = "";
    String[] parts = origUrl.Complete.split("#", 2);
    if (parts.length == 2)
      arg = parts[1];

    try
    {
      arg = URLDecoder.decode(arg, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e)
    {
      LOGGER.error(L.m("Error in dispatch URL \"{0}\":", origUrl.Complete), e);
    }
    return arg;
  }

  /**
   * Check if SynchronMode is active.
   *
   * @param props
   *          The properties of the dispatch.
   * @return True if SynchronMode is active, false otherwise.
   */
  protected boolean isSynchronMode(PropertyValue[] props)
  {
    for (int index = 0; index < props.length; index++)
    {
      if ("SynchronMode".equals(props[index].Name))
      {
        return (Boolean) props[index].Value;
      }
    }
    return false;
  }
}
