package de.muenchen.allg.itd51.wollmux.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.UIElementType;
import com.sun.star.ui.XToolPanel;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class SeriendruckSidebarPanel extends ComponentBase implements XUIElement
{
  private String resourceUrl;
  private XToolPanel panel;

  public SeriendruckSidebarPanel(XComponentContext context, XWindow parentWindow,
      String resourceUrl)
  {
    this.resourceUrl = resourceUrl;
    panel = new SeriendruckSidebarContent(context, parentWindow);
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
    XComponent xPanelComponent = UnoRuntime.queryInterface(XComponent.class, panel);
    xPanelComponent.dispose();
  }

}