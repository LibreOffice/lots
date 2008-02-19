//TODO L.m()
/*
 * Dateiname: TextSection.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert einen Textbereich (TextSection) mit einem
 *            Namensanhang in der Form 'GROUPS <ListeMitSichtbarkeitsgruppen>'.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 02.01.2007 | LUT | Erstellung als TextSection
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.Set;

import com.sun.star.container.XNamed;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;

/**
 * Diese Klasse repräsentiert einen Textbereich (TextSection), dessen Namen um
 * den Zusatz 'GROUPS <ListeMitSichtbarkeitsgruppen>' ergänzt wurde, über den
 * die Sichtbarkeitsgruppen festgelegt sind, die diesen Bereich sichtbar oder
 * unsichtbar schalten können.
 * 
 * @author christoph.lutz
 */
public class TextSection implements VisibilityElement
{
  /**
   * Der Bereich selbst
   */
  private XTextSection section;

  /**
   * Ein Set das die Gruppennamen dieses Sichtbarkeitselements enthält.
   */
  private Set groups;

  /**
   * Erzeugt ein neues TextSection-Objekt.
   * 
   * @param section
   *          UNO-Objekt des Bereichs
   * @param groups
   *          Set mit den Namen (als String) aller Sichtbarkeitsgruppen, die
   *          diesen Bereich sichtbar oder unsichtbar machen können.
   */
  public TextSection(XTextSection section, Set groups)
  {
    this.section = section;
    this.groups = groups;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#isVisible()
   */
  public boolean isVisible()
  {
    try
    {
      return AnyConverter.toBoolean(UNO.getProperty(section, "IsVisible"));
    }
    catch (java.lang.Exception e)
    {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#setVisible(boolean)
   */
  public void setVisible(boolean visible)
  {
    UNO.setProperty(section, "IsVisible", new Boolean(visible));
    UNO.setProperty(section.getAnchor(), "CharHidden", new Boolean(!visible));
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#getGroups()
   */
  public Set getGroups()
  {
    return groups;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#addGroups(java.util.Set)
   */
  public void addGroups(Set groups)
  {
    this.groups.addAll(groups);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#getAnchor()
   */
  public XTextRange getAnchor()
  {
    try
    {
      return section.getAnchor();
    }
    catch (java.lang.Exception e)
    {
      return null;
    }
  }

  /**
   * Liefert den Namen der TextSection, der auch dann noch aktuell bleibt, wenn
   * der Name manuell im Dokument geändert wurde, oder "<disposedTextSection>",
   * wenn die TextSection nicht mehr existiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public String getName()
  {
    XNamed xNamed = UNO.XNamed(section);
    return (xNamed != null) ? xNamed.getName() : "<disposedTextSection>";
  }

  /**
   * Liefert den HashCode von getName() und ermöglicht das Verwenden der
   * TextSection in einem HashSet.
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode()
  {
    return getName().hashCode();
  }

  /**
   * Vergleicht die beiden TextSections über UNORuntime.areSame()
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj)
  {
    try
    {
      return UnoRuntime.areSame(((TextSection) obj).section, this.section);
    }
    catch (java.lang.Exception e)
    {
      return false;
    }
  }

  /**
   * Liefert true, wenn die TextSection (bzw. Ihr Anchor) nicht mehr existiert.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean isRetired()
  {
    return getAnchor() == null;
  }
}
