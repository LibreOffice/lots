/*
 * Dateiname: HashableComponent.java
 * Projekt  : WollMux
 * Funktion : Wrapper, um UNO-Objekte hashbar zu machen.
 * 
 * Copyright: Landeshauptstadt München
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

package de.muenchen.allg.itd51.wollmux;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

/**
 * Hilfsklasse, die es ermöglicht, UNO-Componenten in HashMaps abzulegen; der
 * Vergleich zweier HashableComponents mit equals(...) verwendet dazu den
 * sicheren UNO-Vergleich UnoRuntime.areSame(...). Die Methode hashCode
 * verwendet die sichere Oid, die UnoRuntime.generateOid(...) liefert.
 * 
 * @author lut
 */
public class HashableComponent
{
  private XInterface compo;

  public HashableComponent(XInterface compo)
  {
    this.compo = compo;
  }

  public int hashCode()
  {
    if (compo != null) return UnoRuntime.generateOid(compo).hashCode();
    return 0;
  }

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
