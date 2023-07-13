/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.config;

/**
 * A Utility class for Strings.
 *
 * @author Daniel Sikeler
 */
public final class Trimmer
{

  private Trimmer()
  {
  }

  /**
   * Remove the first and last quote of the string. Replace escape Sequence '%n'
   * with '\n' and '%%' with '%'.
   *
   * @param value
   *          The string to change.
   * @return The input without quotes.
   */
  public static String trimQuotes(final String value)
  {
    String replace = value.replace("%n", "\n");
    replace = replace.replace("%%", "%");
    int start = 0;
    int end = replace.length();
    if ((replace.startsWith("\"") && replace.endsWith("\""))
        || (replace.startsWith("'") && replace.endsWith("'")))
    {
      start = 1;
      end--;
    }
    return replace.substring(start, end);
  }

  /**
   * Add quotes around the string. Escape Sequence '\n' with '%n' and '%' with
   * '%%'.
   *
   * @param value
   *          The string.
   * @return The string with quotes.
   */
  public static String addQuoates(final String value)
  {
    String replace = value.replace("%", "%%");
    replace = replace.replace("\n", "%n");
    if (!replace.contains("\""))
    {
      return "\"" + replace + "\"";
    } else if (!replace.contains("'"))
    {
      return "'" + replace + "'";
    } else
    {
      return "\"" + replace.replace("\"", "\"\"") + "\"";
    }
  }

  /**
   * Remove the last slash of the string.
   *
   * @param value
   *          The string to change.
   * @return The input without the last slash.
   */
  public static String trimSlash(final String value)
  {
    int end = value.length();
    if (value.endsWith("/"))
    {
      end--;
    }
    return value.substring(0, end);
  }
}
