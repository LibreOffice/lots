package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnManagePrintFunction extends BasicEvent
{
	private XTextDocument doc;

	private String functionName;

	private boolean remove;

	public OnManagePrintFunction(XTextDocument doc, String functionName,
	    boolean remove)
	{
		this.doc = doc;
		this.functionName = functionName;
		this.remove = remove;
	}

	@Override
	protected void doit() throws WollMuxFehlerException
	{
		TextDocumentController documentController = DocumentManager
		    .getTextDocumentController(doc);
		if (remove)
			documentController.removePrintFunction(functionName);
		else
			documentController.addPrintFunction(functionName);
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", '"
		    + functionName + "', remove=" + remove + ")";
	}
}