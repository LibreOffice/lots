/*
* Dateiname: TrafoDialogParameters.java
* Projekt  : WollMux
* Funktion : Speichert diverse Parameter für den Aufruf von TrafoDialogen.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 01.02.2008 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Speichert diverse Parameter für den Aufruf von TrafoDialogen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TrafoDialogParameters
{
  /**
   * Die Beschreibung der Funktion, mit der der Dialog vorbelegt werden soll. Oberster Knoten
   * ist ein beliebiger Bezeichner (typischerweise der Funktionsname). Das ConfigThingy kann
   * also direkt aus einem Funktionen-Abschnitt eines Formulars übernommen werden.
   */
  public ConfigThingy conf;
  
  /**
   * Für Dialoge, die Feldnamen zur Auswahl stellen gibt diese Liste an, welche Namen
   * angeboten werden sollen.
   */
  public List fieldNames;
  
  /**
   * Erzeugt ein neues TrafoDialogParameters-Objekt. Alle Parameter dürfen null sein.
   * Alle Parameter werden kopiert. Änderungen an den Objekten nach dem Konstruktoraufruf
   * schlagen also nicht auf das TrafoDialogParameters-Objekt durch.
   * 
   * @param funConf
   *          Die Beschreibung der Funktion, mit der der Dialog vorbelegt werden
   *          soll. Oberster Knoten ist ein beliebiger Bezeichner
   *          (typischerweise der Funktionsname). Das ConfigThingy kann also
   *          direkt aus einem Funktionen-Abschnitt eines Formulars übernommen
   *          werden.
   * @param fieldNames
   *          Für Dialoge, die Feldnamen zur Auswahl stellen gibt diese Liste
   *          von Strings an, welche Namen angeboten werden sollen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public TrafoDialogParameters(ConfigThingy funConf, List fieldNames)
  {
    if (funConf != null) this.conf = new ConfigThingy(funConf);
    if (fieldNames != null) this.fieldNames = new Vector(fieldNames);
  }
  
  public String toString()
  {
    StringBuilder buffy = new StringBuilder();
    buffy.append("conf: ");
    if (conf == null) 
      buffy.append("null");
    else
      buffy.append(conf.stringRepresentation());
    
    buffy.append("\nfieldNames: ");
    
    if (fieldNames == null)
      buffy.append("null");
    else 
    {
      Iterator iter = fieldNames.iterator();
      while (iter.hasNext())
      {
        buffy.append(iter.next().toString());
        if (iter.hasNext()) buffy.append(", ");
      }
    }
    return buffy.toString();
  }
}
