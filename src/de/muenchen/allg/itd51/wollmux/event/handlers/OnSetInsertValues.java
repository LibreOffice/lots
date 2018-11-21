package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass im Textdokument doc alle
 * insertValue-Befehle mit einer DB_SPALTE, die in der übergebenen
 * mapDbSpalteToValue enthalten sind neu für den entsprechenden Wert evaluiert und
 * gesetzt werden, unabhängig davon, ob sie den Status DONE besitzen oder nicht.
 *
 * Das Event wird aus der Implementierung von XWollMuxDocument (siehe
 * compo.WollMux) geworfen, wenn dort die Methode setInsertValue aufgerufen wird.
 *
 * @param doc
 *          Das Dokument, in dem das die insertValue-Kommandos neu gesetzt werden
 *          sollen.
 * @param mapDbSpalteToValue
 *          Enthält eine Zuordnung von DB_SPALTEn auf die zu setzenden Werte.
 *          Enthält ein betroffenes Dokumentkommando eine Trafo, so wird die Trafo
 *          mit dem zugehörigen Wert ausgeführt und das Transformationsergebnis als
 *          neuer Inhalt des Bookmarks gesetzt.
 * @param unlockActionListener
 *          Wird informiert, sobald das Event vollständig abgearbeitet wurde.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class OnSetInsertValues extends BasicEvent 
{
	private static final Logger LOGGER = LoggerFactory
		      .getLogger(OnSetInsertValues.class);
	
    private XTextDocument doc;

    private Map<String, String> mapDbSpalteToValue;

    private ActionListener listener;

    public OnSetInsertValues(XTextDocument doc,
        Map<String, String> mapDbSpalteToValue, ActionListener unlockActionListener)
    {
      this.doc = doc;
      this.mapDbSpalteToValue = mapDbSpalteToValue;
      this.listener = unlockActionListener;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      TextDocumentController documentController =
        DocumentManager.getTextDocumentController(doc);

      for (DocumentCommand cmd :documentController.getModel().getDocumentCommands())
        // stellt sicher, dass listener am Schluss informiert wird
        try
        {
          if (cmd instanceof DocumentCommand.InsertValue)
          {
            DocumentCommand.InsertValue insVal = (DocumentCommand.InsertValue) cmd;
            String value = mapDbSpalteToValue.get(insVal.getDBSpalte());
            if (value != null)
            {
              value = documentController.getTransformedValue(insVal.getTrafoName(), value);
              if ("".equals(value))
              {
                cmd.setTextRangeString("");
              }
              else
              {
                cmd.setTextRangeString(insVal.getLeftSeparator() + value
                  + insVal.getRightSeparator());
              }
            }
          }
        }
        catch (java.lang.Exception e)
        {
          LOGGER.error("", e);
        }
      if (listener != null) listener.actionPerformed(null);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + doc.hashCode()
        + ", Nr.Values=" + mapDbSpalteToValue.size() + ")";
    }
  }