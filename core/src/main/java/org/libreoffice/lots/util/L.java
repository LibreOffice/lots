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
package org.libreoffice.lots.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.libreoffice.lots.config.ConfigThingy;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;


/**
 * Localization functions
 */
public class L
{

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(L.class);

  private static StringBuilder debugMessages;
  private static I18n i18n;

  /**
   * Initialized for the current language and maps an original string to a translated string.
   */
  private static final Map<String, String> mapMessageToTranslation =
    new HashMap<>();

  private L()
  {}

  /**
   * Translate string (based on gettext translations, provided by WollMux)
   *
   * @param original Untranslated string
   * @return Translated string if translation is found, otherwise untranslated string.
   */
  public static String m(String original)
  {
    if (i18n == null)
      return original;
    return i18n.tr(original);
  }

  /**
   * Translate user template strings (based on ConfigThingy translations, provided by user)
   *
   * @param original Untranslated string
   * @return Translated string if translation is found, otherwise untranslated string.
   */
  public static String tm(String original)
  {
    String trans = mapMessageToTranslation.get(original.trim());
    if (trans == null)
      return original;
    else
      return trans;
  }

  /**
   * Translate string (based on gettext translations, provided by WollMux).
   * All occurrences of "{0}" will be replaced by insertion1.
   *
   * @param original Untranslated string
   * @param insertion1 Insertion 1
   * @return Translated string if translation is found, otherwise untranslated string.
   */
  public static String m(String original, Object insertion1)
  {
    return MessageFormat.format(m(original), insertion1);
  }

  /**
   * Translate string (based on gettext translations, provided by WollMux).
   * All occurrences of "{0}" will be replaced by insertion1, "{1}" by insertion2, etc.
   *
   * @param original Untranslated string
   * @param insertion1 Insertion 1
   * @param insertion2 Insertion 2
   * @return Translated string if translation is found, otherwise untranslated string.
   */
  public static String m(String original, Object insertion1, Object insertion2)
  {
    return MessageFormat.format(m(original), insertion1, insertion2 );
  }

  /**
   * Translate string (based on gettext translations, provided by WollMux).
   * All occurrences of "{0}" will be replaced by insertion1, "{1}" by insertion2, etc.
   *
   * @param original Untranslated string
   * @param insertion1 Insertion 1
   * @param insertion2 Insertion 2
   * @param insertion3 Insertion 3
   * @return Translated string if translation is found, otherwise untranslated string.
   */
  public static String m(String original, Object insertion1, Object insertion2,
      Object insertion3)
  {
    return MessageFormat.format(m(original), insertion1, insertion2, insertion3);
  }

  /**
   * Translate string (based on gettext translations, provided by WollMux).
   * All occurrences of "{0}" will be replaced by insertion1, "{1}" by insertion2, etc.
   *
   * @param original Untranslated string
   * @param insertion1 Insertion 1
   * @param insertion2 Insertion 2
   * @param insertion3 Insertion 3
   * @param insertion3 Insertion 4
   * @return Translated string if translation is found, otherwise untranslated string.
   */
  public static String m(String original, Object insertion1, Object insertion2,
      Object insertion3, Object insertion4)
  {
    return MessageFormat.format(m(original), insertion1, insertion2, insertion3, insertion4);
  }

  /**
   * Translate string with plural form
   *
   * @param singular Singular form
   * @param plural Plural form
   * @param count Number to evaluate
   * @return Translated string
   */
  public static String mn(String singular, String plural, int count)
  {
    if (i18n == null)
      return singular;
    return i18n.trn(singular, plural, count);
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
   * Initialize translations
   */
  public static void initTranslations()
  {
    try {
      i18n = I18nFactory.getI18n(L.class);
    } catch (Exception e) {
      // in JUnit tests, no translations are available
    }
  }

  /**
   * Initializes the translation map for template translations with l10n.
   *
   * @param l10n
   *          any node with "L10n" subnodes.
   */
  public static void initTemplateTranslations(ConfigThingy l10n)
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
