/*
 * Dateiname: PersoenlicheAbsenderlisteVerwalten.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Hinzufügen/Entfernen Dialog des BKS
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
 * 17.10.2005 | BNK | Erstellung
 * 18.10.2005 | BNK | PAL Verwalten GUI großteils implementiert (aber funktionslos)
 * 19.10.2005 | BNK | Suche implementiert
 * 20.10.2005 | BNK | Suche getestet
 * 24.10.2005 | BNK | Restliche ACTIONS implementiert
 *                  | Doppelklickbehandlung
 *                  | Sortierung
 *                  | Gegenseitiger Ausschluss der Selektierung
 * 25.10.2005 | BNK | besser kommentiert
 * 27.10.2005 | BNK | back + CLOSEACTION
 * 31.10.2005 | BNK | Behandlung von TimeoutException bei find()
 * 02.11.2005 | BNK | +editNewPALEntry()
 * 10.11.2005 | BNK | +DEFAULT_* Konstanten
 * 14.11.2005 | BNK | Exakter Match "Nachname" entfernt aus 1-Wort-Fall
 * 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
 * 22.11.2005 | BNK | Bei Initialisierung ist der selectedDataset auch in der Liste
 *                  | selektiert.
 * 20.01.2006 | BNK | Default-Anrede für Tinchen WollMux ist "Frau"
 * 19.10.2006 | BNK | Credits
 * 23.10.2006 | BNK | Bugfix: Bei credits an wurden Personen ohne Mail nicht dargestellt.
 * 06.11.2006 | BNK | auf AlwaysOnTop gesetzt.
 * 26.02.2010 | BED | WollMux-Icon für Frame; Löschen aus PAL-Liste mit ENTF-Taste
 * 11.03.2010 | BED | Einsatz von FrameWorker für Suche + Meldung bei Timeout
 *                  | Credits-Bild für BED
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.sun.star.awt.Key;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XKeyHandler;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.constants.XButtonProperties;
import de.muenchen.allg.itd51.wollmux.core.constants.XLabelProperties;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.Search;
import de.muenchen.allg.itd51.wollmux.core.db.TimeoutException;
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
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung einen Dialog
 * mit konfigurierbaren Suchfeldern zum
 * Hinzufügen/Entfernen von Einträgen der Persönlichen Absenderliste auf.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1), Björn Ranft
 */
