package de.muenchen.allg.itd51.wollmux;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

public class WollMuxClassLoader extends URLClassLoader
{
  private ArrayList<String> blacklist;

  private ArrayList<String> whitelist;

  private static WollMuxClassLoader classLoader;

  private static final String[] BLACKLIST = {
  "java.", "com.sun." };

  private WollMuxClassLoader()
  {
    super(new URL[] {});
    blacklist = new ArrayList<String>();
    whitelist = new ArrayList<String>();
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
      if (c != null) return c;
      return super.findClass(name);
    }
    catch (ClassNotFoundException x)
    {
      return WollMuxClassLoader.class.getClassLoader().loadClass(name);
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
   * Liefert einen ClassLoader, der die in wollmux,conf gesetzten
   * CLASSPATH-Direktiven berücksichtigt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static WollMuxClassLoader getClassLoader()
  {
    if (classLoader == null)
    {
      classLoader = new WollMuxClassLoader();
    }
    return classLoader;
  }

  /**
   * Parst die CLASSPATH Direktiven und hängt für jede eine weitere URL an den
   * Suchpfad von {@link WollMuxClassLoader#classLoader} an.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static void initClassLoader()
  {
    ConfigThingy conf = WollMuxFiles.getWollmuxConf().query("CLASSPATH", 1);
    Iterator<ConfigThingy> parentiter = conf.iterator();
    while (parentiter.hasNext())
    {
      ConfigThingy CLASSPATHconf = parentiter.next();
      Iterator<ConfigThingy> iter = CLASSPATHconf.iterator();
      while (iter.hasNext())
      {
        String urlStr = iter.next().toString();
        if (!urlStr.endsWith("/")
          && (urlStr.indexOf('.') < 0 || urlStr.lastIndexOf('/') > urlStr.lastIndexOf('.')))
          urlStr = urlStr + "/"; // Falls keine
        // Dateierweiterung
        // angegeben, /
        // ans Ende setzen, weil nur so Verzeichnisse
        // erkannt werden.
        try
        {
          URL url = WollMuxFiles.makeURL(urlStr);
          WollMuxClassLoader.getClassLoader().addURL(url);
        }
        catch (MalformedURLException e)
        {
          Logger.error(L.m("Fehlerhafte CLASSPATH-Angabe: \"%1\"", urlStr), e);
        }
      }
    }
  
    StringBuilder urllist = new StringBuilder();
    URL[] urls = WollMuxClassLoader.getClassLoader().getURLs();
    for (int i = 0; i < urls.length; ++i)
    {
      urllist.append(urls[i].toExternalForm());
      urllist.append("  ");
    }
  
    for (String s : WollMuxClassLoader.BLACKLIST)
    {
      WollMuxClassLoader.getClassLoader().addBlacklisted(s);
    }
  
    ConfigThingy confWhitelist = WollMuxFiles.getWollmuxConf().query("CPWHITELIST", 1);
    for (ConfigThingy w : confWhitelist)
    {
      WollMuxClassLoader.getClassLoader().addWhitelisted(w.toString());
    }
  
    Logger.debug("CLASSPATH=" + urllist);
  }

}