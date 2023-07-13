/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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

import org.libreoffice.lots.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;

/**
 * Print function for creating one PDF file per mail merge record.
 */
public class ToSinglePDF extends MailMergePrintFunction
{
  /**
   * A {@link PrintFunction} with name "MailMergeNewToSinglePDF" and order 200.
   */
  public ToSinglePDF()
  {
    super("MailMergeNewToSinglePDF", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    boolean isODT = false;
    saveOutputFile(createTempDocument(printModel, isODT), printModel.getTextDocument());
  }

}
