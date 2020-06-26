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
package de.muenchen.allg.itd51.wollmux.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

/**
 * Funktionen zur Lokalisierung.
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 */
public class L
{

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(L.class);

  private static StringBuilder debugMessages;

  /**
   * Wird für die aktuelle Sprache initialisiert und bildet einen Originalstring auf
   * einen übersetzten String ab.
   */
  private static final Map<String, String> mapMessageToTranslation =
    new HashMap<>();

  private L()
  {}

  /**
   * Falls für original eine Übersetzung verfügbar ist, wird diese zurückgeliefert,
   * ansonsten der Originalstring.
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
   * Falls für original eine Übersetzung verfügbar ist, wird diese zurückgeliefert,
   * ansonsten der Originalstring. Dabei werden alle Vorkommen von "%1" durch
   * insertion1 ersetzt.
   */
  public static String m(String original, Object insertion1)
  {
    // nicht replaceAll verwenden, weil es \ und $ besonders
    // interpretiert
    return replace(m(original), "%1", "" + insertion1);
  }

  /**
   * Falls für original eine Übersetzung verfügbar ist, wird diese zurückgeliefert,
   * ansonsten der Originalstring. Dabei werden alle Vorkommen von "%1" durch
   * insertion1 und von "%2" durch insertion2 ersetzt.
   */
  public static String m(String original, Object insertion1, Object insertion2)
  {
    // nicht replaceAll verwenden, weil es \ und $ besonders
    // interpretiert
    return replace(m(original, insertion1), "%2", "" + insertion2);
  }

  public static String m(String original, Object insertion1, Object insertion2,
      Object insertion3)
  {
    // nicht replaceAll verwenden, weil es \ und $ besonders
    // interpretiert
    return replace(m(original, insertion1, insertion2), "%3", "" + insertion3);
  }

  public static String m(String original, Object insertion1, Object insertion2,
      Object insertion3, Object insertion4)
  {
    // nicht replaceAll verwenden, weil es \ und $ besonders
    // interpretiert
    return replace(m(original, insertion1, insertion2, insertion3), "%4", ""
      + insertion4);
  }

  /**
   * Ersetzt in where alle Vorkommen von what durch withWhat und liefert das Ergebnis
   * zurück.
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
   * Liefert alle während der Initialisierung aufgelaufenen Debug-Messages zurück und
   * gibt dann ihren Speicher frei.
   */
  public static String flushDebugMessages()
  {
    String str;
    if (debugMessages != null)
    {
      str = debugMessages.toString();
      debugMessages = null; // Speicher freigeben
    }
    else
      str = "";

    return str;
  }

  /**
   * Initialisiert die Übersetzungs-Map mit l10n.
   *
   * @param l10n
   *          ein beliebiger Knoten mit "L10n"-Unterknoten.
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
