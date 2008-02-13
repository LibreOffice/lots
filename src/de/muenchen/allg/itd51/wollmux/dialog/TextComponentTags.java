/*
 * Dateiname: JTextFieldWithTags.java
 * Projekt  : WollMux
 * Funktion : Erweiterte eine JTextComponent um die Fähigkeit, Tags, die als
 *            <tag> angezeigt werden, wie atomare Elemente zu behandeln.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 13.02.2008 | LUT | Erstellung als JTextFieldWithTags
 * 13.02.2008 | LUT | Verallgemeinerung zur Klasse TextComponentTags
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

/**
 * Erweiterte eine JTextComponent um die Fähigkeit, Tags, die als
 * &quot;&lt;tag&gt;&quot; angezeigt werden, wie atomare Elemente zu behandeln.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class TextComponentTags
{
  /**
   * Prefix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die
   * Zuordnung beginnt mit einem zero width space (nicht sichtbar, aber zur
   * Unterscheidung des Prefix von den Benutzereingaben) und dem "<"-Zeichen.
   */
  private final static String TAG_PREFIX = ""
                                           + Character.toChars(0x200B)[0]
                                           + "<";

  /**
   * Suffix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die
   * Zuordnung beginnt mit einem zero width space (nicht sichtbar, aber zur
   * Unterscheidung des Prefix von den Benutzereingaben) und dem ">"-Zeichen.
   */
  private final static String TAG_SUFFIX = ""
                                           + Character.toChars(0x200B)[0]
                                           + ">";

  /**
   * Beschreibt einen regulären Ausdruck, mit dem nach Tags im Text gesucht
   * werden kann. Ein Match liefert in Gruppe 1 den Text vor dem Tag, in Gruppe
   * 2 das Tag mit Prefix und Suffix und in Gruppe 3 den Tag-Namen zurück.
   */
  private final static Pattern TAG_PATTERN = Pattern.compile("([^("
                                                             + TAG_PREFIX
                                                             + ")]*)("
                                                             + TAG_PREFIX
                                                             + "([^("
                                                             + TAG_SUFFIX
                                                             + ")]*)"
                                                             + TAG_SUFFIX
                                                             + ")");

  /**
   * Die JTextComponent, die durch diese Wrapperklasse erweitert wird.
   */
  private JTextComponent compo;

  /**
   * Erzeugt den Wrapper und nimmt die notwendigen Änderungen am
   * Standardverhalten der JTextComponent component vor.
   */
  public TextComponentTags(JTextComponent component)
  {
    this.compo = component;
    changeInputMap();
    changeCaretListener();
    changeFocusLostListener();
  }

  /**
   * Liefert eine Liste von {@link ContentElement}-Objekten, die den aktuellen
   * Inhalt der JTextComponent repräsentiert und dabei enthaltenen Text und
   * evtl. enthaltene Tags als eigene Objekte kapselt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public List getContent()
  {
    List list = new ArrayList();
    String t = compo.getText();
    Matcher m = TAG_PATTERN.matcher(t);
    int lastEndPos = 0;
    while (m.find())
    {
      lastEndPos = m.end();
      String text = m.group(1);
      String tag = m.group(3);
      if (text.length() > 0) list.add(new ContentElement(text, false));
      if (tag.length() > 0) list.add(new ContentElement(tag, true));
    }
    String text = t.substring(lastEndPos);
    if (text.length() > 0) list.add(new ContentElement(text, false));
    return list;
  }

  /**
   * Beschreibt ein Element des Inhalts dieser JTextComponent und kann entweder
   * ein eingefügtes Tag oder ein normaler String sein. Auskunft über den Typ
   * des Elements erteilt die Methode isTag(), auf den String-Wert kann über die
   * toString()-Methode zugegriffen werden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static class ContentElement
  {
    private String value;

    private boolean isTag;

    private ContentElement(String value, boolean isTag)
    {
      this.value = value;
      this.isTag = isTag;
    }

    public String toString()
    {
      return value;
    }

    /**
     * Liefert true, wenn dieses Element ein Tag ist oder false, wenn es sich um
     * normalen Text handelt.
     * 
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    public boolean isTag()
    {
      return isTag;
    }
  }

  /**
   * Immer wenn der Cursor mit der Maus in einen Bereich innerhalb eines Tags
   * gesetzt wird, sorgt der hier registrierte caret Listener dafür, dass der
   * Bereich auf das gesamte Tag ausgedehnt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void changeCaretListener()
  {
    compo.addCaretListener(new CaretListener()
    {
      public void caretUpdate(CaretEvent e)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();
        int mark = compo.getCaret().getMark();

        for (Iterator iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = (TextComponentTags.TagPos) iter.next();
          if (dot > fp.start && dot < fp.end)
          {
            if (dot < mark)
            {
              caretMoveDot(fp.start);
            }
            else if (dot > mark)
            {
              caretMoveDot(fp.end);
            }
            else
            {
              caretSetDot(fp.end);
              extraHighlight(fp.start, fp.end);
            }
          }
        }
      }
    });
  }

  /**
   * Der FocusLostListener wird hier registriert, damit nach einem FocusLost ein
   * evtl. gesetztes Extra-Highlight aufgehoben werden kann.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void changeFocusLostListener()
  {
    compo.addFocusListener(new FocusListener()
    {
      public void focusLost(FocusEvent e)
      {
        extraHighlightOff();
      }

      public void focusGained(FocusEvent e)
      {
      }
    });
  }

  /**
   * Implementiert die Aktionen für die Tastendrücke Cursor-links,
   * Cursor-rechts, Delete und Backspace neu und berücksichtigt dabei die
   * atomaren Tags.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void changeInputMap()
  {
    InputMap m = compo.getInputMap();
    m.put(KeyStroke.getKeyStroke("LEFT"), "goLeft");
    m.put(KeyStroke.getKeyStroke("RIGHT"), "goRight");
    m.put(KeyStroke.getKeyStroke("shift LEFT"), "expandLeft");
    m.put(KeyStroke.getKeyStroke("shift RIGHT"), "expandRight");
    m.put(KeyStroke.getKeyStroke("DELETE"), "deleteRight");
    m.put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteLeft");

    compo.getActionMap().put("goLeft", new AbstractAction("goLeft")
    {
      private static final long serialVersionUID = 2098288193497911628L;

      public void actionPerformed(ActionEvent evt)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();

        // evtl. vorhandenes Tag überspringen
        for (Iterator iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = (TextComponentTags.TagPos) iter.next();
          if (dot == fp.end)
          {
            caretSetDot(fp.start);
            extraHighlight(fp.start, fp.end);
            return;
          }
        }

        caretSetDot(dot - 1);
      }
    });

    compo.getActionMap().put("goRight", new AbstractAction("goRight")
    {
      private static final long serialVersionUID = 2098288193497911628L;

      public void actionPerformed(ActionEvent evt)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();

        // evtl. vorhandenes Tag überspringen
        for (Iterator iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = (TextComponentTags.TagPos) iter.next();
          if (dot == fp.start)
          {
            caretSetDot(fp.end);
            extraHighlight(fp.start, fp.end);
            return;
          }
        }

        caretSetDot(dot + 1);
      }
    });

    compo.getActionMap().put("expandLeft", new AbstractAction("expandLeft")
    {
      private static final long serialVersionUID = 2098288193497911628L;

      public void actionPerformed(ActionEvent evt)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();

        // evtl. vorhandenes Tag überspringen
        for (Iterator iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = (TextComponentTags.TagPos) iter.next();
          if (dot == fp.end)
          {
            caretMoveDot(fp.start);
            return;
          }
        }

        caretMoveDot(dot - 1);
      }
    });

    compo.getActionMap().put("expandRight", new AbstractAction("expandRight")
    {
      private static final long serialVersionUID = 2098288193497911628L;

      public void actionPerformed(ActionEvent evt)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();

        // evtl. vorhandenes Tag überspringen
        for (Iterator iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = (TextComponentTags.TagPos) iter.next();
          if (dot == fp.start)
          {
            caretMoveDot(fp.end);
            return;
          }
        }

        caretMoveDot(dot + 1);
      }
    });

    compo.getActionMap().put("deleteRight", new AbstractAction("deleteRight")
    {
      private static final long serialVersionUID = 2098288193497911628L;

      public void actionPerformed(ActionEvent evt)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();
        int mark = compo.getCaret().getMark();

        // evtl. vorhandene Selektion löschen
        if (dot != mark)
        {
          deleteAPartOfTheText(dot, mark);
          return;
        }

        // Endposition des zu löschenden Bereichs bestimmen
        int pos2 = dot + 1;
        for (Iterator iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = (TextComponentTags.TagPos) iter.next();
          if (dot == fp.start) pos2 = fp.end;
        }

        deleteAPartOfTheText(dot, pos2);
      }

    });

    compo.getActionMap().put("deleteLeft", new AbstractAction("deleteLeft")
    {
      private static final long serialVersionUID = 2098288193497911628L;

      public void actionPerformed(ActionEvent evt)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();
        int mark = compo.getCaret().getMark();

        // evtl. vorhandene Selektion löschen
        if (dot != mark)
        {
          deleteAPartOfTheText(dot, mark);
          return;
        }

        // Anfangsposition des zu löschenden Bereichs bestimmen
        int pos2 = dot - 1;
        for (Iterator iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = (TextComponentTags.TagPos) iter.next();
          if (dot == fp.end) pos2 = fp.start;
        }

        deleteAPartOfTheText(dot, pos2);
      }
    });

  }

  /**
   * Löscht einen Teil des aktuellen Texts zwischen den zwei Positionen pos1 und
   * pos2. pos1 kann größer, kleiner oder gleich pos2 sein.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void deleteAPartOfTheText(int pos1, int pos2)
  {
    // sicherstellen dass pos1 <= pos2
    if (pos1 > pos2)
    {
      int tmp = pos2;
      pos2 = pos1;
      pos1 = tmp;
    }
    String t = compo.getText();
    String part1 = (pos1 > 0) ? t.substring(0, pos1) : "";
    String part2 = (pos2 < t.length()) ? t.substring(pos2) : "";
    compo.setText(part1 + part2);
    compo.getCaret().setDot(pos1);
  }

  /**
   * Macht das selbe wie getCaret().setDot(pos), aber nur wenn pos >= 0 und pos <=
   * getText().length() gilt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void caretSetDot(int pos)
  {
    if (pos >= 0 && pos <= compo.getText().length())
      compo.getCaret().setDot(pos);
  }

  /**
   * Macht das selbe wie getCaret().setDot(pos), aber nur wenn pos >= 0 und pos <=
   * getText().length() gilt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void caretMoveDot(int pos)
  {
    if (pos >= 0 && pos <= compo.getText().length())
      compo.getCaret().moveDot(pos);
  }

  /**
   * Diese Klasse beschreibt die Position eines Tags innerhalb des Textes der
   * JTextComponent.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  static class TagPos
  {
    public final String name;

    public final int start;

    public final int end;

    TagPos(int start, int end, String name)
    {
      this.start = start;
      this.end = end;
      this.name = name;
    }
  }

  /**
   * Liefert einen Iterator von TagPos-Elementen, die beschreiben an welcher
   * Position im aktuellen Text Tags gefunden werden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private Iterator getTagPosIterator()
  {
    List results = new ArrayList();
    Matcher m = TAG_PATTERN.matcher(compo.getText());
    while (m.find())
    {
      results.add(new TagPos(m.start(2), m.end(2), m.group(3)));
    }
    return results.iterator();
  }

  /**
   * Enthält das tag das beim Erzeugen des Extra-Highlights zurückgeliefert
   * wurde und das Highlight-Objekt auszeichnet.
   */
  private Object extraHighlightTag = null;

  /**
   * Deaktiviert die Anzeige des Extra-Highlights
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void extraHighlightOff()
  {
    if (extraHighlightTag != null)
    {
      Highlighter hl = compo.getHighlighter();
      hl.removeHighlight(extraHighlightTag);
      extraHighlightTag = null;
    }
  }

  /**
   * Highlightet den Textbereich zwischen pos1 und pos2
   * 
   * @param pos1
   * @param pos2
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void extraHighlight(int pos1, int pos2)
  {
    Highlighter hl = compo.getHighlighter();
    try
    {
      if (extraHighlightTag == null)
      {
        Highlighter.HighlightPainter hp = new DefaultHighlighter.DefaultHighlightPainter(
            new Color(0xddddff));
        extraHighlightTag = hl.addHighlight(pos1, pos2, hp);
      }
      else
        hl.changeHighlight(extraHighlightTag, pos1, pos2);
    }
    catch (BadLocationException e1)
    {
    }
  }

  /**
   * Fügt an der aktuellen Cursorposition ein neues Tag tag ein, das anschließen
   * mit der Darstellung &quot;&lt;tag&gt;&quot; angezeigt wird und bezüglich
   * der Editierung wie ein atomares Element behandelt wird.
   * 
   * @param tag
   *          Der Name des tags, das in dieser JTextComponent an der
   *          Cursorposition eingefügt und angezeigt werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void insertTag(String tag)
  {
    String t = compo.getText();
    int inspos = compo.getCaretPosition();
    String p1 = (inspos > 0) ? t.substring(0, inspos) : "";
    String p2 = (inspos < t.length()) ? t.substring(inspos, t.length()) : "";
    t = TAG_PREFIX + tag + TAG_SUFFIX;
    compo.setText(p1 + t + p2);
    compo.getCaret().setDot(inspos + t.length());
    extraHighlight(inspos, inspos + t.length());
  }

  /**
   * Liefert die JTextComponent, die durch diesen Wrapper erweitert wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public JTextComponent getJTextComponent()
  {
    return compo;
  }
}
