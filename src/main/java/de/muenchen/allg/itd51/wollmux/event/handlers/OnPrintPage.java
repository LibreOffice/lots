package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.XPropertySet;
import com.sun.star.text.XPageCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Event for printing the current visible page.
 */
public class OnPrintPage extends WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnPrintPage.class);

  private TextDocumentController documentController;

  /**
   * Create this event.
   *
   * @param documentController
   *          The current document.
   */
  public OnPrintPage(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XTextViewCursorSupplier viewCursorSupplier = UNO.XTextViewCursorSupplier(
        documentController.getModel().doc.getCurrentController());
    XPageCursor pageCursor = UNO
        .XPageCursor(viewCursorSupplier.getViewCursor());
    short page = pageCursor.getPage();
    short currentPage = page;

    try
    {
      if (!isPrintEmptyPages())
      {
        for (short i = page; i > 0; i--)
        {
          if (!pageCursor.jumpToPage(i))
          {
            page--;
          }
        }
        pageCursor.jumpToPage(currentPage);
      }
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.trace("", e);
    }

    XPrintable printable = UNO.XPrintable(documentController.getModel().doc);

    UnoProps props = new UnoProps();
    props.setPropertyValue("CopyCount", (short) 1);
    props.setPropertyValue("Pages", String.valueOf(page));
    props.setPropertyValue("Collate", false);

    printable.print(props.getProps());
  }

  /**
   * Check if print empty pages is active.
   *
   * @return True if empty pages should be printed, false otherwise.
   * @throws com.sun.star.uno.Exception
   *           If we can't access the properties.
   */
  private boolean isPrintEmptyPages() throws com.sun.star.uno.Exception
  {
    try
    {
      XPropertySet inSettings = UNO
          .XPropertySet(UNO.XMultiServiceFactory(documentController.getModel().doc)
              .createInstance("com.sun.star.document.Settings"));
      return (boolean) inSettings.getPropertyValue("PrintEmptyPages");
    } catch (com.sun.star.uno.Exception e)
    {
      Object cfgProvider = UNO
          .createUNOService("com.sun.star.configuration.ConfigurationProvider");

      Object cfgAccess = UNO.XMultiServiceFactory(cfgProvider).createInstanceWithArguments(
          "com.sun.star.configuration.ConfigurationAccess",
          new UnoProps("nodepath", "/org.openoffice.Office.Writer/Print").getProps());
      return (boolean) UNO.XPropertySet(cfgAccess).getPropertyValue("EmptyPages");
    }
  }
}