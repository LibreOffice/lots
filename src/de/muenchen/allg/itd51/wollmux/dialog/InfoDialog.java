package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.MessageBoxType;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit2;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;

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
      XMultiComponentFactory xMCF = UNO.defaultContext.getServiceManager();
      XToolkit2 toolkit = UnoRuntime.queryInterface(XToolkit2.class,
          xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", UNO.defaultContext));
      XMessageBox messageBox = toolkit.createMessageBox(
          UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow()), type, buttons, title,
          message);
      return messageBox.execute();
    } catch (com.sun.star.uno.Exception | NullPointerException e)
    {
      LOGGER.error("Info Dialog {} konnte nicht erstellt werden.", title);
      LOGGER.error("", e);
    }
    return -1;
  }
}
