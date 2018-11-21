package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.XComponent;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.document.VisibleTextFragmentList;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.parser.InvalidIdentifierException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Obsolete, aber aus Kompatibilitätgründen noch vorhanden. Bitte handleOpen()
 * statt dessen verwenden.
 *
 * Erzeugt ein neues WollMuxEvent, welches dafür sorgt, dass ein Dokument geöffnet
 * wird.
 *
 * Dieses Event wird gestartet, wenn der WollMux-Service (...comp.WollMux) das
 * Dispatch-Kommando wollmux:openTemplate bzw. wollmux:openDocument empfängt und
 * sort dafür, dass das entsprechende Dokument geöffnet wird.
 *
 * @param fragIDs
 *          Eine List mit fragIDs, wobei das erste Element die FRAG_ID des zu
 *          öffnenden Dokuments beinhalten muss. Weitere Elemente werden in eine
 *          Liste zusammengefasst und als Parameter für das Dokumentkommando
 *          insertContent verwendet.
 * @param asTemplate
 *          true, wenn das Dokument als "Unbenannt X" geöffnet werden soll (also im
 *          "Template-Modus") und false, wenn das Dokument zum Bearbeiten geöffnet
 *          werden soll.
 */
public class OnOpenDocument extends BasicEvent 
{
	private static final Logger LOGGER = LoggerFactory
		      .getLogger(OnOpenDocument.class);
		      
    private boolean asTemplate;

    private List<String> fragIDs;

    public OnOpenDocument(List<String> fragIDs, boolean asTemplate)
    {
      this.fragIDs = fragIDs;
      this.asTemplate = asTemplate;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      if (!fragIDs.isEmpty())
      {
    	  openTextDocument(fragIDs, asTemplate);
      }
    }
    
    /**
    *
    * @param fragIDs
    * @param asTemplate
    * @throws WollMuxFehlerException
    */
   private void openTextDocument(List<String> fragIDs,
       boolean asTemplate) throws WollMuxFehlerException
   {
     // das erste Argument ist das unmittelbar zu landende Textfragment und
     // wird nach urlStr aufgelöst. Alle weiteren Argumente (falls vorhanden)
     // werden nach argsUrlStr aufgelöst.
     String loadUrlStr = "";
     String[] fragUrls = new String[fragIDs.size() - 1];
     String urlStr = "";

     Iterator<String> iter = fragIDs.iterator();
     for (int i = 0; iter.hasNext(); ++i)
     {
       String frag_id = iter.next();

       // Fragment-URL holen und aufbereiten:
       List<String> urls = new ArrayList<>();

       java.lang.Exception error =
         new ConfigurationErrorException(L.m(
           "Das Textfragment mit der FRAG_ID '%1' ist nicht definiert!", frag_id));
       try
       {
         urls = VisibleTextFragmentList.getURLsByID(WollMuxFiles.getWollmuxConf(), frag_id);
       }
       catch (InvalidIdentifierException e)
       {
         error = e;
       }
       if (urls.isEmpty())
       {
         throw new WollMuxFehlerException(
           L.m(
             "Die URL zum Textfragment mit der FRAG_ID '%1' kann nicht bestimmt werden:",
             frag_id), error);
       }

       // Nur die erste funktionierende URL verwenden. Dazu werden alle URL zu
       // dieser FRAG_ID geprüft und in die Variablen loadUrlStr und fragUrls
       // übernommen.
       String errors = "";
       boolean found = false;
       Iterator<String> iterUrls = urls.iterator();
       while (iterUrls.hasNext() && !found)
       {
         urlStr = iterUrls.next();

         // URL erzeugen und prüfen, ob sie aufgelöst werden kann
         URL url;
         try
         {
           url = WollMuxFiles.makeURL(urlStr);
           urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
           url = WollMuxFiles.makeURL(urlStr);
           WollMuxSingleton.checkURL(url);
         }
         catch (MalformedURLException e)
         {
           LOGGER.info("", e);
           errors +=
             L.m("Die URL '%1' ist ungültig:", urlStr) + "\n"
               + e.getLocalizedMessage() + "\n\n";
           continue;
         }
         catch (IOException e)
         {
           LOGGER.info("", e);
           errors += e.getLocalizedMessage() + "\n\n";
           continue;
         }

         found = true;
       }

       if (!found)
       {
         throw new WollMuxFehlerException(L.m(
           "Das Textfragment mit der FRAG_ID '%1' kann nicht aufgelöst werden:",
           frag_id)
           + "\n\n" + errors);
       }

       // URL in die in loadUrlStr (zum sofort öffnen) und in argsUrlStr (zum
       // später öffnen) aufnehmen
       if (i == 0)
       {
         loadUrlStr = urlStr;
       }
       else
       {
         fragUrls[i - 1] = urlStr;
       }
     }

     // open document as Template (or as document):
     TextDocumentController documentController = null;
     try
     {
       XComponent doc = UNO.loadComponentFromURL(loadUrlStr, asTemplate, true);

       if (UNO.XTextDocument(doc) != null)
       {
         documentController = DocumentManager.getTextDocumentController(UNO.XTextDocument(doc));
         documentController.getModel().setFragUrls(fragUrls);
       }
     }
     catch (java.lang.Exception x)
     {
       // sollte eigentlich nicht auftreten, da bereits oben geprüft.
       throw new WollMuxFehlerException(L.m(
         "Die Vorlage mit der URL '%1' kann nicht geöffnet werden.", loadUrlStr), x);
     }
   }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "("
        + ((asTemplate) ? "asTemplate" : "asDocument") + ", " + fragIDs + ")";
    }
  }
