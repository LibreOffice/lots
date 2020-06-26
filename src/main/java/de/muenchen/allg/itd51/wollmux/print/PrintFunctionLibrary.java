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
package de.muenchen.allg.itd51.wollmux.print;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxClassLoader;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.func.ExternalFunction;
import de.muenchen.allg.itd51.wollmux.func.print.PrintException;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Library of {@link PrintFunction}.
 */
public class PrintFunctionLibrary
{

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintFunctionLibrary.class);

  /**
   * Mapping function names to {@link PrintFunction} implementations.
   */
  private Map<String, PrintFunction> mapIdToFunction;

  /**
   * A base library with more print functions. The base library is asked for a function, if this
   * library doesn't contains a function with the requested name.
   */
  private PrintFunctionLibrary baselib;

  /**
   * Default value for print functions without ORDER-attribute in configuration.
   */
  private static final String DEFAULT_PRINTFUNCTION_ORDER_VALUE = "100";

  /**
   * Creates an empty library.
   */
  public PrintFunctionLibrary()
  {
    this(null);
  }

  /**
   * Create a library which uses {@link #baselib}.
   *
   * @param baselib
   *          A {@link PrintFunctionLibrary}
   */
  public PrintFunctionLibrary(PrintFunctionLibrary baselib)
  {
    mapIdToFunction = new HashMap<>();
    this.baselib = baselib;
  }

  /**
   * Add a new print function to the library. If a function with this name already exists, it is
   * overwritten.
   *
   * @param funcName
   *          The name of the print function.
   * @param func
   *          The implementation of the print function.
   */
  public void add(String funcName, PrintFunction func)
  {
    if (func == null || funcName == null)
      throw new NullPointerException(L.m("Weder Funktionsname noch Funktion darf null sein"));
    mapIdToFunction.put(funcName, func);
  }

  /**
   * Search for a print function with the given name. If this library doesn't contain a function and
   * there is a {@link #baselib}, in {@link #baselib} the function is searched.
   *
   * @param funcName
   *          The name of the function to search.
   * @return The {@link PrintFunction} of null if there is no function with this name.
   */
  public PrintFunction get(String funcName)
  {
    PrintFunction func = mapIdToFunction.get(funcName);
    if (func == null && baselib != null)
      func = baselib.get(funcName);
    return func;
  }

  /**
   * Get all function names accessible by this library.
   *
   * @return All function names accessible by this library.
   */
  public Set<String> getFunctionNames()
  {
    Set<String> names = new HashSet<>(mapIdToFunction.keySet());
    if (baselib != null)
      names.addAll(baselib.getFunctionNames());
    return names;
  }

  /**
   * Read all print functions from the configuration.
   *
   * @param conf
   *          The configuration.
   * @return A {@link PrintFunctionLibrary} with all the configured print functions in conf.
   */
  public static PrintFunctionLibrary parsePrintFunctions(ConfigThingy conf)
  {
    PrintFunctionLibrary funcs = new PrintFunctionLibrary();

    conf = conf.query("Druckfunktionen");
    for (ConfigThingy printFunctionCollection : conf)
    {
      for (ConfigThingy printFunction : printFunctionCollection)
      {
        String name = printFunction.getName();
        try
        {
          ConfigThingy extConf = printFunction.get("EXTERN");

          String orderStr = printFunction.getString("ORDER",
              PrintFunctionLibrary.DEFAULT_PRINTFUNCTION_ORDER_VALUE);
          int order = Integer.parseInt(orderStr);

          PrintFunction func = new ExternalPrintFunction(name, order, extConf);
          funcs.add(name, func);
        } catch (NodeNotFoundException e)
        {
          LOGGER.error(L.m("Druckfunktion '%1' enthält keinen Schlüssel EXTERN", name), e);
        } catch (ConfigurationErrorException e)
        {
          LOGGER.error(L.m("Fehler beim Parsen der Druckfunktion \"%1\"", name));
          LOGGER.debug(L.m("Fehler beim Parsen der Druckfunktion \"%1\"", name), e);
        } catch (NumberFormatException e)
        {
          LOGGER.error(
              L.m("Der Wert des Schlüssels ORDER in der Druckfunktion '%2' ist ungültig.", name),
              e);
        }
      }
    }

    return funcs;
  }

  private static class ExternalPrintFunction extends PrintFunction
  {

    private final ExternalFunction func;

    public ExternalPrintFunction(String functionName, int order, ConfigThingy extConf)
    {
      super(functionName, order);
      func = new ExternalFunction(extConf, WollMuxClassLoader.getClassLoader());
    }

    @Override
    public void print(XPrintModel printModel) throws PrintException
    {
      try
      {
        final Object[] args = new Object[] { printModel };
        func.invoke(args);
      } catch (Exception ex)
      {
        throw new PrintException(
            "Konnte Druckfunktion " + super.getFunctionName() + " nicht ausführen.", ex);
      }
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + Objects.hash(func);
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }
      if (!super.equals(obj))
      {
        return false;
      }
      if (getClass() != obj.getClass())
      {
        return false;
      }
      ExternalPrintFunction other = (ExternalPrintFunction) obj;
      return Objects.equals(func, other.func);
    }
  }

}
