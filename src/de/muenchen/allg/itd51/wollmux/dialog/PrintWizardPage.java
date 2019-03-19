package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

public class PrintWizardPage extends AbstractXWizardPage
{
  
  private static final Logger LOGGER = LoggerFactory.getLogger(PrintWizardPage.class);
  
  private final XTextComponent name;
  private final XButton change;
  
  private MailMergeParams params;
  
  public PrintWizardPage(XWindow parentWindow, short pageId, MailMergeParams params) throws Exception
  {
    super(pageId, parentWindow, "seriendruck_printer");
    this.params = params;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    name = UNO.XTextComponent(container.getControl("name"));
    name.setText("");
    change = UNO.XButton(container.getControl("change"));
    change.addActionListener(new AbstractActionListener()
    {
      
      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        
      }
    });
  }

  @Override
  public boolean canAdvance()
  {
    return false;
  }

  @Override
  public boolean commitPage(short reason)
  {
    window.setVisible(false);
    return true;
  }

}
