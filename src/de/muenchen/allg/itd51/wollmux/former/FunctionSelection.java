/*
* Dateiname: FunctionSelection.java
* Projekt  : WollMux
* Funktion : Speichert eine Funktionsauswahl/-def/-konfiguration des Benutzers
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
package de.muenchen.allg.itd51.wollmux.former;

import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.parser.ConfigThingy;

public class FunctionSelection implements FunctionSelectionAccess
{
  /**
   * Leere Liste von Parameternamen.
   */
  private static final String[] NO_PARAM_NAMES = new String[]{};
  
  /**
   * Falls der Benutzer im Experten-Modus eine Funktionsdefinition selbst geschrieben hat,
   * so wird diese hier abgelegt. Das ConfigThingy hat immer einen Wurzelknoten der keine
   * Basisfunktion ist, d.h. "AUTOFILL", "PLAUSI" oder ähnliches.
   */
  private ConfigThingy expertConf = new ConfigThingy("EXPERT");
  
  /**
   * Der Name der vom Benutzer ausgewählten Funktion.
   */
  private String functionName = NO_FUNCTION;
  
  /**
   * Die Namen aller Parameter, die die Funktion erwartet.
   */
  private String[] paramNames = NO_PARAM_NAMES;
  
  /**
   * Mapped die Namen der Funktionsparameter auf die vom Benutzer konfigurierten Werte
   * als {@link ParamValue} Objekte. Achtung! Diese Map enthält alle jemals vom Benutzer
   * gesetzten Werte, nicht nur die für die aktuelle Funktion. Auf diese Weise kann der
   * Benutzer die Funktion wechseln, ohne dadurch seine Eingaben von früher zu verlieren.
   * Bei Funktionen mit Parametern des selben Namens kann dies je nachdem ob der Name bei
   * beiden Funktionen für das selbe steht oder nicht zu erwünschter
   * Erleichterung oder zu unerwünschter Verwirrung führen.
   */
  private Map mapNameToParamValue = new HashMap();
  
  /**
   * Erzeugt eine FunctionSelection für "keine Funktion".
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionSelection() {}
  
  /**
   * Copy Constructor.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionSelection(FunctionSelection orig)
  {
    expertConf = new ConfigThingy(orig.expertConf);
    functionName = orig.functionName;
    this.paramNames = new String[orig.paramNames.length];
    System.arraycopy(orig.paramNames, 0, this.paramNames, 0, orig.paramNames.length);
    this.mapNameToParamValue = new HashMap(orig.mapNameToParamValue);
  }
  
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#isReference()
   * TODO Testen
   */
  public boolean isReference()
  {
    return (functionName != NO_FUNCTION && functionName != EXPERT_FUNCTION);
  }
  
  public boolean isExpert()
  {
    return functionName == EXPERT_FUNCTION;
  }
  
  public boolean isNone()
  {
    return functionName == NO_FUNCTION;
  }
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#getName()
   * TODO Testen
   */
  public String getName() { return functionName;}
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#setParameterValues(java.util.Map)
   * TODO Testen
   */
  public void setParameterValues(Map mapNameToParamValue)
  {
    this.mapNameToParamValue = mapNameToParamValue;
  }
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#setFunction(java.lang.String, java.lang.String[])
   * TODO Testen
   */
  public void setFunction(String functionName, String[] paramNames)
  {
    if (functionName.equals(EXPERT_FUNCTION))
    {
      this.functionName = EXPERT_FUNCTION;
      this.paramNames = NO_PARAM_NAMES;
    }
    else if (functionName.equals(NO_FUNCTION))
    {
      this.functionName = NO_FUNCTION;
      this.paramNames = NO_PARAM_NAMES;
    }
    else
    {
      this.paramNames = new String[paramNames.length];
      System.arraycopy(paramNames, 0, this.paramNames, 0, paramNames.length);
      this.functionName = functionName;
    }
  }
  
  /*
   * TODO Testen
   */
  public ConfigThingy getExpertFunction()
  {
    return expertConf;
  }
  
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#setExpertFunction(de.muenchen.allg.itd51.parser.ConfigThingy)
   * TODO Testen
   */
  public void setExpertFunction(ConfigThingy funConf)
  {
    this.functionName = EXPERT_FUNCTION;
    this.paramNames = NO_PARAM_NAMES;
    this.expertConf = new ConfigThingy(funConf);
  }

}
