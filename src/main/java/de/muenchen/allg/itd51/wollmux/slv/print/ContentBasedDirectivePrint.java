package de.muenchen.allg.itd51.wollmux.slv.print;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.NoSuchMethodException;

import de.muenchen.allg.itd51.wollmux.SyncActionListener;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveModel;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveDialog;
import de.muenchen.allg.itd51.wollmux.slv.dialog.ContentBasedDirectiveSettings;

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
   * The name of this {@link PrintFunction}.
   */
  public static final String PRINT_FUNCTION_NAME = "SachleitendeVerfuegung";

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
    if (model.adoptNumbers())
    {
      try
      {
        printModel.usePrintFunction("SachleitendeVerfuegungOutput");
      } catch (NoSuchMethodException e)
      {
        LOGGER.error("", e);
        printModel.cancel();
        return;
      }
    }

    List<ContentBasedDirectiveSettings> settings = callPrintDialog(model);
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

        if (settings == null || settings.isEmpty())
        {
          LOGGER.debug(
              "Sachleitende Verfuegung: callPrintDialog(): VerfuegungspunktInfos NULL or empty.");
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
}
