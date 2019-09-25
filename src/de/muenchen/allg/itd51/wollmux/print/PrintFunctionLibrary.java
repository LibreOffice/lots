/*
 * Dateiname: PrintFunctionLibrary.java
 * Projekt  : WollMux
 * Funktion : Eine Bibliothek von benannten Druckfunktionen
 *
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 19.09.2006 | LUT | Erstellung.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.print;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Eine Bibliothek von benannten PrintFunctions
 *
 * @author christoph.lutz
 */
public class PrintFunctionLibrary
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PrintFunctionLibrary.class);

  private Map<String, PrintFunction> mapIdToFunction;

  private PrintFunctionLibrary baselib;

  /**
   * Druckfunktionen, bei denen kein ORDER-Attribut angegeben ist, werden automatisch
   * mit diesem ORDER-Wert versehen.
   */
  private static final String DEFAULT_PRINTFUNCTION_ORDER_VALUE = "100";

  /**
   * Erzeugt eine leere Funktionsbibliothek.
   */
  public PrintFunctionLibrary()
  {
    this(null);
  }

  /**
   * Erzeugt eine Funktionsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Funktionsbibliothek selbst keine
   * Funktion des entsprechenden Namens enthält. baselib darf null sein.
   *
   * @param baselib
   */
  public PrintFunctionLibrary(PrintFunctionLibrary baselib)
  {
    mapIdToFunction = new HashMap<>();
    this.baselib = baselib;
  }

  /**
   * Fügt func dieser Funktionsbibliothek unter dem Namen funcName hinzu. Eine
   * bereits existierende Funktion mit diesem Namen wird dabei ersetzt.
   */
  public void add(String funcName, PrintFunction func)
  {
    if (func == null || funcName == null)
      throw new NullPointerException(
        L.m("Weder Funktionsname noch Funktion darf null sein"));
    mapIdToFunction.put(funcName, func);
  }

  /**
   * Liefert die Function namens funcName zurück oder null, falls keine Funktion mit
   * diesem Namen bekannt ist. Wurde die Funktionsbibliothek mit einer Referenz auf
   * eine andere Funktionsbibliothek initialisiert, so wird diese befragt, falls die
   * Funktionsbibliothek selbst keine Funktion des entsprechenden Namens kennt.
   */
  public PrintFunction get(String funcName)
  {
    PrintFunction func = mapIdToFunction.get(funcName);
    if (func == null && baselib != null) func = baselib.get(funcName);
    return func;
  }

  /**
   * Liefert die Namen aller Funktionen, die über diese Funktionsbibliothek verfügbar
   * sind.
   */
  public Set<String> getFunctionNames()
  {
    Set<String> names = new HashSet<>(mapIdToFunction.keySet());
    if (baselib != null) names.addAll(baselib.getFunctionNames());
    return names;
  }

  /**
   * Parst die "Druckfunktionen" Abschnitte aus conf und liefert eine entsprechende
   * PrintFunctionLibrary.
   */
  public static PrintFunctionLibrary parsePrintFunctions(ConfigThingy conf)
  {
    PrintFunctionLibrary funcs = new PrintFunctionLibrary();

    conf = conf.query("Druckfunktionen");
    Iterator<ConfigThingy> parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      Iterator<ConfigThingy> iter = parentIter.next().iterator();
      while (iter.hasNext())
      {
        ConfigThingy funcConf = iter.next();
        String name = funcConf.getName();
        try
        {
          ConfigThingy extConf;
          try
          {
            extConf = funcConf.get("EXTERN");
          }
          catch (NodeNotFoundException e)
          {
            LOGGER.error(
              L.m("Druckfunktion '%1' enthält keinen Schlüssel EXTERN", name), e);
            continue;
          }

          String orderStr = PrintFunctionLibrary.DEFAULT_PRINTFUNCTION_ORDER_VALUE;
          int order;
          try
          {
            orderStr = funcConf.get("ORDER").toString();
          }
          catch (NodeNotFoundException e)
          {
            LOGGER.debug(L.m(
              "Druckfunktion '%1' enthält keinen Schlüssel ORDER. Verwende Standard-Wert %2",
              name, "" + PrintFunctionLibrary.DEFAULT_PRINTFUNCTION_ORDER_VALUE));
          }
          try
          {
            order = Integer.parseInt(orderStr);
          }
          catch (NumberFormatException e)
          {
            LOGGER.error(
              L.m(
                "Der Wert '%1' des Schlüssels ORDER in der Druckfunktion '%2' ist ungültig.",
                orderStr, name), e);
            continue;
          }

          PrintFunction func = new PrintFunction(extConf, name, order);

          funcs.add(name, func);
        }
        catch (ConfigurationErrorException e)
        {
          LOGGER.error(L.m("Fehler beim Parsen der Druckfunktion \"%1\"", name), e);
        }
      }
    }

    return funcs;
  }

}
