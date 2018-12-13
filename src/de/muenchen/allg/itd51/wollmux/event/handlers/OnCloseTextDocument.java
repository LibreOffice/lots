package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die
 * Formular-GUI schließt und damit auch das zugehörige TextDokument geschlossen
 * werden soll.
 *
 * @author christoph.lutz
 */
public class OnCloseTextDocument extends BasicEvent
{
	private TextDocumentController documentController;

	public OnCloseTextDocument(TextDocumentController documentController)
	{
		this.documentController = documentController;
	}

	@Override
	protected void doit()
	{
		documentController.getModel().close();
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(#"
		    + documentController.getModel().hashCode() + ")";
	}
}