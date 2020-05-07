package de.muenchen.allg.itd51.wollmux.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.RuntimeException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.core.document.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.core.document.SimulationResults;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel.OverrideFragChainException;
import de.muenchen.allg.itd51.wollmux.core.document.VisibilityElement;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.exceptions.UnavailableException;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormValueChangedListener;
import de.muenchen.allg.itd51.wollmux.core.form.model.VisibilityChangedListener;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.functions.Values.SimpleMap;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;
import de.muenchen.allg.itd51.wollmux.dialog.DialogFactory;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormValueChanged;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetVisibleState;
import de.muenchen.allg.itd51.wollmux.form.control.FormController;
import de.muenchen.allg.util.UnoConfiguration;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

/**
 * Controller of the document.
 */
public class TextDocumentController implements FormValueChangedListener, VisibilityChangedListener
{

  private static final Logger LOGGER = LoggerFactory.getLogger(TextDocumentController.class);

  private static final String FORMULAR = "Formular";

  private static final String FUNKTIONEN = "Funktionen";

  private static final String FENSTER = "Fenster";

  private TextDocumentModel model;

  private HashMap<Object, Object> functionContext;

  private DialogLibrary dialogLib;

  /**
   * Enthält die Funktionsbibliothek mit den globalen und dokumentlokalen Funktionen oder null, wenn
   * die Funktionsbilbiothek noch nicht benötigt wurde.
   */
  private FunctionLibrary functionLib;

  /**
   * The results of a simulation. It's initialized in {@link #startSimulation()}. If != null a
   * simulation has been started. {@link #stopSimulation()} stops a simulation and resets this
   * value.
   */
  private SimulationResults simulationResult = null;

  /**
   * Preview mode is set by default. If false, only the field names are displayed and not the field
   * values.
   */
  private boolean formFieldPreviewMode;

  private DialogLibrary globalDialogs;

  private FunctionLibrary globalFunctions;

  private FormModel formModel;

  /**
   * New controller.
   * 
   * @param model
   *          The model.
   * @param globalFunctions
   *          Global functions.
   * @param globalDialogs
   *          Global dialogs.
   */
  public TextDocumentController(TextDocumentModel model, FunctionLibrary globalFunctions, DialogLibrary globalDialogs)
  {
    this.model = model;
    this.formFieldPreviewMode = true;
    this.globalFunctions = globalFunctions;
    this.globalDialogs = globalDialogs;

    functionContext = new HashMap<>();

    parseInitialOverrideFragMap(getInitialOverrideFragMap());
  }

  public TextDocumentModel getModel()
  {
    return model;
  }

  @Override
  public synchronized String toString()
  {
    return "doc('" + getFrameController().getTitle() + "')";
  }

  /**
   * Get the frame controller of the document.
   *
   * @return The controller.
   */
  public FrameController getFrameController()
  {
    return new FrameController(model.doc);
  }

