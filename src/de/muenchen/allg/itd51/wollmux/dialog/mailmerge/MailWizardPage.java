package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.SubmitArgument;

public class MailWizardPage extends AbstractXWizardPage
{

  private final XTextComponent sender;
  private final XTextComponent subject;
  private final XTextComponent message;
  private final XComboBox reciever;
  private String recieverValue = "";
  private final XComboBox mailmerge;
  private final XComboBox special;

  private final MailmergeWizardController controller;

  private AbstractTextListener textListener = new AbstractTextListener()
  {

    @Override
    public void textChanged(TextEvent arg0)
    {
      controller.activateNextButton(canAdvance());
    }
  };

  public MailWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller)
      throws Exception
  {
    super(pageId, parentWindow, "seriendruck_mail");
    this.controller = controller;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    sender = UNO.XTextComponent(container.getControl("sender"));
    sender.addTextListener(textListener);
    subject = UNO.XTextComponent(container.getControl("subject"));
    subject.addTextListener(textListener);
    message = UNO.XTextComponent(container.getControl("message"));
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
        message.setText(message.getText() + MailMergeNew.addMergeFieldTags(mailmerge.getItem((short) event.Selected)));
      }
    });
    special = UNO.XComboBox(container.getControl("special"));
    SpecialField.addItems(special);
    special.addItemListener(new AbstractItemListener()
    {

      @Override
      public void itemStateChanged(ItemEvent event)
      {
        message.setText(message.getText() + special.getItem((short) event.Selected));
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
