package de.muenchen.allg.itd51.wollmux.form.control;

import java.io.Serializable;

/**
 * Represents the model of an Html Element.
 *
 */
public class HTMLElement implements Serializable
{
  private static final long serialVersionUID = -4140645761673580822L;

  private String tagName = "";
  private String text = "";
  private String color = "";
  private String size;
  private String href = "";

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

  public void setColor(String color)
  {
    this.color = color;
  }

  public void setSize(String size)
  {
    this.size = size;
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

  public String getColor()
  {
    return this.color;
  }

  public String getSize()
  {
    return this.size;
  }

}
