package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XControlModel;
import com.sun.star.drawing.XControlShape;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

public class CheckboxNode implements FormControl, Node
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(CheckboxNode.class);

  private XTextDocument doc;

  private XControlShape shape;

  private XControlModel model;

  private static final Short CHECKED_STATE = Short.valueOf((short) 1);

  public CheckboxNode(XControlShape shape, XControlModel model, XTextDocument doc)
  {
    super();
    this.shape = shape;
    this.model = model;
    this.doc = doc;
  }

  public boolean isChecked()
  {
    return CHECKED_STATE.equals(Utils.getProperty(model, "State"));
  }

  @Override
  public String toString()
  {
    return "Checkbox [" + (isChecked() ? "X]" : " ]");
  }

  @Override
  public boolean visit(DocumentTreeVisitor visit)
  {
    return visit.formControl(this);
  }

  @Override
  public int getType()
  {
    return CHECKBOX_CONTROL;
  }

  @Override
  public String getString()
  {
    return Boolean.toString(isChecked());
  }

  @Override
  public String getDescriptor()
  {
    StringBuilder buf = new StringBuilder();
    try
    {
      buf.append((String) UNO.getProperty(model, "HelpText"));
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
    }

    if (buf.toString().trim().length() < 2)
    {
      try
      {
        buf.append(UNO.XNamed(model).getName());
      }
      catch (Exception x)
      {
        LOGGER.trace("", x);
      }
    }
    return buf.toString();
  }

  @Override
  public String surroundWithBookmark(String bmName)
  {
    XTextRange range = UNO.XTextContent(shape).getAnchor();
    XTextCursor cursor = range.getText().createTextCursorByRange(range);
    cursor.goRight((short) 1, true);
    Bookmark bm = new Bookmark(bmName, doc, cursor);
    return bm.getName();
  }
}
