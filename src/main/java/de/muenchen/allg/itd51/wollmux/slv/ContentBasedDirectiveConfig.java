package de.muenchen.allg.itd51.wollmux.slv;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.chaosfirebolt.converter.RomanInteger;
import com.github.chaosfirebolt.converter.constants.IntegerType;
import com.sun.star.lang.IllegalArgumentException;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

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
  private static final NumberFormat FORMAT;

  /**
   * The name of copies.
   */
  private static final String COPY_NAME;

  /**
   * The highlight colors for each {@link PrintBlockSignature}.
   */
  private static final Map<PrintBlockSignature, String> highlightColors = new EnumMap<>(
      PrintBlockSignature.class);

  // statically initialize the properties
  static
  {
    String format = "roman";
    String name = "Abdruck";
    for (PrintBlockSignature pbName : PrintBlockSignature.values())
    {
      highlightColors.put(pbName, null);
    }
    try
    {
      if (WollMuxFiles.getWollmuxConf() != null)
      {
        ConfigThingy conf = WollMuxFiles.getWollmuxConf().query("SachleitendeVerfuegungen");
        if (conf.count() > 0)
        {
          conf = conf.getLastChild();
          for (ConfigThingy child : conf)
          {
            switch (child.getName())
            {
            case "NUMBERS":
              format = child.toString();
              break;
            case "ABDRUCK_NAME":
              name = child.toString();
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
      FORMAT = NumberFormat.valueOf(format.toUpperCase());
      LOGGER.debug("\"Verwende Zahlenformat '{}' aus Attribut NUMBERS.\"", FORMAT);

      COPY_NAME = name;
      LOGGER.debug("Verwende ABDRUCK_NAME '{}'", COPY_NAME);
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
      LOGGER.error(L.m("Der angegebene Farbwert '%1' ist ungültig!", value));
      return defaultColor;
    }
  }

  /**
   * Get the number according to {@link #FORMAT}.
   *
   * @param i
   *          The number to get in the correct format.
   * @return A String of the number.
   */
  public static String getNumber(int i)
  {
    String number = Integer.toString(i);
    if (FORMAT == NumberFormat.ROMAN)
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
    return COPY_NAME;
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
