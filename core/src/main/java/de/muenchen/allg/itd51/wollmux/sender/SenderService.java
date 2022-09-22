/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.sender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.db.AsyncSearch;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.ColumnTransformer;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.Datasources;
import de.muenchen.allg.itd51.wollmux.db.DummyDatasourceWithMessagebox;
import de.muenchen.allg.itd51.wollmux.db.QueryPart;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.interfaces.XPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.interfaces.XPALProvider;
import de.muenchen.allg.itd51.wollmux.sender.dialog.PersoenlicheAbsenderlisteVerwalten;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Entry point for editing or selecting sender and adding or removing sender from the list.
 *
 * It is implemented as a singleton.
 */
public class SenderService implements XPALProvider
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SenderService.class);

  public static final String OVERRIDE_FRAG_DB_SPALTE = "OVERRIDE_FRAG_DB_SPALTE";

  private static SenderService instance;

  /**
   * Used by {@link #getSelectedDatasetTransformed()}; can be null!
   */
  protected ColumnTransformer columnTransformer;

  protected SenderCache cache;

  /**
   * Contains all registered {@code SenderBox} objects.
   */
  private List<XPALChangeEventListener> registeredPALChangeListener;

  /**
   * The selected data set. Null only if data is empty.
   */
  protected Sender selectedSender = null;

  /**
   * The data source to which find(), getLOS(), etc. refer.
   */
  protected Datasource mainDatasource;

  /**
   * A list containing the {@link Dataset}s that are linked to a background
   * database, but whose key was no longer found in it and therefore
   * could not be updated.
   */
  protected List<Sender> lostDatasets = new ArrayList<>(0);

  /**
   * List of all LOSDJDatasets.
   * The list must be ordered so that datasets with the same key
   * can be identified by their position in the list.
   */
  protected List<Sender> data = new ArrayList<>();

  protected String overrideFragDbSpalte;

  /**
   * The string which, in the string representation of PAL entries,
   * separates the key of the PAL entry from the rest of the PAL entry
   * (see also documentation of the methods of the {@link XPALProvider} interface.
   */
  public static final String SENDER_KEY_SEPARATOR = "§§%=%§§";

  public static SenderService getInstance()
  {
    if (instance == null)
    {
      ConfigThingy senderSource = WollMuxFiles.getWollmuxConf().query("SENDER_SOURCE", 1);
      String senderSourceStr = null;
      try
      {
        senderSourceStr = senderSource.getLastChild().toString();
      } catch (NodeNotFoundException e)
      {
        // No longer issue an error message (only when the data source "null" is actually accessed)
      }

      String overrideFragDbSpalte;
      ConfigThingy overrideFragDbSpalteConf = WollMuxFiles.getWollmuxConf().query(OVERRIDE_FRAG_DB_SPALTE, 1);
      try
      {
        overrideFragDbSpalte = overrideFragDbSpalteConf.getLastChild().toString();
      } catch (NodeNotFoundException x)
      {
        // no OVERRIDE_FRAG_DB_SPALTE directive found
        overrideFragDbSpalte = "";
      }

      try
      {
        SenderCache cache = new FileCache(WollMuxFiles.getLosCacheFile(), WollMuxFiles.getDefaultContext());

        Map<String, Datasource> dataSources = Datasources.getDatasources();

        FunctionLibrary funcLib = new FunctionLibrary();
        DialogLibrary dialogLib = new DialogLibrary();
        Map<Object, Object> context = new HashMap<>();
        ColumnTransformer columnTransformer = new ColumnTransformer(FunctionFactory
            .parseTrafos(WollMuxFiles.getWollmuxConf(), "AbsenderdatenSpaltenumsetzung", funcLib, dialogLib, context));

        Datasource datasource = dataSources.get(senderSourceStr);
        if (datasource == null)
        {
          datasource = new DummyDatasourceWithMessagebox(cache.getSchema(), senderSourceStr);
        }
        instance = new SenderService(datasource, columnTransformer, cache, overrideFragDbSpalte);
      } catch (ConfigurationErrorException | SenderException e)
      {
        LOGGER.error("", e);
      }
    }
    return instance;
  }

  SenderService(Datasource mainDatasource, ColumnTransformer columnTransformer,
      SenderCache cache, String overrideFragDbSpalte) throws SenderException
  {
    registeredPALChangeListener = new ArrayList<>();
    this.columnTransformer = columnTransformer;
    this.cache = cache;
    if (mainDatasource == null)
    {
      throw new SenderException("Keine Datenquelle vorhanden.");
    }
    this.mainDatasource = mainDatasource;
    this.overrideFragDbSpalte = overrideFragDbSpalte;
    init(cache);
    selectFromCache(cache);
    addPALChangeEventListener(cache);
  }

  private void init(SenderCache cache) throws SenderException
  {
    List<String> cachedSchema = cache.getSchema();
    Set<String> newColumns = new HashSet<>(getSchema());
    newColumns.removeAll(cachedSchema);

    Set<String> removedColumns = new HashSet<>(cachedSchema);
    removedColumns.removeAll(getSchema());
    List<Sender> newSender = new ArrayList<>();
    for (SenderConf senderConf : cache.getData())
    {
      Sender sender;
      QueryResults res = mainDatasource.getDatasetsByKey(List.of(senderConf.getKey()));
      if (!res.isEmpty())
      {
        Dataset base = res.iterator().next();
        sender = new Sender(senderConf.getKey(), base, senderConf.getOverriddenValues());
      } else
      {
        sender = new Sender(senderConf.getKey(), null, senderConf.getOverriddenValues());
        for (String newColumn : newColumns)
        {
          sender.overrideValue(newColumn, "");
        }
        if (!senderConf.getCachedValues().isEmpty())
        {
          lostDatasets.add(sender);
        }
      }
      removedColumns.forEach(sender::drop);
      newSender.add(sender);
    }

    String lostKeys = lostDatasets.stream().map(Sender::getKey).collect(Collectors.joining(", "));
    if (!lostKeys.isEmpty())
      LOGGER.info("The records with the following keys could not be updated from the database: {}",
          lostKeys);
    updateSenderList(newSender);
  }

  private void selectFromCache(SenderCache cache) throws SenderException
  {
    String selectKey = cache.getSelectedKey();
    int selectIndex = cache.getSelectedSameKeyIndex();
    selectByKeyAndIndex(selectKey, selectIndex);
  }

  private void selectByKeyAndIndex(String selectKey, int selectIndex) throws SenderException
  {
    Sender selected = null;
    Map<String, Integer> keys = new HashMap<>();
    for (Sender sender : data)
    {
      int index = keys.compute(sender.getKey(), (k, v) -> v == null ? 0 : v + 1);
      if (selectKey.equals(sender.getKey()) && selectIndex == index)
      {
        selected = sender;
        break;
      }
    }
    if (selected == null && !data.isEmpty())
    {
      selected = data.get(0);
    }
    select(selected);
  }

  /**
   * Suche nach einem Absender durch Auswertung der Konfiguration oder
   * anhand der Informationen des LibreOffice Profils.
   * Wenn ein Absender gefunden wird, werden weitere Suchstrategien nicht ausgewertet.
   *
   * @return The number of senders found.
   */
  @SuppressWarnings("java:S2629")
  public long searchDefaultSender()
  {
    List<CompletableFuture<List<Sender>>> futures = new ArrayList<>();
    try
    {
      // search by strategy defined in configuration
      ConfigThingy wmConf = WollMuxFiles.getWollmuxConf();
      ConfigThingy strat = wmConf.query("PersoenlicheAbsenderlisteInitialisierung").query("Suchstrategie")
          .getLastChild();
      for (ConfigThingy element : strat)
      {
        if (element.getName().equals("BY_JAVA_PROPERTY"))
        {
          futures.add(new ByJavaPropertyFinder(this).find(element));
        } else if (element.getName().equals("BY_OOO_USER_PROFILE"))
        {
          futures.add(new ByOOoUserProfileFinder(this).find(element));
        } else
        {
          LOGGER.error("Invalid key in search strategy: {}", element.stringRepresentation());
        }
      }
    } catch (NodeNotFoundException e)
    {
      LOGGER.info("Couldn't find any search strategy");
      LOGGER.debug("", e);
      List<Pair<String, String>> query = new ArrayList<>();
      query.add(new ImmutablePair<>(Sender.VORNAME, "${givenname}"));
      query.add(new ImmutablePair<>(Sender.NACHNAME, "${sn}"));
      futures.add(new ByOOoUserProfileFinder(this).find(query));
    }
    List<Sender> newSender = futures.stream().map(CompletableFuture::join).flatMap(List::stream)
        .collect(Collectors.toList());
    try
    {
      updateSenderList(newSender);
    } catch (SenderException e)
    {
      LOGGER.error(e.getMessage(), e);
    }
    return newSender.size();
  }

  /**
   * This method returns a string representation of the sender currently
   * selected from the personal sender list (PAL).
   * The string representation always contains the string "§§%=%§§" at the end
   *  followed by the key of the currently selected sender.
   *  If the PAL is empty or no sender has been selected yet,
   *  the method returns the empty string "".
   *  This special case should of course be handled accordingly by the calling method.
   *
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getCurrentSender()
   *
   * @return the sender currently selected from the PAL as a string.
   *         If no sender is selected the empty string "" is returned.
   */
  @Override
  public String getCurrentSender()
  {
    if (selectedSender == null)
    {
      return "";
    }
    return selectedSender.getDisplayString() + SenderService.SENDER_KEY_SEPARATOR + selectedSender.getKey();
  }

  /**
   * Add a listener for changes in the sender list.
   * A listener can be registered only once.
   *
   * @param listener
   *          The listener to add.
   */
  public void addPALChangeEventListener(XPALChangeEventListener listener)
  {
    LOGGER.trace("SenderService::addPALChangeEventListener()");

    if (listener == null)
    {
      return;
    }

    for (XPALChangeEventListener l : registeredPALChangeListener)
    {
      if (UnoRuntime.areSame(UNO.XInterface(l), listener))
        return;
    }
    registeredPALChangeListener.add(listener);
  }

  /**
   * Remove a listener from being notified by changes of the sender list.
   *
   * @param listener
   *          The listener to remove.
   */
  public void removePALChangeEventListener(XPALChangeEventListener listener)
  {
    LOGGER.trace("PersoenlicheAbsenderliste::removePALChangeEventListener()");
    Iterator<XPALChangeEventListener> i = registeredPALChangeListener.iterator();
    while (i.hasNext())
    {
      XInterface l = UNO.XInterface(i.next());
      if (UnoRuntime.areSame(l, listener))
        i.remove();
    }
  }

  void notifyListener()
  {
    EventObject event = new EventObject();
    event.Source = this;
    for (XPALChangeEventListener listener : registeredPALChangeListener)
    {
      listener.updateContent(event);
    }
  }

  /**
   * This method returns an alphabetically ascending sorted list with string
   * representations of all Personal Sender List (PAL) entries in a string array.
   * Each PAL entry always contains the string "§§%=%§§" at the end
   * followed by the key of the corresponding entry!
   *
   * @see de.muenchen.allg.itd51.wollmux.XPALProvider#getPALEntries()
   * @return Sender names
   */
  @Override
  public String[] getPALEntries()
  {
    List<Sender> pal = getSenderListSorted(Sender.NACHNAME);
    String[] elements = new String[pal.size()];
    for (int i = 0; i < pal.size(); i++)
    {
      elements[i] = pal.get(i).getDisplayString() + SenderService.SENDER_KEY_SEPARATOR + pal.get(i).getKey();
    }

    return elements;
  }

  /**
   * Select a sender by its name and index.
   *
   * @param senderName
   *          The name of the sender.
   * @param idx
   *          The index of the sender in the list.
   */
  public void selectSender(String senderName, int idx)
  {
    List<Sender> palDatasets = getSenderListSorted(Sender.NACHNAME);

    if (idx >= 0 && idx < palDatasets.size())
    {
      String senderListName = palDatasets.get(idx).getDisplayString() + SenderService.SENDER_KEY_SEPARATOR
          + palDatasets.get(idx).getKey();
      if (senderListName.equals(senderName))
      {
        try
        {
          select(palDatasets.get(idx));
        } catch (SenderException e)
        {
          LOGGER.error("Setting sender '{}' failed because index '{}'"
              + " does not match the PAL (inconsistencies?)", senderName, idx, e);
        }
      }
    } else
    {
      LOGGER.error("Setting sender '{}' failed because index '{}'"
          + " does not match the PAL (inconsistencies?)", senderName, idx);
    }
  }

  /**
   * Get the values of the current sender.
   *
   * @return Mapping from schema to value for the current sender.
   *         If a value is null, the column is omitted.
   */
  public Map<String, String> getCurrentSenderValues()
  {
    Map<String, String> values = new HashMap<>();
    try
    {
      Sender ds = getSelectedDatasetTransformed();
      getSchema().forEach(key -> {
        String val = ds.get(key);
        if (val != null)
        {
          values.put(key, val);
        }
      });
    } catch (SenderException x)
    {
      LOGGER.trace("", x);
    }
    return values;
  }

  /**
   * Get the value for a column of the current sender.
   *
   * @param column
   *          The name of the colum.
   * @return The value (never null).
   * @throws SenderException
   *           No sender is selected.
   * @throws ColumnNotFoundException
   *           The schema has no such column.
   */
  public String getCurrentSenderValue(String column) throws SenderException, ColumnNotFoundException
  {
    if (!getSchema().contains(column))
    {
      throw new ColumnNotFoundException("Column does not exist");
    }
    String value = getSelectedDatasetTransformed().get(column);
    if (value == null)
      value = "";
    return value;
  }

  /**
   * Get the personal OverrideFrag list of the sender.
   *
   * @return The list of fragments which are overridden.
   */
  public ConfigThingy getCurrentOverrideFragMap() throws SenderException
  {
    ConfigThingy overrideFragConf = new ConfigThingy("overrideFrag");

    if (!overrideFragDbSpalte.isEmpty())
    {
      try
      {
        String value = getCurrentSenderValue(overrideFragDbSpalte);
        overrideFragConf = new ConfigThingy("overrideFrag", value);
      } catch (SenderException e)
      {
        throw new SenderException("No sender selected => OVERRIDE_FRAG_DB_SPALTE is ineffective", e);
      } catch (ColumnNotFoundException e)
      {
        LOGGER.debug("OVERRIDE_FRAG_DB_SPALTE {} does not exist", overrideFragDbSpalte, e);
      } catch (IOException | SyntaxErrorException e)
      {
        throw new SenderException("Error parsing OVERRIDE_FRAG_DB_SPALTE " + overrideFragDbSpalte, e);
      }
    }

    return overrideFragConf;
  }

  /**
   * Get list of senders which can't be found in the database anymore.
   *
   * @return List of senders in format {@code <oid> <name> <surname>}.
   */
  List<String> getLostDatasetDisplayStrings()
  {
    return lostDatasets.stream().map(Sender::getDisplayString).collect(Collectors.toList());
  }

  /**
   * Show the dialog for managing the sender list.
   */
  public void showManageSenderListDialog()
  {
    PersoenlicheAbsenderlisteVerwalten dialog = new PersoenlicheAbsenderlisteVerwalten(getAllSender());
    dialog.execute();
    try
    {
      updateSenderList(dialog.getSenderList());
    } catch (SenderException e)
    {
      LOGGER.error(e.getMessage(), e);
      InfoDialog.showInfoModal("WollMux", L.m("Persönliche Absenderliste konnte nicht aktualisiert werden."));
    }
  }

  private void updateSenderList(List<Sender> newSenderList) throws SenderException
  {
    if (selectedSender != null)
    {
      int index = -1;
      Map<String, Integer> keys = new HashMap<>();
      for (Sender sender : data)
      {
        int i = keys.compute(sender.getKey(), (k, v) -> v == null ? 0 : v++);
        if (sender.equals(selectedSender))
        {
          index = i;
          break;
        }
      }
      data = newSenderList;
      selectByKeyAndIndex(selectedSender.getKey(), index);
    } else
    {
      data = newSenderList;
      if (!newSenderList.isEmpty())
      {
        select(newSenderList.get(0));
      }
    }
    notifyListener();
  }

  /**
   * If no {@link ColumnTransformer} is set, this function returns the same
   * as {@link #getSender()}, otherwise the dataset transformed
   * by the ColumnTransformer is returned.
   *
   * @throws SenderException
   *           if the LOS is empty (otherwise one record is always selected).
   */
  Sender getSelectedDatasetTransformed() throws SenderException
  {
    if (selectedSender == null)
    {
      throw new SenderException("No sender is selected");
    }
    if (columnTransformer == null)
    {
      return selectedSender;
    }
    return new Sender(columnTransformer.transform(new Dataset()
    {

      @Override
      public String getKey()
      {
        return selectedSender.getKey();
      }

      @Override
      public String get(String columnName) throws ColumnNotFoundException
      {
        return selectedSender.get(columnName);
      }
    }));
  }

  /**
   * Select the given sender.
   *
   * @param sender
   *          The sender to select.
   * @throws SenderException
   *           The sender is not in the list of senders.
   */
  public void select(Sender sender) throws SenderException
  {
    if (sender != null && !data.contains(sender))
    {
      throw new SenderException("Unknown sender was selected.");
    }
    if (this.selectedSender != null)
    {
      this.selectedSender.setSelected(false);
    }
    this.selectedSender = sender;
    if (this.selectedSender != null)
    {
      this.selectedSender.setSelected(true);
    }
    notifyListener();
  }

  /**
   * Create a new sender, where every column of the schema has the column name as value.
   *
   * @return The sender.
   */
  public Sender createNewSender()
  {
    Map<String, String> dsoverride = new HashMap<>();
    for (String column : getSchema())
    {
      dsoverride.put(column, column);
    }
    return new Sender(dsoverride);
  }

  /**
   * Get the sender in alphabetical order.
   *
   * @param column
   *          The column to use for sorting.
   * @return A sorted list of senders.
   */
  public List<Sender> getSenderListSorted(String column)
  {
    List<Sender> listDataset = new ArrayList<>(data);
    Collections.sort(listDataset, Sender.comparatorByColumn(column));
    return listDataset;
  }

  /**
   * Get the schema of the senders.
   *
   * @return Schema of the senders.
   */
  public List<String> getSchema()
  {
    return mainDatasource.getSchema();
  }

  /**
   * Find matches in the main datasource by a List of {@link QueryPart}.
   *
   * @param searchQuery
   *          Query to search against the main datasource.
   * @return Search results as {@link QueryResults}
   */
  public CompletableFuture<List<Sender>> find(Map<String, String> searchQuery)
  {
    AsyncSearch searchAsync = new AsyncSearch(searchQuery, mainDatasource);
    return searchAsync.runSearchAsync()
        .thenApply(res -> StreamSupport.stream(res.spliterator(), false).map(Sender::new).collect(Collectors.toList()));
  }

  /**
   * Get all datasets from the local override storage.
   *
   * @return Datasets from local override storage by type {@link QueryResults}.
   */
  protected List<Sender> getAllSender()
  {
    if (data == null)
    {
      return Collections.emptyList();
    }
    List<Sender> copy = new ArrayList<>();
    data.forEach(s -> copy.add(new Sender(s)));
    return copy;
  }
}
