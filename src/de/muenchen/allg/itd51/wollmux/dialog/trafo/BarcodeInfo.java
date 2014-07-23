/*
 * Dateiname: BarcodeInfoDialog.java
 * Projekt  : WollMux
 * Funktion : Erlaubt die Bearbeitung der Funktion eines BarcodeInfo-Feldes.
 * 
 * Copyright (c) Landeshauptstadt München
 * Copyright (c) CIB software GmbH (Alle Änderungen nach Ableitung von GenderDialog.java)
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
 * 16.07.2014 | LUT | Erstellung als BarcodeInfo, abgeleitet von
 *                  | GenderDialog.java
 * -------------------------------------------------------------------
 *
 * Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
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

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.dialog.TextComponentTags;

/**
 * Erlaubt die Bearbeitung eines Barcodeinfo-Feldes.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
 */
public class BarcodeInfo extends TrafoDialog
{
  private TrafoDialogParameters params;

  private JDialog myDialog;

  private MyWindowListener oehrchen;

  private JPanel barcodeInfoPanel;

  private JComboBox<String> cbBarcodeType;

  private TextComponentTags tfBarcodeContent;

  /**
   * Diese KeyValueSyntx wird im Konstruktor (Parser) benötigt um die eingebetteten
   * "Schlüssel-Wertpaare" aus der WM_BARCODEINFO-Funktion zu parsen. Benötigt werden
   * immer 3 Teile.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
   */
  private static enum KeyValueSyntax {
    key,
    value,
    separator
  };

  private static final String[] BARCODE_TYPES = {
    "EAN128", "QR" };

