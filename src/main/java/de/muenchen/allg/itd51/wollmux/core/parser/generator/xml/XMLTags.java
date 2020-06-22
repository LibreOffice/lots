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