public class PersoenlicheAbsenderlisteVerwalten
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PersoenlicheAbsenderlisteVerwalten.class);

  public static final String DEFAULT_ROLLE = "D-WOLL-MUX-5.1";

  public static final String DEFAULT_NACHNAME = "Mustermann";

  public static final String DEFAULT_VORNAME = "Max";

  public static final String DEFAULT_ANREDE = "Herr";

  private DatasourceJoiner dj;

  private SimpleDialogLayout layout;
  private UNODialogFactory dialogFactory;
  private List<DJDatasetListElement> resultDJDatasetList = null;
  private List<DJDatasetListElement> cachedPAL = new ArrayList<>();

  /**
   * Die Textfelder in dem der Benutzer seine Suchanfrage eintippt.
   */
  private Map<String, String> queryNames = ImmutableMap.of("Nachname", "Nachname", "Vorname",
      "Vorname", "Email", "Mail", "Orga", "OrgaKurz");

  private List<ControlModel> query2 = new ArrayList<>();

  /**
   * Erzeugt einen neuen Dialog.
   *
   * @param conf
   *          das ConfigThingy, das die möglichen Suchfelder beschreibt.
   * @param dj
   *          der DatasourceJoiner, der die zu bearbeitende Liste verwaltet.
   * @throws NodeNotFoundException
   *           Im Falle von nicht konfigurierten Suchfeldern in ConfigThingy.
   */
  public PersoenlicheAbsenderlisteVerwalten(ConfigThingy conf, DatasourceJoiner dj)
  {
    this.dj = dj;

    ConfigThingy suchfelder;
    try
    {
      suchfelder = conf.query("Suchfelder").getFirstChild();

      queryNames = StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(suchfelder.iterator(), 0), false)
          .sorted((a, b) -> {
            int s1 = NumberUtils.toInt(a.getString("SORT"));
            int s2 = NumberUtils.toInt(b.getString("SORT"));
            return s1 - s2;
          }).collect(Collectors.toMap(it -> it.getString("LABEL"), it -> it.getString("DB_SPALTE"),
              (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    } catch (NodeNotFoundException e)
    {
      LOGGER.error(L.m("Es wurden keine Suchfelder definiert."));
    }

    createGUI();
  }

  /**
   * Erzeugt das GUI.
   *
   * @author Björn Ranft
   */
  private void createGUI()
  {
    dialogFactory = new UNODialogFactory();
    XWindow dialogWindow = dialogFactory.createDialog(600, 300, 0xF2F2F2);
    dialogWindow.addWindowListener(xWindowListener);

    Object toolkit = null;
    XToolkit xToolkit = null;
    try
    {
      toolkit = UNO.xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", UNO.defaultContext);
      xToolkit = UnoRuntime.queryInterface(XToolkit.class, toolkit);
      XExtendedToolkit extToolkit = UnoRuntime.queryInterface(XExtendedToolkit.class, xToolkit);
      extToolkit.addKeyHandler(keyHandler);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    dialogFactory.showDialog();

    layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    layout.addControlsToList(addIntroControls());

    List<String> keys = new ArrayList<>(queryNames.keySet());

    for (int i = 1; i < queryNames.keySet().size() + 1; i++)
    {
      // erstellt controls paarweise horizontal
      if (i > 1 && i % 2 == 0)
      {
        ControlModel searchFields = addSearchControlsTwoColumns(keys.get(i - 2), keys.get(i - 1));

        // wenn letzte Zeile, Suchbutton hinzufügen
        if (i == queryNames.keySet().size())
        {
          searchFields.addControlToControlList(addSearchButton());
        }

        layout.addControlsToList(searchFields);
        query2.add(searchFields);
      } else if (i == queryNames.keySet().size())
      {
        // Zeile mit einem Control falls ungerade Anzahl von Suchfeldern konfiguriert wurde.
        ControlModel searchFields = addSearchControlsOneColumn(keys.get(i - 1));
        searchFields.addControlToControlList(addSearchButton());
        layout.addControlsToList(searchFields);
        query2.add(searchFields);
      }
    }

    layout.addControlsToList(addStatusTextField());
    layout.addControlsToList(addPalButtonControls());
    layout.addControlsToList(addSearchResultControls());
    layout.addControlsToList(addBottomControls());

    layout.draw();

    XListBox palListe = UnoRuntime.queryInterface(XListBox.class, layout.getControl("palListe"));

    if (palListe == null)
      return;

    int count = 0;
    short itemToHighlightPos = 0;
    for (Dataset result : dj.getLOS())
    {
      DJDataset ds = (DJDataset) result;

      palListe.addItem(buildListBoxString(ds), (short) count);
      cachedPAL.add(new DJDatasetListElement(ds));

      if (ds.isSelectedDataset())
        itemToHighlightPos = (short) count;

      count++;
    }

    palListe.selectItemPos(itemToHighlightPos, true);
  }

  private XKeyHandler keyHandler = new XKeyHandler()
  {
    @Override
    public void disposing(EventObject arg0)
    {
      // unused
    }

    @Override
    public boolean keyReleased(com.sun.star.awt.KeyEvent arg0)
    {
      if (arg0.KeyCode == Key.RETURN)
      {
        // visuelle Rückmeldung. Fokusiert den Suchknopf beim Drücken von Return.
        // Suchknopf wird je nach OS-Theme farbig markiert.
        XControl xButton = layout.getControl("startSearch");

        if (xButton == null)
          return false;

        XWindow xWnd = UnoRuntime.queryInterface(XWindow.class, xButton);

        if (xWnd == null)
          return false;

        xWnd.setFocus();

        search();
      }

      return false;
    }

    @Override
    public boolean keyPressed(com.sun.star.awt.KeyEvent arg0)
    {
      return false;
    }
  };

  private ControlModel addIntroControls()
  {
    List<SimpleEntry<ControlProperties, XControl>> introControls = new ArrayList<>();

    introControls.add(layout.convertToXControl(new ControlProperties(ControlType.LABEL,
        "introLabel", 0, 30, 100, 0,
        new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
            new Object[] { "Sie können nach Vorname, Nachname, Email und Orga-Einheit suchen" }))));

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, introControls, Optional.empty());
  }

  private ControlModel addSearchControlsTwoColumns(String firstKey, String secondKey)
  {
    List<SimpleEntry<ControlProperties, XControl>> searchControls = new ArrayList<>();

    searchControls.add(layout.convertToXControl(new ControlProperties(ControlType.LABEL, firstKey,
        0, 30, 20, 0, new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
            new Object[] { firstKey }))));

    searchControls
        .add(layout.convertToXControl(new ControlProperties(ControlType.EDIT, firstKey + "Edit", 0,
            30, 25, 0, new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { firstKey + "Edit" }))));

    searchControls.add(layout.convertToXControl(new ControlProperties(ControlType.LABEL, secondKey,
        0, 30, 20, 0, new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
            new Object[] { secondKey }))));

    searchControls
        .add(layout.convertToXControl(new ControlProperties(ControlType.EDIT, secondKey + "Edit", 0,
            30, 25, 0, new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { secondKey + "Edit" }))));

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, searchControls, Optional.empty());
  }

  private ControlModel addSearchControlsOneColumn(String firstKey)
  {
    List<SimpleEntry<ControlProperties, XControl>> searchControls = new ArrayList<>();

    searchControls.add(layout.convertToXControl(new ControlProperties(ControlType.LABEL, firstKey,
        0, 30, 20, 0, new SimpleEntry<String[], Object[]>(new String[] { XLabelProperties.LABEL },
            new Object[] { firstKey }))));

    searchControls
        .add(layout.convertToXControl(new ControlProperties(ControlType.EDIT, firstKey + "Edit", 0,
            30, 25, 0, new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { firstKey }))));

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, searchControls, Optional.empty());
  }

  private SimpleEntry<ControlProperties, XControl> addSearchButton()
  {
    SimpleEntry<ControlProperties, XControl> startSearchBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "startSearch", 0, 30, 10, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Suchen" })));
    UnoRuntime.queryInterface(XButton.class, startSearchBtn.getValue())
        .addActionListener(startSearchBtnActionListener);

    return startSearchBtn;
  }

  private ControlModel addSearchResultControls()
  {
    List<SimpleEntry<ControlProperties, XControl>> searchResultControls = new ArrayList<>();

    SimpleEntry<ControlProperties, XControl> searchResult = layout
        .convertToXControl(new ControlProperties(ControlType.LIST_BOX, "searchResultList", 0, 150,
            45, 0, new SimpleEntry<String[], Object[]>(new String[] {}, new Object[] {})));
    UnoRuntime.queryInterface(XListBox.class, searchResult.getValue()).setMultipleMode(true);

    SimpleEntry<ControlProperties, XControl> addToPal = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "addToPalBtn", 0, 30, 10, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "->" })));
    UnoRuntime.queryInterface(XButton.class, addToPal.getValue())
        .addActionListener(addToPalActionListener);

    SimpleEntry<ControlProperties, XControl> palListe = layout
        .convertToXControl(new ControlProperties(ControlType.LIST_BOX, "palListe", 0, 150, 45, 0,
            new SimpleEntry<String[], Object[]>(new String[] {}, new Object[] {})));

    XListBox palListBox = UnoRuntime.queryInterface(XListBox.class, palListe.getValue());
    palListBox.setMultipleMode(true);
    palListBox.addItemListener(palListBoxItemListener);

    searchResultControls.add(searchResult);
    searchResultControls.add(addToPal);
    searchResultControls.add(palListe);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, searchResultControls,
        Optional.empty());
  }

  private ControlModel addStatusTextField()
  {
    List<SimpleEntry<ControlProperties, XControl>> statusControls = new ArrayList<>();

    statusControls
        .add(layout.convertToXControl(new ControlProperties(ControlType.LABEL, "statusText", 0, 30,
            100, 0, new SimpleEntry<String[], Object[]>(new String[] {}, new Object[] { "" }))));

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, statusControls, Optional.empty());
  }

  private ControlModel addPalButtonControls()
  {
    List<SimpleEntry<ControlProperties, XControl>> bottomControls = new ArrayList<>();

    SimpleEntry<ControlProperties, XControl> deleteBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "delBtn", 0, 30, 25, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Löschen" })));
    UnoRuntime.queryInterface(XButton.class, deleteBtn.getValue())
        .addActionListener(deleteBtnActionListener);

    SimpleEntry<ControlProperties, XControl> editBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "editBtn", 0, 30, 25, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Bearbeiten" })));
    UnoRuntime.queryInterface(XButton.class, editBtn.getValue())
        .addActionListener(editBtnActionListener);

    SimpleEntry<ControlProperties, XControl> copyBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "copyBtn", 0, 30, 25, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Kopieren" })));
    UnoRuntime.queryInterface(XButton.class, copyBtn.getValue())
        .addActionListener(copyBtnActionListener);

    SimpleEntry<ControlProperties, XControl> newBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "newBtn", 0, 30, 25, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Neu" })));
    UnoRuntime.queryInterface(XButton.class, newBtn.getValue())
        .addActionListener(newBtnActionListener);

    bottomControls.add(deleteBtn);
    bottomControls.add(editBtn);
    bottomControls.add(copyBtn);
    bottomControls.add(newBtn);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls, Optional.empty());
  }

  private ControlModel addBottomControls()
  {
    List<SimpleEntry<ControlProperties, XControl>> bottomControls = new ArrayList<>();

    SimpleEntry<ControlProperties, XControl> abortBtn = layout
        .convertToXControl(new ControlProperties(ControlType.BUTTON, "abortBtn", 0, 30, 20, 0,
            new SimpleEntry<String[], Object[]>(new String[] { XButtonProperties.LABEL },
                new Object[] { "Abbrechen" })));
    UnoRuntime.queryInterface(XButton.class, abortBtn.getValue())
        .addActionListener(abortBtnActionListener);

    bottomControls.add(abortBtn);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls,
        Optional.of(Dock.BOTTOM));
  }

  private AbstractWindowListener xWindowListener = new AbstractWindowListener()
  {
    @Override
    public void disposing(EventObject arg0)
    {
      WollMuxEventHandler.getInstance().handlePALChangedNotify();
    }
  };

  private AbstractItemListener palListBoxItemListener = event -> {
    cachedPAL.get(event.Selected).getDataset().select();
    
    WollMuxEventHandler.getInstance().handlePALChangedNotify();
  };

  private AbstractActionListener startSearchBtnActionListener = event -> search();

  private AbstractActionListener addToPalActionListener = event -> addToPAL();

  private void addToPAL()
  {
    XControl xControlResults = layout.getControl("searchResultList");

    if (xControlResults == null)
      return;

    XListBox xListBoxResults = UnoRuntime.queryInterface(XListBox.class, xControlResults);

    if (xListBoxResults == null)
      return;

    short[] selectedItemsPos = xListBoxResults.getSelectedItemsPos();

    List<DJDatasetListElement> entriesAdded = new ArrayList<>();
    for (short index : selectedItemsPos)
    {
      cachedPAL.add(resultDJDatasetList.get(index));
      entriesAdded.add(resultDJDatasetList.get(index));
    }

    String[] selectedItems = xListBoxResults.getSelectedItems();

    XControl xControlPAL = layout.getControl("palListe");

    if (xControlPAL == null)
      return;

    XListBox xListBoxPal = UnoRuntime.queryInterface(XListBox.class, xControlPAL);

    if (xListBoxPal == null)
      return;

    xListBoxPal.addItems(selectedItems, (short) (xListBoxPal.getItemCount() + 1));

    for (DJDatasetListElement entry : entriesAdded) {
      entry.getDataset().copy();
    }
  }

  private AbstractActionListener deleteBtnActionListener = event -> removeFromPAL();

  private void removeFromPAL()
  {
    XControl xControlPAL = layout.getControl("palListe");

    if (xControlPAL == null)
      return;

    XListBox xListBoxPal = UnoRuntime.queryInterface(XListBox.class, xControlPAL);

    if (xListBoxPal == null)
      return;

    short[] selectedItemsPos = xListBoxPal.getSelectedItemsPos();

    for (short pos : selectedItemsPos)
    {
      xListBoxPal.removeItems(pos, (short) 1);
      cachedPAL.remove(pos);
    }
  }

  private AbstractActionListener editBtnActionListener = event -> editEntry();

  private void editEntry()
  {
    // TODO: editEntryDialog(); (DatensatzBearbeiten.java)
  }

  private AbstractActionListener copyBtnActionListener = event -> copyEntry();

  private void copyEntry()
  {
    XControl xControlResults = layout.getControl("searchResultList");

    if (xControlResults == null)
      return;

    XListBox xListBoxResults = UnoRuntime.queryInterface(XListBox.class, xControlResults);

    short[] sel = xListBoxResults.getSelectedItemsPos();

    for (short index : sel)
    {
      cachedPAL.add(
          new DJDatasetListElement(copyDJDataset(resultDJDatasetList.get(index).getDataset())));
    }

    XControl xControlPAL = layout.getControl("palListe");

    if (xControlPAL == null)
      return;

    XListBox xListBoxPal = UnoRuntime.queryInterface(XListBox.class, xControlPAL);

    sel = xListBoxPal.getSelectedItemsPos();
    for (short index : sel)
    {
      DJDataset datasetCopy = copyDJDataset(cachedPAL.get(index).getDataset());
      cachedPAL.add(new DJDatasetListElement(copyDJDataset(datasetCopy)));

      xListBoxPal.addItem(buildListBoxString(datasetCopy),
          (short) (xListBoxPal.getItemCount() + 1));
    }
  }

  private DJDataset copyDJDataset(DJDataset orig)
  {
    DJDataset newDS = orig.copy();
    try
    {
      newDS.set("Rolle", L.m("Kopie"));
    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }

    return newDS;
  }

  private AbstractActionListener newBtnActionListener = event -> {
    // TODO: create new PAL Entry
  };

  private AbstractActionListener abortBtnActionListener = event -> {
    WollMuxEventHandler.getInstance().handlePALChangedNotify();
    dialogFactory.closeDialog();
  };

  private void setListElements(QueryResults data)
  {
    if (data == null)
    {
      LOGGER.debug("PersoenlicheAbsenderlisteVeralten: setListElements: queryresults is NULL.");
      return;
    }

    resultDJDatasetList = new ArrayList<>();

    data.forEach(item -> {
      DJDatasetListElement element = new DJDatasetListElement((DJDataset) item);
      resultDJDatasetList.add(element);
    });

    Collections.sort(resultDJDatasetList);

    XControl xControl = layout.getControl("searchResultList");

    if (xControl == null)
      return;

    XListBox xListBox = UnoRuntime.queryInterface(XListBox.class, xControl);

    if (xListBox == null)
      return;

    if (xListBox.getItemCount() > 0)
    {
      xListBox.removeItems((short) 0, xListBox.getItemCount());
    }

    for (int i = 0; i < resultDJDatasetList.size(); i++)
    {
      DJDataset ds = resultDJDatasetList.get(i).getDataset();
      xListBox.addItem(buildListBoxString(ds), (short) i);
    }
  }

  private String buildListBoxString(DJDataset ds)
  {
    String dbNachname = "";
    String dbVorname = "";
    String dbOrgaKurz = "";
    String dbRolle = "";

    try
    {
      dbRolle = ds.get("Rolle") == null || ds.get("Rolle").isEmpty() ? "" : "(" + ds.get("Rolle") + ")";
      dbNachname = ds.get("Nachname") == null ? "" : ds.get("Nachname");
      dbVorname = ds.get("Vorname") == null ? "" : ds.get("Vorname");
      dbOrgaKurz = ds.get("OrgaKurz") == null ? "" : ds.get("OrgaKurz");

    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }

    return dbRolle + dbNachname + ", " + dbVorname + " " + dbOrgaKurz;
  }

  private void search()
  {
    Map<String, String> q = new HashMap<>();
    XControl statusTextXControl = layout.getControl("statusText");

    if (statusTextXControl == null)
      return;

    XFixedText statusText = UnoRuntime.queryInterface(XFixedText.class, statusTextXControl);

    if (statusText == null)
      return;

    statusText.setText("");

    for (ControlModel model : query2)
    {
      for (int i = 0; i < model.getControls().size(); i++)
      {

        XFixedText xLabel = UnoRuntime.queryInterface(XFixedText.class,
            model.getControls().get(i).getValue());

        if (xLabel == null)
          continue;

        String labelText = xLabel.getText();

        XTextComponent editField = UnoRuntime.queryInterface(XTextComponent.class,
            model.getControls().get(i + 1).getValue());

        if (editField == null)
          continue;

        String mappedDBCloumnName = queryNames.get(labelText);

        if (mappedDBCloumnName == null || mappedDBCloumnName.isEmpty())
          continue;

        q.put(mappedDBCloumnName, editField.getText());
      }
    }

    QueryResults results = null;
    try
    {
      results = Search.search(q, dj);
    } catch (TimeoutException | IllegalArgumentException x1)
    {
      LOGGER.error("", x1);
      statusText.setText(
          "Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen.\n"
              + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten\n"
              + "Suchbegriff, der auf zu viele Ergebnisse zutrifft.\n"
              + "Bitte versuchen Sie eine andere, präzisere Suchanfrage.");
    }

    setListElements(results);
  }

}
