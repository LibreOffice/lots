/*
 * Dateiname: PrintFunction.java
 * Projekt  : WollMux
 * Funktion : Eine durch ein ConfigThingy beschriebene externe Druckfunktion.
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
 * 19.09.2006 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.print;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxClassLoader;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.functions.ExternalFunction;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

/**
 * Eine durch ein ConfigThingy beschriebene externe Druckfunktion.
 *
 * @author christoph.lutz
 */
public class PrintFunction implements Comparable<PrintFunction>
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PrintFunction.class);

  private ExternalFunction func = null;

  private String functionName;

  private int order;

  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * PrintFunction vom Namen functionName mit dem ORDER-Wert order. Die Werte order
   * und functionName werden für korrekte Sortierung verschiedener
   * Druckfuntionktionen und damit für die Bildung einer definierten Reihenfolge bei
   * der Abarbeitung verschachtelter Druckfunktionen benätigt (siehe compareTo(...)).
   *
   * @throws ConfigurationErrorException
   *           falls die Spezifikation in conf fehlerhaft ist.
   */
  public PrintFunction(ConfigThingy conf, String functionName, int order)
      throws ConfigurationErrorException
  {
    func = new ExternalFunction(conf, WollMuxClassLoader.getClassLoader());
    this.functionName = functionName;
    this.order = order;
  }

  /**
   * Liefert den Namen dieser Druckfunktion zurück.
   */
  public String getFunctionName()
  {
    return functionName;
  }

  /**
   * Ruft die Funktion mit dem XPrintModel pmod als Parameter asynchron (d,h, in
   * einem eigenen Thread) auf und liefert den Thread zurück.
   *
   * @param pmod
   *          das XPrintModel des aktuellen Vordergrunddokuments, das die wichtigsten
   *          Druckkomandos bereitstellt, die die externe Funktion verwenden kann.
   * @throws Exception
   */
  public Thread invoke(XPrintModel pmod)
  {
    final Object[] args;
    args = new Object[] { pmod };

    Thread t = new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          func.invoke(args);
        }
        catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
      }
    });
    t.start();
    return t;
  }

  /**
   * Vergleicht this mit otherPrintFunction und liefert -1 zurück, wenn this eine höhere Priorität
   * besitzt als otherPrintFunction (und damit vor otherPrintFunction abgearbeitet werden soll) und
   * 1, wenn otherPrintFunction eine höhere Priorität besitzt als this. Die Priorität ergibt sich
   * dabei aus dem Attribut ORDER der PrintFunction und deren Namen. Ist die this.order kleiner als
   * otherPrintFunction.order, so hat this die höhrer Priorität, sind beide order-Werte gleich, so
   * wird der Name alphabetisch verglichen und ist this.order größer als otherPrintFunction.order,
   * so hat otherPrintFunction hörere Priorität.
   *
   * @param otherPrintFunction
   *          Die PrintFunction mit der vergleichen werden soll.
   * @return @{see {@link String#compareTo(String)}
   */
  @Override
  public int compareTo(PrintFunction otherPrintFunction)
  {
    PrintFunction other = otherPrintFunction;
    if (this.order != other.order) return (this.order < other.order) ? -1 : 1;
    return this.functionName.compareTo(other.functionName);
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == null) return false;
    try
    {
      return (this.compareTo((PrintFunction) o) == 0);
    }
    catch (ClassCastException x)
    {}
    return false;
  }

  @Override
  public int hashCode()
  {
    return this.functionName.hashCode();
  }

  @Override
  public String toString()
  {
    return "PrintFunction['" + functionName + "']";
  }
}
