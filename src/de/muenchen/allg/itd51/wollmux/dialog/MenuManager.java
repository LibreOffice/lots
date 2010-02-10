/* Copyright (C) 2009 Matthias S. Benkmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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
 * *
 * @author Matthias S. Benkmann
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Eine GUI zum Bearbeiten der Menüstruktur der WollMuxBar.
 */
public class MenuManager
{
  private static final String UIELEMENT_WITHOUT_TYPE_ERR =
    L.m("Menüeintrag ohne TYPE-Attribut gefunden");

  /**
   * Text der an verschiedenen Stellen verwendet wird.
   */
  private static final String NEW_FILES_TEXT = L.m("Neue Datei(en)...");

  private static final String[] BUTTONLEISTE_PATH = new String[] {
    "Symbolleisten", "Briefkopfleiste" };

  private static final String[] MENUELEISTE_PATH = new String[] { "Menueleiste" };

  private static DataFlavor[] TRANSFER_DATAFLAVORS;
  static
  {
    try
    {
      TRANSFER_DATAFLAVORS =
        new DataFlavor[] {
          new DataFlavor(MyTransferable.class, "Menu Manager Item"),
          new DataFlavor("text/plain; charset=utf-8") };
    }
    catch (ClassNotFoundException x)
    {}
  }

  /**
   * Die vom Administrator vorgegebene wollmux.conf
   */
  private ConfigThingy defaultConf;

  /**
   * Die wollmuxbar.conf des Benutzers.
   */
  private ConfigThingy userConf;

  /**
   * Die Wurzel des Menübaumes. Direkt darunter liegen die {@link Node}s für
   * Buttonleiste und Menüleiste.
   */
  private Node menuTreeRoot;

  /**
   * Das Hauptfenster der GUI.
   */
  private JFrame myFrame;

  /**
   * Auf {@link #myFrame} registriert als WindowListener.
   */
  private MyWindowListener oehrchen;

  /**
   * Der JTree, der die wichtigste GUI Komponente ist.
   */
  private JTree myTree;

  /**
   * Das zu {@link #myTree} gehörende {@link MyTreeModel}.
   */
  private MyTreeModel myTreeModel;

  /**
   * Das "Bearbeiten" Menü als Popup für den Rechts-Klick.
   */
  private JPopupMenu editMenuPopup = new JPopupMenu();

  /**
   * Der {@link JFileChooser} wird nach erstmaliger Initialisierung immer wieder
   * verwendet, damit das zuletzt gesetzte Verzeichnis immer wieder angeboten.
   */
  private JFileChooser fileChooser;

  /**
   * Wird nach dem Schließen des Dialogs aufgerufen.
   */
  private ActionListener finishedAction;

