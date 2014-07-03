package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.mailmerge.NoTableSelectedException;
import de.muenchen.allg.itd51.wollmux.mailmerge.printsettings.MailmergeWizardController.PATH;
import de.muenchen.allg.itd51.wollmux.mailmerge.printsettings.PrintSettings.ACTION;
import de.muenchen.allg.itd51.wollmux.mailmerge.printsettings.PrintSettings.DatasetSelectionType;

/**
 * The first page of the mail merge wizard. Some basic settings can be done here.
 */
public class StartWizardPage extends AbstractXWizardPage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(StartWizardPage.class);

  private final XRadioButton singleDocument;
  private final XRadioButton singleDocumentPDF;
  private final XRadioButton direct;
  private final XRadioButton mails;
  private final XRadioButton multipleDocuments;

  private MailmergeWizardController controller;

  private PrintSettings settings;

  private final XRadioButton all;
  private final XRadioButton range;
  private final XNumericField from;
  private final XNumericField till;

  private class ActionItemListener implements AbstractItemListener
  {

    private PATH path;

    public ActionItemListener(PATH path)
    {
      this.path = path;
    }

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      controller.changePath(path);
      controller.updateTravelUI();
    }
  }

  /**
   * Creates the first page.
   *
   * @param parentWindow
   *          The containing window.
   * @param pageId
   *          The id of this page.
   * @param controller
   *          The controller of the wizard.
   * @param settings
   *          The print settings.
   * @throws Exception
   *           If the page can't be created.
   */
  public StartWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller,
      PrintSettings settings) throws Exception
  {
    super(pageId, parentWindow, "vnd.sun.star.script:WollMux.seriendruck_start?location=application");
    this.controller = controller;
    this.settings = settings;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    singleDocument = UNO.XRadio(container.getControl("gesamtDoc"));
    singleDocument.addItemListener(new ActionItemListener(PATH.STANDARD));
    singleDocumentPDF = UNO.XRadio(container.getControl("gesamtDocPDF"));
    singleDocumentPDF.addItemListener(new ActionItemListener(PATH.STANDARD));
    direct = UNO.XRadio(container.getControl("drucken"));
    direct.addItemListener(new ActionItemListener(PATH.DIRECT_PRINT));
    mails = UNO.XRadio(container.getControl("emails"));
    mails.addItemListener(new ActionItemListener(PATH.MAIL));
    multipleDocuments = UNO.XRadio(container.getControl("einzel"));
    multipleDocuments.addItemListener(new ActionItemListener(PATH.SINGLE_FILES));
    from = UNO.XNumericField(container.getControl("from"));
    from.setValue(1);
    UNO.XWindow(from).setEnable(false);
    till = UNO.XNumericField(container.getControl("till"));
    int datasets = 0;
    try
    {

      datasets = controller.getModel().getNumberOfRecords();
    } catch (NoTableSelectedException ex)
    {
      // nothing to do
    }
    from.setMax(datasets);
    till.setValue(datasets);
    till.setMax(datasets);

    UNO.XWindow(till).setEnable(false);
    all = UNO.XRadio(container.getControl("selectAll"));
    all.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent arg0)
      {
        UNO.XWindow(from).setEnable(false);
        UNO.XWindow(till).setEnable(false);
        controller.updateTravelUI();
      }
    });
    range = UNO.XRadio(container.getControl("selectRange"));
    range.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent arg0)
      {
        UNO.XWindow(from).setEnable(true);
        UNO.XWindow(till).setEnable(true);
        controller.updateTravelUI();
      }
    });
  }

  @Override
  public boolean canAdvance()
  {
    ACTION action = getSelectedAction();
    return action != ACTION.NOTHING && getSelectedRange() != DatasetSelectionType.NOTHING;
  }

  @Override
  public boolean commitPage(short reason)
  {
    ACTION action = getSelectedAction();
    DatasetSelectionType rangeValue = getSelectedRange();
    switch (rangeValue)
    {
    case ALL:
      settings.setRangeStart(1);
      settings.setRangeEnd(Integer.MAX_VALUE);
      break;
    case RANGE:
      settings.setRangeStart((int) from.getValue());
      settings.setRangeEnd((int) till.getValue());
      break;
    default:
      break;
    }

    LOGGER.debug("Aktion {}, Range {}", action, rangeValue);
    window.setVisible(false);
    settings.setAction(getSelectedAction());
    settings.setSelection(getSelectedRange());
    return true;
  }

  private ACTION getSelectedAction()
  {
    if (singleDocument.getState())
    {
      return ACTION.SINGLE_DOCUMENT_ODT;
    }
    if (singleDocumentPDF.getState())
    {
      return ACTION.SINGLE_DOCUMENT_PDF;
    }
    if (direct.getState())
    {
      return ACTION.DIRECT;
    }
    if (mails.getState())
    {
      return ACTION.MAIL;
    }
    if (multipleDocuments.getState())
    {
      return ACTION.MULTIPLE_DOCUMENTS;
    }
    return ACTION.NOTHING;
  }

  private DatasetSelectionType getSelectedRange()
  {
    if (all.getState())
    {
      return DatasetSelectionType.ALL;
    }
    if (range.getState())
    {
      return DatasetSelectionType.RANGE;
    }
    return DatasetSelectionType.NOTHING;
  }
}
