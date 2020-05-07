package de.muenchen.allg.itd51.wollmux.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.dialog.adapter.AbstractSidebarPanel;

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
