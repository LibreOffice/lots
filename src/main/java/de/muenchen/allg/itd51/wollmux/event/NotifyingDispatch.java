package de.muenchen.allg.itd51.wollmux.event;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.util.URL;

/**
 * Implementiert XNotifyingDispatch und kann alle Dispatch-URLs behandeln, die kein
 * DocumentModel erfordern. Nähere Infos zur Funktionsweise siehe
 * {@link BaseDispatch}.
 *
 * @author Daniel Sikeler
 */
public class NotifyingDispatch extends BaseDispatch implements XNotifyingDispatch
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(NotifyingDispatch.class);

  @Override
  public void dispatchWithNotification(URL url, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    LOGGER.trace(this.getClass().getSimpleName() + ".dispatchWithNotification('"
      + url.Complete + "')");

    String arg = getMethodArgument(url);

    String methodName = getDispatchMethodName(url);

    try
    {
      Class<? extends NotifyingDispatch> myClass = this.getClass();
      if (listener == null)
      {
        Method method =
          myClass.getDeclaredMethod(methodName, String.class, PropertyValue[].class);
        method.invoke(this, arg, props);
      }
      else
      {
        Method method =
          myClass.getDeclaredMethod(methodName, String.class, PropertyValue[].class,
            XDispatchResultListener.class);
        method.invoke(this, arg, props, listener);
      }
    }
    catch (Throwable x)
    {
      LOGGER.error("", x);
    }
  }

}
