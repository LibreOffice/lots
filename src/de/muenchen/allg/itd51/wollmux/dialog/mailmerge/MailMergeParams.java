/*
 * Dateiname: MailMergeParams.java
 * Projekt  : WollMux
 * Funktion : Dialoge zur Bestimmung der Parameter für den wirklichen Merge (z.B. ob in Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * 11.10.2007 | BNK | Erstellung
 * 25.05.2010 | ERT | GUI für PDF-Gesamtdruck
 * 20.12.2010 | ERT | Defaultwerte für Druckdialog von ... bis
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.EventObject;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.dialog.NonNumericKeyConsumer;
import de.muenchen.allg.itd51.wollmux.dialog.PrintParametersDialog;
import de.muenchen.allg.itd51.wollmux.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;

/**
 * Dialoge zur Bestimmung der Parameter für den wirklichen Merge (z.B. ob in
 * Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
class MailMergeParams
{
  /**
   * Übernimmt die Aufgabe des Controllers bezüglich dieser Klasse (MailMergeParams),
   * die die View darstellt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public interface MailMergeController
  {
    /**
     * Gibt Auskunft darüber, ob die Druckfunktion name existiert.
     */
    public boolean hasPrintfunction(String name);

    /**
     * Liefert die Spaltennamen der aktuellen Datenquelle
     */
    public List<String> getColumnNames();

    /**
     * Liefert einen Vorschlag für einen Dateinamen zum Speichern der Einzeldokumente
     * (im Fall von Einzeldokumentdruck und E-Mail versandt), so wie er aus
     * Benutzersicht wahrscheinlich erwünscht ist OHNE Suffix.
     */
    public String getDefaultFilename();

    /**
     * Liefert das Textdokument für das der Seriendruck gestartet werden soll.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public XTextDocument getTextDocument();

    /**
     * Startet den MailMerge
     */
    public void doMailMerge(List<String> usePrintFunctions,
        boolean ignoreDocPrintFuncs, DatasetSelectionType datasetSelectionType,
        Map<SubmitArgument, Object> args);
  }

  /**
   * Zählt alle Schlüsselwörter auf, die Übergabeargumente für
   * {@link MailMergeController#doMailMerge(List, boolean, DatasetSelectionType, Map)}
   * sein können. Jedes UI-Element steuert in {@link UIElement#addSubmitArgs(Map)},
   * ob und welche Argumente es setzt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static enum SubmitArgument {
    targetDirectory,
    filenameTemplate,
    emailFrom,
    emailToFieldName,
    emailText,
    emailSubject,
    indexSelection,
  }

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  public static enum DatasetSelectionType {
    /**
     * Alle Datensätze.
     */
    ALL,

    /**
     * Der durch {@link MailMergeNew#rangeStart} und {@link MailMergeNew#rangeEnd}
     * gegebene Wert.
     */
    RANGE,

    /**
     * Die durch {@link MailMergeNew#selectedIndexes} bestimmten Datensätze.
     */
    INDIVIDUAL;
  }

  public static class IndexSelection
  {
    /**
     * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
     * bestimmt dies den ersten zu druckenden Datensatz (wobei der erste Datensatz
     * die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder größer als
     * {@link #rangeEnd} sein. Dies muss dort behandelt werden, wo er verwendet wird.
     */
    public int rangeStart = 1;

    /**
     * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
     * bestimmt dies den letzten zu druckenden Datensatz (wobei der erste Datensatz
     * die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder kleiner als
     * {@link #rangeStart} sein. Dies muss dort behandelt werden, wo er verwendet
     * wird.
     */
    public int rangeEnd = Integer.MAX_VALUE;

    /**
     * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#INDIVIDUAL}
     * bestimmt dies die Indizes der ausgewählten Datensätze, wobei 1 den ersten
     * Datensatz bezeichnet.
     */
    public List<Integer> selectedIndexes = new Vector<Integer>();

  };

  /**
   * Beschreibt Elementtypen, wie sie im TYPE-Attribut von Einträgen des
   * Seriendruckdialog-Abschnitts der WollMux-Konfiguration verwendet werden können.
   * Der Seriendruckabschnitt definiert im Vergleich zur FormGUI einige
   * Spezial-Typen, die nur im Kontext dieses Dialogs einen Sinn ergeben.
   * 
   * Die Methode {@link #getByname(String)} ermöglicht eine Zuordnung von Strings der
   * Konfigurationsdatei auf den entsprechenden enum-Typen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static enum UIElementType {
    radio,
    label,
    description,
    fromtoradio,
    targetdirpicker,
    filenametemplatechooser,
    emailfrom,
    emailtofieldname,
    emailtext,
    emailsubject,
    printersettings,
    glue,
    button,
    unknown;

    static UIElementType getByname(String s)
    {
      for (UIElementType t : UIElementType.values())
      {
        if (t.toString().equalsIgnoreCase(s)) return t;
      }
      return unknown;
    }
  }

  /**
   * Beschreibt die möglichen Actions, die auf Formularelemente des
   * Seriendruckabschnitts angewendet werden können und deren Event-Handler.
   * 
   * Die Methode {@link #getByname(String)} ermöglicht eine Zuordnung von Strings der
   * Konfigurationsdatei auf den entsprechenden enum-Typen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static enum UIElementAction {
    setActionType() {
      public ActionListener createActionListener(final String value,
          final MailMergeParams mmp)
      {
        return new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            mmp.currentActionType = value;
            mmp.updateView();
          }
        };
      }
    },

    setOutput() {
      public ActionListener createActionListener(final String value,
          final MailMergeParams mmp)
      {
        return new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            mmp.currentOutput = value;
            mmp.updateView();
          }
        };
      }
    },

    selectAll() {
      public ActionListener createActionListener(final String value,
          final MailMergeParams mmp)
      {
        return new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            mmp.datasetSelectionType = DatasetSelectionType.ALL;
          }
        };
      }
    },

    selectRange() {
      public ActionListener createActionListener(final String value,
          final MailMergeParams mmp)
      {
        return new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            mmp.datasetSelectionType = DatasetSelectionType.RANGE;
          }
        };
      }
    },

    abort() {
      public ActionListener createActionListener(final String value,
          final MailMergeParams mmp)
      {
        return new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            mmp.dialog.dispose();
          }
        };
      }
    },

    submit() {
      public ActionListener createActionListener(final String value,
          final MailMergeParams mmp)
      {
        return new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            try
            {
              Map<SubmitArgument, Object> args =
                new HashMap<SubmitArgument, Object>();
              for (Section s : mmp.sections)
                s.addSubmitArgs(args);
              mmp.dialog.dispose();
              boolean ignoreDocPrintFuncs = false;
              if (mmp.ignoreDocPrintFuncs != null && mmp.ignoreDocPrintFuncs == true)
                ignoreDocPrintFuncs = true;
              mmp.mmc.doMailMerge(mmp.usePrintFunctions, ignoreDocPrintFuncs,
                mmp.datasetSelectionType, args);
            }
            catch (UIElement.InvalidArgumentException ex)
            {
              if (ex.getMessage() != null)
                JOptionPane.showMessageDialog(mmp.dialog, ex.getMessage(),
                  L.m("Fehlerhafte Eingabe"), JOptionPane.ERROR_MESSAGE);
            }
          }
        };
      }
    },

    unknown() {
      public ActionListener createActionListener(final String value,
          final MailMergeParams mmp)
      {
        return null;
      }
    };

    static UIElementAction getByname(String s)
    {
      for (UIElementAction a : UIElementAction.values())
      {
        if (a.toString().equalsIgnoreCase(s)) return a;
      }
      return unknown;
    }

    abstract public ActionListener createActionListener(String value,
        MailMergeParams mmp);
  }

  /**
   * Folgende Statements können innerhalb des Regeln-Abschnitts der
   * Seriendruckdialog-Beschreibung angewendet werden. Über sie werden z.B. die
   * Sichtbarkeiten der {@link Section}s und {@link UIElement}e gesteuert und die pro
   * Option zu verwendenden Druckfunktionen spezifiziert.
   * 
   * Die Methode {@link #getByname(String)} ermöglicht eine Zuordnung von Strings der
   * Konfigurationsdatei auf den entsprechenden enum-Typen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static enum RuleStatement {
    ON_ACTION_TYPE,
    ON_OUTPUT,
    SHOW_GROUPS,
    USE_PRINTFUNCTIONS,
    SET_DESCRIPTION,
    IGNORE_DOC_PRINTFUNCTIONS,
    unknown;

    static RuleStatement getByname(String s)
    {
      for (RuleStatement k : RuleStatement.values())
      {
        if (k.toString().equals(s)) return k;
      }
      return unknown;
    }
  }

  /**
   * Beschreibt die Ausrichtung, nach der Formularelemente innerhalb einer
   * {@link Section} ausgerichtet werden können.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static enum Orientation {
    vertical,
    horizontal;

    static Orientation getByname(String s)
    {
      for (Orientation o : Orientation.values())
      {
        if (o.toString().equalsIgnoreCase(s)) return o;
      }
      return vertical;
    }
  }

  /**
   * URL der Konfiguration der Fallback-Konfiguration für den Abschnitt
   * Dialoge/Seriendruckdialog, falls dieser Abschnitt nicht in der
   * WollMux-Konfiguration definiert wurde.
   * 
   * Dieser Fallback wurde eingebaut, um mit alten WollMux-Standard-Configs
   * kompatibel zu bleiben, sollte nach ausreichend Zeit aber wieder entfernt werden!
   */
  private final URL DEFAULT_MAILMERGEDIALOG_URL =
    this.getClass().getClassLoader().getResource("data/seriendruckdialog.conf");

  /**
   * Tag für {@link TextComponentTags}, das als Platzhalter für die Serienbriefnummer
   * steht.
   */
  static final String TAG_SERIENBRIEFNUMMER = "#SB";

  /**
   * Tag für {@link TextComponentTags}, das als Platzhalter für die Datensatznummer
   * steht.
   */
  static final String TAG_DATENSATZNUMMER = "#DS";

  /**
   * Enthält die Hintergrundfarbe des Beschreibungsfeldes im Druckdialog
   */
  static final Color DESC_COLOR = new Color(0xffffc8);

  /**
   * Enthält den {@link MailMergeController}, der Infos zum aktuellen
   * Seriendruckkontext liefern kann und den eigentlichen Seriendruck ausführen kann.
   */
  private MailMergeController mmc;

  /**
   * Der Dialog, der durch {@link #showDoMailmergeDialog(JFrame, MailMergeNew, List)}
   * angezeigt wird. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   */
  private JDialog dialog = null;

  /**
   * Enthält den Regeln-Abschnitt aus der Seriendruckdialog-Beschreibung.
   */
  private ConfigThingy rules;

  /**
   * Enthält alle zum aktuellen Zeitpunkt sichtbaren Gruppen, die über das
   * {@link RuleStatement#SHOW_GROUPS} sichtbar geschaltet wurden.
   */
  private HashSet<String> visibleGroups = new HashSet<String>();

  /**
   * Enthält eine Liste aller erzeugter {@link Section}-Objekte in der Reihenfolge
   * der Seriendruckdialog-Beschreibung.
   */
  private ArrayList<Section> sections = new ArrayList<Section>();

  /**
   * Enthält den String der im Attribut VALUE zur zuletzt ausgeführten
   * {@link UIElementAction#setActionType}-Action angegeben war. Beispiel:
   * 
   * Wird in der GUI das Formularelement '(LABEL "Gesamtdokument erstellen" TYPE
   * "radio" ACTION "setActionType" VALUE "gesamtdok")' ausgewählt, dann enthält
   * diese Variable den Wert "gesamtdok".
   */
  private String currentActionType = "";

  /**
   * Enthält den String der im Attribut VALUE zur zuletzt ausgeführten
   * {@link UIElementAction#setOutput}-Action angegeben war. Beispiel:
   * 
   * Wird in der GUI das Formularelement '(LABEL "ODT-Datei" TYPE "radio" GROUP "odt"
   * ACTION "setOutput" VALUE "odt")' ausgewählt, dann enthält diese Variable den
   * Wert "odt".
   */
  private String currentOutput = "";

  /**
   * Sammelt die JTextComponent-Objekte alle in der Seriendruckdialog-Beschreibung
   * enthaltenen Formularfelder vom Typ {@link UIElementType#description} auf (das
   * ist normalerweise immer nur eins, aber es ist niemand daran gehindert, das
   * Element öfters in den Dialog einzubinden - wenn auch ohne größeren Sinn)
   */
  private ArrayList<JTextComponent> descriptionFields =
    new ArrayList<JTextComponent>();

  /**
   * Enthält die Namen der über das zuletzt ausgeführte
   * {@link RuleStatement#USE_PRINTFUNCTIONS} -Statement gesetzten PrintFunctions.
   */
  private List<String> usePrintFunctions = new ArrayList<String>();

  /**
   * Enthält den Wert des zuletzt ausgeführten
   * {@link RuleStatement#IGNORE_DOC_PRINTFUNCTIONS}-Statements
   */
  private Boolean ignoreDocPrintFuncs;

  /**
   * Enthält den String, der als Vorbelegung im Formularfeld für das
   * Absender-Email-Feld gesetzt wird.
   */
  private String defaultEmailFrom = "";

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   */
  private DatasetSelectionType datasetSelectionType = DatasetSelectionType.ALL;

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues
   * Dokument) anwirft. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   * 
   * @param parent
   *          Elternfenster für den anzuzeigenden Dialog.
   * 
   * @param mmc
   *          Die Methode
   *          {@link MailMergeNew#doMailMerge(de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeType, de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType)}
   *          wird ausgelöst, wenn der Benutzer den Seriendruck startet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void showDoMailmergeDialog(final JFrame parent,
      final MailMergeController mmc)
  {
    ConfigThingy sdConf = null;
    try
    {
      sdConf =
        WollMuxFiles.getWollmuxConf().query("Dialoge").query("Seriendruckdialog").getLastChild();
    }
    catch (NodeNotFoundException e)
    {}
    if (sdConf == null)
    {
      Logger.log(L.m("Kein Abschnitt Dialoge/Seriendruckdialog in der WollMux-Konfiguration "
        + "angegeben! Verwende Default-Konfiguration für den Seriendruckdialog."));
      try
      {
        sdConf =
          new ConfigThingy("Default", DEFAULT_MAILMERGEDIALOG_URL).query("Dialoge").query(
            "Seriendruckdialog").getLastChild();
      }
      catch (Exception e)
      {
        Logger.error(
          L.m(
            "Kann Default-Konfiguration des Seriendruckdialogs nicht aus internem file %1 bestimmen. Dies darf nicht vorkommenen!",
            DEFAULT_MAILMERGEDIALOG_URL), e);
        return;
      }
    }

    String defaultFrom = "";
    try
    {
      ConfigThingy emailConf =
        WollMuxFiles.getWollmuxConf().query("EMailEinstellungen").getLastChild();
      String defaultEmailFromColumnName =
        emailConf.getString("DEFAULT_SENDER_DB_SPALTE", "");
      defaultFrom =
        WollMuxFiles.getDatasourceJoiner().getSelectedDataset().get(
          defaultEmailFromColumnName);
    }
    catch (Exception e)
    {
      Logger.error(
        L.m("Kann Voreinstellung der Absender E-Mailadresse für den Seriendruckdialog nicht bestimmen"),
        e);
    }

    showDoMailmergeDialog(parent, mmc, sdConf, defaultFrom);
  }

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues
   * Dokument) anwirft. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   * 
   * @param parent
   *          Elternfenster für den anzuzeigenden Dialog.
   * 
   * @param mmc
   * @param defaultEmailFrom
   *          Die Methode
   *          {@link MailMergeNew#doMailMerge(de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeType, de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType)}
   *          wird ausgelöst, wenn der Benutzer den Seriendruck startet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   */
  private void showDoMailmergeDialog(final JFrame parent,
      final MailMergeController mmc, ConfigThingy dialogConf, String defaultEmailFrom)
  {
    this.mmc = mmc;
    this.defaultEmailFrom = defaultEmailFrom;

    if (dialog == null || dialog.getParent() != parent)
    {
      String title = L.m("Seriendruck");
      try
      {
        title = dialogConf.get("TITLE").toString();
      }
      catch (NodeNotFoundException e1)
      {}
      dialog = new JDialog(parent, title, true);
      dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

      try
      {
        rules = dialogConf.get("Regeln");
      }
      catch (NodeNotFoundException e2)
      {
        Logger.error(L.m("Dialogbeschreibung für den Seriendruckdialog enthält keinen Abschnitt 'Regeln'"));
        return;
      }

      ConfigThingy fensterConf = new ConfigThingy("");
      try
      {
        fensterConf = dialogConf.get("Fenster");
      }
      catch (NodeNotFoundException e1)
      {}

      Box vbox = Box.createVerticalBox();
      vbox.setBorder(new EmptyBorder(8, 5, 10, 5));
      dialog.add(vbox);

      for (ConfigThingy sectionConf : fensterConf)
        sections.add(new Section(sectionConf, vbox, this));
    }
    updateView();
    dialog.setResizable(false);
    dialog.setVisible(true);
  }

  /**
   * Führt dialog.pack() aus wenn die preferredSize des dialogs die aktuelle Größe
   * überschreitet und platziert den Dialog in die Mitte des Bildschirms.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void repack(JDialog dialog)
  {
    Dimension pref = dialog.getPreferredSize();
    Dimension actual = dialog.getSize();
    if (actual.height < pref.height || actual.width < pref.width)
    {
      dialog.pack();
      int frameWidth = dialog.getWidth();
      int frameHeight = dialog.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width / 2 - frameWidth / 2;
      int y = screenSize.height / 2 - frameHeight / 2;
      dialog.setLocation(x, y);
      dialog.pack(); // manchmal funzt der repaint nicht... warum?
    }
  }

  /**
   * Führt die im Regeln-Abschnitt angegebenen Regeln aus, passt alle Sichtbarkeiten
   * und Vorbelegungen für Radio-Buttons korrekt an und zeichnet den Dialog bei
   * Bedarf neu.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private void updateView()
  {
    processRules();
    for (Section s : sections)
      s.updateView();
    repack(dialog);
  }

  /**
   * Führt die im Regeln-Abschnitt definierten Regeln aus.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private void processRules()
  {
    for (ConfigThingy rule : rules)
    {
      // trifft rule zu?
      boolean matches = true;
      for (ConfigThingy key : rule)
      {
        RuleStatement statement = RuleStatement.getByname(key.getName());
        if (statement == RuleStatement.ON_ACTION_TYPE)
          if (!currentActionType.equals(key.toString())) matches = false;
        if (statement == RuleStatement.ON_OUTPUT)
          if (!currentOutput.equals(key.toString())) matches = false;
      }
      if (!matches) continue;

      // Regel trifft zu. Jetzt die Befehle bearbeiten
      boolean hadUsePrintFunctions = false;
      boolean hadIgnoreDocPrintFunctions = false;

      for (ConfigThingy key : rule)
      {
        RuleStatement k = RuleStatement.getByname(key.getName());
        switch (k)
        {
          case SHOW_GROUPS:
            visibleGroups.clear();
            for (ConfigThingy group : key)
              visibleGroups.add(group.toString());
            break;

          case SET_DESCRIPTION:
            String str = key.toString();
            String[] lines = str.split("\\n");
            for (JTextComponent tf : descriptionFields)
            {
              tf.setText(str);
              if (tf instanceof JTextArea)
              {
                JTextArea ta = (JTextArea) tf;
                if (ta.getRows() < lines.length) ta.setRows(lines.length);
              }
            }
            break;

          case USE_PRINTFUNCTIONS:
            hadUsePrintFunctions = true;
            usePrintFunctions.clear();
            for (ConfigThingy func : key)
              usePrintFunctions.add(func.toString());
            break;

          case IGNORE_DOC_PRINTFUNCTIONS:
            hadIgnoreDocPrintFunctions = true;
            ignoreDocPrintFuncs = Boolean.parseBoolean(key.toString());
            break;
        }
      }

      // implizite Voreinstellung für IGNORE_DOC_PRINTFUNCTIONS, falls nicht in Regel
      // gesetzt:
      if (hadUsePrintFunctions && !hadIgnoreDocPrintFunctions)
        ignoreDocPrintFuncs = null;
    }
  }

  /**
   * Prüft ob die UIElementAction action mit dem Action-Wert value im aktuellen
   * Kontext ausgeführt werden könnte und liefert true zurück, wenn nichts gegen eine
   * Ausführung der Aktion spricht oder false, wenn es Gründe gibt, die eine
   * Ausführung behindern könnten (diese Gründe werden dann als String in die
   * übergebene reasons-Liste aufgenommen). Die Actions setActionType und setOutput
   * sind z.B. dann nicht ausführbar, wenn in einem zugehörigen Regeln-Abschnitt eine
   * USE_PRINTFUNCTIONS-Anweisung steht, deren Druckfunktionen nicht verfügbar sind.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private boolean isActionAvailableInCurrentContext(UIElementAction action,
      String actionValue, List<String> reasons)
  {
    switch (action)
    {
      case selectAll:
        return true;

      case unknown:
        return true;

      case setActionType:
        ConfigThingy myrules =
          rules.queryByChild(RuleStatement.ON_ACTION_TYPE.toString());
        for (ConfigThingy rule : myrules)
        {
          String value =
            rule.getString(RuleStatement.ON_ACTION_TYPE.toString(), null);
          if (!value.equals(actionValue)) continue;
          if (requiredPrintfunctionsAvailable(rule, reasons)) return true;
        }
        return false;

      case setOutput:
        myrules = rules.queryByChild(RuleStatement.ON_OUTPUT.toString());
        for (ConfigThingy rule : myrules)
        {
          String value = rule.getString(RuleStatement.ON_OUTPUT.toString(), null);
          if (!value.equals(actionValue)) continue;
          String actionType =
            rule.getString(RuleStatement.ON_ACTION_TYPE.toString(), null);
          if (actionType == null || !actionType.equals(currentActionType)) continue;
          if (requiredPrintfunctionsAvailable(rule, reasons)) return true;
        }
        return false;
    }
    return false;
  }

  /**
   * Prüft, ob die in einer Regel rule unter dem Schlüssel
   * {@link RuleStatement#USE_PRINTFUNCTIONS} beschriebenen Druckfunktionen
   * ausführbar sind und liefert hängt im Fehlerfall eine textuelle Beschreibung an
   * die übergebene Liste reasons an.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private boolean requiredPrintfunctionsAvailable(ConfigThingy rule,
      List<String> reasons)
  {
    boolean allAvailable = true;
    try
    {
      ConfigThingy usePrintFuncs =
        rule.get(RuleStatement.USE_PRINTFUNCTIONS.toString());
      for (ConfigThingy funcName : usePrintFuncs)
      {
        if (!mmc.hasPrintfunction(funcName.toString()))
        {
          allAvailable = false;
          reasons.add(L.m("Druckfunktion %1 ist nicht verfügbar", funcName));
        }
      }
    }
    catch (NodeNotFoundException e)
    {
      return false;
    }
    return allAvailable;
  }

  /**
   * Fabrik-Methode für die Erzeugung aller {@link UIElement}-Objekte. Die erwarteten
   * String-Argumente können (je nach Formularelement) auch null sein, die anderen
   * Typen müssen mit Objekten != null belegt sein.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static UIElement createUIElement(UIElementType type, String label,
      String labelFrom, String labelTo, UIElementAction action, String value,
      String group, Orientation orient, MailMergeParams mmp, Section section)
  {
    switch (type)
    {
      case label:
      {
        return new UIElement(new JLabel(label), group, mmp);
      }

      case radio:
      {
        return new RadioButtonUIElement(label, action, value, group, mmp);
      }

      case description:
      {
        JTextArea tf = new JTextArea(3, 50);
        tf.setEditable(false);
        tf.setFocusable(false);
        DimAdjust.fixedSize(tf);
        tf.setBackground(DESC_COLOR);
        tf.setBorder(new LineBorder(DESC_COLOR, 4));
        mmp.descriptionFields.add(tf);
        return new UIElement(tf, group, mmp);
      }

      case fromtoradio:
      {
        return new FromToRadioUIElement(labelFrom, labelTo, action, value, group,
          mmp);
      }

      case glue:
      {
        if (orient == Orientation.vertical)
          return new UIElement(Box.createVerticalGlue(), group, mmp);
        else
          return new UIElement(Box.createHorizontalGlue(), group, mmp);
      }

      case button:
      {
        JButton button = new JButton(label);
        button.addActionListener(action.createActionListener(value, mmp));
        return new UIElement(button, group, mmp);
      }

      case targetdirpicker:
      {
        return new TargetDirPickerUIElement(label, action, value, group, mmp);
      }

      case filenametemplatechooser:
      {
        String name = mmp.mmc.getDefaultFilename() + "_.odt";
        JTextField textField = new JTextField(name);
        TextWithDatafieldTagsUIElement el =
          new TextWithDatafieldTagsUIElement(textField, textField,
            SubmitArgument.filenameTemplate,
            L.m("Sie müssen ein Dateinamenmuster angeben!"), group, mmp);
        el.textTags.getJTextComponent().setCaretPosition(name.length() - 4);
        el.textTags.insertTag(TAG_DATENSATZNUMMER);
        return el;
      }

      case emailtext:
      {
        JTextArea tf =
          new JTextArea(
            L.m("Sehr geehrte Damen und Herren,\n\nanbei erhalten Sie ...\n\nMit freundlichen Grüßen\n..."));
        JScrollPane sc = new JScrollPane(tf);
        TextWithDatafieldTagsUIElement el =
          new TextWithDatafieldTagsUIElement(tf, sc, SubmitArgument.emailText, null,
            group, mmp);
        return el;
      }

      case emailsubject:
      {
        return new EMailSubject(label, action, value, group, mmp);
      }

      case emailtofieldname:
      {
        return new EMailToFieldNameUIElement(label, action, value, group, mmp);
      }

      case emailfrom:
      {
        return new EMailFromUIElement(label, action, value, group, mmp);
      }

      case printersettings:
      {
        return new PrinterSettingsUIElement(label, group, mmp);
      }
    }
    return null;
  }

  /**
   * Repräsentiert ein mit {@link UIElementType} beschriebenes Formularelement und
   * kann sichtbar/unsichtbar und aktiviert/nicht aktiviert sein. Außerdem kann es in
   * der Methode {@link UIElement#addSubmitArgs(Map)} vor einem Submit prüfen, ob die
   * Formularinhalte plausibel sind und entsprechend gültige Werte in die
   * Argumentliste aufnehmen oder eine {@link InvalidArgumentException}-Exception
   * werfen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class UIElement
  {
    private Component compo;

    private String group;

    private boolean visible = true;

    private boolean enabled = true;

    private MailMergeParams mmp;

    /**
     * Erzeugt ein UIElement, das über die Hauptkomponente compo dargestellt werden,
     * über die Sichtbarkeitsgruppe group ein-/ausgeblendet werden kann und im
     * Kontext vom {@link MailMergeParams} mmp gültig ist.
     */
    private UIElement(Component compo, String group, MailMergeParams mmp)
    {
      this.compo = compo;
      this.group = group;
      this.mmp = mmp;
    }

    /**
     * Passt die Sichtbarkeit abhängig von den aktuell gesetzten Sichtbarkeitsgruppen
     * aus mmp.visibleGroups an.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void updateView()
    {
      if (group != null)
      {
        visible = mmp.visibleGroups.contains(group);
        compo.setVisible(visible);
      }
    }

    /**
     * Liefert die JComponent dieses UIElements zurück, wobei es sein kann, dass das
     * UIElement aus mehreren JComponents zusammengesetzt ist (in diesem Fall liefert
     * diese Methode nur die oberste JComponent wie z.B. eine HBox zurück).
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public Component getCompo()
    {
      return compo;
    }

    /**
     * Gibt Auskunft, ob das UIElement aktuell sichtbar ist.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public boolean isVisible()
    {
      return visible;
    }

    /**
     * Setzt den Aktiviert-Status des UIElements auf enabled.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void setEnabled(boolean enabled)
    {
      this.enabled = enabled;
      compo.setEnabled(enabled);
    }

    /**
     * Gibt Auskunft, ob das UIElement aktuell aktiviert ist.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public boolean isEnabled()
    {
      return enabled;
    }

    /**
     * Prüft die Benutzereingaben des UIElements und fügt die für eine Submit-Aktion
     * relevanten Benutzereingaben zu der Argumentliste args hinzu, die eine interne
     * Plausibilitätsprüfung bestanden haben. Bei nicht bestandener
     * Plausibilitätsprüfung wird eine {@link InvalidArgumentException} geschmissen.
     * 
     * @throws InvalidArgumentException
     *           Bei nicht bestandener Plausibilitätsprüfung
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws InvalidArgumentException
    {}

    /**
     * wird von {@link UIElement#addSubmitArgs(Map)} geschmissen, wenn die
     * Benutzereingaben unzureichend oder fehlerhaft sind.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private static class InvalidArgumentException extends Exception
    {
      private static final long serialVersionUID = -2091420849047004341L;

      private InvalidArgumentException()
      {
        super(null, null);
      }

      private InvalidArgumentException(String msg)
      {
        super(msg);
      }
    }
  }

  /**
   * Manche UIElemente implementieren dieses Interface um damit anzuzeigen, dass sie
   * eine ButtonGroup zugeordnet werden können und einen Radio-Button enthalten,
   * dessen Vorbelegung bei Ein-/Ausblendungen durch den Seriendruckdialog verwaltet
   * werden sollen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private interface HasRadioElement
  {
    public void setButtonGroup(ButtonGroup g);

    public void setSelected(boolean b);

    public boolean isSelected();
  }

  /**
   * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#radio}.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class RadioButtonUIElement extends UIElement implements
      HasRadioElement
  {
    private JRadioButton radio;

    private UIElementAction action;

    private String actionValue;

    private MailMergeParams mmp;

    public RadioButtonUIElement(String label, UIElementAction action,
        final String value, String group, final MailMergeParams mmp)
    {
      super(Box.createHorizontalBox(), group, mmp);

      // Die hbox benötige ich, da sonst in Kombination mit einem FromToUIElement ein
      // falsches Alignment verwendet wird.
      Box hbox = (Box) getCompo();
      radio = new JRadioButton(label);
      hbox.add(radio);
      hbox.add(Box.createHorizontalGlue());
      ActionListener li = action.createActionListener(value, mmp);
      if (li != null) radio.addActionListener(li);

      this.action = action;
      this.actionValue = value;
      this.mmp = mmp;
    }

    public void setButtonGroup(ButtonGroup g)
    {
      g.add(radio);
    }

    public void setEnabled(boolean enabled)
    {
      super.setEnabled(enabled);
      radio.setEnabled(enabled);
    }

    public void updateView()
    {
      super.updateView();
      if (action == UIElementAction.setActionType
        || action == UIElementAction.setOutput)
      {
        ArrayList<String> reasons = new ArrayList<String>();
        boolean available =
          mmp.isActionAvailableInCurrentContext(action, actionValue, reasons);
        setEnabled(available);
        // Tooltip zur Anzeige der Probleme zusammen bauen
        if (reasons.size() == 0)
          radio.setToolTipText(null);
        else
        {
          String str = "<html>";
          for (String reason : reasons)
            str += "- " + reason + "<br/>";
          str += "</html>";
          radio.setToolTipText(str);
        }
      }
    }

    public boolean isSelected()
    {
      return radio.isSelected();
    }

    public void setSelected(boolean b)
    {
      radio.setSelected(b);
      ActionEvent e = new ActionEvent(this, 0, "setSelected");
      for (ActionListener l : radio.getActionListeners())
        l.actionPerformed(e);
    }
  }

  /**
   * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#fromtoradio}.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class FromToRadioUIElement extends UIElement implements
      HasRadioElement
  {
    private JTextField start;

    private JTextField end;

    private JRadioButton fromRadioButton;

    private MailMergeParams mmp;

    /**
     * Falls {@link DatasetSelectionType} != {@link DatasetSelectionType#ALL}, so
     * bestimmt dies die Indizes der ausgewählten Datensätze.
     */
    private IndexSelection indexSelection = new IndexSelection();

    public FromToRadioUIElement(String labelFrom, String labelTo,
        UIElementAction action, final String value, String group,
        final MailMergeParams mmp)
    {
      super(Box.createHorizontalBox(), group, mmp);
      this.mmp = mmp;

      Box hbox = (Box) getCompo();
      fromRadioButton = new JRadioButton(labelFrom);
      hbox.add(fromRadioButton);
      start = new JTextField(4);
      start.addKeyListener(NonNumericKeyConsumer.instance);
      start.getDocument().addDocumentListener(rangeDocumentListener);
      DimAdjust.fixedSize(start);
      hbox.add(start);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(new JLabel(labelTo));
      hbox.add(Box.createHorizontalStrut(5));
      end = new JTextField(4);
      end.addKeyListener(NonNumericKeyConsumer.instance);
      end.getDocument().addDocumentListener(rangeDocumentListener);
      DimAdjust.fixedSize(end);
      hbox.add(end);
      hbox.add(Box.createHorizontalGlue());
      ActionListener li = action.createActionListener(value, mmp);
      if (li != null) fromRadioButton.addActionListener(li);
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);
    }

    public void setEnabled(boolean enabled)
    {
      super.setEnabled(enabled);
      fromRadioButton.setEnabled(enabled);
    }

    public void setButtonGroup(ButtonGroup g)
    {
      g.add(fromRadioButton);
    }

    public boolean isSelected()
    {
      return fromRadioButton.isSelected();
    }

    public void setSelected(boolean b)
    {
      fromRadioButton.setSelected(b);
      ActionEvent e = new ActionEvent(this, 0, "setSelected");
      for (ActionListener l : fromRadioButton.getActionListeners())
        l.actionPerformed(e);
    }

    private DocumentListener rangeDocumentListener = new DocumentListener()
    {
      public void update()
      {
        fromRadioButton.setSelected(true);
        mmp.datasetSelectionType = DatasetSelectionType.RANGE;
        try
        {
          indexSelection.rangeStart = Integer.parseInt(start.getText());
        }
        catch (Exception x)
        {
          indexSelection.rangeStart = 0;
        }
        try
        {
          indexSelection.rangeEnd = Integer.parseInt(end.getText());
        }
        catch (Exception x)
        {
          indexSelection.rangeEnd = 0;
        }
      }

      public void insertUpdate(DocumentEvent e)
      {
        update();
      }

      public void removeUpdate(DocumentEvent e)
      {
        update();
      }

      public void changedUpdate(DocumentEvent e)
      {
        update();
      }
    };

    public void addSubmitArgs(java.util.Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      args.put(SubmitArgument.indexSelection, indexSelection);
    }
  }

  /**
   * Beschreibt das {@link UIElement} vom Typ
   * {@link UIElementType#filenametemplatechooser}.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class TargetDirPickerUIElement extends UIElement
  {
    private final JTextField targetDirectory;

    public TargetDirPickerUIElement(String label, UIElementAction action,
        final String value, String group, final MailMergeParams mmp)
    {
      super(Box.createHorizontalBox(), group, mmp);
      Box hbox = (Box) getCompo();
      this.targetDirectory = new JTextField();
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(targetDirectory);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(targetDirectory);
      hbox.add(new JButton(new AbstractAction(label)
      {
        private static final long serialVersionUID = -7919862309134895087L;

        public void actionPerformed(ActionEvent e)
        {
          final JFileChooser fc = new JFileChooser(targetDirectory.getText());
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          fc.setMultiSelectionEnabled(false);
          fc.setDialogTitle(L.m("Verzeichnis für die Serienbriefdateien wählen"));
          int ret = fc.showSaveDialog(mmp.dialog);
          if (ret == JFileChooser.APPROVE_OPTION)
            targetDirectory.setText(fc.getSelectedFile().getAbsolutePath());
        }
      }));
      hbox.add(Box.createHorizontalStrut(6));
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      String dir = targetDirectory.getText();
      if (dir.length() == 0)
        throw new UIElement.InvalidArgumentException(
          L.m("Sie müssen ein Zielverzeichnis angeben!"));
      File targetDirFile = new File(dir);
      if (!targetDirFile.isDirectory())
        throw new UIElement.InvalidArgumentException(L.m(
          "%1\n existiert nicht oder ist kein Verzeichnis!", dir));

      args.put(SubmitArgument.targetDirectory, dir);
    }
  }

  /**
   * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#emailfrom}.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class EMailFromUIElement extends UIElement
  {
    private final JTextField fromField;

    public EMailFromUIElement(String label, UIElementAction action,
        final String value, String group, final MailMergeParams mmp)
    {
      super(Box.createVerticalBox(), group, mmp);
      Box vbox = (Box) getCompo();

      Box hbox = Box.createHorizontalBox();
      hbox.add(new JLabel(label));
      hbox.add(Box.createHorizontalStrut(5));
      this.fromField = new JTextField(mmp.defaultEmailFrom);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(fromField);
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

      vbox.add(hbox);
      vbox.add(Box.createVerticalStrut(5));
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      String from = fromField.getText();
      if (from.length() == 0)
        throw new UIElement.InvalidArgumentException(
          L.m("Sie müssen eine Absenderadresse angeben!"));

      args.put(SubmitArgument.emailFrom, from);
    }
  }

  /**
   * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#emailsubject}.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class EMailSubject extends UIElement
  {
    private final JTextField textField;

    private final MailMergeParams mmp;

    public EMailSubject(String label, UIElementAction action, final String value,
        String group, final MailMergeParams mmp)
    {
      super(Box.createVerticalBox(), group, mmp);
      Box vbox = (Box) getCompo();

      Box hbox = Box.createHorizontalBox();
      hbox.add(new JLabel(label));
      hbox.add(Box.createHorizontalStrut(5));
      this.textField = new JTextField("");
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(textField);
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

      vbox.add(hbox);
      vbox.add(Box.createVerticalStrut(5));
      this.mmp = mmp;
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      String subject = textField.getText();
      if (subject.trim().length() == 0)
      {
        int res =
          JOptionPane.showConfirmDialog(mmp.dialog,
            L.m("Ihre Betreffszeile ist leer. Möchten Sie wirklich fortsetzen?"),
            L.m("Betreff fehlt!"), JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.NO_OPTION)
          throw new UIElement.InvalidArgumentException();
      }

      args.put(SubmitArgument.emailSubject, subject);
    }
  }

  /**
   * Beschreibt das {@link UIElement} vom Typ {@link UIElementType#emailtofieldname}.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class EMailToFieldNameUIElement extends UIElement
  {
    private final JComboBox toFieldName;

    public EMailToFieldNameUIElement(String label, UIElementAction action,
        final String value, String group, final MailMergeParams mmp)
    {
      super(Box.createVerticalBox(), group, mmp);
      Box vbox = (Box) getCompo();

      Box hbox = Box.createHorizontalBox();
      hbox.add(new JLabel(label));
      hbox.add(Box.createHorizontalStrut(5));
      String[] fnames = new String[mmp.mmc.getColumnNames().size() + 1];
      fnames[0] = L.m("-- Bitte auswählen --");
      int i = 1;
      int mailIdx = 0;
      for (String fname : mmp.mmc.getColumnNames())
      {
        if (fname.toLowerCase().contains("mail") && mailIdx == 0) mailIdx = i;
        fnames[i++] = "<" + fname + ">";
      }
      this.toFieldName = new JComboBox(fnames);
      toFieldName.setSelectedIndex(mailIdx);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(toFieldName);
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

      vbox.add(hbox);
      vbox.add(Box.createVerticalStrut(5));
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      if (toFieldName.getSelectedIndex() == 0)
        throw new UIElement.InvalidArgumentException(
          L.m("Sie müssen ein Feld angeben, das die Empfängeradresse enthält!"));

      String to = toFieldName.getSelectedItem().toString();
      to = to.substring(1, to.length() - 1);
      args.put(SubmitArgument.emailToFieldName, to);
    }
  }

  /**
   * Beschreibt ein UIElement, das {@link JTextComponent}s über
   * {@link TextComponentTags} editieren kann und derzeit für die Elemente vom Typ
   * {@link UIElementType#emailtext} und
   * {@link UIElementType#filenametemplatechooser} verwendet wird.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class TextWithDatafieldTagsUIElement extends UIElement
  {
    private TextComponentTags textTags;

    private String errorMessageIfEmpty;

    private SubmitArgument argKey;

    public TextWithDatafieldTagsUIElement(JTextComponent textCompo,
        JComponent toAdd, SubmitArgument argKey, String errorMessageIfEmpty,
        String group, final MailMergeParams mmp)
    {
      super(Box.createVerticalBox(), group, mmp);
      Box vbox = (Box) getCompo();

      this.textTags = new TextComponentTags(textCompo);

      Box hbox = Box.createHorizontalBox();
      hbox.add(Box.createHorizontalGlue());
      JPotentiallyOverlongPopupMenuButton insertFieldButton =
        new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
          TextComponentTags.makeInsertFieldActions(mmp.mmc.getColumnNames(),
            textTags));
      hbox.add(insertFieldButton);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(new JPotentiallyOverlongPopupMenuButton(L.m("Spezialfeld"),
        makeSpecialFieldActions(textTags)));
      hbox.add(Box.createHorizontalStrut(6));
      vbox.add(hbox);
      vbox.add(Box.createVerticalStrut(3));

      hbox = Box.createHorizontalBox();
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(toAdd);
      hbox.add(Box.createHorizontalStrut(5));
      vbox.add(hbox);
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(vbox);

      this.errorMessageIfEmpty = errorMessageIfEmpty;
      this.argKey = argKey;
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      if (errorMessageIfEmpty != null && textTags.getContent().isEmpty())
        throw new UIElement.InvalidArgumentException(errorMessageIfEmpty);
      args.put(argKey, textTags);
    }

    /**
     * Erzeugt eine Liste von Actions zum Einfügen von Spezialfeld-Tags in tags.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    private List<Action> makeSpecialFieldActions(final TextComponentTags tags)
    {
      List<Action> actions = new Vector<Action>();
      actions.add(new AbstractAction(L.m("Datensatznummer"))
      {
        private static final long serialVersionUID = 2675809156807460816L;

        public void actionPerformed(ActionEvent e)
        {
          tags.insertTag(TAG_DATENSATZNUMMER);
        }
      });
      actions.add(new AbstractAction(L.m("Serienbriefnummer"))
      {
        private static final long serialVersionUID = 3779132684393223573L;

        public void actionPerformed(ActionEvent e)
        {
          tags.insertTag(TAG_SERIENBRIEFNUMMER);
        }
      });
      return actions;
    }
  }

  private static class PrinterSettingsUIElement extends UIElement
  {
    MailMergeParams mmp;

    JTextField printerNameField;

    public PrinterSettingsUIElement(String label, String group,
        final MailMergeParams mmp)
    {
      super(Box.createHorizontalBox(), group, mmp);
      this.mmp = mmp;

      Box hbox = (Box) getCompo();
      printerNameField = new JTextField();
      printerNameField.setEditable(false);
      printerNameField.setFocusable(false);
      // DimAdjust.maxHeightIsPrefMaxWidthUnlimited(printerNameField);
      hbox.add(new JLabel(label));
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(printerNameField);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(new JButton(new AbstractAction(L.m("Drucker wechseln/einrichten..."))
      {
        private static final long serialVersionUID = 1L;

        public void actionPerformed(ActionEvent e)
        {
          showPrinterSettings();
        }
      }));
      hbox.add(Box.createHorizontalStrut(6));
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);

      updateCurrentPrinter();
    }

    private void updateCurrentPrinter()
    {
      printerNameField.setText(PrintParametersDialog.getCurrentPrinterName(mmp.mmc.getTextDocument()));
    }

    private void showPrinterSettings()
    {
      mmp.dialog.setVisible(false);
      Thread t = new Thread()
      {
        public void run()
        {
          try
          {
            com.sun.star.util.URL url =
              UNO.getParsedUNOUrl(Dispatch.DISP_unoPrinterSetup);
            XNotifyingDispatch disp =
              UNO.XNotifyingDispatch(WollMuxSingleton.getDispatchForModel(
                UNO.XModel(mmp.mmc.getTextDocument()), url));

            if (disp != null)
            {
              disp.dispatchWithNotification(url, new PropertyValue[] {},
                new XDispatchResultListener()
                {
                  public void disposing(EventObject arg0)
                  {}

                  public void dispatchFinished(DispatchResultEvent arg0)
                  {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                      public void run()
                      {
                        updateCurrentPrinter();
                        mmp.dialog.setVisible(true);
                      }
                    });
                  }
                });
            }
          }
          catch (java.lang.Exception e)
          {
            Logger.error(e);
          }
        }
      };
      t.setDaemon(false);
      t.start();
    }
  }

  /**
   * Beschreibt eine Section im Seriendruckdialog (z.B. "Aktionen" oder "Output") und
   * enthält UIElemente. Sind alle UIElement dieser Section unsichtbar, so ist auch
   * die Sektion selbst unsichtbar. Besitzt die Section einen TITLE ungleich null
   * oder Leerstring, so wird die Section mit einer TitledBorder verziert, ansonsten
   * nicht. Radio-Buttons erhalten innerhalb einer Section die selbe ButtonGroup,
   * weshalb für jede neue Gruppe eine neue Section erstellt werden muss.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class Section
  {
    List<UIElement> elements = new ArrayList<UIElement>();

    Box contentBox;

    boolean visible;

    /**
     * Erzeugt die Section, die über das ConfigThingy section beschrieben ist, in der
     * JComponent parent angezeigt werden soll und im Kontext des
     * MailMergeParams-Objekts mmp gültig ist.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private Section(ConfigThingy section, JComponent parent, MailMergeParams mmp)
    {
      String title = "";
      try
      {
        title = section.get("TITLE").toString();
      }
      catch (NodeNotFoundException e1)
      {}

      Orientation orient = Orientation.vertical;
      try
      {
        orient = Orientation.getByname(section.get("ORIENTATION").toString());
      }
      catch (NodeNotFoundException e1)
      {}

      Box hbox = Box.createHorizontalBox();
      parent.add(hbox);
      if (orient == Orientation.vertical)
        contentBox = Box.createVerticalBox();
      else
        contentBox = Box.createHorizontalBox();

      if (title.length() > 0)
      {
        Border border =
          BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), title);
        contentBox.setBorder(border);
      }
      else
      {
        if (orient == Orientation.vertical)
          contentBox.add(Box.createVerticalStrut(5));
      }
      hbox.add(contentBox);

      ConfigThingy elementsConf = section.queryByChild("TYPE");
      ButtonGroup buttonGroup = null;
      for (ConfigThingy element : elementsConf)
      {
        String label = element.getString("LABEL", null);
        String labelFrom = element.getString("LABEL_FROM", null);
        String labelTo = element.getString("LABEL_TO", null);
        UIElementType type =
          UIElementType.getByname(element.getString("TYPE", null));
        UIElementAction action =
          UIElementAction.getByname(element.getString("ACTION", null));
        String value = element.getString("VALUE", null);
        String group = element.getString("GROUP", null);
        UIElement uiel =
          createUIElement(type, label, labelFrom, labelTo, action, value, group,
            orient, mmp, this);
        if (uiel != null)
        {
          elements.add(uiel);
          contentBox.add(uiel.getCompo());
          if (uiel instanceof HasRadioElement)
          {
            if (buttonGroup == null) buttonGroup = new ButtonGroup();
            ((HasRadioElement) uiel).setButtonGroup(buttonGroup);
          }
        }
      }
      if (orient == Orientation.vertical)
        contentBox.add(Box.createVerticalStrut(5));
    }

    /**
     * Aktualisiert alle in der Section enthaltenen UIElemente (bezüglich ihrer
     * Sichtbarkeit und Aktiviertheit) und passt ggf. die Voreinstellungen von allen
     * UIElementen an, die {@link HasRadioElement} implementieren.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private void updateView()
    {
      visible = false;
      HasRadioElement firstEnabledRadio = null;
      boolean hasEnabledPreset = false;
      for (UIElement el : elements)
      {
        el.updateView();
        if (el.isVisible()) visible = true;

        // ggf. Voreinstellungen von Radio-Buttons anpassen
        if (!(el instanceof HasRadioElement)) continue;
        HasRadioElement hre = (HasRadioElement) el;
        if (el.isVisible() && el.isEnabled())
        {
          if (firstEnabledRadio == null) firstEnabledRadio = hre;
          if (hre.isSelected()) hasEnabledPreset = true;
        }
      }

      if (!hasEnabledPreset && firstEnabledRadio != null)
        firstEnabledRadio.setSelected(true);

      contentBox.setVisible(visible);
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      for (UIElement el : elements)
      {
        if (el.isVisible() && el.isEnabled()) el.addSubmitArgs(args);
      }
    }
  }

  /**
   * Testmethode
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void main(String[] args)
  {
    final List<String> fieldNames = new ArrayList<String>();
    fieldNames.add("Anrede");
    fieldNames.add("Name");
    fieldNames.add("EMail");
    final MailMergeParams mmp = new MailMergeParams();
    ConfigThingy sdConf = new ConfigThingy("sdConf");
    try
    {
      sdConf =
        new ConfigThingy(
          "dialogConf",
          new URL(
            "file:///home/christoph.lutz/workspace/WollMux/src/data/seriendruckdialog.conf"));
      sdConf = sdConf.get("Dialoge").get("Seriendruckdialog");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    final ConfigThingy seriendruckConf = sdConf;
    MailMergeController mmc = new MailMergeController()
    {
      public boolean hasPrintfunction(String name)
      {
        String[] funcs =
          new String[] {
            "OOoMailMergeToPrinter", "OOoMailMergeToOdtFile", "MailMergeNewToEMail",
            "MailMergeNewSetFormValue", /* "PDFGesamtdokument", */
            "MailMergeNewToSingleODT", "PDFGesamtdokumentOutput" };
        for (String func : funcs)
          if (func.equals(name)) return true;
        return false;
      }

      public void doMailMerge(List<String> usePrintFunctions,
          boolean ignoreDocPrintFuncs, DatasetSelectionType datasetSelectionType,
          Map<SubmitArgument, Object> pmodArgs)
      {
        Logger.init(System.out, Logger.ALL);
        System.out.print("PrintFunctions: ");
        for (String func : usePrintFunctions)
          System.out.print("'" + func + "' ");
        System.out.println("");
        System.out.println("IgnoreDocPrintFuncs: " + ignoreDocPrintFuncs);
        System.out.println("datasetSelectionType: " + datasetSelectionType);
        System.out.println("pmodArgs: ");
        for (Entry<SubmitArgument, Object> en : pmodArgs.entrySet())
          System.out.println("  " + en.getKey() + ": " + en.getValue());
      }

      public List<String> getColumnNames()
      {
        return fieldNames;
      }

      public String getDefaultFilename()
      {
        return "MeinDokument";
      }

      private boolean initialized = false;

      public XTextDocument getTextDocument()
      {
        if (!initialized) try
        {
          initialized = true;
          UNO.init();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        return UNO.XTextDocument(UNO.desktop.getCurrentComponent());
      }
    };

    mmp.showDoMailmergeDialog(new JFrame("irgendwas"), mmc, seriendruckConf,
      "christoph.lutz@muenchen.de");

    try
    {
      Thread.sleep(5000);
    }
    catch (InterruptedException e)
    {}
    System.exit(0);
  }
}
