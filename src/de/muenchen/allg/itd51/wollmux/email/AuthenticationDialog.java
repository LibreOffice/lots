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
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;

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
      controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);

      UNO.XTextComponent(controlContainer.getControl("txtUsername")).setText(username);
      UNO.XButton(controlContainer.getControl("btnOK")).addActionListener(okActionListener);
      UNO.XButton(controlContainer.getControl("btnAbort")).addActionListener(abortActionListener);

      dialog = UnoRuntime.queryInterface(XDialog.class, window);
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
