/* Copyright (C) 2009 Matthias S. Benkmann
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
 * 
 * @author Matthias S. Benkmann
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;

/**
 * Bietet einen Dialog zur Bearbeitung eines UIElement-beschreibenden
 * {@link ConfigThingy}s an.
 */
public class UIElementConfigThingyEditor
{
  /**
   * Dieser String wird herangezogen um die Breite der JLabels mit den Attributnamen
   * zu bestimmen. Kein Attribut sollte je länger sein als dieser String.
   */
  private static final String MAX_ATTRIBUTE_NAME_STRING = "MSBRULEZ";

  /**
   * Bildet den Namen eines Attributs auf die Liste aller ACTIONs ab für die dieses
   * Attribut erforderlich ist. Wenn ein Attribut nicht in der Map enthalten ist als
   * Schlüssel, so wird es für alle ACTIONs angezeigt.
   */
  private static Map<String, String[]> mapAttributeNameToListOfActionsForWhichItIsNeeded;
  static
  {
    mapAttributeNameToListOfActionsForWhichItIsNeeded =
      new HashMap<String, String[]>();
    mapAttributeNameToListOfActionsForWhichItIsNeeded.put("FRAG_ID", new String[] {
      "openTemplate", "openDocument" });
    mapAttributeNameToListOfActionsForWhichItIsNeeded.put("EXT",
      new String[] { "openExt" });
    mapAttributeNameToListOfActionsForWhichItIsNeeded.put("URL",
      new String[] { "openExt" });
    mapAttributeNameToListOfActionsForWhichItIsNeeded.put("OPEN",
      new String[] { "open" });

    /*
     * FAVO und menu sind nur bei Menüs interessant, gehören also nicht zu einer
     * ACTION Damit sie nicht immer ausgegeben werden, ordnen wir sie einer
     * Fantasie-ACTION zu
     */
    mapAttributeNameToListOfActionsForWhichItIsNeeded.put("FAVO",
      new String[] { "Enidnu" });
    mapAttributeNameToListOfActionsForWhichItIsNeeded.put("MENU",
      new String[] { "Enidnu" });
  }

  private JAttributeEditor type;

  private JAttributeEditor action;

  private JTextAttributeEditor menu;

  private JFavoAttributeEditor favo;

  private JTextAttributeEditor label;

  private Frame parent;

  private ConfigThingy conf;

  private ActionListener finishedListener;

  private List<AttributeEditor> attributeEditors = new Vector<AttributeEditor>();

  private JDialog myDialog;

