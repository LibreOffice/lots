package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.XAdjustmentListener;
import com.sun.star.lang.EventObject;

public interface AbstractAdjustmentListener extends XAdjustmentListener
{

  @Override
  public default void disposing(EventObject event)
  {
    // default implementation
  }
}
