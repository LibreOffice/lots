/*
 * Dateiname: FunctionFactory.java
 * Projekt  : WollMux
 * Funktion : Erzeugt Functions aus ConfigThingys.
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
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
 * 03.08.2007 | BNK | +SUM,MINUS,PRODUCT,DIFF,ABS,SIGN
 * 08.08.2007 | BNK | SELECT-Verhalten im Fehlerfalle entsprechend Doku implementiert
 *                  | +NUMCMP, LE, GE, GT, LT 
 * 09.08.2007 | BNK | +ISERROR, ISERRORSTRING, ONERROR (für SELECT)
 * 01.02.2008 | BNK | +LENGTH
 * 07.03.2008 | BNK | [R16048] doppelte Parameter eliminieren
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Erzeugt Functions aus ConfigThingys.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FunctionFactory.class);

  /**
   * Eine Funktion, die immer true liefert.
   */
  private static final Function myAlwaysTrueFunction = new AlwaysTrueFunction();

  private FunctionFactory()
  {
    // hide public ctor
  }

  /**
   * Liefert eine Funktion, die immer true liefert.
   */
  public static Function alwaysTrueFunction()
  {
    return myAlwaysTrueFunction;
  }

  /**
   * Erzeugt ein Function-Objekt aus den ENKELN von conf. Hat conf keine Enkel, so
   * wird null geliefert. Hat conf genau einen Enkel, so wird eine Funktion
   * geliefert, die diesem Enkel entspricht. Hat conf mehr als einen Enkel, so wird
   * eine Funktion geliefert, die alle Enkel als Booleans auswertet und
   * UND-verknüpft.
   * 
   * @param funcLib
   *          die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *          aufgelöst werden sollen.
   * @param dialogLib
   *          die Dialogbibliothek anhand derer Referenzen auf Dialoge aufgelöst
   *          werden sollen.
   * @param context
   *          Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige Werte.
   *          Zur Unterscheidung der verschiedenen Instanzen dient die context Map,
   *          in der die verschiedenen Instanzen abgelegt werden. Wird hier null
   *          übergeben, dann wird eine ConfigurationErrorException geworfen, wenn
   *          conf eine Funktion enthält, die einen Kontext benötigt.
   * @throws ConfigurationErrorException
   *           falls conf keine korrekte Funktionsbeschreibung ist oder die Funktion
   *           einen context benötigt aber null übergeben wurde.
   */
  public static Function parseGrandchildren(ConfigThingy conf,
      FunctionLibrary funcLib, DialogLibrary dialogLib, Map<Object, Object> context)
  {
    List<Function> andFunction = new ArrayList<>();
    for (ConfigThingy children : conf)
    {
      for (ConfigThingy func : children)
      {
        Function cons = parse(func, funcLib, dialogLib, context);

        andFunction.add(cons);
      }
    }

    if (andFunction.isEmpty())
    {
      return null;
    }
    if (andFunction.size() == 1)
    {
      return andFunction.get(0);
    }

    return new AndFunction(andFunction);
  }

  /**
   * Erzeugt ein Function-Objekt aus den KINDERN von conf. Hat conf keine Kinder, so
   * wird null geliefert. Hat conf genau ein Kind, so wird eine Funktion geliefert,
   * die diesem Kind entspricht. Hat conf mehr als ein Kind, so wird eine Funktion
   * geliefert, die alle Kinder als Booleans auswertet und UND-verknüpft.
   * 
   * @param funcLib
   *          die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *          aufgelöst werden sollen.
   * @param dialogLib
   *          die Dialogbibliothek anhand derer Referenzen auf Dialoge aufgelöst
   *          werden sollen.
   * @param context
   *          Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige Werte.
   *          Zur Unterscheidung der verschiedenen Instanzen dient die context Map,
   *          in der die verschiedenen Instanzen abgelegt werden. Wird hier null
   *          übergeben, dann wird eine ConfigurationErrorException geworfen, wenn
   *          conf eine Funktion enthält, die einen Kontext benötigt.
   * @throws ConfigurationErrorException
   *           falls conf keine korrekte Funktionsbeschreibung ist oder die Funktion
   *           einen context benötigt aber null übergeben wurde.
   */
  public static Function parseChildren(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    List<Function> andFunction = new ArrayList<>();
    for (ConfigThingy func : conf)
    {
      Function cons = parse(func, funcLib, dialogLib, context);
      andFunction.add(cons);
    }

    if (andFunction.isEmpty())
    {
      return null;
    }
    if (andFunction.size() == 1)
    {
      return andFunction.get(0);
    }

    return new AndFunction(andFunction);
  }

  /**
   * Liefert ein Function Objekt zu conf, wobei conf selbst schon ein erlaubter
   * Knoten der Funktionsbeschreibung (z,B, "AND" oder "MATCH") sein muss.
   * 
   * @param funcLib
   *          die Funktionsbibliothek anhand derer Referenzen auf Funktionen
   *          aufgelöst werden sollen.
   * @param dialogLib
   *          die Dialogbibliothek anhand derer Referenzen auf Dialoge aufgelöst
   *          werden sollen.
   * @param context
   *          Manche Grundfunktionen (insbes. DIALOG) halten kontextabhängige Werte.
   *          Zur Unterscheidung der verschiedenen Instanzen dient die context Map,
   *          in der die verschiedenen Instanzen abgelegt werden. Wird hier null
   *          übergeben, dann wird eine ConfigurationErrorException geworfen, wenn
   *          conf eine Funktion enthält, die einen Kontext benötigt.
   * @throws ConfigurationErrorException
   *           falls conf keine korrekte Funktionsbeschreibung ist oder die Funktion
   *           einen context benötigt aber null übergeben wurde.
   */
  public static Function parse(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    String name = conf.getName();

    if (conf.count() == 0) return new StringLiteralFunction(name);

    if (name.equals("AND"))
    {
      return new AndFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("NOT"))
    {
      return new NotFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("OR"))
    {
      return new OrFunction(conf, funcLib, dialogLib, context);
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
    else if (name.equals("SPLIT"))
    {
      return parseSPLIT(conf, funcLib, dialogLib, context);
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
      return new SelectFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("CAT") || name.equals("THEN") || name.equals("ELSE"))
    {
      return new CatFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("LENGTH"))
    {
      return new LengthFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("FORMAT") || name.equals("DIVIDE"))
    {
      return parseDIVIDE(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("MINUS"))
    {
      return new MinusFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("SUM"))
    {
      return new SumFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("DIFF"))
    {
      return new DiffFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("PRODUCT"))
    {
      return new ProductFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("ABS"))
    {
      return new AbsFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("SIGN"))
    {
      return new SignFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("LT"))
    {
      return new NumberCompareFunction(0, 1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("LE"))
    {
      return new NumberCompareFunction(1, 1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("GT"))
    {
      return new NumberCompareFunction(0, -1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("GE"))
    {
      return new NumberCompareFunction(-1, -1, "true", conf, funcLib, dialogLib,
        context);
    }
    else if (name.equals("NUMCMP"))
    {
      return new NumberCompareFunction(Integer.MAX_VALUE, Integer.MAX_VALUE, null,
        conf, funcLib, dialogLib, context);
    }
    else if (name.equals("STRCMP"))
    {
      return new StrCmpFunction(conf, funcLib, dialogLib, context);
    }
    else if (name.equals("ISERROR"))
    {
      return new IsErrorFunction(true, conf, funcLib, dialogLib, context);
    }
    else if (name.equals("ISERRORSTRING"))
    {
      return new IsErrorFunction(false, conf, funcLib, dialogLib, context);
    }

    if (name.length() == 0)
    {
      throw new ConfigurationErrorException(
        L.m("Öffnende Klammer ohne voranstehenden Funktionsnamen gefunden. ")
          + outputErrorPosition(conf));
    }
    else
    {
      throw new ConfigurationErrorException(L.m(
        "\"%1\" ist keine unterstützte Grundfunktion. ", name)
        + outputErrorPosition(conf));
    }
  }

  /**
   * Liefert "Text an der Fehlerstelle: " + die ersten 100 Zeichen der
   * Stringdarstellung von conf
   */
  private static String outputErrorPosition(ConfigThingy conf)
  {
    String str = conf.stringRepresentation();
    int end = 100;
    if (str.length() < end) end = str.length();
    return L.m("Text an der Fehlerstelle: %1", str.substring(0, end));
  }

  private static Function parseBIND(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    ConfigThingy funcConf = conf.query("FUNCTION"); // funcConf = <query results> -
    // FUNCTION - ...
    if (funcConf.count() != 1)
      throw new ConfigurationErrorException(
        L.m("Funktion vom Typ \"BIND\" erfordert genau 1 Unterelement FUNCTION"));

    Function func;
    funcConf = funcConf.iterator().next(); // funcConf = FUNCTION - ...
    if (funcConf.count() == 0)
      throw new ConfigurationErrorException(
        L.m("Bei Funktionen vom Typ \"BIND\" muss nach \"FUNCTION\" ein Funktionsname oder eine Funktion folgen."));
    if (funcConf.count() > 1)
      throw new ConfigurationErrorException(
        L.m("Bei Funktionen vom Typ \"BIND\" darf nach \"FUNCTION\" keine Liste sondern nur ein Funktionsname oder eine Funktion folgen."));

    funcConf = funcConf.iterator().next(); // <Funktionsname>|<Funktion> - ...

    if (funcConf.count() == 0) // d.h. es wurde nur ein <Funktionsname> angegeben
    {
      String funcName = funcConf.toString();

      func = funcLib.get(funcName);
      if (func == null)
        throw new ConfigurationErrorException(L.m(
          "Funktion \"%1\" wird verwendet, bevor sie definiert ist", funcName));
    }
    else // d.h. es wurde eine ganze Funktion angegeben
    {
      func = parse(funcConf, funcLib, dialogLib, context);
    }

    return new BindFunction(func, conf, funcLib, dialogLib, context);
  }

  private static Function parseDIALOG(ConfigThingy conf, DialogLibrary dialogLib,
      Map<Object, Object> context)
  {
    if (conf.count() != 2)
      throw new ConfigurationErrorException(L.m(
        "Funktion vom Typ \"DIALOG\" erfordert genau 2 Parameter, nicht %1",
        conf.count()));

    String dialogName;
    String dataName;

    try
    {
      dialogName = conf.getFirstChild().toString();
      dataName = conf.getLastChild().toString();
    }
    catch (NodeNotFoundException x)
    {
      /* Kann nicht sein, weil count() getestet */
      dialogName = null;
      dataName = null;
    }

    Dialog dialog = dialogLib.get(dialogName);
    if (dialog == null)
      throw new ConfigurationErrorException(L.m(
        "Dialog \"%1\" ist nicht definiert, aber wird in DIALOG-Funktion verwendet",
        dialogName));

    if (context == null)
      throw new ConfigurationErrorException(
        L.m("DIALOG-Funktion ist kontextabhängig und kann deshalb hier nicht verwendet werden."));

    return new DialogFunction(dialogName, dialog, dataName, context);
  }

  private static Function parseIF(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    ConfigThingy thenConf = conf.query("THEN");
    ConfigThingy elseConf = conf.query("ELSE");
    if (thenConf.count() > 1 || elseConf.count() > 1)
      throw new ConfigurationErrorException(
        L.m("In IF darf maximal ein THEN und ein ELSE vorhanden sein"));

    if (conf.count() - thenConf.count() - elseConf.count() != 1)
      throw new ConfigurationErrorException(
        L.m("IF muss genau eine Bedingung enthalten."));

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

    Iterator<ConfigThingy> iter = conf.iterator();
    ConfigThingy condition;
    do
    { // oben wurde überprüft, dass es genau einen Knoten gibt, der nicht ELSE oder
      // THEN ist
      condition = iter.next();
    } while (condition.getName().equals("THEN")
      || condition.getName().equals("ELSE"));

    Function ifFun = parse(condition, funcLib, dialogLib, context);
    Function thenFun = parseChildren(thenConf, funcLib, dialogLib, context);
    Function elseFun = parseChildren(elseConf, funcLib, dialogLib, context);

    return new IfFunction(ifFun, thenFun, elseFun);
  }

  private static Function parseREPLACE(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.count() != 3)
      throw new ConfigurationErrorException(L.m(
        "Funktion vom Typ \"REPLACE\" erfordert genau 3 Parameter, nicht %1",
        conf.count()));

    Function strFun;
    Function reFun;
    Function repFun;

    Iterator<ConfigThingy> iter = conf.iterator();
    strFun = parse(iter.next(), funcLib, dialogLib, context);
    reFun = parse(iter.next(), funcLib, dialogLib, context);
    repFun = parse(iter.next(), funcLib, dialogLib, context);

    String regex = reFun.getString(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Fehler in regex \"%1\"", regex), x);
    }
    return new ReplaceFunction(strFun, p, repFun);
  }

  private static Function parseSPLIT(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    if (conf.count() != 3)
      throw new ConfigurationErrorException(L.m(
        "Funktion vom Typ \"SPLIT\" erfordert genau 3 Parameter, nicht %1",
        conf.count()));

    Function strFun;
    Function reFun;
    int idx;

    Iterator<ConfigThingy> iter = conf.iterator();
    strFun = parse(iter.next(), funcLib, dialogLib, context);
    reFun = parse(iter.next(), funcLib, dialogLib, context);

    idx = -1;
    try
    {
      ConfigThingy idxConf = iter.next();
      if (idxConf.count() == 0)
      {
        idx = Integer.parseInt(idxConf.toString());
      }
    }
    catch (Exception x)
    {}
    if (idx < 0)
      throw new ConfigurationErrorException(L.m(
        "Index-Argument von %1 muss \"<NichtNegativeGanzeZahl>\" sein",
        conf.getName()));

    String regex = reFun.getString(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Fehler in regex \"%1\"", regex), x);
    }
    return new SplitFunction(strFun, p, idx);
  }

  private static Function parseMATCH(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    Function strFun;
    Function reFun;

    try
    {
      strFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
      reFun = parse(conf.getLastChild(), funcLib, dialogLib, context);
    }
    catch (NodeNotFoundException x)
    {
      throw new ConfigurationErrorException(
          L.m("Funktion vom Typ \"MATCH\" erfordert genau 2 Parameter, nicht %1", conf.count()), x);
    }

    String regex = reFun.getString(new Values.None());
    Pattern p;
    try
    {
      p = Pattern.compile(regex);
    }
    catch (PatternSyntaxException x)
    {
      throw new ConfigurationErrorException(L.m("Fehler in regex \"%1\"", regex), x);
    }
    return new MatchFunction(strFun, p);
  }

  private static Function parseVALUE(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    Function valueNameFun;
    try
    {
      valueNameFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
    }
    catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(L.m(
          "Funktion vom Typ \"VALUE\" erfordert genau 1 Parameter, nicht %1",
          conf.count()), e);
    }

    return new ValueFunction(valueNameFun.getString(new Values.None()));
  }

  private static Function parseDIVIDE(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    Function dividendFun = null;
    Function byFun = null;
    int minScale = 0;
    int maxScale = -1;

    for (ConfigThingy funConf : conf)
    {
      String name = funConf.getName();
      if (name.equals("BY"))
      {
        if (funConf.count() != 1)
        {
          throw new ConfigurationErrorException(L.m(
            "BY-Angabe von %1 muss genau eine Funktion oder einen String enthalten",
            conf.getName()));
        }

        if (byFun != null)
        {
          throw new ConfigurationErrorException(L.m(
            "%1-Funktion darf maximal eine BY-Angabe haben", conf.getName()));
        }

        byFun = parseChildren(funConf, funcLib, dialogLib, context);
      }
      else if (name.equals("MIN"))
      {
        int num = -1;
        try
        {
          if (funConf.getFirstChild().count() == 0)
          {
            num = Integer.parseInt(funConf.toString());
          }
        }
        catch (Exception x)
        {}

        if (num < 0)
        {
          throw new ConfigurationErrorException(L.m(
            "MIN-Angabe von %1 muss \"<NichtNegativeGanzeZahl>\" sein",
            conf.getName()));
        }

        minScale = num;

      }
      else if (name.equals("MAX"))
      {
        int num = -1;
        try
        {
          if (funConf.getFirstChild().count() == 0)
          {
            num = Integer.parseInt(funConf.toString());
          }
        }
        catch (Exception x)
        {}

        if (num < 0)
        {
          throw new ConfigurationErrorException(L.m(
            "MAX-Angabe von %1 muss \"<NichtNegativeGanzeZahl>\" sein",
            conf.getName()));
        }

        maxScale = num;
      }
      else
      {
        if (dividendFun != null)
        {
          throw new ConfigurationErrorException(
            L.m(
              "Bei %1-Funktion wurde mehr als eine unqualifizierte Funktion angegeben. Beachten Sie, dass der Divisor mit BY(...) umschlossen sein muss.",
              conf.getName()));
        }
        dividendFun = parse(funConf, funcLib, dialogLib, context);
      }
    }

    if (dividendFun == null)
    {
      throw new ConfigurationErrorException(L.m(
        "Bei %1-Funktion muss genau eine unqualifizierte Funktion angegeben werden",
        conf.getName()));
    }

    if (maxScale < 0)
    {
      if (byFun == null)
      {
        // Division durch 1 nichts kaputt macht
        maxScale = 1024; // eigentlich sollte hier Integer.MAX_SIZE stehen, aber
        // auch bei Division durch 1 reserviert die BigDecimal-Klasse Speicher entsprechend der maximalen Stelligkeit
      }
      else
      {
        throw new ConfigurationErrorException(
          L.m(
            "%1 erfordert die Angabe MAX \"<NichtNegativeZahl>\", wenn mit BY ein Divisor angegeben wird",
            conf.getName()));
      }
    }

    if (maxScale < minScale)
    {
      throw new ConfigurationErrorException(L.m(
        "Bei %1 muss MIN kleiner oder gleich MAX sein", conf.getName()));
    }

    return new DivideFunction(dividendFun, byFun, minScale, maxScale);
  }

  /**
   * Parst die "Funktionen" Abschnitte aus conf und liefert eine entsprechende
   * FunctionLibrary.
   * 
   * @param context
   *          der Kontext in dem die Funktionsdefinitionen ausgewertet werden sollen
   *          (insbesondere DIALOG-Funktionen). ACHTUNG! Hier werden Werte
   *          gespeichert, es ist nicht nur ein Schlüssel.
   * 
   * @param baselib
   *          falls nicht-null wird diese als Fallback verlinkt, um Funktionen zu
   *          liefern, die anderweitig nicht gefunden werden.
   */
  public static FunctionLibrary parseFunctions(ConfigThingy conf,
      DialogLibrary dialogLib, Map<Object, Object> context, FunctionLibrary baselib)
  {
    return parseFunctions(new FunctionLibrary(baselib), conf, "Funktionen",
      dialogLib, context);
  }

  /**
   * Parst die Inhalte von conf,query(section) als Funktionsdefinitionen und fügt sie
   * funcs hinzu.
   * 
   * @param context
   *          der Kontext in dem die Funktionsdefinitionen ausgewertet werden sollen
   *          (insbesondere DIALOG-Funktionen). ACHTUNG! Hier werden Werte
   *          gespeichert, es ist nicht nur ein Schlüssel.
   * 
   * @return funcs
   */
  public static FunctionLibrary parseFunctions(FunctionLibrary funcs,
      ConfigThingy conf, String section, DialogLibrary dialogLib,
      Map<Object, Object> context)
  {
    conf = conf.query(section);
    Iterator<ConfigThingy> parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      Iterator<ConfigThingy> iter = parentIter.next().iterator();
      while (iter.hasNext())
      {
        ConfigThingy funcConf = iter.next();
        String name = funcConf.getName();
        try
        {
          Function func =
            parseChildren(funcConf, funcs, dialogLib, context);
          funcs.add(name, func);
        }
        catch (ConfigurationErrorException e)
        {
          LOGGER.error(L.m(
            "Fehler beim Parsen der Funktion \"%1\" im Abschnitt \"%2\"", name,
            section), e);
        }
      }
    }

    return funcs;
  }

  public static Map<String, Function> parseTrafos(ConfigThingy trafoConf, String nodeName, FunctionLibrary funcLib, DialogLibrary dialogLib,
      Map<Object, Object> context)
  {
    Map<String, Function> trafos = new HashMap<>();
    Iterator<ConfigThingy> suIter = trafoConf.query(nodeName, 1).iterator();
    while (suIter.hasNext())
    {
      ConfigThingy spaltenumsetzung = suIter.next();

      for (ConfigThingy transConf : spaltenumsetzung)
      {
        String name = transConf.getName();
        try
        {
          Function func = FunctionFactory.parseChildren(transConf, funcLib, dialogLib, context);
          if (func == null)
            throw new ConfigurationErrorException(
                L.m("Leere Funktionsdefinition ist nicht erlaubt. Verwenden Sie stattdessen den leeren String \"\""));
          trafos.put(name, func);
        } catch (ConfigurationErrorException e)
        {
          LOGGER.error(L.m("Fehler beim Parsen der Spaltenumsetzungsfunktion für Ergebnisspalte \"%1\"", name), e);
        }
      }
    }
    return trafos;
  }
}
