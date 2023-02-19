/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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

import de.muenchen.allg.itd51.wollmux.dialog.DialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibrary;

/**
 * Collection of all globally defined functions.
 */
public class GlobalFunctions
{

  private static GlobalFunctions instance;

  /**
   * Contains the functions defined in the functions section of wollmux.conf.
   */
  private FunctionLibrary globalFunctions;
  /**
   * Contains the functions defined in the Wollmux.conf document actions.
   */
  private FunctionLibrary documentActionFunctions;
  /**
   * Contains the dialogs defined in the function dialogs section of wollmux,conf.
   */
  private DialogLibrary funcDialogs;
  /**
   * Contains the functions defined in the functions section of wollmux.conf.
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
     * Parse global function dialogs. DANGER! Must be done before parseGlobalFunctions(). As
     * context is passed null because global functions have no context.
     */
    funcDialogs =
      DialogFactory.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, null);

    /*
     * Parse global functions. DANGER! Uses the function dialogs. This
     * must therefore be parsed beforehand. As context, null is passed because global
     * Functions have no context.
     */
    globalFunctions =
      FunctionFactory.parseFunctions(WollMuxFiles.getWollmuxConf(),
        getFunctionDialogs(), null, null);

    /*
     * Parse global print functions.
     */
    globalPrintFunctions =
      PrintFunctionLibrary.parsePrintFunctions(WollMuxFiles.getWollmuxConf());
    PrintFunction.addPrintFunctions(globalPrintFunctions);

    /*
     * Parse document actions. These have neither context nor dialogues.
     */
    documentActionFunctions = new FunctionLibrary(null, true);
    FunctionFactory.parseFunctions(documentActionFunctions,
      WollMuxFiles.getWollmuxConf(), "Dokumentaktionen", null, null);
  }

  /**
   * Returns the function library containing the globally defined functions.
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   * Returns the function library containing the document actions.
   */
  public FunctionLibrary getDocumentActionFunctions()
  {
    return documentActionFunctions;
  }

  /**
   * Returns the function library containing the globally defined print functions
   * contains.
   */
  public PrintFunctionLibrary getGlobalPrintFunctions()
  {
    return globalPrintFunctions;
  }

  /**
   * Returns the dialog library containing the dialogs contained in functions
   * (basic function "DIALOG") can be used.
   */
  public DialogLibrary getFunctionDialogs()
  {
    return funcDialogs;
  }

}
