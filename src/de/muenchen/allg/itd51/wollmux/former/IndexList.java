//TODO L.m()
/*
* Dateiname: IndexList.java
* Projekt  : WollMux
* Funktion : Verwaltet eine Liste von Indices.
* 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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

import java.util.Collections;
import java.util.ListIterator;
import java.util.Vector;

public class IndexList
{
  /**
   * Die (immer aufsteigend sortierte) Liste der Indizes.
   */
  private Vector<Integer> indices = new Vector<Integer>();
  
  
  /**
   * Addiert auf alle Indizes in der {@link #indices} Liste größer gleich start den offset.
   * Indizes die dabei < 0 oder > maxindex werden werden aus der Liste gelöscht.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
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
          iter.set(new Integer(i));
      }
    }
  }
  
  /**
   * Ersetzt in {@link #indices} index1 durch index2 und index2 durch index1.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public void swap(int index1, int index2)
  {
    ListIterator<Integer> iter = indices.listIterator();
    while (iter.hasNext())
    {
      Integer I = iter.next();
      int i = I.intValue();
      if (i == index1)
        iter.set(new Integer(index2));
      else if (i == index2)
        iter.set(new Integer(index1));
    }
    
    Collections.sort(indices);
  }

  /**
   * Liefert einen Iterator über die Integers in dieser Liste.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ListIterator<Integer> iterator()
  {
    return indices.listIterator();
  }
  
  /**
   * Liefert einen Listiterator, der hinter dem letzten Element der Liste (von Integers) startet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ListIterator<Integer> reverseIterator()
  {
    return indices.listIterator(indices.size());
  }
  
  /**
   * Entfernt den Index i aus der {@link #indices} Liste falls er dort enthalten ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED*/
  public void remove(int i)
  {
    if (indices.remove(new Integer(i)))
      Collections.sort(indices);
  }
  
  public void add(int i)
  {
    indices.add(new Integer(i));
    Collections.sort(indices);
  }
  
  public boolean contains(int i)
  {
    return indices.contains(new Integer(i));
  }
  
  public boolean isEmpty()
  {
    return indices.isEmpty();
  }
  
  
  
  public int firstElement()
  {
    if (indices.size() == 0) return -1;
    return indices.firstElement().intValue(); 
  }
  
  public int lastElement()
  {
    if (indices.size() == 0) return -1;
    return indices.lastElement().intValue(); 
  }
  
  public void clear()
  {
    indices.clear();
  }
  

}
