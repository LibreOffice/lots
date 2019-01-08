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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.Search;
import de.muenchen.allg.itd51.wollmux.core.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung
 * einen Dialog zum Hinzufügen/Entfernen von Einträgen der Persönlichen Absenderliste
 * auf. Die private-Funktionen dagegen NUR aus dem Event-Dispatching Thread heraus
 * aufgerufen werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PersoenlicheAbsenderlisteVerwalten
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PersoenlicheAbsenderlisteVerwalten.class);

  public static final String DEFAULT_ROLLE = "D-WOLL-MUX-5.1";

  public static final String DEFAULT_NACHNAME = "Wollmux";

  public static final String DEFAULT_VORNAME = "Tinchen";

  public static final String DEFAULT_ANREDE = "Frau";

  /**
   * Gibt an, wie die Personen in den Listen angezeigt werden sollen, wenn es nicht
   * explizit in der Konfiguration über das DISPLAY-Attribut für eine listbox
   * festgelegt ist. %{Spalte}-Syntax um entsprechenden Wert des Datensatzes
   * einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   *
   * An dieser Stelle einen Default-Wert hardzucodieren (der noch dazu LHM-spezifisch
   * ist!) ist sehr unschön und wurde nur gemacht um abwärtskompatibel zu alten
   * WollMux-Konfigurationen zu bleiben. Sobald sichergestellt ist, dass überall auf
   * eine neue WollMux-Konfiguration geupdatet wurde, sollte man diesen Fallback
   * wieder entfernen.
   */
  private static final String DEFAULT_DISPLAYTEMPLATE =
    "%{Nachname}, %{Vorname} (%{Rolle})";

  private final URL mbURL =
    this.getClass().getClassLoader().getResource("data/mb.png");

  private final URL clURL =
    this.getClass().getClassLoader().getResource("data/cl.png");

  private final URL dbURL =
    this.getClass().getClassLoader().getResource("data/db.png");

  /**
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListenerAbort = e -> abort();

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListenerAbort;

  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;

  /**
   * Das JPanel der obersten Hierarchiestufe.
   */
  private JComponent mainPanel;

  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;

  /**
   * Speichert Referenzen auf die JButtons, die zu deaktivieren sind, wenn kein
   * Eintrag in einer Liste selektiert ist.
   */
  private List<JButton> buttonsToGreyOutIfNothingSelected = new ArrayList<>();

  /**
   * Die Listbox mit den Suchresultaten.
   */
  private JList<DJDatasetListElement> resultsJList;

  /**
   * Die Listbox mit der persönlichen Absenderliste.
   */
  private JList<DJDatasetListElement> palJList;

  /**
   * Gibt an, wie die Suchresultate in der {@link #resultsJList} angezeigt werden
   * sollen. Der Wert wird in der Konfiguration dieses Dialogs bei der "listbox" mit
   * ID "suchanfrage" durch Angeben des DISPLAY-Attributs konfiguriert.
   * %{Spalte}-Syntax um entsprechenden Wert des Datensatzes einzufügen, z.B.
   * "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   */
  private String resultsDisplayTemplate;

  /**
   * Gibt an, wie die Suchresultate in der {@link #palJList} angezeigt werden sollen.
   * Der Wert wird in der Konfiguration bei der "listbox" mit ID "suchanfrage" durch
   * Angeben des DISPLAY-Attributs konfiguriert. %{Spalte}-Syntax um entsprechenden
   * Wert des Datensatzes einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige
   * "Meier, Hans" etc.
   */
  private String palDisplayTemplate;

  /**
   * Die Textfelder in dem der Benutzer seine Suchanfrage eintippt.
   */
  private List<JTextField> query;
  private Map<String, String> queryNames = ImmutableMap.of("Nachname",
      "Nachname", "Vorname", "Vorname", "Email", "Mail",
      "Orga", "OrgaKurz");

  /**
   * Der dem
   * {@link #PersoenlicheAbsenderlisteVerwalten(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener)
   * Konstruktor} übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Das ConfigThingy, das diesen Dialog spezifiziert.
   */
  private ConfigThingy myConf;

  /**
   * Das ConfigThingy, das den Dialog Datensatz Bearbeiten für das Bearbeiten eines
   * Datensatzes der PAL spezifiziert.
   */
  private ConfigThingy abConf;

  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden wenn
   * kein Eintrag selektiert ist.
   */
  private MyListSelectionListener myListSelectionListener =
    new MyListSelectionListener();

  /**
   * Erzeugt einen neuen Dialog.
   *
   * @param conf
   *          das ConfigThingy, das den Dialog beschreibt (der Vater des
   *          "Fenster"-Knotens.
   * @param abConf
   *          das ConfigThingy, das den Absenderdaten Bearbeiten Dialog beschreibt.
   * @param dj
   *          der DatasourceJoiner, der die zu bearbeitende Liste verwaltet.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der Dialog
   *          geschlossen wurde. Das actionCommand des ActionEvents gibt die Aktion
   *          an, die das Speichern des Dialogs veranlasst hat.
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem
   *           Dialog unmöglich macht, zu funktionieren (z.B. dass der "Fenster"
   *           Schlüssel fehlt.
   */
  public PersoenlicheAbsenderlisteVerwalten(ConfigThingy conf, ConfigThingy abConf,
      DatasourceJoiner dj, ActionListener dialogEndListener)
  {
    this.dj = dj;
    this.myConf = conf;
    this.abConf = abConf;
    this.dialogEndListener = dialogEndListener;
    this.resultsDisplayTemplate = DEFAULT_DISPLAYTEMPLATE;
    this.palDisplayTemplate = DEFAULT_DISPLAYTEMPLATE;


    ConfigThingy suchfelder;
    try
    {
      suchfelder = conf.query("Suchfelder").getFirstChild();

      queryNames = StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(suchfelder.iterator(), 0),
              false)
          .sorted((a, b) -> {
            int s1 = NumberUtils.toInt(a.getString("SORT"));
            int s2 = NumberUtils.toInt(b.getString("SORT"));
            return s1 - s2;
          })
          .collect(Collectors.toMap(it -> it.getString("LABEL"),
              it -> it.getString("DB_SPALTE"),
              (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
    catch (NodeNotFoundException e)
    {
      LOGGER.error(L.m("Es wurden keine Suchfelder definiert."));
    }
    
    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            createGUI();
          }
          catch (Exception x)
          {
            LOGGER.error("", x);
          }
        }
      });
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Getter Methode für Konstante DEFAULT_DISPLAYTEMPLATE
   */
  public static String getDefaultDisplaytemplate()
  {
        return DEFAULT_DISPLAYTEMPLATE;
  }

  /**
   * Erzeugt das GUI.
   *
   * @param fensterDesc
   *          die Spezifikation dieses Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void createGUI()
  {
    Common.setLookAndFeelOnce();

    String title = L.m("Absenderliste Verwalten (WollMux)");

    closeAction = actionListenerAbort;

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosing(WindowEvent e)
      {
        closeAction.actionPerformed(null);
      }
    });
    myFrame.setAlwaysOnTop(true);
    // WollMux-Icon für PAL-Frame
    Common.setWollMuxIcon(myFrame);

    mainPanel = Box.createVerticalBox();
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    mainPanel.setPreferredSize(new Dimension(831, 357));
    myFrame.getContentPane().add(mainPanel);

    addUIElements();

    Dataset dsToSelect = null;
    try
    {
      dsToSelect = dj.getSelectedDataset();
    }
    catch (DatasetNotFoundException x)
    {}
    setListElements(palJList, dj.getLOS(), palDisplayTemplate, false, dsToSelect);

    updateButtonStates();

    myFrame.pack();

    /**
     * Beschränkung der Höhe erst nach pack() aufheben, damit Fenster nicht unnötig
     * groß wird.
     */
    palJList.setFixedCellHeight(-1);
    resultsJList.setFixedCellHeight(-1);

    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(false);
    myFrame.setVisible(true);
    myFrame.requestFocus();
  }

  private void addUIElements()
  {
    Box panelIntro = new Box(BoxLayout.X_AXIS);

    JLabel label = new JLabel(L.m(
        "Sie können nach Vorname, Nachname, Email und Orga-Einheit suchen"));
    panelIntro.add(label);

    Box glue = Box.createHorizontalBox();
    glue.add(Box.createHorizontalGlue());
    panelIntro.add(glue);

    mainPanel.add(panelIntro);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    Box panelSuche = Box.createHorizontalBox();
    panelSuche.setMaximumSize(new Dimension(900, 28));

    JComponent suchFelder = new JPanel(new GridLayout(2, 2, 5, 5));
    suchFelder.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    query = new ArrayList<>();
        
    KeyAdapter keyAdapter = new KeyAdapter()
    {
      @Override
      public void keyPressed(KeyEvent e)
      {
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
        {
          search();
          return;
        }

        super.keyPressed(e);
      }
    };

    queryNames.keySet().forEach(it -> {
      Box box = Box.createHorizontalBox();
      JLabel label1 = new JLabel(it);
      box.add(label1);
      box.add(Box.createHorizontalStrut(5));

      JTextField tf = new JTextField(22);
      tf.addKeyListener(keyAdapter);
      box.add(tf);

      label1.setLabelFor(tf);

      query.add(tf);
      suchFelder.add(box);
    });

    panelSuche.add(suchFelder);

    panelSuche.add(Box.createRigidArea(new Dimension(5, 0)));

    JButton button = new JButton(L.m("Suchen"));
    button.setMnemonic('S');
    button.addActionListener(e -> search());
    panelSuche.add(button);

    mainPanel.add(panelSuche);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    Box panelSelection = Box.createHorizontalBox();
    mainPanel.add(panelSelection);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    Box panelSuchergebnis = new Box(BoxLayout.Y_AXIS);

    Box label1Box = Box.createHorizontalBox();
    JLabel label1 = new JLabel(L.m("Suchergebnis"));
    label1Box.add(label1);
    label1Box.add(Box.createHorizontalGlue());
    panelSuchergebnis.add(label1Box);

    JList<DJDatasetListElement> list = new JList<DJDatasetListElement>();
    list.setModel(new DefaultListModel<DJDatasetListElement>());
    list.setVisibleRowCount(10);
    list.addMouseListener(
        new MyActionMouseListener(list, e -> addToPAL()));
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.setLayoutOrientation(JList.VERTICAL);
    list.setFixedCellWidth((int) new JLabel(
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX").getPreferredSize()
            .getWidth());
    list.addListSelectionListener(myListSelectionListener);

    resultsJList = list;
    resultsJList.setCellRenderer(new MyListCellRenderer());

    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setHorizontalScrollBarPolicy(
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    panelSuchergebnis.add(scrollPane);

    panelSelection.add(panelSuchergebnis);
    panelSelection.add(Box.createRigidArea(new Dimension(5, 0)));

    Box panelHinUndHer = new Box(BoxLayout.Y_AXIS);

    JButton button1 = new JButton(L.m("→"));
    button1.addActionListener(e -> addToPAL());
    buttonsToGreyOutIfNothingSelected.add(button1);

    panelHinUndHer.add(button1);
    panelHinUndHer.add(Box.createRigidArea(new Dimension(0, 5)));

    panelSelection.add(panelHinUndHer);
    panelSelection.add(Box.createRigidArea(new Dimension(5, 0)));

    Box panelAbsenderliste = new Box(BoxLayout.Y_AXIS);

    JLabel label2 = new JLabel(L.m("Persönliche Absenderliste"));

    Box label2Box = Box.createHorizontalBox();
    label2Box.add(label2);
    label2Box.add(Box.createHorizontalGlue());
    panelAbsenderliste.add(label2Box);


    JList<DJDatasetListElement> list1 = new JList<DJDatasetListElement>();
    list1.setModel(new DefaultListModel<DJDatasetListElement>());
    list1.setVisibleRowCount(10);
    list1.addMouseListener(
        new MyActionMouseListener(list, e -> editEntry()));
    list1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list1.setLayoutOrientation(JList.VERTICAL);
    list1.setFixedCellWidth((int) new JLabel(
        "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX").getPreferredSize()
            .getWidth());
    list1.addListSelectionListener(myListSelectionListener);
    palJList = list1;

    // KeyListener hinzufügen, damit Einträge in der PAL-Liste durch Drücken der
    // ENTF-Taste gelöscht werden können
    palJList.addKeyListener(new KeyAdapter()
    {
      @Override
      public void keyPressed(KeyEvent e)
      {
        super.keyPressed(e);
        if (e.getKeyCode() == KeyEvent.VK_DELETE)
        {
          removeFromPAL();
        }
      }
    });

    palJList.setCellRenderer(new MyListCellRenderer());

    JScrollPane scrollPane1 = new JScrollPane(list1);
    scrollPane.setHorizontalScrollBarPolicy(
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    panelAbsenderliste.add(scrollPane1);

    panelSelection.add(panelAbsenderliste);

    Box panelFussbereich = Box.createHorizontalBox();

    JButton button3 = new JButton(L.m("Löschen"));
    button3.setMnemonic('L');
    button3.addActionListener(e -> removeFromPAL());
    buttonsToGreyOutIfNothingSelected.add(button3);
    panelFussbereich.add(button3);

    JButton button4 = new JButton(L.m("Bearbeiten..."));
    button4.setMnemonic('B');
    button4.addActionListener(e -> editEntry());
    buttonsToGreyOutIfNothingSelected.add(button4);
    panelFussbereich.add(button4);

    JButton button5 = new JButton(L.m("Kopieren"));
    button5.setMnemonic('K');
    button5.addActionListener(e -> copyEntry());
    buttonsToGreyOutIfNothingSelected.add(button5);
    panelFussbereich.add(button5);

    JButton button6 = new JButton(L.m("Neu"));
    button6.setMnemonic('N');
    button6.addActionListener(e -> editNewPALEntry());
    panelFussbereich.add(button6);

    JButton button7 = new JButton(L.m("Schließen"));
    button7.setMnemonic('C');
    button7.addActionListener(e -> abort());
    panelFussbereich.add(button7);

    mainPanel.add(panelFussbereich);
  }

  /**
   * Wartet auf Doppelklick und führt dann die actionPerformed() Methode eines
   * ActionListeners aus.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class MyActionMouseListener extends MouseAdapter
  {
    private JList<DJDatasetListElement> list;

    private ActionListener action;

    public MyActionMouseListener(JList<DJDatasetListElement> list, ActionListener action)
    {
      this.list = list;
      this.action = action;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
      if (e.getClickCount() == 2)
      {
        Point location = e.getPoint();
        int index = list.locationToIndex(location);
        if (index < 0)
          return;
        Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(location))
          return;
        action.actionPerformed(null);
      }
    }
  }

  /**
   * Nimmt eine JList list, die ein DefaultListModel haben muss und ändert ihre
   * Wertliste so, dass sie data entspricht. Die Datasets aus data werden nicht
   * direkt als Werte verwendet, sondern in {@link DJDatasetListElement}-Objekte
   * gewrappt, deren Inhalt entsprechend des übergebenen displayTemplates angezeigt
   * wird. data == null wird interpretiert als leere Liste. Wenn datasetToSelect !=
   * null ist, so wird der entsprechende Datensatz in der Liste selektiert, wenn er
   * darin vorhanden ist.
   *
   * @param list
   *          die Liste deren Wertliste geändert werden soll
   * @param data
   *          enthält die Datensätze, mit denen die Liste gefüllt werden soll
   * @param displayTemplate
   *          gibt an wie die Datensätze in der Liste als Strings repräsentiert
   *          werden sollen, siehe z.B. {@link #DEFAULT_DISPLAYTEMPLATE}.
   * @param append
   *          Falls true, werden die Elemente an die Liste angehängt anstatt sie zu
   *          ersetzen.
   * @param datasetToSelect
   *          gibt den Datensatz an, der selektiert werden soll. Wenn
   *          <code>null</code> übergeben wird, wird entsprechend kein Datensatz
   *          ausgewählt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList<DJDatasetListElement> list, QueryResults data,
      String displayTemplate, boolean append, Dataset datasetToSelect)
  {
    int selectedIndex = -1;
    DJDatasetListElement[] elements;
    if (data == null)
      elements = new DJDatasetListElement[] {};
    else
    {
      elements = new DJDatasetListElement[data.size()];
      Iterator<Dataset> iter = data.iterator();
      int i = 0;
      while (iter.hasNext())
      {
        DJDataset ds = (DJDataset) iter.next();
        Icon icon = null;
        String mail = null;
        try
        {
          mail = ds.get("Mail"); // liefert null, wenn nicht belegt.
        }
        catch (ColumnNotFoundException x)
        {}

        if (mail == null)
          mail = "";

        if (WollMuxFiles.showCredits())
        {
          if ("matthias.benkmann@muenchen.de".equals(mail))
            icon = new ImageIcon(mbURL);
          else if ("christoph.lutz@muenchen.de".equals(mail))
            icon = new ImageIcon(clURL);
          else if ("daniel.benkmann@muenchen.de".equals(mail))
            icon = new ImageIcon(dbURL);
        }

        elements[i++] = new DJDatasetListElement(ds, displayTemplate, icon);
      }
      Arrays.sort(elements);
    }

    DefaultListModel<DJDatasetListElement> listModel = (DefaultListModel<DJDatasetListElement>) list.getModel();
    if (!append)
      listModel.clear();
    int oldSize = listModel.size();
    for (int i = 0; i < elements.length; ++i)
    {
      listModel.addElement(elements[i]);
      if (datasetToSelect != null
        && elements[i].getDataset().getKey().equals(datasetToSelect.getKey()))
        selectedIndex = i;
    }

    if (selectedIndex >= 0)
      list.setSelectedIndex(selectedIndex + oldSize);
  }

  /**
   * Wie {@link #setListElements(JList, QueryResults, String, boolean, Dataset)},
   * aber es wird kein Datensatz selektiert.
   *
   * @param list
   *          die Liste deren Wertliste geändert werden soll
   * @param data
   *          enthält die Datensätze, mit denen die Liste gefüllt werden soll
   * @param displayTemplate
   *          gibt an wie die Datensätze in der Liste als Strings repräsentiert
   *          werden sollen, siehe z.B. {@link #DEFAULT_DISPLAYTEMPLATE}.
   * @param append
   *          Falls true, werden die Elemente an die Liste angehängt anstatt sie zu
   *          ersetzen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList<DJDatasetListElement> list, QueryResults data,
      String displayTemplate, boolean append)
  {
    setListElements(list, data, displayTemplate, append, null);
  }

  /**
   * Aktiviert oder Deaktiviert die {@link #buttonsToGreyOutIfNothingSelected} gemäss
   * der Selektion oder nicht Selektion von Werten in den Listboxen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void updateButtonStates()
  {
    boolean enabled = false;
    try
    {
      enabled =
        (resultsJList.getSelectedIndex() >= 0) || (palJList.getSelectedIndex() >= 0);
    }
    catch (NullPointerException x)
    {
      LOGGER.error(L.m("Listbox mit ID \"suchergebnisse\" oder \"pal\" fehlt"));
    }

    Iterator<JButton> iter = buttonsToGreyOutIfNothingSelected.iterator();
    while (iter.hasNext())
      iter.next().setEnabled(enabled);
  }

  private static class MyListCellRenderer extends DefaultListCellRenderer
  {
    private static final long serialVersionUID = -540148680826568290L;

    @Override
    public Component getListCellRendererComponent(
        @SuppressWarnings("rawtypes") JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
      try
      {
        DJDatasetListElement ele = (DJDatasetListElement) value;

        Icon icon = ele.getIcon();
        if (icon != null)
          value = icon;
        else
          value = ele.toString();
      }
      catch (ClassCastException x)
      {}

      return super.getListCellRendererComponent(list, value, index, isSelected,
        cellHasFocus);
    }
  }

  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden wenn
   * kein Eintrag selektiert ist.
   */
  private class MyListSelectionListener implements ListSelectionListener
  {
    @Override
    public void valueChanged(ListSelectionEvent e)
    {
      @SuppressWarnings("unchecked")
      JList<DJDatasetListElement> list = (JList<DJDatasetListElement>) e.getSource();
      if (list != palJList && list != resultsJList)
        return;

      /*
       * Dafür sorgen, dass nie in beiden Listen ein Element selektiert ist.
       */
      JList<DJDatasetListElement> otherlist = (list == palJList) ? resultsJList : palJList;
      if (list.getSelectedIndex() >= 0)
        otherlist.clearSelection();

      /*
       * Buttons ausgrauen, falls nichts selektiert, einschwarzen sonst.
       */
      updateButtonStates();
    }
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    dialogEnd("abort");
  }

  /**
   * Beendet den Dialog und ruft falls nötig den dialogEndListener auf wobei das
   * gegebene actionCommand übergeben wird.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(this, 0, actionCommand));
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addToPAL()
  {
    List<DJDatasetListElement> sel = resultsJList.getSelectedValuesList();
    addEntries: for (DJDatasetListElement e : sel)
    {
      DJDataset ds = e.getDataset();
      String eStr = e.toString();
      ListModel<DJDatasetListElement> model = palJList.getModel();
      for (int j = model.getSize() - 1; j >= 0; --j)
      {
        DJDatasetListElement e2 = model.getElementAt(j);
        if (e2.toString().equals(eStr))
          continue addEntries;
      }
      ds.copy();
    }

    listsHaveChanged();
  }

  /**
   * Aktualisiert die Werte in der PAL Listbox, löscht die Selektionen in beiden
   * Listboxen und passt den Ausgegraut-Status der Buttons entsprechend an.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void listsHaveChanged()
  {
    setListElements(palJList, dj.getLOS(), palDisplayTemplate, false);
    palJList.clearSelection();
    resultsJList.clearSelection();
    updateButtonStates();
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeFromPAL()
  {
    List<DJDatasetListElement> sel = palJList.getSelectedValuesList();
    for (DJDatasetListElement e : sel)
    {
      e.getDataset().remove();
    }

    listsHaveChanged();
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editEntry()
  {
    DJDatasetListElement e = palJList.getSelectedValue();
    DJDataset ds;
    if (e == null)
    {
      e = resultsJList.getSelectedValue();
      if (e == null)
        return;
      ds = e.getDataset().copy();
    }
    else
      ds = e.getDataset();

    editDataset(ds, true);
  }

  private void editDataset(DJDataset ds, boolean edit)
  {
    ActionListener del =
      new MyDialogEndListener(this, myConf, abConf, dj, dialogEndListener, null);
    dialogEndListener = null;
    abort();
    try
    {
      new DatensatzBearbeiten(abConf, ds, del, edit);
    }
    catch (ConfigurationErrorException x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Erzeugt eine Kopue von orig und ändert ihre Rolle auf "Kopie".
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void copyDJDataset(DJDataset orig)
  {
    DJDataset newDS = orig.copy();
    try
    {
      newDS.set("Rolle", L.m("Kopie"));
    }
    catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void copyEntry()
  {
    List<DJDatasetListElement> sel = resultsJList.getSelectedValuesList();
    for (DJDatasetListElement e : sel)
    {
      copyDJDataset(e.getDataset());
    }

    sel = palJList.getSelectedValuesList();
    for (DJDatasetListElement e : sel)
    {
      copyDJDataset(e.getDataset());
    }

    listsHaveChanged();
  }

  /**
   * Implementiert die gleichnamige ACTION. Liefert den neuen Datensatz zurück.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private DJDataset newPALEntry()
  {
    DJDataset ds = dj.newDataset();
    try
    {
      ds.set("Vorname", DEFAULT_VORNAME);
      ds.set("Nachname", DEFAULT_NACHNAME);
      ds.set("Rolle", DEFAULT_ROLLE);
      ds.set("Anrede", DEFAULT_ANREDE);
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
    listsHaveChanged();
    return ds;
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editNewPALEntry()
  {
    editDataset(newPALEntry(), false);
  }

  /**
   * Implementiert die gleichnamige ACTION. Hier stecken die ganzen komplexen
   * Heuristiken drinnen zur Auswertung der Suchanfrage.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void search()
  {
    // Erzeugen eines Runnable-Objekts, das die Geschäftslogik enthält und nachher an
    // FrameWorker.disableFrameAndWork übergeben werden kann.
    Runnable r = () -> {
      Map<String, String> q = query.stream().collect(Collectors
          .toMap(it -> queryNames.get(it.getName()), it -> it.getText()));

      QueryResults results = null;
      try
      {
        results = Search.search(q, dj);
      }
      catch (TimeoutException x1)
      {
        JOptionPane.showMessageDialog(
            myFrame,
            L.m("Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen.\n"
                + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten\n"
                + "Suchbegriff, der auf zu viele Ergebnisse zutrifft.\n"
                + "Bitte versuchen Sie eine andere, präzisere Suchanfrage."),
            L.m("Timeout bei Suchanfrage"), JOptionPane.WARNING_MESSAGE);
        LOGGER.error("", x1);
      }
      catch (IllegalArgumentException x2)
      { // wird bei illegalen Suchanfragen geworfen
        LOGGER.error("", x2);
      }

      // Wir benötigen finalResults, da eine nicht-finale Variable nicht in der
      // unten definierten anonymen Runnable-Klasse referenziert werden darf.
      final QueryResults finalResults = results;

      // Folgendes muss im Event Dispatch Thread ausgeführt werden
      SwingUtilities.invokeLater(() -> {
        // kann mit finalResults == null umgehen
        setListElements(resultsJList, finalResults, resultsDisplayTemplate,
            false);
        updateButtonStates();
      });
    }; // Ende des Erzeugens des Runnable-Objekts r

    // Frame disablen und Suche in eigenem Thread starten
    FrameWorker.disableFrameAndWork(myFrame, r, true);
  }

  private static class MyDialogEndListener implements ActionListener
  {
    private ConfigThingy conf;

    private ConfigThingy abConf;

    private DatasourceJoiner dj;

    private ActionListener dialogEndListener;

    private String actionCommand;

    private PersoenlicheAbsenderlisteVerwalten mySource;

    /**
     * Falls actionPerformed() mit getActionCommand().equals("back") aufgerufen wird,
     * wird ein neuer AbsenderAuswaehlen Dialog mit den übergebenen Parametern
     * erzeugt. Ansonsten wird der dialogEndListener mit actionCommand aufgerufen.
     * Falls actionCommand null ist wird das actioncommand und die source des
     * ActionEvents weitergereicht, der actionPerformed() übergeben wird, ansonsten
     * werden die übergebenen Werte für actionCommand und source verwendet.
     */
    public MyDialogEndListener(PersoenlicheAbsenderlisteVerwalten source,
        ConfigThingy conf, ConfigThingy abConf, DatasourceJoiner dj,
        ActionListener dialogEndListener, String actionCommand)
    {
      this.conf = conf;
      this.abConf = abConf;
      this.dj = dj;
      this.dialogEndListener = dialogEndListener;
      this.actionCommand = actionCommand;
      this.mySource = source;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if ("back".equals(e.getActionCommand()))
        try
        {
          new PersoenlicheAbsenderlisteVerwalten(conf, abConf, dj, dialogEndListener);
        }
        catch (Exception x)
        {
          LOGGER.error("", x);
        }
      else
      {
        Object source = mySource;
        if (actionCommand == null)
        {
          actionCommand = e.getActionCommand();
          source = e.getSource();
        }
        if (dialogEndListener != null)
          dialogEndListener.actionPerformed(new ActionEvent(source, 0, actionCommand));
      }
    }
  }

  /**
   * Zerstört den Dialog. Nach Aufruf dieser Funktion dürfen keine weiteren Aufrufe
   * von Methoden des Dialogs erfolgen. Die Verarbeitung erfolgt asynchron. Wurde dem
   * Konstruktor ein entsprechender ActionListener übergeben, so wird seine
   * actionPerformed() Funktion aufgerufen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    // GUI im Event-Dispatching Thread zerstören wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(() -> abort());
    }
    catch (Exception x)
    {/* Hope for the best */}
  }
}
