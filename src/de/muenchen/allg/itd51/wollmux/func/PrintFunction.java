//TODO L.m()
/*
 * Dateiname: PrintFunction.java
 * Projekt  : WollMux
 * Funktion : Eine durch ein ConfigThingy beschriebene externe Druckfunktion.
 * 
 * Copyright: Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.func;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

/**
 * Eine durch ein ConfigThingy beschriebene externe Druckfunktion.
 * 
 * @author christoph.lutz
 */
public class PrintFunction implements Comparable
{

  private ExternalFunction func = null;

  private String functionName;

  private int order;

  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * PrintFunction vom Namen functionName mit dem ORDER-Wert order. Die Werte
   * order und functionName werden für korrekte Sortierung verschiedener
   * Druckfuntionktionen und damit für die Bildung einer definierten Reihenfolge
   * bei der Abarbeitung verschachtelter Druckfunktionen benätigt (siehe
   * compareTo(...)).
   * 
   * @throws ConfigurationErrorException
   *           falls die Spezifikation in conf fehlerhaft ist.
   */
  public PrintFunction(ConfigThingy conf, String functionName, int order)
      throws ConfigurationErrorException
  {
    func = new ExternalFunction(conf, WollMuxFiles.getClassLoader());
    this.functionName = functionName;
    this.order = order;
  }

  /**
   * Liefert den Namen dieser Druckfunktion zurück.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
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
   *          das XPrintModel des aktuellen Vordergrunddokuments, das die
   *          wichtigsten Druckkomandos bereitstellt, die die externe Funktion
   *          verwenden kann.
   * @throws Exception
   */
  public Thread invoke(XPrintModel pmod)
  {
    return invoke(pmod, null);
  }

  /**
   * Ruft die Funktion mit dem XPrintModel pmod als Parameter und dem
   * Konfigurationsargument confArg (ein Objekt, das Argumente für diesen einen
   * Druckvorgang enthält) asynchron (d,h, in einem eigenen Thread) auf und
   * liefert den Thread zurück.
   * 
   * @param pmod
   *          das XPrintModel des aktuellen Vordergrunddokuments, das die
   *          wichtigsten Druckkomandos bereitstellt, die die externe Funktion
   *          verwenden kann.
   * @throws Exception
   */
  public Thread invoke(XPrintModel pmod, Object confArg)
  {
    final Object[] args;
    if (confArg != null)
      args = new Object[] { pmod, confArg };
    else
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
          Logger.error(e);
        }
      }
    });
    t.start();
    return t;
  }

  /**
   * Vergleicht this mit otherPrintFunction und liefert -1 zurück, wenn this
   * eine höhere Priorität besitzt als otherPrintFunction (und damit vor
   * otherPrintFunction abgearbeitet werden soll) und 1, wenn otherPrintFunction
   * eine höhere Priorität besitzt als this. Die Priorität ergibt sich dabei aus
   * dem Attribut ORDER der PrintFunction und deren Namen. Ist die this.order
   * kleiner als otherPrintFunction.order, so hat this die höhrer Priorität,
   * sind beide order-Werte gleich, so wird der Name alphabetisch verglichen und
   * ist this.order größer als otherPrintFunction.order, so hat
   * otherPrintFunction hörere Priorität.
   * 
   * @param otherPrintFunction
   *          Die PrintFunction mit der vergleichen werden soll.
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public int compareTo(Object otherPrintFunction)
  {
    PrintFunction other = (PrintFunction) otherPrintFunction;
    if (this.order != other.order) return (this.order < other.order) ? -1 : 1;
    return this.functionName.compareTo(other.functionName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    return "PrintFunction['" + functionName + "']";
  }
}
