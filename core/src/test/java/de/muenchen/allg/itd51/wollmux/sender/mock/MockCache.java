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
package de.muenchen.allg.itd51.wollmux.sender.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.lang.EventObject;

import de.muenchen.allg.itd51.wollmux.sender.SenderCache;
import de.muenchen.allg.itd51.wollmux.sender.SenderConf;

public class MockCache implements SenderCache
{
  private boolean saved = false;
  private SenderConf sender1 = new SenderConf("ds", Map.of("column", "value1"), new HashMap<>());
  private SenderConf sender2 = new SenderConf("lost", Map.of("column", "lost1"), new HashMap<>());
  private List<String> schema = new ArrayList<>();

  public MockCache()
  {
    schema.add("column");
  }

  public boolean isSaved()
  {
    return saved;
  }

  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  @Override
  public int getSelectedSameKeyIndex()
  {
    return 0;
  }

  @Override
  public String getSelectedKey()
  {
    return sender1.getKey();
  }

  @Override
  public List<SenderConf> getData()
  {
    return List.of(sender1, sender2);
  }

  @Override
  public void updateContent(EventObject event)
  {
    saved = true;
  }

  @Override
  public void disposing(EventObject event)
  {
    // nothing to do;
  }

}
