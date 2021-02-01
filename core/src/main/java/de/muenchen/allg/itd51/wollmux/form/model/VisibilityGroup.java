/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.form.model;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.form.config.VisibilityGroupConfig;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values;

/**
 * A visibility in a form.
 */
public class VisibilityGroup
{
  /**
   * The condition of the group.
   */
  private Function condition;

  /**
   * True if the group is visible, false otherwise.
   */
  private boolean visible = true;

  /**
   * The id of the group.
   */
  private String groupId;

  /**
   * Create a new visibility group.
   *
   * @param conf
   *          The configuration.
   * @param funcLib
   *          The function library.
   * @param dialogLib
   *          The dialog library.
   * @param functionContext
   *          The function context.
   */
  public VisibilityGroup(VisibilityGroupConfig conf, FunctionLibrary funcLib, DialogLibrary dialogLib,
      Map<Object, Object> functionContext)
  {
    groupId = conf.getGroupId();
    try
    {
      condition = FunctionFactory.parseChildren(conf.getCondition(), funcLib, dialogLib, functionContext);
      if (condition == null)
      {
        condition = FunctionFactory.alwaysTrueFunction();
      }
    } catch (ConfigurationErrorException e)
    {
      condition = FunctionFactory.alwaysTrueFunction();
    }
  }

  public String getGroupId()
  {
    return groupId;
  }

  public Function getCondition()
  {
    return condition;
  }

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Compute the visibility of this group.
   *
   * @param values
   *          The form values.
   */
  public void computeVisibility(Values values)
  {
    visible = condition.getBoolean(values);
  }
}
