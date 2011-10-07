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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.dialog.NonNumericKeyConsumer;
import de.muenchen.allg.itd51.wollmux.dialog.TextComponentTags;

/**
 * Dialoge zur Bestimmung der Parameter für den wirklichen Merge (z.B. ob in
 * Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
class MailMergeParams
{
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
   * URL der Konfiguration der Fallback-Konfiguration für den Abschnitt
   * Dialoge/Seriendruckdialog, falls dieser Abschnitt nicht in der
   * WollMux-Konfiguration definiert wurde.
   * 
   * Dieser Fallback wurde eingebaut, um mit alten WollMux-Standard-Configs
   * kompatibel zu bleiben, sollte nach ausreichend Zeit aber wieder entfernt werden!
   */
  private final URL DEFAULT_MAILMERGEDIALOG_URL =
    this.getClass().getClassLoader().getResource("data/seriendruckdialog.conf");

  // TODO: diese enum löschen.
  enum MailMergeType {
    MULTI_FILE,
    SINGLE_FILE;

    String[] requiredPrintFunctions()
    {
      return new String[] {};
    }
  }

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  enum DatasetSelectionType {
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
   * TODO dok
   */
  private MailMergeController mmc;

  /**
   * Der Dialog, der durch {@link #showDoMailmergeDialog(JFrame, MailMergeNew, List)}
   * angezeigt wird. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   */
  private JDialog dialog = null;

  /**
   * TODO: dok
   */
  private ConfigThingy rules;

  /**
   * TODO: dok
   */
  private HashSet<String> visibleGroups = new HashSet<String>();

  /**
   * TODO: dok
   */
  private ArrayList<Section> sections = new ArrayList<Section>();

  /**
   * TODO: dok
   */
  private String currentActionType = "";

  /**
   * TODO: dok
   */
  private String currentOutput = "";

  /**
   * TODO dok
   */
  private ArrayList<JTextComponent> descriptionFields =
    new ArrayList<JTextComponent>();

  /**
   * TODO dok
   */
  private List<String> fieldNames;

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
   * TODO dok
   */
  public String defaultEmailFrom = "";

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
  void showDoMailmergeDialog(final JFrame parent, final MailMergeController mmc,
      List<String> fieldNames)
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

    showDoMailmergeDialog(parent, mmc, fieldNames, sdConf, defaultFrom);
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
  void showDoMailmergeDialog(final JFrame parent, final MailMergeController mmc,
      List<String> fieldNames, ConfigThingy dialogConf, String defaultEmailFrom)
  {
    this.mmc = mmc;
    this.fieldNames = fieldNames;
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
     * TODO: comment Section.createSection
     * 
     * @param section
     * @param compo
     * @param gm
     * @return
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
     * TODO: comment Section.updateVisibility Prüft, ob die Voreinstellungen unter
     * Berücksichtigung der aktuellen Verhältnisse bezüglich der Sichtbarkeit und
     * Aktiviertheit von UIElementen (derzeit nur die, die {@link HasRadioElement}
     * implementieren) zugänglich sind und setzt ggf. die Default-Einstellungen um.
     * 
     * @return
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
   * TODO: comment MailMergeParams.updateVisibilities
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
   * TODO: comment MailMergeParams.processRules
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
   * TODO: comment MailMergeParams.createUIElement
   * 
   * @param type
   * @param label
   * @param labelFrom
   * @param labelTo
   * @param action
   * @param value
   * @param group
   * @return
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
        return new UIElement(new JLabel(label), group, mmp);

      case radio:
        return new RadioButtonUIElement(label, action, value, group, mmp);

      case description:
        JTextArea tf = new JTextArea(3, 50);
        tf.setEditable(false);
        tf.setFocusable(false);
        // tf.setLineWrap(true);
        DimAdjust.fixedSize(tf);
        tf.setBackground(Color.YELLOW);
        mmp.descriptionFields.add(tf);
        return new UIElement(tf, group, mmp);

      case fromtoradio:
        return new FromToRadioUIElement(labelFrom, labelTo, action, value, group,
          mmp);

      case glue:
        if (orient == Orientation.vertical)
          return new UIElement(Box.createVerticalGlue(), group, mmp);
        else
          return new UIElement(Box.createHorizontalGlue(), group, mmp);

      case button:
        JButton button = new JButton(label);
        button.addActionListener(action.createActionListener(value, mmp));
        return new UIElement(button, group, mmp);

      case targetdirpicker:
        return new TargetDirPickerUIElement(label, action, value, group, mmp);

      case filenametemplatechooser:
        return new FilenameTemplateChooserUIElement(label, action, value, group, mmp);

      case emailtofieldname:
        return new EMailToFieldNameUIElement(label, action, value, group, mmp);

      case emailfrom:
        return new EMailFromUIElement(label, action, value, group, mmp);
    }
    return null;
  }

  /**
   * TODO: comment MailMergeParams
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
     * TODO
     * 
     * @param compo
     * @param group
     * @param mmp
     */
    private UIElement(Component compo, String group, MailMergeParams mmp)
    {
      this.compo = compo;
      this.group = group;
      this.mmp = mmp;
    }

    /**
     * TODO: comment UIElement.updateVisibility
     * 
     * @return
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
     * TODO: comment UIElement.isVisible
     * 
     * @return
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public boolean isVisible()
    {
      return visible;
    }

    /**
     * TODO: comment UIElement.setEnabled
     * 
     * @param enabled
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    public void setEnabled(boolean enabled)
    {
      this.enabled = enabled;
      compo.setEnabled(enabled);
    }

    /**
     * TODO: comment UIElement.isEnabled
     * 
     * @return
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

      private InvalidArgumentException(String msg)
      {
        super(msg);
      }
    }
  }

  /**
   * TODO: comment MailMergeParams
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
   * TODO: comment MailMergeParams
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
   * TODO: comment MailMergeParams
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
   * TODO: comment MailMergeParams
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
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(targetDirectory));
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
   * TODO: comment MailMergeParams
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class EMailFromUIElement extends UIElement
  {
    private final JTextField fromField;

    public EMailFromUIElement(String label, UIElementAction action,
        final String value, String group, final MailMergeParams mmp)
    {
      super(Box.createHorizontalBox(), group, mmp);
      Box hbox = (Box) getCompo();
      hbox.add(new JLabel(label));
      hbox.add(Box.createHorizontalStrut(5));
      this.fromField = new JTextField(mmp.defaultEmailFrom);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(fromField));
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
   * TODO: comment MailMergeParams
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class EMailToFieldNameUIElement extends UIElement
  {
    private final JComboBox toFieldName;

    public EMailToFieldNameUIElement(String label, UIElementAction action,
        final String value, String group, final MailMergeParams mmp)
    {
      super(Box.createHorizontalBox(), group, mmp);
      Box hbox = (Box) getCompo();
      hbox.add(new JLabel(label));
      hbox.add(Box.createHorizontalStrut(5));
      String[] fnames = new String[mmp.fieldNames.size() + 1];
      fnames[0] = L.m("<Bitte auswählen>");
      int i = 1;
      for (String fname : mmp.fieldNames)
        fnames[i++] = fname;
      this.toFieldName = new JComboBox(fnames);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(toFieldName));
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      String to = toFieldName.getSelectedItem().toString();
      if (toFieldName.getSelectedIndex() == 0)
        throw new UIElement.InvalidArgumentException(
          L.m("Sie müssen ein Feld angeben, das die Empfängeradresse enthält!"));

      args.put(SubmitArgument.emailToFieldName, to);
    }
  }

  /**
   * TODO: comment MailMergeParams
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class FilenameTemplateChooserUIElement extends UIElement
  {
    private TextComponentTags textTags;

    public FilenameTemplateChooserUIElement(String label, UIElementAction action,
        final String value, String group, final MailMergeParams mmp)
    {
      super(Box.createVerticalBox(), group, mmp);
      Box vbox = (Box) getCompo();

      final JTextField targetPattern = new JTextField(".odt");
      textTags = new TextComponentTags(targetPattern);
      targetPattern.setCaretPosition(0);
      textTags.insertTag(TAG_DATENSATZNUMMER);

      Box hbox = Box.createHorizontalBox();
      hbox.add(Box.createHorizontalGlue());
      JPotentiallyOverlongPopupMenuButton insertFieldButton =
        new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
          TextComponentTags.makeInsertFieldActions(mmp.fieldNames, textTags));
      hbox.add(insertFieldButton);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(new JPotentiallyOverlongPopupMenuButton(L.m("Spezialfeld"),
        makeSpecialFieldActions(textTags)));
      hbox.add(Box.createHorizontalStrut(6));
      vbox.add(hbox);
      vbox.add(Box.createVerticalStrut(3));

      hbox = Box.createHorizontalBox();
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(targetPattern);
      hbox.add(Box.createHorizontalStrut(5));
      vbox.add(hbox);
    }

    public void addSubmitArgs(Map<SubmitArgument, Object> args)
        throws UIElement.InvalidArgumentException
    {
      if (textTags.getContent().isEmpty())
        throw new UIElement.InvalidArgumentException(
          L.m("Sie müssen ein Dateinamenmuster angeben!"));
      args.put(SubmitArgument.filenameTemplate, textTags.getContent());
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

  /**
   * TODO: comment MailMergeParams
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private enum UIElementType {
    radio,
    label,
    description,
    fromtoradio,
    targetdirpicker,
    filenametemplatechooser,
    emailfrom,
    emailtofieldname,
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
   * TODO: comment MailMergeParams
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private enum UIElementAction {
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
   * TODO: comment MailMergeParams
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
   * TODO: comment MailMergeParams
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
   * TODO: comment MailMergeParams
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static enum SubmitArgument {
    targetDirectory,
    filenameTemplate,
    emailFrom,
    emailToFieldName,
    indexSelection,
  }

  /**
   * TODO: comment MailMergeParams.isActionAvailableInCurrentContext Prüft ob die
   * UIElementAction action mit dem Action-Wert value im aktuellen Kontext ausgeführt
   * werden könnte. Eine Action ist u.A. dann nicht ausführbar, wenn in einem
   * zugehörigen Regeln-Abschnitt eine USE_PRINTFUNCTIONS-Anweisung steht, deren
   * Druckfunktionen nicht verfügbar sind.
   * 
   * @param action
   * @param actionValue
   * @param reasons
   * @return
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
   * TODO: comment MailMergeParams.requiredPrintfunctionsAvailable
   * 
   * @param rule
   * @param reasons
   * @return
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
   * Übernimmt die Aufgabe des Controllers bezüglich dieser Klasse (MailMergeParams),
   * die die View darstellt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public interface MailMergeController
  {
    public boolean hasPrintfunction(String name);

    public void doMailMerge(List<String> usePrintFunctions,
        boolean ignoreDocPrintFuncs, DatasetSelectionType datasetSelectionType,
        Map<SubmitArgument, Object> args);
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
        new ConfigThingy("dialogConf", new URL(
          "file:///home/christoph.lutz/wollmux-standard-config/conf/dialoge.conf"));
      sdConf = sdConf.get("Dialoge").get("Seriendruckdialog");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    final ConfigThingy seriendruckConf = sdConf;
    MailMergeController pfInfo = new MailMergeController()
    {
      public boolean hasPrintfunction(String name)
      {
        String[] funcs =
          new String[] {
            "OOoMailMergeToPrinter", /* "OOoMailMergeToOdtFile", */
            "MailMergeNewSetFormValue", /* "PDFGesamtdokument", */"toBeImplemented",
            "PDFGesamtdokumentOutput" };
        for (String func : funcs)
          if (func.equals(name)) return true;
        return false;
      }

      public void doMailMerge(List<String> usePrintFunctions,
          boolean ignoreDocPrintFuncs, DatasetSelectionType datasetSelectionType,
          Map<SubmitArgument, Object> pmodArgs)
      {
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
    };

    mmp.showDoMailmergeDialog(new JFrame("irgendwas"), pfInfo, fieldNames,
      seriendruckConf, "christoph.lutz@muenchen.de");

    System.exit(0);
  }

}
