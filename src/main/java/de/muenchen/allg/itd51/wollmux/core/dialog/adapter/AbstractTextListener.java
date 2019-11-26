package de.muenchen.allg.itd51.wollmux.core.dialog.adapter;

import com.sun.star.awt.XTextListener;
import com.sun.star.lang.EventObject;

@FunctionalInterface
public interface AbstractTextListener extends XTextListener
{
  @Override
  default void disposing(EventObject event)
  {
    // default implementation
  }
}
