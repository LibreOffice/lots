package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.util.UnoProperty;

public class InputNode extends TextFieldNode implements FormControl
{

  private static final Logger LOGGER = LoggerFactory.getLogger(InputNode.class);

  public InputNode(XDependentTextField textField, XTextDocument doc)
  {
    super(textField, doc);
  }

  public String getContent()
  {
    return (String) Utils.getProperty(textfield, UnoProperty.CONTENT);
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
      buf.append((String) UnoProperty.getProperty(textfield, UnoProperty.HINT));
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
    }
    
    if (buf.toString().trim().length() < 2)
    {
      try

      {
        buf.append((String) UnoProperty.getProperty(textfield, UnoProperty.CONTENT));
      }
      catch (Exception x)
      {
        LOGGER.trace("", x);
      }
    }
    return buf.toString();
  }

  @Override
  public String surroundWithBookmark(String bmName) throws UnoHelperException
  {
    XTextRange range = UNO.XTextContent(textfield).getAnchor();
    Bookmark bm = new Bookmark(bmName, doc, range);
    return bm.getName();
  }
}
