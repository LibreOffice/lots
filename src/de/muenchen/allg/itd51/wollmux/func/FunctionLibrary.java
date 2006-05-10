/*
* Dateiname: FunctionLibrary.java
* Projekt  : WollMux
* Funktion : Eine Bibliothek von benannten Functions
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 03.05.2006 | BNK | Erstellung
* 08.05.2006 | BNK | Fertig implementiert.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

import java.util.HashMap;
import java.util.Map;

/**
 * Eine Bibliothek von benannten Functions
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionLibrary
{
  private Map mapIdToFunction;
  private FunctionLibrary baselib;
  
  /**
   * Erzeugt eine leere Funktionsbibliothek. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionLibrary()
  {
    mapIdToFunction = new HashMap();
    baselib = null;
  }
  
  /**
   * Erzeugt eine Funktionsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Funktionsbibliothek selbst keine
   * Funktion des entsprechenden Namens enthält.  
   * @param baselib
   */
  public FunctionLibrary(FunctionLibrary baselib)
  {
    mapIdToFunction = new HashMap();
    this.baselib = baselib; 
  }

  /**
   * Fügt func dieser Funktionsbibliothek unter dem Namen funcName hinzu.
   * Eine bereits existierende Funktion mit diesem Namen wird dabei ersetzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(String funcName, Function func)
  {
    if (func == null || funcName == null) throw new NullPointerException("Weder Funktionsname noch Funktion darf null sein");
    mapIdToFunction.put(funcName, func);
  }

  /**
   * Liefert die Function namens funcName zurück oder null, falls keine Funktion
   * mit diesem Namen bekannt ist. Wurde die Funktionsbibliothek mit einer
   * Referenz auf eine andere Funktionsbibliothek initialisiert, so wird diese
   * befragt, falls die Funktionsbibliothek selbst keine Funktion des entsprechenden
   * Namens kennt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Function get(String funcName)
  {
    Function func = (Function)mapIdToFunction.get(funcName);
    if (func == null && baselib != null) func = baselib.get(funcName);
    return func;
  }
  
}
