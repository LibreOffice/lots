/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.document.nodes;

import java.util.Collections;
import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.document.DocumentTreeVisitor;

/**
 * Oberklasse für die Knoten des Dokumentbaums.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Node extends Iterable<Node>
{
  /**
   * Liefert einen Iterator über alle Kindknoten.
   */
  @Override
  public default Iterator<Node> iterator()
  {
    return Collections.emptyIterator();
  }

  /**
   * Besucht den Knoten und falls es ein Container ist den ganzen Teilbaum mit
   * diesem Knoten als Wurzel. Es werden die entsprechenden Methoden des
   * {@link DocumentTreeVisitor}s visit aufgerufen.
   *
   * @return false falls die entsprechende Methode von visit zurückliefert, dass
   *         keine weiteren Knoten mehr besucht werden sollen.
   */
  public default boolean visit(DocumentTreeVisitor visit)
  {
    return true;
  }
}
