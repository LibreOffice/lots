/*
 * Dateiname: PrintParametersDialog.java
 * Projekt  : WollMux
 * Funktion : Dialog für Druckeinstellungen
 *
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.SpinEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XSpinField;
import com.sun.star.awt.XSpinListener;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XTextListener;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.constants.XButtonProperties;
import de.muenchen.allg.itd51.wollmux.core.constants.XLabelProperties;
import de.muenchen.allg.itd51.wollmux.core.constants.XNumericFieldProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;

public class PrintParametersDialog
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PrintParametersDialog.class);

  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der
   * Dialog über den Drucken-Knopf geschlossen wird.
   */
  public static final String CMD_SUBMIT = "submit";

  /**
   * Kommando-String, der dem closeActionListener übermittelt wird, wenn der
   * Dialog über den Abbrechen oder "X"-Knopf geschlossen wird.
   */
  public static final String CMD_CANCEL = "cancel";

  private XTextDocument doc;

  private boolean showCopyCount;

  private ActionListener closeActionListener;

  private PageRangeType currentPageRangeType = null;
  
  private short copyCount = 1;

  private String currentPageRangeValue = null;

  public PrintParametersDialog(XTextDocument doc, boolean showCopyCount,
      ActionListener listener)
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
   * Definiert die in diesem Dialog möglichen Einstellungen zur Auswahl des
   * Seitenbereichs.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static enum PageRangeType
  {
    ALL(L.m("Alles")),

    USER_DEFINED(
        L.m("Seiten"),
        "1,3,5,10-100<etwasPlatz>",
        L.m("Mögliche Eingaben sind z.B. '1', '2-5' oder '1,3,5'")),

    CURRENT_PAGE(L.m("Aktuelle Seite")),

    CURRENT_AND_FOLLOWING(L.m("Aktuelle Seite bis Dokumentende"));

    public final String label;

    public final boolean hasAdditionalTextField;

    public final String additionalTextFieldPrototypeDisplayValue;

    public final String additionalTextFieldHint;

    private PageRangeType(String label)
    {
      this.label = label;
      this.hasAdditionalTextField = false;
      this.additionalTextFieldPrototypeDisplayValue = null;
      this.additionalTextFieldHint = "";
    }

    private PageRangeType(String label,
        String additionalTextFieldPrototypeDisplayValue,
        String additionalTextFieldHint)
    {
      this.label = label;
      this.hasAdditionalTextField = true;
      this.additionalTextFieldPrototypeDisplayValue = additionalTextFieldPrototypeDisplayValue;
      this.additionalTextFieldHint = additionalTextFieldHint;
    }
  };

  /**
   * Liefert ein PageRange-Objekt zurück, das Informationen über den aktuell
   * ausgewählten Druckbereich enthält.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public PageRange getPageRange()
  {
    return new PageRange(currentPageRangeType, currentPageRangeValue);
  }

  /**
   * Liefert die Anzahl in der GUI eingestellter Kopien als Short zurück; Zeigt
   * der Dialog kein Elemente zur Eingabe der Kopien an, oder ist die Eingabe
   * keine gültige Zahl, so wird new Short((short) 1) zurück geliefert.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private Short getCopyCount()
  {
    return this.copyCount;
  }

  private UNODialogFactory dialogFactory;
  private SimpleDialogLayout layout;

  private void createGUI()
  {
    this.dialogFactory = new UNODialogFactory();

    XWindow dialogWindow = this.dialogFactory.createDialog(600, 300, 0xF2F2F2);

    layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    // Bereiche
    List<ControlModel> printRangeControls = new ArrayList<>();
    List<SimpleEntry<ControlProperties, XControl>> radioControls = null;

    for (int i = 0; i < PageRangeType.values().length; i++)
    {
      PageRangeType pageRangeType = PageRangeType.values()[i];
      radioControls = new ArrayList<>();
      SimpleEntry<ControlProperties, XControl> radioButton = layout
          .convertToXControl(new ControlProperties(ControlType.RADIO,
              "radioSectionRadioButton" + i, 0, 30, 60, 0,
              new SimpleEntry<String[], Object[]>(
                  new String[] { XButtonProperties.LABEL },
                  new Object[] { pageRangeType.label })));
      // zusätzlich zu controlModel.marginLeft etwas einrücken da um diese
      // Controls später eine GroupBox gezeichnet wird
      radioButton.getKey().setMarginLeft(40);
      
      // ersten RadioButton aktivieren
      XRadioButton xRadioButton = UnoRuntime.queryInterface(XRadioButton.class, radioButton.getValue());
      if (i == 0) {
    	  xRadioButton.setState(true);
    	  this.currentPageRangeType = pageRangeType;
    	  this.currentPageRangeValue = "1-9999";
      }
      
      xRadioButton.addItemListener(radioButtonListener);

      radioControls.add(radioButton);

      if (pageRangeType.hasAdditionalTextField)
      {
        SimpleEntry<ControlProperties, XControl> addtionalTextfield = layout
            .convertToXControl(new ControlProperties(ControlType.EDIT,
                "radioSectionTextField" + i, 0, 30, 20, 0,
                new SimpleEntry<String[], Object[]>(
                    new String[] { XButtonProperties.LABEL }, new Object[] {
                    		pageRangeType.additionalTextFieldPrototypeDisplayValue })));
        UnoRuntime.queryInterface(XTextComponent.class, addtionalTextfield.getValue()).addTextListener(additionalTextFieldListener);

        radioControls.add(addtionalTextfield);
      }

      ControlModel printRangeModel = new ControlModel(Orientation.HORIZONTAL,
          Align.NONE, radioControls, Optional.empty());
      printRangeControls.add(printRangeModel);
    }

    // Header
    ControlModel headerModel = this.addHeader();
    headerModel.setLinebreakHeight(0);
    layout.addControlsToList(headerModel);
    // printer info / settings
    layout.addControlsToList(addPrinterInfoModel());
    
    if (showCopyCount) {
      layout.addControlsToList(addCopyCount());
    }
    
    // GroupBox um 'Bereiche' zeichnen
    layout.addControlsToList(addGroupBox(printRangeControls));

    for (ControlModel model : printRangeControls)
    {
      layout.addControlsToList(model);
    }

    // Buttons "Abbrechen" "Drucken"
    layout.addControlsToList(addBottomControlButtons());

    this.dialogFactory.showDialog();

  }
  
  private ControlModel addCopyCount() {
    List<SimpleEntry<ControlProperties, XControl>> copyCountControls = new ArrayList<>();
    
    SimpleEntry<ControlProperties, XControl> labelExemplare = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL,
            "labelExemplare", 0, 30, 80, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XLabelProperties.LABEL },
                new Object[] { "Exemplare  " })));
    
    SimpleEntry<ControlProperties, XControl> printCountField = layout
        .convertToXControl(new ControlProperties(ControlType.NUMERIC_FIELD,
            "printCountField", 0, 30, 20, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XNumericFieldProperties.BORDER,
                    XNumericFieldProperties.BORDER_COLOR,
                    XNumericFieldProperties.LABEL,
                    XNumericFieldProperties.SPIN,
                    XNumericFieldProperties.VALUE,
                    XNumericFieldProperties.MIN_VALUE,
                    XNumericFieldProperties.DECIMAL_ACCURACY },
                new Object[] { (short) 2, 666666, "Kopien", Boolean.TRUE, 1, 0,
                    (short) 0 })));
    
    UnoRuntime.queryInterface(XSpinField.class, printCountField.getValue()).addSpinListener(printCountSpinFieldListener);
    
    copyCountControls.add(labelExemplare);
    copyCountControls.add(printCountField);
    
    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, copyCountControls,
        Optional.empty());
  }

  private ControlModel addHeader()
  {
    List<SimpleEntry<ControlProperties, XControl>> headerLabel = new ArrayList<>();
    SimpleEntry<ControlProperties, XControl> printerDialogLabel = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL,
            "printerDialogLabel", 0, 20, 100, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XLabelProperties.LABEL },
                new Object[] { "Drucker" })));
    printerDialogLabel.getKey().setMarginBetweenControls(0);
    
    SimpleEntry<ControlProperties, XControl> hLine = layout
        .convertToXControl(new ControlProperties(ControlType.FIXEDLINE,
            "printerDialogLabelHLine", 0, 5, 100, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { },
                new Object[] { })));
    hLine.getKey().setMarginBetweenControls(0);
    
    headerLabel.add(printerDialogLabel);
    headerLabel.add(hLine);

    return new ControlModel(Orientation.VERTICAL, Align.NONE, headerLabel,
        Optional.empty());
  }

  private ControlModel addPrinterInfoModel()
  {
    List<SimpleEntry<ControlProperties, XControl>> printerSettings = new ArrayList<>();

    SimpleEntry<ControlProperties, XControl> printerSettingsLabel = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL,
            "printerSettingsLabel", 0, 30, 20, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XLabelProperties.LABEL },
                new Object[] { "Name" })));

    SimpleEntry<ControlProperties, XControl> printerSettingsPrintModel = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL,
            "printerSettingsPrintModel", 0, 30, 30, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XLabelProperties.LABEL },
                new Object[] { getCurrentPrinterName(this.doc) })));

    SimpleEntry<ControlProperties, XControl> printerSettingsSelectPrinterButton = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON,
            "printerSettingsSelectPrinterButton", 0, 30, 50, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XButtonProperties.LABEL },
                new Object[] { "Drucker wechseln / einrichten" })));

    XButton selectPrinterButton = UnoRuntime.queryInterface(XButton.class, printerSettingsSelectPrinterButton.getValue());
    selectPrinterButton.setActionCommand("selectPrinter");
    selectPrinterButton.addActionListener(selectPrinterActionListener);

    printerSettings.add(printerSettingsLabel);
    printerSettings.add(printerSettingsPrintModel);
    printerSettings.add(printerSettingsSelectPrinterButton);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, printerSettings,
        Optional.empty());
  }

  private ControlModel addGroupBox(List<ControlModel> printRangeControls)
  {
    int groupBoxHeight = layout
        .calcGroupBoxHeightByControlProperties(printRangeControls);
    List<SimpleEntry<ControlProperties, XControl>> groupBoxRadioModel = new ArrayList<>();
    SimpleEntry<ControlProperties, XControl> groupBox = layout
        .convertToXControl(new ControlProperties(ControlType.GROUPBOX,
            "groupBoxRadio", 0, groupBoxHeight, 100, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XButtonProperties.LABEL },
                new Object[] { "Bereiche" })));

    groupBoxRadioModel.add(groupBox);

    ControlModel groupBoxModel = new ControlModel(Orientation.HORIZONTAL,
        Align.NONE, groupBoxRadioModel, Optional.empty());
    groupBoxModel.setLinebreakHeight(10);

    return groupBoxModel;
  }

  private ControlModel addBottomControlButtons()
  {
    List<SimpleEntry<ControlProperties, XControl>> bottomButtonsSection = new ArrayList<>();
    SimpleEntry<ControlProperties, XControl> bottomButtonAbort = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON,
            "printerDialogCancel", 0, 30, 50, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XButtonProperties.LABEL },
                new Object[] { "Abbrechen" })));

    XButton abortButton = UnoRuntime.queryInterface(XButton.class, bottomButtonAbort.getValue());
    abortButton.setActionCommand("abort");
    abortButton.addActionListener(abortListener);

    SimpleEntry<ControlProperties, XControl> bottomButtonPrint = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON,
            "printerDialogPrint", 0, 30, 50, 0,
            new SimpleEntry<String[], Object[]>(
                new String[] { XButtonProperties.LABEL },
                new Object[] { "Drucken" })));
    
    XButton printButton = UnoRuntime.queryInterface(XButton.class, bottomButtonPrint.getValue());
    printButton.setActionCommand("print");
    printButton.addActionListener(printListener);

    bottomButtonsSection.add(bottomButtonAbort);
    bottomButtonsSection.add(bottomButtonPrint);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE,
        bottomButtonsSection, Optional.empty());
  }

  private XActionListener printListener = new XActionListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused

    }

    @Override
    public void actionPerformed(com.sun.star.awt.ActionEvent arg0)
    {
      printButtonPressed();
    }
  };

  private XActionListener abortListener = new XActionListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused

    }

    @Override
    public void actionPerformed(com.sun.star.awt.ActionEvent arg0)
    {
      dialogFactory.closeDialog();
    }
  };

  private XActionListener selectPrinterActionListener = new XActionListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused
    }

    @Override
    public void actionPerformed(com.sun.star.awt.ActionEvent arg0)
    {
      showPrintSettingsDialog();
    }
    
    /**
     * Ruft den printSettings-Dialog auf.
     *
     * @author christoph.lutz
     */
    private void showPrintSettingsDialog()
    {
      Thread t = new Thread()
      {
        @Override
        public void run()
        {
          // Druckereinstellungen-Dialog anzeigen:
          try
          {
            com.sun.star.util.URL url = UNO
                .getParsedUNOUrl(Dispatch.DISP_unoPrinterSetup);
            XNotifyingDispatch disp = UNO
                .XNotifyingDispatch(getDispatchForModel(UNO.XModel(doc), url));

            if (disp != null)
            {
              disp.dispatchWithNotification(url, new PropertyValue[] {},
                  new XDispatchResultListener()
                  {
                    @Override
                    public void disposing(EventObject arg0)
                    {
                      // unused
                    }

                    @Override
                    public void dispatchFinished(DispatchResultEvent arg0)
                    {
                      SwingUtilities.invokeLater(new Runnable()
                      {
                        @Override
                        public void run()
                        {
                          XFixedText printerNameLabel = UnoRuntime.queryInterface(
                              XFixedText.class,
                              layout.getControl("printerSettingsPrintModel"));

                          if (printerNameLabel == null)
                            return;

                          printerNameLabel
                              .setText(" " + getCurrentPrinterName(doc) + " ");
                        }
                      });
                    }
                  });
            }
          }
          catch (java.lang.Exception e)
          {
            LOGGER.error("", e);
          }
        }
      };
      t.setDaemon(false);
      t.start();
    }
    
    /**
     * Holt sich den Frame von doc, führt auf diesem ein queryDispatch() mit der
     * zu urlStr gehörenden URL aus und liefert den Ergebnis XDispatch zurück oder
     * null, falls der XDispatch nicht verfügbar ist.
     *
     * @param doc
     *          Das Dokument, dessen Frame für den Dispatch verwendet werden soll.
     * @param urlStr
     *          die URL in Form eines Strings (wird intern zu URL umgewandelt).
     * @return den gefundenen XDispatch oder null, wenn der XDispatch nicht
     *         verfügbar ist.
     */
    private XDispatch getDispatchForModel(XModel doc, com.sun.star.util.URL url)
    {
      if (doc == null)
      {
        return null;
      }

      XDispatchProvider dispProv = null;
      try
      {
        dispProv = UNO.XDispatchProvider(doc.getCurrentController().getFrame());
      }
      catch (Exception e)
      {
      }

      if (dispProv != null)
      {
        return dispProv.queryDispatch(url, "_self",
            com.sun.star.frame.FrameSearchFlag.SELF);
      }
      return null;
    }
  };

  protected void abort(String commandStr)
  {
    SimpleEntry<Short, PageRange> data = new SimpleEntry<>(this.getCopyCount(), this.getPageRange());
    
    if (closeActionListener != null) {
      dialogFactory.closeDialog();
      closeActionListener.actionPerformed(new ActionEvent(data, 0, commandStr));
    }
  }

  protected void printButtonPressed()
  {
    abort(CMD_SUBMIT);
  }
  
  
  private XSpinListener printCountSpinFieldListener = new XSpinListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused
    }

    @Override
    public void up(SpinEvent arg0)
    {
      XNumericField copyCountControl = UnoRuntime.queryInterface(XNumericField.class, arg0.Source);
      
      if (copyCountControl == null)
	return;
      
      setCopyCount((short) copyCountControl.getValue());
    }

    @Override
    public void last(SpinEvent arg0)
    {
      // unused
    }

    @Override
    public void first(SpinEvent arg0)
    {
      // unused

    }

    @Override
    public void down(SpinEvent arg0)
    {
      XNumericField copyCountControl = UnoRuntime.queryInterface(XNumericField.class, arg0.Source);

      if (copyCountControl == null)
	return;
      
      setCopyCount((short) copyCountControl.getValue());
    }
    
    private void setCopyCount(short count) {
      copyCount = count;
    }
  };

  /**
   * Liefert den Namen des aktuell zu diesem Dokument eingestellten Druckers.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
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
      return (String) printerInfo.getPropertyValue("Name");
    }
    catch (UnknownPropertyException e)
    {
      return L.m("unbekannt");
    }
  }

  /**
   * Setzt den Namen des aktuell zu diesem Dokument eingestellten Druckers.
   *
   * @author Judith Baur, Simona Loi
   */
  public static void setCurrentPrinterName(XTextDocument doc,
      String druckerName)
  {
    XPrintable printable = UNO.XPrintable(doc);
    PropertyValue[] printer = null;
    UnoProps printerInfo = new UnoProps(printer);
    try
    {
      printerInfo.setPropertyValue("Name", druckerName);
      if (printable != null)
      {
        printable.setPrinter(printerInfo.getProps());
      }
    }
    catch (Exception e)
    {
      System.out.println("property setzen: " + e.getMessage());
    }
  }
  
  private XTextListener additionalTextFieldListener = new XTextListener()
  {
    
    @Override
    public void disposing(EventObject arg0)
    {

    }
    
    @Override
    public void textChanged(TextEvent arg0)
    {
      XTextComponent xTextComponent = UnoRuntime.queryInterface(XTextComponent.class, arg0.Source);
      
      if (xTextComponent == null)
        return;
      
      currentPageRangeValue = xTextComponent.getText();
    }
  };

  private XItemListener radioButtonListener = new XItemListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused

    }

    @Override
    public void itemStateChanged(ItemEvent arg0)
    {
      XControl xControl = UnoRuntime.queryInterface(XControl.class,
          arg0.Source);

      for (XControl control : layout.getControls())
      {
        if (!control.equals(xControl))
        {
          XRadioButton radioButton = UnoRuntime
              .queryInterface(XRadioButton.class, control);

          if (radioButton == null)
            continue;
          
          radioButton.setState(false);
          
        } else {
          XControl radioButton = UnoRuntime
              .queryInterface(XControl.class, control);
          XControlModel model = radioButton.getModel();
          
          XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, model);
          
          try
          {
            String label = (String)propertySet.getPropertyValue("Label");
            
            for (PageRangeType type : PageRangeType.values()) {
              if(type.label.equals(label)) {
                currentPageRangeType = type;
                
                break;
              }
            }
            
          } catch (UnknownPropertyException | WrappedTargetException e)
          {
            LOGGER.error("", e);
          }
        }
      }
    }
  };
}
