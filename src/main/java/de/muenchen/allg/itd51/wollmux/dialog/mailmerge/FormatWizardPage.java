package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

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
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.FORMAT;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.SubmitArgument;
import de.muenchen.allg.itd51.wollmux.func.print.MailMergePrintFunction;
import de.muenchen.allg.itd51.wollmux.func.print.SetFormValue;

/**
 * Format page of the mailmerge wizard.
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

  private final AbstractItemListener formatListener = new AbstractItemListener()
  {

    @Override
    public void itemStateChanged(ItemEvent event)
    {
      controller.activateNextButton(canAdvance());
    }
  };

  public FormatWizardPage(XWindow parentWindow, short pageId, MailmergeWizardController controller)
      throws Exception
  {
    super(pageId, parentWindow, "seriendruck_format");
    this.controller = controller;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    odt = UNO.XRadio(container.getControl("odt"));
    odt.addItemListener(formatListener);
    pdf = UNO.XRadio(container.getControl("pdf"));
    pdf.addItemListener(formatListener);
    name = UNO.XTextComponent(container.getControl("name"));
    name.addTextListener(new AbstractTextListener()
    {

      @Override
      public void textChanged(TextEvent arg0)
      {
        controller.activateNextButton(canAdvance());
      }
    });
    name.setText(
        controller.getController().getDefaultFilename()
            + MailMergePrintFunction.createMergeFieldTag(SetFormValue.TAG_RECORD_ID));
    mailmerge = UNO.XComboBox(container.getControl("mailmerge"));
    new MailMergeField(mailmerge).setMailMergeDatasource(controller.getController().getDs());
    mailmerge.addItemListener(new AbstractItemListener()
    {
      @Override
      public void itemStateChanged(ItemEvent event)
      {
        name.setText(name.getText()
            + MailMergePrintFunction.createMergeFieldTag(mailmerge.getItem((short) event.Selected)));
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
          append = MailMergePrintFunction
              .createMergeFieldTag(SetFormValue.TAG_RECORD_ID);
          break;
        case 2:
          append = MailMergePrintFunction
              .createMergeFieldTag(SetFormValue.TAG_MAILMERGE_ID);
          break;
        default:
          break;
        }
        name.setText(name.getText() + append);
      }
    });
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
    LOGGER.debug("canAdvance");
    return getSelectedFormat() != FORMAT.NOTHING && !getNamingTemplate().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    controller.arguments.put(SubmitArgument.FILENAME_TEMPLATE, name.getText());
    controller.format = getSelectedFormat();
    window.setVisible(false);
    LOGGER.debug("Format {}, Name {}", getSelectedFormat(), getNamingTemplate());
    return true;
  }
}
