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
package org.libreoffice.lots.slv.dispatch;

import org.libreoffice.lots.dispatch.WollMuxDispatch;
import org.libreoffice.lots.document.DocumentManager;
import org.libreoffice.lots.event.handlers.WollMuxEvent;
import org.libreoffice.lots.slv.events.OnMarkBlock;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

/**
 * Dispatch for marking a block as print block.
 */
public class MarkBlockDispatch extends WollMuxDispatch
{
  /**
   * The command of this dispatch.
   */
  public static final String COMMAND = "wollmux:MarkBlock";

  MarkBlockDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL arg0, PropertyValue[] arg1)
  {
    WollMuxEvent event = new OnMarkBlock(DocumentManager.getTextDocumentController(frame),
        getMethodArgument());
    event.emit();
  }

}
