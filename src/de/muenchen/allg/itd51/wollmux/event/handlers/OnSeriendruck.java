package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeNew;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Erzeugt ein neues WollMuxEvent, das signasisiert, dass die neue
 * Seriendruckfunktion des WollMux gestartet werden soll.
 *
 * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
 * "Extras->Seriendruck (WollMux)" die dispatch-url wollmux:SeriendruckNeu
 * abgesetzt wurde.
 */
public class OnSeriendruck extends BasicEvent
{
	private TextDocumentController documentController;

	public OnSeriendruck(TextDocumentController documentController,
	    boolean useDocumentPrintFunctions)
	{
		this.documentController = documentController;
	}

	@Override
	protected void doit() throws WollMuxFehlerException
	{
		// Bestehenden Max in den Vordergrund holen oder neuen Max erzeugen.
		MailMergeNew mmn = DocumentManager.getDocumentManager()
		    .getCurrentMailMergeNew(documentController.getModel().doc);
		if (mmn != null)
		{
			return;
		} else
		{
			mmn = new MailMergeNew(documentController, actionEvent -> {
				if (actionEvent.getSource() instanceof MailMergeNew)
					WollMuxEventHandler.getInstance()
					    .handleMailMergeNewReturned(documentController);
			});
			DocumentManager.getDocumentManager()
			    .setCurrentMailMergeNew(documentController.getModel().doc, mmn);
		}
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + documentController.getModel()
		    + ")";
	}
}