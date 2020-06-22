package de.muenchen.allg.itd51.wollmux.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.FontDescriptor;

import javafx.css.CssParser;
import javafx.css.Declaration;
import javafx.css.ParsedValue;
import javafx.css.Rule;
import javafx.css.Stylesheet;
import javafx.scene.Group;
import javafx.scene.Node;

/**
 * Parses HTML-Tags / CSS in line style.
 */
public class HTMLParserCallback extends HTMLEditorKit.ParserCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger(HTMLParserCallback.class);

  private CssParser cssParser = new CssParser();
  private HTMLElement htmlElement;
  private List<HTMLElement> htmlElements = new ArrayList<>();

  public List<HTMLElement> getHtmlElement()
  {
    return this.htmlElements;
  }

  @Override
  public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos)
  {
    htmlElement = new HTMLElement();

    if (t.equals(HTML.Tag.FONT))
    {
      String colorAttr = (String) a.getAttribute(HTML.Attribute.COLOR);
      htmlElement.setTagName(t.toString());

      if (colorAttr != null)
      {
        java.awt.Color color = null;

        if (colorAttr.contains("#"))
        {
          color = java.awt.Color.decode(colorAttr);
        } else
        {
          color = getRGBByColorName(colorAttr);
        }

        htmlElement.setRGBColor(color.getRGB() & ~0xFF000000);
      }

      String fontSize = (String) a.getAttribute(CSS.Attribute.FONT_SIZE);

      if (fontSize != null && !fontSize.isEmpty())
      {
        FontDescriptor desc = new FontDescriptor();
        desc.Height = Short.valueOf(fontSize);
        htmlElement.setFontDescriptor(desc);
      }

    } else if (t.equals(HTML.Tag.A))
    {
      htmlElement.setTagName(t.toString());
      htmlElement.setHref((String) a.getAttribute(HTML.Attribute.HREF));
    } else if (t.equals(HTML.Tag.SPAN))
    {
      htmlElement.setTagName(t.toString());
    }

    String val = (String) a.getAttribute(HTML.Attribute.STYLE);
    parseCss(val);

    super.handleStartTag(t, a, pos);
  }

  private void parseCss(String style)
  {
    if (style == null || style.isEmpty())
    {
      return;
    }

    Node node = new Group();
    node.setStyle(style);
    Stylesheet sheet2 = cssParser.parseInlineStyle(node);

    for (Rule rule : sheet2.getRules())
    {
      for (Declaration dec : rule.getDeclarations())
      {
        String property = dec.getProperty();

        if (property.equalsIgnoreCase("font-size"))
        {
          ParsedValue parsedVal = (ParsedValue) dec.getParsedValue().getValue();
          String realVal = parsedVal.getValue().toString();

          FontDescriptor desc = new FontDescriptor();
          // HTMLEditorKit transforms "18pt" to "18.0pt"
          realVal = realVal.replace("pt", "").trim();

          double asDouble = 0;

          try
          {
            asDouble = Double.parseDouble(realVal);
          } catch (NumberFormatException e)
          {
            LOGGER.trace("", e);
          }

          desc.Height = (short) asDouble;
          htmlElement.setFontDescriptor(desc);
        } else if (property.equalsIgnoreCase("color"))
        {
          javafx.scene.paint.Color parsedColor = (javafx.scene.paint.Color) dec.getParsedValue().getValue();
          java.awt.Color awtColor = convertColorType(parsedColor);
          htmlElement.setRGBColor(awtColor.getRGB());
        }
      }
    }
  }

  @Override
  public void handleText(char[] data, int pos)
  {
    if (htmlElement == null)
    {
      htmlElement = new HTMLElement();
    }

    htmlElement.setText(String.valueOf(data));
    htmlElements.add(htmlElement);
  }

  /**
   * Get RGB Color values by color's string representation. Color name must be css valid.
   *
   * @param color
   *          Name of the color.
   * @return @{link Color}
   */
  private java.awt.Color getRGBByColorName(String color)
  {
    StyleSheet s = new StyleSheet();
    java.awt.Color result = s.stringToColor(color);

    if (result == null)
    {
      return new java.awt.Color(0, 0, 0);
    }

    return result;
  }

  private java.awt.Color convertColorType(javafx.scene.paint.Color color)
  {
    int r = (int) Math.min(color.getRed() * 256, 255);
    int g = (int) Math.min(color.getGreen() * 256, 255);
    int b = (int) Math.min(color.getBlue() * 256, 255);

    return new java.awt.Color(r, g, b);
  }

}
