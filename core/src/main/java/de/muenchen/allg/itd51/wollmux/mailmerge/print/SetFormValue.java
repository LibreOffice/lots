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
package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.func.print.PrintException;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

/**
 * A print function, which sets the mail merge fields to the values of the next mail merge data.
 */
public class SetFormValue extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SetFormValue.class);

  /**
   * Tag replaced by the mail merge number.
   */
  public static final String TAG_MAILMERGE_ID = "#SB";

  /**
   * Tag replaced by the record number.
   */
  public static final String TAG_RECORD_ID = "#DS";

  /**
   * Key for saving the records values as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link Map} of key-value pairs of type String.
   */
  public static final String PROP_DATASET_EXPORT = "MailMergeNew_DatasetExport";

  /**
   * Key for saving the selected records as a property of a {@link XPrintModel}. The id of the
   * records must be in ascending order.
   *
   * The property type is a {@link List} of Integers.
   */
  public static final String PROP_RECORD_SELECTION = "MailMergeNew_Selection";

  /**
   * Key for saving the content of the datasource as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link QueryResults}.
   */
  public static final String PROP_QUERYRESULTS = "MailMergeNew_QueryResults";

  /**
   * A {@link PrintFunction} with name "MailMergeNewSetFormValue" and order 75.
   */
  public SetFormValue()
  {
    super("MailMergeNewSetFormValue", 75);
  }

  @Override
  public void print(XPrintModel printModel) throws PrintException
  {
    try
    {
      mailMergeNewSetFormValue(printModel, null);
    } catch (Exception ex)
    {
      throw new PrintException(ex.toString(), ex);
    }
  }

  /**
   * Take next data record and set mailmerge fields to its values. Calls next {@link PrintFunction}.
   *
   * If there is a {@link SimulationResultsProcessor}, modification is only simulated and its
   * handler is called after each record instead of calling the next {@link PrintFunction}.
   *
   * @param pmod
   *          The {@link XPrintModel}.
   * @param simProc
   *          The {@link SimulationResultsProcessor}, can be null.
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod, SimulationResultsProcessor simProc)
  {
    TextDocumentController documentController = DocumentManager
        .getTextDocumentController(pmod.getTextDocument());

    @SuppressWarnings("unchecked")
    Table<Integer, String, String> data = (Table<Integer, String, String>) pmod
        .getProp(PROP_QUERYRESULTS, HashBasedTable.create());
    @SuppressWarnings("unchecked")
    List<Integer> selection = (List<Integer>) pmod.getProp(PROP_RECORD_SELECTION,
        Collections.emptyList());
    if (selection.isEmpty())
    {
      return;
    }

    pmod.setPrintProgressMaxValue((short) selection.size());

    HashMap<String, String> dataSetExport = new HashMap<>();
    try
    {
      pmod.setPropertyValue(PROP_DATASET_EXPORT, dataSetExport);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.trace("Could not set map with mailmerge data", e);
    }

    int mailMergeNumber = 1;
    documentController.setFormFieldsPreviewMode(true);
    for (int sel : selection)
    {
      if (pmod.isCanceled())
      {
        documentController.setFormFieldsPreviewMode(false);
        return;
      }

      if (simProc != null)
      {
        documentController.startSimulation();
      }

      Map<String, String> record = data.row(sel);
      for (Map.Entry<String, String> entry : record.entrySet())
      {
        pmod.setFormValue(entry.getKey(), entry.getValue());
        dataSetExport.put(entry.getKey(), entry.getValue());
      }
      pmod.setFormValue(TAG_RECORD_ID, "" + sel);
      dataSetExport.put(TAG_RECORD_ID, "" + sel);
      pmod.setFormValue(TAG_MAILMERGE_ID, "" + mailMergeNumber);
      dataSetExport.put(TAG_MAILMERGE_ID, "" + mailMergeNumber);

      // Pass to next print function, if there is no simProc. Otherwise
      // processing is done by simProc.
      if (simProc == null)
      {
        pmod.printWithProps();
      } else
      {
        simProc.processSimulationResults(documentController.stopSimulation());
      }

      pmod.setPrintProgressValue((short) mailMergeNumber);
      ++mailMergeNumber;
    }

    documentController.setFormFieldsPreviewMode(false);
  }
}
