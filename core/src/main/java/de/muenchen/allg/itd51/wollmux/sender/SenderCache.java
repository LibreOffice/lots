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
package de.muenchen.allg.itd51.wollmux.sender;

import java.util.List;

import de.muenchen.allg.itd51.wollmux.interfaces.XPALChangeEventListener;

/**
 * A cache for the sender list of an user. Every time the
 * {{@link #updateContent(com.sun.star.lang.EventObject)} is called, the cache should be persisted.
 */
public interface SenderCache extends XPALChangeEventListener
{

  /**
   * Get the schema of the cache.
   *
   * @return The schema.
   */
  List<String> getSchema();

  /**
   * Get the index of the selected sender. As there may be more sender with the same key, the index
   * can be used to get the right one.
   *
   * @return The index of the sender. Sender with different keys can have the same index. Sender
   *         with the same key always have a different index.
   */
  int getSelectedSameKeyIndex();

  /**
   * Get the key of the selected sender. There may be more sender with the same key.
   *
   * @return The key.
   */
  String getSelectedKey();

  /**
   * Get all sender definitions from the persistent cache.
   *
   * @return List of all senders.
   */
  List<SenderConf> getData();

}
