/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.func;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.script.provider.ScriptFrameworkErrorException;
import com.sun.star.script.provider.XScript;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Eine durch ein ConfigThingy beschriebene externe (d,h, nicht als ConfigThingy
 * definierte) Funktion.
 */
public class ExternalFunction
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ExternalFunction.class);

  /**
   * Falls die Funktion eine Funktion des Scripting Frameworks ist, ist hier die
   * Referenz auf das Skript gespeichert.
   */
  private XScript script = null;

  /**
   * Falls die Funktion eine statische Java-Methode ist, so wird hier die Referenz
   * auf diese gespeichert.
   */
  private Method method = null;

  /**
   * Die Namen der Parameter, die die Funktion erwartet.
   */
  private String[] params;

  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * ExternalFunction, wobei zum Laden von Java-Klassen der selbe ClassLoader wie für
   * das Laden dieser Klasse verwendet wird.
   *
   * @throws ConfigurationErrorException
   *           falls die Spezifikation in conf fehlerhaft ist. TESTED
   */
  public ExternalFunction(ConfigThingy conf)
  {
    this(conf, null);
  }

  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * ExternalFunction, wobei zum Laden von Java-Klassen classLoader verwendet wird.
   *
   * @throws ConfigurationErrorException
   *           falls die Spezifikation in conf fehlerhaft ist. TESTED
   */
  public ExternalFunction(ConfigThingy conf, ClassLoader classLoader)
  {
    ClassLoader cLoader = (classLoader == null)
        ? this.getClass().getClassLoader()
        : classLoader;

    String url = conf.getString("URL");

    try
    {
      if (url.startsWith("java:"))
      {
        String classStr = url.substring(5, url.lastIndexOf('.'));
        String methodStr = url.substring(url.lastIndexOf('.') + 1);
        Class<?> c = cLoader.loadClass(classStr);
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i)
          if (methods[i].getName().equals(methodStr)
            && Modifier.isPublic(methods[i].getModifiers()))
          {
            if (method != null)
            {
              LOGGER.error("Klasse \"{}\" enthält 2 Methoden namens \"{}\"", classStr, methodStr);
              break;
            }
            method = methods[i];
          }

        if (method == null)
          throw new ConfigurationErrorException(L.m(
            "Klasse \"%1\" enthält keine PUBLIC Methode namens \"%2", classStr,
            methodStr));
      }
      else
      {
        script = UNO.masterScriptProvider.getScript(url);
      }
    }
    catch (ClassNotFoundException | ScriptFrameworkErrorException e)
    {
      throw new ConfigurationErrorException(
        L.m("Skript \"%1\" nicht verfügbar", url), e);
    }

    ConfigThingy paramsConf = conf.query("PARAMS");

    List<String> paramList = new ArrayList<>();
    for (ConfigThingy param : paramsConf)
    {
      for (ConfigThingy it : param)
      {
        String p = it.toString();
        paramList.add(p);
      }
    }

    params = paramList.toArray(new String[] {});
  }

  /**
   * Liefert die Namen der Parameter, die die Funktion erwartet.
   */
  public String[] parameters()
  {
    return params;
  }

  /**
   * Ruft die Funktion auf mit den String-Parametern aus parameters.
   *
   * @param parameters
   *          sollte zu jedem der von {@link #parameters()} gelieferten Namen einen
   *          String-Wert enthalten.
   * @return den Wert des Funktionsaufrufs oder null falls es ein Problem gab, das
   *         nicht zu einer Exception geführt hat.
   * @throws Exception
   *           falls ein Problem auftritt
   */
  public Object invoke(Values parameters) throws Exception
  {
    Object[] args = new Object[params.length];
    for (int i = 0; i < params.length; ++i)
      args[i] = parameters.getString(params[i]);
    return invoke(args);
  }

  /**
   * Ruft die Funktion auf mit den Argumenten args.
   *
   * @param args
   *          muss für jeden der von {@link #parameters()} gelieferten Namen einen
   *          Wert enthalten.
   * @return den Wert des Funktionsaufrufs oder null falls es ein Problem gab, das
   *         nicht zu einer Exception geführt hat.
   * @throws Exception
   *           falls ein Problem auftritt
   */
  public Object invoke(Object[] args) throws Exception
  {
    short[][] aOutParamIndex = new short[][] { new short[0] };
    Object[][] aOutParam = new Object[][] { new Object[0] };
    if (script != null)
    {
      Object result = script.invoke(args, aOutParamIndex, aOutParam);
      if (AnyConverter.isVoid(result)) {
        result = null;
      }
      return result;
    }
    else if (method != null)
    {
      return method.invoke(null, args);
    }
    return null;
  }
}
