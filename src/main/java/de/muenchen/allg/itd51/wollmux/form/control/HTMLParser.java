package de.muenchen.allg.itd51.wollmux.form.control;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * More error tolerant HTML parser for &lt;font&gt; and &lt;a&gt; tags.
 */
public class HTMLParser extends HTMLDocument
{
  private static final long serialVersionUID = -287169644574884663L;

  private List<HTMLElement> htmlElements = new ArrayList<>();

  public List<HTMLElement> getHtmlElement()
  {
    return this.htmlElements;
  }

  @Override
  public HTMLEditorKit.ParserCallback getReader(int pos)
  {
    Object desc = getProperty(Document.StreamDescriptionProperty);
    if (desc instanceof URL)
    {
      setBase((URL) desc);
    }
    return new MyHTMLReader(pos);
  }

  /**
   * More error tolerant HTML reader for &lt;font&gt; and &lt;a&gt; tags.
   */
  public class MyHTMLReader extends HTMLDocument.HTMLReader
  {
    private HTMLElement htmlElement;

    /**
     * New tolerant HTML reader.
     *
     * @param offset
     *          See {@link HTMLDocument.HTMLReader#HTMLReader(int)}.
     */
    public MyHTMLReader(int offset)
    {
      super(offset);
    }

    @Override
    public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos)
    {
      if (t.equals(HTML.Tag.FONT))
      {
        htmlElement = new HTMLElement();
        htmlElement.setTagName(t.toString());
        htmlElement.setColor((String) a.getAttribute(HTML.Attribute.COLOR));
        htmlElement.setSize((String) a.getAttribute(javax.swing.text.html.CSS.Attribute.FONT_SIZE));
        registerTag(t, new BlockAction());
      } else if (t.equals(HTML.Tag.A))
      {
        htmlElement = new HTMLElement();
        htmlElement.setTagName(t.toString());
        htmlElement.setHref((String) a.getAttribute(HTML.Attribute.HREF));
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

  }
}
