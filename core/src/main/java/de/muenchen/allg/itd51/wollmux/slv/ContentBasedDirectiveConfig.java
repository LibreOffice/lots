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
package de.muenchen.allg.itd51.wollmux.slv;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.chaosfirebolt.converter.RomanInteger;
import com.github.chaosfirebolt.converter.constants.IntegerType;
import com.sun.star.lang.IllegalArgumentException;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * The configuration for content based directives.
 */
public class ContentBasedDirectiveConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentBasedDirectiveConfig.class);

  private enum NumberFormat
  {
    ARABIC,
    ROMAN;
  }

  /**
   * The format of the numbers. One of {@link NumberFormat}.
   */
  private static NumberFormat format;

  /**
   * The name of copies.
   */
  private static String copyName;

  /**
   * The highlight colors for each {@link PrintBlockSignature}.
   */
  private static final Map<PrintBlockSignature, String> highlightColors = new EnumMap<>(
      PrintBlockSignature.class);

  // statically initialize the properties
  static
  {
    configure(WollMuxFiles.getWollmuxConf());
  }

  /**
   * Configure Content Based Directives
   *
   * @param config
   *          The configuration.
   */
  public static void configure(ConfigThingy config)
  {
    String formatOption = "roman";
    String nameOption = "Abdruck";
    for (PrintBlockSignature pbName : PrintBlockSignature.values())
    {
      highlightColors.put(pbName, null);
    }
    try
    {
      if (config != null)
      {
        ConfigThingy conf = config.query("SachleitendeVerfuegungen");
        if (conf.count() > 0)
        {
          conf = conf.getLastChild();
          for (ConfigThingy child : conf)
          {
            switch (child.getName())
            {
            case "NUMBERS":
              formatOption = child.toString();
              break;
            case "ABDRUCK_NAME":
              nameOption = child.toString();
              break;
            case "ALL_VERSIONS_HIGHLIGHT_COLOR":
              highlightColors.put(PrintBlockSignature.ALL_VERSIONS,
                  checkHighlightColor(child.toString()));
              break;
            case "DRAFT_ONLY_HIGHLIGHT_COLOR":
              highlightColors.put(PrintBlockSignature.DRAFT_ONLY,
                  checkHighlightColor(child.toString()));
              break;
            case "NOT_IN_ORIGINAL_HIGHLIGHT_COLOR":
              highlightColors.put(PrintBlockSignature.NOT_IN_ORIGINAL,
                  checkHighlightColor(child.toString()));
              break;
            case "ORIGINAL_ONLY_HIGHLIGHT_COLOR":
              highlightColors.put(PrintBlockSignature.ORIGINAL_ONLY,
                  checkHighlightColor(child.toString()));
              break;
            case "COPY_ONLY_HIGHLIGHT_COLOR":
              highlightColors.put(PrintBlockSignature.COPY_ONLY, checkHighlightColor(child.toString()));
              break;
            default:
              LOGGER.warn("Unbekannte Einstellung {}", child.getName());
            }
          }
        }
      }
    } catch (NodeNotFoundException e)
    {
      LOGGER.debug("", e);
    } finally
    {
      format = NumberFormat.valueOf(formatOption.toUpperCase());
      LOGGER.debug("\"Verwende Zahlenformat '{}' aus Attribut NUMBERS.\"", format);

      copyName = nameOption;
      LOGGER.debug("Verwende ABDRUCK_NAME '{}'", copyName);
    }
  }

  /**
   * Checks if a string is a valid color in hex format (AARRGGBB).
   *
   * @param value
   *          The color string.
   * @return The value if it is value or black if not.
   */
  private static String checkHighlightColor(String value)
  {
    String defaultColor = "00000000";
    try
    {
      if (value == null || value.equals("") || value.equalsIgnoreCase("none"))
        return defaultColor;
      // Check if valid hex number
      Integer.parseInt(value, 16);
      return value;
    } catch (NumberFormatException e)
    {
      LOGGER.error(L.m("The specified colour value in attribute '%1' is invalid!", value));
      return defaultColor;
    }
  }

  /**
   * Get the number according to {@link #format}.
   *
   * @param i
   *          The number to get in the correct format.
   * @return A String of the number.
   */
  public static String getNumber(int i)
  {
    String number = Integer.toString(i);
    if (format == NumberFormat.ROMAN)
    {
      try
      {
        number = RomanInteger.parse(Integer.toString(i), IntegerType.ARABIC).toString();
      } catch (NumberFormatException | IllegalArgumentException e)
      {
        LOGGER.debug("Not a valid number", e);
      }
    }
    return number + ".";
  }

  public static String getName()
  {
    return copyName;
  }

  /**
   * Get color of a printing block.
   *
   * @param name
   *          The type of printing block.
   * @return The color as Integer.
   */
  public static String getHighlightColor(PrintBlockSignature name)
  {
    return highlightColors.get(name);
  }
}
