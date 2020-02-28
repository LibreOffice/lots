package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

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
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.mailmerge.MailMergeController.SubmitArgument;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.MailMergePrintFunction;
import de.muenchen.allg.itd51.wollmux.mailmerge.ui.MailMergeField;

/**
 * Mail page of the mailmerge wizard.
 */
public class MailWizardPage extends AbstractXWizardPage
{
	private static final Logger LOGGER = LoggerFactory
		      .getLogger(MailWizardPage.class);

  private final XTextComponent sender;
  private final XTextComponent subject;
  private final XTextComponent message;
  private final XComboBox reciever;
  private String recieverValue = "";
  private final XComboBox mailmerge;
  private final XComboBox special;
  private final XControlContainer container;

  private final MailmergeWizardController controller;

  private AbstractTextListener textListener = new AbstractTextListener()
  {

    @Override
    public void textChanged(TextEvent arg0)
    {
        controller.enableFinishButton(true);
    }
  };

  public MailWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller)
      throws Exception
  {
    super(pageId, parentWindow, "seriendruck_mail");
    this.controller = controller;
    container = UnoRuntime.queryInterface(XControlContainer.class, window);
    sender = UNO.XTextComponent(container.getControl("sender"));
    String senderName = "";
	try {
		senderName = DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDataset().get("Mail");
	} catch (ColumnNotFoundException | DatasetNotFoundException e) {
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
    reciever.addItems(controller.getController().getColumnNames().toArray(new String[] {}),
        (short) 0);
    reciever.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        controller.activateNextButton(canAdvance());
        recieverValue = reciever.getItem((short) event.Selected);
      }
    });
    mailmerge = UNO.XComboBox(container.getControl("mailmerge"));
    new MailMergeField(mailmerge).setMailMergeDatasource(controller.getController().getDs());
    mailmerge.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        if (event.Selected != 0)
        {
          Selection currentSelection = message.getSelection();
          message.insertText(currentSelection,
              MailMergePrintFunction.createMergeFieldTag(mailmerge.getItem((short) event.Selected)));
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
    controller.arguments.put(SubmitArgument.EMAIL_FROM, sender.getText());
    controller.arguments.put(SubmitArgument.EMAIL_SUBJECT, subject.getText());
    controller.arguments.put(SubmitArgument.EMAIL_TEXT, message.getText());
    controller.arguments.put(SubmitArgument.EMAIL_TO_FIELD_NAME, recieverValue);
    window.setVisible(false);
    return true;
  }

}
