package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

public class InputNode extends TextFieldNode implements FormControl
{
  public InputNode(XDependentTextField textField, XTextDocument doc)
  {
    super(textField, doc);
  }

  public String getContent()
  {
    return (String) Utils.getProperty(textfield, "Content");
  }

  @Override
  public String getString()
  {
    return getContent();
  }

  @Override
  public String toString()
  {
    return "Input [" + getContent() + "]";
  }

  @Override
  public boolean visit(DocumentTreeVisitor visit)
  {
    return visit.formControl(this);
  }

  @Override
  public int getType()
  {
    return INPUT_CONTROL;
  }

  @Override
  public String getDescriptor()
  {
    StringBuilder buf = new StringBuilder();
    try
    {
      buf.append((String) UNO.getProperty(textfield, "Hint"));
    }
    catch (Exception x)
    {}
    
    if (buf.toString().trim().length() < 2)
    {
      try

      {
        buf.append((String) UNO.getProperty(textfield, "Content"));
      }
      catch (Exception x)
      {}
    }
    return buf.toString();
  }

  @Override
  public String surroundWithBookmark(String bmName)
  {
    XTextRange range = UNO.XTextContent(textfield).getAnchor();
    Bookmark bm = new Bookmark(bmName, doc, range);
    return bm.getName();
  }
}
