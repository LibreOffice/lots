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
package org.libreoffice.lots.mailmerge.printsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractItemListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractXWizardPage;
import org.libreoffice.lots.mailmerge.NoTableSelectedException;
import org.libreoffice.lots.mailmerge.printsettings.MailmergeWizardController.PATH;
import org.libreoffice.lots.mailmerge.printsettings.PrintSettings.ACTION;
import org.libreoffice.lots.mailmerge.printsettings.PrintSettings.DatasetSelectionType;

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
    XControlContainer container = UNO.XControlContainer(window);
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
