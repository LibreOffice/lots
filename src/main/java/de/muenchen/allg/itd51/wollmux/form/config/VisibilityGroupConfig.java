package de.muenchen.allg.itd51.wollmux.form.config;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

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