  public BarcodeInfo(TrafoDialogParameters params)
  {
    this.params = params;
    if (!params.isValid || params.conf == null || params.fieldNames == null
      || params.fieldNames.size() == 0) throw new IllegalArgumentException();

    ConfigThingy conf = params.conf;

    /*
     * Parsen von conf mit dem für Barcodeinfos typischen Aufbau: Func( CAT(
     * "BARCODEINFO(", "TYPE """, CAT("QR"), """ ", "CONTENT """, CAT("Content"),
     * """ ", ")" ) )
     */
    try
    {
      String barcodeType = null;
      ConfigThingy barcodeContent = null;

      if (conf.count() != 1) stop();
      ConfigThingy cat = conf.getFirstChild();
      if (!cat.getName().equals("CAT")) stop();

      boolean first = true;
      KeyValueSyntax expect = KeyValueSyntax.key;
      String currentKey = null;
      ConfigThingy currentValue = null;
      for (ConfigThingy el : cat)
      {
        // start-token
        if (first)
        {
          if (el.getName().equals("BARCODEINFO("))
          {
            first = false;
            continue;
          }
          else
            stop();
        }

        // end-token
        if (el.getName().equals(")")) break;

        // extract key-value-pairs
        switch (expect)
        {
          case key:
            currentKey = el.getName();
            // 'TYPE "' --> 'TYPE'
            currentKey = currentKey.substring(0, currentKey.length() - 2);

            if (!("TYPE".equals(currentKey) || "CONTENT".equals(currentKey)))
              stop();
            expect = KeyValueSyntax.value;

            break;

          case value:
            if (!el.getName().equals("CAT")) stop();
            currentValue = el;
            expect = KeyValueSyntax.separator;
            break;

          case separator:
            if (!el.getName().equals("\" ")) stop();

            if ("TYPE".equals(currentKey))
              barcodeType = currentValue.getFirstChild().getName();
            else if ("CONTENT".equals(currentKey)) barcodeContent = currentValue;

            currentKey = null;
            currentValue = null;
            expect = KeyValueSyntax.key;
            break;
        }
      }

      if (barcodeType == null || barcodeContent == null) stop();

      HashSet<String> uniqueFieldNames = new HashSet<String>(params.fieldNames);
      List<String> sortedNames = new ArrayList<String>(uniqueFieldNames);
      Collections.sort(sortedNames);

      buildGUI(barcodeType, barcodeContent, sortedNames);
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
   * @author Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
   */
  private void stop() throws IllegalArgumentException
  {
    throw new IllegalArgumentException();
  }

  /**
   * Baut das BarcodeInfoPanel auf.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
   */
  private void buildGUI(String barcodeType, ConfigThingy barcodeContent,
      final List<String> fieldNames)
  {
    barcodeInfoPanel = new JPanel(new BorderLayout());
    barcodeInfoPanel.setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createEtchedBorder(),
      L.m("Barcode-Informationen")));
    Box vbox = Box.createVerticalBox();
    vbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 2, 5));
    barcodeInfoPanel.add(vbox, BorderLayout.CENTER);

    Box hbox;
    JLabel label;
    int maxLabelWidth = 0;
    List<JLabel> labels = new ArrayList<JLabel>();

    hbox = Box.createHorizontalBox();
    label = new JLabel(L.m("Barcode Typ"));
    label.setFont(label.getFont().deriveFont(Font.PLAIN));
    hbox.add(label);
    cbBarcodeType = new JComboBox<String>(BARCODE_TYPES);
    cbBarcodeType.setSelectedItem(barcodeType);
    cbBarcodeType.setEditable(false);
    hbox.add(Box.createHorizontalGlue());
    hbox.add(cbBarcodeType);
    vbox.add(hbox);
    addText(vbox, " ");

    hbox = Box.createHorizontalBox();
    JTextField textField = new JTextField();
    tfBarcodeContent = new TextComponentTags(textField);
    tfBarcodeContent.setContent(TextComponentTags.CAT_VALUE_SYNTAX, barcodeContent);
    hbox.add(Box.createHorizontalGlue());
    JPotentiallyOverlongPopupMenuButton butt =
      new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
        TextComponentTags.makeInsertFieldActions(fieldNames,
          tfBarcodeContent));
    butt.setFocusable(false);
    hbox.add(butt);
    vbox.add(hbox);
    
    hbox = Box.createHorizontalBox();
    label = new JLabel(L.m("Barcode-Inhalt"));
    label.setFont(label.getFont().deriveFont(Font.PLAIN));
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(10));
    hbox.add(textField);
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
      L.m("Erzeugt aus dem folgenden Bild einen Barcode mit den hier gesetzten Eigenschaften."));
  }

  /**
   * Fügt der JComponent compo abhängig vom Text ein oder mehrere H-Boxen mit dem
   * Text text hinzu, wobei der Text an Zeilenumbrüchen umgebrochen und linksbündig
   * dargestellt wird.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1)
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
   * Aktualisiert {@link #params},conf anhand des aktuellen Dialogzustandes und setzt
   * params,isValid auf true.
   * 
   */
  private void updateTrafoConf()
  {
    params.conf = new ConfigThingy(params.conf.getName());
    params.conf.addChild(generateBarcodeInfoTrafoConf(cbBarcodeType.getSelectedItem().toString(),
      tfBarcodeContent));
    params.isValid = true;
  }

  /**
   * Fügt {@link #barcodeInfoPanel} in dialog ein und zeigt ihn an.
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
    JScrollPane scrollPane = new JScrollPane(barcodeInfoPanel);
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
   * @see
   * de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog#show(java.lang.String,
   * java.awt.Dialog)
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
   * @see
   * de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog#show(java.lang.String,
   * java.awt.Frame)
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
   * @author Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
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
   * @author Christoph Lutz (D-III-ITD 5.1)
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
   * Erzeugt ein ConfigThingy mit dem Aufbau CAT( "BARCODEINFO(", "TYPE """,
   * CAT("QR"), """ ", "CONTENT """, CAT("Content"), """ ", ")" )
   * 
   * 
   * @param barcodeType
   *          Enthält den Barcode-Typen (z.B. "QR", "EAN128", ...)
   * @param barcodeContent
   *          In diesem TextComponentTags kann ein dynamischer Inhalt für den Barcode
   *          übergeben werden. Die enthaltenen Tags werden dann durch VALUE
   *          "<tagname>" Anweisungen ersetzt.
   * 
   * @author Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
   */
  public static ConfigThingy generateBarcodeInfoTrafoConf(String barcodeType,
      TextComponentTags barcodeContent)
  {
    ConfigThingy cat = new ConfigThingy("CAT");
    cat.add("BARCODEINFO(");

    cat.add("TYPE \"");
    ConfigThingy innerCat=new ConfigThingy("CAT");
    cat.addChild(innerCat);
    innerCat.add(barcodeType);
    cat.add("\" ");

    cat.add("CONTENT \"");
    if (barcodeContent != null)
    {
      innerCat = barcodeContent.getContent(TextComponentTags.CAT_VALUE_SYNTAX);
    }
    else
    {
      innerCat=new ConfigThingy("CAT");
      innerCat.add("");
    }
    cat.addChild(innerCat);
    cat.add("\" ");
    
    cat.add(")");
    
    return cat;
  }

  /**
   * für Tests
   * 
   * @author Christoph Lutz (D-III-ITD 5.1), Christoph Lutz (CIB software GmbH)
   */
  public static void main(String[] args)
  {
    TrafoDialogParameters params = new TrafoDialogParameters();
    params.closeAction = null;
    params.conf = new ConfigThingy("Func");
    params.conf.addChild(generateBarcodeInfoTrafoConf("QR", null));
    params.fieldNames = new ArrayList<String>();
    params.fieldNames.add("Inventarnummer");

    new BarcodeInfo(params).show("Test BarcodeInfoDialog", (Frame) null);
  }
}
