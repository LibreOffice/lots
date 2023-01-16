/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.document.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.document.DocumentTreeVisitor;
import de.muenchen.allg.util.UnoProperty;

public class DropdownNode extends TextFieldNode implements
    DropdownFormControl
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DropdownNode.class);

  public DropdownNode(XDependentTextField textField, XTextDocument doc)
  {
    super(textField, doc);
  }

  @Override
  public String[] getItems()
  {
    try
    {
    return (String[]) UnoProperty.getProperty(textfield, UnoProperty.ITEMS);
  } catch (UnoHelperException ex)
  {
    LOGGER.trace("", ex);
    return new String[] {};
  }
  }

  public String getSelectedItem()
  {
    try
    {
      return (String) UnoProperty.getProperty(textfield, UnoProperty.SELECTED_ITEM);
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
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
  public FormControlType getType()
  {
    return FormControlType.DROPDOWN_CONTROL;
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
      buf.append((String) UnoProperty.getProperty(textfield, UnoProperty.NAME));
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
    }

    if (buf.toString().trim().length() < 2)
    {
      buf.append(getSelectedItem());
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
