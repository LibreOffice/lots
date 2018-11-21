package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager.Info;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Der Handler wird aufgerufen, wenn die View erstellt wurde. Es wird geprüft,
 * ob das Dokument überhaupt vom WollMux bearbeitet werden muss und
 * gegebenenfalls wird es wieder aus dem DocumentManager gelöscht.
 * 
 * @param comp
 *          Das Dokumenten Modell.
 */
public class OnViewCreated extends BasicEvent 
{
    private static final Logger LOGGER = LoggerFactory
		      .getLogger(OnViewCreated.class);

    private XModel comp;

    public OnViewCreated(XModel comp)
    {
      this.comp = comp;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      DocumentManager docManager = DocumentManager.getDocumentManager();
      // Keine Aktion bei neu (mit Create) erzeugten und temporären,
      // unsichtbaren Textdokumente des OOo-Seriendrucks. Sicherstellen,
      // dass diese Dokumente auch nicht im docManager mitgeführt werden.
      if (isTempMailMergeDocument(comp))
      {
        // docManager.remove(source) ist hier nicht erforderlich, weil für
        // Dokumente mit URL kein OnCreate-Event kommt.
        return;
      }
      Info docInfo = docManager.getInfo(comp);
      // docInfo ist hier nur dann ungleich null, wenn das Dokument mit Create
      // erzeugt wurde.
      XTextDocument xTextDoc = UNO.XTextDocument(comp);
      if (xTextDoc != null && docInfo != null && isDocumentLoadedHidden(comp))
      {
        docManager.remove(comp);
        return;
      }

      // Dokument ggf. in docManager aufnehmen und abhängig vom Typ verarbeiten.
      if (xTextDoc != null)
      {
        if (docInfo == null)
          docManager.addTextDocument(xTextDoc);
        WollMuxEventHandler.getInstance().handleProcessTextDocument(
            DocumentManager.getTextDocumentController(xTextDoc),
            !isDocumentLoadedHidden(comp));
      } else
      {
        if (docInfo == null)
          docManager.add(comp);
        WollMuxEventHandler.getInstance().handleNotifyDocumentEventListener(null,
            WollMuxEventHandler.ON_WOLLMUX_PROCESSING_FINISHED, comp);
      }
    }

    /**
     * Liefert zurück, ob es sich bei dem Dokument source um ein Temporäres
     * Dokument des OOo-Seriendrucks handelt und wird benötigt um solche
     * Dokumente im Workaround für Ticket #3091 zu ignorieren. Dabei kann diese
     * Methode nur Dokumente erkennen, die anhand der Eigenschaft URL als
     * temporäre Datei zu erkennen sind.
     * 
     * Anmerkung: Der OOo-Seriendruck kann über Datei->Drucken und über
     * Extras->Seriendruck-Assistent gestartet werden. Verschiedene
     * OOo-Versionen verhalten sich diesbezüglich verschieden:
     * 
     * OOo 3.0.1 erzeugt in beiden Varianten für jeden Datensatz eine
     * unsichtbare temporäre Datei mit einer URL, die eine Erkennung der
     * temporären Datei zulässt.
     * 
     * OOo 3.2.1 erzeugt nur noch über Datei->Drucken temoräre Dateien mit
     * gesetzter URL. Über Extras->Seriendruck-Assistent ist die URL-Eigenschaft
     * jedoch nicht mehr gesetzt, so dass diese Methode nicht mehr ausreicht, um
     * temporäre Dokumente des Seriendrucks zu identifizieren.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private boolean isTempMailMergeDocument(XModel compo)
    {
      String url = compo.getURL();
      int idx = url.lastIndexOf('/');
      PropertyValue[] args = compo.getArgs();
      String fileName = "";
      boolean hidden = false;
      for (PropertyValue p : args)
      {
        if (p.Name.equals("FileName"))
          fileName = (String) p.Value;
        if (p.Name.equals("Hidden"))
          hidden = (Boolean) p.Value;
      }

      boolean mmdoc = (/* wird über datei->Drucken in Serienbrief erzeugt: */(url
          .startsWith(".tmp/", idx - 4) && url.endsWith(".tmp"))
          || /* wird über den Service css.text.MailMerge erzeugt: */(url
              .startsWith("/SwMM", idx) && url.endsWith(".odt"))
          || /* wird vom WollMux erzeugt: */url.startsWith("/WollMuxMailMerge",
              idx - 20)
          || (fileName.equals("private:object") && hidden));

      // debug-Meldung bewusst ohne L.m gewählt (WollMux halt dich raus!)
      if (mmdoc)
        LOGGER.trace("temporary document: " + url);
      return mmdoc;
    }

    /**
     * Liefert nur dann true zurück, wenn das Dokument mit der
     * MediaDescriptor-Eigenschaft Hidden=true geöffnet wurde.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private boolean isDocumentLoadedHidden(XModel compo)
    {
      UnoProps props = new UnoProps(compo.getArgs());
      try
      {
        return AnyConverter.toBoolean(props.getPropertyValue("Hidden"));
      } catch (UnknownPropertyException e)
      {
        return false;
      } catch (IllegalArgumentException e)
      {
        LOGGER.error(L.m("das darf nicht vorkommen!"), e);
        return false;
      }
    }
  }