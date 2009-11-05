/*
 * Dateiname: BasicWollMuxDispatchProvider.java
 * Projekt  : WollMux
 * Funktion : Liefert zu Dispatch-URLs, die der WollMux ohne ein zugehöriges TextDocumentModel behandeln kann XDispatch-Objekte.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 28.10.2006 | LUT | Erstellung als DispatchInterceptor
 * 10.01.2007 | LUT | Umbenennung in DispatchHandler: Behandelt jetzt
 *                    auch globale WollMux Dispatches
 * 05.11.2009 | BNK | Auf Verwendung der neuen Dispatch und DocumentDispatch
 *                    Klassen umgeschrieben
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @author Matthias S. Benkmann (D-III-ITD-D101)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import java.lang.reflect.Method;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchDescriptor;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Liefert zu Dispatch-URLs, die der WollMux ohne ein zugehöriges TextDocumentModel
 * behandeln kann XDispatch-Objekte.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class BasicWollMuxDispatchProvider implements XDispatchProvider
{

  /**
   * Liefert die Methode methodName(String, PropertyValue[]) aus der Klasse c, oder
   * null falls so eine Methode nicht vorhanden ist.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  protected Method getMethod(Class<?> c, String methodName)
  {
    try
    {
      Method method =
        c.getDeclaredMethod(methodName, String.class, PropertyValue[].class);
      return method;
    }
    catch (Throwable x)
    {
      Logger.debug2(x);
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatch(com.sun.star.util.URL,
   *      java.lang.String, int)
   */
  public XDispatch queryDispatch(URL url, String frameName, int fsFlag)
  {
    String methodName = Dispatch.getMethodName(url);

    if (getMethod(Dispatch.class, methodName) != null)
      return new Dispatch();
    else
      return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatchProvider#queryDispatches(com.sun.star.frame.DispatchDescriptor[])
   */
  public XDispatch[] queryDispatches(DispatchDescriptor[] seqDescripts)
  {
    int nCount = seqDescripts.length;
    XDispatch[] lDispatcher = new XDispatch[nCount];

    for (int i = 0; i < nCount; ++i)
      lDispatcher[i] =
        queryDispatch(seqDescripts[i].FeatureURL, seqDescripts[i].FrameName,
          seqDescripts[i].SearchFlags);

    return lDispatcher;
  }
}
