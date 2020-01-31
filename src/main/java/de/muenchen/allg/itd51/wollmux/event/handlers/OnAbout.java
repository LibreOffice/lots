package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;

/**
 * Shows a modal dialog providing important information about WollMux and its configuration.
 */
public class OnAbout extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnAbout.class);
  private static final String IMAGE_URL = "/image/wollmux.jpg";
  private static final String EXTENSION_ID = "de.muenchen.allg.d101.wollmux";

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.about?location=application", "", peer, null);
      XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);

      // allgemeiner Teil
      XFixedText introLabel = UNO.XFixedText(container.getControl("introLabel"));
      introLabel.setText("WollMux " + WollMuxSingleton.getVersion());
      XFixedText copyright = UNO.XFixedText(container.getControl("copyright"));
      copyright.setText("Copyright (c) 2005-2019 Landeshauptstadt München");
      XFixedText license = UNO.XFixedText(container.getControl("license"));
      license.setText("Lizenz: European Union Public License");
      XFixedText homepage = UNO.XFixedText(container.getControl("homepage"));
      homepage.setText("Homepage: www.wollmux.org");
      XControl logo = UnoRuntime.queryInterface(XControl.class, container.getControl("logo"));
      XPropertySet logoProperties = UnoRuntime.queryInterface(XPropertySet.class, logo.getModel());
      XPackageInformationProvider xPackageInformationProvider = PackageInformationProvider.get(UNO.defaultContext);
      String location = xPackageInformationProvider.getPackageLocation(EXTENSION_ID);
      logoProperties.setPropertyValue("ImageURL", location + IMAGE_URL);

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
          .setText("DEFAULT_CONTEXT: " + WollMuxFiles.getDEFAULT_CONTEXT().toExternalForm());

      UnoRuntime.queryInterface(XDialog.class, window).execute();
    } catch (Exception e)
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