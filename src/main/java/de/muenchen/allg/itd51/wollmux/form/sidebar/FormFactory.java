package de.muenchen.allg.itd51.wollmux.form.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XModel;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractSidebarFactory;

/**
 * Factory for creating the sidebar.
 */
public class FormFactory extends AbstractSidebarFactory
{

  public static final String SERVICE_NAME = "de.muenchen.allg.itd51.wollmux.form.sidebar.FormFactory";

  /**
   * Create the factory.
   *
   * @param context
   *          The context of the factory.
   */
  public FormFactory(XComponentContext context)
  {
    super(SERVICE_NAME, context);
  }

  @Override
  public XUIElement createUIElement(String resourceUrl, PropertyValue[] arguments)
      throws NoSuchElementException
  {
    if (!resourceUrl
        .startsWith("private:resource/toolpanel/FormFactory/FormSidebarPanel"))
    {
      throw new NoSuchElementException(resourceUrl, this);
    }

    XWindow parentWindow = null;
    XModel model = null;
    for (int i = 0; i < arguments.length; i++)
    {
      if (arguments[i].Name.equals("ParentWindow"))
      {
        parentWindow = UNO.XWindow(arguments[i].Value);
      } else if (arguments[i].Name.equals("Controller"))
      {
        model = UNO.XController(arguments[i].Value).getModel();
      }
    }

    FormSidebarController formSidebarController = new FormSidebarController(resourceUrl, context, parentWindow, model);
    return formSidebarController.getFormSidebarPanel();
  }
}
