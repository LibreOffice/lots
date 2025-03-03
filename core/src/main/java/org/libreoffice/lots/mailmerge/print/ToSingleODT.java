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

import org.libreoffice.lots.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;

/**
 * Print function for creating one odt file per mailmerge record.
 */
public class ToSingleODT extends MailMergePrintFunction
{
  /**
   * A {@link PrintFunction} with name "MailMergeNewToSingleODT" and order 200.
   */
  public ToSingleODT()
  {
    super("MailMergeNewToSingleODT", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    boolean isODT = true;
    saveOutputFile(createTempDocument(printModel, isODT), printModel.getTextDocument());
  }

}
