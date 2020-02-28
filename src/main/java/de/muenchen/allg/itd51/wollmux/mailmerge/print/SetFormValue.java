package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResultsList;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.func.print.PrintException;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

/**
 * A print function, which sets the mailmerge fields to the values of the next mailmerge data.
 */
public class SetFormValue extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SetFormValue.class);

  /**
   * Tag replaced by the mailmerge number.
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
   * Key for saving the schema of the datasource as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link Collection} of Strings.
   */
  public static final String PROP_SCHEMA = "MailMergeNew_Schema";

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

    QueryResults data = (QueryResults) pmod.getProp(PROP_QUERYRESULTS,
        new QueryResultsList(Collections.emptyList()));
    @SuppressWarnings("unchecked")
    Collection<String> schema = (Collection<String>) pmod.getProp(PROP_SCHEMA,
        Collections.emptySet());
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
    for (Map.Entry<Integer, Dataset> record : getSelectedDatasets(data, selection).entrySet())
    {
      if (pmod.isCanceled())
      {
        return;
      }

      if (simProc != null)
      {
        documentController.startSimulation();
      }

      for (String column : schema)
      {
        try
        {
          String value = record.getValue().get(column);
          pmod.setFormValue(column, value);
          dataSetExport.put(column, value);
        } catch (ColumnNotFoundException e)
        {
          LOGGER.trace("Should not happen, because we iterate over all columns", e);
        }
      }
      pmod.setFormValue(TAG_RECORD_ID, "" + record.getKey());
      dataSetExport.put(TAG_RECORD_ID, "" + record.getKey());
      pmod.setFormValue(TAG_MAILMERGE_ID, "" + mailMergeNumber);
      dataSetExport.put(TAG_MAILMERGE_ID, "" + mailMergeNumber);

      // Pass to next print function, if there is no simProc. Otherwise processing is done by
      // simProc.
      if (simProc == null)
        pmod.printWithProps();
      else
        simProc.processSimulationResults(documentController.stopSimulation());

      pmod.setPrintProgressValue((short) mailMergeNumber);
      ++mailMergeNumber;
    }
  }

  /**
   * Collects the selected data records.
   *
   * @param data
   *          All possible data records.
   * @param selection
   *          List of indices to select the data.
   * @return A map of the selected data. Key is the index, value is the data.
   */
  private static Map<Integer, Dataset> getSelectedDatasets(QueryResults data,
      List<Integer> selection)
  {
    Map<Integer, Dataset> selected = new TreeMap<>();
    Iterator<Dataset> iter = data.iterator();
    Iterator<Integer> selIter = selection.iterator();
    int selectedIdx = selIter.next();
    int index = -1;
    while (iter.hasNext() && selectedIdx >= 0)
    {
      if (++index < selectedIdx)
      {
        continue;
      }

      Dataset ds = iter.next();
      int datensatzNummer = index + 1;
      selected.put(datensatzNummer, ds);

      if (selIter.hasNext())
        selectedIdx = selIter.next();
      else
        selectedIdx = -1;
    }
    return selected;
  }

}
