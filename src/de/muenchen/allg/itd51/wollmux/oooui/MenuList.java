package de.muenchen.allg.itd51.wollmux.oooui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * TODO: add important checks, where the "null" is possible; e.g. calls to "topMenu.get("MENU")", where the 
 *       MENU value can be unavailable
 * @author GOLOVKO
 *
 */

public class MenuList
{
  protected static int BUTTON = 1;
  protected static int MENU = 1;
  private Hashtable htIdToMenu = new Hashtable();
  private List topLevelMenues = new ArrayList();

  /**
   * Der Konstruktor erzeugt eine neue MenuList aus einer
   * gegebenen Konfiguration.
   * 
   * @param root
   *          Wurzel des Konfigurationsbaumes der Konfigurationsdatei.
   * @throws NodeNotFoundException
   */
  public MenuList(ConfigThingy root)
      throws NodeNotFoundException
  {
    // 0. read in the names of all top-level menues.
    Iterator mlIter = root.getByChild("Menueleiste").getByChild("LABEL").iterator();
    while (mlIter.hasNext())
    {
      ConfigThingy topMenu = (ConfigThingy) mlIter.next();
      topLevelMenues.add(topMenu.get("MENU").toString());
    }
    
    List menues = new ArrayList();
    // 1. use .query() to get all Menues fragments  
    
    Iterator menuesIter = root.query("Menues").iterator();
    // 2. iterate over "Menues"
      while (menuesIter.hasNext()){
        // each element is "Menues(...)"
        ConfigThingy menue = (ConfigThingy) menuesIter.next();
        Iterator lhmVorlageIter = menue.iterator();
//        htIdToMenu.put()
        // 2.1 iterate over "vorlagen" inside of the single menu
        while (lhmVorlageIter.hasNext()){
          // each element smthng like "LHMVorlagen(...)"
          ConfigThingy lhmVorlage = (ConfigThingy)lhmVorlageIter.next();
          menues = getMenueItems(lhmVorlage);
          // 2.2 put something like "LHMVorlagen"=> <array of Menu> into hash 
          //     for sake of uniquness
          htIdToMenu.put(lhmVorlage.getName(),menues);
        }   
    }
     
      for (Iterator iter = topLevelMenues.iterator(); iter.hasNext();)
      {
        String element = (String) iter.next();
        printMenu(element);
      }
      
  }
  
  private void printMenu(String sMenu){
    List arr1 = (List)htIdToMenu.get(sMenu);
    for (Iterator iter1 = arr1.iterator(); iter1.hasNext();)
    {
      // menu can be of "TYPE" either "button" or "menu" 
      ConfigThingy menu = (ConfigThingy) iter1.next();
      System.out.println("> "+getProperty(menu, "LABEL"));
      if (getProperty(menu,"TYPE").equals("menu")){
        List arr2 = (List)htIdToMenu.get(getProperty(menu,"MENU"));
        if (arr2==null){
          System.out.println("\t> EMPTY");
          continue;
        }
        Iterator iter2 = arr2.iterator();
        while(iter2.hasNext())
        {
          ConfigThingy element = (ConfigThingy) iter2.next();
          System.out.println("\t> "+getProperty(element, "LABEL"));
        }
        
      }
//      Logger.debug(getProperty(menu,"LABEL"));
    }
    
  }
  
  private String getProperty(ConfigThingy ct, String label){
    String result="";
    try
    {
      result = ct.get(label).toString();
    }
    catch (NodeNotFoundException e)
    {
//      Logger.error(e);
    }
    return result;
  }
  
  private List getMenueItems(ConfigThingy lhmVorlage) throws NodeNotFoundException{
    ArrayList results = new ArrayList();
    // an element is smth like "LHMVorlagen(...)"
    Iterator iter1 = lhmVorlage.getByChild("TYPE").iterator();
    while (iter1.hasNext()){
      // every element is something like "Element(...)" entry
      ConfigThingy child1 = (ConfigThingy)iter1.next();
//      Logger.debug(child1.get("LABEL").toString());
      results.add(child1);
    }
    return results;
  }
  





  /**
   * Testet die Funktionsweise der MenuList. Eine in url
   * angegebene Konfigdatei wird eingelesen und die dazugehörige
   * MenuList erstellt. Anschliessend wird die ausgegeben.
   * 
   * @param args
   *          url, dabei ist url die URL einer zu lesenden Config-Datei. Das
   *          Programm gibt die Liste der Textfragmente aus.
   * @author Christoph Lutz (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws IOException
  {
    try
    {
      if (args.length < 1)
      {
        System.out.println("USAGE: <url>");
        System.exit(0);
      }
      Logger.init(Logger.DEBUG);

      File cwd = new File(".");

      args[0] = args[0].replaceAll("\\\\", "/");
      ConfigThingy conf = new ConfigThingy(args[0], 
          new URL(cwd.toURL(), args[0]));

      MenuList tfrags;
      // 1. initialize the menu hierarchy 
      tfrags = new MenuList(conf);
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
    System.exit(0);
  }
  
 
//  // private classes
//  /*
//   * Holder for the data, which is related to menu (e.g. Frag_Id)
//   */
//  private class Menu{
//    
//    private Hashtable htPropToValue = new Hashtable();
//    
//
//    protected Menu(){
//      
//    }
//    
//    protected Menu getSubMenu(){
//      return null;
//    }
//    
//    protected void setProperty(String propName, String propValue){
//      //TODO add not-null checks
//      htPropToValue.put(propName,propValue);
//    }
//    
//    protected String getProperty(String propName){
//      //TODO add not-null checks
//      return (String)htPropToValue.get(propName);
//    }
//    
//  }
//  
}

