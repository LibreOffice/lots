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
    XPALChangeEventBroadcaster mux = (XPALChangeEventBroadcaster) UnoRuntime
        .queryInterface(XPALChangeEventBroadcaster.class, ctx
            .getServiceManager().createInstanceWithContext(
                "de.muenchen.allg.itd51.wollmux.WollMux",
                ctx));
    if (mux != null)
    {
      mux.addPALChangeEventListener(this);
    }
  }

  public void updateContent(EventObject event)
  {
    System.out.println("updateContent:");
    XPALProvider palProv = (XPALProvider) UnoRuntime.queryInterface(
        XPALProvider.class,
        event.Source);
    if (palProv != null)
    {
      String sender = palProv.getCurrentSender();

      String[] entries = palProv.getPALEntries();
      for (int i = 0; i < entries.length; i++)
      {
        System.out.println("  "
                           + entries[i]
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

}
