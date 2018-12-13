package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erzeugt ein Event, das den ViewCursor des Dokuments auf das aktuell in der
 * Formular-GUI bearbeitete Formularfeld setzt.
 *
 * Dieses Event wird (derzeit) vom FormModelImpl ausgelöst, wenn in der
 * Formular-GUI ein Formularfeld den Fokus bekommen hat und es sorgt dafür, dass
 * der View-Cursor des Dokuments das entsprechende FormField im Dokument anspringt.
 *
 * @param idToFormValues
 *          Eine HashMap die unter dem Schlüssel fieldID den Vektor aller
 *          FormFields mit der ID fieldID liefert.
 * @param fieldId
 *          die ID des Formularfeldes das den Fokus bekommen soll. Besitzen mehrere
 *          Formularfelder diese ID, so wird bevorzugt das erste Formularfeld aus
 *          dem Vektor genommen, das keine Trafo enthält. Ansonsten wird das erste
 *          Formularfeld im Vektor verwendet.
 */
public class OnFocusFormField extends BasicEvent
{
	private String fieldId;
	private TextDocumentController documentController;

	public OnFocusFormField(TextDocumentController documentController,
	    String fieldId)
	{
		this.documentController = documentController;
		this.fieldId = fieldId;
	}

	@Override
	protected void doit()
	{
		documentController.getModel().focusFormField(fieldId);
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(#"
		    + documentController.getModel().doc + ", '" + fieldId
		    + "')";
	}
}
