/*
 * Dateiname: ExternalFunction.java
 * Projekt  : WollMux
 * Funktion : Eine durch ein ConfigThingy beschriebene externe Funktion.
 *
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
 * 25.01.2006 | BNK | Erstellung
 * 05.12.2006 | BNK | ClassLoader kann übergeben werden
 *                  | +invoke(Object[])
 * 28.12.2006 | BNK | nur noch public Methoden als ExternalFunctions finden.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.core.functions;

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
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

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
              LOGGER.error(L.m("Klasse \"%1\" enthält 2 Methoden namens \"%2\"",
                classStr, methodStr));
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
