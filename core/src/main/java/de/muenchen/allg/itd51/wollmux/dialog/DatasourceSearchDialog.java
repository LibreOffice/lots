/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XKeyListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainer;
import com.sun.star.awt.tab.XTabPageContainerModel;
import com.sun.star.lang.EventObject;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractTabPageContainerListener;
import de.muenchen.allg.dialog.adapter.AbstractTopWindowListener;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.ColumnTransformer;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.Search;
import de.muenchen.allg.itd51.wollmux.db.SearchStrategy;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.ui.GuiFactory;
import de.muenchen.allg.itd51.wollmux.ui.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.ui.layout.ControlLayout;
import de.muenchen.allg.itd51.wollmux.ui.layout.HorizontalLayout;
import de.muenchen.allg.itd51.wollmux.ui.layout.Layout;
import de.muenchen.allg.itd51.wollmux.ui.layout.VerticalLayout;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.util.UnoComponent;
import de.muenchen.allg.util.UnoProperty;

/**
 * Dialog for searching data sources.
 */
public class DatasourceSearchDialog implements Dialog
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceSearchDialog.class);

  private static final String ABORT = "abort";

  /**
   * The description of the dialog.
   */
  private ConfigThingy myConf;

  /**
   * All IDs provided by Spaltenumsetzung
   */
  private Set<String> schema;

  /**
   * The currently selected data.
   */
  private Map<String, String> data;

  /**
   * Used to perform queries. Mapping from data source name to data source.
   */
  private Map<String, Datasource> datasources;

  /**
   * If true UI actions have no effect.
   */
  private boolean processUIElementEvents = false;

  /**
   * The function library.
   */
  private FunctionLibrary funcLib;

  /**
   * The dialog library for functions.
   */
  private DialogLibrary dialogLib;

  /**
   * Context in which functions are executed.
   */
  private Map<Object, Object> context = new HashMap<>();

  /**
   * The listener to call as soon as the dialog is closed.
   */
  private ActionListener dialogEndListener;

  /**
   * The container of the tab pages.
   */
  private XTabPageContainer tabPageContainer;

  /**
   * Mapping from tab ID to tab.
   */
  private Map<Short, Tab> tabs;

  /**
   * The UNO dialog.
   */
  private XDialog dialog;

  /**
   * A new data source search dialog.
   *
   * @param conf
   *          The configuration of the dialog.
   * @param datasources
   *          Used to perform the queries.
   */
  public DatasourceSearchDialog(ConfigThingy conf, Map<String, Datasource> datasources)
  {
    this.myConf = conf;
    this.datasources = datasources;
    schema = parseSchema(conf);
    if (schema.isEmpty())
    {
      throw new ConfigurationErrorException(
          L.m("Fehler in Funktionsdialog: Abschnitt 'Spaltenumsetzung' konnte nicht geparst werden!"));
    }
    this.data = new HashMap<>();
  }

  /**
   * In the context the data is stored. So if the context has no mapping for this type of dialog, a
   * new entry is added. This data is used for all following calls with the same context.
   */
  @Override
  public Dialog instanceFor(Map<Object, Object> context)
  {
    if (!context.containsKey(this))
    {
      context.put(this, new HashMap<>());
    }
    this.data = (Map<String, String>) context.get(this);
    return this;
  }

  @Override
  public Collection<String> getSchema()
  {
    return new HashSet<>(schema);
  }

  @Override
  public Object getData(String id)
  {
    if (!schema.contains(id))
    {
      return null;
    }
    String str = data.get(id);
    if (str == null)
    {
      return "";
    }
    return str;
  }

  @Override
  public void show(ActionListener dialogEndListener, FunctionLibrary funcLib, DialogLibrary dialogLib)
  {
    this.dialogEndListener = dialogEndListener;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;

    String title = L.m("Datensatz Auswählen");
    try
    {
      title = L.m(myConf.get("TITLE", 1).toString());
    } catch (Exception x)
    {
      LOGGER.trace("", x);
    }

    String type = L.m("<keiner>");
    try
    {
      type = myConf.get("TYPE", 1).toString();
    } catch (Exception x)
    {
      LOGGER.trace("", x);
    }
    if (!"dbSelect".equals(type))
      throw new ConfigurationErrorException(
          L.m("Ununterstützter TYPE \"%1\" in Funktionsdialog \"%2\"", type, myConf.getName()));

    final ConfigThingy fensterDesc = myConf.query("Fenster");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException(L.m("Schlüssel 'Fenster' fehlt in %1", myConf.getName()));

    try
    {
      createGUI(title, fensterDesc.getLastChild());
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Create the dialog.
   *
   * @param title
   *          The title of the dialog.
   * @param fensterDesc
   *          The configuration of the dialog.
   */
  private void createGUI(String title, ConfigThingy fensterDesc)
  {
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
    XContainerWindowProvider provider = UNO.XContainerWindowProvider(
        UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));

    if (provider == null)
      return;

    XWindow window = provider.createContainerWindow(
        "vnd.sun.star.script:WollMux.DatasourceSearchDialog?location=application", "", peer, null);
    UNO.XTopWindow(window).addTopWindowListener(new AbstractTopWindowListener()
    {
      @Override
      public void windowClosed(EventObject arg0)
      {
        dialogEnd(ABORT);
      }
    });
    XControlContainer controlContainer = UNO.XControlContainer(window);
    dialog = UNO.XDialog(window);
    dialog.setTitle(title);

    tabs = new HashMap<>();
    tabPageContainer = UNO.XTabPageContainer(GuiFactory.createTabPageContainer(UNO.xMCF, UNO.defaultContext));
    XTabPageContainerModel tabPageContainerModel = UNO
        .XTabPageContainerModel(UNO.XControl(tabPageContainer).getModel());
    AbstractTabPageContainerListener listener = event -> paint();
    tabPageContainer.addTabPageContainerListener(listener);
    controlContainer.addControl("tabPages", UNO.XControl(tabPageContainer));

    short tabId = 1;
    for (ConfigThingy neuesFenster : fensterDesc)
    {
      String tabTitle = L.m("Eingabe");
      String tip = "";
      for (ConfigThingy childConf : neuesFenster)
      {
        String name = childConf.getName();
        if ("TIP".equals(name))
        {
          tip = childConf.toString();
        } else if ("TITLE".equals(name))
        {
          tabTitle = L.m(childConf.toString());
        }
      }

      GuiFactory.createTab(UNO.xMCF, UNO.defaultContext, tabPageContainerModel, tabTitle, tabId, -1);
      XTabPage xTabPage = UNO.XTabPageContainer(tabPageContainer).getTabPageByID(tabId);
      UNO.XTabPageModel(UNO.XControl(xTabPage).getModel()).setToolTip(tip);
      XControlContainer tabPageControlContainer = UNO.XControlContainer(xTabPage);
      Tab newWindow = new Tab(neuesFenster, tabPageControlContainer);
      tabs.put(tabId, newWindow);

      ++tabId;
    }

    Rectangle r = window.getPosSize();
    int h = tabs.values().stream().map(Tab::getLayout).mapToInt(l -> l.getHeightForWidth(r.Width)).max()
        .orElse(r.Height);
    r.Height = h + UNO.XWindow(tabPageContainer.getTabPageByID(tabPageContainer.getActiveTabPageID())).getPosSize().Y;
    window.setPosSize(0, 0, r.Width, r.Height, PosSize.SIZE);
    UNO.XWindow(tabPageContainer).setPosSize(r.X, r.Y, r.Width, r.Height, PosSize.POSSIZE);
    tabPageContainer.setActiveTabPageID((short) 1);
    paint();
    processUIElementEvents = true;
    dialog.execute();
  }

  /**
   * Update the window after the tab was changed.
   */
  private void paint()
  {
    short activeTab = tabPageContainer.getActiveTabPageID();
    Rectangle tabRect = UNO.XWindow(tabPageContainer.getTabPageByID(activeTab)).getPosSize();
    tabRect.Y = 0;
    Pair<Integer, Integer> tabSize = tabs.get(activeTab).getLayout().layout(tabRect);

    UNO.XWindow(tabPageContainer.getTabPageByID(activeTab)).setPosSize(0, tabRect.Y, tabSize.getRight(),
        tabSize.getLeft() + tabRect.Y, PosSize.SIZE);
  }

  /**
   * Tab of the GUI containing a search for a defined data source.
   */
  private class Tab
  {

    /**
     * The layout of the tab.
     */
    private Layout layout;

    /**
     * The columns defined in the section Spaltenumsetzung.
     */
    private List<String> dialogWindowSchema;

    /**
     * The search strategy.
     */
    private SearchStrategy searchStrategy;

    /**
     * The {@link ColumnTransformer} for the columns.
     */
    private ColumnTransformer columnTransformer;

    /**
     * The template for showing the search results. Can contain variables like {@code ${name}}.
     */
    private String displayTemplate = L.m("<Datensatz>");

    /**
     * The control containing the results.
     */
    private XListBox resultsList = null;

    /**
     * The field containing the search parameters.
     */
    private XTextComponent query = null;

    /**
     * If true a search is started after the tab has been build.
     */
    private boolean autosearch = false;

    /**
     * Mapping from data source column to control in which the value should be displayed.
     */
    private Map<String, XControl> mapSpalteToUIElement = new HashMap<>();

    /**
     * Mapping from search results display string to the actual data set.
     */
    Map<String, Dataset> datasets = new TreeMap<>();

    /**
     * Create a new tab.
     *
     * @param conf
     *          The configuration of the tab.
     * @param controlContainer
     *          The control container of the dialog.
     */
    public Tab(ConfigThingy conf, XControlContainer controlContainer)
    {
      searchStrategy = SearchStrategy.parse(conf);
      try
      {
        columnTransformer = new ColumnTransformer(
            FunctionFactory.parseTrafos(conf, "Spaltenumsetzung", funcLib, dialogLib, context));
      } catch (ConfigurationErrorException x)
      {
        LOGGER.error(L.m("Fehler beim Parsen des Abschnitts 'Spaltenumsetzung'"), x);
      }
      dialogWindowSchema = columnTransformer.getSchema();
      layout = new VerticalLayout();
      Layout introLayout = new VerticalLayout(5, 0, 5, 5, 5);
      addUIElements(conf.query("Intro"), controlContainer, introLayout);
      layout.addLayout(introLayout, 1);

      Layout searchLayout = new HorizontalLayout(10, 0, 5, 5, 15);
      addUIElements(conf.query("Suche"), controlContainer, searchLayout);
      layout.addLayout(searchLayout, 1);

      Layout mainLayout = new HorizontalLayout(10, 0, 5, 5, 15);
      layout.addLayout(mainLayout, 1);
      Layout resultLayout = new VerticalLayout();
      addUIElements(conf.query("Suchergebnis"), controlContainer, resultLayout);
      mainLayout.addLayout(resultLayout, 1);
      Layout previewLayout = new VerticalLayout(0, 0, 0, 0, 5);
      addUIElements(conf.query("Vorschau"), controlContainer, previewLayout);
      mainLayout.addLayout(previewLayout, 1);

      Layout footerLayout = new HorizontalLayout(10, 0, 5, 5, 15);
      addUIElements(conf.query("Fussbereich"), controlContainer, footerLayout);
      layout.addLayout(footerLayout, 1);

      if (autosearch)
      {
        search();
      }
    }

    public Layout getLayout()
    {
      return layout;
    }

    /**
     * Add all controls in the configuration to the container and the layout.
     *
     * @param conf
     *          The configuration of a part of the tab.
     * @param container
     *          The control container for the controls.
     * @param layout
     *          The layout to which the controls should be added.
     */
    private void addUIElements(ConfigThingy conf, XControlContainer container, Layout layout)
    {
      for (ConfigThingy parent : conf)
      {
        for (ConfigThingy uiConf : parent)
        {
          try
          {
            UIElementConfig config = new UIElementConfig(uiConf);
            XControl uiElement = createControl(container, layout, config);
            treatSpecialId(config, uiElement);
          } catch (ConfigurationErrorException e)
          {
            LOGGER.error("", e);
            continue;
          }
        }
      }
    }

    /**
     * The controls with the ID suchanfrage and sucherergebnis are treated specially.
     *
     * @param config
     *          The configuration of the control.
     * @param uiElement
     *          The control.
     */
    private void treatSpecialId(UIElementConfig config, XControl uiElement)
    {
      String id = config.getId();
      if (id.equals("suchanfrage"))
      {
        autosearch = false;
        query = UNO.XTextComponent(uiElement);
        XKeyListener handler = new XKeyListener()
        {
          @Override
          public void keyPressed(KeyEvent event)
          {
            if (event.KeyCode == Key.RETURN)
            {
              search();
            }
          }

          @Override
          public void disposing(EventObject arg0)
          {
            // nothing to do
          }

          @Override
          public void keyReleased(KeyEvent arg0)
          {
            // nothing to do
          }
        };
        UNO.XWindow(uiElement).addKeyListener(handler);
        ConfigThingy autofill = config.getAutofill();
        if (autofill.count() > 0)
        {
          query.setText(autofill.toString());
          autosearch = true;
        }
      } else if ("suchergebnis".equals(id))
      {
        resultsList = UNO.XListBox(uiElement);
        if (resultsList == null)
        {
          LOGGER.error("UI Element mit ID \"suchergebnis\" muss vom TYPE \"listbox\" sein!");
        }
        displayTemplate = config.getDisplay();
        if (displayTemplate == null)
        {
          displayTemplate = L.m("<Datensatz>");
        }
      }
    }

    /**
     * Create a control.
     *
     * @param container
     *          The container for the control.
     * @param layout
     *          The layout to which the control belongs.
     * @param config
     *          The configuration of the control.
     * @return The control.
     */
    private XControl createControl(XControlContainer container, Layout layout, UIElementConfig config)
    {
      Layout uiLayout = null;
      XControl uiElement = null;
      switch (config.getType())
      {
      case LABEL:
        uiElement = GuiFactory.createLabel(UNO.xMCF, UNO.defaultContext, config.getLabel(),
            new Rectangle(0, 0, 300, 20), null);
        uiLayout = new ControlLayout(uiElement);
        break;
      case TEXTFIELD:
        uiLayout = new HorizontalLayout(0, 0, 0, 0, 5);
        if (config.getLabel() != null && !config.getLabel().isEmpty())
        {
          XControl label = GuiFactory.createLabel(UNO.xMCF, UNO.defaultContext, config.getLabel(),
              new Rectangle(0, 0, 300, 20), null);
          uiLayout.addControl(label);
          container.addControl(config.getId() + "label", label);
        }
        SortedMap<String, Object> props = new TreeMap<>();
        props.put(UnoProperty.DEFAULT_CONTROL, config.getId());
        props.put(UnoProperty.READ_ONLY, config.isReadonly());
        props.put(UnoProperty.HELP_TEXT, config.getTip());
        uiElement = GuiFactory.createTextfield(UNO.xMCF, UNO.defaultContext, "", new Rectangle(0, 0, 300, 20), props,
            null);
        uiLayout.addControl(uiElement);
        break;
      case BUTTON:
        AbstractActionListener listener = event -> processUiElementEvent(config.getAction());
        uiElement = GuiFactory.createButton(UNO.xMCF, UNO.defaultContext, config.getLabel(), listener,
            new Rectangle(0, 0, 300, 20), null);
        uiLayout = new ControlLayout(uiElement);
        break;
      case LISTBOX:
        AbstractItemListener itemlistener = event -> {
          List<String> selected = Arrays.asList(resultsList.getSelectedItems());
          if (!selected.isEmpty())
          {
            updatePreview(datasets.get(selected.get(0)));
          }
        };
        uiElement = GuiFactory.createListBox(UNO.xMCF, UNO.defaultContext, itemlistener, new Rectangle(0, 0, 300, 300),
            null);
        uiLayout = new ControlLayout(uiElement);
        break;
      default:
        break;
      }
      if (uiElement != null)
      {
        container.addControl(config.getId(), uiElement);
        layout.addLayout(uiLayout, 1);
        if (config.getDbSpalte() != null)
        {
          mapSpalteToUIElement.put(config.getDbSpalte(), uiElement);
        }
      }
      return uiElement;
    }

    /**
     * Write the data of a data set in the preview controls.
     *
     * @param ds
     *          The data set.
     */
    private void updatePreview(Dataset ds)
    {
      for (Map.Entry<String, XControl> entry : mapSpalteToUIElement.entrySet())
      {
        String dbSpalte = entry.getKey();
        XControl uiElement = entry.getValue();
        try
        {
          if (ds == null)
          {
            UNO.XTextComponent(uiElement).setText("");
          } else
          {
            UNO.XTextComponent(uiElement).setText(ds.get(dbSpalte));
          }
        } catch (ColumnNotFoundException e)
        {
          LOGGER.error("Fehler im Abschnitt \"Spaltenumsetzung\" oder \"Vorschau\". Spalte \"{}\" soll in "
              + "Vorschau angezeigt werden ist aber nicht in der Spaltenumsetzung definiert.", dbSpalte);
        }
      }
    }

    /**
     * Display the search results in the control {@link #resultsList}.
     *
     * @param data
     *          The results of the search
     */
    private void setListElements(QueryResults data)
    {
      datasets.clear();
      if (resultsList == null)
      {
        return;
      }
      if (data != null)
      {
        for (Dataset d : data)
        {
          datasets.put(substituteVars(d), d);
        }
      }

      resultsList.removeItems((short) 0, resultsList.getItemCount());
      resultsList.addItems(datasets.keySet().toArray(String[]::new), (short) 0);
      updatePreview(null);
    }

    /**
     * Select a data set.
     */
    private void select(Collection<String> schema, Dataset ds)
    {
      if (ds != null)
      {
        Map<String, String> newData = new HashMap<>();
        for (String columnName : schema)
        {
          try
          {
            newData.put(columnName, ds.get(columnName));
          } catch (Exception x)
          {
            LOGGER.error(L.m("Huh? Dies sollte nicht passieren können"), x);
          }
        }
        data = newData;
      }
      dialogEnd("select");
    }

    /**
     * Start the query and update the result list.
     */
    private void search()
    {
      if (query == null)
      {
        return;
      }

      CompletableFuture.supplyAsync(() -> {
        QueryResults r = null;
        try
        {
          r = Search.search(query.getText(), searchStrategy, datasources);
        } catch (IllegalArgumentException x)
        {
          LOGGER.error("", x);
          InfoDialog.showInfoModal(L.m("Timeout bei Suchanfrage"),
              L.m("Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen.\n"
                  + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten\n"
                  + "Suchbegriff, der auf zu viele Ergebnisse zutrifft.\n"
                  + "Bitte versuchen Sie eine andere, präzisere Suchanfrage."));
        }
        return r;
      }).thenAcceptAsync(r -> {
        if (r != null && resultsList != null)
        {
          setListElements(columnTransformer.transform(r));
        }
      });
    }

    /**
     * Process the actions of the buttons.
     *
     * @param action
     *          The action to perform.
     */
    private void processUiElementEvent(String action)
    {
      if (!processUIElementEvents)
      {
        return;
      }
      try
      {
        processUIElementEvents = false;

        if (ABORT.equals(action))
        {
          dialogEnd(ABORT);
        } else if ("back".equals(action))
        {
          dialogEnd("back");
        } else if ("search".equals(action))
        {
          search();
        } else if ("select".equals(action))
        {
          Dataset ds = null;
          if (resultsList != null)
          {
            List<String> selected = Arrays.asList(resultsList.getSelectedItems());
            if (!selected.isEmpty())
              ds = datasets.get(selected.get(0));
          }
          select(dialogWindowSchema, ds);
        }
      } catch (Exception x)
      {
        LOGGER.error("", x);
      } finally
      {
        processUIElementEvents = true;
      }
    }

    /**
     * Replace {@code ${SPALTENNAME}} in {@link #displayTemplate} with the values from the datas et.
     *
     * @param ds
     *          Associated data set.
     * @return String without '$' characters.
     */
    private String substituteVars(Dataset ds)
    {
      Pattern p = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z_0-9]*)\\}");
      String display = displayTemplate;
      Matcher m = p.matcher(display);
      while (m.find())
      {
        String spalte = m.group(1);
        String wert = spalte;
        try
        {
          String wert2 = ds.get(spalte);
          if (wert2 != null)
          {
            wert = wert2.replaceAll("\\$", "");
          }
        } catch (ColumnNotFoundException e)
        {
          LOGGER.error(
              L.m("Fehler beim Auflösen des Platzhalters \"${%1}\": Spalte für den Datensatz nicht definiert", spalte));
        }
        display = display.substring(0, m.start()) + wert + display.substring(m.end());
        m = p.matcher(display);
      }
      return display;
    }
  }

  /**
   * Terminate the dialog and call the listener.
   *
   * @param actionComand
   *          The command for the listener.
   */
  private void dialogEnd(String actionCommand)
  {
    if (dialog != null)
    {
      dialog.endExecute();
    }
    if (dialogEndListener != null)
    {
      dialogEndListener.actionPerformed(new ActionEvent(this, 0, actionCommand));
    }
  }

  /**
   * Parse the column mapping of all tabs.
   *
   * @param conf
   *          The configuration.
   * @return The column schema.
   */
  private HashSet<String> parseSchema(ConfigThingy conf)
  {
    HashSet<String> newSchema = new HashSet<>();
    for (ConfigThingy fenster : conf.query("Fenster"))
    {
      for (ConfigThingy tab : fenster)
      {
        for (ConfigThingy columnMapping : tab.query("Spaltenumsetzung", 1))
        {
          for (ConfigThingy column : columnMapping)
          {
            newSchema.add(column.getName());
          }
        }
      }
    }

    return newSchema;
  }
}
