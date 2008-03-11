/*
* Dateiname: DialogLibrary.java
* Projekt  : WollMux
* Funktion : Eine Bibliothek von benannten Dialogs
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 03.05.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.L;

/**
 * Eine Bibliothek von benannten Dialogs.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DialogLibrary
{
  private Map<String, Dialog> mapIdToDialog = new HashMap<String, Dialog>();
  private DialogLibrary baselib;
  
  
  /**
   * Erzeugt eine leere Dialogsbibliothek. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DialogLibrary(){}
  
  /**
   * Erzeugt eine Dialogsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Dialogsbibliothek selbst keinen
   * Dialog des entsprechenden Namens enthält.  
   * @param baselib
   */
  public DialogLibrary(DialogLibrary baselib)
  {
    this.baselib = baselib;
  }

  /**
   * Fügt dialog dieser Dialogsbibliothek unter dem Namen dlgName hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(String dlgName, Dialog dialog)
  {
    if (dialog == null || dlgName == null) throw new NullPointerException(L.m("Weder Dialogname noch Dialog darf null sein"));
    mapIdToDialog.put(dlgName, dialog);
  }

  /**
   * Liefert den Dialog namens dlgName zurück oder null, falls kein Dialog
   * mit diesem Namen bekannt ist. Wurde die Dialogsbibliothek mit einer
   * Referenz auf eine andere Bibliothek initialisiert, so wird diese
   * befragt, falls die Dialogsbibliothek selbst keinen Dialog des entsprechenden
   * Namens kennt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Dialog get(String dlgName)
  {
    Dialog dialog = mapIdToDialog.get(dlgName);
    if (dialog == null && baselib != null) dialog = baselib.get(dlgName);
    return dialog;
  }
  
}
