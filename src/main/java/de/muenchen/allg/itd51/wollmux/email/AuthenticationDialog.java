/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;

public class AuthenticationDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationDialog.class);

  private IAuthenticationDialogListener authDialogListener;
  private XControlContainer controlContainer = null;
  private XWindow window = null;
  private XDialog dialog = null;

  public AuthenticationDialog(String username, IAuthenticationDialogListener authDialogListener)
  {
    this.authDialogListener = authDialogListener;
    
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());

    try
    {
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));

      window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.email_auth?location=application", "", peer, null);
      controlContainer = UNO.XControlContainer(window);

      UNO.XTextComponent(controlContainer.getControl("txtUsername")).setText(username);
      UNO.XButton(controlContainer.getControl("btnOK")).addActionListener(okActionListener);
      UNO.XButton(controlContainer.getControl("btnAbort")).addActionListener(abortActionListener);

      dialog = UNO.XDialog(window);
      dialog.execute();
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }

  private AbstractActionListener okActionListener = new AbstractActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      String username = UNO.XTextComponent(controlContainer.getControl("txtUsername")).getText();
      String password = UNO.XTextComponent(controlContainer.getControl("txtPassword")).getText();

      authDialogListener.setCredentails(username, password);
      dialog.endExecute();
    }
  };

  private AbstractActionListener abortActionListener = new AbstractActionListener()
  {

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      dialog.endExecute();
    }
  };
}
