/*
 * Dateiname: SachleitendeVerfuegungenDruckdialog.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Dialog zum Drucken von Sachleitenden Verfügungen
 *
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * 09.10.2006 | LUT | Erstellung (basierend auf AbsenderAuswaehlen.java)
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionListener;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.SpinEvent;
import com.sun.star.awt.XActionListener;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XSpinField;
import com.sun.star.awt.XSpinListener;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.Verfuegungspunkt;
import de.muenchen.allg.itd51.wollmux.core.constants.XButtonProperties;
import de.muenchen.allg.itd51.wollmux.core.constants.XLabelProperties;
import de.muenchen.allg.itd51.wollmux.core.constants.XNumericFieldProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Dock;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung einen Dialog zum
 * Drucken von Sachleitenden Verfügungen. Die private-Funktionen dürfen NUR aus dem
 * Event-Dispatching Thread heraus aufgerufen werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
 */
public class SachleitendeVerfuegungenDruckdialog
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(SachleitendeVerfuegungenDruckdialog.class);

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
   * Anzahl der Zeichen, nach der der Text der Verfügungspunkte abgeschnitten wird, damit der Dialog
   * nicht platzt.
   */
  private static final int CONTENT_CUT = 75;

  /**
   * Vector of Verfuegungspunkt, der die Beschreibungen der Verfügungspunkte enthält.
   */
  private List<Verfuegungspunkt> verfuegungspunkte;

  private java.awt.event.ActionListener dialogEndListener;

  /**
   * Nach jedem Aufruf von printAll oder printElement enthält diese Methode die aktuelle Liste
   * Einstellungen für die zu druckenden Verfügungspunkte.
   */
  private List<VerfuegungspunktInfo> currentSettings;

  private SimpleDialogLayout layout = null;

  /**
   * Enthält die Information ob die Methode printAll in auf- oder absteigender Reihenfolge drucken
   * soll.
   */
  private boolean printOrderAsc;

  /**
   * Erzeugt einen neuen Dialog.
   *
   * @param conf
   *          das ConfigThingy, das den Dialog beschreibt (der Vater des "Fenster"-Knotens.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)} Methode aufgerufen
   *          (im Event Dispatching Thread), nachdem der Dialog geschlossen wurde. Das actionCommand
   *          des ActionEvents gibt die Aktion an, die das Beenden des Dialogs veranlasst hat.
   * @param verfuegungspunkte
   *          Vector of Verfuegungspunkt, der die Beschreibungen der Verfügungspunkte enthält.
   * @throws NodeNotFoundException
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem Dialog unmöglich
   *           macht, zu funktionieren (z.B. dass der "Fenster" Schlüssel fehlt.
   */
  public SachleitendeVerfuegungenDruckdialog(List<Verfuegungspunkt> verfuegungspunkte,
      ActionListener dialogEndListener)
  {
    this.verfuegungspunkte = verfuegungspunkte;
    this.dialogEndListener = dialogEndListener;

    createGUI();
  }

  /**
   * Enthält die Einstellungen, die zu einem Verfügungspunkt im Dialog getroffen wurden.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class VerfuegungspunktInfo
  {
    public final int verfPunktNr;

    public final short copyCount;

    public final boolean isDraft;

    public final boolean isOriginal;

    public VerfuegungspunktInfo(int verfPunktNr, short copyCount, boolean isDraft,
        boolean isOriginal)
    {
      this.verfPunktNr = verfPunktNr;
      this.copyCount = copyCount;
      this.isDraft = isDraft;
      this.isOriginal = isOriginal;
    }

    @Override
    public String toString()
    {
      return "VerfuegungspunktInfo(verfPunkt=" + verfPunktNr + ", copyCount=" + copyCount
          + ", isDraft=" + isDraft + ", isOriginal=" + isOriginal + ")";
    }
  }

  /**
   * Liefert die aktuellen in diesem Dialog getroffenen Einstellung zur Reihenfolge des Ausdrucks.
   * 
   * @return true falls in aufsteigender Reihenfloge gedruckt werden soll, false sonst.
   *
   * @author ulrich.kitzinger
   */
  public boolean getPrintOrderAsc()
  {
    return printOrderAsc;
  }

  private UNODialogFactory unoDialog = null;

  /**
   * Erzeugt das GUI.
   *
   * @param fensterDesc
   *          die Spezifikation dieses Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   * @throws com.sun.star.uno.Exception
   */
  private void createGUI()
  {
    unoDialog = new UNODialogFactory();
    XWindow dialogWindow = unoDialog.createDialog(600, 300, 0xF2F2F2);

    // FYI: showDialog() muss vor Initialisierung von SimpleLayout aufgerufen
    // werden, da
    // sonst dialogWindow zu diesem Zeitpunkt noch nicht gezeichnet ist und
    // demnach dialogWindow.getPosSize() 0 ist,
    // SimpleLayout braucht jedoch die Werte um Controls richtig anzuordnen.
    unoDialog.showDialog();

    layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    int size = verfuegungspunkte.size();

    // Header, "Ausdrucke" "Kopien"
    layout.addControlsToList(addHeader());

    // Verfuegungspunkte
    for (ControlModel verfuegungsPunkt : addVerfuegungsPunktControls(size))
    {
      layout.addControlsToList(verfuegungsPunkt);
    }

    layout.addControlsToList(addHorizontalLine());

    // Summe aller Ausdrucke
    layout.addControlsToList(printSumControls(size));

    // Ausdruck in umgekehrter Reihenfolge
    layout.addControlsToList(addPrintOrderControls());

    // Abbrechen, AlleDrucken-Button
    layout.addControlsToList(addBottomButtons());
  }

  /**
   * Wenn value mehr als CONTENT_CUT Zeichen besitzt, dann wird eine gekürzte Form von value
   * zurückgeliefert (mit "..." ergänzt) oder ansonsten value selbst.
   *
   * @param value
   *          der zu kürzende String
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static String cutContent(String value)
  {
    if (value.length() > CONTENT_CUT)
      return value.substring(0, CONTENT_CUT) + " ...";
    else
      return value;
  }

  private List<ControlModel> addVerfuegungsPunktControls(int size)
  {
    List<ControlModel> verfuegungsPunktModels = new ArrayList<>();

    for (int i = 0; i < size; ++i)
    {
      Verfuegungspunkt verfPunkt = verfuegungspunkte.get(i);
      this.currentSettings = new ArrayList<>();

      List<SimpleEntry<ControlProperties, XControl>> controls = new ArrayList<>();

      SimpleEntry<ControlProperties, XControl> verfLabel = layout
          .convertToXControl(new ControlProperties(ControlType.LABEL, "verfLabel" + i, 0, 30, 60, 0,
              new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
                  new Object[] { cutContent(verfPunkt.getHeading()) })));

      SimpleEntry<ControlProperties, XControl> printCountField = layout
          .convertToXControl(
              new ControlProperties(ControlType.NUMERIC_FIELD, "printCountField" + i, 0, 30, 20, 0,
                  new SimpleEntry<String[], Object[]>(new String[] { XNumericFieldProperties.BORDER,
                      XNumericFieldProperties.BORDER_COLOR, XNumericFieldProperties.LABEL,
                      XNumericFieldProperties.SPIN, XNumericFieldProperties.VALUE,
                      XNumericFieldProperties.MIN_VALUE, XNumericFieldProperties.DECIMAL_ACCURACY },
                      new Object[] { (short) 2, 666666, "Test", Boolean.TRUE, 1, 0, (short) 0 })));

      UnoRuntime.queryInterface(XSpinField.class, printCountField.getValue())
          .addSpinListener(printCountSpinFieldListener);

      SimpleEntry<ControlProperties, XControl> printButtons = layout
          .convertToXControl(new ControlProperties(ControlType.BUTTON, "printButton" + i, 0, 30, 20,
              0, new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                  new Object[] { "Drucken" })));
      printButtons.getKey().setButtonCommand("printElement");
      UnoRuntime.queryInterface(XButton.class, printButtons.getValue())
          .addActionListener(printElementActionListener);

      controls.add(verfLabel);
      controls.add(printCountField);
      controls.add(printButtons);

      ControlModel verfPunktModel = new ControlModel(Orientation.HORIZONTAL, Align.NONE, controls,
          Optional.empty());
      verfuegungsPunktModels.add(verfPunktModel);
    }

    return verfuegungsPunktModels;
  }

  private ControlModel addHorizontalLine()
  {
    List<SimpleEntry<ControlProperties, XControl>> lineControl = new ArrayList<>();

    lineControl
        .add(layout.convertToXControl(new ControlProperties(ControlType.FIXEDLINE, "fixedLineSum",
            0, 30, 100, 0, new SimpleEntry<String[], Object[]>(new String[] {}, new Object[] {}))));

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, lineControl, Optional.empty());
  }

  private ControlModel printSumControls(int size)
  {
    List<SimpleEntry<ControlProperties, XControl>> sumControls = new ArrayList<>();
    SimpleEntry<ControlProperties, XControl> sumLabel = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL, "sumLabel", 0, 30, 60, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
                new Object[] { "Summe aller Ausdrucke" })));

    SimpleEntry<ControlProperties, XControl> sumNumericTextfield = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL, "sumNumericTextfield", 0, 30,
            20, 0, new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
                new Object[] { " " + size })));

    sumControls.add(sumLabel);
    sumControls.add(sumNumericTextfield);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, sumControls, Optional.empty());
  }

  private ControlModel addPrintOrderControls()
  {
    List<SimpleEntry<ControlProperties, XControl>> printOrderControls = new ArrayList<>();

    SimpleEntry<ControlProperties, XControl> printOrderCheckbox = layout
        .convertToXControl(new ControlProperties(ControlType.CHECKBOX, "printOrderCheckbox", 0, 30,
            100, 0, new SimpleEntry<String[], Object[]>(new String[] { "Label" },
                new Object[] { "Ausdruck in umgekehrter Reihenfolge." })));
    UnoRuntime.queryInterface(XCheckBox.class, printOrderCheckbox.getValue())
        .addItemListener(printOrderCheckBoxListener);

    printOrderControls.add(printOrderCheckbox);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, printOrderControls,
        Optional.empty());
  }

  private ControlModel addHeader()
  {
    List<SimpleEntry<ControlProperties, XControl>> headerControls = new ArrayList<>();

    SimpleEntry<ControlProperties, XControl> headerLabelAusdrucke = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL, "headerLabelAusdrucke", 0, 30,
            60, 0, new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
                new Object[] { "Ausdrucke" })));

    SimpleEntry<ControlProperties, XControl> headerLabelKopien = layout
        .convertToXControl(new ControlProperties(ControlType.LABEL, "headerLabelKopien", 0, 30, 20,
            0, new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
                new Object[] { "Kopien" })));

    headerControls.add(headerLabelAusdrucke);
    headerControls.add(headerLabelKopien);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, headerControls, Optional.empty());
  }

  private ControlModel addBottomButtons()
  {
    List<SimpleEntry<ControlProperties, XControl>> controls = new ArrayList<>();
    ControlModel controlModel = null;

    SimpleEntry<ControlProperties, XControl> abortButton = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "abortButton", 0, 40, 50, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Abbrechen" })));
    abortButton.getKey().setButtonCommand("abort");
    UnoRuntime.queryInterface(XButton.class, abortButton.getValue())
        .addActionListener(abortListener);
    controls.add(abortButton);

    SimpleEntry<ControlProperties, XControl> printAllButton = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "printAllButton", 0, 40, 50, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Alle Drucken" })));
    printAllButton.getKey().setButtonCommand("printAll");
    UnoRuntime.queryInterface(XButton.class, printAllButton.getValue())
        .addActionListener(printAllActionListener);
    controls.add(printAllButton);

    controlModel = new ControlModel(Orientation.HORIZONTAL, Align.NONE, controls,
        Optional.of(Dock.BOTTOM));

    return controlModel;
  }

  /**
   * Ermittelt die Druckdaten (Verfügungspunkt, Anzahl-Ausfertigungen, ...) zum Verfügungspunkt
   * verfPunkt und liefert sie als VerfuegungspunktInfo-Objekt zurück.
   *
   * @author christoph.lutz
   */
  private VerfuegungspunktInfo getVerfuegungspunktInfo(int verfPunkt)
  {
    // Anzahl der Kopien lesen
    XNumericField printCountField = UnoRuntime.queryInterface(XNumericField.class,
        layout.getControl("printCountField" + (verfPunkt - 1)));

    if (printCountField == null)
    {
      LOGGER.error(
          "SachleitendeVerfuegungenDruckDialog: getVerfuegungspunktInfo: printCountField is NULL.");
      return new VerfuegungspunktInfo(0, (short) 0, false, false);
    }

    short numberOfCopies = (short) printCountField.getValue();

    boolean isDraft = (verfPunkt == verfuegungspunkte.size());
    boolean isOriginal = (verfPunkt == 1);

    return new VerfuegungspunktInfo(verfPunkt, numberOfCopies, isDraft, isOriginal);
  }

  public List<VerfuegungspunktInfo> getCurrentSettings()
  {
    return this.currentSettings;
  }

  private void getCurrentSettingsForAllElements()
  {
    this.currentSettings.clear();

    for (int verfPunkt = 1; verfPunkt <= verfuegungspunkte.size(); verfPunkt++)
    {
      this.currentSettings.add(getVerfuegungspunktInfo(verfPunkt));
    }
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
      int printFieldSum = 0;

      for (int i = 0; i < verfuegungspunkte.size(); i++)
      {
        XNumericField printCountField = UnoRuntime.queryInterface(XNumericField.class,
            layout.getControl("printCountField" + i));

        if (printCountField == null)
          continue;

        printFieldSum += printCountField.getValue();
      }

      setSumFieldValue(printFieldSum);
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
      int printFieldSum = 0;

      for (int i = 0; i < verfuegungspunkte.size(); i++)
      {
        XControl printCount = layout.getControl("printCountField" + i);

        if (printCount == null)
          continue;

        XNumericField printCountField = UnoRuntime.queryInterface(XNumericField.class, printCount);

        if (printCountField == null)
          continue;

        printFieldSum += printCountField.getValue();
      }

      setSumFieldValue(printFieldSum);
    }
  };

  private XItemListener printOrderCheckBoxListener = new XItemListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused

    }

    @Override
    public void itemStateChanged(ItemEvent arg0)
    {
      XCheckBox checkBox = UnoRuntime.queryInterface(XCheckBox.class, arg0.Source);

      if (checkBox == null)
        return;

      printOrderAsc = checkBox.getState() == 1;
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
    public void actionPerformed(ActionEvent arg0)
    {
      unoDialog.closeDialog();
    }
  };

  private XActionListener printElementActionListener = new XActionListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused

    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      XControl xControl = UnoRuntime.queryInterface(XControl.class, arg0.Source);

      getCurrentSettingsForElement(xControl);

      SimpleEntry<List<VerfuegungspunktInfo>, Boolean> config = new SimpleEntry<>(
          getCurrentSettings(), getPrintOrderAsc());

      if (dialogEndListener != null)
        dialogEndListener.actionPerformed(new java.awt.event.ActionEvent(config, 0,
            SachleitendeVerfuegungenDruckdialog.CMD_SUBMIT));

      unoDialog.closeDialog();
    }
    
    private void getCurrentSettingsForElement(XControl control)
    {
      currentSettings.clear();

      for (ControlModel model : layout.getControlList())
      {
        for (SimpleEntry<ControlProperties, XControl> entry : model.getControls())
        {
          if (entry.getValue().equals(control))
          {
            int verfPunktIndex = Integer.parseInt(entry.getKey().getControlName()
                .substring(entry.getKey().getControlName().length() - 1));

            currentSettings.add(getVerfuegungspunktInfo(verfPunktIndex + 1));
          }
        }
      }
    }
  };

  private XActionListener printAllActionListener = new XActionListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      // unused

    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      printOrderAsc = getSelectedPrintOrderAsc();
      getCurrentSettingsForAllElements();

      SimpleEntry<List<VerfuegungspunktInfo>, Boolean> config = new SimpleEntry<>(
          getCurrentSettings(), getPrintOrderAsc());

      if (dialogEndListener != null)
        dialogEndListener.actionPerformed(new java.awt.event.ActionEvent(config, 0,
            SachleitendeVerfuegungenDruckdialog.CMD_SUBMIT));

      unoDialog.closeDialog();
    }
  };

  private void setSumFieldValue(int value)
  {
    XFixedText sumLabel = UnoRuntime.queryInterface(XFixedText.class,
        layout.getControl("sumNumericTextfield"));
    sumLabel.setText(" " + value);
  }

  private boolean getSelectedPrintOrderAsc()
  {
    XCheckBox isOrderAscSelected = UnoRuntime.queryInterface(XCheckBox.class,
        layout.getControl("printOrderCheckbox"));

    return isOrderAscSelected.getState() == 0;
  }
}
