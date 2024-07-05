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
package org.libreoffice.lots.config;


/**
 * Handles the different name formats for files, which are included.
 *
 * @author Daniel Sikeler
 */
public final class PathProcessor
{

  private PathProcessor()
  {
  }

  /**
   * Process include instruction. Extracts the file path and the protocol.
   *
   * @param url
   *          The URL of the file.
   * @return A string representation of the file.
   */
  public static String processInclude(final String url)
  {
    String path = url;
    final String filePrefix = "file:";
    final String[] prefix = { "///", "//localhost/", "/", "//" };
    if (path.startsWith(filePrefix))
    {
      path = path.substring(filePrefix.length());
    }
    for (int index = 0; index < prefix.length; index++)
    {
      if (path.startsWith(prefix[index]))
      {
        return trim(path, prefix[index].length());
      }
    }
    return path;
  }

  /**
   * Remove a offset from a String. If it is a Linux-System removes one char
   * less than offset.
   *
   * @param string
   *          The string to trim.
   * @param offset
   *          The number of chars to remove.
   * @return A new String starting at offset in string.
   */
  private static String trim(final String string, final int offset)
  {
    if (System.getProperty("os.name").toLowerCase().contains("win"))
    {
      return string.substring(offset);
    } else
    {
      return string.substring(offset - 1);
    }
  }
}
