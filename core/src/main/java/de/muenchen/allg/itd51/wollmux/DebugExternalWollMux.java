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
package de.muenchen.allg.itd51.wollmux;

import java.io.OutputStreamWriter;

import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.XSet;
import com.sun.star.frame.TerminationVetoException;
import com.sun.star.frame.XTerminateListener;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XUIElementFactoryRegistration;
import com.sun.star.uno.UnoRuntime;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;
import de.muenchen.allg.itd51.wollmux.form.sidebar.FormFactory;
import de.muenchen.allg.itd51.wollmux.mailmerge.sidebar.MailMergeFactory;
import de.muenchen.allg.itd51.wollmux.sidebar.WollMuxSidebarFactory;
import de.muenchen.allg.itd51.wollmux.util.LogConfig;
import org.libreoffice.ext.unohelper.util.UnoComponent;

/**
 * With this class, WollMux can be started for debugging in the local JVM
 * without installing the extension.
 *
 * The debugging is also possible with the WollMuxBar with the configuration
 * option ALLOW_EXTERNAL_WOLLMUX set to "true".
 * This class is helpful for making the debugging possible without WollMuxBar.
 * The debuging can be simpliefied together with the new ant build-target
 * WollMux.oxt-ButtonsOnly.
 *
 * Usage: Simply run this main method via debugger.
 *
 * @author Christoph
 *
 */
public class DebugExternalWollMux
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DebugExternalWollMux.class);

  /**
   * Start LibreOffice, register WollMux extension and open a text document.
   *
   * @param args
   *          Program arguments (unused).
   * @throws Exception
   *           Something went wrong.
   */
  @SuppressWarnings("java:S106")
  public static void main(String[] args) throws Exception
  {
    // Initialize logger to stdout for debugging and ignore any future
    // logger settings from wollmuxconfig.
    LogConfig.init(new OutputStreamWriter(System.out), Level.DEBUG);
    LogConfig.setIgnoreInit(true);

    UNO.init();

    UNO.desktop.addTerminateListener(new XTerminateListener()
    {

      @Override
      public void disposing(EventObject arg0)
      {
	// nothing to do
      }

      @Override
      public void queryTermination(EventObject arg0) throws TerminationVetoException
      {
	// nothing to do
      }

      @Override
      public void notifyTermination(EventObject arg0)
      {
        LOGGER.info("Desktop environment was closed - finish DebugExternalWollMux");
        System.exit(0);
      }
    });

    XUIElementFactoryRegistration factoryRegistration = UnoRuntime.queryInterface(XUIElementFactoryRegistration.class,
        UnoComponent.createComponentWithContext(
            UnoComponent.CSS_UI_UI_ELEMENT_FACTORY_MANAGER,
            UNO.defaultContext.getServiceManager(), UNO.defaultContext));

    String interfaceType = "toolpanel";
    factoryRegistration.registerFactory(interfaceType, "WollMuxSidebarFactory", null,
        WollMuxSidebarFactory.SERVICE_NAME);

    factoryRegistration.registerFactory(interfaceType, "SeriendruckSidebarFactory", null,
        MailMergeFactory.SERVICE_NAME);

    factoryRegistration.registerFactory(interfaceType, "FormFactory", null, FormFactory.SERVICE_NAME);

    XSet set = UNO.XSet(UNO.defaultContext.getServiceManager());
    set.insert(ComponentRegistration.__getComponentFactory(WollMux.class.getName()));
    set.insert(ComponentRegistration
        .__getComponentFactory(WollMuxSidebarFactory.class.getName()));
    set.insert(ComponentRegistration
        .__getComponentFactory(MailMergeFactory.class.getName()));
    set.insert(ComponentRegistration.__getComponentFactory(FormFactory.class.getName()));

    new WollMux(UNO.defaultContext);

    UNO.loadComponentFromURL("private:factory/swriter", false, true);
  }
}
