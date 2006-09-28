/*
* Dateiname: InsertionModel.java
* Projekt  : WollMux
* Funktion : Stellt eine Einfügestelle im Dokument (insertValue oder insertFormValue) dar.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 06.09.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.text.XBookmarksSupplier;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.Bookmark;

/**
 * Stellt eine Einfügestelle im Dokument (insertValue oder insertFormValue) dar.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class InsertionModel
{
  /**
   * Pattern zum Erkennen von insertValue und insertFormValue-Bookmarks.
   */
  public static final Pattern INSERTION_BOOKMARK = Pattern.compile("\\A\\s*(WM\\s*\\(.*CMD\\s*'((insertValue)|(insertFormValue))'.*\\))\\s*\\d*\\z");
  
  /**
   * Attribut-ID-Konstante für {@link ModelChangeListener#attributeChanged(InsertionModel, int, Object)}.
   */
  public static final int ID_ATTR = 0;
  
  /** 
   * Konstante für {@link #sourceType}, die angibt, dass die Daten für die Einfügung
   * aus einer externen Datenquelle kommen. 
   */
  private static final int DATABASE_TYPE = 0;
  
  /** 
   * Konstante für {@link #sourceType}, die angibt, dass die Daten für die Einfügung
   * aus dem Formular kommen. 
   */
  private static final int FORM_TYPE = 1;
  
  /**
   * Gibt an, um woher die Einfügung ihre Daten bezieht.
   * @see #FORM_TYPE
   * @see #DATABASE_TYPE
   */
  private int sourceType = FORM_TYPE;
  
  /**
   * DB_SPALTE oder ID je nach {@link #sourceType}.
   */
  private String dataId = "";
  
  /**
   * Das Bookmarks, das diese Einfügestelle umschließt.
   */
  private Bookmark bookmark;
  
  /**
   * Die TRAFO für diese Einfügung.
   */
  private FunctionSelection trafo;
  
  /**
   * Die {@link ModelChangeListener}, die über Änderungen dieses Models informiert werden wollen.
   */
  private List listeners = new Vector(1);

  /**
   * Erzeugt ein neues InsertionModel für das Bookmark mit Namen bookmarkName, das bereits
   * im Dokument vorhanden sein muss.
   * @param doc das Dokument in dem sich das Bookmark befindet
   * @param funcSelections ein FunctionSelectionProvider, der für das TRAFO Attribut eine passende
   *        FunctionSelection liefern kann.
   * @throws SyntaxErrorException wenn bookmarkName nicht korrekte ConfigThingy-Syntax hat oder
   *         kein korrektes Einfügekommando ist.
   * @throws NoSuchElementException wenn ein Bookmark dieses Namens in doc nicht existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public InsertionModel(String bookmarkName, XBookmarksSupplier doc, FunctionSelectionProvider funcSelections) throws SyntaxErrorException, NoSuchElementException
  {
    bookmark = new Bookmark(bookmarkName,doc);
    String confStr = bookmarkName.replaceAll("\\d*\\z",""); //eventuell vorhandene Ziffern am Ende löschen
    URL url = null;
    try{
      url = new URL("file:///");
    }catch(MalformedURLException x){}
    
    ConfigThingy conf;
    try{
      conf = new ConfigThingy("INSERT", url, new StringReader(confStr));
    }catch(IOException x)
    {
      throw new SyntaxErrorException(x);
    }
    
    String cmd = conf.query("CMD").toString();
    if (cmd.equals("insertValue"))
    {
      ConfigThingy dbSpalteConf = conf.query("DB_SPALTE");
      if (dbSpalteConf.count() == 0) throw new SyntaxErrorException();
      dataId = dbSpalteConf.toString();
      sourceType = DATABASE_TYPE;
    } else if (cmd.equals("insertFormValue"))
    {
      ConfigThingy idConf = conf.query("ID");
      if (idConf.count() == 0) throw new SyntaxErrorException();
      dataId = idConf.toString();
      sourceType = FORM_TYPE;
    } else 
      throw new SyntaxErrorException();
    
    ConfigThingy trafoConf = conf.query("TRAFO");
    if (trafoConf.count() == 0)
      this.trafo = new FunctionSelection();
    else
    {
      String functionName = trafoConf.toString();
      this.trafo = funcSelections.getFunctionSelection(functionName);
    }
  }
  
  /**
   * Liefert je nach Typ der Einfügung das DB_SPALTE oder ID Attribut.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getDataID()
  {
    return dataId;
  }
  
  /**
   * Ändert je nach Type der Einfügung DB_SPALTE oder ID Attribut auf den Wert newId.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setDataID(String newId)
  {
    dataId = newId;
    notifyListeners(ID_ATTR, newId);
  }
  
  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden Container
   * aufgerufen werden, der das Model enthält.
   * @param index der Index an dem sich das Model in seinem Container befand.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED */
  public void hasBeenRemoved()
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = (ModelChangeListener)iter.next();
      listener.modelRemoved(this);
    }
  }
  
  /**
   * Ruft für jeden auf diesem Model registrierten {@link ModelChangeListener} die Methode
   * {@link ModelChangeListener#attributeChanged(FormControlModel, int, Object)} auf. 
   */
  private void notifyListeners(int attributeId, Object newValue)
  {
    Iterator iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = (ModelChangeListener)iter.next();
      listener.attributeChanged(this, attributeId, newValue);
    }
  }
  
  /**
   * listener wird über Änderungen des Models informiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }
  
  /**
   * Liefert ein Interface zum Zugriff auf die TRAFO dieses Objekts.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public FunctionSelectionAccess getTrafoAccess()
  {
    return new MyTrafoAccess();
  }
  
  /**
   * Interface für Listener, die über Änderungen eines Models informiert
   * werden wollen. 
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich geändert hat. 
     * @param model das InsertionModel, das sich geändert hat.
     * @param attributeId eine der {@link InsertionModel#ID_ATTR Attribut-ID-Konstanten}.
     * @param newValue der neue Wert des Attributs. Numerische Attribute werden als Integer übergeben.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void attributeChanged(InsertionModel model, int attributeId, Object newValue);
    
    
    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit
     * in keiner View mehr angezeigt werden soll).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void modelRemoved(InsertionModel model);
  }
  
  private class MyTrafoAccess implements FunctionSelectionAccess
  {
    public boolean isReference() { return trafo.isReference();}
    public boolean isExpert()    { return trafo.isExpert(); }
    public boolean isNone()      { return trafo.isNone(); }
    public String getName()      { return trafo.getName();}
    public ConfigThingy getExpertFunction() { return trafo.getExpertFunction(); }

    public void setParameterValues(Map mapNameToParamValue)
    {
      trafo.setParameterValues(mapNameToParamValue);
    }

    public void setFunction(String functionName, String[] paramNames)
    {
      trafo.setFunction(functionName, paramNames);
    }
    
    public void setExpertFunction(ConfigThingy funConf)
    {
      trafo.setExpertFunction(funConf);
    }
    
  }
  
}
