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

/**
 * Eine Bibliothek von benannten Dialogs.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DialogLibrary
{

  /**
   * Erzeugt eine leere Dialogsbibliothek. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DialogLibrary()
  {
  
  }
  
  /**
   * Erzeugt eine Dialogsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Dialogsbibliothek selbst keinen
   * Dialog des entsprechenden Namens enthält.  
   * @param baselib
   */
  public DialogLibrary(DialogLibrary baselib)
  {
  
  }

  /**
   * Fügt dialog dieser Dialogsbibliothek unter dem Namen dlgName hinzu.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public void add(String dlgName, Dialog dialog)
  {
  }

  /**
   * Liefert den Dialog namens dlgName zurück oder null, falls kein Dialog
   * mit diesem Namen bekannt ist. Wurde die Dialogsbibliothek mit einer
   * Referenz auf eine andere Bibliothek initialisiert, so wird diese
   * befragt, falls die Dialogsbibliothek selbst keinen Dialog des entsprechenden
   * Namens kennt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public Dialog get(String dlgName)
  {
    return null;
  }
  
}
