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

import com.sun.star.awt.XControlModel;
import com.sun.star.drawing.XControlShape;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoProperty;

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
    return CHECKED_STATE.equals(Utils.getProperty(model, UnoProperty.STATE));
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
  public FormControlType getType()
  {
    return FormControlType.CHECKBOX_CONTROL;
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
      buf.append((String) UnoProperty.getProperty(model, UnoProperty.HELP_TEXT));
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
  public String surroundWithBookmark(String bmName) throws UnoHelperException
  {
    XTextRange range = UNO.XTextContent(shape).getAnchor();
    XTextCursor cursor = range.getText().createTextCursorByRange(range);
    cursor.goRight((short) 1, true);
    Bookmark bm = new Bookmark(bmName, doc, cursor);
    return bm.getName();
  }
}
