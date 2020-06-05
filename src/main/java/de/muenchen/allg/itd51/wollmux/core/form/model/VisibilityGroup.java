package de.muenchen.allg.itd51.wollmux.core.form.model;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.form.config.VisibilityGroupConfig;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

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
   * DThe id of the group.
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
