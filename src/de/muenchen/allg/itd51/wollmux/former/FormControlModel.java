/*
* Dateiname: FormControlModel.java
* Projekt  : WollMux
* Funktion : Repräsentiert ein Formularsteuerelement.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 07.08.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Repräsentiert ein Formularsteuerelement.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModel
{
  public static final String COMBOBOX_TYPE = "combobox";
  public static final String TEXTFIELD_TYPE = "textfield";
  public static final String TEXTAREA_TYPE = "textarea";
  public static final String TAB_TYPE = "tab";
  public static final String SEPARATOR_TYPE = "separator";
  public static final String GLUE_TYPE = "glue";
  public static final String CHECKBOX_TYPE = "checkbox";
  public static final String BUTTON_TYPE = "button";
  
  public static final String NO_ACTION = "";
  
  private String label;
  private String type;
  private String id;
  private String action = NO_ACTION;
  private String dialog = "";
  private String tooltip = "";
  private char hotkey = 0;
  private List items = new Vector(0);
  private boolean editable = false;
  private boolean readonly = false;
  private Set groups = new HashSet();
  private int lines = 4;
  private int minsize = 0;
  private ConfigThingy plausi = new ConfigThingy("PLAUSI");
  private ConfigThingy autofill = new ConfigThingy("AUTOFILL");
  
  /**
   * Parst conf als Steuerelement und erzeugt ein entsprechendes FormControlModel.
   * @param conf direkter Vorfahre von "TYPE", "LABEL", usw.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public FormControlModel(ConfigThingy conf)
  {
    label = "Steuerelement";
    type = "textfield";
    id = "";
    
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy attr = (ConfigThingy)iter.next();
      String name = attr.getName();
      String str = attr.toString();
      if (name.equals("LABEL")) label = str;
      else if (name.equals("TYPE")) setType(str);
      else if (name.equals("ID")) id = str;
      else if (name.equals("ACTION")) action = str;
      else if (name.equals("DIALOG")) dialog = str;
      else if (name.equals("TIP")) tooltip = str;
      else if (name.equals("HOTKEY")) hotkey = str.length() > 0 ? str.charAt(0) : 0;
      else if (name.equals("EDIT")) editable = str.equalsIgnoreCase("true");
      else if (name.equals("READONLY")) readonly = str.equalsIgnoreCase("true");
      else if (name.equals("LINES")) try{lines = Integer.parseInt(str); }catch(Exception x){}
      else if (name.equals("MINSIZE")) try{minsize = Integer.parseInt(str); }catch(Exception x){}
      else if (name.equals("VALUES")) items = parseValues(attr);
      else if (name.equals("GROUPS")) groups = parseGroups(attr);
      else if (name.equals("PLAUSI")) plausi = new ConfigThingy(attr);
      else if (name.equals("AUTOFILL")) autofill = new ConfigThingy(attr);
    }
  }
  
  /**
   * Liefert eine Liste, die die String-Werte aller Kinder von conf enthält. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private List parseValues(ConfigThingy conf)
  {
    Vector list = new Vector(conf.count());
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      list.add(iter.next().toString());
    }
    return list;
  }
  
  /**
   * Liefert eine Menge, die die String-Werte aller Kinder von conf enthält. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private Set parseGroups(ConfigThingy conf)
  {
    HashSet set = new HashSet(conf.count());
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      set.add(iter.next().toString());
    }
    return set;
  }
  
  private FormControlModel(String label, String type, String id)
  {
    this.label = label;
    this.type = type;
    this.id = id;
  }
  
  public static FormControlModel createCheckbox(String label, String id)
  {
    return new FormControlModel(label, CHECKBOX_TYPE, id);
  }
  
  public static FormControlModel createTextfield(String label, String id)
  {
    FormControlModel model = new FormControlModel(label, TEXTFIELD_TYPE, id);
    model.editable = true;
    return model;
  }
  
  public static FormControlModel createComboBox(String label, String id, String[] items)
  {
    FormControlModel model = new FormControlModel(label, COMBOBOX_TYPE, id);
    model.items = new Vector(Arrays.asList(items));
    return model;
  }
  
  public static FormControlModel createTab(String label, String id)
  {
    FormControlModel model = new FormControlModel(label, TAB_TYPE, id);
    model.action = "abort";
    return model;
  }
  
  public String getId()
  {
    return id;
  }
  
  public String getType()
  {
    return type;
  }
  
  public String getLabel()
  {
    return label;
  }
  
  public String getAction()
  {
    return action;
  }
  
  public String getDialog()
  {
    return dialog;
  }
  
  public String getTooltip()
  {
    return tooltip;
  }
  
  public char getHotkey()
  {
    return hotkey; 
  }
  
  public boolean getReadonly()
  {
    return readonly; 
  }
  
  public boolean getEditable()
  {
    return editable; 
  }
  
  public int getLines()
  {
    return lines; 
  }
  
  public int getMinsize()
  {
    return minsize; 
  }
  
  public List getItems()
  {
    return items; 
  }
  
  public Set getGroups()
  {
    return groups; 
  }
  
  /**
   * Ersetzt den aktuellen AUTOFILL durch conf. ACHTUNG! Es wird keine Kopie von conf
   * gemacht, sondern direkt eine Referenz auf conf integriert.
   * @param conf der "AUTOFILL"-Knoten.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setAutofill(ConfigThingy conf)
  {
    autofill = conf;
  }
  
  public void setAction(String action)
  {
    this.action = action;
  }
  
  public void setLabel(String label)
  {
    this.label = label;
  }
  
  public void setTooltip(String tooltip)
  {
    this.tooltip = tooltip;
  }
  
  public void setHotkey(char hotkey)
  {
    this.hotkey = hotkey; 
  }
  
  public void setReadonly(boolean readonly)
  {
    this.readonly = readonly;
  }
  
  public void setEditable(boolean editable)
  {
    this.editable = editable; 
  }
  
  public void setLines(int lines)
  {
    this.lines = lines; 
  }
  
  public void setMinsize(int minsize)
  {
    this.minsize = minsize; 
  }
  
  public void setType(String type)
  {
    if (type.equals(COMBOBOX_TYPE)) this.type = COMBOBOX_TYPE;
    else if (type.equals(TEXTFIELD_TYPE)) this.type = TEXTFIELD_TYPE;
    else if (type.equals(TEXTAREA_TYPE)) this.type = TEXTAREA_TYPE;
    else if (type.equals(TAB_TYPE)) this.type = TAB_TYPE;
    else if (type.equals(SEPARATOR_TYPE)) this.type = SEPARATOR_TYPE;
    else if (type.equals(GLUE_TYPE)) this.type = GLUE_TYPE;
    else if (type.equals(CHECKBOX_TYPE)) this.type = CHECKBOX_TYPE;
    else if (type.equals(BUTTON_TYPE)) this.type = BUTTON_TYPE;
    else this.type = type;
  }
  
  public ConfigThingy export()
  {
    ConfigThingy conf = new ConfigThingy("");
    conf.add("LABEL").add(getLabel());
    conf.add("TYPE").add(getType().toLowerCase());
    conf.add("ID").add(getId());
    conf.add("TIP").add(getTooltip());
    conf.add("READONLY").add(""+getReadonly());
    conf.add("EDIT").add(""+getEditable());
    conf.add("LINES").add(""+getLines());
    conf.add("MINSIZE").add(""+getMinsize());
    if (getAction().length() > 0) conf.add("ACTION").add(""+getAction());
    if (getDialog().length() > 0) conf.add("DIALOG").add(""+getDialog());
    if (getHotkey() > 0)
      conf.add("HOTKEY").add(""+getHotkey());
    
    List items = getItems();
    if (items.size() > 0)
    {
      ConfigThingy values = conf.add("VALUES");
      Iterator iter = items.iterator();
      while (iter.hasNext())
      {
        values.add(iter.next().toString());
      }
    }
    
    Set groups = getGroups();
    if (groups.size() > 0)
    {
      ConfigThingy grps = conf.add("GROUPS");
      Iterator iter = items.iterator();
      while (iter.hasNext())
      {
        grps.add(iter.next().toString());
      }
    }
    
    if (plausi.count() > 0)
      conf.addChild(new ConfigThingy(plausi));
    if (autofill.count() > 0)
      conf.addChild(new ConfigThingy(autofill));
    
    return conf; 
  }
}
