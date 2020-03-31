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
package de.muenchen.allg.itd51.wollmux.func.print;

import java.util.Objects;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibrary;

/**
 * Definition of print functions. Each print function is a service.
 *
 * All subclasses are automatically registered as a print function if they're mentioned in
 * META-INF/services/de.muenchen.allg.itd51.wollmux.func.print.
 *
 * Print functions are ordered by {@link #order}.
 *
 * Print functions can be accessed in a {@link PrintFunctionLibrary} by their {@link #functionName}.
 */
public abstract class PrintFunction implements Comparable<PrintFunction>
{

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintFunction.class);

  public static final String PRINT_RESULT = "WollMux_Print_Result";

  public static final String PRINT_RESULT_FILE = "WollMux_Print_Result_File";

  private String functionName;

  private int order;

  /**
   * Create a new print function with name and order.
   *
   * @param functionName
   *          The name of the print function.
   * @param order
   *          The order of the print function.
   */
  public PrintFunction(String functionName, int order)
  {
    this.functionName = functionName;
    this.order = order;
  }

  public String getFunctionName()
  {
    return functionName;
  }

  /**
   * Compares this print function with an other print function.
   *
   * This print function comes before the other one if
   * <ol>
   * <li>its {@link #order} is smaller than the {@link #order} of the other print function</li>
   * <li>or according to {@link String#compareTo(String)} its {@link #functionName} is before the
   * {@link #functionName} of the other print function.</li>
   * </ol>
   *
   * @param other
   *          The other print function.
   */
  @Override
  public int compareTo(PrintFunction other)
  {
    if (this.order != other.order)
      return (this.order < other.order) ? -1 : 1;
    return this.functionName.compareTo(other.functionName);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(functionName, order);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    PrintFunction other = (PrintFunction) obj;
    return Objects.equals(functionName, other.functionName) && order == other.order;
  }

  /**
   * Execute this print function in a separate thread.
   *
   * @param printModel
   *          The {@link XPrintModel} to print.
   * @return The separate thread.
   */
  public Thread printAsync(XPrintModel printModel)
  {
    Thread t = new Thread(() -> {
      try
      {
        print(printModel);
      } catch (Exception ex)
      {
        LOGGER.error("Fehler beim Drucken", ex);
      }
    });
    t.start();
    return t;
  }

  /**
   * Do the purpose of this print function.
   *
   * @param printModel
   *          The {@link XPrintModel} to print.
   * @throws PrintException
   *           An error occurred during printing.
   */
  public abstract void print(XPrintModel printModel) throws PrintException;

  /**
   * Add print functions defined as service of type {@link PrintFunction}.
   *
   * @param library
   *          The library which gets the print functions.
   */
  public static void addPrintFunctions(PrintFunctionLibrary library)
  {
    ServiceLoader.load(PrintFunction.class, PrintFunction.class.getClassLoader())
        .forEach(printFunction -> {
      if (library.get(printFunction.functionName) == null)
      {
        library.add(printFunction.functionName, printFunction);
        LOGGER.debug("Registriere interne Druckfunktion {} als Fallback",
            printFunction.functionName);
      } else
      {
        LOGGER.debug("Druckfunktion mit dem Namen {} wurde bereits registriert.",
            printFunction.functionName);
      }
    });
  }
}
