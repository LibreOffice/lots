/*
 * Dateiname: PALChangeEventTest.java
 * Projekt  : WollMux
 * Funktion : Programm zum Testen der Registrierung eines XPALChangeEventListeners.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330/5980
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class PALChangeEventTest implements XPALChangeEventListener
{

  public static void main(String[] args) throws BootstrapException, Exception
  {
    XComponentContext ctx = Bootstrap.bootstrap();

    new PALChangeEventTest(ctx);
  }

  public PALChangeEventTest(XComponentContext ctx) throws Exception
  {
    XPALChangeEventBroadcaster mux =
      (XPALChangeEventBroadcaster) UnoRuntime.queryInterface(
        XPALChangeEventBroadcaster.class,
        ctx.getServiceManager().createInstanceWithContext(
          "de.muenchen.allg.itd51.wollmux.WollMux", ctx));
    if (mux != null)
    {
      mux.addPALChangeEventListener(this);
    }
  }

  public void updateContent(EventObject event)
  {
    System.out.println("updateContent:");
    XPALProvider palProv =
      (XPALProvider) UnoRuntime.queryInterface(XPALProvider.class, event.Source);
    if (palProv != null)
    {
      String sender = palProv.getCurrentSender();

      String[] entries = palProv.getPALEntries();
      for (int i = 0; i < entries.length; i++)
      {
        System.out.println("  " + entries[i]
          + (sender.equals(entries[i]) ? " <==" : ""));

      }
    }

  }

  public void updateContentForFrame(XFrame arg0)
  {
    System.out.println("updateContentForFrame");
  }

  public void disposing(EventObject arg0)
  {
    System.out.println("disposing");
  }

  public int getWollMuxConfHashCode()
  {
    // Keine Konsistenzprüfung im Testfall
    return 0;
  }

}
