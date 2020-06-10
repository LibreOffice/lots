/*
 * Dateiname: PrintParametersDialog.java
 * Projekt  : WollMux
 * Funktion : Dialog für Druckeinstellungen
 *
 * Copyright (c) 2008-2019 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 03.05.2008 | LUT | Erstellung
 * 08.08.2006 | BNK | Viel Arbeit reingesteckt.
 * 23.01.2014 | loi | Set Methode hinzugefügt.
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.print;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.AbstractMap.SimpleEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.SpinEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractSpinListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.dialog.adapter.AbstractTopWindowListener;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dispatch.PrintDispatch;
import de.muenchen.allg.util.UnoProperty;

public class PrintParametersDialog
{

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintParametersDialog.class);

  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der Dialog über den
   * Drucken-Knopf geschlossen wird.
   */
  public static final String CMD_SUBMIT = "submit";

  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der Dialog über den
   * Abbrechen oder "X"-Knopf geschlossen wird.
   */
  public static final String CMD_CANCEL = "cancel";

  /**
   * Das aktuelle Dokument.
   */
  private XTextDocument doc;

  /**
   * Soll einen Steuerelemente für die Anzahl der Ausdrucke angezeigt werden?
   */
  private boolean showCopyCount;

  /**
   * Der Listener, der aufgerufen wird, wenn der Dialog geschlossen wird.
   */
  private ActionListener closeActionListener;

  /**
   * Der Bereich, der gedruckt werden soll.
   */
  private PageRangeType currentPageRangeType = PageRangeType.ALL;

  /**
   * Die Anzahl der Ausdrucke.
   */
  private short copyCount = 1;

  /**
   * Die Auswahl der Seiten, die gedruckt werden sollen.
   */
  private String currentPageRangeValue = null;

  /**
   * Die Aktion, die an den closeActionListener übermittelt wird.
   */
  private String action = CMD_CANCEL;

  /**
   * Der Container mit den Steuerelementen.
   */
  private XControlContainer container;

  /**
   * Erzeugt die GUI für die Einstellungen des Druckers.
   *
   * @param doc
   *          Das aktuelle Dokument.
   * @param showCopyCount
   *          Soll eine Anzeige für die Anzahl der Ausdrucke enthalten sein.
   * @param listener
   *          Der Listener, der aufgerufen wird, wenn der Dialog geschlossen wird.
   */
  public PrintParametersDialog(XTextDocument doc, boolean showCopyCount, ActionListener listener)
  {
    this.doc = doc;
    this.showCopyCount = showCopyCount;
    this.closeActionListener = listener;
    createGUI();
  }

  /**
   * Mini-Klasse für den Rückgabewert eines Seitenbereichs.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class PageRange
  {
    public final PageRangeType pageRangeType;

    public final String pageRangeValue;

    public PageRange(PageRangeType pageRangeType, String pageRangeValue)
    {
      this.pageRangeType = pageRangeType;
      this.pageRangeValue = pageRangeValue;
    }

    @Override
    public String toString()
    {
      return "PageRange(" + pageRangeType + ", '" + pageRangeValue + "')";
    }
  }

  /**
   * Definiert die in diesem Dialog möglichen Einstellungen zur Auswahl des Seitenbereichs.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public enum PageRangeType
  {
    ALL,
    USER_DEFINED,
    CURRENT_PAGE,
    CURRENT_AND_FOLLOWING;
  }

  /**
   * Liefert ein PageRange-Objekt zurück, das Informationen über den aktuell ausgewählten
   * Druckbereich enthält.
   */
  public PageRange getPageRange()
  {
    return new PageRange(currentPageRangeType, currentPageRangeValue);
  }

  /**
   * Liefert die Anzahl in der GUI eingestellter Kopien als Short zurück; Zeigt der Dialog kein
   * Elemente zur Eingabe der Kopien an, oder ist die Eingabe keine gültige Zahl, so wird new
   * Short((short) 1) zurück geliefert.
   */
  private Short getCopyCount()
  {
    return this.copyCount;
  }

  /**
   * Initialisiert die GUI.
   */
  private void createGUI()
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.print_parameter?location=application", "", peer, null);
      container = UNO.XControlContainer(window);
      XDialog dialog = UNO.XDialog(window);
      UNO.XTopWindow(dialog).addTopWindowListener(new AbstractTopWindowListener()
      {
        @Override
        public void windowClosed(EventObject arg0)
        {
          SimpleEntry<Short, PageRange> data = new SimpleEntry<>(getCopyCount(), getPageRange());
          if (closeActionListener != null)
          {
            closeActionListener.actionPerformed(new ActionEvent(data, 0, action));
          }
        }
      });

      XFixedText printer = UNO.XFixedText(container.getControl("printer"));
      printer.setText(getCurrentPrinterName(this.doc));
      XButton changePrinter = UNO.XButton(container.getControl("change"));
      AbstractActionListener changePrinterListener = event -> showPrintSettingsDialog();
      changePrinter.addActionListener(changePrinterListener);

      XRadioButton all = UNO.XRadio(container.getControl("all"));
      AbstractItemListener allListener = event -> currentPageRangeType = PageRangeType.ALL;
      all.addItemListener(allListener);
      XRadioButton pages = UNO.XRadio(container.getControl("pages"));
      AbstractItemListener pagesListener = event -> currentPageRangeType = PageRangeType.USER_DEFINED;
      pages.addItemListener(pagesListener);
      XTextComponent pageText = UNO.XTextComponent(container.getControl("pageText"));
      AbstractTextListener additionalTextFieldListener = event -> currentPageRangeValue = pageText
          .getText();
      pageText.addTextListener(additionalTextFieldListener);
      XRadioButton current = UNO.XRadio(container.getControl("current"));
      AbstractItemListener currentListener = event -> currentPageRangeType = PageRangeType.CURRENT_PAGE;
      current.addItemListener(currentListener);
      XRadioButton tillEnd = UNO.XRadio(container.getControl("tillEnd"));
      AbstractItemListener tillEndListener = event -> currentPageRangeType = PageRangeType.CURRENT_AND_FOLLOWING;
      tillEnd.addItemListener(tillEndListener);

      XNumericField count = UNO.XNumericField(container.getControl("count"));
      AbstractTextListener countListener = event -> copyCount = (short) count.getValue();
      UNO.XTextComponent(count).addTextListener(countListener);
      UNO.XSpinField(count).addSpinListener(printCountSpinFieldListener);
      UNO.XWindow(count).setVisible(showCopyCount);
      UNO.XWindow(container.getControl("countFrame")).setVisible(showCopyCount);
      UNO.XWindow(container.getControl("countLabel")).setVisible(showCopyCount);

      XButton abort = UNO.XButton(container.getControl("abort"));
      AbstractActionListener abortListener = event -> {
        action = CMD_CANCEL;
        dialog.endExecute();
      };
      abort.addActionListener(abortListener);

      XButton print = UNO.XButton(container.getControl("print"));
      AbstractActionListener printListener = event -> {
        action = CMD_SUBMIT;
        dialog.endExecute();
      };
      print.addActionListener(printListener);

      dialog.execute();
    } catch (Exception e)
    {
      LOGGER.error("SLV-Druck-Dialog konnte nicht angezeigt werden.", e);
    }
  }

  /**
   * Ruft den printSettings-Dialog auf.
   */
  private void showPrintSettingsDialog()
  {
    // Druckereinstellungen-Dialog anzeigen:
    try
    {
      com.sun.star.util.URL url = UNO.getParsedUNOUrl(PrintDispatch.COMMAND_PRINTER_SETUP);
      XNotifyingDispatch disp = UNO.XNotifyingDispatch(getDispatchForModel(UNO.XModel(doc), url));

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
            UNO.XFixedText(container.getControl("printer")).setText(getCurrentPrinterName(doc));
          }
        });
      }
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
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
  private XDispatch getDispatchForModel(XModel doc, com.sun.star.util.URL url)
  {
    if (doc == null)
    {
      return null;
    }

    XDispatchProvider dispProv = null;
    dispProv = UNO.XDispatchProvider(doc.getCurrentController().getFrame());

    if (dispProv != null)
    {
      return dispProv.queryDispatch(url, "_self", com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }

  /**
   * Der Listener für das SpinControl für die Anzahl der Dokumente.
   */
  private AbstractSpinListener printCountSpinFieldListener = new AbstractSpinListener()
  {

    @Override
    public void up(SpinEvent event)
    {
      getAndSetCopyCount(event);
    }

    @Override
    public void down(SpinEvent event)
    {
      getAndSetCopyCount(event);
    }

    private void getAndSetCopyCount(SpinEvent event)
    {
      XNumericField copyCountControl = UNO.XNumericField(event.Source);

      if (copyCountControl == null)
        return;

      copyCount = (short) copyCountControl.getValue();
    }
  };

  /**
   * Liefert den Namen des aktuell zu diesem Dokument eingestellten Druckers.
   */
  public static String getCurrentPrinterName(XTextDocument doc)
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
      return (String) printerInfo.getPropertyValue(UnoProperty.NAME);
    } catch (UnknownPropertyException e)
    {
      return L.m("unbekannt");
    }
  }

  /**
   * Setzt den Namen des aktuell zu diesem Dokument eingestellten Druckers.
   */
  public static void setCurrentPrinterName(XTextDocument doc, String druckerName)
  {
    XPrintable printable = UNO.XPrintable(doc);
    PropertyValue[] printer = null;
    UnoProps printerInfo = new UnoProps(printer);
    try
    {
      printerInfo.setPropertyValue(UnoProperty.NAME, druckerName);
      if (printable != null)
      {
        printable.setPrinter(printerInfo.getProps());
      }
    } catch (IllegalArgumentException e)
    {
      LOGGER.trace("property setzen: {}", e.getMessage());
    }
  }
}
