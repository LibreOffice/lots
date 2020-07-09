/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
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
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractFocusListener;
import de.muenchen.allg.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.db.AsyncLdapSearch;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl.LOSDJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPALChangedNotify;

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

  private boolean palListeHasFocus = false;

  /**
   * Erzeugt einen neuen Dialog.
   *
   * @param dj
   *          der DatasourceJoiner, der die zu bearbeitende Liste verwaltet.
   * @param palListener
   *          Listener, which is called if a pal is changed.
   */
  public PersoenlicheAbsenderlisteVerwalten(DatasourceJoiner dj,
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

    XExtendedToolkit extToolkit = UNO.XExtendedToolkit(xToolkit);
    extToolkit.addKeyHandler(keyHandler);

    XControlContainer controlContainer = UNO.XControlContainer(window);

    palListe = UNO.XListBox(controlContainer.getControl("palListe"));
    palListe.setMultipleMode(true);

    searchResultList = UNO.XListBox(controlContainer.getControl("searchResultList"));
    searchResultList.setMultipleMode(true);

    txtFieldNachname = UNO.XTextComponent(controlContainer.getControl("txtNachname"));
    txtFieldVorname = UNO.XTextComponent(controlContainer.getControl("txtVorname"));
    txtFieldEMail = UNO.XTextComponent(controlContainer.getControl("txtEmail"));
    txtFieldOrga = UNO.XTextComponent(controlContainer.getControl("txtOrga"));

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

    UNO.XWindow(palListe).addFocusListener(new AbstractFocusListener()
    {
      @Override
      public void focusGained(com.sun.star.awt.FocusEvent event)
      {
        palListeHasFocus = true;
      }

      @Override
      public void focusLost(com.sun.star.awt.FocusEvent event)
      {
        palListeHasFocus = false;
      }
    });

    dialog = UNO.XDialog(window);
    dialog.execute();
  }

  private void addPalEntriesToListBox()
  {
    cachedPAL.clear();
    palListe.removeItems((short) 0, palListe.getItemCount());

    for (Dataset dataset : dj.getLOS())
    {
      boolean valueChanged = false;
      LOSDJDataset ds = (LOSDJDataset) dataset;

      if (ds.getLOS() != null && !ds.getLOS().isEmpty())
      {
        for (String attribute : dj.getMainDatasourceSchema())
        {
          if (ds.isDifferentFromLdapDataset(attribute, ds))
          {
            valueChanged = true;
            break;
          } else
          {
            valueChanged = false;
          }
        }
      }

      if (valueChanged)
      {
        palListe.addItem("* " + ds.toString(), (short) count);
      } else
      {
        palListe.addItem(ds.toString(), (short) count);
      }

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
      } else if (arg0.KeyCode == Key.DELETE && palListeHasFocus)
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

        new OnPALChangedNotify().emit();
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
      new OnPALChangedNotify().emit();
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

      if (!dj.getLOS().isEmpty())
      {
        for (Dataset ds : dj.getLOS())
        {
          try
          {
            if (ds.get("OID").equals(dsToAdd.get("OID"))) {
              short res = InfoDialog.showYesNoModal("PAL",
                  "Datensatz ist bereits in der Absenderliste vorhanden, Datensatz trotzdem erneut speichern?");

              if (res == MessageBoxResults.YES)
              {
                entriesToAdd.add(dsToAdd);
                break;
              }
            } else
            {
              entriesToAdd.add(dsToAdd);
              break;
            }
          } catch (ColumnNotFoundException e)
          {
            LOGGER.error("", e);
          }
        }
      } else
      {
        entriesToAdd.add(dsToAdd);
      }
    }

    for (DJDataset entry : entriesToAdd)
    {
      try
      {
        // Es kann im produktiven LDAP eigentlich nicht vorkommen das OID leer oder '*'
        // ist (Pflichtfeld produktiv, wird automatisch beim Anlegen eines neuen Users erzeugt).
        // Der LDAP im Entwicklungs und Testnetz lässt das allerdings zu, die LDAP-Abfrage bei
        // nicht gesetzter OID ist dann eine wildcard-Abfrage (*), es tauchen dann daher
        // alle User in der PAL auf.
        // Wildcard-Suche ist daher erlaubt, in die PAL übernehmen jedoch nicht.
        String oid = entry.get("OID");
        if (oid == null || oid.isEmpty() || "*".equals(oid))
        {
          InfoDialog.showInfoModal("Fehler.", "Der Datensatz enthält keine gültige OID und wird "
              + "daher nicht in die PAL übernommen.");
          continue;
        }
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }

      entry.copy();
    }

    new OnPALChangedNotify().emit();
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

    new OnPALChangedNotify().emit();
  }

  private AbstractActionListener editBtnActionListener = event -> editEntry(
      palListe.getSelectedItemPos());

  private void editEntry(short index)
  {
    if (index == -1)
    {
      LOGGER.debug(
          "PersoenlicheAbsenderListe: editEntry(): index -1 is not a valid value.returning..");
      return;
    }

    DJDataset djDataset = cachedPAL.get(index);

    DatensatzBearbeitenWizardController controller = new DatensatzBearbeitenWizardController((LOSDJDataset) djDataset,
        dj.getMainDatasourceSchema());
    short result = controller.startWizard();

    if (result == ExecutableDialogResults.OK)
    {
      new OnPALChangedNotify().emit();
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
    short newItemIndex = (palListe.getItemCount());
    palListe.addItem("Neuer Datensatz", newItemIndex);
    editEntry(newItemIndex);
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

    String vorname = txtFieldVorname.getText();
    if (!vorname.isEmpty())
    {
      vorname += !vorname.endsWith("*") ? "*" : "";
      resultSearchQuery.put("Vorname", vorname);
    }
    String nachname = txtFieldNachname.getText();
    if (!nachname.isEmpty())
    {
      nachname += !nachname.endsWith("*") ? "*" : "";
      resultSearchQuery.put("Nachname", nachname);
    }
    String mail = txtFieldEMail.getText();
    if (!mail.isEmpty())
    {
      mail += !mail.endsWith("*") ? "*" : "";
      resultSearchQuery.put("Mail", mail);
    }
    String orga = txtFieldOrga.getText();
    if (!orga.isEmpty())
    {
      orga += !orga.endsWith("*") ? "*" : "";
      resultSearchQuery.put("OrgaKurz", orga);
    }

    return resultSearchQuery;
  }

  private void showInfoDialog(QueryResults ldapSearchResults)
  {
    if (ldapSearchResults == null)
    {
      InfoDialog.showInfoModal("Fehler.",
          "Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen. "
              + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten "
              + "Suchbegriff, der auf zu viele Ergebnisse zutrifft. "
              + "Bitte versuchen Sie eine andere, präzisere Suchanfrage.");

    } else if (ldapSearchResults.isEmpty())
    {
      InfoDialog.showInfoModal("Fehler.", "Es wurde nichts gefunden.");
    }
  }

}
