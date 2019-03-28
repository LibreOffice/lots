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

import com.sun.star.awt.SpinEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
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
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Dock;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractSpinListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
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
    layout.setMarginRight(20);
    layout.setWindowBottomMargin(10);

    // Bereiche
    List<ControlModel> printRangeControls = new ArrayList<>();
    List<ControlProperties> radioControls = null;

    for (int i = 0; i < PageRangeType.values().length; i++)
    {
      PageRangeType pageRangeType = PageRangeType.values()[i];
      radioControls = new ArrayList<>();

      ControlProperties radioButton = new ControlProperties(ControlType.RADIO, "radioSectionRadioButton" + i);
      radioButton.setControlPercentSize(60, 30);
      radioButton.setLabel(pageRangeType.label);
      
      // zusätzlich zu controlModel.marginLeft etwas einrücken da um diese
      // Controls später eine GroupBox gezeichnet wird
      radioButton.setMarginLeft(40);
      
      // ersten RadioButton aktivieren
      XRadioButton xRadioButton = UNO.XRadio(radioButton.getXControl());
      if (i == 0) {
    	  xRadioButton.setState(true);
    	  this.currentPageRangeType = pageRangeType;
    	  this.currentPageRangeValue = "1-9999";
      }
      
      xRadioButton.addItemListener(radioButtonListener);

      radioControls.add(radioButton);

      if (pageRangeType.hasAdditionalTextField)
      {
        ControlProperties addtionalTextfield = new ControlProperties(ControlType.EDIT, "radioSectionTextField" + i);
        addtionalTextfield.setControlPercentSize(30, 30);
        addtionalTextfield.setLabel(pageRangeType.additionalTextFieldPrototypeDisplayValue);
        UNO.XTextComponent(addtionalTextfield.getXControl()).addTextListener(additionalTextFieldListener);

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
    List<ControlProperties> copyCountControls = new ArrayList<>();
    
    ControlProperties labelExemplare = new ControlProperties(ControlType.LABEL, "labelExemplare");
    labelExemplare.setControlPercentSize(30, 30);
    labelExemplare.setLabel("Exemplare");
    
    ControlProperties printCountField = new ControlProperties(ControlType.NUMERIC_FIELD, "printCountField");
    printCountField.setControlPercentSize(20, 30);
    printCountField.setBorder((short) 2);
    printCountField.setBorderColor(666666);
    printCountField.setLabel("Kopien");
    printCountField.setSpinEnabled(Boolean.TRUE);
    printCountField.setValue(1);
    printCountField.setDecimalAccuracy((short) 0);
    UNO.XSpinField(printCountField.getXControl()).addSpinListener(printCountSpinFieldListener);
    
    copyCountControls.add(labelExemplare);
    copyCountControls.add(printCountField);
    
    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, copyCountControls,
        Optional.empty());
  }

  private ControlModel addHeader()
  {
    List<ControlProperties> headerLabel = new ArrayList<>();
    
    ControlProperties printerDialogLabel = new ControlProperties(ControlType.LABEL, "printerDialogLabel");
    printerDialogLabel.setControlPercentSize(20, 20);
    printerDialogLabel.setLabel("Drucker");
    printerDialogLabel.setMarginBetweenControls(0);
    
    ControlProperties hLine = new ControlProperties(ControlType.FIXEDLINE, "printerDialogLabelHLine");
    hLine.setControlPercentSize(5, 20);
    hLine.setMarginBetweenControls(0);
    
    headerLabel.add(printerDialogLabel);
    headerLabel.add(hLine);

    return new ControlModel(Orientation.VERTICAL, Align.NONE, headerLabel,
        Optional.empty());
  }

  private ControlModel addPrinterInfoModel()
  {
    List<ControlProperties> printerSettings = new ArrayList<>();

    ControlProperties printerSettingsLabel = new ControlProperties(ControlType.LABEL, "printerSettingsLabel");
    printerSettingsLabel.setControlPercentSize(30, 20);
    printerSettingsLabel.setLabel("Name");
    
    ControlProperties printerSettingsPrintModel = new ControlProperties(ControlType.LABEL, "printerSettingsPrintModel");
    printerSettingsPrintModel.setControlPercentSize(30, 30);
    printerSettingsPrintModel.setLabel(getCurrentPrinterName(this.doc));

    ControlProperties printerSettingsSelectPrinterButton = new ControlProperties(ControlType.BUTTON, "printerSettingsSelectPrinterButton");
    printerSettingsSelectPrinterButton.setControlPercentSize(50, 30);
    printerSettingsSelectPrinterButton.setLabel("Drucker wechseln / einrichten");
    XButton selectPrinterButton = UNO.XButton(printerSettingsSelectPrinterButton.getXControl());
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
    
    List<ControlProperties> groupBoxRadioModel = new ArrayList<>();
    
    ControlProperties groupBox = new ControlProperties(ControlType.GROUPBOX, "groupBoxRadio");
    groupBox.setControlPercentSize(100, 0);
    groupBox.setControlSize(0, groupBoxHeight);
    groupBox.setLabel("Bereiche");
    
    groupBoxRadioModel.add(groupBox);

    ControlModel groupBoxModel = new ControlModel(Orientation.HORIZONTAL,
        Align.NONE, groupBoxRadioModel, Optional.empty());
    groupBoxModel.setLinebreakHeight(10);

    return groupBoxModel;
  }

  private ControlModel addBottomControlButtons()
  {
    List<ControlProperties> bottomButtonsSection = new ArrayList<>();
    
    ControlProperties bottomButtonAbort = new ControlProperties(ControlType.BUTTON, "printerDialogCancel");
    bottomButtonAbort.setControlPercentSize(50, 40);
    bottomButtonAbort.setLabel("Abbrechen");
    XButton abortButton = UNO.XButton(bottomButtonAbort.getXControl());
    abortButton.addActionListener(abortListener);

    ControlProperties bottomButtonPrint = new ControlProperties(ControlType.BUTTON, "printerDialogPrint");
    bottomButtonPrint.setControlPercentSize(50, 40);
    bottomButtonPrint.setLabel("Drucken");
    XButton printButton = UNO.XButton(bottomButtonPrint.getXControl());
    printButton.addActionListener(printListener);

    bottomButtonsSection.add(bottomButtonAbort);
    bottomButtonsSection.add(bottomButtonPrint);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE,
        bottomButtonsSection, Optional.of(Dock.BOTTOM));
  }

  private AbstractActionListener printListener = event -> printButtonPressed();

  private AbstractActionListener abortListener = event -> dialogFactory.closeDialog();

  private AbstractActionListener selectPrinterActionListener = event -> showPrintSettingsDialog();
    
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
                        XFixedText printerNameLabel = UNO.XFixedText(layout.getControl("printerSettingsPrintModel"));

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
    
    private void getAndSetCopyCount(SpinEvent event) {
      XNumericField copyCountControl = UNO.XNumericField(event.Source);
      
      if (copyCountControl == null)
        return;
      
      copyCount = (short) copyCountControl.getValue();
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
  
  private AbstractTextListener additionalTextFieldListener = event -> {
    XTextComponent xTextComponent = UNO.XTextComponent(event.Source);

    if (xTextComponent == null)
      return;

    currentPageRangeValue = xTextComponent.getText();
  };

  private AbstractItemListener radioButtonListener = event -> {
    XControl xControl = UNO.XControl(event.Source);

    for (XControl control : layout.getControls())
    {
      if (!control.equals(xControl))
      {
        XRadioButton radioButton = UNO.XRadio(control);

        if (radioButton == null)
          continue;
        
        radioButton.setState(false);
        
      } else {
        XControl radioButton = UNO.XControl(control);
        XControlModel model = radioButton.getModel();
        
        XPropertySet propertySet = UNO.XPropertySet(model);
        
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
  };
}
