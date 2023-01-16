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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XPageCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.view.XPrintable;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.common.UnoHelperRuntimeException;
import org.libreoffice.ext.unohelper.common.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import org.libreoffice.ext.unohelper.util.UnoConfiguration;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.ext.unohelper.util.UnoService;

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
    } catch (UnoHelperException e)
    {
      LOGGER.trace("", e);
    }

    XPrintable printable = UNO.XPrintable(documentController.getModel().doc);

    UnoProps props = new UnoProps();
    props.setPropertyValue(UnoProperty.COPY_COUNT, (short) 1);
    props.setPropertyValue(UnoProperty.PAGES, String.valueOf(page));
    props.setPropertyValue(UnoProperty.COLLATE, false);

    printable.print(props.getProps());
  }

  /**
   * Check if print empty pages is active.
   *
   * @return True if empty pages should be printed, false otherwise.
   * @throws com.sun.star.uno.Exception
   *           If we can't access the properties.
   */
  private boolean isPrintEmptyPages() throws UnoHelperException
  {
    try
    {
      return (boolean) UnoProperty.getProperty(
          UnoService.createService(UnoService.CSS_DOCUMENT_SETTINGS, documentController.getModel().doc),
          UnoProperty.PRINT_EMPTY_PAGES);
    } catch (UnoHelperRuntimeException | UnoHelperException e)
    {
      return (boolean) UnoConfiguration.getConfiguration("/org.openoffice.Office.Writer/Print",
          UnoProperty.EMPTY_PAGES);
    }
  }
}
