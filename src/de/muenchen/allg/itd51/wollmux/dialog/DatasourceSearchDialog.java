/*
 * Dateiname: DatasourceSearchDialog.java
 * Projekt  : WollMux
 * Funktion : Dialog zur Suche nach Daten in einer Datenquelle, die über DIALOG-Funktion verfügbar gemacht werden.
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
 * 22.05.2006 | BNK | Erstellung
 * 23.05.2006 | BNK | Rohbau
 * 24.05.2006 | BNK | angefangen mit Suchstrategie auswerten etc.
 * 26.05.2006 | BNK | Suchstrategie,... fertig implementiert
 * 29.05.2006 | BNK | Umstellung auf UIElementFactory.Context
 * 30.05.2006 | BNK | Suche implementiert
 * 29.06.2006 | BNK | setResizable(true)
 * 10.07.2006 | BNK | suchanfrageX statt wortX als Platzhalter.
 * 03.08.2006 | BNK | +getSchema()
 * 11.03.2010 | BED | Einsatz von FrameWorker für Suche + Meldung bei Timeout
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnTransformer;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.Search;
import de.muenchen.allg.itd51.wollmux.core.db.SearchStrategy;
import de.muenchen.allg.itd51.wollmux.core.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementContext;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementEventHandler;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementFactory;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Listbox;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.UIElement;
import de.muenchen.allg.itd51.wollmux.core.form.model.Control;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormType;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Dialog zur Suche nach Daten in einer Datenquelle, die über DIALOG-Funktion
 * verfügbar gemacht werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasourceSearchDialog implements Dialog
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatasourceSearchDialog.class);

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private static final int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private static final int BUTTON_BORDER = 2;

  /**
   * Das ConfigThingy, das die Beschreibung des Dialogs enthält.
   */
  private ConfigThingy myConf;

  /**
   * Der Instantiator, der diesen Dialog instanziiert hat und auch für die Erstellung
   * weiterer Instanzen herangezogen wird.
   */
  private Instantiator ilse;

  /**
   * Alle ids, die durch Spaltenumsetzungsabschnitte definiert werden.
   */
  private Set<String> schema;

  /**
   * data[0] speichert die aktuell ausgewählten Formulardaten. ACHTUNG! Nur in
   * synchronized(data)-Blöcken verwenden!
   */
  private Map<String, String>[] data;

  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;

  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;

  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(UIElementContext, ConfigThingy)},
   * der verwendet wird für das Erzeugen von vertikal angeordneten UI Elementen (mit
   * Ausnahme der Vorschau).
   */
  private UIElementContext vertiContext;

  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(UIElementContext, ConfigThingy)},
   * der verwendet wird für das Erzeugen der vertikal angeordneten UI Elemente der
   * Vorschau.
   */
  private UIElementContext previewContext;

  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(UIElementContext, ConfigThingy)},
   * der verwendet wird für das Erzeugen von horizontal angeordneten Elementen.
   */
  private UIElementContext horiContext;

  /**
   * Solange dieses Flag false ist, werden Events von UI Elementen ignoriert.
   */
  private boolean processUIElementEvents = false;

  /**
   * Wird zur Auflösung von Funktionsreferenzen in Spaltenumsetzung-Abschnitten
   * verwendet.
   */
  private FunctionLibrary funcLib;

  /**
   * Zur Zeit noch nicht unterstützt, aber im Prinzip könnte man in einem
   * Datenquellensuchdialog ebenfalls wieder Funktionsdialoge verwenden. Diese würden
   * dann aus dieser Bibliothek bezogen.
   */
  private DialogLibrary dialogLib;

  /**
   * Werden durch diesen Funktionsdialog weitere Funktionsdialoge erzeugt, so wird
   * dieser Kontext übergeben.
   */
  private Map<Object, Object> context = new HashMap<>();

  /**
   * Der show übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Wird von show() getestet und auf true gesetzt um mehrfache gleichzeitige show()s
   * zu verhindern.
   */
  private boolean[] shown = new boolean[] { false };

  /**
   * Erzeugt einen neuen DSD.
   *
   * @param ilse
   *          der Instantiator, der für instanceFor()-Aufrufe verwendet werden soll.
   * @param conf
   *          die Beschreibung des Dialogs.
   */
  @SuppressWarnings("unchecked")
  private DatasourceSearchDialog(Instantiator ilse, Set<String> schema,
      ConfigThingy conf, DatasourceJoiner dj)
  {
    this.myConf = conf;
    this.ilse = ilse;
    this.dj = dj;
    this.schema = schema;
    // Unfortunately, creating generic arrays is not possible, i.e.
    // new Map<String,String>[] doesn't work.
    this.data = new Map[] { new HashMap<String, String>() };
  }

  /**
   * Erzeugt einen neuen Dialog, dessen Instanzen Datenquellensuchdialoge gemäß
   * der Beschreibung in conf darstellen. Die Suchergebnisse liefert dj.
   *
   * @throws ConfigurationErrorException
   *           falls ein Fehler in der Dialogbeschreibung vorliegt.
   */
  public static Dialog create(ConfigThingy conf, DatasourceJoiner dj)
  {
    return new Instantiator(conf, dj);
  }

  @Override
  public Dialog instanceFor(Map<Object, Object> context)
  {
    return ilse.instanceFor(context);
  }

  @Override
  public Collection<String> getSchema()
  {
    return new HashSet<>(schema);
  }

  @Override
  public Object getData(String id)
  {
    String str;
    synchronized (data)
    {
      if (!schema.contains(id)) {
        return null;
      }
      str = data[0].get(id);
    }
    if (str == null) {
      return "";
    }
    return str;
  }

  @Override
  public void show(ActionListener dialogEndListener, FunctionLibrary funcLib,
      DialogLibrary dialogLib)
  {
    synchronized (shown)
    {
      if (shown[0]) {
        return;
      }
      shown[0] = true;
    }
    this.dialogEndListener = dialogEndListener;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;

    String title = L.m("Datensatz Auswählen");
    try
    {
      title = L.m(myConf.get("TITLE", 1).toString());
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
    }

    String type = L.m("<keiner>");
    try
    {
      type = myConf.get("TYPE", 1).toString();
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
    }
    if (!"dbSelect".equals(type))
      throw new ConfigurationErrorException(L.m(
        "Ununterstützter TYPE \"%1\" in Funktionsdialog \"%2\"", type,
        myConf.getName()));

    final ConfigThingy fensterDesc = myConf.query("Fenster");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException(L.m("Schlüssel 'Fenster' fehlt in %1",
        myConf.getName()));

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      final String title2 = title;
      javax.swing.SwingUtilities.invokeLater(() ->
      {
        try
        {
          createGUI(title2, fensterDesc.getLastChild());
        } catch (Exception x)
        {
          LOGGER.error("", x);
        }
      });
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Erzeugt das GUI. Muss im EDT aufgerufen werden.
   *
   * @param title
   *          der Titel des Fensters.
   * @param fensterDesc
   *          der "Fenster" Abschnitt, der die Tabs der GUI beschreibt.
   */
  private void createGUI(String title, ConfigThingy fensterDesc)
  {
    Common.setLookAndFeelOnce();

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    MyWindowListener oehrchen = new MyWindowListener();
    // der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen);

    // WollMux-Icon für den Frame
    Common.setWollMuxIcon(myFrame);

    JPanel contentPanel = new JPanel();
    myFrame.getContentPane().add(contentPanel);
    JTabbedPane myTabbedPane = new JTabbedPane();
    contentPanel.add(myTabbedPane);

    /********************************************************************************
     * Tabs erzeugen.
     *******************************************************************************/
    Iterator<ConfigThingy> iter = fensterDesc.iterator();
    int tabIndex = 0;
    while (iter.hasNext())
    {
      ConfigThingy neuesFenster = iter.next();

      /*
       * Die folgende Schleife ist nicht nur eleganter als mehrere try-catch-Blöcke
       * um get()-Befehle, sie verhindert auch, dass TIP oder HOTKEY aus Versehen von
       * einem enthaltenen Button aufgeschnappt werden.
       */
      String tabTitle = L.m("Eingabe");
      char hotkey = 0;
      String tip = "";
      Iterator<ConfigThingy> childIter = neuesFenster.iterator();
      while (childIter.hasNext())
      {
        ConfigThingy childConf = childIter.next();
        String name = childConf.getName();
        if ("TIP".equals(name))
          tip = childConf.toString();
        else if ("TITLE".equals(name))
          tabTitle = L.m(childConf.toString());
        else if ("HOTKEY".equals(name))
        {
          String str = childConf.toString();
          if (str.length() > 0) {
            hotkey = str.toUpperCase().charAt(0);
          }
        }
      }

      DialogWindow newWindow = new DialogWindow(tabIndex, neuesFenster);

      myTabbedPane.addTab(tabTitle, null, newWindow.jPanel(), tip);
      if (hotkey != 0) {
        myTabbedPane.setMnemonicAt(tabIndex, hotkey);
      }

      ++tabIndex;
    }

    /*
     * Event-Verarbeitung starten.
     */
    processUIElementEvents = true;

    myFrame.pack();
    myFrame.setAlwaysOnTop(true);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(true);
    myFrame.setAlwaysOnTop(true);
    myFrame.setVisible(true);
  }

  /**
   * Ein Tab der GUI.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class DialogWindow implements UIElementEventHandler
  {
    /**
     * Das Panel das die GUI-Elemente enthält.
     */
    private JPanel myPanel;

    /**
     * Die durch den Spaltenumsetzung-Abschnitt definierten Spaltennamen.
     */
    private Set<String> dialogWindowSchema;

    /**
     * Die Suchstrategie für Suchanfragen.
     */
    private SearchStrategy searchStrategy;

    /**
     * Der für die Spaltenumsetzung verantwortliche {@link ColumnTransformer}.
     */
    private ColumnTransformer columnTransformer;

    /**
     * Legt fest, wie die Datensätze in der Ergebnisliste dargestellt werden sollen.
     * Kann Variablen der Form "${name}" enthalten.
     */
    private String displayTemplate = L.m("<Datensatz>");

    /**
     * Die Listbox mit den Suchresultaten.
     */
    private Listbox resultsList = null;

    /**
     * Das Textfeld in dem der Benutzer seine Suchanfrage eintippt.
     */
    private UIElement query = null;

    /**
     * Suche automatisch ausführen, nach Aufbau des Tabs.
     */
    private boolean autosearch = false;

    /**
     * Die für die Erzeugung der UI Elemente verwendete Factory.
     */
    private UIElementFactory uiElementFactory;

    /**
     * Bildet DB_SPALTE Werte des Vorschau-Abschnitts auf die entsprechenden
     * UIElemente ab (jeweils 1 UIElement pro DB_SPALTE, keine Liste).
     */
    private Map<String, UIElement> mapSpalteToUIElement;

    /**
     * Erzeugt ein neues Tab.
     *
     * @param tabIndex
     *          Die Nummer (von 0 gezählt) des Tabs, das dieses DialogWindow
     *          darstellt.
     * @param conf
     *          der Kind-Knoten des Fenster-Knotens der das Tab beschreibt. conf ist
     *          direkter Elternknoten der Knoten "Intro" et al.
     */
    public DialogWindow(int tabIndex, ConfigThingy conf)
    {
      searchStrategy = SearchStrategy.parse(conf);
      try
      {
        columnTransformer =
          new ColumnTransformer(FunctionFactory.parseTrafos(conf, "Spaltenumsetzung", funcLib, dialogLib,
            context));
      }
      catch (ConfigurationErrorException x)
      {
        LOGGER.error(L.m("Fehler beim Parsen des Abschnitts 'Spaltenumsetzung'"), x);
      }
      dialogWindowSchema = columnTransformer.getSchema();
      initFactories();

      myPanel = new JPanel(new BorderLayout());

      JPanel introSuche = new JPanel();
      introSuche.setLayout(new BoxLayout(introSuche, BoxLayout.PAGE_AXIS));
      JPanel suchergebnisUndVorschau = new JPanel();
      suchergebnisUndVorschau.setLayout(new BoxLayout(suchergebnisUndVorschau,
        BoxLayout.LINE_AXIS));
      JPanel fussbereich = new JPanel(new GridBagLayout());
      JPanel intro = new JPanel(new GridBagLayout());
      JPanel suche = new JPanel(new GridBagLayout());
      JPanel suchergebnis = new JPanel(new GridBagLayout());
      JPanel vorschau = new JPanel(new GridBagLayout());

      myPanel.add(introSuche, BorderLayout.PAGE_START);
      myPanel.add(suchergebnisUndVorschau, BorderLayout.CENTER);
      myPanel.add(fussbereich, BorderLayout.PAGE_END);

      introSuche.add(intro);
      introSuche.add(suche);

      suchergebnisUndVorschau.add(suchergebnis);
      suchergebnisUndVorschau.add(vorschau);

      addUIElements(conf, "Intro", intro, 0, 1, vertiContext, null);
      addUIElements(conf, "Suche", suche, 1, 0, horiContext, null);
      addUIElements(conf, "Suchergebnis", suchergebnis, 0, 1, vertiContext, null);
      mapSpalteToUIElement = new HashMap<>();
      addUIElements(conf, "Vorschau", vorschau, 0, 1, previewContext,
        mapSpalteToUIElement);
      addUIElements(conf, "Fussbereich", fussbereich, 1, 0, horiContext, null);

      if (autosearch) {
        search();
      }
    }

    /**
     * Liefert das JPanel, das die Elemente dieses Tabs enthält.
     */
    public JPanel jPanel()
    {
      return myPanel;
    }

    /**
     * Fügt compo UI Elemente gemäss den Kindern von conf.query(key) hinzu. compo
     * muss ein GridBagLayout haben. stepx und stepy geben an um wieviel mit jedem UI
     * Element die x und die y Koordinate der Zelle erhöht werden soll. Wirklich
     * sinnvoll sind hier nur (0,1) und (1,0).
     *
     * @param context
     *          ist der Kontext, der
     *          {@link UIElementFactory#createUIElement(UIElementContext, ConfigThingy)}
     *          übergeben werden soll für die Erzeugung der UIElemente.
     * @param in
     *          dieser Map werden all erzeugten UIElemente registriert, die ein
     *          DB_SPALTE Attribut haben. null ist nicht erlaubt. TESTED
     */
    private void addUIElements(ConfigThingy conf, String key, JComponent compo,
        int stepx, int stepy, UIElementContext context,
        Map<String, UIElement> mapSpalteToUIElement)
    {
      int y = 0;
      int x = 0;

      for (ConfigThingy parent : conf.query(key))
      {
        for (ConfigThingy uiConf : parent)
        {
          UIElement uiElement;
          try
          {
            uiElement = uiElementFactory.createUIElement(context, Control.create(uiConf, null));
            String dbSpalte = uiConf.getString("DB_SPALTE", null);
            if (dbSpalte != null)
            {
              mapSpalteToUIElement.put(dbSpalte, uiElement);
            }
          }
          catch (ConfigurationErrorException | FormModelException e)
          {
            LOGGER.error("", e);
            continue;
          }

          /*
           * Besondere IDs auswerten.
           */
          String id = uiElement.getId();
          if (id.equals("suchanfrage"))
          {
            autosearch = false;
            query = uiElement;
            String autofill = uiConf.getString("AUTOFILL", null);
            if (autofill != null)
            {
              query.setString(autofill);
              autosearch = true;
            }
          }

          if ("suchergebnis".equals(id))
          {
            try
            {
              resultsList = (Listbox) uiElement;
              displayTemplate = uiConf.getString("DISPLAY", L.m("<Datensatz>"));
            }
            catch (ClassCastException e)
            {
              LOGGER.error(L.m("UI Element mit ID \"suchergebnis\" muss vom TYPE \"listbox\" sein!"));
            }
          }

          /**************************************************************************
           * UI Element und evtl. vorhandenes Zusatzlabel zum GUI hinzufügen.
           *************************************************************************/
          int compoX = 0;
          int labelmod = 1;
          if (!uiElement.getLabelType().equals(UIElement.LabelPosition.NONE))
          {
            labelmod = 2;
            int labelX = 0;
            if (uiElement.getLabelType().equals(UIElement.LabelPosition.LEFT))
            {
              compoX = 1;
            }
            else
            {
              labelX = 1;
            }

            Component label = uiElement.getLabel();
            if (label != null)
            {
              GridBagConstraints gbc =
                (GridBagConstraints) uiElement.getLabelLayoutConstraints();
              gbc.gridx = x + labelX;
              gbc.gridy = y;
              compo.add(label, gbc);
            }
          }
          GridBagConstraints gbc =
            (GridBagConstraints) uiElement.getLayoutConstraints();
          gbc.gridx = x + compoX;
          gbc.gridy = y;
          x += stepx * labelmod;
          y += stepy;
          compo.add(uiElement.getComponent(), gbc);
        }
      }
    }

    /**
     * Geht alle Elemente von {@link #mapSpalteToUIElement} durch und updated die
     * Felder mit den entsprechenden Werten aus dem Datensatz, der an ele dranhängt.
     */
    private void updatePreview(ListElement ele)
    {
      Dataset ds = null;
      if (ele != null) {
        ds = ele.getDataset();
      }
      Iterator<Map.Entry<String, UIElement>> iter =
        mapSpalteToUIElement.entrySet().iterator();
      while (iter.hasNext())
      {
        Map.Entry<String, UIElement> entry = iter.next();
        String dbSpalte = entry.getKey();
        UIElement uiElement = entry.getValue();
        try
        {
          if (ds == null)
            uiElement.setString("");
          else
            uiElement.setString(ds.get(dbSpalte));
        }
        catch (ColumnNotFoundException e)
        {
          LOGGER.error(L.m(
            "Fehler im Abschnitt \"Spaltenumsetzung\" oder \"Vorschau\". Spalte \"%1\" soll in Vorschau angezeigt werden ist aber nicht in der Spaltenumsetzung definiert.",
            dbSpalte));
        }
      }
    }

    /**
     * Ändert die Werteliste von list so, dass sie data entspricht. Die Datasets aus
     * data werden nicht direkt als Werte verwendet, sondern in {@link ListElement}
     * Objekte gewrappt. data == null wird interpretiert als leere Liste. list kann
     * null sein (dann tut diese Funktion nichts).
     */
    private void setListElements(Listbox list, QueryResults data)
    {
      if (list == null) {
        return;
      }
      ListElement[] elements;
      if (data == null)
        elements = new ListElement[] {};
      else
      {
        elements = new ListElement[data.size()];
        Iterator<Dataset> iter = data.iterator();
        int i = 0;
        while (iter.hasNext())
          elements[i++] = new ListElement(iter.next());
        Arrays.sort(elements, (Object o1, Object o2) -> o1.toString().compareTo(o2.toString()));
      }

      list.setList(Arrays.asList(elements));
      updatePreview(null);
    }

    /**
     * Implementiert die gleichnamige ACTION.
     */
    private void back()
    {
      dialogEnd("back");
    }

    /**
     * Implementiert die gleichnamige ACTION.
     */
    private void select(Collection<String> schema, Dataset ds)
    {
      if (ds != null)
      {
        Map<String, String> newData = new HashMap<>();
        Iterator<String> iter = schema.iterator();
        while (iter.hasNext())
        {
          String columnName = iter.next();
          try
          {
            newData.put(columnName, ds.get(columnName));
          }
          catch (Exception x)
          {
            LOGGER.error(L.m("Huh? Dies sollte nicht passieren können"), x);
          }
        }

        synchronized (data)
        {
          data[0] = newData;
        }
      }
      dialogEnd("select");
    }

    /**
     * Wrapper um ein Dataset zum Einfügen in eine JList.
     */
    private class ListElement
    {
      private String displayString;

      private Dataset ds;

      public ListElement(Dataset ds)
      {
        displayString = getDisplayString(ds);
        this.ds = ds;
      }

      @Override
      public String toString()
      {
        return displayString;
      }

      public Dataset getDataset()
      {
        return ds;
      }

      /**
       * Liefert zu einem Datensatz den in einer Listbox anzuzeigenden String.
       */
      private String getDisplayString(Dataset ds)
      {
        return substituteVars(displayTemplate, ds);
      }
    }

    /**
     * Führt die Suchanfrage im Feld {@link #query} aus (falls dieses nicht null ist)
     * gemäß {@link #searchStrategy} und ändert {@link #resultsList} (falls nicht
     * null) so dass sie die Suchergebnisse enthält.
     */
    public void search()
    {
      if (query == null) {
        return;
      }

      // Erzeugen eines Runnable-Objekts, das die Geschäftslogik enthält und nachher
      // an FrameWorker.disableFrameAndWork übergeben werden kann.
      Runnable r = () -> {
        QueryResults results = null;
        try
        {
          results = Search.search(query.getString(), searchStrategy, dj, false);
        } catch (TimeoutException x)
        {
          InfoDialog.showInfoModal(L.m("Timeout bei Suchanfrage"), L.m(
              "Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen.\n"
                  + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten\n"
                  + "Suchbegriff, der auf zu viele Ergebnisse zutrifft.\n"
                  + "Bitte versuchen Sie eine andere, präzisere Suchanfrage."));
          LOGGER.error("", x);
        } catch (IllegalArgumentException x)
        {
          LOGGER.error("", x);
        } // wird bei illegalen Suchanfragen geworfen

        if (results != null && resultsList != null)
        {
          // Wir benötigen finalResults, da eine nicht-finale Variable nicht in der
          // unten definierten anonymen Runnable-Klasse referenziert werden darf.
          final QueryResults finalResults = results;

          // Folgendes muss im Event Dispatch Thread ausgeführt werden
          SwingUtilities.invokeLater(
              () -> setListElements(resultsList, columnTransformer.transform(finalResults)));
        }
      }; // Ende des Erzeugens des Runnable-Objekts r

      // Frame disablen und Suche in eigenem Thread starten
      FrameWorker.disableFrameAndWork(myFrame, r, true);
    }

    /**
     * Die zentrale Anlaufstelle für alle von UIElementen ausgelösten Events (siehe
     * {@link UIElementEventHandler#processUiElementEvent(UIElement, String, Object[])}
     * ).
     */
    @Override
    public void processUiElementEvent(UIElement source, String eventType,
        Object[] args)
    {
      if (!processUIElementEvents) {
        return;
      }
      try
      {
        processUIElementEvents = false; // Reentranz bei setString() unterbinden

        if (WollMuxFiles.isDebugMode())
        {
          StringBuilder buffy =
            new StringBuilder("UIElementEvent: " + eventType + "(");
          for (int i = 0; i < args.length; ++i)
            buffy.append((i == 0 ? "" : ",") + args[i]);
          buffy.append(") on UIElement " + source.getId());
          LOGGER.debug(buffy.toString());
        }

        if ("action".equals(eventType))
        {
          String action = (String) args[0];
          if ("abort".equals(action))
            abort();
          else if ("back".equals(action))
            back();
          else if ("search".equals(action))
            search();
          else if ("select".equals(action))
          {
            Dataset ds = null;
            if (resultsList != null)
            {
              List<Object> selected = resultsList.getSelected();
              if (!selected.isEmpty())
                ds = ((ListElement) selected.get(0)).getDataset();
            }
            select(dialogWindowSchema, ds);
          }
        }
        else if ("listSelectionChanged".equals(eventType))
        {
          List<Object> selected = ((Listbox) source).getSelected();
          if (!selected.isEmpty()) {
            updatePreview((ListElement) selected.get(0));
          }
        }

      }
      catch (Exception x)
      {
        LOGGER.error("", x);
      }
      finally
      {
        processUIElementEvents = true;
      }
    }

    /**
     * Initialisiert die UIElementFactory, die zur Erzeugung der UIElements verwendet
     * wird.
     */
    private void initFactories()
    {
      Map<FormType, GridBagConstraints> mapTypeToLayoutConstraints = new EnumMap<>(FormType.class);
      Map<FormType, UIElement.LabelPosition> mapTypeToLabelType = new EnumMap<>(FormType.class);
      Map<FormType, GridBagConstraints> mapTypeToLabelLayoutConstraints = new EnumMap<>(
          FormType.class);

      // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
      // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
      GridBagConstraints gbcTextfield =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcListbox =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
          GridBagConstraints.BOTH, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcCombobox =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcTextarea =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcLabelLeft =
        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcCheckbox =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcLabel =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcPreviewLabel =
        new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      GridBagConstraints gbcButton =
        new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
            BUTTON_BORDER, BUTTON_BORDER), 0, 0);
      GridBagConstraints gbcHsep =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(3 * TF_BORDER, 0, 2 * TF_BORDER,
            0), 0, 0);
      GridBagConstraints gbcPreviewHsep =
        new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(3 * TF_BORDER, 0, 2 * TF_BORDER,
            0), 0, 0);
      GridBagConstraints gbcVsep =
        new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
          GridBagConstraints.VERTICAL, new Insets(0, TF_BORDER, 0, TF_BORDER), 0, 0);
      GridBagConstraints gbcGlue =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      GridBagConstraints gbcPreviewGlue =
        new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

      mapTypeToLayoutConstraints.put(FormType.DEFAULT, gbcTextfield);
      mapTypeToLabelType.put(FormType.DEFAULT, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.DEFAULT, null);

      mapTypeToLayoutConstraints.put(FormType.TEXTFIELD, gbcTextfield);
      mapTypeToLabelType.put(FormType.TEXTFIELD, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.TEXTFIELD, null);

      mapTypeToLayoutConstraints.put(FormType.COMBOBOX, gbcCombobox);
      mapTypeToLabelType.put(FormType.COMBOBOX, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.COMBOBOX, null);

      mapTypeToLayoutConstraints.put(FormType.H_GLUE, gbcGlue);
      mapTypeToLabelType.put(FormType.H_GLUE, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.H_GLUE, null);
      mapTypeToLayoutConstraints.put(FormType.V_GLUE, gbcGlue);
      mapTypeToLabelType.put(FormType.V_GLUE, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.V_GLUE, null);

      mapTypeToLayoutConstraints.put(FormType.TEXTAREA, gbcTextarea);
      mapTypeToLabelType.put(FormType.TEXTAREA, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.TEXTAREA, null);

      mapTypeToLayoutConstraints.put(FormType.LISTBOX, gbcListbox);
      mapTypeToLabelType.put(FormType.LISTBOX, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.LISTBOX, null);

      mapTypeToLayoutConstraints.put(FormType.LABEL, gbcLabel);
      mapTypeToLabelType.put(FormType.LABEL, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.LABEL, null);

      mapTypeToLayoutConstraints.put(FormType.CHECKBOX, gbcCheckbox);
      mapTypeToLabelType.put(FormType.CHECKBOX, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.CHECKBOX, null); // hat label integriert

      mapTypeToLayoutConstraints.put(FormType.BUTTON, gbcButton);
      mapTypeToLabelType.put(FormType.BUTTON, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.BUTTON, null);

      mapTypeToLayoutConstraints.put(FormType.H_SEPARATOR, gbcHsep);
      mapTypeToLabelType.put(FormType.H_SEPARATOR, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.H_SEPARATOR, null);
      mapTypeToLayoutConstraints.put(FormType.V_SEPARATOR, gbcVsep);
      mapTypeToLabelType.put(FormType.V_SEPARATOR, UIElement.LabelPosition.NONE);
      mapTypeToLabelLayoutConstraints.put(FormType.V_SEPARATOR, null);

      Set<String> supportedActions = new HashSet<>();
      supportedActions.add("abort");
      supportedActions.add("back");
      supportedActions.add("search");
      supportedActions.add("select");

      vertiContext = new UIElementContext();
      vertiContext.setMapTypeToLabelLayoutConstraints(mapTypeToLabelLayoutConstraints);
      vertiContext.setMapTypeToLabelType(mapTypeToLabelType);
      vertiContext.setMapTypeToLayoutConstraints(mapTypeToLayoutConstraints);
      vertiContext.setUiElementEventHandler(this);
      Map<FormType, FormType> vertiMapTypeToType = new EnumMap<>(FormType.class);
      vertiMapTypeToType.put(FormType.SEPARATOR, FormType.H_SEPARATOR);
      vertiMapTypeToType.put(FormType.GLUE, FormType.V_GLUE);
      vertiContext.setMapTypeToType(vertiMapTypeToType);
      vertiContext.setSupportedActions(supportedActions);

      horiContext = new UIElementContext();
      horiContext.setMapTypeToLabelLayoutConstraints(mapTypeToLabelLayoutConstraints);
      horiContext.setMapTypeToLabelType(mapTypeToLabelType);
      horiContext.setMapTypeToLayoutConstraints(mapTypeToLayoutConstraints);
      horiContext.setUiElementEventHandler(this);
      Map<FormType, FormType> horiMapTypeToType = new EnumMap<>(FormType.class);
      horiMapTypeToType.put(FormType.SEPARATOR, FormType.V_SEPARATOR);
      horiMapTypeToType.put(FormType.GLUE, FormType.H_GLUE);
      horiContext.setMapTypeToType(horiMapTypeToType);
      horiContext.setSupportedActions(supportedActions);

      Map<FormType, GridBagConstraints> previewLabelLayoutConstraints = new EnumMap<>(
          mapTypeToLabelLayoutConstraints);
      previewLabelLayoutConstraints.put(FormType.TEXTFIELD, gbcLabelLeft);
      Map<FormType, UIElement.LabelPosition> previewLabelType =
          new EnumMap<>(mapTypeToLabelType);
      previewLabelType.put(FormType.TEXTFIELD, UIElement.LabelPosition.LEFT);
      Map<FormType, GridBagConstraints> previewLayoutConstraints = new EnumMap<>(
          mapTypeToLayoutConstraints);
      previewLayoutConstraints.put(FormType.H_GLUE, gbcPreviewGlue);
      previewLayoutConstraints.put(FormType.V_GLUE, gbcPreviewGlue);
      previewLayoutConstraints.put(FormType.LABEL, gbcPreviewLabel);
      previewLayoutConstraints.put(FormType.H_SEPARATOR, gbcPreviewHsep);
      previewContext = new UIElementContext();
      previewContext.setMapTypeToLabelLayoutConstraints(previewLabelLayoutConstraints);
      previewContext.setMapTypeToLabelType(previewLabelType);
      previewContext.setMapTypeToLayoutConstraints(previewLayoutConstraints);
      previewContext.setUiElementEventHandler(this);
      Map<FormType, FormType> previewMapTypeToType = new EnumMap<>(FormType.class);
      previewMapTypeToType.put(FormType.SEPARATOR, FormType.H_SEPARATOR);
      previewMapTypeToType.put(FormType.GLUE, FormType.V_GLUE);
      previewContext.setMapTypeToType(previewMapTypeToType);
      previewContext.setSupportedActions(supportedActions);

      uiElementFactory = new UIElementFactory();

    }

  }

  /**********************************************************************************
   * end of class DialogWindow
   *********************************************************************************/

  /**
   * Ersetzt "${SPALTENNAME}" in str durch den Wert der entsprechenden Spalte im
   * datensatz.
   */
  public String substituteVars(String str, Dataset datensatz)
  {
    Pattern p = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z_0-9]*)\\}");
    Matcher m = p.matcher(str);
    if (m.find())
      do
      {
        String spalte = m.group(1);
        String wert = spalte;
        try
        {
          String wert2 = datensatz.get(spalte);
          if (wert2 != null) {
            wert = wert2.replaceAll("\\$", "");
          }
        }
        catch (ColumnNotFoundException e)
        {
          LOGGER.error(L.m(
            "Fehler beim Auflösen des Platzhalters \"${%1}\": Spalte für den Datensatz nicht definiert",
            spalte));
        }
        str = str.substring(0, m.start()) + wert + str.substring(m.end());
        m = p.matcher(str);
      } while (m.find());
    return str;
  }

  /**
   * Implementiert die gleichnamige ACTION.
   */
  private void abort()
  {
    dialogEnd("abort");
  }

  /**
   * Beendet den Dialog und ruft falls nötig den dialogEndListener auf wobei das
   * gegebene actionCommand übergeben wird.
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    synchronized (shown)
    {
      shown[0] = false;
    }
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(this, 0, actionCommand));
  }

  /**
   * Liefert neue Instanzen eines DatasourceSearchDialogs. Alle Dialoge, die über den
   * selben Instantiator erzeugt wurden erzeugen ihrerseits wieder neue Instanzen
   * über diesen Instantiator, d.h. insbesondere dass
   * instanceFor(context).instanceFor(context).instanceFor(context) den selben Dialog
   * liefert wie instanceFor(context).
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class Instantiator implements Dialog
  {
    private ConfigThingy conf;

    private DatasourceJoiner dj;

    private Set<String> schema;

    public Instantiator(ConfigThingy conf, DatasourceJoiner dj)
    {
      this.conf = conf;
      this.dj = dj;
      schema = parseSchema(conf);
      if (schema.isEmpty())
        throw new ConfigurationErrorException(
          L.m("Fehler in Funktionsdialog: Abschnitt 'Spaltenumsetzung' konnte nicht geparst werden!"));
    }

    @Override
    public Dialog instanceFor(Map<Object, Object> context)
    {
      if (!context.containsKey(this))
        context.put(this, new DatasourceSearchDialog(this, schema, conf, dj));
      return (Dialog) context.get(this);
    }

    @Override
    public Object getData(String id)
    {
      return null;
    }

    @Override
    public void show(ActionListener dialogEndListener, FunctionLibrary funcLib,
        DialogLibrary dialogLib)
    {
      // nothing to show
    }

    @Override
    public Collection<String> getSchema()
    {
      return new HashSet<>(schema);
    }

    private HashSet<String> parseSchema(ConfigThingy conf)
    {
      HashSet<String> newSchema = new HashSet<>();
      Iterator<ConfigThingy> fensterIter = conf.query("Fenster").iterator();
      while (fensterIter.hasNext())
      {
        ConfigThingy fenster = fensterIter.next();
        Iterator<ConfigThingy> tabIter = fenster.iterator();
        while (tabIter.hasNext())
        {
          ConfigThingy tab = tabIter.next();
          Iterator<ConfigThingy> suIter =
            tab.query("Spaltenumsetzung", 1).iterator();
          while (suIter.hasNext())
          {
            Iterator<ConfigThingy> spaltenIterator = suIter.next().iterator();
            while (spaltenIterator.hasNext())
            {
              ConfigThingy spalte = spaltenIterator.next();
              newSchema.add(spalte.getName());
            }
          }
        }
      }

      return newSchema;
    }

  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als Reaktion auf
   * den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener extends WindowAdapter
  {
    @Override
    public void windowClosing(WindowEvent e)
    {
      abort();
    }
  }
}
