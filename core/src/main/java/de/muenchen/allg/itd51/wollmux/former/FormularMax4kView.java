/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.util.L;

public class FormularMax4kView extends JFrame
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FormularMax4kView.class);

  private static final long serialVersionUID = 1L;

  private final transient FormularMax4kController controller;

  private transient MyWindowListener windowCloseListener;

  private transient LeftPanel leftPanel;

  private transient RightPanel rightPanel;

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
    
    setLocationRelativeTo(null);
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

    setFrameSize();
    setResizable(true);
    SwingUtilities.invokeLater(this::toFront);
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
    JMenu menu = new JMenu(L.m("Form"));

    JMenuItem menuItem = new JMenuItem(L.m("Get form fields from template"));
    menuItem.addActionListener(e -> {
      controller.scan();
      controller.updateDocument();
      setFrameSize();
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Set title of form"));
    menuItem.addActionListener(e -> {
      String newTitle = JOptionPane.showInputDialog(FormularMax4kView.this, L.m("Please enter form title"),
          controller.getTitle());

      if (newTitle != null && !newTitle.isEmpty())
      {
        controller.setFormTitle(newTitle);
        setFrameSize();
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Set print function"));
    menuItem.addActionListener(e -> {
      controller.setPrintFunction();
      setFrameSize();
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Enter file name"));
    menuItem.addActionListener(e -> {
      controller.setFilenameGeneratorFunction();
      setFrameSize();
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Remove WollMux-form-elements from template"));
    menuItem.addActionListener(e -> controller.deForm());
    menu.add(menuItem);

    /*
     * Das Entfernen von Bookmarks kann Referenzfelder (Felder die Kopien anderer
     * Teile des Dokuments enthalten) zerstören, da diese dann ins Leere greifen.
     * Solange dies nicht erkannt wird, muss die Funktion deaktiviert sein.
     */
    if (Integer.valueOf(3).equals(Integer.valueOf(0)))
    {
      menuItem = new JMenuItem(L.m("Optimize document loading time"));
      menuItem.addActionListener(e -> controller.removeNonWMBookmarks());
      menu.add(menuItem);
    }

    menuItem = new JMenuItem(L.m("Edit form description"));
    menuItem.addActionListener(e -> controller.editFormDescriptor());
    menu.add(menuItem);
    return menu;
  }

  private JMenu createMenuAnsicht()
  {
    JMenu menu = new JMenu(L.m("View"));

    final ViewVisibilityDescriptor viewVisibilityDescriptor =
      new ViewVisibilityDescriptor();

    JMenuItem menuItem = new JCheckBoxMenuItem("ID");
    menuItem.addActionListener(e -> {
      viewVisibilityDescriptor.setFormControlLineViewId(((AbstractButton) e.getSource()).isSelected());
      controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
    });
    menuItem.setSelected(viewVisibilityDescriptor.isFormControlLineViewId());
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("LABEL");
    menuItem.addActionListener(e -> {
      viewVisibilityDescriptor.setFormControlLineViewLabel(((AbstractButton) e.getSource()).isSelected());
      controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
    });
    menuItem.setSelected(viewVisibilityDescriptor.isFormControlLineViewLabel());
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TOOLTIP");
    menuItem.addActionListener(e -> {
      viewVisibilityDescriptor.setFormControlLineViewTooltip(((AbstractButton) e.getSource()).isSelected());
      controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
    });
    menuItem.setSelected(viewVisibilityDescriptor.isFormControlLineViewTooltip());
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TYPE");
    menuItem.addActionListener(e -> {
      viewVisibilityDescriptor.setFormControlLineViewType(((AbstractButton) e.getSource()).isSelected());
      controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
    });
    menuItem.setSelected(viewVisibilityDescriptor.isFormControlLineViewType());
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem(L.m("Element-specific fields"));
    menuItem.addActionListener(e -> {
      viewVisibilityDescriptor.setFormControlLineViewAdditional(((AbstractButton) e.getSource()).isSelected());
      controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
    });
    menuItem.setSelected(viewVisibilityDescriptor.isFormControlLineViewAdditional());
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("READONLY");
    menuItem.addActionListener(e -> {
      viewVisibilityDescriptor.setFormControlLineViewReadonly(((AbstractButton) e.getSource()).isSelected());
      controller.broadcast(new BroadcastViewVisibilitySettings(viewVisibilityDescriptor));
    });
    menuItem.setSelected(viewVisibilityDescriptor.isFormControlLineViewReadonly());
    menu.add(menuItem);

    menuItem = new JCheckBoxMenuItem("TRAFO, PLAUSI, AUTOFILL, GROUPS");
    menuItem.addActionListener(e -> {
      if (((AbstractButton) e.getSource()).isSelected())
      {
        mainContentPanel.setDividerSize(defaultDividerSize);
        mainContentPanel.setRightComponent(rightPanel.getComponent());
        mainContentPanel.setResizeWeight(0.6);
      } else
      {
        mainContentPanel.setDividerSize(0);
        mainContentPanel.setRightComponent(nonExistingRightPanel);
        mainContentPanel.setResizeWeight(1.0);
      }
      setFrameSize();
    });
    menu.add(menuItem);

    menu.addSeparator();
    menuItem = new JMenuItem(L.m("Function Tester"));
    menuItem.addActionListener(e -> controller.showFunctionTester());
    menu.add(menuItem);
    return menu;
  }

  private JMenu createMenuBearbeiten()
  {
    JMenu menu = new JMenu(L.m("Edit"));

    JMenu submenu = new JMenu(L.m("Insert main elements"));

    if (!createStandardelementeMenuNew(submenu))
      createStandardelementeMenuOld(submenu);

    menu.add(submenu);

    menu.addSeparator();

    JMenuItem menuItem = new JMenuItem(L.m("Checkboxes to ComboBox"));
    menuItem.addActionListener(e -> {
      ComboboxMergeDescriptor desc = leftPanel.mergeCheckboxesIntoCombobox();
      controller.mergeCheckBoxesIntoComboBox(desc);
    });

    menu.add(menuItem);
    return menu;
  }

  private JMenu createMenuDatei()
  {
    JMenu menu = new JMenu(L.m("File"));

    JMenuItem menuItem = new JMenuItem(L.m("Save"));
    menuItem.addActionListener(e -> controller.save());
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Save as..."));
    menuItem.addActionListener(e -> controller.saveAs());
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Exit"));
    menuItem.addActionListener(e -> controller.abort());

    menu.add(menuItem);
    return menu;
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

  private class MyWindowListener extends WindowAdapter
  {

    @Override
    public void windowClosing(WindowEvent e)
    {
      controller.abort();
    }
  }

  /**
   * Workaround für Problem unter Windows, dass das Layout bei myFrame.pack() die
   * Taskleiste nicht berücksichtigt (das Fenster also dahinter verschwindet),
   * zumindest solange nicht bis man die Taskleiste mal in ihrer Größe verändert hat.
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
    for (ConfigThingy fm4000conf : WollMuxFiles.getWollmuxConf().query(
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
                L.m("More than one Tab section"));
            if (tabConf.count() == 1)
            {
              JMenuItem menuItem;
              menuItem = new JMenuItem(L.m(label));
              final ConfigThingy tabConfEntry =
                tabConf.getFirstChild().getFirstChild();
              menuItem.addActionListener(e -> {
                controller.insertStandardTab(tabConfEntry, null);
                setFrameSize();
              });
              submenu.add(menuItem);

            }
            else
            {
              ConfigThingy buttonsConf = conf.query("Buttons", 1);
              if (buttonsConf.count() > 1)
                throw new ConfigurationErrorException(
                  L.m("More than one Buttons section"));
              if (buttonsConf.count() == 0)
                throw new ConfigurationErrorException(
                  L.m("No Tab or Buttons section"));

              final ConfigThingy buttonsConfEntry = buttonsConf.getFirstChild();

              JMenuItem menuItem = new JMenuItem(L.m(label));
              menuItem.addActionListener(e -> {
                controller.insertStandardButtons(buttonsConfEntry, null, leftPanel.getButtonInsertionIndex());
                setFrameSize();
              });
              submenu.add(menuItem);

            }
          }
          catch (Exception x)
          {
            LOGGER.error(
              L.m("Error while parsing the sections FormularMax4000/Standardelemente"),
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
    menuItem = new JMenuItem(L.m("Recipient Selection Tab"));
    
    final URL EMPFAENGER_TAB_URL =
      this.getClass().getClassLoader().getResource(
            "default_buttons/empfaengerauswahl_controls.conf");
    
    menuItem.addActionListener(e -> {
      controller.insertStandardTab(null, EMPFAENGER_TAB_URL);
      setFrameSize();
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Cancel, <-Back, Next->"));
    
    final URL STANDARD_BUTTONS_MIDDLE_URL =
        this.getClass().getClassLoader().getResource("default_buttons/standardbuttons_mitte.conf");

    menuItem.addActionListener(e -> {
      controller.insertStandardButtons(null, STANDARD_BUTTONS_MIDDLE_URL, leftPanel.getButtonInsertionIndex());
      setFrameSize();
    });
    submenu.add(menuItem);

    menuItem = new JMenuItem(L.m("Cancel, <-Back, PDF, Print"));
    
    final URL STANDARD_BUTTONS_LAST_URL =
      this.getClass().getClassLoader().getResource(
            "default_buttons/standardbuttons_letztes.conf");
    
    menuItem.addActionListener(e -> {
      controller.insertStandardButtons(null, STANDARD_BUTTONS_LAST_URL, leftPanel.getButtonInsertionIndex());
      setFrameSize();
    });
    submenu.add(menuItem);
    
    menuItem = new JMenuItem(L.m("Cancel, <-Back, Send as Email, Print"));//TODO
    
    final URL STANDARD_BUTTONS_EMAIL =
    	      this.getClass().getClassLoader().getResource(
            "default_buttons/standardbuttons_email.conf");
    	    
    menuItem.addActionListener(e -> {
      controller.insertStandardButtons(null, STANDARD_BUTTONS_EMAIL, leftPanel.getButtonInsertionIndex());
      setFrameSize();
    });
    submenu.add(menuItem);
  }
}
