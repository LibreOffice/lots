package de.muenchen.allg.itd51.wollmux.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.dialog.adapter.AbstractSidebarFactory;

/**
 * Factory for the WollMux sidebar. It's mentioned in Factories.xcu.
 */
public class WollMuxSidebarFactory extends AbstractSidebarFactory
{

  public static final String SERVICE_NAME = "de.muenchen.allg.itd51.wollmux.sidebar.WollMuxSidebarFactory";

  /**
   * Create the WollMux bar.
   *
   * @param context
   *          The context of the bar.
   */
  public WollMuxSidebarFactory(XComponentContext context)
  {
    super(SERVICE_NAME, context);
  }

  @Override
  public XUIElement createUIElement(String resourceUrl, PropertyValue[] arguments)
      throws NoSuchElementException
  {
    if (!resourceUrl.startsWith("private:resource/toolpanel/WollMuxSidebarFactory/WollMuxSidebarPanel"))
    {
      throw new NoSuchElementException(resourceUrl, this);
    }

    XWindow parentWindow = null;
    for (int i = 0; i < arguments.length; i++)
    {
      if (arguments[i].Name.equals("ParentWindow"))
      {
        parentWindow =
          UnoRuntime.queryInterface(XWindow.class, arguments[i].Value);
        break;
      }
    }

    return new WollMuxSidebarPanel(context, parentWindow, resourceUrl);
  }
}
