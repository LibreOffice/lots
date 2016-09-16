package de.muenchen.allg.itd51.wollmux.event;

import java.lang.reflect.Method;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Implementiert XNotifyingDispatch und kann alle Dispatch-URLs behandeln, die kein
 * DocumentModel erfordern. Nähere Infos zur Funktionsweise siehe
 * {@link BaseDispatch}.
 *
 * @author Daniel Sikeler
 */
public class NotifyingDispatch extends BaseDispatch implements XNotifyingDispatch
{

  @Override
  public void dispatchWithNotification(URL url, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    Logger.debug2(this.getClass().getSimpleName() + ".dispatchWithNotification('"
      + url.Complete + "')");

    String arg = getMethodArgument(url);

    String methodName = getMethodName(url);

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
      Logger.error(x);
    }
  }

}
