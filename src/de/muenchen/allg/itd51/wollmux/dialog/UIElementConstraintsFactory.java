/*
* Dateiname: UIElementConstraintsFactory.java
* Projekt  : WollMux
* Funktion : Parst ConfigThingys in UIElement.Constraints.
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
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.ExternalFunction;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.UIElement.Constraints;

/**
 * Parst ConfigThingys in UIElement.Constraints.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UIElementConstraintsFactory
{

  /**
   * Erzeugt ein Constraints-Objekt aus einer ConfigThingy-Beschreibung.
   * Geparst werden alle ENKEL von conf und dann und-verknüpft. Sind keine
   * Enkel vorhanden, so wird null geliefert.
   * @throws ConfigurationErrorException falls ein Teil von conf nicht als Constraint
   * geparst werden konnte.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static Constraints getGrandchildConstraints(ConfigThingy conf) throws ConfigurationErrorException
  {
    Vector andConstraints = new Vector();
    Iterator iter1 = conf.iterator();
    while (iter1.hasNext())
    {
      Iterator iter = ((ConfigThingy)iter1.next()).iterator();
      while (iter.hasNext())
      {
        Constraints cons = getConstraints((ConfigThingy)iter.next());
        
        andConstraints.add(cons);
      }
    }
    
    if (andConstraints.isEmpty()) return null;
    
    andConstraints.trimToSize();
    return new AndConstraints(andConstraints);
  }
  
  /**
   * Liefert ein Constraints Objekt zu conf, wobei conf selbst schon ein erlaubter
   * Knoten der Constraint-Beschreibung (z,B, "AND" oder "FUNCTION") sein muss.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws ConfigurationErrorException falls ein Teil von conf nicht als Constraint
   * geparst werden konnte.
   * 
   * TODO Testen
   */
  public static Constraints getConstraints(ConfigThingy conf) throws ConfigurationErrorException
  {
    String name = conf.getName();
    if (name.equals("AND"))
    {
      Vector andConstraints = new Vector();
      Iterator iter = conf.iterator();
      while (iter.hasNext())
      {
        Constraints cons = getConstraints((ConfigThingy)iter.next());
        andConstraints.add(cons);
      }
      
      andConstraints.trimToSize();
      return new AndConstraints(andConstraints);
    }
    else if (name.equals("OR"))
    {
      Vector orConstraints = new Vector();
      Iterator iter = conf.iterator();
      while (iter.hasNext())
      {
        Constraints cons = getConstraints((ConfigThingy)iter.next());
        orConstraints.add(cons);
      }
      
      orConstraints.trimToSize();
      return new OrConstraints(orConstraints);
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
      return new MatchConstraints(id, p);
    }
    else if (name.equals("FUNCTION"))
    {
      return new ExternalFunctionConstraints(conf);
    }
    throw new ConfigurationErrorException("\""+name+"\" ist kein unterstütztes Element für Plausis");
  }

  private static class ExternalFunctionConstraints implements UIElement.Constraints
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
    public ExternalFunctionConstraints(ConfigThingy conf) throws ConfigurationErrorException
    {
      func = new ExternalFunction(conf);
      deps = func.getDependencies();
      depsCollection = new Vector(deps.length);
      for (int i = 0; i < deps.length; ++i)
        depsCollection.add(deps[i]);
    }
    
    public boolean checkValid(Map mapIdToUIElement)
    {
      Map depParams = new HashMap();
      for (int i = 0; i < deps.length; ++i)
      {
        UIElement uiElement = (UIElement)mapIdToUIElement.get(deps[i]);
        if (uiElement != null)
          depParams.put(deps[i], uiElement.getString());
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
  
  private static class MatchConstraints implements UIElement.Constraints
  {
    private Pattern pattern;
    private Collection deps;
    private String id;
    
    
    public MatchConstraints(String id, Pattern p)
    {
      pattern = p;
      this.id = id;
      deps = new Vector(1);
      deps.add(id);
    }
    
    
    public boolean checkValid(Map mapIdToUIElement)
    {
      UIElement elefant = (UIElement)mapIdToUIElement.get(id);
      if (elefant == null) return false;
      if (pattern.matcher(elefant.getString()).matches()) return true;
      return false;
    }

    public Collection dependencies()
    {
      return deps;
    }
  }

  
  private static class AndConstraints implements UIElement.Constraints
  {
    private Collection subConstraints;
    private Collection deps;
    
    /**
     * Achtung: Subconstraints wird als Referenz eingebunden, nicht kopiert!
     * Falls subConstraints leer ist, liefert checkValid() immer true.
     */
    public AndConstraints(Collection subConstraints)
    {
      this.subConstraints = subConstraints;
      deps = new Vector();
      Iterator iter = subConstraints.iterator();
      while (iter.hasNext())
        deps.addAll(((Constraints)iter.next()).dependencies());
      
      ((Vector)deps).trimToSize();
    }
    
    
    public boolean checkValid(Map mapIdToUIElement)
    {
      Iterator iter = subConstraints.iterator();
      while (iter.hasNext())
      {
        Constraints cons = (Constraints)iter.next();
        if (!cons.checkValid(mapIdToUIElement)) return false;
      }
      return true;
    }

    public Collection dependencies()
    {
      return deps;
    }
  }
  
  private static class OrConstraints implements UIElement.Constraints
  {
    private Collection subConstraints;
    private Collection deps;
    
    /**
     * Achtung: Subconstraints wird als Referenz eingebunden, nicht kopiert!
     * Falls subConstraints leer ist, liefert checkValid() immer false.
     */
    public OrConstraints(Collection subConstraints)
    {
      this.subConstraints = subConstraints;
      deps = new Vector();
      Iterator iter = subConstraints.iterator();
      while (iter.hasNext())
        deps.addAll(((Constraints)iter.next()).dependencies());
      
      ((Vector)deps).trimToSize();
    }
    
    
    public boolean checkValid(Map mapIdToUIElement)
    {
      Iterator iter = subConstraints.iterator();
      while (iter.hasNext())
      {
        Constraints cons = (Constraints)iter.next();
        if (cons.checkValid(mapIdToUIElement)) return true;
      }
      return false;
    }

    public Collection dependencies()
    {
      return deps;
    }
  }

}