  private UIElementConfigThingyEditor(Frame parent, ConfigThingy conf,
      ActionListener finishedListener)
  {
    this.parent = parent;
    this.conf = conf;
    this.finishedListener = finishedListener;

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
    myDialog = new JDialog(parent, L.m("GUI Element Bearbeiten"));
    myDialog.setModal(true);
    myDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myDialog.addWindowListener(new MyWindowListener());

    Box myBox = Box.createVerticalBox();
    myDialog.setContentPane(myBox);
    myBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    label = new JTextAttributeEditor("LABEL", 0, false, false);
    myDialog.add(label);
    attributeEditors.add(label);

    JShortTextAttributeEditor hotkey = new JShortTextAttributeEditor("HOTKEY", 2);
    myDialog.add(hotkey);
    attributeEditors.add(hotkey);

    type = new JTypeAttributeEditor();
    myDialog.add(type);
    attributeEditors.add(type);

    favo = new JFavoAttributeEditor();
    myDialog.add(favo);
    attributeEditors.add(favo);

    action = new JActionAttributeEditor();
    myDialog.add(action);
    attributeEditors.add(action);

    JTextAttributeEditor fragid =
      new JTextAttributeEditor("FRAG_ID", 0, false, false);
    myDialog.add(fragid);
    attributeEditors.add(fragid);

    menu = new JTextAttributeEditor("MENU", 0, false, false);
    myDialog.add(menu);
    attributeEditors.add(menu);

    JShortTextAttributeEditor ext = new JShortTextAttributeEditor("EXT", 6);
    myDialog.add(ext);
    attributeEditors.add(ext);

    JTextAttributeEditor url = new JTextAttributeEditor("URL", 0, false, false);
    myDialog.add(url);
    attributeEditors.add(url);

    JTextAttributeEditor open = new JTextAttributeEditor("OPEN", 10, false, true);
    myDialog.add(open);
    attributeEditors.add(open);

    myDialog.add(Box.createVerticalGlue());

    myDialog.add(Box.createVerticalStrut(4));
    myDialog.add(DimAdjust.maxHeightIsPrefMaxWidthUnlimited(new JSeparator(
      SwingConstants.HORIZONTAL)));
    myDialog.add(Box.createVerticalStrut(4));

    Box buttonBox = Box.createHorizontalBox();
    myDialog.add(buttonBox);
    buttonBox.add(new JButton(new AbstractAction(L.m("Abbrechen"))
    {
      private static final long serialVersionUID = 2595972767797238686L;

      public void actionPerformed(ActionEvent e)
      {
        myDialog.dispose();
        finishedListener.actionPerformed(new ActionEvent(this, 0, "CANCEL"));
      }
    }));
    buttonBox.add(Box.createHorizontalGlue());

    buttonBox.add(new JButton(new AbstractAction(L.m("OK"))
    {
      private static final long serialVersionUID = 6641563851370571888L;

      public void actionPerformed(ActionEvent e)
      {
        myDialog.dispose();
        finishedListener.actionPerformed(new ActionEvent(getConfigThingy(), 0, "OK"));
      }
    }));

    setEditorVisibility();
    myDialog.pack();

    Rectangle parentBounds = parent.getBounds();
    int frameWidth = myDialog.getWidth();
    int frameHeight = myDialog.getHeight();
    int x = parentBounds.x + parentBounds.width / 2 - frameWidth / 2;
    int y = parentBounds.y + parentBounds.height / 2 - frameHeight / 2;
    myDialog.setLocation(x, y);
    myDialog.setResizable(true);

    myDialog.setVisible(true);
  }

