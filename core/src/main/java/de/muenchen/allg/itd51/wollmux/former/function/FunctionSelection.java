/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.former.function;

import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.util.L;

public class FunctionSelection implements FunctionSelectionAccess
{

  /**
   * Dieser spezielle Funktionsname signalisiert, dass keine Funktion ausgewählt ist.
   */
  public static final String NO_FUNCTION = L.m("<keine>");

  /**
   * Dieser spezielle Funktionsname signalisiert, dass der Benutzer die Funktion manuell eingegeben
   * hat und sie direkt in dieser FunctionSelection gespeichert ist.
   */
  public static final String EXPERT_FUNCTION = L.m("<Experte>");

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
  private Map<String, ParamValue> mapNameToParamValue = new HashMap<>();

  /**
   * Erzeugt eine FunctionSelection für "keine Funktion".
   */
  public FunctionSelection()
  {}

  /**
   * Copy Constructor.
   */
  public FunctionSelection(FunctionSelection orig)
  {
    expertConf = new ConfigThingy(orig.expertConf);
    functionName = orig.functionName;
    this.paramNames = new String[orig.paramNames.length];
    System.arraycopy(orig.paramNames, 0, this.paramNames, 0, orig.paramNames.length);
    this.mapNameToParamValue = new HashMap<>(orig.mapNameToParamValue);
  }

  @Override
  public boolean isReference()
  {
    return !(isNone() || isExpert());
  }

  @Override
  public boolean isExpert()
  {
    return functionName.equals(EXPERT_FUNCTION);
  }

  @Override
  public boolean isNone()
  {
    return functionName.equals(NO_FUNCTION);
  }

  @Override
  public String getFunctionName()
  {
    return functionName;
  }

  @Override
  public String[] getParameterNames()
  {
    return paramNames;
  }

  @Override
  public void setParameterValues(Map<String, ParamValue> mapNameToParamValue)
  {
    this.mapNameToParamValue = mapNameToParamValue;
  }

  @Override
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

  @Override
  public ConfigThingy getExpertFunction()
  {
    return new ConfigThingy(expertConf);
  }

  @Override
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
   */
  public ConfigThingy export(String root)
  {
    return export(root, null);
  }

  /**
   * Liefert ein ConfigThingy, das diese FunctionSelection repräsentiert (ein leeres, falls keine
   * Funktion ausgewählt).
   *
   * @param root
   *          der Name des Wurzelknotens des zu liefernden ConfigThingys.
   * @param defaultBind
   *          falls nicht null, so werden alle unspezifizierten Parameter dieser FunctionSelection
   *          an VALUE("&lt;defaultBind&gt;") gebunden.
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

  @Override
  public boolean hasSpecifiedParameters()
  {
    for (int i = 0; i < paramNames.length; ++i)
    {
      ParamValue value = mapNameToParamValue.get(paramNames[i]);
      if (value != null && !value.isUnspecified()) return true;
    }
    return false;
  }

  @Override
  public ParamValue getParameterValue(String paramName)
  {
    ParamValue val = mapNameToParamValue.get(paramName);
    if (val == null) return ParamValue.unspecified();
    return new ParamValue(val);
  }

  @Override
  public void setParameterValue(String paramName, ParamValue paramValue)
  {
    mapNameToParamValue.put(paramName, paramValue);
  }

}
