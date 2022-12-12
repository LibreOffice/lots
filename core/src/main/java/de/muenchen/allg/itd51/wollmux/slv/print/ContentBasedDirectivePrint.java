/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv.print;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.ui.dialogs.ExecutableDialogResults;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.func.print.PrintException;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveDialog;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveSettings;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Print function for configuration of the content based directive print. It creates a GUI and
 * passes the settings as a property called {@link #PROP_SLV_SETTINGS} of the {@link XPrintModel} to
 * the next {@link PrintFunction}.
 */
public class ContentBasedDirectivePrint extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentBasedDirectivePrint.class);

  /**
   * Key for saving the content based directive settings as a property of a {@link XPrintModel}.
   * This property is read by {@link ContentBasedDirectivePrintOutput}.
   *
   * The property type is a {@link List} of {@link ContentBasedDirectiveSettings}.
   */
  public static final String PROP_SLV_SETTINGS = "SLV_Settings";

  /**
   * Key for saving the created prints as a property of a {@link XPrintModel}. This property is
   * written by {@link ContentBasedDirectivePrintCollect} and is read by
   * {@link ContentBasedDirectivePrint}.
   *
   * The property type is a {@link List} of {@link File}.
   */
  public static final String PROP_SLV_COLLECT = "SLV_Collect";

  /**
   * The name of this {@link PrintFunction}.
   */
  public static final String PRINT_FUNCTION_NAME = "SachleitendeVerfuegung";

  private boolean collect = false;

  /**
   * A {@link PrintFunction} with name "SachleitendeVerfuegung" and order 50.
   */
  public ContentBasedDirectivePrint()
  {
    super(PRINT_FUNCTION_NAME, 50);
  }

  @Override
  public void print(XPrintModel printModel) throws PrintException
  {
    // add print function SachleitendeVerfuegungOutput
    ContentBasedDirectiveModel model = ContentBasedDirectiveModel
        .createModel(DocumentManager.getTextDocumentController(printModel.getTextDocument()));
    boolean hasItems = model.adoptNumbers();
    List<ContentBasedDirectiveSettings> settings = callPrintDialog(model);
    if (hasItems)
    {
      try
      {
        printModel.usePrintFunction("SachleitendeVerfuegungOutput");
        if (collect)
        {
          printModel.usePrintFunction("SachleitendeVerfuegungCollect");
        }
      } catch (NoSuchMethodException e)
      {
        LOGGER.error("", e);
        printModel.cancel();
        return;
      }
    }
    if (settings != null && !settings.isEmpty())
    {
      try
      {
        printModel.setPropertyValue(PROP_SLV_SETTINGS, settings);
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
        printModel.cancel();
        return;
      }
      printModel.printWithProps();

      // Handle Collection
      if (collect)
      {
        collectPrintsAndShowResult(printModel);
      }
      model.adoptNumbers();
    }
  }

  private void collectPrintsAndShowResult(XPrintModel printModel) throws PrintException
  {
    try
    {
      @SuppressWarnings("unchecked")
      List<File> collection = (List<File>) printModel.getProp(ContentBasedDirectivePrint.PROP_SLV_COLLECT,
          new ArrayList<File>());
      if (collection.isEmpty())
      {
        return;
      }
      PDFMergerUtility merger = new PDFMergerUtility();
      for (File doc : collection)
      {
        LOGGER.debug(doc.getAbsolutePath());
        merger.addSource(doc);
      }
      File outputFile = Files.createTempFile("WollMux_SLV_", ".pdf").toFile();
      merger.setDestinationFileName(outputFile.getAbsolutePath());
      merger.mergeDocuments(null);
      printModel.setPropertyValue(PrintFunction.PRINT_RESULT_FILE, outputFile);
      PrintFunction showFileFunc = GlobalFunctions.getInstance().getGlobalPrintFunctions().get("ShowDocument");
      if (showFileFunc != null)
      {
        showFileFunc.print(printModel);
      }
    } catch (IOException | IllegalArgumentException | UnknownPropertyException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error(L.m("Could not merge PDF documents."), e);
      InfoDialog.showInfoModal(L.m("Print content based directives"),
          L.m("Could not merge PDF documents."));
    }
  }

  /**
   * Show the print dialog for content based directives.
   *
   * @param model
   *          The content based directive model of the document to print.
   *
   * @return A list of settings for each content based directive item.
   */
  private List<ContentBasedDirectiveSettings> callPrintDialog(ContentBasedDirectiveModel model)
  {
    // update document structure (commands, text sections)
    model.getDocumentController().updateDocumentCommands();
    List<ContentBasedDirective> directives = model.scanItems();
    List<ContentBasedDirectiveSettings> settings = new ArrayList<>();

    // call dialog and wait for return.
    try
    {
      ContentBasedDirectiveDialog dialog = new ContentBasedDirectiveDialog(directives);
      short result = dialog.execute();

      if (ExecutableDialogResults.OK == result)
      {
        settings = dialog.getSettings();
        collect = dialog.isCollect();

        if (settings == null || settings.isEmpty())
        {
          LOGGER.debug("Sachleitende Verfuegung: callPrintDialog(): VerfuegungspunktInfos NULL or empty.");
          settings = new ArrayList<>();
        }

        if (!dialog.getPrintOrderAsc())
        {
          // print in descending order.
          List<ContentBasedDirectiveSettings> descSettings = new ArrayList<>();
          ListIterator<ContentBasedDirectiveSettings> lIt = settings.listIterator(settings.size());

          while (lIt.hasPrevious())
          {
            descSettings.add(lIt.previous());
          }
          settings = descSettings;
        }
      }
    } catch (ConfigurationErrorException e)
    {
      LOGGER.error("", e);
    }
    return settings;
  }

  @Override
  public boolean equals(Object obj)
  {
    boolean equal = super.equals(obj);
    if (!equal)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    ContentBasedDirectivePrint other = (ContentBasedDirectivePrint) obj;
    return collect == other.collect;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(super.hashCode(), collect);
  }
}
