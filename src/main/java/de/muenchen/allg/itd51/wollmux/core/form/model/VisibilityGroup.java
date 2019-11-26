package de.muenchen.allg.itd51.wollmux.core.form.model;

import java.util.Optional;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Eine Sichtbarkeit im Formular-Model.
 * 
 * @author daniel.sikeler
 *
 */
public class VisibilityGroup
{

  /**
   * Die Bedingung für die Sichtbarkeit (true = sichtbar), wenn keine Sichtbarkeitsbedingung
   * definiert ist wird immer true geliefert.
   */
  private Optional<Function> condition;

  /**
   * true, wenn die Gruppe im Augenblick sichtbar ist.
   */
  private boolean visible = true;

  /**
   * Die GROUP id dieser Gruppe.
   */
  private String groupId;

  /**
   * Initialisiert die Gruppe. Dabei wird keine Bedingung gesetzt, also ist die Gruppe erstmal immer
   * sichtbar.
   * 
   * @param groupId
   *          Die Id der Gruppe.
   */
  public VisibilityGroup(String groupId)
  {
    this.groupId = groupId;
    this.condition = Optional.ofNullable(null);
  }

  public String getGroupId()
  {
    return groupId;
  }

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Eine neue Bedigung für die Sichtbarkeit setzen.
   * 
   * @param condition
   *          Die neue Bedigung.
   * @throws FormModelException
   *           Wenn bereits eine Bedignung definiert wurde.
   */
  public void setCondition(Optional<Function> condition) throws FormModelException
  {
    if (this.condition.isPresent())
    {
      throw new FormModelException(
          L.m("Mehrere Sichtbarkeitsregeln für Gruppe \"%1\" angegeben.", groupId));
    }
    this.condition = condition;
  }

  /**
   * Berechnet anhand der Formularwerte values die Sichtbarkeit dieser Gruppe. Alle
   * Gruppenmitglieder werden über die Änderung informiert.
   * 
   * @param values
   *          Die Formularwerte.
   */
  public void computeVisibility(Values values)
  {
    visible = condition.orElse(FunctionFactory.alwaysTrueFunction()).getBoolean(values);
  }
}
