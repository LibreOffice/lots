package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.DocumentTree;
import de.muenchen.allg.itd51.wollmux.core.document.DocumentTreeVisitor;

public class DropdownNode extends TextFieldNode implements
    DropdownFormControl
{
  public DropdownNode(XDependentTextField textField, XTextDocument doc)
  {
    super(textField, doc);
  }

  @Override
  public String[] getItems()
  {
    return (String[]) UNO.getProperty(textfield, "Items");
  }

  public String getSelectedItem()
  {
    try
    {
      return (String) UNO.getProperty(textfield, "SelectedItem");
    }
    catch (Exception x)
    {
      return "";
    }
  }

  @Override
  public String getString()
  {
    return getSelectedItem();
  }

  @Override
  public boolean visit(DocumentTreeVisitor visit)
  {
    return visit.formControl(this);
  }

  @Override
  public int getType()
  {
    return DROPDOWN_CONTROL;
  }

  @Override
  public String toString()
  {
    StringBuilder buf = new StringBuilder("Dropdown ");
    String[] items = getItems();
    String selectedItem = getSelectedItem();
    for (int i = 0; i < items.length; ++i)
    {
      if (i > 0)
      {
        buf.append(", ");
      }
      String item = items[i];
      if (item.equals(selectedItem))
      {
        buf.append("((").append(item).append("))");
      }
      else
      {
        buf.append("\"").append(item).append("\"");
      }
    }

    return buf.toString();
  }

  @Override
  public String getDescriptor()
  {
    StringBuilder buf = new StringBuilder();
    try
    {
      buf.append((String) UNO.getProperty(textfield, "Name"));
    }
    catch (Exception x)
    {}

    if (buf.toString().trim().length() < 2) 
    {
      buf.append(getSelectedItem());
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