package de.muenchen.allg.itd51.wollmux.dialog;

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
import de.muenchen.allg.itd51.wollmux.dialog.MailmergeWizardController.PATH;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.ACTION;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType;

public class StartWizardPage extends AbstractXWizardPage
{
  
  private static final Logger LOGGER = LoggerFactory.getLogger(StartWizardPage.class);
  
  private final XRadioButton singleDocument;
  private final XRadioButton direct;
  private final XRadioButton mails;
  private final XRadioButton multipleDocuments;
  
  private MailMergeParams params;
  private MailmergeWizardController controller;
  
  private final XRadioButton all;
  private final XRadioButton range;
  private final XNumericField from;
  private final XNumericField till;
  
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
  
  public StartWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller, MailMergeParams params) throws Exception
  {
    super(pageId, parentWindow, "seriendruck_start");
    this.params = params;
    this.controller = controller;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    singleDocument = UNO.XRadio(container.getControl("gesamtDoc"));
    singleDocument.addItemListener(new ActionItemListener(PATH.STANDRAD));
    direct = UNO.XRadio(container.getControl("drucken"));
    direct.addItemListener(new ActionItemListener(PATH.DIRECT_PRINT));
    mails = UNO.XRadio(container.getControl("emails"));
    mails.addItemListener(new ActionItemListener(PATH.MAIL));
    multipleDocuments = UNO.XRadio(container.getControl("einzel"));
    multipleDocuments.addItemListener(new ActionItemListener(PATH.SINGLE_FILES));
    all = UNO.XRadio(container.getControl("selectAll"));
    range = UNO.XRadio(container.getControl("selectRange"));
    from = UNO.XNumericField(container.getControl("from"));
    till = UNO.XNumericField(container.getControl("till"));
  }

  @Override
  public boolean canAdvance()
  {
    LOGGER.debug("canAdvance");
    ACTION action = getSelectedAction();
    return (action != ACTION.NOTHING || action == ACTION.SINGLE_DOCUMENT)
        && getSelectedRange() != DatasetSelectionType.NOTHING;
  }

  @Override
  public boolean commitPage(short reason)
  {
    ACTION action = getSelectedAction();
    DatasetSelectionType rangeValue = getSelectedRange();
    double start = -1;
    double end = -1;
    switch (rangeValue)
    {
      case ALL:
        start = 0;
        end = Double.MAX_VALUE;
        break;
      case RANGE:
        start = from.getValue();
        end = till.getValue();
        break;
      default:
        break;  
    }
    LOGGER.debug("Aktion {}, Range {}, Start {}, End {}", action, rangeValue, start, end);
    window.setVisible(false);
    params.setCurrentActionType(getSelectedAction());
    params.setDatasetSelectionType(getSelectedRange());
    return true;
  }
  
  private ACTION getSelectedAction()
  {
    if (singleDocument.getState())
    {
      return ACTION.SINGLE_DOCUMENT;
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
