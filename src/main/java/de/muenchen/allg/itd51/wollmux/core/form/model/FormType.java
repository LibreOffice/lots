package de.muenchen.allg.itd51.wollmux.core.form.model;

/**
 * Die Typen von Formularelementen.
 * 
 * @author daniel.sikeler
 *
 */
public enum FormType
{
  /**
   * Ein Textfeld.
   */
  TEXTFIELD,
  /**
   * Eine Textarea.
   */
  TEXTAREA,
  /**
   * Eine Combobox.
   */
  COMBOBOX,
  /**
   * Eine Checkbox.
   */
  CHECKBOX,
  /**
   * Ein Label.
   */
  LABEL,
  /**
   * Ein Separator. Wird je nach Kontext zu {@link FormType#V_SEPARATOR} oder
   * {@link FormType#H_SEPARATOR}.
   */
  SEPARATOR,
  /**
   * Ein vertikaler Separator.
   */
  V_SEPARATOR,
  /**
   * Ein horizontaler Separator.
   */
  H_SEPARATOR,
  /**
   * Ein Abstand. Wird je nach Kontext zu {@link FormType#V_GLUE} oder {@link FormType#H_GLUE}.
   */
  GLUE,
  /**
   * Ein vertikaler Abstand.
   */
  V_GLUE,
  /**
   * Ein horizontaler Abstand.
   */
  H_GLUE,
  /**
   * Ein Button.
   */
  BUTTON,
  /**
   * Ein Menüeintrag.
   */
  MENUITEM,
  /**
   * Eine Listbox.
   */
  LISTBOX,
  /**
   * Default-Type
   */
  DEFAULT;

  /**
   * Ordnet einem String den entsprechende FormType zu.
   * 
   * @param type
   *          Der String.
   * @return Der FormType.
   * @throws FormModelException
   *           Wenn für den String kein FormType existiert.
   */
  public static FormType getType(String type) throws FormModelException
  {
    try
    {
      return FormType.valueOf(FormType.class, type.toUpperCase().replaceAll("-", "_"));
    } catch (IllegalArgumentException e)
    {
      throw new FormModelException("Unbekannte TYPE-Angabe für ein Formularelement.", e);
    }
  }

  @Override
  public String toString()
  {
    return super.toString().toLowerCase().replaceAll("_", "-");
  }
}
