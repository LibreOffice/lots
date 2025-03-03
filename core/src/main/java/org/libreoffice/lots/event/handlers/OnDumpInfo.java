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
package org.libreoffice.lots.event.handlers;

import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.util.L;

/**
 * Event for dumping addintional information about WollMux.
 *
 * @see WollMuxFiles#dumpInfo()
 */
public class OnDumpInfo extends WollMuxEvent
{

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    final String title = L.m("Create error information");
    String name = WollMuxFiles.dumpInfo();

    if (name != null)
    {
      InfoDialog.showInfoModal(title, L.m(
          "The WollMux error information has been successfully written to file \"{0}\".",
          name));
    } else
    {
      InfoDialog.showInfoModal(title, L.m(
          "The WollMux error information could not be written. For details have a look at the file wollmux.log!"));
    }
  }
}
