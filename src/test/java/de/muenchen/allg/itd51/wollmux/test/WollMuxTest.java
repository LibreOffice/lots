package de.muenchen.allg.itd51.wollmux.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.sun.star.document.EventObject;
import com.sun.star.document.XEventListener;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.XWollMux;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.util.UnoComponent;

public abstract class WollMuxTest extends OfficeTest
{

  protected static XWollMux wollmux;

  @BeforeAll
  public static void initWollMux() throws Exception
  {
    wollmux = UnoRuntime.queryInterface(XWollMux.class,
        UnoComponent.createComponentWithContext("de.muenchen.allg.itd51.wollmux.WollMux"));
  }

  @AfterAll
  public static void dumpJacoco() throws Exception
  {
    try (FileOutputStream localFile = new FileOutputStream("/srv/WollMux/WollMux/target/jacoco-office.exec",
        true);
        Socket socket = new Socket(InetAddress.getByName("localhost"), 6300);)
    {
      ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);
      RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
      RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
      reader.setSessionInfoVisitor(localWriter);
      reader.setExecutionDataVisitor(localWriter);

      writer.visitDumpCommand(true, false);
      if (!reader.read())
      {
        throw new IOException("Socket closed unexpectedly.");
      }
    }
  }

  /**
   * Load a component as hidden document without macros.
   *
   * @param filename
   *          The name of the component to load.
   * @return A future to be completed with a component as soon as WollMux processed the file.
   * @throws UnoHelperException
   *           Component can't be loaded.
   */
  public static CompletableFuture<XComponent> loadAsyncComponent(String filename) throws UnoHelperException
  {
    CompletableFuture<XComponent> future = new CompletableFuture<>();
    wollmux.addEventListener(new XEventListener()
    {

      @Override
      public void disposing(com.sun.star.lang.EventObject arg0)
      {
        // nothing to do
      }

      @Override
      public void notifyEvent(EventObject event)
      {
        if (WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED.equals(event.EventName))
        {
          future.complete(UNO.XComponent(event.Source));
        }
      }
    });
    OfficeTest.loadComponent(filename);
    return future;
  }

}
