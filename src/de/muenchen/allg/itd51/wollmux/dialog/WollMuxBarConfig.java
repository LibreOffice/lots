/* Copyright (C) 2009 Matthias S. Benkmann
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
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.Workarounds;

/**
 * Verwaltet die Konfiguration der WollMuxbar und bietet einen Dialog zum Ändern
 * derselben.
 */
public class WollMuxBarConfig
{
  /**
   * Titel des WollMuxBar-Fensters (falls nicht anders konfiguriert).
   */
  private static final String DEFAULT_TITLE = L.m("Vorlagen und Formulare");

  /**
   * Wenn die WollMuxBar den Fokus verliert, minimiert sich das Fenster.
   */
  static final int MINIMIZE_TO_TASKBAR_MODE = 1;

  /**
   * Die WollMuxBar verhält sich wie ein normales Fenster.
   */
  static final int NORMAL_WINDOW_MODE = 2;

  /**
   * Die WollMuxBar ist immer im Vordergrund.
   */
  static final int ALWAYS_ON_TOP_WINDOW_MODE = 3;

  /**
   * Die WollMuxBar verschwindet am oberen Rand, wenn der Mauscursor sie verlässt.
   */
  static final int UP_AND_AWAY_WINDOW_MODE = 4;

  /**
   * Kein TrayIcon für die WollMuxBar.
   */
  static final int NO_TRAY_ICON = 0;

  /**
   * TrayIcon, auf das die WollMuxBar ikonifiziert wird und das sie per Klick wieder
   * de-ikonifiziert.
   */
  static final int ICONIFY_TRAY_ICON = 1;

  /**
   * TrayIcon, das Popup-Menü für den Schnellzugriff auf die WollMuxBar-Einträge zur
   * Verfügung stellt.
   */
  static final int POPUP_TRAY_ICON = 2;

  /**
   * TrayIcon, auf das die WollMuxBar ikonifiziert wird und das sie per Klick wieder
   * de-ikonifiziert, und das außerdem ein Popup-Menü für den Schnellzugriff auf die
   * WollMuxBar-Einträge zur Verfügung stellt.
   */
  static final int ICONIFY_AND_POPUP_TRAY_ICON = 3;

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;
  
  /**
   * Der Fenstertitel der WollMuxBar.
   */
  private String myFrame_title;

  /**
   * Der zentrale Vorgabewert aus der wollmux.conf für {@link #myFrame_title}.
   */
  private String myFrame_title_default;

  /**
   * Falls > 0, so ist dies eine von wollmux,conf fest vorgegebene Breite. Falls 0,
   * so wird die natürliche Breite verwendet. Falls -1, so wird die maximale Breite
   * verwendet.
   */
  private int myFrame_width;

  /**
   * Der zentrale Vorgabewert aus der wollmux.conf für {@link #myFrame_width}.
   */
  private int myFrame_width_default;

  /**
   * Falls > 0, so ist dies eine von wollmux,conf fest vorgegebene Höhe. Falls 0, so
   * wird die natürliche Höhe verwendet. Falls -1, so wird die maximale Höhe
   * verwendet.
   */
  private int myFrame_height;

  /**
   * Der zentrale Vorgabewert aus der wollmux.conf für {@link #myFrame_height}.
   */
  private int myFrame_height_default;

  /**
   * Falls >= 0, so ist dies eine von wollmux,conf fest vorgegebene x-Koordinate.
   * Falls -1, so wird das Fenster zentriert. Falls -2, so wird die größte sinnvolle
   * Koordinate verwendet. Falls -3, so wird die kleinste sinnvolle Koordinate
   * verwendet. Falls Integer.MIN_VALUE, so ist keine Koordinate fest vorgegeben.
   */
  private int myFrame_x;

  /**
   * Der zentrale Vorgabewert aus der wollmux.conf für {@link #myFrame_x}.
   */
  private int myFrame_x_default;

  /**
   * Falls >= 0, so ist dies eine von wollmux,conf fest vorgegebene y-Koordinate.
   * Falls -1, so wird das Fenster zentriert. Falls -2, so wird die größte sinnvolle
   * Koordinate verwendet. Falls -3, so wird die kleinste sinnvolle Koordinate
   * verwendet. Falls Integer.MIN_VALUE, so ist keine Koordinate fest vorgegeben.
   */
  private int myFrame_y;

  /**
   * Der zentrale Vorgabewert aus der wollmux.conf für {@link #myFrame_y}.
   */
  private int myFrame_y_default;

  /**
   * Der Anzeigemodus für die WollMuxBar (z,B,
   * {@link WollMuxBarConfig#UP_AND_AWAY_WINDOW_MODE}).
   */
  private int windowMode;

  /**
   * Der zentrale Vorgabewert aus der wollmux.conf für {@link #windowMode}.
   */
  private int windowMode_default;

  /**
   * Der Modus für das WollMuxTrayIcon (zum Beispiel
   * {@link WollMuxBarConfig#ICONIFY_TRAY_ICON}).
   */
  private int trayIconMode;

  /**
   * Der zentrale Vorgabewert für den {@link #trayIconMode};
   */
  private int trayIconMode_default;

