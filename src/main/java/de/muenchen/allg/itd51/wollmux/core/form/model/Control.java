package de.muenchen.allg.itd51.wollmux.core.form.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.functions.Values.SimpleMap;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Beschreibung eines Formularelementes samt Business-Logik (Plausis)
 *
 * @author daniel.sikeler
 *
 */
public class Control
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Control.class);

  /**
   * Die Id.
   */
  private String id;
  /**
   * Das Label.
   */
  private String label;
  /**
   * Der Typ.
   */
  private FormType type;
  /**
   * Ein Tipp, kann null sein.
   */
  private String tip;
  /**
   * Eine Liste von Optionen für Comboboxen. Kann null sein.
   */
  private List<String> options = new ArrayList<>();
  /**
   * Anzahl der Zeilen für Textareas.
   */
  private int lines = 3;
  /**
   * Wird der Text automatischen umgebrochen in Textareas?
   */
  private boolean wrap = true;
  /**
   * Ist das Formularelement bearbeitbar?
   */
  private boolean readonly = false;
  /**
   * Können einer Combobox weitere Element hinzugefügt werden?
   */
  private boolean editable = false;
  /**
   * Die mindest Größe.
   */
  private int minsize = 0;
  /**
   * Die bevorzugte Größe.
   */
  private int prefsize = 0;
  /**
   * Die maximale Größe.
   */
  private int maxsize = Integer.MAX_VALUE;
  /**
   * Der Hotkey.
   */
  private char hotkey = 0;
  /**
   * Die Aktion, die beim Drücken des Buttons ausgelöst wird.
   */
  private String action;
  /**
   * Der Dialog, der bei der Aktion funcDialog geöffnet wird.
   */
  private String dialog;
  /**
   * Die externe Anwendung, die ausgeführt wird.
   */
  private String ext;
  /**
   * Die URL, die mit der externen Anwendung ausgeführt werden soll.
   */
  private String url;
  /**
   * Die Plausi.
   */
  private Optional<Function> plausi = Optional.ofNullable(null);
  /**
   * Das zu aktivierende Dialogfenster für die Aktion switchWindow.
   */
  private String window;
  /**
   * Die FragId für die Aktionen openTemplate und openDocument.
   */
  private String fragId;
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

  public String getId()
  {
    return id;
  }

  public String getLabel()
  {
    return label;
  }

  public FormType getType()
  {
    return type;
  }

  public String getTip()
  {
    return tip;
  }

  public List<String> getOptions()
  {
    return options;
  }

  /**
   * Fügt eine neue Option hinzu. Wenn bisher noch keine Option gesetzt wurde, dann wird die Option
   * zusätzlich als value gesetzt.
   *
   * @param option
   *          Die Option.
   */
  public void addOption(String option)
  {
    if (options.isEmpty())
    {
      value = option;
    }
    options.add(option);
  }

  public int getLines()
  {
    return lines;
  }

  public boolean isWrap()
  {
    return wrap;
  }

  public boolean isReadonly()
  {
    return readonly;
  }

  public boolean isEditable()
  {
    return editable;
  }

  public int getMinsize()
  {
    return minsize;
  }

  public char getHotkey()
  {
    return hotkey;
  }

  public String getAction()
  {
    return action;
  }

  public String getDialog()
  {
    return dialog;
  }

  public String getExt()
  {
    return ext;
  }

  public Optional<Function> getPlausi()
  {
    return plausi;
  }

  public int getPrefsize()
  {
    return prefsize;
  }

  public int getMaxsize()
  {
    return maxsize;
  }

  public String getWindow()
  {
    return window;
  }

  public String getFragId()
  {
    return fragId;
  }

  public String getUrl()
  {
    return url;
  }

  public boolean isOkay()
  {
    return okay;
  }

  public Optional<Function> getAutofill()
  {
    return autofill;
  }

  public String getValue()
  {
    return value;
  }

  public void addDependingAutoFillFormField(Control control)
  {
    dependingAutoFillFormFields.add(control);
  }

  public void addDependingPlausiFormField(Control control)
  {
    dependingPlausiFormFields.add(control);
  }

  public List<VisibilityGroup> getDependingGroups()
  {
    return dependingGroups;
  }

  public void addDependingGroup(VisibilityGroup group)
  {
    if (!dependingGroups.contains(group))
    {
      dependingGroups.add(group);
    }
  }

  /**
   * Setzt den Wert des Formularelementes, berechnet die Sichtbarkeiten neu und informartiert die
   * Listener.
   *
   * @param value
   *          Der neue Wert.
   * @param values
   *          Die neuen Werte des Models.
   */
  public void setValue(String value)
  {
    if (options != null && !options.isEmpty() && !options.contains(value))
    {
      this.value = options.get(0);
    } else
    {
      this.value = value;
    }
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
   *         muss mit {@link #setValue(String, SimpleMap)} gesetzt werden.
   */
  public String computeValue(SimpleMap values)
  {
    if (autofill.isPresent())
      return autofill.get().getString(values);
    else
      return "";
  }

  /**
   * Überprüft ob das Element, ein gültigen Wert enthält und informiert die Listener.
   *
   * @param values
   *          Die Werte aus dem Model.
   */
  public void setOkay(Values values)
  {
    boolean newOkay = plausi.orElse(FunctionFactory.alwaysTrueFunction()).getBoolean(values);
    if (newOkay == okay)
      return;

    okay = newOkay;
  }

  public List<VisibilityGroup> getGroups()
  {
    return groups;
  }

  /**
   * Parst ein Formularelement.
   *
   * @param controlConf
   *          Die Beschreibung des Fromularelementes.
   * @return Ein Control, das dem Formularelement entspricht.
   * @throws FormModelException
   *           Unbekannte Type-Angabe.
   */
  public static Control create(ConfigThingy controlConf, FormModel model) throws FormModelException
  {
    Control control = new Control();
    control.type = FormType.getType(controlConf.getString("TYPE", ""));
    control.id = controlConf.getString("ID", "");
    control.label = controlConf.getString("LABEL", "");
    control.tip = controlConf.getString("TIP", "");
    String hotkeyString = controlConf.getString("HOTKEY", "");
    if (!hotkeyString.isEmpty())
    {
      control.hotkey = hotkeyString.toUpperCase().charAt(0);
    }
    control.action = controlConf.getString("ACTION", null);
    control.dialog = controlConf.getString("DIALOG", null);
    control.window = controlConf.getString("WINDOW", null);
    ConfigThingy fids = controlConf.query("FRAG_ID");
    StringBuilder fragId = new StringBuilder();
    Iterator<ConfigThingy> i = fids.iterator();
    if (i.hasNext())
    {
      fragId.append(i.next().toString());
      while (i.hasNext())
      {
        fragId.append("&");
        fragId.append(i.next().toString());
      }
    }
    control.fragId = fragId.toString();
    control.ext = controlConf.getString("EXT", null);
    control.url = controlConf.getString("URL", null);
    control.readonly = Boolean.parseBoolean(controlConf.getString("READONLY", ""));
    control.editable = Boolean.parseBoolean(controlConf.getString("EDIT", ""));
    control.lines = Integer.parseInt(controlConf.getString("LINES", "3"));
    control.wrap = Boolean.parseBoolean(controlConf.getString("WRAP", "true"));
    control.minsize = Integer.parseInt(controlConf.getString("MINSIZE", "0"));
    control.prefsize = Integer.parseInt(controlConf.getString("PREFSIZE", "0"));
    control.maxsize = Integer.parseInt(controlConf.getString("MAXSIZE", "" + Integer.MAX_VALUE));
    try
    {
      for (ConfigThingy val : controlConf.get("VALUES"))
      {
        control.addOption(val.toString());
      }
    } catch (NodeNotFoundException x)
    {
      if (control.getType() == FormType.COMBOBOX)
      {
        LOGGER.error(L.m("Fehlerhaftes Element des Typs \"combobox\""), x);
      }
    }
    if (model != null)
    {
      control.plausi = Optional.ofNullable(model.createFunction(controlConf.query("PLAUSI")));
      control.autofill = Optional.ofNullable(model.createFunction(controlConf.query("AUTOFILL")));
      ConfigThingy groupsConf = controlConf.query("GROUPS");
      for (ConfigThingy groups : groupsConf)
      {
        for (ConfigThingy group : groups)
        {
          String groupId = group.toString();
          VisibilityGroup g = model.getGroup(groupId);
          control.groups.add(g);
        }
      }
    } else
    {
      control.plausi = Optional.ofNullable(null);
      control.autofill = Optional.ofNullable(null);
    }
    return control;
  }
}
