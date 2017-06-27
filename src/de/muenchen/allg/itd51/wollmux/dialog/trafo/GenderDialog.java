/*
 * Dateiname: GenderDialog.java
 * Projekt  : WollMux
 * Funktion : Erlaubt die Bearbeitung der Funktion eines Gender-Feldes.
 * 
 * Copyright (c) 2008-2017 Landeshauptstadt München
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
 * 21.02.2008 | LUT | Erstellung als GenderDialog
 * -------------------------------------------------------------------
 *
 * Christoph lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;

/**
 * Erlaubt die Bearbeitung der Funktion eines Gender-Feldes.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class GenderDialog extends TrafoDialog
{
  private TrafoDialogParameters params;

  private JDialog myDialog;

  private MyWindowListener oehrchen;

  private JPanel genderPanel;

  private JComboBox<String> cbAnrede;

  private JTextField tfHerr;

  private JTextField tfFrau;

  private JTextField tfSonst;

  public GenderDialog(TrafoDialogParameters params)
  {
    this.params = params;
    if (!params.isValid || params.conf == null || params.fieldNames == null
      || params.fieldNames.size() == 0) throw new IllegalArgumentException();

    ConfigThingy conf = params.conf;

    /*
     * Parsen von conf mit dem Aufbau Func(BIND(FUNCTION "Gender" SET("Anrede", VALUE "<anredeFieldId>")
     * SET("Falls_Anrede_HerrN", "<textHerr>") SET("Falls_Anrede_Frau", "<textFrau>")
     * SET("Falls_sonstige_Anrede", "<textSonst>")))
     */
    try
    {
      String textHerr = null;
      String textFrau = null;
      String textSonst = null;
      String anredeId = null;

      if (conf.count() != 1) stop();
      ConfigThingy bind = conf.getFirstChild();
      if (!bind.getName().equals("BIND")) stop();

      ConfigThingy funcs = bind.query("FUNCTION", 1);
      if (funcs.count() != 1) stop();
      ConfigThingy func = funcs.getLastChild();
      if (!func.toString().equals("Gender")) stop();

      for (ConfigThingy set : bind)
      {
        if (!set.getName().equals("SET") || set.count() != 2) continue;
        String setKey = set.getFirstChild().toString();
        ConfigThingy value = set.getLastChild();

        if (setKey.equals("Anrede") && value.getName().equals("VALUE")
          && value.count() == 1) anredeId = value.toString();
        if (setKey.equals("Falls_Anrede_HerrN") && value.count() == 0)
          textHerr = value.getName();
        if (setKey.equals("Falls_Anrede_Frau") && value.count() == 0)
          textFrau = value.getName();
        if (setKey.equals("Falls_sonstige_Anrede") && value.count() == 0)
          textSonst = value.getName();
      }

      if (anredeId == null || textHerr == null || textFrau == null
        || textSonst == null) stop();

      HashSet<String> uniqueFieldNames = new HashSet<String>(params.fieldNames);
      uniqueFieldNames.add(anredeId);
      List<String> sortedNames = new ArrayList<String>(uniqueFieldNames);
      Collections.sort(sortedNames);

      buildGUI(anredeId, textHerr, textFrau, textSonst, sortedNames);
    }
    catch (NodeNotFoundException e)
    {
      stop();
    }
  }

  /**
   * Schreiberleichterung für throw new IllegalArgumentException();
   * 
   * @throws IllegalArgumentException
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void stop() throws IllegalArgumentException
  {
    throw new IllegalArgumentException();
  }

  /**
   * Baut das genderPanel auf.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void buildGUI(String anredeId, String textHerr, String textFrau,
      String textSonst, final List<String> fieldNames)
  {
    genderPanel = new JPanel(new BorderLayout());
    genderPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createEtchedBorder(),
      L.m("Verschiedene Texte abhängig vom Geschlecht")));
    Box vbox = Box.createVerticalBox();
    vbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 5));
    genderPanel.add(vbox, BorderLayout.CENTER);

    Box hbox;
    JLabel label;
    int maxLabelWidth = 0;
    List<JLabel> labels = new ArrayList<JLabel>();

    hbox = Box.createHorizontalBox();
    label = new JLabel(L.m("Geschlechtsbestimmendes Feld"));
    label.setFont(label.getFont().deriveFont(Font.PLAIN));
    hbox.add(label);
    cbAnrede = new JComboBox<String>(new Vector<String>(fieldNames));
    cbAnrede.setSelectedItem(anredeId);
    cbAnrede.setEditable(false);
    hbox.add(Box.createHorizontalGlue());
    hbox.add(cbAnrede);
    vbox.add(hbox);

    addText(vbox, L.m("(Kann z.B. \"Herr\", \"weibl.\", \"m\", \"w\" enthalten)")
      + "\n ");

    hbox = Box.createHorizontalBox();
    label = new JLabel(L.m("Text weibl."));
    labels.add(label);
    maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
    hbox.add(label);
    tfFrau = new JTextField(textFrau);
    hbox.add(tfFrau);
    vbox.add(hbox);
    addText(vbox, " ");

    hbox = Box.createHorizontalBox();
    label = new JLabel(L.m("Text männl."));
    labels.add(label);
    maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
    hbox.add(label);
    tfHerr = new JTextField(textHerr);
    hbox.add(tfHerr);
    vbox.add(hbox);
    addText(vbox, " ");

    hbox = Box.createHorizontalBox();
    label = new JLabel(L.m("Text sonst."));
    labels.add(label);
    maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
    hbox.add(label);
    tfSonst = new JTextField(textSonst);
    hbox.add(tfSonst);
    vbox.add(hbox);
    addText(vbox, " ");

    // einheitliche Breite für alle Labels vergeben:
    for (Iterator<JLabel> iter = labels.iterator(); iter.hasNext();)
    {
      label = iter.next();
      Dimension d = label.getPreferredSize();
      d.width = maxLabelWidth + 10;
      label.setPreferredSize(d);
    }

    addText(
      vbox,
      L.m("Der Text sonst. wird z.B. verwendet, um bei der Anrede\nvon Firmen \"geehrte Damen und Herren\" einzufügen."));
  }

  /**
   * Fügt der JComponent compo abhängig vom Text ein oder mehrere H-Boxen mit dem
   * Text text hinzu, wobei der Text an Zeilenumbrüchen umgebrochen und linksbündig
   * dargestellt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void addText(JComponent compo, String text)
  {
    String[] split = text.split("\n");
    for (int i = 0; i < split.length; i++)
    {
      Box hbox = Box.createHorizontalBox();
      JLabel label = new JLabel(split[i]);
      label.setFont(label.getFont().deriveFont(Font.PLAIN));
      hbox.add(label);
      hbox.add(Box.createHorizontalGlue());
      compo.add(hbox);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog#getExitStatus()
   */
  public TrafoDialogParameters getExitStatus()
  {
    return params;
  }

  /**
   * Aktualisiert {@link #params},conf anhand des aktuellen Dialogzustandes und
   * setzt params,isValid auf true.
   * 
   */
  private void updateTrafoConf()
  {
    params.conf = new ConfigThingy(params.conf.getName());
    params.conf.addChild(generateGenderTrafoConf(
      cbAnrede.getSelectedItem().toString(), tfHerr.getText(), tfFrau.getText(),
      tfSonst.getText()));
    params.isValid = true;
  }

  /**
   * Fügt {@link #genderPanel} in dialog ein und zeigt ihn an.
   * 
   * @param dialog
   * @author Matthias Benkmann (D-III-ITD D.10), Christoph Lutz (D-III-ITD D.10)
   */
  private void show(String windowTitle, JDialog dialog)
  {
    params.isValid = false; // wird später in updateTrafoConf auf true gesetzt.

    dialog.setAlwaysOnTop(true);
    dialog.setTitle(windowTitle);
    oehrchen = new MyWindowListener();
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(oehrchen);

    JPanel myPanel = new JPanel(new BorderLayout());
    myPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
    dialog.add(myPanel);
    JScrollPane scrollPane = new JScrollPane(genderPanel);
    scrollPane.setBorder(null);
    myPanel.add(scrollPane, BorderLayout.CENTER);
    Box lowerButtons = Box.createHorizontalBox();
    lowerButtons.setBorder(new EmptyBorder(10, 4, 5, 4));
    myPanel.add(lowerButtons, BorderLayout.SOUTH);
    JButton cancel = new JButton(L.m("Abbrechen"));
    cancel.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        abort();
      }
    });
    JButton insert = new JButton(L.m("OK"));
    insert.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        updateTrafoConf();
        abort();
      }
    });
    lowerButtons.add(cancel);
    lowerButtons.add(Box.createHorizontalGlue());
    lowerButtons.add(insert);

    dialog.setVisible(false);
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setResizable(false);
    dialog.setVisible(true);

    this.myDialog = dialog;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog#show(java.lang.String,
   *      java.awt.Dialog)
   */
  public void show(String windowTitle, Dialog owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog#show(java.lang.String,
   *      java.awt.Frame)
   */
  public void show(String windowTitle, Frame owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }

  /**
   * Der Windowlistener, der die Close-Action des "X"-Knopfs abfängt und den Dialog
   * sauber mit abort beendet.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      abort();
    }

    public void windowClosed(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowActivated(WindowEvent e)
    {}

    public void windowDeactivated(WindowEvent e)
    {}
  }

  /**
   * Beendet den Dialog und ruft insbesondere den close-ActionListener der
   * darüberliegenden Anwendung auf.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void abort()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen nötig, damit der Dialog gc'ed werden kann. Die Befehle sorgen dafür,
     * dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager) indirekt über
     * den JFrame den MailMerge kennt.
     */
    if (myDialog != null)
    {
      myDialog.removeWindowListener(oehrchen);
      myDialog.getContentPane().remove(0);
      myDialog.setJMenuBar(null);

      myDialog.dispose();
      myDialog = null;
    }

    if (params.closeAction != null)
      params.closeAction.actionPerformed(new ActionEvent(this, 0, ""));
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog#dispose()
   */
  public void dispose()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            abort();
          }
          catch (Exception x)
          {}
        }
      });
    }
    catch (Exception x)
    {}
  }

  /**
   * Erzeugt ein ConfigThingy mit dem Aufbau BIND(FUNCTION "Gender" SET("Anrede",
   * VALUE "<anredeFieldId>") SET("Falls_Anrede_HerrN", "<textHerr>")
   * SET("Falls_Anrede_Frau", "<textFrau>") SET("Falls_sonstige_Anrede", "<textSonst>"))
   * 
   * @param anredeId
   *          Id des geschlechtsbestimmenden Feldes
   * @param textHerr
   *          Text für Herr
   * @param textFrau
   *          Text für Frau
   * @param textSonst
   *          Text für sonstige Anreden
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static ConfigThingy generateGenderTrafoConf(String anredeId,
      String textHerr, String textFrau, String textSonst)
  {
    ConfigThingy bind = new ConfigThingy("BIND");
    bind.add("FUNCTION").add("Gender");

    ConfigThingy setAnrede = bind.add("SET");
    setAnrede.add("Anrede");
    setAnrede.add("VALUE").add(anredeId);

    ConfigThingy setHerr = bind.add("SET");
    setHerr.add("Falls_Anrede_HerrN");
    setHerr.add(textHerr);

    ConfigThingy setFrau = bind.add("SET");
    setFrau.add("Falls_Anrede_Frau");
    setFrau.add(textFrau);

    ConfigThingy setSonst = bind.add("SET");
    setSonst.add("Falls_sonstige_Anrede");
    setSonst.add(textSonst);

    return bind;
  }

  /**
   * für Tests
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void main(String[] args)
  {
    TrafoDialogParameters params = new TrafoDialogParameters();
    params.closeAction = null;
    params.conf = new ConfigThingy("Func");
    params.conf.addChild(generateGenderTrafoConf("Anrede", "Hallo Herr",
      "Hallo Frau", "Liebe Firma"));
    params.fieldNames = new ArrayList<String>();
    params.fieldNames.add("MyAnrede");

    new GenderDialog(params).show("Test GenderDialog", (Frame) null);
  }
}
