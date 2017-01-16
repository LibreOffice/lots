package de.muenchen.uno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.sun.mail.iap.Argument;
import com.sun.star.beans.MethodConcept;
import com.sun.star.beans.XIntrospection;
import com.sun.star.beans.XIntrospectionAccess;
import com.sun.star.lang.NoSuchMethodException;
import com.sun.star.reflection.InvocationTargetException;
import com.sun.star.reflection.XIdlMethod;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

public class UnoReflect
{
  private static XIntrospection intro;

  static
  {
    try
    {
      intro = UnoRuntime.queryInterface(XIntrospection.class,
        UNO.xMSF.createInstance("com.sun.star.beans.Introspection"));
    }
    catch (java.lang.Exception e)
    {
      Logger.error(e);
    }
  }

  private XIntrospectionAccess access;
  private Object object;
  private XIdlMethod method;
  private List<Object> args = new ArrayList<Object>();

  private UnoReflect(Object o)
  {
    this.object = o;
    access = intro.inspect(o);
    if (access == null)
    {
      throw new IllegalArgumentException(String.format("%s is not a valid UNO class.", o.getClass().getName()));
    }
  }

  public static UnoReflect with(Object o)
  {
    return new UnoReflect(o);
  }
  
  public UnoReflect method(String name) throws NoSuchMethodException
  {
    method = access.getMethod(name, MethodConcept.ALL);
    return this;
  }
  
  public UnoReflect withArgs(Object ... args)
  {
    this.args.clear();
    CollectionUtils.addAll(this.args, args);
    return this;
  }
  
  public Object invoke()
  {
    if (method == null)
    {
      throw new UnoReflectionException("No method selected. Call 'method' before 'invoke'.");
    }
    
    try
    {
      return method.invoke(object, new Object[][] { args.toArray() });
    }
    catch (Exception e)
    {
      throw new UnoReflectionException(e);
    }
  }
}
