package de.muenchen.allg.itd51.wollmux.core.document;

import java.util.Set;

import com.sun.star.text.XTextRange;

/**
 * Dieses Interface beschreibt ein Sichtbarkeitselement, das gesteuert über sog.
 * Sichtbarkeitsgruppen sichtbar oder unsichtbar geschalten werden kann.
 * 
 * Derzeit wird das Interface von folgenden Klassen implementiert:
 * DocumentCommand und TextSection
 * 
 * @author christoph.lutz
 */
public interface VisibilityElement
{

  /**
   * gibt den Sichtbarkeitsstatus des Sichtbarkeitselements zurück.
   * 
   * @return true=sichtbar, false=ausgeblendet
   */
  public abstract boolean isVisible();

  /**
   * Setzt den Sichtbarkeitsstatus des Elements.
   * 
   * @param visible
   *          true=sichtbar, false=ausgeblendet
   */
  public abstract void setVisible(boolean visible);

  /**
   * Liefert alle Sichtbarkeitsgruppen zu diesem Sichtbarkeitselement.
   * 
   * @return Ein Set, das alle zugeordneten groupId's als Strings enthält.
   */
  public abstract Set<String> getGroups();

  /**
   * fügt diesem Elements all in groups definierten Sichtbarkeitsgruppen hinzu.
   */
  public abstract void addGroups(Set<String> groups);

  /**
   * Liefert die TextRange an der das VisibleElement verankert ist oder null,
   * falls das VisibleElement nicht mehr existiert.
   */
  public abstract XTextRange getAnchor();

}
