package de.muenchen.allg.itd51.wollmux;

import com.sun.star.bridge.UnoUrlResolver;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.comp.WollMux;

/**
 * Über diese Klasse kann der WollMux zum Debuggen in der lokalen JVM gestartet
 * werden, ohne dass die Extension in OpenOffice/LibreOffice installiert ist. Bisher
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
public class DebugRemoteWollMux
{
  public static void main(String[] args) throws Exception
  {

    UNO.init();

    new WollMux(UNO.defaultContext);
  }
}
