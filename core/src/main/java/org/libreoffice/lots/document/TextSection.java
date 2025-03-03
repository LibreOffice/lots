/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.document;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.XNamed;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.util.Utils;

/**
 * Diese Klasse repräsentiert einen Textbereich (TextSection), dessen Namen um den
 * Zusatz {@code GROUPS <ListeMitSichtbarkeitsgruppen>} ergänzt wurde, über den die
 * Sichtbarkeitsgruppen festgelegt sind, die diesen Bereich sichtbar oder unsichtbar
 * schalten können.
 *
 * @author christoph.lutz
 */
public class TextSection implements VisibilityElement
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(TextSection.class);

  /**
   * Der Bereich selbst
   */
  private XTextSection section;

  /**
   * Ein Set das die Gruppennamen dieses Sichtbarkeitselements enthält.
   */
  private Set<String> groups;

  /**
   * Enthält den Namen der TextSection (zum Erzeugungszeitpunkt). Ändert sich dieser,
   * wird die TextSection invalide.
   */
  private String initialName;

  /**
   * Erzeugt ein neues TextSection-Objekt.
   *
   * @param section
   *          UNO-Objekt des Bereichs
   * @param groups
   *          Set mit den Namen (als String) aller Sichtbarkeitsgruppen, die diesen
   *          Bereich sichtbar oder unsichtbar machen können.
   */
  public TextSection(XTextSection section, Set<String> groups)
  {
    this.section = section;
    this.groups = groups;
    this.initialName = null;
    XNamed xNamed = UNO.XNamed(section);
    if (xNamed != null) {
      initialName = xNamed.getName();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.VisibilityElement#isVisible()
   */
  @Override
  public boolean isVisible()
  {
    try
    {
      return AnyConverter.toBoolean(UnoProperty.getProperty(section, UnoProperty.IS_VISIBLE));
    }
    catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
      return false;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.VisibilityElement#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible)
  {
    try
    {
      Utils.setProperty(section, "IsVisible", Boolean.valueOf(visible));
      UNO.hideTextRange(section.getAnchor(), !visible);
    }
    catch (UnoHelperException e)
    {
      LOGGER.error("Sichtbarkeit konnte nicht geändert werden.", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.VisibilityElement#getGroups()
   */
  @Override
  public Set<String> getGroups()
  {
    return groups;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.VisibilityElement#addGroups(java.util.Set)
   */
  @Override
  public void addGroups(Set<String> groups)
  {
    this.groups.addAll(groups);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.VisibilityElement#getAnchor()
   */
  @Override
  public XTextRange getAnchor()
  {
    try
    {
      return section.getAnchor();
    }
    catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
      return null;
    }
  }

  /**
   * Liefert den Namen der TextSection oder null, wenn die TextSection
   * zwischenzeitig invalide wurde.
   *
   * TextSection ist dann invalide, wenn sie nicht XNamed implementiert, sich
   * ihr Name seit dem Erzeugungszeitpunkt ändert oder sie nicht mehr im
   * Dokument existiert.
   *
   * @return Den Namen oder null.
   */
  public String getName()
  {
    XNamed xNamed = UNO.XNamed(section);
    if (xNamed == null) {
      return null;
    }
    String newName = xNamed.getName();
    if (initialName != null && initialName.equals(newName)) {
      return initialName;
    }
    return null;
  }

  /**
   * Liefert den HashCode von getName() und ermöglicht das Verwenden der TextSection
   * in einem HashSet.
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    return (initialName != null) ? initialName.hashCode() : 0;
  }

  /**
   * Vergleicht die beiden TextSections über UNORuntime.areSame()
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj)
  {
    if (obj == null)
    {
      return false;
    }

    if (this.getClass() != obj.getClass())
    {
      return false;
    }
    try
    {
      return UnoRuntime.areSame(((TextSection) obj).section, this.section);
    }
    catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
      return false;
    }
  }

  /**
   * Liefert true, wenn die TextSection (bzw. Ihr Anchor) nicht mehr valide ist.
   *
   * Dies ist der Fall, wenn seit der Erzeugung ihr Name (im Dokument) geändert wurde
   * oder sie nicht mehr existiert.
   */
  public boolean isInvalid()
  {
    return getAnchor() == null || getName() == null;
  }

  @Override
  public String toString()
  {
    return this.getClass().getName() + "('" + initialName + "')";
  }
}
