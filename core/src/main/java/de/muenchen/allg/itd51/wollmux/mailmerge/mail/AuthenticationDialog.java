/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.mail;

import java.util.function.BiConsumer;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.util.UnoComponent;

/**
 * An authentication dialog.
 */
public class AuthenticationDialog
{
  private AuthenticationDialog()
  {
    // nothing to do
  }
  
  /**
   * Show the dialog.
   *
   * @param username
   *          The initial value of the username field.
   * @param authDialogListener
   *          The listener to call after the dialog has been finished. The first parameter is the
   *          username. The second parameter the password.
   */
  public static void show(String username, BiConsumer<String, String> authDialogListener)
  {

    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());

    XContainerWindowProvider provider = UNO.XContainerWindowProvider(
        UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));

    XWindow window = provider.createContainerWindow("vnd.sun.star.script:WollMux.email_auth?location=application", "",
        peer,
        null);
    XControlContainer controlContainer = UNO.XControlContainer(window);
    XDialog dialog = UNO.XDialog(window);

    UNO.XTextComponent(controlContainer.getControl("txtUsername")).setText(username);

    AbstractActionListener okActionListener = e ->
    {
      String user = UNO.XTextComponent(controlContainer.getControl("txtUsername")).getText();
      String password = UNO.XTextComponent(controlContainer.getControl("txtPassword")).getText();

      authDialogListener.accept(user, password);
      dialog.endExecute();
    };
    UNO.XButton(controlContainer.getControl("btnOK")).addActionListener(okActionListener);

    AbstractActionListener abortActionListener = e -> dialog.endExecute();
    UNO.XButton(controlContainer.getControl("btnAbort")).addActionListener(abortActionListener);
    dialog.execute();
  }
}
