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
package de.muenchen.allg.itd51.wollmux.ui;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;
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

  private String html;
  private HTMLDocument htmlDocument;

  /**
   * Parses an HTML string with java swing's {@link HTMLDocument}. {@code <br>
   * } is replaced by {@code \n}.
   *
   * @param html
   *          HTML string.
   */
  public HTMLElement(String html)
  {
    this.html = html;
    try
    {
      htmlDocument = new HTMLDocument();
      htmlDocument.setParser(new ParserDelegator());
      html = "<html>" + html.replaceAll("<html>", "").replaceAll("</html>", "") + "</html>";
      htmlDocument.insertAfterStart(htmlDocument.getDefaultRootElement(), html);
      replaceBR(htmlDocument);
    } catch (BadLocationException | IOException e)
    {
      LOGGER.debug("", e);
    }
  }

  private void replaceBR(HTMLDocument doc) throws BadLocationException
  {
    HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.BR);
    while (iter != null && iter.isValid())
    {
      doc.replace(iter.getStartOffset(), iter.getEndOffset() - iter.getStartOffset(), "\n", null);
      iter.next();
    }
  }

  public FontDescriptor getFontDescriptor()
  {
    FontDescriptor fontDescriptor = new FontDescriptor();
    ElementIterator iterator = new ElementIterator(htmlDocument);
    Element elem = iterator.next();
    while (elem != null)
    {
      if (elem.getAttributes().isDefined(CSS.Attribute.FONT_SIZE))
      {
        String size = elem.getAttributes().getAttribute(CSS.Attribute.FONT_SIZE).toString();
        fontDescriptor.Height = Short.valueOf(size.replaceAll("pt", ""));
        break;
      }
      elem = iterator.next();
    }
    return fontDescriptor;
  }

  public int getRGBColor()
  {
    ElementIterator iterator = new ElementIterator(htmlDocument);
    Element elem = iterator.next();
    while (elem != null)
    {
      if (elem.getAttributes().isDefined(CSS.Attribute.COLOR))
      {
        String colorAttr = elem.getAttributes().getAttribute(CSS.Attribute.COLOR).toString();
        Color color = new StyleSheet().stringToColor(colorAttr);
        return color.getRGB() & ~0xFF000000;
      }
      elem = iterator.next();
    }
    return 0;
  }

  public String getHref()
  {
    HTMLDocument.Iterator iter = htmlDocument.getIterator(HTML.Tag.A);
    if (iter != null && iter.isValid() && iter.getAttributes().isDefined(HTML.Attribute.HREF))
    {
      String href = (String) iter.getAttributes().getAttribute(HTML.Attribute.HREF);
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
        return "";
      }
    }
    return "";
  }

  public String getText()
  {
    try
    {
      return htmlDocument.getText(0, htmlDocument.getLength()).strip();
    } catch (BadLocationException e)
    {
      return html;
    }
  }
}
