package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.UIElementType;
import com.sun.star.ui.XToolPanel;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * The sidebar panel.
 */
public class MailMergePanel extends ComponentBase implements XUIElement
{
  private String resourceUrl;
  private XToolPanel panel;

  /**
   * Creates a new sidebar panel.
   *
   * @param context
   *          The context of the sidebar.
   * @param parentWindow
   *          The parent window.
   * @param model
   *          The model of the document to which the sidebar belongs.
   * @param resourceUrl
   *          The resource description.
   */
  public MailMergePanel(XComponentContext context, XWindow parentWindow,
      XModel model, String resourceUrl)
  {
    this.resourceUrl = resourceUrl;
    MailMergeController controller = new MailMergeController(context,
        parentWindow, model);
    panel = controller.getGUI();
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