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
