package de.muenchen.allg.itd51.wollmux.slv.print;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.ucb.XFileIdentifierConverter;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.SyncActionListener;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveDialog;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveSettings;
import de.muenchen.allg.util.UnoComponent;

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
  public void print(XPrintModel printModel)
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
        collectPrints(printModel);
      }
    }
  }

  private void collectPrints(XPrintModel printModel)
  {
    try
    {
      @SuppressWarnings("unchecked")
      List<File> collection = (List<File>) printModel.getProp(ContentBasedDirectivePrint.PROP_SLV_COLLECT,
          new ArrayList<File>());
      PDFMergerUtility merger = new PDFMergerUtility();
      for (File doc : collection)
      {
        LOGGER.debug(doc.getAbsolutePath());
        merger.addSource(doc);
      }

      XFilePicker3 picker = FilePicker.createWithMode(UNO.defaultContext, TemplateDescription.FILESAVE_AUTOEXTENSION);
      String filterName = "PDF Dokument";
      picker.appendFilter(filterName, "*.pdf");
      picker.appendFilter("Alle Dateien", "*");
      picker.setCurrentFilter(filterName);
      short res = picker.execute();
      if (res == com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
      {
        String[] files = picker.getFiles();
        XFileIdentifierConverter xFileConverter = UNO.XFileIdentifierConverter(
            UnoComponent.createComponentWithContext(UnoComponent.CSS_UCB_FILE_CONTENT_PROVIDER));
        String outputFile = xFileConverter.getSystemPathFromFileURL(files[0]);
        merger.setDestinationFileName(outputFile);
        merger.mergeDocuments(null);
        Desktop.getDesktop().open(new File(outputFile));
      } else
      {
        InfoDialog.showInfoModal("Sachleitende Verfügungen drucken",
            "PDF Dokument mit allen Ausdrucken wurde nicht gespeichert.");
      }
    } catch (IOException e)
    {
      LOGGER.error("PDF Dokumente konnten nicht zusammengefügt werden.", e);
      InfoDialog.showInfoModal("Sachleitende Verfügungen drucken",
          "PDF Dokumente konnten nicht zusammengefügt werden.");
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
      SyncActionListener s = new SyncActionListener();
      new ContentBasedDirectiveDialog(directives, s);
      ActionEvent result = s.synchronize();

      if (ContentBasedDirectiveDialog.CMD_SUBMIT.equals(result.getActionCommand()))
      {
        ContentBasedDirectiveDialog dialog = (ContentBasedDirectiveDialog) result.getSource();
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
