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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;

/**
 * Shows a modal dialog providing important information about WollMux and its configuration.
 */
public class OnAbout extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnAbout.class);
  private static final String IMAGE_URL = "/image/wollmux.jpg";
  private static final String EXTENSION_ID = Utils.getWollMuxProperties().getProperty("extension.id");

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UNO.XContainerWindowProvider(
          UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));
      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.about?location=application", "", peer, null);
      XControlContainer container = UNO.XControlContainer(window);

      // allgemeiner Teil
      XFixedText introLabel = UNO.XFixedText(container.getControl("introLabel"));
      introLabel.setText("WollMux " + WollMuxSingleton.getVersion());
      XFixedText copyright = UNO.XFixedText(container.getControl("copyright"));
      copyright.setText("Copyright (c) 2005-2019 Landeshauptstadt München");
      XFixedText license = UNO.XFixedText(container.getControl("license"));
      license.setText("Lizenz: European Union Public License");
      XFixedText homepage = UNO.XFixedText(container.getControl("homepage"));
      homepage.setText("Homepage: www.wollmux.org");
      XControl logo = UNO.XControl(container.getControl("logo"));
      XPackageInformationProvider xPackageInformationProvider = PackageInformationProvider.get(UNO.defaultContext);
      String location = xPackageInformationProvider.getPackageLocation(EXTENSION_ID);
      UnoProperty.setProperty(logo.getModel(), UnoProperty.IMAGE_URL, location + IMAGE_URL);

      // Autoren
      XListBox authors = UNO.XListBox(container.getControl("authors"));
      authors.addItems(new String[] { "Matthias S. Benkmann", "Christoph Lutz", "Daniel Benkmann",
          "Bettina Bauer", "Andor Ertsey", "Max Meier" }, (short) 0);

      // Info
      XFixedText wmVersion = UNO.XFixedText(container.getControl("wmVersion"));
      wmVersion.setText("WollMux " + WollMuxSingleton.getVersion());
      XFixedText wmConfig = UNO.XFixedText(container.getControl("wmConfig"));
      wmConfig
          .setText("WollMux-Konfiguration: " + WollMuxSingleton.getInstance().getConfVersionInfo());
      XFixedText defaultContext = UNO.XFixedText(container.getControl("default"));
      defaultContext
          .setText("DEFAULT_CONTEXT: " + WollMuxFiles.getDefaultContext().toExternalForm());

      UNO.XDialog(window).execute();
    } catch (UnoHelperException e)
    {
      LOGGER.error("Info-Dialog konnte nicht angezeigt werden.", e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}
