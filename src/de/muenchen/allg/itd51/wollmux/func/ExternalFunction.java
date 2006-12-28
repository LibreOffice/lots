/*
* Dateiname: ExternalFunction.java
* Projekt  : WollMux
* Funktion : Eine durch ein ConfigThingy beschriebene externe Funktion.
* 
* Copyright: Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.func;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.sun.star.script.provider.XScript;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Eine durch ein ConfigThingy beschriebene externe (d,h, nicht als ConfigThingy
 * definierte) Funktion.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ExternalFunction
{
  /**
   * Falls die Funktion eine Funktion des Scripting Frameworks ist, ist hier
   * die Referenz auf das Skript gespeichert.
   */
  private XScript script = null;
  
  /**
   * Falls die Funktion eine statische Java-Methode ist, so wird hier die
   * Referenz auf diese gespeichert.
   */
  private Method method = null;
  
  /**
   * Die Namen der Parameter, die die Funktion erwartet.
   */
  private String[] params;
  
  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * ExternalFunction, wobei zum Laden von Java-Klassen der selbe ClassLoader wie für das
   * Laden dieser Klasse verwendet wird. 
   * @throws ConfigurationErrorException falls die Spezifikation in conf fehlerhaft ist.
   * TESTED */
  public ExternalFunction(ConfigThingy conf) throws ConfigurationErrorException
  {
    this(conf, null);
  }
  
  /**
   * Erzeugt aus einem ConfigThingy (übergeben wird der EXTERN-Knoten) eine
   * ExternalFunction, wobei zum Laden von Java-Klassen classLoader verwendet wird.
   * @throws ConfigurationErrorException falls die Spezifikation in conf fehlerhaft ist.
   * TESTED */
  public ExternalFunction(ConfigThingy conf, ClassLoader classLoader) throws ConfigurationErrorException
  {
    if (classLoader == null) classLoader = this.getClass().getClassLoader();
    String url;
    try{
      url = conf.get("URL").toString();
    }catch(NodeNotFoundException x)
    {
      throw new ConfigurationErrorException("URL fehlt in EXTERN");
    }
    
    try
    {
      if (url.startsWith("java:"))
      {
        String classStr = url.substring(5,url.lastIndexOf('.'));
        String methodStr = url.substring(url.lastIndexOf('.')+1);
        Class c = classLoader.loadClass(classStr);
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i)
          if (methods[i].getName().equals(methodStr) && Modifier.isPublic(methods[i].getModifiers()))
          {
            if (method != null)
            { 
              Logger.error("Klasse \""+classStr+"\" enthält 2 Methoden namens \""+methodStr+"\"");
              break;
            }
            method = methods[i];
          }
        
        if (method == null) throw new ConfigurationErrorException("Klasse \""+classStr+"\" enthält keine PUBLIC Methode namens \""+methodStr+"\"");
      }
      else
      {
        script = UNO.masterScriptProvider.getScript(url);
      }
    }
    catch (Exception e)
    {
      throw new ConfigurationErrorException("Skript \""+url+"\" nicht verfügbar",e);
    }
    
    ConfigThingy paramsConf = conf.query("PARAMS");
    
    List paramList = new Vector();
    Iterator iter = paramsConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy param = (ConfigThingy)iter.next();
      Iterator paramIter = param.iterator();
      while (paramIter.hasNext())
      {
        String p = paramIter.next().toString();
        paramList.add(p); 
      }
    }
    
    params = new String[paramList.size()];
    for (int i = 0; i < paramList.size(); ++i)
      params[i] = (String)paramList.get(i);
  }
  
  /**
   * Liefert die Namen der Parameter, die die Funktion erwartet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String[] parameters()
  {
    return params;
  }
  
  /**
   * Ruft die Funktion auf mit den String-Parametern aus parameters.
   * @param parameters sollte zu jedem der von {@link #parameters()} gelieferten
   *        Namen einen String-Wert enthalten. 
   * @return den Wert des Funktionsaufrufs oder null falls es ein Problem gab, das
   *         nicht zu einer Exception geführt hat.
   * @throws Exception falls ein Problem auftritt
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
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
   * @param args muss für jeden der von {@link #parameters()} gelieferten
   *        Namen einen Wert enthalten. 
   * @return den Wert des Funktionsaufrufs oder null falls es ein Problem gab, das
   *         nicht zu einer Exception geführt hat.
   * @throws Exception falls ein Problem auftritt
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public Object invoke(Object[] args) throws Exception
  {
    short[][] aOutParamIndex = new short[][]{new short[0]};
    Object[][] aOutParam = new Object[][]{new Object[0]};
    if (script != null) 
    {
      Object result = script.invoke(args, aOutParamIndex, aOutParam);
      if (AnyConverter.isVoid(result)) result = null;
      return result;
    }
    else if (method != null) 
    {
      Object result = method.invoke(null, args);
      return result;
    }
    return null;
  }
}
