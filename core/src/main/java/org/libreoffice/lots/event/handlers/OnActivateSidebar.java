/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XController2;
import com.sun.star.ui.XDeck;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.util.UnoSidebar;
import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.form.sidebar.FormSidebarController;
import org.libreoffice.lots.sidebar.WollMuxSidebarPanel;

/**
 * Activate the sidebar belonging to the document.
 */
public class OnActivateSidebar extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnActivateSidebar.class);

  private TextDocumentController documentController;

  /**
   * A new sidebar activation event.
   *
   * @param documentController
   *          The controller of the document.
   */
  public OnActivateSidebar(TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    XController2 controller = UNO.XController2(documentController.getModel().doc.getCurrentController());
    controller.getSidebar().showDecks(true);
    controller.getSidebar().setVisible(true);
    if (controller.getSidebar().getSidebar() != null)
    {
      if (documentController.getModel().isFormDocument())
      {
        this.activateSidebarPanel(controller, FormSidebarController.WM_FORM_GUI);
        try
        {
          documentController.getFrameController().setDocumentZoom(
              WollMuxFiles.getWollmuxConf().query("Fenster").query("Formular").getLastChild().query("ZOOM"));
        } catch (java.lang.Exception e)
        {
          // configuration for Fenster isn't mandatory
        }
      } else
      {
        this.activateSidebarPanel(controller, WollMuxSidebarPanel.WM_BAR);
      }
      controller.getSidebar().getSidebar().requestLayout();
    }
  }

  private void activateSidebarPanel(XController2 controller, String deckName)
  {
    try
    {
      XDeck deck = UnoSidebar.getDeckByName(deckName, controller);
      if (deck != null && !deck.isActive())
      {
        deck.activate(true);
      }
    } catch (UnoHelperException e)
    {
      LOGGER.trace("", e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel().doc.hashCode() + ")";
  }
}
