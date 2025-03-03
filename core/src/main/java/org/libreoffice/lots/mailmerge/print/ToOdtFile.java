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

import javax.print.PrintException;

import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.interfaces.XPrintModel;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.MailMergeType;


/**
 * Print function for mailmerge by LibreOffice mailmerge with type {@link MailMergeType#SHELL}.
 */
public class ToOdtFile extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ToOdtFile.class);

  /**
   * A {@link PrintFunction} with name "OOoMailMergeToOdtFile" and order 200.
   */
  public ToOdtFile()
  {
    super("OOoMailMergeToOdtFile", 200);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    try (OOoBasedMailMerge mailMerge = new OOoBasedMailMerge(printModel, MailMergeType.SHELL))
    {
      mailMerge.doMailMerge();
    } catch (PrintException e)
    {
      LOGGER.error("Fehler beim Seriendruck", e);
      printModel.cancel();
      InfoDialog.showInfoModal(L.m("WollMux mail merge"), L.m(e.getMessage()));
    } catch (Exception ex)
    {
      LOGGER.warn("Fehler beim Aufräumen der temporären Dokumente", ex);
    }
  }
}
