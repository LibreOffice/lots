/* 
 * Dateiname: SachleitendeVerfuegungenDruckdialog.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Dialog zum Drucken von Sachleitenden Verfügungen
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 09.10.2006 | LUT | Erstellung (basierend auf AbsenderAuswaehlen.java)
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.sun.org.apache.xerces.internal.impl.xs.opti.DefaultNode;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.Verfuegungspunkt;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen
 * Dialogbeschreibung einen Dialog zum Drucken von Sachleitenden Verfügungen.
 * Die private-Funktionen dürfen NUR aus dem Event-Dispatching Thread heraus
 * aufgerufen werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
 */
public class SachleitendeVerfuegungenDruckdialog
{
  /**
   * Gibt an, wie die Personen in den Listen angezeigt werden sollen. %{Spalte}
   * Syntax um entsprechenden Wert des Datensatzes einzufügen.
   */
  private final static String displayTemplate = "%{Nachname}, %{Vorname} (%{Rolle})";

  /**
   * Standardbreite für Textfelder
   */
  // private final static int TEXTFIELD_DEFAULT_WIDTH = 22;
  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in
   * Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * ActionListener für Buttons mit der ACTION "printElement".
   */
  private ActionListener actionListener_printElement = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      printElement();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "printAll".
   */
  private ActionListener actionListener_printAll = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      printAll();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "printSettings".
   */
  private ActionListener actionListener_printSettings = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      printSettings();
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
   * Die Array mit allen comboBoxen, die elementCount beinhalten.
   */
  private JSpinner[] elementCountSpinner;

  /**
   * Die Array mit allen comboBoxen, die verfügungspunkte+zuleitungszeilen
   * beinhalten.
   */
  private JComboBox[] elementComboBoxes;

  /**
   * Die Array mit allen buttons auf printElement-Actions
   */
  private JButton[] printElementButtons;

  /**
   * Der dem
   * {@link #AbsenderAuswaehlen(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener) Konstruktor}
   * übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Vector of Verfuegungspunkt, der die Beschreibungen der Verfügungspunkte
   * enthält.
   */
  private Vector verfuegungspunkte;

  /**
   * Überwacht Änderungen in der Auswahl und wählt den entsprechenden Datensatz
   * im DJ.
   */
  private MyListSelectionListener myListSelectionListener = new MyListSelectionListener();

  /**
   * Erzeugt einen neuen Dialog.
   * 
   * @param conf
   *          das ConfigThingy, das den Dialog beschreibt (der Vater des
   *          "Fenster"-Knotens.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der
   *          Dialog geschlossen wurde. Das actionCommand des ActionEvents gibt
   *          die Aktion an, die das Beenden des Dialogs veranlasst hat.
   * @param verfuegungspunkte
   *          Vector of Verfuegungspunkt, der die Beschreibungen der
   *          Verfügungspunkte enthält.
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem
   *           Dialog unmöglich macht, zu funktionieren (z.B. dass der "Fenster"
   *           Schlüssel fehlt.
   */
  public SachleitendeVerfuegungenDruckdialog(ConfigThingy conf,
      Vector /* of Verfuegungspunkt */verfuegungspunkte,
      ActionListener dialogEndListener) throws ConfigurationErrorException
  {
    this.verfuegungspunkte = verfuegungspunkte;
    this.dialogEndListener = dialogEndListener;

    ConfigThingy fensterDesc1 = conf.query("Fenster");
    if (fensterDesc1.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "
                                            + conf.getName());

    final ConfigThingy fensterDesc = fensterDesc1.query("Drucken");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Drucken' fehlt in "
                                            + conf.getName());

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
          {
          }
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
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  private void createGUI(ConfigThingy fensterDesc)
  {
    Common.setLookAndFeelOnce();

    int size = verfuegungspunkte.size();

    // element
    elementComboBoxes = new JComboBox[size];
    elementCountSpinner = new JSpinner[size];
    printElementButtons = new JButton[size];

    for (int i = 0; i < size; ++i)
    {
      Verfuegungspunkt verfPunkt = (Verfuegungspunkt) verfuegungspunkte.get(i);
      Vector zuleitungszeilen = verfPunkt.getZuleitungszeilen();

      // elementComboBoxes vorbelegen:
      Vector content = new Vector();
      content.add(verfPunkt.getHeading());
      Iterator iter = verfPunkt.getZuleitungszeilen().iterator();
      while (iter.hasNext())
      {
        String zuleitung = (String) iter.next();
        content.add(zuleitung);
      }
      elementComboBoxes[i] = new JComboBox(content);

      // elementCountComboBoxes vorbelegen:
      SpinnerNumberModel model = new SpinnerNumberModel(
          zuleitungszeilen.size(), 0, 50, 1);
      elementCountSpinner[i] = new JSpinner(model);

      // printElementButtons vorbelegen:
      printElementButtons[i] = new JButton();
    }

    String title = "TITLE fehlt für Fenster Drucken";
    try
    {
      title = fensterDesc.get("TITLE").toString();
    }
    catch (Exception x)
    {
    }

    try
    {
      closeAction = getAction(fensterDesc.get("CLOSEACTION").toString());
    }
    catch (Exception x)
    {
    }

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());

    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myFrame.getContentPane().add(mainPanel);

