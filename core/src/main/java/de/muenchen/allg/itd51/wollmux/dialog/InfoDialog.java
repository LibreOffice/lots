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
package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.MessageBoxType;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit2;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.frame.XFrame;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperRuntimeException;
import org.libreoffice.ext.unohelper.util.UnoComponent;

/**
 * Helper for creating simple, modal dialogs.
 */
public class InfoDialog
{

  private static final Logger LOGGER = LoggerFactory.getLogger(InfoDialog.class);

  private InfoDialog()
  {
    // hide constructor
  }

  /**
   * Show some information and wait until dialog has been closed. Uses the window of the current
   * frame.
   *
   * @param title
   *          The title.
   * @param message
   *          The information.
   */
  public static void showInfoModal(String title, String message)
  {
    XFrame frame = UNO.desktop.getCurrentFrame();
    if (frame == null)
    {
      LOGGER.debug("Frame isn't available");
      return;
    }
    XWindow window = frame.getContainerWindow();
    showInfoModal(UNO.XWindowPeer(window), title, message);
  }

  /**
   * Show some information and wait until dialog has been closed.
   *
   * @param windowPeer
   *          The calling window.
   * @param title
   *          The title.
   * @param message
   *          The information.
   */
  public static void showInfoModal(XWindowPeer windowPeer, String title, String message)
  {
    createDialog(windowPeer, title, message, MessageBoxType.INFOBOX, MessageBoxButtons.BUTTONS_OK);
  }

  /**
   * Show some information and wait until dialog has been closed and return the result. Uses the
   * window of the current frame.
   *
   * @param title
   *          The title.
   * @param message
   *          The information.
   * @return True if the dialog has been canceled, false otherwise.
   */
  public static boolean showCancelModal(String title, String message)
  {
    return showCancelModal(UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow()), title, message);
  }

  /**
   * Show some information and wait until dialog has been closed and return the result.
   *
   * @param windowPeer
   *          The calling window.
   * @param title
   *          The title.
   * @param message
   *          The information.
   * @return True if the dialog has been canceled, false otherwise.
   */
  public static boolean showCancelModal(XWindowPeer windowPeer, String title, String message)
  {
    short res = createDialog(windowPeer, title, message, MessageBoxType.MESSAGEBOX,
        MessageBoxButtons.BUTTONS_OK_CANCEL);
    return res == MessageBoxResults.CANCEL;
  }

  /**
   * Show some information and ask to user for submission. Wait until dialog has been closed and
   * return the result. Uses the window of the current frame.
   *
   * @param title
   *          The title.
   * @param message
   *          The information.
   * @return One of {@link MessageBoxResults}.
   */
  public static short showYesNoModal(String title, String message)
  {
    return showYesNoModal(UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow()), title, message);
  }

  /**
   * Show some information and ask to user for submission. Wait until dialog has been closed and
   * return the result.
   *
   * @param windowPeer
   *          The calling window.
   * @param title
   *          The title.
   * @param message
   *          The information.
   * @return One of {@link MessageBoxResults}.
   */
  public static short showYesNoModal(XWindowPeer windowPeer, String title, String message)
  {
    return createDialog(windowPeer, title, message, MessageBoxType.MESSAGEBOX,
        MessageBoxButtons.BUTTONS_YES_NO);
  }

  private static short createDialog(XWindowPeer windowPeer, String title, String message, MessageBoxType type,
      int buttons)
  {
    try
    {
      XToolkit2 toolkit = createToolkit();

      if (toolkit == null)
        return -1;

      XMessageBox messageBox = toolkit.createMessageBox(windowPeer, type, buttons, title, message);

      return messageBox.execute();
    } catch (NullPointerException e)
    {
      LOGGER.error("Info Dialog {} konnte nicht erstellt werden.", title);
      LOGGER.error("", e);
    }
    return -1;
  }

  private static XToolkit2 createToolkit()
  {
    XToolkit2 toolkit = null;
    try
    {
      toolkit = UNO.XToolkit2(UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_TOOLKIT));
    } catch (UnoHelperRuntimeException e)
    {
      LOGGER.error("", e);
    }

    return toolkit;
  }
}
