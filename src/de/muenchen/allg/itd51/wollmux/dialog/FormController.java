/*
* Dateiname: FormController.java
* Projekt  : WollMux
* Funktion : Stellt UI bereit, um ein Formulardokument zu bearbeiten.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 27.12.2005 | BNK | Erstellung
* 27.01.2006 | BNK | JFrame-Verwaltung nach FormGUI ausgelagert.
* 02.02.2006 | BNK | Ein/Ausblendungen begonnen
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Condition;
import de.muenchen.allg.itd51.wollmux.ConditionFactory;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.FormModel;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Stellt UI bereit, um ein Formulardokument zu bearbeiten.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormController implements UIElementEventHandler
{

  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet)
   * in Pixeln.
   */
  private final static int TF_BORDER = 4;
  
  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;
  
  /**
   * Bildet Fensternamen (umschliessender Schlüssel in der Beschreibungssprache)
   * auf {@link DialogWindow}s ab. Wird unter anderem zum Auflösen der Bezeichner
   * der switchTo-ACTION verwendet.
   */
  private Map fenster;

  /**
   * Der Name (siehe {@link #fenster}) des ersten Fensters des Dialogs,
   * das ist das erste Fenster, das in der Dialog-Beschreibung aufgeführt ist.
   */
  private String firstWindow;
  
  private JPanel contentPanel;
  
  private UIElementFactory uiElementFactory;
  
  private Map panelContext;
  private Map buttonContext;
  
  private Map mapIdToUIElement = new HashMap();
  private Map mapIdToListOfDependingUIElements = new HashMap();
  private Map mapIdToListOfDependingGroups = new HashMap();
  private Map mapGroupIdToGroup = new HashMap();
  
  private FormModel formModel;
  
  /**
   * ACHTUNG! Darf nur im Event Dispatching Thread aufgerufen werden.
   * @param model
   * @param uidesc
   * @param idToPresetValue
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormController(ConfigThingy conf, FormModel model, final Map idToPresetValue)
  throws ConfigurationErrorException
  {
    fenster = new HashMap();
    formModel = model;
    
    final ConfigThingy fensterDesc = conf.query("Fenster");
    ConfigThingy visibilityDesc = conf.query("Sichtbarkeit");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "+conf.getName());
    
    initFactories();  
    
    try{
      if (visibilityDesc.count() > 0) visibilityDesc = visibilityDesc.getLastChild();
      final ConfigThingy visDesc = visibilityDesc;
      createGUI(fensterDesc.getLastChild(), visDesc, idToPresetValue);
    }
    catch(Exception x) {Logger.error(x);}

  }

  private void createGUI(ConfigThingy fensterDesc, ConfigThingy visibilityDesc, Map mapIdToPresetValue)
  {
    Common.setLookAndFeel();
    
    contentPanel = new JPanel();
    JTabbedPane tabbedPane = new JTabbedPane();
    contentPanel.add(tabbedPane);
    
    Iterator iter = fensterDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy neuesFenster = (ConfigThingy)iter.next();
      String fensterName = neuesFenster.getName();
      String tabTitle = "Eingabe";
      try{
        tabTitle = neuesFenster.get("TITLE").toString();
      } catch(Exception x){}
      DialogWindow newWindow = new DialogWindow(fensterName, neuesFenster);
      if (firstWindow == null) firstWindow = fensterName;
      fenster.put(fensterName,newWindow);
      tabbedPane.add(tabTitle,newWindow.JPanel()); //TODO insertTab() verwenden, Tooltip und Mnemonic einführen
    }
    
    iter = visibilityDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy visRule = (ConfigThingy)iter.next();
      String groupId = visRule.getName();
      Condition cond;
      try{
        cond = ConditionFactory.getChildCondition(visRule);
      }catch(ConfigurationErrorException x)
      {
        Logger.error(x);
        continue;
      }
      
      if (!mapGroupIdToGroup.containsKey(groupId))
        mapGroupIdToGroup.put(groupId,new Group());
 
      Group group = (Group)mapGroupIdToGroup.get(groupId);
      group.condition = cond;
      
      Collection deps = cond.dependencies();
      Iterator depsIter = deps.iterator();
      while (depsIter.hasNext())
      {
        String elementId = (String)depsIter.next();
        if (!mapIdToListOfDependingGroups.containsKey(elementId))
          mapIdToListOfDependingGroups.put(elementId,new Vector(1));
        
        ((List)mapIdToListOfDependingGroups.get(elementId)).add(group);
      }
    }
    
    //TODO nachdem alle Felder erzeugt wurden, mit Default-Werten befuellen. Danach alle Plausis testen und Felder entsprechend einfärben.
  }
  

  public JPanel JPanel(){ return contentPanel;}
  
  private class DialogWindow
  {
    private JPanel myPanel;
    
    public DialogWindow(String name, ConfigThingy conf)
    {
      myPanel = new JPanel(new GridBagLayout());
      int y = 0;
      
      Iterator parentiter = conf.query("Eingabefelder").iterator();
      while (parentiter.hasNext())
      {
        Iterator iter = ((ConfigThingy)parentiter.next()).iterator();
        while (iter.hasNext())
        {
          ConfigThingy uiConf = (ConfigThingy)iter.next();
          UIElement uiElement;
          try{
            uiElement = uiElementFactory.createUIElement(panelContext, uiConf);
          } catch(ConfigurationErrorException x)
          {
            Logger.error(x);
            continue;
          }
          
          if (mapIdToUIElement.containsKey(uiElement.getId()))
            Logger.error("ID \""+uiElement.getId()+"\" mehrfach vergeben");
          
          mapIdToUIElement.put(uiElement.getId(), uiElement);
          
          ConfigThingy groupsConf = uiConf.query("GROUPS");
          Iterator groupsIter = groupsConf.iterator();
          while (groupsIter.hasNext())
          {
            ConfigThingy groups = (ConfigThingy)groupsIter.next();
            Iterator groupIter = groups.iterator();
            while (groupIter.hasNext())
            {
              String group = groupIter.next().toString();
              if (!mapGroupIdToGroup.containsKey(group))
                mapGroupIdToGroup.put(group, new Group());
              
              Group g = (Group)mapGroupIdToGroup.get(group);
              g.uiElements.add(uiElement);
            }
          }
          
          Condition cons = uiElement.getConstraints();
          if (cons != null)
          {
            Iterator consIter = cons.dependencies().iterator();
            while (consIter.hasNext())
            {
              String dependency = (String)consIter.next();
              if (!mapIdToListOfDependingUIElements.containsKey(dependency))
                mapIdToListOfDependingUIElements.put(dependency, new Vector(1));
              
              List deps = (List)mapIdToListOfDependingUIElements.get(dependency);
              deps.add(uiElement);
            }
            
            if (!mapIdToListOfDependingUIElements.containsKey(uiElement.getId()))
              mapIdToListOfDependingUIElements.put(uiElement.getId(), new Vector(1));
            
            List deps = (List)mapIdToListOfDependingUIElements.get(uiElement.getId());
            if (!deps.contains(uiElement)) deps.add(uiElement);
          }
          
          int compoX = 0;
          if (!uiElement.getLabelType().equals(UIElement.LABEL_NONE))
          {
            int labelX = 0;
            if (uiElement.getLabelType().equals(UIElement.LABEL_LEFT))
              compoX = 1;
            else
              labelX = 1;
            
            Component label = uiElement.getLabel();
            if (label != null)
            {
              GridBagConstraints gbc = (GridBagConstraints)uiElement.getLabelLayoutConstraints();
              gbc.gridx = labelX;
              gbc.gridy = y;
              myPanel.add(label, gbc);
            }
          }
          GridBagConstraints gbc = (GridBagConstraints)uiElement.getLayoutConstraints();
          gbc.gridx = compoX;
          gbc.gridy = y;
          ++y;
          myPanel.add(uiElement.getComponent(), gbc);
          
        }
      }
      
      JPanel buttonPanel = new JPanel(new GridBagLayout());
      GridBagConstraints gbcPanel = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      gbcPanel.gridx = 0;
      gbcPanel.gridy = y;
      ++y;
      myPanel.add(buttonPanel,gbcPanel);
      
      int x = 0;
      
      parentiter = conf.query("Buttons").iterator();
      while (parentiter.hasNext())
      {
        Iterator iter = ((ConfigThingy)parentiter.next()).iterator();
        while (iter.hasNext())
        {
          ConfigThingy uiConf = (ConfigThingy)iter.next();
          UIElement uiElement;
          try{
            uiElement = uiElementFactory.createUIElement(buttonContext, uiConf);
          } catch(ConfigurationErrorException e)
          {
            Logger.error(e);
            continue;
          }
          
          int compoX = x;
          if (!uiElement.getLabelType().equals(UIElement.LABEL_NONE))
          {
            int labelX = x;
            ++x;
            if (uiElement.getLabelType().equals(UIElement.LABEL_LEFT))
              compoX = x;
            else
              labelX = x;
            
            Component label = uiElement.getLabel();
            if (label != null)
            {
              GridBagConstraints gbc = (GridBagConstraints)uiElement.getLabelLayoutConstraints();
              gbc.gridx = labelX;
              gbc.gridy = 0;
              buttonPanel.add(label, gbc);
            }
            
          }
          GridBagConstraints gbc = (GridBagConstraints)uiElement.getLayoutConstraints();
          gbc.gridx = compoX;
          gbc.gridy = 0;
          ++x;
          buttonPanel.add(uiElement.getComponent(), gbc);
          
        }
      }

      
    }

    public JPanel JPanel()
    {
      return myPanel;
    }
  }
  
  

  private void initFactories()
  {
    Map mapTypeToLayoutConstraints = new HashMap();
    Map mapTypeToLabelType = new HashMap();
    Map mapTypeToLabelLayoutConstraints = new HashMap();

    //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
    GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcCombobox  = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcTextarea  = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabelLeft = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcCheckbox  = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel =     new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcButton    = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
    GridBagConstraints gbcHsep      = new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(3*TF_BORDER,0,2*TF_BORDER,0),0,0);
    GridBagConstraints gbcVsep      = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,       new Insets(0,TF_BORDER,0,TF_BORDER),0,0);
    GridBagConstraints gbcGlue      = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);

    mapTypeToLayoutConstraints.put("default", gbcTextfield);
    mapTypeToLabelType.put("default", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("default", gbcLabelLeft);
    
    mapTypeToLayoutConstraints.put("textfield", gbcTextfield);
    mapTypeToLabelType.put("textfield", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("textfield", gbcLabelLeft);
    
    mapTypeToLayoutConstraints.put("combobox", gbcCombobox);
    mapTypeToLabelType.put("combobox", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("combobox", gbcLabelLeft);
    
    mapTypeToLayoutConstraints.put("h-glue", gbcGlue);
    mapTypeToLabelType.put("h-glue", UIElement.LABEL_NONE);
    //mapTypeToLabelLayoutConstraints.put("h-glue", none);
    mapTypeToLayoutConstraints.put("v-glue", gbcGlue);
    mapTypeToLabelType.put("v-glue", UIElement.LABEL_NONE);
    //mapTypeToLabelLayoutConstraints.put("v-glue", none);
    
    mapTypeToLayoutConstraints.put("textarea", gbcTextarea);
    mapTypeToLabelType.put("textarea", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("textarea", gbcLabelLeft);
    
    mapTypeToLayoutConstraints.put("label", gbcLabel);
    mapTypeToLabelType.put("label", UIElement.LABEL_NONE);
    //mapTypeToLabelLayoutConstraints.put("label", none);
    
    mapTypeToLayoutConstraints.put("checkbox", gbcCheckbox);
    mapTypeToLabelType.put("checkbox", UIElement.LABEL_NONE);
    //mapTypeToLabelLayoutConstraints.put("checkbox", none);
    
    mapTypeToLayoutConstraints.put("button", gbcButton);
    mapTypeToLabelType.put("button", UIElement.LABEL_NONE);
    //mapTypeToLabelLayoutConstraints.put("button", none);
    
    mapTypeToLayoutConstraints.put("h-separator", gbcHsep);
    mapTypeToLabelType.put("h-separator", UIElement.LABEL_NONE);
    //mapTypeToLabelLayoutConstraints.put("h-separator", none);
    mapTypeToLayoutConstraints.put("v-separator", gbcVsep);
    mapTypeToLabelType.put("v-separator", UIElement.LABEL_NONE);
    //mapTypeToLabelLayoutConstraints.put("v-separator", none);
    
    panelContext = new HashMap();
    panelContext.put("separator","h-separator");
    panelContext.put("glue","v-glue");
    
    buttonContext = new HashMap();
    buttonContext.put("separator","v-separator");
    buttonContext.put("glue","h-glue");
    
    Set supportedActions = new HashSet();
    
    uiElementFactory = new UIElementFactory(mapTypeToLayoutConstraints,
        mapTypeToLabelType, mapTypeToLabelLayoutConstraints, supportedActions, this);

  }
  
  public void processUiElementEvent(UIElement source, String eventType, Object[] args)
  {
    System.out.println("UIElementEvent: "+eventType+" on UIElement "+source.getId());
    List dependingUIElements = (List)mapIdToListOfDependingUIElements.get(source.getId());
    if (dependingUIElements != null)
    {
      Iterator iter = dependingUIElements.iterator();
      while (iter.hasNext())
      {
        UIElement dependingUIElement = (UIElement)iter.next();
        Condition cons =  dependingUIElement.getConstraints();
        if (cons == null) continue;
        //TODO momentante Plausi-Zustand merken und Background nur ändern wenn Zustand geändert. Farben nicht fest verdrahten. WHITE aus dem standardbackground eines neuen Elements holen. PINK aus Config.
        if (cons.check(mapIdToUIElement))
          dependingUIElement.setBackground(Color.WHITE);
        else
          dependingUIElement.setBackground(Color.PINK);
      }
    }
    
    List dependingGroups = (List)mapIdToListOfDependingGroups.get(source.getId());
    if (dependingGroups != null)
    {
      Iterator iter = dependingGroups.iterator();
      while (iter.hasNext())
      {
        Group dependingGroup = (Group)iter.next();
        Condition cond =  dependingGroup.condition;
        if (cond == null) continue;
        boolean result = cond.check(mapIdToUIElement);
        if (result == dependingGroup.visible) continue;
        dependingGroup.visible = result;
        
        Iterator uiElementIter = dependingGroup.uiElements.iterator();
        while (uiElementIter.hasNext())
        {
          UIElement ele = (UIElement)uiElementIter.next();
          ele.setVisible(dependingGroup.visible);
        }
      }
    }
  }
  
  
  
  private static class Group
  {
    public List uiElements = new Vector(1);
    public Condition condition = null;
    public boolean visible = true;
  }
  

}
