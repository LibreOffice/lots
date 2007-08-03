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
* 08.05.2006 | BNK | Testing und Debugging, mehr Grund-Funktionen
* 09.05.2006 | BNK | weitere Grundfunktionen
* 11.05.2006 | BNK | NOT implementiert
*                  | MATCH.getString() kann jetzt Function.ERROR liefern
* 31.05.2006 | BNK | +getFunctionDialogReferences()
* 26.07.2006 | BNK | +REPLACE-Grundfunktion
* 05.12.2006 | BNK | WollMuxFiles.getClassLoader() wird für ExternalFunctions übergeben.
* 21.03.2007 | BNK | BIND erweitert so dass auch direkt eine Funktion als FUNCTION verwendet werden kann.
* 25.07.2007 | BNK | +DIVIDE/FORMAT
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

import java.awt.event.ActionListener;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
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
   * @param dialogLib die Dialogbibliothek anhand derer Referenzen auf Dialoge 
   *        aufgelöst werden sollen.
   * @param context Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige
   *        Werte. Zur Unterscheidung der verschiedenen Instanzen dient die context
   *        Map, in der die verschiedenen Instanzen abgelegt werden. 
   *        Wird hier null übergeben, dann wird eine ConfigurationErrorException
   *        geworfen, wenn conf eine Funktion enthält, die einen Kontext benötigt. 
   * @throws ConfigurationErrorException falls conf keine korrekte Funktionsbeschreibung 
   *        ist oder die Funktion einen context benötigt aber null übergeben wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static Function parseGrandchildren(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Vector andFunction = new Vector();
    Iterator iter1 = conf.iterator();
    while (iter1.hasNext())
    {
      Iterator iter = ((ConfigThingy)iter1.next()).iterator();
      while (iter.hasNext())
      {
        Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
        
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
   * @param dialogLib die Dialogbibliothek anhand derer Referenzen auf Dialoge 
   *        aufgelöst werden sollen.
   * @param context Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige
   *        Werte. Zur Unterscheidung der verschiedenen Instanzen dient die context
   *        Map, in der die verschiedenen Instanzen abgelegt werden. 
   *        Wird hier null übergeben, dann wird eine ConfigurationErrorException
   *        geworfen, wenn conf eine Funktion enthält, die einen Kontext benötigt. 
   * @throws ConfigurationErrorException falls conf keine korrekte Funktionsbeschreibung 
   *        ist oder die Funktion einen context benötigt aber null übergeben wurde.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static Function parseChildren(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Vector andFunction = new Vector();
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
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
   * @param dialogLib die Dialogbibliothek anhand derer Referenzen auf Dialoge 
   *        aufgelöst werden sollen.
   * @param context Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige
   *        Werte. Zur Unterscheidung der verschiedenen Instanzen dient die context
   *        Map, in der die verschiedenen Instanzen abgelegt werden. 
   *        Wird hier null übergeben, dann wird eine ConfigurationErrorException
   *        geworfen, wenn conf eine Funktion enthält, die einen Kontext benötigt. 
   * @throws ConfigurationErrorException falls conf keine korrekte Funktionsbeschreibung 
   *        ist oder die Funktion einen context benötigt aber null übergeben wurde.
   * 
   * TESTED
   */
  public static Function parse(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    String name = conf.getName();
    
    if (conf.count() == 0)
      return new StringLiteralFunction(name);
    
    if (name.equals("AND"))
    {
      return parseAND(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("NOT"))
    {
      return parseNOT(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("OR"))
    {
      return parseOR(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("VALUE"))
    {
      return parseVALUE(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("MATCH"))
    {
      return parseMATCH(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("REPLACE"))
    {
      return parseREPLACE(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("IF"))
    {
      return parseIF(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("EXTERN"))
    {
      return new ExternalFunctionFunction(conf);
    }
    else if (name.equals("DIALOG"))
    {
      return parseDIALOG(conf, dialogLib, context);
    }
    else if (name.equals("BIND"))
    {
      return parseBIND(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("SELECT"))
    {
      return parseSELECT(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("CAT") || name.equals("THEN") || name.equals("ELSE"))
    {
      return parseCAT(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("FORMAT") || name.equals("DIVIDE"))
    {
      return parseDIVIDE(conf, funcLib, dialogLib, context);
    }
    
    throw new ConfigurationErrorException("\""+name+"\" ist keine unterstützte Grundfunktion");
  }

  private static Function parseCAT(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Vector catFunction = new Vector(conf.count());
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      Function fun = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
      catFunction.add(fun);
    }
    
    catFunction.trimToSize();
    return new CatFunction(catFunction);
  }

  private static Function parseSELECT(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Vector selFunction = new Vector(conf.count());
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      Function fun = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
      selFunction.add(fun);
    }
    
    selFunction.trimToSize();
    return new SelectFunction(selFunction);
  }

  private static Function parseBIND(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    ConfigThingy funcConf = conf.query("FUNCTION"); //funcConf = <query results> - FUNCTION - ...
    if (funcConf.count() != 1)
      throw new ConfigurationErrorException("Funktion vom Typ \"BIND\" erfordert genau 1 Unterelement FUNCTION");
    
    Function func;
    funcConf = (ConfigThingy)funcConf.iterator().next();  //funcConf = FUNCTION - ...
    if (funcConf.count() == 0)
      throw new ConfigurationErrorException("Bei Funktionen vom Typ \"BIND\" muss nach \"FUNCTION\" ein Funktionsname oder eine Funktion folgen.");
    if (funcConf.count() > 1)
      throw new ConfigurationErrorException("Bei Funktionen vom Typ \"BIND\" darf nach \"FUNCTION\" keine Liste sondern nur ein Funktionsname oder eine Funktion folgen.");

    funcConf = (ConfigThingy)funcConf.iterator().next(); //<Funktionsname>|<Funktion> - ...
    
    if (funcConf.count() == 0) //d.h. es wurde nur ein <Funktionsname> angegeben
    {
      String funcName = funcConf.toString();
      
      func = funcLib.get(funcName);
      if (func == null)
        throw new ConfigurationErrorException("Funktion \""+funcName+"\" wird verwendet, bevor sie definiert ist");
    }
    else //if (funcConf.count() > 0) d.h. es wurde eine ganze Funktion angegeben
    {
      func = parse(funcConf, funcLib, dialogLib, context);
    }
    
    return new BindFunction(func, conf, funcLib, dialogLib, context);
  }

  private static Function parseDIALOG(ConfigThingy conf, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    if (conf.count() != 2)
      throw new ConfigurationErrorException("Funktion vom Typ \"DIALOG\" erfordert genau 2 Parameter, nicht "+conf.count());
    
    String dialogName;
    String dataName; 
    
    try{
      dialogName = conf.getFirstChild().toString();
      dataName = conf.getLastChild().toString();
    }catch(NodeNotFoundException x)
    {
      /*Kann nicht sein, weil count() getestet*/
      dialogName = null;
      dataName = null;
    }
    
    Dialog dialog = dialogLib.get(dialogName);
    if (dialog == null)
      throw new ConfigurationErrorException("Dialog \""+dialogName+"\" ist nicht definiert, aber wird in DIALOG-Funktion verwendet");
    
    if (context == null)
      throw new ConfigurationErrorException("DIALOG-Funktion ist kontextabhängig und kann deshalb hier nicht verwendet werden.");
    
    return new DialogFunction(dialogName, dialog, dataName, context);
  }

  private static Function parseIF(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    ConfigThingy thenConf = conf.query("THEN");
    ConfigThingy elseConf = conf.query("ELSE");
    if (thenConf.count() > 1 || elseConf.count() > 1)
      throw new ConfigurationErrorException("In IF darf maximal ein THEN und ein ELSE vorhanden sein");
    
    if (conf.count() - thenConf.count() - elseConf.count() != 1)
      throw new ConfigurationErrorException("IF muss genau eine Bedingung enthalten.");
    
    if (thenConf.count() == 0)
    {
      thenConf = new ConfigThingy("dummy");
      thenConf.add("THEN").add("");
    }
    
    if (elseConf.count() == 0)
    {
      elseConf = new ConfigThingy("dummy");
      elseConf.add("ELSE").add("");
    }
    
    Iterator iter = conf.iterator();
    ConfigThingy condition;
    do{ //oben wurde überprüft, dass es genau einen Knoten gibt, der nicht ELSE oder THEN ist
      condition = (ConfigThingy)iter.next();
    } while(condition.getName().equals("THEN") || condition.equals("ELSE"));
    
    Function ifFun = parse(condition, funcLib, dialogLib, context);
    Function thenFun = parseChildren(thenConf, funcLib, dialogLib, context);
    Function elseFun = parseChildren(elseConf, funcLib, dialogLib, context);
    
    return new IfFunction(ifFun, thenFun, elseFun);
  }

  private static Function parseREPLACE(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    if (conf.count() != 3)
      throw new ConfigurationErrorException("Funktion vom Typ \"REPLACE\" erfordert genau 3 Parameter, nicht "+conf.count());
    
    Function strFun;
    Function reFun;
    Function repFun;
    
    Iterator iter = conf.iterator();
    strFun = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
    reFun  = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
    repFun = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
    
    String regex = reFun.getString(noValues);
    Pattern p;
    try{
      p = Pattern.compile(regex);
    }catch(PatternSyntaxException x)
    {
      throw new ConfigurationErrorException("Fehler in regex \""+regex+"\"", x);
    }
    return new ReplaceFunction(strFun, p, repFun);
  }

  private static Function parseMATCH(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    if (conf.count() != 2)
      throw new ConfigurationErrorException("Funktion vom Typ \"MATCH\" erfordert genau 2 Parameter, nicht "+conf.count());
    
    Function strFun;
    Function reFun; 
    
    try{
      strFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
      reFun = parse(conf.getLastChild(), funcLib, dialogLib, context);
    }catch(NodeNotFoundException x)
    {
      /*Kann nicht sein, weil count() getestet*/
      strFun = null;
      reFun = null;
    }
    
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

  private static Function parseVALUE(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    if (conf.count() != 1)
      throw new ConfigurationErrorException("Funktion vom Typ \"VALUE\" erfordert genau 1 Parameter, nicht "+conf.count()); 
    
    Function valueNameFun;
    try
    {
      valueNameFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
    }
    catch (NodeNotFoundException e) { 
      /* Kann nicht passieren. Hab count() getestet. */
      valueNameFun = null;
    }
    
    return new ValueFunction(valueNameFun.getString(noValues));
  }

  private static Function parseOR(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Vector orFunction = new Vector();
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
      orFunction.add(cons);
    }
    
    orFunction.trimToSize();
    return new OrFunction(orFunction);
  }

  private static Function parseNOT(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Vector notFunction = new Vector();
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
      notFunction.add(cons);
    }
    
    notFunction.trimToSize();
    return new NotFunction(notFunction);
  }

  private static Function parseAND(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Vector andFunction = new Vector();
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      Function cons = parse((ConfigThingy)iter.next(), funcLib, dialogLib, context);
      andFunction.add(cons);
    }
    
    andFunction.trimToSize();
    return new AndFunction(andFunction);
  }
  
  private static Function parseDIVIDE(ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
  {
    Function dividendFun = null;
    Function byFun = null;
    int minScale = 0;
    int maxScale = -1;
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy funConf = (ConfigThingy)iter.next();
      String name = funConf.getName();
      if (name.equals("BY"))
      {
        if (funConf.count() != 1)
          throw new ConfigurationErrorException("BY-Angabe von DIVIDE/FORMAT muss genau eine Funktion oder einen String enthalten");
        
        if (byFun != null)
          throw new ConfigurationErrorException("DIVIDE/FORMAT-Funktion darf maximal eine BY-Angabe haben");
        
        byFun = parseChildren(funConf, funcLib, dialogLib, context);
      } else if (name.equals("MINSCALE"))
      {
        int num = -1;
        try{
          if (funConf.getFirstChild().count() == 0)
          {
            num = Integer.parseInt(funConf.toString());
          }
        }catch(Exception x){}
        
        if (num < 0)
          throw new ConfigurationErrorException("MINSCALE-Angabe von DIVIDE/FORMAT muss \"<NichtNegativeZahl>\" sein");

        minScale = num;
        
      } else if (name.equals("MAXSCALE"))
      {
        int num = -1;
        try{
          if (funConf.getFirstChild().count() == 0)
          {
            num = Integer.parseInt(funConf.toString());
          }
        }catch(Exception x){}
        
        if (num < 0)
          throw new ConfigurationErrorException("MAXSCALE-Angabe von DIVIDE/FORMAT muss \"<NichtNegativeZahl>\" sein");

        maxScale = num;        
      } else
      {
        if (dividendFun != null) throw new ConfigurationErrorException("Bei DIVIDE/FORMAT-Funktion wurde mehr als eine unqualifizierte Funktion angegeben. Beachten Sie, dass der Divisor mit BY(...) umschlossen sein muss.");
        dividendFun = parse(funConf, funcLib, dialogLib, context);
      }
    }
    
    if (dividendFun == null)
      throw new ConfigurationErrorException("Bei DIVIDE/FORMAT-Funktion muss genau eine unqualifizierte Funktion angegeben werden");
    
    if (maxScale < 0)
      throw new ConfigurationErrorException("DIVIDE/FORMAT erfordert die Angabe MAXSCALE \"<NichtNegativeZahl>\"");
    
    if (maxScale < minScale)
      throw new ConfigurationErrorException("MINSCALE muss kleiner oder gleich MAXSCALE sein");
    
    return new DivideFunction(dividendFun, byFun, minScale, maxScale);
  }

  private static class DialogFunction implements Function
  {
    private Dialog dialog;
    private String dataName;
    private String dialogName;
    
    public DialogFunction(String dialogName, Dialog dialog, String dataName, Map context)
    throws ConfigurationErrorException
    {
      this.dialog = dialog.instanceFor(context);
      this.dataName = dataName;
      this.dialogName = dialogName;
    }
    
    public String[] parameters() { return noParams; }
    public void getFunctionDialogReferences(Collection set)
    {
      set.add(dialogName);
    }

    public String getString(Values parameters)
    {
      Object data = dialog.getData(dataName);
      if (data == null) return Function.ERROR;
      return data.toString();
    }

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }
  }
  
  private static class BindFunction implements Function
  {
    private Map mapParamNameToSetFunction = new HashMap();
    private Function func;
    private String[] params;
    private Set functionDialogReferences = new HashSet();
    
    public BindFunction(Function func, ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib, Map context) throws ConfigurationErrorException
    {
      this.func = func;
      
      Set myParams = new HashSet(Arrays.asList(func.parameters()));
      Set setFuncParams = new HashSet();
      
      ConfigThingy sets = conf.query("SET");
      Iterator iter = sets.iterator();
      while (iter.hasNext())
      {
        ConfigThingy set = (ConfigThingy)iter.next();
        if (set.count() != 2)
          throw new ConfigurationErrorException("BIND: SET benötigt genau 2 Parameter");
        
        String name;
        Function setFunc;
        try{
          name = set.getFirstChild().toString();
          setFunc = parse(set.getLastChild(), funcLib, dialogLib, context);
        }catch(NodeNotFoundException x)
        { //kann nicht passieren, hab count() getestet
          name = null; setFunc = null;
        }
        
        if (mapParamNameToSetFunction.containsKey(name))
          throw new ConfigurationErrorException("BIND: Der Parameter "+name+" wird 2 mal mit SET gebunden");
        
        mapParamNameToSetFunction.put(name, setFunc);
        setFuncParams.addAll(Arrays.asList(setFunc.parameters()));
        setFunc.getFunctionDialogReferences(functionDialogReferences);
        
        /*
         * name wurde gebunden, wird also nicht mehr als Parameter benötigt,
         * außer wenn eine der setFuncs den Parameter benötigt. In diesem Fall ist
         * der Parameter in setFuncParams erfasst und wird nachher wieder zu
         * myparams hinzugefügt. 
         */
        myParams.remove(name); 
      }
      
      /*
       * Parameter der setFuncs den benötigten Parametern hinzufügen und
       * in String[] konvertieren.
       */
      myParams.addAll(setFuncParams);
      params = (String[])myParams.toArray(new String[0]);
    }
    
    public String[] parameters()
    {
      return params;
    }
    
    public void getFunctionDialogReferences(Collection set)
    {
      set.addAll(functionDialogReferences);
    }

    public String getString(Values parameters)
    {
      TranslatedValues trans = new TranslatedValues(parameters); 
      String res = func.getString(trans);
      if (trans.hasError) return Function.ERROR;
      return res;
    }

    public boolean getBoolean(Values parameters)
    {
      TranslatedValues trans = new TranslatedValues(parameters); 
      boolean res = func.getBoolean(trans);
      if (trans.hasError) return false;
      return res;
    }
    
    private class TranslatedValues implements Values
    {
      private Values values;
      public boolean hasError;
     
      public TranslatedValues(Values values)
      {
        this.values = values;
        hasError = false;
      }
      
      public boolean hasValue(String id)
      {
        /*
         * ACHTUNG! Wenn die id an eine Funktion gebunden ist, dann liefern
         * wir immer true, auch wenn die Funktion evtl. einen Fehler liefert.
         * Es gäbe 2 alternative Verhaltensweisen:
         * - nur true liefern, wenn values.hasValue(id2) == true für alle
         *   Parameter, die die Funktion erwartet.
         *   Nachteil: Zu strikt bei Funktionen, bei denen manche Argumente
         *   optional sind
         * - die Funktion ausführen und sehen, ob sie einen Fehler liefert
         *   Nachteil: Die Funktion wird zu einem Zeitpunkt ausgeführt, zu dem
         *   dies evtl. nicht erwartet wird. Außerdem wird die Funktion einmal
         *   mehr ausgeführt. Bei externen Funktionen (insbes. Basic-Makros) ist
         *   dies nicht wünschenswert. 
         */
        if (mapParamNameToSetFunction.containsKey(id)) return true;
        return (values.hasValue(id));
      }

      public String getString(String id)
      {
        Function setFunc = (Function)mapParamNameToSetFunction.get(id);
        if (setFunc != null)
        {
          String res = setFunc.getString(values);
          if (res == Function.ERROR) 
          {
            hasError = true;
            return "";
          }
          return res;
        }
        return values.getString(id);
      }

      public boolean getBoolean(String id)
      {
        Function setFunc = (Function)mapParamNameToSetFunction.get(id);
        if (setFunc != null)
        {
          String res = setFunc.getString(values);
          if (res == Function.ERROR) 
          {
            hasError = true;
            return false;
          }
          return res.equalsIgnoreCase("true");
        }
        return values.getBoolean(id);
      }
    }
  }
  
  private static class ValueFunction implements Function
  {
    String[] params;
    
    public ValueFunction(String valueName)
    {
      params = new String[]{valueName};
    }
    
    public String[] parameters()
    {
      return params;
    }
    
    public void getFunctionDialogReferences(Collection set) {}

    public String getString(Values parameters)
    {
      if (!parameters.hasValue(params[0])) return Function.ERROR;
      return parameters.getString(params[0]);
    }

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }
  }
  
  private static class StringLiteralFunction implements Function
  {
    private String literal;
    private boolean bool;
    
    public String[] parameters() { return noParams; }
    public void getFunctionDialogReferences(Collection set) {}

    public StringLiteralFunction(String str)
    {
      literal = str;
      bool = literal.equalsIgnoreCase("true");
    }
    
    public String getString(Values parameters)
    {
      return literal;
    }

    public boolean getBoolean(Values parameters)
    {
      return bool; 
    }
  }
  
  private static class ExternalFunctionFunction implements Function
  {
    private ExternalFunction func;

    public ExternalFunctionFunction(ConfigThingy conf) throws ConfigurationErrorException
    {
      func = new ExternalFunction(conf, WollMuxFiles.getClassLoader());
    }
    
    public String[] parameters()
    {
      return func.parameters();
    }
    
    public void getFunctionDialogReferences(Collection set) {}
    
    public String getString(Values parameters)
    {
      try
      {
        Object result = func.invoke(parameters);
        if (result == null) throw new Exception("Unbekannter Fehler beim Ausführen einer externen Funktion");
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
    
    public String getString(Values parameters)
    {
      String str = input.getString(parameters);
      if (str == Function.ERROR) return Function.ERROR;
      if (pattern.matcher(str).matches()) return "true";
      return "false";
    }

    public String[] parameters()
    {
      return input.parameters();
    }
    
    public void getFunctionDialogReferences(Collection set) 
    {
      input.getFunctionDialogReferences(set);
    }

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }
  }

  private static class ReplaceFunction implements Function
  {
    private Pattern pattern;
    private Function input;
    private Function replace;
    private String[] params;
    
    public ReplaceFunction(Function input, Pattern p, Function replace)
    {
      pattern = p;
      this.input = input;
      this.replace = replace;
      Set paramset = new HashSet();
      paramset.addAll(Arrays.asList(input.parameters()));
      paramset.addAll(Arrays.asList(replace.parameters()));
      this.params = (String[])paramset.toArray(new String[]{});
    }
    
    public String getString(Values parameters)
    {
      String str = input.getString(parameters);
      String repStr = replace.getString(parameters);
      if (str == Function.ERROR || repStr == Function.ERROR) return Function.ERROR;
      return pattern.matcher(str).replaceAll(repStr);
    }

    public String[] parameters()
    {
      return params;
    }
    
    public void getFunctionDialogReferences(Collection set) 
    {
      input.getFunctionDialogReferences(set);
      replace.getFunctionDialogReferences(set);
    }

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }
  }

  
  private static abstract class MultiFunction implements Function
  {
    protected Collection subFunction;
    private String[] params;
    
    public MultiFunction(Collection subFunction)
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
    
    public abstract String getString(Values parameters);

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }

    public String[] parameters() {  return params; }
    public void getFunctionDialogReferences(Collection set) 
    {
      Iterator iter = subFunction.iterator();
      while (iter.hasNext())
      {
        ((Function)iter.next()).getFunctionDialogReferences(set);
      }
    }
  }

  private static class CatFunction extends MultiFunction
  {
    public CatFunction(Collection subFunction)
    {
      super(subFunction);
    }

    public String getString(Values parameters)
    {
      Iterator iter = subFunction.iterator();
      StringBuffer res = new StringBuffer();
      while (iter.hasNext())
      {
        Function func = (Function)iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        res.append(str);
      }
      return res.toString();
    }
  }
  
  private static class AndFunction extends MultiFunction
  {
  
    public AndFunction(Collection subFunction)
    { 
      super(subFunction);
    }
    
    public String getString(Values parameters)
    { 
      Iterator iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = (Function)iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        if (!str.equalsIgnoreCase("true")) return "false";
      }
      return "true";
    }
  }
  
  private static class NotFunction extends MultiFunction
  {
  
    public NotFunction(Collection subFunction)
    { 
      super(subFunction);
    }
    
    public String getString(Values parameters)
    { 
      Iterator iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = (Function)iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        if (!str.equalsIgnoreCase("true")) return "true";
      }
      return "false";
    }
  }
  
  
  private static class OrFunction extends MultiFunction
  {
    public OrFunction(Collection subFunction)
    {
      super(subFunction);
    }
    
    public String getString(Values parameters)
    {
      Iterator iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = (Function)iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        if (str.equalsIgnoreCase("true")) return "true";
      }
      return "false";
    }
  }
  
  private static class SelectFunction extends MultiFunction
  {
    public SelectFunction(Collection subFunction)
    {
      super(subFunction);
    }

    public String getString(Values parameters)
    {
      Iterator iter = subFunction.iterator();
      String str = Function.ERROR; //wird nie zurückgeliefert, da subFunction nie leer sein kann
      while (iter.hasNext())
      {
        Function func = (Function)iter.next();
        str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        if (str.length() > 0) return str;
      }
      return str;
    }
  }
  
  private static class IfFunction implements Function
  {
    private Function ifFunction;
    private Function thenFunction;
    private Function elseFunction;
    private String[] params;
    
    public IfFunction(Function ifFunction, Function thenFunction, Function elseFunction)
    {
      Set myparams = new HashSet();
      myparams.addAll(Arrays.asList(ifFunction.parameters()));
      myparams.addAll(Arrays.asList(thenFunction.parameters()));
      myparams.addAll(Arrays.asList(elseFunction.parameters()));
      params = (String[])myparams.toArray(new String[0]);
      this.ifFunction = ifFunction;
      this.thenFunction = thenFunction;
      this.elseFunction = elseFunction;
    }
    
    public String[] parameters()
    {
      return params;
    }
    
    public void getFunctionDialogReferences(Collection set)
    {
      ifFunction.getFunctionDialogReferences(set);
      thenFunction.getFunctionDialogReferences(set);
      elseFunction.getFunctionDialogReferences(set);
    }

    public String getString(Values parameters)
    {
      String condition = ifFunction.getString(parameters);
      if (condition == Function.ERROR) return Function.ERROR;
      if (condition.equalsIgnoreCase("true"))
        return thenFunction.getString(parameters);
      else
        return elseFunction.getString(parameters);
    }

    public boolean getBoolean(Values parameters)
    {
      String condition = ifFunction.getString(parameters);
      if (condition == Function.ERROR) return false;
      if (condition.equalsIgnoreCase("true"))
        return thenFunction.getBoolean(parameters);
      else
        return elseFunction.getBoolean(parameters);
    }
  }
  
  private static class DivideFunction implements Function
  {
    private Function dividendFunction;
    private Function divisorFunction = null;
    private int minScale;
    private int maxScale;
    private String[] params;
    
    /**
     * Wenn divisorFunction null ist wird 1 angenommen.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED*/
    public DivideFunction(Function dividendFunction, Function divisorFunction, int minScale, int maxScale)
    {
      Set myparams = new HashSet();
      myparams.addAll(Arrays.asList(dividendFunction.parameters()));
      if (divisorFunction != null) myparams.addAll(Arrays.asList(divisorFunction.parameters()));
      params = (String[])myparams.toArray(new String[0]);
      this.dividendFunction = dividendFunction;
      this.divisorFunction = divisorFunction;
      this.minScale = minScale;
      this.maxScale = maxScale;
    }
    
    public String[] parameters()
    {
      return params;
    }
    
    public void getFunctionDialogReferences(Collection set)
    {
      dividendFunction.getFunctionDialogReferences(set);
      if (divisorFunction != null) divisorFunction.getFunctionDialogReferences(set);
    }

    public String getString(Values parameters)
    { //TESTED
      char decimalPoint = '.';
      try{
        decimalPoint = ((DecimalFormat)NumberFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
      }catch(Exception x){};
      
      String dividend = dividendFunction.getString(parameters);
      if (dividend == Function.ERROR) return Function.ERROR;
      
      String divisor = "1";
      if (divisorFunction != null)
        divisor = divisorFunction.getString(parameters);
      if (divisor == Function.ERROR) return Function.ERROR;
      
      /*
       * Falls der Dezimaltrenner nicht '.' ist, ersetzte alle '.' durch
       * etwas, das kein Dezimaltrenner ist, um eine NumberFormatException
       * beim Konvertieren zu provozieren. Dies ist eine Vorsichtsmaßnahme, da
       * '.' zum Beispiel in Deutschland alls Gruppierungszeichen verwendet wird
       * und wir wollen nicht fälschlicher weise "100.000" als 100 interpretieren,
       * wenn die eingebende Person 100000 gemeint hat.
       */
      if (decimalPoint != '.')
      {
        dividend = dividend.replace('.','ß');
        divisor = divisor.replace('.','ß');
      }
      
      BigDecimal bigResult;
      try{
        BigDecimal bigDividend = new BigDecimal(dividend.replace(decimalPoint,'.'));
        BigDecimal bigDivisor  = new BigDecimal(divisor.replace(decimalPoint,'.'));
        
        bigResult = bigDividend.divide(bigDivisor, maxScale, RoundingMode.HALF_UP);
      }catch(Exception x)
      {
        return Function.ERROR;
      }

      /*
       * NumberFormat kann leider nicht zum formatieren verwendet werden, da es nur die
       * Genauigkeit von double hat (laut Java Doc).
       */
      
      String result = bigResult.stripTrailingZeros().toPlainString();
      StringBuilder buffy = new StringBuilder(result);
      int idx = result.indexOf('.');
      if (idx == 0)
      {
        buffy.insert(0,"0");
        idx = 1;
      }
      if (idx < 0 && minScale > 0)
      {
        buffy.append(".0");
        idx = buffy.length() - 2;
      }
      
      int decimalDigits = (idx < 0) ? 0 : buffy.length() - idx - 1;
      for (int i = decimalDigits; i < minScale; ++i) buffy.append('0');

      result = buffy.toString().replace('.', decimalPoint);
      return result;
    }

    public boolean getBoolean(Values parameters)
    {
      return false;
    }
  }

  
  private static class AlwaysTrueFunction implements Function
  {
    public String[] parameters()
    {
      return noParams;
    }
    
    public void getFunctionDialogReferences(Collection set) {} 

    public String getString(Values parameters)
    {
      return "true";
    }

    public boolean getBoolean(Values parameters)
    {
     return true;
    }
  }

  private static void printFunction(String funcStr, Function func, Values vals)
  {
    System.out.println("\nFunktion: "+funcStr);
    System.out.print("Parameter: ");
    String[] args = func.parameters();
    for (int i = 0; i < args.length; ++i)
    {
      System.out.print((i==0?"":", ")+args[i]+"=");
      if (vals.hasValue(args[i]))
        System.out.print("\""+vals.getString(args[i])+"\""+"("+vals.getBoolean(args[i])+")");
      else
        System.out.print("n/a");
    }
    System.out.println();
    System.out.println("Funktionsergebnis: \""+func.getString(vals)+"\"("+func.getBoolean(vals)+")");
  }
  
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    
    Map context = new HashMap();
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();
    
    dialogLib.add("Empfaenger", new Dialog(){
      public Dialog instanceFor(Map context) { return this;}
      public Object getData(String id)
      { if (id.equals("Strasse")) return "Herzog-Wilhelm-Str. 22";
        return null; }
      public void show(ActionListener dialogEndListener, FunctionLibrary funcLib, DialogLibrary dialogLib){}
      public Collection getSchema()
      {
        return new Vector(0);
      }
      });
    
    printFunction("alwaysTrueFunction()",alwaysTrueFunction(), noValues);
    
    String funcStr = "BAR('true' 'false' 'true')";
    ConfigThingy funcThingy = new ConfigThingy("FOO", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseGrandchildren(funcThingy, funcLib, dialogLib, context), noValues);
    
    funcStr = "BAR('true' 'true' 'true')";
    funcThingy = new ConfigThingy("FOO", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseGrandchildren(funcThingy, funcLib, dialogLib, context), noValues);
    
    funcStr = "AND('true' 'false' 'true')";
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), noValues);
    
    funcStr = "OR('true' 'false' 'true')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), noValues);
    
    Values.SimpleMap values = new Values.SimpleMap();
    values.put("Name","WollMux");
    values.put("Funktion", "BKS");
    values.put("LegtEier", "true");
    values.put("GibtMilch", "false");
    values.put("Anrede", "Herr");
    values.put("TextWeibl", "(weibl.)");
    values.put("TextMaennl", "(männl.)");
    
    funcStr = "OR(VALUE('Fehler') VALUE('LegtEier') VALUE('GibtMilch'))";
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "AND(VALUE('LegtEier') VALUE('Fehler'))"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "NOT(VALUE('GibtMilch'))"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "MATCH(VALUE('Name'),'llMux')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "REPLACE(CAT(VALUE('Name') '%n' 'Mux'),'Mux\\p{Space}Mux', 'Max')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "MATCH(VALUE('Name'),'.*llMux.*')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "EXTERN(URL 'vnd.sun.star.script:WollMux.Trafo.MannOderFrau?language=Basic&location=application' PARAMS('Anrede', 'TextWeibl', 'TextMaennl'))"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "EXTERN(URL 'java:de.muenchen.allg.itd51.wollmux.func.Standard.herrFrauText' PARAMS('Anrede', 'TextWeibl', 'TextMaennl'))"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    Function func = parseChildren(funcThingy, funcLib, dialogLib, context);
    funcLib.add("AnredeText", func);
    values.put("Anrede", "Frau");
    printFunction(funcStr, func, values);
    
    funcStr = "BIND( FUNCTION('AnredeText') SET('TextWeibl' 'die') SET('TextMaennl' 'der' ) SET('Anrede' VALUE('SGAnrede')))"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    values.put("SGAnrede", "Herr");
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "BIND( FUNCTION('AnredeText') SET('TextWeibl' 'die') SET('TextMaennl' 'der' ) SET('Anrede' VALUE('Fehler')))"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "CAT('1' '2' '3')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "CAT('1' VALUE('Fehler') '3')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "SELECT('' '1' VALUE('Fehler') '3')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "SELECT('' VALUE('Fehler') '3')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "SELECT('' '')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "IF(THEN 'then' VALUE('Fehler') ELSE 'else')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "IF(THEN 'then' VALUE('LegtEier') ELSE 'else')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "IF(VALUE('GibtMilch') THEN 'then'  ELSE 'else')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "IF(VALUE('GibtMilch') THEN 'then')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "IF(VALUE('LegtEier') ELSE 'else')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "DIALOG('Empfaenger', 'Strasse')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "DIALOG('Empfaenger', 'Fehler')"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    funcStr = "AND()"; 
    funcThingy = new ConfigThingy("BAR", new URL("file:///"), 
        new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context), values);
    
    System.exit(0);
  }

}
