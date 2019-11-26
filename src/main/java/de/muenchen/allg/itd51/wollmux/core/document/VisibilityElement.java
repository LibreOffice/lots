/*
 * Dateiname: VisibilityElement.java
 * Projekt  : WollMux
 * Funktion : Beschreibt ein Sichtbarkeitselement, das gesteuert über Sichtbarkeitsgruppen
 *            auf "sichtbar" oder "unsichtbar" geschalten werden kann.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 02.01.2007 | LUT | Erstellung als VisibilityElement
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
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
