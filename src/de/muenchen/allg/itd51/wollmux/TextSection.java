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

import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.container.XNameAccess;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;

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
   * Das Pattern über das der Namenszusatz mit der Festlegung der
   * Sichtbarkeitsgruppen dieses Bereichs
   */
  private static final Pattern groupedSection = Pattern
      .compile("^.*(GROUPS.*)$");

  /**
   * Der Bereich selbst
   */
  private XTextSection section;

  /**
   * Ein Set das die Gruppennamen dieses Sichtbarkeitselements enthält.
   */
  private Set groups;

  /**
   * Diese Methode erzeugt ein Set mit Elementen vom Typ TextSection, das alle
   * TextSections enthält, die mit einem Namenszusatz 'GROUPS ...' versehen sind
   * und sich im Dokument doc befinden.
   * 
   * @param doc
   *          Das Textdokument, in dem die Bereiche gesucht werden.
   * @return Eine Menge aller TextSections mit Namenszusatz 'GROUPS ...', die in
   *         diesem Dokument gefunden wurden.
   */
  public static Set createVisibleSections(XTextSectionsSupplier doc)
  {
    Set visibleSections = new HashSet();

    if (doc == null) return visibleSections;
    XNameAccess sectionsAccess = UNO.XNameAccess(doc.getTextSections());
    if (sectionsAccess == null) return visibleSections;

    // Alle Elementnamen herausfiltern, die GROUPS enthalten und damit als
    // VisibleSection erzeugt werden sollen.
    String[] names = sectionsAccess.getElementNames();
    for (int i = 0; i < names.length; i++)
    {
      String name = names[i];
      Matcher m = groupedSection.matcher(name);
      if (m.matches())
      {

        // HashSet mit allen Gruppen GROUPS aufbauen:
        Set groups = new HashSet();
        String groupsStr = m.group(1);
        try
        {
          ConfigThingy groupsCfg = new ConfigThingy("", null, new StringReader(
              groupsStr));
          Iterator giter = groupsCfg.get("GROUPS").iterator();
          while (giter.hasNext())
            groups.add(giter.next().toString());
        }
        catch (java.lang.Exception e)
        {
          Logger.error("Der Textbereich mit dem Namen '"
                       + name
                       + "' enthält ein fehlerhaftes GROUPS-Attribut.", e);
        }

        try
        {
          XTextSection section = UNO.XTextSection(sectionsAccess
              .getByName(name));
          if (section != null)
          {
            visibleSections.add(new TextSection(section, groups));
          }
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    return visibleSections;
  }

  /**
   * Erzeugt ein neues TextSection-Objekt.
   * 
   * @param section
   *          UNO-Objekt des Bereichs
   * @param groups
   *         * 
  Set mit den Namen (als String) aller Sichtbarkeitsgruppen, die
   *          diesen Bereich sichtbar oder unsichtbar machen können.
   */
  private TextSection(XTextSection section, Set groups)
  {
    this.section = section;
    this.groups = groups;
  }

  /* (non-Javadoc)
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

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#setVisible(boolean)
   */
  public void setVisible(boolean visible)
  {
    UNO.setProperty(section, "IsVisible", new Boolean(visible));
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#getGroups()
   */
  public Set getGroups()
  {
    return groups;
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.VisibilityElement#getAnchor()
   */
  public XTextRange getAnchor()
  {
    return section.getAnchor();
  }

}
