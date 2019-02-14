/*
 * Dateiname: AbsenderAuswaehlen.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Absenderdaten auswählen Dialog des BKS
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
 * 25.10.2005 | BNK | Erstellung
 * 27.10.2005 | BNK | back + CLOSEACTION
 * 02.11.2005 | BNK | Absenderliste nicht mehr mit Vorname = M* befüllen,
 *                    weil jetzt der TestDJ schon eine Absenderliste
 *                    mit Einträgen hat.
 * 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
 * 03.01.2005 | BNK | Bug korrigiert;  .gridy = x  sollte .gridx = x sein.
 * 19.05.2006 | BNK | [R1898]Wenn die Liste leer ist, dann gleich den PAL Verwalten Dialog aufrufen
 * 26.02.2010 | BED | WollMux-Icon für das Fenster
 * 08.04.2010 | BED | [R52334] Anzeige über DISPLAY-Attribut konfigurierbar
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.itd51.wollmux.core.constants.XButtonProperties;
import de.muenchen.allg.itd51.wollmux.core.constants.XLabelProperties;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Diese Klasse stellt einen Dialog zum
 * Auswählen eines Eintrages aus der Persönlichen Absenderliste bereit.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1), Björn Ranft
 */
public class AbsenderAuswaehlen
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AbsenderAuswaehlen.class);

  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;

  /**
   * Das ConfigThingy, das den Dialog zum Bearbeiten der Absenderliste spezifiziert.
   */
  private ConfigThingy verConf;

  private List<DJDatasetListElement> elements = null;
  
  private UNODialogFactory dialogFactory;
  
  private SimpleDialogLayout layout;

  /**
   * Erzeugt einen neuen Dialog.
   *
   * @param verConf
   *          das ConfigThingy, das den Absenderliste Verwalten Dialog beschreibt.
   * @param dj
   *          der DatasourceJoiner, der die PAL verwaltet.
   */
  public AbsenderAuswaehlen(ConfigThingy verConf, DatasourceJoiner dj)
  {
    this.dj = dj;
    this.verConf = verConf;

    createUNOGUI();
  }

  private void createUNOGUI()
  {
    dialogFactory = new UNODialogFactory();
    XWindow dialogWindow = dialogFactory.createDialog(600, 300, 0xF2F2F2);
    dialogWindow.addWindowListener(windowListener);

    dialogFactory.showDialog();

    layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    layout.addControlsToList(addMainControls());
    layout.addControlsToList(addBottomButtons());

    layout.draw();

    QueryResults palEntries = dj.getLOS();
    if (palEntries.isEmpty())
    {
      new PersoenlicheAbsenderlisteVerwalten(verConf, dj);
    } else
    {
      setListElements(dj.getLOS());
    }
  }

  private AbstractWindowListener windowListener = new AbstractWindowListener()
  {

    @Override
    public void disposing(EventObject arg0)
    {
      WollMuxEventHandler.getInstance().handlePALChangedNotify();
      dialogFactory.closeDialog();
    }
  };

  private ControlModel addMainControls()
  {
    List<SimpleEntry<ControlProperties, XControl>> mainControls = new ArrayList<>();

    mainControls.add(layout.convertToXControl(new ControlProperties(ControlType.LABEL, "absLabel",
        0, 30, 100, 0, new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
            new Object[] { "Welchen Absender möchten Sie für Ihre Briefköpfe verwenden?" }))));

    SimpleEntry<ControlProperties, XControl> absListBox = layout
        .convertToXControl(new ControlProperties(ControlType.LIST_BOX, "absListBox", 0, 130, 100, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
                new Object[] { "" })));
    XListBox absXListBox = UnoRuntime.queryInterface(XListBox.class, absListBox.getValue());
    absXListBox.addItemListener(absListBoxItemListener);

    mainControls.add(absListBox);

    return new ControlModel(Orientation.VERTICAL, Align.NONE, mainControls, Optional.empty());
  }

  private ControlModel addBottomButtons()
  {
    List<SimpleEntry<ControlProperties, XControl>> bottomBtns = new ArrayList<>();

    SimpleEntry<ControlProperties, XControl> abortBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "abortBtn", 0, 30, 50, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Abbrechen" })));
    XButton abortXBtn = UnoRuntime.queryInterface(XButton.class, abortBtn.getValue());
    abortXBtn.setActionCommand("abort");
    abortXBtn.addActionListener(abortActionListener);

    SimpleEntry<ControlProperties, XControl> editBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "editBtn", 0, 30, 50, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Bearbeiten" })));
    XButton editXBtn = UnoRuntime.queryInterface(XButton.class, editBtn.getValue());
    editXBtn.setActionCommand("printElement");
    editXBtn.addActionListener(editActionListener);

    bottomBtns.add(abortBtn);
    bottomBtns.add(editBtn);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomBtns, Optional.empty());
  }

  private AbstractItemListener absListBoxItemListener = event -> {
    DJDatasetListElement selectedElement = elements.get(event.Selected);

    if (selectedElement == null) {
      LOGGER.debug("AbsenderAuswaehlen: itemStateChanged: selectedDataset is NULL.");
      return;
    }

    selectedElement.getDataset().select();
  };

  private AbstractActionListener abortActionListener = event -> {
    WollMuxEventHandler.getInstance().handlePALChangedNotify();
    dialogFactory.closeDialog();
  };

  private AbstractActionListener editActionListener = event -> new PersoenlicheAbsenderlisteVerwalten(verConf, dj);

  private void setListElements(QueryResults data)
  {
    elements = new ArrayList<>();

    data.forEach(item -> {
      DJDatasetListElement element = new DJDatasetListElement((DJDataset) item);
      elements.add(element);
    });
    Collections.sort(elements);

    XControl xControl = layout.getControl("absListBox");

    if (xControl == null)
      return;

    XListBox xListBox = UnoRuntime.queryInterface(XListBox.class, xControl);

    if (xListBox == null)
      return;

    if (xListBox.getItemCount() > 0)
    {
      xListBox.removeItems((short) 0, xListBox.getItemCount());
    }

    short itemToHightlightPos = 0;
    
    for (int i = 0; i < elements.size(); i++)
    {
      try
      {
        Dataset ds = elements.get(i).getDataset();
        String dbNachname = ds.get("Nachname") == null ? "" : ds.get("Nachname");
        String dbVorname = ds.get("Vorname") == null ? "" : ds.get("Vorname");
        String dbOrgaKurz = ds.get("OrgaKurz") == null ? "" : ds.get("OrgaKurz");

        xListBox.addItem(dbNachname + ", " + dbVorname + " " + dbOrgaKurz, (short) i);
        
        if(elements.get(i).getDataset().isSelectedDataset())
        {
          itemToHightlightPos = (short) i;
        }
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }
    }
    
    xListBox.selectItemPos(itemToHightlightPos, true);
  }
}
