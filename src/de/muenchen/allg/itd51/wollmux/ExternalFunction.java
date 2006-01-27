/*
* Dateiname: ExternalFunction.java
* Projekt  : WollMux
* Funktion : Eine durch ein ConfigThingy beschriebene externe Funktion
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 25.01.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.sun.star.script.provider.XScript;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Eine durch ein ConfigThingy beschriebene externe Funktion.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ExternalFunction
{
  private XScript script = null;
  private Method method = null;
  private String[] params;
  private String[] depends;
  
  
  /**
   * Übergeben wird der FUNCTION-Knoten.
   * @param conf
   * @throws ConfigurationErrorException falls die Spezifikation in conf fehlerhaft ist.
   */
  public ExternalFunction(ConfigThingy conf) throws ConfigurationErrorException
  {
    String url;
    try{
      url = conf.get("URL").toString();
    }catch(NodeNotFoundException x)
    {
      throw new ConfigurationErrorException("URL fehlt in FUNCTION");
    }
    
    try
    {
      if (url.startsWith("java:"))
      {
        String classStr = url.substring(5,url.lastIndexOf('.'));
        String methodStr = url.substring(url.lastIndexOf('.')+1);
        Class c = this.getClass().getClassLoader().loadClass(classStr);
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length; ++i)
          if (methods[i].getName().equals(methodStr))
          {
            if (method != null)
            { 
              Logger.error("Klasse \""+classStr+"\" enthält 2 Methoden namens \""+methodStr+"\"");
              break;
            }
            method = methods[i];
          }
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
    
    ConfigThingy paramsConf = conf.query("PARAM");
    ConfigThingy dependsConf = conf.query("DEPENDS");
    List depList = new Vector();
    Iterator iter = dependsConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy dep = (ConfigThingy)iter.next();
      Iterator depIter = dep.iterator();
      while (depIter.hasNext())
      {
        String id = depIter.next().toString();
        depList.add(id); 
      }
    }
    
    List paramList = new Vector();
    iter = paramsConf.iterator();
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

    depends = new String[depList.size()];
    for (int i = 0; i < depList.size(); ++i)
      depends[i] = (String)depList.get(i);
    
  }
  
  /**
   * Liefert die IDs aller Objekte, die invoke() übergeben werden müssen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String[] getDependencies()
  {
    return depends;
  }
  
  public Object invoke(Map mapIdToValue) throws Exception
  {
    Object[] args = new Object[params.length + depends.length];
    int i = 0;
    for (; i < params.length; ++i)
      args[i] = params[i];
    for (int j = 0; j < depends.length; ++j)
      args[i++] = mapIdToValue.get(depends[j]);
    short[][] aOutParamIndex = new short[][]{new short[0]};
    Object[][] aOutParam = new Object[][]{new Object[0]};
    try{
        if (script != null) return script.invoke(args, aOutParamIndex, aOutParam);
        if (method != null) return method.invoke(null, args);
        return null;
    } catch(Exception x)
    {
      throw new Exception(x);
    }
  }
}
