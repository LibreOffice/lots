package de.muenchen.allg.itd51.wollmux.dialog;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XButton;
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

public class MailWizardPage extends AbstractXWizardPage
{
  
  private final XTextComponent sender;
  private final XTextComponent subject;
  private final XTextComponent message;
  private final XComboBox reciever;
  private final XButton mailmerge;
  private final XButton special;
  
  private final MailmergeWizardController controller;
  
  private AbstractTextListener textListener = new AbstractTextListener()
  {
    
    @Override
    public void textChanged(TextEvent arg0)
    {
      controller.activateNextButton(canAdvance());
    }
  };
  
  public MailWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller) throws Exception
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
    reciever.addItemListener(new AbstractItemListener()
    {
      
      @Override
      public void itemStateChanged(ItemEvent arg0)
      {
        controller.activateNextButton(canAdvance());
      }
    });
    mailmerge = UNO.XButton(container.getControl("mailmerge"));
    special = UNO.XButton(container.getControl("special"));
  }

  @Override
  public boolean canAdvance()
  {
    return !sender.getText().isEmpty()
        && !subject.getText().isEmpty()
        && !message.getText().isEmpty()
        && !UNO.XTextComponent(reciever).getText().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    window.setVisible(false);
    return true;
  }

}
