package de.muenchen.allg.itd51.wollmux.core;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    whitelist.add("com.sun.star.lib.loader"); // Ausnahme für Klassen in der Standardconfig
  }

  @Override
  public void addURL(URL url)
  {
    super.addURL(url);
  }

  public void addBlacklisted(String name)
  {
    blacklist.add(name);
  }

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
   * Liefert einen ClassLoader, der die in wollmux.conf gesetzten CLASSPATH-Direktiven
   * berücksichtigt.
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