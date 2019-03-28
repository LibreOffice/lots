/*
 * Dateiname: SachleitendeVerfuegungenDruckdialog.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Dialog zum Drucken von Sachleitenden Verfügungen
 * 
 * Copyright (c) 2010-2019 Landeshauptstadt München
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

import com.sun.star.awt.SpinEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XWindow;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.Verfuegungspunkt;
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
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;

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
    layout.setMarginRight(20);
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

      List<ControlProperties> controls = new ArrayList<>();

      ControlProperties verfLabel = new ControlProperties(ControlType.LABEL, "verfLabel" + i);
      verfLabel.setControlPercentSize(60, 30);
      verfLabel.setLabel(cutContent(verfPunkt.getHeading()));
      
      ControlProperties printCountField = new ControlProperties(ControlType.NUMERIC_FIELD, "printCountField" + i);
      printCountField.setControlPercentSize(15, 30);
      printCountField.setBorder((short) 2);
      printCountField.setBorderColor(666666);
      printCountField.setLabel("Test");
      printCountField.setSpinEnabled(Boolean.TRUE);
      printCountField.setValue(1);
      printCountField.setDecimalAccuracy((short) 0);
      UNO.XSpinField(printCountField.getXControl())
          .addSpinListener(printCountSpinFieldListener);
      AbstractTextListener textListener = event -> {
        int printFieldSum = 0;

        for (int j = 0; j < verfuegungspunkte.size(); j++)
        {
          XControl printCount = layout.getControl("printCountField" + j);
          XNumericField nField = UNO.XNumericField(printCount);

          if (nField == null)
            continue;

          printFieldSum += nField.getValue();
        }

        setSumFieldValue(printFieldSum);
      };
      
      UNO.XTextComponent(printCountField.getXControl()).addTextListener(textListener);

      ControlProperties printButtons = new ControlProperties(ControlType.BUTTON, "printButton" + i);
      printButtons.setControlPercentSize(25, 30);
      printButtons.setLabel("Drucken");
      XButton printElementButton = UNO.XButton(printButtons.getXControl());
      printElementButton.addActionListener(printElementActionListener);

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
    List<ControlProperties> lineControl = new ArrayList<>();

    ControlProperties verfLabel = new ControlProperties(ControlType.FIXEDLINE, "fixedLineSum");
    verfLabel.setControlPercentSize(100, 30);
    
    lineControl.add(verfLabel);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, lineControl, Optional.empty());
  }

  private ControlModel printSumControls(int size)
  {
    List<ControlProperties> sumControls = new ArrayList<>();
    
    ControlProperties sumLabel = new ControlProperties(ControlType.LABEL, "sumLabel");
    sumLabel.setControlPercentSize(60, 30);
    sumLabel.setLabel("Summe aller Ausdrucke");
    
    ControlProperties sumNumericTextfield = new ControlProperties(ControlType.LABEL, "sumNumericTextfield");
    sumNumericTextfield.setControlPercentSize(20, 30);
    sumNumericTextfield.setLabel(" " + size);

    sumControls.add(sumLabel);
    sumControls.add(sumNumericTextfield);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, sumControls, Optional.empty());
  }

  private ControlModel addPrintOrderControls()
  {
    List<ControlProperties> printOrderControls = new ArrayList<>();

    ControlProperties printOrderCheckbox = new ControlProperties(ControlType.CHECKBOX, "printOrderCheckbox");
    printOrderCheckbox.setControlPercentSize(100, 30);
    printOrderCheckbox.setLabel("Ausdruck in umgekehrter Reihenfolge.");
    UNO.XCheckBox(printOrderCheckbox.getXControl())
        .addItemListener(printOrderCheckBoxListener);

    printOrderControls.add(printOrderCheckbox);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, printOrderControls,
        Optional.empty());
  }

  private ControlModel addHeader()
  {
    List<ControlProperties> headerControls = new ArrayList<>();

    ControlProperties headerLabelAusdrucke = new ControlProperties(ControlType.LABEL, "headerLabelAusdrucke");
    headerLabelAusdrucke.setControlPercentSize(60, 30);
    headerLabelAusdrucke.setLabel("Ausdrucke");
    
    ControlProperties headerLabelKopien = new ControlProperties(ControlType.LABEL, "headerLabelKopien");
    headerLabelKopien.setControlPercentSize(20, 30);
    headerLabelKopien.setLabel("Kopien");
    
    headerControls.add(headerLabelAusdrucke);
    headerControls.add(headerLabelKopien);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, headerControls, Optional.empty());
  }

  private ControlModel addBottomButtons()
  {
    List<ControlProperties> controls = new ArrayList<>();

    ControlProperties abortButton = new ControlProperties(ControlType.BUTTON, "abortButton");
    abortButton.setControlPercentSize(50, 40);
    abortButton.setLabel("Abbrechen");
    XButton abortXButton = UNO.XButton(abortButton.getXControl());
    abortXButton.addActionListener(abortListener);
    
    controls.add(abortButton);

    ControlProperties printAllButton = new ControlProperties(ControlType.BUTTON, "printAllButton");
    printAllButton.setControlPercentSize(50, 40);
    printAllButton.setLabel("Alle Drucken");
    XButton printAllXButton = UNO.XButton(printAllButton.getXControl());
    printAllXButton.addActionListener(printAllActionListener);
    
    controls.add(printAllButton);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, controls,
        Optional.of(Dock.BOTTOM));
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
    XNumericField printCountField = UNO.XNumericField(layout.getControl("printCountField" + (verfPunkt - 1)));

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

  private AbstractSpinListener printCountSpinFieldListener = new AbstractSpinListener()
  {

    @Override
    public void up(SpinEvent event)
    {
      int printFieldSum = 0;

      for (int i = 0; i < verfuegungspunkte.size(); i++)
      {
        XNumericField printCountField = UNO.XNumericField(layout.getControl("printCountField" + i));

        if (printCountField == null)
          continue;

        printFieldSum += printCountField.getValue();
      }

      setSumFieldValue(printFieldSum);
    }

    @Override
    public void down(SpinEvent event)
    {
      int printFieldSum = 0;

      for (int i = 0; i < verfuegungspunkte.size(); i++)
      {
        XControl printCount = layout.getControl("printCountField" + i);
        XNumericField printCountField = UNO.XNumericField(printCount);

        if (printCountField == null)
          continue;

        printFieldSum += printCountField.getValue();
      }

      setSumFieldValue(printFieldSum);
    }
  };
  
  private void setSumFieldValue(int value)
  {
    XFixedText sumLabel = UNO.XFixedText(layout.getControl("sumNumericTextfield"));
    sumLabel.setText(" " + value);
  }

  private AbstractItemListener printOrderCheckBoxListener = event -> {
    XCheckBox checkBox = UNO.XCheckBox(event.Source);

    if (checkBox == null)
      return;

    printOrderAsc = checkBox.getState() == 1;
  };

  private AbstractActionListener abortListener = event -> unoDialog.closeDialog();

  private AbstractActionListener printElementActionListener = event ->
  {
    XControl xControl = UNO.XControl(event.Source);

    getCurrentSettingsForElement(xControl);

    SimpleEntry<List<VerfuegungspunktInfo>, Boolean> config = new SimpleEntry<>(
        getCurrentSettings(), getPrintOrderAsc());

    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new java.awt.event.ActionEvent(config, 0,
          SachleitendeVerfuegungenDruckdialog.CMD_SUBMIT));

    unoDialog.closeDialog();
  };
    
  private void getCurrentSettingsForElement(XControl control)
  {
    currentSettings.clear();

    for (ControlModel model : layout.getControlList())
    {
      for (ControlProperties entry : model.getControls())
      {
        if (entry.getXControl().equals(control))
        {
          int verfPunktIndex = Integer.parseInt(entry.getControlName()
              .substring(entry.getControlName().length() - 1));

          currentSettings.add(getVerfuegungspunktInfo(verfPunktIndex + 1));
        }
      }
    }
  }

  private AbstractActionListener printAllActionListener = event -> {
    printOrderAsc = getSelectedPrintOrderAsc();
    getCurrentSettingsForAllElements();

    SimpleEntry<List<VerfuegungspunktInfo>, Boolean> config = new SimpleEntry<>(
        getCurrentSettings(), getPrintOrderAsc());

    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new java.awt.event.ActionEvent(config, 0,
          SachleitendeVerfuegungenDruckdialog.CMD_SUBMIT));

    unoDialog.closeDialog();
  };

  private boolean getSelectedPrintOrderAsc()
  {
    XCheckBox isOrderAscSelected = UNO.XCheckBox(layout.getControl("printOrderCheckbox"));

    return isOrderAscSelected.getState() == 0;
  }
}
