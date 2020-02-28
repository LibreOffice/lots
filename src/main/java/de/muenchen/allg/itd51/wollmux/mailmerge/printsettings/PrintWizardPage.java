package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.dispatch.AbstractDispatchResultListener;

/**
 * A page of the mail merge wizard. Settings of the printer can be done here.
 */
public class PrintWizardPage extends AbstractXWizardPage
{

  private final XTextComponent name;
  private final XButton change;

  /**
   * Creates this page.
   *
   * @param parentWindow
   *          The containing window.
   * @param pageId
   *          The id of this page.
   * @param model
   *          The model of the document.
   * @throws Exception
   *           If the page can't be created.
   */
  public PrintWizardPage(XWindow parentWindow, short pageId, TextDocumentModel model)
      throws Exception
  {
    super(pageId, parentWindow, "seriendruck_printer");
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    name = UNO.XTextComponent(container.getControl("name"));
    name.setText(model.getCurrentPrinterName());
    change = UNO.XButton(container.getControl("change"));
    AbstractDispatchResultListener listener = event -> name.setText(model.getCurrentPrinterName());
    AbstractActionListener changeListener = event -> model.configurePrinter(listener);
    change.addActionListener(changeListener);
  }

  @Override
  public boolean canAdvance()
  {
    return !name.getText().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    window.setVisible(false);
    return true;
  }
}
