/*
* Dateiname: FunctionSelectionProvider.java
* Projekt  : WollMux
* Funktion : Liefert {@link FunctionSelection}s.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 25.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former.function;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

/**
 * Liefert {@link FunctionSelection}s.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionSelectionProvider
{
  /**
   * Eine Funktionsbibliothek, die Funktionen liefert zu denen der FunctionSelectionProvider
   * FunctionSelections produzieren kann.
   */
  private FunctionLibrary funcLib;
  
  /**
   * Bildet Funktionsnamen auf dazugehörige FunctionSelections ab.
   */
  private Map mapNameToFunctionSelection;
  
  /**
   * Erzeugt einen FunctionSelectionProvider, der {@link FunctionSelection}s sowohl zu
   * Funktionen aus funcLib als auch zu Funktionen, die funConf (welches ein
   * legaler "Funktionen"-Abschnitt eines Formulars sein muss) definiert liefern kann.
   * Bei gleichem Namen haben Funktionen aus funConf vorrang vor solchen aus funcLib.
   * ACHTUNG! Die Funktionsnamen aus funConf sind NICHT zwangsweise die Funktionsnamen für die
   * FunctionSelections. Die FunctionSelections kriegen ihre Funktionsnamen aus den
   * FUNCTION Attributen der geparsten BIND-Funktionen. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public FunctionSelectionProvider(FunctionLibrary funcLib, ConfigThingy funConf)
  {
    this.funcLib = funcLib;
    mapNameToFunctionSelection = new HashMap();
    Iterator iter = funConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy conf = (ConfigThingy)iter.next();
      FunctionSelection funcSel = getFunctionSelection(conf);
      if (funcSel.isReference())
        mapNameToFunctionSelection.put(funcSel.getName(), funcSel);
    }
  }
  
  /**
   * Liefert eine FunctionSelection zur Funktion mit Name funcName. Falls funcName diesem
   * FunctionSelectionProvider nicht bekannt ist, wird trotzdem eine FunctionSelection
   * geliefert, falls vorhanden mit den Parameterinformationen aus der {@link FunctionLibrary},
   * die dem Konstruktor übergeben wurde ansonsten mit einer leeren Parameterliste.
   * Die gelieferte FunctionSelection ist auf jeden Fall neu erstellt und unabhängig von
   * anderen für den selben Namen früher gelieferten FunctionSelections.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public FunctionSelection getFunctionSelection(String funcName)
  {
    FunctionSelection funcSel = (FunctionSelection)mapNameToFunctionSelection.get(funcName);
    if (funcSel != null)
      return new FunctionSelection(funcSel);
    else
    {
      funcSel = new FunctionSelection();
      Function func = funcLib.get(funcName);
      if (func != null)
        funcSel.setFunction(funcName, func.parameters());
      else
        funcSel.setFunction(funcName, new String[]{});
      return funcSel;
    }
  }
  
  /**
   * Liefert zu einer Funktionsdefinition in conf eine FunctionSelection. conf muss einen
   * beliebigen Wurzelknoten haben, der noch keine Grundfunktion ist 
   * (z.B. "PLAUSI" oder "AUTOFILL").
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public FunctionSelection getFunctionSelection(ConfigThingy conf)
  {
    FunctionSelection funcSel = new FunctionSelection();
    
    if (conf.count() == 0) return funcSel;
    
    funcSel.setExpertFunction(conf);
    
    if (conf.count() != 1 || !((ConfigThingy)conf.iterator().next()).getName().equals("BIND"))
      return funcSel;
      
    /**
     * Wir haben es mit einem einzelnen BIND zu tun. Wir versuchen, diesen zu parsen.
     * Wenn wir das nicht können, dann liefern wir das ganze als Expert-Funktion zurück,
     * ansonsten setzen wir das BIND entsprechend um.
     */
    Map mapNameToParamValue = new HashMap();
    String funcName = FunctionSelection.NO_FUNCTION;
    Iterator iter = ((ConfigThingy)conf.iterator().next()).iterator();
    while (iter.hasNext())
    {
      ConfigThingy childConf = (ConfigThingy)iter.next();
      String name = childConf.getName();
      if (name.equals("FUNCTION"))
        funcName = childConf.toString();
      else if (name.equals("SET"))
      {
        if (childConf.count() != 2) return funcSel;
        String paramName = null;
        ConfigThingy paramConf = null;
        try{ 
          paramName = childConf.getFirstChild().toString(); 
          paramConf = childConf.getLastChild();
        } catch(Exception x){};
        
        ParamValue paramValue;
        
        if (paramConf.count() == 0)
          paramValue = ParamValue.literal(paramConf.toString());
        else if (paramConf.count() == 1 && paramConf.getName().equals("VALUE"))
          paramValue = ParamValue.field(paramConf.toString());
        else
          return funcSel;
          
        mapNameToParamValue.put(paramName, paramValue);
          
      } else return funcSel;
    }
    
    Function func = funcLib.get(funcName);
    if (func != null)
    {
      funcSel.setFunction(funcName, func.parameters());
    }
    else
    {
      funcSel.setFunction(funcName, (String[])mapNameToParamValue.keySet().toArray(new String[]{}));
    }
    
    funcSel.setParameterValues(mapNameToParamValue);
    
    return funcSel;
  }
}

