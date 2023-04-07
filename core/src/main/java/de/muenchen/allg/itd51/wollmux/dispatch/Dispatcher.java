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

import java.util.Arrays;
import java.util.List;

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

/**
 * A service for creating libreoffice dispatches.
 */
public abstract class Dispatcher
{
  private List<String> commands;

  /**
   * Create a new dispatcher for the given commands.
   *
   * @param commands
   *          List of command strings.
   */
  public Dispatcher(String... commands)
  {
    this.commands = Arrays.asList(commands);
  }

  /**
   * The command defined by the URL.
   *
   * @param url
   *          A command URL.
   * @return The command.
   */
  protected String getDispatchMethodName(URL url)
  {
    return url.Complete.split("#")[0];
  }

  /**
   * Can this dispatcher create dispatches for the given URL.
   *
   * @param url
   *          A command URL.
   * @return True if this dispatcher is capable of creating dispatches.
   */
  public boolean supports(URL url)
  {
    return commands.contains(getDispatchMethodName(url));
  }

  /**
   * Creates a new dispatch for the URL.
   *
   * @param origDisp
   *          The original dispatch.
   * @param url
   *          The command URL.
   * @param frame
   *          The corresponding frame.
   * @return A new dispatch.
   */
  public abstract WollMuxDispatch create(XDispatch origDisp, URL url,
      XFrame frame);
}
