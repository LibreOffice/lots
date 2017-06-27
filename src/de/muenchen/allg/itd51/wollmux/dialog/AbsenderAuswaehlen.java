/* 
 * Dateiname: AbsenderAuswaehlen.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Absenderdaten auswählen Dialog des BKS
 * 
 * Copyright (c) 2010-2017 Landeshauptstadt München
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
 * 25.10.2005 | BNK | Erstellung
 * 27.10.2005 | BNK | back + CLOSEACTION
 * 02.11.2005 | BNK | Absenderliste nicht mehr mit Vorname = M* befüllen,
 *                    weil jetzt der TestDJ schon eine Absenderliste
 *                    mit Einträgen hat.
 * 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
 * 03.01.2005 | BNK | Bug korrigiert;  .gridy = x  sollte .gridx = x sein.
 * 19.05.2006 | BNK | [R1898]Wenn die Liste leer ist, dann gleich den PAL Verwalten Dialog aufrufen
 * 26.02.2010 | BED | WollMux-Icon für das Fenster
 * 08.04.2010 | BED | [R52334] Anzeige über DISPLAY-Attribut konfigurierbar
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.TestDatasourceJoiner;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung
 * einen Dialog zum Auswählen eines Eintrages aus der Persönlichen Absenderliste. Die
 * private-Funktionen dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen
 * werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class AbsenderAuswaehlen
{
  /**
   * Default-Wert dafür, wie die Personen in der Absenderliste angezeigt werden
   * sollen, wenn es nicht explizit in der Konfiguration über das DISPLAY-Attribut
   * für eine listbox festgelegt ist. %{Spalte}-Syntax um entsprechenden Wert des
   * Datensatzes einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige
   * "Meier, Hans" etc.
   * 
   * An dieser Stelle einen Default-Wert hardzucodieren (der noch dazu LHM-spezifisch
   * ist!) ist sehr unschön und wurde nur gemacht um abwärtskompatibel zu alten
   * WollMux-Konfigurationen zu bleiben. Sobald sichergestellt ist, dass überall auf
   * eine neue WollMux-Konfiguration geupdatet wurde, sollte man diesen Fallback
   * wieder entfernen.
   */
  private static final String DEFAULT_DISPLAYTEMPLATE =
    "%{Nachname}, %{Vorname} (%{Rolle})";

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

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
   * ActionListener für Buttons mit der ACTION "back".
   */
  private ActionListener actionListener_back = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      back();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "editList".
   */
  private ActionListener actionListener_editList = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      editList();
    }
  };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;

  /**
   * Das JPanel der obersten Hierarchiestufe.
   */
  private JPanel mainPanel;

  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;

  /**
   * Die Listbox mit der persönlichen Absenderliste.
   */
  private JList<Object> palJList;

  /**
   * Gibt an, wie die Suchresultate in der {@link #palJList} angezeigt werden sollen.
   * Der Wert wird in der Konfiguration bei der "listbox" mit ID "suchanfrage" durch
   * Angeben des DISPLAY-Attributs konfiguriert. %{Spalte}-Syntax um entsprechenden
   * Wert des Datensatzes einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige
   * "Meier, Hans" etc.
   */
  private String palDisplayTemplate;

  /**
   * Der dem
   * {@link #AbsenderAuswaehlen(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener)
   * Konstruktor} übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Das ConfigThingy, das diesen Dialog spezifiziert.
   */
  private ConfigThingy myConf;

  /**
   * Das ConfigThingy, das den Dialog zum Bearbeiten der Absenderliste spezifiziert.
   */
  private ConfigThingy verConf;

  /**
   * Das ConfigThingy, das den Dialog zum Bearbeiten eines Datensatzes der
   * Absenderliste spezifiziert.
   */
  private ConfigThingy abConf;

  /**
   * Überwacht Änderungen in der Auswahl und wählt den entsprechenden Datensatz im
   * DJ.
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
   *          das ConfigThingy, das den Dialog zum Bearbeiten eines Datensatzes
   *          beschreibt.
   * @param verConf
   *          das ConfigThingy, das den Absenderliste Verwalten Dialog beschreibt.
   * @param dj
   *          der DatasourceJoiner, der die PAL verwaltet.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der Dialog
   *          geschlossen wurde. Das actionCommand des ActionEvents gibt die Aktion
   *          an, die das Beenden des Dialogs veranlasst hat.
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem
   *           Dialog unmöglich macht, zu funktionieren (z.B. dass der "Fenster"
   *           Schlüssel fehlt.
   */
  public AbsenderAuswaehlen(ConfigThingy conf, ConfigThingy verConf,
      ConfigThingy abConf, DatasourceJoiner dj, ActionListener dialogEndListener)
      throws ConfigurationErrorException
  {
    this.dj = dj;
    this.myConf = conf;
    this.abConf = abConf;
    this.verConf = verConf;
    this.dialogEndListener = dialogEndListener;
    this.palDisplayTemplate = DEFAULT_DISPLAYTEMPLATE;

    ConfigThingy fensterDesc1 = conf.query("Fenster");
    if (fensterDesc1.count() == 0)
      throw new ConfigurationErrorException(L.m("Schlüssel 'Fenster' fehlt in %1",
        conf.getName()));

    final ConfigThingy fensterDesc = fensterDesc1.query("Auswaehlen");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException(L.m("Schlüssel 'Auswaehlen' fehlt in ",
        conf.getName()));

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            createGUI(fensterDesc.getLastChild());
          }
          catch (Exception x)
          {}
          ;
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Erzeugt das GUI.
   * 
   * @param fensterDesc
   *          die Spezifikation dieses Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void createGUI(ConfigThingy fensterDesc)
  {
    Common.setLookAndFeelOnce();

    palJList = new JList<Object>(new DefaultListModel<Object>());

    String title = L.m("TITLE fehlt für Fenster AbsenderAuswaehlen/Auswaehlen");
    try
    {
      title = L.m(fensterDesc.get("TITLE").toString());
    }
    catch (Exception x)
    {}
    ;

    try
    {
      closeAction = getAction(fensterDesc.get("CLOSEACTION").toString());
    }
    catch (Exception x)
    {}

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    // WollMux-Icon für AbsenderAuswaehlen-Frame
    Common.setWollMuxIcon(myFrame);

    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myFrame.getContentPane().add(mainPanel);

    JPanel absenderliste = new JPanel(new GridBagLayout());
    JPanel buttons = new JPanel(new GridBagLayout());

    mainPanel.add(absenderliste, BorderLayout.CENTER);
    mainPanel.add(buttons, BorderLayout.PAGE_END);

    addUIElements(fensterDesc, "Absenderliste", absenderliste, 0, 1);
    addUIElements(fensterDesc, "Buttons", buttons, 1, 0);

    QueryResults palEntries = dj.getLOS();
    if (palEntries.isEmpty())
    {
      editList();
    }
    else
    {
      setListElements(palJList, dj.getLOS(), palDisplayTemplate);
      selectSelectedDataset(palJList);

      myFrame.pack();
      int frameWidth = myFrame.getWidth();
      int frameHeight = myFrame.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width / 2 - frameWidth / 2;
      int y = screenSize.height / 2 - frameHeight / 2;
      myFrame.setLocation(x, y);
      myFrame.setResizable(false);
      myFrame.setAlwaysOnTop(true);
      myFrame.setVisible(true);
      myFrame.requestFocus();
    }
  }

  /**
   * Fügt compo UI Elemente gemäss den Kindern von conf.query(key) hinzu. compo muss
   * ein GridBagLayout haben. stepx und stepy geben an um wieviel mit jedem UI
   * Element die x und die y Koordinate der Zelle erhöht werden soll. Wirklich
   * sinnvoll sind hier nur (0,1) und (1,0).
   */
  private void addUIElements(ConfigThingy conf, String key, JComponent compo,
      int stepx, int stepy)
  {
    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    // GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
    // GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new
    // Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel =
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
    GridBagConstraints gbcListBox =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);

    ConfigThingy felderParent = conf.query(key);
    int y = -stepy;
    int x = -stepx;

    Iterator<ConfigThingy> piter = felderParent.iterator();
    while (piter.hasNext())
    {
      Iterator<ConfigThingy> iter = (piter.next()).iterator();
      while (iter.hasNext())
      {
        y += stepy;
        x += stepx;

        ConfigThingy uiElementDesc = iter.next();
        try
        {
          /*
           * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN, DASS DER
           * ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET() UND EINER EVTL. DARAUS
           * RESULTIERENDEN NULLPOINTEREXCEPTION NOCH KONSISTENT IST!
           */

          // boolean readonly = false;
          String id = "";
          try
          {
            id = uiElementDesc.get("ID").toString();
          }
          catch (NodeNotFoundException e)
          {}
          // try{ if (uiElementDesc.get("READONLY").toString().equals("true"))
          // readonly = true; }catch(NodeNotFoundException e){}
          String type = uiElementDesc.get("TYPE").toString();

          if (type.equals("label"))
          {
            JLabel uiElement = new JLabel();
            gbcLabel.gridx = x;
            gbcLabel.gridy = y;
            compo.add(uiElement, gbcLabel);
            uiElement.setText(L.m(uiElementDesc.get("LABEL").toString()));
          }
          else if (type.equals("glue"))
          {
            Box uiElement = Box.createHorizontalBox();
            try
            {
              int minsize =
                Integer.parseInt(uiElementDesc.get("MINSIZE").toString());
              uiElement.add(Box.createHorizontalStrut(minsize));
            }
            catch (Exception e)
            {}
            uiElement.add(Box.createHorizontalGlue());

            gbcGlue.gridx = x;
            gbcGlue.gridy = y;
            compo.add(uiElement, gbcGlue);
          }
          else if (type.equals("listbox"))
          {
            int lines = 10;
            try
            {
              lines = Integer.parseInt(uiElementDesc.get("LINES").toString());
            }
            catch (Exception e)
            {}

            JList<Object> list;
            if (id.equals("pal"))
            {
              list = palJList;
              try
              {
                palDisplayTemplate = uiElementDesc.get("DISPLAY").toString();
              }
              catch (NodeNotFoundException e)
              {
                Logger.log(L.m(
                  "Kein DISPLAY-Attribut für die listbox mit ID \"pal\" im AbsenderAuswaehlen-Dialog angegeben! Verwende Fallback: %1",
                  DEFAULT_DISPLAYTEMPLATE));
                // Das DISPLAY-ATTRIBUT sollte eigentlich verpflichtend sein und wir
                // sollten an dieser Stelle einen echten Error loggen bzw. eine
                // Meldung in der GUI ausgeben und evtl. sogar abbrechen. Wir tun
                // dies allerdings nicht, da das DISPLAY-Attribut erst mit
                // WollMux 6.4.0 eingeführt wurde und wir abwärtskompatibel zu alten
                // WollMux-Konfigurationen bleiben müssen und Benutzer alter
                // Konfigurationen nicht mit Error-Meldungen irritieren wollen.
                // Dies ist allerdings nur eine Übergangslösung. Die obige Meldung
                // sollte nach ausreichend Zeit genauso wie DEFAULT_DISPLAYTEMPLATE
                // entfernt werden (bzw. wie oben gesagt überarbeitet).
              }
            }
            else
            {
              list = new JList<Object>(new DefaultListModel<Object>());
            }

            list.setVisibleRowCount(lines);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setLayoutOrientation(JList.VERTICAL);
            list.setPrototypeCellValue("Al-chman hemnal ulhillim el-WollMux(W-OLL-MUX-5.1)");

            list.addListSelectionListener(myListSelectionListener);

            String action = "";
            try
            {
              action = uiElementDesc.get("ACTION").toString();
            }
            catch (NodeNotFoundException e)
            {}

            ActionListener actionL = getAction(action);
            if (actionL != null)
              list.addMouseListener(new MyActionMouseListener(list, actionL));

            JScrollPane scrollPane = new JScrollPane(list);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

            gbcListBox.gridx = x;
            gbcListBox.gridy = y;
            compo.add(scrollPane, gbcListBox);
          }
          else if (type.equals("button"))
          {
            String action = "";
            try
            {
              action = uiElementDesc.get("ACTION").toString();
            }
            catch (NodeNotFoundException e)
            {}

            String label = L.m(uiElementDesc.get("LABEL").toString());

            char hotkey = 0;
            try
            {
              hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
            }
            catch (Exception e)
            {}

            JButton button = new JButton(label);
            button.setMnemonic(hotkey);

            gbcButton.gridx = x;
            gbcButton.gridy = y;
            compo.add(button, gbcButton);

            ActionListener actionL = getAction(action);
            if (actionL != null) button.addActionListener(actionL);

          }
          else
          {
            Logger.error(L.m("Ununterstützter TYPE für User Interface Element: ",
              type));
          }
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(e);
        }
      }
    }
  }

  /**
   * Wartet auf Doppelklick und führt dann die actionPerformed() Methode eines
   * ActionListeners aus.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class MyActionMouseListener extends MouseAdapter
  {
    private JList<Object> list;

    private ActionListener action;

    public MyActionMouseListener(JList<Object> list, ActionListener action)
    {
      this.list = list;
      this.action = action;
    }

    public void mouseClicked(MouseEvent e)
    {
      if (e.getClickCount() == 2)
      {
        Point location = e.getPoint();
        int index = list.locationToIndex(location);
        if (index < 0) return;
        Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(location)) return;
        action.actionPerformed(null);
      }
    }
  }

  /**
   * Übersetzt den Namen einer ACTION in eine Referenz auf das passende
   * actionListener_... Objekt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ActionListener getAction(String action)
  {
    if (action.equals("abort"))
    {
      return actionListener_abort;
    }
    else if (action.equals("back"))
    {
      return actionListener_back;
    }
    else if (action.equals("editList"))
    {
      return actionListener_editList;
    }
    else if (action.equals(""))
    {
      return null;
    }
    else
      Logger.error(L.m("Ununterstützte ACTION: ", action));

    return null;
  }

  /**
   * Nimmt eine JList list, die ein DefaultListModel haben muss und ändert ihre
   * Wertliste so, dass sie data entspricht. Die Datasets aus data werden nicht
   * direkt als Werte verwendet, sondern in {@link DJDatasetListElement}-Objekte
   * gewrappt, deren Inhalt entsprechend des übergebenen displayTemplates angezeigt
   * wird.
   * 
   * @param list
   *          die Liste deren Wertliste geändert werden soll
   * @param data
   *          enthält die Datensätze, mit denen die Liste gefüllt werden soll
   * @param displayTemplate
   *          gibt an wie die Datensätze in der Liste als Strings repräsentiert
   *          werden sollen, siehe z.B. {@link #DEFAULT_DISPLAYTEMPLATE}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList<Object> list, QueryResults data, String displayTemplate)
  {
    Object[] elements = new Object[data.size()];
    Iterator<Dataset> iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] =
        new DJDatasetListElement((DJDataset) iter.next(), displayTemplate);
    Arrays.sort(elements, new Comparator<Object>()
    {
      public int compare(Object o1, Object o2)
      {
        return o1.toString().compareTo(o2.toString());
      }
    });

    DefaultListModel<Object> listModel = (DefaultListModel<Object>) list.getModel();
    listModel.clear();
    for (i = 0; i < elements.length; ++i)
      listModel.addElement(elements[i]);
  }

  private void selectSelectedDataset(JList<?> list)
  {
    DefaultListModel<?> listModel = (DefaultListModel<?>) list.getModel();
    for (int i = 0; i < listModel.size(); ++i)
      if (((DJDatasetListElement) listModel.get(i)).getDataset().isSelectedDataset())
        list.setSelectedValue(listModel.get(i), true);
  }

  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden wenn
   * kein Eintrag selektiert ist.
   */
  private class MyListSelectionListener implements ListSelectionListener
  {
    public void valueChanged(ListSelectionEvent e)
    {
      @SuppressWarnings("unchecked")
      JList<Object> list = (JList<Object>) e.getSource();
      if (list != palJList) return;

      DJDatasetListElement ele = (DJDatasetListElement) list.getSelectedValue();
      if (ele == null)
        selectSelectedDataset(list);
      else
        ele.getDataset().select();
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
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void back()
  {
    dialogEnd("back");
  }

  /**
   * Beendet den Dialog und liefer actionCommand an den dialogEndHandler zurück
   * (falls er nicht null ist).
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
  private void editList()
  {
    ActionListener del =
      new MyDialogEndListener(this, myConf, verConf, abConf, dj, dialogEndListener,
        null);
    dialogEndListener = null;
    abort();
    try
    {
      new PersoenlicheAbsenderlisteVerwalten(verConf, abConf, dj, del);
    }
    catch (ConfigurationErrorException x)
    {
      Logger.error(x);
    }
  }

  private static class MyDialogEndListener implements ActionListener
  {
    private ConfigThingy conf;

    private ConfigThingy abConf;

    private ConfigThingy verConf;

    private DatasourceJoiner dj;

    private ActionListener dialogEndListener;

    private String actionCommand;

    private AbsenderAuswaehlen mySource;

    /**
     * Falls actionPerformed() mit getActionCommand().equals("back") aufgerufen wird,
     * wird ein neuer AbsenderAuswaehlen Dialog mit den übergebenen Parametern
     * erzeugt. Ansonsten wird der dialogEndListener mit actionCommand aufgerufen.
     * Falls actionCommand null ist wird das action command des ActionEvents
     * weitergereicht, der actionPerformed() übergeben wird. Falls actionPerformed ==
     * null wird auch die source weitergereicht, ansonsten wird die übergebene source
     * verwendet.
     */
    public MyDialogEndListener(AbsenderAuswaehlen source, ConfigThingy conf,
        ConfigThingy verConf, ConfigThingy abConf, DatasourceJoiner dj,
        ActionListener dialogEndListener, String actionCommand)
    {
      this.conf = conf;
      this.verConf = verConf;
      this.abConf = abConf;
      this.dj = dj;
      this.dialogEndListener = dialogEndListener;
      this.actionCommand = actionCommand;
      this.mySource = source;
    }

    public void actionPerformed(ActionEvent e)
    {
      if (e.getActionCommand().equals("back"))
        try
        {
          new AbsenderAuswaehlen(conf, verConf, abConf, dj, dialogEndListener);
        }
        catch (Exception x)
        {
          Logger.error(x);
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
   * Ein WindowListener, der auf den JFrame registriert wird, damit als Reaktion auf
   * den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
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
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          abort();
        }
      });
    }
    catch (Exception x)
    {/* Hope for the best */}
  }

  /**
   * Sorgt für das dauernde Neustarten des Dialogs.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class RunTest implements ActionListener
  {
    private DatasourceJoiner dj;

    private ConfigThingy conf;

    private ConfigThingy verConf;

    private ConfigThingy abConf;

    public RunTest(ConfigThingy conf, ConfigThingy verConf, ConfigThingy abConf,
        DatasourceJoiner dj)
    {
      this.dj = dj;
      this.conf = conf;
      this.abConf = abConf;
      this.verConf = verConf;
    }

    public void actionPerformed(ActionEvent e)
    {
      try
      {
        try
        {
          if (e.getActionCommand().equals("abort")) System.exit(0);
        }
        catch (Exception x)
        {}
        new AbsenderAuswaehlen(conf, verConf, abConf, dj, this);
      }
      catch (ConfigurationErrorException x)
      {
        Logger.error(x);
      }
    }
  }

  public static void main(String[] args) throws Exception
  {
    LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
    for (int i = 0; i < lf.length; ++i)
      System.out.println(lf[i].getClassName());
    System.out.println("Default L&F: " + UIManager.getSystemLookAndFeelClassName());
    String confFile = "testdata/WhoAmI.conf";
    String verConfFile = "testdata/PAL.conf";
    String abConfFile = "testdata/AbsenderdatenBearbeiten.conf";
    ConfigThingy conf =
      new ConfigThingy("", new URL(
        new File(System.getProperty("user.dir")).toURI().toURL(), confFile));
    ConfigThingy verConf =
      new ConfigThingy("", new URL(
        new File(System.getProperty("user.dir")).toURI().toURL(), verConfFile));
    ConfigThingy abConf =
      new ConfigThingy("", new URL(
        new File(System.getProperty("user.dir")).toURI().toURL(), abConfFile));
    TestDatasourceJoiner dj = new TestDatasourceJoiner();
    RunTest test =
      new RunTest(conf.get("AbsenderAuswaehlen"),
        verConf.get("PersoenlicheAbsenderliste"),
        abConf.get("AbsenderdatenBearbeiten"), dj);
    test.actionPerformed(null);
    Thread.sleep(600000);
    System.exit(0);
  }

}
