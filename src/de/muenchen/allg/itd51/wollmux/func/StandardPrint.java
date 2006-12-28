package de.muenchen.allg.itd51.wollmux.func;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

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
    mailMerge(pmod, false);
  }
  
  /**
   * Druckt das zu pmod gehörende Dokument für die Datensätze, die der Benutzer in einem Dialog
   * auswählt. Für die Anzeige der Datensätze im Dialog wird die Spalte "WollMuxDescription"
   * verwendet. Falls die Spalte "WollMuxSelected" vorhanden ist und "1", "ja" oder "true"
   * enthält, so ist der entsprechende Datensatz in der Auswahlliste bereits vorselektiert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  public static void mailMergeWithSelection(XPrintModel pmod)
  {
    mailMerge(pmod, true);
  }

  private static void mailMerge(XPrintModel pmod, boolean offerSelection)
  { //TESTED
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
    
    mailMerge(pmod, datasource, table, type, offerSelection);
  }

  /**
   * Falls offerSelection == false wird das zu pmod gehörende Dokument für jeden Datensatz aus 
   * Tabelle table in Datenquelle datasource einmal ausgedruckt.
   * Falls offerSelection == true, wird dem Benutzer ein Dialog präsentiert, in dem er die
   * "WollMuxDescription"-Spalten aller Datensätze angezeigt bekommt und die auszudruckenden 
   * Datensätze auswählen kann. Dabei sind alle Datensätze, die eine Spalte "WollMuxSelected"
   * haben, die den Wert "true", "ja" oder "1" enthält bereits vorselektiert.  
   * @param type muss {@link CommandType#TABLE} sein.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private static void mailMerge(XPrintModel pmod, String datasource, String table, Integer type, boolean offerSelection)
  {
    /*
     * Kann nur mit Tabellennamen umgehen, nicht mit beliebigen Statements. Falls eine andere
     * Art von Kommando eingestellt ist, wird nichts getan. Der Benutzer soll in diesem Fall
     * einfach eine Tabelle auswählen, bevor er druckt.
     */
    if (datasource == null || datasource.length() == 0 || table == null || table.length() == 0 || type == null || type.intValue() != CommandType.TABLE)
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
    
    Vector list = new Vector();
    Iterator iter = data.iterator();
    while (iter.hasNext())
    {
      Dataset dataset = (Dataset)iter.next();
      list.add(new ListElement(dataset));
    }
    
    if (offerSelection) 
    {
      if (!selectFromListDialog(list)) return;
    }
    
    iter = list.iterator();
    while (iter.hasNext())
    {
      ListElement ele = (ListElement)iter.next();
      if (offerSelection && !ele.isSelected()) continue;
      Iterator colIter = schema.iterator();
      while (colIter.hasNext())
      {
        String column = (String)colIter.next();
        String value = null;
        try
        {
          value = ele.getDataset().get(column);
        }
        catch (Exception e)
        {
          Logger.error("Spalte \""+column+"\" fehlt unerklärlicherweise => Abbruch des Drucks",e);
          return;
        }
        
        if (value != null) pmod.setFormValue(column, value);
      }
      pmod.print((short)1);
    }
  }
  
  private static class ListElement
  {
    private Dataset ds;
    private boolean selected = false;
    private String description = "Keine Beschreibung vorhanden"; 
    public ListElement(Dataset ds)
    {
      this.ds = ds;
      try{
        String des = ds.get("WollMuxDescription");
        if (des != null && des.length() > 0) description = des;
      } catch(Exception x){}
      try{
        String sel = ds.get("WollMuxSelected");
        if (sel != null && (sel.equalsIgnoreCase("true") || sel.equals("1") || sel.equalsIgnoreCase("ja"))) selected = true;
      } catch(Exception x){}
    }
    public void setSelected(boolean selected)
    {
      this.selected = selected;
    }
    public boolean isSelected() { return selected; }
    public Dataset getDataset() {return ds;}
    public String toString() { return description;}
  }
  
  /**
   * Präsentiert einen Dialog, der den Benutzer aus list (enthält {@link ListElement}s) auswählen
   * lässt. ACHTUNG! Diese Methode kehrt erst zurück nachdem der Benutzer den Dialog geschlossen
   * hat.
   * @return true, falls der Benutzer mit Okay bestätigt hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private static boolean selectFromListDialog(final Vector list)
  {
    final boolean[] result = new boolean[]{false,false}; 
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{createSelectFromListDialog(list, result);}catch(Exception x)
            {
              Logger.error(x);
              synchronized(result)
              {
                result[0] = true;
                result.notifyAll();
              }
            };
        }
      });
      
      synchronized(result)
      {
        while(!result[0]) result.wait();
      }
      return result[1];
      
    }
    catch(Exception x) 
    {
      Logger.error(x);
      return false;
    }
  }
  
  /**
   * Präsentiert einen Dialog, der den Benutzer aus list (enthält {@link ListElement}s) auswählen
   * lässt. ACHTUNG! Diese Methode darf nur im Event Dispatching Thread aufgerufen werden. 
   * @param result ein 2-elementiges Array auf das nur synchronisiert zugegriffen wird. Das
   *        erste Element wird auf false gesetzt, sobald der Dialog geschlossen wird.
   *        Das zweite Element enthält in diesem Fall true, wenn der Benutzer mir Okay
   *        bestätigt hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private static void createSelectFromListDialog(final Vector list, final boolean[] result)
  {
    final JFrame myFrame = new JFrame("Gewünschte Ausdrucke wählen");
    myFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    myFrame.addWindowListener(new WindowListener(){
      public void windowOpened(WindowEvent e) {}
      public void windowClosing(WindowEvent e) {}
      public void windowClosed(WindowEvent e) 
      {
        synchronized(result)
        {
          result[0] = true;
          result.notifyAll();
        }
      }
      public void windowIconified(WindowEvent e) {}
      public void windowDeiconified(WindowEvent e) {}
      public void windowActivated(WindowEvent e) { }
      public void windowDeactivated(WindowEvent e) {}});
    myFrame.setAlwaysOnTop(true);
    JPanel myPanel = new JPanel(new BorderLayout());
    myPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    myFrame.setContentPane(myPanel);
    
    final JList myList = new JList(list);
    myList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    for (int i = 0; i < list.size(); ++i)
    {
      ListElement ele = (ListElement)list.get(i);
      if (ele.isSelected()) myList.addSelectionInterval(i,i);
    }
    
    JScrollPane scrollPane = new JScrollPane(myList);
    myPanel.add(scrollPane, BorderLayout.CENTER);
    
    Box top = Box.createVerticalBox();
    top.add(new JLabel("Bitte wählen Sie, welche Ausdrucke Sie bekommen möchten"));
    top.add(Box.createVerticalStrut(5));
    myPanel.add(top, BorderLayout.NORTH);
    
    Box bottomV = Box.createVerticalBox();
    bottomV.add(Box.createVerticalStrut(5));
    Box bottom = Box.createHorizontalBox();
    bottomV.add(bottom);
    myPanel.add(bottomV, BorderLayout.SOUTH);
    
    JButton button = new JButton("Abbrechen");
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        myFrame.dispose();
      }}
    );
    bottom.add(button);
    
    bottom.add(Box.createHorizontalGlue());
    
    button = new JButton("Alle");
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        myList.setSelectionInterval(0, list.size()-1);
      }}
    );
    bottom.add(button);
    
    bottom.add(Box.createHorizontalStrut(5));
    
    button = new JButton("Keinen");
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        myList.clearSelection();
      }}
    );
    bottom.add(button);
    
    bottom.add(Box.createHorizontalGlue());
    
    button = new JButton("Drucken");
    button.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e)
      {
        for (int i = 0; i < list.size(); ++i) ((ListElement)list.get(i)).setSelected(false);
        int[] sel = myList.getSelectedIndices();
        for (int i = 0; i < sel.length; ++i)
        {
          ((ListElement)list.get(sel[i])).setSelected(true);
        }
        synchronized(result)
        {
          result[1] = true;
        }
        myFrame.dispose();
      }}
    );
    bottom.add(button);
    
    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setVisible(true);
    myFrame.requestFocus();
  }
  
}
