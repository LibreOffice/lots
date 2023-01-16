/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;

import de.muenchen.allg.afid.UNO;

/**
 * Es gibt viele Möglichkeiten wie zwei Textbereiche (XTextRange-Objekte)
 * zueinander angeordnet sein können; Diese Klasse übersetzt die möglichen
 * Beziehungen in eine verständliche Form und repräsentiert damit die Beziehung
 * zwischen zwei Textbereichen.
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class TextRangeRelation
{
  /**
   * Ein interner integer-Wert der das Ergebnis von compareTextRanges() enthält.
   */
  private int rel;

  /**
   * Erzeugt ein neues Objekt das die Beziehung der Textbereiche a und b
   * darstellt.
   */
  public TextRangeRelation(XTextRange a, XTextRange b)
  {
    this.rel = compareTextRanges(a, b);
  }

  /**
   * Der Textbereich b tritt im Dokument vor dem Textbereich a auf. Das
   * Orderscheme beschreibt dabei die Position anschaulich und ist wie folgt
   * definiert: A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die
   * Textbereiche a und b überlappen sich an dieser Stelle, B:=der Textbereich b
   * steht an dieser Stelle alleine.
   */
  public boolean followsOrderschemeBBAA()
  {
    return rel == -4;
  }

  /**
   * Der Textbereich b startet vor dem Textbereich a, aber hört gleichzeitig mit
   * A auf. Das Orderscheme beschreibt dabei die Position anschaulich und ist
   * wie folgt definiert: A:=Der Textbereich a steht an dieser Stelle alleine,
   * 8:=Die Textbereiche a und b überlappen sich an dieser Stelle, B:=der
   * Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderschemeBB88()
  {
    return rel == -3;
  }

  /**
   * Der Textbereich b enthält den Textbereich a vollständig. Das Orderscheme
   * beschreibt dabei die Position anschaulich und ist wie folgt definiert:
   * A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die Textbereiche a
   * und b überlappen sich an dieser Stelle, B:=der Textbereich b steht an
   * dieser Stelle alleine.
   */
  public boolean followsOrderschemeB88B()
  {
    return rel == -2;
  }

  /**
   * Der Textbereich b startet mit dem Textbereich a, hört jedoch vor dem
   * Textbereich a auf. Das Orderscheme beschreibt dabei die Position
   * anschaulich und ist wie folgt definiert: A:=Der Textbereich a steht an
   * dieser Stelle alleine, 8:=Die Textbereiche a und b überlappen sich an
   * dieser Stelle, B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderscheme88AA()
  {
    return rel == -1;
  }

  /**
   * A und B liegen an der selben Position. Das Orderscheme beschreibt dabei die
   * Position anschaulich und ist wie folgt definiert: A:=Der Textbereich a
   * steht an dieser Stelle alleine, 8:=Die Textbereiche a und b überlappen sich
   * an dieser Stelle, B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderscheme8888()
  {
    return rel == -0;
  }

  /**
   * Der Textbereich a startet mit dem Textbereich b, hört jedoch vor dem
   * Textbereich b auf Das Orderscheme beschreibt dabei die Position anschaulich
   * und ist wie folgt definiert: A:=Der Textbereich a steht an dieser Stelle
   * alleine, 8:=Die Textbereiche a und b überlappen sich an dieser Stelle,
   * B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderscheme88BB()
  {
    return rel == 1;
  }

  /**
   * Der Textbereich a enthält den Textbereich b vollständig. Das Orderscheme
   * beschreibt dabei die Position anschaulich und ist wie folgt definiert:
   * A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die Textbereiche a
   * und b überlappen sich an dieser Stelle, B:=der Textbereich b steht an
   * dieser Stelle alleine.
   */
  public boolean followsOrderschemeA88A()
  {
    return rel == 2;
  }

  /**
   * Der Textbereich a startet vor dem Textbereich b, hört jedoch gemeinsam mit
   * dem Textbereich b auf. Das Orderscheme beschreibt dabei die Position
   * anschaulich und ist wie folgt definiert: A:=Der Textbereich a steht an
   * dieser Stelle alleine, 8:=Die Textbereiche a und b überlappen sich an
   * dieser Stelle, B:=der Textbereich b steht an dieser Stelle alleine.
   */
  public boolean followsOrderschemeAA88()
  {
    return rel == 3;
  }

  /**
   * Der Textbereich a liegt im Dokument vor dem Textbereich b. Das Orderscheme
   * beschreibt dabei die Position anschaulich und ist wie folgt definiert:
   * A:=Der Textbereich a steht an dieser Stelle alleine, 8:=Die Textbereiche a
   * und b überlappen sich an dieser Stelle, B:=der Textbereich b steht an
   * dieser Stelle alleine.
   */
  public boolean followsOrderschemeAABB()
  {
    return rel == 4;
  }

  /**
   * Die beiden Textbereiche vergleichbar und liegen im selben Text-Objekt.
   */
  public boolean rangesAreComparable()
  {
    return rel >= -4 && rel <= 4;
  }

  /**
   * Diese Methode vergleicht die beiden TextRanges a und b und liefert einen
   * internen Integer-Wert der durch diese Klasse vernünftiger Form aufbereitet
   * wird.
   */
  private static int compareTextRanges(XTextRange a, XTextRange b)
  {
    // Fälle: A:=a alleine, 8:=Überlagerung von a und b, B:=b alleine
    // -4 = BBBBAAAA bzw. BB88AA
    // -3 = BB88
    // -2 = B88B
    // -1 = 88AA
    // +0 = 8888
    // +1 = 88BB
    // +2 = A88A
    // +3 = AA88
    // +4 = AAAABBBB bzw. AA88BB

    XTextRangeCompare compare = null;
    if (a != null) {
      compare = UNO.XTextRangeCompare(a.getText());
    }
    if (compare != null && b != null)
    {
      try
      {
        int start = compare.compareRegionStarts(a, b) + 1;
        int end = compare.compareRegionEnds(a, b) + 1;
        return (3 * start + 1 * end) - 4;
      }
      catch (IllegalArgumentException e)
      {
        // nicht loggen! Tritt regulär auf, wenn Textbereiche mit verschiedenen
        // Text-Objekten verglichen werden.
      }
    }
    return -50;
  }
}
