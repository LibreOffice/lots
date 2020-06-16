package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.awt.GridBagConstraints;
import java.util.Map;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.core.dialog.controls.UIElement;

public class UIElementContext
{
  /**
   * Bildet einen TYPE auf die dazugehörigen layout constraints (d,i, der optionale
   * zweite Parameter von
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object)
   * java.awt.Container.add()}) ab. Darf null-Werte enthalten. Ist für einen TYPE
   * kein Mapping angegeben (auch kein null-Wert), so wird erst geschaut, ob ein
   * Mapping für "default" vorhanden ist. Falls ja, so wird dieses der
   * entsprechenden Eigenschaft des erzeugten UIElements zugewiesen, ansonsten
   * null.
   */
  private Map<UIElementType, ?> mapTypeToLayoutConstraints;

  /**
   * Bildet einen TYPE auf einen Integer ab, der angibt, ob das UI Element ein
   * zusätzliches Label links oder rechts bekommen soll. Mögliche Werte sind
   * {@link UIElement#LabelPosition#LEFT}, {@link UIElement#LabelPosition#RIGHT}
   * und {@link UIElement#LabelPosition#NONE}. Darf null-Werte enthalten. Ist
   * für einen TYPE kein Mapping angegeben (auch kein null-Wert), so wird erst
   * geschaut, ob ein Mapping für "default" vorhanden ist. Falls ja, so wird
   * dieses der entsprechenden Eigenschaft des erzeugten UIElements zugewiesen,
   * ansonsten null.
   */
  private Map<UIElementType, UIElement.LabelPosition> mapTypeToLabelType;

  /**
   * Für UI Elemente, die ein zusätzliches Label links oder rechts bekommen sollen
   * (siehe {@link #mapTypeToLabelType}) liefert diese Map die layout constraints
   * für das Label. Achtung! UI Elemente mit TYPE "label" beziehen ihre layout
   * constraints nicht aus dieser Map, sondern wie alle anderen UI Elemente auch
   * aus {@link #mapTypeToLayoutConstraints}. Darf null-Werte enthalten. Ist für
   * einen TYPE kein Mapping angegeben (auch kein null-Wert), so wird erst
   * geschaut, ob ein Mapping für "default" vorhanden ist. Falls ja, so wird dieses
   * der entsprechenden Eigenschaft des erzeugten UIElements zugewiesen, ansonsten
   * null.
   */
  private Map<UIElementType, ?> mapTypeToLabelLayoutConstraints;

  /**
   * Die Menge (von Strings) der ACTIONs, die akzeptiert werden sollen. Alle
   * anderen produzieren eine Fehlermeldung.
   */
  private Set<String> supportedActions;

  /**
   * Der {@link UIElementEventHandler}, an den die erzeugten UI Elemente ihre
   * Ereignisse melden.
   */
  private UIElementEventHandler uiElementEventHandler;

  /**
   * Enthält diese Map für einen TYPE ein Mapping auf einen anderen TYPE, so wird
   * der andere TYPE verwendet. Dies ist nützlich, um abhängig vom Kontext den TYPE
   * "separator" entweder auf "h-separator" oder "v-separator" abzubilden.
   */
  private Map<UIElementType, UIElementType> mapTypeToType;

  /**
   * Get the layout constraints for a type.
   *
   * @param type
   *          The type.
   * @return The constraints.
   */
  public Object getLayoutConstraints(UIElementType type)
  {
    Object layoutConstraints;
    
    if (mapTypeToLayoutConstraints.containsKey(type))
      layoutConstraints = mapTypeToLayoutConstraints.get(type);
    else
      layoutConstraints = mapTypeToLayoutConstraints.get(UIElementType.DEFAULT);
    
    if (layoutConstraints instanceof GridBagConstraints)
      layoutConstraints = ((GridBagConstraints) layoutConstraints).clone();
    
    return layoutConstraints;
  }

  public void setMapTypeToLayoutConstraints(Map<UIElementType, ?> mapTypeToLayoutConstraints)
  {
    this.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
  }

  /**
   * Get the type of the label for an element type.
   *
   * @param type
   *          The element type.
   * @return The label type.
   */
  public UIElement.LabelPosition getLabelType(UIElementType type)
  {
    if (mapTypeToLabelType.containsKey(type))
      return mapTypeToLabelType.get(type);
    else
      return mapTypeToLabelType.get(UIElementType.DEFAULT);
  }

  public void setMapTypeToLabelType(Map<UIElementType, UIElement.LabelPosition> mapTypeToLabelType)
  {
    this.mapTypeToLabelType = mapTypeToLabelType;
  }

  /**
   * Get the layout constraints for the label of an element type.
   *
   * @param type
   *          The element type.
   * @return The layout constraints of its lable.
   */
  public Object getLabelLayoutConstraints(UIElementType type)
  {
    Object labelLayoutConstraints;
    if (mapTypeToLabelLayoutConstraints.containsKey(type))
      labelLayoutConstraints = mapTypeToLabelLayoutConstraints.get(type);
    else
      labelLayoutConstraints = mapTypeToLabelLayoutConstraints.get(UIElementType.DEFAULT);

    if (labelLayoutConstraints instanceof GridBagConstraints)
      labelLayoutConstraints = ((GridBagConstraints) labelLayoutConstraints).clone();
    
    return labelLayoutConstraints;
  }

  public void setMapTypeToLabelLayoutConstraints(Map<UIElementType, ?> mapTypeToLabelLayoutConstraints)
  {
    this.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
  }

  public Set<String> getSupportedActions()
  {
    return supportedActions;
  }

  public void setSupportedActions(Set<String> supportedActions)
  {
    this.supportedActions = supportedActions;
  }

  public UIElementEventHandler getUiElementEventHandler()
  {
    return uiElementEventHandler;
  }

  public void setUiElementEventHandler(UIElementEventHandler uiElementEventHandler)
  {
    this.uiElementEventHandler = uiElementEventHandler;
  }

  /**
   * Map an element type to another type.
   *
   * @param type
   *          The type to map.
   * @return The new type.
   */
  public UIElementType getMappedType(UIElementType type)
  {
    if (mapTypeToType != null && mapTypeToType.containsKey(type))
      return mapTypeToType.get(type);
    return type;
  }

  public void setMapTypeToType(Map<UIElementType, UIElementType> mapTypeToType)
  {
    this.mapTypeToType = mapTypeToType;
  }
}