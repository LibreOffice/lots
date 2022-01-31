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
package de.muenchen.allg.itd51.wollmux.dispatch;

import java.util.Arrays;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpenDocument;

/**
 * Dispatch for opening files as template or document.
 */
public class OpenTemplateDispatch extends WollMuxDispatch
{
  /**
   * Command for templates.
   */
  public static final String COMMAND_TEMPLATE = "wollmux:OpenTemplate";

  /**
   * Command for documents.
   */
  public static final String COMMAND_DOCUMENT = "wollmux:OpenDocument";

  /**
   * If true, file is opened as template and not processed by WollMux, otherwise WollMux processes
   * the file.
   */
  private boolean asTemplate;

  OpenTemplateDispatch(XDispatch origDisp, URL origUrl, XFrame frame, boolean asTemplate)
  {
    super(origDisp, origUrl, frame);
    this.asTemplate = asTemplate;
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    String[] fragIds = getMethodArgument().split("&");
    new OnOpenDocument(Arrays.asList(fragIds), asTemplate).emit();
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
