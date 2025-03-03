/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.form.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.libreoffice.lots.dialog.DialogLibrary;
import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.form.config.FormConfig;
import org.libreoffice.lots.form.config.TabConfig;
import org.libreoffice.lots.form.config.VisibilityGroupConfig;
import org.libreoffice.lots.form.sidebar.FormSidebarController;
import org.libreoffice.lots.func.FunctionLibrary;
import org.libreoffice.lots.func.Values;
import org.libreoffice.lots.func.Values.SimpleMap;
import org.libreoffice.lots.ui.UIElementConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model of a form.
 */
public class FormModel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormModel.class);

  /**
   * The function library.
   */
  private final FunctionLibrary funcLib;

  /**
   * The dialog library.
   */
  private final DialogLibrary dialogLib;

  /**
   * The context of functions.
   */
  private final Map<Object, Object> functionContext;

  /**
   * Mapping form visibility group IDs to visibility groups.
   */
  private final Map<String, VisibilityGroup> visiblities = new HashMap<>();

  /**
   * Mapping from control element IDs to form controls.
   */
  private final Map<String, Control> formControls = new LinkedHashMap<>();

  /**
   * Mapping from function dialog names to controls whose AUTOFILL dependens on this dialog.
   */
  private Map<String, List<Control>> mapDialogNameToListOfControlsWithDependingAutofill = new HashMap<>();

  /**
   * Instance of TextDocumentController.
   */
  private TextDocumentController txtDocController;

  /**
   * Instance of FormSidebarController.
   */
  private FormSidebarController formSidebarController;

  /**
   * A new form model.
   *
   * @param conf
   *          The configuration of the model.
   * @param functionContext
   *          The context of functions.
   * @param funcLib
   *          The function library.
   * @param dialogLib
   *          The dialog library.
   * @param presetValues
   *          The values already set in the document.
   * @param txtDocController
   *          Instance of TextDocumentController.
   */
  @SuppressWarnings("squid:S3776")
  public FormModel(FormConfig conf, Map<Object, Object> functionContext, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<String, String> presetValues, TextDocumentController txtDocController)
  {
    this.functionContext = functionContext;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;
    this.txtDocController = txtDocController;

    for (VisibilityGroupConfig config : conf.getVisibilities())
    {
      VisibilityGroup group = new VisibilityGroup(config, funcLib, dialogLib, functionContext);
      if (!visiblities.containsKey(config.getGroupId()))
      {
        visiblities.put(config.getGroupId(), group);
      }
    }

    for (TabConfig tab : conf.getTabs())
    {
      for (UIElementConfig config : tab.getControls())
      {
        Control control = new Control(config, funcLib, dialogLib, functionContext);
        config.getGroups().forEach(id -> control.addGroup(visiblities.get(id)));
        addFormField(control);
      }
      for (UIElementConfig config : tab.getButtons())
      {
        addFormField(new Control(config, funcLib, dialogLib, functionContext));
      }
    }
    for (Control control : formControls.values())
    {
      storeDepsForFormField(control);
    }

    this.initControls(presetValues);
  }

  /**
   * Init Controls autofills, trafos and visibilites.
   *
   * @param presetValues
   */
  private void initControls(Map<String, String> presetValues)
  {
    // Initialize controls with preset values or AUTOFILL function
    SimpleMap values = idToValue();
    for (Control control : formControls.values())
    {
      String value = "";
      if (presetValues.containsKey(control.getId()))
      {
        value = presetValues.get(control.getId());
      } else
      {
        value = control.computeValue(values);
      }
      if (!value.equals(control.getValue()))
      {
        txtDocController.setValueChanged(control.getId(), value);
        control.setValue(value);
        values.put(control.getId(), value);
      }
    }

    for (Control control : formControls.values())
    {
      control.setOkay(values);
    }
    for (VisibilityGroup group : visiblities.values())
    {
      storeDepsForVisibility(group);
    }
  }

  public void setFormSidebarController(FormSidebarController formSidebarController)
  {
    this.formSidebarController = formSidebarController;
  }

  public DialogLibrary getDialogLib()
  {
    return dialogLib;
  }

  public Map<Object, Object> getFunctionContext()
  {
    return functionContext;
  }

  public FunctionLibrary getFuncLib()
  {
    return funcLib;
  }

  /**
   * Get a visibility group.
   *
   * @param groupId
   *          The ID of the group.
   * @return The group or null if there's no group with this id.
   */
  public VisibilityGroup getGroup(String groupId)
  {
    return visiblities.get(groupId);
  }

  /**
   * Get a control.
   *
   * @param controlId
   *          The id of the control.
   * @return The control or null if there's no control with this id.
   */
  public Control getControl(String controlId)
  {
    return formControls.get(controlId);
  }

  /**
   * List of Form Controls.
   *
   * @return All form controls from the container.
   */
  public Map<String, Control> getFormControls()
  {
    return formControls;
  }

  /**
   * Update visibilites in form ui.
   */
  public void updateFormControlsVisibility()
  {
    for (Map.Entry<String, Control> entry : this.getFormControls().entrySet())
    {
      for (VisibilityGroup vs: entry.getValue().getGroups())
      {
        //notifiy form gui ui
        formSidebarController.visibilityChanged(vs.getGroupId());
      }
    }
  }

  /**
   * Set the value of a control and notify the listeners. All depending controls are updated. For
   * all changed controls the state is computed.
   *
   * @param id
   *          The ID of the control.
   * @param value
   *          The value of the control.
   */
  public void setValue(final String id, final String value)
  {
    if (formControls.containsKey(id) && !formControls.get(id).getValue().equals(value))
    {
      Control field = formControls.get(id);
      SimpleMap modified = new SimpleMap();

      // compute dependent controls
      field.computeNewValues(value, idToValue(), modified);
      SimpleMap newValues = idToValue();
      newValues.putAll(modified);
      List<VisibilityGroup> modifiedGroups = new ArrayList<>();

      // update values and notify listener
      for (Map.Entry<String, String> changedEntries : modified)
      {
        Control control = formControls.get(changedEntries.getKey());
        control.setValue(changedEntries.getValue());
        control.setOkay(newValues);
        formSidebarController.setFormUiValue(control.getId(), control.getValue());
        formSidebarController.setControlBackground(control.getId(), control.isOkay(), false);
        txtDocController.setValueChanged(control.getId(), control.getValue());
        modifiedGroups.addAll(control.getDependingGroups());
      }
      modifiedGroups.forEach(g -> g.computeVisibility(newValues));

        for (VisibilityGroup g : modifiedGroups)
        {
          if (txtDocController != null && formSidebarController != null) {
            //update vis. in doc
            txtDocController.setVisibilityChanged(g.getGroupId(), g.isVisible());
            //update vis in form-ui
            formSidebarController.visibilityChanged(g.getGroupId());
          }
        }
    }
  }

  /**
   * Get the value of a control.
   *
   * @param id
   *          The ID of the control.
   * @return The value of the control.
   * @throws FormModelException
   *           There's no control with the given ID.
   */
  public String getValue(final String id) throws FormModelException
  {
    if (formControls.containsKey(id))
    {
      return formControls.get(id).getValue();
    }
    throw new FormModelException("Unbekanntes Formularelement " + id);
  }

  /**
   * Get a collection of {@link Control} with belong to the group.
   *
   * @param groupId
   *          The ID of the group.
   * @return Collection of {@link Control}.
   */
  public Collection<Control> getControlsByGroupId(String groupId)
  {
    List<Control> controls = new ArrayList<>();

    for (Map.Entry<String, Control> entry : formControls.entrySet())
    {
      for (VisibilityGroup vs : entry.getValue().getGroups())
      {
        if (vs.getGroupId().equals(groupId))
        {
          controls.add(entry.getValue());
        }
      }
    }

    return controls;
  }

  /**
   * Get the state of a control.
   *
   * @param id
   *          The ID of the control.
   * @return The state of the control.
   * @throws FormModelException
   *           There's no control with the given ID.
   */
  public boolean getStatus(final String id) throws FormModelException
  {
    if (formControls.containsKey(id))
    {
      return formControls.get(id).isOkay();
    }
    throw new FormModelException("Unbekanntes Formularelement " + id);
  }

  /**
   * Set the value of all controls which depend on the dialog.
   *
   * @param dialogName
   *          The name of the dialog.
   */
  public void setDialogAutofills(String dialogName)
  {
    for (Control c : mapDialogNameToListOfControlsWithDependingAutofill.get(dialogName))
    {
      c.getAutofill()
          .ifPresent(autofill -> setValue(c.getId(), autofill.getResult(idToValue())));
    }
  }

  /**
   * Has the form a control with the given ID.
   *
   * @param fieldId
   *          The ID of a control.
   * @return True if there's a conrol with this ID, false otherwise.
   */
  public boolean hasFieldId(String fieldId)
  {
    return formControls.containsKey(fieldId);
  }

  /**
   * Create a mapping from control ID to control value.
   *
   * @return A mapping which can be used as function parameter.
   */
  private SimpleMap idToValue()
  {
    SimpleMap values = new Values.SimpleMap();
    for (Map.Entry<String, Control> entry : formControls.entrySet())
    {
      values.put(entry.getKey(), entry.getValue().getValue());
    }
    return values;
  }

  /**
   * Add a dependencies for all function dialogs which are referenced by the AUTOFILL function.
   *
   * @param contro
   *          The control with the AUTOFILL function.
   */
  private void storeAutofillFunctionDialogDeps(Control control)
  {
    control.getAutofill().ifPresent(autofill -> {
      Set<String> funcDialogNames = new HashSet<>();
      autofill.getFunctionDialogReferences(funcDialogNames);
      for (String dialogName : funcDialogNames)
      {
        if (!mapDialogNameToListOfControlsWithDependingAutofill.containsKey(dialogName))
          mapDialogNameToListOfControlsWithDependingAutofill.put(dialogName,
              new ArrayList<Control>(1));

        List<Control> l = mapDialogNameToListOfControlsWithDependingAutofill.get(dialogName);
        l.add(control);
      }
    });
  }

  /**
   * Add a new control to the model and compute its initial value and dependencies.
   *
   * @param control
   *          The new control.
   */
  private void addFormField(Control control)
  {
    if (formControls.containsKey(control.getId()))
    {
      LOGGER.error("ID \"{}\" mehrfach vergeben", control.getId());
    }
    formControls.put(control.getId(), control);
  }

  /**
   * Computes for a control the dependency to other controls. Should be called after all controls
   * have been added to the model.
   *
   * @param control
   *          The new control for which we want the dependencies.
   */
  private void storeDepsForFormField(Control control)
  {
    storeDeps(control);
    storeAutofillFunctionDialogDeps(control);
  }

  /**
   * Compute the visibility of a visibility group and add the dependencies to the controls.
   *
   * @param group
   *          A visibility group.
   */
  private void storeDepsForVisibility(VisibilityGroup group)
  {
    String[] deps = group.getCondition().parameters();
    for (int i = 0; i < deps.length; ++i)
    {
      String elementId = deps[i];
      if (formControls.containsKey(elementId))
      {
        formControls.get(elementId).addDependingGroup(group);
      }
    }

    group.computeVisibility(idToValue());
  }

  /**
   * Compute the dependency on other controls based on the PLAUSI and AUTOFILL function.
   *
   * @param control
   *          The control which may depend on others.
   */
  private void storeDeps(Control control)
  {
    control.getAutofill().ifPresent(autofill -> Stream.of(autofill
        .parameters())
        .filter(id -> {
          if (!formControls.containsKey(id))
            LOGGER.warn("Unbekanntes Controlelement {} wird referenziert in {}", id,
                    control.getId());
          return formControls.containsKey(id);
        }).map(formControls::get).forEach(f -> f.addDependingAutoFillFormField(control)));
    Stream.of(control.getPlausi().parameters()).filter(id -> {
      if (!formControls.containsKey(id))
        LOGGER.warn("Unbekanntes Controlelement {} wird referenziert in {}", id,
                control.getId());
      return formControls.containsKey(id);
    }).map(formControls::get).forEach(f -> f.addDependingPlausiFormField(control));
    control.addDependingPlausiFormField(control);
  }

  /**
   * Notifies the listener with the current values and states.
   *
   * @param l
   *          The listener.
   */
  public void notifyWithCurrentValues(FormValueChangedListener l)
  {
    formControls.values().forEach(c -> {
      switch (c.getType())
      {
      case TEXTFIELD:
      case TEXTAREA:
      case COMBOBOX:
      case CHECKBOX:
      case LISTBOX:
        l.valueChanged(c.getId(), c.getValue());
        l.statusChanged(c.getId(), c.isOkay());
        break;
      default:
        break;
      }
    });
  }

  /**
   * Notifies the listener with the current visibility states.
   *
   * @param l
   *          The listener.
   */
  public void notifyWithCurrentVisibilites(VisibilityChangedListener l)
  {
    visiblities.values().forEach(g -> l.visibilityChanged(g.getGroupId(), g.isVisible()));
  }
}
