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
package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.MessageBoxType;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit2;
import com.sun.star.awt.XWindow;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperRuntimeException;
import de.muenchen.allg.util.UnoComponent;

public class InfoDialog
{

  private static final Logger LOGGER = LoggerFactory.getLogger(InfoDialog.class);

  private InfoDialog()
  {
    // hide constructor
  }

  /**
   * Diese Methode erzeugt einen modalen Dialog zur Anzeige von Informationen und kehrt erst nach
   * Beendigung des Dialogs wieder zurück.
   *
   * @param title
   *          Titelzeile des Dialogs
   * @param message
   *          die Nachricht, die im Dialog angezeigt werden soll.
   */
  public static void showInfoModal(XWindow window, String title, String message)
  {
    createDialog(window, title, message, MessageBoxType.INFOBOX, MessageBoxButtons.BUTTONS_OK);
  }

  public static void showInfoModal(String title, String message)
  {
    createDialog(title, message, MessageBoxType.INFOBOX, MessageBoxButtons.BUTTONS_OK);
  }

  /**
   * Diese Methode erzeugt einen modalen Dialog zur Anzeige von Informationen und kehrt erst nach
   * Beendigung mit dem Ergebnis zurück
   *
   * @param title
   *          Titelzeile des Dialogs
   * @param message
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @return true, wenn der Dialog abgebrochen wurde (Cancel oder X-Button), ansonsten false.
   */
  public static boolean showCancelModal(String title, String message)
  {
    short res = createDialog(title, message, MessageBoxType.MESSAGEBOX,
        MessageBoxButtons.BUTTONS_OK_CANCEL);
    return res == MessageBoxResults.CANCEL;
  }
  
  public static short showYesNoModal(String title, String message)
  {
    return createDialog(title, message, MessageBoxType.MESSAGEBOX,
        MessageBoxButtons.BUTTONS_YES_NO);
  }

  /**
   * Erzeugt eine Libreoffice MessageBox und führt sie aus.
   *
   * @param title
   *          Titelzeile des Dialogs
   * @param message
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param type
   *          Der Type der MessageBox {@link MessageBoxType}
   * @param buttons
   *          Die Buttons der MessageBox {@link MessageBoxButtons}
   * @return Ein MessageBoxResult {@link MessageBoxResults}
   */
  private static short createDialog(String title, String message, MessageBoxType type, int buttons)
  {
    try
    {
      XToolkit2 toolkit = createToolkit();

      if (toolkit == null)
        return -1;

      XMessageBox messageBox = toolkit.createMessageBox(
          UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow()), type, buttons, title,
          message);

      return messageBox.execute();
    } catch (NullPointerException e)
    {
      LOGGER.error("Info Dialog {} konnte nicht erstellt werden.", title);
      LOGGER.error("", e);
    }
    return -1;
  }

  private static short createDialog(XWindow window, String title, String message,
      MessageBoxType type, int buttons)
  {
    try
    {
      XToolkit2 toolkit = createToolkit();

      if (toolkit == null)
        return -1;

      XMessageBox messageBox = toolkit.createMessageBox(
          UNO.XWindowPeer(window), type, buttons, title,
          message);

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
      toolkit = UNO.XToolkit2(UnoComponent.createComponentWithContext(
          UnoComponent.CSS_AWT_TOOLKIT, UNO.defaultContext.getServiceManager(), UNO.defaultContext));
    } catch (UnoHelperRuntimeException e)
    {
      LOGGER.error("", e);
    }

    return toolkit;
  }
}
