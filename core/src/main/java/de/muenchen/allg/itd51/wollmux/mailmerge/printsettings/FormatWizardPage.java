/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractItemListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractTextListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.MailMergePrintFunction;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.SetFormValue;
import de.muenchen.allg.itd51.wollmux.mailmerge.printsettings.PrintSettings.FORMAT;
import de.muenchen.allg.itd51.wollmux.mailmerge.ui.MailMergeField;
import de.muenchen.allg.itd51.wollmux.mailmerge.ui.SpecialField;

/**
 * A page of the mail merge wizard. Settings for creating files can be done here.
 */
public class FormatWizardPage extends AbstractXWizardPage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormatWizardPage.class);

  private final XRadioButton odt;
  private final XRadioButton pdf;
  private final XTextComponent name;
  private final XComboBox mailmerge;
  private final XComboBox special;

  private final MailmergeWizardController controller;
  private final PrintSettings settings;

  private boolean inConstructor;

  private final AbstractItemListener formatListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      controller.updateTravelUI();
    }
  };

  /**
   * Create this page.
   *
   * @param parentWindow
   *          The containing window.
   * @param pageId
   *          The id of this page.
   * @param controller
   *          The wizard controller.
   * @param settings
   *          The print settings.
   * @throws Exception
   *           If this page can't be created.
   */
  public FormatWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller,
      PrintSettings settings) throws Exception
  {
    super(pageId, parentWindow, "vnd.sun.star.script:WollMux.seriendruck_format?location=application");
    inConstructor = true;
    this.controller = controller;
    this.settings = settings;
    XControlContainer container = UNO.XControlContainer(window);
    odt = UNO.XRadio(container.getControl("odt"));
    odt.addItemListener(formatListener);
    pdf = UNO.XRadio(container.getControl("pdf"));
    pdf.addItemListener(formatListener);
    name = UNO.XTextComponent(container.getControl("name"));
    name.setText(
        controller.getDefaultFilename() + MailMergePrintFunction.createMergeFieldTag(SetFormValue.TAG_RECORD_ID));
    name.addTextListener(new AbstractTextListener()
    {

      @Override
      public void textChanged(TextEvent arg0)
      {
        // avoid cascade of calls between LO and WollMux
        if(!inConstructor)
          controller.updateTravelUI();
      }
    });
    mailmerge = UNO.XComboBox(container.getControl("mailmerge"));
    new MailMergeField(mailmerge).setMailMergeDatasource(controller.getModel());
    mailmerge.addItemListener(new AbstractItemListener()
    {
      @Override
      public void itemStateChanged(ItemEvent event)
      {
        name.setText(name.getText() + MailMergePrintFunction
            .createMergeFieldTag(mailmerge.getItem((short) event.Selected)));
      }
    });
    special = UNO.XComboBox(container.getControl("special"));
    SpecialField.addItems(special,
        new String[] { "Bitte wählen...", "Datensatznummer", "Serienbriefnummer" });
    special.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        String append = "";
        switch (event.Selected)
        {
        case 1:
          append = MailMergePrintFunction.createMergeFieldTag(SetFormValue.TAG_RECORD_ID);
          break;
        case 2:
          append = MailMergePrintFunction.createMergeFieldTag(SetFormValue.TAG_MAILMERGE_ID);
          break;
        default:
          break;
        }
        name.setText(name.getText() + append);
      }
    });
    inConstructor = false;
  }

  private FORMAT getSelectedFormat()
  {
    if (odt.getState())
    {
      return FORMAT.ODT;
    }
    if (pdf.getState())
    {
      return FORMAT.PDF;
    }
    return FORMAT.NOTHING;
  }

  private String getNamingTemplate()
  {
    return name.getText();
  }

  @Override
  public boolean canAdvance()
  {
    return getSelectedFormat() != FORMAT.NOTHING && !getNamingTemplate().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    settings.setFilenameTemplate(name.getText());
    settings.setFormat(getSelectedFormat());
    window.setVisible(false);
    LOGGER.debug("Format {}, Name {}", getSelectedFormat(), getNamingTemplate());
    return true;
  }
}
