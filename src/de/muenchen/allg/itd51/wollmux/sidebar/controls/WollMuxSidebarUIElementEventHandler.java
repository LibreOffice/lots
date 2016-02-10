package de.muenchen.allg.itd51.wollmux.sidebar.controls;

import java.io.IOException;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.dialog.MultiOpenDialog;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementEventHandler;
import de.muenchen.allg.itd51.wollmux.dialog.WollMuxBar;
import de.muenchen.allg.itd51.wollmux.dialog.WollMuxBarEventHandler;
import de.muenchen.allg.itd51.wollmux.dialog.controls.UIElement;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;

/**
 * EventHandler für die WollMux-Sidebar. Der EventHandler behandelt alle WollMux-Aktionen,
 * die von Steuerelementen in der Sidebar ausgelöst werden, z.B. das Öffnen einer
 * WollMux-Vorlage. 
 * 
 */
public class WollMuxSidebarUIElementEventHandler implements UIElementEventHandler
{
  private WollMuxBarEventHandler eventHandler;

  public WollMuxSidebarUIElementEventHandler(WollMuxBarEventHandler eventHandler)
  {
    super();
    this.eventHandler = eventHandler;
  }

  @Override
  public void processUiElementEvent(UIElement source, String eventType, Object[] args)
  {
    if (!eventType.equals("action")) return;

    String action = args[0].toString();
    if (action.equals("absenderAuswaehlen"))
    {
      eventHandler.handleWollMuxUrl(Dispatch.DISP_wmAbsenderAuswaehlen, "");
    }
    else if (action.equals("openDocument"))
    {
      String fragId = getFragId((ConfigThingy) args[1], action);
      if (fragId != null)
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmOpenDocument, fragId);
    }
    else if (action.equals("openTemplate"))
    {
      String fragId = getFragId((ConfigThingy) args[1], action);
      if (fragId != null)
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmOpenTemplate, fragId);
    }
    else if (action.equals("open"))
    {
      showMultiOpenDialog((ConfigThingy) args[1]);
    }
    else if (action.equals("openExt"))
    {
      ConfigThingy conf = (ConfigThingy)args[1];
      ConfigThingy ext = conf.query("EXT");
      ConfigThingy url = conf.query("URL");
      executeOpenExt(ext.toString(), url.toString());
    }
    else if (action.equals("dumpInfo"))
    {
      eventHandler.handleWollMuxUrl(Dispatch.DISP_wmDumpInfo, null);
    }
    else if (action.equals("abort"))
    {
      // abort();
    }
    else if (action.equals("kill"))
    {
      eventHandler.handleWollMuxUrl(Dispatch.DISP_wmKill, null);
      // abort();
    }
    else if (action.equals("about"))
    {
      eventHandler.handleWollMuxUrl(Dispatch.DISP_wmAbout, WollMuxBar.getBuildInfo());
    }
    else if (action.equals("menuManager"))
    {
      // menuManager();
    }
    else if (action.equals("options"))
    {
      // options();
    }
  }
  
  /**
   * Erwartet in conf eine Spezifikation gemäß wollmux:Open und bringt einen
   * Auswahldialog, um die zu öffnenden Vorlagen/Dokumente auszuwählen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void showMultiOpenDialog(final ConfigThingy conf)
  {
    final JFrame multiOpenFrame =
      new MultiOpenDialog(L.m("Was möchten Sie öffnen ?"), conf, eventHandler);
    multiOpenFrame.setVisible(true);
  }
  
  /**
   * Führt die gleichnamige ACTION aus.
   * 
   * TESTED
   */
  private void executeOpenExt(String ext, String url)
  {
    try
    {
      final OpenExt openExt = OpenExt.getInstance(ext, url);

      try
      {
        openExt.storeIfNecessary();
      }
      catch (IOException x)
      {
        Logger.error(x);
        showError(L.m("Fehler beim Download der Datei:\n%1", x.getMessage()));
        return;
      }

      Runnable launch = new Runnable()
      {
        @Override
        public void run()
        {
          openExt.launch(new OpenExt.ExceptionHandler()
          {
            @Override
            public void handle(Exception x)
            {
              Logger.error(x);
              showError(x.getMessage());
            }
          });
        }
      };

      /**
       * Falls /loadComponentFromURL/ bei den Programmen ist, muss ein Kontakt zu OOo
       * hergestellt werden vor dem Launch.
       */
      boolean mustConnectToOOo = false;
      for (String program : openExt.getPrograms())
        if (program.startsWith("/loadComponentFromURL/")) mustConnectToOOo = true;

      if (mustConnectToOOo)
        eventHandler.handleDoWithConnection(launch);
      else
        launch.run();
    }
    catch (Exception x)
    {
      Logger.error(x);
      showError(x.getMessage());
    }
  }

  private String getFragId(ConfigThingy conf, String action)
  {
    ConfigThingy fids = conf.query("FRAG_ID");
    StringBuffer fragId = new StringBuffer();
    if (fids.count() > 0)
    {
      Iterator<ConfigThingy> i = fids.iterator();
      fragId.append(i.next().toString());
      while (i.hasNext())
      {
        fragId.append("&");
        fragId.append(i.next().toString());
      }
    }
    else
    {
      Logger.error(L.m("ACTION \"%1\" erfordert mindestens ein Attribut FRAG_ID",
        action));
    }
    return fragId.toString();
  }
  
  private void showError(String errorMsg)
  {
    JOptionPane.showMessageDialog(null, L.m(
      "%1\nVerständigen Sie Ihre Systemadministration.", errorMsg),
      L.m("Fehlerhafte Konfiguration"), JOptionPane.ERROR_MESSAGE);
  }
}