  /**
   * Der Maximalwert für den {@link #myFrame_fontzoom}
   * (größer 10 wird von der zoom-Methode verweigert).
   */
  private final float myFrame_fontzoom_Max = 10;
  /**
   * Der Zoomfaktor für Fonts in der WollMux-Bar.
   * Eingeschränkt auf 0 .. {@link #myFrame_fontzoom_Max}.
   */
  private float myFrame_fontzoom;

  /**
   * Der zentrale Vorgabewert für den {@link #myFrame_fontzoom};
   */
  private float  myFrame_fontzoom_default;

  /**
   * Die aktiven CONF_IDs.
   */
  private Set<String> conf_ids;

  /**
   * Die vom Administrator vorgegebene wollmux.conf
   */
  private ConfigThingy defaultConf;

  /**
   * Die wollmuxbar.conf des Benutzers.
   */
  private ConfigThingy userConf;

  /**
   * Der Elternframe zu dem die Dialoge dieser Klasse modal sind.
   */
  private JFrame parent;

  /**
   * Falls nicht-null die gerade oder zuletzt angezeigte Instanz des Optionen
   * Dialogs.
   */
  private JDialog myDialog;

  /**
   * Erzeugt eine neue Konfiguration.
   * 
   * @param winMode
   *          falls > 0 overridet dieser windowMode die entsprechenden Angaben sowohl
   *          aus userConf als auch aus defaultConf
   * @param defaultConf
   *          die wollmux.conf
   * @param userConf
   *          die wollmuxbar.conf
   */
  public WollMuxBarConfig(int winMode, ConfigThingy defaultConf,
      ConfigThingy userConf, boolean allowUserConfig)
  {
    this.userConf = userConf;
    this.defaultConf = defaultConf;

    readConfig(defaultConf, true);
    if (allowUserConfig) readConfig(userConf, false);

    /*
     * Falls ein winMode übergeben wurde overridet er userConf und defaultConf
     */
    if (winMode > 0) windowMode = winMode;
  
    this.conf_ids = new HashSet<String>();
    ConfigThingy active_ids = new ConfigThingy("aciveIDs");
    if (userConf != null)
      active_ids = userConf.query("WollMuxBarKonfigurationen", 1).query("Aktiv", 2);
    if (active_ids.count() == 0)
      active_ids =
        defaultConf.query("WollMuxBarKonfigurationen", 1).query("Aktiv", 2);
  
    if (active_ids.count() > 0)
    {
      try
      {
        active_ids = active_ids.getLastChild();
      }
      catch (NodeNotFoundException x)
      {}
      for (ConfigThingy idConf : active_ids)
      {
        conf_ids.add(idConf.getName());
      }
    }
  }

  /**
   * Liest die Einstellungen aus der Konfiguration configuration aus und
   * initialisiert die internen Konfigurationsparameter.
   * 
   * @param configuration
   *          Wurzel einer Konfiguration. Darf auch null sein, dann macht die Methode
   *          nichts.
   * @param setAsDefault
   *          falls true, werden zusätzlich auch die internen Defaulteinstellungen
   *          mit den gelesenen Werten initialisiert.
   * 
   * @author Matthias S. Benkmann, Christoph Lutz (CIB software GmbH)
   */
  private void readConfig(ConfigThingy configuration, boolean setAsDefault)
  {
    if(configuration == null)
      return;

    // Vorbelegung mit Defaults für den Fall eines unvollständigen Konfigurationsabschnitts
    String lFrame_title = myFrame_title_default;
    int lWindowMode = windowMode_default;
    int lTrayIconMode = trayIconMode_default;
    int lFrame_x = myFrame_x_default;
    int lFrame_y = myFrame_y_default;
    int lFrame_width = myFrame_width_default;
    int lFrame_height = myFrame_height_default;
    float lFrame_fontzoom = myFrame_fontzoom_default;

    // wenn wir die defaults erst definieren, dann stelle sicher, dass sie "frisch" sind
    if (setAsDefault) 
    {
      lFrame_title = WollMuxBarConfig.DEFAULT_TITLE;
      lWindowMode = WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE;
      lTrayIconMode = WollMuxBarConfig.NO_TRAY_ICON;
      lFrame_x = Integer.MIN_VALUE;
      lFrame_y = Integer.MIN_VALUE;
      lFrame_width = 0;
      lFrame_height = 0;
      lFrame_fontzoom = 1;
    }

    // nutzen wir die bereits wenn wir die Defaults nicht setzen, gehen wir davon aus, dass die Defaults vorher schon 

    // configuration auswerten
    ConfigThingy wmbConf = configuration.query("Fenster", 1).query("WollMuxBar", 2);
    try
    {
      wmbConf = wmbConf.getLastChild();
      for (ConfigThingy conf : wmbConf)
      {
        if (conf.getName().equals("TITLE"))
          lFrame_title = conf.toString();
        else if (conf.getName().equals("MODE"))
          lWindowMode = getWindowMode(conf.toString());
        else if (conf.getName().equals("TRAYICON"))
          lTrayIconMode = getTrayIconMode(conf.toString());
        else if (conf.getName().equals("X"))
          lFrame_x = getXY(conf.toString());
        else if (conf.getName().equals("Y"))
          lFrame_y = getXY(conf.toString());
        else if (conf.getName().equals("WIDTH"))
          lFrame_width = getWidthHeight(conf.toString());
        else if (conf.getName().equals("HEIGHT"))
          lFrame_height = getWidthHeight(conf.toString());
      }
    }
    catch (NodeNotFoundException x)
    {
      // Abschnitt nicht da -> oben gesetzte Defaults werden verwendet
    }
    
    // Noch nach der Font Größe suchen, die steht in einem anderen Abschnitt
    ConfigThingy dialogConf = configuration.query("Dialoge", 1);
    try
    {
      dialogConf = dialogConf.getLastChild();
      for (ConfigThingy conf : dialogConf)
      {
        if (conf.getName().equals("FONT_ZOOM"))
          lFrame_fontzoom = getFontZoom(conf.toString());
      }
    }
    catch (NodeNotFoundException x)
    {
      // Abschnitt nicht da -> oben gesetzte Defaults werden verwendet
    }

    myFrame_title = lFrame_title;
    windowMode = lWindowMode;
    trayIconMode = lTrayIconMode;
    myFrame_x = lFrame_x;
    myFrame_y = lFrame_y;
    myFrame_width = lFrame_width;
    myFrame_height = lFrame_height;
    myFrame_fontzoom = lFrame_fontzoom;

    if (setAsDefault)
    {
      myFrame_title_default = lFrame_title;
      windowMode_default = lWindowMode;
      trayIconMode_default = lTrayIconMode;
      myFrame_x_default = lFrame_x;
      myFrame_y_default = lFrame_y;
      myFrame_width_default = lFrame_width;
      myFrame_height_default = lFrame_height;
      myFrame_fontzoom_default = lFrame_fontzoom;
    }
  }

