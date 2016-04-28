/*
 * Dateiname: DocumentTree.java
 * Projekt  : WollMux
 * Funktion : Stellt die interessanten Teile eines Textdokuments als Baum zur Verfügung.
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
 * 04.08.2006 | BNK | Erstellung
 * 28.08.2006 | BNK | kommentiert
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.document;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.awt.XControlModel;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XControlShape;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.table.XCell;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextTable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.CheckboxNode;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.Container;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.ContainerNode;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.DropdownNode;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.GroupBookmarkNode;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.InputNode;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.InsertionBookmarkNode;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.Node;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.ParagraphNode;
import de.muenchen.allg.itd51.wollmux.core.document.nodes.TextRangeNode;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Stellt die interessanten Teile eines Textdokuments als Baum zur Verfügung.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DocumentTree
{
  /**
   * Pattern zum Erkennen von setGroups-Bookmarks.
   */
  private static final Pattern GROUP_BOOKMARK =
    DocumentCommands.getPatternForCommand("setGroups");

  public static final int TEXFIELD_TYPE_INPUT = 0;
  public static final int TEXFIELD_TYPE_DROPDOWN = 1;

  /**
   * Die Wurzel des Dokumentbaums.
   */
  private Node root;

  /**
   * Erzeugt einen neuen Dokumentbaum für das Dokument doc.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public DocumentTree(XTextDocument doc)
  {
    List<Node> topLevelNodes = new ArrayList<Node>();

    /*
     * Zuerst enumerieren wir den Inhalt des Body Texts
     */
    XEnumerationAccess enuAccess = UNO.XEnumerationAccess(doc.getText());
    if (enuAccess == null) 
    {
      return;
    }
    List<Node> nodes = new ArrayList<Node>();
    XEnumeration enu = enuAccess.createEnumeration();
    handleParagraphEnumeration(enu, nodes, doc);
    topLevelNodes.add(new ContainerNode(nodes));

    /*
     * Jetzt kommen die Frames dran.
     */
    XTextFramesSupplier supp = UNO.XTextFramesSupplier(doc);
    XNameAccess access = supp.getTextFrames();
    String[] names = access.getElementNames();
    if (names.length > 0)
    {
      nodes = new Vector<Node>(names.length);
      for (int i = 0; i < names.length; ++i)
      {
        Object frame;
        try
        {
          frame = access.getByName(names[i]);
        }
        catch (Exception x)
        {
          continue;
        }

        enu = UNO.XEnumerationAccess(frame).createEnumeration();
        List<Node> childNodes = new ArrayList<Node>();
        handleParagraphEnumeration(enu, childNodes, doc);

        nodes.add(new ContainerNode(childNodes));
      }

      topLevelNodes.add(new ContainerNode(nodes));
    }

    root = new ContainerNode(topLevelNodes);
  }
  
  public Node getRoot()
  {
    return root;
  }

  /**
   * Nimmt eine XEnumeration enu von Absätzen und TextTables und fügt für jedes
   * Element von enu zu nodes einen entsprechenden {@link ContainerNode} hinzu.
   * 
   * @param doc
   *          das Dokument in dem die Absätze liegen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void handleParagraphEnumeration(XEnumeration enu, List<Node> nodes,
      XTextDocument doc)
  {
    XEnumerationAccess enuAccess;
    while (enu.hasMoreElements())
    {
      Object ele;
      try
      {
        ele = enu.nextElement();
      }
      catch (Exception x)
      {
        continue;
      }
      enuAccess = UNO.XEnumerationAccess(ele);
      if (enuAccess != null) // ist wohl ein SwXParagraph
      {
        handleParagraph(enuAccess, nodes, doc);
      }
      else
      // unterstützt nicht XEnumerationAccess, ist wohl SwXTextTable
      {
        XTextTable table = UNO.XTextTable(ele);
        if (table != null) 
        {
          handleTextTable(table, nodes, doc);
        }
      }
    }
  }

  /**
   * Fügt nodes einen neuen {@link ContainerNode} hinzu, der die Zellen von table
   * enthält.
   * 
   * @param doc
   *          das Dokument das die Tabelle enthält.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void handleTextTable(XTextTable table, Collection<Node> nodes,
      XTextDocument doc)
  {
    List<Node> cells = new ArrayList<Node>();
    String[] cellNames = table.getCellNames();
    for (int i = 0; i < cellNames.length; ++i)
    {
      XCell cell = table.getCellByName(cellNames[i]);
      List<Node> cellContents = new ArrayList<Node>();
      handleParagraphEnumeration(UNO.XEnumerationAccess(cell).createEnumeration(),
        cellContents, doc);
      cells.add(new ContainerNode(cellContents));
    }

    nodes.add(new ContainerNode(cells));
  }

  /**
   * Fügt nodes einen neuen {@link ParagraphNode} hinzu, der die Inhalte des Absatzes
   * paragraph.
   * 
   * @param doc
   *          das Dokument das den Absatz enthält.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void handleParagraph(XEnumerationAccess paragraph, Collection<Node> nodes,
      XTextDocument doc)
  {
    List<Node> textPortions = new ArrayList<Node>();

    /*
     * enumeriere alle TextPortions des Paragraphs
     */
    XEnumeration textPortionEnu = paragraph.createEnumeration();
    while (textPortionEnu.hasMoreElements())
    {
      Object textPortion;
      try
      {
        textPortion = textPortionEnu.nextElement();
      }
      catch (Exception x)
      {
        continue;
      }

      String textPortionType =
        (String) UNO.getProperty(textPortion, "TextPortionType");
      if (textPortionType.equals("Bookmark"))
      {
        handleBookmark(textPortion, textPortions, doc);
      }
      else if (textPortionType.equals("TextField"))
      {
        handleTextfield(textPortion, textPortions, doc);
      }
      else if (textPortionType.equals("Frame"))
      {
        handleFrame(textPortion, textPortions, doc);
      }
      else if (textPortionType.equals("Text"))
      {
        XTextRange textRange = UNO.XTextRange(textPortion);
        if (textRange != null)
        {
          textPortions.add(new TextRangeNode(textRange));
        }
      }
      else
        // sonstige TextPortion
        continue;
    }

    nodes.add(new ParagraphNode(textPortions));
  }
  
  private void handleBookmark(Object textPortion, List<Node> textPortions, XTextDocument doc)
  {
    boolean isStart = false;
    boolean isCollapsed = false;
    XNamed bookmark = null;
    try
    {
      isStart =
        ((Boolean) UNO.getProperty(textPortion, "IsStart")).booleanValue();
      isCollapsed =
        ((Boolean) UNO.getProperty(textPortion, "IsCollapsed")).booleanValue();
      if (isCollapsed) 
      {
        isStart = true;
      }
      bookmark = UNO.XNamed(UNO.getProperty(textPortion, "Bookmark"));
    }
    catch (Exception x)
    {
      return;
    }
    if (bookmark == null) 
    {
      return;
    }

    String name = bookmark.getName();
    Matcher m = DocumentCommands.INSERTION_BOOKMARK.matcher(name);
    if (m.matches())
    {
      try
      {
        ConfigThingy conf = new ConfigThingy("", null, new StringReader(m.group(1)));
        textPortions.add(new InsertionBookmarkNode(new Bookmark(bookmark, doc),
          isStart, conf));
        if (isCollapsed)
        {
          textPortions.add(new InsertionBookmarkNode(new Bookmark(bookmark, doc),
            false, conf));
        }
      }
      catch (Exception x)
      {
        Logger.error(L.m("Fehlerhaftes WM()-Bookmark: \"%1\"", name), x);
        return;
      }
      return;
    }

    m = GROUP_BOOKMARK.matcher(name);
    if (m.matches())
    {
      try
      {
        ConfigThingy conf = new ConfigThingy("", null, new StringReader(m.group(1)));
        textPortions.add(new GroupBookmarkNode(new Bookmark(bookmark, doc),
          isStart, conf));
        if (isCollapsed)
        {
          textPortions.add(new GroupBookmarkNode(new Bookmark(bookmark, doc),
            false, conf));
        }
      }
      catch (Exception x)
      {
        Logger.error(L.m("Fehlerhaftes WM()-Bookmark: \"%1\"", name), x);
        return;
      }
    }
  }

  private void handleTextfield(Object textPortion, List<Node> textPortions, XTextDocument doc)
  {
    XDependentTextField textField = null;
    int textfieldType = TEXFIELD_TYPE_INPUT;
    try
    {
      textField =
        UNO.XDependentTextField(UNO.getProperty(textPortion, "TextField"));
      XServiceInfo info = UNO.XServiceInfo(textField);
      if (info.supportsService("com.sun.star.text.TextField.DropDown"))
        textfieldType = TEXFIELD_TYPE_DROPDOWN;
      else if (info.supportsService("com.sun.star.text.TextField.Input"))
        textfieldType = TEXFIELD_TYPE_INPUT;
      else
        return; // sonstiges TextField
    }
    catch (Exception x)
    {
      return;
    }

    switch (textfieldType)
    {
      case TEXFIELD_TYPE_INPUT:
        textPortions.add(new InputNode(textField, doc));
        break;
      case TEXFIELD_TYPE_DROPDOWN:
        textPortions.add(new DropdownNode(textField, doc));
        break;
    }
  }

  private void handleFrame(Object textPortion, List<Node> textPortions, XTextDocument doc)
  {
    XControlShape shape = null;
    XControlModel model = null;
    try
    {
      XEnumeration contentEnum =
        UNO.XContentEnumerationAccess(textPortion).createContentEnumeration(
          "com.sun.star.text.TextPortion");
      while (contentEnum.hasMoreElements())
      {
        XControlShape tempShape;
        try
        {
          tempShape = UNO.XControlShape(contentEnum.nextElement());
        }
        catch (com.sun.star.uno.Exception x)
        { // Wegen OOo Bugs kann nextElement() werfen auch wenn hasMoreElements()
          continue;
        }
        if (tempShape != null)
        {
          XControlModel tempModel = tempShape.getControl();
          XServiceInfo info = UNO.XServiceInfo(tempModel);
          if (info.supportsService("com.sun.star.form.component.CheckBox"))
          {
            shape = tempShape;
            model = tempModel;
          }
        }
      }
    }
    catch (Exception x)
    {
      return;
    }

    if (shape != null && model != null)
    {
      textPortions.add(new CheckboxNode(shape, model, doc));
    }
  }
  
  /**
   * Liefert eine textuelle Baumdarstellung des Baums mit Wurzel root. Jeder Zeile
   * wird childPrefix vorangestellt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static String treeDump(Node root, String childPrefix)
  {
    StringBuilder buf = new StringBuilder();
    buf.append("" + root.toString() + "\n");
    Iterator<Node> iter = root.iterator();
    while (iter.hasNext())
    {
      Node child = iter.next();
      buf.append(childPrefix + "|\n" + childPrefix + "+--");
      char ch = iter.hasNext() ? '|' : ' ';
      buf.append(treeDump(child, childPrefix + ch + "  "));
    }
    return buf.toString();
  }

  public static void main(String[] args) throws Exception
  {
    UNO.init();
    XTextDocument doc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());

    /*
     * Parsen dauert ca. 1/10 s pro Seite
     */
    long start = System.currentTimeMillis();
    DocumentTree tree = new DocumentTree(doc);
    long time = System.currentTimeMillis() - start;
    System.out.println(L.m("Dokument geparst in %1ms", time));
    System.out.println(treeDump(tree.root, ""));
    System.exit(0);
  }

}
