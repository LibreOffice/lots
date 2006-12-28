package de.muenchen.allg.itd51.wollmux.func;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sdb.CommandType;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.TimeoutException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.Datasource;
import de.muenchen.allg.itd51.wollmux.db.OOoDatasource;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;

public class StandardPrint
{
  /**
   * Anzahl Millisekunden, die maximal gewartet wird, bis alle Datensätze für den
   * Serienbrief aus der Datenbank gelesen wurden.
   */
  private static final int DATABASE_TIMEOUT = 20000;
  
  public static void sachleitendeVerfuegung(XPrintModel pmod)
  {
    SachleitendeVerfuegung.showPrintDialog(pmod);
  }

  public static void printVerfuegungspunktTest(XPrintModel pmod)
  {
    pmod.printVerfuegungspunkt((short) 1, (short) 1, false, true);
    pmod.printVerfuegungspunkt((short) 2, (short) 1, false, false);
    pmod.printVerfuegungspunkt((short) 3, (short) 1, false, false);
    pmod.printVerfuegungspunkt((short) 4, (short) 1, true, false);
  }

  public static void myTestPrintFunction(XPrintModel pmod)
  {
    new UnoService(pmod).msgboxFeatures();

    pmod.setFormValue("EmpfaengerZeile1", "Hallo, ich bin's");
    pmod.setFormValue("SGAnrede", "Herr");
    pmod.setFormValue("AbtAnteile", "true");
    pmod.print((short)1);

    pmod.setFormValue("EmpfaengerZeile1", "Noch eine Empfängerzeile");
    pmod.setFormValue("SGAnrede", "Frau");
    pmod.setFormValue("AbtAnteile", "false");
    pmod.setFormValue("AbtKaution", "true");
    pmod.print((short)1);
    
    new UnoService(pmod).msgboxFeatures();
  }
  
  /**
   * Druckt das zu pmod gehörende Dokument für jeden Datensatz der aktuell über
   * Bearbeiten/Datenbank austauschen eingestellten Tabelle einmal aus.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeWithoutSelection(XPrintModel pmod)
  {
    XTextDocument doc = pmod.getTextDocument();
    XPropertySet settings = null;
    try{
      settings = UNO.XPropertySet(UNO.XMultiServiceFactory(doc).createInstance("com.sun.star.document.Settings"));
    } catch(Exception x)
    {
      Logger.error("Kann DocumentSettings nicht auslesen", x);
      return;
    }
    
    String datasource = (String)UNO.getProperty(settings, "CurrentDatabaseDataSource");
    String table = (String)UNO.getProperty(settings, "CurrentDatabaseCommand");
    Integer type = (Integer) UNO.getProperty(settings, "CurrentDatabaseCommandType"); 
    
    Logger.debug("Ausgewählte Datenquelle: \""+datasource+"\"  Tabelle/Kommando: \""+table+"\"  Typ: \""+type+"\"");
    
    mailMergeWithoutSelection(pmod, datasource, table, type);
  }

  /**
   * Druckt das zu pmod gehörende Dokument für jeden Datensatz aus Tabelle table in Datenquelle
   * datasource einmal aus. Das Argument type muss {@link CommandType#TABLE} sein.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private static void mailMergeWithoutSelection(XPrintModel pmod, String datasource, String table, Integer type)
  {
    /*
     * Kann nur mit Tabellennamen umgehen, nicht mit beliebigen Statements. Falls eine andere
     * Art von Kommando eingestellt ist, wird nichts getan. Der Benutzer soll in diesem Fall
     * einfach eine Tabelle auswählen, bevor er druckt.
     */
    if (datasource == null || table == null || type == null || type.intValue() != CommandType.TABLE)
    {
      Logger.debug("Es ist keine Tabelle für den MailMerge ausgewählt worden => Druck wird abgebrochen");
      return;
    }
    
    ConfigThingy conf = new ConfigThingy("Datenquelle");
    conf.add("NAME").add("Knuddel");
    conf.add("TABLE").add(table);
    conf.add("SOURCE").add(datasource);
    Datasource ds;
    try{
      ds = new OOoDatasource(new HashMap(),conf,new URL("file:///"));
    }catch(Exception x)
    {
      Logger.error(x);
      return;
    }
    
    Set schema = ds.getSchema();
    QueryResults data;
    try
    {
      data = ds.getContents(DATABASE_TIMEOUT);
    }
    catch (TimeoutException e)
    {
      Logger.error("Konnte Daten für Serienbrief nicht aus der Datenquelle auslesen",e);
      return;
    }
    
    Iterator iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset dataset = (Dataset)iter.next();
      Iterator colIter = schema.iterator();
      while (colIter.hasNext())
      {
        String column = (String)colIter.next();
        String value;
        try
        {
          value = dataset.get(column);
        }
        catch (Exception e)
        {
          Logger.error("Spalte fehlt unerklärlicherweise => Abbruch des Drucks",e);
          return;
        }
        
        pmod.setFormValue(column, value);
      }
      pmod.print((short)1);
    }
  }
}
