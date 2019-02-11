package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.MessageBoxButtons;
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
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   */
  public static void showInfoModal(java.lang.String sTitle, java.lang.String sMessage)
  {
    try
    {
      XMultiComponentFactory xMCF = UNO.defaultContext.getServiceManager();
      XToolkit2 toolkit = UnoRuntime.queryInterface(XToolkit2.class,
          xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", UNO.defaultContext));
      XMessageBox messageBox = toolkit.createMessageBox(
          UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow()),
          MessageBoxType.INFOBOX, MessageBoxButtons.BUTTONS_OK, sTitle, sMessage);
      messageBox.execute();
    } catch (com.sun.star.uno.Exception | NullPointerException e)
    {
      LOGGER.error("Info Dialog {} konnte nicht erstellt werden.", sTitle, e);
    }
  }
}