    JPanel verfPunktPanel = new JPanel(new GridBagLayout());
    JPanel allElements = new JPanel(new GridBagLayout());
    JPanel buttons = new JPanel(new GridBagLayout());

    mainPanel.add(verfPunktPanel, BorderLayout.NORTH);
    mainPanel.add(allElements, BorderLayout.CENTER);
    mainPanel.add(buttons, BorderLayout.PAGE_END);

    for (int i = 0; i < size; i++)
    {
      addUIElements(fensterDesc, "Verfuegungspunkt", i, verfPunktPanel, 1, 0);
    }
    addUIElements(fensterDesc, "AllElements", size, verfPunktPanel, 1, 0);
    addUIElements(fensterDesc, "Buttons", 0, buttons, 1, 0);

    myFrame.pack();
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

  /**
   * Fügt compo UI Elemente gemäss den Kindern von conf.query(key) hinzu. compo
   * muss ein GridBagLayout haben. stepx und stepy geben an um wieviel mit jedem
   * UI Element die x und die y Koordinate der Zelle erhöht werden soll.
   * Wirklich sinnvoll sind hier nur (0,1) und (1,0).
   */
  private void addUIElements(ConfigThingy conf, String key, int verfPunktNr,
      JComponent compo, int stepx, int stepy)
  {
    // int gridx, int gridy, int gridwidth, int gridheight, double weightx,
    // double weighty, int anchor, int fill, Insets insets, int ipadx, int
    // ipady)
    // GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0,
    // 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new
    // Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcGlue = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
        GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0,
            0, 0, 0), 0, 0);
    GridBagConstraints gbcButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(
            BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    GridBagConstraints gbcComboBox = new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcTextField = new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcSpinner = new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);

    ConfigThingy felderParent = conf.query(key);
    int y = -stepy + verfPunktNr;
    int x = -stepx;

    Iterator piter = felderParent.iterator();
    while (piter.hasNext())
    {
      Iterator iter = ((ConfigThingy) piter.next()).iterator();
      while (iter.hasNext())
      {
        y += stepy;
        x += stepx;

        ConfigThingy uiElementDesc = (ConfigThingy) iter.next();
        try
        {
          /*
           * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN, DASS DER
           * ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET() UND EINER EVTL.
           * DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION NOCH KONSISTENT IST!
           */

          // boolean readonly = false;
          String id = "";
          try
          {
            id = uiElementDesc.get("ID").toString();
          }
          catch (NodeNotFoundException e)
          {
          }
          // try{ if (uiElementDesc.get("READONLY").toString().equals("true"))
          // readonly = true; }catch(NodeNotFoundException e){}
          String type = uiElementDesc.get("TYPE").toString();

          if (type.equals("label"))
          {
            JLabel uiElement = new JLabel();
            gbcLabel.gridx = x;
            gbcLabel.gridy = y;
            compo.add(uiElement, gbcLabel);
            uiElement.setText(uiElementDesc.get("LABEL").toString());
          }

          else if (type.equals("glue"))
          {
            Box uiElement = Box.createHorizontalBox();
            try
            {
              int minsize = Integer.parseInt(uiElementDesc.get("MINSIZE")
                  .toString());
              uiElement.add(Box.createHorizontalStrut(minsize));
            }
            catch (Exception e)
            {
            }
            uiElement.add(Box.createHorizontalGlue());

            gbcGlue.gridx = x;
            gbcGlue.gridy = y;
            compo.add(uiElement, gbcGlue);
          }

          else if (type.equals("spinner"))
          {
            JSpinner spinner;
            if (id.equals("elementCount")
                && verfPunktNr < elementCountSpinner.length)
              spinner = elementCountSpinner[verfPunktNr];
            else
              spinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 0));

            // comboBox.addListSelectionListener(myListSelectionListener);

            // String action = "";
            // try{ action = uiElementDesc.get("ACTION").toString();
            // }catch(NodeNotFoundException e){}
            //            
            // ActionListener actionL = getAction(action);
            // if (actionL != null) comboBox.addMouseListener(new
            // MyActionMouseListener(comboBox, actionL));

            gbcSpinner.gridx = x;
            gbcSpinner.gridy = y;
            compo.add(spinner, gbcSpinner);
          }

          else if (type.equals("combobox"))
          {
            JComboBox comboBox;
            if (id.equals("element") && verfPunktNr < elementComboBoxes.length)
              comboBox = elementComboBoxes[verfPunktNr];
            else
              comboBox = new JComboBox();

            // comboBox.addListSelectionListener(myListSelectionListener);

            gbcComboBox.gridx = x;
            gbcComboBox.gridy = y;
            compo.add(comboBox, gbcComboBox);
          }

          else if (type.equals("textfield"))
          {
            JTextField textField;
            if (id.equals("allElementCount"))
            {
              textField = new JTextField("" + getAllElementCount());
              textField.setEditable(false);
            }
            else
              textField = new JTextField();

            gbcTextField.gridx = x;
            gbcTextField.gridy = y;
            compo.add(textField, gbcTextField);
          }

          else if (type.equals("button"))
          {
            String action = "";
            try
            {
              action = uiElementDesc.get("ACTION").toString();
            }
            catch (NodeNotFoundException e)
            {
            }

            String label = uiElementDesc.get("LABEL").toString();

            char hotkey = 0;
            try
            {
              hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
            }
            catch (Exception e)
            {
            }

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
            Logger.error("Ununterstützter TYPE für User Interface Element: "
                         + type);
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
   * TODO: berechnung von allElementCount
   * 
   * @return
   */
  private int getAllElementCount()
  {
    int count = 0;
    for (int i = 0; i < elementCountSpinner.length; i++)
    {
      count += new Integer(elementCountSpinner[i].getValue().toString())
          .intValue();
    }
    return count;
  }

  /**
   * Wartet auf Doppelklick und führt dann die actionPerformed() Methode eines
   * ActionListeners aus.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyActionMouseListener extends MouseAdapter
  {
    private JList list;

    private ActionListener action;

    public MyActionMouseListener(JList list, ActionListener action)
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
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
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
    else if (action.equals("printElement"))
    {
      return actionListener_printElement;
    }
    else if (action.equals("printAll"))
    {
      return actionListener_printAll;
    }
    else if (action.equals("printSettings"))
    {
      return actionListener_printSettings;
    }
    else if (action.equals(""))
    {
      return null;
    }
    else
      Logger.error("Ununterstützte ACTION: " + action);

    return null;
  }

  /**
   * Nimmt eine JList list, die ein DefaultListModel haben muss und ändert ihre
   * Wertliste so, dass sie data entspricht. Die Datasets aus data werden nicht
   * direkt als Werte verwendet, sondern in {@link ListElement} Objekte
   * gewrappt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList list, QueryResults data)
  {
    Object[] elements = new Object[data.size()];
    Iterator iter = data.iterator();
    int i = 0;
    while (iter.hasNext())
      elements[i++] = new ListElement((DJDataset) iter.next());
    Arrays.sort(elements, new Comparator()
    {
      public int compare(Object o1, Object o2)
      {
        return o1.toString().compareTo(o2.toString());
      }
    });

    DefaultListModel listModel = (DefaultListModel) list.getModel();
    listModel.clear();
    for (i = 0; i < elements.length; ++i)
      listModel.addElement(elements[i]);
  }

  private void selectSelectedDataset(JList list)
  {
    DefaultListModel listModel = (DefaultListModel) list.getModel();
    for (int i = 0; i < listModel.size(); ++i)
      if (((ListElement) listModel.get(i)).getDataset().isSelectedDataset())
        list.setSelectedValue(listModel.get(i), true);
  }

  /**
   * Liefert zu einem Datensatz den in einer Listbox anzuzeigenden String.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String getDisplayString(DJDataset ds)
  {
    return substituteVars(displayTemplate, ds);
  }

  /**
   * Wrapper um ein DJDataset zum Einfügen in eine JList. Die
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class ListElement
  {
    private String displayString;

    private DJDataset ds;

    public ListElement(DJDataset ds)
    {
      displayString = getDisplayString(ds);
      this.ds = ds;
    }

    public String toString()
    {
      return displayString;
    }

    public DJDataset getDataset()
    {
      return ds;
    }
  }

  /**
   * Ersetzt "%{SPALTENNAME}" in str durch den Wert der entsprechenden Spalte im
   * datensatz.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String substituteVars(String str, Dataset datensatz)
  {
    Pattern p = Pattern.compile("%\\{([a-zA-Z0-9]+)\\}");
    Matcher m = p.matcher(str);
    if (m.find()) do
    {
      String spalte = m.group(1);
      String wert = spalte;
      try
      {
        String wert2 = datensatz.get(spalte);
        if (wert2 != null) wert = wert2.replaceAll("%", "");
      }
      catch (ColumnNotFoundException e)
      {
        Logger.error(e);
      }
      str = str.substring(0, m.start()) + wert + str.substring(m.end());
      m = p.matcher(str);
    } while (m.find());
    return str;
  }

  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden
   * wenn kein Eintrag selektiert ist.
   */
  private class MyListSelectionListener implements ListSelectionListener
  {
    public void valueChanged(ListSelectionEvent e)
    {
      JList list = (JList) e.getSource();
      // if (list != palJList) return;

      ListElement ele = (ListElement) list.getSelectedValue();
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
      dialogEndListener
          .actionPerformed(new ActionEvent(this, 0, actionCommand));
  }

  /**
   * TODO: doc printSettings
   * 
   * @author christoph.lutz
   */
  private void printSettings()
  {
    // TODO Auto-generated method stub

  }

  /**
   * TODO: doc printAll
   * 
   * @author christoph.lutz
   */
  private void printAll()
  {
    // TODO Auto-generated method stub

  }

  /**
   * TODO: doc printElement()
   * 
   * @author christoph.lutz
   */
  private void printElement()
  {
    // TODO Auto-generated method stub

  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als Reaktion
   * auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener()
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
  }

  /**
   * Zerstört den Dialog. Nach Aufruf dieser Funktion dürfen keine weiteren
   * Aufrufe von Methoden des Dialogs erfolgen. Die Verarbeitung erfolgt
   * asynchron. Wurde dem Konstruktor ein entsprechender ActionListener
   * übergeben, so wird seine actionPerformed() Funktion aufgerufen.
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
    {/* Hope for the best */
    }
  }

  // /**
  // * Sorgt für das dauernde Neustarten des Dialogs.
  // * @author Matthias Benkmann (D-III-ITD 5.1)
  // */
  // private static class RunTest implements ActionListener
  // {
  // private DatasourceJoiner dj;
  // private ConfigThingy conf;
  // private ConfigThingy verConf;
  // private ConfigThingy abConf;
  //    
  // public RunTest(ConfigThingy conf, ConfigThingy verConf, ConfigThingy
  // abConf, DatasourceJoiner dj)
  // {
  // this.dj = dj;
  // this.conf = conf;
  // this.abConf = abConf;
  // this.verConf = verConf;
  // }
  //    
  // public void actionPerformed(ActionEvent e)
  // {
  // try{
  // try{
  // if (e.getActionCommand().equals("abort")) System.exit(0);
  // }catch(Exception x){}
  // new SachleitendeVerfuegungenDruckdialog(conf, this);
  // } catch(ConfigurationErrorException x)
  // {
  // Logger.error(x);
  // }
  // }
  // }
  //  
  // public static void main(String[] args) throws Exception
  // {
  // LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
  // for (int i = 0; i < lf.length; ++i)
  // System.out.println(lf[i].getClassName());
  // System.out.println("Default L&F:
  // "+UIManager.getSystemLookAndFeelClassName());
  // String confFile = "testdata/WhoAmI.conf";
  // String verConfFile = "testdata/PAL.conf";
  // String abConfFile = "testdata/AbsenderdatenBearbeiten.conf";
  // ConfigThingy conf = new ConfigThingy("",new URL(new
  // File(System.getProperty("user.dir")).toURL(),confFile));
  // ConfigThingy verConf = new ConfigThingy("",new URL(new
  // File(System.getProperty("user.dir")).toURL(),verConfFile));
  // ConfigThingy abConf = new ConfigThingy("",new URL(new
  // File(System.getProperty("user.dir")).toURL(),abConfFile));
  // TestDatasourceJoiner dj = new TestDatasourceJoiner();
  // RunTest test = new RunTest(conf.get("AbsenderAuswaehlen"),
  // verConf.get("PersoenlicheAbsenderliste"),
  // abConf.get("AbsenderdatenBearbeiten"), dj);
  // test.actionPerformed(null);
  // Thread.sleep(600000);
  // System.exit(0);
  // }

}
