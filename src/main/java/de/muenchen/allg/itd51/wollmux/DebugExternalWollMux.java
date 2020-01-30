package de.muenchen.allg.itd51.wollmux;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.XSet;
import com.sun.star.frame.TerminationVetoException;
import com.sun.star.frame.XTerminateListener;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XUIElementFactoryRegistration;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.LogConfig;
import de.muenchen.allg.itd51.wollmux.form.sidebar.FormFactory;
import de.muenchen.allg.itd51.wollmux.mailmerge.sidebar.MailMergeFactory;
import de.muenchen.allg.itd51.wollmux.sidebar.WollMuxSidebarFactory;
import de.muenchen.allg.util.UnoComponent;

/**
 * Über diese Klasse kann der WollMux zum Debuggen in der lokalen JVM gestartet
 * werden, ohne dass die Extension in Office installiert ist. Bisher
 * haben wir für diesen Zweck die WollMuxBar mit der Konfigurationsoption
 * ALLOW_EXTERNAL_WOLLMUX "true" verwendet. Damit das Debuggen auch ohne WollMuxBar
 * möglich ist, wurde diese Klasse eingefügt. Zusammen mit dem neuen ant build-target
 * WollMux.oxt-ButtonsOnly kann so das Debugging vereinfacht werden.
 *
 * Verwendung: Diese Main-Methode einfach per Debugger starten.
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
    // Logger zum Debuggen auf stdout initialisieren und die zukünftigen
    // Logger-Einstellungen aus der wollmuxconfig ignorieren.
    LogConfig.init(System.out, Level.DEBUG);
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
        LOGGER.info(L.m("Desktop wurde geschlossen - beende DebugExternalWollMux"));
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
