/*
* Dateiname: SimpleDataset.java
* Projekt  : WollMux
* Funktion : Eine simple Implementierung des Interfaces Dataset.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 06.12.2005 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Eine simple Implementierung des Interfaces Dataset.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class SimpleDataset implements Dataset
{
  private Map data;
  private String key;
  
   /**
    * Erzeugt ein SimpleDataset, das eine Kopie von ds ist. Das erzeugte
    * SimpleDataset ist von schema und ds unabhängig und hält keine Verknüpfungen
    * darauf.
    * @param schema enthält die Namen aller zu kopierenden Spalten
    * @param ds der zu kopierende Datensatz.
    * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws ColumnNotFoundException falls eine Spalte aus schema dem 
   *         Datensatz ds nicht bekannt ist
    */
  public SimpleDataset(Collection schema, Dataset ds) throws ColumnNotFoundException
  {
    key = ds.getKey();
    data = new HashMap();
    Iterator iter = schema.iterator();
    while (iter.hasNext())
    {
      String spalte = (String)iter.next();
      data.put(spalte, ds.get(spalte));
    }
  }
  
   
  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Dataset#get(java.lang.String)
   */
  public String get(String columnName) throws ColumnNotFoundException
  {
    if (!data.containsKey(columnName)) throw new ColumnNotFoundException("Datensatz kennt Spalte \""+columnName+"\" nicht!");
    return (String)data.get(columnName);
  }

  /* (non-Javadoc)
   * @see de.muenchen.allg.itd51.wollmux.db.Dataset#getKey()
   */
  public String getKey()
  {
    return key;
  }
}
