/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package de.muenchen.allg.itd51.wollmux.document;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.document.nodes.Container;
import de.muenchen.allg.itd51.wollmux.document.nodes.FormControl;


/**
 * Abstrakte Basis-Klasse für das Besuchen aller Knoten eines DocumentTrees. Dazu
 * wird ein Objekt erzeugt, das von Visitor abgeleitet ist und dann auf diesem
 * Objekt obj die Methode obj.visit(tree) aufgerufen, wobei tree ein DocumentTree
 * ist. Für jeden Node des Baums wird dann die entsprechende Methode von Visitor
 * aufgerufen (teilweise mehrfach). Die Methoden können alle false zurückliefern um
 * zu signalisieren, dass das Durchlaufen des Baumes abgebrochen werden soll. Die
 * von Standard-Methoden liefern alle true.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface DocumentTreeVisitor
{
  public default void visit(XTextDocument doc)
  {
    DocumentTree tree = new DocumentTree(doc);
    tree.getRoot().visit(this);
  }

  /**
   * Wird für Knoten aufgerufen, die eine Einfügestelle (insertValue,
   * insertFormValue) repräsentieren.
   *
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   */
  public default boolean insertionBookmark(InsertionBookmark bookmark)
  {
    return true;
  }

  /**
   * Wird für Knoten aufgerufen, die Formularsteuerelement repräsentieren.
   *
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   */
  public default boolean formControl(FormControl control)
  {
    return true;
  }

  /**
   * Wird für Knoten aufgerufen, die im inneren des Baumes liegen, d,h, Kindknoten
   * haben können. Dies sind zum Beispiel Absätze.
   *
   * @param count
   *          Der Knoten wird einmal mit count == 0 besucht bevor der erste
   *          Nachfahre besucht wird und einmal nach dem Besuchen aller Nachfahren
   *          mit count == 1.
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   */
  public default boolean container(Container container, int count)
  {
    return true;
  }

  /**
   * Wird für Knoten aufgerufen, die Textabschnitte repräsentieren.
   *
   * @return false, wenn keine weiteren Knoten besucht werden sollen.
   */
  public default boolean textRange(TextRange textRange)
  {
    return true;
  }
}
