/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFunctionDialog;

/**
 * Dispatch for handling a function dialog.
 */
public class FunctionDialogDispatch extends WollMuxDispatch
{
  /**
   * Command of this dispatch.
   */
  public static final String COMAND = "wollmux:FunctionDialog";

  FunctionDialogDispatch(XDispatch origDisp, URL origUrl, XFrame frame)
  {
    super(origDisp, origUrl, frame);
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    new OnFunctionDialog(DocumentManager.getTextDocumentController(frame), getMethodArgument())
        .emit();
  }

  @Override
  public boolean status()
  {
    return true;
  }

  @Override
  public boolean isGlobal()
  {
    return true;
  }

}
