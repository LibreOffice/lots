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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.sun.star.awt.Key;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XKeyHandler;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
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
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
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

  private List<DJDataset> resultDJDatasetList = null;

  private List<DJDataset> cachedPAL = new ArrayList<>();

  private INotify palListener;

  private XButton searchBtn;

  private XListBox searchResultList;

  private XListBox palListe;

  private XTextComponent txtFieldNachname;

  private XTextComponent txtFieldVorname;

  private XTextComponent txtFieldOrga;

  private XTextComponent txtFieldEMail;

  private XDialog dialog;

  private int count = 0;

  private short itemToHighlightPos = 0;

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

    createGUI();
  }

  /**
   * Erzeugt das GUI.
   *
   * @author Björn Ranft
   */
  private void createGUI()
  {
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
    XContainerWindowProvider provider = null;

    try
    {
      provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }

    if (provider == null)
    {
      LOGGER.error("PersoenlicheAbsenderliste: createGUI(): XContainerProvider is NULL.");
      return;
    }

    XWindow window = provider.createContainerWindow(
        "vnd.sun.star.script:WollMux.pal_dialog?location=application", "", peer, null);
    window.addWindowListener(xWindowListener);

    XToolkit xToolkit = null;
    try
    {
      xToolkit = UNO.XToolkit(
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", UNO.defaultContext));
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    XExtendedToolkit extToolkit = UnoRuntime.queryInterface(XExtendedToolkit.class, xToolkit);
    extToolkit.addKeyHandler(keyHandler);

    XControlContainer controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);

    palListe = UNO.XListBox(controlContainer.getControl("palListe"));
    palListe.setMultipleMode(true);

    searchResultList = UNO.XListBox(controlContainer.getControl("searchResultList"));
    searchResultList.setMultipleMode(true);

    txtFieldNachname = UNO.XTextComponent(controlContainer.getControl("txtNachname"));
    txtFieldVorname = UNO.XTextComponent(controlContainer.getControl("txtVorname"));
    txtFieldEMail = UNO.XTextComponent(controlContainer.getControl("txtEmail"));
    txtFieldOrga = UNO.XTextComponent(controlContainer.getControl("txtOrga"));

    XButton okBtn = UNO.XButton(controlContainer.getControl("okBtn"));
    okBtn.addActionListener(okActionListener);

    XButton abortBtn = UNO.XButton(controlContainer.getControl("abortBtn"));
    abortBtn.addActionListener(abortBtnActionListener);

    searchBtn = UNO.XButton(controlContainer.getControl("btnSearch"));
    searchBtn.addActionListener(startSearchBtnActionListener);

    XButton newBtn = UNO.XButton(controlContainer.getControl("btnNew"));
    newBtn.addActionListener(newBtnActionListener);

    XButton editBtn = UNO.XButton(controlContainer.getControl("btnEdit"));
    editBtn.addActionListener(editBtnActionListener);

    XButton copyBtn = UNO.XButton(controlContainer.getControl("btnCopy"));
    copyBtn.addActionListener(copyBtnActionListener);

    XButton delBtn = UNO.XButton(controlContainer.getControl("btnDel"));
    delBtn.addActionListener(deleteBtnActionListener);

    XButton addToPalBtn = UNO.XButton(controlContainer.getControl("addToPALBtn"));
    addToPalBtn.addActionListener(addToPalActionListener);

    addPalEntriesToListBox();

    dialog = UnoRuntime.queryInterface(XDialog.class, window);
    dialog.execute();
  }

  private void addPalEntriesToListBox()
  {
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
        XWindow xWnd = UNO.XWindow(searchBtn);

        if (xWnd == null)
          return false;

        xWnd.setFocus();

        AsyncLdapSearch ldapSearchAsync = new AsyncLdapSearch(buildSearchQuery(),
            DatasourceJoinerFactory.getDatasourceJoiner());
        ldapSearchAsync.runLdapSearchAsync().thenAcceptAsync(result -> {
          setLdapSearchResults(result);
          showInfoDialog(result);
        });
      } else if (arg0.KeyCode == Key.DELETE)
      {
        // Einträge aus PAL-Listbox entfernen
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

  private AbstractWindowListener xWindowListener = new AbstractWindowListener()
  {
    @Override
    public void disposing(EventObject arg0)
    {
      palListener.dialogClosed();
      WollMuxEventHandler.getInstance().handlePALChangedNotify();
      dialog.endExecute();
    }
  };

  private AbstractActionListener startSearchBtnActionListener = event -> {
    AsyncLdapSearch ldapSearchAsync = new AsyncLdapSearch(buildSearchQuery(),
        DatasourceJoinerFactory.getDatasourceJoiner());
    ldapSearchAsync.runLdapSearchAsync().thenAcceptAsync(result -> {
      setLdapSearchResults(result);
      showInfoDialog(result);
    });
  };

  private AbstractActionListener addToPalActionListener = event -> addToPAL();

  private void addToPAL()
  {
    short[] selectedItemsPos = searchResultList.getSelectedItemsPos();

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
    short[] selectedItemsPos = palListe.getSelectedItemsPos();

    boolean firstIteration = true;
    List<Dataset> datasetArray = Lists.newArrayList(dj.getLOS());

    int iteration = 0;
    for (short pos : selectedItemsPos)
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

  private AbstractActionListener editBtnActionListener = event -> editEntry();

  private void editEntry()
  {
    short selectedPos = palListe.getSelectedItemPos();
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
    short[] sel = palListe.getSelectedItemsPos();

    for (short index : sel)
    {
      DJDataset datasetCopy = copyDJDataset(cachedPAL.get(index));
      cachedPAL.add(datasetCopy);
      palListe.addItem(datasetCopy.toString(), (short) (palListe.getItemCount() + 1));
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

    cachedPAL.add(newDataset);
    palListe.addItem("Neuer Datensatz", (short) (palListe.getItemCount() + 1));
  };

  private AbstractActionListener abortBtnActionListener = event -> {
    WollMuxEventHandler.getInstance().handlePALChangedNotify();
    palListener.dialogClosed();
    dialog.endExecute();
  };

  private AbstractActionListener okActionListener = event -> {
    DJDataset selectedDataset = cachedPAL.get(palListe.getSelectedItemPos());

    if (selectedDataset == null)
    {
      dialog.endExecute();
      return;
    }

    selectedDataset.select();
    WollMuxEventHandler.getInstance().handlePALChangedNotify();
    palListener.dialogClosed();
    dialog.endExecute();
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

    if (searchResultList.getItemCount() > 0)
    {
      searchResultList.removeItems((short) 0, searchResultList.getItemCount());
    }

    for (int i = 0; i < resultDJDatasetList.size(); i++)
    {
      searchResultList.addItem(resultDJDatasetList.get(i).toString(), (short) i);
    }
  }

  private Map<String, String> buildSearchQuery()
  {
    Map<String, String> resultSearchQuery = new HashMap<>();

    resultSearchQuery.put("Vorname", txtFieldVorname.getText());
    resultSearchQuery.put("Nachname", txtFieldNachname.getText());
    resultSearchQuery.put("Mail", txtFieldEMail.getText());
    resultSearchQuery.put("OrgaKurz", txtFieldOrga.getText());

    return resultSearchQuery;
  }

  private void showInfoDialog(QueryResults ldapSearchResults)
  {
    if (ldapSearchResults == null)
    {
      InfoDialog.showInfoModal("Fehler.",
          "Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen.\n"
              + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten\n"
              + "Suchbegriff, der auf zu viele Ergebnisse zutrifft.\n"
              + "Bitte versuchen Sie eine andere, präzisere Suchanfrage.");

    } else if (ldapSearchResults.isEmpty())
    {
      InfoDialog.showInfoModal("Fehler.", "Es wurde nichts gefunden.");
    }
  }

}
