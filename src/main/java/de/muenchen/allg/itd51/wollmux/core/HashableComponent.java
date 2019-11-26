/*
 * Dateiname: HashableComponent.java
 * Projekt  : WollMux
 * Funktion : Wrapper, um UNO-Objekte hashbar zu machen.
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
 * 10.12.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux.core;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import de.muenchen.allg.afid.UNO;

/**
 * Hilfsklasse, die es ermöglicht, UNO-Componenten in HashMaps abzulegen; der
 * Vergleich zweier HashableComponents mit equals(...) verwendet dazu den sicheren
 * UNO-Vergleich UnoRuntime.areSame(...). Die Methode hashCode verwendet die sichere
 * Oid, die UnoRuntime.generateOid(...) liefert.
 */
public class HashableComponent
{
  private XInterface compo;

  /*
   * Anmerkung: Es mag ineffizient wirken, hier ein Object zu nehmen und immer in
   * XInterface zu casten, aber wegen Bugs in OOo ist es die einzig sichere Methode,
   * auch wenn man schon ein X... Objekt hat, das von XInterface abgeleitet ist.
   */
  public HashableComponent(Object compo)
  {
    this.compo = UNO.XInterface(compo);
    if (this.compo == null)
    {
      throw new ClassCastException();
    }
  }

  /**
   * Liefert die Komponente, die durch diese {@link HashableComponent} gewrappt ist.
   */
  public XInterface getComponent()
  {
    return compo;
  }

  @Override
  public int hashCode()
  {
    if (compo != null)
    {
      return UnoRuntime.generateOid(compo).hashCode();
    }
    return 0;
  }

  @Override
  public boolean equals(Object b)
  {
    if (b != null && b instanceof HashableComponent)
    {
      HashableComponent other = (HashableComponent) b;
      return UnoRuntime.areSame(this.compo, other.compo);
    }
    return false;
  }
}
