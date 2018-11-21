package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;

/**
 * Der Handler für das Erzeugen von neuen Dokumenten. Das Dokuement wird dem
 * DocumentManager hinzugefügt.
 * 
 * @param comp
 *          Das neue Dokument.
 */
public class OnCreateDocument extends BasicEvent
{
    private XComponent comp;

    public OnCreateDocument(XComponent comp)
    {
      this.comp = comp;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      DocumentManager docManager = DocumentManager.getDocumentManager();
      XTextDocument xTextDoc = UNO.XTextDocument(comp);

      // durch das Hinzufügen zum docManager kann im Event onViewCreated erkannt
      // werden, dass das Dokument frisch erzeugt wurde.
      if (xTextDoc != null)
      {
        docManager.addTextDocument(xTextDoc);
      } else
      {
        docManager.add(comp);
      }
    }
  }