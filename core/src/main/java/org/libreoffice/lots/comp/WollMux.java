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
package org.libreoffice.lots.comp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XIndexContainer;
import com.sun.star.document.XEventListener;
import com.sun.star.form.FormButtonType;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.XModuleUIConfigurationManagerSupplier;
import com.sun.star.ui.XUIConfigurationManager;
import com.sun.star.uno.XComponentContext;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoList;
import org.libreoffice.ext.unohelper.common.UnoProps;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.WollMuxSingleton;
import org.libreoffice.lots.db.ColumnNotFoundException;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.dispatch.AboutDispatch;
import org.libreoffice.lots.dispatch.DispatchProviderAndInterceptor;
import org.libreoffice.lots.event.handlers.OnAddDocumentEventListener;
import org.libreoffice.lots.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.interfaces.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.interfaces.XWollMux;
import de.muenchen.allg.itd51.wollmux.interfaces.XWollMuxDocument;
import org.libreoffice.lots.sender.SenderException;
import org.libreoffice.lots.sender.SenderService;
import org.libreoffice.lots.util.L;

/**
 * The main service of WollMux. It treats all Dispatches starting with "wollmux:" and is the
 * interface to other UNO components.
 *
 * The service is instantiated several times. Every time a dispatch with "wollmux:" occurs a new
 * instance is created.
 */
