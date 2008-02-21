/*
 * Dateiname: GenderDialog.java
 * Projekt  : WollMux
 * Funktion : Erlaubt die Bearbeitung der Funktion eines Gender-Feldes.
 * 
 * Copyright: Landeshauptstadt München
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
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;

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

  private JComboBox cbAnrede;

  private JTextField tfHerr;

  private JTextField tfFrau;

  private JTextField tfSonst;

  public GenderDialog(TrafoDialogParameters params)
  {
    this.params = params;
    if (!params.isValid || params.conf == null || params.fieldNames == null
        || params.fieldNames.size() == 0) throw new IllegalArgumentException();

    params.isValid = false; // wird später in updateTrafoConf auf true gesetzt.

    ConfigThingy conf = params.conf;

    /*
     * Parsen von conf mit dem Aufbau Func(BIND(FUNCTION "Gender" SET("Anrede",
     * VALUE "<anredeFieldId>") SET("Falls_Anrede_HerrN", "<textHerr>")
     * SET("Falls_Anrede_Frau", "<textFrau>") SET("Falls_sonstige_Anrede", "<textSonst>")))
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

      for (Iterator iter = bind.iterator(); iter.hasNext();)
      {
        ConfigThingy set = (ConfigThingy) iter.next();
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

      buildGUI(anredeId, textHerr, textFrau, textSonst, params.fieldNames);
    }
    catch (NodeNotFoundException e)
    {
      stop();
    }
  }

  private void stop() throws IllegalArgumentException
  {
    throw new IllegalArgumentException();
  }

  private void buildGUI(String anredeId, String textHerr, String textFrau,
      String textSonst, List fieldNames)
  {
    genderPanel = new JPanel();
    Box vbox = Box.createVerticalBox();
    genderPanel.add(vbox);

    cbAnrede = new JComboBox(new Vector(fieldNames));
    cbAnrede.setSelectedItem(anredeId);
    cbAnrede.setEditable(false);
    tfHerr = new JTextField(textHerr);
    tfFrau = new JTextField(textFrau);
    tfSonst = new JTextField(textSonst);
    vbox.add(cbAnrede);
    vbox.add(tfHerr);
    vbox.add(tfFrau);
    vbox.add(tfSonst);
  }

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
   * Fügt {@link #ifThenElsePanel} in dialog ein und zeigt ihn an.
   * 
   * @param dialog
   * @author Matthias Benkmann (D-III-ITD D.10) TESTED
   */
  private void show(String windowTitle, JDialog dialog)
  {
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
    dialog.setVisible(true);

    this.myDialog = dialog;
  }

  public void show(String windowTitle, Dialog owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }

  public void show(String windowTitle, Frame owner)
  {
    if (owner == null)
      show(windowTitle, new JDialog());
    else
      show(windowTitle, new JDialog(owner));
  }

  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
      abort();
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }
  }

  private void abort()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die
     * folgenden 3 Zeilen nötig, damit der Dialog gc'ed werden kann. Die Befehle
     * sorgen dafür, dass kein globales Objekt (wie z.B. der
     * Keyboard-Fokus-Manager) indirekt über den JFrame den MailMerge kennt.
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
          {
          }
        }
      });
    }
    catch (Exception x)
    {
    }
  }

  /**
   * Erzeugt ein ConfigThingy mit dem Aufbau BIND(FUNCTION "Gender"
   * SET("Anrede", VALUE "<anredeFieldId>") SET("Falls_Anrede_HerrN", "<textHerr>")
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
    params.fieldNames = new ArrayList();
    params.fieldNames.add("MyAnrede");

    new GenderDialog(params).show("Test GenderDialog", (Frame) null);
  }
}
