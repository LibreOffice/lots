/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.afid.UnoIterator;
import de.muenchen.allg.util.UnoProperty;

public class Utils
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  private Utils()
  {
  }

  public static Object getProperty(Object o, String propName)
  {
    try
    {
      return UnoProperty.getProperty(o, propName);
    }
    catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return null;
    }
  }

  public static Object setProperty(Object o, String propName, Object propVal)
  {
    try
    {
      return UnoProperty.setProperty(o, propName, propVal);
    }
    catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return null;
    }
  }

  /**
   * Get the string of the given {@link XTextRange}.
   *
   * @param textRange
   *          A text range object.
   *
   * @return String The content of the text range.
   */
  public static String getStringOfXTextRange(XTextRange textRange)
  {
    String str = "";
    if (UnoIterator.create(textRange, XTextRange.class).hasNext())
    {
      str = textRange.getString();
    }
    return str;
  }
}
