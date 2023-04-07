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
package de.muenchen.allg.itd51.wollmux.sender.dispatch;

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.dispatch.Dispatcher;
import de.muenchen.allg.itd51.wollmux.dispatch.WollMuxDispatch;

/**
 * Dispatcher for all sender dispatches.
 */
public class SenderDispatcher extends Dispatcher
{
  /**
   * Register all sender dispatches.
   */
  public SenderDispatcher()
  {
    super(PALVerwaltenDispatch.COMMAND);
  }

  @Override
  public WollMuxDispatch create(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    if (PALVerwaltenDispatch.COMMAND.equals(getDispatchMethodName(origUrl)))
    {
      return new PALVerwaltenDispatch(origDisp, origUrl, frame);
    } else
    {
      return null;
    }
  }

}
