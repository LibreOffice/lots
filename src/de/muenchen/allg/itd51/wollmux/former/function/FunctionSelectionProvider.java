/*
 * Dateiname: FunctionSelectionProvider.java
 * Projekt  : WollMux
 * Funktion : Liefert {@link FunctionSelection}s.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
 * 25.09.2006 | BNK | Erstellung
 * 16.11.2006 | BNK | null funclib unterstützt
 * 01.10.2010 | ERT | fehlende null-Prüfung in getFunctionSelection
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.function;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.former.IDManager;

/**
 * Liefert {@link FunctionSelection}s.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FunctionSelectionProvider
{
  /**
   * Eine Funktionsbibliothek, die Funktionen liefert zu denen der
   * FunctionSelectionProvider FunctionSelections produzieren kann.
   */
  private FunctionLibrary funcLib;

  /**
   * Bildet Funktionsnamen auf dazugehörige FunctionSelections ab.
   */
  private Map<String, FunctionSelection> mapNameToFunctionSelection;

  /**
   * Der {@link IDManager}, der die IDs von Feldreferenzen verwaltet.
   */
  private IDManager idManager;

  /**
   * Der Namensraum von {@link #idManager} aus dem die IDs für Feldreferenzen kommen.
   */
  private Object namespace;

  /**
   * Erzeugt einen FunctionSelectionProvider, der {@link FunctionSelection}s sowohl
   * zu Funktionen aus funcLib (darf null sein) als auch zu Funktionen, die funConf
   * (welches ein legaler "Funktionen"-Abschnitt eines Formulars sein muss) definiert
   * liefern kann. Bei gleichem Namen haben Funktionen aus funConf vorrang vor
   * solchen aus funcLib.
   * 
   * @param idManager
   *          der IDManager, dessen IDs für Feldreferenzen (VALUE "&lt;id>")verwendet
   *          werden sollen.
   * @param namespace
   *          der Namensraum von idManager, aus dem die IDs für Feldreferenzen
   *          kommen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelectionProvider(FunctionLibrary funcLib, ConfigThingy funConf,
      IDManager idManager, Object namespace)
  {
    this.funcLib = funcLib;
    this.idManager = idManager;
    this.namespace = namespace;
    mapNameToFunctionSelection = new HashMap<String, FunctionSelection>();
    Iterator<ConfigThingy> iter = funConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy conf = iter.next();
      FunctionSelection funcSel = getFunctionSelection(conf);
      mapNameToFunctionSelection.put(conf.getName(), funcSel);
    }
  }

  /**
   * Liefert eine FunctionSelection zur Funktion mit Name funcName. Falls funcName
   * diesem FunctionSelectionProvider nicht bekannt ist, wird trotzdem eine
   * FunctionSelection geliefert, falls vorhanden mit den Parameterinformationen aus
   * der {@link FunctionLibrary}, die dem Konstruktor übergeben wurde ansonsten mit
   * einer leeren Parameterliste. Die gelieferte FunctionSelection ist auf jeden Fall
   * neu erstellt und unabhängig von anderen für den selben Namen früher gelieferten
   * FunctionSelections.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelection getFunctionSelection(String funcName)
  {
    FunctionSelection funcSel = mapNameToFunctionSelection.get(funcName);
    if (funcSel != null)
      return new FunctionSelection(funcSel);
    else
    {
      funcSel = new FunctionSelection();
      Function func = null;
      if (funcLib != null) func = funcLib.get(funcName);
      if (func != null)
        funcSel.setFunction(funcName, func.parameters());
      else
        funcSel.setFunction(funcName, new String[] {});
      return funcSel;
    }
  }

  /**
   * Liefert zu einer Funktionsdefinition in conf eine FunctionSelection. conf muss
   * einen beliebigen Wurzelknoten haben, der noch keine Grundfunktion ist (z.B.
   * "PLAUSI" oder "AUTOFILL").
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelection getFunctionSelection(ConfigThingy conf)
  {
    return getFunctionSelection(conf, null);
  }

  /**
   * Liefert zu einer Funktionsdefinition in conf eine FunctionSelection. conf muss
   * einen beliebigen Wurzelknoten haben, der noch keine Grundfunktion ist (z.B.
   * "PLAUSI" oder "AUTOFILL").
   * 
   * @param defaultValue
   *          falls nicht null, so werden bei einer Funktionsreferenz an VALUE
   *          "&lt;defaultValue>" gebundene Parameter auf unspecified gesetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public FunctionSelection getFunctionSelection(ConfigThingy conf,
      String defaultValue)
  {
    FunctionSelection funcSel = new FunctionSelection();

    if (conf.count() == 0) return funcSel;

    funcSel.setExpertFunction(conf);

    if (conf.count() != 1) return funcSel;

    conf = conf.iterator().next();

    if (!conf.getName().equals("BIND")) return funcSel;

    /**
     * Wir haben es mit einem einzelnen BIND zu tun. Wir versuchen, diesen zu parsen.
     * Wenn wir das nicht können, dann liefern wir das ganze als Expert-Funktion
     * zurück, ansonsten setzen wir das BIND entsprechend um.
     */
    Map<String, ParamValue> mapNameToParamValue = new HashMap<String, ParamValue>();
    String funcName = null;
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy childConf = iter.next();
      String name = childConf.getName();
      if (name.equals("FUNCTION"))
      {
        if (childConf.count() != 1) return funcSel;
        if (childConf.iterator().next().count() != 0) return funcSel;
        funcName = childConf.toString();
      }
      else if (name.equals("SET"))
      {
        if (childConf.count() != 2) return funcSel;
        String paramName = null;
        ConfigThingy paramConf = null;
        try
        {
          paramName = childConf.getFirstChild().toString();
          paramConf = childConf.getLastChild();
        }
        catch (NodeNotFoundException x)
        {
          // Kann nicht passieren, weil childConf.count() getestet wurde. Ist nur
          // hier
          // um Compiler-Warnungen und findBugs zu besänftigen
          throw new RuntimeException(x);
        }

        ParamValue paramValue;

        if (paramConf.count() == 0)
          paramValue = ParamValue.literal(paramConf.toString());
        else if (paramConf.count() == 1 && paramConf.getName().equals("VALUE"))
        {
          String valueName = paramConf.toString();
          if (valueName.equals(defaultValue))
            paramValue = ParamValue.unspecified();
          else
            paramValue = ParamValue.field(idManager.getID(namespace, valueName));
        }
        else
          return funcSel;

        mapNameToParamValue.put(paramName, paramValue);

      }
      else
        return funcSel;
    }

    if (funcName == null) return funcSel;

    Function func = null;

    if (funcLib != null) func = funcLib.get(funcName);

    if (func != null)
    {
      funcSel.setFunction(funcName, func.parameters());
    }
    else
    {
      funcSel.setFunction(funcName, mapNameToParamValue.keySet().toArray(
        new String[] {}));
    }

    funcSel.setParameterValues(mapNameToParamValue);

    return funcSel;
  }
}
