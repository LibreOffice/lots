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
package org.libreoffice.lots.former.control.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.SyntaxErrorException;
import org.libreoffice.lots.former.DuplicateIDException;
import org.libreoffice.lots.former.FormMaxConstants;
import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.IDManager;
import org.libreoffice.lots.former.function.FunctionSelection;
import org.libreoffice.lots.former.function.FunctionSelectionAccess;
import org.libreoffice.lots.former.function.FunctionSelectionProvider;
import org.libreoffice.lots.former.function.ParamValue;
import org.libreoffice.lots.former.group.GroupsProvider;
import org.libreoffice.lots.former.model.IdModel;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repräsentiert ein Formularsteuerelement.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModel
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormControlModel.class);

  /**
   * {@link Pattern} für legale IDs.
   */
  private static Pattern ID_PATTERN = Pattern.compile("^([a-zA-Z_][a-zA-Z_0-9]*)");

  public static final String COMBOBOX_TYPE = "combobox";

  public static final String TEXTFIELD_TYPE = "textfield";

  public static final String TEXTAREA_TYPE = "textarea";

  public static final String LABEL_TYPE = "label";

  public static final String TAB_TYPE = "tab";

  public static final String SEPARATOR_TYPE = "separator";

  public static final String GLUE_TYPE = "glue";

  public static final String CHECKBOX_TYPE = "checkbox";

  public static final String BUTTON_TYPE = "button";

  public static final String FRAG_ID_TYPE = "frag_id";

  /**
   * Wird gesetzt, wenn versucht wird, einen TYPE einzustellen, der nicht bekannt
   * ist.
   */
  public static final String UNKNOWN_TYPE = "unknown";

  /**
   * Signalisiert, dass dem Element keine ACTION zugeordnet ist.
   */
  public static final String NO_ACTION = "";

  /**
   * Attribut ID für das Attribut "LABEL". F1XME: Eigentlich müsste jede der set...
   * Funktionen einen entsprechenden Event an die ModelChangeListener absetzen, aber
   * ich habe keine Lust(Zeit), das durchzuziehen. Da derzeit bei den meisten set...
   * Funktionen nur genau eine View den entsprechenden Wert anzeigt ist dies auch
   * noch nicht erforderlich und wer weiß ob der FM4000 jemals so erweitert wird,
   * dass es mehrere Views pro Wert gibt. Also warten wir einfach mal ab, bis was
   * kaputt geht.
   */
  public static final int LABEL_ATTR = 0;

  /** Attribut ID für das Attribut "TYPE". */
  public static final int TYPE_ATTR = 1;

  /** Attribut ID für das Attribut "ID". */
  public static final int ID_ATTR = 2;

  /** LABEL. */
  private String label;

  /** TYPE. Muss eine der *_TYPE Konstanten sein, da mit == verglichen wird. */
  private String type;

  /** ID (null => keine ID). */
  private IdModel id = null;

  /** ACTION. */
  private String action = NO_ACTION;

  /** DIALOG. */
  private String dialog = "";

  /** EXT. */
  private String ext = "";

  /** URL. */
  private String url = "";

  /** FRAG_ID */
  private String fragId = "";

  /** TIP. */
  private String tooltip = "";

  /** HOTKEY. */
  private char hotkey = 0;

  /** VALUES. */
  private List<String> items = new ArrayList<>(0);

  /** EDIT. */
  private boolean editable = false;

  /** READONLY. */
  private boolean readonly = false;

  /** WRAP. */
  private boolean wrap = true;

  /** GROUPS. */
  private GroupsProvider groups;

  /** LINES. */
  private int lines = 4;

  /** MINSIZE. */
  private int minsize = 0;

  /** MAXSIZE. */
  private int maxsize = 0;

  /** PLAUSI. */
  private FunctionSelection plausi = new FunctionSelection();

  /** AUTOFILL. */
  private FunctionSelection autofill = new FunctionSelection();

  /**
   * Die {@link ModelChangeListener}, die über Änderungen dieses Models informiert
   * werden wollen.
   */
  private List<ModelChangeListener> listeners = new ArrayList<>(1);

  /**
   * Der FormularMax4000 zu dem dieses Model gehört. Dieser wird über Änderungen des
   * Models informiert, um das Zurückschreiben der Daten in das Dokument anzustoßen.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Parst conf als Steuerelement und erzeugt ein entsprechendes FormControlModel.
   * ACHTUNG! Es wird der {@link IDManager} von formularMax4000 verwendet, um
   * sicherzustellen, dass keine doppelten IDs vorkommen. Im Falle einer doppelten ID
   * wird automatisch durch ein Suffix disambiguiert. Nicht-existierende bzw. leere
   * IDs sind allerdings okay.
   *
   * @param conf
   *          direkter Vorfahre von "TYPE", "LABEL", usw.
   * @param funcSelProv
   *          der {@link FunctionSelectionProvider}, der zu PLAUSI und AUTOFILL
   *          passende {@link FunctionSelection}s liefern kann.
   * @param formularMax4000 instance of fmax4kcontroller.
   */
  public FormControlModel(ConfigThingy conf, FunctionSelectionProvider funcSelProv,
      FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    this.groups = new GroupsProvider();
    label = L.m("Control element");
    type = TEXTFIELD_TYPE;
    String idStr = "";
    ConfigThingy plausiConf = null;

    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy attr = iter.next();
      String name = attr.getName();
      String str = attr.toString();
      if (name.equals("LABEL"))
        label = str;
      else if (name.equals("TYPE"))
        setType(str);
      else if (name.equals("ID"))
        idStr = str;
      else if (name.equals("ACTION"))
        action = str;
      else if (name.equals("DIALOG"))
        dialog = str;
      else if (name.equals("EXT"))
        ext = str;
      else if (name.equals("TIP"))
        tooltip = str;
      else if (name.equals("HOTKEY"))
        hotkey = str.length() > 0 ? str.charAt(0) : 0;
      else if (name.equals("EDIT"))
        editable = str.equalsIgnoreCase("true");
      else if (name.equals("READONLY"))
        readonly = str.equalsIgnoreCase("true");
      else if (name.equals("WRAP"))
        wrap = str.equalsIgnoreCase("true");
      else if (name.equals("LINES"))
        try
        {
          lines = Integer.parseInt(str);
        }
        catch (Exception x)
        {
          LOGGER.trace("", x);
        }
      else if (name.equals("MINSIZE"))
        try
        {
          minsize = Integer.parseInt(str);
        }
        catch (Exception x)
        {
          LOGGER.trace("", x);
        }
      else if (name.equals("MAXSIZE"))
        try
        {
          maxsize = Integer.parseInt(str);
        }
        catch (Exception x)
        {
          LOGGER.trace("", x);
        }
      else if (name.equals("VALUES"))
        items = parseValues(attr);
      else if (name.equals("GROUPS"))
        parseGroups(attr);
      else if (name.equals("PLAUSI"))
        plausiConf = attr;
      else if (name.equals(FormMaxConstants.AUTOFILL))
        autofill = funcSelProv.getFunctionSelection(attr);
      else if (name.equals("FRAG_ID"))
    	  fragId = str;
      else if (name.equals("URL"))
    	  url = str;
    }

    if (plausiConf != null)
      plausi = funcSelProv.getFunctionSelection(plausiConf, idStr);

    if (idStr.length() > 0) makeInactiveID(idStr);

    if (isGlue()) label = "glue";
  }

  /**
   * Liefert eine Liste, die die String-Werte aller Kinder von conf enthält.
   */
  private List<String> parseValues(ConfigThingy conf)
  {
    List<String> list = new ArrayList<>(conf.count());
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      list.add(iter.next().toString());
    }
    return list;
  }

  /**
   * Liefert eine Menge, die {@link IdModel} Objekte im Namensraum
   * {@link FormularMax4000#NAMESPACE_GROUPS} für die String-Werte aller Kinder von
   * conf enthält.
   */
  private void parseGroups(ConfigThingy conf)
  {
    HashSet<IdModel> set = new HashSet<>(conf.count());
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      set.add(formularMax4000.getIDManager().getID(FormularMax4kController.NAMESPACE_GROUPS,
        iter.next().toString()));
    }
    groups.initGroups(set);
  }

  /**
   * Erzeugt ein neues FormControlModel mit den gegebenen Parametern. Alle anderen
   * Eigenschaften erhalten Default-Werte (normalerweise der leere String). ACHTUNG!
   * Es wird der {@link IDManager} von formularMax4000 verwendet, um sicherzustellen,
   * dass keine doppelten IDs vorkommen. Im Falle einer doppelten ID wird automatisch
   * durch ein Suffix disambiguiert. Nicht-existierende bzw. leere IDs sind
   * allerdings okay.
   */
  private FormControlModel(String label, String type, String id,
      FormularMax4kController formularMax4000)
  {
    this.label = label;
    this.type = type;
    this.formularMax4000 = formularMax4000;
    this.groups = new GroupsProvider();
    makeInactiveID(id);
  }

  /**
   * Liefert ein FormControlModel, das eine Checkbox darstellt mit gegebenem LABEL
   * label und ID id.
   */
  public static FormControlModel createCheckbox(String label, String id,
      FormularMax4kController formularMax4000)
  {
    return new FormControlModel(label, CHECKBOX_TYPE, id, formularMax4000);
  }

  /**
   * Liefert ein FormControlModel, das ein Textfeld darstellt mit gegebenem LABEL
   * label und ID id.
   */
  public static FormControlModel createTextfield(String label, String id,
      FormularMax4kController formularMax4000)
  {
    FormControlModel model =
      new FormControlModel(label, TEXTFIELD_TYPE, id, formularMax4000);
    model.editable = true;
    return model;
  }

  /**
   * Liefert ein FormControlModel, das ein Label label darstellt.
   */
  public static FormControlModel createLabel(String label, String id,
      FormularMax4kController formularMax4000)
  {
    return new FormControlModel(label, LABEL_TYPE, id, formularMax4000);
  }

  /**
   * Liefert ein FormControlModel, das eine Combobox darstellt mit gegebenem LABEL
   * label und ID id.
   */
  public static FormControlModel createComboBox(String label, String id,
      String[] items, FormularMax4kController formularMax4000)
  {
    FormControlModel model =
      new FormControlModel(label, COMBOBOX_TYPE, id, formularMax4000);
    model.items = new Vector<>(Arrays.asList(items));
    return model;
  }

  /**
   * Liefert ein FormControlModel, das den Beginn eines neuen Tabs darstellt mit
   * gegebenem LABEL label und ID id.
   */
  public static FormControlModel createTab(String label, String id,
      FormularMax4kController formularMax4000)
  {
    FormControlModel model =
      new FormControlModel(label, TAB_TYPE, id, formularMax4000);
    model.action = "abort";
    return model;
  }

  /**
   * Liefert den FormularMax4000 zu dem dieses Model gehört.
   */
  public FormularMax4kController getFormularMax4000()
  {
    return formularMax4000;
  }

  public IdModel getId()
  {
    return id;
  }

  /**
   * Liefert den TYPE dieses FormControlModels, wobei immer eine der
   * {@link #COMBOBOX_TYPE *_TYPE Konstanten} geliefert wird, so dass == verglichen
   * werden kann.
   */
  public String getType()
  {
    return type;
  }

  /**
   * Liefert true gdw dieses FormControlModel ein Tab darstellt.
   */
  public boolean isTab()
  {
    return type.equals(TAB_TYPE);
  }

  /**
   * Liefert true gdw dieses FormControlModel eine ComboBox darstellt.
   */
  public boolean isCombo()
  {
    return type.equals(COMBOBOX_TYPE);
  }

  /**
   * Liefert true gdw dieses FormControlModel einen Button darstellt.
   */
  public boolean isButton() {
	  return type.equals(BUTTON_TYPE);
  }

  /**
   * Liefert true gdw dieses FormControlModel eine TextArea darstellt.
   */
  public boolean isTextArea()
  {
    return type.equals(BUTTON_TYPE);
  }

  /**
   * Liefert true gdw dieses FormControlModel einen Glue darstellt.
   */
  public boolean isGlue()
  {
    return type.equals(GLUE_TYPE);
  }

  /**
   * Liefert das LABEL dieses FormControlModels.
   */
  public String getLabel()
  {
    return label;
  }

  /**
   * Liefert die ACTION dieses FormControlModels. Falls keine ACTION gesetzt ist wird
   * die Konstante {@link #NO_ACTION} geliefert.
   */
  public String getAction()
  {
    return action;
  }

  /**
   * Liefert die FRAG_ID dieses FormControlModels.
   */
  public String getFragID() {
	  return this.fragId;
  }

  /**
   * Liefert den DIALOG dieses FormControlModels.
   */
  public String getDialog()
  {
    return dialog;
  }

  /**
   * Liefert den EXT dieses FormControlModels.
   */
  public String getExt()
  {
    return ext;
  }

  /**
   * Liefert URL dieses FormControlModels für openExt.
   */
  public String getUrl() {
	  return url;
  }

  /**
   * Liefert den TIP dieses FormControlModels.
   */
  public String getTooltip()
  {
    return tooltip;
  }

  /**
   * Liefert den HOTKEY dieses FormControlModels.
   */
  public char getHotkey()
  {
    return hotkey;
  }

  /**
   * Liefert das READONLY-Attribut dieses FormControlModels.
   */
  public boolean getReadonly()
  {
    return readonly;
  }

  /**
   * Liefert das WRAP-Attribut dieses FormControlModels.
   */
  public boolean getWrap()
  {
    return wrap;
  }

  /**
   * Liefert das EDIT-Attribut dieses FormControlModels.
   */
  public boolean getEditable()
  {
    return editable;
  }

  /**
   * Liefert das LINES-Attribut dieses FormControlModels.
   */
  public int getLines()
  {
    return lines;
  }

  /**
   * Liefert das MINSIZE-Attribut dieses FormControlModels.
   */
  public int getMinsize()
  {
    return minsize;
  }

  /**
   * Liefert das MAXSIZE-Attribut dieses FormControlModels.
   */
  public int getMaxsize()
  {
    return maxsize;
  }

  /**
   * Liefert die Liste der VALUES-Werte dieses FormControlModels.
   */
  public List<String> getItems()
  {
    return items;
  }

  /**
   * Liefert ein Interface zum Zugriff auf das AUTOFILL-Attribut dieses Objekts.
   */
  public FunctionSelectionAccess getAutofillAccess()
  {
    return new MyTrafoAccess(autofill);
  }

  /**
   * Liefert ein Interface zum Zugriff auf das PLAUSI-Attribut dieses Objekts.
   */
  public FunctionSelectionAccess getPlausiAccess()
  {
    return new MyTrafoAccess(plausi);
  }

  /**
   * Ersetzt den aktuellen AUTOFILL durch conf. ACHTUNG! Es wird keine Kopie von conf gemacht,
   * sondern direkt eine Referenz auf conf integriert.
   *
   * @param funcSel
   *          der "AUTOFILL"-Knoten.
   */
  public void setAutofill(FunctionSelection funcSel)
  {
    autofill = funcSel;
  }

  /**
   * Liefert true gdw ein AUTOFILL gesetzt ist.
   */
  public boolean hasAutofill()
  {
    return !autofill.isNone();
  }

  /**
   * Liefert true gdw eine PLAUSI gesetzt ist.
   */
  public boolean hasPlausi()
  {
    return !plausi.isNone();
  }

  /**
   * Liefert true gdw, die GROUPS-Angabe nicht leer ist.
   */
  public boolean hasGroups()
  {
    return groups.hasGroups();
  }

  /**
   * Liefert den GroupsProvider, der die GROUPS dieses Models managet.
   */
  public GroupsProvider getGroupsProvider()
  {
    return groups;
  }

  /**
   * Setzt das ACTION-Attribut auf action, wobei ein leerer String zu
   * {@link #NO_ACTION} konvertiert wird.
   */
  public void setAction(String action)
  {
    if (action.length() == 0) action = NO_ACTION;
    this.action = action;
  }

  /**
   * Setzt das ID-Attribut. Falls id=="", wird die ID gelöscht.
   *
   * @throws DuplicateIDException
   *           falls es bereits ein anderes FormControlModel mit der ID id gibt.
   * @throws SyntaxErrorException
   *           falls id nicht leer und keine legale ID ist.
   */
  public void setId(final String id) throws DuplicateIDException,
      SyntaxErrorException
  {
    IdModel idO = getId();
    if (id.length() == 0)
    {
      if (idO == null) return;
      idO.deactivate();
      this.id = null;
    }
    else
    {
      if (!isLegalID(id))
        throw new SyntaxErrorException(L.m("\"{0}\" is not a correct ID",
          id));
      if (idO == null)
      {
        this.id =
          formularMax4000.getIDManager().getActiveID(
            FormularMax4kController.NAMESPACE_FORMCONTROLMODEL, id);
      }
      else
      {
        idO.setID(id);
      }
    }
    notifyListeners(ID_ATTR, getId());
  }

  /**
   * Liefert true gdw, id auf {@link #ID_PATTERN} matcht.
   */
  private boolean isLegalID(String id)
  {
    return ID_PATTERN.matcher(id).matches();
  }

  /**
   * Initialisiert das Feld {@link #id} mit einer inaktiven ID basierend auf idStr
   * (falls noch keine aktive ID idStr existiert, wird idStr verwendet). Falls idStr ==
   * "", wird die ID gelöscht.
   */
  private void makeInactiveID(String idStr)
  {
    if (idStr.length() == 0)
    {
      if (id == null) return;
      id.deactivate();
      id = null;
      return;
    }
    IDManager idMan = formularMax4000.getIDManager();
    String idStr2 = idStr;
    int count = 1;
    while (true)
    {
      IdModel inactiveId = idMan.getID(FormularMax4kController.NAMESPACE_FORMCONTROLMODEL, idStr2);
      if (!inactiveId.isActive())
      {
        this.id = inactiveId;
        return;
      }
      ++count;
      idStr2 = idStr + count;
    }
  }

  /**
   * Initialisiert das Feld {@link #id} mit einer eindeutigen ID basierend auf idStr
   * (falls noch kein FormControlModel mit ID idStr existiert, wird idStr verwendet).
   * Falls idStr == "", wird die ID gelöscht.
   */
  private void makeActiveID(String idStr)
  {
    if (idStr.length() == 0)
    {
      if (id == null) return;
      id.deactivate();
      this.id = null;
      return;
    }
    IDManager idMan = formularMax4000.getIDManager();
    String idStr2 = idStr;
    int count = 1;
    while (true)
    {
      try
      {
        this.id = idMan.getActiveID(FormularMax4kController.NAMESPACE_FORMCONTROLMODEL, idStr2);
        return;
      }
      catch (DuplicateIDException e)
      {
        LOGGER.trace("", e);
      }
      ++count;
      idStr2 = idStr + count;
    }
  }

  /**
   * Setzt das LABEL-Attribut.
   */
  public void setLabel(String label)
  {
    this.label = label;
    notifyListeners(LABEL_ATTR, label);
  }

  /**
   * Setzt das TIP-Attribut.
   */
  public void setTooltip(String tooltip)
  {
    this.tooltip = tooltip;
  }

  /**
   * Setzt das HOTKEY-Attribut.
   */
  public void setHotkey(char hotkey)
  {
    this.hotkey = hotkey;
  }

  /**
   * Setzt das READONLY-Attribut.
   */
  public void setReadonly(boolean readonly)
  {
    this.readonly = readonly;
  }

  /**
   * Setzt das WRAP-Attribut.
   */
  public void setWrap(boolean wrap)
  {
    this.wrap = wrap;
  }

  /**
   * Setzt das EDIT-Attribut.
   */
  public void setEditable(boolean editable)
  {
    this.editable = editable;
  }

  /**
   * Setzt das LINES-Attribut.
   */
  public void setLines(int lines)
  {
    this.lines = lines;
  }

  /**
   * Setzt das FRAGID-Attribut.
   */
  public void setFragID(String fragId) {
	  this.fragId = fragId;
  }

  /**
   *  Setzt EXT-Attribut.
   * @param ext
   */
  public void setExt(String ext) {
	  this.ext = ext;
  }

  /**
   *  Setzt das URL-Attribut.
   * @param url
   */
  public void setUrl(String url) {
	  this.url = url;
  }

  /**
   * Setzt das MINSIZE-Attribut.
   */
  public void setMinsize(int minsize)
  {
    this.minsize = minsize;
  }

  /**
   * Setzt das MAXSIZE-Attribut.
   */
  public void setMaxsize(int maxsize)
  {
    this.maxsize = maxsize;
  }

  /**
   * Setzt das TYPE-Attribut. Dabei wird der übergebene String in eine der
   * {@link #COMBOBOX_TYPE *_TYPE-Konstanten} übersetzt. ACHTUNG! Der TYPE von Tabs
   * kann nicht verändert werden und andere Elemente können auch nicht in Tabs
   * verwandelt werden.
   */
  public void setType(String type)
  {
    if (this.type.equals(TAB_TYPE)) return; // Tabs bleiben Tabs.

    if (type.equals(TAB_TYPE))
      return; // Andere Elemente können keine Tabs werden
    else if (type.equals(COMBOBOX_TYPE))
      this.type = COMBOBOX_TYPE;
    else if (type.equals(TEXTFIELD_TYPE))
      this.type = TEXTFIELD_TYPE;
    else if (type.equals(TEXTAREA_TYPE))
      this.type = TEXTAREA_TYPE;
    else if (type.equals(LABEL_TYPE))
      this.type = LABEL_TYPE;
    else if (type.equals(SEPARATOR_TYPE))
      this.type = SEPARATOR_TYPE;
    else if (type.equals(GLUE_TYPE))
      this.type = GLUE_TYPE;
    else if (type.equals(CHECKBOX_TYPE))
      this.type = CHECKBOX_TYPE;
    else if (type.equals(BUTTON_TYPE))
      this.type = BUTTON_TYPE;
    else if (type.equals(FRAG_ID_TYPE))
    	this.type = FRAG_ID_TYPE;
    else
      this.type = UNKNOWN_TYPE;
    notifyListeners(TYPE_ATTR, this.type);
  }

  /**
   * Setzt items als neue VALUES Liste.
   */
  public void setItems(String[] items)
  {
    this.items.clear();
    for (int i = 0; i < items.length; ++i)
      this.items.add(items[i]);

    formularMax4000.comboBoxItemsHaveChanged(this);
  }

  /**
   * Liefert ein ConfigThingy, das dieses FormControlModel darstellt. Das
   * ConfigThingy wird immer neu erzeugt, kann vom Aufrufer also frei verwendet
   * werden.
   */
  public ConfigThingy export()
  {
    ConfigThingy conf = new ConfigThingy("");
    conf.add("LABEL").add(getLabel());
    conf.add("TYPE").add(getType().toLowerCase());
    if (getId() != null)
      conf.add("ID").add(getId().toString());
    conf.add("TIP").add(getTooltip());
    conf.add("READONLY").add("" + getReadonly());
    if (isCombo()) conf.add("EDIT").add("" + getEditable());
    if (isTextArea())
    {
      conf.add("LINES").add("" + getLines());
      conf.add("WRAP").add("" + getWrap());
    }
    if (isGlue() && getMinsize() > 0) conf.add("MINSIZE").add("" + getMinsize());
    if (isGlue() && getMaxsize() > 0) conf.add("MAXSIZE").add("" + getMaxsize());
    if (getAction().length() > 0) conf.add("ACTION").add("" + getAction());
    if (getDialog().length() > 0) conf.add("DIALOG").add("" + getDialog());
    if (getExt().length() > 0) conf.add("EXT").add("" + getExt());
    if (getHotkey() > 0) conf.add("HOTKEY").add("" + getHotkey());
    if (getFragID().length() > 0) conf.add("FRAG_ID").add("" + getFragID());
    if (getUrl().length() > 0) conf.add("URL").add("" + getUrl());

    List<String> values = getItems();
    if (!values.isEmpty())
    {
      ConfigThingy valuesConf = conf.add("VALUES");
      Iterator<String> iter = values.iterator();
      while (iter.hasNext())
      {
        valuesConf.add(iter.next());
      }
    }

    if (groups.hasGroups())
    {
      ConfigThingy grps = conf.add("GROUPS");
      for (IdModel gid : groups)
      {
        grps.add(gid.toString());
      }
    }

    if (!autofill.isNone()) conf.addChild(autofill.export(FormMaxConstants.AUTOFILL));

    String idStr = null;
    if (getId() != null)
      idStr = getId().toString();
    if (!plausi.isNone()) conf.addChild(plausi.export("PLAUSI", idStr));

    return conf;
  }

  /**
   * Ruft für jeden auf diesem Model registrierten {@link ModelChangeListener} die
   * Methode
   * {@link ModelChangeListener#attributeChanged(FormControlModel, int, Object)} auf.
   */
  private void notifyListeners(int attributeId, Object newValue)
  {
    for (ModelChangeListener listener : listeners)
    {
      listener.attributeChanged(this, attributeId, newValue);
    }
  }

  /**
   * Sagt dem Model, dass es einem Container hinzugefügt wurde.
   */
  public void hasBeenAdded()
  {
    makeActiveID((id == null) ? "" : id.toString());
  }

  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden
   * Container aufgerufen werden, der das Model enthält.
   */
  public void hasBeenRemoved()
  {
    if (id != null) id.deactivate();
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.modelRemoved(this);
    }
  }

  /**
   * listener wird über Änderungen des FormControlModels informiert.
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * listener wird in Zukunft nicht mehr über Änderungen des FormControlModels
   * informiert.
   */
  public void removeListener(ModelChangeListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Interface für Listener, die über Änderungen eines FormControlModels informiert
   * werden wollen.
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich geändert hat.
     *
     * @param model
     *          das FormControlModel, das sich geändert hat.
     * @param attributeId
     *          eine der {@link FormControlModel#LABEL_ATTR *_ATTR-Konstanten}.
     * @param newValue
     *          der neue Wert des Attributs. Numerische Attribute werden als Integer
     *          übergeben.
     */
    public void attributeChanged(FormControlModel model, int attributeId,
        Object newValue);

    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit in
     * keiner View mehr angezeigt werden soll).
     */
    public void modelRemoved(FormControlModel model);
  }

  /**
   * Diese Klasse leitet Zugriffe weiter an ein FunctionSelection Objekt. Bei
   * ändernden Zugriffen wird auch noch der FormularMax4000 benachrichtigt, dass das
   * Dokument geupdatet werden muss. Im Prinzip müsste korrekterweise ein ändernder
   * Zugriff auch einen Event an die ModelChangeListener schicken. Allerdings ist
   * dies derzeit nicht implementiert, weil es derzeit je genau eine View gibt für
   * AUTOFILL und PLAUSI, so dass konkurrierende Änderungen gar nicht möglich sind.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyTrafoAccess implements FunctionSelectionAccess
  {
    private FunctionSelection sel;

    public MyTrafoAccess(FunctionSelection sel)
    {
      this.sel = sel;
    }

    @Override
    public boolean isReference()
    {
      return sel.isReference();
    }

    @Override
    public boolean isExpert()
    {
      return sel.isExpert();
    }

    @Override
    public boolean isNone()
    {
      return sel.isNone();
    }

    @Override
    public String getFunctionName()
    {
      return sel.getFunctionName();
    }

    @Override
    public ConfigThingy getExpertFunction()
    {
      return sel.getExpertFunction();
    }

    @Override
    public void setParameterValues(Map<String, ParamValue> mapNameToParamValue)
    {
      sel.setParameterValues(mapNameToParamValue);
    }

    @Override
    public void setFunction(String functionName, String[] paramNames)
    {
      sel.setFunction(functionName, paramNames);
    }

    @Override
    public void setExpertFunction(ConfigThingy funConf)
    {
      sel.setExpertFunction(funConf);
    }

    @Override
    public void setParameterValue(String paramName, ParamValue paramValue)
    {
      sel.setParameterValue(paramName, paramValue);
    }

    @Override
    public String[] getParameterNames()
    {
      return sel.getParameterNames();
    }

    @Override
    public boolean hasSpecifiedParameters()
    {
      return sel.hasSpecifiedParameters();
    }

    @Override
    public ParamValue getParameterValue(String paramName)
    {
      return sel.getParameterValue(paramName);
    }

  }
}
