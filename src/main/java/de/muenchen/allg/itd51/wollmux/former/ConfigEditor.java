package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.StringReader;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.fife.rsta.ui.CollapsibleSectionPanel;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.FindToolBar;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.search.ReplaceToolBar;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;

public class ConfigEditor extends JFrame implements SearchListener
{
  private FormularMax4kController controller;

  private CollapsibleSectionPanel sectionPanel;

  private TextEditorPane editor;

  private FindDialog findDialog;

  private ReplaceDialog replaceDialog;

  private FindToolBar findToolBar;

  private ReplaceToolBar replaceToolBar;

  public ConfigEditor(String title, FormularMax4kController controller)
      throws HeadlessException
  {
    super(title);

    this.controller = controller;

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter()
    {

      @Override
      public void windowClosing(WindowEvent e)
      {
        if (editor.isDirty())
        {
          if (JOptionPane.showConfirmDialog(ConfigEditor.this,
            L.m("Wollen Sie den Editor schließen ohne zu speichern?"),
            L.m("Editor schließen"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
          {
            ConfigEditor.this.dispose();
          }
        } 
        else
        {
          ConfigEditor.this.dispose();
        }
      }
    });

    createGui();
    setPreferredSize(new Dimension(1024, 768));
    pack();
    setLocationRelativeTo(null);
  }

  public void setText(String text)
  {
    editor.setText(text);
    editor.discardAllEdits();
    editor.setCaretPosition(0);
    editor.setDirty(false);
  }

  private void createGui()
  {
    setLayout(new BorderLayout());

    sectionPanel = new CollapsibleSectionPanel();

    editor = new TextEditorPane();
    editor.setBracketMatchingEnabled(true);

    Font font = new Font("Monospaced", Font.PLAIN, editor.getFont().getSize() + 2);
    editor.setFont(font);

    SyntaxScheme syntax = editor.getSyntaxScheme();
    syntax.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = Color.BLUE;
    syntax.getStyle(Token.LITERAL_CHAR).foreground = Color.BLUE;
    syntax.getStyle(Token.SEPARATOR).foreground = Color.DARK_GRAY;
    syntax.getStyle(Token.RESERVED_WORD).font = font;

    RTextScrollPane scrollPane = new RTextScrollPane(editor);

    sectionPanel.add(scrollPane);

    add(sectionPanel, BorderLayout.CENTER);

    CompletionProvider provider = createCompletionProvider();
    AutoCompletion ac = new AutoCompletion(provider);
    ac.install(editor);

    AbstractTokenMakerFactory tokenMakerFactory =
      (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
    tokenMakerFactory.putMapping("text/conf",
      "de.muenchen.allg.itd51.wollmux.former.ConfigTokenMaker");
    editor.setSyntaxEditingStyle("text/conf");

    setJMenuBar(createMenu());

    initDialogs();
    editor.setCaretPosition(0);
  }

  private void initDialogs()
  {
    findDialog = new FindDialog(this, this);
    replaceDialog = new ReplaceDialog(this, this);

    SearchContext context = findDialog.getSearchContext();
    replaceDialog.setSearchContext(context);

    findToolBar = new FindToolBar(this);
    findToolBar.setSearchContext(context);
    replaceToolBar = new ReplaceToolBar(this);
    replaceToolBar.setSearchContext(context);

    KeyStroke keyStroke =
      KeyStroke.getKeyStroke(KeyEvent.VK_F, getToolkit().getMenuShortcutKeyMask()
        | InputEvent.SHIFT_MASK);
    editor.getInputMap().put(keyStroke, "SHOW_FINDTOOLBAR");
    editor.getActionMap().put("SHOW_FINDTOOLBAR",
      sectionPanel.addBottomComponent(keyStroke, findToolBar));
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

    menu = new JMenu(L.m("Bearbeiten"));
    menu.add(new JMenuItem(RTextArea.getAction(RTextArea.UNDO_ACTION)));
    menu.add(new JMenuItem(RTextArea.getAction(RTextArea.REDO_ACTION)));
    menu.addSeparator();
    menu.add(new JMenuItem(RTextArea.getAction(RTextArea.CUT_ACTION)));
    menu.add(new JMenuItem(RTextArea.getAction(RTextArea.COPY_ACTION)));
    menu.add(new JMenuItem(RTextArea.getAction(RTextArea.PASTE_ACTION)));
    menu.add(new JMenuItem(RTextArea.getAction(RTextArea.DELETE_ACTION)));
    menu.addSeparator();
    menu.add(new JMenuItem(new ShowFindDialogAction()));
    menu.add(new JMenuItem(new ShowReplaceDialogAction()));
    menu.addSeparator();
    menu.add(new JMenuItem(RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)));
    editorMenuBar.add(menu);

    return editorMenuBar;
  }

  private CompletionProvider createCompletionProvider()
  {
    DefaultCompletionProvider provider = new DefaultCompletionProvider();

    provider.addCompletion(new BasicCompletion(provider, "WM"));
    provider.addCompletion(new BasicCompletion(provider, "Eingabefelder"));
    provider.addCompletion(new BasicCompletion(provider, "Fenster"));
    provider.addCompletion(new BasicCompletion(provider, "Formular"));
    provider.addCompletion(new BasicCompletion(provider, "Tab"));
    provider.addCompletion(new BasicCompletion(provider, "TITLE"));
    provider.addCompletion(new BasicCompletion(provider, "CLOSEACTION"));
    provider.addCompletion(new BasicCompletion(provider, "TIP"));
    provider.addCompletion(new BasicCompletion(provider, "LABEL"));
    provider.addCompletion(new BasicCompletion(provider, "TYPE"));
    provider.addCompletion(new BasicCompletion(provider, "READONLY"));
    provider.addCompletion(new BasicCompletion(provider, "AUTOFILL"));
    provider.addCompletion(new BasicCompletion(provider, "DIALOG"));
    provider.addCompletion(new BasicCompletion(provider, "ID"));
    provider.addCompletion(new BasicCompletion(provider, "HOTKEY"));
    provider.addCompletion(new BasicCompletion(provider, "EDIT"));
    provider.addCompletion(new BasicCompletion(provider, "FRAG_ID"));
    
    return provider;
  }

  @Override
  public String getSelectedText()
  {
    return editor.getSelectedText();
  }

  @Override
  public void searchEvent(SearchEvent event)
  {
    SearchResult result;

    switch (event.getType())
    {
      case MARK_ALL:
        result = SearchEngine.markAll(editor, event.getSearchContext());
        break;
      case FIND:
        result = SearchEngine.find(editor, event.getSearchContext());
        if (!result.wasFound())
        {
          UIManager.getLookAndFeel().provideErrorFeedback(editor);
        }
        break;
      case REPLACE:
        result = SearchEngine.replace(editor, event.getSearchContext());
        if (!result.wasFound())
        {
          UIManager.getLookAndFeel().provideErrorFeedback(editor);
        }
        break;
      case REPLACE_ALL:
        result = SearchEngine.replaceAll(editor, event.getSearchContext());
        JOptionPane.showMessageDialog(null,
          String.format(L.m("%d Ersetzung vorgenommen."), result.getCount()));
        break;
    }

  }

  private class ShowFindDialogAction extends AbstractAction
  {
    public ShowFindDialogAction()
    {
      super(L.m("Suchen..."));
      int c = getToolkit().getMenuShortcutKeyMask();
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, c));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (replaceDialog.isVisible())
      {
        replaceDialog.setVisible(false);
      }
      findDialog.setVisible(true);
    }
  }

  private class ShowReplaceDialogAction extends AbstractAction
  {
    public ShowReplaceDialogAction()
    {
      super(L.m("Ersetzen..."));
      int c = getToolkit().getMenuShortcutKeyMask();
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_H, c));
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (findDialog.isVisible())
      {
        findDialog.setVisible(false);
      }
      replaceDialog.setVisible(true);
    }
  }

}
