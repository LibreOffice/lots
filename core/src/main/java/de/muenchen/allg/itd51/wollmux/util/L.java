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
package de.muenchen.allg.itd51.wollmux.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;

/**
 * Localization functions
 */
public class L
{

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(L.class);

  private static StringBuilder debugMessages;

  /**
   * Initialized for the current language and maps an original string to a translated string.
   */
  private static final Map<String, String> mapMessageToTranslation =
    new HashMap<>();

  private L()
  {}

  /**
   * If a translation is available for original, it will be returned,
   * otherwise the original string.
   */
  public static String m(String original)
  {
    String trans = mapMessageToTranslation.get(original.trim());
    if (trans == null)
      return original;
    else
      return trans;
  }

  /**
   * If a translation is available for original, it will be returned,
   * otherwise the original string.
   * All occurrences of "%1" will be replaced by insertion1.
   */
  public static String m(String original, Object insertion1)
  {
    // do not use replaceAll because it interprets \ and $ specially
    return replace(m(original), "%1", "" + insertion1);
  }

  /**
   * If a translation is available for original, it will be returned,
   * otherwise the original string.
   * All occurrences of "%1" will be replaced by insertion1 and of "%2" by insertion2.
   */
  public static String m(String original, Object insertion1, Object insertion2)
  {
 // do not use replaceAll because it interprets \ and $ specially
    return replace(m(original, insertion1), "%2", "" + insertion2);
  }

  public static String m(String original, Object insertion1, Object insertion2,
      Object insertion3)
  {
    // do not use replaceAll because it interprets \ and $ specially
    return replace(m(original, insertion1, insertion2), "%3", "" + insertion3);
  }

  public static String m(String original, Object insertion1, Object insertion2,
      Object insertion3, Object insertion4)
  {
    // do not use replaceAll because it interprets \ and $ specially
    return replace(m(original, insertion1, insertion2, insertion3), "%4", ""
      + insertion4);
  }

  /**
   * Replaces in where all occurrences of what by withWhat and returns the result returned.
   */
  private static String replace(String where, String what, String withWhat)
  {
    int i = where.indexOf(what);
    if (i < 0 || what.length() == 0) {
      return where;
    }

    StringBuilder buffy = new StringBuilder(where);
    while (i >= 0)
    {
      buffy.replace(i, i + what.length(), withWhat);
      i = buffy.indexOf(what, i + withWhat.length());
    }

    return buffy.toString();
  }

  /**
   * Returns all debug messages accumulated during initialization and then releases their memory.
   */
  public static String flushDebugMessages()
  {
    String str;
    if (debugMessages != null)
    {
      str = debugMessages.toString();
      debugMessages = null; // Free memory
    }
    else
      str = "";

    return str;
  }

  /**
   * Initializes the translation map with l10n.
   *
   * @param l10n
   *          any node with "L10n" subnodes.
   */
  public static void init(ConfigThingy l10n)
  {
    try
    {
      String messageLanguage = Locale.getDefault().getLanguage();
      debugMessages = new StringBuilder();
      debugMessages.append("Message language from locale: " + messageLanguage + '\n');
      String lcMessages = System.getenv("LC_MESSAGES");
      if (lcMessages != null && lcMessages.length() >= 2)
      {
        int i = lcMessages.indexOf('.');
        if (i >= 0) {
          lcMessages = lcMessages.substring(0, i);
        }
        i = lcMessages.indexOf('@');
        if (i >= 0) {
          lcMessages = lcMessages.substring(0, i);
        }
        debugMessages.append("LC_MESSAGES override: " + lcMessages + '\n');
        messageLanguage = lcMessages;
      }

      ConfigThingy aliases = l10n.get("LanguageAliases", 2);
      Iterator<?> iter = aliases.iterator();
      while (iter.hasNext())
      {
        ConfigThingy aliasConf = (ConfigThingy) iter.next();
        if (aliasConf.count() > 1)
        {
          Iterator<?> subIter = aliasConf.iterator();
          String languageCode = subIter.next().toString();
          if (messageLanguage.equals(languageCode)) {
            break;
          }
          boolean findAlias = true;
          while (subIter.hasNext() && findAlias)
          {
            String alias = subIter.next().toString();
            if (messageLanguage.equals(alias))
            {
              debugMessages.append("Alias mapping => " + languageCode + '\n');
              messageLanguage = languageCode;
              findAlias = false;
            }
          }
        }
        else
        {
          debugMessages.append("Aliases line with less than 2 entries: "
            + aliasConf.stringRepresentation());
          debugMessages.append('\n');
        }
      }

      ConfigThingy messages = l10n.query("Messages", 2);
      for (ConfigThingy msg : messages)
      {
        iter = msg.iterator();
        String original = "foo";
        while (iter.hasNext())
        {
          ConfigThingy conf = (ConfigThingy) iter.next();
          if ("original".equalsIgnoreCase(conf.getName()))
            original = conf.toString();

          if (conf.getName().equalsIgnoreCase(messageLanguage))
            mapMessageToTranslation.put(original, conf.toString());
        }
      }
    }
    catch (Exception x)
    {
      LOGGER.error("Error initializing localized strings", x);
    }
  }
}
