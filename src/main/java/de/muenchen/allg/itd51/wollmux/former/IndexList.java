/*
 * Dateiname: IndexList.java
 * Projekt  : WollMux
 * Funktion : Verwaltet eine Liste von Indices.
 *
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 29.09.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.former;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Vector;

public class IndexList
{
  /**
   * Die (immer aufsteigend sortierte) Liste der Indizes.
   */
  private Vector<Integer> indices = new Vector<>();

  /**
   * Addiert auf alle Indizes in der {@link #indices} Liste größer gleich start den offset. Indizes
   * die dabei &lt; 0 oder &gt; maxindex werden werden aus der Liste gelöscht.
   */
  public void fixup(int start, int offset, int maxindex)
  {
    ListIterator<Integer> iter = indices.listIterator();
    while (iter.hasNext())
    {
      Integer I = iter.next();
      int i = I.intValue();
      if (i >= start)
      {
        i = i + offset;
        if (i < 0 || i > maxindex)
          iter.remove();
        else
          iter.set(Integer.valueOf(i));
      }
    }
  }

  /**
   * Ersetzt in {@link #indices} index1 durch index2 und index2 durch index1.
   */
  public void swap(int index1, int index2)
  {
    ListIterator<Integer> iter = indices.listIterator();
    while (iter.hasNext())
    {
      Integer I = iter.next();
      int i = I.intValue();
      if (i == index1)
        iter.set(Integer.valueOf(index2));
      else if (i == index2) iter.set(Integer.valueOf(index1));
    }

    Collections.sort(indices);
  }

  /**
   * Liefert einen Iterator über die Integers in dieser Liste.
   *
   * @return An independent iterator of the current list.
   */
  public ListIterator<Integer> iterator()
  {
    // copy list, so that it is independent of changing selections
    return new ArrayList<Integer>(indices).listIterator();
  }

  /**
   * Liefert einen Listiterator, der hinter dem letzten Element der Liste (von Integers) startet.
   *
   * @return An independent iterator of the current list.
   */
  public ListIterator<Integer> reverseIterator()
  {
    // copy list, so that it is independent of changing selections
    return new ArrayList<Integer>(indices).listIterator(indices.size());
  }

  /**
   * Entfernt den Index i aus der {@link #indices} Liste falls er dort enthalten ist.
   */
  public void remove(int i)
  {
    if (indices.remove(Integer.valueOf(i))) Collections.sort(indices);
  }

  public void add(int i)
  {
    indices.add(Integer.valueOf(i));
    Collections.sort(indices);
  }

  public boolean contains(int i)
  {
    return indices.contains(Integer.valueOf(i));
  }

  public boolean isEmpty()
  {
    return indices.isEmpty();
  }

  public int firstElement()
  {
    if (indices.isEmpty())
      return -1;
    return indices.firstElement().intValue();
  }

  public int lastElement()
  {
    if (indices.isEmpty())
      return -1;
    return indices.lastElement().intValue();
  }

  public void clear()
  {
    indices.clear();
  }

}