  /**
   * Liefert true gdw conf_id aktiv ist (d.h. damit markierte Menüelemente angezeigt
   * werden sollen).
   */
  public boolean isIDActive(String conf_id)
  {
    return conf_ids.contains(conf_id);
  }

  /**
   * Liefert den Titel, den das WollMuxBar-Fenster haben soll.
   * 
   */
  public String getWindowTitle()
  {
    return myFrame_title;
  }

  /**
   * Liefert das gewünschte Fensterverhalten der WollMuxBar, z.B.
   * {@link #UP_AND_AWAY_WINDOW_MODE}.
   */
  public int getWindowMode()
  {
    return windowMode;
  }

  /**
   * Liefert das gewünschte Verhalten für das Tray-Icon der WollMuxBar, z.B.
   * {@link #ICONIFY_TRAY_ICON}.
   */
  public int getTrayIconMode()
  {
    return trayIconMode;
  }

  /**
   * Liefert die konfigurierte Breite der WollMuxBar. Falls > 0, so ist dies eine von
   * wollmux,conf fest vorgegebene Breite. Falls 0, so wird die natürliche Breite
   * verwendet. Falls -1, so wird die maximale Breite verwendet.
   */
  public int getWidth()
  {
    return myFrame_width;
  }

  /**
   * Liefert die konfigurierte Höhe der WollMuxBar. Falls > 0, so ist dies eine von
   * wollmux,conf fest vorgegebene Höhe. Falls 0, so wird die natürliche Höhe
   * verwendet. Falls -1, so wird die maximale Höhe verwendet.
   */
  public int getHeight()
  {
    return myFrame_height;
  }

  /**
   * Liefert die konfigurierte X-Position der WollMuxBar. Falls >= 0, so ist dies
   * eine von wollmux,conf fest vorgegebene x-Koordinate. Falls -1, so wird das
   * Fenster zentriert. Falls -2, so wird die größte sinnvolle Koordinate verwendet.
   * Falls -3, so wird die kleinste sinnvolle Koordinate verwendet. Falls
   * Integer.MIN_VALUE, so ist keine Koordinate fest vorgegeben.
   * 
   * ACHTUNG! Ist hier ein fester Wert gesetzt, so sollte dieser nur einmal so
   * gesetzt werden. Danach sollte der Benutzer das Fenster frei verschieben können.
   */
  public int getX()
  {
    return myFrame_x;
  }

  /**
   * Liefert die konfigurierte Y-Position der WollMuxBar. Falls >= 0, so ist dies
   * eine von wollmux,conf fest vorgegebene y-Koordinate. Falls -1, so wird das
   * Fenster zentriert. Falls -2, so wird die größte sinnvolle Koordinate verwendet.
   * Falls -3, so wird die kleinste sinnvolle Koordinate verwendet. Falls
   * Integer.MIN_VALUE, so ist keine Koordinate fest vorgegeben.
   * 
   * ACHTUNG! Ist hier ein fester Wert gesetzt, so sollte dieser nur einmal so
   * gesetzt werden. Danach sollte der Benutzer das Fenster frei verschieben können.
   */
  public int getY()
  {
    return myFrame_y;
  }

  /**
   * Liefert den konfigurierten FONT_ZOOM-Wert. 
   */
  public float getFontZoom()
  {
    return myFrame_fontzoom;
  }

