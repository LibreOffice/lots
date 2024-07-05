/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.form.config;

import org.libreoffice.lots.config.ConfigThingy;

/**
 * A description of a visibility group.
 */
public class VisibilityGroupConfig
{

  /**
   * The description of the condition.
   */
  private ConfigThingy condition;

  /**
   * The ID of the group.
   */
  private String groupId;

  /**
   * Create a new visibility group configuration.
   *
   * @param visibilityDesc
   *          The description of the visibility group.
   */
  public VisibilityGroupConfig(ConfigThingy visibilityDesc)
  {
    groupId = visibilityDesc.getName();
    condition = visibilityDesc;
  }

  public String getGroupId()
  {
    return groupId;
  }

  public ConfigThingy getCondition()
  {
    return condition;
  }
}
