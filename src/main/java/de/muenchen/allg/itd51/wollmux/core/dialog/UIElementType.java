package de.muenchen.allg.itd51.wollmux.core.dialog;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

/**
 * Types of control elements.
 */
public enum UIElementType
{
  /**
   * An input field.
   */
  TEXTFIELD,

  /**
   * A text area.
   */
  TEXTAREA,

  /**
   * A combo box.
   */
  COMBOBOX,

  /**
   * A check box.
   */
  CHECKBOX,

  /**
   * A label.
   */
  LABEL,

  /**
   * A separator. Depending on the context it is a {@link UIElementType#V_SEPARATOR} or
   * {@link UIElementType#H_SEPARATOR}.
   */
  SEPARATOR,

  /**
   * A vertical separator.
   */
  V_SEPARATOR,

  /**
   * A horizontal separator.
   */
  H_SEPARATOR,

  /**
   * A glue. Depending on the context it is a {@link UIElementType#V_GLUE} or
   * {@link UIElementType#H_GLUE}.
   */
  GLUE,

  /**
   * A vertical glue.
   */
  V_GLUE,

  /**
   * A horizontal glue.
   */
  H_GLUE,

  /**
   * A button.
   */
  BUTTON,

  /**
   * A menu entry.
   */
  MENUITEM,

  /**
   * A list box.
   */
  LISTBOX,

  /**
   * A menu.
   */
  MENU,

  /**
   * A sender box.
   */
  SENDERBOX,

  /**
   * A search box.
   */
  SEARCHBOX,

  /**
   * Default-Type.
   */
  DEFAULT;

  /**
   * Map a string to a {@link UIElementType}.
   * 
   * @param type
   *          The string.
   * @return The type.
   * @throws ConfigurationErrorException
   *           There's no type for the given string.
   */
  public static UIElementType getType(String type)
  {
    try
    {
      return UIElementType.valueOf(UIElementType.class, type.toUpperCase().replaceAll("-", "_"));
    } catch (IllegalArgumentException e)
    {
      throw new ConfigurationErrorException("Unbekannte TYPE-Angabe für ein Element.", e);
    }
  }

  @Override
  public String toString()
  {
    return super.toString().toLowerCase().replaceAll("_", "-");
  }
}
