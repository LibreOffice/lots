/*
* Dateiname: FormControlModelList.java
* Projekt  : WollMux
* Funktion : Verwaltet eine Liste von FormControlModels.
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

import java.util.Iterator;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Verwaltet eine Liste von FormControlModels.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormControlModelList
{
  private Vector models = new Vector();
  
  /**
   * Löscht alle bestehenden FormControlModels aus der Liste.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void clear()
  {
    models.clear();
  }
  
  /**
   * Macht aus str einen Identifier, der noch von keinem FormControlModel dieser Liste
   * verwendet wird und liefert diesen Identifier zurück.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public String makeUniqueId(String str)
  {
    Iterator iter = models.iterator();
    int count = 0;
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      String id = model.getId();
      if (id.startsWith(str))
      {
        if (count == 0) ++count;
        String suffix = id.substring(str.length());
        try{
          int idx = Integer.parseInt(suffix);
          if (idx >= count) count = idx + 1;
        }catch(Exception x){}
      }
    }
    
    if (count > 0) 
      return str + count;
    else
      return str;
  }
  
  /**
   * Falls idx >= 0 wird model an Index idx in die Liste eingefügt 
   * (das Element das sich vorher an diesem Index befand hat danach Index idx+1);
   * falls idx < 0 wird model an das Ende der Liste angehängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(FormControlModel model, int idx)
  {
    if (idx < 0)
      models.add(model);
    else
      models.add(idx, model);
  }
  
  /**
   * model wird an das Ende der Liste angehängt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void add(FormControlModel model)
  {
    models.add(model);
  }
  
  public ConfigThingy export()
  {
    ConfigThingy export = new ConfigThingy("Fenster");
    ConfigThingy conf = export;
    ConfigThingy tabConf = export;
    
    int phase = 0; //0: tab start, 1: Eingabefelder, 2: Buttons
    FormControlModel currentTab = FormControlModel.createTab("Eingabe", "Reiter1");
    Iterator iter = models.iterator();
    while (iter.hasNext())
    {
      FormControlModel model = (FormControlModel)iter.next();
      if (phase == 0 && model.getType() == FormControlModel.TAB_TYPE)
        currentTab = model;
      else if (phase > 0 && model.getType() == FormControlModel.TAB_TYPE)
      {
        currentTab = model;
        conf = export;
        phase = 0;
      }
      else if (phase == 0 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        tabConf = outputTab(currentTab, export);
        conf = tabConf.add("Buttons");
        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 1 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        conf = tabConf.add("Buttons");
        conf.addChild(model.export());
        phase = 2;
      }
      else if (phase == 2 && model.getType() == FormControlModel.BUTTON_TYPE)
      {
        conf.addChild(model.export());
      }
      else if (phase == 0)
      {
        tabConf = outputTab(currentTab, export);
        conf = tabConf.add("Eingabefelder");
        conf.addChild(model.export());
        phase = 1;
      } 
      else if (phase >= 1)
      {
        conf.addChild(model.export());
      }
    }
  
    return export;
  }
  
  private ConfigThingy outputTab(FormControlModel tab, ConfigThingy conf)
  {
    conf = conf.add(tab.getId());
    conf.add("TITLE").add(tab.getLabel());
    conf.add("CLOSEACTION").add(tab.getAction());
    conf.add("TIP").add(tab.getTooltip());
    char hotkey = tab.getHotkey();
    if (hotkey > 0)
      conf.add("HOTKEY").add(""+hotkey);
    
    return conf;
  }
  
  
  
  

}
