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
package de.muenchen.allg.itd51.wollmux;

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
