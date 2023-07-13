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
package org.libreoffice.lots;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.libreoffice.lots.config.ConfigThingy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClassLoader which uses the classpath defined in the WollMux configuration. It's implemented as a
 * singleton.
 *
 * @see <a href=
 *      "https://wollmux.org/18.2/Konfigurationsdatei_wollmux_conf.html#einbinden-referatseigener-pluginsclasspath">
 *      wollmux.org</a>
 */
public class ConfClassLoader extends URLClassLoader
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfClassLoader.class);

  protected ArrayList<String> denylist;

  protected ArrayList<String> allowlist;

  protected static ConfClassLoader classLoader;

  protected static final String[] DEFAULT_DENYLIST = { "java.", "com.sun." };

  protected ConfClassLoader()
  {
    super(new URL[] {});
    denylist = new ArrayList<>();
    for (String s : ConfClassLoader.DEFAULT_DENYLIST)
    {
      addDenylisted(s);
    }
    allowlist = new ArrayList<>();
    allowlist.add("com.sun.star.lib.loader"); // exception for classes in default configuration
  }

  @Override
  public void addURL(URL url)
  {
    super.addURL(url);
  }

  /**
   * Deny-list a classpath entry.
   *
   * @param name
   *          The classpath entry.
   */
  public void addDenylisted(String name)
  {
    denylist.add(name);
  }

  /**
   * White list a classpath entry.
   *
   * @param name
   *          The classpath entry.
   */
  public void addAllowlisted(String name)
  {
    allowlist.add(name);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    try
    {
      if (isDenylisted(name) && !isAllowlisted(name))
      {
        throw new ClassNotFoundException();
      }

      Class<?> c = findLoadedClass(name);
      if (c != null)
        return c;
      return super.findClass(name);
    } catch (ClassNotFoundException x)
    {
      return ConfClassLoader.class.getClassLoader().loadClass(name);
    }
  }

  private boolean isDenylisted(String name)
  {
    for (String bl : denylist)
    {
      if (name.startsWith(bl))
      {
        return true;
      }
    }

    return false;
  }

  private boolean isAllowlisted(String name)
  {
    for (String wl : allowlist)
    {
      if (name.startsWith(wl))
      {
        return true;
      }
    }

    return false;
  }

  /**
   * Initialize the ClassLoader with the WollMux configuration.
   *
   * @param conf
   *          The "CLASSPATH" section of the WollMux configuration.
   */
  public static void initClassLoader(ConfigThingy conf)
  {
    for (ConfigThingy classpathConf : conf)
    {
      for (ConfigThingy urlConf : classpathConf)
      {
        String urlStr = urlConf.toString();
        if (!urlStr.endsWith("/") && (urlStr.indexOf('.') < 0 || urlStr.lastIndexOf('/') > urlStr.lastIndexOf('.')))
        {
          // If the URL has no file extension, add a slash to use it as directory
          urlStr = urlStr + "/";
        }
        try
        {
          URL url = WollMuxFiles.makeURL(urlStr);
          getClassLoader().addURL(url);
        } catch (MalformedURLException e)
        {
          LOGGER.error("Illegal CLASSPATH specified: '{}'", urlStr, e);
        }
      }
    }

    StringBuilder urllist = new StringBuilder();
    URL[] urls = getClassLoader().getURLs();
    for (int i = 0; i < urls.length; ++i)
    {
      urllist.append(urls[i].toExternalForm());
      urllist.append("  ");
    }

    ConfigThingy confAllowlist = WollMuxFiles.getWollmuxConf().query("CPALLOWLIST", 1);
    for (ConfigThingy w : confAllowlist)
    {
      getClassLoader().addAllowlisted(w.toString());
    }

    LOGGER.debug("CLASSPATH={}", urllist);
  }

  /**
   * Create an instance of this ClassLoader and return it.
   *
   * @return An instance of this class.
   */
  public static ConfClassLoader getClassLoader()
  {
    if (classLoader == null)
    {
      LOGGER.warn("ClassLoader wurde nicht konfiguriert");
      classLoader = new ConfClassLoader();
    }
    return classLoader;
  }
}
