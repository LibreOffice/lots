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
