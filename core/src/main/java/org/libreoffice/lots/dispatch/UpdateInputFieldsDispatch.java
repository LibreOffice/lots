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
package org.libreoffice.lots.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.lots.event.handlers.OnUpdateInputFields;
import org.libreoffice.lots.event.handlers.WollMuxEvent;

/**
 * Dispatch for updating input fields.
 */
public class UpdateInputFieldsDispatch extends WollMuxNotifyingDispatch
{
  /**
   * Command of this dispatch.
   */
  public static final String COMMAND = ".uno:UpdateInputFields";

  UpdateInputFieldsDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatchWithNotification(URL url, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    this.props = props;
    this.listener = listener;
    emitEvent();
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    this.props = props;
    emitEvent();
  }

  private void emitEvent()
  {
    WollMuxEvent event = new OnUpdateInputFields(UNO.XTextDocument(frame.getController().getModel()),
        this);
    if (isSynchronMode(props))
    {
      event.process();
    } else
    {
      event.emit();
    }
  }

}
