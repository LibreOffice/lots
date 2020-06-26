/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementType;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values;
import de.muenchen.allg.itd51.wollmux.func.Values.SimpleMap;

/**
 * Representation of a form control.
 */
public class Control
{
  /**
   * The ID.
   */
  private String id;

  /**
   * The type of the control.
   */
  private UIElementType type;

  /**
   * The plausibility function.
   */
  private Function plausi;

  /**
   * The function to compute the value.
   */
  private Optional<Function> autofill;

  /**
   * Visibility groups to which this control belongs.
   */
  private List<VisibilityGroup> groups = new ArrayList<>(1);

  /**
   * Is the control in a valid state. So {@link #plausi} would return true.
   */
  private boolean okay = true;

  /**
   * The value of the control.
   */
  private String value = "";

  /**
   * If {@link #tpye} is {@link UIElementType#COMBOBOX} this field holds the options.
   */
  private List<String> options;

  /**
   * List of visibility groups which depend on this control.
   */
  private List<VisibilityGroup> dependingGroups = new ArrayList<>(1);

  /**
   * List of controls which {@link #autofill} depend on this control.
   */
  private List<Control> dependingAutoFillFormFields = new ArrayList<>(1);

  /**
   * List of controls which {@link #plausi} depend on this control.
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
   * Check if the control is valid based on the given values.
   *
   * @param values
   *          The form values.
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
   * Compute the value of all dependent fields. The value is only computed once for each control.
   *
   * @param value
   *          The new value of this control.
   * @param values
   *          The values of all fields.
   * @param modified
   *          Already computed fields.
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
   * Compute the value of the control based on {@link #autofill} and the provided values. The value
   * has to be set with {@link #setValue(String)}.
   *
   * @param values
   *          The values of the model.
   * @return The result of the AUTOFILL function if there's one. Otherwise the first option if it's
   *         a {@link UIElementType#COMBOBOX} or the empty string.
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

  /**
   * Is this control visible?
   *
   * @return True if all groups in which this control is are visible. False otherwise or if the
   *         control has no visible element.
   */
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
