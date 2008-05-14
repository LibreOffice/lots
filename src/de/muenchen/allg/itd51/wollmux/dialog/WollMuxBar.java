/*
 * Dateiname: WollMuxBar.java
 * Projekt  : WollMux
 * Funktion : Menü-Leiste als zentraler Ausgangspunkt für WollMux-Funktionen
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import com.sun.star.document.MacroExecMode;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.DispatchHandler;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Menü-Leiste als zentraler Ausgangspunkt für WollMux-Funktionen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxBar
{
  /**
   * Titel des WollMuxBar-Fensters (falls nicht anders konfiguriert).
   */
  private static final String DEFAULT_TITLE = L.m("Vorlagen und Formulare");

  /**
   * Spezialeintrag in der Absenderliste, der genau dann vorhanden ist, wenn die
   * Absenderliste leer ist.
   */
  private static final String LEERE_LISTE = L.m("<kein Absender vorhanden>");

  /**
   * Präfix für Verzeichnisnamen zum Herunterladen von URLs für ACTION "openExt".
   */
  private static final String WOLLMUX_DOWNLOAD_DIR_PREFIX =
    "wollmuxbar-temp-download-";

  /**
   * Wenn die WollMuxBar den Fokus verliert, minimiert sich das Fenster.
   */
  private static final int MINIMIZE_TO_TASKBAR_MODE = 1;

  /**
   * Die WollMuxBar verhält sich wie ein normales Fenster.
   */
  private static final int NORMAL_WINDOW_MODE = 2;

  /**
   * Die WollMuxBar ist immer im Vordergrund.
   */
  private static final int ALWAYS_ON_TOP_WINDOW_MODE = 3;

  /**
   * Die WollMuxBar verschwindet am oberen Rand, wenn der Mauscursor sie verlässt.
   */
  private static final int UP_AND_AWAY_WINDOW_MODE = 4;

  /**
   * TODO Die WollMuxBar ist vertikal und verschwindet am linken Rand, wenn der
   * Mauscursor sie verlässt.
   */
  // private static final int LEFT_AND_AWAY_WINDOW_MODE = 5;
  /**
   * Der Anzeigemodus für die WollMuxBar (z,B, {@link #UP_AND_AWAY_WINDOW_MODE}).
   */
  private int windowMode;

  /**
   * Dient der thread-safen Kommunikation mit dem entfernten WollMux.
   */
  private WollMuxBarEventHandler eventHandler;

  /**
   * Der Rahmen, der die Steuerelemente enthält.
   */
  private JFrame myFrame;

  /**
   * Falls > 0, so ist dies eine von wollmux,conf fest vorgegebene Breite. Falls 0,
   * so wird die natürliche Breite verwendet. Falls -1, so wird die maximale Breite
   * verwendet.
   */
  private int myFrame_width;

  /**
   * Falls > 0, so ist dies eine von wollmux,conf fest vorgegebene Höhe. Falls 0, so
   * wird die natürliche Höhe verwendet. Falls -1, so wird die maximale Höhe
   * verwendet.
   */
  private int myFrame_height;

  /**
   * Falls >= 0, so ist dies eine von wollmux,conf fest vorgegebene x-Koordinate.
   * Diese wird nur einmal gesetzt. Danach kann der Benutzer das Fenster verschieben,
   * wenn er möchte. Falls -1, so wird das Fenster zentriert. Falls -2, so wird die
   * größte sinnvolle Koordinate verwendet. Falls -3, so wird die kleinste sinnvolle
   * Koordinate verwendet. Falls Integer.MIN_VALUE, so ist keine Koordinate fest
   * vorgegeben.
   */
  private int myFrame_x;

  /**
   * Falls >= 0, so ist dies eine von wollmux,conf fest vorgegebene y-Koordinate.
   * Diese wird nur einmal gesetzt. Danach kann der Benutzer das Fenster verschieben,
   * wenn er möchte. Falls -1, so wird das Fenster zentriert. Falls -2, so wird die
   * größte sinnvolle Koordinate verwendet. Falls -3, so wird die kleinste sinnvolle
   * Koordinate verwendet. Falls Integer.MIN_VALUE, so ist keine Koordinate fest
   * vorgegeben.
   */
  private int myFrame_y;

  /**
   * Das Panel für den Inhalt des Fensters der WollMuxBar (myFrame).
   */
  private JPanel contentPanel;

  /**
   * Mappt einen Menü-Namen auf ein entsprechendes JPopupMenu.
   */
  private Map<String, JComponent> mapMenuNameToJPopupMenu =
    new HashMap<String, JComponent>();

  /**
   * Mappt einen EXT Attributwert auf die zugehörige
   * {@link WollMuxBar.ExternalApplication}.
   */
  private Map<String, ExternalApplication> mapExtToExternalApplication =
    new HashMap<String, ExternalApplication>();

  /**
   * Die UIElementFactory, die verwendet wird, um das GUI aufzubauen.
   */
  private UIElementFactory uiElementFactory;

  /**
   * Kontext für GUI-Elemente in JPanels (für Übergabe an die uiElementFactory).
   */
  private UIElementFactory.Context panelContext;

  /**
   * Kontext für GUI-Elemente in JMenus und JPopupMenus (für Übergabe an die
   * uiElementFactory).
   */
  private UIElementFactory.Context menuContext;

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
   * Verbindung zum WollMux in OOo hergestellt werden konnte.
   */
  private static final String CONNECTION_FAILED_MESSAGE =
    L.m("Es konnte keine Verbindung zur WollMux-Komponente von OpenOffice hergestellt werden.\n"
      + "Eine mögliche Ursache ist ein fehlerhaft installiertes OpenOffice.\n"
      + "Eine weitere mögliche Ursache ist, dass WollMux.uno.pkg nicht oder fehlerhaft installiert wurde.");

  private static final String WOLLMUX_CONFIG_ERROR_MESSAGE =
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
   * Aufgerufen wenn der Spezialeintrag "Liste Bearbeiten" in der Senderbox gewählt
   * wird.
   */
  private ActionListener actionListener_editSenderList = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmPALVerwalten, null);
      minimize();
    }
  };

  /**
   * ActionListener wenn anderer Absender in Senderbox ausgewählt.
   */
  private ActionListener senderboxActionListener = new ActionListener()
  {
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
   * Die breite der minimierten WollMux-Leiste im UP_AND_AWAY_WINDOW_MODE.
   */
  private int minimizedWidth = 300;

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
   * true zeigt an, dass die Leiste minimiert ist.
   */
  private boolean isMinimized = false;

  /**
   * Erzeugt eine neue WollMuxBar.
   * 
   * @param winMode
   *            Anzeigemodus, z.B. {@link #UP_AND_AWAY_WINDOW_MODE}.
   * @param conf
   *            der Inhalt der wollmux.conf
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public WollMuxBar(int winMode, final ConfigThingy conf)
  {
    windowMode = winMode;

    parseExternalApplications(conf.query("ExterneAnwendungen"));

    eventHandler = new WollMuxBarEventHandler(this);

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

    String title = DEFAULT_TITLE;
    ConfigThingy wmBarConf = new ConfigThingy("");
    try
    {
      wmBarConf = conf.query("Fenster").query("WollMuxBar").getLastChild();
    }
    catch (Exception x)
    {}
    try
    {
      title = wmBarConf.get("TITLE").toString();
    }
    catch (Exception x)
    {}

    myFrame_x = Integer.MIN_VALUE;
    try
    {
      String xStr = wmBarConf.get("X").toString();
      if (xStr.equalsIgnoreCase("center"))
        myFrame_x = -1;
      else if (xStr.equalsIgnoreCase("max"))
        myFrame_x = -2;
      else if (xStr.equalsIgnoreCase("min"))
        myFrame_x = -3;
      else
      {
        myFrame_x = Integer.parseInt(xStr);
        // Ja, das folgende ist eine Einschränkung, aber
        // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
        // obigen Festlegungen
        if (myFrame_x < 0) myFrame_x = 0;
      }
    }
    catch (Exception x)
    {}

    myFrame_y = Integer.MIN_VALUE;
    try
    {
      String yStr = wmBarConf.get("Y").toString();
      if (yStr.equalsIgnoreCase("center"))
        myFrame_y = -1;
      else if (yStr.equalsIgnoreCase("max"))
        myFrame_y = -2;
      else if (yStr.equalsIgnoreCase("min"))
        myFrame_y = -3;
      else
      {
        myFrame_y = Integer.parseInt(yStr);
        // Ja, das folgende ist eine Einschränkung, aber
        // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
        // obigen Festlegungen
        if (myFrame_y < 0) myFrame_y = 0;
      }
    }
    catch (Exception x)
    {}

    myFrame_width = 0;
    try
    {
      String widthStr = wmBarConf.get("WIDTH").toString();
      if (widthStr.equalsIgnoreCase("max"))
        myFrame_width = -1;
      else
      {
        myFrame_width = Integer.parseInt(widthStr);
        if (myFrame_width < 0) myFrame_width = 0;
      }
    }
    catch (Exception x)
    {}

    myFrame_height = 0;
    try
    {
      String heightStr = wmBarConf.get("HEIGHT").toString();
      if (heightStr.equalsIgnoreCase("max"))
        myFrame_height = -1;
      else
      {
        myFrame_height = Integer.parseInt(heightStr);
        if (myFrame_height < 0) myFrame_height = 0;
      }
    }
    catch (Exception x)
    {}

    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
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

    menuBar = new JMenuBar();
    menuBar.addMouseListener(myIsInsideMonitor);
    try
    {
      ConfigThingy menubar = conf.query("Menueleiste");
      if (menubar.count() > 0)
      {
        addUIElements(conf.query("Menues"), menubar.getLastChild(), menuBar, 1, 0,
          "menu");
      }
    }
    catch (NodeNotFoundException x)
    {
      Logger.error(x);
    }
    myFrame.setJMenuBar(menuBar);

    setupMinimizedFrame(title, wmBarConf);

    if (windowMode != NORMAL_WINDOW_MODE) myFrame.setAlwaysOnTop(true);

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
    if (isMinimized) return;
    // Toolkit tk = Toolkit.getDefaultToolkit();
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    // Dimension screenSize = tk.getScreenSize();
    Rectangle bounds = genv.getMaximumWindowBounds();

    myFrame.pack();
    Dimension naturalFrameSize = myFrame.getSize();
    Dimension frameSize = new Dimension(naturalFrameSize);
    Point frameLocation = myFrame.getLocation();

    switch (myFrame_width)
    {
      case 0: // natural width
        break;
      case -1: // max
        frameSize.width = bounds.width;
        break;
      default: // specified width
        frameSize.width = myFrame_width;
        break;
    }

    switch (myFrame_height)
    {
      case 0: // natural height
        break;
      case -1: // max
        frameSize.height = bounds.height;
        break;
      default: // specified height
        frameSize.height = myFrame_height;
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

    minimizedWidth = frameSize.width;
    if (minimizedWidth > 128) minimizedWidth -= 64;

  }

  /**
   * Erzeugt den JFrame für die minimierte Darstellung (WollMux-Logo oder schmaler
   * Streifen).
   * 
   * @param title
   *            der Titel für das Fenster (nur für Anzeige in Taskleiste)
   * @param wmBarConf
   *            ConfigThingy des Fenster/WollMuxBar-Abschnitts.
   * @param upAndAwayWidth
   *            breite des Streifens für Modus "UpAndAway"
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setupMinimizedFrame(String title, ConfigThingy wmBarConf)
  {
    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
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
   *            die Kinder dieses ConfigThingys müssen "Menues"-Knoten sein, deren
   *            Kinder Menübeschreibungen sind für die Menüs, die als UI Elemente
   *            verwendet werden.
   * @param elementParent
   *            das Element, dessen Kinder die UI Elemente beschreiben.
   * @param context
   *            kann die Werte "menu" oder "panel" haben und gibt an, um was es sich
   *            bei compo handelt. Abhängig vom context werden manche UI Elemente
   *            anders interpretiert, z.B. werden "button" Elemente im context "menu"
   *            zu JMenuItems.
   * @param compo
   *            die Komponente zu der die UI Elemente hinzugefügt werden sollen.
   *            Falls context nicht "menu" ist, muss compo ein GridBagLayout haben.
   * @param stepx
   *            stepx und stepy geben an, um wieviel mit jedem UI Element die x und
   *            die y Koordinate innerhalb des GridBagLayouts erhöht werden sollen.
   *            Sinnvoll sind hier normalerweise nur (0,1) und (1,0).
   * @param stepy
   *            siehe stepx
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addUIElements(ConfigThingy menuConf, ConfigThingy elementParent,
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

    UIElementFactory.Context contextMap =
      context.equals("menu") ? menuContext : panelContext;

    Iterator piter = elementParent.iterator();
    while (piter.hasNext())
    {
      ConfigThingy uiElementDesc = (ConfigThingy) piter.next();
      y += stepy;
      x += stepx;

      try
      {
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

          gbcMenuButton.gridx = x;
          gbcMenuButton.gridy = y;
          button.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
            compo.add(button);
          else
            compo.add(button, gbcSenderbox);
        }
        else if (type.equals("menu"))
        {
          String label = L.m("LABEL FEHLT ODER FEHLERHAFT!");
          try
          {
            label = uiElementDesc.get("LABEL").toString();
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
          UIElement uiElement =
            uiElementFactory.createUIElement(contextMap, uiElementDesc);
          GridBagConstraints gbc =
            (GridBagConstraints) uiElement.getLayoutConstraints();
          gbc.gridx = x;
          gbc.gridy = y;
          Component uiComponent = uiElement.getComponent();
          uiComponent.addMouseListener(myIsInsideMonitor);
          if (context.equals("menu"))
            compo.add(uiComponent);
          else
            compo.add(uiComponent, gbc);
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
   *            das JMenu oder JPopupMenu zu dem die UI Elemente hinzugefügt werden
   *            sollen.
   * @param menuConf
   *            die Kinder dieses ConfigThingys müssen "Menues"-Knoten sein, deren
   *            Kinder Menübeschreibungen sind.
   * @param menuName
   *            identifiziert das Menü aus menuConf, das geparst wird. Gibt es
   *            mehrere, so wird das letzte verwendet.
   * @param mapMenuNameToMenu
   *            falls nicht-null, so wird falls bereits ein Eintrag menuName
   *            enthalten ist, dieser zurückgeliefert, ansonsten wird ein Mapping von
   *            menuName auf menu hinzugefügt. Falls null, so wird immer ein neues
   *            Menü erzeugt, außer das menuName ist in alreadySeen, dann gibt es
   *            eine Fehlermeldung.
   * @param alreadySeen
   *            falls menuName hier enthalten ist und mapMenuNameToMenu==null dann
   *            wird eine Fehlermeldung ausgegeben und null zurückgeliefert.
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

    panelContext = new UIElementFactory.Context();
    panelContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    panelContext.mapTypeToLabelType = mapTypeToLabelType;
    panelContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    panelContext.uiElementEventHandler = myUIElementEventHandler;
    panelContext.mapTypeToType = new HashMap<String, String>();
    panelContext.mapTypeToType.put("separator", "v-separator");
    panelContext.mapTypeToType.put("glue", "h-glue");

    menuContext = new UIElementFactory.Context();
    menuContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    menuContext.mapTypeToLabelType = mapTypeToLabelType;
    menuContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    menuContext.uiElementEventHandler = myUIElementEventHandler;
    menuContext.mapTypeToType = new HashMap<String, String>();
    menuContext.mapTypeToType.put("separator", "h-separator");
    menuContext.mapTypeToType.put("glue", "v-glue");
    menuContext.mapTypeToType.put("button", "menuitem");

    Set<String> supportedActions = new HashSet<String>();
    supportedActions.add("openTemplate");
    supportedActions.add("absenderAuswaehlen");
    supportedActions.add("openDocument");
    supportedActions.add("openExt");
    supportedActions.add("open");
    supportedActions.add("dumpInfo");
    supportedActions.add("abort");
    supportedActions.add("kill");
    supportedActions.add("about");

    panelContext.supportedActions = supportedActions;
    menuContext.supportedActions = supportedActions;

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
    public void processUiElementEvent(UIElement source, String eventType,
        Object[] args)
    {
      if (!eventType.equals("action")) return;

      String action = args[0].toString();
      if (action.equals("absenderAuswaehlen"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmAbsenderAuswaehlen, "");
      }
      else if (action.equals("openDocument"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmOpenDocument,
          args[1].toString());
      }
      else if (action.equals("openTemplate"))
      {
        minimize();
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmOpenTemplate,
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
        OpenExt openExt =
          new OpenExt((String) args[1], mapExtToExternalApplication.get(args[1]),
            (String) args[2]);
        openExt.setDaemon(false);
        openExt.start();
      }
      else if (action.equals("dumpInfo"))
      {
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmDumpInfo, null);
      }
      else if (action.equals("abort"))
      {
        abort();
      }
      else if (action.equals("kill"))
      {
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmKill, null);
        abort();
      }
      else if (action.equals("about"))
      {
        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmAbout, getBuildInfo());
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
    final JFrame multiOpenFrame = new JFrame(L.m("Was möchten Sie öffnen ?"));
    multiOpenFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    Box vbox = Box.createVerticalBox();
    multiOpenFrame.getContentPane().add(vbox);
    vbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    Box hbox;
    /*
     * hbox = Box.createHorizontalBox(); hbox.add(new JLabel(L.m("Was möchten Sie
     * öffnen ?"))); hbox.add(Box.createHorizontalGlue()); vbox.add(hbox);
     * vbox.add(Box.createVerticalStrut(5));
     */
    final ConfigThingy openConf = new ConfigThingy(conf); // Kopie machen, die
    // manipuliert werden darf.
    Iterator iter;
    try
    {
      iter = conf.get("Labels").iterator();
    }
    catch (NodeNotFoundException e2)
    {
      Logger.error(L.m("ACTION \"open\" erfordert Abschnitt \"Labels\" in den OPEN-Angaben"));
      return;
    }
    final List<JCheckBox> checkBoxes = new Vector<JCheckBox>();
    while (iter.hasNext())
    {
      hbox = Box.createHorizontalBox();
      String label = iter.next().toString();
      JCheckBox checkbox = new JCheckBox(label, true);
      checkBoxes.add(checkbox);
      hbox.add(checkbox);
      hbox.add(Box.createHorizontalGlue());
      vbox.add(hbox);
      vbox.add(Box.createVerticalStrut(5));
    }

    hbox = Box.createHorizontalBox();
    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        multiOpenFrame.dispose();
      }
    });
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Alle"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Iterator<JCheckBox> iter = checkBoxes.iterator();
        while (iter.hasNext())
          iter.next().setSelected(true);
      }
    });
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(5));

    button = new JButton(L.m("Keine"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Iterator<JCheckBox> iter = checkBoxes.iterator();
        while (iter.hasNext())
          iter.next().setSelected(false);
      }
    });
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Öffnen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        multiOpenFrame.dispose();
        Iterator<JCheckBox> iter = checkBoxes.iterator();
        ConfigThingy fragConf;
        try
        {
          fragConf = openConf.get("Fragmente", 1);
        }
        catch (NodeNotFoundException e1)
        {
          Logger.error(L.m("Abschnitt \"Fragmente\" fehlt in OPEN-Angabe"));
          return;
        }
        Iterator fragIter = fragConf.iterator();
        while (iter.hasNext() && fragIter.hasNext())
        {
          fragIter.next();
          JCheckBox checkbox = iter.next();
          if (!checkbox.isSelected()) fragIter.remove();
        }

        eventHandler.handleWollMuxUrl(DispatchHandler.DISP_wmOpen,
          openConf.stringRepresentation(true, '"', false));
      }
    });
    hbox.add(button);

    vbox.add(hbox);

    multiOpenFrame.setAlwaysOnTop(true);
    multiOpenFrame.pack();
    int frameWidth = multiOpenFrame.getWidth();
    int frameHeight = multiOpenFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    multiOpenFrame.setLocation(x, y);
    multiOpenFrame.setVisible(true);
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    eventHandler.handleTerminate();
    myFrame.dispose();
    eventHandler.waitForThreadTermination();

    System.exit(0);
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
  public String getBuildInfo()
  {
    try
    {
      URL url = WollMuxBar.class.getClassLoader().getResource("buildinfo");
      if (url != null)
      {
        BufferedReader in =
          new BufferedReader(new InputStreamReader(url.openStream()));
        return in.readLine().toString();
      }
    }
    catch (java.lang.Exception x)
    {}
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
   *            die Einträge, die die Senderboxen enthalten sollen.
   * @param current
   *            der ausgewählte Eintrag
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
          senderbox.addItem(entries[i], senderboxActionListener, "" + i + ":"
            + entries[i], myIsInsideMonitor);
        }
      }
      else
        senderbox.addItem(LEERE_LISTE, null, null, myIsInsideMonitor);

      senderbox.addSeparator();
      senderbox.addItem(L.m("Liste Bearbeiten"), actionListener_editSenderList,
        null, myIsInsideMonitor);

      if (current != null && !current.equals(""))
        senderbox.setSelectedItem(current);
    }

    setSizeAndLocation();
  }

  private static abstract class Senderbox
  {
    protected JComponent menu;

    public void removeAllItems()
    {
      menu.removeAll();
    }

    public void addItem(String item, ActionListener listen, String actionCommand,
        MouseListener mouseListen)
    {
      JMenuItem menuItem = new JMenuItem(item);
      menuItem.addActionListener(listen);
      menuItem.setActionCommand(actionCommand);
      menuItem.addMouseListener(mouseListen);
      menu.add(menuItem);
    }

    public void addSeparator()
    {
      menu.add(new JSeparator());
    }

    public abstract void setSelectedItem(String item);

    public static Senderbox create(JMenu menu)
    {
      return new JMenuSenderbox(menu);
    }

    public static Senderbox create(JPopupMenu menu, AbstractButton button)
    {
      return new JPopupMenuSenderbox(menu, button);
    }

    private static class JMenuSenderbox extends Senderbox
    {

      public JMenuSenderbox(JMenu menu)
      {
        this.menu = menu;
      }

      public void setSelectedItem(String item)
      {
        ((JMenu) menu).setText(item);
      }
    }

    private static class JPopupMenuSenderbox extends Senderbox
    {
      private AbstractButton button;

      public JPopupMenuSenderbox(JPopupMenu menu, AbstractButton button)
      {
        this.menu = menu;
        this.button = button;
      }

      public void setSelectedItem(String item)
      {
        button.setText(item);
      }
    }
  }

  /**
   * Erzeugt ein Popup-Fenster, das den Benutzer darüber informiert, dass keine
   * Verbindung zur WollMux-Komponente in OpenOffice hergestellt werden konnte.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void connectionFailedWarning()
  {
    JOptionPane.showMessageDialog(null, CONNECTION_FAILED_MESSAGE,
      L.m("WollMux-Fehler"), JOptionPane.ERROR_MESSAGE);
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

    public void windowActivated(WindowEvent e)
    {}

    public void windowClosed(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
    }

    public void windowDeactivated(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

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
    public void windowGainedFocus(WindowEvent e)
    {}

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

    public void mouseClicked(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {}

    public void mouseReleased(MouseEvent e)
    {}

    public void mouseEntered(MouseEvent e)
    {
      timer.restart();
    }

    public void mouseExited(MouseEvent e)
    {
      timer.stop();
    }

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

    public void mouseClicked(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {}

    public void mouseReleased(MouseEvent e)
    {}

    public void mouseEntered(MouseEvent e)
    {
      if (windowMode != UP_AND_AWAY_WINDOW_MODE) return;
      timer.stop();
    }

    public void mouseExited(MouseEvent e)
    {
      delayedMinimize();
    }

    public void delayedMinimize()
    {
      if (windowMode != UP_AND_AWAY_WINDOW_MODE) return;
      timer.restart();
    }

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
    if (windowMode == ALWAYS_ON_TOP_WINDOW_MODE || windowMode == NORMAL_WINDOW_MODE)
      return;
    if (windowMode == MINIMIZE_TO_TASKBAR_MODE)
    {
      myFrame.setExtendedState(Frame.ICONIFIED);
      return;
    }

    if (isMinimized) return;
    isMinimized = true;

    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.setJMenuBar(null);
      Container contentPane = myFrame.getContentPane();
      contentPane.remove(contentPanel);
      contentPane.add(upAndAwayMinimizedPanel);
      myFrame.setSize(minimizedWidth, 5);
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
    if (windowMode == MINIMIZE_TO_TASKBAR_MODE)
    {
      myFrame.setExtendedState(Frame.NORMAL);
      return;
    }

    if (!isMinimized) return;
    isMinimized = false;

    if (windowMode == UP_AND_AWAY_WINDOW_MODE)
    {
      myFrame.removeMouseListener(upAndAwayWindowTransformer);
      Container contentPane = myFrame.getContentPane();
      contentPane.remove(upAndAwayMinimizedPanel);
      contentPane.add(contentPanel);
      myFrame.setJMenuBar(menuBar);
      setSizeAndLocation();
    }
  }

  private static class ExternalApplication
  {
    public boolean downloadUrl = false;

    public List<String> commands = new Vector<String>();
  }

  /**
   * Parst ExterneAnwendungen-Abschnitte und initialisiert
   * {@link #mapExtToExternalApplication}.
   * 
   * @param conf
   *            Knoten, dessen Kinder "ExterneAnwendungen" Knoten sein müssen.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void parseExternalApplications(ConfigThingy conf)
  {
    Iterator parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      ConfigThingy parentConf = (ConfigThingy) parentIter.next();
      Iterator iter = parentConf.iterator();
      while (iter.hasNext())
      {
        ConfigThingy appConf = (ConfigThingy) iter.next();
        ExternalApplication app = new ExternalApplication();
        ConfigThingy extConf;
        try
        {
          extConf = appConf.get("EXT");
          extConf.getFirstChild(); // Testen, ob mindestens ein Kind vorhanden ist,
          // ansonsten Exception
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(L.m("Ein Eintrag im Abschnitt \"ExterneAnwendungen\" enthält keine gültige EXT-Angabe."));
          continue;
        }

        try
        {
          app.downloadUrl =
            appConf.get("DOWNLOAD").toString().equalsIgnoreCase("true");
        }
        catch (Exception x)
        {}

        try
        {
          ConfigThingy programConf = appConf.get("PROGRAM");
          programConf.getFirstChild(); // Testen, ob mindestens ein Kind vorhanden
          // ist, ansonsten Exception
          Iterator progiter = programConf.iterator();
          while (progiter.hasNext())
          {
            String prog = progiter.next().toString();
            app.commands.add(prog);
          }
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(L.m("Ein Eintrag im Abschnitt \"ExterneAnwendungen\" enthält keine gültige PROGRAM-Angabe."));
          continue;
        }

        Iterator extIter = extConf.iterator();
        while (extIter.hasNext())
        {
          mapExtToExternalApplication.put(extIter.next().toString(), app);
        }
      }
    }
  }

  /**
   * Ruft eine {@link WollMuxBar.ExternalApplication} auf, nachdem falls nötig eine
   * URL in eine temporäre Datei heruntergeladen wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class OpenExt extends Thread
  {
    private String ext;

    private ExternalApplication app;

    private String url;

    /**
     * Erzeugt ein neues OpenExt Objekt.
     * 
     * @param ext
     *            ID-String für die Anwendung, normalerweise die Dateierweiterung.
     *            Falls aus der url kein Dateiname abgeleitet werden konnte wird
     *            dieser String an einen generierten Dateinamen angehängt.
     * @param app
     *            die zu startende {@link WollMuxBar.ExternalApplication}. Falls
     *            null, so wird eine Fehlermeldung geloggt und nichts weiter getan.
     * @param url
     *            die URL die der Anwendung als Argument übergeben werden soll (bzw.
     *            die heruntergeladen und als temporäre Datei an die Anwendung
     *            übergeben werden soll.)
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    public OpenExt(String ext, ExternalApplication app, String url)
    {
      this.ext = ext;
      this.app = app;
      this.url = url;
    }

    public void run()
    { // TESTED
      try
      {
        if (app == null)
        {
          error(L.m("Für die Erweiterung \"%1\" wurde keine Anwendung definiert.",
            ext));
          return;
        }

        URL srcUrl = WollMuxFiles.makeURL(url);
        String appArgument = srcUrl.toExternalForm();

        if (app.downloadUrl)
        {
          File tmpDir = new File(System.getProperty("java.io.tmpdir"));
          if (!tmpDir.isDirectory() && !tmpDir.canWrite())
          {
            error(L.m(
              "Temporäres Verzeichnis\n\"%1\"\nexistiert nicht oder kann nicht beschrieben werden!",
              tmpDir.getPath()));
            return;
          }

          File downloadDir = null;
          for (int i = 0; i < 1000; ++i)
          {
            downloadDir = new File(tmpDir, WOLLMUX_DOWNLOAD_DIR_PREFIX + i);
            if (downloadDir.mkdir())
              break;
            else
              downloadDir = null;
          }

          if (downloadDir == null)
          {
            error(L.m("Konnte kein temporäres Verzeichnis für den Download der Datei anlegen!"));
            return;
          }

          String srcFile = srcUrl.getPath();
          int idx1 = srcFile.lastIndexOf('/');
          int idx2 = srcFile.lastIndexOf('\\');
          if (idx2 > idx1) idx1 = idx2;
          if (idx1 >= 0) srcFile = srcFile.substring(idx1 + 1);

          if (srcFile.length() == 0) srcFile = "foo" + ext;

          File destFile = new File(downloadDir, srcFile);
          appArgument = destFile.getAbsolutePath();

          try
          {
            InputStream istream = srcUrl.openStream();
            if (!destFile.createNewFile())
              throw new IOException(L.m(
                "Konnte temporäre Datei \"%1\" nicht anlegen", destFile.getPath()));
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buffy = new byte[4096];
            int len;
            while (0 <= (len = istream.read(buffy)))
              out.write(buffy, 0, len);
            out.close();
            istream.close();
          }
          catch (IOException x)
          {
            Logger.error(x);
            JOptionPane.showMessageDialog(
              null,
              L.m(
                "Fehler beim Download der Datei:\n%1\nVerständigen Sie Ihre Systemadministration.",
                x.getMessage()), L.m("Fehlerhafte Konfiguration"),
              JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        Iterator<String> iter = app.commands.iterator();
        while (iter.hasNext())
        {
          String command = iter.next();
          ProcessBuilder proc = new ProcessBuilder(new String[] {
            command, appArgument });
          proc.redirectErrorStream(true);
          try
          {
            Process process = proc.start();
            /*
             * Wenn der gestartete Prozess Ein- oder Ausgabe tätigt, so wird er
             * blocken, wenn an der anderen Seite nichts hängt das schreibt oder
             * liest. Am liebsten würden wir natürlich nach /dev/null umleiten, aber
             * das kann Java nicht (vor allem nicht portabel). Für Stdin ist die
             * Lösung einfach. Man schließt den Strom. Damit muss jedes Programm
             * zurecht kommen. Für Stdout/Stderr (oben über redirectErrorStream
             * zusammengelegt) kann man das zwar auch machen (und das tut der unten
             * stehende Code auch), aber das ist etwas böse, weil Programme zumindest
             * unter Unix für gewöhnlich nicht dafür ausgelegt sind, kein
             * Stdout+Stderr zu haben. Falls ein Programm damit Probleme hat, kann
             * ein einfaches Shell-Skript als Wrapper verwendet werden, das die
             * Umleitung nach /dev/null erledigt. Eine alternative Lösung wäre der
             * unten auskommentierte Code, der einfach Stdout+Stderr ausliest.
             * Unschön an dieser Lösung ist, dass der Java-Thread weiterläuft solange
             * wie das externe Programm läuft.
             */
            process.getOutputStream().close(); // Prozess daran hindern zu blocken
            // durch Eingabe
            process.getInputStream().close(); // böse
            process.getErrorStream().close(); // böse
            /*
             * InputStream istream = process.getInputStream(); byte[] buffy = new
             * byte[256]; while (( 0 <= istream.read(buffy)));
             */
            return;
          }
          catch (Exception x)
          {}
        }

        error(L.m(
          "Keines der für die Erweiterung \"%1\"konfigurierten Programme konnte gestartet werden!",
          ext));
        return;

      }
      catch (Exception x)
      {
        Logger.error(x);
        JOptionPane.showMessageDialog(null, L.m(
          "%1\nVerständigen Sie Ihre Systemadministration.", x.getMessage()),
          L.m("Fehlerhafte Konfiguration"), JOptionPane.ERROR_MESSAGE);
        return;
      }
    }

    private void error(String errorMsg)
    {
      Logger.error(errorMsg);
      JOptionPane.showMessageDialog(null, L.m(
        "%1\nVerständigen Sie Ihre Systemadministration.", errorMsg),
        L.m("Fehlerhafte Konfiguration"), JOptionPane.ERROR_MESSAGE);
    }
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
      System.exit(0);
    }
    catch (Exception x)
    {
      System.err.println(L.m("Versuch, URL \"%1\" zu öffnen gescheitert!", urlStr));
      x.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Startet die WollMuxBar.
   * 
   * @param args
   *            --minimize, --topbar, --normalwindow um das Anzeigeverhalten
   *            festzulegen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args)
  {
    int windowMode = UP_AND_AWAY_WINDOW_MODE;
    if (args.length > 0)
    {
      if (args[0].equals("--minimize"))
        windowMode = MINIMIZE_TO_TASKBAR_MODE;
      else if (args[0].equals("--topbar"))
        windowMode = ALWAYS_ON_TOP_WINDOW_MODE;
      else if (args[0].equals("--normalwindow"))
        windowMode = NORMAL_WINDOW_MODE;
      else if (args[0].equals("--load"))
      {
        if (args.length < 2 || args[1].length() == 0) System.exit(0);
        load(args[1]);
      }
      else
      {
        System.err.println(L.m("Unbekannter Aufrufparameter: %1", args[0]));
        System.exit(1);
      }

      if (args.length > 1)
      {
        System.err.println(L.m("Zu viele Aufrufparameter!"));
        System.exit(1);
      }
    }

    WollMuxFiles.setupWollMuxDir();

    ConfigThingy wollmuxConf = WollMuxFiles.getWollmuxConf();

    try
    {
      Logger.debug(L.m("WollMuxBar gestartet"));

      try
      {
        String windowMode2 =
          wollmuxConf.query("Fenster").query("WollMuxBar").getLastChild().query(
            "MODE").getLastChild().toString();
        if (windowMode2.equalsIgnoreCase("AlwaysOnTop"))
          windowMode = ALWAYS_ON_TOP_WINDOW_MODE;
        else if (windowMode2.equalsIgnoreCase("Window"))
          windowMode = NORMAL_WINDOW_MODE;
        else if (windowMode2.equalsIgnoreCase("Minimize"))
          windowMode = MINIMIZE_TO_TASKBAR_MODE;
        else if (windowMode2.equalsIgnoreCase("UpAndAway"))
          windowMode = UP_AND_AWAY_WINDOW_MODE;
        else
          Logger.error(L.m("Ununterstützer MODE für WollMuxBar-Fenster: '%1'",
            windowMode2));
      }
      catch (Exception x)
      {}

      if (wollmuxConf.query("Symbolleisten").count() == 0)
      {
        Logger.error(WOLLMUX_CONFIG_ERROR_MESSAGE);
        JOptionPane.showMessageDialog(null, WOLLMUX_CONFIG_ERROR_MESSAGE,
          L.m("Fehlerhafte Konfiguration"), JOptionPane.ERROR_MESSAGE);
      }
      else
        new WollMuxBar(windowMode, wollmuxConf);

    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

}
