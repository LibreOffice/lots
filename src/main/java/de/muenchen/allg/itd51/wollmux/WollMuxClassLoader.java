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
package de.muenchen.allg.itd51.wollmux;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.util.L;

public class WollMuxClassLoader extends ConfClassLoader
{

  private static final Logger LOGGER = LoggerFactory.getLogger(WollMuxClassLoader.class);

  private WollMuxClassLoader()
  {
    super();
  }

  /**
   * Parst die CLASSPATH Direktiven und hängt für jede eine weitere URL an den
   * Suchpfad von {@link WollMuxClassLoader#classLoader} an.
   */
  public static void initClassLoader()
  {
    ConfigThingy conf = WollMuxFiles.getWollmuxConf().query("CLASSPATH", 1);
    for (ConfigThingy classpathConf : conf)
    {
      for (ConfigThingy urlConf : classpathConf)
      {
        String urlStr = urlConf.toString();
        if (!urlStr.endsWith("/")
          && (urlStr.indexOf('.') < 0 || urlStr.lastIndexOf('/') > urlStr.lastIndexOf('.')))
        {
          // Falls keine Dateierweiterung angegeben, / ans Ende setzen, weil nur so Verzeichnisse
          // erkannt werden.
          urlStr = urlStr + "/";
        }
        try
        {
          URL url = WollMuxFiles.makeURL(urlStr);
          WollMuxClassLoader.getClassLoader().addURL(url);
        }
        catch (MalformedURLException e)
        {
          LOGGER.error(L.m("Fehlerhafte CLASSPATH-Angabe: \"%1\"", urlStr), e);
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

    ConfigThingy confWhitelist = WollMuxFiles.getWollmuxConf().query("CPWHITELIST", 1);
    for (ConfigThingy w : confWhitelist)
    {
      WollMuxClassLoader.getClassLoader().addWhitelisted(w.toString());
    }

    LOGGER.debug("CLASSPATH=" + urllist);
  }

}