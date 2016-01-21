package de.muenchen.allg.itd51.wollmux.former;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.dialog.Common;

public class FormularMax4kView extends JFrame
{
  /**
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      controller.abort();
    }
  };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  private static final long serialVersionUID = 1L;

  private final FormularMax4kController controller;

  private MyWindowListener windowCloseListener;

  private LeftPanel leftPanel;

  private RightPanel rightPanel;

  private JPanel nonExistingRightPanel;

  private JSplitPane mainContentPanel;

  private int defaultDividerSize;

  public FormularMax4kView(String title, FormularMax4kController controller)
      throws HeadlessException
  {
    super(title);

    this.controller = controller;

    Common.setLookAndFeelOnce();
    createGUI();
  }

  private void createGUI()
  {
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    windowCloseListener = new MyWindowListener();
    // der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert
    // wird
    addWindowListener(windowCloseListener);

    // WollMux-Icon für das Frame
    Common.setWollMuxIcon(this);

    leftPanel = controller.createLeftPanel();
    rightPanel = controller.createRightPanel();

    // damit sich Slider von JSplitPane vernünftig bewegen lässt.
    rightPanel.getComponent().setMinimumSize(new Dimension(100, 0));
    nonExistingRightPanel = new JPanel();
    nonExistingRightPanel.setMinimumSize(new Dimension(0, 0));
    nonExistingRightPanel.setPreferredSize(nonExistingRightPanel.getMinimumSize());
    nonExistingRightPanel.setMaximumSize(nonExistingRightPanel.getMinimumSize());
    mainContentPanel =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel.getComponent(),
        nonExistingRightPanel);
    mainContentPanel.setResizeWeight(1.0);
    defaultDividerSize = mainContentPanel.getDividerSize();
    mainContentPanel.setDividerSize(0);

    getContentPane().add(mainContentPanel);

    setJMenuBar(createMainMenu());

    initEditor();

    setFrameSize();
    setResizable(true);

  }

  private JMenuBar createMainMenu()
  {
    JMenuBar mainMenuBar = new JMenuBar();

    JMenu menu = createMenuDatei();
    mainMenuBar.add(menu);

    menu = createMenuBearbeiten();
    mainMenuBar.add(menu);

    menu = createMenuAnsicht();
    mainMenuBar.add(menu);

    menu = createMenuFormular();
    mainMenuBar.add(menu);

    return mainMenuBar;
  }

  private JMenu createMenuFormular()
  {
    JMenu menu = new JMenu(L.m("Formular"));

    JMenuItem menuItem = new JMenuItem(L.m("Formularfelder aus Vorlage einlesen"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.scan();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Formulartitel setzen"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        String newTitle =
          JOptionPane.showInputDialog(FormularMax4kView.this, L.m("Bitte Formulartitel eingeben"),
            controller.getFormTitle());

        if (newTitle != null && !newTitle.isEmpty())
        {
          controller.setFormTitle(newTitle);
          setFrameSize();
        }
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Druckfunktionen setzen"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.setPrintFunction();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Dateiname vorgeben"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.setFilenameGeneratorFunction();
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("WollMux-Formularmerkmale aus Vorlage entfernen"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.deForm();
      }
    });
    menu.add(menuItem);

    /*
     * Das Entfernen von Bookmarks kann Referenzfelder (Felder die Kopien anderer
     * Teile des Dokuments enthalten) zerstören, da diese dann ins Leere greifen.
     * Solange dies nicht erkannt wird, muss die Funktion deaktiviert sein.
     */
    if (Integer.valueOf(3).equals(Integer.valueOf(0)))
    {
      menuItem = new JMenuItem(L.m("Ladezeit des Dokuments optimieren"));
      menuItem.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          controller.removeNonWMBookmarks();
        }
      });
      menu.add(menuItem);
    }

    menuItem = new JMenuItem(L.m("Formularbeschreibung editieren"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.editFormDescriptor();
      }
    });
    menu.add(menuItem);
    return menu;
  }

  private JMenu createMenuAnsicht()
  {
    JMenu menu = new JMenu(L.m("Ansicht"));

    final ViewVisibilityDescriptor viewVisibilityDescriptor =
      new ViewVisibilityDescriptor();

    JMenuItem menuItem = new JCheckBoxMenuItem("ID");
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewId =
          ((AbstractButton) e.getSource()).isSelected();
        controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewId);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("LABEL");
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewLabel =
          ((AbstractButton) e.getSource()).isSelected();
        controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewLabel);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TOOLTIP");
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewTooltip =
          ((AbstractButton) e.getSource()).isSelected();
        controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewTooltip);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TYPE");
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewType =
          ((AbstractButton) e.getSource()).isSelected();
        controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewType);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem(L.m("Elementspezifische Felder"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewAdditional =
          ((AbstractButton) e.getSource()).isSelected();
        controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewAdditional);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("READONLY");
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        viewVisibilityDescriptor.formControlLineViewReadonly =
          ((AbstractButton) e.getSource()).isSelected();
        controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
      }
    });
    menuItem.setSelected(viewVisibilityDescriptor.formControlLineViewReadonly);
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TRAFO, PLAUSI, AUTOFILL, GROUPS");
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (((AbstractButton) e.getSource()).isSelected())
        {
          mainContentPanel.setDividerSize(defaultDividerSize);
          mainContentPanel.setRightComponent(rightPanel.getComponent());
          mainContentPanel.setResizeWeight(0.6);
        }
        else
        {
          mainContentPanel.setDividerSize(0);
          mainContentPanel.setRightComponent(nonExistingRightPanel);
          mainContentPanel.setResizeWeight(1.0);
        }
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menu.addSeparator();
    menuItem = new JMenuItem(L.m("Funktionstester"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.showFunctionTester();
      }
    });
    menu.add(menuItem);
    return menu;
  }

  private JMenu createMenuBearbeiten()
  {
    JMenu menu = new JMenu(L.m("Bearbeiten"));

    JMenu submenu = new JMenu(L.m("Standardelemente einfügen"));

    if (!createStandardelementeMenuNew(submenu))
      createStandardelementeMenuOld(submenu);

    menu.add(submenu);

    menu.addSeparator();

    JMenuItem menuItem = new JMenuItem(L.m("Checkboxen zu ComboBox"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        ComboboxMergeDescriptor desc = leftPanel.mergeCheckboxesIntoCombobox();
        controller.mergeCheckBoxesIntoComboBox(desc);
      }
    });

    menu.add(menuItem);
    return menu;
  }

  private JMenu createMenuDatei()
  {
    JMenu menu = new JMenu(L.m("Datei"));

    JMenuItem menuItem = new JMenuItem(L.m("Speichern"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.save();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Speichern unter..."));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.saveAs();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Beenden"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.abort();
      }
    });

    menu.add(menuItem);
    return menu;
  }
  
  /**
   * Initialisiert die GUI für den Quelltexteditor.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void initEditor()
  {
//    JMenu menu;
//    JMenuItem menuItem;
//    editorMenuBar = new JMenuBar();
//    // ========================= Datei ============================
//    menu = new JMenu(L.m("Datei"));
//
//    menuItem = new JMenuItem(L.m("Speichern"));
//    menuItem.addActionListener(new ActionListener()
//    {
//      public void actionPerformed(ActionEvent e)
//      {
//        try
//        {
//          ConfigThingy conf =
//            new ConfigThingy("", null, new StringReader(editor.getText()));
//          myFrame.setJMenuBar(mainMenuBar);
//          myFrame.getContentPane().remove(editorContentPanel);
//          myFrame.getContentPane().add(mainContentPanel);
//          initModelsAndViews(conf);
//          documentNeedsUpdating();
//        }
//        catch (Exception e1)
//        {
//          JOptionPane.showMessageDialog(myFrame, e1.getMessage(),
//            L.m("Fehler beim Parsen der Formularbeschreibung"),
//            JOptionPane.WARNING_MESSAGE);
//        }
//      }
//    });
//    menu.add(menuItem);
//
//    menuItem = new JMenuItem(L.m("Abbrechen"));
//    menuItem.addActionListener(new ActionListener()
//    {
//      public void actionPerformed(ActionEvent e)
//      {
//        myFrame.setJMenuBar(mainMenuBar);
//        myFrame.getContentPane().remove(editorContentPanel);
//        myFrame.getContentPane().add(mainContentPanel);
//        setFrameSize();
//      }
//    });
//    menu.add(menuItem);
//
//    editorMenuBar.add(menu);
//
//    Workarounds.applyWorkaroundForOOoIssue102164();
//    editor = new JEditorPane("text/plain", "");
//    editor.setEditorKit(new NoWrapEditorKit());
//
//    editor.setFont(new Font("Monospaced", Font.PLAIN, editor.getFont().getSize() + 2));
//    JScrollPane scrollPane =
//      new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
//        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
//    editorContentPanel = new JPanel(new BorderLayout());
//    editorContentPanel.add(scrollPane, BorderLayout.CENTER);
  }
  
  public void close()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen nötig, damit der FormularMax4000 gc'ed werden kann. Die Befehle
     * sorgen dafür, dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager)
     * indirekt über den JFrame den FM4000 kennt.
     */
    removeWindowListener(windowCloseListener);
    getContentPane().removeAll();
    setJMenuBar(null);
    
    dispose();
  }

  private class MyWindowListener implements WindowListener
  {
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
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
   * Workaround für Problem unter Windows, dass das Layout bei myFrame.pack() die
   * Taskleiste nicht berücksichtigt (das Fenster also dahinter verschwindet),
   * zumindest solange nicht bis man die Taskleiste mal in ihrer Größe verändert hat.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setFrameSize()
  {
    pack();
    fixFrameSize(this);
  }

  /**
   * Sorgt dafür, dass die Ausdehnung von frame nicht die maximal erlaubten
   * Fensterdimensionen überschreitet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void fixFrameSize(JFrame frame)
  {
    Rectangle maxWindowBounds;

    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();
    String lafName = UIManager.getSystemLookAndFeelClassName();
    if (!lafName.contains("plaf.windows.")) maxWindowBounds.height -= 32; // Sicherheitsabzug
    // für KDE
    // Taskleiste

    Rectangle frameBounds = frame.getBounds();
    if (frameBounds.x < maxWindowBounds.x)
    {
      frameBounds.width -= (maxWindowBounds.x - frameBounds.x);
      frameBounds.x = maxWindowBounds.x;
    }
    if (frameBounds.y < maxWindowBounds.y)
    {
      frameBounds.height -= (maxWindowBounds.y - frameBounds.y);
      frameBounds.y = maxWindowBounds.y;
    }
    if (frameBounds.width > maxWindowBounds.width)
      frameBounds.width = maxWindowBounds.width;
    if (frameBounds.height > maxWindowBounds.height)
      frameBounds.height = maxWindowBounds.height;
    frame.setBounds(frameBounds);
  }
  
  /**
   * Wertet den Konfigurationsabschnitt FormularMax4000/Standardelemente aus und fügt
   * submenu entsprechende Einträge hinzu.
   * 
   * @return false, falls der Konfigurationsabschnitt nicht existiert.
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TESTED
   */
  private boolean createStandardelementeMenuNew(JMenu submenu)
  {
    boolean found = false;
    for (ConfigThingy fm4000conf : WollMuxSingleton.getInstance().getWollmuxConf().query(
      "FormularMax4000", 1))
    {
      for (ConfigThingy eleConf : fm4000conf.query("Standardelemente", 1))
      {
        found = true;
        for (ConfigThingy conf : eleConf)
        {
          try
          {
            String label = conf.get("LABEL", 1).toString();
            ConfigThingy tabConf = conf.query("Tab", 1);
            if (tabConf.count() > 1)
              throw new ConfigurationErrorException(
                L.m("Mehr als ein Tab-Abschnitt"));
            if (tabConf.count() == 1)
            {
              JMenuItem menuItem;
              menuItem = new JMenuItem(L.m(label));
              final ConfigThingy tabConfEntry =
                tabConf.getFirstChild().getFirstChild();
              menuItem.addActionListener(new ActionListener()
              {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                  controller.insertStandardTab(tabConfEntry, null);
                  setFrameSize();
                }
              });
              submenu.add(menuItem);

            }
            else
            {
              ConfigThingy buttonsConf = conf.query("Buttons", 1);
              if (buttonsConf.count() > 1)
                throw new ConfigurationErrorException(
                  L.m("Mehr als ein Buttons-Abschnitt"));
              if (buttonsConf.count() == 0)
                throw new ConfigurationErrorException(
                  L.m("Weder Tab noch Buttons-Abschnitt"));

              final ConfigThingy buttonsConfEntry = buttonsConf.getFirstChild();

              JMenuItem menuItem = new JMenuItem(L.m(label));
              menuItem.addActionListener(new ActionListener()
              {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                  controller.insertStandardButtons(buttonsConfEntry, null, leftPanel.getButtonInsertionIndex());
                  setFrameSize();
                }
              });
              submenu.add(menuItem);

            }
          }
          catch (Exception x)
          {
            Logger.error(
              L.m("Fehler beim Parsen des Abschnitts FormularMax4000/Standardelemente"),
              x);
          }
        }
      }
    }
    return found;
  }

  /**
   * Fügt submenu die alten im WollMux gespeicherten Standardelemente-Einträge hinzu.
   * Sollte nur verwendet werden, wenn der entsprechende Konfigurationsabschnitt in
   * der wollmux,conf fehlt.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  private void createStandardelementeMenuOld(JMenu submenu)
  {
    JMenuItem menuItem;
    menuItem = new JMenuItem(L.m("Empfängerauswahl-Tab"));
    
    final URL EMPFAENGER_TAB_URL =
      this.getClass().getClassLoader().getResource(
        "data/empfaengerauswahl_controls.conf");
    
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.insertStandardTab(null, EMPFAENGER_TAB_URL);
        setFrameSize();
      }
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen, <-Zurück, Weiter->"));
    
    final URL STANDARD_BUTTONS_MIDDLE_URL =
        this.getClass().getClassLoader().getResource("data/standardbuttons_mitte.conf");

    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.insertStandardButtons(null, STANDARD_BUTTONS_MIDDLE_URL, leftPanel.getButtonInsertionIndex());
        setFrameSize();
      }
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen, <-Zurück, PDF, Drucken"));
    
    final URL STANDARD_BUTTONS_LAST_URL =
      this.getClass().getClassLoader().getResource(
        "data/standardbuttons_letztes.conf");
    
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        controller.insertStandardButtons(null, STANDARD_BUTTONS_LAST_URL, leftPanel.getButtonInsertionIndex());
        setFrameSize();
      }
    });
    submenu.add(menuItem);
  }
}
