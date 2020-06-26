/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.comp;

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

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoList;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.XWollMux;
import de.muenchen.allg.itd51.wollmux.XWollMuxDocument;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dispatch.AboutDispatch;
import de.muenchen.allg.itd51.wollmux.dispatch.DispatchProviderAndInterceptor;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemovePALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetSender;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.util.UnoComponent;
import de.muenchen.allg.util.UnoProperty;

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
  public static final String SERVICENAME = "de.muenchen.allg.itd51.wollmux.WollMux";

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
    new OnAddPALChangeEventListener(l, null).emit();
  }

  @Override
  public void addPALChangeEventListenerWithConsistencyCheck(XPALChangeEventListener l, int wollmuxConfHashCode)
  {
    new OnAddPALChangeEventListener(l, wollmuxConfHashCode).emit();
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
    new OnRemovePALChangeEventListener(l).emit();
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
    new OnSetSender(sender, idx).emit();
  }

  @Override
  public PropertyValue[] getInsertValues()
  {
    // No synchronization with WollMuxEventHandler as reading isn't critical.
    DatasourceJoiner dj = DatasourceJoinerFactory.getDatasourceJoiner();
    UnoProps p = new UnoProps();
    try
    {
      DJDataset ds = dj.getSelectedDataset();
      dj.getMainDatasourceSchema().forEach(key -> {
        try
        {
          String val = ds.get(key);
          if (val != null)
          {
            p.setPropertyValue(key, val);
          }
        } catch (ColumnNotFoundException x1)
        {
          LOGGER.trace("", x1);
        }
      });
    } catch (DatasetNotFoundException x)
    {
      LOGGER.trace("", x);
    }
    return p.getProps();
  }

  @Override
  public String getValue(String dbSpalte)
  {
    // No synchronization with WollMuxEventHandler as reading isn't critical.
    try
    {
      String value = DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDatasetTransformed().get(dbSpalte);
      if (value == null)
        value = "";
      return value;
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
      return "";
    }
  }

  @Deprecated(since = "10.8")
  @Override
  public void addPrintFunction(XTextDocument doc, String functionName)
  {
    XWollMuxDocument wdoc = getWollMuxDocument(doc);
    if (wdoc != null)
      wdoc.removePrintFunction(functionName);
  }

  @Deprecated(since = "10.8")
  @Override
  public void removePrintFunction(XTextDocument doc, String functionName)
  {
    XWollMuxDocument wdoc = getWollMuxDocument(doc);
    if (wdoc != null)
      wdoc.removePrintFunction(functionName);
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
    WollMux.createMenuButton(AboutDispatch.COMMAND, L.m("Info über Vorlagen und Formulare (WollMux)"),
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
