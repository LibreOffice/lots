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
* 05.05.2006 | BNK | Condition -> Function, kommentiert
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
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.FormModel;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values;

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
   * Das JPanel, das die GUI des FormControllers enthält.
   */
  private JPanel contentPanel;
  
  /**
   * Die für die Erzeugung der UI Elemente verwendete Factory.
   */
  private UIElementFactory uiElementFactory;
  
  /**
   * Die Funktionsbibliothek, die für das Interpretieren von Plausis etc, 
   * herangezogen wird.
   */
  private FunctionLibrary funcLib;
  
  /**
   * Die Dialogbibliothek, die die Dialoge liefert, die für die automatische
   * Befüllung von Formularfeldern benötigt werden.
   */
  private DialogLibrary dialogLib;
  
  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(Map, ConfigThingy)},
   * der verwendet wird für das Erzeugen der vertikal angeordneten UI Elemente, die
   * die Formularfelder darstellen.
   */
  private Map panelContext;
  
  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(Map, ConfigThingy)},
   * der verwendet wird für das Erzeugen der horizontal angeordneten Buttons
   * unter den Formularfeldern.
   */
  private Map buttonContext;
  
  /**
   * Bildet IDs auf die dazugehörigen UIElements ab.
   */
  private Map mapIdToUIElement = new HashMap();
  
  /**
   * Bildet IDs auf Lists von UIElements ab, die vom UIElement ID abhängen
   * (zum Beispiel weil ihre Plausi davon abhängt).
   */
  private Map mapIdToListOfDependingUIElements = new HashMap();
  
  /**
   * Bildet die ID eines UIElements ab auf eine List der Groups, die von
   * Änderungen des UIElements betroffen sind (zum Beispiel weil die 
   * Sichtbarkeitsfunktion der Gruppe von dem UIElement abhängt).
   */
  private Map mapIdToListOfDependingGroups = new HashMap();
  
  /**
   * Bildet GROUPS Bezeichner auf die entsprechenden Group-Instanzen ab.
   */
  private Map mapGroupIdToGroup = new HashMap();
  
  /**
   * Die Inhalte der UIElemente aus {@link #mapIdToUIElement} als Values zur
   * Verfügung gestellt.
   */
  private Values myUIElementValues = new UIElementMapValues(mapIdToUIElement);
  
  /**
   * Das Writer-Dokument, das zum Formular gehört (gekapselt als FormModel).
   */
  private FormModel formModel;
  
  /**
   * ACHTUNG! Darf nur im Event Dispatching Thread aufgerufen werden.
   * @param conf der Formular-Knoten, der die Formularbeschreibung enthält.
   * @param model das zum Formular gehörende Writer-Dokument (gekapselt als FormModel)
   * @param idToPresetValue bildet IDs von Formularfeldern auf Vorgabewerte ab.
   *        Falls hier ein Wert für ein Formularfeld vorhanden ist, so wird dieser
   *        allen anderen automatischen Befüllungen vorgezogen.
   * @param funcLib die Funktionsbibliothek, die zur Auswertung von Plausis etc.
   *        herangezogen werden soll.
   * @param dialogLib die Dialogbibliothek, die die Dialoge bereitstellt, die
   *        für automatisch zu befüllende Formularfelder benötigt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormController(ConfigThingy conf, FormModel model, final Map idToPresetValue, 
      FunctionLibrary funcLib, DialogLibrary dialogLib)
  throws ConfigurationErrorException
  {
    this.formModel = model;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;
    
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

  /**
   * Liefert ein JPanel, das das gesamte GUI des FormControllers enthält.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public JPanel JPanel(){ return contentPanel;}

  
  /**
   * Baut die GUI auf (darf nur im EDT aufgerufen werden).
   * @param fensterDesc der Fenster()-Knoten der Formularbeschreibung.
   * @param visibilityDesc der Sichtbarkeit-Knoten der Formularbeschreibung oder
   *               ein leeres ConfigThingy falls der Knoten nicht existiert.
   * @param mapIdToPresetValue bildet IDs von Formularfeldern auf Vorgabewerte ab.
   *        Falls hier ein Wert für ein Formularfeld vorhanden ist, so wird dieser
   *        allen anderen automatischen Befüllungen vorgezogen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  private void createGUI(ConfigThingy fensterDesc, ConfigThingy visibilityDesc, Map mapIdToPresetValue)
  {
    Common.setLookAndFeel();
    
    contentPanel = new JPanel();
    JTabbedPane tabbedPane = new JTabbedPane();
    contentPanel.add(tabbedPane);
    
    /********************************************************
     * Tabs erzeugen.
     ******************************************************/
    Iterator iter = fensterDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy neuesFenster = (ConfigThingy)iter.next();
      String tabTitle = "Eingabe";
      try{
        tabTitle = neuesFenster.get("TITLE").toString();
      } catch(Exception x){}
      DialogWindow newWindow = new DialogWindow(neuesFenster);
      tabbedPane.add(tabTitle,newWindow.JPanel()); //TODO insertTab() verwenden, Tooltip und Mnemonic einführen
    }
    
    /************************************************************
     * Sichtbarkeit auswerten. 
     ***********************************************************/
    iter = visibilityDesc.iterator();
    while (iter.hasNext())
    {
      ConfigThingy visRule = (ConfigThingy)iter.next();
      String groupId = visRule.getName();
      Function cond;
      try{
        cond = FunctionFactory.parseChildren(visRule, funcLib, dialogLib);
      }catch(ConfigurationErrorException x)
      {
        Logger.error(x);
        continue;
      }
      
      if (!mapGroupIdToGroup.containsKey(groupId))
        mapGroupIdToGroup.put(groupId,new Group());
 
      Group group = (Group)mapGroupIdToGroup.get(groupId);
      group.condition = cond;
      
      String[] deps = cond.parameters();
      for (int i = 0; i < deps.length; ++i)
      {
        String elementId = deps[i];
        if (!mapIdToListOfDependingGroups.containsKey(elementId))
          mapIdToListOfDependingGroups.put(elementId,new Vector(1));
        
        ((List)mapIdToListOfDependingGroups.get(elementId)).add(group);
      }
    }
    
    //TODO nachdem alle Felder erzeugt wurden, mit Default-Werten befuellen. Danach alle Plausis testen und Felder entsprechend einfärben.
  }
  

  /**
   * Ein Tab der Formular-GUI.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class DialogWindow
  {
    /**
     * Das Panel das die GUI-Elemente enthält.
     */
    private JPanel myPanel;
    
    /**
     * Erzeugt ein neues Tab.
     * @param conf der Kind-Knoten des Fenster-Knotens der das Formular beschreibt.
     *        conf ist direkter Elternknoten des Knotens "Eingabefelder".
     * @author Matthias Benkmann (D-III-ITD 5.1)     
     */
    public DialogWindow(ConfigThingy conf)
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
            uiElement.setConstraints(FunctionFactory.parseGrandchildren(uiConf.query("PLAUSI"), funcLib, dialogLib));
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
          
          /**
           * Falls das Element eine Plausi hat.
           */
          Function cons = uiElement.getConstraints();
          if (cons != null)
          {
            /**
             * Für alle Felder von denen die Plausi abhängt das uiElement in
             * die Liste der abhängigen UI Elemente einfügen. 
             */
            String[] consParams = cons.parameters();
            for (int i = 0; i < consParams.length; ++i)
            {
              String dependency = consParams[i];
              if (!mapIdToListOfDependingUIElements.containsKey(dependency))
                mapIdToListOfDependingUIElements.put(dependency, new Vector(1));
              
              List deps = (List)mapIdToListOfDependingUIElements.get(dependency);
              deps.add(uiElement);
            }
            
            /**
             * Dafür sorgen, dass uiElement immer in seiner eigenen Abhängigenliste
             * steht, damit bei jeder Änderung an uiElement auf jeden Fall die
             * Plausi neu ausgewertet wird, auch wenn sie nicht von diesem Element
             * abhängt. Man denke sich zum Beispiel einen Zufallsgenerator als Plausi.
             * Er hängt zwar nicht vom Wert des Felds ab, sollte aber bei jeder
             * Änderung des Feldes erneut befragt werden.
             */
            if (!mapIdToListOfDependingUIElements.containsKey(uiElement.getId()))
              mapIdToListOfDependingUIElements.put(uiElement.getId(), new Vector(1));
            List deps = (List)mapIdToListOfDependingUIElements.get(uiElement.getId());
            if (!deps.contains(uiElement)) deps.add(uiElement);
          }
          
          /********************************************************************
           * UI Element und evtl. vorhandenes Zusatzlabel zum GUI hinzufügen.
           *********************************************************************/
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
      
      
      /*****************************************************************************
       * Für die Buttons ein eigenes Panel anlegen und mit UIElementen befüllen. 
       *****************************************************************************/
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

    /**
     * Liefert das Panel zurück, dass den ganzen Inhalt dieses Tabs enthält.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public JPanel JPanel()
    {
      return myPanel;
    }
  }
  
  
  /**
   * Initialisiert die UIElementFactory, die zur Erzeugung der UIElements
   * verwendet wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
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
  
  /**
  * Die zentrale Anlaufstelle für alle von UIElementen ausgelösten Events. 
  * @param source
  * @param eventType
  * @param args
  * @author Matthias Benkmann (D-III-ITD 5.1)
  * TODO Testen
  */
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
        Function cons =  dependingUIElement.getConstraints();
        if (cons == null) continue;
        //TODO momentante Plausi-Zustand merken und Background nur ändern wenn Zustand geändert. Farben nicht fest verdrahten. WHITE aus dem standardbackground eines neuen Elements holen. PINK aus Config.
        if (cons.getBoolean(myUIElementValues))
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
        Function cond =  dependingGroup.condition;
        if (cond == null) continue;
        boolean result = cond.getBoolean(myUIElementValues);
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
  
  
  /**
   * Eine Sichtbarkeitsgruppe von UIElementen.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class Group
  {
    /**
     * Die Mitglieder der Gruppe.
     */
    public List uiElements = new Vector(1);
    
    /**
     * Die Bedingung für die Sichtbarkeit (true = sichtbar) oder null, wenn
     * keine Sichtbarkeitsbedingung definiert.
     */
    public Function condition = null;
    
    /**
     * true, wenn die Gruppe im Augenblick sichtbar ist.
     */
    public boolean visible = true;
  }
  
  /**
   * Stellt den Inhalt einer Map von IDs auf UIElemente als Values zur
   * Verfügung.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementMapValues implements Values
  {
    private Map mapIdToUIElement;
    
    public UIElementMapValues(Map mapIdToUIElement)
    {
      this.mapIdToUIElement = mapIdToUIElement;
    }

    public boolean hasValue(String id)
    {
      return mapIdToUIElement.containsKey(id);
    }

    public String getString(String id)
    {
      UIElement uiElement = (UIElement)mapIdToUIElement.get(id);
      if (uiElement == null) return "";
      return uiElement.getString();
    }

    public boolean getBoolean(String id)
    {
      UIElement uiElement = (UIElement)mapIdToUIElement.get(id);
      if (uiElement == null) return false;
      return uiElement.getBoolean();
    }
  }

}
