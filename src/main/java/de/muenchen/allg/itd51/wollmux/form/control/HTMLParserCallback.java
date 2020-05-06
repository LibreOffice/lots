package de.muenchen.allg.itd51.wollmux.form.control;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.sun.star.awt.FontDescriptor;

import javafx.css.CssParser;
import javafx.css.Declaration;
import javafx.css.ParsedValue;
import javafx.css.Rule;
import javafx.css.Stylesheet;
import javafx.scene.Group;
import javafx.scene.Node;

public class HTMLParserCallback extends HTMLEditorKit.ParserCallback
{
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
    if (t.equals(HTML.Tag.FONT))
    {
      String colorAttr = (String) a.getAttribute(HTML.Attribute.COLOR);
      java.awt.Color color = null;

      if (colorAttr.contains("#"))
      {
        color = java.awt.Color.decode(colorAttr);
      } else
      {
        color = getRGBByColorName(colorAttr);
      }

      htmlElement = new HTMLElement();
      htmlElement.setTagName(t.toString());
      htmlElement.setRGBColor(color.getRGB());

      String fontSize = (String) a.getAttribute(javax.swing.text.html.CSS.Attribute.FONT_SIZE);

      if (fontSize != null && !fontSize.isEmpty())
      {
        FontDescriptor desc = new FontDescriptor();
        desc.Height = Short.valueOf(fontSize);
        htmlElement.setFontDescriptor(desc);
      }

      // registerTag(t, new BlockAction());
    } else if (t.equals(HTML.Tag.A))
    {
      htmlElement = new HTMLElement();
      htmlElement.setTagName(t.toString());
      htmlElement.setHref((String) a.getAttribute(HTML.Attribute.HREF));
    } else if (t.equals(HTML.Tag.SPAN))
    {
      Object val = a.getAttribute(HTML.Attribute.STYLE);

      if (val != null)
      {
        String style = val.toString();
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

              htmlElement = new HTMLElement();
              FontDescriptor desc = new FontDescriptor();
              // HTMLEditorKit transforms "18pt" to "18.0pt"
              realVal = realVal.replace("pt", "").trim();
              double asDouble = Double.valueOf(realVal);
              desc.Height = (short) asDouble;
              htmlElement.setFontDescriptor(desc);
            } else if (property.equalsIgnoreCase("color"))
            {
              javafx.scene.paint.Color parsedColor = (javafx.scene.paint.Color) dec.getParsedValue().getValue();

              java.awt.Color awtColor = convertColorType(parsedColor);
              htmlElement = new HTMLElement();
              htmlElement.setRGBColor(awtColor.getRGB());

            }
          }
        }
      }
    }
    super.handleStartTag(t, a, pos);
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
