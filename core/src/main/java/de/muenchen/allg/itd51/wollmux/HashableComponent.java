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

import org.libreoffice.ext.unohelper.common.UNO;

/**
 * Helper class that makes it possible to store UNO components in HashMaps; the
 * Comparing two HashableComponents with equals(...) uses the safe one
 * UNO comparison UnoRuntime.areSame(...). The hashCode method uses the safe one
 * Oid that UnoRuntime.generateOid(...) returns.
 */
public class HashableComponent
{
  private XInterface compo;

  /*
   * Note: It may seem inefficient to take an Object here and always in
   * Cast XInterface, but because of bugs in OOo it's the only safe way
   * even if you already have an X... object derived from XInterface.
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
   * Returns the component wrapped by this {@link HashableComponent}.
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
