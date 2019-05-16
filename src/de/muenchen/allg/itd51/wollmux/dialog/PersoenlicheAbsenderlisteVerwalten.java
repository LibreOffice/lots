/*
 * Dateiname: PersoenlicheAbsenderlisteVerwalten.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Hinzufügen/Entfernen Dialog des BKS
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
import com.google.common.collect.Lists;
import com.sun.star.awt.Key;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XKeyHandler;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.AsyncLdapSearch;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.Search;
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
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung einen Dialog mit
 * konfigurierbaren Suchfeldern zum Hinzufügen/Entfernen von Einträgen der Persönlichen
 * Absenderliste auf.
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
  private List<DJDataset> resultDJDatasetList = null;
  private List<DJDataset> cachedPAL = new ArrayList<>();
  private INotify palListener;

  private int count = 0;
  private short itemToHighlightPos = 0;

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
  public PersoenlicheAbsenderlisteVerwalten(ConfigThingy conf, DatasourceJoiner dj,
      INotify palListener)
  {
    this.dj = dj;
    this.palListener = palListener;

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
    XWindow dialogWindow = dialogFactory.createDialog(780, 450, 0xF2F2F2);
    dialogWindow.addWindowListener(xWindowListener);

    Object toolkit = null;
    XToolkit xToolkit = null;
    try
    {
      toolkit = UNO.xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", UNO.defaultContext);
      xToolkit = UNO.XToolkit(toolkit);
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
        boolean isLastRow = i == queryNames.keySet().size();

        ControlModel searchFields = addSearchControlsTwoColumns(keys.get(i - 2), keys.get(i - 1),
            isLastRow);

        layout.addControlsToList(searchFields);
        query2.add(searchFields);
      } else if (i == queryNames.keySet().size())
      {
        // Zeile mit einem Control-Paar falls ungerade Anzahl von Suchfeldern konfiguriert wurde.
        ControlModel searchFields = addSearchControlsOneColumn(keys.get(i - 1));
        layout.addControlsToList(searchFields);
        query2.add(searchFields);
      }
    }

    layout.addControlsToList(addStatusTextField());
    layout.addControlsToList(addPalButtonControls());
    layout.addControlsToList(addSearchResultControls());
    layout.addControlsToList(addBottomControls());

    layout.draw();

    addPalEntriesToListBox();
  }

  private void addPalEntriesToListBox()
  {
    XListBox palListe = UNO.XListBox(layout.getControl("palListe"));

    cachedPAL.clear();
    palListe.removeItems((short) 0, palListe.getItemCount());

    for (Dataset dataset : dj.getLOS())
    {
      DJDataset ds = (DJDataset) dataset;

      Dataset ldapDataset = null;
      try
      {
        ldapDataset = dj.getCachedLdapResultByOID(dataset.get("OID"));
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }

      if (dj.getCachedLdapResults() != null && Search.hasLDAPDataChanged(dataset, ldapDataset,
          DatasourceJoinerFactory.getDatasourceJoiner()))
        palListe.addItem("* " + ds.toString(), (short) count);
      else
        palListe.addItem(ds.toString(), (short) count);

      cachedPAL.add(ds);

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

        XWindow xWnd = UNO.XWindow(xButton);

        if (xWnd == null)
          return false;

        xWnd.setFocus();

        AsyncLdapSearch ldapSearchAsync = new AsyncLdapSearch(buildSearchQuery(),
            DatasourceJoinerFactory.getDatasourceJoiner());
        ldapSearchAsync.runLdapSearchAsync().thenAcceptAsync(result -> {
          setLdapSearchResults(result);
          setStatusText(result);
        });
      } else if (arg0.KeyCode == Key.DELETE)
      {
        // Einträge aus PAL-Listbox entfernen
        XListBox palListe = UNO.XListBox(layout.getControl("palListe"));
        short[] selectedItems = palListe.getSelectedItemsPos();

        boolean firstIteration = true;
        List<Dataset> datasetArray = Lists.newArrayList(dj.getLOS());

        int iteration = 0;
        for (short pos : selectedItems)
        {
          if (firstIteration)
          {
            palListe.removeItems(pos, (short) 1);
            cachedPAL.remove(pos);
            DJDataset ds = (DJDataset) datasetArray.remove(pos);
            ds.remove();
            firstIteration = false;
          } else
          {
            palListe.removeItems((short) (pos - 1 - iteration), (short) 1);
            cachedPAL.remove(pos - 1 - iteration);
            DJDataset ds = (DJDataset) datasetArray.remove(pos - 1 - iteration);
            ds.remove();
            iteration++;
          }
        }

        WollMuxEventHandler.getInstance().handlePALChangedNotify();
      }

      return true;
    }

    @Override
    public boolean keyPressed(com.sun.star.awt.KeyEvent arg0)
    {
      return false;
    }
  };

  private ControlModel addIntroControls()
  {
    List<ControlProperties> introControls = new ArrayList<>();

    ControlProperties label = new ControlProperties(ControlType.LABEL, "introLabel");
    label.setControlPercentSize(100, 20);
    label.setLabel("Sie können nach Vorname, Nachname, Email und Orga-Einheit suchen");

    introControls.add(label);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, introControls, Optional.empty());
  }

  private ControlModel addSearchControlsTwoColumns(String firstKey, String secondKey,
      boolean isLastRow)
  {
    List<ControlProperties> searchControls = new ArrayList<>();

    ControlProperties label = new ControlProperties(ControlType.LABEL, firstKey + "Label");
    label.setControlPercentSize(10, 30);
    label.setLabel(firstKey);
    searchControls.add(label);

    ControlProperties edit = new ControlProperties(ControlType.EDIT, firstKey + "Edit");
    edit.setControlPercentSize(30, 30);
    searchControls.add(edit);

    ControlProperties labelSecond = new ControlProperties(ControlType.LABEL, secondKey + "Label");
    labelSecond.setControlPercentSize(10, 30);
    labelSecond.setLabel(secondKey);
    searchControls.add(labelSecond);

    ControlProperties editSecond = new ControlProperties(ControlType.EDIT, secondKey + "Edit");
    editSecond.setControlPercentSize(30, 30);
    searchControls.add(editSecond);

    ControlProperties labelPlaceholder = null;
    ControlProperties startSearchBtn = null;

    if (!isLastRow)
    {
      labelPlaceholder = new ControlProperties(ControlType.LABEL, firstKey + "LabelP");
      labelPlaceholder.setControlPercentSize(20, 30);
      labelPlaceholder.setLabel("");
      searchControls.add(labelPlaceholder);
    } else
    {
      startSearchBtn = new ControlProperties(ControlType.BUTTON, "startSearch");
      startSearchBtn.setControlPercentSize(20, 30);
      startSearchBtn.setLabel("Suchen");
      UNO.XButton(startSearchBtn.getXControl()).addActionListener(startSearchBtnActionListener);
      searchControls.add(startSearchBtn);
    }

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, searchControls, Optional.empty());
  }

  private ControlModel addSearchControlsOneColumn(String firstKey)
  {
    List<ControlProperties> searchControls = new ArrayList<>();

    ControlProperties label = new ControlProperties(ControlType.LABEL, firstKey);
    label.setControlPercentSize(30, 20);
    label.setLabel(firstKey);

    ControlProperties edit = new ControlProperties(ControlType.EDIT, firstKey);
    edit.setControlPercentSize(70, 20);

    searchControls.add(label);
    searchControls.add(edit);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, searchControls, Optional.empty());
  }

  private ControlModel addSearchResultControls()
  {
    List<ControlProperties> searchResultControls = new ArrayList<>();

    ControlProperties searchResultListBox = new ControlProperties(ControlType.LIST_BOX,
        "searchResultList");
    searchResultListBox.setControlPercentSize(45, 150);
    UNO.XListBox(searchResultListBox.getXControl()).setMultipleMode(true);

    ControlProperties addToPalBtn = new ControlProperties(ControlType.BUTTON, "addToPalBtn");
    addToPalBtn.setControlPercentSize(10, 30);
    addToPalBtn.setLabel("->");
    UNO.XButton(addToPalBtn.getXControl()).addActionListener(addToPalActionListener);

    ControlProperties palListBox = new ControlProperties(ControlType.LIST_BOX, "palListe");
    palListBox.setControlPercentSize(45, 150);
    XListBox palXListBox = UNO.XListBox(palListBox.getXControl());
    palXListBox.setMultipleMode(true);
    palXListBox.addItemListener(palListBoxItemListener);

    searchResultControls.add(searchResultListBox);
    searchResultControls.add(addToPalBtn);
    searchResultControls.add(palListBox);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, searchResultControls,
        Optional.empty());
  }

  private ControlModel addStatusTextField()
  {
    List<ControlProperties> statusControls = new ArrayList<>();

    ControlProperties label = new ControlProperties(ControlType.LABEL, "statusText");
    label.setControlPercentSize(100, 20);

    statusControls.add(label);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, statusControls, Optional.empty());
  }

  private ControlModel addPalButtonControls()
  {
    List<ControlProperties> bottomControls = new ArrayList<>();

    ControlProperties deleteBtn = new ControlProperties(ControlType.BUTTON, "delBtn");
    deleteBtn.setControlPercentSize(25, 25);
    deleteBtn.setMarginBetweenControls(5);
    deleteBtn.setLabel("Löschen");
    UNO.XButton(deleteBtn.getXControl()).addActionListener(deleteBtnActionListener);

    ControlProperties editBtn = new ControlProperties(ControlType.BUTTON, "editBtn");
    editBtn.setControlPercentSize(25, 25);
    editBtn.setMarginBetweenControls(5);
    editBtn.setLabel("Bearbeiten");
    UNO.XButton(editBtn.getXControl()).addActionListener(editBtnActionListener);

    ControlProperties copyBtn = new ControlProperties(ControlType.BUTTON, "copyBtn");
    copyBtn.setControlPercentSize(25, 25);
    copyBtn.setMarginBetweenControls(5);
    copyBtn.setLabel("Kopieren");
    UNO.XButton(copyBtn.getXControl()).addActionListener(copyBtnActionListener);

    ControlProperties newBtn = new ControlProperties(ControlType.BUTTON, "newBtn");
    newBtn.setControlPercentSize(25, 25);
    newBtn.setMarginBetweenControls(5);
    newBtn.setLabel("Neu");
    UNO.XButton(newBtn.getXControl()).addActionListener(newBtnActionListener);

    bottomControls.add(deleteBtn);
    bottomControls.add(editBtn);
    bottomControls.add(copyBtn);
    bottomControls.add(newBtn);

    ControlModel controlModel = new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls,
        Optional.empty());
    controlModel.setBindToControlWidthAndXOffset("palListe");

    return controlModel;
  }

  private ControlModel addBottomControls()
  {
    List<ControlProperties> bottomControls = new ArrayList<>();

    ControlProperties abortBtn = new ControlProperties(ControlType.BUTTON, "abortBtn");
    abortBtn.setControlPercentSize(30, 40);
    abortBtn.setLabel("Abbrechen");
    UNO.XButton(abortBtn.getXControl()).addActionListener(abortBtnActionListener);

    bottomControls.add(abortBtn);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomControls,
        Optional.of(Dock.BOTTOM));
  }

  private AbstractWindowListener xWindowListener = new AbstractWindowListener()
  {
    @Override
    public void disposing(EventObject arg0)
    {
      palListener.dialogClosed();
      WollMuxEventHandler.getInstance().handlePALChangedNotify();
    }
  };

  private AbstractItemListener palListBoxItemListener = event -> {
    cachedPAL.get(event.Selected).select();

    WollMuxEventHandler.getInstance().handlePALChangedNotify();
  };

  private AbstractActionListener startSearchBtnActionListener = event -> {
    AsyncLdapSearch ldapSearchAsync = new AsyncLdapSearch(buildSearchQuery(),
        DatasourceJoinerFactory.getDatasourceJoiner());
    ldapSearchAsync.runLdapSearchAsync().thenAcceptAsync(result -> {
      setLdapSearchResults(result);
      setStatusText(result);
    });
  };

  private AbstractActionListener addToPalActionListener = event -> addToPAL();

  private void addToPAL()
  {
    XControl xControlResults = layout.getControl("searchResultList");

    if (xControlResults == null)
      return;

    XListBox xListBoxResults = UNO.XListBox(xControlResults);

    if (xListBoxResults == null)
      return;

    short[] selectedItemsPos = xListBoxResults.getSelectedItemsPos();

    List<DJDataset> entriesToAdd = new ArrayList<>();
    for (short index : selectedItemsPos)
    {
      DJDataset dsToAdd = resultDJDatasetList.get(index);

      try
      {
        if (dj.getOIDsFromLOS().contains(dsToAdd.get("OID")))
        {
          short res = InfoDialog.showYesNoModal("PAL",
              "Datensatz ist bereits in der Absenderliste vorhanden, Datensatz trotzdem erneut speichern?");

          if (res == MessageBoxResults.YES)
          {
            entriesToAdd.add(dsToAdd);
          }
        } else
        {
          entriesToAdd.add(dsToAdd);
        }
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }
      
      dj.addCachedLdapResult(dsToAdd);
    }

    for (DJDataset entry : entriesToAdd)
    {
      entry.copy();
    }
    
    WollMuxEventHandler.getInstance().handlePALChangedNotify();
    addPalEntriesToListBox();
  }

  private AbstractActionListener deleteBtnActionListener = event -> removeFromPAL();

  private void removeFromPAL()
  {
    XControl xControlPAL = layout.getControl("palListe");
    if (xControlPAL == null)
      return;
    XListBox xListBoxPal = UNO.XListBox(xControlPAL);
    if (xListBoxPal == null)
      return;

    short[] selectedItemsPos = xListBoxPal.getSelectedItemsPos();

    boolean firstIteration = true;
    List<Dataset> datasetArray = Lists.newArrayList(dj.getLOS());

    int iteration = 0;
    for (short pos : selectedItemsPos)
    {
      if (firstIteration)
      {
        xListBoxPal.removeItems(pos, (short) 1);
        cachedPAL.remove(pos);
        DJDataset ds = (DJDataset) datasetArray.remove(pos);
        ds.remove();
        firstIteration = false;
      } else
      {
        xListBoxPal.removeItems((short) (pos - 1 - iteration), (short) 1);
        cachedPAL.remove(pos - 1 - iteration);
        DJDataset ds = (DJDataset) datasetArray.remove(pos - 1 - iteration);
        ds.remove();
        iteration++;
      }
    }

    WollMuxEventHandler.getInstance().handlePALChangedNotify();
  }

  private AbstractActionListener editBtnActionListener = event -> editEntry();

  private void editEntry()
  {
    XListBox pal = UNO.XListBox(layout.getControl("palListe"));
    short selectedPos = pal.getSelectedItemPos();
    DJDataset djDataset = cachedPAL.get(selectedPos);

    Dataset ldapDataset = null;
    try
    {
      ldapDataset = dj.getCachedLdapResultByOID(djDataset.get("OID"));
    } catch (ColumnNotFoundException e)
    {
      LOGGER.error("", e);
    }

    DatensatzBearbeiten editDSWizard = new DatensatzBearbeiten(djDataset, ldapDataset,
        dj.getMainDatasourceSchema());

    short result = editDSWizard.executeWizard();

    if (result == ExecutableDialogResults.OK)
    {
      WollMuxEventHandler.getInstance().handlePALChangedNotify();
      addPalEntriesToListBox();
      LOGGER.debug("Datensatz bearbeiten: DatensatzBearbeiten(): ExecutableDialogResult.OK");
    } else
    {
      LOGGER.debug("Datensatz bearbeiten: DatensatzBearbeiten(): ExecutableDialogResult.CANCEL");
    }
  }

  private AbstractActionListener copyBtnActionListener = event -> copyEntry();

  private void copyEntry()
  {
    XControl xControlPAL = layout.getControl("palListe");

    if (xControlPAL == null)
      return;

    XListBox xListBoxPal = UNO.XListBox(xControlPAL);
    short[] sel = xListBoxPal.getSelectedItemsPos();

    for (short index : sel)
    {
      DJDataset datasetCopy = copyDJDataset(cachedPAL.get(index));
      cachedPAL.add(datasetCopy);
      xListBoxPal.addItem(datasetCopy.toString(),
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
    DJDataset newDataset = dj.newDataset();

    XControl xControlPAL = layout.getControl("palListe");

    if (xControlPAL == null)
      return;

    XListBox xListBoxPal = UNO.XListBox(xControlPAL);

    if (xListBoxPal == null)
      return;

    cachedPAL.add(newDataset);
    xListBoxPal.addItem("Neuer Datensatz", (short) (xListBoxPal.getItemCount() + 1));
  };

  private AbstractActionListener abortBtnActionListener = event -> {
    WollMuxEventHandler.getInstance().handlePALChangedNotify();
    palListener.dialogClosed();
    dialogFactory.closeDialog();
  };

  private void setLdapSearchResults(QueryResults data)
  {
    if (data == null)
    {
      LOGGER.debug("PersoenlicheAbsenderlisteVeralten: setListElements: queryresults is NULL.");
      return;
    }

    resultDJDatasetList = new ArrayList<>();

    data.forEach(item -> resultDJDatasetList.add((DJDataset) item));

    Collections.sort(resultDJDatasetList, DatasourceJoiner.sortPAL);

    XControl xControl = layout.getControl("searchResultList");

    if (xControl == null)
      return;

    XListBox xListBox = UNO.XListBox(xControl);

    if (xListBox == null)
      return;

    if (xListBox.getItemCount() > 0)
    {
      xListBox.removeItems((short) 0, xListBox.getItemCount());
    }

    for (int i = 0; i < resultDJDatasetList.size(); i++)
    {
      xListBox.addItem(resultDJDatasetList.get(i).toString(), (short) i);
    }
  }

  private Map<String, String> buildSearchQuery()
  {
    Map<String, String> resultSearchQuery = new HashMap<>();

    for (ControlModel model : query2)
    {
      for (int i = 0; i < model.getControls().size(); i++)
      {

        XFixedText xLabel = UNO.XFixedText(model.getControls().get(i).getXControl());

        if (xLabel == null)
          continue;

        String labelText = xLabel.getText();

        if (labelText.isEmpty())
          continue;

        XTextComponent editField = UNO.XTextComponent(model.getControls().get(i + 1).getXControl());

        if (editField == null)
          continue;

        String mappedDBCloumnName = queryNames.get(labelText);

        if (mappedDBCloumnName == null || mappedDBCloumnName.isEmpty())
          continue;

        resultSearchQuery.put(mappedDBCloumnName, editField.getText());
      }
    }

    return resultSearchQuery;
  }

  private void setStatusText(QueryResults ldapSearchResults)
  {
    XControl statusTextXControl = layout.getControl("statusText");

    if (statusTextXControl == null)
      return;

    XFixedText statusText = UNO.XFixedText(statusTextXControl);

    if (statusText == null)
      return;

    statusText.setText("");

    if (ldapSearchResults == null)
    {
      statusText.setText(
          "Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen.\n"
              + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten\n"
              + "Suchbegriff, der auf zu viele Ergebnisse zutrifft.\n"
              + "Bitte versuchen Sie eine andere, präzisere Suchanfrage.");
    } else if (ldapSearchResults.isEmpty())
    {
      statusText.setText("Es wurde nichts gefunden.");
    }
  }

}
