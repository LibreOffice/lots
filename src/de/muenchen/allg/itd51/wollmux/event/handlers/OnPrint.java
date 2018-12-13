package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

public class OnPrint extends BasicEvent
{
	private XDispatch origDisp;

	private com.sun.star.util.URL origUrl;

	private PropertyValue[] origArgs;

	private TextDocumentController documentController;

	public OnPrint(TextDocumentController documentController, XDispatch origDisp,
	    com.sun.star.util.URL origUrl, PropertyValue[] origArgs)
	{
		this.documentController = documentController;
		this.origDisp = origDisp;
		this.origUrl = origUrl;
		this.origArgs = origArgs;
	}

	@Override
	protected void doit() throws WollMuxFehlerException
	{
		boolean hasPrintFunction = !documentController.getModel()
		    .getPrintFunctions().isEmpty();

		if (hasPrintFunction)
		{
			// Druckfunktion aufrufen
			WollMuxEventHandler.getInstance()
			    .handleExecutePrintFunctions(documentController);
		} else
		{
			// Forward auf Standardfunktion
			if (origDisp != null)
				origDisp.dispatch(origUrl, origArgs);
		}
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + documentController.getModel()
		    + ")";
	}
}