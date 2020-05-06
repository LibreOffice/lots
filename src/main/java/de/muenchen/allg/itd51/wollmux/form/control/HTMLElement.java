package de.muenchen.allg.itd51.wollmux.form.control;

import com.sun.star.awt.FontDescriptor;

/**
 * Represents the model of an Html Element.
 *
 */
public class HTMLElement
{
  private String tagName = "";
  private String text = "";
  private String href = "";
  private int rgbColor;
  private FontDescriptor fontDescriptor;

  public FontDescriptor getFontDescriptor()
  {
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

}
