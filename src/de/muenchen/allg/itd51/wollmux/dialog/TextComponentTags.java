/*
 * Dateiname: JTextFieldWithTags.java
 * Projekt  : WollMux
 * Funktion : Erweiterte eine JTextComponent um die Fähigkeit, Tags, die als
 *            <tag> angezeigt werden, wie atomare Elemente zu behandeln.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
 * 13.02.2008 | LUT | Erstellung als JTextFieldWithTags
 * 13.02.2008 | LUT | Verallgemeinerung zur Klasse TextComponentTags
 * 13.02.2008 | BNK | public Methoden zusammengruppiert, Ortografie         
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
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;

/**
 * Erweiterte eine JTextComponent um die Fähigkeit, Tags, die als
 * &quot;&lt;tag&gt;&quot; angezeigt werden, wie atomare Elemente zu behandeln.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class TextComponentTags
{
  /**
   * Syntax für {@link #getContent(int)}: CAT(... VALUE "&lt;tagname>" ... VALUE
   * "&lt;tagname"> ...)
   */
  final public static int CAT_VALUE_SYNTAX = 0;

/**
   * Präfix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die
   * Zuordnung beginnt mit einem zero width space (nicht sichtbar, aber zur
   * Unterscheidung des Präfix von den Benutzereingaben) und dem "<"-Zeichen.
   */
  private final static String TAG_PREFIX = "" + Character.toChars(0x200B)[0] + "<";

  /**
   * Suffix, mit dem Tags in der Anzeige der Zuordnung angezeigt werden. Die
   * Zuordnung beginnt mit einem zero width space (nicht sichtbar, aber zur
   * Unterscheidung des Präfix von den Benutzereingaben) und dem ">"-Zeichen.
   */
  private final static String TAG_SUFFIX = "" + Character.toChars(0x200B)[0] + ">";

  /**
   * Beschreibt einen regulären Ausdruck, mit dem nach Tags im Text gesucht werden
   * kann. Ein Match liefert in Gruppe 1 den Text des Tags.
   */
  private final static Pattern TAG_PATTERN =
    Pattern.compile("(" + TAG_PREFIX + "(.*?)" + TAG_SUFFIX + ")");

  /**
   * Farbe, mit dem der Hintergund eines Textfeldes im Dialog "Felder anpassen"
   * eingefärbt wird, wenn ein ungültiger Inhalt enthalten ist.
   */
  private static final Color invalidEntryBGColor = Color.PINK;

  /**
   * Die JTextComponent, die durch diese Wrapperklasse erweitert wird.
   */
  private JTextComponent compo;

  /**
   * Erzeugt den Wrapper und nimmt die notwendigen Änderungen am Standardverhalten
   * der JTextComponent component vor.
   */
  public TextComponentTags(JTextComponent component)
  {
    this.compo = component;
    changeInputMap();
    changeCaretHandling();
    changeFocusLostHandling();
    changeDocumentUpdateHandling();
  }

  /**
   * Fügt an der aktuellen Cursorposition ein neues Tag tag ein, das anschließend mit
   * der Darstellung &quot;&lt;tag&gt;&quot; angezeigt wird und bezüglich der
   * Editierung wie ein atomares Element behandelt wird.
   * 
   * @param tag
   *          Der Name des tags, das in dieser JTextComponent an der Cursorposition
   *          eingefügt und angezeigt werden soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public void insertTag(String tag)
  {
    String t = compo.getText();
    int inspos = compo.getCaretPosition();
    String p1 = (inspos > 0) ? t.substring(0, inspos) : "";
    String p2 = (inspos < t.length()) ? t.substring(inspos, t.length()) : "";
    // ACHTUNG! Änderungen hier müssen auch in setContent() gemacht werden
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

  /**
   * Liefert eine Liste von {@link ContentElement}-Objekten, die den aktuellen Inhalt
   * der JTextComponent repräsentiert und dabei enthaltenen Text und evtl. enthaltene
   * Tags als eigene Objekte kapselt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public List<ContentElement> getContent()
  {
    List<ContentElement> list = new ArrayList<ContentElement>();
    String t = compo.getText();
    Matcher m = TAG_PATTERN.matcher(t);
    int lastEndPos = 0;
    int startPos = 0;
    while (m.find())
    {
      startPos = m.start();
      String tag = m.group(2);
      list.add(new ContentElement(t.substring(lastEndPos, startPos), false));
      if (tag.length() > 0) list.add(new ContentElement(tag, true));
      lastEndPos = m.end();
    }
    String text = t.substring(lastEndPos);
    if (text.length() > 0) list.add(new ContentElement(text, false));
    return list;
  }

  /**
   * Liefert den Inhalt der Textkomponente in der durch syntaxType spezifizierten
   * Syntax.
   * 
   * @see #CAT_VALUE_SYNTAX
   * @throws IllegalArgumentException
   *           falls der syntaxType nicht existiert.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10) TESTED
   */
  public ConfigThingy getContent(int syntaxType)
  {
    if (syntaxType != CAT_VALUE_SYNTAX)
      throw new IllegalArgumentException(L.m("Unbekannter syntaxType: %1", ""
        + syntaxType));

    ConfigThingy conf = new ConfigThingy("CAT");
    List<ContentElement> content = getContent();
    if (content.isEmpty())
    {
      conf.add("");
    }
    else
    {
      Iterator<ContentElement> iter = content.iterator();
      while (iter.hasNext())
      {
        ContentElement ele = iter.next();
        if (ele.isTag())
          conf.add("VALUE").add(ele.toString());
        else
          conf.add(ele.toString());
      }
    }

    return conf;
  }

  /**
   * Liefert den Inhalt der TextComponentTag als String mit aufgelösten Tags, wobei
   * an Stelle jedes Tags der entsprechende Inhalt eingesetzt wird, der in
   * mapTagToValue unter dem Schlüssel des Tagnamens gefunden wird oder der String
   * "<tagname>", wenn das Tag nicht aufgelöst werden kann.
   * 
   * @param mapTagToValue
   *          Map mit Schlüssel-/Wertpaaren, die die entsprechenden Werte für die
   *          Tags enthält.
   * @return Inhalt der TextComponentTag als String mit aufgelösten Tags (soweit
   *         möglich).
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public String getContent(Map<String, String> mapTagToValue)
  {
    StringBuffer buf = new StringBuffer();
    for (ContentElement el : getContent())
    {
      if (el.isTag())
      {
        String key = el.toString();
        String value = mapTagToValue.get(key);
        if (value != null)
          buf.append(mapTagToValue.get(key));
        else
          buf.append("<" + key + ">");
      }
      else
        buf.append(el.toString());
    }
    return buf.toString();
  }

  /**
   * Das Gegenstück zu {@link #getContent(int)}.
   * 
   * @throws IllegalArgumentException
   *           wenn syntaxType illegal oder ein Fehler in conf ist.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10)
   */
  public void setContent(int syntaxType, ConfigThingy conf)
  {
    if (syntaxType != CAT_VALUE_SYNTAX)
      throw new IllegalArgumentException(L.m("Unbekannter syntaxType: %1", ""
        + syntaxType));

    if (!conf.getName().equals("CAT"))
      throw new IllegalArgumentException(L.m("Oberster Knoten muss \"CAT\" sein"));

    StringBuilder buffy = new StringBuilder();
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy subConf = iter.next();
      if (subConf.getName().equals("VALUE") && subConf.count() == 1)
      {
        // ACHTUNG! Änderungen hier müssen auch in insertTag() gemacht werden
        buffy.append(TAG_PREFIX);
        buffy.append(subConf.toString());
        buffy.append(TAG_SUFFIX);
      }
      else
      {
        buffy.append(subConf.toString());
      }
    }

    extraHighlightOff();
    compo.setText(buffy.toString());
  }

  /**
   * Liefert zur String-Liste fieldNames eine Liste von Actions, die die
   * entsprechenden Strings in text einfügen.
   * 
   * @author Matthias Benkmann (D-III-ITD D.10) TESTED
   */
  public static List<Action> makeInsertFieldActions(List<String> fieldNames,
      final TextComponentTags text)
  {
    List<Action> actions = new Vector<Action>();
    Iterator<String> iter = fieldNames.iterator();
    while (iter.hasNext())
    {
      final String name = iter.next();
      Action action = new AbstractAction(name)
      {
        private static final long serialVersionUID = -9123184290299840565L;

        public void actionPerformed(ActionEvent e)
        {
          text.insertTag(name);
        }
      };
      actions.add(action);
    }
    return actions;
  }

  /**
   * Kann überschrieben werden um eine Logik zu hinterlegen, die berechnet, ob das
   * Feld einen gültigen Inhalt besitzt. Ist der Inhalt nicht gültig, dann wird das
   * Feld mit einem roten Hintergrund hinterlegt.
   * 
   * @return true, wenn der Inhalt gültig ist und false, wenn der Inhalt nicht gültig
   *         ist.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean isContentValid()
  {
    return true;
  }

  /**
   * Beschreibt ein Element des Inhalts dieser JTextComponent und kann entweder ein
   * eingefügtes Tag oder ein normaler String sein. Auskunft über den Typ des
   * Elements erteilt die Methode isTag(), auf den String-Wert kann über die
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
   * Immer wenn der Cursor mit der Maus in einen Bereich innerhalb eines Tags gesetzt
   * wird, sorgt der hier registrierte caret Listener dafür, dass der Bereich auf das
   * gesamte Tag ausgedehnt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void changeCaretHandling()
  {
    compo.addCaretListener(new CaretListener()
    {
      public void caretUpdate(CaretEvent e)
      {
        extraHighlightOff();
        int dot = compo.getCaret().getDot();
        int mark = compo.getCaret().getMark();

        for (Iterator<TagPos> iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = iter.next();
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
  private void changeFocusLostHandling()
  {
    compo.addFocusListener(new FocusListener()
    {
      public void focusLost(FocusEvent e)
      {
        extraHighlightOff();
      }

      public void focusGained(FocusEvent e)
      {}
    });
  }

  /**
   * Implementiert die Aktionen für die Tastendrücke Cursor-links, Cursor-rechts,
   * Delete und Backspace neu und berücksichtigt dabei die atomaren Tags.
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
        for (Iterator<TagPos> iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = iter.next();
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
        for (Iterator<TagPos> iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = iter.next();
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
        for (Iterator<TagPos> iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = iter.next();
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
        for (Iterator<TagPos> iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = iter.next();
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
        for (Iterator<TagPos> iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = iter.next();
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
        for (Iterator<TagPos> iter = getTagPosIterator(); iter.hasNext();)
        {
          TextComponentTags.TagPos fp = iter.next();
          if (dot == fp.end) pos2 = fp.start;
        }

        deleteAPartOfTheText(dot, pos2);
      }
    });

  }

  /**
   * Der hier registrierte DocumentListener sorgt dafür, dass nach jeder Textänderung
   * geprüft wird, ob der Inhalt der JTextComponent noch gültig ist und im Fehlerfall
   * mit der Hintergrundfarbe invalidEntryBGColor eingefärbt wird.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void changeDocumentUpdateHandling()
  {
    compo.getDocument().addDocumentListener(new DocumentListener()
    {
      private Color oldColor = null;

      public void changedUpdate(DocumentEvent e)
      {
        update();
      }

      public void removeUpdate(DocumentEvent e)
      {
        update();
      }

      public void insertUpdate(DocumentEvent e)
      {
        update();
      }

      private void update()
      {
        if (isContentValid())
        {
          if (oldColor != null) compo.setBackground(oldColor);
        }
        else
        {
          if (oldColor == null) oldColor = compo.getBackground();
          compo.setBackground(invalidEntryBGColor);
        }
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
    if (pos >= 0 && pos <= compo.getText().length()) compo.getCaret().setDot(pos);
  }

  /**
   * Macht das selbe wie getCaret().setDot(pos), aber nur wenn pos >= 0 und pos <=
   * getText().length() gilt.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private void caretMoveDot(int pos)
  {
    if (pos >= 0 && pos <= compo.getText().length()) compo.getCaret().moveDot(pos);
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
   * Liefert einen Iterator von TagPos-Elementen, die beschreiben an welcher Position
   * im aktuellen Text Tags gefunden werden.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private Iterator<TagPos> getTagPosIterator()
  {
    List<TagPos> results = new ArrayList<TagPos>();
    Matcher m = TAG_PATTERN.matcher(compo.getText());
    while (m.find())
    {
      results.add(new TagPos(m.start(1), m.end(1), m.group(2)));
    }
    return results.iterator();
  }

  /**
   * Enthält das tag das beim Erzeugen des Extra-Highlights zurückgeliefert wurde und
   * das Highlight-Objekt auszeichnet.
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
        Highlighter.HighlightPainter hp =
          new DefaultHighlighter.DefaultHighlightPainter(new Color(0xddddff));
        extraHighlightTag = hl.addHighlight(pos1, pos2, hp);
      }
      else
        hl.changeHighlight(extraHighlightTag, pos1, pos2);
    }
    catch (BadLocationException e1)
    {}
  }

}
