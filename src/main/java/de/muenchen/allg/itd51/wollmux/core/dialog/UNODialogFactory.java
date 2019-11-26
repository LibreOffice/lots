package de.muenchen.allg.itd51.wollmux.core.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;

public class UNODialogFactory
{
  private static final Logger LOGGER = LoggerFactory.getLogger(UNODialogFactory.class);

  private XWindow modalBaseDialogWindow = null;

  public XWindow createDialog(int width, int height, int backgroundColor)
  {
    Object cont = UNO.createUNOService("com.sun.star.awt.UnoControlContainer");
    XControl dialogControl = UnoRuntime.queryInterface(XControl.class, cont);

    Object unoControlContainerModelO = UNO
        .createUNOService("com.sun.star.awt.UnoControlContainerModel");
    XControlModel unoControlContainerModel = UnoRuntime.queryInterface(XControlModel.class,
        unoControlContainerModelO);
    dialogControl.setModel(unoControlContainerModel);

    XWindow contXWindow = UNO.XWindow(dialogControl);

    Object toolkit = null;
    XToolkit xToolkit = null;
    try
    {
      toolkit = UNO.xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit", UNO.defaultContext);
      xToolkit = UnoRuntime.queryInterface(XToolkit.class, toolkit);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    XWindow currentWindow = UNO.desktop.getCurrentFrame().getContainerWindow();
    XWindowPeer currentWindowPeer = UNO.XWindowPeer(currentWindow);
    XWindowPeer modalBaseDialog = createModalBaseDialog(xToolkit, currentWindowPeer, width, height);
    this.modalBaseDialogWindow = UNO.XWindow(modalBaseDialog);

    Object testFrame;
    XFrame xFrame = null;
    try
    {
      testFrame = UNO.xMCF.createInstanceWithContext("com.sun.star.frame.Frame",
          UNO.defaultContext);

      xFrame = UNO.XFrame(testFrame);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    xFrame.initialize(this.modalBaseDialogWindow);
    XFramesSupplier creator = UNO.desktop.getCurrentFrame().getCreator();
    xFrame.setCreator(creator);
    xFrame.activate();
    dialogControl.createPeer(xToolkit, modalBaseDialog);
    XWindowPeer testPeer = dialogControl.getPeer();
    testPeer.setBackground(backgroundColor);

    boolean isSuccessfullySet = xFrame.setComponent(contXWindow, null);

    if (!isSuccessfullySet)
    {
      LOGGER.error("UNODialogExample: createDialog: XFrame has not been set successfully.");
      return contXWindow;
    }

    return contXWindow;
  }

  public static XControl convertToXControl(
      ControlProperties controlProperties)
  {
    String controlType = controlProperties.getControlType().toString();
    Object control = UNO.createUNOService(controlType);

    Object editModel = null;
    try
    {
      editModel = UNO.xMSF.createInstance(controlType + "Model");
    } catch (Exception e1)
    {
      LOGGER.error("", e1);
    }

    XControlModel modelX = UnoRuntime.queryInterface(XControlModel.class, editModel);

    XControl xControl = UnoRuntime.queryInterface(XControl.class, control);
    XWindow wnd = UnoRuntime.queryInterface(XWindow.class, xControl);
    wnd.setPosSize(0, 0, 
        controlProperties.getControlSize().getWidth() > 0
        ? controlProperties.getControlSize().getWidth()
            : controlProperties.getControlPercentSize().getWidth(), 
            controlProperties.getControlSize().getHeight() > 0
            ? controlProperties.getControlSize().getHeight()
                : controlProperties.getControlPercentSize().getHeight(),
        PosSize.SIZE);

    xControl.setModel(modelX);

    return xControl;
  }

  public void showDialog()
  {
    if (this.modalBaseDialogWindow == null)
    {
      LOGGER.error(
          "Es wurde kein exestierendes Dialog-Fenster gefunden. Ein Dialog muss zuvor erstellt werden.");
      return;
    }

    this.modalBaseDialogWindow.setEnable(true);
    this.modalBaseDialogWindow.setVisible(true);
  }

  public void closeDialog()
  {
    if (this.modalBaseDialogWindow == null)
    {
      LOGGER.error(
          "Es wurde kein exestierendes Dialog-Fenster gefunden. Ein Dialog muss zuvor erstellt werden.");
      return;
    }

    this.modalBaseDialogWindow.setEnable(false);
    this.modalBaseDialogWindow.dispose();
    this.modalBaseDialogWindow = null;
  }
  
  public void setVisible(boolean value) {
    this.modalBaseDialogWindow.setVisible(value);
  }

  private XWindowPeer createModalBaseDialog(XToolkit toolkit, XWindowPeer parentWindow, int width,
      int height)
  {
    com.sun.star.awt.Rectangle rect = new Rectangle();

    XWindow parentXWindow = UNO.XWindow(parentWindow);
    rect.X = (parentXWindow.getPosSize().Width / 2) - (width / 2);
    rect.Y = (parentXWindow.getPosSize().Height / 2) - (height / 2);
    rect.Width = width;
    rect.Height = height;

    WindowDescriptor aWindow = new WindowDescriptor();
    aWindow.Type = WindowClass.TOP;
    aWindow.WindowServiceName = "window";
    aWindow.Parent = parentWindow;
    aWindow.ParentIndex = -1;
    aWindow.Bounds = rect;

    aWindow.WindowAttributes = WindowAttribute.CLOSEABLE | WindowAttribute.SIZEABLE
        | WindowAttribute.MOVEABLE | WindowAttribute.BORDER;

    return toolkit.createWindow(aWindow);
  }
}
