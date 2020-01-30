package de.muenchen.allg.itd51.wollmux.form.control;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.FontDescriptor;

/**
 * Represents the model of an HTML Element.
 */
public class HTMLElement
{

  private static final Logger LOGGER = LoggerFactory.getLogger(HTMLElement.class);

  private String tagName = "";
  private String text = "";
  private String href = "";
  private int rgbColor;
  private FontDescriptor fontDescriptor;

  public FontDescriptor getFontDescriptor()
  {
    if (this.fontDescriptor == null)
      return new FontDescriptor();

    return fontDescriptor;
  }

  public void setFontDescriptor(FontDescriptor fontDescriptor)
  {
    this.fontDescriptor = fontDescriptor;
  }

  public int getRGBColor()
  {
    return this.rgbColor;
  }

  public void setRGBColor(int color)
  {
    this.rgbColor = color;
  }

  public void setHref(String href)
  {
    this.href = href;
  }

  public void setTagName(String tagName)
  {
    this.tagName = tagName;
  }

  public void setText(String text)
  {
    this.text = text;
  }

  public String getHref()
  {
    return this.href;
  }

  public String getTagName()
  {
    return this.tagName;
  }

  public String getText()
  {
    return this.text;
  }

  /**
   * Parses an html string with java swing's {@link HTMLEditorKit} to {@link HTMLElement}-Model. If
   * parsing fails, HTMLElement.getText() returns the plain label as a fallback.
   *
   * @param html
   *          HTML string.
   * @return List of HTML-Elements.
   */
  public static HTMLElement parseHtml(String html)
  {
    HTMLElement htmlElement = new HTMLElement();

    if (!html.contains("<html>"))
    {
      htmlElement.setText(html);
      return htmlElement;
    }

    Reader stringReader = new StringReader(html);
    HTMLEditorKit.Parser parser = new ParserDelegator();
    HTMLParserCallback callback = new HTMLParserCallback();

    try
    {
      parser.parse(stringReader, callback, true);
    } catch (IOException e)
    {
      LOGGER.trace("", e);
    } finally
    {
      try
      {
        stringReader.close();
      } catch (IOException e)
      {
        LOGGER.trace("", e);
      }
    }

    List<HTMLElement> htmlElements = callback.getHtmlElement();

    if (!htmlElements.isEmpty())
    {
      htmlElement = htmlElements.get(0);
    } else
    {
      String result = cleanHtmlTag(html);
      htmlElement.setText(result);
    }

    return htmlElement;
  }

  /**
   * Fallback for {@code "<html><br>
   * <br>
   * <br>
   * <br>
   * </html>"}. HTMLParserCallback.handleStartTag() is not able to parse things like that. fails on
   * recognizing {@code <br>
   * } start tag if no further HTML-Tags like {@code <font>} are embedded.
   * 
   * @param html
   *          String with HTML-Tag.
   * @return new String without HTML-Tag.
   */
  public static String cleanHtmlTag(String html)
  {
    return html.replace("<html>", "").replace("</html>", "");
  }

  /**
   * Converts <br>
   * to \r\n for LO's text field
   *
   * @param html
   *          HTML string.
   * @return cleaned html.
   */
  public static String convertLineBreaks(String html)
  {
    return html.replace("<br>", "\n");
  }

}
