/*
 * Dateiname: MailMergeNew.java
 * Projekt  : WollMux
 * Funktion : Die neuen erweiterten Serienbrief-Funktionalitäten
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 11.10.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.UnavailableException;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.FieldSubstitution;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel.ReferencedFieldID;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.MailMergeDatasource;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.QueryResultsWithSchema;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.GenderDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialog;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.trafo.TrafoDialogParameters;

/**
 * Die neuen erweiterten Serienbrief-Funktionalitäten.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew
{
  /**
   * ID der Property in der die Serienbriefdaten gespeichert werden.
   */
  private static final String PROP_QUERYRESULTS = "MailMergeNew_QueryResults";

  /**
   * Das {@link TextDocumentModel} zu dem Dokument an dem diese Toolbar hängt.
   */
  private TextDocumentModel mod;

  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;

  /**
   * Verschlingt alle KeyEvents die keine Ziffern oder Editierbefehle sind.
   */
  private KeyListener nonNumericKeyConsumer = new NonNumericKeyConsumer();

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
   * Wird auf true gesetzt, wenn der Benutzer beim Seriendruck auswählt, dass er die
   * Ausgabe in einem neuen Dokument haben möchte.
   */
  private boolean printIntoDocument = true;

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  private enum DatasetSelectionType {
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
    INDIVIDUAL
  };

  /**
   * Auf welche Art hat der Benutzer die zu druckenden Datensätze ausgewählt.
   */
  private DatasetSelectionType datasetSelectionType = DatasetSelectionType.ALL;

  /**
   * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
   * bestimmt dies den ersten zu druckenden Datensatz. ACHTUNG! Der Wert hier kann 0
   * oder größer als {@link #rangeEnd} sein. Dies muss dort behandelt werden, wo er
   * verwendet wird.
   */
  private int rangeStart = 1;

  /**
   * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
   * bestimmt dies den letzten zu druckenden Datensatz. ACHTUNG! Der Wert hier kann 0
   * oder kleiner als {@link #rangeStart} sein. Dies muss dort behandelt werden, wo
   * er verwendet wird.
   */
  private int rangeEnd = Integer.MAX_VALUE;

  /**
   * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#INDIVIDUAL}
   * bestimmt dies die Indizes der ausgewählten Datensätze, wobei 1 den ersten
   * Datensatz bezeichnet.
   */
  private List selectedIndexes = new Vector();

  /**
   * Das Textfield in dem Benutzer direkt eine Datensatznummer für die Vorschau
   * eingeben können.
   */
  private JTextField previewDatasetNumberTextfield;

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

  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   * 
   * @param mod
   *          das {@link TextDocumentModel} an dem die Toolbar hängt.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public MailMergeNew(TextDocumentModel mod, ActionListener abortListener)
  {
    this.mod = mod;
    this.ds = new MailMergeDatasource(mod);
    this.abortListener = abortListener;

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
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
          ;
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
    myFrame = new JFrame(L.m("Seriendruck (WollMux)"));
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    myFrame.addWindowListener(oehrchen);

    Box hbox = Box.createHorizontalBox();
    myFrame.add(hbox);
    JButton button;
    button = new JButton(L.m("Datenquelle"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ds.showDatasourceSelectionDialog(myFrame);
      }
    });
    hbox.add(button);

    // FIXME: Ausgrauen, wenn kein Datenquelle ausgewählt
    button = new JPotentiallyOverlongPopupMenuButton(L.m("Serienbrieffeld"),
      new Iterable()
      {
        public Iterator<Action> iterator()
        {
          return getInsertFieldActionList().iterator();
        }
      });
    hbox.add(button);

    button = new JButton(L.m("Spezialfeld"));
    final JButton specialFieldButton = button;
    button.addActionListener(new ActionListener()
    {
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
    mod.setFormFieldsPreviewMode(previewMode);

    final JButton previewButton = button;
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (!ds.hasDatasource()) return;
        if (previewMode)
        {
          mod.collectNonWollMuxFormFields();
          previewButton.setText(VORSCHAU);
          previewMode = false;
          mod.setFormFieldsPreviewMode(false);
        }
        else
        {
          mod.collectNonWollMuxFormFields();
          previewButton.setText(L.m("<Feldname>"));
          previewMode = true;
          mod.setFormFieldsPreviewMode(true);
          updatePreviewFields();
        }
      }
    });
    hbox.add(DimAdjust.fixedSize(button));

    // FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus oder wenn erster
    // Datensatz angezeigt.
    button = new JButton("|<");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = 1;
        updatePreviewFields();
      }
    });
    hbox.add(button);

    // FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus oder wenn erster
    // Datensatz angezeigt
    button = new JButton("<");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        --previewDatasetNumber;
        if (previewDatasetNumber < 1) previewDatasetNumber = 1;
        updatePreviewFields();
      }
    });
    hbox.add(button);

    // FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus.
    previewDatasetNumberTextfield = new JTextField("1", 3);
    previewDatasetNumberTextfield.addKeyListener(nonNumericKeyConsumer);
    previewDatasetNumberTextfield.addActionListener(new ActionListener()
    {
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

    // FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus oder wenn letzter
    // Datensatz angezeigt.
    button = new JButton(">");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ++previewDatasetNumber;
        updatePreviewFields();
      }
    });
    hbox.add(button);

    // FIXME: Muss ausgegraut sein, wenn nicht im Vorschau-Modus oder wenn letzter
    // Datensatz angezeigt.
    button = new JButton(">|");
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        previewDatasetNumber = Integer.MAX_VALUE;
        updatePreviewFields();
      }
    });
    hbox.add(button);

       // FIXME: Ausgrauen, wenn keine Datenquelle gewählt ist.
    button = new JButton(L.m("Drucken"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if (ds.hasDatasource()) showMailmergeTypeSelectionDialog();
      }
    });
    hbox.add(button);

    final JPopupMenu tabelleMenu = new JPopupMenu();
    JMenuItem item = new JMenuItem(L.m("Tabelle bearbeiten"));
    item.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ds.toFront();
      }
    });
    tabelleMenu.add(item);

    final JMenuItem addColumnsMenuItem = new JMenuItem(
      L.m("Tabellenspalten ergänzen"));
    addColumnsMenuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        showAddMissingColumnsDialog();
      }
    });
    tabelleMenu.add(addColumnsMenuItem);

    final JMenuItem adjustFieldsMenuItem = new JMenuItem(L.m("Alle Felder anpassen"));
    adjustFieldsMenuItem.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        showAdjustFieldsDialog();
      }
    });
    tabelleMenu.add(adjustFieldsMenuItem);

    // FIXME: Button darf nur angezeigt werden, wenn tatsächlich eine Calc-Tabelle
    // ausgewählt ist.
    button = new JButton(L.m("Tabelle"));
    final JButton tabelleButton = button;
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // Anpassen des Menüpunktes "Felder anpassen"
        if (mod.hasSelection())
        {
          adjustFieldsMenuItem.setText(L.m("Ausgewählte Felder anpassen"));
        }
        else
        {
          adjustFieldsMenuItem.setText(L.m("Alle Felder anpassen"));
        }

        // Ausgrauen der Anpassen-Knöpfe, wenn alle Felder mit den
        // entsprechenden Datenquellenfeldern zugeordnet werden können.
        boolean hasUnmappedFields = mod.getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(
          ds.getColumnNames())).length > 0;
        adjustFieldsMenuItem.setEnabled(hasUnmappedFields);
        // TODO: einkommentieren wenn implementiert:
        // addColumnsMenuItem.setEnabled(hasUnmappedFields);
        addColumnsMenuItem.setEnabled(false);

        tabelleMenu.show(tabelleButton, 0, tabelleButton.getSize().height);
      }
    });
    hbox.add(button);

    myFrame.setAlwaysOnTop(true);
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = frameHeight * 3;// screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(false);
    mod.addCoupledWindow(myFrame);
    myFrame.setVisible(true);

    if (!ds.hasDatasource()) ds.showDatasourceSelectionDialog(myFrame);
  }

  /**
   * Passt {@link #previewDatasetNumber} an, falls sie zu groß oder zu klein ist und
   * setzt dann falls {@link #previewMode} == true alle Feldwerte auf die Werte des
   * entsprechenden Datensatzes.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   * 
   * TESTED
   */
  private void updatePreviewFields()
  {
    if (!ds.hasDatasource()) return;

    int count = ds.getNumberOfDatasets();

    if (previewDatasetNumber > count) previewDatasetNumber = count;
    if (previewDatasetNumber <= 0) previewDatasetNumber = 1;

    previewDatasetNumberTextfield.setText("" + previewDatasetNumber);

    if (!previewMode) return;

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
      // FIXME: Ist das so richtig? Geht das effizienter?
      mod.setFormFieldValue(column, dataIter.next());
      mod.updateFormFields(column);
    }

  }

  /**
   * Diese Methode zeigt den Dialog an, mit dem die Felder im Dokument an eine
   * Datenquelle angepasst werden können, die nicht die selben Spalten enthält wie
   * die Datenquelle, für die das Dokument gemacht wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected void showAdjustFieldsDialog()
  {
    ReferencedFieldID[] fieldIDs = mod.getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(
      ds.getColumnNames()));
    ActionListener submitActionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        HashMap mapIdToSubstitution = (HashMap) e.getSource();
        for (Iterator iter = mapIdToSubstitution.keySet().iterator(); iter.hasNext();)
        {
          String fieldId = (String) iter.next();
          FieldSubstitution subst = (FieldSubstitution) mapIdToSubstitution.get(fieldId);
          mod.applyFieldSubstitution(fieldId, subst);
        }
      }
    };
    showFieldMappingDialog(fieldIDs, L.m("Felder anpassen"), L.m("Altes Feld"),
      L.m("Neue Belegung"), L.m("Felder anpassen"), submitActionListener);
  }

  /**
   * Diese Methode zeigt den Dialog an, mit dem die Spalten der Tabelle ergänzt
   * werden können, wenn es zu Feldern im Dokument keine passenden Spalten in der
   * Tabelle gibt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  protected void showAddMissingColumnsDialog()
  {
    ReferencedFieldID[] fieldIDs = mod.getReferencedFieldIDsThatAreNotInSchema(new HashSet<String>(
      ds.getColumnNames()));
    ActionListener submitActionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // Enthält die Zuordnung ID ->
        // de.muenchen.allg.itd51.wollmux.TextDocumentModel.FieldSubstitution,
        // in der die anzuwendende Ersetzungsregel beschrieben ist.

        // HashMap mapIdToSubstitution = (HashMap) e.getSource();
        // TODO: tabellenspalten wie in mapIdToSubstitution beschrieben ergänzen
      }
    };
    showFieldMappingDialog(fieldIDs, L.m("Tabellenspalten ergänzen"), L.m("Spalte"),
      L.m("Vorbelegung"), L.m("Spalten ergänzen"), submitActionListener);
  }

  /**
   * Zeigt einen Dialog mit dem bestehende Felder fieldIDs über ein Textfeld neu
   * belegt werden können; für die neue Belegung stehen die neuen Felder der
   * aktuellen Datasource und Freitext zur Verfügung. Die Felder fieldIDs werden
   * dabei in der Reihenfolge angezeigt, in der sie in der Liste aufgeführt sind, ein
   * bereits aufgeführtes Feld wird aber nicht zweimal angezeigt. Ist bei einem Feld
   * die Eigenschaft isTransformed()==true, dann wird für dieses Feld nur die Eingabe
   * einer 1-zu-1 Zuordnung von Feldern akzeptiert, das andere Zuordnungen für
   * transformierte Felder derzeit nicht unterstützt werden.
   * 
   * @param fieldIDs
   *          Die field-IDs der alten, bereits im Dokument enthaltenen Felder, die in
   *          der gegebenen Reihenfolge angezeigt werden, Dupletten werden aber
   *          entfernt.
   * @param title
   *          Die Titelzeile des Dialogs
   * @param labelOldFields
   *          Die Spaltenüberschrift für die linke Spalte, in der die alten Felder
   *          angezeigt werden.
   * @param labelNewFields
   *          Die Spaltenüberschrift für die rechte Spalte, in dem die neue Zuordnung
   *          getroffen wird.
   * @param labelSubmitButton
   *          Die Beschriftung des Submit-Knopfes unten rechts, der die entsprechende
   *          Aktion auslöst.
   * @param submitActionListener
   *          Nach Beendigung des Dialogs über den Submit-Knopf (unten rechts) wird
   *          die Methode submitActionListener.actionPerformed(actionEvent) in einem
   *          separaten Thread aufgerufen. Dort kann der Code stehen, der gewünschten
   *          Aktionen durchführt. Der ActionListener bekommt dabei in actionEvent
   *          eine HashMap übergeben, die eine Zuordnung von den alten fieldIDs auf
   *          den jeweiligen im Dialog gewählten Ersatzstring enthält.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void showFieldMappingDialog(ReferencedFieldID[] fieldIDs, String title,
      String labelOldFields, String labelNewFields, String labelSubmitButton,
      final ActionListener submitActionListener)
  {
    final JDialog dialog = new JDialog(myFrame, title, true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final TextComponentTags[] currentField = new TextComponentTags[] { null };
    final HashMap<TextComponentTags, String> mapTextComponentTagsToFieldname = new HashMap<TextComponentTags, String>();

    Box headers = Box.createHorizontalBox();
    final JButton insertFieldButton = new JPotentiallyOverlongPopupMenuButton(
      L.m("Serienbrieffeld"), new Iterable()
      {
        public Iterator<Action> iterator()
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
              private static final long serialVersionUID = 0;

              public void actionPerformed(ActionEvent e)
              {
                if (currentField[0] != null) currentField[0].insertTag(name);
              }
            };
            actions.add(button);
          }

          return actions.iterator();
        }
      });
    insertFieldButton.setFocusable(false);
    headers.add(Box.createHorizontalGlue());
    headers.add(insertFieldButton);

    Box itemBox = Box.createVerticalBox();
    ArrayList<JLabel> labels = new ArrayList<JLabel>();
    int maxLabelWidth = 0;

    Box hbox = Box.createHorizontalBox();
    JLabel label = new JLabel(labelOldFields);
    labels.add(label);
    maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    hbox.add(label);
    label = new JLabel(labelNewFields);
    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(label);
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(200));
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(hbox);
    itemBox.add(hbox);

    HashSet<String> addedFields = new HashSet<String>();
    for (int i = 0; i < fieldIDs.length; i++)
    {
      String fieldId = fieldIDs[i].getFieldId();
      if (addedFields.contains(fieldId)) continue;
      final boolean isTransformed = fieldIDs[i].isTransformed();
      addedFields.add(fieldId);

      hbox = Box.createHorizontalBox();

      label = new JLabel(fieldId);
      label.setFont(label.getFont().deriveFont(Font.PLAIN));
      labels.add(label);
      maxLabelWidth = DimAdjust.maxWidth(maxLabelWidth, label);
      label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
      hbox.add(label);

      final TextComponentTags field = new TextComponentTags(new JTextField())
      {
        public boolean isContentValid()
        {
          if (!isTransformed) return true;
          List c = getContent();
          if (c.size() == 0) return true;
          return c.size() == 1
                 && ((TextComponentTags.ContentElement) c.get(0)).isTag();
        }
      };
      mapTextComponentTagsToFieldname.put(field, fieldId);
      Box fbox = Box.createHorizontalBox();
      hbox.add(fbox); // fbox für zeilenbündige Ausrichtung benötigt
      fbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
      field.getJTextComponent().addFocusListener(new FocusListener()
      {
        public void focusLost(FocusEvent e)
        {
        }

        public void focusGained(FocusEvent e)
        {
          currentField[0] = field;
        }
      });
      DimAdjust.maxHeightIsPrefMaxWidthUnlimited(field.getJTextComponent());
      fbox.add(field.getJTextComponent());

      itemBox.add(hbox);
    }

    // einheitliche Breite für alle Labels vergeben:
    for (Iterator<JLabel> iter = labels.iterator(); iter.hasNext();)
    {
      label = iter.next();

      Dimension d = label.getPreferredSize();
      d.width = maxLabelWidth + 10;
      label.setPreferredSize(d);
    }

    Box buttonBox = Box.createHorizontalBox();
    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        dialog.dispose();
      }
    });
    buttonBox.add(button);

    buttonBox.add(Box.createHorizontalGlue());

    button = new JButton(labelSubmitButton);
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        final HashMap<String, FieldSubstitution> result = new HashMap<String, FieldSubstitution>();
        for (Iterator<TextComponentTags> iter = mapTextComponentTagsToFieldname.keySet().iterator(); iter.hasNext();)
        {
          TextComponentTags f = iter.next();
          if (!f.isContentValid()) continue;
          String fieldId = "" + mapTextComponentTagsToFieldname.get(f);
          FieldSubstitution subst = new TextDocumentModel.FieldSubstitution();
          for (Iterator contentIter = f.getContent().iterator(); contentIter.hasNext();)
          {
            TextComponentTags.ContentElement ce = (TextComponentTags.ContentElement) contentIter.next();
            if (ce.isTag())
              subst.addField(ce.toString());
            else
              subst.addFixedText(ce.toString());
          }
          result.put(fieldId, subst);
        }

        dialog.dispose();

        if (submitActionListener != null) new Thread()
        {
          public void run()
          {
            submitActionListener.actionPerformed(new ActionEvent(result, 0,
              "showSubstitutionDialogReturned"));
          }
        }.start();
      }
    });
    buttonBox.add(button);

    JScrollPane spane = new JScrollPane(itemBox,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    spane.setBorder(BorderFactory.createEmptyBorder());
    dialog.add(spane);

    Box vbox = Box.createVerticalBox();
    vbox.add(headers);
    vbox.add(spane);
    vbox.add(Box.createVerticalGlue());
    vbox.add(buttonBox);
    vbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    dialog.add(vbox);

    dialog.pack();
    int frameWidth = dialog.getWidth();
    int frameHeight = dialog.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    dialog.setLocation(x, y);
    dialog.setVisible(true);
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
        public void run()
        {
          try
          {
            abort();
          }
          catch (Exception x)
          {
          }
          ;
        }
      });
    }
    catch (Exception x)
    {
    }
  }

  /**
   * Zeigt den Dialog an, der die Serienbriefverarbeitung (Direktdruck oder in neues
   * Dokument) anwirft.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void showMailmergeTypeSelectionDialog()
  {
    final JDialog dialog = new JDialog(myFrame, L.m("Seriendruck"), true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    Box vbox = Box.createVerticalBox();
    vbox.setBorder(new EmptyBorder(8,5,10,5));
    dialog.add(vbox);

    Box hbox = Box.createHorizontalBox();
    JLabel label = new JLabel(L.m("Serienbriefe"));
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(5));

    Vector<String> types = new Vector<String>();
    types.add(L.m("in neues Dokument schreiben"));
    types.add(L.m("auf dem Drucker ausgeben"));
    final JComboBox typeBox = new JComboBox(types);
    typeBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent e)
      {
        printIntoDocument = (typeBox.getSelectedIndex() == 0);
      }
    });
    hbox.add(typeBox);

    vbox.add(hbox);
    vbox.add(Box.createVerticalStrut(5));

    Box selectBox = Box.createVerticalBox();
    Border border = BorderFactory.createTitledBorder(
      BorderFactory.createLineBorder(Color.GRAY),
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
    start.addKeyListener(nonNumericKeyConsumer);
    hbox.add(start);
    hbox.add(Box.createHorizontalStrut(5));
    label = new JLabel("Bis");
    hbox.add(label);
    hbox.add(Box.createHorizontalStrut(5));

    final JTextField end = new JTextField(4);
    end.addKeyListener(nonNumericKeyConsumer);

    DocumentListener rangeDocumentListener = new DocumentListener()
    {
      public void update()
      {
        rangebutton.setSelected(true);
        datasetSelectionType = DatasetSelectionType.RANGE;
        try
        {
          rangeStart = Integer.parseInt(start.getText());
        }
        catch (Exception x)
        {
        }
        try
        {
          rangeEnd = Integer.parseInt(end.getText());
        }
        catch (Exception x)
        {
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
    final JRadioButton einzelauswahlRadioButton = new JRadioButton("");
    hbox.add(einzelauswahlRadioButton);
    radioGroup.add(einzelauswahlRadioButton);

    ActionListener einzelauswahlActionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        einzelauswahlRadioButton.setSelected(true);
        datasetSelectionType = DatasetSelectionType.INDIVIDUAL;
        // TODO showEinzelauswahlDialog();
      }
    };

    einzelauswahlRadioButton.addActionListener(einzelauswahlActionListener);

    JButton button = new JButton(L.m("Einzelauswahl..."));
    hbox.add(button);
    hbox.add(Box.createHorizontalGlue());
    button.addActionListener(einzelauswahlActionListener);

    selectBox.add(hbox);
    vbox.add(selectBox);
    vbox.add(Box.createVerticalStrut(5));

    hbox = Box.createHorizontalBox();
    button = new JButton(L.m("Abbrechen"));
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
        dialog.dispose();
        doMailMerge();
      }
    });
    hbox.add(button);

    vbox.add(hbox);

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

        public void actionPerformed(ActionEvent e)
        {
          mod.insertMailMergeFieldAtCursorPosition(name);
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
   * @author Matthias Benkmann (D-III-ITD 5.1) TODO Testen
   */
  private void showInsertSpecialFieldPopup(JComponent invoker, int x, int y)
  {
    boolean dsHasFields = ds.getColumnNames().size() > 0;
    final TrafoDialog editFieldDialog = getTrafoDialogForCurrentSelection();

    JPopupMenu menu = new JPopupMenu();

    JMenuItem button;

    final String genderButtonName = L.m("Gender");
    button = new JMenuItem(genderButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy für leere Gender-Funktion zusammenbauen.
        ConfigThingy genderConf = GenderDialog.generateGenderTrafoConf(
          ds.getColumnNames().get(0).toString(), "", "", "");
        insertFieldFromTrafoDialog(ds.getColumnNames(), genderButtonName, genderConf);
      }
    });
    menu.add(button);

    final String iteButtonName = L.m("Wenn...Dann...Sonst...");
    button = new JMenuItem(iteButtonName);
    button.setEnabled(dsHasFields);
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // ConfigThingy für leere WennDannSonst-Funktion zusammenbauen. Aufbau:
        // IF(STRCMP(VALUE '<firstField>', '') THEN('') ELSE(''))
        ConfigThingy ifConf = new ConfigThingy("IF");
        ConfigThingy strCmpConf = ifConf.add("STRCMP");
        strCmpConf.add("VALUE").add(ds.getColumnNames().get(0).toString());
        strCmpConf.add("");
        ifConf.add("THEN").add("");
        ifConf.add("ELSE").add("");
        insertFieldFromTrafoDialog(ds.getColumnNames(), iteButtonName, ifConf);
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Datensatznummer"));
    button.setEnabled(false); // NOT YET IMPLEMENTED
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // TODO insertDatasetIndex();
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Serienbriefnummer"));
    button.setEnabled(false); // NOT YET IMPLEMENTED
    button.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        // TODO insertMailMergeIndex();
      }
    });
    menu.add(button);

    button = new JMenuItem(L.m("Feld bearbeiten..."));
    button.setEnabled(editFieldDialog != null);
    button.addActionListener(new ActionListener()
    {
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
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            mod.replaceSelectionWithTrafoField(status.conf, buttonName);
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
    ConfigThingy trafoConf = mod.getFormFieldTrafoFromSelection();
    if (trafoConf == null) return null;

    final String trafoName = trafoConf.getName();

    TrafoDialogParameters params = new TrafoDialogParameters();
    params.conf = trafoConf;
    params.isValid = true;
    params.fieldNames = ds.getColumnNames();
    params.closeAction = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        TrafoDialog dialog = (TrafoDialog) e.getSource();
        TrafoDialogParameters status = dialog.getExitStatus();
        if (status.isValid)
        {
          try
          {
            mod.setTrafo(trafoName, status.conf);
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
   * Führt den Seriendruck durch.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void doMailMerge()
  {
    // TODO Fortschrittsanzeiger
    // TODO hier kann man mit lockControllers auf das Gesamtdokument vielleicht noch
    // etwas Performance rausholen - das bitte testen.
    mod.collectNonWollMuxFormFields();
    QueryResultsWithSchema data = ds.getData();
    final XPrintModel pmod = mod.createPrintModel(true);
    try
    {
      pmod.setPropertyValue("MailMergeNew_Schema", data.getSchema());
      pmod.setPropertyValue(PROP_QUERYRESULTS, data);
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }
    pmod.usePrintFunction("MailMergeNewSetFormValue");
    if (printIntoDocument) pmod.usePrintFunction("Gesamtdokument");

    // Drucken im Hintergrund, damit der EDT nicht blockiert.
    new Thread()
    {
      public void run()
      {
        mod.setFormFieldsPreviewMode(true);
        pmod.printWithProps();
        mod.setFormFieldsPreviewMode(previewMode);
      }
    }.start();
  }

  /**
   * PrintFunction, die das jeweils nächste Element der Seriendruckdaten nimmt und
   * die Seriendruckfelder im Dokument entsprechend setzt. Herangezogen werden die
   * Properties {@link #PROP_QUERYRESULTS} (ein Objekt vom Typ {@link QueryResults})
   * und "MailMergeNew_Schema", was ein Set mit den Spaltennamen enthält. Dies
   * funktioniert natürlich nur dann, wenn pmod kein Proxy ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void mailMergeNewSetFormValue(XPrintModel pmod) throws Exception
  {
    QueryResults data = (QueryResults) pmod.getPropertyValue(PROP_QUERYRESULTS);
    Collection schema = (Collection) pmod.getPropertyValue("MailMergeNew_Schema");

    Iterator iter = data.iterator();

    while (iter.hasNext())
    {
      if(pmod.isCanceled()) return;
      
      Dataset ds = (Dataset) iter.next();
      Iterator schemaIter = schema.iterator();
      while (schemaIter.hasNext())
      {
        String spalte = (String) schemaIter.next();
        pmod.setFormValue(spalte, ds.get(spalte));
      }
      pmod.printWithProps();
    }
  }

  private static class NonNumericKeyConsumer implements KeyListener
  {
    public void keyTyped(KeyEvent e)
    {
      char c = e.getKeyChar();
      if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))))
      {
        e.consume();
      }
    }

    public void keyPressed(KeyEvent e)
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }
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
    mod.removeCoupledWindow(myFrame);
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

  public static void main(String[] args) throws Exception
  {
    UNO.init();
    Logger.init(Logger.ALL);
    WollMuxSingleton.initialize(UNO.defaultContext);
    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
    if (doc == null)
    {
      System.err.println(L.m("Vordergrunddokument ist kein XTextDocument!"));
      System.exit(1);
    }

    MailMergeNew mm = new MailMergeNew(new TextDocumentModel(doc), null);

    while (mm.myFrame == null)
      Thread.sleep(1000);
    while (mm.myFrame != null)
      Thread.sleep(1000);
    System.exit(0);
  }
}
