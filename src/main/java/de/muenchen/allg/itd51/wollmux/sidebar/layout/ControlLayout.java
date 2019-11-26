package de.muenchen.allg.itd51.wollmux.sidebar.layout;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XWindow;

import de.muenchen.allg.afid.UNO;

/**
 * Ein Layout, dass nur ein Control enthalten kann.
 *
 * Als Höhe wird immer die Höhe des XControl verwendet. Sie ist damit statisch.
 *
 * @author daniel.sikeler
 */
public class ControlLayout implements Layout
{
  private XWindow control;

  private int height;

  public ControlLayout(XWindow control)
  {
    this.control = control;
    height = control.getPosSize().Height;
  }

  public ControlLayout(XControl control)
  {
    this(UNO.XWindow(control));
  }

  @Override
  public int layout(Rectangle rect)
  {
    control.setPosSize(rect.X, rect.Y, rect.Width, height, PosSize.POSSIZE);
    return height;
  }

  /**
   * Diese Operation ist nicht erlaubt.
   */
  @Override
  public void addLayout(Layout layout, int space)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getHeight()
  {
    return height;
  }

}
