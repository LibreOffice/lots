/*
* Dateiname: PrintIntoFile.java
* Projekt  : WollMux
* Funktion : "Druck"funktion, die das zu druckende Dokument an ein Ergebnisdokument anhängt.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 29.10.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.func;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.XEnumeration;
import com.sun.star.io.IOException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

/**
 * "Druck"funktion, die das zu druckende Dokument an ein Ergebnisdokument anhängt.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PrintIntoFile
{
  private static final String TEMP_DIR_PREFIX = "wollmux-seriendruck-";
  
  /**
   * Hängt das zu pmod gehörige TextDocument an das im Property
   * PrintIntoFile_OutputDocument gespeicherte XTextDocument an. Falls
   * noch kein solches Property existiert, wird ein leeres Dokument angelegt.
   * @throws Exception
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static void printIntofile(XPrintModel pmod) throws Exception
  {
    boolean initFormats = true;
    XTextDocument outputDoc = null;
    try
    {
      outputDoc = UNO.XTextDocument(pmod.getPropertyValue("PrintIntoFile_OutputDocument"));
      initFormats = false;
    }
    catch (UnknownPropertyException e)
    {
      outputDoc = UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true, true));
      pmod.setPropertyValue("PrintIntoFile_OutputDocument", outputDoc);
    }
    
    appendToFile(outputDoc, pmod.getTextDocument(), initFormats);
  }
  
  /**
   * Hängt den Inhalt von inputDoc an outputDoc an.
   * @param initFormats falls true werden die Formate aus inputDoc zuerst nach
   *        outputDoc übertragen. 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public static void appendToFile(XTextDocument outputDoc, XTextDocument inputDoc, boolean initFormats)
  {
    File destFile = null;
    File downloadDir = null;
    try{
      /** 
       * Zuerst inputDoc in eine temporäre Datei schreiben 
       */
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      if (!tmpDir.isDirectory() && !tmpDir.canWrite())
      {
        Logger.error("Temporäres Verzeichnis\n\""+tmpDir.getPath()+"\"\nexistiert nicht oder kann nicht beschrieben werden!");
        return;
      }
      
      for (int i = 0; i < 1000; ++i)
      {
        downloadDir = new File(tmpDir, TEMP_DIR_PREFIX+i);
        if (downloadDir.mkdir())
          break;
        else 
          downloadDir = null;
      }
      
      if (downloadDir == null)
      {
        Logger.error("Konnte kein temporäres Verzeichnis für die temporären Seriendruckdaten anlegen!");
        return;
      }
      
      destFile = new File(downloadDir, "serienbrief.odt");
      String url = UNO.getParsedUNOUrl(destFile.toURI().toURL().toExternalForm()).Complete;
      
      PropertyValue[] arguments = new PropertyValue[]{new PropertyValue(), new PropertyValue()};
      arguments[0].Name = "Overwrite";
      arguments[0].Value = Boolean.FALSE;
      arguments[1].Name = "FilterName"; //found in /opt/openoffice.org/share/registry/modules/org/openoffice/TypeDetection/Filter/fcfg_writer_filters.xcu
      arguments[1].Value = "StarOffice XML (Writer)";
      UNO.XStorable(inputDoc).storeToURL(url, arguments);
    }
    catch(Exception x)
    {
      Logger.error(x);
    }
    finally{
      try{destFile.delete();}catch(Exception x){}
      try{downloadDir.delete();}catch(Exception x){}
    }
  }

  /**
   *@author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws Exception
  {
    UNO.init();
    Logger.init(Logger.ALL);
    
    final boolean[] done = new boolean[]{false};
    SwingUtilities.invokeAndWait(new Runnable(){
      public void run()
      {
        JFrame myFrame = new JFrame("PrintIntoFile");
        myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        myFrame.addWindowListener(new WindowAdapter()
        {
          public void windowClosing(WindowEvent e) {synchronized(done){done[0] = true; done.notifyAll();} }
        });
        
        final XTextDocument[] doc = new XTextDocument[]{null};
        final boolean[] initFormats = new boolean[]{true};
        
        myFrame.setLayout(new GridLayout(1,2));
        JButton button = new JButton("Neues Gesamtdokument");
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            if (doc[0] != null)
            {
              try{ UNO.XCloseable(doc[0]).close(true);}catch(Exception x){}
              doc[0] = null;
            }
            initFormats[0] = true;
            try{ doc[0] = UNO.XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true, true));}catch(Exception x){}
          }});
        myFrame.add(button);
        button = new JButton("Dokument anhängen");
        button.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent e)
          {
            if (doc[0] == null) return;
            
            /*
             * Wenn das aktuelle Vordergrunddok ein Textdokument ist und nicht das
             * Gesamtdokument, so wähle es aus.
             */
            XTextDocument inputDoc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
            if (inputDoc == null || UnoRuntime.areSame(inputDoc, doc[0]))
            {
              /*
               * Ansonsten suchen wir, ob wir ein Textdokument finden, das nicht das
               * Gesamtdokument ist. 
               */
              try{
                XEnumeration xenu = UNO.desktop.getComponents().createEnumeration();
                while(xenu.hasMoreElements())
                {
                  inputDoc = UNO.XTextDocument(xenu.nextElement());
                  if (inputDoc != null && !UnoRuntime.areSame(inputDoc, doc[0]))
                    break;
                }
              }
              catch(Exception x)
              {}
            }
            
            /*
             * Falls wir keinen andere Kandidaten gefunden haben, so will der
             * Benutzer wohl das Gesamtdokument an sich selbst anhängen.
             */
            if (inputDoc == null) inputDoc = doc[0];
            
            appendToFile(doc[0], inputDoc, initFormats[0]);
            initFormats[0] = false;
          }});
        myFrame.add(button);
        
        myFrame.setAlwaysOnTop(true);
        myFrame.pack();
        int frameWidth = myFrame.getWidth();
        int frameHeight = myFrame.getHeight();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width/2 - frameWidth/2; 
        int y = screenSize.height/2 - frameHeight/2;
        myFrame.setLocation(x,y);
        myFrame.setResizable(false);
        myFrame.setVisible(true);
      }});
    
    synchronized(done)
    {
      while(!done[0]) done.wait();
    }
    
    System.exit(0);
  }

}
