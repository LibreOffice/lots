package de.muenchen.allg.itd51.wollmux.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.XNameContainer;
import com.sun.star.style.XStyle;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;

/**
 * Service for creating and accessing styles.
 */
public class StyleService
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(StyleService.class);

  private static final String PARAGRAPH_STYLES = "ParagraphStyles";

  private static final String CHARACTER_STYLES = "CharacterStyles";

  private StyleService()
  {
  }

  /**
   * Get the paragraph style with a given name.
   *
   * @param doc
   *          The document.
   * @param name
   *          The name of the paragraph style.
   * @return The style with the name or null if there is no such style.
   */
  public static XStyle getParagraphStyle(XTextDocument doc, String name)
  {
    XStyle style = null;

    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    if (pss != null)
    {
      try
      {
	style = UNO.XStyle(pss.getByName(name));
      } catch (java.lang.Exception e)
      {
	LOGGER.trace("", e);
      }
    }
    return style;
  }

  /**
   * Creates a new paragraph style.
   *
   * @param doc
   *          The document.
   * @param name
   *          The name of the paragraph style.
   * @param parentStyleName
   *          The name of the parent paragraph style or null if there is no
   *          parent, which defaults to "Standard"
   * @return The style or null if it cloudn't be created.
   */
  public static XStyle createParagraphStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, PARAGRAPH_STYLES);
    if (pss != null)
    {
      return createStyle(doc, pss, "com.sun.star.style.ParagraphStyle", name,
          parentStyleName);
    }
    return null;
  }

  /**
   * Get the character style with a given name.
   *
   * @param doc
   *          The document.
   * @param name
   *          The name of the character style.
   * @return The style with the name or null if there is no such style.
   */
  public static XStyle getCharacterStyle(XTextDocument doc, String name)
  {
    XStyle style = null;

    XNameContainer pss = getStyleContainer(doc, CHARACTER_STYLES);
    if (pss != null)
    {
      try
      {
	style = UNO.XStyle(pss.getByName(name));
      } catch (java.lang.Exception e)
      {
	LOGGER.trace("", e);
      }
    }
    return style;
  }

  /**
   * Creates a new character style.
   *
   * @param doc
   *          The document.
   * @param name
   *          The name of the character style.
   * @param parentStyleName
   *          The name of the parent paragraph style or null if there is no
   *          parent, which defaults to "Standard"
   * @return The style or null if it cloudn't be created.
   */
  public static XStyle createCharacterStyle(XTextDocument doc, String name,
      String parentStyleName)
  {
    XNameContainer pss = getStyleContainer(doc, CHARACTER_STYLES);
    if (pss != null)
    {
      return createStyle(doc, pss, "com.sun.star.style.CharacterStyle", name,
          parentStyleName);
    }
    return null;
  }

  /**
   * Create a new style.
   * 
   * @param doc
   *          The document.
   * @param styles
   *          The style container of the document.
   * @param styleType
   *          The of style.
   * @param name
   *          The name of the style.
   * @param parentStyleName
   *          The parent style.
   * @return The new style
   */
  private static XStyle createStyle(XTextDocument doc, XNameContainer styles,
      String styleType, String name, String parentStyleName)
  {
    try
    {
      XStyle style = UNO
          .XStyle(UNO.XMultiServiceFactory(doc).createInstance(styleType));
      styles.insertByName(name, style);
      if (style != null && parentStyleName != null)
      {
	style.setParentStyle(parentStyleName);
      }
      return UNO.XStyle(styles.getByName(name));
    } catch (Exception e)
    {
      LOGGER.trace("", e);
      return null;
    }
  }

  /**
   * Get the styles of the document.
   *
   * @param doc
   *          The document.
   * @param type
   *          The type of styles ({@link #CHARACTER_STYLES},
   *          {@link #PARAGRAPH_STYLES})
   * @return The containter of the styles or null.
   */
  private static XNameContainer getStyleContainer(XTextDocument doc,
      String containerName)
  {
    try
    {
      return UNO.XNameContainer(
          UNO.XNameAccess(UNO.XStyleFamiliesSupplier(doc).getStyleFamilies())
              .getByName(containerName));
    } catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
    }
    return null;
  }

}
