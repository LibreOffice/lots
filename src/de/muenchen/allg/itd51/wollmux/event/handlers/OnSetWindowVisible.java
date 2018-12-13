package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Dieses Event wird vom FormModelImpl ausgelöst, wenn die Formular-GUI das
 * bearbeitete Dokument sichtbar/unsichtbar schalten möchte. Ruft direkt setVisible
 * der UNO-API auf.
 *
 * @author christoph.lutz
 */
public class OnSetWindowVisible extends BasicEvent
{
	boolean visible;

	private TextDocumentController documentController;

	public OnSetWindowVisible(TextDocumentController documentController,
	    boolean visible)
	{
		this.documentController = documentController;
		this.visible = visible;
	}

	@Override
	protected void doit()
	{
		documentController.getFrameController().setWindowVisible(visible);
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + visible + ")";
	}
}