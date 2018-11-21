package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.text.XPageCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.view.XPrintable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnPrintPage extends BasicEvent 
{

    private TextDocumentController documentController;

		public OnPrintPage(TextDocumentController documentController) {
			this.documentController = documentController;
		}

		@Override
    protected void doit() throws WollMuxFehlerException
    {
		  XTextViewCursorSupplier viewCursorSupplier = UNO.XTextViewCursorSupplier(documentController.getModel().doc.getCurrentController());
		  XPageCursor pageCursor = UNO.XPageCursor(viewCursorSupplier.getViewCursor());
		  short page = pageCursor.getPage();
		  XPrintable printable = UNO.XPrintable(documentController.getModel().doc);

		  UnoProps props = new UnoProps();
		  props.setPropertyValue("CopyCount", (short)1);
      props.setPropertyValue("Pages", String.valueOf(page));
      props.setPropertyValue("Collate", false);

      printable.print(props.getProps());
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }