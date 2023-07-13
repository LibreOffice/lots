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
package org.libreoffice.lots.sender;

import java.util.Map;

/**
 * Description of a sender in the cache.
 */
public class SenderConf
{
  private final String key;
  private final Map<String, String> cachedValues;
  private final Map<String, String> overriddenValues;

  /**
   * New description of a sender.
   *
   * @param key
   *          The key of the sender.
   * @param cachedValues
   *          It's cached values.
   * @param overriddenValues
   *          It's overridden values.
   */
  public SenderConf(String key, Map<String, String> cachedValues, Map<String, String> overriddenValues)
  {
    this.key = key;
    this.cachedValues = cachedValues;
    this.overriddenValues = overriddenValues;
  }

  public String getKey()
  {
    return key;
  }

  public Map<String, String> getCachedValues()
  {
    return cachedValues;
  }

  public Map<String, String> getOverriddenValues()
  {
    return overriddenValues;
  }
}
