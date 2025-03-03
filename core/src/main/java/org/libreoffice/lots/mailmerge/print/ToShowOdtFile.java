/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.mailmerge.print;

import com.sun.star.text.XTextDocument;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.dispatch.DispatchProviderAndInterceptor;
import org.libreoffice.lots.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import org.libreoffice.lots.util.L;

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
      DispatchProviderAndInterceptor.registerDocumentDispatchInterceptor(result.getCurrentController().getFrame());
      result.getCurrentController().getFrame().getContainerWindow().setVisible(true);
      UNO.XTopWindow(result.getCurrentController().getFrame().getContainerWindow()).toFront();
    } else
    {
      InfoDialog.showInfoModal(L.m("WollMux mail merge"),
          L.m("The generated merged document can not be displayed."));
      printModel.cancel();
    }
  }
}
