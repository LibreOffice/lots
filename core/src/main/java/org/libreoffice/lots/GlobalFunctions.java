/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots;

import org.libreoffice.lots.dialog.DialogFactory;
import org.libreoffice.lots.dialog.DialogLibrary;
import org.libreoffice.lots.func.FunctionFactory;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.func.print.PrintFunction;
import org.libreoffice.lots.print.PrintFunctionLibrary;

/**
 * Collection of all globally defined functions.
 */
public class GlobalFunctions
{

  private static GlobalFunctions instance;

  /**
   * Contains the functions defined in the "Functions" section of lots.conf.
   */
  private FunctionLibrary globalFunctions;
  /**
   * Contains the functions defined in the lots.conf "DocumentActions" section.
   */
  private FunctionLibrary documentActionFunctions;
  /**
   * Contains the dialogs defined in the "FunctionDialogs" section of lots.conf.
   */
  private DialogLibrary funcDialogs;
  /**
   * Contains the functions defined in the "Functions" section of lots.conf.
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
     * Parse global function dialogs.
     * ATTENTION! Must be done before parseGlobalFunctions().
     * Null is passed as context, because global functions have no context.
     */
    funcDialogs =
      DialogFactory.parseFunctionDialogs(WollMuxFiles.getWollmuxConf(), null, null);

    /*
     * Parse global functions. ATTENTION! Use the function dialogs.
     * These must be parsed before. Null is passed as context,
     * because global functions have no context.
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
     * Parse document actions. These have neither context nor dialogs.
     */
    documentActionFunctions = new FunctionLibrary(null, true);
    FunctionFactory.parseFunctions(documentActionFunctions,
      WollMuxFiles.getWollmuxConf(), "DocumentActions", null, null);
  }

  /**
   * Returns the function library containing the globally defined functions.
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   *  Returns the function library that contains the document actions.
   */
  public FunctionLibrary getDocumentActionFunctions()
  {
    return documentActionFunctions;
  }

  /**
   * Returns the function library that contains the globally defined print functions.
   */
  public PrintFunctionLibrary getGlobalPrintFunctions()
  {
    return globalPrintFunctions;
  }

  /**
   * Returns the dialog library containing the dialogs that are used in functions
   * (basic function "DIALOG").
   */
  public DialogLibrary getFunctionDialogs()
  {
    return funcDialogs;
  }

}
