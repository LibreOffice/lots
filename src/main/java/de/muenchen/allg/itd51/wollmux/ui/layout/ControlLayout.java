package de.muenchen.allg.itd51.wollmux.ui.layout;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.Size;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XLayoutConstrains;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindow2;

import de.muenchen.allg.afid.UNO;

/**
 * A layout with one control.
 */
public class ControlLayout implements Layout
{

  private XWindow control;
  private XWindow2 control2;
  private XLayoutConstrains constrains;

  private int height;

  public ControlLayout(XWindow control)
  {
    this.control = control;
    this.control2 = UNO.XWindow2(control);
    this.constrains = UNO.XLayoutConstrains(control);
    height = control.getPosSize().Height;
  }

  public ControlLayout(XControl control)
  {
    this(UNO.XWindow(control));
  }

  @Override
  public Pair<Integer, Integer> layout(Rectangle rect)
  {
    int h = height;
    int w = rect.Width;
    if (constrains != null)
    {
      Size size = constrains.calcAdjustedSize(new Size(rect.Width, height));
      h = Integer.max(h, size.Height);
      w = Integer.max(w, size.Width);
    }
    control.setPosSize(rect.X, rect.Y, w, h, PosSize.POSSIZE);
    if (UNO.XWindow2(control) != null && !UNO.XWindow2(control).isVisible())
    {
      return Pair.of(0, 0);
    }
    return Pair.of(h, w);
  }

  /**
   * Unsupported operation.
   */
  @Override
  public void addLayout(Layout layout, int space)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getHeightForWidth(int width)
  {
    int h = 0;
    if (control2 != null && control2.isVisible())
    {
      h = height;
      if (constrains != null)
      {
        Size size = constrains.calcAdjustedSize(new Size(width, height));
        h = Integer.max(h, size.Height);
      }
    }
    return h;
  }

  @Override
  public int getMinimalWidth(int maxWidth)
  {
    int w = 0;
    if (constrains != null)
    {
      Size size = constrains.getMinimumSize();
      w = size.Width;
    }
    return Integer.min(maxWidth, w);
  }

  @Override
  public boolean isVisible()
  {
    if (control2 != null)
    {
      return control2.isVisible();
    }
    return true;
  }
}
