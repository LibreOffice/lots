/*
 * Dateiname: WollMuxBar.java
 * Projekt  : WollMux
 * Funktion : Menü-Leiste als zentraler Ausgangspunkt für WollMux-Funktionen
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
 * 02.01.2006 | BNK | Erstellung
 * 03.01.2006 | BNK | Menüs unterstützt
 * 10.01.2006 | BNK | Icon und Config-File pfadunabhängig über Classloader
 *                  | switches --minimize, --topbar, --normalwindow
 * 06.02.2006 | BNK | Menüleiste hinzugefügt
 * 14.02.2006 | BNK | Minimieren rückgängig machen bei Aktivierung der Leiste.
 * 15.02.2006 | BNK | ordentliches Abort auch bei schliessen des Icon-Fensters
 * 19.04.2006 | BNK | [R1342][R1398]große Aufräumaktion, Umstellung auf WollMuxBarEventHandler
 * 20.04.2006 | BNK | [R1207][R1205]Icon der WollMuxBar konfigurierbar, Anzeigemodus konfigurierbar
 * 21.04.2006 | BNK | Umgestellt auf UIElementFactory
 *                  | Bitte Warten... in der Senderbox solange noch keine Verbindung besteht
 *                  | Wenn ein Menü mehrfach verwendet wird, so wird jetzt jedes
 *                  | Mal ein neues erzeugt, um Probleme zu vermeiden, die auftreten
 *                  | könnten, wenn das selbe JMenu an mehreren Stellen in der
 *                  | Komponentenhierarchie erscheint.
 * 24.04.2006 | BNK | kleinere Aufräumarbeiten. Code Review.
 * 24.04.2006 | BNK | [R1390]Popup-Fenster, wenn Verbindung zu OOo WollMux nicht hergestellt
 *                  | werden konnte.
 * 24.04.2006 | BNK | [R1460]Popup-Fenster, wenn WollMux nicht konfiguriert.
 * 02.05.2006 | BNK | [R1202 Teil 1] Fensterposition und Größe von WollMuxBar konfigurierbar
 * 29.05.2006 | BNK | in initFactories() Label Typen explizit genullt.
 *                  | Umstellung auf UIElementFactory.Context
 * 16.06.2006 | BNK | Fokusverlust wird simuliert jedes Mal wenn der Benutzer was
 *                  | drückt, damit sich die WollMuxBar dann minimiert.
 * 21.06.2006 | BNK | Gross/Kleinschreibung ignorieren beim Auswertden des MODE
 *                  | Es wird jetzt der letzte Fenster/WollMuxBar-Abschnitt verwendet.
 * 23.06.2006 | BNK | Senderbox von JComboBox auf JPopupMenu umgestellt.
 * 27.06.2006 | BNK | WIDTH, HEIGHT max korrekt unterstützt
 * 29.06.2006 | BNK | min, max, center unterstützt
 * 19.07.2006 | BNK | MODE "Icon" repariert
 * 02.08.2006 | BNK | bessere Fehlermeldung wenn Konfiguration nicht gefunden.
 * 19.10.2006 | BNK | +ACTION "kill" +ACTION "dumpInfo"
 * 25.10.2006 | BNK | [P923][R3585]Für den minimierten Zustand wird kein extra Fenster mehr verwendet.
 * 25.10.2006 | BNK | Icon-Mode entfernt.
 * 26.10.2006 | LUT | +ACTION "about"
 *                  | +getBuildInfo(), das die buildinfo-Datei der WollMuxBar.jar ausliest
 * 15.01.2007 | BNK | --load hinzugefuegt
 * 23.03.2007 | BNK | openExt implementiert
 * 15.06.2007 | BNK | Beim Download für openExt URL urlEncoden genau wie ConfigThingy für %include
 * 25.06.2007 | BNK | [R7224]Im Minimize-Modus bei Absenderauswahl nicht minimieren
 * 19.07.2007 | BNK | [22882]--load sollte jetzt auch unter Windows funzen
 * 17.12.2010 | ERT | [#5704] Menü der Senderbox wird versteckt, wenn die WollMuxBar unsichtbar wird
 * 19.01.2011 | ERT | Menü der Suchbox wird versteckt, wenn die WollMuxBar unsichtbar wird
 * 28.07.2011 | ERT | [R98726] Verliert die WollMuxBar den Fokus, wird der Fokus von der Suchliste auf das
 *                  | nächste Element verschoben. Dadurch bleibt das Suchmenü geschlossen, wenn die
 *                  | WollMuxBar den Fokus zurückerhält.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import com.sun.star.document.MacroExecMode;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxClassLoader;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.Workarounds;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.controls.UIElement;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;

/**
 * Menü-Leiste als zentraler Ausgangspunkt für WollMux-Funktionen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBar
{
  public static final String ALLOW_USER_CONFIG = "ALLOW_USER_CONFIG";

  private static final String ALLOW_MENUMANAGER = "ALLOW_MENUMANAGER";

  /**
   * Name der Datei in der die WollMuxBar ihre Konfiguration schreibt.
   */
  public static final String WOLLMUXBAR_CONF = "wollmuxbar.conf";

  /**
   * Spezialeintrag in der Absenderliste, der genau dann vorhanden ist, wenn die
   * Absenderliste leer ist.
   */
  private static final String LEERE_LISTE = L.m("<kein Absender vorhanden>");

  public static final Set<String> SUPPORTED_ACTIONS = new HashSet<String>();
  static
  {
    SUPPORTED_ACTIONS.add("openTemplate");
    SUPPORTED_ACTIONS.add("absenderAuswaehlen");
    SUPPORTED_ACTIONS.add("openDocument");
    SUPPORTED_ACTIONS.add("openExt");
    SUPPORTED_ACTIONS.add("open");
    SUPPORTED_ACTIONS.add("dumpInfo");
    SUPPORTED_ACTIONS.add("abort");
    SUPPORTED_ACTIONS.add("kill");
    SUPPORTED_ACTIONS.add("about");
    SUPPORTED_ACTIONS.add("menuManager");
    SUPPORTED_ACTIONS.add("options");
  }

  /**
   * Im Prinzip ist WollMuxBar ein Singleton, wobei instance die aktuell aktive
   * Instanz der WollMuxBar enthält. Wird gesetzt in
   * readWollMuxBarConfAndStartWollMuxBar(...) und gelöscht in WollMuxBar.abort()
   */
  private static WollMuxBar instance = null;

  /**
   * Wird in startWollMuxBar(args) und in terminate(status) verwendet um
   * sicherzustellen, dass das Programm erst bei Beendigung der letzten laufenden
   * Instanz beendet wird.
   */
  private static Integer startedCounter = 0;

  /**
   * Verwaltet die Konfiguration der WollMuxBar.
   */
  private WollMuxBarConfig config;

  /**
   * Siehe {@link WollMuxBarConfig#getX()}. Der Wert aus {@link #config} wird nur
   * einmal gelesen und dann diese Variable hier verwendet, damit im Falle, dass eine
   * feste Koordinate gesetzt ist, der Benutzer trotzdem das Fenster frei verschieben
   * kann nachdem es einmal an die feste Koordinate gesetzt wurde.
   */
  private int myFrame_x;

  /**
   * Siehe {@link WollMuxBarConfig#getY()}. Der Wert aus {@link #config} wird nur
   * einmal gelesen und dann diese Variable hier verwendet, damit im Falle, dass eine
   * feste Koordinate gesetzt ist, der Benutzer trotzdem das Fenster frei verschieben
   * kann nachdem es einmal an die feste Koordinate gesetzt wurde.
   *
   * Außerdem wird dieser Wert immer auf 0 gesetzt wenn der
   * {@link WollMuxBarConfig#UP_AND_AWAY_WINDOW_MODE} gesetzt ist.
   */
  private int myFrame_y;

  /**
   * Das Verhalten der WollMuxBar wird nur einmal aus {@link #config} ausgelesen und
   * dann hier gecachet, weil es ansonsten zu komischen Effekten kommen kann, wenn
   * über den Optionen-Dialog das Fensterverhalten verändert wird und Handler wie
   * z.B. der der auf Focus-Veränderungen reagiert, das Fensterverhalten auswerten
   * bevor der {@link #reinit()} stattgefunden hat.
   */
  private int windowMode;

  /**
   * Enthält das Verhalten desTrayIcons (wie z.B.
   * {@link WollMuxBarConfig#ICONIFY_TRAY_ICON}), das nur einmal aus {@link #config}
   * ausgelesen und dann hier gecachet wird. Siehe auch {@link #windowMode}.
   */
  private int trayIconMode;

  /**
   * Dient der thread-safen Kommunikation mit dem entfernten WollMux.
   */
  private WollMuxBarEventHandler eventHandler;

  /**
   * Der Rahmen, der die Steuerelemente enthält.
   */
  private JFrame myFrame;

  /**
   * Das Panel für den Inhalt des Fensters der WollMuxBar (myFrame).
   */
  private JPanel contentPanel;

  /**
   * Das (optionale) TrayIcon für die WollMuxBar. Kann <code>null</code> sein, wenn
   * kein TrayIcon aktiviert wurde!
   */
  private WollMuxBarTrayIcon trayIcon;

  /**
   * Mappt einen Menü-Namen auf ein entsprechendes JPopupMenu.
   */
  private Map<String, JComponent> mapMenuNameToJPopupMenu =
    new HashMap<String, JComponent>();

  /**
   * Die UIElementFactory, die verwendet wird, um das GUI aufzubauen.
   */
  private UIElementFactory uiElementFactory;

  /**
   * Kontext für GUI-Elemente in JPanels (für Übergabe an die uiElementFactory).
   */
  private UIElementContext panelContext;

  /**
   * Kontext für GUI-Elemente in JMenus und JPopupMenus (für Übergabe an die
   * uiElementFactory).
   */
  private UIElementContext menuContext;

  /**
   * Enthält nach dem Aufruf von initMenuOrder(...) eine Liste aller IDs von Menüs
   * und deren per Tiefensuche ermittelten Untermenüs
   */
  List<String> menuOrder = new ArrayList<String>();

  /**
   * Enthält nach dem Aufruf von initMenuOrder(...) eine Zuordnung von MenüIDs zu den
   * mit vollständigen Pfaden (der Menünavigation) aufgeführten Namen der Menüs
   */
  Map<String, String> mapMenuIDToLabel = new HashMap<String, String>();

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * Die Fehlermeldung die in einem Popup-Fenster gebracht wird, wenn keine
   * Verbindung zu OOo hergestellt werden konnte.
   */
  private static final String CONNECTION_FAILED_MESSAGE =
    L.m("Es konnte keine Verbindung zu OpenOffice bzw. zur WollMux-Komponente in OpenOffice hergestellt werden.\n");

  public static final String WOLLMUX_CONFIG_ERROR_MESSAGE =
    L.m("Aus Ihrer WollMux-Konfiguration konnte kein Abschnitt \"Symbolleisten\" gelesen werden.\n"
      + "Die WollMux-Leiste kann daher nicht gestartet werden. Bitte überprüfen Sie, ob in Ihrer wollmux.conf\n"
      + "der %include für die Konfiguration der WollMuxBar (z.B. wollmuxbar_standard.conf) vorhanden ist und\n"
      + "überprüfen Sie anhand der wollmux.log ob evtl. beim Verarbeiten eines %includes ein Fehler\n"
      + "aufgetreten ist.");

  /**
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent e)
    {
      abort();
    }
  };

  /**
   * ActionListener für Buttons, denen ein Menü zugeordnet ist.
   */
  private ActionListener actionListener_openMenu = new ActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent e)
    {
      openMenu(e);
    }
  };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Aufgerufen wenn der Spezialeintrag "Absenderliste verwalten..." in der Senderbox
   * gewählt wird.
   */
  private ActionListener actionListener_editSenderList = new ActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent e)
    {
      eventHandler.handleWollMuxUrl(Dispatch.DISP_wmPALVerwalten, null);
      minimize();
    }
  };

  /**
   * ActionListener wenn anderer Absender in Senderbox ausgewählt.
   */
  private ActionListener senderboxActionListener = new ActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent e)
    {
      senderBoxItemChanged(e);
    }
  };

  /**
   * Überwacht, ob sich die Maus in irgendwo innerhalb einer Komponente der
   * WollMuxBar befindet.
   */
  private IsInsideMonitor myIsInsideMonitor = new IsInsideMonitor();

  /**
   * Alle {@link Senderbox}es der Leiste.
   */
  private List<Senderbox> senderboxes = new Vector<Senderbox>();

  /**
   * Wird im UP_AND_AWAY_WINDOW_MODE auf das Fenster registriert.
   */
  private UpAndAwayWindowTransformer upAndAwayWindowTransformer =
    new UpAndAwayWindowTransformer();

  /**
   * Das Panel, das das Aussehen des Strichs im UP_AND_AWAY_WINDOW_MODE bestimmt.
   */
  private JPanel upAndAwayMinimizedPanel;

  /**
   * Die Menüleiste der WollMuxBar.
   */
  private JMenuBar menuBar;

  /**
   * Das Kontext-PopupMenu für das Tray-Icon der WollMuxBar. Wird nur bei bestimmten
   * Tray-Icon-Modi befüllt und kann daher <code>null</code> sein!
   */
  private JPopupMenu trayIconMenu;

  /**
   * <code>true</code> zeigt an, dass die Leiste im
   * {@link WollMuxBarConfig#UP_AND_AWAY_WINDOW_MODE} minimiert ist.
   */
  private boolean windowIsAway = false;

  /**
   * Die wollmux.conf.
   */
  private ConfigThingy defaultConf;

  /**
   * Die wollmuxbar.conf.
   */
  private ConfigThingy userConf;

  /**
   * Ist allowUserConfig==false, darf die userConf nicht ausgewertet werden (und
   * damit die Defaulteinstellungen überschreiben). Außerdem dürfen die Buttons mit
   * den ACTIONs "menuManager" und "options" nicht in der WollMuxBar erscheinen.
   */
  private boolean allowUserConfig;

  /**
   * Ist allowMenuManager==false, darf der Button mit der ACTION "menuManager" nicht
   * in der WollMuxBar erscheinen.
   */
  private boolean allowMenuManager;

  /**
   * Erzeugt eine neue WollMuxBar.
   *
   * @param winMode
   *          Anzeigemodus, z.B. {@link WollMuxBarConfig#UP_AND_AWAY_WINDOW_MODE}.
   * @param conf
   *          combinedConf(wollmuxConf(<Inhalt der wollmux.conf>)
   *          wollmuxbarConf(<Inhalt der wollmuxbar.conf>)
   * @param defaultConf
   *          die wollmux.conf
   * @param userConf
   *          die wollmuxbar.conf
   * @param allowUserConfig
   *          falls allowUserConfig==false, darf die wollmuxbar.conf nicht
   *          ausgewertet werden und die buttons mit der Aktion "options" und
   *          "menuManager" dürfen nicht in der WollMuxBar erscheinen.
   * @param allowMenuManager
   *          falls allowMenuManager==false, darf der Button mit der Aktion
   *          "menuManager" nicht in der WollMuxBar erscheinen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar(int winMode, final ConfigThingy conf, ConfigThingy defaultConf,
      ConfigThingy userConf, boolean allowUserConfig, boolean allowMenuManager)
  {
    this.defaultConf = defaultConf;
    this.userConf = userConf;
    this.allowUserConfig = allowUserConfig;
    this.allowMenuManager = allowMenuManager;
    config = new WollMuxBarConfig(winMode, defaultConf, userConf, allowUserConfig);

    eventHandler = new WollMuxBarEventHandler(this);
    eventHandler.start();

    WollMuxFiles.zoomFonts(config.getFontZoom());
    /*
     * Die GUI wird im Event-Dispatching Thread erzeugt wg. Thread-Safety. Auch
     * eventHandler.connectWithWollMux() wird im EDT ausgeführt, um sicherzustellen,
     * dass kein updateSenderBoxes() ausgeführt wird, bevor nicht die Senderboxen
     * erzeugt wurden.
     */
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            /*
             * Dieser Befehl steht VOR dem Aufruf von createGUI(), damit OOo schon
             * gestartet wird, während wir noch mit GUI aufbauen beschäftigt sind. Es
             * ist trotztdem sichergestellt, dass updateSenderboxes() nicht vor der
             * Beendigung von createGUI() aufgerufen werden kann, weil
             * updateSenderboxes() durch den WollMuxBarEventHandler ebenfalls mit
             * invokeLater() in den EDT geschickt wird und dort erst zum Zug kommen
             * kann, wenn diese run() Methode beendet ist.
             */
            eventHandler.connectWithWollMux();

            Workarounds.applyWorkaroundForWMClass();

            createGUI(conf);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
          ;
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  public IsInsideMonitor getMyIsInsideMonitor()
  {
    return myIsInsideMonitor;
  }
  
  private void createGUI(ConfigThingy conf)
  {
    initFactories();

    // Wohl nicht mehr erforderlich, seit auf ein einziges Fenster umgestellt wurde:
    // Mit
    // file:///C:/Programme/j2sdk1.4.2_08/docs/api/java/awt/doc-files/FocusSpec.html
    // das Blink-Problem in Griff kriegen und vielleicht auch die WollMuxBar nicht
    // mehr fokussierbar machen (vor allem die minimierte Version). Eventuell
    // nuetzlich dazu sind JWindow-Klasse und evtl. muss ein blinder JFrame oder ein
    // blindes JWindow als Parent in die Hierarchie eingefuegt werden (als Parent der
    // eigentlichen WollMuxBar-Fenster)

    // Toolkit tk = Toolkit.getDefaultToolkit();
    // GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    // Dimension screenSize = tk.getScreenSize();
    // Rectangle bounds = genv.getMaximumWindowBounds();

    myFrame = new JFrame(config.getWindowTitle());
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    myFrame_x = config.getX();
    myFrame_y = config.getY();
    windowMode = config.getWindowMode();
    trayIconMode = config.getTrayIconMode();

    // set the icon for the WollMuxBar frame
    Common.setWollMuxIcon(myFrame);

    if (windowMode == WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.setUndecorated(true);
      // myFrame.setFocusable(false);
      // myFrame.setFocusableWindowState(false);
      myFrame_y = 0;
    }

    // Ein WindowListener, der auf den JFrame registriert wird, damit als
    // Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
    myFrame.addWindowListener(new MyWindowListener());

    WindowTransformer myWindowTransformer = new WindowTransformer();
    myFrame.addWindowFocusListener(myWindowTransformer);

    contentPanel = new JPanel();
    contentPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    contentPanel.setLayout(new GridBagLayout());
    contentPanel.addMouseListener(myIsInsideMonitor);
    myFrame.getContentPane().add(contentPanel);

    try
    {
      ConfigThingy bkl = conf.query("Symbolleisten").query("Briefkopfleiste");
      if (bkl.count() > 0)
      {
        addUIElements(conf.query("Menues"), bkl.getLastChild(), contentPanel, 1, 0,
          "panel");
      }
    }
    catch (NodeNotFoundException x)
    {
      Logger.error(x);
    }

    trayIconMenu = null;
    menuBar = new JMenuBar();
    menuBar.addMouseListener(myIsInsideMonitor);
    try
    {
      ConfigThingy menubar = conf.query("Menueleiste");
      if (menubar.count() > 0)
      {
        ConfigThingy menueConf = conf.query("Menues");
        initMenuOrder(menueConf, menubar.getLastChild(), "");
        addUIElements(menueConf, menubar.getLastChild(), menuBar, 1, 0, "menu");

        // Gegebenfalls Kontext-Menü für TrayIcon befüllen
        if (trayIconMode == WollMuxBarConfig.POPUP_TRAY_ICON
          || trayIconMode == WollMuxBarConfig.ICONIFY_AND_POPUP_TRAY_ICON)
        {
          trayIconMenu = new JPopupMenu();
          addUIElements(menueConf, menubar.getLastChild(), trayIconMenu, 1, 0,
            "menu");
        }
      }
    }
    catch (NodeNotFoundException x)
    {
      Logger.error(x);
    }
    myFrame.setJMenuBar(menuBar);

    setupMinimizedFrame(config.getWindowTitle());

    if (windowMode != WollMuxBarConfig.NORMAL_WINDOW_MODE)
      myFrame.setAlwaysOnTop(true);

    if (trayIconMode == WollMuxBarConfig.ICONIFY_TRAY_ICON)
    {
      trayIcon = new WollMuxBarTrayIcon(null, myFrame, true);
    }
    else if (trayIconMode == WollMuxBarConfig.POPUP_TRAY_ICON)
    {
      trayIcon = new WollMuxBarTrayIcon(trayIconMenu, myFrame, false);
    }
    else if (trayIconMode == WollMuxBarConfig.ICONIFY_AND_POPUP_TRAY_ICON)
    {
      trayIcon = new WollMuxBarTrayIcon(trayIconMenu, myFrame, true);
    }

    if (trayIcon != null)
    {
      try
      {
        trayIcon.addToTray();
      }
      catch (UnavailableException e)
      {
        Logger.log(L.m("Konnte Tray-Icon nicht zur System-Tray hinzufügen."));
      }
    }

    setSizeAndLocation();
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }

  /**
   * Passt die Größe und Position der Fenster an.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setSizeAndLocation()
  {
    if (windowIsAway) return;
    // Toolkit tk = Toolkit.getDefaultToolkit();
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    // Dimension screenSize = tk.getScreenSize();
    Rectangle bounds = genv.getMaximumWindowBounds();

    myFrame.pack();
    Dimension naturalFrameSize = myFrame.getSize();
    Dimension frameSize = new Dimension(naturalFrameSize);
    Point frameLocation = myFrame.getLocation();

    switch (config.getWidth())
    {
      case 0: // natural width
        break;
      case -1: // max
        frameSize.width = bounds.width;
        break;
      default: // specified width
        frameSize.width = config.getWidth();
        break;
    }

    switch (config.getHeight())
    {
      case 0: // natural height
        break;
      case -1: // max
        frameSize.height = bounds.height;
        break;
      default: // specified height
        frameSize.height = config.getHeight();
        break;
    }

    switch (myFrame_x)
    {
      case -1: // center
        frameLocation.x = bounds.x + (bounds.width - frameSize.width) / 2;
        break;
      case -2: // max
        frameLocation.x = bounds.x + bounds.width - frameSize.width;
        break;
      case -3: // min
        frameLocation.x = bounds.x;
        break;
      case Integer.MIN_VALUE: // kein Wert angegeben
        break;
      default: // Wert angegeben, wird nur einmal berücksichtigt.
        frameLocation.x = myFrame_x;
        myFrame_x = Integer.MIN_VALUE;
        break;
    }

    switch (myFrame_y)
    {
      case -1: // center
        frameLocation.y = bounds.y + (bounds.height - frameSize.height) / 2;
        break;
      case -2: // max
        frameLocation.y = bounds.y + bounds.height - frameSize.height;
        break;
      case -3: // min
        frameLocation.y = bounds.y;
        break;
      case Integer.MIN_VALUE: // kein Wert angegeben
        break;
      default: // Wert angegeben, wird nur einmal berücksichtigt.
        frameLocation.y = myFrame_y;
        myFrame_y = Integer.MIN_VALUE;
        break;
    }

    myFrame.setSize(frameSize);
    myFrame.setLocation(frameLocation);
    myFrame.validate(); // ohne diese wurde in Tests manchmal nicht neu gezeichnet
  }

  /**
   * Erzeugt den JFrame für die minimierte Darstellung (WollMux-Logo oder schmaler
   * Streifen).
   *
   * @param title
   *          der Titel für das Fenster (nur für Anzeige in Taskleiste)
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setupMinimizedFrame(String title)
  {
    if (windowMode == WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE)
    {
      upAndAwayMinimizedPanel = new JPanel();
      upAndAwayMinimizedPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    }
  }

  /**
   * Fügt der Komponente compo UI Elemente hinzu, eines für jedes Kind von
   * elementParent.
   *
   * @param menuConf
   *          die Kinder dieses ConfigThingys müssen "Menues"-Knoten sein, deren
   *          Kinder Menübeschreibungen sind für die Menüs, die als UI Elemente
   *          verwendet werden.
   * @param elementParent
   *          das Element, dessen Kinder die UI Elemente beschreiben.
   * @param context
   *          kann die Werte "menu" oder "panel" haben und gibt an, um was es sich
   *          bei compo handelt. Abhängig vom context werden manche UI Elemente
   *          anders interpretiert, z.B. werden "button" Elemente im context "menu"
   *          zu JMenuItems.
   * @param compo
   *          die Komponente zu der die UI Elemente hinzugefügt werden sollen. Falls
   *          context nicht "menu" ist, muss compo ein GridBagLayout haben.
   * @param stepx
   *          stepx und stepy geben an, um wieviel mit jedem UI Element die x und die
   *          y Koordinate innerhalb des GridBagLayouts erhöht werden sollen.
   *          Sinnvoll sind hier normalerweise nur (0,1) und (1,0).
   * @param stepy
   *          siehe stepx
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  void addUIElements(ConfigThingy menuConf, ConfigThingy elementParent,
      JComponent compo, int stepx, int stepy, String context)
  {
    addUIElementsChecked(new HashSet<String>(), menuConf, elementParent, compo,
      stepx, stepy, context);
  }

  /**
   * Wie addUIElements, aber reicht den Parameter alreadySeen an parseMenu weiter, um
   * sich gegenseitig enthaltende Menüs zu erkennen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void addUIElementsChecked(Set<String> alreadySeen, ConfigThingy menuConf,
      ConfigThingy elementParent, JComponent compo, int stepx, int stepy,
      String context)
  {
    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    // GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
    // GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new
    // Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcMenuButton =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    GridBagConstraints gbcSenderbox =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);

    int y = -stepy;
    int x = -stepx;

    UIElementContext contextMap =
      context.equals("menu") ? menuContext : panelContext;

    Iterator<ConfigThingy> piter = elementParent.iterator();
    while (piter.hasNext())
    {
      ConfigThingy uiElementDesc = piter.next();
      y += stepy;
      x += stepx;

      try
      {
        /*
         * Falls kein CONF_ID vorhanden ist, wird das Element angezeigt, ansonsten
         * nur dann wenn mindestens eine CONF_ID aktiv ist.
         */
        ConfigThingy conf_ids = uiElementDesc.query("CONF_ID");
        if (conf_ids.count() > 0)
        {
          boolean active = false;
          for (ConfigThingy conf_id_group : conf_ids)
            for (ConfigThingy conf_id : conf_id_group)
              if (config.isIDActive(conf_id.getName()))
              {
                active = true;
                break;
              }
          if (!active) continue;
        }

        String type;
        try
        {
          type = uiElementDesc.get("TYPE").toString();
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(L.m("Ein User Interface Element ohne TYPE wurde entdeckt"));
          continue;
        }

        if (type.equals("senderbox"))
        {
          char hotkey = 0;
          try
          {
            hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
          }
          catch (Exception e)
          {}

          String label = L.m("Bitte warten...");
          Senderbox senderbox;
          JComponent menu;
          AbstractButton button;
          if (context.equals("menu"))
          {
            menu = new JMenu(label);
            button = (AbstractButton) menu;
            senderbox = Senderbox.create((JMenu) menu);
          }
          else
          {
            menu = new JPopupMenu();
            String menuName = "SenD3rB0x_" + Math.random();
            mapMenuNameToJPopupMenu.put(menuName, menu);
            button = new JButton(label);
            button.addActionListener(actionListener_openMenu);
            button.setActionCommand(menuName);
            button.setBackground(Color.WHITE);
            button.setFocusable(false);
            senderbox = Senderbox.create((JPopupMenu) menu, button);
          }

          button.setMnemonic(hotkey);

          senderboxes.add(senderbox);

          gbcSenderbox.gridx = x;
          gbcSenderbox.gridy = y;
          button.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
            compo.add(button);
          else
            compo.add(button, gbcSenderbox);
        }
        else if (type.equals("searchbox"))
        {
          String label = L.m("Suchen...");
          try
          {
            String str = uiElementDesc.get("LABEL").toString();
            label = L.m(str);
          }
          catch (Exception e)
          {}

          SearchBox searchBox = new SearchBox(this, label, menuConf);
          JTextField sfield = searchBox.getTextField();
          sfield.setMinimumSize(sfield.getPreferredSize());
          sfield.setMaximumSize(sfield.getPreferredSize());

          gbcMenuButton.gridx = x;
          gbcMenuButton.gridy = y;
          sfield.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
          {
            if (compo != trayIconMenu) compo.add(sfield);
          }
          else
          {
            compo.add(sfield, gbcMenuButton);
          }
        }
        else if (type.equals("menu"))
        {
          String label = L.m("LABEL FEHLT ODER FEHLERHAFT!");
          try
          {
            String str = uiElementDesc.get("LABEL").toString();
            label = L.m(str);
          }
          catch (Exception e)
          {}

          char hotkey = 0;
          try
          {
            hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
          }
          catch (Exception e)
          {}

          String menuName = "";
          try
          {
            menuName = uiElementDesc.get("MENU").toString();
          }
          catch (NodeNotFoundException e)
          {}

          AbstractButton button;
          if (context.equals("menu"))
          {
            button =
              (AbstractButton) parseMenu(alreadySeen, null, menuConf, menuName,
                new JMenu(label));
            if (button == null) button = new JMenu(label);
          }
          else
          {
            parseMenu(alreadySeen, mapMenuNameToJPopupMenu, menuConf, menuName,
              new JPopupMenu());
            button = new JButton(label);
            button.addActionListener(actionListener_openMenu);
            button.setActionCommand(menuName);
          }

          button.setMnemonic(hotkey);

          gbcMenuButton.gridx = x;
          gbcMenuButton.gridy = y;
          button.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
            compo.add(button);
          else
            compo.add(button, gbcMenuButton);
        }
        else
        {
          // deactivate menuManager and options buttons if allowUserConfig is not activated
          boolean active = true;
          try
          {
            String action = uiElementDesc.get("ACTION").toString();
            if(action.equals("menuManager") && !(allowUserConfig && allowMenuManager))
              active = false;
            if(action.equals("options") && !(allowUserConfig))
              active = false;
          }
          catch (Exception e)
          {}

          UIElement uiElement =
            uiElementFactory.createUIElement(contextMap, uiElementDesc);
          GridBagConstraints gbc =
            (GridBagConstraints) uiElement.getLayoutConstraints();
          gbc.gridx = x;
          gbc.gridy = y;
          Component uiComponent = uiElement.getComponent();
          uiComponent.addMouseListener(myIsInsideMonitor);

          if(active)
          {
            if (context.equals("menu"))
              compo.add(uiComponent);
            else
              compo.add(uiComponent, gbc);
          }
          else
          {
            String label = "";
            try
            {
              String str = uiElementDesc.get("LABEL").toString();
              label = L.m(str);
            }
            catch (Exception e)
            {}
            Logger.debug(L.m(
              "Warnung: Button '%1' wurde konfigurativ deaktiviert und ist daher nicht in der WollMuxBar enthalten.",
              label));
          }
        }
      }
      catch (ConfigurationErrorException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Parst eine Menübeschreibung und erzeugt ein entsprechendes Menü.
   *
   * @param menu
   *          das JMenu oder JPopupMenu zu dem die UI Elemente hinzugefügt werden
   *          sollen.
   * @param menuConf
   *          die Kinder dieses ConfigThingys müssen "Menues"-Knoten sein, deren
   *          Kinder Menübeschreibungen sind.
   * @param menuName
   *          identifiziert das Menü aus menuConf, das geparst wird. Gibt es mehrere,
   *          so wird das letzte verwendet.
   * @param mapMenuNameToMenu
   *          falls nicht-null, so wird falls bereits ein Eintrag menuName enthalten
   *          ist, dieser zurückgeliefert, ansonsten wird ein Mapping von menuName
   *          auf menu hinzugefügt. Falls null, so wird immer ein neues Menü erzeugt,
   *          außer das menuName ist in alreadySeen, dann gibt es eine Fehlermeldung.
   * @param alreadySeen
   *          falls menuName hier enthalten ist und mapMenuNameToMenu==null dann wird
   *          eine Fehlermeldung ausgegeben und null zurückgeliefert.
   *
   * @return menu, falls das Menü erfolgreich aufgebaut werden konnte, null, wenn das
   *         Menü nicht in menuConf definiert ist oder wenn es in alreadySeen ist und
   *         mapMenuNameToMenu == null.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private JComponent parseMenu(Set<String> alreadySeen,
      Map<String, JComponent> mapMenuNameToMenu, ConfigThingy menuConf,
      String menuName, JComponent menu)
  {
    if (mapMenuNameToMenu != null && mapMenuNameToMenu.containsKey(menuName))
      return mapMenuNameToMenu.get(menuName);

    if (mapMenuNameToMenu == null && alreadySeen.contains(menuName))
    {
      Logger.error(L.m(
        "Menü \"%1\" ist an einer Endlosschleife sich gegenseitig enthaltender Menüs beteiligt",
        menuName));
      return null;
    }

    ConfigThingy conf;
    try
    {
      conf = menuConf.query(menuName).getLastChild().get("Elemente");
    }
    catch (Exception x)
    {
      Logger.error(L.m(
        "Menü \"%1\" nicht definiert oder enthält keinen Abschnitt \"Elemente()\"",
        menuName));
      return null;
    }

    /*
     * Zur Vermeidung von Endlosschleifen müssen die folgenden BEIDEN Statements vor
     * dem Aufruf von addUIElementsChecked stehen.
     */
    alreadySeen.add(menuName);
    if (mapMenuNameToMenu != null) mapMenuNameToMenu.put(menuName, menu);

    addUIElementsChecked(alreadySeen, menuConf, conf, menu, 0, 1, "menu");
    alreadySeen.remove(menuName);
    return menu;
  }

  /**
   * Initialisiert uiElementFactory.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void initFactories()
  {
    Map<String, GridBagConstraints> mapTypeToLayoutConstraints =
      new HashMap<String, GridBagConstraints>();
    Map<String, Integer> mapTypeToLabelType = new HashMap<String, Integer>();
    Map<String, Object> mapTypeToLabelLayoutConstraints =
      new HashMap<String, Object>();

    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcCombobox =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcLabel =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcButton =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    GridBagConstraints gbcHsep =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL,
        new Insets(3 * TF_BORDER, 0, 2 * TF_BORDER, 0), 0, 0);
    GridBagConstraints gbcVsep =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
        GridBagConstraints.VERTICAL, new Insets(0, TF_BORDER, 0, TF_BORDER), 0, 0);
    GridBagConstraints gbcGlue =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    mapTypeToLayoutConstraints.put("default", gbcButton);
    mapTypeToLabelType.put("default", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("default", null);

    mapTypeToLayoutConstraints.put("combobox", gbcCombobox);
    mapTypeToLabelType.put("combobox", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("combobox", null);

    mapTypeToLayoutConstraints.put("h-glue", gbcGlue);
    mapTypeToLabelType.put("h-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-glue", null);
    mapTypeToLayoutConstraints.put("v-glue", gbcGlue);
    mapTypeToLabelType.put("v-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-glue", null);

    mapTypeToLayoutConstraints.put("label", gbcLabel);
    mapTypeToLabelType.put("label", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("label", null);

    mapTypeToLayoutConstraints.put("button", gbcButton);
    mapTypeToLabelType.put("button", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("button", null);

    mapTypeToLayoutConstraints.put("h-separator", gbcHsep);
    mapTypeToLabelType.put("h-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-separator", null);
    mapTypeToLayoutConstraints.put("v-separator", gbcVsep);
    mapTypeToLabelType.put("v-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-separator", null);

    UIElementEventHandler myUIElementEventHandler = new MyUIElementEventHandler();

    panelContext = new UIElementContext();
    panelContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    panelContext.mapTypeToLabelType = mapTypeToLabelType;
    panelContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    panelContext.uiElementEventHandler = myUIElementEventHandler;
    panelContext.mapTypeToType = new HashMap<String, String>();
    panelContext.mapTypeToType.put("separator", "v-separator");
    panelContext.mapTypeToType.put("glue", "h-glue");

    menuContext = new UIElementContext();
    menuContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    menuContext.mapTypeToLabelType = mapTypeToLabelType;
    menuContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    menuContext.uiElementEventHandler = myUIElementEventHandler;
    menuContext.mapTypeToType = new HashMap<String, String>();
    menuContext.mapTypeToType.put("separator", "h-separator");
    menuContext.mapTypeToType.put("glue", "v-glue");
    menuContext.mapTypeToType.put("button", "menuitem");

    panelContext.supportedActions = SUPPORTED_ACTIONS;
    menuContext.supportedActions = SUPPORTED_ACTIONS;

    uiElementFactory = new UIElementFactory();
  }

  /**
   * Behandelt die Events der Eingabeelemente, die über die uiElementFactory erzeugt
   * wurden (also fast alle).
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyUIElementEventHandler implements UIElementEventHandler
  {
    @Override
    public void processUiElementEvent(UIElement source, String eventType,
        Object[] args)
    {
      if (!eventType.equals("action")) return;

      String action = args[0].toString();
      if (action.equals("absenderAuswaehlen"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmAbsenderAuswaehlen, "");
      }
      else if (action.equals("openDocument"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmOpenDocument,
          args[1].toString());
      }
      else if (action.equals("openTemplate"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmOpenTemplate,
          args[1].toString());
      }
      else if (action.equals("open"))
      {
        minimize();
        multiOpenDialog((ConfigThingy) args[1]);
      }
      else if (action.equals("openExt"))
      {
        minimize();
        openExt((String) args[1], (String) args[2]);
      }
      else if (action.equals("dumpInfo"))
      {
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmDumpInfo, null);
      }
      else if (action.equals("abort"))
      {
        abort();
      }
      else if (action.equals("kill"))
      {
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmKill, null);
        abort();
      }
      else if (action.equals("about"))
      {
        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmAbout, getBuildInfo());
      }
      else if (action.equals("menuManager"))
      {
        menuManager();
      }
      else if (action.equals("options"))
      {
        options();
      }
    }
  }

  /**
   * Erwartet in conf eine Spezifikation gemäß wollmux:Open und bringt einen
   * Auswahldialog, um die zu öffnenden Vorlagen/Dokumente auszuwählen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void multiOpenDialog(final ConfigThingy conf)
  {
    final JFrame multiOpenFrame = new MultiOpenDialog(L.m("Was möchten Sie öffnen ?"), conf, eventHandler);
    multiOpenFrame.setVisible(true);
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    instance = null;
    eventHandler.handleTerminate();
    myFrame.dispose();
    if (trayIcon != null)
    {
      try
      {
        trayIcon.removeFromTray();
      }
      catch (UnavailableException e)
      {
        // do nothing
      }
    }
    eventHandler.waitForThreadTermination();

    terminate(0);
  }

  /**
   * Startet den {@link MenuManager} und führt dann eine Reinitialisierung der
   * WollMuxBar aus.
   *
   */
  private void menuManager()
  {
    new MenuManager(defaultConf, userConf, new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        reinit();
      }
    });
  }

  /**
   * Zeigt den Optionsdialog von {@link WollMuxBarConfig} und führt dann eine
   * Reinitialisierung der WollMuxBar aus.
   *
   */
  private void options()
  {
    config.showOptionsDialog(myFrame, new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        // derzeit ist der OK-Vergleich unnötig, da bei negativer Beendigung des
        // Dialogs der ActionListener eh
        // nicht aufgerufen wird. Aber das kann sich ändern.
        if (e.getActionCommand().equals("OK")) reinit();
      }
    });
  }

  /**
   * Lässt die WollMuxBar sich komplett neu starten.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private void reinit()
  {
    eventHandler.handleTerminate();
    myFrame.dispose();
    if (trayIcon != null)
    {
      try
      {
        trayIcon.removeFromTray();
      }
      catch (UnavailableException e)
      {
        // do nothing
      }
    }
    eventHandler.waitForThreadTermination();
    readWollMuxBarConfAndStartWollMuxBar(config.getWindowMode(),
      false, allowUserConfig, allowMenuManager, defaultConf);
  }

  /**
   * Diese Methode liefert die erste Zeile aus der buildinfo-Datei der aktuellen
   * WollMuxBar zurück. Der Build-Status wird während dem Build-Prozess mit dem
   * Kommando "svn info" auf das Projektverzeichnis erstellt. Die Buildinfo-Datei
   * buildinfo enthält die Paketnummer und die svn-Revision und ist in der Datei
   * WollMuxBar.jar enthalten.
   *
   * Kann dieses File nicht gelesen werden, so wird eine entsprechende Ersatzmeldung
   * erzeugt (siehe Sourcecode).
   *
   * @return Der Build-Status der aktuellen WollMuxBar.
   */
  public static String getBuildInfo()
  {
    BufferedReader in = null;
    try
    {
      URL url = WollMuxBar.class.getClassLoader().getResource("buildinfo");
      if (url != null)
      {
        in = new BufferedReader(new InputStreamReader(url.openStream()));
        String str = in.readLine();
        if (str != null) return str;
      }
    }
    catch (Exception x)
    {}
    finally
    {
      try
      {
        in.close();
      }
      catch (Exception y)
      {}
    }

    return L.m("Version: unbekannt");
  }

  /**
   * Wird aufgerufen, wenn ein Button aktiviert wird, dem ein Menü zugeordnet ist und
   * lässt dann das entsprechende Menü aus mapMenuNameToJPopupMenu erscheinen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void openMenu(ActionEvent e)
  {
    String menuName = e.getActionCommand();
    JComponent compo;
    try
    {
      compo = (JComponent) e.getSource();
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }

    JPopupMenu menu = (JPopupMenu) mapMenuNameToJPopupMenu.get(menuName);
    if (menu == null) return;

    menu.show(compo, 0, compo.getHeight());
  }

  /**
   * Diese Methode wird aufgerufen, wenn in der Senderbox ein anderes Element
   * ausgewählt wurde und setzt daraufhin den aktuellen Absender im entfernten
   * WollMux neu.
   *
   * @author Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  private void senderBoxItemChanged(ActionEvent e)
  {
    String[] str = e.getActionCommand().split(":", 2);
    int index = Integer.parseInt(str[0]);
    String item = str[1];
    eventHandler.handleSelectPALEntry(item, index);
    myIsInsideMonitor.delayedMinimize();
  }

  /**
   * Setzt die Einträge aller Senderboxes neu.
   *
   * @param entries
   *          die Einträge, die die Senderboxen enthalten sollen.
   * @param current
   *          der ausgewählte Eintrag
   * @author Matthias Benkmann, Christoph Lutz (D-III-ITD 5.1) TESTED
   */
  public void updateSenderboxes(String[] entries, String current)
  {
    Iterator<Senderbox> iter = senderboxes.iterator();
    while (iter.hasNext())
    {
      Senderbox senderbox = iter.next();

      // alte Items löschen
      senderbox.removeAllItems();

      // neue Items eintragen
      if (entries.length > 0)
      {
        for (int i = 0; i < entries.length; i++)
        {
          String entry = entries[i];
          // Da die anzuzeigenden Einträge in der Regel irgendwo aus der PAL kommen
          // und über einen XPALProvider bekommen wurden, hängt am Eintrag-Ende
          // noch der Schlüssel des zugrundeliegenden Datensatzes getrennt mit
          // "§§%=%§§" an. Diesen wollen wir aber nicht in der WollMuxBar-Senderbox
          // anzeigen, weshalb wir ihn an dieser Stelle abschneiden.
          // (Dabei hoffen wir einfach, dass "§§%=%§§" nur einmal vorkommt.)
          String entryNoKey = entry.split("§§%=%§§")[0];
          senderbox.addItem(entryNoKey, senderboxActionListener, "" + i + ":"
            + entry, myIsInsideMonitor);
        }
      }
      else
        senderbox.addItem(LEERE_LISTE, null, null, myIsInsideMonitor);

      senderbox.addSeparator();
      senderbox.addItem(L.m("Absenderliste verwalten..."),
        actionListener_editSenderList, null, myIsInsideMonitor);

      if (current != null && !current.equals(""))
      {
        // Da die anzuzeigenden Einträge in der Regel irgendwo aus der PAL kommen
        // und über einen XPALProvider bekommen wurden, hängt am Eintrag-Ende
        // noch der Schlüssel des zugrundeliegenden Datensatzes getrennt mit
        // "§§%=%§§" an. Diesen wollen wir aber nicht in der WollMuxBar-Senderbox
        // anzeigen, weshalb wir ihn an dieser Stelle abschneiden.
        // (Dabei hoffen wir einfach, dass "§§%=%§§" nur einmal vorkommt.)
        String currentNoKey = current.split("§§%=%§§")[0];
        senderbox.setSelectedItem(currentNoKey);
      }
    }

    setSizeAndLocation();
  }

  /**
   * Diese Methode initialisiert die Datenstrukturen menuOrder und mapMenuIDToLabel
   * zur Verwendung für die Searchbox. Dabei werden ausgehend von currentMenu alle
   * dort beschriebenen Untermenüs zu menuOrder hinzugefügt; Weitere Unter-Untermenüs
   * werden rekursiv durchsucht und ebenfalls angehängt.
   *
   * enthält menuOrder ausgehend vom Startmenü currentMenu (in gegebener Reihenfolge)
   * alle Menüs und Untermenüs
   *
   * @param allMenues
   *          Erwartet die "Menues"-Knoten der WollMux-Konfiguration, in denen die
   *          verfügbaren Menü-IDs aller möglichen Menüs beschrieben sind.
   * @param currentMenu
   *          Ausgehend von currentMenu werden rekursiv alle enthaltenen Unter- und
   *          Unter-Untermenüs zu menuOrder hinzugefügt.
   * @param path
   *          Beschreibt den Namen des jeweils übergeordneten Menüs (initial sollte
   *          "" übergeben werden), aus dem der Name für mapMenuIDToLabel
   *          zusammengesetzt wird.
   * @author Christoph Lutz (privat)
   */
  public void initMenuOrder(ConfigThingy allMenues, ConfigThingy currentMenu,
      String path)
  {
    for (ConfigThingy sub : currentMenu.queryByChild("MENU"))
    {
      try
      {
        String id = sub.get("MENU").toString();
        String label = path + sub.get("LABEL").toString();
        menuOrder.add(id);
        mapMenuIDToLabel.put(id, label);
        initMenuOrder(allMenues, allMenues.query(id).getLastChild(), label + " / ");
      }
      catch (NodeNotFoundException e)
      {
        Logger.log(e);
      }
    }
  }

  /**
   * Erzeugt ein Popup-Fenster, das den Benutzer darüber informiert, dass keine
   * Verbindung zu OpenOffice hergestellt werden konnte.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void connectionFailedWarning()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          JOptionPane.showMessageDialog(null, CONNECTION_FAILED_MESSAGE,
            L.m("WollMux-Fehler"), JOptionPane.ERROR_MESSAGE);
        }
      });
    }
    catch (Exception y)
    {}
  }

  /**
   * Ein WindowListener, der auf die JFrames der Leiste registriert wird, damit als
   * Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener()
    {}

    @Override
    public void windowActivated(WindowEvent e)
    {}

    @Override
    public void windowClosed(WindowEvent e)
    {}

    @Override
    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
    }

    @Override
    public void windowDeactivated(WindowEvent e)
    {}

    @Override
    public void windowDeiconified(WindowEvent e)
    {}

    @Override
    public void windowIconified(WindowEvent e)
    {}

    @Override
    public void windowOpened(WindowEvent e)
    {}
  }

  /**
   * Wird auf das Leistenfenster als WindowFocusListener registriert, um falls
   * erforderlich das minimieren anzustoßen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class WindowTransformer implements WindowFocusListener
  {
    @Override
    public void windowGainedFocus(WindowEvent e)
    {}

    @Override
    public void windowLostFocus(WindowEvent e)
    {
      minimize();
    }

  }

  /**
   * Wird auf den Strich am oberen Bildschirmrand registriert im UpAndAway Modus, um
   * darauf reagieren zu können, wenn die Maus dort eindringt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class UpAndAwayWindowTransformer implements MouseListener, ActionListener
  {
    private Timer timer;

    public UpAndAwayWindowTransformer()
    {
      timer = new Timer(500, this);
      timer.setRepeats(false);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {}

    @Override
    public void mousePressed(MouseEvent e)
    {}

    @Override
    public void mouseReleased(MouseEvent e)
    {}

    @Override
    public void mouseEntered(MouseEvent e)
    {
      timer.restart();
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
      timer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      maximize();
    }
  }

  /**
   * Wird auf alle Komponenten der WollMuxBar registriert, um zu überwachen, ob die
   * Maus in einer dieser Komponenten ist.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class IsInsideMonitor implements MouseListener, ActionListener
  {
    private Timer timer;

    public IsInsideMonitor()
    {
      timer = new Timer(1000, this);
      timer.setRepeats(false);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {}

    @Override
    public void mousePressed(MouseEvent e)
    {}

    @Override
    public void mouseReleased(MouseEvent e)
    {}

    @Override
    public void mouseEntered(MouseEvent e)
    {
      if (windowMode != WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE) return;
      timer.stop();
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
      delayedMinimize();
    }

    public void delayedMinimize()
    {
      if (windowMode != WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE) return;
      timer.restart();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      minimize();
    }
  }

  /**
   * Je nach windowMode wird die WollMuxBar auf andere Art und Weise in den
   * Wartezustand versetzt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void minimize()
  {
    /*
     * Minimieren stört die Anzeige des modalen Options-Dialogs (zumindest unter
     * manchen Window-Managern).
     */
    if (config.isDialogVisible()) return;

    if (windowMode == WollMuxBarConfig.ALWAYS_ON_TOP_WINDOW_MODE
      || windowMode == WollMuxBarConfig.NORMAL_WINDOW_MODE) return;
    if (windowMode == WollMuxBarConfig.MINIMIZE_TO_TASKBAR_MODE
      || trayIconMode == WollMuxBarConfig.ICONIFY_TRAY_ICON
      || trayIconMode == WollMuxBarConfig.ICONIFY_AND_POPUP_TRAY_ICON)
    {
      if (windowMode == WollMuxBarConfig.MINIMIZE_TO_TASKBAR_MODE)
      {
        /*
         * Wir brauchen hier einen Timer, weil wenn auf das Icon in der
         * Taskbar gedrückt wird, zuerst der Fokus verloren geht und dadurch
         * sonst auch der Status auf ICONIFIED gesetzt wird und dann
         * der Frame durch den Klick wieder in den Vordergrund gebracht
         * werden würde.
         */
        Timer t = new Timer(200, new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e)
          {
            if (myFrame.getExtendedState() != Frame.ICONIFIED)
            {
              myFrame.setExtendedState(Frame.ICONIFIED);
            }
          }
        });
        t.setRepeats(false);
        t.start();
      } else
      {
        myFrame.setExtendedState(Frame.ICONIFIED);
      }
      return;
    }

    if (windowIsAway) return;
    windowIsAway = true;

    if (windowMode == WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.setJMenuBar(null);
      Container contentPane = myFrame.getContentPane();
      contentPane.remove(contentPanel);
      upAndAwayMinimizedPanel.setPreferredSize(new Dimension(contentPanel.getWidth(), 6));
      contentPane.add(upAndAwayMinimizedPanel);
      myFrame.pack();
      myFrame.setLocation(
        GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint().x
          - (myFrame.getWidth() / 2), 0);
      myFrame.addMouseListener(upAndAwayWindowTransformer);
    }
  }

  /**
   * Je nach windowMode wird die WollMuxBar aus dem Wartezustand wieder in den
   * aktiven Zustand versetzt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void maximize()
  {
    if (windowMode == WollMuxBarConfig.MINIMIZE_TO_TASKBAR_MODE)
    {
      myFrame.setExtendedState(Frame.NORMAL);
      return;
    }

    if (!windowIsAway) return;
    windowIsAway = false;

    if (windowMode == WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.removeMouseListener(upAndAwayWindowTransformer);
      Container contentPane = myFrame.getContentPane();
      contentPane.remove(upAndAwayMinimizedPanel);
      contentPane.add(contentPanel);
      myFrame.setJMenuBar(menuBar);
      setSizeAndLocation();
    }
  }

  /**
   * Führt die gleichnamige ACTION aus.
   *
   * TESTED
   */
  private void openExt(String ext, String url)
  {
    try
    {
      final OpenExt openExt = OpenExt.getInstance(ext, url);

      try
      {
        openExt.storeIfNecessary();
      }
      catch (IOException x)
      {
        Logger.error(x);
        error(L.m("Fehler beim Download der Datei:\n%1", x.getMessage()));
        return;
      }

      Runnable launch = new Runnable()
      {
        @Override
        public void run()
        {
          openExt.launch(new OpenExt.ExceptionHandler()
          {
            @Override
            public void handle(Exception x)
            {
              Logger.error(x);
              error(x.getMessage());
            }
          });
        }
      };

      /**
       * Falls /loadComponentFromURL/ bei den Programmen ist, muss ein Kontakt zu OOo
       * hergestellt werden vor dem Launch.
       */
      boolean mustConnectToOOo = false;
      for (String program : openExt.getPrograms())
        if (program.startsWith("/loadComponentFromURL/")) mustConnectToOOo = true;

      if (mustConnectToOOo)
        eventHandler.handleDoWithConnection(launch);
      else
        launch.run();
    }
    catch (Exception x)
    {
      Logger.error(x);
      error(x.getMessage());
    }
  }

  private void error(String errorMsg)
  {
    JOptionPane.showMessageDialog(null, L.m(
      "%1\nVerständigen Sie Ihre Systemadministration.", errorMsg),
      L.m("Fehlerhafte Konfiguration"), JOptionPane.ERROR_MESSAGE);
  }

  /**
   * Öffnet path als Vorlage.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void load(String path)
  {
    String urlStr = "";
    try
    {
      UNO.init();

      File toOpen = new File(path).getAbsoluteFile();
      URL toOpenUrl = toOpen.toURI().toURL();
      urlStr = UNO.getParsedUNOUrl(toOpenUrl.toExternalForm()).Complete;
      UNO.loadComponentFromURL(urlStr, true, MacroExecMode.USE_CONFIG);
      System.exit(0); // terminate() hier nicht notwendig (noch keine instanz)
    }
    catch (Exception x)
    {
      System.err.println(L.m("Versuch, URL \"%1\" zu öffnen gescheitert!", urlStr));
      x.printStackTrace();
      System.exit(1); // terminate() hier nicht notwendig (noch keine instanz)
    }
  }

  /**
   * Startet die WollMuxBar, wobei die Argumente --load, --fifo und --firstrun
   * unabhängig von den restlichen Argumenten der WollMuxBar zuerst ausgewertet
   * werden.
   *
   * @param args
   *          --fifo, --firstrun, --load, --mm. Außerdem --minimize, --topbar,
   *          --normalwindow um das Anzeigeverhalten festzulegen.
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   */
  public static void main(String[] args)
  {
    // Logger.init(Logger.DEBUG); // Logger wird erst in startWollMuxBar korrekt
    // initialisiert. Daher hier für Debug-Zwecke per Hand initialisieren.

    // Schalter --fifo und --firstrun und --load vorgezogen behandeln:
    List<String> restArgs = new ArrayList<String>();
    boolean firstrun = false;
    String fifoName = null;
    for (int i = 0; i < args.length; ++i)
    {
      String arg = args[i];
      if (arg.equals("--firstrun"))
        firstrun = true;
      else if (arg.equals("--fifo"))
      {
        if (i == args.length - 1)
        {
          System.err.println(L.m("--fifo erwartet genau einen weiteren Parameter!"));
          System.exit(1); // terminate() hier nicht notwendig (noch keine instanz)
        }
        i++; // nächsten Parameter auslesen
        fifoName = args[i];
        if (!"-".equals(fifoName) && !new File(fifoName).canRead())
        {
          System.err.println(L.m("fifo-Datei '%1' kann nicht gelesen werden!",
            fifoName));
          System.exit(1); // terminate() hier nicht notwendig (noch keine instanz)
        }
      }
      else if (arg.equals("--load"))
      {
        if (i == args.length - 1)
        {
          System.err.println(L.m("--load erwartet genau einen weiteren Parameter!"));
          System.exit(1); // terminate() hier nicht notwendig (noch keine instanz)
        }
        i++; // nächsten Paramter auslesen
        String loadArg = args[i];
        if (loadArg.length() == 0) terminate(0);
        load(loadArg);
        // sollte nie erreicht werden, da load() exit() aufruft.
        System.exit(1); // terminate() hier nicht notwendig (noch keine instanz)
      }
      else
      {
        restArgs.add(arg);
      }
    }

    // Ist --fifo gesetzt, wird WollMuxBar über fifo-Modus gestartet, andernfalls
    // direkt.
    if (fifoName != null)
    {
      FifoHandler.handleFifo(fifoName, firstrun, restArgs);
    }
    else
    {
      startWollMuxBar(restArgs);
    }
  }

  /**
   * Startet die WollMuxBar und kann aus mehreren Threads aufgerufen werden, da
   * synchronized.
   *
   * @param args
   *          --minimize, --topbar, --normalwindow um das Anzeigeverhalten
   *          festzulegen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  synchronized public static void startWollMuxBar(List<String> args)
  {
    Logger.debug(L.m("Starte WollMuxBar mit args '%1'.", niceArgs(args)));
    synchronized (startedCounter)
    {
      startedCounter++;
    }

    // Läuft bereits eine WollMuxBar, so wird diese hier zunächst geschlossen:
    if (instance != null)
    {
      try
      {
        SwingUtilities.invokeAndWait(new Runnable()
        {
          @Override
          public void run()
          {
            instance.abort();
          }
        });
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    // neue Instanz starten:
    int windowMode = -1;
    boolean menumanager = false;
    Iterator<String> i = args.iterator();
    while (i.hasNext())
    {
      String arg = i.next();

      if (arg.equals("--minimize"))
        windowMode = WollMuxBarConfig.MINIMIZE_TO_TASKBAR_MODE;
      else if (arg.equals("--topbar"))
        windowMode = WollMuxBarConfig.ALWAYS_ON_TOP_WINDOW_MODE;
      else if (arg.equals("--normalwindow"))
        windowMode = WollMuxBarConfig.NORMAL_WINDOW_MODE;
      // TODO: die Quickstarteroption hat keine Auswirkung mehr
      // und kann zu gegebener Zeit entfernt werden.
      else if (arg.equals("--quickstarter"))
        continue;
      else if (arg.equals("--mm"))
        menumanager = true;
      else
      {
        System.err.println(L.m("Unbekannter Aufrufparameter: %1", arg));
        terminate(1);
      }

    }

    WollMuxFiles.setupWollMuxDir();

    WollMuxClassLoader.initClassLoader();

    ConfigThingy wollmuxConf = WollMuxFiles.getWollmuxConf();

    // Darf der Menümanager (experimentelles Feature) angezeigt werden?
    // Solange wir den Menümanager als experimentelles Feature betrachten, wird
    // allowMenuManager mit false vorbelegt. Das kann sich später ggf. ändern.
    boolean allowMenuManager = false;
    try
    {
        allowMenuManager =
          wollmuxConf.query(ALLOW_MENUMANAGER, 1).getLastChild().toString().equalsIgnoreCase("true");
    }
    catch (NodeNotFoundException e)
    {}
    // --mm schaltet allowMenuManager implizit an
    if(menumanager) allowMenuManager = true;


    // Darf die userConfig (wollmuxbar.conf) ausgewertet werden?
    boolean allowUserConfig = true;
    try
    {
        allowUserConfig =
          wollmuxConf.query(ALLOW_USER_CONFIG, 1).getLastChild().toString().equalsIgnoreCase("true");
    }
    catch (NodeNotFoundException e)
    {}
    // --mm schaltet allowUserConfig implizit an
    if(menumanager) allowUserConfig = true;

    readWollMuxBarConfAndStartWollMuxBar(windowMode, menumanager,
      allowUserConfig, allowMenuManager, wollmuxConf);
  }

  /**
   * Wird die letzte mit startWollMuxBar() gestartete Instanz beendet, so räumt diese
   * Methode zunächst auf und macht dann einen System.exit(status). Diese Methode
   * MUSS an allen Stellen verwendet werden, an denen normalerweise System.exit()
   * verwendet würde (außer der System.exit()-Aufruf wird explizit per Kommentar
   * begründet).
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void terminate(int status)
  {
    synchronized (startedCounter)
    {
      startedCounter--;
      if (startedCounter <= 0)
      {
        Logger.debug(L.m("Beende WollMuxBar mit Status %1.", status));
        FifoHandler.terminate();
        System.exit(status);
      }
    }
  }

  /**
   * Enthält alle Daten und Methoden die für den Fifo-Modus notwendig sind. Im
   * Fifo-Modus gibt es eine WollMuxBar, die als Server-Prozess fungiert, der auf
   * einer fifo-Pipe lauscht und später aufgerufene WollMuxBar-Prozesse, die als
   * Clients auftreten. Beim Aufruf der WollMuxBar werden alle Argumente dieses
   * Aufrufs an den Server-Prozess weitergeleitet, der sie dann an zentraler Stelle
   * abhängig vom bisherigen Kontext (z.B. WollMuxBar ist bereits geöffnet) behandeln
   * kann.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class FifoHandler
  {
    /**
     * Enthält einen in dieser VM statischen Zufallswert um die FifoHandler
     * verschiedener WollMuxBar-Aufrufe in Debugausgaben voneinander unterscheiden zu
     * können.
     */
    private static int randInt = new Random().nextInt();

    /**
     * Enthält den Thread des aktuell laufenden fifo-Listener-Servers (falls
     * vorhanden), oder null.
     */
    private static Thread fifoListenerThread = null;

    /**
     * Enthält das File-Objekt auf die verwendete fifo-Datei.
     */
    private static File fifo = null;

    /**
     * Behandelt den Aufruf der WollMuxBar mit den Argumenten args im Fifo-Modus, bei
     * dem über die Pipe fifoName zwischen den Prozessen kommuniziert wird; Ist
     * firstrun angegeben, so weiß der Handler, dass er nicht erst nach einem
     * entfernten "Server" suchen muss, sondern diese Instanz der Server sein soll
     * und sofort gestartet werden kann.
     *
     * @param fifoName
     *          Filename der Fifo-Pipe
     * @param firstrun
     *          Shortcut zum Überspringen des Tests nach einer entfernten WollMuxBar
     * @param args
     *          Die Argumente, mit der die WollMuxBar gestartet werden soll.
     *
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public static void handleFifo(String fifoName, boolean firstrun,
        final List<String> args)
    {
      fifo = new File(fifoName);
      if (firstrun)
      {
        // Der firstrun-Schalter dient der Beschleunigung, dass callRemoteApplication
        // nicht nutzlos aufgerufen werden muss und damit in einen Timeout laufen
        // würde. Deshalb wird startWollMuxBar direkt aufgerufen.
        startFifoListener(fifo);
        startWollMuxBar(args);
      }
      else if (callRemoteApplication(fifo, args))
      {
        Logger.debug(L.m("%1: Entfernter Aufruf bestätigt.", randInt));
        // startWollMuxBar(args) wird in der entfernten WollMuxBar ausgeführt.
        System.exit(0); // terminate() hier nicht notwendig (noch keine instanz)
      }
      else
      {
        Logger.debug(L.m("%1: Entfernter Aufruf missglückt.", randInt));
        startFifoListener(fifo);
        // startWollMuxBar(args) wird indirekt aufgerufen, da das obige
        // callRemoteApplication() bereits in die Pipe geschrieben hat und
        // startFifoListener(fifo) das erste Element aus der Pipe ausliest und damit
        // die WollMuxBar startet.
      }
    }

    /**
     * Startet einen Listener, der in einem eigenen Server-Thread auf der fifo-Pipe
     * fifo lauscht und übergebene Argumente externer WollMuxBar-Aufrufe bearbeitet.
     *
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private static void startFifoListener(final File fifo)
    {
      Logger.debug(L.m("%1: Starte Fifo-Listener mit Fifo-Pipe '%2'.", randInt, fifo));
      fifoListenerThread = new Thread()
      {
        @Override
        public void run()
        {
          while (true)
          {
            try
            {
              // Die folgende Zeile blockt so lange, bis sich ein Client-Prozess
              // connected.
              ObjectInputStream ois =
                new ObjectInputStream(new FileInputStream(fifo));
              try
              {
                @SuppressWarnings("unchecked")
                final List<String> args = (List<String>) ois.readObject();
                new Thread()
                {
                  @Override
                  public void run()
                  {
                    startWollMuxBar(args);
                  }
                }.start();
              }
              catch (ClassNotFoundException e)
              {
                Logger.error(e);
              }
              ois.close();
            }
            catch (IOException e)
            {
              Logger.debug(L.m("%1: Lesevorgang von FIFO-Pipe '%2' unterbrochen.",
                randInt, fifo));
              Logger.debug(e);
            }
          }
        }
      };
      fifoListenerThread.start();
    }

    /**
     * Versucht args über die fifo-Pipe fifo an einen evtl. gerade laufende
     * Server-Prozess weiter zu geben und liefert true zurück, wenn das geklappt hat,
     * oder false, wenn kein Server gelauscht hat und daher ein Timeout (von derzeit
     * 2s) aktiv werden musste.
     *
     * @param fifo
     *          Die fifo, über die der entfernte Aufruf versucht werden soll.
     * @param args
     *          Die Argumente, mit denen die WollMuxBar über den entfernten
     *          Server-Prozess ausgeführt werden soll.
     * @return true, wenn ein Server-Prozess an die Pipe connected ist und den Aufruf
     *         angenommen hat oder false, wenn innerhalb des Timeouts (2s) kein
     *         Serverprozess verbunden ist.
     *
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private static boolean callRemoteApplication(final File fifo,
        final List<String> args)
    {
      Logger.debug(L.m("%1: Versuche entfernte WollMuxBar aufzurufen mit '%2'.",
        randInt, niceArgs(args)));
      final boolean[] fertig = new boolean[] { false };
      Thread t = new Thread()
      {
        @Override
        public void run()
        {
          try
          {
            // Der folgende Aufruf blockt so lange, bis sich ein Serverprozess
            // connected.
            ObjectOutputStream o =
              new ObjectOutputStream(new FileOutputStream(fifo));
            o.writeObject(args);
            o.close();
            synchronized (fertig)
            {
              fertig[0] = true;
              fertig.notify();
            }
          }
          catch (Exception e)
          {
            Logger.error(e);
          }
        }
      };
      t.start();
      try
      {
        synchronized (fertig)
        {
          fertig.wait(2000);
        }
      }
      catch (InterruptedException e)
      {}
      return fertig[0];
    }

    /**
     * Sollte vor der Beendigung des Prozesses aufgerufen werden und sorgt dafür,
     * dass das fifo-File des Server-Prozesses (falls er hier läuft) beim Beenden
     * gelöscht wird und auch sonst aufgeräumt wird.
     *
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public static void terminate()
    {
      if (fifoListenerThread != null)
      {
        if (fifo != null)
        {
          Logger.debug(L.m("%1: Lösche fifo-File '%2'.", randInt, fifo));
          if (!fifo.delete()) {
            Logger.debug(L.m("fifo.delete fehlgeschlagen!"));
          }
        }
        fifoListenerThread = null;
      }
    }
  }

  /**
   * Liest die wollmuxbar.conf ein und startet die WollMuxBar.
   *
   * @param windowMode
   *          falls >0, overridet dieser windowMode den aus der Konfiguration
   *          gelesenen Wert.
   * @param menumanager
   *          falls true wird automatisch der {@link MenuManager} gestartet.
   * @param allowUserConfig
   *          falls allowUserConfig==false, darf die wollmuxbar.conf nicht
   *          ausgewertet werden und die buttons mit der Aktion "options" und
   *          "menuManager" dürfen nicht in der WollMuxBar erscheinen.
   * @param allowMenuManager
   *          falls allowMenuManager==false, darf der Button mit der Aktion
   *          "menuManager" nicht in der WollMuxBar erscheinen.
   * @param wollmuxConf
   *          die wollmux.conf
   */
  private static void readWollMuxBarConfAndStartWollMuxBar(int windowMode,
      boolean menumanager, boolean allowUserConfig, boolean allowMenuManager, ConfigThingy wollmuxConf)
  {
    ConfigThingy wollmuxbarConf = null;
    File wollmuxbarConfFile =
      new File(WollMuxFiles.getWollMuxDir(), WOLLMUXBAR_CONF);

    if (wollmuxbarConfFile.exists())
    {
      if (allowUserConfig)
      {
        try
        {
          wollmuxbarConf =
            new ConfigThingy("wollmuxbarConf", wollmuxbarConfFile.toURI().toURL());
        }
        catch (Exception x)
        {
          Logger.error(
            L.m("Fehler beim Lesen von '%1'", wollmuxbarConfFile.toString()), x);
        }
      }
      else
      {
        Logger.debug(L.m(
          "Die Verwendung der Konfigurationsdatei '%1' ist deaktiviert. Sie wird nicht ausgewertet!",
          wollmuxbarConfFile.toString()));
      }
    }

    if (wollmuxbarConf == null) wollmuxbarConf = new ConfigThingy("wollmuxbarConf");

    ConfigThingy combinedConf = new ConfigThingy("combinedConf");
    combinedConf.addChild(wollmuxConf);
    combinedConf.addChild(wollmuxbarConf);

    try
    {
      Logger.debug(L.m("WollMuxBar gestartet"));

      if (combinedConf.query("Symbolleisten").count() == 0)
      {
        Logger.error(WOLLMUX_CONFIG_ERROR_MESSAGE);
        JOptionPane.showMessageDialog(null, WOLLMUX_CONFIG_ERROR_MESSAGE,
          L.m("Fehlerhafte Konfiguration"), JOptionPane.ERROR_MESSAGE);
      }
      else
      {
        instance =
          new WollMuxBar(windowMode, combinedConf, wollmuxConf, wollmuxbarConf,
            allowUserConfig, allowMenuManager);
      }

      if (menumanager)
      {
        if (instance != null)
          instance.menuManager();
        else
          new MenuManager(wollmuxConf, wollmuxbarConf, null);
      }

    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Konkatiniert args in eine Stringzeile für Debugausgaben.
   */
  private static String niceArgs(List<String> args)
  {
    StringBuffer buffy = new StringBuffer();
    for (String s : args)
      if (buffy.length() == 0)
        buffy.append(s);
      else
        buffy.append(" " + s);
    return buffy.toString();
  }
}
