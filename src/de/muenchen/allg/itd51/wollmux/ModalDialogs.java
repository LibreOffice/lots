package de.muenchen.allg.itd51.wollmux;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.Common;

public class ModalDialogs
{

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zurück. Der sichtbare Text
   * wird dabei ab einer Länge von 50 Zeichen automatisch umgebrochen.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   */
  public static void showInfoModal(java.lang.String sTitle, java.lang.String sMessage)
  {
    showInfoModal(sTitle, sMessage, 50);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zurück.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param margin
   *          ist margin > 0 und sind in einer Zeile mehr als margin Zeichen
   *          vorhanden, so wird der Text beim nächsten Leerzeichen umgebrochen.
   */
  public static void showInfoModal(java.lang.String sTitle,
      java.lang.String sMessage, int margin)
  {
    showDialog(sTitle, sMessage, margin, javax.swing.JOptionPane.INFORMATION_MESSAGE,
      javax.swing.JOptionPane.DEFAULT_OPTION);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige einer Frage
   * und kehrt erst nach Beendigung des Dialogs wieder zurück. Der sichtbare Text
   * wird dabei ab einer Länge von 50 Zeichen automatisch umgebrochen. 
   * Wenn die Benutzerin oder der Benutzer "OK" geklickt hat, gibt die Methode
   * true zurück, andernfalls false.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @return true wenn die Benutzerin oder der Benutzer "OK" geklickt hat, false sonst.
   */
  public static boolean showQuestionModal(java.lang.String sTitle, java.lang.String sMessage)
  {
    return showQuestionModal(sTitle, sMessage, 50);
  }

  /**
   * Diese Methode erzeugt einen modalen Swing-Dialog zur Anzeige von Informationen
   * und kehrt erst nach Beendigung des Dialogs wieder zurück.
   * Wenn die Benutzerin oder der Benutzer "OK" geklickt hat, gibt die Methode
   * true zurück, andernfalls false.
   * 
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param margin
   *          ist margin > 0 und sind in einer Zeile mehr als margin Zeichen
   *          vorhanden, so wird der Text beim nächsten Leerzeichen umgebrochen.
   * @return true wenn die Benutzerin oder der Benutzer "OK" geklickt hat, false sonst.
   */
  public static boolean showQuestionModal(java.lang.String sTitle,
      java.lang.String sMessage, int margin)
  {
    return showDialog(sTitle, sMessage, margin, javax.swing.JOptionPane.QUESTION_MESSAGE,
      javax.swing.JOptionPane.YES_NO_OPTION);
  }

  private static boolean showDialog(java.lang.String sTitle, 
      java.lang.String sMessage, int margin, int messageType, int optionType)
  {    
    boolean ret = false;
    try
    {
      // zu lange Strings ab margin Zeichen umbrechen:
      String formattedMessage = "";
      String[] lines = sMessage.split("\n");
      for (int i = 0; i < lines.length; i++)
      {
        String[] words = lines[i].split(" ");
        int chars = 0;
        for (int j = 0; j < words.length; j++)
        {
          String word = words[j];
          if (margin > 0 && chars > 0 && chars + word.length() > margin)
          {
            formattedMessage += "\n";
            chars = 0;
          }
          formattedMessage += word + " ";
          chars += word.length() + 1;
        }
        if (i != lines.length - 1) formattedMessage += "\n";
      }
  
      // infobox ausgeben:
      Common.setLookAndFeelOnce();
  
      JOptionPane pane =
        new JOptionPane(formattedMessage, messageType, optionType);
      JDialog dialog = pane.createDialog(null, sTitle);
      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true);
      Integer retValue = (Integer)pane.getValue();
      if (retValue.intValue() == 0){
        ret = true;
      }
      Logger.debug(retValue.toString());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    return ret;
  }

}
