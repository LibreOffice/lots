/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.util.L;

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

  protected ArrayList<String> blacklist;

  protected ArrayList<String> whitelist;

  protected static ConfClassLoader classLoader;

  protected static final String[] DEFAULT_BLACKLIST = { "java.", "com.sun." };

  protected ConfClassLoader()
  {
    super(new URL[] {});
    blacklist = new ArrayList<>();
    for (String s : ConfClassLoader.DEFAULT_BLACKLIST)
    {
      addBlacklisted(s);
    }
    whitelist = new ArrayList<>();
    whitelist.add("com.sun.star.lib.loader"); // exception for classes in default configuration
  }

  @Override
  public void addURL(URL url)
  {
    super.addURL(url);
  }

  /**
   * Black list a classpath entry.
   *
   * @param name
   *          The classpath entry.
   */
  public void addBlacklisted(String name)
  {
    blacklist.add(name);
  }

  /**
   * White list a classpath entry.
   *
   * @param name
   *          The classpath entry.
   */
  public void addWhitelisted(String name)
  {
    whitelist.add(name);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException
  {
    try
    {
      if (isBlacklisted(name) && !isWhitelisted(name))
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

  private boolean isBlacklisted(String name)
  {
    for (String bl : blacklist)
    {
      if (name.startsWith(bl))
      {
        return true;
      }
    }

    return false;
  }

  private boolean isWhitelisted(String name)
  {
    for (String wl : whitelist)
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
          LOGGER.error(L.m("Fehlerhafte CLASSPATH-Angabe: \"%1\"", urlStr), e);
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

    ConfigThingy confWhitelist = WollMuxFiles.getWollmuxConf().query("CPWHITELIST", 1);
    for (ConfigThingy w : confWhitelist)
    {
      getClassLoader().addWhitelisted(w.toString());
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
