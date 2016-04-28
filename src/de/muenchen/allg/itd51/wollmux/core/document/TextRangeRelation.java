/*
 * Dateiname: TextRangeRelation.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse repräsentiert die möglichen Beziehungen zwischen 
 *            zwei Textbereichen a und b.
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
 * 30.10.2007 | LUT | Erstellung als TextRangeRelation
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.document;

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
    if (a != null) compare = UNO.XTextRangeCompare(a.getText());
    if (compare != null && a != null && b != null)
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
