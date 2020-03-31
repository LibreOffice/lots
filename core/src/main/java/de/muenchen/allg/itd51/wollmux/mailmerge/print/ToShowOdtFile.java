/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.dispatch.DispatchProviderAndInterceptor;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Open a mail merge result as LibreOffice document.
 */
public class ToShowOdtFile extends PrintFunction
{

  /**
   * A {@link PrintFunction} with name "OOoMailMergeToShowOdtFile" and order 350.
   */
  public ToShowOdtFile()
  {
    super("OOoMailMergeToShowOdtFile", 350);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    XTextDocument result = UNO
        .XTextDocument(printModel.getProp(PrintFunction.PRINT_RESULT, printModel.getTextDocument()));
    if (result != null && result.getCurrentController() != null && result.getCurrentController().getFrame() != null
        && result.getCurrentController().getFrame().getContainerWindow() != null)
    {
      DocumentManager.getDocumentManager().addTextDocument(result);
      DispatchProviderAndInterceptor.registerDocumentDispatchInterceptor(result.getCurrentController().getFrame());
      result.getCurrentController().getFrame().getContainerWindow().setVisible(true);
      UNO.XTopWindow(result.getCurrentController().getFrame().getContainerWindow()).toFront();
    } else
    {
      InfoDialog.showInfoModal(L.m("WollMux-Seriendruck"),
          L.m("Das erzeugte Gesamtdokument kann leider nicht angezeigt werden."));
      printModel.cancel();
    }
  }
}
