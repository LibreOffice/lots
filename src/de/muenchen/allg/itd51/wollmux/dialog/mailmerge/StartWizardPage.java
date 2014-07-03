package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

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
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.ACTION;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.DatasetSelectionType;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.IndexSelection;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.SubmitArgument;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailmergeWizardController.PATH;

public class StartWizardPage extends AbstractXWizardPage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(StartWizardPage.class);

  private final XRadioButton singleDocument;
  private final XRadioButton singleDocumentPDF;
  private final XRadioButton direct;
  private final XRadioButton mails;
  private final XRadioButton multipleDocuments;

  private MailmergeWizardController controller;

  private final XRadioButton all;
  private final XRadioButton range;
  private final XNumericField from;
  private final XNumericField till;

  private IndexSelection selection = new IndexSelection();

  private class ActionItemListener implements AbstractItemListener {

    private PATH path;

    public ActionItemListener(PATH path)
    {
      this.path = path;
    }

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      controller.changePath(path);
      controller.activateNextButton(canAdvance());
    }
  }

  public StartWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller) throws Exception
  {
    super(pageId, parentWindow, "seriendruck_start");
    this.controller = controller;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    singleDocument = UNO.XRadio(container.getControl("gesamtDoc"));
    singleDocument.addItemListener(new ActionItemListener(PATH.STANDRAD));
    singleDocumentPDF = UNO.XRadio(container.getControl("gesamtDocPDF"));
    singleDocument.addItemListener(new ActionItemListener(PATH.STANDRAD));
    direct = UNO.XRadio(container.getControl("drucken"));
    direct.addItemListener(new ActionItemListener(PATH.DIRECT_PRINT));
    mails = UNO.XRadio(container.getControl("emails"));
    mails.addItemListener(new ActionItemListener(PATH.MAIL));
    multipleDocuments = UNO.XRadio(container.getControl("einzel"));
    multipleDocuments.addItemListener(new ActionItemListener(PATH.SINGLE_FILES));
    from = UNO.XNumericField(container.getControl("from"));
    from.setValue(1);
    from.setMax(controller.getController().getDs().getNumberOfDatasets());
    UNO.XWindow(from).setEnable(false);
    till = UNO.XNumericField(container.getControl("till"));
    till.setValue(controller.getController().getDs().getNumberOfDatasets());
    till.setMax(controller.getController().getDs().getNumberOfDatasets());
    UNO.XWindow(till).setEnable(false);
    all = UNO.XRadio(container.getControl("selectAll"));
    all.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent arg0)
      {
        UNO.XWindow(from).setEnable(false);
        UNO.XWindow(till).setEnable(false);
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
      }
    });
  }

  @Override
  public boolean canAdvance()
  {
    LOGGER.debug("canAdvance");
    ACTION action = getSelectedAction();
    return (action != ACTION.NOTHING || action != ACTION.SINGLE_DOCUMENT_ODT)
        && getSelectedRange() != DatasetSelectionType.NOTHING;
  }

  @Override
  public boolean commitPage(short reason)
  {
    ACTION action = getSelectedAction();
    DatasetSelectionType rangeValue = getSelectedRange();
    switch (rangeValue)
    {
      case ALL:
        selection = new IndexSelection();
        break;
      case RANGE:
        selection.rangeStart = (int) from.getValue();
        selection.rangeEnd = (int) till.getValue();
        break;
      default:
        break;
    }

    LOGGER.debug("Aktion {}, Range {}", action, rangeValue);
    window.setVisible(false);
    controller.setCurrentActionType(getSelectedAction());
    controller.setDatasetSelectionType(getSelectedRange());
    controller.arguments.put(SubmitArgument.INDEX_SELECTION, selection);
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
