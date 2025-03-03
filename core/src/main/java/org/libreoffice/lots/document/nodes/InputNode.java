/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.document.nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.document.text.Bookmark;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.document.DocumentTreeVisitor;
import org.libreoffice.lots.util.Utils;

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
  public FormControlType getType()
  {
    return FormControlType.INPUT_CONTROL;
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
