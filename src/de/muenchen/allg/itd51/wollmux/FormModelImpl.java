//TODO: FormModelImpl dokumentieren!

package de.muenchen.allg.itd51.wollmux;

import java.util.HashMap;

import com.sun.star.awt.PosSize;
import com.sun.star.lang.XComponent;

import de.muenchen.allg.afid.UnoService;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;

public class FormModelImpl implements FormModel
{
  private final UnoService document;

  private final FunctionLibrary funcLib;

  private final HashMap idToFormValues;

  public FormModelImpl(XComponent doc, FunctionLibrary funcLib,
      HashMap idToFormValues)
  {
    this.document = new UnoService(doc);
    this.funcLib = funcLib;
    this.idToFormValues = idToFormValues;
  }

  public void close()
  {
    try
    {
      document.xCloseable().close(true);
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  public void setWindowVisible(boolean vis)
  {
    UnoService frame = new UnoService(document.xModel().getCurrentController()
        .getFrame());
    if (frame.xFrame() != null)
    {
      frame.xFrame().getContainerWindow().setVisible(vis);
    }
  }

  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
  {
    UnoService frame = new UnoService(document.xModel().getCurrentController()
        .getFrame());
    if (frame.xFrame() != null)
    {
      frame.xFrame().getContainerWindow().setPosSize(
          docX,
          docY,
          docWidth,
          docHeight,
          PosSize.POSSIZE);
    }
  }

  public void setVisibleState(String groupId, boolean visible)
  {
    // TODO setVisibleState implementieren

  }

  public void valueChanged(String fieldId, String newValue)
  {
    WollMuxEventHandler.handleFormValueChanged(
        document.xTextDocument(),
        idToFormValues,
        fieldId,
        newValue,
        funcLib);
  }

  public void focusGained(String fieldId)
  {
    WollMuxEventHandler.handleFocusFormField(idToFormValues, fieldId, document
        .xTextDocument());
  }

  public void focusLost(String fieldId)
  {
    WollMuxEventHandler.handleUnFocusFormField(
        idToFormValues,
        fieldId,
        document.xTextDocument());
  }
}
