package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.XItemListener;
import com.sun.star.lang.EventObject;

@FunctionalInterface
public interface AbstractItemListener extends XItemListener
{
  @Override
  default void disposing(EventObject event)
  {
    // default implementation
  }
}
