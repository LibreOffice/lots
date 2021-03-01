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
package de.muenchen.allg.itd51.wollmux.document;

import com.sun.star.text.XTextRange;

/**
 * Diese Klasse repräsentiert die Beziehung zweier Textbereiche die abhängig
 * von ihrer Verschachtelung (Überlappung) in einem Baum angeordnet werden
 * sollen. Dabei legt die TreeRelation fest, ob die beiden Textbereiche
 * identisch, benachbart, Eltern bzw. Kinder des jeweils anderen oder gar
 * nicht vergleichbar sind.
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class TreeRelation
{
  /**
   * Enthält die TextRangeRelation zu den Bereichen a und b.
   */
  private TextRangeRelation rel;

  /**
   * Erzeugt eine neue Relation für eine baumartige Anordnung der Textbereiche
   * a und b.
   */
  public TreeRelation(XTextRange a, XTextRange b)
  {
    this.rel = new TextRangeRelation(a, b);
  }

  /**
   * Erzeugt eine neue Relation für eine baumartige Anordnung der Textbereiche
   * a und b aus der TextRangeRelation rel.
   */
  public TreeRelation(TextRangeRelation rel)
  {
    this.rel = rel;
  }

  /**
   * Der Textbereich b ist Teil des Textbereichs a und damit in Kind von a.
   */
  public boolean isBChildOfA()
  {
    return rel.followsOrderscheme88AA() || rel.followsOrderschemeA88A()
           || rel.followsOrderschemeAA88();
  }

  /**
   * Der Textbereich a ist Teil des Textbereichs b und damit ein Kind von b.
   */
  public boolean isAChildOfB()
  {
    return rel.followsOrderscheme88BB() || rel.followsOrderschemeB88B()
           || rel.followsOrderschemeBB88();
  }

  /**
   * Die beiden Textbereiche sind benachbart, wobei a hinter b liegt.
   */
  public boolean isASiblingAfterB()
  {
    return rel.followsOrderschemeBBAA();
  }

  /**
   * Die beiden Textbereiche sind benachbart, wobei a vor b liegt.
   */
  public boolean isASiblingBeforeB()
  {
    return rel.followsOrderschemeAABB();
  }

  /**
   * Die beiden Textbereiche liegen exakt übereinander.
   */
  public boolean isAEqualB()
  {
    return rel.followsOrderscheme8888();
  }

  /**
   * Die beiden Textbereiche vergleichbar und liegen im selben Text-Objekt.
   */
  public boolean rangesAreComparable()
  {
    return rel.rangesAreComparable();
  }

  /**
   * Liefert true, wenn der Textbereich A logisch vor dem Textbereich B
   * startet - Diese Bedingung ist genau dann erfüllt, wenn A als Nachbar vor
   * B liegt oder A den Textbereich B voll umschließt (also B ein Kind von A
   * ist) und A somit z.B. bei sortierten Listen vor B einsortiert gehört.
   */
  public boolean isALessThanB()
  {
    return isASiblingBeforeB() || isBChildOfA();
  }

  /**
   * Liefert true, wenn der Textbereich A logisch nach dem Textbereich B
   * startet - Diese Bedingung ist genau dann erfüllt, wenn A als Nachbar nach
   * B liegt oder B den Textbereich A voll umschließt (also A ein Kind von B
   * ist) und B somit z.B. bei sortierten Listen vor A einsortiert gehört.
   */
  public boolean isAGreaterThanB()
  {
    return isASiblingAfterB() || isAChildOfB();
  }
}
