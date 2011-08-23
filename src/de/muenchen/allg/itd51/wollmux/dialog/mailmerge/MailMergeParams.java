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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import de.muenchen.allg.itd51.wollmux.L;
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

  enum MailMergeType {
    /**
     * Gesamtdokument erzeugen, das alle Serienbriefe in allen Ausfertigungen
     * enthält.
     */
    OOO_MAILMERGE(L.m("mit dem OOo-Seriendruck erzeugen")) {
      public String[] requiredPrintFunctions()
      {
        return new String[] {
          "MailMergeSimulateSetFormValue", "OOoMailMergeToSingleFile" };
      }
    },

    /**
     * Alle Ausfertigungen in ein großes pdf-Dokument schreiben
     */
    SINGLE_PDF_FILE(L.m("PDF-Gesamtdokument erzeugen")) {
      public String[] requiredPrintFunctions()
      {
        return new String[] {
          "MailMergeNewSetFormValue", "PDFGesamtdokument", "PDFGesamtdokumentOutput" };
      }
    },

    /**
     * Gesamtdokument erzeugen, das alle Serienbriefe in allen Ausfertigungen
     * enthält.
     */
    SINGLE_FILE(L.m("in neues Dokument schreiben")) {
      public String[] requiredPrintFunctions()
      {
        return new String[] {
          "MailMergeNewSetFormValue", "Gesamtdokument" };
      }
    },

    /**
     * Eine Datei pro Serienbrief, wobei jede Datei alle Versionen (bei SLV-Druck)
     * enthält.
     */
    MULTI_FILE(L.m("in einzelne Dateien schreiben")) {
      /**
       * Auch im MULTI_FILE Fall wird die Gesamtdokument-Funktion verwendet,
       * allerdings werden in diesem Fall mehrere Gesamtdokumente erzeugt, eines pro
       * Datensatz (aber z.B. mit allen Versionen beim SLV-Druck im selben Dokument).
       */
      public String[] requiredPrintFunctions()
      {
        return new String[] {
          "MailMergeNewSetFormValue", "Gesamtdokument" };
      }
    },

    /**
     * Direkte Ausgabe auf dem Drucker.
     */
    PRINTER(L.m("auf dem Drucker ausgeben")) {
      public String[] requiredPrintFunctions()
      {
        return new String[] { "MailMergeNewSetFormValue" };
      }
    },

    /**
     * Versenden per E-Mail.
     */
    // EMAIL(L.m("als E-Mails versenden"))
    ;

    /**
     * Label für die Anzeige dieser Option.
     */
    private final String menuLabel;

    MailMergeType(String menuLabel)
    {
      this.menuLabel = menuLabel;
    }

    public String toString()
    {
      return menuLabel;
    }

    /**
     * Liefert in einem String[] die Namen der für die Ausführung des Seriendrucks
     * notwendigen Druckfunktionen (so wie sie in der funktionen.conf definiert sein
     * müssen).
     */
    public String[] requiredPrintFunctions()
    {
      return new String[] {};
    }
  }

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   */
  private DatasetSelectionType datasetSelectionType = DatasetSelectionType.ALL;

  /**
   * Falls {@link DatasetSelectionType} != {@link DatasetSelectionType#ALL}, so
   * bestimmt dies die Indizes der ausgewählten Datensätze.
   */
  private IndexSelection indexSelection = new IndexSelection();

  /**
   * Der Dialog, der durch {@link #showDoMailmergeDialog(JFrame, MailMergeNew, List)}
   * angezeigt wird. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   */
  private JDialog dialog = null;

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues
   * Dokument) anwirft. Bei jedem Aufruf mit dem gleichen parent Frame wird der selbe
   * Dialog verwendet, damit die Vorbelegungen erhalten bleiben.
   * 
   * @param parent
   *          Elternfenster für den anzuzeigenden Dialog.
   * 
   * @param mm
   *          Die Methode
   *          {@link MailMergeNew#doMailMerge(de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeType, de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType)}
   *          wird ausgelöst, wenn der Benutzer den Seriendruck startet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  void showDoMailmergeDialog(final JFrame parent, final MailMergeNew mm,
      List<String> fieldNames)
  {
    if (dialog == null || dialog.getParent() != parent)
    {
      dialog = new JDialog(parent, L.m("Seriendruck"), true);
      dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

      Box vbox = Box.createVerticalBox();
      vbox.setBorder(new EmptyBorder(8, 5, 10, 5));
      dialog.add(vbox);

      Box hbox = Box.createHorizontalBox();
      JLabel label = new JLabel(L.m("Serienbriefe"));
      hbox.add(label);
      hbox.add(Box.createHorizontalStrut(5));

      // Angezeige festlegen: nur verfügbare Druckfunktionen anzeigen
      Vector<MailMergeType> types = new Vector<MailMergeType>();
      for (MailMergeType type : MailMergeType.values())
      {
        boolean functionsAvailable = true;
        for (String f : type.requiredPrintFunctions())
          if (!mm.hasPrintfunction(f)) functionsAvailable = false;
        if (functionsAvailable) types.add(type);
      }
      final JComboBox typeBox = new JComboBox(types);
      hbox.add(typeBox);

      vbox.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox));
      vbox.add(Box.createVerticalStrut(5));

      Box selectBox = Box.createVerticalBox();
      Border border =
        BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
          L.m("Folgende Datensätze verwenden"));
      selectBox.setBorder(border);

      hbox = Box.createHorizontalBox();
      ButtonGroup radioGroup = new ButtonGroup();
      JRadioButton rbutton;
      rbutton = new JRadioButton(L.m("Alle"), true);
      rbutton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          datasetSelectionType = DatasetSelectionType.ALL;
        }
      });
      hbox.add(rbutton);
      hbox.add(Box.createHorizontalGlue());
      radioGroup.add(rbutton);

      selectBox.add(hbox);
      selectBox.add(Box.createVerticalStrut(5));

      hbox = Box.createHorizontalBox();

      final JRadioButton rangebutton = new JRadioButton(L.m("Von"), false);
      rangebutton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          datasetSelectionType = DatasetSelectionType.RANGE;
        }
      });
      hbox.add(rangebutton);
      radioGroup.add(rangebutton);

      final JTextField start = new JTextField(4);
      start.addKeyListener(NonNumericKeyConsumer.instance);
      hbox.add(start);
      hbox.add(Box.createHorizontalStrut(5));
      label = new JLabel("Bis");
      hbox.add(label);
      hbox.add(Box.createHorizontalStrut(5));

      final JTextField end = new JTextField(4);
      end.addKeyListener(NonNumericKeyConsumer.instance);

      DocumentListener rangeDocumentListener = new DocumentListener()
      {
        public void update()
        {
          rangebutton.setSelected(true);
          datasetSelectionType = DatasetSelectionType.RANGE;
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

      Document tfdoc = start.getDocument();
      tfdoc.addDocumentListener(rangeDocumentListener);
      tfdoc = end.getDocument();
      tfdoc.addDocumentListener(rangeDocumentListener);
      hbox.add(end);

      selectBox.add(hbox);
      selectBox.add(Box.createVerticalStrut(5));

      hbox = Box.createHorizontalBox();

      // TODO Anwahl muss selben Effekt haben wie das Drücken des "Einzelauswahl"
      // Buttons
      // final JRadioButton einzelauswahlRadioButton = new JRadioButton("");
      // hbox.add(einzelauswahlRadioButton);
      // radioGroup.add(einzelauswahlRadioButton);
      //
      // ActionListener einzelauswahlActionListener = new ActionListener()
      // {
      // public void actionPerformed(ActionEvent e)
      // {
      // einzelauswahlRadioButton.setSelected(true);
      // datasetSelectionType = DatasetSelectionType.INDIVIDUAL;
      // // TODO showEinzelauswahlDialog();
      // }
      // };
      //
      // einzelauswahlRadioButton.addActionListener(einzelauswahlActionListener);
      //
      // JButton button = new JButton(L.m("Einzelauswahl..."));
      // hbox.add(button);
      // hbox.add(Box.createHorizontalGlue());
      // button.addActionListener(einzelauswahlActionListener);

      selectBox.add(hbox);
      vbox.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(selectBox));
      vbox.add(Box.createVerticalStrut(5));

      final Box multiFileParamsGUI = Box.createVerticalBox();
      hbox = Box.createHorizontalBox();
      hbox.add(new JLabel(L.m("Zielverzeichnis")));
      hbox.add(Box.createHorizontalGlue());
      multiFileParamsGUI.add(hbox);

      hbox = Box.createHorizontalBox();
      final JTextField targetDirectory = new JTextField();
      hbox.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(targetDirectory));
      hbox.add(new JButton(new AbstractAction(L.m("Suchen..."))
      {
        private static final long serialVersionUID = -7919862309134895087L;

        public void actionPerformed(ActionEvent e)
        {
          final JFileChooser fc = new JFileChooser(targetDirectory.getText());
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          fc.setMultiSelectionEnabled(false);
          fc.setDialogTitle(L.m("Verzeichnis für die Serienbriefdateien wählen"));
          int ret = fc.showSaveDialog(dialog);
          if (ret == JFileChooser.APPROVE_OPTION)
            targetDirectory.setText(fc.getSelectedFile().getAbsolutePath());
        }
      }));

      hbox.add(Box.createHorizontalGlue());
      multiFileParamsGUI.add(hbox);

      multiFileParamsGUI.add(Box.createVerticalStrut(5));

      hbox = Box.createHorizontalBox();
      hbox.add(new JLabel(L.m("Dateinamenmuster")));
      final JTextField targetPattern = new JTextField(".odt");
      final TextComponentTags textTags = new TextComponentTags(targetPattern);
      targetPattern.setCaretPosition(0);
      textTags.insertTag(TAG_DATENSATZNUMMER);
      hbox.add(Box.createHorizontalStrut(5));
      JPotentiallyOverlongPopupMenuButton insertFieldButton =
        new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
          TextComponentTags.makeInsertFieldActions(fieldNames, textTags));
      hbox.add(insertFieldButton);
      hbox.add(Box.createHorizontalStrut(5));
      hbox.add(new JPotentiallyOverlongPopupMenuButton(L.m("Spezialfeld"),
        makeSpecialFieldActions(textTags)));
      multiFileParamsGUI.add(hbox);

      multiFileParamsGUI.add(Box.createVerticalStrut(3));

      hbox = Box.createHorizontalBox();
      hbox.add(targetPattern);
      multiFileParamsGUI.add(hbox);

      multiFileParamsGUI.add(Box.createVerticalStrut(5));

      vbox.add(multiFileParamsGUI);
      multiFileParamsGUI.setVisible(false);

      typeBox.addItemListener(new ItemListener()
      {
        public void itemStateChanged(ItemEvent e)
        {
          multiFileParamsGUI.setVisible((MailMergeType) typeBox.getSelectedItem() == MailMergeType.MULTI_FILE);
          dialog.pack();
        }
      });
      vbox.add(Box.createVerticalGlue());
      hbox = Box.createHorizontalBox();
      JButton button = new JButton(L.m("Abbrechen"));
      button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          dialog.dispose();
        }
      });
      hbox.add(button);

      hbox.add(Box.createHorizontalGlue());

      button = new JButton(L.m("Los geht's!"));
      button.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          if (!checkInput(dialog, (MailMergeType) typeBox.getSelectedItem(),
            datasetSelectionType, indexSelection, targetDirectory.getText(),
            textTags)) return;
          dialog.dispose();
          mm.doMailMerge((MailMergeType) typeBox.getSelectedItem(),
            datasetSelectionType, indexSelection, targetDirectory.getText(),
            textTags);
        }
      });
      hbox.add(button);

      vbox.add(hbox);
    }
    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setResizable(false);
    dialog.setVisible(true);
  }

  /**
   * Falls die vom Benutzer gemachten Eingaben anscheinsmäßig korrekt sind, wird true
   * geliefert, ansonsten wird ein modaler Dialog angezeigt mit einer Fehlermeldung
   * und es wird (nach Beendigung des Dialoges) false zurückgeliefert.
   * 
   * @param parent
   *          Elternkomponente für die Anzeige modaler Dialoge
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  private boolean checkInput(Component parent, MailMergeType type,
      DatasetSelectionType datasetSelectionType, IndexSelection indexSelection,
      String dir, TextComponentTags filePattern)
  {
    if (type == MailMergeType.MULTI_FILE)
    {
      if (dir.length() == 0)
      {
        JOptionPane.showMessageDialog(parent,
          L.m("Sie müssen ein Zielverzeichnis angeben!"),
          L.m("Zielverzeichnis fehlt"), JOptionPane.ERROR_MESSAGE);
        return false;
      }

      if (filePattern.getContent().isEmpty())
      {
        JOptionPane.showMessageDialog(parent,
          L.m("Sie müssen ein Dateinamenmuster angeben!"),
          L.m("Dateinamenmuster fehlt"), JOptionPane.ERROR_MESSAGE);
        return false;
      }

      File targetDir = new File(dir);
      if (!targetDir.isDirectory())
      {
        JOptionPane.showMessageDialog(parent, L.m(
          "%1\n existiert nicht oder ist kein Verzeichnis!", dir),
          L.m("Zielverzeichnis fehlerhaft"), JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }
    return true;
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
