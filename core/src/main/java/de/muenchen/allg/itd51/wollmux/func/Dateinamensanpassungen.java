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
package de.muenchen.allg.itd51.wollmux.func;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.uno.AnyConverter;

import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;

public class Dateinamensanpassungen
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(Dateinamensanpassungen.class);

  private static final Pattern PROP = Pattern.compile("\\$\\{([^\\}]+)\\}");

  private Dateinamensanpassungen()
  {
  }

  /**
   * This function can take a pipe ('|') separated list with
   * Paths/filenames are passed, of which the first entry in this list
   * is returned whose path portion is actually available.
   * Within a path/filename, prior to the availability check, with
   * ${&lt;name&gt;} the value of a Java system property inserted into the filename
   * become.
   */
  public static String verfuegbarenPfadVerwenden(String fileName)
  {
    String[] paths = fileName.split("\\s*\\|\\s*");
    String first = null;
    for (String p : paths)
    {
      String replacePath = replaceProperties(p);

      if (first == null)
        first = replacePath;

      File f = new File(replacePath);
      File parent = f.getParentFile();
      if (parent != null && parent.isDirectory())
        return f.toString();
    }
    if (first == null)
      first = paths[0];
    return new File(first).getName();
  }

  /**
   * works like
   * {@link Dateinamensanpassungen#verfuegbarenPfadVerwenden(String)} and
   * additionally takes the following LHM-specific file name adjustments
   * before:
   *
   * a. ß in ss ä in ae ö in oe ü in ue, Ä in Ae, Ü in ue,
   * O to Oe
   *
   * b. All special characters, punctuation marks etc. should be replaced by _,
   * except for the dot in front of the file extension (.odt)
   *
   * c. This means that only the numbers from 0-9, the letters, are left in the file name
   * from a-z and A-Z and the underscore _ present
   *
   * i.e. The length of the file name is limited to a maximum of 240 characters (incl. path)
   * limited; if the determined file name is longer, it becomes after 240
   * Characters truncated (actually truncated after 236 characters
   * and then the extension .odt is appended).
   *
   * Working directory path in LibreOffice is appended to filename if specified filename is not absolute.
   */
  public static String lhmDateinamensanpassung(String fileName)
  {
    fileName = replaceProperties(fileName);
    File f = new File(fileName);
    if (!f.isAbsolute())
    {
      try
      {
        // gets the working directory path from LO
        Object ps = UnoComponent.createComponentWithContext(UnoComponent.CSS_UTIL_PATH_SETTINGS);
        URL dir = new URL(AnyConverter.toString(UnoProperty.getProperty(ps, UnoProperty.WORK)));
        f = new File(dir.getPath(), fileName);
      } catch (MalformedURLException | UnoHelperException e)
      {
        LOGGER.error("", e);
      }
    }
    String pfad = verfuegbarenPfadVerwenden(f.getAbsolutePath());
    File file = new File(pfad);
    int parentLength = 0;
    if (file.getParent() != null)
      parentLength = file.getParent().length() + 1;

    String name = file.getName();
    String suffix = "";
    int idx = name.lastIndexOf('.');
    if (idx >= 0)
    {
      suffix = name.substring(idx);
      if (suffix.matches("\\.\\w{3,4}"))
        name = name.substring(0, idx);
      else
        suffix = "";
    }

    name = name.replaceAll("ß", "ss");
    name = name.replaceAll("ä", "ae");
    name = name.replaceAll("ö", "oe");
    name = name.replaceAll("ü", "ue");
    name = name.replaceAll("Ä", "Ae");
    name = name.replaceAll("Ö", "Oe");
    name = name.replaceAll("Ü", "Ue");
    name = name.replaceAll("[^a-zA-Z_0-9]", "_");

    int maxlength = 240 - suffix.length() - parentLength;
    if (name.length() > maxlength)
      name = name.substring(0, maxlength);

    name = name + suffix;

    file = new File(file.getParentFile(), name);
    return file.toString();
  }

  private static String replaceProperties(final String fileName)
  {
    // replace all ${<prop>} with evaluated content
    Matcher m = PROP.matcher(fileName);
    StringBuffer buf = new StringBuffer();
    while (m.find())
    {
      String propVal = System.getProperty(m.group(1).trim());
      if (propVal == null)
        propVal = "";
      m.appendReplacement(buf, Matcher.quoteReplacement(propVal));
    }
    m.appendTail(buf);
    return buf.toString();
  }
}
