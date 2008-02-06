/*
* Dateiname: TrafoDialogFactory.java
* Projekt  : WollMux
* Funktion : Erzeugt zu einem Satz von Parametern einen passenden TrafoDialog.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 01.02.2008 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.lang.reflect.Constructor;

import de.muenchen.allg.itd51.wollmux.UnavailableException;

/**
 * Erzeugt zu einem Satz von Parametern einen passenden TrafoDialog.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TrafoDialogFactory
{
  private static final Class[] dialogClasses = {IfThenElseDialog.class};
  private static final Class[] DIALOG_CONSTRUCTOR_SIGNATURE = {TrafoDialogParameters.class}; 
  
  /**
   * Versucht, einen zu params passenden Dialog zu instanziieren und liefert ihn
   * zurück, falls es klappt.
   * 
   * @throws UnavailableException wenn kein passender Dialog gefunden wurde.
   *  
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static TrafoDialog createDialog(TrafoDialogParameters params)
  throws UnavailableException
  {
    Object[] oparams = {params};
    for (int i = 0; i < dialogClasses.length; ++i)
    {
      try{
        Constructor cons = dialogClasses[i].getConstructor(DIALOG_CONSTRUCTOR_SIGNATURE);
        return (TrafoDialog)cons.newInstance(oparams);
      }
      catch(Exception x){};
    }
    
    throw new UnavailableException("Kein Dialog verfügbar für die übergebenen Parameter:"+params);
  }
  
}
