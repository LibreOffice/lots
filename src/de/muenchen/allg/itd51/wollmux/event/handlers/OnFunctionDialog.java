package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, das den Funktionsdialog dialogName aufruft und
 * die zurückgelieferten Werte in die entsprechenden FormField-Objekte des
 * Dokuments doc einträgt.
 *
 * Dieses Event wird vom WollMux-Service (...comp.WollMux) und aus dem
 * WollMuxEventHandler ausgelöst.
 */
public class OnFunctionDialog extends BasicEvent
{
	private TextDocumentController documentController;

	private String dialogName;

	public OnFunctionDialog(TextDocumentController documentController,
	    String dialogName)
	{
		this.documentController = documentController;
		this.dialogName = dialogName;
	}

	@Override
	protected void doit() throws WollMuxFehlerException
	{
		// Dialog aus Funktionsdialog-Bibliothek holen:
		Dialog dialog = GlobalFunctions.getInstance().getFunctionDialogs()
		    .get(dialogName);
		if (dialog == null)
			throw new WollMuxFehlerException(L.m(
			    "Funktionsdialog '%1' ist nicht definiert.", dialogName));

		// Dialoginstanz erzeugen und modal anzeigen:
		Dialog dialogInst = null;
		try
		{
			dialogInst = dialog.instanceFor(new HashMap<Object, Object>());

			setLock();
			dialogInst.show(unlockActionListener,
			    documentController.getFunctionLibrary(),
			    documentController.getDialogLibrary());
			waitForUnlock();
		} catch (ConfigurationErrorException e)
		{
			throw new CantStartDialogException(e);
		}

		// Abbruch, wenn der Dialog nicht mit OK beendet wurde.
		String cmd = unlockActionListener.actionEvent.getActionCommand();
		if (!cmd.equalsIgnoreCase("select"))
			return;

		// Dem Dokument den Fokus geben, damit die Änderungen des Benutzers
		// transparent mit verfolgt werden können.
		try
		{
			documentController.getFrameController().getFrame().getContainerWindow()
			    .setFocus();
		} catch (java.lang.Exception e)
		{
			// keine Gefährdung des Ablaufs falls das nicht klappt.
		}

		// Alle Werte die der Funktionsdialog sicher zurück liefert werden in
		// das Dokument übernommen.
		Collection<String> schema = dialogInst.getSchema();
		Iterator<String> iter = schema.iterator();
		while (iter.hasNext())
		{
			String id = iter.next();
			String value = dialogInst.getData(id).toString();

			documentController.addFormFieldValue(id, value);
		}

		stabilize();
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(" + documentController + ", '"
		    + dialogName
		    + "')";
	}
}