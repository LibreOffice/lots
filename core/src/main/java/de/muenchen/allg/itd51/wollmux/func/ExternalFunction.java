/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * An external described by a ConfigThingy (i.e. not as a ConfigThingy
 * defined) function.
 */
public class ExternalFunction
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ExternalFunction.class);

  /**
   * If the function is a Scripting Framework function, here is it
   * Reference saved to the script.
   */
  private XScript script = null;

  /**
   * If the function is a static Java method, then here is the reference
   * stored on this.
   */
  private Method method = null;

  /**
   * The names of the parameters that the function expects.
   */
  private String[] params;

  /**
   * Generates a ConfigThingy (the EXTERN node is transferred).
   * ExternalFunction, using the same ClassLoader for loading Java classes as for
   * loading this class is used.
   *
   * @throws ConfigurationErrorException
   *           if the specification in conf is wrong. TESTED
   */
  public ExternalFunction(ConfigThingy conf)
  {
    this(conf, null);
  }

  /**
   * Generates a ConfigThingy (the EXTERN node is transferred).
   * ExternalFunction, using classLoader to load Java classes.
   *
   * @throws ConfigurationErrorException
   *           if the specification in conf is wrong. TESTED
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
              LOGGER.error("Class '{}' contains 2 methods named '{}'", classStr, methodStr);
              break;
            }
            method = methods[i];
          }

        if (method == null)
          throw new ConfigurationErrorException(L.m(
            "Class \"{0}\" contains no PUBLIC method called \"{1}", classStr,
            methodStr));
      }
      else
      {
        script = UNO.mainScriptProvider.getScript(url);
      }
    }
    catch (ClassNotFoundException | ScriptFrameworkErrorException e)
    {
      throw new ConfigurationErrorException(
        L.m("Script \"{0}\" not available", url), e);
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
   * Returns the names of the parameters expected by the function.
   */
  public String[] parameters()
  {
    return params;
  }

  /**
   * Calls the function with the string parameters from parameters.
   *
   * @param parameters
   *          should be one for each of the names returned by {@link #parameters()}
   *          String value included.
   * @return the value of the function call, or null if there was a problem that
   *         did not result in an exception.
   * @throws Exception
   *           if there is a problem
   */
  public Object invoke(Values parameters) throws Exception
  {
    Object[] args = new Object[params.length];
    for (int i = 0; i < params.length; ++i)
      args[i] = parameters.getString(params[i]);
    return invoke(args);
  }

  /**
   * Calls the function with arguments args.
   *
   * @param args
   *          must be one for each of the names returned by {@link #parameters()}
   *          Value included.
   * @return the value of the function call, or null if there was a problem that
   *         did not result in an exception.
   * @throws Exception
   *           if there is a problem
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
