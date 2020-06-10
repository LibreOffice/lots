package de.muenchen.allg.itd51.wollmux.form.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementType;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.functions.Values.SimpleMap;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

/**
 * Beschreibung eines Formularelementes samt Business-Logik (Plausis)
 *
 * @author daniel.sikeler
 *
 */
public class Control
{
  /**
   * Die Id.
   */
  private String id;
  /**
   * Der Typ.
   */
  private UIElementType type;
  /**
   * Die Plausi.
   */
  private Function plausi;
  /**
   * Die Funtkion, aus der der initiale Wert berechnet wird.
   */
  private Optional<Function> autofill;
  /**
   * Die Sichtbarkeitsgruppen, in denen das Formularelement ist.
   */
  private List<VisibilityGroup> groups = new ArrayList<>(1);

  /**
   * Ist das Element gerade in einem validen Zustand (Plausi).
   */
  private boolean okay = true;

  /**
   * Der aktuelle Wert des Elements.
   */
  private String value = "";

  /**
   * Options of ComboBoxes.
   */
  private List<String> options;

  /**
   * Eine Liste der abhängigen Sichtbarkeitsgruppen.
   */
  private List<VisibilityGroup> dependingGroups = new ArrayList<>(1);

  /**
   * Eine Liste der Formularfelder deren Autofill von diesem ab hängt.
   */
  private List<Control> dependingAutoFillFormFields = new ArrayList<>(1);

  /**
   * Eine Liste der Formularfelder deren Plausi von diesem ab hängt.
   */
  private List<Control> dependingPlausiFormFields = new ArrayList<>(1);

  /**
   * A form control
   *
   * @param conf
   *          The configuration of the control.
   * @param funcLib
   *          The function library.
   * @param dialogLib
   *          The dialog library.
   * @param functionContext
   *          The function context.
   */
  public Control(UIElementConfig conf, FunctionLibrary funcLib, DialogLibrary dialogLib,
      Map<Object, Object> functionContext)
  {
    id = conf.getId();
    type = conf.getType();
    try
    {
      plausi = FunctionFactory.parseGrandchildren(conf.getPlausi(), funcLib, dialogLib, functionContext);
      if (plausi == null)
      {
        plausi = FunctionFactory.alwaysTrueFunction();
      }
    } catch (ConfigurationErrorException e)
    {
      plausi = FunctionFactory.alwaysTrueFunction();
    }
    try
    {
      autofill = Optional
          .ofNullable(FunctionFactory.parseGrandchildren(conf.getAutofill(), funcLib, dialogLib, functionContext));
    } catch (ConfigurationErrorException e)
    {
      autofill = Optional.empty();
    }
    this.options = conf.getOptions();
  }

  public String getId()
  {
    return id;
  }

  public UIElementType getType()
  {
    return type;
  }

  public Function getPlausi()
  {
    return plausi;
  }

  public boolean isOkay()
  {
    return okay;
  }

  /**
   * Überprüft ob das Element, ein gültigen Wert enthält und informiert die Listener.
   *
   * @param values
   *          Die Werte aus dem Model.
   */
  public void setOkay(Values values)
  {
    okay = plausi.getBoolean(values);
  }

  public Optional<Function> getAutofill()
  {
    return autofill;
  }

  public String getValue()
  {
    return value;
  }

  public void setValue(String value)
  {
    this.value = value;
  }

  public List<VisibilityGroup> getGroups()
  {
    return groups;
  }

  /**
   * Mark this control as part of the group.
   *
   * @param group
   *          The group.
   */
  public void addGroup(VisibilityGroup group)
  {
    groups.add(group);
  }

  public List<VisibilityGroup> getDependingGroups()
  {
    return dependingGroups;
  }

  /**
   * Add a dependency for a visibility group.
   *
   * @param group
   *          The dependent group.
   */
  public void addDependingGroup(VisibilityGroup group)
  {
    if (!dependingGroups.contains(group))
    {
      dependingGroups.add(group);
    }
  }

  /**
   * Add a dependency for an AUTOFILL function.
   *
   * @param control
   *          The dependent control.
   */
  public void addDependingAutoFillFormField(Control control)
  {
    dependingAutoFillFormFields.add(control);
  }

  /**
   * Add a dependency for a PLAUSI function.
   *
   * @param control
   *          The dependent control.
   */
  public void addDependingPlausiFormField(Control control)
  {
    dependingPlausiFormFields.add(control);
  }

  /**
   * Berechnet für alle abhängigen Felder die Werte neu. Dabei werden Zyklen aufgelöst. Für jedes
   * Feld wird nur einmal der Wert berechnet.
   *
   * @param value
   *          Der neue Wert dieses Feldes.
   * @param values
   *          Die bisherigen Werte aller Felder.
   * @param modified
   *          Eine Map, in die alle geänderten Felder eingetragen werden.
   */
  public void computeNewValues(String value, SimpleMap values, SimpleMap modified)
  {
    modified.put(id, value);
    SimpleMap newValues = new SimpleMap(values);
    newValues.putAll(modified);
    for (Control control : dependingAutoFillFormFields)
    {
      if (!modified.hasValue(control.id))
      {
        String v = control.computeValue(newValues);
        if (v != null)
        {
          control.computeNewValues(v, newValues, modified);
        }
      }
    }
  }

  /**
   * Berechnet den Wert aus der Autofill-Funtkion und den bisherigen Werten aus dem Model.
   *
   * @param values
   *          Die Werte aus dem Model.
   * @return Den berechneten Wert oder null wenn keine Autofill-Funktion vorhanden ist. Der Wert
   *         muss mit {@link #setValue(String)} gesetzt werden.
   */
  public String computeValue(SimpleMap values)
  {
    if (autofill.isPresent())
      return autofill.get().getString(values);
    else if (type == UIElementType.COMBOBOX && !options.isEmpty())
      return options.get(0);
    else
      return "";
  }

  public boolean isVisible()
  {
    switch (type)
    {
    case BUTTON:
    case CHECKBOX:
    case COMBOBOX:
    case LABEL:
    case LISTBOX:
    case TEXTAREA:
    case TEXTFIELD:
      return groups.stream().allMatch(VisibilityGroup::isVisible);
    default:
      return false;
    }
  }
}
