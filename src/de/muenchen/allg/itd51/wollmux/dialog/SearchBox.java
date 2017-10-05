package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Implementiert eine SearchBox, die in einem JTextField nach Menüeinträgen der
 * WollMuxBar suchen kann und so den Schnellzugriff auf bestimmte Menüeinträge
 * ermöglicht.
 * 
 * @author Christoph Lutz (privat)
 */
public class SearchBox
{
  /**
   * 
   */
  private final WollMuxBar wollMuxBar;

  private static final int MAX_SHOWN = 20;

  private static final int TEXTFIELD_COLUMNS = 12;

  private final JTextField textField;

  private final JPopupMenu menu;

  private final ConfigThingy menuConf;

  private boolean ignoreNextFocusRequest;

  public SearchBox(WollMuxBar wollMuxBar, final String label, ConfigThingy menuConf)
  {
    this.wollMuxBar = wollMuxBar;
    this.textField = new JTextField(L.m(label), TEXTFIELD_COLUMNS);
    this.menu = new JPopupMenu();
    this.menuConf = menuConf;
    this.ignoreNextFocusRequest = false;

    this.textField.putClientProperty("menu", menu);

    this.textField.addAncestorListener(new AncestorListener()
    {

      @Override
      public void ancestorRemoved(AncestorEvent event)
      {
        if (event.getComponent().isVisible())
          ((JPopupMenu) event.getComponent().getClientProperty("menu")).setVisible(false);
      }

      @Override
      public void ancestorMoved(AncestorEvent event)
      {}

      @Override
      public void ancestorAdded(AncestorEvent event)
      {}
    });

    textField.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent arg0)
      {
        for (Component compo : menu.getComponents())
        {
          if (compo instanceof JMenuItem)
          {
            JMenuItem item = (JMenuItem) compo;
            if (item.isArmed())
            {
              item.doClick();
              menu.setVisible(false);
              return;
            }
          }
        }
      }
    });

    // Der MouseListener wird benötigt, um den 'Suchen...'-String zu löschen, wenn
    // das Suchfeld direkt nach dem Start angeklickt wird. Dann hat das Suchfeld
    // bereits den Fokus und der FocusListener wird nicht ausgelöst.
    textField.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mousePressed(MouseEvent e)
      {
        if (textField.hasFocus() && textField.getText().equals(label))
        {
          textField.setText("");
        }
      }
    });

    textField.addFocusListener(new FocusListener()
    {
      @Override
      public void focusLost(FocusEvent arg0)
      {
        if (arg0.getOppositeComponent() == null)
        {
          arg0.getComponent().transferFocus();
        }
      }

      @Override
      public void focusGained(FocusEvent arg0)
      {
        if (ignoreNextFocusRequest)
        {
          ignoreNextFocusRequest = false;
          return;
        }

        // "Suchen..." löschen wenn der erste nutzerinitiiert Focus kommt
        if (arg0.getOppositeComponent() != null
          && textField.getText().equals(label)) textField.setText("");

        // Menü sichtbar machen, wenn nicht bereits sichtbar
        if (menu.getComponentCount() > 0 && !menu.isVisible())
        {
          menu.setVisible(true);
          textField.requestFocusInWindow();
        }

        // den ganzen Text markieren
        textField.setSelectionStart(0);
        textField.setSelectionEnd(textField.getText().length());
      }
    });

    textField.getDocument().addDocumentListener(new DocumentListener()
    {
      @Override
      public void changedUpdate(DocumentEvent e)
      {
        update(e);
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        update(e);
      }

      @Override
      public void insertUpdate(DocumentEvent e)
      {
        update(e);
      }

      private void update(DocumentEvent e)
      {
        Document doc = e.getDocument();
        String text = "";
        try
        {
          text = doc.getText(0, doc.getLength()).trim();
        }
        catch (BadLocationException e1)
        {}

        String[] words = null;
        if (text.length() > 0) words = text.split("\\s+");
        updateResultPopupMenu(words);
      }
    });
  }

  public JTextField getTextField()
  {
    return textField;
  }

  private void updateResultPopupMenu(String[] words)
  {
    menu.setVisible(false);
    menu.removeAll();

    int count = 0;
    for (String menuId : this.wollMuxBar.menuOrder)
    {
      boolean added = false;
      ConfigThingy elementeKnoten = new ConfigThingy("");
      try
      {
        elementeKnoten = menuConf.query(menuId).getLastChild().query("Elemente");
      }
      catch (NodeNotFoundException e)
      {}

      ConfigThingy matches = new ConfigThingy("Matches");
      for (ConfigThingy elemente : elementeKnoten)
        for (ConfigThingy button : elemente)
          if (buttonMatches(button, words) && count++ <= MAX_SHOWN)
          {
            if (!added)
            {
              JMenuItem label = new JMenuItem(this.wollMuxBar.mapMenuIDToLabel.get(menuId));
              label.setBorder(BorderFactory.createBevelBorder(1));
              label.setBackground(Color.WHITE);
              label.setEnabled(false);
              label.addMouseListener(wollMuxBar.getMyIsInsideMonitor());
              menu.add(label);
              added = true;
            }
            matches.addChild(button);
          }
      this.wollMuxBar.addUIElements(menuConf, matches, menu, 0, 1, "menu");
    }

    if (count > 0)
    {
      // nur anzeigen, wenn mindestens zwei Treffer nicht angezeigt wurden
      if (count > (MAX_SHOWN + 1))
      {
        menu.addSeparator();
        menu.add(new JLabel(L.m("und %1 nicht angezeigte Treffer", count
          - MAX_SHOWN)));
      }
      menu.show(textField, 0, textField.getHeight());
      ignoreNextFocusRequest = true;
      textField.requestFocusInWindow();
    }
  }

  /**
   * Liefert true gdw. das durch button beschriebene Element ein button ist
   * (TYPE-Attribut muss "button" sein) und alle in words enthaltenen strings ohne
   * Beachtung der Groß-/Kleinschreibung im Wert des LABEL-Attributs (das natürlich
   * vorhanden sein muss) vorkommen.
   * 
   * @param button
   *          Den ConfigThingy-Knoten, der ein UI-Element beschreibt, wie z.B.
   *          "(TYPE 'button' LABEL 'Hallo' ...)"
   * @param words
   *          Diese Wörter müssen ALLE im LABEL vorkommen (ohne Beachtung der
   *          Groß-/Kleinschreibung).
   */
  public static boolean buttonMatches(ConfigThingy button, String[] words)
  {
    if (words == null || words.length == 0) return false;
    try
    {
      String type = button.get("TYPE").toString();
      if (!type.equals("button")) return false;
    }
    catch (NodeNotFoundException e1)
    {
      return false;
    }

    String label;
    try
    {
      label = button.get("LABEL").toString();
    }
    catch (NodeNotFoundException e1)
    {
      return false;
    }

    for (String word : words)
      if (!label.toLowerCase().contains(word.toLowerCase())) return false;
    return true;
  }
}