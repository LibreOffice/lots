/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.ds;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.ui.GuiFactory;
import de.muenchen.allg.itd51.wollmux.ui.layout.Layout;
import de.muenchen.allg.itd51.wollmux.ui.layout.VerticalLayout;

/**
 * A dialog for selecting a data source which is registered in LibreOffice as database.
 */
public class DBDatasourceDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(DBDatasourceDialog.class);

  /**
   * Start the dialog.
   *
   * @param listener
   *          The listener is called when the dialog is closed.
   */
  public DBDatasourceDialog(Consumer<String> listener)
  {
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
    XContainerWindowProvider provider = null;

    try
    {
      provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider", UNO.defaultContext));
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }

    if (provider == null)
      return;

    XWindow window = provider
        .createContainerWindow("vnd.sun.star.script:WollMux.DBDatasourceDialog?location=application", "", peer, null);
    XControlContainer controlContainer = UNO.XControlContainer(window);
    XDialog dialog = UNO.XDialog(window);
    Layout layout = new VerticalLayout(20, 10, 20, 20, 15);

    AbstractActionListener oooDSActionListener = event -> {
      listener.accept(event.ActionCommand);
      dialog.endExecute();
    };

    for (String ds : getRegisteredDatabaseNames())
    {
      XControl control = GuiFactory.createButton(UNO.xMCF, UNO.defaultContext, ds, oooDSActionListener,
          new Rectangle(0, 0, 200, 30), null);
      UNO.XButton(control).setActionCommand(ds);
      layout.addControl(control);
      controlContainer.addControl(ds, control);
    }

    Pair<Integer, Integer> newSize = layout.layout(UNO.XWindow2(window).getPosSize());
    window.setPosSize(0, 0, newSize.getRight(), newSize.getLeft(), PosSize.SIZE);
    dialog.execute();
  }

  /**
   * Returns the names of all data sources registered in OOo.
   *
   * @return List of all registered data sources.
   */
  private Set<String> getRegisteredDatabaseNames()
  {
    try
    {
      return UnoDictionary.create(UNO.dbContext, Object.class).keySet();
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
    return Collections.emptySet();
  }
}
