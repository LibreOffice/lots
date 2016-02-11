package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.io.StringReader;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;

public class ConfigEditor extends JFrame
{
  private FormularMax4kController controller;
  private JEditorPane editor;

  public ConfigEditor(String title, FormularMax4kController controller) throws HeadlessException
  {
    super(title);

    this.controller = controller;
    
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    
    createGui();
    setPreferredSize(new Dimension(1024, 768));
    pack();
    setLocationRelativeTo(null);
  }
  
  public void setText(String text)
  {
    editor.setText(text);
  }

  private void createGui()
  {
    setJMenuBar(createMenu());

    setLayout(new BorderLayout());

    editor = new JEditorPane("text/plain", "");
    editor.setCaretPosition(0);
    editor.setEditorKit(new NoWrapEditorKit());

    editor.setFont(new Font("Monospaced", Font.PLAIN, editor.getFont().getSize() + 2));
    JScrollPane scrollPane =
      new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    JPanel editorContentPanel = new JPanel(new BorderLayout());
    editorContentPanel.add(scrollPane, BorderLayout.CENTER);

    add(editorContentPanel, BorderLayout.CENTER);
  }

  private JMenuBar createMenu()
  {
    JMenu menu;
    JMenuItem menuItem;
    JMenuBar editorMenuBar = new JMenuBar();
    // ========================= Datei ============================
    menu = new JMenu(L.m("Datei"));

    menuItem = new JMenuItem(L.m("Speichern"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        try
        {
          ConfigThingy conf =
            new ConfigThingy("", null, new StringReader(editor.getText()));
          controller.initModelsAndViews(conf);
          controller.documentNeedsUpdating();
          dispose();
        }
        catch (Exception e1)
        {
          JOptionPane.showMessageDialog(ConfigEditor.this, e1.getMessage(),
            L.m("Fehler beim Parsen der Formularbeschreibung"),
            JOptionPane.WARNING_MESSAGE);
        }
      }
    });
    menu.add(menuItem);

    menuItem = new JMenuItem(L.m("Abbrechen"));
    menuItem.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        dispose();
      }
    });
    menu.add(menuItem);

    editorMenuBar.add(menu);

    return editorMenuBar;
  }

  private static class NoWrapEditorKit extends DefaultEditorKit
  {
    private static final long serialVersionUID = -2741454443147376514L;

    private ViewFactory vf = null;

    @Override
    public ViewFactory getViewFactory()
    {
      if (vf == null) vf = new NoWrapFactory();
      return vf;
    };

    private static class NoWrapFactory implements ViewFactory, Serializable
    {
      private static final long serialVersionUID = -932935111327537530L;

      @Override
      public View create(Element e)
      {
        return new PlainView(e);
      }
    };
  };
}
