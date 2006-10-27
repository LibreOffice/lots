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

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.sun.star.script.provider.XScript;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

/**
 * Eine durch ein ConfigThingy beschriebene externe Druckfunktion.
 * 
 * @author christoph.lutz
 */
public class PrintFunction
{
  /**
   * Falls die Funktion eine Funktion des Scripting Frameworks ist, ist hier die
   * Referenz auf das Skript gespeichert.
   */
  private XScript script = null;

  /**
   * Falls die Funktion eine statische Java-Methode ist, so wird hier die
   * Referenz auf diese gespeichert.
   */
  private Method method = null;

  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * PrintFunction.
   * 
   * @throws ConfigurationErrorException
   *           falls die Spezifikation in conf fehlerhaft ist.
   */
  public PrintFunction(ConfigThingy conf) throws ConfigurationErrorException
  {
    String url;
    try
    {
      url = conf.get("URL").toString();
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException("URL fehlt in EXTERN");
    }

    try
    {
      if (url.startsWith("java:"))
      {
        String classStr = url.substring(5, url.lastIndexOf('.'));
        String methodStr = url.substring(url.lastIndexOf('.') + 1);
        Class c = this.getClass().getClassLoader().loadClass(classStr);
        Method[] methods = c.getDeclaredMethods();

        for (int i = 0; i < methods.length; ++i)
        {
          Method m = methods[i];
          if (!m.getName().equals(methodStr)) continue;

          // Typ des ersten (und einzigen Parameters) prüfen:
          Type[] types = m.getParameterTypes();
          if (types.length != 1) continue;

          if (types[0].equals(XPrintModel.class)) method = m;
        }

        if (method == null)
          throw new ConfigurationErrorException(
              "Klasse \""
                  + classStr
                  + "\" enthält keine Methode namens \""
                  + methodStr
                  + "(XPrintModel model)\"");
      }
      else
      {
        script = UNO.masterScriptProvider.getScript(url);
      }
    }
    catch (Exception e)
    {
      throw new ConfigurationErrorException("Skript \""
                                            + url
                                            + "\" nicht verfügbar", e);
    }
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
    final short[][] aOutParamIndex = new short[][] { new short[0] };
    final Object[][] aOutParam = new Object[][] { new Object[0] };

    Thread externPrintFuncThread = null;

    if (script != null)
    {
      externPrintFuncThread = new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            script.invoke(args, aOutParamIndex, aOutParam);
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }
      });
    }

    else if (method != null)
    {
      externPrintFuncThread = new Thread(new Runnable()
      {
        public void run()
        {
          try
          {
            method.invoke(null, args);
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }
      });
    }

    // Thread starten:
    if (externPrintFuncThread != null) externPrintFuncThread.start();
  }
}
