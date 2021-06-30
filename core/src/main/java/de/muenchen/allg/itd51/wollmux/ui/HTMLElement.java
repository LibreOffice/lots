/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.ui;

import java.awt.Color;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.StyleSheet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.FontDescriptor;

/**
 * Represents the model of an HTML Element.
 */
public class HTMLElement
{

  private static final Logger LOGGER = LoggerFactory.getLogger(HTMLElement.class);

  private Document doc;

  /**
   * Parses an HTML string. {@code <br>
   * } is replaced by {@code \n}.
   *
   * @param html
   *          HTML string.
   */
  public HTMLElement(String html)
  {
    doc = Jsoup.parse(html);
    Document.OutputSettings outputSettings = new Document.OutputSettings();
    outputSettings.prettyPrint(false);
    doc.outputSettings(outputSettings);
    doc.select("br").after("\n");
    doc.select("p").after("\n");
  }

  public FontDescriptor getFontDescriptor()
  {
    org.jsoup.nodes.Element element = doc.selectFirst("[style]");
    FontDescriptor fontDescriptor = new FontDescriptor();
    if (element != null)
    {
      String style = element.attr("style");
      StyleSheet styleSheet = new StyleSheet();
      AttributeSet attributes = styleSheet.getDeclaration(style);
      Object o = attributes.getAttribute(CSS.Attribute.FONT_SIZE);
      if (o != null)
      {
        try
        {
          fontDescriptor.Height = Short.valueOf(o.toString().replaceAll("pt", ""));
        } catch (NumberFormatException ex)
        {
          LOGGER.info("parsing font height failed due number format exception, trying to parse string %s", o.toString());
          short fontSize = parseFontSizeString(o.toString());
          LOGGER.info("parsed %s to %i pt", o.toString(), fontSize);
          fontDescriptor.Height = fontSize;
        }
      }
    }
    return fontDescriptor;
  }
  
  private short parseFontSizeString(String value)
  {
    switch (value)
    {
      case "x-small" : 
        return 7;
      case "small" :
        return 10;
      case "medium" : 
        return 12;
      case "large":
        return 14;
      case "x-large":
        return 18;
      case "xx-large":
        return 24;
      default:
        return 12;
    }
  }

  public int getRGBColor()
  {
    org.jsoup.nodes.Element element = doc.selectFirst("[color], [style]");
    if (element != null)
    {
      StyleSheet styleSheet = new StyleSheet();
      String colorAttr = "";
      if (element.hasAttr("color"))
      {
        colorAttr = element.attr("color");
      } else
      {
        String style = element.attr("style");
        AttributeSet attributes = styleSheet.getDeclaration(style);
        if (attributes.isDefined(CSS.Attribute.COLOR))
        {
          colorAttr = attributes.getAttribute(CSS.Attribute.COLOR).toString();
        }
      }
      Color color = styleSheet.stringToColor(colorAttr);
      return color.getRGB() & ~0xFF000000;
    }
    return 0;
  }

  public String getHref()
  {
    org.jsoup.nodes.Element link = doc.selectFirst("a[href]");
    if (link != null)
    {
      String href = link.attr("href");
      try
      {
        URI uri = new URI(href);
        if (!uri.isAbsolute())
        {
          uri = new URI("http://" + href);
        }
        return uri.toString();
      } catch (URISyntaxException e)
      {
        LOGGER.error("Invalide URL {}", href, e);
      }
    }
    return "";
  }

  public String getText()
  {
    return doc.body().wholeText().trim();
  }
}
