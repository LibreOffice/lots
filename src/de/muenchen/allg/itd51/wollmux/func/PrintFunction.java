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
public class PrintFunction
{

  private ExternalFunction func = null;

  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * PrintFunction.
   * 
   * @throws ConfigurationErrorException
   *           falls die Spezifikation in conf fehlerhaft ist.
   */
  public PrintFunction(ConfigThingy conf) throws ConfigurationErrorException
  {
    func = new ExternalFunction(conf, WollMuxFiles.getClassLoader());
  }

  /**
   * Ruft die Funktion mit dem XPrintModel pmod als Parameter asynchron (d,h, in
   * einem eigenen Thread) auf.
   * 
   * @param pmod
   *          das XPrintModel des aktuellen Vordergrunddokuments, das die
   *          wichtigsten Druckkomandos bereitstellt, die die externe Funktion
   *          verwenden kann.
   * @throws Exception
   */
  public void invoke(XPrintModel pmod)
  {
    final Object[] args = new Object[] { pmod };
    new Thread(new Runnable()
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
    }).start();
  }
}
