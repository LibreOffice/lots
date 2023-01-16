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
package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XModel;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractSidebarFactory;

/**
 * Factory for creating the sidebar.
 */
public class MailMergeFactory extends AbstractSidebarFactory
{

  @SuppressWarnings("java:S115")
  public static final String SERVICE_NAME = "de.muenchen.allg.itd51.wollmux.mailmerge.sidebar."
      + "SeriendruckSidebarFactory";

  /**
   * Create the factory.
   *
   * @param context
   *          The context of the factory.
   */
  public MailMergeFactory(XComponentContext context)
  {
    super(SERVICE_NAME, context);
  }

  @Override
  public XUIElement createUIElement(String resourceUrl, PropertyValue[] arguments)
      throws NoSuchElementException
  {
    if (!resourceUrl
        .startsWith("private:resource/toolpanel/SeriendruckSidebarFactory/SeriendruckSidebar"))
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

    MailMergeController controller = new MailMergeController(resourceUrl,
        context, parentWindow, model);
    return controller.getGUI();
  }
}
