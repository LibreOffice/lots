/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

import org.libreoffice.ext.unohelper.common.UNO;

/**
 * Helper class that allows to store UNO components in HashMaps;
 * the comparison of two HashableComponents with equals(...) uses the secure
 * UNO comparison UnoRuntime.areSame(...) for this purpose.
 * The hashCode method uses the safe Oid provided by UnoRuntime.generateOid(...).
 */
public class HashableComponent
{
  private XInterface compo;

  /*
   * Note: It may seem inefficient to take an object here and always cast to
   * XInterface, but because of bugs in OOo it is the only safe method,
   * even if you already have an X Object derived from XInterface.
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