public class WollMux extends WeakBase implements XServiceInfo, XDispatchProvider, XWollMux
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMux.class);

  /**
   * The FQDN of the implemented service.
   */
  public static final String SERVICENAME = "org.libreoffice.lots.WollMux";

  /**
   * Initialize WollMux and its menus.
   *
   * @param ctx
   *          The context of WollMux.
   */
  public WollMux(XComponentContext ctx)
  {
    WollMuxSingleton.initialize(ctx);

    if (!WollMuxSingleton.getInstance().isMenusCreated())
    {
      createMenuItems();
      WollMuxSingleton.getInstance().setMenusCreated(true);
    }
  }

  @Override
  public String[] getSupportedServiceNames()
  {
    return new String[] { SERVICENAME };
  }

  @Override
  public boolean supportsService(String sService)
  {
    return sService.equals(SERVICENAME);
  }

  @Override
  public String getImplementationName()
  {
    return (WollMux.class.getName());
  }

  @Override
  public XDispatch queryDispatch(com.sun.star.util.URL aURL, String sTargetFrameName, int iSearchFlags)
  {
    return DispatchProviderAndInterceptor.globalWollMuxDispatches.queryDispatch(aURL, sTargetFrameName, iSearchFlags);
  }

  @Override
  public XDispatch[] queryDispatches( /* IN */DispatchDescriptor[] seqDescripts)
  {
    return DispatchProviderAndInterceptor.globalWollMuxDispatches.queryDispatches(seqDescripts);
  }

  @Override
  public void addPALChangeEventListener(XPALChangeEventListener l)
  {
    SenderService.getInstance().addPALChangeEventListener(l);
  }

  @Override
  public void addPALChangeEventListenerWithConsistencyCheck(XPALChangeEventListener l, int wollmuxConfHashCode)
  {
    addPALChangeEventListener(l);

    // Check if calling and my configuration are the same
    int myWmConfHash = WollMuxFiles.getWollmuxConf().stringRepresentation().hashCode();
    if (myWmConfHash != wollmuxConfHashCode)
    {
      InfoDialog.showInfoModal(L.m("WollMux error"),
          L.m("The configuration of the Wollmux must be re-read.\n\n"
              + "Please close Wollmux and LibreOffice and make sure "
              + "no 'soffice.bin' processes are still running."));
    }
  }

  /**
   * Add a listener for events fired by WollMux. A listener can only be registered ones. All
   * listeners are called in a separate thread.
   *
   * WollMux fires the following events:
   * <ol>
   * <li>OnWollMuxProcessingFinished: Fired as soon as WollMux has finished processing the document.
   * For text documents all WollMux commands are executed before firing the event. For all other
   * types of documents the event is fired immediately.</li>
   * </ol>
   *
   * @param l
   *          The listener to notify.
   *
   * @see com.sun.star.document.XEventBroadcaster#addEventListener(com.sun.star.document.XEventListener)
   */
  @Override
  public void addEventListener(XEventListener l)
  {
    new OnAddDocumentEventListener(l).emit();
  }

  @Override
  public void removePALChangeEventListener(XPALChangeEventListener l)
  {
    SenderService.getInstance().removePALChangeEventListener(l);
  }

  @Override
  public void removeEventListener(XEventListener l)
  {
    new OnRemoveDocumentEventListener(l).emit();
  }

  @Override
  public void setCurrentSender(String sender, short idx)
  {
    LOGGER.trace("WollMux.setCurrentSender(\"{}\", \"{}\")", sender, idx);
    SenderService.getInstance().selectSender(sender, idx);
  }

  @Override
  public PropertyValue[] getInsertValues()
  {
    UnoProps p = new UnoProps();
    for (Map.Entry<String, String> value : SenderService.getInstance().getCurrentSenderValues().entrySet())
    {
      p.setPropertyValue(value.getKey(), value.getValue());
    }
    return p.getProps();
  }

  @Override
  public String getValue(String dbSpalte)
  {
    try
    {
      return SenderService.getInstance().getCurrentSenderValue(dbSpalte);
    } catch (SenderException | ColumnNotFoundException e)
    {
      LOGGER.error(e.getMessage(), e);
    }
    return "";
  }

  @Override
  public XWollMuxDocument getWollMuxDocument(XComponent doc)
  {
    XTextDocument tdoc = UNO.XTextDocument(doc);
    if (tdoc != null)
      return new WollMuxDocument(tdoc);
    return null;
  }

  private void createMenuItems()
  {
    deleteMenuButton("wollmux:Seriendruck", ".uno:ToolsMenu");
    deleteMenuButton(AboutDispatch.COMMAND, ".uno:HelpMenu");
    WollMux.createMenuButton(AboutDispatch.COMMAND, L.m("About WollMux"),
        ".uno:HelpMenu", ".uno:About");
  }

  /**
   * Delete a persistent entry form writer top level menu.
   *
   * @param removeCmdUrl
   *          The dispatch URL of the entry.
   * @param deleteFromMenuUrl
   *          The dispatch of top level menu.
   */
  private static void deleteMenuButton(String removeCmdUrl, String deleteFromMenuUrl)
  {
    final String settingsUrl = "private:resource/menubar/menubar";

    try
    {
      XModuleUIConfigurationManagerSupplier suppl = UNO.XModuleUIConfigurationManagerSupplier(
          UnoComponent.createComponentWithContext(UnoComponent.CSS_UI_MODULE_UI_CONFIGURATION_MANAGER_SUPPLIER));
      XUIConfigurationManager cfgMgr = UNO
          .XUIConfigurationManager(suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
      UnoList<PropertyValue[]> menubar = UnoList.create(cfgMgr.getSettings(settingsUrl, true), PropertyValue[].class);

      int idx = findElementWithCmdURL(menubar, deleteFromMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps(menubar.get(idx));
        XIndexContainer toolsMenu = UNO.XIndexContainer(desc.getPropertyValue(UnoProperty.ITEM_DESCRIPTOR_CONTAINER));

        idx = findElementWithCmdURL(UnoList.create(toolsMenu, PropertyValue[].class), removeCmdUrl);
        if (idx >= 0)
        {
          toolsMenu.removeByIndex(idx);
          cfgMgr.replaceSettings(settingsUrl, menubar.getAccess());
          UNO.XUIConfigurationPersistence(cfgMgr).store();
        }
      }
    } catch (Exception e)
    {
      LOGGER.trace("", e);
    }
  }

  /**
   * Add a persistent menu entry to a top level menu. At first the entry should be deleted with
   * {@link #deleteMenuButton(String, String)}.
   *
   * @param cmdUrl
   *          The dispatch URL of the entry.
   * @param label
   *          The label of the entry.
   * @param insertIntoMenuUrl
   *          The dispatch of the top level menu.
   * @param insertBeforeElementUrl
   *          The dispatch of entry before the new entry.
   */
  private static void createMenuButton(String cmdUrl, String label, String insertIntoMenuUrl,
      String insertBeforeElementUrl)
  {
    final String settingsUrl = "private:resource/menubar/menubar";

    try
    {
      XModuleUIConfigurationManagerSupplier suppl = UNO.XModuleUIConfigurationManagerSupplier(
          UnoComponent.createComponentWithContext(UnoComponent.CSS_UI_MODULE_UI_CONFIGURATION_MANAGER_SUPPLIER));
      XUIConfigurationManager cfgMgr = UNO
          .XUIConfigurationManager(suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
      UnoList<PropertyValue[]> menubar = UnoList.create(cfgMgr.getSettings(settingsUrl, true), PropertyValue[].class);

      int idx = findElementWithCmdURL(menubar, insertIntoMenuUrl);
      if (idx >= 0)
      {
        UnoProps desc = new UnoProps(menubar.get(idx));
        XIndexContainer toolsMenu = UNO.XIndexContainer(desc.getPropertyValue(UnoProperty.ITEM_DESCRIPTOR_CONTAINER));

        idx = findElementWithCmdURL(UnoList.create(toolsMenu, PropertyValue[].class), insertBeforeElementUrl);
        if (idx >= 0)
        {
          UnoProps newDesc = new UnoProps();
          newDesc.setPropertyValue(UnoProperty.COMMAND_URL, cmdUrl);
          newDesc.setPropertyValue(UnoProperty.TYPE, FormButtonType.PUSH);
          newDesc.setPropertyValue(UnoProperty.LABEL, label);
          toolsMenu.insertByIndex(idx, newDesc.getProps());
          cfgMgr.replaceSettings(settingsUrl, menubar.getAccess());
          UNO.XUIConfigurationPersistence(cfgMgr).store();
        }
      }
    } catch (Exception e)
    {
      LOGGER.trace("", e);
    }
  }

  /**
   * Get the index of the first of occurrence of a menu entry.
   *
   * @param menu
   *          All the menu entries.
   * @param cmdUrl
   *          The dispatch URL of the menu entry to search.
   * @return The index or -1 if no such entry exists.
   */
  private static int findElementWithCmdURL(UnoList<PropertyValue[]> menu, String cmdUrl)
  {
    try
    {
      for (int i = 0; i < menu.size(); ++i)
      {
        UnoProps desc = new UnoProps(menu.get(i));
        for (PropertyValue prop : desc.getProps())
        {
          if (UnoProperty.COMMAND_URL.equals(prop.Name) && cmdUrl.equals(prop.Value))
            return i;
        }
      }
    } catch (Exception e)
    {
      LOGGER.trace("", e);
    }
    return -1;
  }

}
