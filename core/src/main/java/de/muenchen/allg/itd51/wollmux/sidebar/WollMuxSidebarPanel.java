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
package de.muenchen.allg.itd51.wollmux.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.dialog.adapter.AbstractSidebarPanel;

/**
 * The panel of the WollMux Bar.
 */
public class WollMuxSidebarPanel extends AbstractSidebarPanel
{

  public static final String WM_BAR = "WollMuxDeck";

  /**
   * Create the panel.
   *
   * @param context
   *          The context.
   * @param parentWindow
   *          The parent window.
   * @param resourceUrl
   *          The resource URL.
   */
  public WollMuxSidebarPanel(XComponentContext context, XWindow parentWindow,
      String resourceUrl)
  {
    super(resourceUrl);
    panel = new WollMuxSidebarContent(context, parentWindow);
  }
}
