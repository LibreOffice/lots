package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.dialog.formmodel.SingleDocumentFormModel;

public class OnSetWindowPosSizeSingleForm extends BasicEvent
{
	private int docX;
	private int docY;
	private int docWidth;
	private int docHeight;
	private SingleDocumentFormModel m;

	public OnSetWindowPosSizeSingleForm(SingleDocumentFormModel m, int docX,
	    int docY,
	    int docWidth, int docHeight)
	{
		this.m = m;
		this.docX = docX;
		this.docY = docY;
		this.docWidth = docWidth;
		this.docHeight = docHeight;
	}

	@Override
	protected void doit()
	{
		m.setWindowPosSize(docX, docY, docWidth, docHeight);
	}
}
