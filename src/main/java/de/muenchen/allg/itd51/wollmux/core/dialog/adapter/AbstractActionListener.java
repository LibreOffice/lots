package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.XActionListener;
import com.sun.star.lang.EventObject;

@FunctionalInterface
public interface AbstractActionListener extends XActionListener
{

  @Override
  public default void disposing(EventObject event)
  {
    // default implementation
  }
}
