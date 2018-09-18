/*
 * Dateiname: MailMergeNew.java
 * Projekt  : WollMux
 * Funktion : Die neuen erweiterten Serienbrief-Funktionalitäten
 *
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * 25.05.2010 | ERT | Aufruf von PDFGesamtdruck-Druckfunktion
 * 20.12.2010 | ERT | Bei ungültigem indexSelection.rangeEnd wird der
 *                    Wert auf den letzten Datensatz gesetzt
 * 08.05.2012 | jub | um beim serienbrief/emailversand die auswahl zwischen odt und pdf
 *                    anhängen anbieten zu können, sendAsEmail() und saveToFile() mit
 *                    einer flage versehen, die zwischen den beiden formaten
 *                    unterscheidet.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.sun.star.awt.XTopWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults.SimulationResultsProcessor;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.QueryResultsWithSchema;
import de.muenchen.allg.itd51.wollmux.dialog.Common;
import de.muenchen.allg.itd51.wollmux.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.dialog.JPotentiallyOverlongPopupMenuButton;
import de.muenchen.allg.itd51.wollmux.dialog.NonNumericKeyConsumer;
import de.muenchen.allg.itd51.wollmux.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.DatasetSelectionType;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.IndexSelection;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.SubmitArgument;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogParameters;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.email.EMailSender;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;

/**
 * Die neuen erweiterten Serienbrief-Funktionalitäten.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew implements MailMergeParams.MailMergeController
{
  /**
   * ID der Property in der die Serienbriefdaten gespeichert werden.
   */
  private static final String PROP_QUERYRESULTS = "MailMergeNew_QueryResults";

  /**
   * ID der Property in der das Zielverzeichnis für den Druck in Einzeldokumente
   * gespeichert wird.
   */
  private static final String PROP_TARGETDIR = "MailMergeNew_TargetDir";

  /**
   * ID der Property in der das Dateinamenmuster für den Einzeldokumentdruck
   * gespeichert wird.
   */
  private static final String PROP_FILEPATTERN = "MailMergeNew_FilePattern";

  /**
   * ID der Property in der der Name des Feldes gespeichert wird, in dem die
   * E-Mail-Adressen der Empfänger enthalten ist.
   */
  private static final String PROP_EMAIL_TO_FIELD_NAME =
    "MailMergeNew_EMailToFieldName";

  /**
   * ID der Property in der der Name des Feldes gespeichert wird, in dem die
   * E-Mail-Adressen der Empfänger enthalten sind.
   */
  private static final String PROP_EMAIL_FROM = "MailMergeNew_EMailFrom";

  /**
   * ID der Property in der die Betreffzeile vom Typ String der zu verschickenden
   * E-Mail enthalten ist.
   */
  private static final String PROP_EMAIL_SUBJECT = "MailMergeNew_EMailSubject";

  /**
   * ID der Property in der die Betreffzeile vom Typ String der zu verschickenden
   * E-Mail enthalten ist.
   */
  private static final String PROP_EMAIL_MESSAGE_TEXTTAGS =
    "MailMergeNew_EMailMessageTextTags";

  /**
   * ID der Property in der das Dateinamenmuster für den Einzeldokumentdruck
   * gespeichert wird.
   */
  private static final String PROP_DATASET_EXPORT = "MailMergeNew_DatasetExport";

  /**
   * ID der Property, die einen List der Indizes der zu druckenden Datensätze
   * speichert.
   */
  private static final String PROP_MAILMERGENEW_SELECTION = "MailMergeNew_Selection";

  private static final String TEMP_MAIL_DIR_PREFIX = "wollmuxmail";

  private static final String MAIL_ERROR_MESSAGE_TITLE =
    L.m("Fehler beim E-Mail-Versand");

  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;

  /**
   * true gdw wir uns im Vorschau-Modus befinden.
   */
  private boolean previewMode;

  /**
   * Die Nummer des zu previewenden Datensatzes. ACHTUNG! Kann aufgrund von
   * Veränderung der Daten im Hintergrund größer sein als die Anzahl der Datensätze.
   * Darauf muss geachtet werden.
   */
  private int previewDatasetNumber = 1;

  /**
   * Die beim letzten Aufruf von {@link #updatePreviewFields()} aktuelle Anzahl an
   * Datensätzen in {@link #ds}.
   */
  private int previewDatasetNumberMax = Integer.MAX_VALUE;

  /**
   * Das Textfield in dem Benutzer direkt eine Datensatznummer für die Vorschau
   * eingeben können.
   */
  private JTextField previewDatasetNumberTextfield;

  private Collection<JComponent> elementsDisabledWhenNoDatasourceSelected =
    new Vector<JComponent>();

  private Collection<JComponent> elementsDisabledWhenNotInPreviewMode =
    new Vector<JComponent>();

  private Collection<JComponent> elementsDisabledWhenFirstDatasetSelected =
    new Vector<JComponent>();

  private Collection<JComponent> elementsDisabledWhenLastDatasetSelected =
    new Vector<JComponent>();

  /**
   * Enthält alle elementsDisabledWhen... Collections.
   */
  private Vector<Collection<JComponent>> listsOfElementsDisabledUnderCertainCircumstances =
    new Vector<Collection<JComponent>>();

  /**
   * Das Toolbar-Fenster.
   */
  private JFrame myFrame;

  /**
   * Der WindowListener, der an {@link #myFrame} hängt.
   */
  private MyWindowListener oehrchen;

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der MailMergeNew
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;

  private MailMergeParams mailMergeParams = new MailMergeParams();

  /**
   * Enthält den Controller, der an das Dokumentfenster dieses Dokuments angekoppelte
   * Fenster überwacht und steuert.
   */
  private CoupledWindowController coupledWindowController = null;

  private TextDocumentController documentController;

  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   *
   * @param documentController
   *          das {@link TextDocumentModel} an dem die Toolbar hängt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public MailMergeNew(TextDocumentController documentController, ActionListener abortListener)
  {
    this.documentController = documentController;
    this.ds = new MailMergeDatasource(documentController);
    this.abortListener = abortListener;

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            createGUI();
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  private void createGUI()
  {
    elementsDisabledWhenNoDatasourceSelected.clear();
    elementsDisabledWhenNotInPreviewMode.clear();
    elementsDisabledWhenFirstDatasetSelected.clear();
    elementsDisabledWhenLastDatasetSelected.clear();
    listsOfElementsDisabledUnderCertainCircumstances.clear();
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenNoDatasourceSelected);
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenNotInPreviewMode);
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenFirstDatasetSelected);
    listsOfElementsDisabledUnderCertainCircumstances.add(elementsDisabledWhenLastDatasetSelected);

    myFrame = new JFrame(L.m("Seriendruck (WollMux)"));
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    myFrame.addWindowListener(oehrchen);

    // WollMux-Icon für den Seriendruck-Frame
    Common.setWollMuxIcon(myFrame);

    Box hbox = Box.createHorizontalBox();
    myFrame.add(hbox);
    JButton button;
    button = new JButton(L.m("Datenquelle"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        ds.showDatasourceSelectionDialog(myFrame, new Runnable()
        {
          @Override
          public void run()
          {
            updateEnabledDisabledState();
          }
        });
      }
    });
    hbox.add(button);

    button =
      new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
        new Iterable<Action>()
        {
          @Override
          public Iterator<Action> iterator()
          {
            return getInsertFieldActionList().iterator();
          }
        });
    hbox.add(button);
    elementsDisabledWhenNoDatasourceSelected.add(button);

    button = new JButton(L.m("Spezialfeld"));
    final JButton specialFieldButton = button;
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        showInsertSpecialFieldPopup(specialFieldButton, 0,
          specialFieldButton.getSize().height);
      }
    });
    hbox.add(button);

    final String VORSCHAU = L.m("   Vorschau   ");
    button = new JButton(VORSCHAU);
    previewMode = false;
    documentController.setFormFieldsPreviewMode(previewMode);

    final JButton previewButton = button;
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (!ds.hasDatasource()) {
          return;
        }
        if (previewMode)
        {
          documentController.collectNonWollMuxFormFields();
          previewButton.setText(VORSCHAU);
          previewMode = false;
          documentController.setFormFieldsPreviewMode(false);
          updateEnabledDisabledState();
        }
        else
        {
          documentController.collectNonWollMuxFormFields();
          previewButton.setText(L.m("<Feldname>"));
          previewMode = true;
          documentController.setFormFieldsPreviewMode(true);
          updatePreviewFields();
        }
      }
    });
    hbox.add(DimAdjust.fixedPreferredSize(button));
    elementsDisabledWhenNoDatasourceSelected.add(button);

    button = new JButton("|<");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = 1;
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenFirstDatasetSelected.add(button);

    button = new JButton("<");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        --previewDatasetNumber;
        if (previewDatasetNumber < 1) {
          previewDatasetNumber = 1;
        }
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenFirstDatasetSelected.add(button);

    previewDatasetNumberTextfield = new JTextField("1", 3);
    previewDatasetNumberTextfield.addKeyListener(NonNumericKeyConsumer.instance);
    previewDatasetNumberTextfield.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        String tfValue = previewDatasetNumberTextfield.getText();
        try
        {
          int newValue = Integer.parseInt(tfValue);
          previewDatasetNumber = newValue;
        }
        catch (Exception x)
        {
          previewDatasetNumberTextfield.setText("" + previewDatasetNumber);
        }
        updatePreviewFields();
      }
    });
    previewDatasetNumberTextfield.setMaximumSize(new Dimension(Integer.MAX_VALUE,
      button.getPreferredSize().height));
    hbox.add(previewDatasetNumberTextfield);
    elementsDisabledWhenNotInPreviewMode.add(previewDatasetNumberTextfield);

    button = new JButton(">");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        ++previewDatasetNumber;
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenLastDatasetSelected.add(button);

    button = new JButton(">|");
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = Integer.MAX_VALUE;
        updatePreviewFields();
      }
    });
    hbox.add(button);
    elementsDisabledWhenNotInPreviewMode.add(button);
    elementsDisabledWhenLastDatasetSelected.add(button);

    button = new JButton(L.m("Drucken"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (ds.hasDatasource())
          mailMergeParams.showDoMailmergeDialog(myFrame, MailMergeNew.this);
      }
    });
    hbox.add(button);
    elementsDisabledWhenNoDatasourceSelected.add(button);

    final JPopupMenu tabelleMenu = new JPopupMenu();
    JMenuItem item = new JMenuItem(L.m("Tabelle bearbeiten"));
    item.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        ds.toFront();
      }
    });
    tabelleMenu.add(item);

    final JMenuItem addColumnsMenuItem =
      new JMenuItem(L.m("Tabellenspalten ergänzen"));
    addColumnsMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        AdjustFields.showAddMissingColumnsDialog(myFrame, documentController, ds);
      }
    });
    tabelleMenu.add(addColumnsMenuItem);

    final JMenuItem adjustFieldsMenuItem =
      new JMenuItem(L.m("Alle Felder anpassen"));
    adjustFieldsMenuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        AdjustFields.showAdjustFieldsDialog(myFrame, documentController, ds);
      }
    });
    tabelleMenu.add(adjustFieldsMenuItem);

    button = new JButton(L.m("Tabelle"));
    final JButton tabelleButton = button;
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        // Ausgrauen der Anpassen-Knöpfe, wenn alle Felder mit den
        // entsprechenden Datenquellenfeldern zugeordnet werden können.
        // Tabellenspalten ergänzen wird außerdem ausgegraut, wenn die Datenquelle
        // dies nicht unterstützt
        boolean hasUnmappedFields =
            documentController.getModel().getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(
            ds.getColumnNames())).length > 0;
        adjustFieldsMenuItem.setEnabled(hasUnmappedFields);
        addColumnsMenuItem.setEnabled(hasUnmappedFields && ds.supportsAddColumns());

        tabelleMenu.show(tabelleButton, 0, tabelleButton.getSize().height);
      }
    });
    hbox.add(button);
    elementsDisabledWhenNoDatasourceSelected.add(button);

    updateEnabledDisabledState();
    myFrame.setAlwaysOnTop(true);
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = frameHeight * 3;// screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(false);
    addCoupledWindow(myFrame);
    myFrame.setVisible(true);

    if (!ds.hasDatasource())
      ds.showDatasourceSelectionDialog(myFrame, new Runnable()
      {
        @Override
        public void run()
        {
          updateEnabledDisabledState();
        }
      });
  }

  /**
   * Geht alle Komponenten durch, die unter bestimmten Bedingungen ausgegraut werden
   * müssen und setzt ihren Status korrekt.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   *
   */
  private void updateEnabledDisabledState()
  {
    // Zuerst alles enablen.
    for (Collection<JComponent> list : listsOfElementsDisabledUnderCertainCircumstances)
    {
      for (JComponent compo : list)
      {
        compo.setEnabled(true);
      }
    }

    if (!ds.hasDatasource())
      for (JComponent compo : elementsDisabledWhenNoDatasourceSelected)
        compo.setEnabled(false);

    if (previewDatasetNumber <= 1)
      for (JComponent compo : elementsDisabledWhenFirstDatasetSelected)
        compo.setEnabled(false);

    if (previewDatasetNumber >= previewDatasetNumberMax)
      for (JComponent compo : elementsDisabledWhenLastDatasetSelected)
        compo.setEnabled(false);

    if (!previewMode) {
      for (JComponent compo : elementsDisabledWhenNotInPreviewMode) {
        compo.setEnabled(false);
      }
    }
  }

  /**
   * Passt {@link #previewDatasetNumber} an, falls sie zu groß oder zu klein ist,
   * setzt {@link #previewDatasetNumberMax} und setzt dann falls {@link #previewMode}
   * == true alle Feldwerte auf die Werte des entsprechenden Datensatzes. Ruft
   * außerdem {@link #updateEnabledDisabledState()} auf.
   *
   * @author Matthias Benkmann (D-III-ITD D.10)
   *
   *         TESTED
   */
  private void updatePreviewFields()
  {
    if (!ds.hasDatasource()) {
      return;
    }

    int count = ds.getNumberOfDatasets();
    previewDatasetNumberMax = count;

    if (previewDatasetNumber > count) {
      previewDatasetNumber = count;
    }
    if (previewDatasetNumber <= 0) {
      previewDatasetNumber = 1;
    }

    String previewDatasetNumberStr = "" + previewDatasetNumber;
    previewDatasetNumberTextfield.setText(previewDatasetNumberStr);

    updateEnabledDisabledState();

    if (!previewMode) {
      return;
    }

    List<String> schema = ds.getColumnNames();
    List<String> data = ds.getValuesForDataset(previewDatasetNumber);

    if (schema.size() != data.size())
    {
      Logger.error(L.m("Daten haben sich zwischen dem Auslesen von Schema und Werten verändert"));
      return;
    }

    Iterator<String> dataIter = data.iterator();
    for (String column : schema)
    {
      WollMuxEventHandler.handleSetFormValue(documentController.getModel().doc, column, dataIter.next(), null);
    }
    WollMuxEventHandler.handleSetFormValue(documentController.getModel().doc,
      MailMergeParams.TAG_DATENSATZNUMMER, previewDatasetNumberStr, null);
    WollMuxEventHandler.handleSetFormValue(documentController.getModel().doc,
      MailMergeParams.TAG_SERIENBRIEFNUMMER, previewDatasetNumberStr, null);
  }

  /**
   * Schliesst den MailMergeNew und alle zugehörigen Fenster.
   *
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public void dispose()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        @Override
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
   * Erzeugt eine Liste mit {@link javax.swing.Action}s für alle Namen aus
   * {@link #ds},getColumnNames(), die ein entsprechendes Seriendruckfeld einfügen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List<Action> getInsertFieldActionList()
  {
    List<Action> actions = new Vector<Action>();
    List<String> columnNames = ds.getColumnNames();

    Collections.sort(columnNames);

    Iterator<String> iter = columnNames.iterator();
    while (iter.hasNext())
    {
      final String name = iter.next();
      Action button = new AbstractAction(name)
      {
        private static final long serialVersionUID = 0; // Eclipse-Warnung totmachen

        @Override
        public void actionPerformed(ActionEvent e)
        {
          documentController.insertMailMergeFieldAtCursorPosition(name);
        }
      };
      actions.add(button);
    }

    return actions;
  }

  /**
   * Erzeugt ein JPopupMenu, das Einträge für das Einfügen von Spezialfeldern enthält
   * und zeigt es an neben invoker an der relativen Position x,y.
   *
   * @param invoker
   *          zu welcher Komponente gehört das Popup
   * @param x
   *          Koordinate des Popups im Koordinatenraum von invoker.
   * @param y
   *          Koordinate des Popups im Koordinatenraum von invoker.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void showInsertSpecialFieldPopup(JComponent invoker, int x, int y)
  {
    boolean dsHasFields = !ds.getColumnNames().isEmpty();
    final TrafoDialog editFieldDialog = getTrafoDialogForCurrentSelection();

    JPopupMenu menu = new JPopupMenu();

    JMenuItem button;

    final String genderButtonName = L.m("Gender");
    button = new JMenuItem(genderButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy für leere Gender-Funktion zusammenbauen.
        ConfigThingy genderConf =
          GenderDialog.generateGenderTrafoConf(ds.getColumnNames().get(0), "", "",
            "");
        insertFieldFromTrafoDialog(ds.getColumnNames(), genderButtonName, genderConf);
      }
    });
    menu.add(button);

    final String iteButtonName = L.m("Wenn...Dann...Sonst...");
    button = new JMenuItem(iteButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy für leere WennDannSonst-Funktion zusammenbauen. Aufbau:
        // IF(STRCMP(VALUE '<firstField>', '') THEN('') ELSE(''))
        ConfigThingy ifConf = new ConfigThingy("IF");
        ConfigThingy strCmpConf = ifConf.add("STRCMP");
        strCmpConf.add("VALUE").add(ds.getColumnNames().get(0));
        strCmpConf.add("");
        ifConf.add("THEN").add("");
        ifConf.add("ELSE").add("");
        insertFieldFromTrafoDialog(ds.getColumnNames(), iteButtonName, ifConf);
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Datensatznummer"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        documentController.insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_DATENSATZNUMMER);
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Serienbriefnummer"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        documentController.insertMailMergeFieldAtCursorPosition(MailMergeParams.TAG_SERIENBRIEFNUMMER);
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Nächster Datensatz"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        documentController.insertNextDatasetFieldAtCursorPosition();
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Feld bearbeiten..."));
    button.setEnabled(editFieldDialog != null);
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        editFieldDialog.show(L.m("Spezialfeld bearbeiten"), myFrame);
      }

    });
    menu.add(button);

    menu.show(invoker, x, y);
  }

  /**
   * Öffnet den Dialog zum Einfügen eines Spezialfeldes, das über die Funktion
   * trafoConf beschrieben ist, erzeugt daraus ein transformiertes Feld und fügt
   * dieses Feld in das Dokument mod ein; Es erwartet darüber hinaus den Namen des
   * Buttons buttonName, aus dem das Label des Dialogs, und später der Mouse-Over
   * hint erzeugt wird und die Liste der aktuellen Felder, die evtl. im Dialog zur
   * Verfügung stehen sollen.
   *
   * @param fieldNames
   *          Eine Liste der Feldnamen, die der Dialog anzeigt, falls er Buttons zum
   *          Einfügen von Serienbrieffeldern bereitstellt.
   * @param buttonName
   *          Der Name des Buttons, aus dem die Titelzeile des Dialogs und der
   *          Mouse-Over Hint des neu erzeugten Formularfeldes generiert wird.
   * @param trafoConf
   *          ConfigThingy, das die Funktion und damit den aufzurufenden Dialog
   *          spezifiziert. Der von den Dialogen benötigte äußere Knoten
   *          "Func(...trafoConf...) wird dabei von dieser Methode erzeugt, so dass
   *          trafoConf nur die eigentliche Funktion darstellen muss.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected void insertFieldFromTrafoDialog(List<String> fieldNames,
      final String buttonName, ConfigThingy trafoConf)
  {
    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = new ConfigThingy("Func");
    params.conf.addChild(trafoConf);
    params.isValid = true;
    params.fieldNames = fieldNames;
    params.closeAction = new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            documentController.replaceSelectionWithTrafoField(status.conf, buttonName);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      }
    };

    try
    {
      TrafoDialogFactory.createDialog(params).show(
        L.m("Spezialfeld %1 einfügen", buttonName), myFrame);
    }
    catch (UnavailableException e)
    {
      Logger.error(L.m("Das darf nicht passieren!"));
    }
  }

  /**
   * Prüft, ob sich in der akutellen Selektion ein transformiertes Feld befindet und
   * liefert ein mit Hilfe der TrafoDialogFactory erzeugtes zugehöriges
   * TrafoDialog-Objekt zurück, oder null, wenn keine transformierte Funktion
   * selektiert ist oder für die Trafo kein Dialog existiert.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private TrafoDialog getTrafoDialogForCurrentSelection()
  {
    ConfigThingy trafoConf = documentController.getModel().getFormFieldTrafoFromSelection();
    if (trafoConf == null) {
      return null;
    }

    final String trafoName = trafoConf.getName();

    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = trafoConf;
    params.isValid = true;
    params.fieldNames = ds.getColumnNames();
    params.closeAction = new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            documentController.setTrafo(trafoName, status.conf);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      }
    };

    try
    {
      return TrafoDialogFactory.createDialog(params);
    }
    catch (UnavailableException e)
    {
      return null;
    }
  }

  /**
   * Startet den Seriendruck (und wird vom Seriendruckdialog aus
   * {@link MailMergeParams} über das Submit-Event aufgerufen.
   *
   * @param usePrintFunctions
   *          Liste der (in der Konfigurationsdatei definierten) Namen der
   *          Druckfunktionen, die für den Seriendruck verwendet werden sollen.
   * @param ignoreDocPrintFuncs
   *          gibt an, ob bereits im Dokument festgelegte Druckfunktionen für den
   *          Seriendruck ignoriert werden sollen (wird z.B. im E-Mail Fall benötigt,
   *          in dem SLVs nicht bereits zum Zeitpunkt des Seriendrucks expandiert
   *          werden sollen, damit mit der E-Mail-Funktion WollMuxFormulare
   *          verschickt werden können)
   * @param datasetSelectionType
   *          Beschreibt den Typ der Datensatzauswahl.
   * @param args
   *          Weitere Argumente, die abhängig von den Benutzereingaben im
   *          Seriendruckdialog gesetzt oder nicht gesetzt sind.
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  @Override
  public void doMailMerge(List<String> usePrintFunctions,
      boolean ignoreDocPrintFuncs, DatasetSelectionType datasetSelectionType,
      Map<SubmitArgument, Object> args)
  {
    documentController.collectNonWollMuxFormFields();
    QueryResultsWithSchema data = ds.getData();

    List<Integer> selected = new Vector<Integer>();
    switch (datasetSelectionType)
    {
      case ALL:
        for (int i = 0; i < data.size(); ++i)
          selected.add(i);
        break;
      case INDIVIDUAL:
        IndexSelection indexSelection =
          (IndexSelection) args.get(SubmitArgument.indexSelection);
        selected.addAll(indexSelection.selectedIndexes);
        break;
      case RANGE:
        indexSelection = (IndexSelection) args.get(SubmitArgument.indexSelection);
        if (indexSelection.rangeStart < 1) {
          indexSelection.rangeStart = 1;
        }
        if (indexSelection.rangeEnd < 1) {
          indexSelection.rangeEnd = data.size();
        }
        if (indexSelection.rangeEnd > data.size())
          indexSelection.rangeEnd = data.size();
        if (indexSelection.rangeStart > data.size())
          indexSelection.rangeStart = data.size();
        if (indexSelection.rangeStart > indexSelection.rangeEnd)
        {
          int t = indexSelection.rangeStart;
          indexSelection.rangeStart = indexSelection.rangeEnd;
          indexSelection.rangeEnd = t;
        }
        for (int i = indexSelection.rangeStart; i <= indexSelection.rangeEnd; ++i)
          selected.add(i - 1); // wir zählen ab 0, anders als rangeStart/End
        break;
    }

    // PrintModel erzeugen und Parameter setzen:
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, !ignoreDocPrintFuncs);
    try
    {
      pmod.setPropertyValue("MailMergeNew_Schema", data.getSchema());
      pmod.setPropertyValue(PROP_QUERYRESULTS, data);
      pmod.setPropertyValue(PROP_MAILMERGENEW_SELECTION, selected);

      Object o = args.get(SubmitArgument.targetDirectory);
      if (o != null) {
        pmod.setPropertyValue(PROP_TARGETDIR, o);
      }

      o = args.get(SubmitArgument.filenameTemplate);
      if (o != null) {
        pmod.setPropertyValue(PROP_FILEPATTERN, o);
      }

      o = args.get(SubmitArgument.emailToFieldName);
      if (o != null) {
        pmod.setPropertyValue(PROP_EMAIL_TO_FIELD_NAME, o);
      }

      o = args.get(SubmitArgument.emailFrom);
      if (o != null) {
        pmod.setPropertyValue(PROP_EMAIL_FROM, o);
      }

      o = args.get(SubmitArgument.emailSubject);
      if (o != null) {
        pmod.setPropertyValue(PROP_EMAIL_SUBJECT, o);
      }

      o = args.get(SubmitArgument.emailText);
      if (o != null) {
        pmod.setPropertyValue(PROP_EMAIL_MESSAGE_TEXTTAGS, o);
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }

    // Benötigte Druckfunktionen zu pmod hinzufügen:
    try
    {
      for (String printFunctionName : usePrintFunctions)
        pmod.usePrintFunction(printFunctionName);
    }
    catch (NoSuchMethodException e)
    {
      Logger.error(e);
      ModalDialogs.showInfoModal(
        L.m("Fehler beim Drucken"),
        L.m(
          "Eine notwendige Druckfunktion ist nicht definiert. Bitte wenden Sie sich an Ihre Systemadministration damit Ihre Konfiguration entsprechend erweitert bzw. aktualisiert werden kann.\n\n%1",
          e));
      pmod.cancel();
      return;
    }

    // Drucken im Hintergrund, damit der EDT nicht blockiert.
    new Thread()
    {
      @Override
      public void run()
      {
        long startTime = System.currentTimeMillis();

        documentController.setFormFieldsPreviewMode(true);
        pmod.printWithProps();
        documentController.setFormFieldsPreviewMode(false);

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        Logger.debug(L.m("MailMerge finished after %1 seconds", duration));
      }
    }.start();
  }

  /**
   * PrintFunction, die das jeweils nächste Element der Seriendruckdaten nimmt und
   * die Seriendruckfelder im Dokument entsprechend setzt. Herangezogen werden die
   * Properties {@link #PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults})
   * und "MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält, sowie
   * {@link #PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz). Dies funktioniert
   * natürlich nur dann, wenn pmod kein Proxy ist.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod) throws Exception
  {
    mailMergeNewSetFormValue(pmod, null);
  }

  /**
   * Implementierung einer Druckfunktion, die das jeweils nächste Element der
   * Seriendruckdaten nimmt und die Seriendruckfelder im Dokument entsprechend setzt;
   * wird der SimulationsResultsProcessor simProc übergeben, so werden die
   * Dokumentänderungen nur simuliert und nicht tatsächlich im Dokument ausgeführt.
   * Im Fall, dass simProc != null ist, wird auch die nächste Druckfunktion in der
   * Aufrufkette nicht aufgerufen, sondern statt dessen der in simProc enthaltene
   * handler. Die Druckfunktion zieht folgende Properties heran:
   *
   * <ul>
   * <li>{@link #PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults})</li>
   *
   * <li>"MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält</li>
   *
   * <li>{@link #PROP_MAILMERGENEW_SELECTION}, was eine Liste der Indizes der
   * ausgewählten Datensätze ist (0 ist der erste Datensatz).</li> *
   * <ul>
   *
   * @param pmod
   *          PrintModel welches das Hauptdokument des Seriendrucks beschreibt.
   * @param simProc
   *          Ist simProc != null, so werden die Wertänderungen nur simuliert und
   *          nach jedem Datensatz einmal der in simProc enthaltene handler
   *          aufgerufen.
   * @throws Exception
   *           Falls irgend etwas schief geht
   *
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
   *         TESTED
   */
  @SuppressWarnings("unchecked")
  public static void mailMergeNewSetFormValue(XPrintModel pmod,
      SimulationResultsProcessor simProc) throws Exception
  {
    TextDocumentController documentController =
      DocumentManager.getTextDocumentController(pmod.getTextDocument());

    QueryResults data = (QueryResults) pmod.getPropertyValue(PROP_QUERYRESULTS);
    Collection<String> schema = (Collection<String>) pmod.getPropertyValue("MailMergeNew_Schema");
    List<Integer> selection =
      (List<Integer>) pmod.getPropertyValue(PROP_MAILMERGENEW_SELECTION);
    if (selection.isEmpty()) {
      return;
    }

    Iterator<Dataset> iter = data.iterator();
    Iterator<Integer> selIter = selection.iterator();
    int selectedIdx = selIter.next();

    pmod.setPrintProgressMaxValue((short) selection.size());

    int index = -1;
    int serienbriefNummer = 1;
    while (iter.hasNext() && selectedIdx >= 0)
    {
      if (pmod.isCanceled()) {
        return;
      }

      Dataset ds = iter.next();
      if (++index < selectedIdx) {
        continue;
      }

      int datensatzNummer = index + 1; // same as datensatzNummer = selectedIdx+1;

      if (selIter.hasNext())
        selectedIdx = selIter.next();
      else
        selectedIdx = -1;

      if (simProc != null) {
        documentController.startSimulation();
      }

      HashMap<String, String> dataSetExport = new HashMap<String, String>();
      try
      {
        pmod.setPropertyValue(PROP_DATASET_EXPORT, dataSetExport);
      }
      catch (Exception x)
      {}

      for(String spalte : schema)
      {
        String value = ds.get(spalte);
        // Wert zuerst entsetzen um sicher eine Änderung hervorzurufen.
        // Denn ansonsten werden die Sichtbarkeiten nicht richtig aktualisiert.
        pmod.setFormValue(spalte, "");
        pmod.setFormValue(spalte, value);
        dataSetExport.put(spalte, value);
      }
      pmod.setFormValue(MailMergeParams.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      dataSetExport.put(MailMergeParams.TAG_DATENSATZNUMMER, "" + datensatzNummer);
      pmod.setFormValue(MailMergeParams.TAG_SERIENBRIEFNUMMER, ""
        + serienbriefNummer);
      dataSetExport.put(MailMergeParams.TAG_SERIENBRIEFNUMMER, ""
        + serienbriefNummer);

      // Weiterreichen des Drucks an die nächste Druckfunktion. Dies findet nicht
      // statt, wenn simProc != null ist, da die Verarbeitung in diesem Fall über
      // simProc durchgeführt wird.
      if (simProc == null)
        pmod.printWithProps();
      else
        simProc.processSimulationResults(documentController.stopSimulation());

      pmod.setPrintProgressValue((short) serienbriefNummer);
      ++serienbriefNummer;
    }
  }

  /**
   * Liefert die Größe der von MailMergeNew im XPrintModel gesetzten Selection.
   */
  @SuppressWarnings("unchecked")
  public static int mailMergeNewGetSelectionSize(XPrintModel pmod)
  {
    List<Integer> selection;
    try
    {
      selection = (List<Integer>) pmod.getPropertyValue(PROP_MAILMERGENEW_SELECTION);
    }
    catch (Exception e)
    {
      Logger.error(e);
      return 0;
    }
    return selection.size();
  }

  /**
   * Speichert das übergebene Dokument in eine ODF-Datei. Die WollMux-Daten bleiben
   * dabei erhalten.
   *
   * @author Ignaz Forster (D-III-ITD-D102)
   */
  public static File saveToFile(XPrintModel pmod, boolean isODT)
  {
    XTextDocument textDocument = pmod.getTextDocument();

    File outputDir =
      new File(pmod.getProp(PROP_TARGETDIR,
        System.getProperty("user.home") + "/Seriendruck").toString());

    String filename;
    TextComponentTags filePattern =
      (TextComponentTags) pmod.getProp(PROP_FILEPATTERN, null);
    if (filePattern != null)
      filename = createOutputPathFromPattern(filePattern, pmod);
    else
      filename = L.m("Dokument.odt");

    // jub .odt/.pdf ergänzen, falls nicht angegeben.
    if (!filename.toLowerCase().endsWith(".odt")
      && !filename.toLowerCase().endsWith(".pdf"))
    {
      if (isODT)
        filename = filename + ".odt";
      else
        filename = filename + ".pdf";
    }

    File file = new File(outputDir, filename);

    saveOutputFile(file, textDocument);

    return file;
  }

  /**
   * Speichert das übergebene Dokument in eine ODF-Datei. Die WollMux-Daten bleiben
   * dabei erhalten.
   *
   * @author Ignaz Forster (D-III-ITD-D102)
   */
  public static void sendAsEmail(XPrintModel pmod, boolean isODT)
  {
    String targetDir = (String) pmod.getProp(PROP_TARGETDIR, null);
    File tmpOutDir = null;
    if (targetDir != null)
      tmpOutDir = new File(targetDir);
    else
      try
      {
        tmpOutDir = File.createTempFile(TEMP_MAIL_DIR_PREFIX, null);
        tmpOutDir.delete();
        tmpOutDir.mkdir();
        try
        {
          pmod.setPropertyValue(PROP_TARGETDIR, tmpOutDir.toString());
        }
        catch (Exception e)
        {
          Logger.error(L.m("darf nicht vorkommen"), e);
        }
      }
      catch (java.io.IOException e)
      {
        Logger.error(e);
      }
    if (tmpOutDir == null)
    {
      ModalDialogs.showInfoModal(MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Das temporäre Verzeichnis %1 konnte nicht angelegt werden.",
        TEMP_MAIL_DIR_PREFIX));
      pmod.cancel();
      return;
    }

    String from = pmod.getProp(PROP_EMAIL_FROM, "").toString();
    if (!isMailAddress(from))
    {
      ModalDialogs.showInfoModal(MAIL_ERROR_MESSAGE_TITLE, L.m(
        "Die Absenderadresse '%1' ist ungültig.", from));
      pmod.cancel();
      return;
    }

    String fieldName = pmod.getProp(PROP_EMAIL_TO_FIELD_NAME, "").toString();
    @SuppressWarnings("unchecked")
    HashMap<String, String> ds =
      new HashMap<String, String>((HashMap<String, String>) pmod.getProp(
        PROP_DATASET_EXPORT, new HashMap<String, String>()));
    String to = ds.get(fieldName);
    PrintModels.setStage(pmod, L.m("Sende an %1", to));
    if (!isMailAddress(to))
    {
      int res =
        JOptionPane.showConfirmDialog(
          null,
          L.m(
            "Die Empfängeradresse '%1' ist ungültig!\n\nDiesen Datensatz überspringen und fortsetzen?",
            to), MAIL_ERROR_MESSAGE_TITLE, JOptionPane.OK_CANCEL_OPTION);
      if (res == JOptionPane.CANCEL_OPTION) {
        pmod.cancel();
      }
      return;
    }

    String subject =
      pmod.getProp(PROP_EMAIL_SUBJECT, L.m("<kein Betreff>")).toString();

    String message = "";
    TextComponentTags messageTags =
      (TextComponentTags) pmod.getProp(PROP_EMAIL_MESSAGE_TEXTTAGS, null);
    if (messageTags != null) {
      message = messageTags.getContent(ds);
    }

    File attachment = null;
    try
    {
      attachment = saveToFile(pmod, isODT);
      EMailSender mail = new EMailSender();
      mail.createNewMultipartMail(from, to, subject, message);
      mail.addAttachment(attachment);
      mail.sendMessage();
    }
    catch (ConfigurationErrorException e)
    {
      Logger.error(e);
      ModalDialogs.showInfoModal(
        MAIL_ERROR_MESSAGE_TITLE,
        L.m("Es konnten keine Angaben zum Mailserver gefunden werden - eventuell ist die WollMux-Konfiguration nicht vollständig."));
      pmod.cancel();
      return;
    }
    catch (MessagingException e)
    {
      Logger.error(e);
      ModalDialogs.showInfoModal(MAIL_ERROR_MESSAGE_TITLE,
        L.m("Der Versand der E-Mail ist fehlgeschlagen."));
      pmod.cancel();
      return;
    }
    catch (Exception e)
    {
      Logger.error(e);
      pmod.cancel();
      return;
    }
    finally
    {
      if (attachment != null) {
        attachment.delete();
      }
    }
  }

  /**
   * grobe Plausiprüfung, ob E-Mailadresse gültig ist.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static boolean isMailAddress(String mail)
  {
    return mail != null && mail.length() > 0 && mail.matches("[^ ]+@[^ ]+");
  }

  /**
   * Speichert doc unter dem in outFile angegebenen Dateipfad und schließt dann doc.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   *
   *         TESTED
   */
  private static void saveOutputFile(File outFile, XTextDocument doc)
  {
    try
    {
      String unparsedUrl = outFile.toURI().toURL().toString();

      XStorable store = UNO.XStorable(doc);
      PropertyValue[] options;

      /*
       * For more options see:
       *
       * http://wiki.services.openoffice.org/wiki/API/Tutorials/PDF_export
       */
      if (unparsedUrl.endsWith(".pdf"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "writer_pdf_Export";
      }
      else if (unparsedUrl.endsWith(".doc"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "MS Word 97";
      }
      else
      {
        if (!unparsedUrl.endsWith(".odt")) {
          unparsedUrl = unparsedUrl + ".odt";
        }

        options = new PropertyValue[0];
      }

      com.sun.star.util.URL url = UNO.getParsedUNOUrl(unparsedUrl);

      /*
       * storeTOurl() has to be used instead of storeASurl() for PDF export
       */
      store.storeToURL(url.Complete, options);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Nimmt filePattern, ersetzt darin befindliche Tags durch entsprechende
   * Spaltenwerte aus ds und setzt daraus einen Dateipfad mit Elternverzeichnis
   * targetDir zusammen. Die Spezialtags {@link MailMergeParams#TAG_DATENSATZNUMMER}
   * und {@link MailMergeParams#TAG_SERIENBRIEFNUMMER} werden durch die Strings
   * datensatzNummer und serienbriefNummer ersetzt.
   *
   * @param totalDatasets
   *          die Gesamtzahl aller Datensätze (auch der für den aktuellen
   *          Druckauftrag nicht gewählten). Wird verwendet um datensatzNummer und
   *          serienbriefNummer mit 0ern zu padden.
   *
   * @throws MissingMapEntryException
   *           wenn ein Tag verwendet wird, zu dem es keine Spalte im aktuellen
   *           Datensatz existiert.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private static String createOutputPathFromPattern(TextComponentTags filePattern,
      XPrintModel pmod)
  {
    int digits = 4;
    try
    {
      QueryResults r = (QueryResults) pmod.getPropertyValue(PROP_QUERYRESULTS);
      digits = ("" + r.size()).length();
    }
    catch (Exception e)
    {}

    @SuppressWarnings("unchecked")
    HashMap<String, String> dataset =
      new HashMap<String, String>((HashMap<String, String>) pmod.getProp(PROP_DATASET_EXPORT,
        new HashMap<String, String>()));

    // Zähler für #DS und #SB mit gleicher Länge erzeugen (ggf. mit 0en auffüllen)
    fillWithLeading0(dataset, MailMergeParams.TAG_DATENSATZNUMMER, digits);
    fillWithLeading0(dataset, MailMergeParams.TAG_SERIENBRIEFNUMMER, digits);

    String fileName = filePattern.getContent(dataset);
    return simplifyFilename(fileName);
  }

  /**
   * Holt sich Element key aus dataset, sorgt dafür, dass der Wert digit-stellig wird
   * und speichert diesen Wert wieder in dataset ab.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static void fillWithLeading0(HashMap<String, String> dataset, String key,
      int digits)
  {
    String value = dataset.get(key);
    if (value == null) {
      value = "";
    }
    while (value.length() < digits)
      value = "0" + value;
    dataset.put(key, value);
  }

  /**
   * Ersetzt alle möglicherweise bösen Zeichen im Dateinamen name durch eine
   * Unterstrich.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String simplifyFilename(String name)
  {
    return name.replaceAll("[^\\p{javaLetterOrDigit},.()=+_-]", "_");
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeController
   * #hasPrintfunction(java.lang.String)
   */
  @Override
  public boolean hasPrintfunction(String name)
  {
    final XPrintModel pmod = PrintModels.createPrintModel(documentController, true);
    try
    {
      pmod.usePrintFunction(name);
      return true;
    }
    catch (NoSuchMethodException ex)
    {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeController
   * #getColumnNames()
   */
  @Override
  public List<String> getColumnNames()
  {
    return ds.getColumnNames();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeController
   * #getDefaultFilename()
   */
  @Override
  public String getDefaultFilename()
  {
    String title = documentController.getFrameController().getTitle();
    // Suffix entfernen:
    if (title.toLowerCase().matches(".+\\.(odt|doc|ott|dot)$"))
      title = title.substring(0, title.length() - 4);
    return simplifyFilename(title);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams.MailMergeController
   * #getTextDocument()
   */
  @Override
  public XTextDocument getTextDocument()
  {
    return documentController.getModel().doc;
  }

  private class MyWindowListener extends WindowAdapter
  {
    @Override
    public void windowClosing(WindowEvent e)
    {
      abort();
    }
  }

  private void abort()
  {
    removeCoupledWindow(myFrame);
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen nötig, damit der MailMerge gc'ed werden kann. Die Befehle sorgen
     * dafür, dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager)
     * indirekt über den JFrame den MailMerge kennt.
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);

    myFrame.dispose();
    myFrame = null;

    ds.dispose();

    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }

  /**
   * Koppelt das AWT-Window window an das Fenster dieses Textdokuments an. Die
   * Methode muss aufgerufen werden, solange das Fenster window unsichtbar und nicht
   * aktiv ist (also z.B. vor dem Aufruf von window.setVisible(true)).
   *
   * @param window
   *          das Fenster, das an das Hauptfenster angekoppelt werden soll.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private synchronized void addCoupledWindow(Window window)
  {
    if (window == null) {
      return;
    }
    if (coupledWindowController == null)
    {
      coupledWindowController = new CoupledWindowController();
      XFrame f = documentController.getFrameController().getFrame();
      XTopWindow w = null;
      if (f != null) {
        w = UNO.XTopWindow(f.getContainerWindow());
      }
      if (w != null) {
        coupledWindowController.setTopWindow(w);
      }
    }

    coupledWindowController.addCoupledWindow(window);
  }

  /**
   * Löst die Bindung eines angekoppelten Fensters window an das Dokumentfenster.
   *
   * @param window
   *          das Fenster, dessen Bindung zum Hauptfenster gelöst werden soll. Ist
   *          das Fenster nicht angekoppelt, dann passiert nichts.
   *
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private synchronized void removeCoupledWindow(Window window)
  {
    if (window == null || coupledWindowController == null) {
      return;
    }

    coupledWindowController.removeCoupledWindow(window);

    if (!coupledWindowController.hasCoupledWindows())
    {
      // deregistriert den windowListener.
      XFrame f = documentController.getFrameController().getFrame();
      XTopWindow w = null;
      if (f != null) {
        w = UNO.XTopWindow(f.getContainerWindow());
      }
      if (w != null) {
        coupledWindowController.unsetTopWindow(w);
      }
      coupledWindowController = null;
    }
  }
}
