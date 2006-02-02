/*
* Dateiname: ConditionFactory.java
* Projekt  : WollMux
* Funktion : Parst ConfigThingys in Conditions.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 12.01.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Parst ConfigThingys in Conditions.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ConditionFactory
{

  /**
   * Erzeugt ein Condition-Objekt aus einer ConfigThingy-Beschreibung.
   * Geparst werden alle ENKEL von conf und dann und-verknüpft. Sind keine
   * Enkel vorhanden, so wird null geliefert.
   * @throws ConfigurationErrorException falls ein Teil von conf nicht als Constraint
   * geparst werden konnte.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static Condition getGrandchildCondition(ConfigThingy conf) throws ConfigurationErrorException
  {
    Vector andCondition = new Vector();
    Iterator iter1 = conf.iterator();
    while (iter1.hasNext())
    {
      Iterator iter = ((ConfigThingy)iter1.next()).iterator();
      while (iter.hasNext())
      {
        Condition cons = getCondition((ConfigThingy)iter.next());
        
        andCondition.add(cons);
      }
    }
    
    if (andCondition.isEmpty()) return null;
    
    andCondition.trimToSize();
    return new AndCondition(andCondition);
  }
  
  /**
   * Liefert ein Condition Objekt zu conf, wobei conf selbst schon ein erlaubter
   * Knoten der Constraint-Beschreibung (z,B, "AND" oder "FUNCTION") sein muss.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws ConfigurationErrorException falls ein Teil von conf nicht als Constraint
   * geparst werden konnte.
   * 
   * TODO Testen
   */
  public static Condition getCondition(ConfigThingy conf) throws ConfigurationErrorException
  {
    String name = conf.getName();
    if (name.equals("AND"))
    {
      Vector andCondition = new Vector();
      Iterator iter = conf.iterator();
      while (iter.hasNext())
      {
        Condition cons = getCondition((ConfigThingy)iter.next());
        andCondition.add(cons);
      }
      
      andCondition.trimToSize();
      return new AndCondition(andCondition);
    }
    else if (name.equals("OR"))
    {
      Vector orCondition = new Vector();
      Iterator iter = conf.iterator();
      while (iter.hasNext())
      {
        Condition cons = getCondition((ConfigThingy)iter.next());
        orCondition.add(cons);
      }
      
      orCondition.trimToSize();
      return new OrCondition(orCondition);
    }
    else if (name.equals("MATCH"))
    {
      if (conf.count() != 2)
        throw new ConfigurationErrorException("Plausi vom Typ \"MATCH\" erfordert genau 2 Parameter, nicht "+conf.count());
      
      String id = "", regex = "";
      try{
        id = conf.getFirstChild().toString();
        regex = conf.getLastChild().toString();
      }catch(Exception x){/*kann nicht sein, weil vorher count() getestet.*/}
      Pattern p;
      try{
        p = Pattern.compile(regex);
      }catch(PatternSyntaxException x)
      {
        throw new ConfigurationErrorException("Fehler in regex \""+regex+"\"", x);
      }
      return new MatchCondition(id, p);
    }
    else if (name.equals("FUNCTION"))
    {
      return new ExternalFunctionCondition(conf);
    }
    throw new ConfigurationErrorException("\""+name+"\" ist kein unterstütztes Element für Plausis");
  }

  private static class ExternalFunctionCondition implements Condition
  {
    private ExternalFunction func;
    private String[] deps;
    private Collection depsCollection; 

    /**
     * 
     * @param conf
     * @throws ConfigurationErrorException
     * TODO test
     */
    public ExternalFunctionCondition(ConfigThingy conf) throws ConfigurationErrorException
    {
      func = new ExternalFunction(conf);
      deps = func.getDependencies();
      depsCollection = new Vector(deps.length);
      for (int i = 0; i < deps.length; ++i)
        depsCollection.add(deps[i]);
    }
    
    public boolean check(Map mapIdToValue)
    {
      Map depParams = new HashMap();
      for (int i = 0; i < deps.length; ++i)
      {
        Value value = (Value)mapIdToValue.get(deps[i]);
        if (value != null)
          depParams.put(deps[i], value.getString());
      }
      try
      {
        return ((Boolean)func.invoke(depParams)).booleanValue();
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
      return false;
    }

    public Collection dependencies()
    {
      return depsCollection;
    }
  }
  
  private static class MatchCondition implements Condition
  {
    private Pattern pattern;
    private Collection deps;
    private String id;
    
    
    public MatchCondition(String id, Pattern p)
    {
      pattern = p;
      this.id = id;
      deps = new Vector(1);
      deps.add(id);
    }
    
    
    public boolean check(Map mapIdToValue)
    {
      Value elefant = (Value)mapIdToValue.get(id);
      if (elefant == null) return false;
      if (pattern.matcher(elefant.getString()).matches()) return true;
      return false;
    }

    public Collection dependencies()
    {
      return deps;
    }
  }

  
  private static class AndCondition implements Condition
  {
    private Collection subCondition;
    private Collection deps;
    
    /**
     * Achtung: SubCondition wird als Referenz eingebunden, nicht kopiert!
     * Falls subCondition leer ist, liefert check() immer true.
     */
    public AndCondition(Collection subCondition)
    {
      this.subCondition = subCondition;
      deps = new Vector();
      Iterator iter = subCondition.iterator();
      while (iter.hasNext())
        deps.addAll(((Condition)iter.next()).dependencies());
      
      ((Vector)deps).trimToSize();
    }
    
    
    public boolean check(Map mapIdToValue)
    {
      Iterator iter = subCondition.iterator();
      while (iter.hasNext())
      {
        Condition cons = (Condition)iter.next();
        if (!cons.check(mapIdToValue)) return false;
      }
      return true;
    }

    public Collection dependencies()
    {
      return deps;
    }
  }
  
  private static class OrCondition implements Condition
  {
    private Collection subCondition;
    private Collection deps;
    
    /**
     * Achtung: SubCondition wird als Referenz eingebunden, nicht kopiert!
     * Falls subCondition leer ist, liefert check() immer false.
     */
    public OrCondition(Collection subCondition)
    {
      this.subCondition = subCondition;
      deps = new Vector();
      Iterator iter = subCondition.iterator();
      while (iter.hasNext())
        deps.addAll(((Condition)iter.next()).dependencies());
      
      ((Vector)deps).trimToSize();
    }
    
    
    public boolean check(Map mapIdToValue)
    {
      Iterator iter = subCondition.iterator();
      while (iter.hasNext())
      {
        Condition cons = (Condition)iter.next();
        if (cons.check(mapIdToValue)) return true;
      }
      return false;
    }

    public Collection dependencies()
    {
      return deps;
    }
  }

}
