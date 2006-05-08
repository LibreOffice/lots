/*
* Dateiname: FunctionFactory.java
* Projekt  : WollMux
* Funktion : Erzeugt Functions aus ConfigThingys.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 03.05.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;

/**
 * Erzeugt Functions aus ConfigThingys.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionFactory
{
  /**
   * Eine Funktion, die immer true liefert.
   */
  private static final Function myAlwaysTrueFunction = new AlwaysTrueFunction();
  
  /**
   * Eine leere Values-Sammlung.
   */
  private static final Values noValues = new Values.None();
  
  /**
   * Eine leere Liste von Parameter-Namen.
   */
  private static final String[] noParams = new String[]{};
  
  /**
   * Liefert eine Funktion, die immer true liefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Function alwaysTrueFunction()
  {
    return myAlwaysTrueFunction;
  }
  
  /**
   * Erzeugt ein Function-Objekt aus den ENKELN von conf.
   * Hat conf keine Enkel, so wird null geliefert. Hat conf genau einen Enkel,
   * so wird eine Funktion geliefert, die diesem Enkel entspricht. Hat conf
   * mehr als einen Enkel, so wird eine Funktion geliefert, die alle Enkel als
   * Booleans auswertet und UND-verknüpft.
   * @param funcLib die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *        aufgelöst werden sollen.
   * @throws ConfigurationErrorException falls conf keine korrekte Funktionsbeschreibung ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static Function parseGrandchildren(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib) throws ConfigurationErrorException
  {
    Vector andFunction = new Vector();
    Iterator iter1 = conf.iterator();
    while (iter1.hasNext())
    {
      Iterator iter = ((ConfigThingy)iter1.next()).iterator();
      while (iter.hasNext())
      {
        Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib);
        
        andFunction.add(cons);
      }
    }
    
    if (andFunction.isEmpty()) return null;
    if (andFunction.size() == 1) return (Function)andFunction.get(0);
    
    andFunction.trimToSize();
    return new AndFunction(andFunction);
  }
  
  /**
   * Erzeugt ein Function-Objekt aus den KINDERN von conf.
   * Hat conf keine Kinder, so wird null geliefert. Hat conf genau ein Kind,
   * so wird eine Funktion geliefert, die diesem Enkel entspricht. Hat conf
   * mehr als ein Kind, so wird eine Funktion geliefert, die alle Kinder als
   * Booleans auswertet und UND-verknüpft.
   * @param funcLib die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *        aufgelöst werden sollen.
   * @throws ConfigurationErrorException falls conf keine korrekte Funktionsbeschreibung ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static Function parseChildren(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib) throws ConfigurationErrorException
  {
    Vector andFunction = new Vector();
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib);
      andFunction.add(cons);
    }
    
    if (andFunction.isEmpty()) return null;
    if (andFunction.size() == 1) return (Function)andFunction.get(0);
    
    andFunction.trimToSize();
    return new AndFunction(andFunction);
  }
  
  /**
   * Liefert ein Function Objekt zu conf, wobei conf selbst schon ein erlaubter
   * Knoten der Funktionsbeschreibung (z,B, "AND" oder "MATCH") sein muss.
   * @param funcLib die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *        aufgelöst werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws ConfigurationErrorException falls ein Teil von conf nicht als Funktion
   * geparst werden konnte.
   * 
   * TODO Testen
   */
  public static Function parse(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib) throws ConfigurationErrorException
  {
    String name = conf.getName();
    
    if (conf.count() == 0)
      return new StringLiteralFunction(name);
    
    if (name.equals("AND"))
    {
      Vector andFunction = new Vector();
      Iterator iter = conf.iterator();
      while (iter.hasNext())
      {
        Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib);
        andFunction.add(cons);
      }
      
      andFunction.trimToSize();
      return new AndFunction(andFunction);
    }
    else if (name.equals("OR"))
    {
      Vector orFunction = new Vector();
      Iterator iter = conf.iterator();
      while (iter.hasNext())
      {
        Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib);
        orFunction.add(cons);
      }
      
      orFunction.trimToSize();
      return new OrFunction(orFunction);
    }
    else if (name.equals("MATCH"))
    {
      if (conf.count() != 2)
        throw new ConfigurationErrorException("Funktion vom Typ \"MATCH\" erfordert genau 2 Parameter, nicht "+conf.count());
      
      Function strFun = null;
      Function reFun = null; 
      
      try{
        strFun = parse(conf.getFirstChild(), funcLib, dialogLib);
        reFun = parse(conf.getLastChild(), funcLib, dialogLib);
      }catch(NodeNotFoundException x){/*Kann nicht sein, weil count() getestet*/}
      
      String regex = reFun.getString(noValues);
      Pattern p;
      try{
        p = Pattern.compile(regex);
      }catch(PatternSyntaxException x)
      {
        throw new ConfigurationErrorException("Fehler in regex \""+regex+"\"", x);
      }
      return new MatchFunction(strFun, p);
    }
    else if (name.equals("EXTERN"))
    {
      return new ExternalFunctionFunction(conf);
    }
    throw new ConfigurationErrorException("\""+name+"\" ist kein unterstütztes Element für Plausis");
  }

  private static class StringLiteralFunction implements Function
  {
    private String literal;
    
    public String[] parameters() { return noParams; }

    public StringLiteralFunction(String str)
    {
      literal = str;
    }
    
    public String getString(Values parameters)
    {
      return literal;
    }

    public boolean getBoolean(Values parameters)
    {
      return literal.equalsIgnoreCase("true");
    }
  }
  
  private static class ExternalFunctionFunction implements Function
  {
    private ExternalFunction func;

    public ExternalFunctionFunction(ConfigThingy conf) throws ConfigurationErrorException
    {
      func = new ExternalFunction(conf);
    }
    
    public String[] parameters()
    {
      return func.parameters();
    }
    
    public String getString(Values parameters)
    {
      try
      {
        Object result = func.invoke(parameters);
        return result.toString();
      }
      catch (Exception e)
      {
        Logger.error(e);
        return Function.ERROR;
      }
    }

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }
  }
  
  private static class MatchFunction implements Function
  {
    private Pattern pattern;
    private Function input;
    
    public MatchFunction(Function input, Pattern p)
    {
      pattern = p;
      this.input = input;
    }
    
    public boolean getBoolean(Values parameters)
    {
      String str = input.getString(parameters);
      if (str == Function.ERROR) return false;
      if (pattern.matcher(str).matches()) return true;
      return false;
    }

    public String[] parameters()
    {
      return input.parameters();
    }

    public String getString(Values parameters)
    {
      return ""+getBoolean(parameters);
    }

  }

  
  private static class AndFunction implements Function
  {
    private Collection subFunction;
    private String[] params;
    
    /**
     * Achtung: SubFunction wird als Referenz eingebunden, nicht kopiert!
     * Falls subFunction leer ist, liefert getBoolean() immer true.
     */
    public AndFunction(Collection subFunction)
    {
      this.subFunction = subFunction;
      Collection deps = new Vector();
      Iterator iter = subFunction.iterator();
      int count = 0;
      while (iter.hasNext())
      {
        String[] params = ((Function)iter.next()).parameters();
        count += params.length;
        deps.add(params);
      }
      
      params = new String[count];
      iter = deps.iterator();
      int i = 0;
      while (iter.hasNext())
      {
        String[] p = (String[])iter.next();
        System.arraycopy(p, 0, params, i, p.length);
        i += p.length;
      }
    }
    
    public boolean getBoolean(Values parameters)
    {
      Iterator iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = (Function)iter.next();
        if (!func.getBoolean(parameters)) return false;
      }
      return true;
    }

    public String[] parameters()
    {
      return params;
    }


    public String getString(Values parameters)
    {
      return ""+getBoolean(parameters);
    }
  }
  
  private static class OrFunction implements Function
  {
    private Collection subFunction;
    private String[] params;
    
    /**
     * Achtung: SubFunction wird als Referenz eingebunden, nicht kopiert!
     * Falls subFunction leer ist, liefert getBoolean() immer false.
     */
    public OrFunction(Collection subFunction)
    {
      this.subFunction = subFunction;
      Collection deps = new Vector();
      Iterator iter = subFunction.iterator();
      int count = 0;
      while (iter.hasNext())
      {
        String[] params = ((Function)iter.next()).parameters();
        count += params.length;
        deps.add(params);
      }
      
      params = new String[count];
      iter = deps.iterator();
      int i = 0;
      while (iter.hasNext())
      {
        String[] p = (String[])iter.next();
        System.arraycopy(p, 0, params, i, p.length);
        i += p.length;
      }
    }
    
    public boolean getBoolean(Values parameters)
    {
      Iterator iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = (Function)iter.next();
        if (func.getBoolean(parameters)) return true;
      }
      return false;
    }

    public String[] parameters()
    {
      return params;
    }


    public String getString(Values parameters)
    {
      return ""+getBoolean(parameters);
    }
  }
  
  private static class AlwaysTrueFunction implements Function
  {
    public String[] parameters()
    {
      return noParams;
    }
    

    public String getString(Values parameters)
    {
      return "true";
    }

    public boolean getBoolean(Values parameters)
    {
     return true;
    }
  }


}
