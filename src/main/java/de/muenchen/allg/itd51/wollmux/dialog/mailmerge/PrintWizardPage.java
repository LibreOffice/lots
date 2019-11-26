package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractXWizardPage;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;

public class PrintWizardPage extends AbstractXWizardPage
{

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintWizardPage.class);

  private final XTextComponent name;
  private final XButton change;

  private XTextDocument doc;

  public PrintWizardPage(XWindow parentWindow, short pageId, XTextDocument doc) throws Exception
  {
    super(pageId, parentWindow, "seriendruck_printer");
    this.doc = doc;
    XControlContainer container = UnoRuntime.queryInterface(XControlContainer.class, window);
    name = UNO.XTextComponent(container.getControl("name"));
    name.setText(getCurrentPrinterName());
    change = UNO.XButton(container.getControl("change"));
    change.addActionListener(new AbstractActionListener()
    {

      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        // Druckereinstellungen-Dialog anzeigen:
        try
        {
          com.sun.star.util.URL url = UNO.getParsedUNOUrl(Dispatch.DISP_unoPrinterSetup);
          XNotifyingDispatch disp = UNO.XNotifyingDispatch(getDispatchForModel(url));

          if (disp != null)
          {
            disp.dispatchWithNotification(url, new PropertyValue[] {}, new XDispatchResultListener()
            {
              @Override
              public void disposing(EventObject arg0)
              {
                // unused
              }

              @Override
              public void dispatchFinished(DispatchResultEvent arg0)
              {
                name.setText(getCurrentPrinterName());
              }
            });
          }
        } catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
      }
    });
  }

  @Override
  public boolean canAdvance()
  {
    return !name.getText().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    window.setVisible(false);
    return true;
  }

  /**
   * Holt sich den Frame von doc, führt auf diesem ein queryDispatch() mit der zu urlStr gehörenden
   * URL aus und liefert den Ergebnis XDispatch zurück oder null, falls der XDispatch nicht
   * verfügbar ist.
   *
   * @param doc
   *          Das Dokument, dessen Frame für den Dispatch verwendet werden soll.
   * @param urlStr
   *          die URL in Form eines Strings (wird intern zu URL umgewandelt).
   * @return den gefundenen XDispatch oder null, wenn der XDispatch nicht verfügbar ist.
   */
  private XDispatch getDispatchForModel(com.sun.star.util.URL url)
  {
    XDispatchProvider dispProv = null;

    dispProv = UNO.XDispatchProvider(UNO.XModel(doc).getCurrentController().getFrame());

    if (dispProv != null)
    {
      return dispProv.queryDispatch(url, "_self", com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }

  /**
   * Liefert den Namen des aktuell zu diesem Dokument eingestellten Druckers.
   */
  public String getCurrentPrinterName()
  {
    XPrintable printable = UNO.XPrintable(doc);
    PropertyValue[] printer = null;
    if (printable != null)
    {
      printer = printable.getPrinter();
    }
    UnoProps printerInfo = new UnoProps(printer);
    try
    {
      return (String) printerInfo.getPropertyValue("Name");
    } catch (UnknownPropertyException e)
    {
      return L.m("unbekannt");
    }
  }
}
