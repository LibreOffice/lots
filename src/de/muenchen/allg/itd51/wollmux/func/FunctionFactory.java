/*
 * Dateiname: FunctionFactory.java
 * Projekt  : WollMux
 * Funktion : Erzeugt Functions aus ConfigThingys.
 * 
 * Copyright (c) 2008-2016 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.func;

import java.awt.event.ActionListener;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
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
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;

/**
 * Erzeugt Functions aus ConfigThingys.
 * 
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
  private static final String[] noParams = new String[] {};

  /**
   * Liefert eine Funktion, die immer true liefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static Function parseGrandchildren(ConfigThingy conf,
      FunctionLibrary funcLib, DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    Vector<Function> andFunction = new Vector<Function>();
    Iterator<ConfigThingy> iter1 = conf.iterator();
    while (iter1.hasNext())
    {
      Iterator<ConfigThingy> iter = iter1.next().iterator();
      while (iter.hasNext())
      {
        Function cons = parse(iter.next(), funcLib, dialogLib, context);

        andFunction.add(cons);
      }
    }

    if (andFunction.isEmpty()) return null;
    if (andFunction.size() == 1) return andFunction.get(0);

    andFunction.trimToSize();
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
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static Function parseChildren(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    Vector<Function> andFunction = new Vector<Function>();
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      Function cons = parse(iter.next(), funcLib, dialogLib, context);
      andFunction.add(cons);
    }

    if (andFunction.isEmpty()) return null;
    if (andFunction.size() == 1) return andFunction.get(0);

    andFunction.trimToSize();
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
   * 
   *           TESTED
   */
  public static Function parse(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
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
      throw new ConfigurationErrorException(
        L.m("Öffnende Klammer ohne voranstehenden Funktionsnamen gefunden. ")
          + outputErrorPosition(conf));
    else
      throw new ConfigurationErrorException(L.m(
        "\"%1\" ist keine unterstützte Grundfunktion. ", name)
        + outputErrorPosition(conf));
  }

  /**
   * Liefert "Text an der Fehlerstelle: " + die ersten 100 Zeichen der
   * Stringdarstellung von conf
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static String outputErrorPosition(ConfigThingy conf)
  {
    String str = conf.stringRepresentation();
    // str = str.replaceAll("\\p{Space}", " ");
    int end = 100;
    if (str.length() < end) end = str.length();
    return L.m("Text an der Fehlerstelle: %1", str.substring(0, end));
  }

  private static Function parseBIND(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
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
    else
    // if (funcConf.count() > 0) d.h. es wurde eine ganze Funktion angegeben
    {
      func = parse(funcConf, funcLib, dialogLib, context);
    }

    return new BindFunction(func, conf, funcLib, dialogLib, context);
  }

  private static Function parseDIALOG(ConfigThingy conf, DialogLibrary dialogLib,
      Map<Object, Object> context) throws ConfigurationErrorException
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
      throws ConfigurationErrorException
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
      throws ConfigurationErrorException
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

    String regex = reFun.getString(noValues);
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
      throws ConfigurationErrorException
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

    String regex = reFun.getString(noValues);
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
      throws ConfigurationErrorException
  {
    if (conf.count() != 2)
      throw new ConfigurationErrorException(L.m(
        "Funktion vom Typ \"MATCH\" erfordert genau 2 Parameter, nicht %1",
        conf.count()));

    Function strFun;
    Function reFun;

    try
    {
      strFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
      reFun = parse(conf.getLastChild(), funcLib, dialogLib, context);
    }
    catch (NodeNotFoundException x)
    {
      /*
       * Kann nicht sein, weil count() getestet wurde. Statement ist nur hier, um
       * Warnungen des Compilers und von findBugs zu besänftigen.
       */
      throw new RuntimeException(x);
    }

    String regex = reFun.getString(noValues);
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
      throws ConfigurationErrorException
  {
    if (conf.count() != 1)
      throw new ConfigurationErrorException(L.m(
        "Funktion vom Typ \"VALUE\" erfordert genau 1 Parameter, nicht %1",
        conf.count()));

    Function valueNameFun;
    try
    {
      valueNameFun = parse(conf.getFirstChild(), funcLib, dialogLib, context);
    }
    catch (NodeNotFoundException e)
    {
      /*
       * Kann nicht sein, weil count() getestet wurde. Statement ist nur hier, um
       * Warnungen des Compilers und von findBugs zu besänftigen.
       */
      throw new RuntimeException(e);
    }

    return new ValueFunction(valueNameFun.getString(noValues));
  }

  private static Function parseDIVIDE(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    Function dividendFun = null;
    Function byFun = null;
    int minScale = 0;
    int maxScale = -1;
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy funConf = iter.next();
      String name = funConf.getName();
      if (name.equals("BY"))
      {
        if (funConf.count() != 1)
          throw new ConfigurationErrorException(L.m(
            "BY-Angabe von %1 muss genau eine Funktion oder einen String enthalten",
            conf.getName()));

        if (byFun != null)
          throw new ConfigurationErrorException(L.m(
            "%1-Funktion darf maximal eine BY-Angabe haben", conf.getName()));

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
          throw new ConfigurationErrorException(L.m(
            "MIN-Angabe von %1 muss \"<NichtNegativeGanzeZahl>\" sein",
            conf.getName()));

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
          throw new ConfigurationErrorException(L.m(
            "MAX-Angabe von %1 muss \"<NichtNegativeGanzeZahl>\" sein",
            conf.getName()));

        maxScale = num;
      }
      else
      {
        if (dividendFun != null)
          throw new ConfigurationErrorException(
            L.m(
              "Bei %1-Funktion wurde mehr als eine unqualifizierte Funktion angegeben. Beachten Sie, dass der Divisor mit BY(...) umschlossen sein muss.",
              conf.getName()));
        dividendFun = parse(funConf, funcLib, dialogLib, context);
      }
    }

    if (dividendFun == null)
      throw new ConfigurationErrorException(L.m(
        "Bei %1-Funktion muss genau eine unqualifizierte Funktion angegeben werden",
        conf.getName()));

    if (maxScale < 0)
    {
      if (byFun == null) // falls kein Divisor, dann ist MAX nicht erforderlich, da
        // Division durch 1 nichts kaputt macht
        maxScale = 1024; // eigentlich sollte hier Integer.MAX_SIZE stehen, aber
      // auch bei Division durch 1 reserviert die
      // BigDecimal-Klasse Speicher entsprechend der maximalen
      // Stelligkeit
      else
        throw new ConfigurationErrorException(
          L.m(
            "%1 erfordert die Angabe MAX \"<NichtNegativeZahl>\", wenn mit BY ein Divisor angegeben wird",
            conf.getName()));
    }

    if (maxScale < minScale)
      throw new ConfigurationErrorException(L.m(
        "Bei %1 muss MIN kleiner oder gleich MAX sein", conf.getName()));

    return new DivideFunction(dividendFun, byFun, minScale, maxScale);
  }

  private static class DialogFunction implements Function
  {
    private Dialog dialog;

    private String dataName;

    private String dialogName;

    public DialogFunction(String dialogName, Dialog dialog, String dataName,
        Map<Object, Object> context) throws ConfigurationErrorException
    {
      this.dialog = dialog.instanceFor(context);
      this.dataName = dataName;
      this.dialogName = dialogName;
    }

    public String[] parameters()
    {
      return noParams;
    }

    public void getFunctionDialogReferences(Collection<String> set)
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
    private Map<String, Function> mapParamNameToSetFunction =
      new HashMap<String, Function>();

    private Function func;

    private String[] params;

    private Set<String> functionDialogReferences = new HashSet<String>();

    public BindFunction(Function func, ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      this.func = func;

      Set<String> myParams = new HashSet<String>(Arrays.asList(func.parameters()));
      Set<String> setFuncParams = new HashSet<String>();

      ConfigThingy sets = conf.query("SET");
      Iterator<ConfigThingy> iter = sets.iterator();
      while (iter.hasNext())
      {
        ConfigThingy set = iter.next();
        if (set.count() != 2)
          throw new ConfigurationErrorException(
            L.m("BIND: SET benötigt genau 2 Parameter"));

        String name;
        Function setFunc;
        try
        {
          name = set.getFirstChild().toString();
          setFunc = parse(set.getLastChild(), funcLib, dialogLib, context);
        }
        catch (NodeNotFoundException x)
        { // kann nicht passieren, hab count() getestet
          name = null;
          setFunc = null;
        }

        if (mapParamNameToSetFunction.containsKey(name))
          throw new ConfigurationErrorException(L.m(
            "BIND: Der Parameter %1 wird 2 mal mit SET gebunden", name));

        mapParamNameToSetFunction.put(name, setFunc);
        setFuncParams.addAll(Arrays.asList(setFunc.parameters()));
        setFunc.getFunctionDialogReferences(functionDialogReferences);

        /*
         * name wurde gebunden, wird also nicht mehr als Parameter benötigt, außer
         * wenn eine der setFuncs den Parameter benötigt. In diesem Fall ist der
         * Parameter in setFuncParams erfasst und wird nachher wieder zu myparams
         * hinzugefügt.
         */
        myParams.remove(name);
      }

      /*
       * Parameter der setFuncs den benötigten Parametern hinzufügen und in String[]
       * konvertieren.
       */
      myParams.addAll(setFuncParams);
      params = myParams.toArray(new String[0]);
    }

    public String[] parameters()
    {
      return params;
    }

    public void getFunctionDialogReferences(Collection<String> set)
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
         * ACHTUNG! Wenn die id an eine Funktion gebunden ist, dann liefern wir immer
         * true, auch wenn die Funktion evtl. einen Fehler liefert. Es gäbe 2
         * alternative Verhaltensweisen: - nur true liefern, wenn
         * values.hasValue(id2) == true für alle Parameter, die die Funktion
         * erwartet. Nachteil: Zu strikt bei Funktionen, bei denen manche Argumente
         * optional sind - die Funktion ausführen und sehen, ob sie einen Fehler
         * liefert Nachteil: Die Funktion wird zu einem Zeitpunkt ausgeführt, zu dem
         * dies evtl. nicht erwartet wird. Außerdem wird die Funktion einmal mehr
         * ausgeführt. Bei externen Funktionen (insbes. Basic-Makros) ist dies nicht
         * wünschenswert.
         */
        if (mapParamNameToSetFunction.containsKey(id)) return true;
        return (values.hasValue(id));
      }

      public String getString(String id)
      {
        Function setFunc = mapParamNameToSetFunction.get(id);
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
        Function setFunc = mapParamNameToSetFunction.get(id);
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

  private static class IsErrorFunction implements Function
  {
    private Function func;

    private boolean objectCompare;

    /**
     * Falls objectCompare == true, wird == Function,ERROR getestet, ansonsten
     * equals(Function,ERROR).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public IsErrorFunction(boolean objectCompare, ConfigThingy conf,
        FunctionLibrary funcLib, DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      if (conf.count() != 1)
        throw new ConfigurationErrorException(L.m(
          "Funktion %1 muss genau einen Parameter haben", conf.getName()));

      this.objectCompare = objectCompare;
      func = parseChildren(conf, funcLib, dialogLib, context);
    }

    public String[] parameters()
    {
      return func.parameters();
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {
      func.getFunctionDialogReferences(set);
    }

    public String getString(Values parameters)
    {
      String str = func.getString(parameters);
      if (isError(str))
        return "true";
      else
        return "false";
    }

    public boolean getBoolean(Values parameters)
    {
      String str = func.getString(parameters);
      return isError(str);
    }

    private boolean isError(String str)
    {
      if (objectCompare)
      {
        return Function.ERROR == str;
      }
      else
      {
        return Function.ERROR.equals(str);
      }
    }

  }

  private static class ValueFunction implements Function
  {
    String[] params;

    public ValueFunction(String valueName)
    {
      params = new String[] { valueName };
    }

    public String[] parameters()
    {
      return params;
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {}

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

    public String[] parameters()
    {
      return noParams;
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {}

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

    public ExternalFunctionFunction(ConfigThingy conf)
        throws ConfigurationErrorException
    {
      func = new ExternalFunction(conf, WollMuxFiles.getClassLoader());
    }

    public String[] parameters()
    {
      return func.parameters();
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {}

    public String getString(Values parameters)
    {
      try
      {
        Object result = func.invoke(parameters);
        if (result == null)
          throw new Exception(
            L.m("Unbekannter Fehler beim Ausführen einer externen Funktion"));
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

    public void getFunctionDialogReferences(Collection<String> set)
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
      Set<String> paramset = new HashSet<String>();
      paramset.addAll(Arrays.asList(input.parameters()));
      paramset.addAll(Arrays.asList(replace.parameters()));
      this.params = paramset.toArray(new String[] {});
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

    public void getFunctionDialogReferences(Collection<String> set)
    {
      input.getFunctionDialogReferences(set);
      replace.getFunctionDialogReferences(set);
    }

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }
  }

  private static class SplitFunction implements Function
  {
    private String regex;

    private Function input;

    private int index;

    private String[] params;

    public SplitFunction(Function input, Pattern p, int idx)
    {
      this.regex = p.toString();
      this.input = input;
      this.index = idx;
      this.params = input.parameters();
    }

    public String getString(Values parameters)
    {
      String str = input.getString(parameters);
      if (str == Function.ERROR) return Function.ERROR;
      String[] a = str.split(regex);
      if (index < 0 || index >= a.length) return "";
      return a[index];
    }

    public String[] parameters()
    {
      return params;
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {
      input.getFunctionDialogReferences(set);
    }

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }
  }

  private static abstract class MultiFunction implements Function
  {
    protected Collection<Function> subFunction;

    private String[] params;

    public MultiFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      Vector<Function> subFunction = new Vector<Function>(conf.count());
      Iterator<ConfigThingy> iter = conf.iterator();
      while (iter.hasNext())
      {
        ConfigThingy subFunConf = iter.next();
        if (handleParam(subFunConf, funcLib, dialogLib, context)) continue;
        Function fun = parse(subFunConf, funcLib, dialogLib, context);
        subFunction.add(fun);
      }

      if (subFunction.size() == 0)
        throw new ConfigurationErrorException(L.m(
          "Funktion %1 erfordert mindestens einen Parameter", conf.getName()));

      subFunction.trimToSize();
      init(subFunction);
    }

    /**
     * Liefert true, wenn conf von handleParam bereits vollständig behandelt wurde
     * und nicht mehr als Subfunktion erfasst werden soll. Diese Funktion wird von
     * Unterklassen überschrieben, um spezielle Parameter zu behandeln. ACHTUNG! Wird
     * diese Methode überschrieben, so sind normalerweise auch
     * {@link #getAdditionalParams()} und
     * {@link #getFunctionDialogReferences(Collection)} zu überschreiben, um die
     * zusätzlichen Funktionen aus dem behandelten Parameter zu behandeln.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      return false;
    }

    /**
     * Liefert die Namen der Parameter der zusätzlichen Funktionen, die von
     * {@link #handleParam(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}
     * geparst wurden oder null, falls es keine gibt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    protected String[] getAdditionalParams()
    {
      return null;
    }

    public MultiFunction(Collection<Function> subFunction)
    {
      init(subFunction);
    }

    private void init(Collection<Function> subFunction)
    {
      this.subFunction = subFunction;

      // Ein Set wäre performanter, aber so wird die Reihenfolge beibehalten. Evtl.
      // macht es Sinn, auch die anderen Function-Klassen, die im Moment Sets
      // verwenden auf das Konstrukt mit Liste und contains()-Test umzustellen und
      // dadurch eine wohldefinierte Reihenfolge zu erreichen. Derzeit ist der Bedarf
      // dafür aber nicht gegeben, insbes. da insertFunctionValue umgeschrieben
      // werden soll und der FM4000 bereits umgeschrieben wurde um nicht von der
      // Reihenfolge abzuhängen.
      ArrayList<String> deps = new ArrayList<String>();
      for (Function f : subFunction)
      {
        String[] params = f.parameters();
        for (String str : params)
        {
          if (!deps.contains(str)) deps.add(str);
        }
      }

      String[] additionalparams = getAdditionalParams();
      if (additionalparams != null)
      {
        for (String str : additionalparams)
        {
          if (!deps.contains(str)) deps.add(str);
        }
      }

      params = new String[deps.size()];
      params = deps.toArray(params);
    }

    public abstract String getString(Values parameters);

    public boolean getBoolean(Values parameters)
    {
      return getString(parameters).equalsIgnoreCase("true");
    }

    public String[] parameters()
    {
      return params;
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {
      Iterator<Function> iter = subFunction.iterator();
      while (iter.hasNext())
      {
        iter.next().getFunctionDialogReferences(set);
      }
    }
  }

  private static class CatFunction extends MultiFunction
  {
    public CatFunction(Collection<Function> subFunction)
    {
      super(subFunction);
    }

    public CatFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    public String getString(Values parameters)
    {
      Iterator<Function> iter = subFunction.iterator();
      StringBuffer res = new StringBuffer();
      while (iter.hasNext())
      {
        Function func = iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        res.append(str);
      }
      return res.toString();
    }
  }

  private static class LengthFunction extends CatFunction
  {
    public LengthFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    public String getString(Values parameters)
    {
      String res = super.getString(parameters);
      if (res == Function.ERROR) return Function.ERROR;
      return "" + res.length();
    }

    public boolean getBoolean(Values parameters)
    {
      return false;
    }
  }

  private static class AndFunction extends MultiFunction
  {

    public AndFunction(Collection<Function> subFunction)
    {
      super(subFunction);
    }

    public AndFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    public String getString(Values parameters)
    {
      Iterator<Function> iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        if (!str.equalsIgnoreCase("true")) return "false";
      }
      return "true";
    }
  }

  private static class NotFunction extends MultiFunction
  {

    public NotFunction(Collection<Function> subFunction)
    {
      super(subFunction);
    }

    public NotFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    public String getString(Values parameters)
    {
      Iterator<Function> iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        if (!str.equalsIgnoreCase("true")) return "true";
      }
      return "false";
    }
  }

  private static class OrFunction extends MultiFunction
  {
    public OrFunction(Collection<Function> subFunction)
    {
      super(subFunction);
    }

    public OrFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    public String getString(Values parameters)
    {
      Iterator<Function> iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        if (str.equalsIgnoreCase("true")) return "true";
      }
      return "false";
    }
  }

  private static class SelectFunction extends MultiFunction
  {
    private Function onErrorFunction;

    public SelectFunction(Collection<Function> subFunction)
    {
      super(subFunction);
    }

    public SelectFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      if (conf.getName().equals("ONERROR"))
      {
        onErrorFunction = new CatFunction(conf, funcLib, dialogLib, context);
        return true;
      }
      return false;
    }

    protected String[] getAdditionalParams()
    {
      if (onErrorFunction != null)
        return onErrorFunction.parameters();
      else
        return null;
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {
      super.getFunctionDialogReferences(set);
      if (onErrorFunction != null) onErrorFunction.getFunctionDialogReferences(set);
    }

    public String getString(Values parameters)
    {
      Iterator<Function> iter = subFunction.iterator();
      String result = Function.ERROR;
      while (iter.hasNext())
      {
        Function func = iter.next();
        String str = func.getString(parameters);
        if (str != Function.ERROR)
        {
          result = str;
          if (str.length() > 0) break;
        }
        else if (onErrorFunction != null)
        {
          return onErrorFunction.getString(parameters);
        }
      }
      return result;
    }
  }

  private static class StrCmpFunction extends MultiFunction
  {
    public StrCmpFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
      if (subFunction.size() < 2)
        throw new ConfigurationErrorException(L.m(
          "Funktion %1 erfordert mindestens 2 Parameter", conf.getName()));
    }

    public String getString(Values parameters)
    {
      Iterator<Function> iter = subFunction.iterator();
      Function func = iter.next();
      String compare = func.getString(parameters);
      if (compare == Function.ERROR) return Function.ERROR;
      int prevCompare = 0;
      while (iter.hasNext())
      {
        func = iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        int res = Integer.signum(compare.compareTo(str));
        if (res * prevCompare < 0) return "0";
        prevCompare += res;
      }

      switch (Integer.signum(prevCompare))
      {
        case -1:
          return "-1";
        case 1:
          return "1";
        default:
          return "true";
      }
    }
  }

  private static abstract class NumberFunction extends MultiFunction
  {
    protected char decimalPoint = '.';

    public NumberFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
      try
      {
        decimalPoint =
          ((DecimalFormat) NumberFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
      }
      catch (Exception x)
      {}
      ;
    }

    /**
     * Startet eine neue Auswertung der Funktion für Parameter parameters. Im Falle
     * einer Summen-Funktion würde dies den Summenzähler mit 0 initialisieren.
     * 
     * @return Falls zu diesem Zeitpunkt bereits ein Ergebnis bestimmt werden kann
     *         (z.B. Function.ERROR, wenn ein benötigter Parameter nicht in
     *         parameters übergeben wurde), so wird dieses zurückgeliefert, ansonsten
     *         null.
     */
    protected abstract String initComputation(Values parameters);

    /**
     * Fügt den Wert num der aktuellen Berechnung hinzu. Im Falle einer
     * Summen-Funktion würde er auf den Summen-Zähler addiert. Darf eine Exception
     * werfen. In diesem Fall wird die Funktion Function.ERROR zurückliefern.
     * 
     * @return Falls zu diesem Zeitpunkt bereits ein Ergebnis bestimmt werden kann
     *         (z.B. im Falle einer Vergleichsfunktion, die Kurzschlussauswertung
     *         macht), so wird dieses zurückgeliefert, ansonsten null.
     */
    protected abstract String addToComputation(BigDecimal num);

    /**
     * Wird aufgerufen, nachdem der letzte Wert mittels addComputation() verarbeitet
     * wurde, wenn jeder addComputation()-Aufruf null geliefert hat.
     * 
     * @return das Endergebnis der Berechnung. null ist NICHT erlaubt.
     */
    protected abstract String computationResult();

    public String getString(Values parameters)
    {
      String result = initComputation(parameters);
      if (result != null) return result;
      Iterator<Function> iter = subFunction.iterator();
      while (iter.hasNext())
      {
        Function func = iter.next();
        String str = func.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        try
        {
          BigDecimal num = makeBigDecimal(str);
          result = addToComputation(num);
          if (result != null) return result;
        }
        catch (Exception x)
        {
          return Function.ERROR;
        }
      }
      return computationResult();
    }

    protected BigDecimal makeBigDecimal(String str) throws NumberFormatException
    {
      /*
       * Falls der Dezimaltrenner nicht '.' ist, ersetzte alle '.' durch etwas, das
       * kein Dezimaltrenner ist, um eine NumberFormatException beim Konvertieren zu
       * provozieren. Dies ist eine Vorsichtsmaßnahme, da '.' zum Beispiel in
       * Deutschland alls Gruppierungszeichen verwendet wird und wir wollen nicht
       * fälschlicher weise "100.000" als 100 interpretieren, wenn die eingebende
       * Person 100000 gemeint hat.
       */
      if (decimalPoint != '.') str = str.replace('.', 'ß');

      return new BigDecimal(str.replace(decimalPoint, '.'));
    }

    /**
     * Liefert eine Stringrepräsentation von num
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    protected String formatBigDecimal(BigDecimal num)
    {
      /*
       * Workaround für Bug
       * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480539
       * stripTrailingZeros() funktioniert nicht für 0.
       */
      String result;
      if (num.compareTo(BigDecimal.ZERO) == 0)
        result = "0";
      else
        result = num.stripTrailingZeros().toPlainString().replace('.', decimalPoint);
      return result;
    }
  }

  private static class SumFunction extends NumberFunction
  {
    private BigDecimal sum;

    public SumFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    protected String initComputation(Values parameters)
    {
      sum = BigDecimal.ZERO;
      return null;
    }

    protected String addToComputation(BigDecimal num)
    {
      sum = sum.add(num);
      return null;
    }

    protected String computationResult()
    {
      return formatBigDecimal(numericComputationResult());
    }

    protected BigDecimal numericComputationResult()
    {
      return sum;
    }
  }

  private static class MinusFunction extends SumFunction
  {
    public MinusFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    protected String computationResult()
    {
      return formatBigDecimal(numericComputationResult().negate());
    }
  }

  private static class AbsFunction extends SumFunction
  {
    public AbsFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    protected String computationResult()
    {
      return formatBigDecimal(numericComputationResult().abs());
    }
  }

  private static class SignFunction extends SumFunction
  {
    public SignFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    protected String computationResult()
    {
      // signum() liefert int, deswegen hier nur ""+ und nicht formatBigDecimal()
      return "" + numericComputationResult().signum();
    }
  }

  private static class DiffFunction extends NumberFunction
  {
    private BigDecimal sum;

    private boolean first;

    public DiffFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    protected String initComputation(Values parameters)
    {
      sum = BigDecimal.ZERO;
      first = true;
      return null;
    }

    protected String addToComputation(BigDecimal num)
    {
      if (first)
      {
        sum = sum.add(num);
        first = false;
      }
      else
      {
        sum = sum.subtract(num);
      }
      return null;
    }

    protected String computationResult()
    {
      return formatBigDecimal(sum);
    }
  }

  private static class ProductFunction extends NumberFunction
  {
    private BigDecimal prod;

    public ProductFunction(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
    }

    protected String initComputation(Values parameters)
    {
      prod = BigDecimal.ONE;
      return null;
    }

    protected String addToComputation(BigDecimal num)
    {
      prod = prod.multiply(num);
      return null;
    }

    protected String computationResult()
    {
      return formatBigDecimal(prod);
    }
  }

  private static class NumberCompareFunction extends NumberFunction
  {
    private BigDecimal compare;

    private BigDecimal lowBound;

    private BigDecimal highBound;

    private int cmp1;

    private int cmp2;

    private int prevCompare;

    private String result;

    private BigDecimal margin;

    Function marginFun;

    /**
     * Die Unterfunktionen von conf werden als BigDecimals ausgewertet und jeweils
     * mit dem Ergebnis der ersten Funktion verglichen via compareTo, wobei falls
     * MARGIN angegeben ist, dessen Abstand <= abs(MARGIN) ist als gleich angesehen
     * wird (entspricht compareTo Ergebnis 0). cmp1 und cmp2 sind Abbruchergebnisse
     * dieses Vergleichs. Sobald ein compareTo cmp1 oder cmp2 liefert bricht die
     * Berechnung ab und es wird das Ergebnis "false" geliefert. Falls ein compareTo
     * +1 liefert und ein späteres compareTo -1, so wird abgebrochen und als Ergebnis
     * "0" geliefert. Analog wenn ein compareTo -1 liefert und eine späteres +1.
     * Falls result nicht null ist, so wird dieser String als Ergebnis geliefert,
     * wenn nicht wie oben beschrieben mit "false" oder "0" abgebrochen wurde. Falls
     * result null ist, so wird falls alle compareTos 0 geliefert haben "true"
     * geliefert. Falls mindestens ein compareTo -1 geliefert hat wird "-1"
     * zurückgeliefert. Falls mindestens ein compareTo +1 geliefert hat wird "1"
     * geliefert. Der Fall, dass ein compareTo -1 und ein anderer +1 geliefert hat
     * wird wie bereits oben beschrieben schon früher abgefangen.
     * 
     * @throws ConfigurationErrorException
     *           falls nicht mindestens 2 Unterfunktionen in conf enthalten sind.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public NumberCompareFunction(int cmp1, int cmp2, String result,
        ConfigThingy conf, FunctionLibrary funcLib, DialogLibrary dialogLib,
        Map<Object, Object> context) throws ConfigurationErrorException
    {
      super(conf, funcLib, dialogLib, context);
      if (subFunction.size() < 2)
        throw new ConfigurationErrorException(L.m(
          "Funktion %1 erfordert mindestens 2 Parameter", conf.getName()));
      this.cmp1 = cmp1;
      this.cmp2 = cmp2;
      this.result = result;
    }

    protected boolean handleParam(ConfigThingy conf, FunctionLibrary funcLib,
        DialogLibrary dialogLib, Map<Object, Object> context)
        throws ConfigurationErrorException
    {
      if (conf.getName().equals("MARGIN"))
      {
        if (conf.count() != 1)
          throw new ConfigurationErrorException(
            L.m("MARGIN muss genau eine Funktion enthalten"));
        marginFun = parseChildren(conf, funcLib, dialogLib, context);
        return true;
      }
      else
        return false;
    }

    protected String[] getAdditionalParams()
    {
      if (marginFun != null)
        return marginFun.parameters();
      else
        return null;
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {
      super.getFunctionDialogReferences(set);
      if (marginFun != null) marginFun.getFunctionDialogReferences(set);
    }

    protected String initComputation(Values parameters)
    {
      compare = null;
      prevCompare = 0;
      if (marginFun != null)
      {
        String str = marginFun.getString(parameters);
        if (str == Function.ERROR) return Function.ERROR;
        try
        {
          margin = makeBigDecimal(str);
          margin = margin.abs();
        }
        catch (Exception x)
        {
          return Function.ERROR;
        }
      }
      else
        margin = BigDecimal.ZERO;

      return null;
    }

    protected String addToComputation(BigDecimal num)
    {
      if (compare == null)
      {
        compare = num;
        lowBound = compare.subtract(margin);
        highBound = compare.add(margin);
        return null;
      }

      int res;
      if (lowBound.compareTo(num) <= 0 && num.compareTo(highBound) <= 0)
        res = 0;
      else
        res = compare.compareTo(num);

      if (res == cmp1 || res == cmp2) return "false";
      if (res * prevCompare < 0) return "0";
      prevCompare += res;

      return null;
    }

    protected String computationResult()
    {
      if (result != null) return result;
      switch (Integer.signum(prevCompare))
      {
        case 1:
          return "1";
        case -1:
          return "-1";
        default:
          return "true";
      }
    }
  }

  private static class IfFunction implements Function
  {
    private Function ifFunction;

    private Function thenFunction;

    private Function elseFunction;

    private String[] params;

    public IfFunction(Function ifFunction, Function thenFunction,
        Function elseFunction)
    {
      Set<String> myparams = new HashSet<String>();
      myparams.addAll(Arrays.asList(ifFunction.parameters()));
      myparams.addAll(Arrays.asList(thenFunction.parameters()));
      myparams.addAll(Arrays.asList(elseFunction.parameters()));
      params = myparams.toArray(new String[0]);
      this.ifFunction = ifFunction;
      this.thenFunction = thenFunction;
      this.elseFunction = elseFunction;
    }

    public String[] parameters()
    {
      return params;
    }

    public void getFunctionDialogReferences(Collection<String> set)
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
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    public DivideFunction(Function dividendFunction, Function divisorFunction,
        int minScale, int maxScale)
    {
      Set<String> myparams = new HashSet<String>();
      myparams.addAll(Arrays.asList(dividendFunction.parameters()));
      if (divisorFunction != null)
        myparams.addAll(Arrays.asList(divisorFunction.parameters()));
      params = myparams.toArray(new String[0]);
      this.dividendFunction = dividendFunction;
      this.divisorFunction = divisorFunction;
      this.minScale = minScale;
      this.maxScale = maxScale;
    }

    public String[] parameters()
    {
      return params;
    }

    public void getFunctionDialogReferences(Collection<String> set)
    {
      dividendFunction.getFunctionDialogReferences(set);
      if (divisorFunction != null) divisorFunction.getFunctionDialogReferences(set);
    }

    public String getString(Values parameters)
    { // TESTED
      char decimalPoint = '.';
      try
      {
        decimalPoint =
          ((DecimalFormat) NumberFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
      }
      catch (Exception x)
      {}
      ;

      String dividend = dividendFunction.getString(parameters);
      if (dividend == Function.ERROR) return Function.ERROR;

      String divisor = "1";
      if (divisorFunction != null) divisor = divisorFunction.getString(parameters);
      if (divisor == Function.ERROR) return Function.ERROR;

      /*
       * Falls der Dezimaltrenner nicht '.' ist, ersetzte alle '.' durch etwas, das
       * kein Dezimaltrenner ist, um eine NumberFormatException beim Konvertieren zu
       * provozieren. Dies ist eine Vorsichtsmaßnahme, da '.' zum Beispiel in
       * Deutschland alls Gruppierungszeichen verwendet wird und wir wollen nicht
       * fälschlicher weise "100.000" als 100 interpretieren, wenn die eingebende
       * Person 100000 gemeint hat.
       */
      if (decimalPoint != '.')
      {
        dividend = dividend.replace('.', 'ß');
        divisor = divisor.replace('.', 'ß');
      }

      BigDecimal bigResult;
      try
      {
        BigDecimal bigDividend = new BigDecimal(dividend.replace(decimalPoint, '.'));
        BigDecimal bigDivisor = new BigDecimal(divisor.replace(decimalPoint, '.'));

        bigResult = bigDividend.divide(bigDivisor, maxScale, RoundingMode.HALF_UP);
      }
      catch (Exception x)
      {
        return Function.ERROR;
      }

      /*
       * NumberFormat kann leider nicht zum formatieren verwendet werden, da es nur
       * die Genauigkeit von double hat (laut Java Doc).
       */

      /*
       * Workaround für Bug
       * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6480539
       * stripTrailingZeros() funktioniert nicht für 0.
       */
      String result;
      if (bigResult.compareTo(BigDecimal.ZERO) == 0)
        result = "0";
      else
        result = bigResult.stripTrailingZeros().toPlainString();

      StringBuilder buffy = new StringBuilder(result);
      int idx = result.indexOf('.');
      if (idx == 0)
      {
        buffy.insert(0, "0");
        idx = 1;
      }
      if (idx < 0 && minScale > 0)
      {
        buffy.append(".0");
        idx = buffy.length() - 2;
      }

      int decimalDigits = (idx < 0) ? 0 : buffy.length() - idx - 1;
      for (int i = decimalDigits; i < minScale; ++i)
        buffy.append('0');

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

    public void getFunctionDialogReferences(Collection<String> set)
    {}

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
    System.out.println("\nFunktion: " + funcStr);
    System.out.print("Parameter: ");
    String[] args = func.parameters();
    for (int i = 0; i < args.length; ++i)
    {
      System.out.print((i == 0 ? "" : ", ") + args[i] + "=");
      if (vals.hasValue(args[i]))
        System.out.print("\"" + vals.getString(args[i]) + "\"" + "("
          + vals.getBoolean(args[i]) + ")");
      else
        System.out.print("n/a");
    }
    System.out.println();
    System.out.println("Funktionsergebnis: \"" + func.getString(vals) + "\"("
      + func.getBoolean(vals) + ")");
  }

  public static void main(String[] args) throws Exception
  {
    UNO.init();

    Map<Object, Object> context = new HashMap<Object, Object>();
    FunctionLibrary funcLib = new FunctionLibrary();
    DialogLibrary dialogLib = new DialogLibrary();

    dialogLib.add("Empfaenger", new Dialog()
    {
      public Dialog instanceFor(Map<Object, Object> context)
      {
        return this;
      }

      public Object getData(String id)
      {
        if (id.equals("Strasse")) return "Herzog-Wilhelm-Str. 22";
        return null;
      }

      public void show(ActionListener dialogEndListener, FunctionLibrary funcLib,
          DialogLibrary dialogLib)
      {}

      public Collection<String> getSchema()
      {
        return new Vector<String>(0);
      }
    });

    printFunction("alwaysTrueFunction()", alwaysTrueFunction(), noValues);

    String funcStr = "BAR('true' 'false' 'true')";
    ConfigThingy funcThingy =
      new ConfigThingy("FOO", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseGrandchildren(funcThingy, funcLib, dialogLib,
      context), noValues);

    funcStr = "BAR('true' 'true' 'true')";
    funcThingy =
      new ConfigThingy("FOO", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseGrandchildren(funcThingy, funcLib, dialogLib,
      context), noValues);

    funcStr = "AND('true' 'false' 'true')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      noValues);

    funcStr = "OR('true' 'false' 'true')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      noValues);

    Values.SimpleMap values = new Values.SimpleMap();
    values.put("Name", "WollMux");
    values.put("Funktion", "BKS");
    values.put("LegtEier", "true");
    values.put("GibtMilch", "false");
    values.put("Anrede", "Herr");
    values.put("TextWeibl", "(weibl.)");
    values.put("TextMaennl", "(männl.)");

    funcStr = "OR(VALUE('Fehler') VALUE('LegtEier') VALUE('GibtMilch'))";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "AND(VALUE('LegtEier') VALUE('Fehler'))";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "NOT(VALUE('GibtMilch'))";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "MATCH(VALUE('Name'),'llMux')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "REPLACE(CAT(VALUE('Name') '%n' 'Mux'),'Mux\\p{Space}Mux', 'Max')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "MATCH(VALUE('Name'),'.*llMux.*')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr =
      "EXTERN(URL 'vnd.sun.star.script:WollMux.Trafo.MannOderFrau?language=Basic&location=application' PARAMS('Anrede', 'TextWeibl', 'TextMaennl'))";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr =
      "EXTERN(URL 'java:de.muenchen.allg.itd51.wollmux.func.Standard.herrFrauText' PARAMS('Anrede', 'TextWeibl', 'TextMaennl'))";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    Function func = parseChildren(funcThingy, funcLib, dialogLib, context);
    funcLib.add("AnredeText", func);
    values.put("Anrede", "Frau");
    printFunction(funcStr, func, values);

    funcStr =
      "BIND( FUNCTION('AnredeText') SET('TextWeibl' 'die') SET('TextMaennl' 'der' ) SET('Anrede' VALUE('SGAnrede')))";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    values.put("SGAnrede", "Herr");
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr =
      "BIND( FUNCTION('AnredeText') SET('TextWeibl' 'die') SET('TextMaennl' 'der' ) SET('Anrede' VALUE('Fehler')))";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "CAT('1' '2' '3')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "CAT('1' VALUE('Fehler') '3')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SELECT('' '1' VALUE('Fehler') '3')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SELECT('' VALUE('Fehler') '3')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SELECT('' '')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "IF(THEN 'then' VALUE('Fehler') ELSE 'else')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "IF(THEN 'then' VALUE('LegtEier') ELSE 'else')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "IF(VALUE('GibtMilch') THEN 'then'  ELSE 'else')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "IF(VALUE('GibtMilch') THEN 'then')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "IF(VALUE('LegtEier') ELSE 'else')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "DIALOG('Empfaenger', 'Strasse')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "DIALOG('Empfaenger', 'Fehler')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "AND()";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SPLIT('a##b#c', '#+', '0')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SPLIT('a##b#c', '#+', '1')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SPLIT('a##b#c', '#+', '2')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SPLIT('a##b#c', '#+', '3')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    funcStr = "SPLIT('a#b#c', '#+', '4')";
    funcThingy =
      new ConfigThingy("BAR", new URL("file:///"), new StringReader(funcStr));
    printFunction(funcStr, parseChildren(funcThingy, funcLib, dialogLib, context),
      values);

    System.exit(0);
  }

}
