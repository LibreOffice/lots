package de.muenchen.allg.itd51.wollmux.dispatch;

import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.lang.EventObject;

/**
 * A default implementation of {@link XDispatchResultListener}.
 */
@FunctionalInterface
public interface AbstractDispatchResultListener extends XDispatchResultListener
{
  @Override
  public default void disposing(EventObject event)
  {
    // default implementation
  }
}