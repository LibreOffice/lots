/*
* Dateiname: DocumentTree.java
* Projekt  : WollMux
* Funktion : Stellt die interessanten Teile eines Textdokuments als Baum zur Verfügung.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 04.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
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
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextTable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Bookmark;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Stellt die interessanten Teile eines Textdokuments als Baum zur Verfügung.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DocumentTree
{
  private static final Pattern INSERTION_BOOKMARK = Pattern.compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'((insertValue)|(insertFormValue))'.*\\))\\s*\\d*\\z");
  private static final Pattern GROUP_BOOKMARK = Pattern.compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'setGroups'.*\\))\\s*\\d*\\z");
  public static final int CHECKBOX_CONTROL = 0;
  public static final int DROPDOWN_CONTROL = 1;
  public static final int INPUT_CONTROL = 2;
  
  private Node root;  
  
  public DocumentTree(XTextDocument doc)
  {
    Vector topLevelNodes = new Vector();
    
    /*
     * Zuerst enumerieren wir den Inhalt des Body Texts
     */
    XEnumerationAccess enuAccess = UNO.XEnumerationAccess(doc.getText());
    if (enuAccess == null) return;
    Vector nodes = new Vector();
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
      nodes = new Vector(names.length);
      for (int i = 0; i < names.length; ++i)
      {
        Object frame;
        try{ frame = access.getByName(names[i]); } catch(Exception x){ continue;}
        
        enu = UNO.XEnumerationAccess(frame).createEnumeration();
        Vector childNodes = new Vector();
        handleParagraphEnumeration(enu, childNodes, doc);
        
        nodes.add(new ContainerNode(childNodes));
      }
      
      topLevelNodes.add(new ContainerNode(nodes));
    }
    
    root = new ContainerNode(topLevelNodes);
  }

  private void handleParagraphEnumeration(XEnumeration enu, Vector nodes, XTextDocument doc)
  {
    XEnumerationAccess enuAccess;
    while (enu.hasMoreElements())
    {
      Object ele;
      try{ ele = enu.nextElement(); } catch(Exception x){continue;}
      enuAccess = UNO.XEnumerationAccess(ele);
      if (enuAccess != null) //ist wohl ein SwXParagraph
      {
        handleParagraph(enuAccess, nodes, doc);
      }
      else //unterstützt nicht XEnumerationAccess, ist wohl SwXTextTable 
      {
        XTextTable table = UNO.XTextTable(ele);
        if (table != null) handleTextTable(table, nodes, doc);
      }
    }
  }

  private void handleTextTable(XTextTable table, Collection nodes, XTextDocument doc)
  {
    Vector cells = new Vector();
    String[] cellNames = table.getCellNames();
    for (int i = 0; i < cellNames.length; ++i)
    {
      XCell cell = table.getCellByName(cellNames[i]);
      Vector cellContents = new Vector();
      handleParagraphEnumeration(UNO.XEnumerationAccess(cell).createEnumeration(), cellContents, doc);
      cells.add(new ContainerNode(cellContents));
    }
    
    nodes.add(new ContainerNode(cells));
  }

  private void handleParagraph(XEnumerationAccess paragraph, Collection nodes, XTextDocument doc)
  {
    Vector textPortions = new Vector();
    
    /*
     * enumeriere alle TextPortions des Paragraphs
     */
    XEnumeration textPortionEnu = paragraph.createEnumeration();
    while (textPortionEnu.hasMoreElements())
    {
      Object textPortion;
      try{ textPortion = textPortionEnu.nextElement(); } catch(Exception x){continue;};
      
      String textPortionType = (String)UNO.getProperty(textPortion, "TextPortionType");
      if (textPortionType.equals("Bookmark"))
      {
        boolean isStart = false;
        XNamed bookmark = null; 
        try{
          isStart = ((Boolean)UNO.getProperty(textPortion, "IsStart")).booleanValue();
          bookmark = UNO.XNamed(UNO.getProperty(textPortion, "Bookmark"));
        } catch(Exception x){ continue;}
        if (bookmark == null) continue;
        
        String name = bookmark.getName();
        Matcher m = INSERTION_BOOKMARK.matcher(name);
        if (m.matches())
        {
          ConfigThingy conf;
          try{
            conf = new ConfigThingy("", null, new StringReader(m.group(1)));
          } catch(Exception x)
          {
            Logger.error("Fehlerhaftes WM()-Bookmark: \""+name+"\"");
            continue;
          }
          textPortions.add(new InsertionBookmarkNode(new Bookmark(bookmark, doc), isStart, conf));
          continue;
        }
        
        m = GROUP_BOOKMARK.matcher(name);
        if (m.matches())
        {
          ConfigThingy conf;
          try{
            conf = new ConfigThingy("", null, new StringReader(m.group(1)));
          } catch(Exception x)
          {
            Logger.error("Fehlerhaftes WM()-Bookmark: \""+name+"\"");
            continue;
          }
          textPortions.add(new GroupBookmarkNode(new Bookmark(bookmark, doc), isStart, conf));
        }
        
      } else if (textPortionType.equals("TextField"))
      {
        XDependentTextField textField = null;
        boolean isDropdown;
        try{
          textField = UNO.XDependentTextField(UNO.getProperty(textPortion,"TextField"));
          XServiceInfo info = UNO.XServiceInfo(textField); 
          if (info.supportsService("com.sun.star.text.TextField.DropDown"))
            isDropdown = true;
          else if (info.supportsService("com.sun.star.text.TextField.Input"))
            isDropdown = false;
          else 
            continue; //sonstiges TextField 
        } catch(Exception x){ continue;}
        
        if (isDropdown)
          textPortions.add(new DropdownNode(textField, doc));
        else
          textPortions.add(new InputNode(textField, doc));
        
      } else if (textPortionType.equals("Frame"))
      {
        XControlShape shape = null;
        XControlModel model = null;
        try{
          XEnumeration contentEnum = UNO.XContentEnumerationAccess(textPortion).createContentEnumeration("com.sun.star.text.TextPortion");
          while (contentEnum.hasMoreElements())
          {
            XControlShape tempShape = null;
            try{tempShape = UNO.XControlShape(contentEnum.nextElement());}catch(Exception x){}
            XControlModel tempModel = tempShape.getControl();
            XServiceInfo info = UNO.XServiceInfo(tempModel); 
            if (info.supportsService("com.sun.star.form.component.CheckBox"))
            {
              shape = tempShape;
              model = tempModel;
            }
          }
        } catch(Exception x){ continue;}

        textPortions.add(new CheckboxNode(shape, model, doc));
        
      } else if (textPortionType.equals("Text"))
      {
        XTextRange textRange = UNO.XTextRange(textPortion);
        if (textRange != null)
          textPortions.add(new TextRangeNode(textRange));
      }
      else //sonstige TextPortion
        continue;
    }
    
    nodes.add(new ContainerNode(textPortions));
  }
  
  public static interface InsertionBookmark
  {
    public String getName();
    public boolean isStart();
  }
  
  public static interface FormControl
  {
    public int getType();
    public String getDescriptor();
    public void surroundWithBookmark(String bmName);
    /**
     * Liefert den aktuell im Steuerelement eingestellten Wert zurück.
     * Boolesche Steuerelemente (Checkbox) liefern "true" oder "false".
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String getString();
  }
  
  public static interface DropdownFormControl extends FormControl
  {
    public String[] getItems();
  }
  
  public static interface TextRange
  {
    public String getString();
  }
  
  public static abstract class Visitor
  {
    public void visit(DocumentTree tree)
    {
      tree.root.visit(this);
    }
    
    public boolean insertionBookmark(InsertionBookmark bookmark) {return true;}
    public boolean formControl(FormControl control) {return true;}
    public boolean container(int count) {return true;}
    public boolean textRange(TextRange textRange) {return true;}
  }
    
  public static abstract class Node
  {
    public Iterator iterator() { return (new Vector(0)).iterator();};
    protected Node() {}
    public boolean visit(Visitor visit) {return true;}
  }
  
  public static class ContainerNode extends Node
  {
    private Collection children;
    
    public ContainerNode(Collection children)
    {
      super();
      this.children = children;
    }
    
    public Iterator iterator() { return children.iterator();}
    public String toString() { return "CONTAINER";}
    
    public boolean visit(Visitor visit)
    {
      if (!visit.container(0)) return false;
      
      Iterator iter = iterator();
      while (iter.hasNext())
      {
        if (!((Node)iter.next()).visit(visit)) return false;
      }
      if (!visit.container(1)) return false;
      return true;
    }
  }
  
  public static class BookmarkNode extends Node
  {
    private boolean isStart;
    private Bookmark bookmark;
    
    public BookmarkNode(Bookmark bookmark, boolean isStart)
    {
      super();
      this.bookmark = bookmark;
      this.isStart = isStart;
    }
    
    public String getName() {return bookmark.getName();}
    public boolean isStart() {return isStart;}
    
    public String toString() {return "Bookmark '"+bookmark.getName()+ (isStart ? "' Start" : "' End");};
  }
  
  public static class WollmuxBookmarkNode extends BookmarkNode
  {
    protected ConfigThingy conf;
    
    public WollmuxBookmarkNode(Bookmark bookmark, boolean isStart, ConfigThingy conf)
    {
      super(bookmark, isStart);
      this.conf = conf;
    }
  }

  
  
  public static class InsertionBookmarkNode extends WollmuxBookmarkNode 
                                         implements InsertionBookmark
  {
    public InsertionBookmarkNode(Bookmark bookmark, boolean isStart, ConfigThingy conf)
    {
      super(bookmark, isStart, conf);
    }
    
    public boolean visit(Visitor visit)
    {
      return visit.insertionBookmark(this);
    }
  }
  
  public static class GroupBookmarkNode extends WollmuxBookmarkNode
  {
    public GroupBookmarkNode(Bookmark bookmark, boolean isStart, ConfigThingy conf)
    {
      super(bookmark, isStart, conf);
    }
  }
  
  public static class CheckboxNode extends Node implements FormControl
  {
    private XTextDocument doc;
    private XControlShape shape;
    private XControlModel model;
    private static final Short CHECKED_STATE = new Short((short)1);
    
    public CheckboxNode(XControlShape shape, XControlModel model, XTextDocument doc)
    {
      super();
      this.shape = shape;
      this.model = model;
      this.doc = doc;
    }
    
    public boolean isChecked()
    {
      return CHECKED_STATE.equals(UNO.getProperty(model, "State"));
    }
    
    public String toString()
    {
      return "Checkbox ["+(isChecked()? "X]" : " ]");
    }
    
    public boolean visit(Visitor visit)
    {
      return visit.formControl(this);
    }

    public int getType()
    {
      return CHECKBOX_CONTROL;
    }
    
    public String getString()
    {
      return ""+isChecked();
    }

    public String getDescriptor()
    {
      StringBuilder buffy = new StringBuilder();
      try{buffy.append((String)UNO.getProperty(model,"HelpText"));}catch(Exception x){};
      if (buffy.toString().trim().length() < 2)
        try{buffy.append(UNO.XNamed(model).getName());}catch(Exception x){};
      return buffy.toString();
    }

    public void surroundWithBookmark(String bmName)
    {
      XTextRange range = UNO.XTextContent(shape).getAnchor();
      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      cursor.goRight((short)1, true);
      new Bookmark(bmName, doc, cursor);
    }
  }
  
  public static class TextRangeNode extends Node implements TextRange
  {
    protected XTextRange range;
    
    public TextRangeNode(XTextRange range)
    {
      super();
      this.range = range;
    }
    
    public String toString() { return "\""+range.getString()+"\"";}
    
    public boolean visit(Visitor visit)
    {
      return visit.textRange(this);
    }

    public String getString()
    {
      return range.getString();
    }
  }
  
  public static class TextFieldNode extends Node 
  {
    protected XDependentTextField textfield;
    protected XTextDocument doc;
    
    public TextFieldNode(XDependentTextField textField, XTextDocument doc)
    {
      super();
      this.textfield = textField;
      this.doc = doc;
    }
  }
  
  public static class DropdownNode extends TextFieldNode implements DropdownFormControl
  {
    public DropdownNode(XDependentTextField textField, XTextDocument doc)
    {
      super(textField, doc);
    }
    
    public String[] getItems()
    {
      return (String[])UNO.getProperty(textfield, "Items");
    }
    
    public String getSelectedItem()
    {
      try{
        return (String)UNO.getProperty(textfield, "SelectedItem");
      }catch(Exception x)
      {
        return "";
      }
    }
    
    public String getString()
    {
      return getSelectedItem();
    }

    public boolean visit(Visitor visit)
    {
      return visit.formControl(this);
    }
    
    public int getType()
    {
      return DROPDOWN_CONTROL;
    }
    
    public String toString()
    {
      StringBuffer buffy = new StringBuffer("Dropdown ");
      String[] items = getItems();
      String selectedItem = getSelectedItem();
      for (int i = 0; i < items.length; ++i)
      {
        if (i > 0) buffy.append(", ");
        String item = items[i];
        if (item.equals(selectedItem))
        {
          buffy.append("((");
          buffy.append(item);
          buffy.append("))");
        }
        else
        {
          buffy.append("\"");
          buffy.append(item);
          buffy.append("\"");
        }
      }
      
      return buffy.toString();
    }

    public String getDescriptor()
    {
      StringBuilder buffy = new StringBuilder();
      try{buffy.append((String)UNO.getProperty(textfield,"Name"));}catch(Exception x){};
      if (buffy.toString().trim().length() < 2) buffy.append(getSelectedItem());
      return buffy.toString();
    }

    public void surroundWithBookmark(String bmName)
    {
      XTextRange range = UNO.XTextContent(textfield).getAnchor();
      new Bookmark(bmName, doc, range);
    }
  }
  
  public static class InputNode extends TextFieldNode implements FormControl
  {
    public InputNode(XDependentTextField textField, XTextDocument doc)
    {
      super(textField, doc);
    }
    
    public String getContent()
    {
      return (String)UNO.getProperty(textfield, "Content");
    }
    
    public String getString()
    {
      return getContent();      
    }
    
    public String toString()
    {
      return "Input [" + getContent() + "]";
    }
    
    public boolean visit(Visitor visit)
    {
      return visit.formControl(this);
    }
    
    public int getType()
    {
      return INPUT_CONTROL;
    }

    public String getDescriptor()
    {
      StringBuilder buffy = new StringBuilder();
      try{buffy.append((String)UNO.getProperty(textfield,"Hint"));}catch(Exception x){};
      if (buffy.toString().trim().length() < 2)
        try{buffy.append((String)UNO.getProperty(textfield,"Content"));}catch(Exception x){};
      return buffy.toString();
    }
    
    public void surroundWithBookmark(String bmName)
    {
      XTextRange range = UNO.XTextContent(textfield).getAnchor();
      new Bookmark(bmName, doc, range);
    }
  }

  /**
   * Liefert eine textuelle Baumdarstellung des Baums mit Wurzel root. Jeder Zeile wird
   * childPrefix vorangestellt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static String treeDump(Node root, String childPrefix)
  {
    StringBuffer buf = new StringBuffer();
    buf.append(""+root.toString()+"\n");
    Iterator iter = root.iterator();
    while (iter.hasNext())
    {
      Node child = (Node)iter.next();
      buf.append(childPrefix+"|\n"+childPrefix+"+--");
      char ch = iter.hasNext()?'|':' ';
      buf.append(treeDump(child, childPrefix+ch+"  "));
    }
    return buf.toString();
  }
  
  
  /**
   * @param args
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   * @throws Exception 
   */
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
    System.out.println("Dokument geparst in "+time+"ms");
    System.out.println(treeDump(tree.root,""));
    System.exit(0);
  }

}
