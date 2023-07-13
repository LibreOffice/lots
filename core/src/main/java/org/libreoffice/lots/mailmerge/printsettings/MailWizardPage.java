/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
import com.sun.star.awt.Selection;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractItemListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractTextListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractXWizardPage;
import org.libreoffice.lots.db.ColumnNotFoundException;
import org.libreoffice.lots.mailmerge.NoTableSelectedException;
import org.libreoffice.lots.mailmerge.print.MailMergePrintFunction;
import org.libreoffice.lots.mailmerge.ui.MailMergeField;
import org.libreoffice.lots.sender.SenderException;
import org.libreoffice.lots.sender.SenderService;

/**
 * A page of the mail merge wizard. Settings for mails can be done here.
 */
public class MailWizardPage extends AbstractXWizardPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger(MailWizardPage.class);

  private final XTextComponent sender;
  private final XTextComponent subject;
  private final XTextComponent message;
  private final XComboBox reciever;
  private String recieverValue = "";
  private final XComboBox mailmerge;
  private final XComboBox special;
  private final XControlContainer container;

  private final MailmergeWizardController controller;
  private final PrintSettings settings;

  private AbstractTextListener textListener = new AbstractTextListener()
  {

    @Override
    public void textChanged(TextEvent arg0)
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
  public MailWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller,
      PrintSettings settings) throws Exception
  {
    super(pageId, parentWindow, "vnd.sun.star.script:WollMux.seriendruck_mail?location=application");
    this.controller = controller;
    this.settings = settings;
    container = UNO.XControlContainer(window);
    sender = UNO.XTextComponent(container.getControl("sender"));
    String senderName = "";
    try
    {
      senderName = SenderService.getInstance().getCurrentSenderValue("Mail");
    } catch (SenderException | ColumnNotFoundException e)
    {
      LOGGER.debug("Kein Eintrag für Mail vorhanden", e);
    }
    sender.setText(senderName == null ? "" : senderName);
    sender.addTextListener(textListener);
    subject = UNO.XTextComponent(container.getControl("subject"));
    subject.addTextListener(textListener);

    message = UNO.XTextComponent(container.getControl("message"));
    // default text
    message.setText(
        "Sehr geehrte Damen und Herren, \n\n anbei erhalten Sie ... \n\n Mit freundlichen Grüßen \n ...");
    message.addTextListener(textListener);

    reciever = UNO.XComboBox(container.getControl("reciever"));
    try
    {
      reciever.addItems(
          controller.getModel().getColumnNames().toArray(new String[] {}),
          (short) 0);
    } catch (NoTableSelectedException e)
    {
      // nothing to do
    }
    reciever.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        controller.updateTravelUI();
        recieverValue = reciever.getItem((short) event.Selected);
      }
    });
    mailmerge = UNO.XComboBox(container.getControl("mailmerge"));
    new MailMergeField(mailmerge).setMailMergeDatasource(controller.getModel());
    mailmerge.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        if (event.Selected != 0)
        {
          Selection currentSelection = message.getSelection();
          message.insertText(currentSelection, MailMergePrintFunction
              .createMergeFieldTag(mailmerge.getItem((short) event.Selected)));
          UNO.XTextComponent(special).setText(special.getItem((short) 0));
        }
      }
    });

    special = UNO.XComboBox(container.getControl("special"));
    special.addItems(new String[] { "Bitte wählen", "Datensatznummer", "Serienbriefnummer" },
        (short) 0);
    UNO.XTextComponent(special).setText(special.getItem((short) 0));
    special.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        if (event.Selected != 0)
        {
          String selectedValue = special.getItem((short) event.Selected);

          if (selectedValue.equals("Datensatznummer"))
            selectedValue = "{{#DS}}";
          else
            selectedValue = "{{#SB}}";

          Selection currentSelection = message.getSelection();
          message.insertText(currentSelection, selectedValue);
          UNO.XTextComponent(special).setText(special.getItem((short) 0));
        }
      }
    });
  }

  @Override
  public boolean canAdvance()
  {
    return !sender.getText().isEmpty() && !subject.getText().isEmpty()
        && !message.getText().isEmpty() && !UNO.XTextComponent(reciever).getText().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    settings.setEmailFrom(sender.getText());
    settings.setEmailSubject(subject.getText());
    settings.setEmailText(message.getText());
    settings.setEmailToFieldName(recieverValue);
    window.setVisible(false);
    return true;
  }

}
