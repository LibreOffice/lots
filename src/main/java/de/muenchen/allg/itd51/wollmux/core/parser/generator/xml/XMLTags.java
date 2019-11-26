package de.muenchen.allg.itd51.wollmux.core.parser.generator.xml;

/**
 * Enum for the XML-elements in the generated XML by the {@link XMLGenerator}.
 * 
 * @author daniel.sikeler
 * 
 */
public enum XMLTags
{
  /**
   * XML-element key.
   */
  KEY("key"),
  /**
   * XML-element value.
   */
  VALUE("value"),
  /**
   * XML-element group.
   */
  GROUP("group"),
  /**
   * XML-element comment.
   */
  COMMENT("comment"),
  /**
   * XML-element fileReference.
   */
  FILEREFERENCE("fileReference"),
  /**
   * XML-element file.
   */
  FILE("file"),
  /**
   * XML-element config.
   */
  CONFIG("config");

  /**
   * The XML-tag name.
   */
  private final String name;

  /**
   * Create a new XMLTag.
   * 
   * @param name
   *          The name of the tag.
   */
  private XMLTags(final String name)
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }

}