  private void setEditorVisibility()
  {
    String type = this.type.getAttributeValue();
    for (AttributeEditor editor : attributeEditors)
      editor.setEditorVisible(false);

    this.type.setEditorVisible(true);

    if (type.equals("menu"))
    {
      menu.setEditorVisible(true);
      favo.setEditorVisible(true);
      label.setEditorVisible(true);
    }
    else if (type.equals("button"))
    {
      for (AttributeEditor editor : attributeEditors)
      {
        String action = this.action.getAttributeValue();
        String name = editor.getAttributeName();
        if (!mapAttributeNameToListOfActionsForWhichItIsNeeded.containsKey(name))
          editor.setEditorVisible(true);
        else
        {
          for (String action2 : mapAttributeNameToListOfActionsForWhichItIsNeeded.get(name))
            if (action2.equals(action)) editor.setEditorVisible(true);
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
      myDialog.dispose();
      finishedListener.actionPerformed(new ActionEvent(this, 0, "CANCEL"));
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

  private ConfigThingy getConfigThingy()
  {
    ConfigThingy conf = new ConfigThingy("");
    String type = this.type.getAttributeValue();

    if (type.equals("menu"))
    {
      conf.add("TYPE").add(type);
      conf.add("MENU").add(menu.getAttributeValue());
      String favoStr = favo.getAttributeValue();
      if (favoStr.equals("1")) conf.add("FAVO").add(favoStr);
      conf.add("LABEL").add(label.getAttributeValue());
    }
    else if (type.equals("button"))
    {
      for (AttributeEditor editor : attributeEditors)
      {
        String action = this.action.getAttributeValue();
        String name = editor.getAttributeName();
        if (!mapAttributeNameToListOfActionsForWhichItIsNeeded.containsKey(name))
          conf.add(name).add(editor.getAttributeValue());
        else
        {
          for (String action2 : mapAttributeNameToListOfActionsForWhichItIsNeeded.get(name))
            if (action2.equals(action))
              conf.add(name).add(editor.getAttributeValue());
        }
      }
    }
    else
      conf.add("TYPE").add(type);

    return conf;
  }

  /**
   * Zeigt einen Dialog zum Bearbeiten von originalConf an, was ein
   * UIElement-beschreibendes ConfigThingy sein muss. Diese Funktion kehrt sofort
   * zurück, nicht erst wenn der Dialog geschlossen wurde.
   * 
   * @param parent
   *          zu dieser Komponente ist der Dialog modal
   * @param listen
   *          wird wenn der Benutzer das Editieren beendet hat aufgerufen. Als
   *          ActionCommand des {@link ActionEvent}s wird "OK" oder "CANCEL"
   *          übergeben. Im "OK" Fall wird als Source das geänderte ConfigThingy
   *          übergeben.
   * 
   */
  public static void showEditDialog(Frame parent, ConfigThingy originalConf,
      ActionListener listen)
  {
    new UIElementConfigThingyEditor(parent, originalConf, listen);
  }

  private static abstract class JAttributeEditor extends JPanel implements
      AttributeEditor
  {
    private static final long serialVersionUID = -4979751095800681712L;

    protected Box mainEditorVBox;

    private String attributeName;

    public JAttributeEditor(String attributeName, boolean addGlue)
    {
      this.attributeName = attributeName;
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      Box vbox = Box.createVerticalBox();
      this.add(vbox);
      JLabel attributeLabel = new JLabel(MAX_ATTRIBUTE_NAME_STRING);
      Dimension preferredSize = new Dimension(attributeLabel.getPreferredSize());
      attributeLabel.setText(attributeName);
      attributeLabel.setPreferredSize(preferredSize);
      vbox.add(attributeLabel);
      if (addGlue) vbox.add(Box.createVerticalGlue());

      mainEditorVBox = Box.createVerticalBox();
      this.add(mainEditorVBox);
    }

    public String getAttributeName()
    {
      return attributeName;
    }

    public void setEditorVisible(boolean visible)
    {
      this.setVisible(visible);
    }
  }

  private class JTextAttributeEditor extends JAttributeEditor
  {
    private static final long serialVersionUID = -3689716244286247562L;

    protected JTextComponent myTextComponent;

    protected Box hboxAroundMyTextComponent;

    /**
     * Erzeugt einen Editor für ein Text-Attribut.
     * 
     * @param attributeName
     *          der Name des Attributs.
     * @param lines
     *          falls 0 wird ein JTextField verwendet, ansonsten eine JTextArea mit
     *          lines Zeilen.
     * @param addGlue
     *          falls true wird ein vertikaler glue unter der Textkomponente
     *          eingefügt
     * @param scrollable
     *          falls <code>true</code> zeigt der Editor bei Bedarf ScrollBars an
     */
    public JTextAttributeEditor(String attributeName, int lines, boolean addGlue,
        boolean scrollable)
    {
      super(attributeName, false);

      hboxAroundMyTextComponent = Box.createHorizontalBox();
      if (lines == 0)
      {
        myTextComponent =
          (JTextComponent) DimAdjust.maxHeightIsPrefMaxWidthUnlimited(new JTextField());
        hboxAroundMyTextComponent.add(myTextComponent);
        mainEditorVBox.add(hboxAroundMyTextComponent);
        if (addGlue) mainEditorVBox.add(Box.createVerticalGlue());
      }
      else
      {
        JTextArea ta = new JTextArea();
        ta.setLineWrap(false);
        ta.setRows(lines);

        JPanel panel = new JPanel(new GridLayout(1, 1));
        JScrollPane scrollPane = new JScrollPane(ta);
        if (scrollable)
        {
          scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
          scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        }
        else
        {
          scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
          scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        }
        panel.add(scrollPane);

        myTextComponent = ta;
        hboxAroundMyTextComponent.add(panel);
        mainEditorVBox.add(hboxAroundMyTextComponent);
      }

      if (attributeName.equals("MENU")) myTextComponent.setEnabled(false);

      try
      {
        if (attributeName.equals("OPEN"))
        {
          myTextComponent.setEnabled(false);
          myTextComponent.setText(conf.get(attributeName).stringRepresentation(true,
            '"'));
        }
        else
        {
          myTextComponent.setText(conf.get(attributeName).toString());
        }
      }
      catch (NodeNotFoundException x)
      {}
    }

    public String getAttributeValue()
    {
      return myTextComponent.getText();
    }
  }

  private class JShortTextAttributeEditor extends JTextAttributeEditor
  {
    private static final long serialVersionUID = -6387855504728566060L;

    /**
     * Editor for a String whose {@link JTextField} is dimensioned for len
     * characters.
     */
    public JShortTextAttributeEditor(String attributeName, int len)
    {
      super(attributeName, 0, false, false);
      String str = myTextComponent.getText();
      StringBuilder buffy = new StringBuilder("X");
      while (len-- > 1)
        buffy.append('X');
      myTextComponent.setText(buffy.toString());
      DimAdjust.fixedSize(myTextComponent);
      myTextComponent.setText(str);
      hboxAroundMyTextComponent.add(Box.createHorizontalGlue());
    }
  }

  private class JTypeAttributeEditor extends JAttributeEditor
  {
    private static final long serialVersionUID = 8511287349879988272L;

    private JComboBox<String> combo;

    public JTypeAttributeEditor()
    {
      super("TYPE", false);
      combo = new JComboBox<String>(new String[] {
        "button", "separator", "glue", "senderbox", "searchbox" }); // "menu" NICHT!
      prepareComboBox(combo, "TYPE");

      if (combo.getSelectedItem().equals("menu")) combo.setEnabled(false);

      Box hbox = Box.createHorizontalBox();
      hbox.add(DimAdjust.fixedSize(combo));
      hbox.add(Box.createHorizontalGlue());
      mainEditorVBox.add(hbox);
      // mainEditorVBox.add(Box.createVerticalGlue());
    }

    public String getAttributeValue()
    {
      return combo.getSelectedItem().toString();
    }
  }

  private class JFavoAttributeEditor extends JAttributeEditor
  {
    private static final long serialVersionUID = 6025044029161150947L;

    private JCheckBox check;

    public JFavoAttributeEditor()
    {
      super("FAVO", false);
      check = new JCheckBox();
      try
      {
        String favo = conf.get("FAVO").toString();
        check.setSelected(favo.equals("1"));
      }
      catch (NodeNotFoundException x)
      {}

      Box hbox = Box.createHorizontalBox();
      hbox.add(check);
      hbox.add(new JLabel(
        " ("
          + L.m("Ist diese Checkbox aktiviert, wird das Menü als Favoriten-Menü behandelt")
          + ")"));
      hbox.add(Box.createHorizontalGlue());
      mainEditorVBox.add(hbox);
      // mainEditorVBox.add(Box.createVerticalGlue());
    }

    public String getAttributeValue()
    {
      return check.isSelected() ? "1" : "0";
    }
  }

  private class JActionAttributeEditor extends JAttributeEditor
  {
    private static final long serialVersionUID = -6633764674615890730L;

    private JComboBox<String> combo;

    public JActionAttributeEditor()
    {
      super("ACTION", false);
      Vector<String> actions = new Vector<String>(WollMuxBar.SUPPORTED_ACTIONS);
      actions.remove("open"); // FIXME: "open"-Actions werden nicht unterstützt!
      Collections.sort(actions);
      combo = new JComboBox<String>(actions);

      prepareComboBox(combo, "ACTION");

      if (combo.getSelectedItem().equals("menu")) combo.setEnabled(false);

      Box hbox = Box.createHorizontalBox();
      hbox.add(DimAdjust.fixedSize(combo));
      hbox.add(Box.createHorizontalGlue());
      mainEditorVBox.add(hbox);
      // mainEditorVBox.add(Box.createVerticalGlue());
    }

    public String getAttributeValue()
    {
      return combo.getSelectedItem().toString();
    }
  }

  private void prepareComboBox(JComboBox<String> combo, String attributeName)
  {
    combo.setEditable(false);
    try
    {
      String type = conf.get(attributeName).toString();
      boolean found = false;
      for (int i = 0; i < combo.getItemCount(); ++i)
        if (combo.getItemAt(i).equals(type))
        {
          // Falls combo.setSelectedItem() mit == statt equals arbeitet
          type = (String) combo.getItemAt(i);
          found = true;
          break;
        }

      if (!found) combo.addItem(type);
      combo.setSelectedItem(type);

      combo.addItemListener(new ItemListener()
      {
        public void itemStateChanged(ItemEvent e)
        {
          myDialog.pack();
          setEditorVisibility();
        }
      });
    }
    catch (NodeNotFoundException x)
    {}
  }

  private interface AttributeEditor
  {
    public String getAttributeName();

    public String getAttributeValue();

    public void setEditorVisible(boolean visible);
  }

}
