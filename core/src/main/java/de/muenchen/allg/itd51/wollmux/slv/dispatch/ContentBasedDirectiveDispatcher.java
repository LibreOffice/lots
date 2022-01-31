/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv.dispatch;

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.dispatch.Dispatcher;
import de.muenchen.allg.itd51.wollmux.dispatch.WollMuxDispatch;

/**
 * Dispatcher for content based directive commands.
 */
public class ContentBasedDirectiveDispatcher extends Dispatcher
{
  /**
   * Register dispatches for content based directive commands.
   */
  public ContentBasedDirectiveDispatcher()
  {
    super(DirectiveDispatch.COMMAND, CopyDispatch.COMMAND, RecipientDispatch.COMMAND,
        MarkBlockDispatch.COMMAND);
  }

  @Override
  public WollMuxDispatch create(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    switch (getDispatchMethodName(origUrl))
    {
    case DirectiveDispatch.COMMAND:
      return new DirectiveDispatch(origDisp, origUrl, frame);
    case CopyDispatch.COMMAND:
      return new CopyDispatch(origDisp, origUrl, frame);
    case RecipientDispatch.COMMAND:
      return new RecipientDispatch(origDisp, origUrl, frame);
    case MarkBlockDispatch.COMMAND:
      return new MarkBlockDispatch(origDisp, origUrl, frame);
    default:
      return null;
    }
  }

}
