/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.dialog.DialogFactory;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibrary;

/**
 * Collection of all globally defined functions.
 */
public class GlobalFunctions
{

  private static GlobalFunctions instance;

  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private FunctionLibrary globalFunctions;
  /**
   * Enthält die im Dokumentaktionen der wollmux,conf definierten Funktionen.
   */
  private FunctionLibrary documentActionFunctions;
  /**
   * Enthält die im Funktionsdialoge-Abschnitt der wollmux,conf definierten Dialoge.
   */
  private DialogLibrary funcDialogs;
  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private PrintFunctionLibrary globalPrintFunctions;

  public static GlobalFunctions getInstance()
  {
    if (instance == null)
      instance = new GlobalFunctions();

    return instance;
  }

  private GlobalFunctions()
  {
    /*
     * Globale Funktionsdialoge parsen. ACHTUNG! Muss vor parseGlobalFunctions() erfolgen. Als
     * context wird null übergeben, weil globale Funktionen keinen Kontext haben.
     */
    funcDialogs =
      DialogFactory.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, null);

    /*
     * Globale Funktionen parsen. ACHTUNG! Verwendet die Funktionsdialoge. Diese
     * müssen also vorher geparst sein. Als context wird null übergeben, weil globale
     * Funktionen keinen Kontext haben.
     */
    globalFunctions =
      FunctionFactory.parseFunctions(WollMuxFiles.getWollmuxConf(),
        getFunctionDialogs(), null, null);

    /*
     * Globale Druckfunktionen parsen.
     */
    globalPrintFunctions =
      PrintFunctionLibrary.parsePrintFunctions(WollMuxFiles.getWollmuxConf());
    PrintFunction.addPrintFunctions(globalPrintFunctions);

    /*
     * Dokumentaktionen parsen. Diese haben weder Kontext noch Dialoge.
     */
    documentActionFunctions = new FunctionLibrary(null, true);
    FunctionFactory.parseFunctions(documentActionFunctions,
      WollMuxFiles.getWollmuxConf(), "Dokumentaktionen", null, null);
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Funktionen enthält.
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   * Liefert die Funktionsbibliothek, die die Dokumentaktionen enthält.
   */
  public FunctionLibrary getDocumentActionFunctions()
  {
    return documentActionFunctions;
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Druckfunktionen
   * enthält.
   */
  public PrintFunctionLibrary getGlobalPrintFunctions()
  {
    return globalPrintFunctions;
  }

  /**
   * Liefert die Dialogbibliothek, die die Dialoge enthält, die in Funktionen
   * (Grundfunktion "DIALOG") verwendung finden.
   */
  public DialogLibrary getFunctionDialogs()
  {
    return funcDialogs;
  }

}
