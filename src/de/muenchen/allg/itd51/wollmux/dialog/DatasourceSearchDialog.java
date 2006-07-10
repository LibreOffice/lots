/*
* Dateiname: DatasourceSearchDialog.java
* Projekt  : WollMux
* Funktion : Dialog zur Suche nach Daten in einer Datenquelle, die über DIALOG-Funktion verfügbar gemacht werden.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 22.05.2006 | BNK | Erstellung
* 23.05.2006 | BNK | Rohbau
* 24.05.2006 | BNK | angefangen mit Suchstrategie auswerten etc.
* 26.05.2006 | BNK | Suchstrategie,... fertig implementiert
* 29.05.2006 | BNK | Umstellung auf UIElementFactory.Context
* 30.05.2006 | BNK | Suche implementiert
* 29.06.2006 | BNK | setResizable(true)
* 10.07.2006 | BNK | suchanfrageX statt wortX als Platzhalter.
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.TimeoutException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.db.Query;
import de.muenchen.allg.itd51.wollmux.db.QueryPart;
import de.muenchen.allg.itd51.wollmux.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.db.SimpleDataset;
import de.muenchen.allg.itd51.wollmux.dialog.UIElementFactory.Context;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values;

/**
 * Dialog zur Suche nach Daten in einer Datenquelle, die über DIALOG-Funktion 
 * verfügbar gemacht werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatasourceSearchDialog implements Dialog
{
  /**
   * Erzeugt einen neuen Dialog, dessen Instanzen Datenquellensuchdialoge gemäß
   * der Beschreibung in conf darstellen. Die Suchergebnisse liefert dj.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Dialog create(ConfigThingy conf, DatasourceJoiner dj) 
  {
    return new Instantiator(conf, dj);
  };
  
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
   * Das ConfigThingy, das die Beschreibung des Dialogs enthält.
   */
  private ConfigThingy myConf;
  
  /**
   * Der Instantiator, der diesen Dialog instanziiert hat und auch für die
   * Erstellung weiterer Instanzen herangezogen wird.
   */
  private Instantiator ilse;
  
  /**
   * data[0] speichert die aktuell ausgewählten Formulardaten.
   */
  private Map[] data = new Map[]{new HashMap()};
  
  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;
  
  /**
   * Die JTabbedPane, die die ganzen Tabs der GUI enthält.
   */
  private JTabbedPane myTabbedPane;
  
  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;
  
  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(Context, ConfigThingy)},
   * der verwendet wird für das Erzeugen von vertikal angeordneten UI Elementen 
   * (mit Ausnahme der Vorschau).
   */
  private UIElementFactory.Context vertiContext;
  
  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(Context, ConfigThingy)},
   * der verwendet wird für das Erzeugen der vertikal angeordneten UI Elemente 
   * der Vorschau.
   */
  private UIElementFactory.Context previewContext;
  
  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(Context, ConfigThingy)},
   * der verwendet wird für das Erzeugen von horizontal angeordneten Elementen.
   */
  private UIElementFactory.Context horiContext;
  
  /**
   * Solange dieses Flag false ist, werden Events von UI Elementen ignoriert.
   */
  private boolean processUIElementEvents = false;
  
  /**
   * Wird zur Auflösung von Funktionsreferenzen in Spaltenumsetzung-Abschnitten
   * verwendet.
   */
  private FunctionLibrary funcLib;
  
  /**
   * Zur Zeit noch nicht unterstützt, aber im Prinzip könnte man in einem
   * Datenquellensuchdialog ebenfalls wieder Funktionsdialoge verwenden. Diese
   * würden dann aus dieser Bibliothek bezogen.
   */
  private DialogLibrary dialogLib;
  
  /**
   * Werden durch diesen Funktionsdialog weitere Funktionsdialoge erzeugt, so
   * wird dieser Kontext übergeben. 
   */
  private Map context = new HashMap();
  
  /**
   * Der show übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Wird von show() getestet und auf true gesetzt um mehrfache 
   * gleichzeitige show()s zu verhindern.
   */
  private boolean[] shown = new boolean[]{false};
    
  /**
   * Erzeugt einen neuen DSD.
   * @param ilse der Instantiator, der für instanceFor()-Aufrufe verwendet werden
   *        soll.
   * @param conf die Beschreibung des Dialogs.
   */
  private DatasourceSearchDialog(Instantiator ilse, ConfigThingy conf, DatasourceJoiner dj)
  throws ConfigurationErrorException
  {
    this.myConf = conf;
    this.ilse = ilse;
    this.dj = dj;
  }

  public Dialog instanceFor(Map context) throws ConfigurationErrorException
  {
    return ilse.instanceFor(context);
  }

  public Object getData(String id) 
  { 
    String str;
    synchronized(data)
    {
      str = (String)data[0].get(id);
    }
    if (str == null) return "";
    return str;
  }
  //TESTED
  public void show(ActionListener dialogEndListener, FunctionLibrary funcLib, DialogLibrary dialogLib) throws ConfigurationErrorException
  {
    synchronized (shown)
    {
      if (shown[0]) return;
      shown[0] = true;
    }
    this.dialogEndListener = dialogEndListener;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;
    
    String title = "Datensatz Auswählen";
    try{ title = myConf.get("TITLE",1).toString();} catch(Exception x){}
    
    String type = "<keiner>";
    try{ type = myConf.get("TYPE",1).toString();} catch(Exception x){}
    if (!type.equals("dbSelect"))
      throw new ConfigurationErrorException("Ununterstützter TYPE \""+type+"\" in Funktionsdialog \""+myConf.getName()+"\"");
    
    final ConfigThingy fensterDesc = myConf.query("Fenster");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schlüssel 'Fenster' fehlt in "+myConf.getName());
    
    //  GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try{
      final String title2 = title;
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            try{
              createGUI(title2, fensterDesc.getLastChild());}catch(Exception x)
            {
              Logger.error(x);
            };
        }
      });
    }
    catch(Exception x) {Logger.error(x);}
  }
  
  /**
   * Erzeugt das GUI. Muss im EDT aufgerufen werden.
   * @param title der Titel des Fensters.
   * @param fensterDesc der "Fenster" Abschnitt, der die Tabs der GUI beschreibt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private void createGUI(String title, ConfigThingy fensterDesc)
  {
    Common.setLookAndFeelOnce();
    
    //Create and set up the window.
    myFrame = new JFrame(title);
    //leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    MyWindowListener oehrchen = new MyWindowListener();
    //der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert wird
    myFrame.addWindowListener(oehrchen); //TODO CLOSEACTION statt einfach nur abort()
    
    JPanel contentPanel = new JPanel();
    myFrame.getContentPane().add(contentPanel);
    myTabbedPane = new JTabbedPane();
    contentPanel.add(myTabbedPane);
    
    /********************************************************
     * Tabs erzeugen.
     ******************************************************/
    Iterator iter = fensterDesc.iterator();
    int tabIndex = 0;
    while (iter.hasNext())
    {
      ConfigThingy neuesFenster = (ConfigThingy)iter.next();
      
      /*
       * Die folgende Schleife ist nicht nur eleganter als mehrere try-catch-Blöcke
       * um get()-Befehle, sie verhindert auch, dass TIP oder HOTKEY aus Versehen
       * von einem enthaltenen Button aufgeschnappt werden.
       */
      String tabTitle = "Eingabe";
      char hotkey = 0;
      String tip = "";
      Iterator childIter = neuesFenster.iterator();
      while (childIter.hasNext())
      { //TODO CLOSEACTION unterstuetzen
        ConfigThingy childConf = (ConfigThingy)childIter.next();
        String name = childConf.getName();
        if (name.equals("TIP")) tip = childConf.toString(); else
        if (name.equals("TITLE")) tabTitle = childConf.toString(); else
        if (name.equals("HOTKEY"))
        {
          String str = childConf.toString();
          if (str.length() > 0) hotkey = str.toUpperCase().charAt(0);
        }
      }
      
      DialogWindow newWindow = new DialogWindow(tabIndex, neuesFenster);
      
      myTabbedPane.addTab(tabTitle, null, newWindow.JPanel(), tip);
      if (hotkey != 0) myTabbedPane.setMnemonicAt(tabIndex, hotkey);

      ++tabIndex;
    }
    
        
    /*
     * Event-Verarbeitung starten.
     */
    processUIElementEvents = true;
    
    myFrame.pack();
    myFrame.setAlwaysOnTop(true);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    //frameHeight = screenSize.height * 8 / 10;
    //myFrame.setSize(frameWidth, frameHeight);
    int x = screenSize.width/2 - frameWidth/2; 
    int y = screenSize.height/2 - frameHeight/2;
    myFrame.setLocation(x,y);
    myFrame.setResizable(true);
    myFrame.setVisible(true);
  }

  /**
   * Ein Tab der GUI.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class DialogWindow implements UIElementEventHandler
  {
    /**
     * Das Panel das die GUI-Elemente enthält.
     */
    private JPanel myPanel;
    
    /**
     * Die Suchstrategie für Suchanfragen. 
     */
    private SearchStrategy searchStrategy;
    
    /**
     * Eine Liste von ColumnTranslations, die die Umsetzung der Spalten der
     * gefundenen Datasets auf die vom Dialog zu exportierenden Werte
     * realisieren. Jede ColumnTranslation entspricht einem Eintrag des
     * Spaltenumsetzung-Abschnitts. 
     */
    private List columnTranslations;
    
    /**
     * Enthält die Namen aller Ergebnisspalten, d,h, die Menge der newColumnNames
     * aller ColumnTranslation-Objekte in columnTranslations.
     */
    private Collection schema;

    /**
     * Legt fest, wie die Datensätze in der Ergebnisliste dargestellt werden
     * sollen. Kann Variablen der Form "${name}" enthalten.
     */
    private String displayTemplate = "<Datensatz>";
    
    /**
     * Die Listbox mit den Suchresultaten.
     */
    private UIElement.Listbox resultsList = null;
    
    /**
     * Das Textfeld in dem der Benutzer seine Suchanfrage eintippt.
     */
    private UIElement query = null;
       
    /**
     * Liefert das JPanel, das die Elemente dieses Tabs enthält.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public JPanel JPanel() {return myPanel;}
    
    /**
     * Die für die Erzeugung der UI Elemente verwendete Factory.
     */
    private UIElementFactory uiElementFactory;

    /**
     * Bildet DB_SPALTE Werte des Vorschau-Abschnitts auf die entsprechenden
     * UIElemente ab (jeweils 1 UIElement pro DB_SPALTE, keine Liste).
     */
    private Map mapDB_SPALTEtoUIElement;
    
    
    /**
     * Erzeugt ein neues Tab.
     * @param tabIndex Die Nummer (von 0 gezählt) des Tabs, das dieses DialogWindow
     *        darstellt.
     * @param conf der Kind-Knoten des Fenster-Knotens der das Tab beschreibt.
     *        conf ist direkter Elternknoten der Knoten "Intro" et al.
     * @author Matthias Benkmann (D-III-ITD 5.1)     
     * TESTED 
     */
    public DialogWindow(int tabIndex, ConfigThingy conf)
    {
      searchStrategy = SearchStrategy.parse(conf);
      schema = new HashSet();
      columnTranslations = parseColumnTranslations(conf, schema);
      initFactories();
      
      myPanel = new JPanel(new BorderLayout());
      
      JPanel introSuche = new JPanel();
      introSuche.setLayout(new BoxLayout(introSuche, BoxLayout.PAGE_AXIS));
      JPanel suchergebnisUndVorschau = new JPanel();
      suchergebnisUndVorschau.setLayout(new BoxLayout(suchergebnisUndVorschau, BoxLayout.LINE_AXIS));
      JPanel fussbereich = new JPanel(new GridBagLayout());
      JPanel intro = new JPanel(new GridBagLayout());
      JPanel suche = new JPanel(new GridBagLayout());
      JPanel suchergebnis = new JPanel(new GridBagLayout());
      JPanel vorschau = new JPanel(new GridBagLayout());
      
      myPanel.add(introSuche, BorderLayout.PAGE_START);
      myPanel.add(suchergebnisUndVorschau, BorderLayout.CENTER);
      myPanel.add(fussbereich, BorderLayout.PAGE_END);
      
      introSuche.add(intro);
      introSuche.add(suche);
      
      suchergebnisUndVorschau.add(suchergebnis);
      suchergebnisUndVorschau.add(vorschau);
      
      addUIElements(conf, "Intro", intro, 0, 1, vertiContext, null);
      addUIElements(conf, "Suche", suche, 1, 0, horiContext, null);
      addUIElements(conf, "Suchergebnis", suchergebnis, 0, 1, vertiContext, null);
      mapDB_SPALTEtoUIElement = new HashMap();
      addUIElements(conf, "Vorschau", vorschau, 0, 1, previewContext, mapDB_SPALTEtoUIElement);
      addUIElements(conf, "Fussbereich", fussbereich, 1, 0, horiContext, null);
    }

    
    /** 
     * Fügt compo UI Elemente gemäss den Kindern von conf.query(key) hinzu.
     * compo muss ein GridBagLayout haben. stepx und stepy geben an um
     * wieviel mit jedem UI Element die x und die y Koordinate der Zelle
     * erhöht werden soll. Wirklich sinnvoll sind hier nur (0,1) und (1,0).
     * @param context ist der Kontext, der {@link UIElementFactory#createUIElement(Context, ConfigThingy)}
     *        übergeben werden soll für die Erzeugung der UIElemente.
     * @param in dieser Map werden all erzeugten UIElemente registriert, die ein
     *        DB_SPALTE Attribut haben. null ist nicht erlaubt.   
     * TESTED 
     */
    private void addUIElements(ConfigThingy conf, String key, JComponent compo, int stepx, int stepy, UIElementFactory.Context context, Map mapDB_SPALTEtoUIElement)
    {
      int y = 0;
      int x = 0; 
      
      Iterator parentiter = conf.query(key).iterator();
      while (parentiter.hasNext())
      {
        Iterator iter = ((ConfigThingy)parentiter.next()).iterator();
        while (iter.hasNext())
        {
          ConfigThingy uiConf = (ConfigThingy)iter.next();
          UIElement uiElement;
          try{
            uiElement = uiElementFactory.createUIElement(context, uiConf);
            try {
              String dbSpalte = uiConf.get("DB_SPALTE").toString();
              mapDB_SPALTEtoUIElement.put(dbSpalte, uiElement);
            }catch(Exception e) {}
          } catch(ConfigurationErrorException e)
          {
            Logger.error(e);
            continue;
          }
          
          /*
           * Besondere IDs auswerten.
           */
          String id = uiElement.getId();
          if (id.equals("suchanfrage"))
            query = uiElement;
            
          if (id.equals("suchergebnis"))
          {
            try {
              resultsList = (UIElement.Listbox)uiElement;
              try{ displayTemplate = uiConf.get("DISPLAY").toString(); }catch(Exception e) {};
            }catch(ClassCastException e)
            {
              Logger.error("UI Element mit ID \"suchergebnis\" muss vom TYPE \"listbox\" sein!");
            }
          }
            
          
          /********************************************************************
           * UI Element und evtl. vorhandenes Zusatzlabel zum GUI hinzufügen.
           *********************************************************************/
          int compoX = 0;
          int labelmod = 1;
          if (!uiElement.getLabelType().equals(UIElement.LABEL_NONE))
          {
            labelmod = 2;
            int labelX = 0;
            if (uiElement.getLabelType().equals(UIElement.LABEL_LEFT))
              compoX = 1;
            else
              labelX = 1;
            
            Component label = uiElement.getLabel();
            if (label != null)
            {
              GridBagConstraints gbc = (GridBagConstraints)uiElement.getLabelLayoutConstraints();
              gbc.gridx = x + labelX;
              gbc.gridy = y;
              compo.add(label, gbc);
            }
          }
          GridBagConstraints gbc = (GridBagConstraints)uiElement.getLayoutConstraints();
          gbc.gridx = x + compoX;
          gbc.gridy = y;
          x += stepx * labelmod;
          y += stepy;
          compo.add(uiElement.getComponent(), gbc);
          
        }  
      }    
    }
    
    /**
     * Geht alle Elemente von {@link #mapDB_SPALTEtoUIElement} durch und updated die
     * Felder mit den entsprechenden Werten aus dem Datensatz, der an ele
     * dranhängt.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void updatePreview(ListElement ele)
    {
      Dataset ds = null;
      if (ele != null) ds = ele.getDataset();
      Iterator iter = mapDB_SPALTEtoUIElement.entrySet().iterator();
      while (iter.hasNext())
      {
        Map.Entry entry = (Map.Entry)iter.next();
        String dbSpalte = (String)entry.getKey();
        UIElement uiElement = (UIElement)entry.getValue();
        try
        {
          if (ds == null)
            uiElement.setString("");
          else
            uiElement.setString(ds.get(dbSpalte));
        }
        catch (ColumnNotFoundException e)
        {
          Logger.error("Fehler im Abschnitt \"Spaltenumsetzung\" oder \"Vorschau\". Spalte \""+dbSpalte+"\" soll in Vorschau angezeigt werden ist aber nicht in der Spaltenumsetzung definiert.");
        }
      }
    }
    
    /**
     * Ändert die Werteliste von list so, 
     * dass sie data entspricht. Die Datasets aus data werden nicht
     * direkt als Werte verwendet, sondern in {@link ListElement} Objekte gewrappt.
     * data == null wird interpretiert als leere Liste.
     * list kann null sein (dann tut diese Funktion nichts). 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED 
     */
    private void setListElements(UIElement.Listbox list, QueryResults data)
    {
      if (list == null) return;
      ListElement[] elements;
      if (data == null)
        elements = new ListElement[]{};
      else
      {
        elements = new ListElement[data.size()];
        Iterator iter = data.iterator();
        int i = 0;
        while (iter.hasNext()) elements[i++] = new ListElement((Dataset)iter.next());
        Arrays.sort(elements, new Comparator()
        {
          public int compare(Object o1, Object o2)
          {
            return o1.toString().compareTo(o2.toString());
          }
        });
      }
      
      list.setList(Arrays.asList(elements));
      updatePreview(null);
    }
      
      
    /**
     * Liefert zu einem Datensatz den in einer Listbox anzuzeigenden String.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private String getDisplayString(Dataset ds)
    {
      return substituteVars(displayTemplate, ds);
    }
      
    /**
     * Wrapper um ein Dataset zum Einfügen in eine JList.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private class ListElement
    {
      private String displayString;
      private Dataset ds;
      
      public ListElement(Dataset ds)
      {
        displayString = getDisplayString(ds);
        this.ds = ds;
      }
      
      public String toString()
      {
        return displayString;
      }
      
      public Dataset getDataset() {return ds;}
    }
    

    /**
     * Führt die Suchanfrage im Feld {@link #query} aus (falls dieses nicht null ist)
     * gemäß {@link #searchStrategy} und ändert
     * {@link #resultsList} (falls nicht null) 
     * so dass sie die Suchergebnisse enthält.  
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public void search()
    {
      if (query == null) return;
      List queries = parseQuery(searchStrategy, query.getString());
      
      QueryResults results = null;
      try{
        Iterator iter = queries.iterator();
        while (iter.hasNext())
        {
          results = dj.find((Query)iter.next());
          if (!results.isEmpty()) break;
        }
      }catch(TimeoutException x) { Logger.error(x);}
      catch(IllegalArgumentException x) { Logger.error(x);} //wird bei illegalen Suchanfragen geworfen
      
      if (results != null && resultsList != null)
        setListElements(resultsList, new TranslatedQueryResults(results, columnTranslations)); 
    }

    /**
    * Die zentrale Anlaufstelle für alle von UIElementen ausgelösten Events
    * (siehe {@link UIElementEventHandler#processUiElementEvent(UIElement, String, Object[])}).
    *  
    * @author Matthias Benkmann (D-III-ITD 5.1)
    */
    public void processUiElementEvent(UIElement source, String eventType, Object[] args)
    {
      if (!processUIElementEvents) return;
      try{
        processUIElementEvents = false; // Reentranz bei setString() unterbinden
        
        if (WollMuxFiles.isDebugMode())
        {
          StringBuffer buffy = new StringBuffer("UIElementEvent: "+eventType+"(");
          for (int i = 0; i < args.length; ++i)
            buffy.append((i == 0?"":",")+args[i]);
          buffy.append(") on UIElement "+source.getId());
          Logger.debug(buffy.toString());
        }
        
        if (eventType.equals("action"))
        {
          String action = (String)args[0];
          if (action.equals("abort"))
            abort();
          else if (action.equals("back"))
            back();
          else if (action.equals("search"))
            search();
          else if (action.equals("select"))
          {
            Dataset ds = null;
            if (resultsList != null)
            {
              Object[] selected = resultsList.getSelected();
              if (selected.length > 0)
                ds = ((ListElement)selected[0]).getDataset();
            }
            select(schema, ds);
          }
        }
        else if (eventType.equals("listSelectionChanged"))
        {
          Object[] selected = ((UIElement.Listbox)source).getSelected();
          if (selected.length > 0)
            updatePreview((ListElement)selected[0]);
        }
          
        
      }catch (Exception x)
      {
        Logger.error(x);
      }
      finally
      {
        processUIElementEvents = true;
      }
    }

    /**
     * Initialisiert die UIElementFactory, die zur Erzeugung der UIElements
     * verwendet wird.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    private void initFactories()
    {
      Map mapTypeToLayoutConstraints = new HashMap();
      Map mapTypeToLabelType = new HashMap();
      Map mapTypeToLabelLayoutConstraints = new HashMap();
      
      //int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor,          int fill,                  Insets insets, int ipadx, int ipady) 
      GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcListbox   = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,   GridBagConstraints.BOTH, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcCombobox  = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcTextarea  = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,   GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcLabelLeft = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcCheckbox  = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcLabel =     new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcPreviewLabel =new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
      GridBagConstraints gbcButton    = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,       new Insets(BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER,BUTTON_BORDER),0,0);
      GridBagConstraints gbcHsep      = new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(3*TF_BORDER,0,2*TF_BORDER,0),0,0);
      GridBagConstraints gbcPreviewHsep=new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,       new Insets(3*TF_BORDER,0,2*TF_BORDER,0),0,0);
      GridBagConstraints gbcVsep      = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,       new Insets(0,TF_BORDER,0,TF_BORDER),0,0);
      GridBagConstraints gbcGlue      = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
      GridBagConstraints gbcPreviewGlue= new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,       new Insets(0,0,0,0),0,0);
      
      mapTypeToLayoutConstraints.put("default", gbcTextfield);
      mapTypeToLabelType.put("default", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("default", null);
      
      mapTypeToLayoutConstraints.put("textfield", gbcTextfield);
      mapTypeToLabelType.put("textfield", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("textfield", null);
      
      mapTypeToLayoutConstraints.put("combobox", gbcCombobox);
      mapTypeToLabelType.put("combobox", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("combobox", null);
      
      mapTypeToLayoutConstraints.put("h-glue", gbcGlue);
      mapTypeToLabelType.put("h-glue", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("h-glue", null);
      mapTypeToLayoutConstraints.put("v-glue", gbcGlue);
      mapTypeToLabelType.put("v-glue", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("v-glue", null);
      
      mapTypeToLayoutConstraints.put("textarea", gbcTextarea);
      mapTypeToLabelType.put("textarea", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("textarea", null);
      
      mapTypeToLayoutConstraints.put("listbox", gbcListbox);
      mapTypeToLabelType.put("listbox", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("listbox", null);
      
      mapTypeToLayoutConstraints.put("label", gbcLabel);
      mapTypeToLabelType.put("label", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("label", null);
      
      mapTypeToLayoutConstraints.put("checkbox", gbcCheckbox);
      mapTypeToLabelType.put("checkbox", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("checkbox", null); //hat label integriert
      
      mapTypeToLayoutConstraints.put("button", gbcButton);
      mapTypeToLabelType.put("button", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("button", null);
      
      mapTypeToLayoutConstraints.put("h-separator", gbcHsep);
      mapTypeToLabelType.put("h-separator", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("h-separator", null);
      mapTypeToLayoutConstraints.put("v-separator", gbcVsep);
      mapTypeToLabelType.put("v-separator", UIElement.LABEL_NONE);
      mapTypeToLabelLayoutConstraints.put("v-separator", null);

      Set supportedActions = new HashSet();
      supportedActions.add("abort");
      supportedActions.add("back");
      supportedActions.add("search");
      supportedActions.add("select");
      
      vertiContext = new UIElementFactory.Context();
      vertiContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
      vertiContext.mapTypeToLabelType = mapTypeToLabelType;
      vertiContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
      vertiContext.uiElementEventHandler = this;
      vertiContext.mapTypeToType = new HashMap();
      vertiContext.mapTypeToType.put("separator","h-separator");
      vertiContext.mapTypeToType.put("glue","v-glue");
      vertiContext.supportedActions = supportedActions;
      vertiContext.uiElementEventHandler = this;
      
      horiContext = new UIElementFactory.Context();
      horiContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
      horiContext.mapTypeToLabelType = mapTypeToLabelType;
      horiContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
      horiContext.uiElementEventHandler = this;
      horiContext.mapTypeToType = new HashMap();
      horiContext.mapTypeToType.put("separator","v-separator");
      horiContext.mapTypeToType.put("glue","h-glue");
      horiContext.supportedActions = supportedActions;
      horiContext.uiElementEventHandler = this;

      Map previewLabelLayoutConstraints = new HashMap(mapTypeToLabelLayoutConstraints);
      previewLabelLayoutConstraints.put("textfield",gbcLabelLeft);
      Map previewLabelType = new HashMap(mapTypeToLabelType);
      previewLabelType.put("textfield", UIElement.LABEL_LEFT);
      Map previewLayoutConstraints = new HashMap(mapTypeToLayoutConstraints);
      previewLayoutConstraints.put("h-glue", gbcPreviewGlue);
      previewLayoutConstraints.put("v-glue", gbcPreviewGlue);
      previewLayoutConstraints.put("label", gbcPreviewLabel);
      previewLayoutConstraints.put("h-separator", gbcPreviewHsep);
      previewContext = new UIElementFactory.Context();
      previewContext.mapTypeToLabelLayoutConstraints = previewLabelLayoutConstraints;
      previewContext.mapTypeToLabelType = previewLabelType;
      previewContext.mapTypeToLayoutConstraints = previewLayoutConstraints;
      previewContext.uiElementEventHandler = this;
      previewContext.mapTypeToType = new HashMap();
      previewContext.mapTypeToType.put("separator","h-separator");
      previewContext.mapTypeToType.put("glue","v-glue");
      previewContext.supportedActions = supportedActions;
      previewContext.uiElementEventHandler = this;

      
      uiElementFactory = new UIElementFactory();
      
    }
  }/*******************************************************************
                end of class DialogWindow
  *********************************************************************/

  
  
  
  
  
  
  
  
  /**
   * Eine Suchstrategie liefert für eine gegebene Wortzahl eine Liste von
   * Templates für Suchanfragen, die der Reihe nach mit den Wörtern probiert werden
   * sollen bis ein Ergebnis gefunden ist.
   */
  private static class SearchStrategy
  {
    /**
     * Bildet eine Wortanzahl ab auf eine Liste von Query Objekten, die
     * passende Templates darstellen.
     */
    private Map mapWordcountToListOfQuerys;
    
    /**
     * Parst den "Suchstrategie"-Abschnitt von conf und liefert eine entsprechende
     * SearchStrategy.
     * @param conf
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public static SearchStrategy parse(ConfigThingy conf)
    {
      Map mapWordcountToListOfQuerys = new HashMap();
      conf = conf.query("Suchstrategie");
      Iterator parentIter = conf.iterator();
      while (parentIter.hasNext())
      {
        Iterator iter = ((ConfigThingy)parentIter.next()).iterator();
        while (iter.hasNext())
        {
          ConfigThingy queryConf = (ConfigThingy)iter.next();
          String datasource = queryConf.getName();
          List listOfQueryParts = new Vector();
          Iterator columnIter = queryConf.iterator();
          int wordcount = 0;
          while (columnIter.hasNext())
          {
            ConfigThingy qconf = (ConfigThingy)columnIter.next();
            String columnName = qconf.getName();
            String searchString = qconf.toString();
            Matcher m = Pattern.compile("\\$\\{suchanfrage[1-9]\\}").matcher(searchString); 
            while (m.find())
            {
              int wordnum = searchString.charAt(m.end() - 2) - '0';
              if (wordnum > wordcount) wordcount = wordnum;
            }
            listOfQueryParts.add(new QueryPart(columnName, searchString));
          }
          
          Integer wc = new Integer(wordcount);
          if (!mapWordcountToListOfQuerys.containsKey(wc))
            mapWordcountToListOfQuerys.put(wc, new Vector());
          
          List listOfQueries = (List)mapWordcountToListOfQuerys.get(wc);
          listOfQueries.add(new Query(datasource, listOfQueryParts));
        }
      }
      
      return new SearchStrategy(mapWordcountToListOfQuerys);
    }

    
    /**
     * mapWordcountToListOfQueries wird per Referenz eingebunden und entsprechende
     * Ergebnisse aus dieser Map werden von {@link #getTemplate(int)} zurückgeliefert.
     */
    private SearchStrategy(Map mapWordcountToListOfQuerys)
    {
      this.mapWordcountToListOfQuerys = mapWordcountToListOfQuerys; 
    }

    /**
     * Liefert eine Liste von Query-Objekten, die jeweils ein Template für eine
     * Query sind, die bei einer Suchanfrage mit wordcount Wörtern durchgeführt
     * werden soll. Die Querys sollen in der Reihenfolge in der sie in der Liste
     * stehen durchgeführt werden solange bis eine davon ein Ergebnis liefert.
     * @return null falls keine Strategie für den gegebenen wordcount vorhanden.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * TESTED
     */
    public List getTemplate(int wordcount)
    {
      return (List)mapWordcountToListOfQuerys.get(new Integer(wordcount));
    }
  }
  
  /**
   * Nimmt ein Template für eine Suchanfrage entgegen (das Variablen der Form
   * "${suchanfrageX}" enthalten kann) und instanziiert es mit Wörtern aus words, wobei
   * nur die ersten wordcount Einträge von words beachtet werden. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private Query resolveTemplate(Query template, String[] words, int wordcount)
  {
    String dbName = template.getDatasourceName();
    List listOfQueryParts = new Vector();
    Iterator qpIter = template.iterator();
    while (qpIter.hasNext())
    {
      QueryPart templatePart = (QueryPart)qpIter.next();
      String str = templatePart.getSearchString();

      for (int i = 0; i < wordcount; ++i)
      {
        str = str.replaceAll("\\$\\{suchanfrage"+(i+1)+"\\}", words[i].replaceAll("\\$","\\\\\\$"));
      }

      QueryPart part = new QueryPart(templatePart.getColumnName(), str); 
      listOfQueryParts.add(part);
    }
    return  new Query(dbName, listOfQueryParts);
  }
  
  /**
   * Liefert zur Anfrage queryString eine Liste von {@link Query}s, die der Reihe
   * nach probiert werden sollten,
   * gemäß der Suchstrategie searchStrategy (siehe {@link #parseSearchstrategy(ConfigThingy)}).
   * Gibt es für die übergebene Anzahl Wörter keine Suchstrategie, so wird solange
   * das letzte Wort entfernt bis entweder nichts mehr übrig ist oder eine Suchstrategie
   * für die Anzahl Wörter gefunden wurde.
   * @return die leere Liste falls keine Liste bestimmt werden konnte.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private List parseQuery(SearchStrategy searchStrategy, String queryString)
  {
    List queryList = new Vector();
    
    /*
     * Kommata durch Space ersetzen (d.h. "Benkmann,Matthias" -> "Benkmann Matthias")
     */
    queryString = queryString.replaceAll(","," ");
    
    /*
     * Suchstring zerlegen.
     */
    String[] queryArray = queryString.trim().split("\\p{Space}+");
    
    /*
     * Benutzerseitig wir nur ein einzelnes Sternchen am Ende eines Wortes 
     * akzeptiert. Deswegen entferne alle anderen Sternchen. 
     * Ein Punkt am Ende eines Wortes wird als Abkürzung interpretiert
     * und durch Sternchen ersetzt. Ausserdem
     * entferne leere Wörter und berechne neue Arraylänge. 
     */
    int count = queryArray.length;
    for (int i = 0; i < queryArray.length && queryArray[i] != null; ++i)
    {
      boolean suffixStar = queryArray[i].endsWith("*") || queryArray[i].endsWith(".");
      if (queryArray[i].endsWith(".")) 
        queryArray[i] = queryArray[i].substring(0,queryArray[i].length()-1);
      
      queryArray[i] = queryArray[i].replaceAll("\\*","");
      if (queryArray[i].length() == 0)
      {
        for (int j = i + 1; j < queryArray.length; ++j)
          queryArray[j-1] = queryArray[j];
        
        --count;
        --i;
        queryArray[queryArray.length - 1] = null;
      }
      else
      {
        if (suffixStar) queryArray[i] = queryArray[i] + "*";
      }
    }
    
    /*
     * Passende Suchstrategie finden; falls nötig dazu Wörter am Ende weglassen.
     */
    while( count > 0 && 
           searchStrategy.getTemplate(count) == null
         )
      --count;
    
    /*
     * leerer Suchstring (bzw. nur einzelne Sternchen)
     * oder keine Suchstrategie gefunden
     */
    if (count == 0) return queryList; 
    
    List templateList = searchStrategy.getTemplate(count);
    Iterator iter = templateList.iterator();
    while (iter.hasNext())
    {
      Query template = (Query)iter.next();
      queryList.add(resolveTemplate(template, queryArray, count));
    }
    
    return queryList;
  }
  
  /**
   * Ersetzt "${SPALTENNAME}" in str durch den Wert der entsprechenden Spalte im
   * datensatz.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String substituteVars(String str, Dataset datensatz)
  {
    Pattern p = Pattern.compile("\\$\\{([a-zA-Z0-9]+)\\}");
    Matcher m = p.matcher(str);
    if (m.find()) do
    {
      String spalte = m.group(1);
      String wert = spalte;
      try{
        String wert2 = datensatz.get(spalte);
        if (wert2 != null)
          wert = wert2.replaceAll("\\$", "");
      } catch (ColumnNotFoundException e) { Logger.error("Fehler beim Auflösen des Platzhalters \"${"+spalte+"}\": Spalte für den Datensatz nicht definiert");}
      str = str.substring(0, m.start()) + wert + str.substring(m.end());
      m = p.matcher(str);
    } while (m.find());
    return str;
  }

    

    

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    dialogEnd("abort");
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void back()
  {
    dialogEnd("back");
  }
  
  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void select(Collection schema, Dataset ds)
  {
    if (ds != null)
    {
      Map newData = new HashMap();
      Iterator iter = schema.iterator();
      while (iter.hasNext())
      {
        String columnName = (String)iter.next();
        try{
          newData.put(columnName, ds.get(columnName));
        }catch(Exception x) {Logger.error("Huh? Dies sollte nicht passieren können", x);}
      }
      
      synchronized(data)
      {
        data[0] = newData;
      } 
    }
    dialogEnd("select");
  }
  
  /**
   * Beendet den Dialog und ruft falls nötig den dialogEndListener auf
   * wobei das gegebene actionCommand übergeben wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  { 
    myFrame.dispose();
    synchronized(shown)
    {
      shown[0] = false;
    }
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent("PersoenlicheAbsenderliste",0,actionCommand));
  }
  
  
  /**
   * Liefert neue Instanzen eines DatasourceSearchDialogs. Alle Dialoge, die
   * über den selben Instantiator erzeugt wurden erzeugen ihrerseits wieder neue
   * Instanzen über diesen Instantiator, d.h. insbesondere dass 
   * instanceFor(context).instanceFor(context).instanceFor(context) den selben
   * Dialog liefert wie instanceFor(context).
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class Instantiator implements Dialog
  {
    private ConfigThingy conf;
    private DatasourceJoiner dj;
    
    public Instantiator(ConfigThingy conf, DatasourceJoiner dj)
    {
      this.conf = conf;
      this.dj = dj;
    }
    
    public Dialog instanceFor(Map context) throws ConfigurationErrorException
    {
      if (!context.containsKey(this))
        context.put(this, new DatasourceSearchDialog(this, conf, dj));
      return (Dialog)context.get(this);
    }
    
    public Object getData(String id)  { return null;}
    public void show(ActionListener dialogEndListener, FunctionLibrary funcLib, DialogLibrary dialogLib) {}
  }
  
  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als
   * Reaktion auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener(){}
    public void windowActivated(WindowEvent e) { }
    public void windowClosed(WindowEvent e) {}
    public void windowClosing(WindowEvent e) { abort(); }
    public void windowDeactivated(WindowEvent e) { }
    public void windowDeiconified(WindowEvent e) {}
    public void windowIconified(WindowEvent e) { }
    public void windowOpened(WindowEvent e) {}
  }
  
  /**
   * Definition einer neuen Spalte als Funktion alter Spaltenwerte.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class ColumnTranslation
  {
    /**
     * der Name der neuen Spalte.
     */
    private String newColumnName;
    
    /**
     * Die Funktion, die die neue Spalte definiert.
     */
    private Function func;
    
    /**
     * Erzeugt eine neue ColumnTranslation, die eine neue Spalte mit Namen
     * newColumnName definiert als Wert der Funktion func.
     */
    public ColumnTranslation(String newColumnName, Function func)
    {
      this.newColumnName = newColumnName;
      this.func = func;
    }
    
    /**
     * Liefert den Namen der neuen Spalte.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String getNewColumnName()
    {
      return newColumnName;
    }
    
    /**
     * Liefert den Wert der neuen Spalte für Datensatz ds.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String getNewColumnValue(Dataset ds)
    {
      return func.getString(new DatasetValues(ds));
    }
  }
  
  /**
   * Parst conf,query("Spaltenumsetzung") und liefert eine Liste, die
   * für jeden Eintrag ein entsprechendes ColumnTranslation Objekt enthält.
   * @param schema Dieser Collection werden die Namen aller Ergebnisspalten
   *        hinzugefügt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TESTED
   */
  private List parseColumnTranslations(ConfigThingy conf, Collection schema)
  {
    Vector columnTrans = new Vector();
    Iterator parentIter = conf.query("Spaltenumsetzung").iterator();
    while (parentIter.hasNext())
    {
      Iterator iter = ((ConfigThingy)parentIter.next()).iterator();
      while (iter.hasNext())
      {
        ConfigThingy transConf = (ConfigThingy)iter.next();
        String name = transConf.getName();
        try
        {
          Function func = FunctionFactory.parseChildren(transConf, funcLib, dialogLib, context);
          columnTrans.add(new ColumnTranslation(name, func));
          schema.add(name);
        }
        catch (ConfigurationErrorException e)
        {
          Logger.error("Fehler beim Parsen der Spaltenumsetzungsfunktion für Ergebnisspalte \"" + name + "\"", e);        
        }
      }
    }
    
    columnTrans.trimToSize();
    return columnTrans;
  }
    
  /**
   * Stellt die Spalten eines Datasets als Values zur Verfügung.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class DatasetValues implements Values
  {
    private Dataset ds;
    
    public DatasetValues(Dataset ds)
    {
      this.ds = ds;
    }

    public boolean hasValue(String id)
    {
      try{
        ds.get(id);
      }catch(ColumnNotFoundException x)
      {
        return false;
      }
      return true;
    }

    public String getString(String id)
    {
      String str = null;
      try{
        str = ds.get(id);
      }catch(ColumnNotFoundException x) {}
      
      return str == null ? "": str;
    }

    public boolean getBoolean(String id)
    {
      return getString(id).equalsIgnoreCase("true");
    }
  }
  
  /**
   * Wendet Spaltenumsetzungen auf QueryResults an und stellt das Ergebnis
   * wieder als QueryResults zur Verfügung.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class TranslatedQueryResults implements QueryResults
  {
    private List qres;
    
    /**
     * Die QueryResults res werden mit den columnTranslations (Liste von
     * ColumnTranslation Objekten) übersetzt. 
     */
    public TranslatedQueryResults(QueryResults res, List columnTranslations)
    {
      qres = new Vector(res.size());
      Iterator iter = res.iterator();
      while (iter.hasNext())
      {
        Dataset ds = (Dataset)iter.next();
        Map data = new HashMap();
        Iterator transIter = columnTranslations.iterator();
        while (transIter.hasNext())
        {
          ColumnTranslation trans = (ColumnTranslation)transIter.next();
          data.put(trans.getNewColumnName(), trans.getNewColumnValue(ds));
        }
        qres.add(new SimpleDataset(ds.getKey(), data));
      }
    }
    
    public int size() {  return qres.size();}
    public Iterator iterator() { return qres.iterator(); }
    public boolean isEmpty() { return qres.isEmpty(); }
  }
  
  /**
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws Exception
  {
    WollMuxFiles.setupWollMuxDir();
    Logger.init(System.err, Logger.DEBUG);
    String confFile = "testdata/formulartest.conf";
    ConfigThingy conf = new ConfigThingy("", new URL(new File(System
        .getProperty("user.dir")).toURL(), confFile));
    Dialog dialog = DatasourceSearchDialog.create(conf.get("Funktionsdialoge").get("Empfaengerauswahl"), WollMuxFiles.getDatasourceJoiner());
    Map myContext = new HashMap();
    dialog.instanceFor(myContext).show(null, new FunctionLibrary(), new DialogLibrary());
  }

  
}
