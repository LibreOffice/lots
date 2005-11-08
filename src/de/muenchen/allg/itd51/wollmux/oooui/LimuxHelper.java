package de.muenchen.allg.itd51.wollmux.oooui;

import com.sun.star.beans.PropertyValue;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
/**
 * 
 * Contains misc helper methods.
 * 
 * TODO: make it singlton? allow multiple instances?
 * 
 * @author GOLOVKO
 *
 */
public class LimuxHelper
{


  /**
   * 
   * Set the Value for the <b>propName<\b> Name of the PropertyValue.
   * 
   * If PropertyValue with the name already exists, its value will be replaced.
   * If it doesn't exists, new PropertyValue with the appropriate Name and Value will be added to the original <b>set</b>.
   * The modified array of PropertyValue is returned.
   * 
   * New PropertyValue[] will be created and returned in the case the <b>set</b> is <i>null</i>.
   * 
   * @param set the original array of PropertyValues. It is left intact.
   * @param propName the Name of the PropertyValue
   * @param propValue the Value of the PropertyValue
   * @return a copy of the (modified) PropertyValue[] array <b>set</b>
   */
   public static PropertyValue[] setProperty(PropertyValue[] set, String propName, Object propValue){
     PropertyValue[] result;
     // replace Value, if exists
     result = set;
     boolean wasSet = false;
     for (int i = 0; result!=null && i < set.length; i++)
     {
       if (result[i].Name.equals(propName)){
         result[i].Value = propValue;
         wasSet = true;
       }
     }
     
     if (wasSet)
       return result;
     
     
     // increase the array, add new Name and its Value
     int lastI = 0;
     if (set==null){
       result = new PropertyValue[1];
       result[lastI] = new PropertyValue();
     } else {
       result = new PropertyValue[set.length+1];
       lastI = result.length-1;
       for (int i = 0; i < set.length; i++)
       {
         result[i] = set[i];
       }
       result[lastI] = new PropertyValue();
     }
     
     result[lastI].Name = propName;
     result[lastI].Value = propValue;
     
     return result;
   }

   /**
    * TODO: helper, what should be the return type instead of null?
    * 
    * @param set
    * @param propName
    * @return value of the PropertyValue.Value or <b>new Object()<\b>, if the value is not defined
    */
   public static Object getProperty(PropertyValue[] set, String propName){
     for (int i = 0; i < set.length; i++)
     {
       if (set[i].Name.equals(propName)){
         return set[i].Value;
       }
     }
     return new Object();
   }

   
   public static String getProperty(ConfigThingy ct, String label)
   {
     String result = "";
     ConfigThingy x = null;
     try
     {
       result = ct.query(label).toString();
       x = ct.getByChild(label);
     }
     catch (NodeNotFoundException e)
     {
       // Logger.error(e);
     }
     return result;
   }

   
}
