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
  private XScript script;
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
      script = UNO.masterScriptProvider.getScript(url);
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
    
    params = new String[paramsConf.count()];
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
        Object retval = script.invoke(args, aOutParamIndex, aOutParam);
        return retval;
    } catch(Exception x)
    {
      throw new Exception(x);
    }
  }
}
