package de.muenchen.allg.itd51.wollmux.event;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XStatusListener;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Implementiert XDispatch und stellt einige Hilfsmethoden für konkrete
 * Implementierungen bereit. Die dispatch()-Methode funktioniert über Reflection.
 * Jede Methode dieser Klasse der Form dispatch_name(String arg, PropertyValue[]
 * props) implementiert den Dispatch der URL name, wobei alle Buchstaben in lowercase
 * konvertiert und nichtalphanumerische Zeichen durch Unterstrich ersetzt sind.
 * Beispiel: dispatch__uno_print() implementiert den dispatch der URL ".uno:Print".
 * Man beachte die beiden Unterstriche im Namen. Der erste kommt von "dispatch_" der
 * zweite ist die Ersetzung des ".". Um diese Klasse eine neue URL unterstützen zu
 * lassen genügt es, eine entsprechende dispatch_Name() Methode hinzuzufügen.
 *
 * Für jede dispatch_name(arg, props) Methode gilt:
 *
 * arg enthält das Argument der URL enthält (z.B. "internerBriefkopf", wenn
 * url="wollmux:openTemplate#internerBriefkopf" war) Es kann davon ausgegangen
 * werden, dass arg nicht null ist und falls es nicht vorhanden ist den Leerstring
 * enthält.
 *
 * props ist das PropertyValue[], das auch schon der ursprünglichen dispatch Methode
 * mitgeliefert wurde.
 *
 * @author daniel.sikeler
 *
 */
public abstract class BaseDispatch implements XDispatch
{

  /**
   * Liefert zu url den Namen der Methode, die den Dispatch behandeln würde.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   *
   */
  public static String getDispatchMethodName(URL url)
  {
    String part = url.Complete.split("#")[0];
    return "dispatch_" + part.replaceAll("\\W", "_").toLowerCase();
  }

  /**
   * Gibt den Namen der Methode zurück, über die der Anzeigestatus
   * eines Menüs oder Toolbarbuttons abgefragt werden kann.
   * 
   * @param url
   * @return
   */
  public static String getStatusMethodName(URL url)
  {
    String part = url.Complete.split("#")[0];
    return "status_" + part.replaceAll("\\W", "_").toLowerCase();
  }
  
  /**
   * Wertet die Properties aus, ob der SynchronMode gesetzt ist.
   *
   * @param props
   *          Die Properties.
   * @return True wenn der SynchronMode gesetzt ist, sonst false.
   */
  public static boolean isSynchronMode(PropertyValue[] props)
  {
    for (int index = 0; index < props.length; index++)
    {
      if ("SynchronMode".equals(props[index].Name))
      {
        return (Boolean) props[index].Value;
      }
    }
    return false;
  }

  public static String getMethodArgument(final URL url)
  {
    // z.B. "wollmux:OpenTemplate#internerBriefkopf"
    // =====> {"wollmux:OpenTemplate", "internerBriefkopf"}
    String arg = "";
    String[] parts = url.Complete.split("#", 2);
    if (parts.length == 2) arg = parts[1];

    // arg durch den URL-Decoder jagen:
    try
    {
      arg = URLDecoder.decode(arg, ConfigThingy.CHARSET);
    }
    catch (UnsupportedEncodingException e)
    {
      Logger.error(L.m("Fehler in Dispatch-URL '%1':", url.Complete), e);
      // Aber wir machen trotzdem weiter. Wer wagt, gewinnt! :-)
    }
    return arg;
  }

  /**
   * Benachrichtigt den übergebenen XStatusListener listener mittels
   * listener.statusChanged() über den aktuellen Zustand des DispatchHandlers und
   * setzt z.B. den Zustände IsEnabled (Standardmäßig wird IsEnabled=true
   * übermittelt).
   *
   * @param listener
   * @param url
   */
  protected void notifyStatusListener(XStatusListener listener, URL url)
  {
    boolean isEnabled = queryStatus(url);
    
    FeatureStateEvent fse = new FeatureStateEvent();
    fse.FeatureURL = url;
    fse.IsEnabled = isEnabled;
    listener.statusChanged(fse);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.sun.star.frame.XDispatch#dispatch(com.sun.star.util.URL,
   * com.sun.star.beans.PropertyValue[])
   */
  @Override
  public void dispatch(URL url, PropertyValue[] props)
  {
    Logger.debug2(this.getClass().getSimpleName() + ".dispatch('" + url.Complete
      + "')");

    String arg = getMethodArgument(url);

    String methodName = getDispatchMethodName(url);

    try
    {
      Class<? extends BaseDispatch> myClass = this.getClass();
      Method method =
        myClass.getDeclaredMethod(methodName, String.class, PropertyValue[].class);
      method.invoke(this, arg, props);
    }
    catch (Throwable x)
    {
      Logger.error(x);
    }
  }
  
  /**
   * Sucht im Dispatcher nach einer Methode "status_<url>", über die entschieden wird,
   * ob ein Menüpunkt oder Toolbarbutton aktiv ist.
   * 
   * @param url
   * @return
   */
  public boolean queryStatus(URL url)
  {
    try
    {
      String methodName = getStatusMethodName(url);
      Class<? extends BaseDispatch> myClass = this.getClass();
      Method method =
        myClass.getDeclaredMethod(methodName);
      Boolean ret = (Boolean) method.invoke(this);
      return ret.booleanValue();
    }
    catch (Throwable x)
    {
      Logger.debug(x);
    }
    
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.sun.star.frame.XDispatch#addStatusListener(com.sun.star.frame.XStatusListener
   * , com.sun.star.util.URL)
   */
  @Override
  public void addStatusListener(XStatusListener listener, URL url)
  {
    // boolean alreadyRegistered = false;
    // Iterator<XStatusListener> iter = statusListener.iterator();
    // while (iter.hasNext())
    // if (UnoRuntime.areSame(UNO.XInterface(iter.next()), listener))
    // alreadyRegistered = true;
    //
    // if (!alreadyRegistered) statusListener.add(listener);

    notifyStatusListener(listener, url);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.sun.star.frame.XDispatch#removeStatusListener(com.sun.star.frame.XStatusListener
   * , com.sun.star.util.URL)
   */
  @Override
  public void removeStatusListener(XStatusListener listener, URL x)
  {
    // Iterator<XStatusListener> iter = statusListener.iterator();
    // while (iter.hasNext())
    // if (UnoRuntime.areSame(UNO.XInterface(iter.next()), listener)) iter.remove();
  }
}
