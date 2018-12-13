package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die Aktion
 * "saveTempAndOpenExt" aktiviert hat.
 *
 * @author matthias.benkmann
 */

public class OnSaveTempAndOpenExt extends BasicEvent
{
	private static final Logger LOGGER = LoggerFactory
	    .getLogger(OnSaveTempAndOpenExt.class);

	private String ext;

	private TextDocumentController documentController;

	public OnSaveTempAndOpenExt(TextDocumentController documentController,
	    String ext)
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
			openExt.launch(x -> LOGGER.error("", x));
		} catch (Exception x)
		{
			LOGGER.error("", x);
			return;
		}
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(#"
		    + documentController.getModel().hashCode() + ", " + ext
		    + ")";
	}
}