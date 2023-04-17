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
package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextbausteinEinfuegen;

/**
 * Dispatch for inserting boilerplates.
 */
public class TextbausteinDispatch extends WollMuxDispatch
{
  /**
   * Command for inserting a boilerplate.
   */
  public static final String COMMAND = "wollmux:TextbausteinEinfuegen";

  /**
   * Command for inserting a reference to a boilerplate
   */
  public static final String COMMAND_REFERENCE = "wollmux:TextbausteinVerweisEinfuegen";

  /**
   * If true, the boilerplate reference is immediately replaced by the text.
   */
  private boolean reprocess;

  TextbausteinDispatch(XDispatch origDisp, URL origUrl, XFrame frame, boolean reprocess)
  {
    super(origDisp, origUrl, frame);
    this.reprocess = reprocess;
  }

  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    new OnTextbausteinEinfuegen(DocumentManager.getTextDocumentController(frame), reprocess).emit();
  }

}
