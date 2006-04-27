//TODO: FormModelImpl dokumentieren!

package de.muenchen.allg.itd51.wollmux;

import com.sun.star.awt.PosSize;
import com.sun.star.lang.XComponent;

import de.muenchen.allg.afid.UnoService;

public class FormModelImpl implements FormModel
{
  private UnoService document;

  public FormModelImpl(XComponent doc)
  {
    this.document = new UnoService(doc);
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
}