  /**
   * Add a print function to the document.
   *
   * @param functionName
   *          The name of the function to add.
   */
  public synchronized void addPrintFunction(String functionName)
  {
    model.addPrintFunction(functionName);
    model.updateLastTouchedByVersionInfo();

    try
    {
      getFrameController().getFrame().contextChanged();
    } catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
    }
  }

  /**
   * Remove a print function from the document.
   *
   * @param functionName
   *          The name of the function to remove.
   */
  public synchronized void removePrintFunction(String functionName)
  {
    model.removePrintFunction(functionName);
    model.updateLastTouchedByVersionInfo();

    try
    {
      getFrameController().getFrame().contextChanged();
    } catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
    }
  }

  /**
   * Get the library of global document functions.
   *
   * @return The library.
   */
  public synchronized FunctionLibrary getFunctionLibrary()
  {
    if (functionLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = model.getFormDescription().get(FORMULAR);
      } catch (NodeNotFoundException e)
      {
        LOGGER.trace("", e);
      }
      functionLib = FunctionFactory.parseFunctions(formConf, getDialogLibrary(), functionContext, globalFunctions);
    }
    return functionLib;
  }

  /**
   * Get the library of global and document dialog functions
   *
   * @return The library.
   */
  public synchronized DialogLibrary getDialogLibrary()
  {
    if (dialogLib == null)
    {
      ConfigThingy formConf = new ConfigThingy("");
      try
      {
        formConf = model.getFormDescription().get(FORMULAR);
      } catch (NodeNotFoundException e)
      {
        LOGGER.trace("", e);
      }
      dialogLib = DialogFactory.parseFunctionDialogs(formConf, globalDialogs, functionContext);
    }
    return dialogLib;
  }

  /**
   * Get the context of the document functions.
   *
   * @return The function context.
   */
  public synchronized Map<Object, Object> getFunctionContext()
  {
    return functionContext;
  }

  /**
   * Get the current form values. If a simulation has been started with {@link #startSimulation()}
   * these values are returned.
   *
   * Changes on the map have no effect.
   *
   * @return Map of field ID to field value.
   */
  public synchronized Map<String, String> getFormFieldValues()
  {
    if (simulationResult == null)
      return new HashMap<>(model.getFormFieldValuesMap());
    else
      return new HashMap<>(simulationResult.getFormFieldValues());
  }

  /**
   * Get the form description. Form adaptations are applied.
   * 
   * @return The form description.
   */
  public synchronized ConfigThingy getFormDescription()
  {
    ConfigThingy formDescription = model.getFormDescription();
    return applyFormularanpassung(formDescription);
  }

  /**
   * Wendet alle matchenden "Formularanpassung"-Abschnitte in der Reihenfolge ihres auftretends in
   * der wollmux,conf auf formularConf an und liefert das Ergebnis zurück. Achtung! Das
   * zurückgelieferte Objekt kann das selbe Objekt sein wie das übergebene.
   *
   * @param formularConf
   *          ein "WM" Knoten unterhalb dessen sich eine normale Formularbeschreibung befindet
   *          ("Formular" Knoten).
   */
  private ConfigThingy applyFormularanpassung(ConfigThingy formularConf)
  {
    ConfigThingy anpassungen = WollMuxFiles.getWollmuxConf().query("Formularanpassung", 1);
    if (anpassungen.count() == 0)
    {
      return formularConf;
    }

    try
    {
      ConfigThingy formularConfOld = formularConf;
      formularConf = formularConf.getFirstChild();
      if (!formularConf.getName().equals(FORMULAR))
      {
        return formularConfOld;
      }
    } catch (NodeNotFoundException x)
    {
      return formularConf;
    }

    for (ConfigThingy conf : anpassungen)
    {
      if (formAdaptationsApplies(formularConf, conf))
      {
        continue;
      }

      ConfigThingy formularAnpassung = conf.query(FORMULAR, 1);
      List<ConfigThingy> mergeForms = new ArrayList<>(2);
      mergeForms.add(formularConf);
      String title = "";
      try
      {
        title = formularConf.get("TITLE", 1).toString();
      } catch (Exception x)
      {
        LOGGER.trace("", x);
      }
      try
      {
        mergeForms.add(formularAnpassung.getFirstChild());
      } catch (NodeNotFoundException x)
      {
        LOGGER.trace("", x);
      }
      ConfigThingy buttonAnpassung = conf.query("Buttonanpassung");
      if (buttonAnpassung.count() == 0)
      {
        buttonAnpassung = null;
      }
      formularConf = TextDocumentModel.mergeFormDescriptors(mergeForms, buttonAnpassung, title);
    }

    ConfigThingy formularConfWithWM = new ConfigThingy("WM");
    formularConfWithWM.addChild(formularConf);
    return formularConfWithWM;
  }

  /**
   * Does all adaptations apply?
   *
   * @param formularConf
   *          The form.
   * @param conf
   *          The adaptations.
   * @return True if all adaptations from conf apply.
   */
  private boolean formAdaptationsApplies(ConfigThingy formularConf, ConfigThingy conf)
  {
    ConfigThingy matches = conf.query("Match", 1);
    for (ConfigThingy matchConf : matches)
    {
      for (ConfigThingy subMatchConf : matchConf)
      {
        if (!matches(formularConf, subMatchConf))
        {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Check if the configuration conf can be modified by removing nodes from it so that it matches
   * matchConf. Removing means replacing by its children.
   *
   * Note: only results on one level are used.
   *
   * @param conf
   *          The configuration to modify.
   * @param matchConf
   *          The configuration to match.
   * @return True if the configuration can be modified to match.
   */
  private static boolean matches(ConfigThingy conf, ConfigThingy matchConf)
  {
    ConfigThingy resConf = conf.query(matchConf.getName());
    if (resConf.count() == 0)
    {
      return false;
    }
    for (ConfigThingy subConf : resConf)
    {
      boolean match = true;
      for (ConfigThingy subMatchConf : matchConf)
      {
        if (!matches(subConf, subMatchConf))
        {
          match = false;
        }
      }

      if (match)
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Update commands.
   */
  public synchronized void updateDocumentCommands()
  {
    model.getDocumentCommands().update();
  }

  /**
   * Set a form value of the form model and document.
   *
   * @param id
   *          The name of the form element.
   * @param value
   *          The value of the form element.
   */
  public synchronized void addFormFieldValue(String id, String value)
  {
    setFormFieldValue(id, value);
    updateFormFields(id);
  }

  /**
   * Execute all functions until one returns with an non-empty String. This String is the action to
   * be executed on the document. Supported actions are:
   * <ul>
   * <li>noaction</li>
   * <li>allactions</li>
   * </ul>
   * As parameter for the functions user variables are possible
   * ({@link #getUserFieldMaster(String)}).
   *
   * @param funcs
   *          The functions to execute.
   * @return 0 if no actions should be executed, {@link Integer#MAX_VALUE} for all actions. -1 as
   *         default value for unknown action.
   */
  public int evaluateDocumentActions(Iterator<Function> funcs)
  {
    Values values = new MyValues();
    while (funcs.hasNext())
    {
      Function f = funcs.next();
      String res = f.getString(values);
      if (res.length() > 0)
      {
        if ("noaction".equals(res))
        {
          return 0;
        }
        if ("allactions".equals(res))
        {
          return Integer.MAX_VALUE;
        }
        LOGGER.error("Unbekannter Rückgabewert \"{}\" von Dokumentaktionen-Funktion", res);
      }
    }
    return -1;
  }

  /**
   * Insert a new input user text field with a TRAFO.
   *
   * @param range
   *          The position of the new text field.
   * @param trafoName
   *          The name of the TRAFO.
   * @param hint
   *          Mouse-over of the text field. If null no mouse-over is set.
   */
  public synchronized void addNewInputUserField(XTextRange range, String trafoName, String hint)
  {
    model.updateLastTouchedByVersionInfo();

    try
    {
      ConfigThingy conf = new ConfigThingy("WM");
      conf.add("FUNCTION").add(trafoName);
      String userFieldName = conf.stringRepresentation(false, '\'', false);

      XPropertySet master = getUserFieldMaster(userFieldName);
      if (master == null)
      {
        master = UNO.XPropertySet(UnoService.createService(UnoService.CSS_TEXT_FIELD_MASTER_USER, model.doc));
        UnoProperty.setProperty(master, UnoProperty.VALUE, Double.valueOf(0));
        UnoProperty.setProperty(master, UnoProperty.NAME, userFieldName);
      }

      XTextContent f = UNO.XTextContent(UnoService.createService(UnoService.CSS_TEXT_TEXT_FIELD_INPUT_USER, model.doc));
      UnoProperty.setProperty(f, UnoProperty.CONTENT, userFieldName);
      if (hint != null)
      {
        UnoProperty.setProperty(f, UnoProperty.HINT, hint);
      }
      range.getText().insertTextContent(range, f, true);
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Collect all data base and input user form fields of the document not surrounded by WollMux
   * commands.
   */
  public synchronized void collectNonWollMuxFormFields()
  {
    model.getIdToTextFieldFormFields().clear();
    model.getStaticTextFieldFormFields().clear();

    UnoCollection<XTextField> textFields = UnoCollection
        .getCollection(UNO.XTextFieldsSupplier(model.doc).getTextFields(), XTextField.class);
    if (textFields == null)
    {
      return;
    }
    for (XTextField textField : textFields)
    {
      try
      {
        XDependentTextField tf = UNO.XDependentTextField(textField);
        if (tf == null)
        {
          continue;
        }

        if (UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_INPUT_USER))
        {
          createTextFieldInputUser(tf);
        } else if (UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_DATABASE))
        {
          createTextFieldDataBase(tf);
        }
      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }
  }

  /**
   * Create a mail merge form field for a text field.
   * 
   * @param tf
   *          The text field.
   * @throws UnoHelperException
   *           Can't get a property.
   */
  private void createTextFieldDataBase(XDependentTextField tf) throws UnoHelperException
  {
    XPropertySet master = tf.getTextFieldMaster();
    String id = (String) UnoProperty.getProperty(master, UnoProperty.DATA_COLUMN_NAME);
    if (id != null && id.length() > 0)
    {
      if (!model.getIdToTextFieldFormFields().containsKey(id))
      {
        model.getIdToTextFieldFormFields().put(id, new ArrayList<FormField>());
      }

      List<FormField> formFields = model.getIdToTextFieldFormFields().get(id);
      formFields.add(FormFieldFactory.createDatabaseFormField(model.doc, tf));
    }
  }

  /**
   * Create a form field for a text field.
   * 
   * @param tf
   *          The text field.
   * @throws UnoHelperException
   *           Can't get a property.
   */
  private void createTextFieldInputUser(XDependentTextField tf) throws UnoHelperException
  {
    String varName = UnoProperty.getProperty(tf, UnoProperty.CONTENT).toString();
    String funcName = TextDocumentModel.getFunctionNameForUserFieldName(varName);

    if (funcName == null)
    {
      return;
    }

    XPropertySet master = getUserFieldMaster(varName);
    FormField f = FormFieldFactory.createInputUserFormField(model.doc, tf, master);
    Function func = getFunctionLibrary().get(funcName);

    if (func == null)
    {
      LOGGER.error("Die im Formularfeld verwendete Funktion '{}' ist nicht definiert.", funcName);
      return;
    }

    String[] pars = func.parameters();
    if (pars.length == 0)
    {
      model.getStaticTextFieldFormFields().add(f);
    }
    for (int i = 0; i < pars.length; i++)
    {
      String id = pars[i];
      if (id != null && id.length() > 0)
      {
        if (!model.getIdToTextFieldFormFields().containsKey(id))
          model.getIdToTextFieldFormFields().put(id, new ArrayList<FormField>());

        List<FormField> formFields = model.getIdToTextFieldFormFields().get(id);
        formFields.add(f);
      }
    }
  }

  /**
   * Get the initial values of all form fields. The value is defined if all form fields with the
   * same ID are unchanged or all fields without TRAFO have the same value. If there's no form field
   * the value from the stored data in the FormDescription is used.
   * 
   * Use this method only after {@link TextDocumentModel#setIDToFormFields(Map)} has been called.
   *
   * @return A mapping from field ID to initial value.
   */
  public synchronized Map<String, String> getIDToPresetValue()
  {
    HashMap<String, String> idToPresetValue = new HashMap<>();
    Set<String> ids = new HashSet<>(model.getFormFieldValuesMap().keySet());

    /*
     * initialize values map: If field without transformation for an id exists its value is taken.
     * If no such field exists the last value of the form values is taken.
     */
    for (String id : ids)
    {
      List<FormField> fields = new ArrayList<>();
      fields.addAll(Optional.ofNullable(model.getIdToFormFields().get(id)).orElse(Collections.emptyList()));
      fields.addAll(Optional.ofNullable(model.getIdToTextFieldFormFields().get(id)).orElse(Collections.emptyList()));

      String value = model.getFirstUntransformedValue(fields);
      if (value == null)
      {
        value = model.getFormFieldValuesMap().get(id);
      }
      if (value != null)
      {
        idToPresetValue.put(id, value);
      }
    }

    // remove inconsistent fields
    for (String id : ids)
    {
      String value = idToPresetValue.get(id);
      if (value != null)
      {
        boolean fieldValuesConsistent = fieldValuesConsistent(model.getIdToFormFields().get(id), idToPresetValue, value)
            && fieldValuesConsistent(model.getIdToTextFieldFormFields().get(id), idToPresetValue, value);
        if (!fieldValuesConsistent)
        {
          idToPresetValue.remove(id);
        }
      }
    }

    // mark fields without valid content as FISHY
    for (String id : ids)
    {
      if (!idToPresetValue.containsKey(id))
      {
        idToPresetValue.put(id, TextDocumentModel.FISHY);
      }
    }
    return idToPresetValue;
  }

  /**
   * Check if the values of some form fields are consistent with the provided parameters.
   *
   * @param fields
   *          List of form fields to check.
   * @param mapIdToValues
   *          The parameters for the TRFAOs of the form fields.
   * @param value
   *          The value of fields without transformation or for fields with TRAFOs using a single
   *          parameter.
   * @return True if all fields are consistent, false otherwise.
   */
  private boolean fieldValuesConsistent(List<FormField> fields, HashMap<String, String> mapIdToValues, String value)
  {
    if (fields == null)
    {
      return true;
    }
    Map<String, String> values = Optional.ofNullable(mapIdToValues).orElse(new HashMap<>());

    for (FormField field : fields)
    {
      String refValue = value;
      String trafoName = field.getTrafoName();
      if (trafoName != null)
      {
        if (field.singleParameterTrafo())
        {
          refValue = getTransformedValue(trafoName, value);
        } else
        {
          // abort if parameters are missing
          Optional<Function> func = Optional.ofNullable(getFunctionLibrary().get(trafoName));
          boolean missing = func
              .map(f -> Arrays.stream(f.parameters()).map(values::get).anyMatch(Objects::isNull)).orElse(false);
          if (missing)
          {
            return false;
          }

          refValue = getTransformedValue(trafoName, values);
        }
      }

      // compare values
      if (!field.getValue().equals(refValue))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Execute a TRAFO with the form values of the document or simulation.
   *
   * @param trafoName
   *          The name of the TRAFO. It has to be defined in the global or document function
   *          library.
   * @return The result of the TRAFO or an error String if the TRAFO is undefined.
   */
  public String getTransformedValue(String trafoName)
  {
    if (simulationResult == null)
    {
      return getTransformedValue(trafoName, model.getFormFieldValuesMap());
    }
    else
    {
      return getTransformedValue(trafoName, simulationResult.getFormFieldValues());
    }
  }

  /**
   * Execute a TRAFO with all parameters set to the given value.
   *
   * @param trafoName
   *          The name of the TRAFO. It has to be defined in the global or document function
   *          library.
   * @param value
   *          The value of all parameters.
   * @return The result of the TRAFO or an error String if the TRAFO is undefined. If trafoName is
   *         null value is returned.
   */
  public String getTransformedValue(String trafoName, String value)
  {
    String transformed = value;
    if (trafoName != null)
    {
      Function func = getFunctionLibrary().get(trafoName);
      if (func != null)
      {
        SimpleMap args = new SimpleMap();
        String[] pars = func.parameters();
        for (int i = 0; i < pars.length; i++)
          args.put(pars[i], value);
        transformed = func.getString(args);
      } else
      {
        transformed = L.m("<FEHLER: TRAFO '%1' nicht definiert>", trafoName);
        LOGGER.error("Die TRAFO '{}' ist nicht definiert.", trafoName);
      }
    }
    return transformed;
  }

  /**
   * Execute a TRAFO.
   *
   * @param trafoName
   *          The name of the TRAFO. It has to be defined in the global or document function
   *          library.
   * @param mapIdToValues
   *          Provides the values used by the TRAFO.
   * @return The result of the TRAFO or an error String if the TRAFO is undefined.
   */
  private String getTransformedValue(String trafoName, Map<String, String> mapIdToValues)
  {
    Function func = getFunctionLibrary().get(trafoName);
    if (func != null)
    {
      SimpleMap args = new SimpleMap();
      String[] pars = func.parameters();
      for (int i = 0; i < pars.length; i++)
        args.put(pars[i], mapIdToValues.get(pars[i]));
      return func.getString(args);
    } else
    {
      LOGGER.error("Die TRAFO '{}' ist nicht definiert.", trafoName);
      return L.m("<FEHLER: TRAFO '%1' nicht definiert>", trafoName);
    }
  }

  /**
   * Mark the document as a form. This state is persisted in the document.
   */
  public synchronized void markAsFormDocument()
  {
    model.updateLastTouchedByVersionInfo();
    model.setType("formDocument");
    model.getPersistentData().setData(DataID.SETTYPE, "formDocument");
  }

  /**
   * Remove the commands "insertFormValue", "setGroups", "setType formDocument" and "form" from the
   * document. Additionally the form description and form values are removed.
   */
  public synchronized void deForm()
  {
    model.updateLastTouchedByVersionInfo();

    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(model.doc);
    UnoDictionary<XTextContent> bookmarks = UnoDictionary.create(bmSupp.getBookmarks(), XTextContent.class);
    for (Entry<String, XTextContent> bookmark : bookmarks.entrySet())
    {
      try
      {
        if (TextDocumentModel.BOOKMARK_KILL_PATTERN.matcher(bookmark.getKey()).matches())
        {
          bookmark.getValue().getAnchor().getText().removeTextContent(bookmark.getValue());
        }

      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }

    model.getPersistentData().removeData(DataID.FORMULARBESCHREIBUNG);
    model.getPersistentData().removeData(DataID.FORMULARWERTE);
  }

  /**
   * Get the function for proposing file names.
   *
   * @return The configuration of the function or null if no file names are proposed.
   */
  public synchronized ConfigThingy getFilenameGeneratorFunc()
  {
    String func = model.getPersistentData().getData(DataID.FILENAMEGENERATORFUNC);
    if (func == null)
    {
      return null;
    }
    try
    {
      return new ConfigThingy("func", func).getFirstChild();
    } catch (Exception e)
    {
      return null;
    }
  }

  /**
   * Set a function for proposing a file name.
   *
   * @param c
   *          The configuration of the function or null if no filename should be proposed.
   */
  public synchronized void setFilenameGeneratorFunc(ConfigThingy c)
  {
    model.updateLastTouchedByVersionInfo();
    if (c == null)
      model.getPersistentData().removeData(DataID.FILENAMEGENERATORFUNC);
    else
      model.getPersistentData().setData(DataID.FILENAMEGENERATORFUNC, c.stringRepresentation());
  }

  /**
   * Remove all non-WollMux book marks from the document.
   */
  public synchronized void removeNonWMBookmarks()
  {
    model.updateLastTouchedByVersionInfo();

    XBookmarksSupplier bmSupp = UNO.XBookmarksSupplier(model.doc);
    UnoDictionary<XTextContent> bookmarks = UnoDictionary.create(bmSupp.getBookmarks(), XTextContent.class);
    for (Entry<String, XTextContent> bookmark : bookmarks.entrySet())
    {
      try
      {
        if (!TextDocumentModel.WOLLMUX_BOOKMARK_PATTERN.matcher(bookmark.getKey()).matches())
        {
          bookmark.getValue().getAnchor().getText().removeTextContent(bookmark.getValue());
        }

      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }
  }

  /**
   * Flush the persistent data.
   */
  public synchronized void flushPersistentData()
  {
    model.getPersistentData().flush();
  }

  /**
   * Calls {@link #insertMailMergeField(String, XTextRange) with the current cursor position.
   *
   * @param fieldId
   *          The column name of the data base on which this field depends
   */
  public synchronized void insertMailMergeFieldAtCursorPosition(String fieldId)
  {
    model.updateLastTouchedByVersionInfo();
    insertMailMergeField(fieldId, model.getViewCursor());
  }

  /**
   * Insert a new mail merge field. It's initial value is "" if the data base doesn't have a value.
   *
   * @param fieldId
   *          The column name of the data base on which this field depends
   * @param range
   *          The position of the mail merge field.
   */
  public void insertMailMergeField(String fieldId, XTextRange range)
  {
    model.updateLastTouchedByVersionInfo();

    if (fieldId == null || fieldId.length() == 0 || range == null)
    {
      return;
    }
    try
    {
      XDependentTextField field = UNO
          .XDependentTextField(UnoService.createService(UnoService.CSS_TEXT_TEXT_FIELD_DATABASE, model.doc));
      XPropertySet master = UNO
          .XPropertySet(UnoService.createService(UnoService.CSS_TEXT_FIELD_MASTER_DATABASE, model.doc));
      UnoProperty.setProperty(master, UnoProperty.DATA_BASE_NAME, "DataBase");
      UnoProperty.setProperty(master, UnoProperty.DATA_TABLE_NAME, "Table");
      UnoProperty.setProperty(master, UnoProperty.DATA_COLUMN_NAME, fieldId);
      if (!formFieldPreviewMode)
        UnoProperty.setProperty(field, UnoProperty.CONTENT, "<" + fieldId + ">");
      field.attachTextFieldMaster(master);

      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      cursor.getText().insertTextContent(cursor, field, true);
      cursor.collapseToEnd();

      // initialize with empty content
      if (!model.getFormFieldValuesMap().containsKey(fieldId))
      {
        setFormFieldValue(fieldId, "");
      }

      // publish form field
      if (!model.getIdToTextFieldFormFields().containsKey(fieldId))
      {
        model.getIdToTextFieldFormFields().put(fieldId, new ArrayList<FormField>());
      }
      List<FormField> formFields = model.getIdToTextFieldFormFields().get(fieldId);
      formFields.add(FormFieldFactory.createDatabaseFormField(model.doc, field));

      // update field view
      updateFormFields(fieldId);
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Insert a "Next record" field at the current cursor position. It is required for mail merge.
   */
  public synchronized void insertNextDatasetFieldAtCursorPosition()
  {
    model.updateLastTouchedByVersionInfo();
    XTextRange range = model.getViewCursor();
    try
    {
      XDependentTextField field = UNO
          .XDependentTextField(UnoService.createService(UnoService.CSS_TEXT_TEXT_FIELD_DATABASE_NEXT_SET, model.doc));
      UnoProperty.setProperty(field, UnoProperty.DATA_BASE_NAME, "DataBaseName");
      UnoProperty.setProperty(field, UnoProperty.DATA_TABLE_NAME, "DataTableName");
      UnoProperty.setProperty(field, UnoProperty.DATA_COMMAND_TYPE, com.sun.star.sdb.CommandType.TABLE);
      UnoProperty.setProperty(field, UnoProperty.CONDITION, "true");

      XTextCursor cursor = range.getText().createTextCursorByRange(range);
      cursor.getText().insertTextContent(cursor, field, true);
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Save the meta data of a mail merge in the persistent data.
   * 
   * @param conf
   *          A valid mail merge configuration or an empty configuration. An empty configuration
   *          deletes the persistent data.
   */
  public synchronized void setMailmergeConfig(ConfigThingy conf)
  {
    model.updateLastTouchedByVersionInfo();

    model.setMailmergeConf(new ConfigThingy("Seriendruck"));
    for (Iterator<ConfigThingy> iter = conf.iterator(); iter.hasNext();)
    {
      ConfigThingy c = new ConfigThingy(iter.next());
      model.getMailmergeConf().addChild(c);
    }
    ConfigThingy wm = new ConfigThingy("WM");
    wm.addChild(model.getMailmergeConf());
    if (model.getMailmergeConf().count() > 0)
      model.getPersistentData().setData(DataID.SERIENDRUCK, wm.stringRepresentation());
    else
      model.getPersistentData().removeData(DataID.SERIENDRUCK);
  }

  /**
   * In preview mode sets the form values form the persistent data in the document. In non-preview
   * mode the field names are set. TRAFOs use the function library. The modified-state of the
   * document is set.
   * 
   * In a simulation the document isn't modified.
   * 
   * @param fieldId
   *          The id of the field to update.
   */
  public void updateFormFields(String fieldId)
  {
    if (formFieldPreviewMode)
    {
      String value = model.getFormFieldValuesMap().get(fieldId);
      if (simulationResult != null)
        value = simulationResult.getFormFieldValues().get(fieldId);
      if (value == null)
      {
        value = "";
      }
      setFormFields(fieldId, value, true);
    } else
    {
      setFormFields(fieldId, "<" + fieldId + ">", false);
    }
    if (simulationResult == null)
    {
      model.setDocumentModified(true);
    }
  }

  /**
   * Show or hide all visibility elements within a group.
   * 
   * @param groupId
   *          The id of the group.
   * @param visible
   *          If true shows the elements, otherwise hides the elements.
   */
  public void setVisibleState(String groupId, boolean visible)
  {
    try
    {
      Map<String, Boolean> groupState;
      if (simulationResult != null)
      {
        groupState = simulationResult.getGroupsVisibilityState();
      } else
      {
        groupState = model.getMapGroupIdToVisibilityState();
      }

      groupState.put(groupId, visible);

      VisibilityElement firstChangedElement = null;

      // update visibilities
      for (VisibilityElement visibleElement : model.getDocumentCommands().getSetGroups())
      {
        Set<String> groups = visibleElement.getGroups();
        if (!groups.contains(groupId))
        {
          continue;
        }

        // get new visibility state
        boolean setVisible = groups.stream().map(groupState::get).filter(Objects::nonNull).reduce(Boolean::logicalAnd)
            .orElse(true);

        /*
         * remember first changed visibility to set the cursor to its position later cursor can't be
         * set in an invisible area.
         */
        if (setVisible != visibleElement.isVisible() && firstChangedElement == null)
        {
          firstChangedElement = visibleElement;
          if (firstChangedElement.isVisible())
          {
            model.focusRangeStart(visibleElement);
          }
        }

        setVisibilityChecked(visibleElement, setVisible);
      }

      // reset cursor to first changed element
      if (firstChangedElement != null && firstChangedElement.isVisible())
      {
        model.focusRangeStart(firstChangedElement);
      }
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Set a visibility state but handle manually deleted content.
   * 
   * @param visibleElement
   *          The visibility element to change.
   * @param setVisible
   *          The new state.
   */
  private void setVisibilityChecked(VisibilityElement visibleElement, boolean setVisible)
  {
    try
    {
      visibleElement.setVisible(setVisible);
    } catch (RuntimeException e)
    {
      // handle manually deleted content
      LOGGER.trace("", e);
    }
  }

  /**
   * In preview mode all form values of the persistend data are showen in the document. TRAFOs are
   * applied if necessary. In non-preview mode only field names are shown.
   */
  private void updateAllFormFields()
  {
    for (String fieldId : model.getAllFieldIDs())
    {
      updateFormFields(fieldId);
    }
  }

  /**
   * Set all form fields with an id to a new value.
   *
   * @param fieldId
   *          The id of the fields.
   * @param value
   *          The new value of the fields.
   * @param applyTrafo
   *          If true TRAFOs are applied.
   */
  private void setFormFields(String fieldId, String value, boolean applyTrafo)
  {
    setFormFields(model.getIdToFormFields().get(fieldId), value, applyTrafo, false);
    setFormFields(model.getIdToTextFieldFormFields().get(fieldId), value, applyTrafo, true);
    setFormFields(model.getStaticTextFieldFormFields(), value, applyTrafo, true);
  }

  /**
   * Set a new value for all the given form fields and apply TRAFOs. If a simulation has been
   * started with {@link #startSimulation()} the values aren't really set.
   *
   * @param formFields
   *          The fields to modify. If null nothing is done.
   * @param value
   *          The new value.
   * @param applyTrafo
   *          If true, TRAFOs are applied.
   * @param useKnownFormValues
   *          If true the TRAFOs use the real value of the fields. If false all fields get the given
   *          value.
   */
  private void setFormFields(List<FormField> formFields, String value, boolean applyTrafo, boolean useKnownFormValues)
  {
    if (formFields == null)
    {
      return;
    }

    if (simulationResult == null)
      model.updateLastTouchedByVersionInfo();

    for (FormField field : formFields)
    {
      try
      {
        String result = value;
        String trafoName = field.getTrafoName();
        if (trafoName != null && applyTrafo)
        {
          if (useKnownFormValues)
          {
            result = getTransformedValue(trafoName);
          }
          else
          {
            result = getTransformedValue(trafoName, value);
          }
        }

        if (simulationResult == null)
        {
          field.setValue(result);
        }
        else
        {
          simulationResult.setFormFieldContent(field, result);
        }
      } catch (RuntimeException e)
      {
        // manually deleted document content
        LOGGER.trace("", e);
      }
    }
  }

  /**
   * Start or stop the preview mode. In preview mode the values are visible. In non-preview mode the
   * field names are visible. This is required by mail merge.
   *
   * @param previewMode
   *          True starts the preview mode, false stops it.
   */
  public synchronized void setFormFieldsPreviewMode(boolean previewMode)
  {
    this.formFieldPreviewMode = previewMode;
    updateAllFormFields();
    cleanupGarbageOfUnreferencedAutofunctions();
  }

  /**
   * Clear form fields.
   */
  public synchronized void clearFormFields()
  {
    model.clearFormFieldValues();
  }

  /**
   * Start a simulation. Changes to fields are only simulated and doesn't modify to document until
   * {@link #stopSimulation()} is called. This feature is required by mail merge.
   */
  public synchronized void startSimulation()
  {
    if (model == null || model.doc == null)
    {
      LOGGER.error("{} startSimulation: model is NULL.", this.getClass().getSimpleName());
      return;
    }

    simulationResult = new SimulationResults();
    simulationResult.setFormFieldValues(model.getFormFieldValuesMap());
    simulationResult.setGroupsVisibilityState(model.getMapGroupIdToVisibilityState());

    // get form field values and publish them to the simulation
    HashSet<FormField> ffs = new HashSet<>();
    for (List<FormField> l : model.getIdToFormFields().values())
      for (FormField ff : l)
        ffs.add(ff);
    for (List<FormField> l : model.getIdToTextFieldFormFields().values())
      for (FormField ff : l)
        ffs.add(ff);
    ffs.addAll(model.getStaticTextFieldFormFields());
    for (FormField ff : ffs)
      simulationResult.setFormFieldContent(ff, ff.getValue());

    FormController formController = DocumentManager.getDocumentManager().getFormController(model.doc);

    if (formController != null)
    {
      for (Map.Entry<String, String> values : simulationResult.getFormFieldValues().entrySet())
      {
        if (formController.hasFieldId(values.getKey()))
        {
          formController.setValue(values.getKey(), values.getValue(), null);
        }
      }
    }
  }

  /**
   * Ends a simulation startet with {@link #startSimulation()}.
   *
   * @return The results of the simulation or null if no simulation was started.
   */
  public synchronized SimulationResults stopSimulation()
  {
    SimulationResults r = simulationResult;
    simulationResult = null;
    return r;
  }

  /**
   * Delete unused auto-functions from function library, form description and unnecessary
   * TextFieldMasters. DocumentModified-State doesn't change.
   */
  private void cleanupGarbageOfUnreferencedAutofunctions()
  {
    boolean modified = model.isDocumentModified();

    // collect all TRAFOs
    HashSet<String> usedFunctions = new HashSet<>();
    List<List<FormField>> allFields = new ArrayList<>(model.getIdToFormFields().values());
    allFields.addAll(model.getIdToTextFieldFormFields().values());
    allFields.add(model.getStaticTextFieldFormFields());
    allFields.stream().flatMap(List::stream).map(FormField::getTrafoName).filter(Objects::nonNull)
        .forEach(usedFunctions::add);

    // delete auto-functions from function library and form description
    FunctionLibrary funcLib = getFunctionLibrary();
    funcLib.getFunctionNames().stream().filter(Objects::nonNull)
        .filter(name -> name.startsWith(TextDocumentModel.AUTOFUNCTION_PREFIX))
        .filter(name -> !usedFunctions.contains(name)).forEach(funcLib::remove);

    ConfigThingy functions = model.getFormDescription().query(FORMULAR).query(FUNKTIONEN);
    for (ConfigThingy funcs : functions)
    {
      for (Iterator<ConfigThingy> iterator = funcs.iterator(); iterator.hasNext();)
      {
        String name = iterator.next().getName();
        if (name == null || !name.startsWith(TextDocumentModel.AUTOFUNCTION_PREFIX) || usedFunctions.contains(name))
        {
          continue;
        }
        iterator.remove();
      }
    }
    storeCurrentFormDescription();

    createUnusedTextFieldMaster(usedFunctions);

    model.setDocumentModified(modified);
  }

  /**
   * Delete unused TextFieldMasters of InputUser-Textfields.
   *
   * @param usedFunctions
   *          Used TextFields.
   */
  private void createUnusedTextFieldMaster(HashSet<String> usedFunctions)
  {
    UnoDictionary<XComponent> masters = UnoDictionary.create(UNO.XTextFieldsSupplier(model.doc).getTextFieldMasters(),
        XComponent.class);
    String prefix = "com.sun.star.text.FieldMaster.User.";
    for (Entry<String, XComponent> master : masters.entrySet())
    {
      if (master == null || !master.getKey().startsWith(prefix))
      {
        continue;
      }
      String varName = master.getKey().substring(prefix.length());
      String trafoName = TextDocumentModel.getFunctionNameForUserFieldName(varName);
      if (trafoName != null && !usedFunctions.contains(trafoName))
      {
        try
        {
          master.getValue().dispose();
        } catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
      }
    }
  }

  /**
   * Set a new form description.
   *
   * @param conf
   *          A ConfigTHingy with "Formular"-children. If null the description is deleted.
   */
  public synchronized void setFormDescription(ConfigThingy conf)
  {
    if (conf != null)
      model.setFormularConf(conf);
    else
      model.setFormularConf(new ConfigThingy("WM"));
    storeCurrentFormDescription();
    model.setDocumentModified(true);
  }

  /**
   * Set a new value for a form field and store the persistend data. The new value is visible after
   * a call of {@link #updateFormFields(String)}.
   *
   * If simulation has beeen startet with {@link #startSimulation()}, the persistend data isn't
   * modified.
   * 
   * @param fieldId
   *          The id of the field.
   * @param value
   *          The new value of the field. If null the field is deleted from the persistend data.
   */
  public void setFormFieldValue(String fieldId, String value)
  {
    if (simulationResult == null)
    {
      model.updateLastTouchedByVersionInfo();
      if (value == null)
      {
        model.getFormFieldValues().remove(fieldId);
      }
      else
      {
        model.getFormFieldValues().put(fieldId, value);
      }
      model.getPersistentData().setData(DataID.FORMULARWERTE, getFormFieldValuesString());
    } else
    {
      simulationResult.setFormFieldValue(fieldId, value);
    }
  }

  /**
   * Serialsise the current form field values.
   *
   * @return A ConfigThingy-String of the values.
   */
  private String getFormFieldValuesString()
  {
    ConfigThingy werte = new ConfigThingy("WM");
    ConfigThingy formwerte = new ConfigThingy("Formularwerte");
    werte.addChild(formwerte);
    for (Map.Entry<String, String> ent : model.getFormFieldValues().entrySet())
    {
      String key = ent.getKey();
      String value = ent.getValue();
      if (key != null && value != null)
      {
        ConfigThingy entry = new ConfigThingy("");
        ConfigThingy cfID = new ConfigThingy("ID");
        cfID.add(key);
        ConfigThingy cfVALUE = new ConfigThingy("VALUE");
        cfVALUE.add(value);
        entry.addChild(cfID);
        entry.addChild(cfVALUE);
        formwerte.addChild(entry);
      }
    }

    return werte.stringRepresentation();
  }

  /**
   * Save all WollMux information (window attributes, visibility, functions, form description) in
   * the persistend data of the document. Deletes the persistend data if there's no information.
   */
  public void storeCurrentFormDescription()
  {
    model.updateLastTouchedByVersionInfo();

    ConfigThingy conf = model.getFormDescription();
    try
    {
      if ((conf.query(FENSTER).count() > 0 && conf.get(FENSTER).count() > 0)
          || (conf.query("Sichtbarkeit").count() > 0 && conf.get("Sichtbarkeit").count() > 0)
          || (conf.query(FUNKTIONEN).count() > 0 && conf.get(FUNKTIONEN).count() > 0))
      {
        model.getPersistentData().setData(DataID.FORMULARBESCHREIBUNG, conf.stringRepresentation());
      }
      else
      {
        model.getPersistentData().removeData(DataID.FORMULARBESCHREIBUNG);
      }
    } catch (NodeNotFoundException e)
    {
      LOGGER.error(L.m("Dies kann nicht passieren."), e);
    }
  }

  /**
   * Replace current selection with a new form field. The form field can be used immediatly after
   * the method returned.
   *
   * If nothing is selected, nothing is done.
   * 
   * @param trafoConf
   *          The TRAFO of the for form field. Can be null. A TRAFO definition is in the form
   *          "Name(FUNCTION_DEFINITION)", where Name is a valid identifier and FUNCTION_DEFINITION
   *          a valid parameter for
   *          {@link de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}.
   *          The first child of FUNCTION_DEFINITION has to be a valid function name like "AND".
   * @param hint
   *          Tooltip of the new form field. Can be null.
   */
  public synchronized void replaceSelectionWithTrafoField(ConfigThingy trafoConf, String hint)
  {
    String trafoName = addLocalAutofunction(trafoConf);

    if (trafoName != null)
    {
      try
      {
        addNewInputUserField(model.getViewCursor(), trafoName, hint);
        collectNonWollMuxFormFields();

        // init all referenced fields
        Function f = getFunctionLibrary().get(trafoName);
        String[] fieldIds = new String[] {};
        if (f != null)
        {
          fieldIds = f.parameters();
        }
        for (int i = 0; i < fieldIds.length; i++)
        {
          String fieldId = fieldIds[i];
          // init with empty string if necessary
          if (!model.getFormFieldValues().containsKey(fieldId))
          {
            setFormFieldValue(fieldId, "");
          }
          updateFormFields(fieldId);
        }

        cleanupGarbageOfUnreferencedAutofunctions();
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
      }
    }
  }

  /**
   * Update the current form description with the definition of the command. The command is deleted
   * and removed.
   *
   * @param formCmd
   *          The command.
   */
  public synchronized void addToCurrentFormDescription(DocumentCommand.Form formCmd)
  {
    XTextRange range = formCmd.getTextCursor();

    XTextContent annotationField = UNO.XTextContent(TextDocumentModel.findAnnotationFieldRecursive(range));
    if (annotationField == null)
    {
      throw new ConfigurationErrorException(L.m("Die zugehörige Notiz mit der Formularbeschreibung fehlt."));
    }

    Object content = Utils.getProperty(annotationField, "Content");
    if (content == null)
    {
      throw new ConfigurationErrorException(
          L.m("Die zugehörige Notiz mit der Formularbeschreibung kann nicht gelesen werden."));
    }

    TextDocumentModel.addToFormDescription(model.getFormDescription(), content.toString());
    storeCurrentFormDescription();

    // delete note and content of the book mark.
    formCmd.setTextRangeString("");
  }

  /**
   * Update a trafo definition. Subsquent calls to {@link #setFormFieldValue(String, String)} use
   * the new definition.
   *
   * @param trafoName
   *          The name of the trafo.
   * @param trafoConf
   *          A definition of function in the form "Name(FUNCTION_DEFINITION)", where Name is a
   *          valid identifier and FUNCTION_DEFINITION a valid parameter for
   *          {@link de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}.
   *          The first child of FUNCTION_DEFINITION has to be a valid function name like "AND".
   * @throws UnavailableException
   *           The TRAFO with this name can't be modified.
   */
  public synchronized void setTrafo(String trafoName, ConfigThingy trafoConf)
      throws UnavailableException
  {
    ConfigThingy func;
    try
    {
      func = model.getFormDescription().query(FORMULAR).query(FUNKTIONEN).query(trafoName, 2).getLastChild();
    } catch (NodeNotFoundException e)
    {
      throw new UnavailableException(e);
    }

    FunctionLibrary funcLib = getFunctionLibrary();
    Function function = FunctionFactory.parseChildren(trafoConf, funcLib, getDialogLibrary(), getFunctionContext());
    funcLib.add(trafoName, function);

    // remove children of func, so that we can reset them later
    for (Iterator<ConfigThingy> iter = func.iterator(); iter.hasNext();)
    {
      iter.next();
      iter.remove();
    }

    for (ConfigThingy f : trafoConf)
    {
      func.addChild(new ConfigThingy(f));
    }

    storeCurrentFormDescription();

    // The new function can depend on other IDs. So we have update the dependencies.
    collectNonWollMuxFormFields();
    updateAllFormFields();
  }

  /**
   * Add a new function with a generated name. Register it in the library so it can be used
   * immediatly.
   *
   * The name is in the form PREFIX_currentTimeInMillis. It's garanteed to be unique.
   *
   * @param funcConf
   *          A definition of function in the form "Name(FUNCTION_DEFINITION)", where Name is a
   *          valid identifier and FUNCTION_DEFINITION a valid parameter for
   *          {@link de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory#parse(ConfigThingy, FunctionLibrary, DialogLibrary, Map)}.
   *          The first child of FUNCTION_DEFINITION has to be a valid function name like "AND".
   * 
   * @return The name of the function or null if the definition is erroneous.
   */
  private String addLocalAutofunction(ConfigThingy funcConf)
  {
    FunctionLibrary funcLib = getFunctionLibrary();
    DialogLibrary dLib = getDialogLibrary();
    Map<Object, Object> context = getFunctionContext();
    // create unique name
    Set<String> currentFunctionNames = funcLib.getFunctionNames();
    String name = null;
    for (int i = 0; name == null || currentFunctionNames.contains(name); ++i)
      name = TextDocumentModel.AUTOFUNCTION_PREFIX + System.currentTimeMillis() + "_" + i;

    try
    {
      funcLib.add(name, FunctionFactory.parseChildren(funcConf, funcLib, dLib, context));

      ConfigThingy betterNameFunc = new ConfigThingy(name);
      for (ConfigThingy func : funcConf)
      {
        betterNameFunc.addChild(func);
      }
      model.getFunktionenConf().addChild(betterNameFunc);

      storeCurrentFormDescription();
      return name;
    } catch (ConfigurationErrorException e)
    {
      LOGGER.error("", e);
      return null;
    }
  }

  /**
   * Get the TextFieldMaster for a requested field.
   *
   * @param userFieldName
   *          The request field
   * @return The TextFieldMaster or null if no such field exists.
   */
  private XPropertySet getUserFieldMaster(String userFieldName)
  {
    UnoDictionary<XPropertySet> masters = UnoDictionary.create(UNO.XTextFieldsSupplier(model.doc).getTextFieldMasters(),
        XPropertySet.class);
    String elementName = "com.sun.star.text.FieldMaster.User." + userFieldName;
    if (masters.containsKey(elementName))
    {
      try
      {
        return masters.get(elementName);
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
      }
    }
    return null;
  }

  /**
   * Provide some values. The values are defined by a namespace an a name separated by a /. The
   * following namespaces are supported.
   * <ul>
   * <li>"User/" Values of user variables({@link #getUserFieldMaster(String)}</li>
   * </ul>
   */
  private class MyValues implements Values
  {
    public static final int MYVALUES_NAMESPACE_UNKNOWN = 0;

    public static final int MYVALUES_NAMESPACE_USER = 1;

    @Override
    public String getString(String id)
    {
      if (namespace(id) == MYVALUES_NAMESPACE_USER)
      {
        return getStringUser(id);
      } else
      {
        return "";
      }
    }

    @Override
    public boolean getBoolean(String id)
    {
      if (namespace(id) == MYVALUES_NAMESPACE_USER)
      {
        return getBooleanUser(id);
      } else
      {
        return false;
      }
    }

    @Override
    public boolean hasValue(String id)
    {
      if (namespace(id) == MYVALUES_NAMESPACE_USER)
      {
        return hasValueUser(id);
      } else
      {
        return false;
      }
    }

    private int namespace(String id)
    {
      if (id.startsWith("User/"))
      {
        return MYVALUES_NAMESPACE_USER;
      }
      return MYVALUES_NAMESPACE_UNKNOWN;
    }

    private String getStringUser(String id)
    {
      try
      {
        id = id.substring(id.indexOf('/') + 1);
        return UnoProperty.getProperty(getUserFieldMaster(id), UnoProperty.CONTENT).toString();
      } catch (Exception x)
      {
        return "";
      }
    }

    private boolean getBooleanUser(String id)
    {
      return "true".equalsIgnoreCase(getStringUser(id));
    }

    private boolean hasValueUser(String id)
    {
      try
      {
        id = id.substring(id.indexOf('/') + 1);
        return getUserFieldMaster(id) != null;
      } catch (Exception x)
      {
        return false;
      }
    }
  }

  /**
   * Parse an OverrideFrag configuration like
   *
   * <pre>
   * overrideFrag(
   *   (FRAG_ID 'A' NEW_FRAG_ID 'B')
   *   (FRAG_ID 'C' NEW_FRAG_ID 'D')
   *   ...
   * )
   * </pre>
   *
   * Initilizes {@link #overrideFragMap}.
   *
   * @param iniinitialOverrideFragMap
   *          The configuration.
   */
  private void parseInitialOverrideFragMap(ConfigThingy initialOverrideFragMap)
  {
    for (ConfigThingy conf : initialOverrideFragMap)
    {
      String oldFragId;
      try
      {
        oldFragId = conf.get("FRAG_ID").toString();
      } catch (NodeNotFoundException x)
      {
        LOGGER.error(L.m(
            "FRAG_ID Angabe fehlt in einem Eintrag der %1: %2\n"
                + "Vielleicht haben Sie die Klammern um (FRAG_ID 'A' NEW_FRAG_ID 'B') vergessen?",
            TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE, conf.stringRepresentation()));
        continue;
      }

      String newFragId = conf.getString("NEW_FRAG_ID", "");

      try
      {
        model.setOverrideFrag(oldFragId, newFragId);
      } catch (OverrideFragChainException x)
      {
        LOGGER.error(
            L.m("Fehlerhafte Angabe in %1: %2", TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE, conf.stringRepresentation()),
            x);
      }
    }
  }

  /**
   * Get the personal OverrideFrag list of the sender.
   *
   * @return The configuration of the OverrideFrag list.
   */
  private ConfigThingy getInitialOverrideFragMap()
  {
    ConfigThingy overrideFragConf = new ConfigThingy("overrideFrag");

    String overrideFragDbSpalte = null;
    ConfigThingy overrideFragDbSpalteConf = WollMuxFiles.getWollmuxConf()
        .query(TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE, 1);
    try
    {
      overrideFragDbSpalte = overrideFragDbSpalteConf.getLastChild().toString();
    } catch (NodeNotFoundException x)
    {
      // keine OVERRIDE_FRAG_DB_SPALTE Direktive gefunden
      overrideFragDbSpalte = "";
    }

    if (!overrideFragDbSpalte.isEmpty())
    {
      try
      {
        Dataset ds = DatasourceJoinerFactory.getDatasourceJoiner().getSelectedDatasetTransformed();
        String value = ds.get(overrideFragDbSpalte);
        if (value == null)
        {
          value = "";
        }
        overrideFragConf = new ConfigThingy("overrideFrag", value);
      } catch (DatasetNotFoundException e)
      {
        LOGGER
            .info(L.m("Kein Absender ausgewählt => %1 bleibt wirkungslos", TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE));
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error(L.m("%2 spezifiziert Spalte '%1', die nicht vorhanden ist", overrideFragDbSpalte,
            TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE), e);
      } catch (IOException | SyntaxErrorException x)
      {
        LOGGER.error(
            L.m("Fehler beim Parsen der %2 '%1'", overrideFragDbSpalte, TextDocumentModel.OVERRIDE_FRAG_DB_SPALTE), x);
      }
    }

    return overrideFragConf;
  }

  /**
   * Get a model of the form.
   *
   * @return The model.
   * @throws FormModelException
   *           The description of the model is invalid.
   */
  public FormModel getFormModel() throws FormModelException
  {
    if (formModel == null)
    {
      try
      {
        ConfigThingy formConf = getFormDescription().get(FORMULAR);
        formModel = new FormModel(formConf, getWindowTitle(), getFunctionContext(), getFunctionLibrary(),
            getDialogLibrary(), getIDToPresetValue());
        formModel.addFormModelChangedListener(this, true);
        formModel.addVisibilityChangedListener(this, true);
      } catch (NodeNotFoundException e)
      {
        throw new FormModelException(L.m("Kein Abschnitt 'Formular' in der Formularbeschreibung vorhanden"));
      }
    }
    return formModel;
  }

  /**
   * Get the controller of the form.
   *
   * @return The controller.
   * @throws FormModelException
   *           Invalid form description.
   */
  public FormController getFormController() throws FormModelException
  {
    FormController formController = DocumentManager.getDocumentManager().getFormController(getModel().doc);
    if (formController == null)
    {
      ConfigThingy formFensterConf;
      try
      {
        formFensterConf = WollMuxFiles.getWollmuxConf().query(FENSTER).query(FORMULAR).getLastChild();
      } catch (NodeNotFoundException x)
      {
        formFensterConf = new ConfigThingy("");
      }
      formController = new FormController(getFormModel(), formFensterConf, this);
      DocumentManager.getDocumentManager().setFormController(getModel().doc, formController);
    }
    return formController;
  }

  /**
   * Get the title of the window.
   *
   * @return The title or null, if the title is unknown.
   */
  public String getWindowTitle()
  {
    try
    {
      XFrame frame = UNO.XModel(getModel().doc).getCurrentController().getFrame();
      String frameTitle = (String) UnoProperty.getProperty(frame, UnoProperty.TITLE);
      frameTitle = UNO.stripOpenOfficeFromWindowName(frameTitle);
      return frameTitle;
    } catch (Exception x)
    {
      return null;
    }
  }

  /**
   * Get the attribute {@link UnoProperty#OO_SETUP_FACTORY_WINDOW_ATTRIBUTES} of the configuration
   *
   * {@link #setDefaultWindowAttributes(String)}
   *
   * @return The attribute value.
   */
  public String getDefaultWindowAttributes()
  {
    try
    {
      return UnoConfiguration.getConfiguration("/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument",
          UnoProperty.OO_SETUP_FACTORY_WINDOW_ATTRIBUTES).toString();
    } catch (UnoHelperException e)
    {
      return null;
    }
  }

  /**
   * Set the attribute {@link UnoProperty#OO_SETUP_FACTORY_WINDOW_ATTRIBUTES} of the configuration
   * to the given value.
   *
   * {@link #getDefaultWindowAttributes()}
   *
   * @param value
   *          The new value.
   */
  public void setDefaultWindowAttributes(String value)
  {
    try
    {
      UnoConfiguration.setConfiguration("/org.openoffice.Setup/Office/Factories/com.sun.star.text.TextDocument",
          new UnoProps(UnoProperty.OO_SETUP_FACTORY_WINDOW_ATTRIBUTES, value));
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Set the value of all form fields with an Id to the given value. Updates all depending form
   * fields.
   *
   * @param id
   *          The id of the form field.
   * @param value
   *          The new value of the form field.
   */
  @Override
  public void valueChanged(String id, String value)
  {
    if (!id.isEmpty())
    {
      new OnFormValueChanged(this, id, value).emit();
    }
  }

  /**
   * Set the visibility of a group.
   *
   * @param groupId
   *          The id of the group.
   * @param visible
   *          True if the group should be visible, false otherwise.
   */
  @Override
  public void visibilityChanged(String groupId, boolean visible)
  {
    new OnSetVisibleState(this, groupId, visible, null).emit();
  }

  @Override
  public void statusChanged(String id, boolean okay)
  {
    // nothing to do here, only for form gui.
  }
}
