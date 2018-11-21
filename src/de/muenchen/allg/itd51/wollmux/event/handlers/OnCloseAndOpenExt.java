package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die Aktion
 * "closeAndOpenExt" aktiviert hat.
 *
 * @author matthias.benkmann
 */
public class OnCloseAndOpenExt extends BasicEvent
{
	private static final Logger LOGGER = LoggerFactory
		      .getLogger(OnCloseAndOpenExt.class);
	
    private String ext;
    private TextDocumentController documentController;

    public OnCloseAndOpenExt(TextDocumentController documentController, String ext)
    {
      this.documentController = documentController;
      this.ext = ext;
    }

    @Override
    protected void doit()
    {
      try
      {
        OpenExt openExt = new OpenExt(ext, WollMuxFiles.getWollmuxConf());
        openExt.setSource(UNO.XStorable(documentController.getModel().doc));
        openExt.storeIfNecessary();
        openExt.launch(new OpenExt.ExceptionHandler()
        {
          @Override
          public void handle(Exception x)
          {
            LOGGER.error("", x);
          }
        });
      }
      catch (Exception x)
      {
        LOGGER.error("", x);
        return;
      }

      documentController.getModel().setDocumentModified(false);
      documentController.getModel().close();
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ", " + ext
        + ")";
    }
  }
