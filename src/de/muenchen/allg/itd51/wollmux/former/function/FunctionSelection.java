/*
 * Dateiname: FunctionSelection.java
 * Projekt  : WollMux
 * Funktion : Speichert eine Funktionsauswahl/-def/-konfiguration des Benutzers
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 25.09.2006 | BNK | Erstellung
 * 16.03.2007 | BNK | +updateFieldReferences()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.function;

import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class FunctionSelection implements FunctionSelectionAccess
{
  /**
   * Leere Liste von Parameternamen.
   */
  private static final String[] NO_PARAM_NAMES = new String[] {};

  /**
   * Falls der Benutzer im Experten-Modus eine Funktionsdefinition selbst geschrieben
   * hat, so wird diese hier abgelegt. Das ConfigThingy hat immer einen Wurzelknoten
   * der keine Basisfunktion ist, d.h. "AUTOFILL", "PLAUSI" oder ähnliches.
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
   * Mapped die Namen der Funktionsparameter auf die vom Benutzer konfigurierten
   * Werte als {@link ParamValue} Objekte. Achtung! Diese Map enthält alle jemals vom
   * Benutzer gesetzten Werte, nicht nur die für die aktuelle Funktion. Auf diese
   * Weise kann der Benutzer die Funktion wechseln, ohne dadurch seine Eingaben von
   * früher zu verlieren. Bei Funktionen mit Parametern des selben Namens kann dies
   * je nachdem ob der Name bei beiden Funktionen für das selbe steht oder nicht zu
   * erwünschter Erleichterung oder zu unerwünschter Verwirrung führen.
   */
  private Map<String, ParamValue> mapNameToParamValue =
    new HashMap<String, ParamValue>();

  /**
   * Erzeugt eine FunctionSelection für "keine Funktion".
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FunctionSelection()
  {}

  /**
   * Copy Constructor.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelection(FunctionSelection orig)
  {
    expertConf = new ConfigThingy(orig.expertConf);
    functionName = orig.functionName;
    this.paramNames = new String[orig.paramNames.length];
    System.arraycopy(orig.paramNames, 0, this.paramNames, 0, orig.paramNames.length);
    this.mapNameToParamValue =
      new HashMap<String, ParamValue>(orig.mapNameToParamValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#isReference()
   *      TESTED
   */
  public boolean isReference()
  {
    return !(isNone() || isExpert());
  }

  public boolean isExpert()
  {
    return functionName.equals(EXPERT_FUNCTION);
  }

  public boolean isNone()
  {
    return functionName.equals(NO_FUNCTION);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#getName()
   */
  public String getFunctionName()
  {
    return functionName;
  }

  public String[] getParameterNames()
  {
    return paramNames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#setParameterValues(java.util.Map)
   */
  public void setParameterValues(Map<String, ParamValue> mapNameToParamValue)
  {
    this.mapNameToParamValue = mapNameToParamValue;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#setFunction(java.lang.String,
   *      java.lang.String[]) TESTED
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

  public ConfigThingy getExpertFunction()
  {
    return new ConfigThingy(expertConf);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.former.FunctionSelectionAccess#setExpertFunction(de.muenchen.allg.itd51.parser.ConfigThingy)
   *      TESTED
   */
  public void setExpertFunction(ConfigThingy funConf)
  {
    this.functionName = EXPERT_FUNCTION;
    this.paramNames = NO_PARAM_NAMES;
    this.expertConf = new ConfigThingy(funConf);
  }

  /**
   * Liefert ein ConfigThingy, das diese FunctionSelection repräsentiert (ein leeres,
   * falls keine Funktion ausgewählt).
   * 
   * @param root
   *          der Name des Wurzelknotens des zu liefernden ConfigThingys.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public ConfigThingy export(String root)
  {
    return export(root, null);
  }

  /**
   * Liefert ein ConfigThingy, das diese FunctionSelection repräsentiert (ein leeres,
   * falls keine Funktion ausgewählt).
   * 
   * @param root
   *          der Name des Wurzelknotens des zu liefernden ConfigThingys.
   * @param defaultBind
   *          falls nicht null, so werden alle unspezifizierten Parameter dieser
   *          FunctionSelection an VALUE("&lt;defaultBind>") gebunden.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public ConfigThingy export(String root, String defaultBind)
  {
    ConfigThingy rootConf = new ConfigThingy(root);

    if (isReference())
    {
      ConfigThingy conf = rootConf.add("BIND");
      conf.add("FUNCTION").add(getFunctionName());
      String[] params = getParameterNames();
      for (int i = 0; i < params.length; ++i)
      {
        ParamValue value = mapNameToParamValue.get(params[i]);
        if (value != null && !value.isUnspecified())
        {
          ConfigThingy set = conf.add("SET");
          set.add(params[i]);
          if (value.isFieldReference())
          {
            set.add("VALUE").add(value.getString());
          }
          else
          {
            set.add(value.getString());
          }
        }
        else if (defaultBind != null)
        {
          ConfigThingy set = conf.add("SET");
          set.add(params[i]);
          set.add("VALUE").add(defaultBind);
        }
      }
    }
    else if (isExpert())
    {
      rootConf = getExpertFunction();
      rootConf.setName(root);
    }

    return rootConf;
  }

  public boolean hasSpecifiedParameters()
  {
    for (int i = 0; i < paramNames.length; ++i)
    {
      ParamValue value = mapNameToParamValue.get(paramNames[i]);
      if (value != null && !value.isUnspecified()) return true;
    }
    return false;
  }

  public ParamValue getParameterValue(String paramName)
  {
    ParamValue val = mapNameToParamValue.get(paramName);
    if (val == null) return ParamValue.unspecified();
    return new ParamValue(val);
  }

  public void setParameterValue(String paramName, ParamValue paramValue)
  {
    mapNameToParamValue.put(paramName, paramValue);
  }

}