  /**
   * Zeigt eine GUI an, über die die Menüstruktur der WollMuxBar bearbeitet werden
   * kann. Alle Änderungen werden in die Datei wollmuxbar.conf geschrieben.
   * 
   * @param defaultConf
   *          Die vom Administrator vorgegebene wollmux.conf
   * @param userConf
   *          Die wollmuxbar.conf des Benutzers.
   * @param finishedAction
   *          wird aufgerufen wenn Dialog geschlossen wurde. Darf null sein.
   */
  public MenuManager(ConfigThingy defaultConf, ConfigThingy userConf,
      ActionListener finishedAction)
  {
    this.defaultConf = defaultConf;
    this.userConf = userConf;
    this.finishedAction = finishedAction;
    this.menuTreeRoot = parseMenuTree(defaultConf, userConf);
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        createGUI();
      }
    });
  }

  private void createGUI()
  {
    myFrame = new JFrame(L.m("Menü-Manager"));
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    oehrchen = new MyWindowListener();
    myFrame.addWindowListener(oehrchen);

    Common.setWollMuxIcon(myFrame);

    myTreeModel = new MyTreeModel();
    myTree = new JTree(myTreeModel);
    myTree.setEditable(false);
    myTree.setDragEnabled(true);
    myTree.setTransferHandler(new MyTransferHandler());
    myTree.setDropMode(DropMode.ON_OR_INSERT);
    myTree.setExpandsSelectedPaths(true);
    myTree.setRootVisible(false);
    myTree.setToggleClickCount(1);
    myTree.setPreferredSize(new Dimension(500, 600));
    myTree.setVisibleRowCount(menuTreeRoot.children.get(0).children.size()
      + menuTreeRoot.children.get(1).children.size() + 10);
    myTree.expandPath(new TreePath(new Object[] {
      menuTreeRoot, myTreeModel.getChild(menuTreeRoot, 1) }));
    myTree.addMouseListener(new MyMouseListener());

    JScrollPane scrollPane =
      new JScrollPane(myTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    JPanel myContentPanel = new JPanel(new BorderLayout());
    myContentPanel.add(scrollPane, BorderLayout.CENTER);
    myContentPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

    JMenuBar menubar = new JMenuBar();
    myFrame.setJMenuBar(menubar);
    JMenu menu = new JMenu(L.m("Datei"));
    menubar.add(menu);
    JMenuItem menuItem;
    menuItem = new JMenuItem(new AbstractAction(L.m("Speichern"))
    {
      public void actionPerformed(ActionEvent e)
      {
        save();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Schließen"))
    {
      public void actionPerformed(ActionEvent e)
      {
        closeAfterQuestion();
      }
    });
    menu.add(menuItem);

    menu = new JMenu(L.m("Bearbeiten"));
    menubar.add(menu);

    createEditMenu(menu);
    createEditMenu(editMenuPopup);

    myFrame.setContentPane(myContentPanel);
    myFrame.pack();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    // frameHeight = screenSize.height * 8 / 10;
    // myFrame.setSize(frameWidth, frameHeight);
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(true);

    myFrame.setVisible(true);
  }

  /**
   * Fügt menu die Menüpunkte für das Bearbeiten-Menü hinzu.
   * 
   */
  private void createEditMenu(JComponent menu)
  {
    JMenuItem menuItem = new JMenuItem(new AbstractAction(L.m("Bearbeiten..."))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.editProperties();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Standard wiederherstellen"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.restoreStandard();
      }
    });
    menu.add(menuItem);
    addSeparator(menu);
    menuItem = new JMenuItem(new AbstractAction(L.m("Neues (Unter)Menü"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newSubMenu();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(NEW_FILES_TEXT)
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newFile();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Neuer Separator"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newSeparator();
      }
    });
    menu.add(menuItem);
    menuItem = new JMenuItem(new AbstractAction(L.m("Neues ..."))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.newSomething();
      }
    });
    menu.add(menuItem);
    addSeparator(menu);
    menuItem = new JMenuItem(new AbstractAction(L.m("Löschen"))
    {
      public void actionPerformed(ActionEvent e)
      {
        myTreeModel.deleteSelection();
      }
    });
    menu.add(menuItem);
  }

  /**
   * Fügt menu einen für die jeweilige Menüart passenden Separator hinzu.
   * 
   */
  private void addSeparator(JComponent menu)
  {
    if (menu instanceof JPopupMenu)
      menu.add(new JPopupMenu.Separator());
    else
      menu.add(new JSeparator(SwingConstants.HORIZONTAL));
  }

  /**
   * Liefert das empfohlene Startverzeichnis für einen {@link JFileChooser}.
   * 
   * TESTED
   */
  private File getFileChooserStartDirectory()
  {
    try
    {
      return new File(WollMuxFiles.getDEFAULT_CONTEXT().toURI());
    }
    catch (Exception x)
    {}

    String userHome = System.getProperty("user.home");
    return new File(userHome);
  }

  /**
   * Versucht aus der absoluten URL von file eine URL zu machen, die relativ zum
   * DEFAULT_CONTEXT ist. Alternativ wird die Variable ${user.home} in die URL
   * eingebaut, wenn sie unterhalb dieses Verzeichnisses liegt und dies günstiger
   * erscheint. Falls diese Heuristiken nicht fruchten wird eine absolute URL
   * zurückgeliefert.
   * 
   * TESTED
   */
  private String getRelativeURLifPossible(File file)
  {
    String path = canonicalPath(file);

    try
    {
      File defaultContextFile = new File(WollMuxFiles.getDEFAULT_CONTEXT().toURI());
      String defaultContextPath = canonicalPath(defaultContextFile);

      String relativePath = makeRelative(path, defaultContextPath);
      if (relativePath != null) return relativePath;
    }
    catch (Exception x)
    {
      // Hier fliegen wir z.B raus, wenn der DEFAULT_CONTEXT keine file: URL ist.
    }

    String userHome = System.getProperty("user.home");
    String homePath = canonicalPath(new File(userHome));
    String relativePath = makeRelative(path, homePath);
    if (relativePath != null) return turnIntoURL("${user.home}/" + relativePath);

    try
    {
      return file.toURI().toURL().toExternalForm();
    }
    catch (MalformedURLException x)
    {
      return "file:" + file.toString();
    }
  }

  private String turnIntoURL(String path)
  {
    return "file:" + path.replaceAll("[" + File.separator + "]", "/");
  }

  private String makeRelative(String pathToMakeRel,
      String pathRelativeToWhichItShouldBe)
  {
    if (pathToMakeRel.startsWith(pathRelativeToWhichItShouldBe))
    {
      pathToMakeRel =
        pathToMakeRel.substring(pathRelativeToWhichItShouldBe.length());
      if (pathToMakeRel.startsWith(File.separator))
        return pathToMakeRel.substring(1);
      else
        return pathToMakeRel;
    }
    else
      return null;
  }

  private String canonicalPath(File file)
  {
    String canonicalPath;
    /*
     * Derzeit auskommentiert, weil ich nicht sicher bin, ob das Auflösen von
     * Symlinks nicht in manchen Anwendungsfällen stört.
     */
    // try
    // {
    // canonicalPath = file.getCanonicalPath();
    // }
    // catch (IOException x)
    // {
    canonicalPath = file.getAbsolutePath();
    // }
    return canonicalPath;
  }

  private class MyTreeModel implements TreeModel
  {
    private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

    public void addTreeModelListener(TreeModelListener l)
    {
      if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l)
    {
      listeners.remove(l);
    }

    public Object getChild(Object parent, int index)
    {
      Node parentNode = (Node) parent;
      try
      {
        return parentNode.children.get(index);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
      return null;
    }

    public int getChildCount(Object parent)
    {
      return ((Node) parent).children.size();
    }

    public int getIndexOfChild(Object parent, Object child)
    {
      if (parent != null && child != null)
        return ((Node) parent).children.indexOf(child);
      return -1;
    }

    public Object getRoot()
    {
      return menuTreeRoot;
    }

    public boolean isLeaf(Object n)
    {
      Node node = (Node) n;
      if (node.isMenuOrBar()) return false;
      return true;
    }

    public void valueForPathChanged(TreePath path, Object newValue)
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Entfernt alle durch paths bezeichneten Knoten aus dem Baum.
     * 
     * TESTED
     */
    public void nuke(TreePath[] paths)
    {
      for (TreePath path : paths)
      {
        // Root und die Leisten können nicht gelöscht werden.
        if (path.getPathCount() < 3) continue;
        TreePath parentPath = path.getParentPath();
        Node removedNode = (Node) path.getLastPathComponent();
        Node parentNode = ((Node) parentPath.getLastPathComponent());
        int childIndex = getIndexOfChild(parentNode, removedNode);
        parentNode.removeChild(removedNode);

        for (TreeModelListener listen : listeners)
          listen.treeNodesRemoved(new TreeModelEvent(this, parentPath,
            new int[] { childIndex }, new Object[] { removedNode }));

        if (!parentNode.userModified)
        {
          parentNode.userModified = true;
          for (TreeModelListener listen : listeners)
            listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
        }
      }

    }

    /**
     * Fügt Kopien von den Nodes die durch paths beschrieben sind an index in den
     * Node identifiziert durch parentPath ein.
     * 
     * TESTED
     */
    public void copy(TreePath[] paths, TreePath parentPath, int index)
    {
      Node parentNode = (Node) parentPath.getLastPathComponent();

      for (TreePath path : paths)
      {
        // Root und die Leisten können nicht kopiert werden.
        if (path.getPathCount() < 3) continue;
        Node sourceNode = (Node) path.getLastPathComponent();
        parentNode.recursiveCopy(sourceNode, index);

        for (TreeModelListener listen : listeners)
        {
          listen.treeNodesInserted(new TreeModelEvent(this, parentPath,
            new int[] { index }, new Object[] { sourceNode }));
        }
        ++index;
      }

      if (!parentNode.userModified)
      {
        parentNode.userModified = true;
        for (TreeModelListener listen : listeners)
          listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
      }
    }

    /**
     * Fügt newNode (was ein neuer unabhängiger Node sein muss) als Referenz in den
     * durch parentPath bezeichneten Knoten ein an Position index.
     * 
     * TESTED
     */
    public void insert(Node newNode, TreePath parentPath, int index)
    {
      Node parentNode = (Node) parentPath.getLastPathComponent();

      parentNode.children.add(index, newNode);

      for (TreeModelListener listen : listeners)
      {
        listen.treeNodesInserted(new TreeModelEvent(this, parentPath,
          new int[] { index }, new Object[] { newNode }));
      }

      if (!parentNode.userModified)
      {
        parentNode.userModified = true;
        for (TreeModelListener listen : listeners)
          listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
      }
    }

    /**
     * Nimmt das erste aktuell ausgewählte Menü und reinitialisiert es, wobei nur
     * Daten aus {@link MenuManager#defaultConf} verwendet werden, keine aus
     * userConf.
     * 
     */
    public void restoreStandard()
    {
      ConfigThingy noUserConf = new ConfigThingy("wollmuxbarConf");

      TreePath selection = myTree.getSelectionPath();
      if (selection == null) return;
      if (!confirm(
        L.m("Änderungen aufheben?"),
        L.m("Wollen Sie wirklich alle Änderungen\ndes ausgewählten Menüs und\naller Untermenüs aufheben?")))
        return;
      if (selection.getPathCount() == 1)
      {
        menuTreeRoot = parseMenuTree(defaultConf, noUserConf);
        for (TreeModelListener listen : listeners)
          listen.treeStructureChanged((new TreeModelEvent(this,
            new Object[] { menuTreeRoot })));
      }
      else if (selection.getPathCount() == 2)
      {
        Node node = (Node) selection.getLastPathComponent();
        String[] menuPath;
        if (node.conf.getName().equals("Menueleiste"))
          menuPath = MENUELEISTE_PATH;
        else
          menuPath = BUTTONLEISTE_PATH;

        ActiveConfigSection leisteSection;
        try
        {
          leisteSection = getActiveConfigSection(menuPath, defaultConf, noUserConf);
        }
        catch (NodeNotFoundException x)
        {
          leisteSection =
            new ActiveConfigSection(new ConfigThingy(node.conf.getName()), true);
        }

        node.children.clear();
        node.userModified = leisteSection.userModified;
        node.conf = new ConfigThingy(leisteSection.conf);
        parseMenuTreeRecursive(node, node.conf, defaultConf, noUserConf,
          new HashSet<String>());
        for (TreeModelListener listen : listeners)
          listen.treeStructureChanged((new TreeModelEvent(this, selection)));
      }
      else
      { // if (selection.getPathCount() > 2)
        Node node = (Node) selection.getLastPathComponent();
        if (!node.isMenuOrBar()) return; // Wir können nur ganze Menüs
        // wiederherstellen

        String menuId = null;
        try
        {
          menuId = node.conf.get("MENU").toString();
        }
        catch (NodeNotFoundException x)
        {
          // Unmöglich, da der initiale Parse-Durchgang schon getestet hat.
        }

        ActiveConfigSection menuSection;
        try
        {
          menuSection = getActiveConfigSection(new String[] {
            "Menues", menuId, "Elemente" }, defaultConf, noUserConf);
        }
        catch (NodeNotFoundException x)
        {
          menuSection = new ActiveConfigSection(new ConfigThingy("Elemente"), true);
        }

        node.children.clear();
        node.userModified = menuSection.userModified;
        node.conf = new ConfigThingy(menuSection.conf);
        parseMenuTreeRecursive(node, node.conf, defaultConf, noUserConf,
          new HashSet<String>());
        for (TreeModelListener listen : listeners)
          listen.treeStructureChanged((new TreeModelEvent(this, selection)));
      }
    };

    public void deleteSelection()
    {
      TreePath[] paths = myTree.getSelectionPaths();
      if (paths != null) nuke(paths);
    };

    public void newSubMenu()
    {
      if (!allowedInsertPositionSelected(true)) return;
      String menuName =
        JOptionPane.showInputDialog(myFrame, L.m("Name des neuen Menüs"),
          L.m("Neues (Unter)Menü"), JOptionPane.QUESTION_MESSAGE);
      if (menuName == null || menuName.length() == 0) return;
      ConfigThingy conf = new ConfigThingy("Elements");
      conf.add("TYPE").add("menu");
      conf.add("MENU").add(generateMenuId(menuName));
      conf.add("LABEL").add(menuName);
      Node newNode = new Node(menuName, new ArrayList<Node>(), true, conf);

      newNode(newNode, true);
    };

    /**
     * Erzeugt eine MENU ID die in keinem Menues-Abschnitt in
     * {@link MenuManager#userConf} oder {@link MenuManager#defaultConf} bisher
     * vorkommt und auch nirgends im Baum. Die Generierung orientiert sich an name.
     * 
     * TESTED
     */
    private String generateMenuId(String name)
    {
      name = name.replaceAll("\\W", "_");
      String baseid = "mm_" + name;
      for (int count = 0;; ++count)
      {
        String id = baseid;
        if (count > 0) id = id + count;

        try
        {
          if (findMenuRecursive(menuTreeRoot, id)) continue;

          getActiveConfigSection(new String[] {
            "Menues", id }, defaultConf, userConf);
        }
        catch (NodeNotFoundException x)
        {
          return id;
        }
      }
    }

    /**
     * Liefert true gdw, node selbst oder ein Nachfahre ein Menü ist mit
     * MENU-Attribut id.
     * 
     * TESTED
     */
    private boolean findMenuRecursive(Node node, String id)
    {
      // id.equals, NICHT node.menuId().equals, da letzteres null sein kann.
      if (id.equals(node.menuId())) return true;

      for (Node child : node.children)
        if (findMenuRecursive(child, id)) return true;

      return false;
    }

    /**
     * Fügt vor dem ersten ausgewählten Element (falls vorhanden und falls erlaubt)
     * einen Separator ein und liefert dessen TreePath zurück (oder null falls keiner
     * eingefügt wurde).
     */
    public TreePath newSeparator()
    {
      ConfigThingy conf = new ConfigThingy("");
      conf.add("TYPE").add("separator");
      Node newNode = new Node(getLabel(conf), new ArrayList<Node>(), false, conf);
      return newNode(newNode, false);
    }

    /**
     * Liefer true, wenn eine erlaubte Einfügeposition selektiert ist.
     * 
     * @param barsAllowed
     *          falls true gilt eine selektierte Menü- oder Buttonleiste als legal.
     */
    private boolean allowedInsertPositionSelected(boolean barsAllowed)
    {
      TreePath selectedPath = myTree.getSelectionPath();
      if (selectedPath == null) return false;
      // Kann auf/in/vor Wurzel nichts einfügen
      if (selectedPath.getPathCount() < 2) return false;
      if (!barsAllowed && selectedPath.getPathCount() < 3) return false;
      return true;
    }

    /**
     * Fügt vor dem ersten ausgewählten Element (falls vorhanden und falls erlaubt)
     * newNode ein (was ein frischer unabhängiger Node sein muss) und liefert den
     * TreePath des neuen Knoten zurück (oder null falls keiner eingefügt wurde).
     * 
     * @param appendIfMenuSelected
     *          falls true, so wird wenn ein Menü selektiert ist, ans Ende von diesem
     *          angehängt. Falls false, so wird auch bei selektiertem Menü davor
     *          eingefügt. In letzterem Fall sind die Menü- und Buttonleiste keine
     *          legalen Einfügepositionen und diese funktioniert tut nichts.
     * 
     */
    private TreePath newNode(Node newNode, boolean appendIfMenuSelected)
    {
      TreePath selectedPath = myTree.getSelectionPath();
      if (selectedPath == null) return null;
      // Kann vor/auf/in Wurzel nichts einfügen
      if (selectedPath.getPathCount() < 2) return null;
      if (!appendIfMenuSelected && selectedPath.getPathCount() < 3) return null;

      Node selectedNode = (Node) selectedPath.getLastPathComponent();
      if (appendIfMenuSelected && selectedNode.isMenuOrBar())
      {
        insert(newNode, selectedPath, getChildCount(selectedNode));
        return selectedPath.pathByAddingChild(newNode);
      }
      else
      {
        TreePath parentPath = selectedPath.getParentPath();
        Node parentNode = (Node) parentPath.getLastPathComponent();
        int index = getIndexOfChild(parentNode, selectedNode);
        insert(newNode, parentPath, index);
        return parentPath.pathByAddingChild(newNode);
      }

    };

    /**
     * Lässt den Benutzer in einem FileChooser Dateien auswählen und fügt für diese
     * entsprechende openExt-Menüeinträge ein. Wenn derzeit ein Menü selektiert ist,
     * werden die Einträge an dessen Ende angefügt. Ist ein anderer Eintrag
     * selektiert, so werden die Einträge davor eingefügt.
     * 
     * TESTED
     */
    public void newFile()
    {
      if (!allowedInsertPositionSelected(true))
      ;

      if (fileChooser == null)
      {
        fileChooser = new JFileChooser(getFileChooserStartDirectory());
      }

      fileChooser.setMultiSelectionEnabled(true);
      fileChooser.setAcceptAllFileFilterUsed(true);
      fileChooser.setFileHidingEnabled(false);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setDialogTitle(NEW_FILES_TEXT);
      fileChooser.setSelectedFiles(new File[] {});

      if (fileChooser.showOpenDialog(myFrame) != JFileChooser.APPROVE_OPTION)
        return;

      for (File file : fileChooser.getSelectedFiles())
        newFile(file);
    }

    /**
     * Falls momentan ein Menü selektiert ist, wird ein openExt-Eintrag für file an
     * sein Ende gesetzt. Falls was anderes selektiert ist, wird der Eintrag davor
     * eingefügt.
     * 
     * TESTED
     */
    private void newFile(File file)
    {
      ConfigThingy conf = new ConfigThingy("");
      String ext = "";
      String label = file.getName();
      int dotIdx = label.lastIndexOf('.');
      if (dotIdx >= 0)
      {
        ext = label.substring(dotIdx + 1);
        label = label.substring(0, dotIdx);
      }
      if (ext.length() == 0) ext = "<noext>";
      conf.add("LABEL").add(label);
      conf.add("TYPE").add("button");
      conf.add("ACTION").add("openExt");
      conf.add("EXT").add(ext);
      String url = getRelativeURLifPossible(file);
      conf.add("URL").add(url);
      Node newNode = new Node(label, new ArrayList<Node>(), false, conf);

      newNode(newNode, true);
    }

    public void newSomething()
    {
      ConfigThingy conf = new ConfigThingy("");
      conf.add("TYPE").add("button");
      conf.add("ACTION").add("abort");
      conf.add("LABEL").add("");
      Node newNode = new Node("", new ArrayList<Node>(), false, conf);
      TreePath path = newNode(newNode, true);
      if (path == null) return;
      myTree.setSelectionPath(path);
      editProperties();
    }

    /**
     * Öffnet einen modalen Dialog über den das zugehörige ConfigThingy des ersten
     * ausgewählten Elements des Baumes bearbeitet werden kann.
     * 
     * TESTED
     */
    public void editProperties()
    {
      final TreePath selectedPath = myTree.getSelectionPath();
      if (selectedPath == null) return;
      // Kann Wurzel oder ...leiste nicht bearbeiten
      if (selectedPath.getPathCount() < 3) return;
      final Node selectedNode = (Node) selectedPath.getLastPathComponent();
      final ConfigThingy originalConf = selectedNode.conf;
      UIElementConfigThingyEditor.showEditDialog(myFrame, originalConf,
        new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            if (e.getActionCommand().equals("OK"))
            {
              ConfigThingy changedConf = (ConfigThingy) e.getSource();
              propertiesHaveBeenEdited(selectedPath, selectedNode, originalConf,
                changedConf);
            }
          }
        });
    }

    private void propertiesHaveBeenEdited(TreePath selectedPath, Node selectedNode,
        ConfigThingy originalConf, ConfigThingy changedConf)
    {
      if (changedConf == null
        || originalConf.stringRepresentation().equals(
          changedConf.stringRepresentation())) return;

      selectedNode.conf = changedConf;
      selectedNode.label = getLabel(changedConf);

      TreePath parentPath = selectedPath.getParentPath();
      Node parentNode = (Node) parentPath.getLastPathComponent();
      parentNode.userModified = true;
      for (TreeModelListener listen : listeners)
        listen.treeNodesChanged(new TreeModelEvent(this, parentPath));
    }
  }

  /**
   * Zeigt einen modalen Dialog an mit Titel title und Frage message und liefert
   * true, wenn der Benutzer Ja anwählt.
   * 
   */
  private boolean confirm(String title, String message)
  {
    return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(myFrame, message,
      title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
  }

  private class MyTransferHandler extends TransferHandler
  {
    public int getSourceActions(JComponent c)
    {
      if (c == myTree)
        return COPY_OR_MOVE;
      else
        return NONE;
    }

    protected Transferable createTransferable(JComponent c)
    {
      if (c != myTree) return null;
      TreePath[] selection = myTree.getSelectionPaths();

      // We cannot drag the root or the 2 nodes below it
      for (TreePath path : selection)
        if (path.getPathCount() < 3) return null;

      return new MyTransferable(selection);
    }

    protected void exportDone(JComponent c, Transferable data, int action)
    {
      if (action == MOVE)
      {
        TreePath[] selection = myTree.getSelectionPaths();
        myTreeModel.nuke(selection);
      }
    }

    /**
     * Liefert true gdw path ein Nachfahre eines Favoriten-Menü-Knotens oder selbst
     * so ein Knoten ist.
     */
    private boolean isInFavoritesMenu(TreePath path)
    {
      for (int i = path.getPathCount() - 1; i > 1; --i)
      {
        if (((Node) path.getPathComponent(i)).isFavoritesMenu()) return true;
      }
      return false;
    }

    public boolean canImport(TransferHandler.TransferSupport trans)
    {
      try
      {
        MyTransferable transferable =
          (MyTransferable) trans.getTransferable().getTransferData(
            TRANSFER_DATAFLAVORS[0]);

        // Feststellen, ob alle markierten Elemente in einem Favoriten-Menü sind.
        TreePath[] sources = transferable.getDraggedStuff();
        boolean allinfavorites = true;
        for (TreePath path : sources)
        {
          if (!isInFavoritesMenu(path))
          {
            allinfavorites = false;
            break;
          }
        }

        JTree.DropLocation location = (JTree.DropLocation) trans.getDropLocation();
        TreePath dropPath = location.getPath();
        // kann weder auf noch in wurzel was droppen
        if (dropPath.getPathCount() < 2) return false;

        // Falls das dropTarget innerhalb von mindestens einer der Selektionen liegt,
        // dürfen wir nicht droppen, weil sonst der Node gelöscht würde
        for (TreePath path : sources)
        {
          if (path.isDescendant(dropPath)) return false;
        }

        // Falls wir von außerhalb eines Favoriten-Menüs in ein solches droppen, dann
        // immer COPY nehmen.
        if (isInFavoritesMenu(dropPath) && !allinfavorites)
          if (trans.isDrop()) trans.setDropAction(COPY);

        return true;
      }
      catch (Exception x)
      {
        return false;
      }
    }

    public boolean importData(TransferHandler.TransferSupport trans)
    {
      if (!canImport(trans)) return false;

      JTree.DropLocation location = (JTree.DropLocation) trans.getDropLocation();
      int childIndex = location.getChildIndex();
      TreePath dropPath = location.getPath();
      Node dropTarget = (Node) dropPath.getLastPathComponent();

      // Falls Drop auf einem Objekt, übersetzen in Einfügung dahinter bzw. am Ende
      if (childIndex < 0)
      {
        if (!dropTarget.isMenuOrBar())
        {
          Node oldDropTarget = dropTarget;
          dropPath = dropPath.getParentPath();
          dropTarget = (Node) dropPath.getLastPathComponent();
          childIndex = myTreeModel.getIndexOfChild(dropTarget, oldDropTarget);
        }
        else
          childIndex = dropTarget.children.size();
      }

      int action = trans.getDropAction();
      if (action == MOVE || action == COPY)
      {
        try
        {
          MyTransferable transferable =
            (MyTransferable) trans.getTransferable().getTransferData(
              TRANSFER_DATAFLAVORS[0]);
          TreePath[] paths = transferable.getDraggedStuff();
          myTreeModel.copy(paths, dropPath, childIndex);
          return true;
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      }
      return false;
    }
  }

  private class MyTransferable implements Transferable
  {
    private TreePath[] selection;

    public TreePath[] getDraggedStuff()
    {
      return selection;
    }

    public MyTransferable(TreePath[] selection)
    {
      this.selection = selection;
    }

    public DataFlavor[] getTransferDataFlavors()
    {
      return TRANSFER_DATAFLAVORS;
    }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
      if (flavor.equals(TRANSFER_DATAFLAVORS[0]))
      {
        return this;
      }
      else if (flavor.equals(TRANSFER_DATAFLAVORS[1]))
      {
        // FIXME: Wenn Export des Baums implementiert ist, hier nachrüsten
        return new ByteArrayInputStream("FIXME".getBytes("utf-8"));
      }
      else
        return null;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
      for (DataFlavor flavor2 : TRANSFER_DATAFLAVORS)
        if (flavor2.equals(flavor)) return true;
      return false;
    }

  }

  private class MyMouseListener implements MouseListener
  {
    public void mouseClicked(MouseEvent e)
    {}

    public void mouseEntered(MouseEvent e)
    {}

    public void mouseExited(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e)
    {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e)
    {
      if (e.isPopupTrigger())
      {
        Point p =
          SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(),
            myTree);
        TreePath path = myTree.getClosestPathForLocation(p.x, p.y);
        if (path != null)
        {
          myTree.setSelectionPath(path);
          Rectangle bounds = myTree.getPathBounds(path);
          editMenuPopup.show(myTree, e.getX() + 16, bounds.y + bounds.height);
        }
      }
    }
  }

  private class MyWindowListener implements WindowListener
  {
    public void windowActivated(WindowEvent e)
    {}

    public void windowClosed(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
      closeAfterQuestion();
    }

    public void windowDeactivated(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowIconified(WindowEvent e)
    {}

    public void windowOpened(WindowEvent e)
    {}

  }

  private void closeAfterQuestion()
  {
    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
      myFrame,
      L.m("Wollen Sie den Menü-Manager wirklich verlassen?\nNicht gespeicherte Änderungen gehen dabei verloren."),
      L.m("Menü-Manager verlassen?"), JOptionPane.YES_NO_OPTION,
      JOptionPane.QUESTION_MESSAGE)) dispose();
  }

  private void dispose()
  {
    /*
     * Wegen folgendem Java Bug (WONTFIX)
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4259304 sind die folgenden
     * 3 Zeilen nötig, damit die GUI gc'ed werden kann. Die Befehle sorgen dafür,
     * dass kein globales Objekt (wie z.B. der Keyboard-Fokus-Manager) indirekt über
     * den JFrame die GUI kennt.
     */
    myFrame.removeWindowListener(oehrchen);
    myFrame.getContentPane().remove(0);
    myFrame.setJMenuBar(null);

    myFrame.dispose();
    myFrame = null;

    if (finishedAction != null)
      finishedAction.actionPerformed(new ActionEvent(this, 0, ""));
  }

  private static class Node
  {
    /**
     * Die Beschriftung des Knotens im Baum.
     */
    public String label;

    /**
     * Die Kindknoten (nie null, aber womöglich leer).
     */
    public List<Node> children;

    /**
     * true gdw die Liste der Kindknoten aus der Benutzerkonfiguration kommt und
     * nicht aus der globalen Systemkonfiguration. ACHTUNG! Wenn das Label eines
     * Knotens geändert wird, setzt das nicht dessen userModified-Flag sondern das,
     * des Vaterknotens. Dies liegt daran, dass das LABEL im aufrufenden Menü
     * vergeben wird und nicht im aufgerufenen Menü.
     */
    public boolean userModified;

    /**
     * Das ConfigThingy das zu dem Node gehört. Mit Ausnahme der Wurzel und beiden
     * darunterliegenden Knoten für Button- und Menüleiste, ist dies ein
     * UIElement-beschreibendes ConfigThingy. ACHTUNG! Dies ist eine von defaultConf
     * bzw. userConf unabhängige Kopie. An ihr darf alles verändert werden außer bei
     * Menüs das MENU-Attribut (weil sonst "Standard wiederherstellen" nicht mehr
     * funktionieren würde).
     */
    public ConfigThingy conf;

    public Node(String label, List<Node> children, boolean userModified,
        ConfigThingy conf)
    {
      this.label = label;
      this.children = children;
      this.userModified = userModified;
      this.conf = new ConfigThingy(conf);
    }

    public String toString()
    { // TESTED
      ConfigThingy cids = conf.query("CONF_ID");
      if (cids.count() > 0)
      {
        boolean comma = false;
        StringBuilder buffy = new StringBuilder("[");

        for (ConfigThingy cidgroup : cids)
        {
          for (ConfigThingy cid : cidgroup)
          {
            if (comma)
              buffy.append(", ");
            else
              comma = true;
            buffy.append(cid.toString());
          }
        }
        buffy.append("] ");

        buffy.append(label);
        if (userModified) buffy.append('*');

        return buffy.toString();
      }

      return userModified ? label + "*" : label;
    }

    /**
     * Liefert true gdw diese Node ein Menü oder die Button- oder die Menüleiste
     * darstellt.
     */
    public boolean isMenuOrBar()
    {
      try
      {
        return conf.getName().length() > 0
          || conf.get("TYPE").toString().equals("menu");
      }
      catch (NodeNotFoundException x)
      {
        return false;
      }
    }

    /**
     * Liefert true gdw dies ein Menü ist, in dem der Benutzer Favoriten ablegt.
     */
    public boolean isFavoritesMenu()
    {
      try
      {
        return conf.get("FAVO").toString().equals("1");
      }
      catch (NodeNotFoundException x)
      {
        return false;
      }
    }

    /**
     * Liefert den Wert des MENU-Attributs oder null, falls es sich bei diesem Node
     * nicht um ein Menü handelt.
     */
    public String menuId()
    {
      try
      {
        return conf.get("MENU").toString();
      }
      catch (NodeNotFoundException x)
      {
        return null;
      }
    }

    /**
     * Entfernt child aus der Liste der Kinder von this.
     * 
     */
    public void removeChild(Node child)
    {
      children.remove(child);
    }

    /**
     * Fügt eine Kopie von sourceNode unter Index index in die Kinder von this ein.
     * Falls sourceNode Kinder hat, so werden diese rekursiv in den kopierten Knoten
     * kopiert.
     * 
     */
    public void recursiveCopy(Node sourceNode, int index)
    {
      children.add(index, new Node(sourceNode));
    }

    /**
     * Copy constructor (deep copy).
     */
    public Node(Node orig)
    {
      this(orig.label, new ArrayList<Node>(), orig.userModified, new ConfigThingy(
        orig.conf));
      for (Node child : orig.children)
        children.add(new Node(child));
    }
  }

  /**
   * Parst die Symbolleisten, Briefkopfleiste, Menueleiste, Menues und
   * WollMuxBarKonfigurationen-Abschnitte und liefert den Wurzel-Knoten des
   * Ergebnisbaumes zurück.
   * 
   * TESTED
   */
  private static Node parseMenuTree(ConfigThingy defaultConf, ConfigThingy userConf)
  {
    ActiveConfigSection buttonleisteSection;
    try
    {
      buttonleisteSection =
        getActiveConfigSection(BUTTONLEISTE_PATH, defaultConf, userConf);
    }
    catch (NodeNotFoundException x)
    {
      buttonleisteSection =
        new ActiveConfigSection(new ConfigThingy("Briefkopfleiste"), true);
    }

    ActiveConfigSection menueleisteSection;
    try
    {
      menueleisteSection =
        getActiveConfigSection(MENUELEISTE_PATH, defaultConf, userConf);
    }
    catch (NodeNotFoundException x)
    {
      menueleisteSection =
        new ActiveConfigSection(new ConfigThingy("Menueleiste"), true);
    }

    Node root =
      new Node("Wurzel", new ArrayList<Node>(), false, new ConfigThingy("Wurzel"));

    Node buttonleisteNode =
      new Node(L.m("Buttonleiste"), new ArrayList<Node>(),
        buttonleisteSection.userModified, buttonleisteSection.conf);
    root.children.add(buttonleisteNode);

    Node menueleisteNode =
      new Node(L.m("Menüleiste"), new ArrayList<Node>(),
        menueleisteSection.userModified, menueleisteSection.conf);
    root.children.add(menueleisteNode);

    parseMenuTreeRecursive(buttonleisteNode, buttonleisteNode.conf, defaultConf,
      userConf, new HashSet<String>());
    parseMenuTreeRecursive(menueleisteNode, menueleisteNode.conf, defaultConf,
      userConf, new HashSet<String>());

    return root;
  }

  /**
   * Wertet die Kinder von menuConf aus, die die übliche Form für UIElemente haben
   * müssen. Für jedes so beschriebene UIElement wird in node.children ein weiterer
   * Node hinzugefügt. Falls der TYPE des UIElements "menu" lautet, so wird dieses
   * Menü aus defaultConf bzw. userConf herausgesucht (die letzte Instanz gewinnt,
   * wobei userConf als nach defaultConf stehend zählt) und rekursiv verarbeitet.
   * 
   * @param alreadySeen
   *          Um rekursive Menüstrukturen zu unterbinden wird hier bei jedem
   *          Rekursschritt die ID des rekursiv betretenen Menüs hineingesteckt und
   *          der Aufbau wird abgebrochen wenn ein Menü hier bereits vorhanden ist.
   * 
   * TESTED
   */
  private static void parseMenuTreeRecursive(Node node, ConfigThingy menuConf,
      ConfigThingy defaultConf, ConfigThingy userConf, Set<String> alreadySeen)
  {
    for (ConfigThingy conf : menuConf)
    {
      String type;
      try
      {
        type = conf.get("TYPE").toString();
      }
      catch (NodeNotFoundException x)
      {
        Logger.error(UIELEMENT_WITHOUT_TYPE_ERR);
        continue;
      }

      String label = getLabel(conf);

      if (type.equals("menu"))
      {
        String menuId;
        try
        {
          menuId = conf.get("MENU").toString();
        }
        catch (NodeNotFoundException x)
        {
          Logger.error(L.m("'menu' Menüeintrag ohne MENU-Attribut gefunden"));
          continue;
        }

        if (alreadySeen.contains(menuId))
        {
          Logger.error(L.m(
            "Menü '%1' enthält sich direkt oder indirekt selbst als Untermenü",
            menuId));
          continue;
        }

        try
        {
          ActiveConfigSection menuSection = getActiveConfigSection(new String[] {
            "Menues", menuId, "Elemente" }, defaultConf, userConf);
          Node menuNode =
            new Node(label, new ArrayList<Node>(), menuSection.userModified, conf);

          node.children.add(menuNode);

          alreadySeen.add(menuId);
          parseMenuTreeRecursive(menuNode, menuSection.conf, defaultConf, userConf,
            alreadySeen);
          alreadySeen.remove(menuId);
        }
        catch (NodeNotFoundException x)
        {
          Logger.error(L.m("Menü '%1' nicht definiert", menuId));
          continue;
        }
      }
      else
      { // if (!type.equals("menu"))
        node.children.add(new Node(label, new ArrayList<Node>(), false, conf));
      }
    }
  }

  private void save()
  {
    if (!confirm(L.m("Menü speichern?"),
      L.m("Wollen Sie das neue Menü wirklich speichern?"))) return;

    ConfigThingy conf = new ConfigThingy("wollmuxbarconf");
    Node buttonleiste = menuTreeRoot.children.get(0);
    if (buttonleiste.userModified)
    {
      ConfigThingy subconf = conf.add("Symbolleisten").add("Briefkopfleiste");
      for (Node child : buttonleiste.children)
        subconf.addChild(new ConfigThingy(child.conf));
    }

    Node menueleiste = menuTreeRoot.children.get(1);
    if (menueleiste.userModified)
    {
      ConfigThingy subconf = conf.add("Menueleiste");
      for (Node child : menueleiste.children)
        subconf.addChild(new ConfigThingy(child.conf));
    }

    addUserModifiedMenuesRecursive(menuTreeRoot, conf.add("Menues"));

    File wollmuxbarConfFile =
      new File(WollMuxFiles.getWollMuxDir(), WollMuxBar.WOLLMUXBAR_CONF);
    try
    {
      WollMuxFiles.writeConfToFile(wollmuxbarConfFile, conf);
    }
    catch (Exception x)
    {
      Logger.error(x);
      JOptionPane.showMessageDialog(myFrame, L.m(
        "Beim Speichern ist ein Fehler aufgetreten:\n%1", x.getMessage()),
        L.m("Fehler beim Speichern"), JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Fügt conf für jedes Menü im Teilbaum der node als Wurzel hat, wenn es
   * userModified gesetzt hat, einen Abschnitt für dieses Menü hinzu.
   * 
   * TESTED
   */
  private void addUserModifiedMenuesRecursive(Node node, ConfigThingy conf)
  {
    String menu = null;
    if (node.userModified)
    {
      try
      {
        menu = node.conf.get("MENU", 1).toString();
      }
      catch (Exception x)
      {}

      if (menu != null)
      {
        ConfigThingy subconf = conf.add(menu).add("Elemente");
        for (Node child : node.children)
          subconf.addChild(new ConfigThingy(child.conf));
      }
    }

    for (Node child : node.children)
      addUserModifiedMenuesRecursive(child, conf);
  }

  /**
   * Liefert ein für das UIElement conf passendes Label für die Anzeige im Baum.
   * 
   */
  private static String getLabel(ConfigThingy conf)
  {
    String type = "";
    try
    {
      type = conf.get("TYPE").toString();
    }
    catch (NodeNotFoundException x)
    {
      Logger.error(UIELEMENT_WITHOUT_TYPE_ERR);
    }

    // Use type as default label (think of "glue" and "separator")
    String label = "--- " + type + " ---";
    try
    {
      if (!type.equals("separator") && !type.equals("glue"))
        label = conf.get("LABEL").toString();
    }
    catch (Exception x)
    {}
    return label;
  }

  /**
   * Sucht das letzte vorkommen eines Abschnitts sectionPath[N] der verschachtelt ist
   * Abschnitten sectionPath[N-1]...sectionPath[0] (wobei bei vorkommen von
   * Abschnitten auf verschiedenen tiefen des Baumes nur die oberste Ebene mit einem
   * sectionName-Abschnitt betrachtet wird (selbes verhalten wie
   * {@link ConfigThingy#query(String)}). Die userConf gilt als hinter der
   * defaultConf stehend.
   * 
   * @throws NodeNotFoundException
   *           falls kein entsprechender Abschnitt gefunden wurde.
   * 
   * TESTED
   */
  private static ActiveConfigSection getActiveConfigSection(String[] sectionPath,
      ConfigThingy defaultConf, ConfigThingy userConf) throws NodeNotFoundException
  {
    if (sectionPath.length == 0) throw new NodeNotFoundException();
    ConfigThingy conf = userConf;
    for (int i = 0; i < sectionPath.length; ++i)
      conf = conf.query(sectionPath[i]);
    if (conf.count() > 0) return new ActiveConfigSection(conf.getLastChild(), true);

    conf = defaultConf;
    for (int i = 0; i < sectionPath.length; ++i)
      conf = conf.query(sectionPath[i]);
    if (conf.count() > 0)
      return new ActiveConfigSection(conf.getLastChild(), false);

    StringBuilder buffy = new StringBuilder();
    for (String name : sectionPath)
      buffy.append("/" + name);

    throw new NodeNotFoundException(L.m(
      "Kein Konfigurationsabschnitt '%1' gefunden", buffy.toString()));
  }

  /**
   * Steht für einen Konfigurationsabschnitt in der wollmux,conf der nach dem
   * Letzter-Gewinnt-Prinzip bestimmt wurde.
   */
  private static class ActiveConfigSection
  {
    /**
     * Die Wurzel des Konfigurationsabschnitts.
     */
    public ConfigThingy conf;

    /**
     * true gdw der Konfigurationsabschnitt aus der Benutzerkonfiguration und nicht
     * aus der globalen Systemkonfiguration kommt.
     */
    public boolean userModified;

    public ActiveConfigSection(ConfigThingy conf, boolean userModified)
    {
      this.conf = conf;
      this.userModified = userModified;
    }
  }

  public static void main(String[] args)
  {
    WollMuxFiles.setupWollMuxDir();

    ConfigThingy wollmuxConf = WollMuxFiles.getWollmuxConf();

    ConfigThingy wollmuxbarConf = null;
    File wollmuxbarConfFile =
      new File(WollMuxFiles.getWollMuxDir(), WollMuxBar.WOLLMUXBAR_CONF);
    if (wollmuxbarConfFile.exists())
    {
      try
      {
        wollmuxbarConf =
          new ConfigThingy("wollmuxbarConf", wollmuxbarConfFile.toURI().toURL());
      }
      catch (Exception x)
      {
        Logger.error(
          L.m("Fehler beim Lesen von '%1'", wollmuxbarConfFile.toString()), x);
      }
    }

    if (wollmuxbarConf == null) wollmuxbarConf = new ConfigThingy("wollmuxbarConf");

    new MenuManager(wollmuxConf, wollmuxbarConf, new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        System.out.println("Finished");
      }
    });
  }

}
