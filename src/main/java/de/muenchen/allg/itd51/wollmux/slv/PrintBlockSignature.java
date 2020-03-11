package de.muenchen.allg.itd51.wollmux.slv;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * The command names of print block commands.
 */
public enum PrintBlockSignature
{
  /**
   * Text marked with this command is always printed.
   */
  ALL_VERSIONS("AllVersions", L.m("wird immer gedruckt")),
  /**
   * Text marked with this command is only printed in the original (first content based directive).
   */
  ORIGINAL_ONLY("OriginalOnly", L.m("wird ausschließlich im Original gedruckt")),
  /**
   * Text marked with this command isn't printed in the original (first content based directive),
   * but everywhere else.
   */
  NOT_IN_ORIGINAL("NotInOriginal", L.m("wird im Original nicht gedruckt")),
  /**
   * Text marked with this command is only printed in drafts.
   */
  DRAFT_ONLY("DraftOnly", L.m("wird nur im Entwurf gedruckt")),
  /**
   * Text marked with this command is only printed in copies (all prints except original and draft).
   */
  COPY_ONLY("CopyOnly", L.m("wird ausschließlich in Abdrucken gedruckt"));

  /**
   * The command name.
   */
  private final String name;

  /**
   * A message to show when such a command is created.
   */
  private final String message;

  PrintBlockSignature(String name, String message)
  {
    this.name = name;
    this.message = message;
  }

  public String getName()
  {
    return name;
  }

  public String getMessage()
  {
    return message;
  }

  /**
   * Print blocks can be used with groups. This returns the groups name.
   *
   * @return "SLV_" + {@link #name}.
   */
  public String getGroupName()
  {
    return "SLV_" + name;
  }

  /**
   * Get the enum constant with the specified name.
   *
   * @param name
   *          The name of the constant.
   * @return An instance of {@link PrintBlockSignature}.
   * @throws IllegalArgumentException
   *           {@link Enum#valueOf(Class, String)}.
   */
  public static PrintBlockSignature valueOfIgnoreCase(String name)
  {
    for (PrintBlockSignature pbName : PrintBlockSignature.values())
    {
      if (pbName.name.equalsIgnoreCase(name))
        return pbName;
    }
    throw new IllegalArgumentException("No constant with the name: " + name);
  }
}
