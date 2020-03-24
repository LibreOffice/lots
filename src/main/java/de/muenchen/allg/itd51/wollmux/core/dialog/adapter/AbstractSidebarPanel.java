package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.UIElementType;
import com.sun.star.ui.XToolPanel;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.UnoRuntime;

/**
 * A default implementation for a sidebar panel.
 */
public abstract class AbstractSidebarPanel extends ComponentBase
    implements XUIElement
{
  /**
   * The resource description.
   */
  private final String resourceUrl;

  /**
   * The panel.
   */
  protected XToolPanel panel;

  /**
   * A default panel. {@link #panel} has to be set manually.
   * 
   * @param resourceUrl
   *          The resource description.
   */
  public AbstractSidebarPanel(String resourceUrl)
  {
    this.resourceUrl = resourceUrl;
  }

  @Override
  public XFrame getFrame()
  {
    return null;
  }

  @Override
  public Object getRealInterface()
  {
    return panel;
  }

  @Override
  public String getResourceURL()
  {
    return resourceUrl;
  }

  @Override
  public short getType()
  {
    return UIElementType.TOOLPANEL;
  }

  @Override
  public void dispose()
  {
    XComponent xPanelComponent = UnoRuntime.queryInterface(XComponent.class,
        panel);
    xPanelComponent.dispose();
  }
}