  /**
   * Zeigt einen Dialog zum Bearbeiten der Optionen. Bei Beendigung mit "OK" wird
   * finishedAction aufgerufen mit ActionCommand "OK". Bei negativer Beendigung
   * erfolgt kein Aufruf des ActionListeners. Diese Methode kann in und außerhalb des
   * EDT aufgerufen werden.
   * 
   * TESTED
   */
  public void showOptionsDialog(final JFrame parent,
      final ActionListener finishedAction)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        createGUI(parent, finishedAction);
      }
    });

  }

  /**
   * Liefert true gdw gerade ein Options-Dialog sichtbar ist.
   */
  public boolean isDialogVisible()
  {
    return (myDialog != null && myDialog.isVisible());
  }

  private void createGUI(final JFrame parent, final ActionListener finishedAction)
  {
    this.parent = parent;
    myDialog = new JDialog(parent, L.m("Optionen"));
    myDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    myDialog.setModal(true);

    JPanel myContentPane = new JPanel(new BorderLayout());
    myDialog.setContentPane(myContentPane);
    JPanel mainPanel = new JPanel(new GridBagLayout());
    JPanel buttonPanel = new JPanel(new GridBagLayout());
    myContentPane.add(mainPanel, BorderLayout.CENTER);
    myContentPane.add(buttonPanel, BorderLayout.SOUTH);

    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcTextfield =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcSeparator =
      new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcCombo =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcLabel =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcCheckbox =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcGlue =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    GridBagConstraints gbcButton =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);

    int x = 0;
    int y = 0;

    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel(L.m("Fenstertitel")), gbcLabel);

    final JTextField inputTitle = new JTextField(20);
    gbcTextfield.gridx = x++;
    gbcTextfield.gridy = y;
    mainPanel.add(inputTitle, gbcTextfield);

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel(L.m("Fensterverhalten")), gbcLabel);
    final JComboBox<String> inputMode = new JComboBox<String>(new String[] {
      "UpAndAway", "Minimize", "Window", "AlwaysOnTop" });
    inputMode.setEditable(false);
    gbcCombo.gridx = x++;
    gbcCombo.gridy = y;
    mainPanel.add(inputMode, gbcCombo);

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    final JComboBox<String> inputTrayIcon = new JComboBox<String>(new String[] {
      "None", "Iconify", "Popup", "IconifyAndPopup" });
    inputTrayIcon.setEditable(false);
    gbcCombo.gridx = x++;
    gbcCombo.gridy = y;
    // Tray-Icon-Auswahl nur in Optionen-Menü anzeigen, wenn Java 6+
    if (!Workarounds.workaroundForJava5("Tray-Icon", false))
    {
      mainPanel.add(new JLabel(L.m("Tray-Icon")), gbcLabel);
      mainPanel.add(inputTrayIcon, gbcCombo);
    }
    
    inputMode.addItemListener(new ItemListener()
    {      
      public void itemStateChanged(ItemEvent e)
      {
        // tray icon auswahl nur anbieten, falls nicht upAndAway
        if(e.getStateChange() == ItemEvent.SELECTED) {
          if(e.getItem().toString().equalsIgnoreCase("UpAndAway")) {
            inputTrayIcon.setSelectedIndex(0);
            inputTrayIcon.setEnabled(false);
          }
        }
        if(e.getStateChange() == ItemEvent.DESELECTED) {
          if(e.getItem().toString().equalsIgnoreCase("UpAndAway")) {
            inputTrayIcon.setEnabled(true);
          }
        }   
      }
    });

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel("X"), gbcLabel);
    final JComboBox<String> inputX = new JComboBox<String>(new String[] {
      "auto", "center", "min", "max" });
    inputX.setEditable(true);
    gbcCombo.gridx = x++;
    gbcCombo.gridy = y;
    mainPanel.add(inputX, gbcCombo);

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel("Y"), gbcLabel);
    final JComboBox<String> inputY = new JComboBox<String>(new String[] {
      "auto", "center", "min", "max" });
    inputY.setEditable(true);
    gbcCombo.gridx = x++;
    gbcCombo.gridy = y;
    mainPanel.add(inputY, gbcCombo);

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel(L.m("Breite")), gbcLabel);
    final JComboBox<String> inputWidth = new JComboBox<String>(new String[] {
      "auto", "max" });
    inputWidth.setEditable(true);
    gbcCombo.gridx = x++;
    gbcCombo.gridy = y;
    mainPanel.add(inputWidth, gbcCombo);

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel(L.m("Höhe")), gbcLabel);
    final JComboBox<String> inputHeight = new JComboBox<String>(new String[] {
      "auto", "max" });
    inputHeight.setEditable(true);
    gbcCombo.gridx = x++;
    gbcCombo.gridy = y;
    mainPanel.add(inputHeight, gbcCombo);

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel(L.m("Font-Zoom")), gbcLabel);
    final JSpinner fontZoom = new JSpinner(new SpinnerNumberModel(1.0,0.0,myFrame_fontzoom_Max,0.1));
    gbcCombo.gridx = x++;
    gbcCombo.gridy = y;
    mainPanel.add(fontZoom, gbcCombo);

    x = 0;
    ++y;
    gbcSeparator.gridx = x++;
    gbcSeparator.gridy = y;
    mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbcSeparator);

    x = 0;
    ++y;
    gbcLabel.gridx = x++;
    gbcLabel.gridy = y;
    mainPanel.add(new JLabel(L.m("Aktive Menügruppen")), gbcLabel);

    x = 0;
    ++y;
    gbcCheckbox.gridx = x;
    gbcCheckbox.gridy = y;
    final List<JCheckBox> checkboxes = new Vector<JCheckBox>();
    addCheckboxesForConfIDs(mainPanel, gbcCheckbox, checkboxes);
    y = gbcCheckbox.gridy;

    inputTitle.setText(myFrame_title);
    setCombo(inputMode, windowModeToText(windowMode));
    
    // bei UpAndAway: tray icon none!
    if(windowMode == WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE) {
      inputTrayIcon.setEnabled(false);
    } else {
      setCombo(inputTrayIcon, trayIconModeToText(trayIconMode));
    }
        
    setCombo(inputX, xyToText(myFrame_x));
    setCombo(inputY, xyToText(myFrame_y));
    setCombo(inputWidth, widthHeightToText(myFrame_width));    
    setCombo(inputHeight, widthHeightToText(myFrame_height));
    setSpinner(fontZoom, myFrame_fontzoom);

    x = 0;
    y = 0;
    gbcButton.gridx = x++;
    gbcButton.gridy = y;
    buttonPanel.add(new JButton(new AbstractAction(L.m("Abbrechen"))
    {
      private static final long serialVersionUID = -7034390091333940094L;

      public void actionPerformed(ActionEvent e)
      {
        myDialog.dispose();
      }
    }), gbcButton);
    gbcGlue.gridx = x++;
    gbcGlue.gridy = y;
    buttonPanel.add(Box.createHorizontalGlue(), gbcGlue);
    gbcButton.gridx = x++;
    gbcButton.gridy = y;
    buttonPanel.add(new JButton(new AbstractAction(L.m("Standard wiederherstellen"))
    {
      private static final long serialVersionUID = -5001745869100349262L;

      public void actionPerformed(ActionEvent e)
      {
        inputTitle.setText(myFrame_title_default);
        setCombo(inputMode, windowModeToText(windowMode_default));
        setCombo(inputTrayIcon, trayIconModeToText(trayIconMode_default));
        setCombo(inputX, xyToText(myFrame_x_default));
        setCombo(inputY, xyToText(myFrame_y_default));
        setCombo(inputWidth, widthHeightToText(myFrame_width_default));
        setCombo(inputHeight, widthHeightToText(myFrame_height_default));
        setSpinner(fontZoom, myFrame_fontzoom_default);
        setCheckboxesForConfIDsToDefaultValues(checkboxes);
      }
    }), gbcButton);
    gbcGlue.gridx = x++;
    gbcGlue.gridy = y;
    buttonPanel.add(Box.createHorizontalGlue(), gbcGlue);
    gbcButton.gridx = x++;
    gbcButton.gridy = y;
    buttonPanel.add(new JButton(new AbstractAction(L.m("OK"))
    {
      private static final long serialVersionUID = -4400165442859398615L;

      public void actionPerformed(ActionEvent e)
      {
        myFrame_title = inputTitle.getText();
        windowMode = getWindowMode(inputMode.getSelectedItem().toString());
        
        // upAndAway modus niemals mit iconify verwenden!
        if(windowMode == WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE)
          trayIconMode = WollMuxBarConfig.NO_TRAY_ICON;
        else
          trayIconMode = getTrayIconMode(inputTrayIcon.getSelectedItem().toString());
               
        myFrame_x = getXY(inputX.getSelectedItem().toString());
        myFrame_y = getXY(inputY.getSelectedItem().toString());
        myFrame_width = getWidthHeight(inputWidth.getSelectedItem().toString());
        myFrame_height = getWidthHeight(inputHeight.getSelectedItem().toString());
        myFrame_fontzoom = getFontZoom(fontZoom.getValue());
        conf_ids = getConfIDsFromCheckboxes(checkboxes);

        doSave();
        finishedAction.actionPerformed(new ActionEvent(this, 0, "OK"));

        myDialog.dispose();
      }
    }), gbcButton);

    myDialog.pack();
    Rectangle parentBounds = parent.getBounds();
    int frameWidth = myDialog.getWidth();
    int frameHeight = myDialog.getHeight();
    x = parentBounds.x + parentBounds.width / 2 - frameWidth / 2;
    y = parentBounds.y + parentBounds.height / 2 - frameHeight / 2;
    if (y < 32) y = 32;
    myDialog.setLocation(x, y);

    myDialog.setResizable(false);
    myDialog.setVisible(true);
  }

  /**
   * Liefert die zu den selektierten Checkboxen aus checkboxes, die durch
   * {@link #addCheckboxesForConfIDs(JPanel, GridBagConstraints, List)} erzeugt
   * worden sein müssen, zugehörigen CONF_IDs.
   * 
   * TESTED
   */
  protected Set<String> getConfIDsFromCheckboxes(List<JCheckBox> checkboxes)
  {
    Set<String> conf_ids = new HashSet<String>();

    List<MenuManager.ConfigID> configIDs =
      MenuManager.parseConfigIDs(defaultConf, userConf);

    Collections.sort(configIDs);

    Iterator<JCheckBox> iter = checkboxes.iterator();

    for (MenuManager.ConfigID configID : configIDs)
    {
      JCheckBox checkBox = iter.next();
      if (checkBox.isSelected()) conf_ids.add(configID.id);
    }

    return conf_ids;
  }

  /**
   * Setzt die Checkboxen aus checkboxes, die durch
   * {@link #addCheckboxesForConfIDs(JPanel, GridBagConstraints, List)} erzeugt
   * worden sein müssen, auf die Zustände auf die sie von
   * {@link #addCheckboxesForConfIDs(JPanel, GridBagConstraints, List)} gesetzt
   * worden wären, wenn für das Bestimmen von {@link #conf_ids} nur
   * {@link #defaultConf} herangezogen worden wäre.
   * 
   * TESTED
   */
  protected void setCheckboxesForConfIDsToDefaultValues(List<JCheckBox> checkboxes)
  {
    List<MenuManager.ConfigID> configIDs =
      MenuManager.parseConfigIDs(defaultConf, userConf);

    Collections.sort(configIDs);

    Set<String> conf_ids = new HashSet<String>();
    ConfigThingy active_ids =
      defaultConf.query("WollMuxBarKonfigurationen", 1).query("Aktiv", 2);

    try
    {
      for (ConfigThingy idConf : active_ids.getLastChild())
        conf_ids.add(idConf.getName());
    }
    catch (NodeNotFoundException x)
    {
      // Falls getLastChild() wirft, bleibt conf_ids einfach leer
    }

    Iterator<JCheckBox> iter = checkboxes.iterator();

    for (MenuManager.ConfigID configID : configIDs)
    {
      JCheckBox checkBox = iter.next();
      checkBox.setSelected((conf_ids.contains(configID.id)));
    }
  }

  /**
   * Fügt mainPanel je eine Checkbox für jede in einem
   * WollMuxBarKonfigurationen/Labels-Abschnitt definierte CONF_ID hinzu, wobei
   * gbcCheckbox als LayoutConstraints übergeben wird (es wird jeweils y eins
   * raufgezählt). Die so erstellten Checkboxen werden außerdem zur Liste checkboxes
   * hinzugefügt. Der initiale Aktivierungswert der Checkboxen richtet sich nach
   * {@link #conf_ids}.
   * 
   * TESTED
   */
  private void addCheckboxesForConfIDs(JPanel mainPanel,
      GridBagConstraints gbcCheckbox, List<JCheckBox> checkboxes)
  {
    List<MenuManager.ConfigID> configIDs =
      MenuManager.parseConfigIDs(defaultConf, userConf);

    Collections.sort(configIDs);

    for (MenuManager.ConfigID configID : configIDs)
    {
      String label = configID.label_user;
      if (label == null) label = configID.label_default;
      JCheckBox checkbox = new JCheckBox(label);
      checkboxes.add(checkbox);
      mainPanel.add(checkbox, gbcCheckbox);
      ++gbcCheckbox.gridy;
      if (isIDActive(configID.id)) checkbox.setSelected(true);
    }
  }

  /**
   * Wählt das Item von combo aus, das equals zu str ist. Falls keines vorhanden ist,
   * wird str direkt gesetzt (ergibt nur bei einer editable ComboBox Sinn).
   */
  private void setCombo(JComboBox<String> combo, String str)
  {
    for (int i = 0; i < combo.getItemCount(); ++i)
      if (str.equals(combo.getItemAt(i)))
      {
        combo.setSelectedIndex(i);
        return;
      }

    combo.setSelectedItem(str);
  }

  /**
   * Wählt das Item von combo aus, das equals zu str ist. Falls keines vorhanden ist,
   * wird str direkt gesetzt (ergibt nur bei einer editable ComboBox Sinn).
   */
  private void setSpinner(JSpinner spinner, float value)
  {
    spinner.setValue(new Float(value));
  }

  private void doSave()
  {
    ConfigThingy conf = userConf;
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy subConf = iter.next();
      if (subConf.getName().equals("Fenster"))
      {
        Iterator<ConfigThingy> subIter = subConf.iterator();
        while (subIter.hasNext())
        {
          String name = subIter.next().getName();
          if (name.equals("WollMuxBar")) subIter.remove();
        }
        if (subConf.count() == 0) iter.remove();
      }
      else if (subConf.getName().equals("WollMuxBarKonfigurationen"))
      {
        Iterator<ConfigThingy> subIter = subConf.iterator();
        while (subIter.hasNext())
        {
          String name = subIter.next().getName();
          if (name.equals("Aktiv")) subIter.remove();
        }
        if (subConf.count() == 0) iter.remove();
      }
      else if (subConf.getName().equals("Dialoge"))
      {
        Iterator<ConfigThingy> subIter = subConf.iterator();
        while (subIter.hasNext())
        {
          String name = subIter.next().getName();
          if (name.equals("FONT_ZOOM")) subIter.remove();
        }
        if (subConf.count() == 0) iter.remove();
      }
    }

    if (myFrame_title.equals(myFrame_title_default)
      && windowMode == windowMode_default && myFrame_x == myFrame_x_default
      && trayIconMode == trayIconMode_default && myFrame_y == myFrame_y_default
      && myFrame_width == myFrame_width_default
      && myFrame_height == myFrame_height_default
      && myFrame_fontzoom == myFrame_fontzoom_default)
    {
      // Falls gegenüber den Werten aus defaultConf nichts geändert wurde, dann
      // schreiben wir keinen Fenster-Abschnitt in die wollmuxbar.conf. Der
      // Schreibcode wird allerdings trotzdem ausgeführt, damit eine eventuell oben
      // durchgeführte Löschung eines Fenster-Abschnitts aktiv wird.
    }
    else
    {
      ConfigThingy wmbConf = conf.add("Fenster").add("WollMuxBar");
      wmbConf.add("TITLE").add(myFrame_title);
      wmbConf.add("MODE").add(windowModeToText(windowMode));
      wmbConf.add("TRAYICON").add(trayIconModeToText(trayIconMode));
      wmbConf.add("X").add(xyToText(myFrame_x));
      wmbConf.add("Y").add(xyToText(myFrame_y));
      wmbConf.add("WIDTH").add(widthHeightToText(myFrame_width));
      wmbConf.add("HEIGHT").add(widthHeightToText(myFrame_height));
      ConfigThingy dialogConf = conf.add("Dialoge");
      dialogConf.add("FONT_ZOOM").add(zoomToText(myFrame_fontzoom));
    }

    ConfigThingy active_ids =
      defaultConf.query("WollMuxBarKonfigurationen", 1).query("Aktiv", 2);
    try
    {
      active_ids = active_ids.getLastChild();
    }
    catch (NodeNotFoundException x1)
    {}

    boolean needToWriteAktiv = (active_ids.count() != conf_ids.size());
    if (!needToWriteAktiv)
    {
      for (ConfigThingy conf_id_conf : active_ids)
        if (!isIDActive(conf_id_conf.toString())) needToWriteAktiv = true;
    }

    if (needToWriteAktiv)
    {
      ConfigThingy wmbk = conf.query("WollMuxBarKonfigurationen", 1);
      try
      {
        wmbk = wmbk.getLastChild();
      }
      catch (NodeNotFoundException x)
      {
        wmbk = conf.add("WollMuxBarKonfigurationen");
      }

      ConfigThingy aktivConf = wmbk.add("Aktiv");
      for (String id : conf_ids)
        aktivConf.add(id);
    }

    File wollmuxbarConfFile =
      new File(WollMuxFiles.getWollMuxDir(), WollMuxBar.WOLLMUXBAR_CONF);
    try
    {
      WollMuxFiles.writeConfToFile(wollmuxbarConfFile, userConf);
    }
    catch (Exception x)
    {
      Logger.error(x);
      JOptionPane.showMessageDialog(parent, L.m(
        "Beim Speichern ist ein Fehler aufgetreten:\n%1", x.getMessage()),
        L.m("Fehler beim Speichern"), JOptionPane.ERROR_MESSAGE);
    }
  }

  private String windowModeToText(int windowMode)
  {
    switch (windowMode)
    {
      case MINIMIZE_TO_TASKBAR_MODE:
        return "Minimize";
      case ALWAYS_ON_TOP_WINDOW_MODE:
        return "AlwaysOnTop";
      case NORMAL_WINDOW_MODE:
        return "Window";
      case UP_AND_AWAY_WINDOW_MODE:
        return "UpAndAway";
    }
    return "UpAndAway";
  }

  private String trayIconModeToText(int trayIconMode)
  {
    switch (trayIconMode)
    {
      case NO_TRAY_ICON:
        return "None";
      case ICONIFY_TRAY_ICON:
        return "Iconify";
      case POPUP_TRAY_ICON:
        return "Popup";
      case ICONIFY_AND_POPUP_TRAY_ICON:
        return "IconifyAndPopup";
    }
    return "None";
  }

  private String xyToText(int xy)
  {
    switch (xy)
    {
      case -1:
        return "center";
      case -2:
        return "max";
      case -3:
        return "min";
      case Integer.MIN_VALUE:
        return "auto";
      default:
        return "" + xy;
    }
  }

  private String widthHeightToText(int wh)
  {
    switch (wh)
    {
      case -1:
        return "max";
      case 0:
        return "auto";
      default:
        return "" + wh;
    }
  }

  /**
   * Wandelt den ZoomWert in einen String um
   * @param zoom der umzuwandelnde Zoom-Wert
   * @return zoom als String
   */
  private String zoomToText(float zoom){
    String text = Float.toString(zoom);
    return text;
  }

  /**
   * Liefert den passenden int zu Font-Zoom (0 <= x <= myFrame_fontzoom_Max). Ist der Wert
   * nicht parsbar, wird ein Fehler geloggert und 1 geliefert.
   */
  private float getFontZoom(String fontzoom)
  {
    float value = 1;
    try
    {
      value = Float.parseFloat(fontzoom);
      if (value < 0) value = 0;
      if (value > myFrame_fontzoom_Max) value = myFrame_fontzoom_Max;
    }
    catch (NumberFormatException x)
    {
      Logger.error(L.m(
        "Fehlerhafte Font-Zoom Angabe: '%1' ist keine Zahl ", fontzoom));
    }
    return value;
  }

  /**
   * Liefert den passenden Integer-Wert zu xy (Zahl, "auto", "min", "max", "center").
   * Falls kein parsbarer Wert, so wird ein Fehler geloggert und der int für "auto"
   * zurückgeliefert.
   * 
   */
  private int getXY(String xy)
  {
    int value = Integer.MIN_VALUE;
    if (xy.equalsIgnoreCase("center"))
      value = -1;
    else if (xy.equalsIgnoreCase("max"))
      value = -2;
    else if (xy.equalsIgnoreCase("min"))
      value = -3;
    else if (xy.equalsIgnoreCase("auto"))
      value = Integer.MIN_VALUE;
    else
      try
      {
        value = Integer.parseInt(xy);
        // Ja, das folgende ist eine Einschränkung, aber
        // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
        // obigen Festlegungen
        if (value < 0) value = 0;
      }
      catch (NumberFormatException x)
      {
        Logger.error(L.m(
          "Fehlerhafte X/Y Angabe: '%1' ist weder Zahl noch 'max', 'min', 'auto' oder 'center'",
          xy));
      }
    return value;
  }

  /**
   * Liefert den passenden int zu widthOrHeight (Zahl, "max", "auto"). Ist der Wert
   * nicht parsbar, wird ein Fehler geloggert und der int für "auto" geliefert.
   */
  private int getWidthHeight(String widthOrHeight)
  {
    int value = 0;
    if (widthOrHeight.equalsIgnoreCase("max"))
      value = -1;
    else if (widthOrHeight.equalsIgnoreCase("auto"))
      value = 0;
    else
      try
      {
        value = Integer.parseInt(widthOrHeight);
        if (value < 0) value = 0;
      }
      catch (NumberFormatException x)
      {
        Logger.error(L.m(
          "Fehlerhafte WIDTH/HEIGHT Angabe: '%1' ist weder Zahl noch 'max' oder 'auto'",
          widthOrHeight));
      }
    return value;
  }

  /**
   * Liefert den passenden Wert zu fontZoom.
   * nicht parsbar, wird ein Fehler geloggert und 1.0 geliefert.
   */
  private float getFontZoom(Object zoom)
  {
    float value = 1;
    if (zoom instanceof Double){
      value = ((Double)zoom).floatValue();
    }
    else
    {
      try
      {
        value = Float.parseFloat(zoom.toString());
      }
      catch (NumberFormatException x)
      {
        Logger.error(L.m(
          "Fehlerhafte Fontzoom Angabe: '%1' ist keine Zahl", zoom));
      }
    }
    return value;
  }

  /**
   * Liefert die zum String windowMode ("AlwaysOnTop", "Window", "Minimize", oder
   * "UpAndAway" in beliebiger Groß-/Kleinschreibung) gehörige Konstante (z.B.
   * {@link #UP_AND_AWAY_WINDOW_MODE}).
   * 
   * Enthält der String keinen identifizierbaren Wert, wird ein Fehler geloggert und
   * {@link #UP_AND_AWAY_WINDOW_MODE} geliefert.
   */
  private int getWindowMode(String windowMode)
  {
    int windowModeInt = UP_AND_AWAY_WINDOW_MODE;
    if (windowMode.equalsIgnoreCase("AlwaysOnTop"))
      windowModeInt = WollMuxBarConfig.ALWAYS_ON_TOP_WINDOW_MODE;
    else if (windowMode.equalsIgnoreCase("Window"))
      windowModeInt = WollMuxBarConfig.NORMAL_WINDOW_MODE;
    else if (windowMode.equalsIgnoreCase("Minimize"))
      windowModeInt = WollMuxBarConfig.MINIMIZE_TO_TASKBAR_MODE;
    else if (windowMode.equalsIgnoreCase("UpAndAway"))
      windowModeInt = WollMuxBarConfig.UP_AND_AWAY_WINDOW_MODE;
    else
      Logger.error(L.m("Ununterstützer MODE für WollMuxBar-Fenster: '%1'",
        windowMode));
    return windowModeInt;
  }

  /**
   * Liefert die zum String trayIconMode ("None", "Iconify", "Popup", oder
   * "IconifyAndPopup" in beliebiger Groß-/Kleinschreibung) gehörige Konstante (z.B.
   * {@link #ICONIFY_TRAY_ICON}).
   * 
   * Enthält der String keinen identifizierbaren Wert, wird ein Fehler geloggt und
   * {@link #NO_TRAY_ICON} geliefert.
   */
  private int getTrayIconMode(String trayIconMode)
  {
    int trayIconModeInt = NO_TRAY_ICON;
    if (trayIconMode.equalsIgnoreCase("None"))
      trayIconModeInt = WollMuxBarConfig.NO_TRAY_ICON;
    else if (trayIconMode.equalsIgnoreCase("Iconify"))
      trayIconModeInt = WollMuxBarConfig.ICONIFY_TRAY_ICON;
    else if (trayIconMode.equalsIgnoreCase("Popup"))
      trayIconModeInt = WollMuxBarConfig.POPUP_TRAY_ICON;
    else if (trayIconMode.equalsIgnoreCase("IconifyAndPopup"))
      trayIconModeInt = WollMuxBarConfig.ICONIFY_AND_POPUP_TRAY_ICON;
    else
      Logger.error(L.m(
        "Ununterstützer Wert von TRAYICON für WollMuxBar-Fenster: '%1'",
        trayIconMode));
    return trayIconModeInt;
  }

}
