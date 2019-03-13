package de.muenchen.allg.itd51.wollmux.email;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Dock;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;

public class AuthenticationDialog
{
  private SimpleDialogLayout layout;
  
  private UNODialogFactory dialogFactory;
  
  private IAuthenticationDialogListener authDialogListener;
  
  public AuthenticationDialog(IAuthenticationDialogListener authDialogListener) {
    this.authDialogListener = authDialogListener;
    
    dialogFactory = new UNODialogFactory();
    XWindow dialogWindow = dialogFactory.createDialog(450, 350, 0xF2F2F2);

    dialogFactory.showDialog();

    layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);  
    layout.addControlsToList(addIntroLabel());
    layout.addControlsToList(addUsernameControls());
    layout.addControlsToList(addPasswordControls());
    layout.addControlsToList(addBottomButtons());
    
    layout.draw();
  }
  
  private ControlModel addIntroLabel() {
    List<ControlProperties> introControls = new ArrayList<>();
    
    ControlProperties lblIntro = new ControlProperties(ControlType.LABEL, "lblIntro");
    lblIntro.setControlPercentSize(100, 20);
    lblIntro.setLabel("Bitte geben Sie Benutzername und Passwort für den E-Mail-Server ein.");
   
    introControls.add(lblIntro);
    
    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, introControls, Optional.empty());
  }
  
  private ControlModel addUsernameControls()
  {
    List<ControlProperties> authControls = new ArrayList<>();

    ControlProperties lblUsername = new ControlProperties(ControlType.LABEL, "lblUsername");
    lblUsername.setControlPercentSize(30, 20);
    lblUsername.setLabel("Benutzername");
    
    ControlProperties editPassword = new ControlProperties(ControlType.EDIT, "editUsername");
    editPassword.setControlPercentSize(70, 20);
    
    authControls.add(lblUsername);
    authControls.add(editPassword);
    
    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, authControls, Optional.empty());
  }
  
  private ControlModel addPasswordControls()
  {
    List<ControlProperties> authControls = new ArrayList<>();

    ControlProperties lblPassword = new ControlProperties(ControlType.LABEL, "lblPassword");
    lblPassword.setControlPercentSize(30, 20);
    lblPassword.setLabel("Passwort");
    
    ControlProperties editPassword = new ControlProperties(ControlType.EDIT, "editPassword");
    editPassword.setControlPercentSize(70, 20);
    
    authControls.add(lblPassword);
    authControls.add(editPassword);
    
    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, authControls, Optional.empty());
  }
  
  private ControlModel addBottomButtons()
  {
    List<ControlProperties> bottomBtns = new ArrayList<>();

    ControlProperties abortBtn = new ControlProperties(ControlType.BUTTON, "abortBtn");
    abortBtn.setControlPercentSize(50, 40);
    abortBtn.setLabel("Abbrechen");
    XButton abortXBtn = UNO.XButton(abortBtn.getXControl());
    abortXBtn.addActionListener(abortActionListener);

    ControlProperties editBtn = new ControlProperties(ControlType.BUTTON, "editBtn");
    editBtn.setControlPercentSize(50, 40);
    editBtn.setLabel("Ok");
    XButton editXBtn = UNO.XButton(editBtn.getXControl());
    editXBtn.addActionListener(okActionListener);

    bottomBtns.add(abortBtn);
    bottomBtns.add(editBtn);

    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, bottomBtns, Optional.of(Dock.BOTTOM));
  }
  
  private AbstractActionListener okActionListener = new AbstractActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      XTextComponent username = UNO.XTextComponent(layout.getControl("editUsername"));
      XTextComponent password = UNO.XTextComponent(layout.getControl("editPassword"));
      
      authDialogListener.setCredentails(username.getText(), password.getText());
    }
  };
  
  private AbstractActionListener abortActionListener = new AbstractActionListener()
  {
    
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
      dialogFactory.closeDialog();
    }
  };
}
