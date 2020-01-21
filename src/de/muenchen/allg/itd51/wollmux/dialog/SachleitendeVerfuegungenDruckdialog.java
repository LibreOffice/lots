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
import java.util.ArrayList;
import java.util.List;

import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.SpinEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XScrollBar;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.Verfuegungspunkt;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.VerfuegungspunktInfo;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractAdjustmentListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractSpinListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTopWindowListener;

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
   * Liste der Verfügungspunkte.
   */
  private List<Verfuegungspunkt> verfuegungspunkte;

  /**
   * Dieser Listener wird benachrichtigt, sobald der Dialog geschlossen wird.
   */
  private java.awt.event.ActionListener dialogEndListener;

  /**
   * Enthält die Information ob die Methode printAll in auf- oder absteigender Reihenfolge drucken
   * soll.
   */
  private boolean printOrderAsc = true;
  
  /**
   * Should all prints be merged into one file?
   */
  private boolean collect = false;

  /**
   * Liste mit den aktuellen Einstellungen für jeden Verfügungspunkt.
   */
  private final List<VerfuegungspunktInfo> settings = new ArrayList<>();

  /**
   * Der Container, der die Steuerelemente enthält.
   */
  private XControlContainer container;

  /**
   * Der Dialog.
   */
  private XDialog dialog;

  /**
   * Array der Listener für die einzel Druckbuttons.
   */
  private final MyActionListener[] printListener = new MyActionListener[4];

  /**
   * Array der Listener für die Anzahl der Kopien.
   */
  private final MySpinListener[] spinListener = new MySpinListener[4];

  /**
   * Die Aktion, die an den dialogEndListener übergeben wird.
   */
  private String action = CMD_CANCEL;

  /**
   * Die Konfiguration, die an den dialogEndListener übergeben wird.
   */
  private Triplet<List<VerfuegungspunktInfo>, Boolean, Boolean> config;

  /**
   * Erzeugt einen neuen Dialog.
   *
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)} Methode aufgerufen
   *          (im Event Dispatching Thread), nachdem der Dialog geschlossen wurde. Das actionCommand
   *          des ActionEvents gibt die Aktion an, die das Beenden des Dialogs veranlasst hat.
   * @param verfuegungspunkte
   *          Vector of Verfuegungspunkt, der die Beschreibungen der Verfügungspunkte enthält.
   */
  public SachleitendeVerfuegungenDruckdialog(List<Verfuegungspunkt> verfuegungspunkte,
      ActionListener dialogEndListener)
  {
    this.verfuegungspunkte = verfuegungspunkte;
    this.dialogEndListener = dialogEndListener;

    for (int i = 0; i < verfuegungspunkte.size(); i++)
    {
      Verfuegungspunkt punkt = verfuegungspunkte.get(i);
      boolean isDraft = (i + 1 == verfuegungspunkte.size());
      boolean isOriginal = (i == 0);
      settings.add(
          new VerfuegungspunktInfo(i + 1, (short) punkt.getNumberOfCopies(), isDraft, isOriginal));
    }

    createGUI();
  }

  /**
   * Liefert die aktuellen in diesem Dialog getroffenen Einstellung zur Reihenfolge des Ausdrucks.
   *
   * @return true falls in aufsteigender Reihenfloge gedruckt werden soll, false sonst.
   */
  public boolean getPrintOrderAsc()
  {
    return printOrderAsc;
  }
  
  public boolean isCollect()
  {
    return collect;
  }

  /**
   * Initializiert die GUI.
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
          "vnd.sun.star.script:WollMux.slv_count?location=application", "", peer, null);
      container = UnoRuntime.queryInterface(XControlContainer.class, window);
      dialog = UnoRuntime.queryInterface(XDialog.class, window);
      UNO.XTopWindow(dialog).addTopWindowListener(new AbstractTopWindowListener()
      {
        @Override
        public void windowClosed(EventObject arg0)
        {
          if (dialogEndListener != null)
          {
            dialogEndListener.actionPerformed(new java.awt.event.ActionEvent(config, 0, action));
          }
        }
      });

      XScrollBar scrollBar = UnoRuntime.queryInterface(XScrollBar.class,
          container.getControl("ScrollBar"));
      scrollBar.setMaximum(verfuegungspunkte.size());
      AbstractAdjustmentListener scrollListener = event -> update(event.Value);
      scrollBar.addAdjustmentListener(scrollListener);

      for (int i = 1; i <= 4; i++)
      {
        spinListener[i - 1] = new MySpinListener();
        UNO.XSpinField(container.getControl("Count" + i)).addSpinListener(spinListener[i - 1]);
        printListener[i - 1] = new MyActionListener();
        UNO.XButton(container.getControl("Print" + i)).addActionListener(printListener[i - 1]);
      }

      XCheckBox printOrder = UNO.XCheckBox(container.getControl("printOrderCheckbox"));
      AbstractItemListener printOrderListener = event -> printOrderAsc = printOrder.getState() == 0;
      printOrder.addItemListener(printOrderListener);
      
      XCheckBox collectBox = UNO.XCheckBox(container.getControl("collect"));
      AbstractItemListener collectListener = event -> collect = collectBox.getState() == 1;
      collectBox.addItemListener(collectListener);

      XButton abort = UNO.XButton(container.getControl("Abort"));
      AbstractActionListener abortListener = event -> {
        action = CMD_CANCEL;
        config = new Triplet<>(settings, getPrintOrderAsc(), isCollect());
        dialog.endExecute();
      };
      abort.addActionListener(abortListener);

      XButton print = UNO.XButton(container.getControl("PrintAll"));
      AbstractActionListener printAllListener = event -> {
        action = CMD_SUBMIT;
        config = new Triplet<>(settings, getPrintOrderAsc(), isCollect());
        dialog.endExecute();
      };
      print.addActionListener(printAllListener);

      update(0);
      updateSum();
      dialog.execute();
    } catch (Exception e)
    {
      LOGGER.error("SLV-Druck-Dialog konnte nicht angezeigt werden.", e);
    }
  }

  /**
   * Aktualisiert die Steuerelemente für die Verfügungspunkte an Hand der Scrollbar.
   *
   * @param value
   *          Der Wert der Scrollbar.
   */
  private void update(int value)
  {
    for (int i = 1; i <= 4; i++)
    {
      int index = value - 1 + i;
      if (index < verfuegungspunkte.size())
      {
        Verfuegungspunkt punkt = verfuegungspunkte.get(index);
        VerfuegungspunktInfo info = settings.get(index);
        XFixedText label = UNO.XFixedText(container.getControl("Label" + i));
        label.setText(cutContent(punkt.getHeading()));

        XNumericField countNum = UNO.XNumericField(container.getControl("Count" + i));
        countNum.setValue(info.getCopyCount());
        spinListener[i - 1].setInfo(info);

        printListener[i - 1].setInfo(info);
      } else
      {
        UNO.XWindow(container.getControl("Label" + i)).setVisible(false);
        UNO.XWindow(container.getControl("Count" + i)).setVisible(false);
        UNO.XWindow(container.getControl("Print" + i)).setVisible(false);
      }
    }
  }

  /**
   * Wenn value mehr als CONTENT_CUT Zeichen besitzt, dann wird eine gekürzte Form von value
   * zurückgeliefert (mit "..." ergänzt) oder ansonsten value selbst.
   *
   * @param value
   *          der zu kürzende String
   * @return der gekürzte String
   */
  private static String cutContent(String value)
  {
    if (value.length() > CONTENT_CUT)
      return value.substring(0, CONTENT_CUT) + " ...";
    else
      return value;
  }

  private void updateSum()
  {
    int sum = 0;
    for (VerfuegungspunktInfo info : settings)
    {
      sum += info.getCopyCount();
    }

    UNO.XFixedText(container.getControl("Sum")).setText("" + sum);
  }

  /**
   * Listener für die Anzahl der Kopien.
   *
   * @author daniel.sikeler
   */
  private class MySpinListener extends AbstractSpinListener
  {
    /**
     * Der Verfügungspunkt, bei dem die Anzahl der Kopien geänder werden soll.
     */
    private VerfuegungspunktInfo info = null;

    public void setInfo(VerfuegungspunktInfo info)
    {
      this.info = info;
    }

    @Override
    public void up(SpinEvent event)
    {
      updateCount((short) UNO.XNumericField(event.Source).getValue());
    }

    @Override
    public void down(SpinEvent event)
    {
      updateCount((short) UNO.XNumericField(event.Source).getValue());
    }

    /**
     * Aktualisiert die Anzahl der Kopien und das Summen-Feld.
     *
     * @param copyCount
     *          Die neue Anzahl an Kopien.
     */
    private void updateCount(short copyCount)
    {
      info.setCopyCount(copyCount);
      updateSum();
    }
  }

  /**
   * Listener für die Druck-Buttons der Verfügungspunkte.
   *
   * @author daniel.sikeler
   */
  private class MyActionListener implements AbstractActionListener
  {
    /**
     * Der Verfügungspunkt, der gedruckt werden soll.
     */
    private VerfuegungspunktInfo info = null;

    public void setInfo(VerfuegungspunktInfo info)
    {
      this.info = info;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      List<VerfuegungspunktInfo> infos = new ArrayList<>(1);
      infos.add(info);
      action = CMD_SUBMIT;
      config = new Triplet<>(infos, getPrintOrderAsc(), isCollect());
      dialog.endExecute();
    }
  }
}
