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
package de.muenchen.allg.itd51.wollmux.core.document;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Streams;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNamed;
import com.sun.star.frame.FrameSearchFlag;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.RuntimeException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XModifiable2;
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoCollection;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.document.FormFieldFactory.FormField;
import de.muenchen.allg.itd51.wollmux.core.document.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.SetJumpMark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommands;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.dispatch.PrintDispatch;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoProperty;
import de.muenchen.allg.util.UnoService;

/**
 * Model of an opened text document.
 */
public class TextDocumentModel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(TextDocumentModel.class);

  public static final String OVERRIDE_FRAG_DB_SPALTE = "OVERRIDE_FRAG_DB_SPALTE";

  private static final String FUNCTION = "FUNCTION";

  private static final String FORMULAR = "Formular";

  private static final String FENSTER = "Fenster";

  private static final String FUNKTIONEN = "Funktionen";

  private static final String BUTTONS = "Buttons";

  private static final String ACTION = "ACTION";

  /**
   * Unknown version number.
   */
  public static final String VERSION_UNKNOWN = "unknown";

  /**
   * Reference to the UNO document object.
   */
  public final XTextDocument doc;

  /**
   * Pattern for recognizing book marks to delete.
   */
  public static final Pattern BOOKMARK_KILL_PATTERN = Pattern.compile(
      "(\\A\\s*(WM\\s*\\(.*CMD\\s*'((form)|(setGroups)|(insertFormValue))'.*\\))\\s*\\d*\\z)"
          + "|(\\A\\s*(WM\\s*\\(.*CMD\\s*'(setType)'.*'formDocument'\\))\\s*\\d*\\z)"
          + "|(\\A\\s*(WM\\s*\\(.*'formDocument'.*CMD\\s*'(setType)'.*\\))\\s*\\d*\\z)");

  /**
   * Pattern for recognizing wollmux book marks.
   */
  public static final Pattern WOLLMUX_BOOKMARK_PATTERN = Pattern.compile("(\\A\\s*(WM\\s*\\(.*\\))\\s*\\d*\\z)");

  /**
   * Pattern for recognizing InputUser-fields with WollMux functions (eg. mail merge fields).
   */
  public static final Pattern INPUT_USER_FUNCTION = Pattern
      .compile("\\A\\s*(WM\\s*\\(.*FUNCTION\\s*'[^']*'.*\\))\\s*\\d*\\z");

  /**
   * If a form field couldn't be restored correctly, this constant is set as the value in
   * {@link TextDocumentController#getIDToPresetValue}.
   *
   * Has be passed as object because of == comparison.
   */
  public static final String FISHY = L.m("!!!PRÜFEN!!!");

  /**
   * Prefix of generated document local function names.
   */
  public static final String AUTOFUNCTION_PREFIX = "AUTOFUNCTION_";

  /**
   * Mapping from form field ID to all form objects with a WollMux book mark in the document. Form
   * objects in this map are not in {@link #idToTextFieldFormFields} and vice versa.
   */
  private Map<String, List<FormField>> idToFormFields;

  /**
   * Mapping from form field ID to all form objects without a WollMux book mark in the document.
   * Form objects in this map are not in {@link #idToFormFields} and vice versa.
   */
  private Map<String, List<FormField>> idToTextFieldFormFields;

  /**
   * List of all text fields without a WollMux book mark, which use a TRAFO-function with fixed
   * return value. The fields in the list are not in the map {@link #idToTextFieldFormFields}.
   */
  private List<FormField> staticTextFieldFormFields;

  /**
   * Cache of fragment URLS of an openTemplate command.
   */
  private volatile String[] fragUrls;

  /**
   * True if this document is a template or should be treated as template.
   *
   * An unnamed document created by a template is treated as a template as well. A "normal" text
   * document which is edited isn't treated as template. Documents which have set the type
   * "templateTemplate" aren't treated as templates.
   *
   * This is the same as the parameter asTemplate of
   * {@link UNO#loadComponentFromURL(String, boolean, boolean)}.
   */
  private volatile boolean isTemplate;

  /**
   * True if this document should be treated as if it has a form.
   */
  private volatile boolean isFormDocument;

  /**
   * The print functions specified for the document.
   */
  private volatile Set<String> printFunctions;

  /**
   * The form description or null if not yet read.
   */
  private volatile ConfigThingy formularConf;

  /**
   * Mapping from field id to form value.
   */
  private Map<String, String> formFieldValues;

  /**
   * Reference to the persistent data.
   */
  private PersistentDataContainer persistentData;

  /**
   * The document commands.
   */
  private volatile DocumentCommands documentCommands;

  /**
   * Mapping from visibility group names to their visibility state.
   */
  private Map<String, Boolean> mapGroupIdToVisibilityState;

  /**
   * Mapping from old FRAG_IDs to NEW_FRAG_IDs specified by OverrideFrag-commands.
   */
  private Map<String, String> overrideFragMap;

  /**
   * Null or the configuration of a mail merge.
   */
  private volatile ConfigThingy mailmergeConf;

  /**
   * Has the version info been updated?
   */
  private boolean haveUpdatedLastTouchedByVersionInfo = false;

  /**
   * A new TextDocumentModel. Shouldn't be called directly. A new model is created by
   * {@link DocumentManager#getTextDocumentController(XTextDocument)}.
   *
   * @param doc
   *          The reference to the UNO document object.
   * @param persistentDataContainer
   *          The reference to the persistent data container.
   */
  public TextDocumentModel(XTextDocument doc, PersistentDataContainer persistentDataContainer)
  {
    this.doc = doc;
    this.idToFormFields = new HashMap<>();
    idToTextFieldFormFields = new HashMap<>();
    staticTextFieldFormFields = new ArrayList<>();
    this.fragUrls = new String[] {};
    this.printFunctions = new HashSet<>();
    this.formularConf = null;
    this.formFieldValues = new HashMap<>();
    this.mapGroupIdToVisibilityState = new HashMap<>();
    this.overrideFragMap = new HashMap<>();

    // parse document commands without changing modified state
    boolean modified = isDocumentModified();
    this.documentCommands = new DocumentCommands(UNO.XBookmarksSupplier(doc));
    documentCommands.update();
    setDocumentModified(modified);

    // read persistent data
    this.persistentData = persistentDataContainer;
    String setTypeData = persistentData.getData(DataID.SETTYPE);
    parsePrintFunctions(persistentData.getData(DataID.PRINTFUNCTION));
    parseFormValues(persistentData.getData(DataID.FORMULARWERTE));

    this.isTemplate = !hasURL();
    this.isFormDocument = false;

    setType(setTypeData);
  }

  public Map<String, List<FormField>> getIdToTextFieldFormFields()
  {
    return idToTextFieldFormFields;
  }

  public List<FormField> getStaticTextFieldFormFields()
  {
    return staticTextFieldFormFields;
  }

  public Map<String, List<FormField>> getIdToFormFields()
  {
    return idToFormFields;
  }

  public Map<String, Boolean> getMapGroupIdToVisibilityState()
  {
    return mapGroupIdToVisibilityState;
  }

  public void setMapGroupIdToVisibilityState(Map<String, Boolean> mapGroupIdToVisibilityState)
  {
    this.mapGroupIdToVisibilityState = mapGroupIdToVisibilityState;
  }

  public PersistentDataContainer getPersistentData()
  {
    return persistentData;
  }

  public void setPersistentData(PersistentDataContainer persistentData)
  {
    this.persistentData = persistentData;
  }

  public ConfigThingy getFormularConf()
  {
    return formularConf;
  }

  public void setFormularConf(ConfigThingy formularConf)
  {
    this.formularConf = formularConf;
  }

  public Set<String> getPrintFunctions()
  {
    return printFunctions;
  }

  public ConfigThingy getMailmergeConf()
  {
    return mailmergeConf;
  }

  public void setMailmergeConf(ConfigThingy mailmergeConf)
  {
    this.mailmergeConf = mailmergeConf;
  }

  public synchronized DocumentCommands getDocumentCommands()
  {
    return documentCommands;
  }

  public synchronized void setIDToFormFields(Map<String, List<FormField>> idToFormFields)
  {
    this.idToFormFields = idToFormFields;
  }

  public String[] getFragUrls()
  {
    return fragUrls;
  }

  public void setFragUrls(String[] fragUrls)
  {
    this.fragUrls = fragUrls;
  }

  public boolean isTemplate()
  {
    return isTemplate;
  }

  public boolean isFormDocument()
  {
    return isFormDocument;
  }

  /**
   * {@link #getFormFieldValuesMap()}
   *
   * @return
   */
  public synchronized Map<String, String> getFormFieldValues()
  {
    return formFieldValues;
  }

  /**
   * Update the version information in the persistent data. The document isn't marked as modified by
   * this method.
   */
  public synchronized void updateLastTouchedByVersionInfo()
  {
    if (!haveUpdatedLastTouchedByVersionInfo)
    {
      haveUpdatedLastTouchedByVersionInfo = true;
      boolean modified = isDocumentModified();
      persistentData.setData(DataID.TOUCH_WOLLMUXVERSION, WollMuxSingleton.getVersion());
      persistentData.setData(DataID.TOUCH_OOOVERSION, UNO.getOOoVersion());
      setDocumentModified(modified);
    }
  }

  /**
   * Parse the print function definition.
   *
   * @param data
   *          The print function definition as string.
   */
  private void parsePrintFunctions(String data)
  {
    if (data == null || data.isEmpty())
    {
      return;
    }

    final String errmsg = L.m("Fehler beim Einlesen des Druckfunktionen-Abschnitts '%1':", data);

    ConfigThingy conf = new ConfigThingy("");
    try
    {
      conf = new ConfigThingy("", data);
    } catch (IOException e)
    {
      LOGGER.error(errmsg, e);
    } catch (SyntaxErrorException e)
    {
      try
      {
        // backwards compatibility for print blocks with only the function name.
        ConfigThingy.checkIdentifier(data);
        conf = new ConfigThingy("", "WM(Druckfunktionen((FUNCTION '" + data + "')))");
      } catch (java.lang.Exception forgetMe)
      {
        LOGGER.error(errmsg, e);
      }
    }

    ConfigThingy functions = conf.query("WM").query("Druckfunktionen").queryByChild(FUNCTION);
    for (Iterator<ConfigThingy> iter = functions.iterator(); iter.hasNext();)
    {
      ConfigThingy func = iter.next();
      String name = func.getString(FUNCTION);

      if (name != null && !name.isEmpty())
      {
        printFunctions.add(name);
      }
    }
  }

  /**
   * Add new form descriptions to an existing form description.
   *
   * @param formDesc
   *          The root node of an existing form description.
   * @param value
   *          Is interpreted as {@link ConfigThingy} and all "Formular" sections are added to
   *          formDesc.
   */
  public static void addToFormDescription(ConfigThingy formDesc, String value)
  {
    if (value == null || value.length() == 0)
    {
      return;
    }

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("", null, new StringReader(value));
    } catch (java.lang.Exception e)
    {
      LOGGER.error(L.m("Die Formularbeschreibung ist fehlerhaft"), e);
      return;
    }

    ConfigThingy formulare = conf.query(FORMULAR);
    for (Iterator<ConfigThingy> iter = formulare.iterator(); iter.hasNext();)
    {
      ConfigThingy formular = iter.next();
      formDesc.addChild(formular);
    }
  }

  /**
   * Write the form values part of the persistent data on the stream.
   *
   * @param out
   *          The stream to write on.
   */
  public void exportFormValues(OutputStream out)
  {
    String data = persistentData.getData(DataID.FORMULARWERTE);
    try (PrintWriter pw = new PrintWriter(out))
    {
      pw.print(data);
    }
  }

  /**
   * Import the form values form a file.
   * 
   * @param file
   *          A file containing a description of form values.
   * @throws IOException
   *           The file can't be read.
   */
  public void importFormValues(File file) throws IOException
  {
    String data = new String(Files.readAllBytes(file.toPath()));
    parseFormValues(data);
  }

  /**
   * Parses the string as {@link ConfigThingy} of the form "WM(FormularWerte(...))" and adds the
   * values to {@link #formFieldValues}.
   *
   * @param werteStr
   *          The string defining the form values. If null nothing is done.
   */
  private void parseFormValues(String werteStr)
  {
    if (werteStr == null)
    {
      return;
    }

    ConfigThingy werte;
    try
    {
      ConfigThingy conf = new ConfigThingy("", null, new StringReader(werteStr));
      werte = conf.get("WM").get("Formularwerte");
    } catch (NodeNotFoundException | IOException | SyntaxErrorException e)
    {
      LOGGER.error(L.m("Formularwerte-Abschnitt ist fehlerhaft"), e);
      return;
    }

    for (ConfigThingy element : werte)
    {
      String id = element.getString("ID");
      String value = element.getString("VALUE");

      if (id != null && value != null)
      {
        formFieldValues.put(id, value);
      }
    }
  }

  /**
   * Get the value of the first untransformed field.
   *
   * @param fields
   *          Find the first untransformed field in this list.
   * @return A form field or null if all fields in the list are transformed or the list is empty.
   */
  public String getFirstUntransformedValue(List<FormField> fields)
  {
    for (FormField field : fields)
    {
      if (field.getTrafoName() == null)
      {
        return field.getValue();
      }
    }
    return null;
  }

  /**
   * Add a new override of a fragment.
   *
   * @param fragId
   *          The ID which should be replaced.
   * @param newFragId
   *          The new ID which should be used instead.
   *
   * @throws OverrideFragChainException
   *           If fragId or newFragId is already part of an override.
   */
  public synchronized void setOverrideFrag(String fragId, String newFragId) throws OverrideFragChainException
  {
    if (overrideFragMap.containsKey(newFragId))
    {
      throw new OverrideFragChainException(newFragId);
    }
    if (overrideFragMap.containsValue(fragId))
    {
      throw new OverrideFragChainException(fragId);
    }
    if (!overrideFragMap.containsKey(fragId))
    {
      overrideFragMap.put(fragId, newFragId);
    }
  }

  /**
   * Exception for chaining overrides.
   */
  public static class OverrideFragChainException extends Exception
  {
    private static final long serialVersionUID = 6792199728784265252L;

    private final String fragId;

    /**
     * A new chaining overrides exception.
     * 
     * @param fragId
     *          The ID of the fragment which causes the exception.
     */
    public OverrideFragChainException(String fragId)
    {
      this.fragId = fragId;
    }

    @Override
    public String getMessage()
    {
      return L.m("Mit overrideFrag können keine Ersetzungsketten definiert werden, das Fragment '%1' "
          + "taucht jedoch bereits in einem anderen overrideFrag-Kommando auf.", fragId);
    }

  }

  /**
   * Get the new ID for the given ID defined by an OverrideFrag-command.
   *
   * @param fragId
   *          The ID of a fragment.
   * @return The new ID if the fragment has an override or the ID itself.
   */
  public synchronized String getOverrideFrag(String fragId)
  {
    if (overrideFragMap.containsKey(fragId))
      return overrideFragMap.get(fragId);
    else
      return fragId;
  }

  /**
   * Has the document an URL?
   *
   * @return True if the document has an URL which is the source of document. This means that the
   *         document is opened in edit mode.
   */
  public synchronized boolean hasURL()
  {
    return doc.getURL() != null && !doc.getURL().isEmpty();
  }

  /**
   * Has the document a form description with a form GUI?
   *
   * @return True if the document has a form description with a defined form GUI, false otherwise.
   */
  public synchronized boolean hasFormGUIWindow()
  {
    try
    {
      ConfigThingy windows = getFormDescription().query(FORMULAR).query(FENSTER);
      if (windows.count() > 0)
      {
        return windows.getLastChild().count() != 0;
      }
      return false;
    } catch (NodeNotFoundException e)
    {
      LOGGER.trace("", e);
      return false;
    }
  }

  /**
   * Mark the document as form, template or template for templates.
   * 
   * @param typeStr
   *          Should be one of the values {@code "normalTemplate"}, {@code "templateTemplate"},
   *          {@code "formDocument"}. Otherwise nothing is done.
   */
  public void setType(String typeStr)
  {
    if ("normalTemplate".equalsIgnoreCase(typeStr))
    {
      isTemplate = true;
    } else if ("templateTemplate".equalsIgnoreCase(typeStr))
    {
      isTemplate = false;
    } else if ("formDocument".equalsIgnoreCase(typeStr))
    {
      isFormDocument = true;
    }
  }

  /**
   * Add a print function to the document.
   *
   * @param functionName
   *          The name of the function.
   */
  public void addPrintFunction(String functionName)
  {
    printFunctions.add(functionName);
    storePrintFunctions();
  }

  /**
   * Remove a print function from the document.
   *
   * @param functionName
   *          The name of the function.
   */
  public void removePrintFunction(String functionName)
  {
    if (!printFunctions.remove(functionName))
    {
      return;
    }
    storePrintFunctions();
  }

  /**
   * Store the print functions in the persistent data. There are 2 syntax variants. Either the
   * section contains only the name or is like
   * 
   * <pre>
   * WM(
   *   Druckfunktionen(
   *     (FUNCTION 'name')
   *          ...
   *     )
   *   )
   * </pre>
   * 
   * If there aren't any print functions the section is removed from the persistent data.
   */
  private void storePrintFunctions()
  {
    if (printFunctions.isEmpty())
    {
      persistentData.removeData(DataID.PRINTFUNCTION);
    } else
    {
      boolean needConfigThingy = printFunctions.size() > 1;

      // sort function names.
      ArrayList<String> names = new ArrayList<>(printFunctions);
      Collections.sort(names);

      ConfigThingy wm = new ConfigThingy("WM");
      ConfigThingy druckfunktionen = new ConfigThingy("Druckfunktionen");
      wm.addChild(druckfunktionen);
      for (String name : names)
      {
        ConfigThingy list = new ConfigThingy("");
        ConfigThingy nameConf = new ConfigThingy(FUNCTION);
        nameConf.addChild(new ConfigThingy(name));
        list.addChild(nameConf);
        druckfunktionen.addChild(list);
      }

      if (needConfigThingy)
      {
        persistentData.setData(DataID.PRINTFUNCTION, wm.stringRepresentation());
      } else
      {
        persistentData.setData(DataID.PRINTFUNCTION, printFunctions.iterator().next());
      }
    }
  }

  /**
   * Set the view cursor at the beginning of the visibility element.
   *
   * @param visibleElement
   *          The visibility element.
   */
  public synchronized void focusRangeStart(VisibilityElement visibleElement)
  {
    try
    {
      getViewCursor().gotoRange(visibleElement.getAnchor().getStart(), false);
    } catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
    }
  }

  /**
   * Get the first SetJumpMark command of the document.
   *
   * @return The command or null if there is no such command.
   */
  public synchronized SetJumpMark getFirstJumpMark()
  {
    return documentCommands.getFirstJumpMark();
  }

  /**
   * Get the IDs of all fields in the document.
   *
   * @return A set of field IDs.
   */
  public synchronized Set<String> getAllFieldIDs()
  {
    HashSet<String> ids = new HashSet<>();
    ids.addAll(idToFormFields.keySet());
    ids.addAll(getIdToTextFieldFormFields().keySet());
    return ids;
  }

  /**
   * Get a copy of the current form value per form ID. Changes on the map have no effect.
   */
  public synchronized Map<String, String> getFormFieldValuesMap()
  {
    return new HashMap<>(formFieldValues);
  }

  /**
   * Set all form fields to the empty string.
   */
  public synchronized void clearFormFieldValues()
  {
    for (String key : formFieldValues.keySet())
    {
      formFieldValues.put(key, "");
    }
  }

  /**
   * Get the view cursor of the document.
   *
   * @return The cursor or null if there isn't a controller or view cursor.
   */
  public synchronized XTextViewCursor getViewCursor()
  {
    if (UNO.XModel(doc) == null)
    {
      return null;
    }
    XTextViewCursorSupplier suppl = UNO.XTextViewCursorSupplier(UNO.XModel(doc).getCurrentController());
    if (suppl != null)
    {
      return suppl.getViewCursor();
    }
    return null;
  }

  /**
   * Has the view cursor a selection?
   *
   * @return True if the cursor isn't collapsed and marks an area, false otherwise.
   */
  public synchronized boolean hasSelection()
  {
    XTextViewCursor vc = getViewCursor();
    if (vc != null)
    {
      return !vc.isCollapsed();
    }
    return false;
  }

  /**
   * Get the form description of the document.
   *
   * @return The form description.
   */
  public synchronized ConfigThingy getFormDescription()
  {
    if (formularConf == null)
    {
      LOGGER.debug("Einlesen der Formularbeschreibung von {}", this);
      formularConf = new ConfigThingy("WM");
      addToFormDescription(formularConf, persistentData.getData(DataID.FORMULARBESCHREIBUNG));

      ConfigThingy title = formularConf.query("TITLE");
      if (title.count() > 0 && LOGGER.isDebugEnabled())
      {
        LOGGER.debug("Formular {} eingelesen.", title.stringRepresentation(true, '\''));
      }
    }

    return formularConf;
  }

  /**
   * Get the mail merge configuration.
   *
   * @return The mail merge configuration which can be empty.
   */
  public synchronized ConfigThingy getMailmergeConfig()
  {
    if (mailmergeConf == null)
    {
      String data = persistentData.getData(DataID.SERIENDRUCK);
      mailmergeConf = new ConfigThingy("Seriendruck");
      if (data != null)
        try
        {
          mailmergeConf = new ConfigThingy("", data).query("WM").query("Seriendruck").getLastChild();
        } catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
    }
    return mailmergeConf;
  }

  /**
   * Get the function definition of the form description.
   *
   * @return The function definition which can be empty.
   */
  public ConfigThingy getFunktionenConf()
  {
    ConfigThingy formDesc = getFormDescription();
    try
    {
      return formDesc.query(FORMULAR).query(FUNKTIONEN).getLastChild();
    } catch (NodeNotFoundException e)
    {
      ConfigThingy funktionen = new ConfigThingy(FUNKTIONEN);
      ConfigThingy formular;
      try
      {
        formular = formDesc.query(FORMULAR).getLastChild();
      } catch (NodeNotFoundException e1)
      {
        formular = new ConfigThingy(FORMULAR);
        formDesc.addChild(formular);
      }
      formular.addChild(funktionen);
      return funktionen;
    }
  }

  /**
   * Focus the first untransformed form field with the given ID. If no untransformed field is
   * available a transformed field is focused.
   *
   * @param fieldId
   *          The ID of the field to focus.
   */
  public synchronized void focusFormField(String fieldId)
  {
    FormField field;
    List<FormField> formFields = getIdToTextFieldFormFields().get(fieldId);
    if (formFields != null)
    {
      field = formFields.get(0);
    } else
    {
      formFields = idToFormFields.get(fieldId);
      field = preferUntransformedFormField(formFields);
    }

    try
    {
      if (field != null)
      {
        field.focus();
      }
    } catch (RuntimeException e)
    {
      LOGGER.trace("", e);
    }
  }

  /**
   * Get the first untransformed field from the list, or the first transformed field if there isn't
   * an untransformed field.
   *
   * @param formFields
   *          List of form field.
   * @return The form field or null if the list is empty or null.
   */
  protected static FormField preferUntransformedFormField(List<FormField> formFields)
  {
    if (formFields == null || formFields.isEmpty())
    {
      return null;
    }
    for (FormField field : formFields)
    {
      if (field.getTrafoName() == null)
      {
        return field;
      }
    }
    return formFields.get(0);
  }

  /**
   * Get the number of pages.
   *
   * @return The number of pages or 0 if can't be determined.
   */
  public synchronized int getPageCount()
  {
    try
    {
      return (int) AnyConverter.toLong(UnoProperty.getProperty(doc.getCurrentController(), UnoProperty.PAGE_COUNT));
    } catch (java.lang.Exception e)
    {
      LOGGER.trace("", e);
      return 0;
    }
  }

  /**
   * Has the document been modified?
   *
   * @return True if the document is modified, false otherwise.
   */
  public synchronized boolean isDocumentModified()
  {
    try
    {
      return UNO.XModifiable(doc).isModified();
    } catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
      return false;
    }
  }

  /**
   * Set the modified state of the document.
   *
   * @param state
   *          True, if the document should be marked as modified, false otherwise.
   */
  public synchronized void setDocumentModified(boolean state)
  {
    try
    {
      UNO.XModifiable(doc).setModified(state);
    } catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
    }
  }

  /**
   * Ignore or update document modified state.
   *
   * @param state
   *          If true the document state will never be modified. If false changes in the document
   *          mark it as modified.
   */
  public synchronized void setDocumentModifiable(boolean state)
  {
    try
    {
      XModifiable2 mod2 = UNO.XModifiable2(doc);
      if (state)
      {
        mod2.enableSetModified();
      } else
      {
        mod2.disableSetModified();
      }
    } catch (java.lang.Exception x)
    {
      LOGGER.trace("", x);
    }
  }

  /**
   * Get the name of the currently selected printer in the document.
   */
  public String getCurrentPrinterName()
  {
    XPrintable printable = UNO.XPrintable(doc);
    PropertyValue[] printer = null;
    if (printable != null)
    {
      printer = printable.getPrinter();
    }
    UnoProps printerInfo = new UnoProps(printer);
    try
    {
      return (String) printerInfo.getPropertyValue(UnoProperty.NAME);
    } catch (UnknownPropertyException e)
    {
      return L.m("unbekannt");
    }
  }

  /**
   * Get the frame of the document and execute {link
   * {@link XDispatchProvider#queryDispatch(com.sun.star.util.URL, String, int)}.
   *
   * @param urlStr
   *          The command URL of the dispatch.
   * @return A dispatch or null, if no such dispatch exists.
   */
  private XDispatch getDispatchForModel(com.sun.star.util.URL url)
  {
    XDispatchProvider dispProv = null;

    dispProv = UNO.XDispatchProvider(UNO.XModel(doc).getCurrentController().getFrame());

    if (dispProv != null)
    {
      return dispProv.queryDispatch(url, "_self", com.sun.star.frame.FrameSearchFlag.SELF);
    }
    return null;
  }

  /**
   * Show the LibreOffice dialog for configuring the printer.
   *
   * @param listener
   *          A result listener.
   */
  public void configurePrinter(XDispatchResultListener listener)
  {
    try
    {
      com.sun.star.util.URL url = UNO.getParsedUNOUrl(PrintDispatch.COMMAND_PRINTER_SETUP);
      XNotifyingDispatch disp = UNO.XNotifyingDispatch(getDispatchForModel(url));

      if (disp != null)
      {
        disp.dispatchWithNotification(url, new PropertyValue[] {}, listener);
      }
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Suspend all controllers of the document. If all controllers of a document are suspend the
   * save/dismiss dialog is opened by Office.
   *
   * @param suspend
   *          If true controllers are suspend, otherwise reactivated.
   * @return True if all controllers could be suspend or reactivated.
   */
  private boolean suspendControllers(boolean suspend)
  {
    boolean closeOk = true;
    if (UNO.XFramesSupplier(UNO.desktop) != null)
    {
      XFrame[] frames = UNO.XFramesSupplier(UNO.desktop).getFrames().queryFrames(FrameSearchFlag.ALL);
      for (int i = 0; i < frames.length; i++)
      {
        XController c = frames[i].getController();
        if (c != null && UnoRuntime.areSame(c.getModel(), doc) && !c.suspend(suspend))
        {
          closeOk = false;
        }
      }
    }
    return closeOk;
  }

  /**
   * Close the document. If it has been modified the save dialog is shown. The document is disposed
   * afterwards.
   */
  public synchronized void close()
  {
    boolean closeOk = suspendControllers(true);

    /*
     * If the document has been saved after suspending all controllers, the second test is true, If
     * the document is unchanged the first test is true.
     */
    if (closeOk || !isDocumentModified())
    {
      try
      {
        if (UNO.XCloseable(doc) != null)
        {
          UNO.XCloseable(doc).close(true);
        }

        XFrame[] frames = UNO.XFramesSupplier(UNO.desktop).getFrames().queryFrames(FrameSearchFlag.ALL);
        if (frames.length <= 1)
        {
          UNO.desktop.terminate();
        }
      } catch (CloseVetoException e)
      {
        LOGGER.trace("", e);
      }

    } else if (UNO.XFramesSupplier(UNO.desktop) != null)
    {
      // dialog was aborted, reactivate controller
      suspendControllers(false);
    }
  }

  /**
   * {@link DocumentCommands#addNewDocumentCommand(XTextRange, String)
   *
   * @param r
   *          The text range.
   * @param cmdStr
   *          The command.
   */
  public synchronized void addNewDocumentCommand(XTextRange r, String cmdStr)
  {
    documentCommands.addNewDocumentCommand(r, cmdStr);
  }

  /**
   * Parse the field name and return the function name.
   *
   * @param userFieldName
   *          The name of the field.
   * @return If the name is in the form {@code WM(FUNCTION '<name>')} the function name is returned,
   *         otherwise null.
   */
  public static String getFunctionNameForUserFieldName(String userFieldName)
  {
    if (userFieldName == null)
    {
      return null;
    }

    Matcher m = TextDocumentModel.INPUT_USER_FUNCTION.matcher(userFieldName);

    if (!m.matches())
    {
      return null;
    }
    String confStr = m.group(1);

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("INSERT", confStr);
    } catch (Exception x)
    {
      LOGGER.trace("", x);
      return null;
    }

    ConfigThingy trafoConf = conf.query(FUNCTION);
    if (trafoConf.count() != 1)
    {
      return null;
    } else
    {
      return trafoConf.toString();
    }
  }

  /**
   * Parse the command name and return the function name.
   *
   * @param cmdStr
   *          The command name.
   * @return If the name is in the form {@code WM(CMD '<command>' .. TRAFO ...)} the function name
   *         is returned otherwise null.
   */
  public static String getFunctionNameForDocumentCommand(String cmdStr)
  {
    ConfigThingy wm = new ConfigThingy("");
    try
    {
      wm = new ConfigThingy("", cmdStr).get("WM");
    } catch (NodeNotFoundException | IOException | SyntaxErrorException e)
    {
      LOGGER.trace("", e);
    }

    String cmd = wm.getString("CMD", "");

    if ("insertFormValue".equalsIgnoreCase(cmd))
    {
      return wm.getString("TRAFO");
    }

    return null;
  }

  /**
   * Get the TRAFO definition used in the current selection. If the selection has a form field which
   * has a TRAFO its definition is returned. If it's a global TRAFO it isn't returned.
   *
   * @return The definition or null if no TRAFO is found or the TRAFO is globally defined.
   */
  public synchronized ConfigThingy getFormFieldTrafoFromSelection()
  {
    XTextCursor vc = getViewCursor();
    if (vc == null)
    {
      return null;
    }

    Map<String, Integer> collectedTrafos = collectTrafosFromEnumeration(vc);

    HashSet<String> completeFields = new HashSet<>();
    HashSet<String> startedFields = new HashSet<>();
    HashSet<String> finishedFields = new HashSet<>();

    for (Map.Entry<String, Integer> ent : collectedTrafos.entrySet())
    {
      String trafo = ent.getKey();
      int complete = ent.getValue().intValue();
      if (complete == 1)
      {
        startedFields.add(trafo);
      }
      if (complete == 2)
      {
        finishedFields.add(trafo);
      }
      if (complete == 3)
      {
        completeFields.add(trafo);
      }
    }

    String trafoName = null;
    if (completeFields.size() > 1 || startedFields.size() > 1)
    {
      return null; // more than one field found
    } else if (completeFields.size() == 1)
    {
      trafoName = completeFields.iterator().next();
    } else if (startedFields.size() == 1)
    {
      trafoName = startedFields.iterator().next();
    }

    if (trafoName != null)
    {
      try
      {
        return getFormDescription().query(FORMULAR).query(FUNKTIONEN).query(trafoName, 2).getLastChild();
      } catch (NodeNotFoundException e)
      {
        LOGGER.trace("", e);
      }
    }

    return null;
  }

  /**
   * Collect all fields in the text range which have a TRAFO. Each TRAFO is categorized as either
   * <ul>
   * <li>1: only the start tag is in the text range</li>
   * <li>2: only the end tag is in the text range</li>
   * <li>3: start and end tag are in the text range</li>
   * </ul>
   *
   * @param textRange
   *          The text range.
   * @return Mapping from TRAFO to category.
   */
  private static Map<String, Integer> collectTrafosFromEnumeration(XTextRange textRange)
  {
    HashMap<String, Integer> collectedTrafos = new HashMap<>();

    if (textRange == null)
    {
      return collectedTrafos;
    }
    UnoCollection<XTextRange> paragraphs = UnoCollection
        .getCollection(textRange.getText().createTextCursorByRange(textRange), XTextRange.class);
    if (paragraphs == null)
    {
      return collectedTrafos;
    }

    for (XTextRange paragraph : paragraphs)
    {
      UnoCollection<XTextRange> portions = UnoCollection.getCollection(paragraph, XTextRange.class);
      if (portions == null)
      {
        continue;
      }

      for (XTextRange portion : portions)
      {
        collectTrafoFromTextField(collectedTrafos, portion);

        collectTrafoFromBookMark(collectedTrafos, portion);
      }
    }
    return collectedTrafos;
  }

  private static void collectTrafoFromBookMark(HashMap<String, Integer> collectedTrafos, XTextRange portion)
  {
    XNamed bm = UNO.XNamed(Utils.getProperty(portion, UnoProperty.BOOKMARK));
    if (bm == null)
    {
      return;
    }
    String name = "" + bm.getName();

    try
    {
      boolean isCollapsed = AnyConverter.toBoolean(Utils.getProperty(portion, UnoProperty.IS_COLLAPSED));
      boolean isStart = AnyConverter.toBoolean(Utils.getProperty(portion, UnoProperty.IS_START)) || isCollapsed;
      boolean isEnd = !isStart || isCollapsed;

      String docCmd = getDocumentCommandByBookmarkName(name);
      if (docCmd != null)
      {
        String t = getFunctionNameForDocumentCommand(docCmd);
        if (t != null)
        {
          collectedTrafos.computeIfAbsent(t, k -> {
            int s = 0;
            if (isStart)
            {
              s += 1;
            }
            if (isEnd)
            {
              s += 2;
            }
            return s;
          });
        }
      }
    } catch (IllegalArgumentException e)
    {
      LOGGER.trace("", e);
    }
  }

  private static void collectTrafoFromTextField(HashMap<String, Integer> collectedTrafos, XTextRange portion)
  {
    XTextField tf = UNO.XTextField(Utils.getProperty(portion, UnoProperty.TEXT_FIELD));
    if (tf != null && UnoService.supportsService(tf, UnoService.CSS_TEXT_TEXT_FIELD_INPUT_USER))
    {
      String varName = "" + Utils.getProperty(tf, UnoProperty.CONTENT);
      String t = getFunctionNameForUserFieldName(varName);
      if (t != null)
      {
        collectedTrafos.put(t, Integer.valueOf(3));
      }
    }
  }

  /**
   * Test if a string is a document command.
   *
   * @param bookmarkName
   *          The string to check.
   * @return The string without tailing numbers or null if it isn't a document command.
   */
  public static String getDocumentCommandByBookmarkName(String bookmarkName)
  {
    Matcher m = WOLLMUX_BOOKMARK_PATTERN.matcher(bookmarkName);
    if (m.matches())
    {
      return m.group(1);
    }
    return null;
  }

  /**
   * Collect information about fields used in InsertFormValue-commands, mail merge fields, user
   * fields or TRAFOs not blacklisted in the schema.
   *
   * @param blacklist
   *          Don't collect information about the fields with an ID in this set.
   * @return Array of information per field ordered alphabetically.
   */
  public synchronized ReferencedFieldID[] getReferencedFieldIDsThatAreNotInSchema(Set<String> blacklist)
  {
    ArrayList<ReferencedFieldID> list = new ArrayList<>();

    List<String> sortedIDs = new ArrayList<>(getAllFieldIDs());
    Collections.sort(sortedIDs);
    for (String id : sortedIDs)
    {
      if (blacklist.contains(id))
      {
        continue;
      }
      List<FormField> fields = new ArrayList<>();
      if (idToFormFields.containsKey(id))
      {
        fields.addAll(idToFormFields.get(id));
      }
      if (getIdToTextFieldFormFields().containsKey(id))
      {
        fields.addAll(getIdToTextFieldFormFields().get(id));
      }
      boolean hasTrafo = false;
      for (FormField field : fields)
      {
        if (field.getTrafoName() != null)
        {
          hasTrafo = true;
        }
      }
      list.add(new ReferencedFieldID(id, hasTrafo));
    }

    return list.toArray(ReferencedFieldID[]::new);
  }

  /**
   * Information about a field.
   */
  public static class ReferencedFieldID
  {
    /**
     * The ID of the field.
     */
    private final String fieldId;

    /**
     * True if there's a TRAFO, otherwise false.
     */
    private final boolean isTransformed;

    /**
     * A new field information.
     *
     * @param fieldId
     *          The ID of the field.
     * @param isTransformed
     *          True if there's a TRAFO, otherwise false.
     */
    public ReferencedFieldID(String fieldId, boolean isTransformed)
    {
      this.fieldId = fieldId;
      this.isTransformed = isTransformed;
    }

    public String getFieldId()
    {
      return fieldId;
    }

    public boolean isTransformed()
    {
      return isTransformed;
    }
  }

  /**
   * Find the first text field annotation object in the enumeration.
   *
   * @param element
   *          If it's an {@link XEnumerationAccess} a the children are search for text field
   *          annotations. If there's no child or no text field annotation is found the element
   *          itself is checked whether it is a text field annotation.
   * @return A text field annotation or null.
   */
  public static XTextField findAnnotationFieldRecursive(Object element)
  {
    if (UNO.XEnumerationAccess(element) != null)
    {
      UnoCollection<Object> children = UnoCollection.getCollection(element, Object.class);

      for (Object child : children)
      {
        try
        {
          XTextField found = findAnnotationFieldRecursive(child);
          if (found != null)
          {
            return found;
          }
        } catch (Exception e)
        {
          LOGGER.error("", e);
        }
      }
    }

    Object textField = Utils.getProperty(element, UnoProperty.TEXT_FIELD);
    if (textField != null && UnoService.supportsService(textField, UnoService.CSS_TEXT_TEXT_FIELD_ANNOTATION))
    {
      return UNO.XTextField(textField);
    }

    return null;
  }

  /**
   * Merge a list of form descriptions. If a tab has the same ID only the last definition is used.
   * The order is defined by the appearance in the list.
   *
   * A new title is given to the form.
   *
   * Functions, function dialogs and visibilities are merged as well. Duplicate names cause an error
   * log and the last definition is used.
   *
   * @param desc
   *          List of form descriptions.
   * @param buttonAnpassung
   *          Adaption for the buttons part of a tab description.
   * @param newTitle
   *          The title of the new form.
   * @return The merged form description.
   */
  public static ConfigThingy mergeFormDescriptors(List<ConfigThingy> desc, ConfigThingy buttonAnpassung,
      String newTitle)
  {
    if (buttonAnpassung == null)
    {
      buttonAnpassung = new ConfigThingy("Buttonanpassung");
    }
    String plausiColor = null;
    Map<String, ConfigThingy> mapFensterIdToConfigThingy = new HashMap<>();
    Map<String, ConfigThingy> mapSichtbarkeitIdToConfigThingy = new HashMap<>();
    Map<String, ConfigThingy> mapFunktionenIdToConfigThingy = new HashMap<>();
    Map<String, ConfigThingy> mapFunktionsdialogeIdToConfigThingy = new HashMap<>();
    List<String> tabNames = new ArrayList<>();
    for (ConfigThingy conf : desc)
    {
      try
      {
        plausiColor = conf.get("PLAUSI_MARKER_COLOR", 1).toString();
      } catch (Exception x)
      {
        LOGGER.trace("", x);
      }

      mergeSection(conf, FENSTER, mapFensterIdToConfigThingy, tabNames, true);
      mergeSection(conf, "Sichtbarkeit", mapSichtbarkeitIdToConfigThingy, null, false);
      mergeSection(conf, FUNKTIONEN, mapFunktionenIdToConfigThingy, null, false);
      mergeSection(conf, "Funktionsdialoge", mapFunktionsdialogeIdToConfigThingy, null, false);
    }

    ConfigThingy conf = new ConfigThingy(FORMULAR);
    conf.add("TITLE").add(newTitle);
    if (plausiColor != null)
    {
      conf.add("PLAUSI_MARKER_COLOR").add(plausiColor);
    }

    ConfigThingy subConf = conf.add(FENSTER);

    Map<TabLocation, Set<String>> neverActions = new EnumMap<>(TabLocation.class);
    Map<TabLocation, List<ActionUIElementPair>> alwaysActions = new EnumMap<>(TabLocation.class);

    // TODO do only once at startup, the actions are always the same
    if (tabNames.size() == 1)
    {
      Pair<Set<String>, List<ActionUIElementPair>> buttonAdaption = collectButtonAdaption(
          buttonAnpassung.query("EinzigerTab"));
      neverActions.put(TabLocation.SINGLE, buttonAdaption.getLeft());
      alwaysActions.put(TabLocation.SINGLE, buttonAdaption.getRight());
    } else if (tabNames.size() > 1)
    {
      Pair<Set<String>, List<ActionUIElementPair>> buttonAdaption = collectButtonAdaption(
          buttonAnpassung.query("ErsterTab"));
      neverActions.put(TabLocation.FIRST, buttonAdaption.getLeft());
      alwaysActions.put(TabLocation.FIRST, buttonAdaption.getRight());
      buttonAdaption = collectButtonAdaption(buttonAnpassung.query("LetzterTab"));
      neverActions.put(TabLocation.LAST, buttonAdaption.getLeft());
      alwaysActions.put(TabLocation.LAST, buttonAdaption.getRight());

    }
    if (tabNames.size() > 2)
    {
      Pair<Set<String>, List<ActionUIElementPair>> buttonAdaption = collectButtonAdaption(
          buttonAnpassung.query("MittlererTab"));
      neverActions.put(TabLocation.MIDDLE, buttonAdaption.getLeft());
      alwaysActions.put(TabLocation.MIDDLE, buttonAdaption.getRight());
    }

    TabLocation loc = TabLocation.FIRST;
    if (tabNames.size() == 1)
    {
      loc = TabLocation.SINGLE;
    }
    for (int i = 0; i < tabNames.size(); i++)
    {
      String tabName = tabNames.get(i);
      ConfigThingy tabConf = mapFensterIdToConfigThingy.get(tabName);
      adoptButtons(tabConf, neverActions.get(loc), alwaysActions.get(loc));
      if (i + 1 == tabNames.size() - 1)
      {
        loc = TabLocation.LAST;
      } else
      {
        loc = TabLocation.MIDDLE;
      }
      subConf.addChild(tabConf);
    }

    addSubConfiguration(conf, "Sichtbarkeit", mapSichtbarkeitIdToConfigThingy.values());
    addSubConfiguration(conf, FUNKTIONEN, mapFunktionenIdToConfigThingy.values());
    addSubConfiguration(conf, "Funktionsdialoge", mapFunktionsdialogeIdToConfigThingy.values());

    return conf;
  }

  /**
   * Add all the configuration under a new section to a configuration.
   *
   * @param conf
   *          The configuration which gets the new section.
   * @param name
   *          The name of the new section.
   * @param values
   *          The entries of the section.
   */
  private static void addSubConfiguration(ConfigThingy conf, String name, Collection<ConfigThingy> values)
  {
    if (!values.isEmpty())
    {
      ConfigThingy subConf = conf.add(name);
      for (ConfigThingy conf2 : values)
      {
        subConf.addChild(conf2);
      }
    }
  }

  /**
   * Iterate through all items in a defined section of the configuration and add them to the
   * provided map. Duplicate items are logged and the last item is used.
   *
   * @param conf
   *          The configuration to merge.
   * @param sectionName
   *          The section of the configuration to merge.
   * @param mapFensterIdToConfigThingy
   *          Mapping from item name to configuration item.
   * @param tabNames
   *          If not null, all tab names are added to the list according to their appearance.
   * @param duplicatesAllowed
   *          If false duplicate tab names cause an error log.
   */
  private static void mergeSection(ConfigThingy conf, String sectionName,
      Map<String, ConfigThingy> mapFensterIdToConfigThingy, List<String> tabNames, boolean duplicatesAllowed)
  {
    for (ConfigThingy parent : conf.query(sectionName))
    {
      for (ConfigThingy node : parent)
      {
        String name = node.getName();
        if (tabNames != null && !tabNames.contains(name))
        {
          tabNames.add(name);
        }
        if (!duplicatesAllowed && mapFensterIdToConfigThingy.containsKey(name))
        {
          LOGGER.error("Fehler beim Zusammenfassen mehrerer Formulare zum gemeinsamen Ausfüllen: Mehrere \"{}\" "
              + "Abschnitte enthalten \"{}\"", sectionName, name);
        }

        mapFensterIdToConfigThingy.put(name, new ConfigThingy(node));
      }
    }
  }

  /**
   * Describes to location of a tab.
   */
  private enum TabLocation
  {
    /**
     * There is only one tab.
     */
    SINGLE,
    /**
     * This is the first tab, there's at least one more tab afterwards.
     */
    FIRST,
    /**
     * This is the last tab, there's at least one tab before.
     */
    LAST,
    /**
     * There's at least one tab before and after this tab.
     */
    MIDDLE;
  }

  /**
   * Change the Buttons-section of a tab. Buttons with an action listed in neverActions are removed.
   * Buttons from alwaysActions with an action which isn't already defined for the tab are added.
   *
   * @param tabConf
   *          The configuration of a tab.
   * @param neverActions
   *          Actions which should occur in this tab.
   * @param alwaysActions
   *          Actions which should be on the tab.
   */
  private static void adoptButtons(ConfigThingy tabConf, Set<String> neverActions,
      List<ActionUIElementPair> alwaysActions)
  {
    List<ActionUIElementPair> existingUIElements = new ArrayList<>();
    ConfigThingy buttonsConf = tabConf.query(BUTTONS);
    Streams.stream(buttonsConf).sequential().flatMap(Streams::stream).sequential().filter(buttonConf -> {
      String action = buttonConf.getString(ACTION);
      return action == null || !neverActions.contains(action);
    }).forEachOrdered(
        buttonConf -> existingUIElements.add(new ActionUIElementPair(buttonConf.getString(ACTION), buttonConf)));

    for (int i = 0; i < alwaysActions.size(); ++i)
    {
      mergeAlwaysButtons(alwaysActions, existingUIElements, i);
    }

    // remove Button section, a new one is created later
    Iterator<ConfigThingy> iter = tabConf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy possiblyButtonsConf = iter.next();
      if (BUTTONS.equals(possiblyButtonsConf.getName()))
      {
        iter.remove();
      }
    }

    // remove "GLUE" form the end
    ListIterator<ActionUIElementPair> liter = existingUIElements.listIterator(existingUIElements.size());
    while (liter.hasPrevious())
    {
      ActionUIElementPair act = liter.previous();
      String type = act.uiElementDesc.getString("TYPE");

      if (type != null && "glue".equals(type))
      {
        liter.remove();
      } else
      {
        break;
      }
    }

    ConfigThingy newButtons = new ConfigThingy(BUTTONS);
    for (ActionUIElementPair act : existingUIElements)
    {
      newButtons.addChild(act.uiElementDesc);
    }
    tabConf.addChild(newButtons);
  }

  /**
   * Get the button adaption from the configuration.
   *
   * @param anpassung
   *          The configuration.
   * @return Pair of actions which should never occur and actions which should be always present.
   */
  private static Pair<Set<String>, List<ActionUIElementPair>> collectButtonAdaption(ConfigThingy anpassung)
  {
    // make a copy, it may be modified
    anpassung = new ConfigThingy(anpassung);

    Set<String> neverActions = new HashSet<>();
    List<ActionUIElementPair> alwaysActions = new ArrayList<>();

    ConfigThingy neverConfs = anpassung.query("NEVER");
    for (ConfigThingy neverConf : neverConfs)
    {
      for (ConfigThingy entry : neverConf)
      {
        neverActions.add(entry.toString());
      }
    }

    ConfigThingy alwaysConfs = anpassung.query("ALWAYS");
    for (ConfigThingy alwaysConf : alwaysConfs)
    {
      try
      {
        String action = alwaysConf.get(ACTION).toString();
        alwaysConf.setName("");
        alwaysActions.add(new ActionUIElementPair(action, alwaysConf));
      } catch (NodeNotFoundException x)
      {
        LOGGER.error(L.m("Fehlerhafter ALWAYS-Angabe in Buttonanpassung-Abschnitt"), x);
      }
    }
    return Pair.of(neverActions, alwaysActions);
  }

  /**
   * Add the ith button from alwaysButtons to the existingUIElements if there isn't already button
   * with this action.
   *
   * @param alwaysButtons
   *          List of buttons which shoudl be always present.
   * @param existingUIElements
   *          The list of currently present buttons.
   * @param i
   *          The index of the button to add.
   */
  private static void mergeAlwaysButtons(
      List<ActionUIElementPair> alwaysButtons,
      List<ActionUIElementPair> existingUIElements, int i)
  {
    ActionUIElementPair act = alwaysButtons.get(i);

    // check if there's already a button with this action
    for (ActionUIElementPair act2 : existingUIElements)
    {
      if (act2.action != null && act2.action.equals(act.action))
      {
        return;
      }
    }

    // try to find the right place for insertion. See if there's a button with the previous action
    // so we can add it behind
    if (i > 0)
    {
      int index = existingUIElements.indexOf(alwaysButtons.get(i - 1));
      if (index >= 0)
      {
        existingUIElements.add(index + 1, act);
        return;
      }
    }

    // try to find the right place for insertion. See if there's a button with the next action
    // so we can add it before
    if (i + 1 < alwaysButtons.size())
    {
      int index = existingUIElements.indexOf(alwaysButtons.get(i + 1));
      if (index >= 0)
      {
        existingUIElements.add(index, act);
        return;
      }
    }

    // no right place found, add at the end
    existingUIElements.add(act);
  }

  /**
   * An UI element description.
   */
  private static class ActionUIElementPair
  {
    /**
     * The action of the UI element.
     */
    public final String action;

    /**
     * The description of the UI Element.
     */
    public final ConfigThingy uiElementDesc;

    /**
     * A new UI element description.
     *
     * @param action
     *          The action
     * @param uiElementDesc
     *          The description.
     */
    public ActionUIElementPair(String action, ConfigThingy uiElementDesc)
    {
      this.action = action;
      this.uiElementDesc = uiElementDesc;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (obj == null)
      {
        return false;
      }
      if (!(obj instanceof ActionUIElementPair))
      {
        return false;
      }
      ActionUIElementPair other = (ActionUIElementPair) obj;
      // ActionUIElements are equals if they have the same action
      return action != null && action.equals(other.action);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(action);
    }
  }
}
