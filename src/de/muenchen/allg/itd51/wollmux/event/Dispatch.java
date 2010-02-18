/*
 * Dateiname: Dispatch.java
 * Projekt  : WollMux
 * Funktion : Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die kein DocumentModel erfordern.
 * 
 * Copyright (c) 2009 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0 (or any later version).
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
 * 05.11.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Vector;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XStatusListener;
import com.sun.star.util.URL;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.Logger;

/**
 * Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die kein
 * DocumentModel erfordern. Die dispatch()-Methode funktioniert über Reflection. Jede
 * Methode dieser Klasse der Form dispatch_name(String arg, PropertyValue[] props)
 * implementiert den Dispatch der URL name, wobei alle Buchstaben in lowercase
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
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class Dispatch implements XDispatch
{
  public static final String DISP_unoPrint = ".uno:Print";

  public static final String DISP_unoPrintDefault = ".uno:PrintDefault";

  public static final String DISP_unoPrinterSetup = ".uno:PrinterSetup";

  public static final String DISP_wmAbsenderAuswaehlen =
    "wollmux:AbsenderAuswaehlen";

  public static final String DISP_wmPALVerwalten = "wollmux:PALVerwalten";

  public static final String DISP_wmOpenTemplate = "wollmux:OpenTemplate";

  public static final String DISP_wmOpen = "wollmux:Open";

  public static final String DISP_wmOpenDocument = "wollmux:OpenDocument";

  public static final String DISP_wmKill = "wollmux:Kill";

  public static final String DISP_wmAbout = "wollmux:About";

  public static final String DISP_wmDumpInfo = "wollmux:DumpInfo";

  public static final String DISP_wmFunctionDialog = "wollmux:FunctionDialog";

  public static final String DISP_wmFormularMax4000 = "wollmux:FormularMax4000";

  public static final String DISP_wmZifferEinfuegen = "wollmux:ZifferEinfuegen";

  public static final String DISP_wmAbdruck = "wollmux:Abdruck";

  public static final String DISP_wmZuleitungszeile = "wollmux:Zuleitungszeile";

  public static final String DISP_wmMarkBlock = "wollmux:MarkBlock";

  public static final String DISP_wmTextbausteinEinfuegen =
    "wollmux:TextbausteinEinfuegen";

  public static final String DISP_wmPlatzhalterAnspringen =
    "wollmux:PlatzhalterAnspringen";

  public static final String DISP_wmTextbausteinVerweisEinfuegen =
    "wollmux:TextbausteinVerweisEinfuegen";

  public static final String DISP_wmSeriendruck = "wollmux:Seriendruck";

  public static final String DISP_wmTest = "wollmux:Test";

  /**
   * Liefert zu url den Namen der Methode, die den Dispatch behandeln würde.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public static String getMethodName(URL url)
  {
    String part = url.Complete.split("#")[0];
    String methodName = "dispatch_" + part.replaceAll("\\W", "_").toLowerCase();
    return methodName;
  }

  /**
   * Enthält alle aktuell registrierten StatusListener Grund für Auskommentierung:
   * Braucht doch keiner, oder?
   */
  // protected final Vector<XStatusListener> statusListener =
  // new Vector<XStatusListener>();
  public void dispatch_wollmux_absenderauswaehlen(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleShowDialogAbsenderAuswaehlen();
  }

  public void dispatch_wollmux_palverwalten(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleShowDialogPersoenlicheAbsenderliste();
  }

  public void dispatch_wollmux_opentemplate(String arg, PropertyValue[] props)
  {
    Vector<String> fragIds = new Vector<String>();
    String[] parts = arg.split("&");
    for (int i = 0; i < parts.length; i++)
      fragIds.add(parts[i]);
    WollMuxEventHandler.handleOpenDocument(fragIds, true);
  }

  public void dispatch_wollmux_open(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleOpen(arg);
  }

  public void dispatch_wollmux_opendocument(String arg, PropertyValue[] props)
  {
    Vector<String> fragIds = new Vector<String>();
    String[] parts = arg.split("&");
    for (int i = 0; i < parts.length; i++)
      fragIds.add(parts[i]);
    WollMuxEventHandler.handleOpenDocument(fragIds, false);
  }

  public void dispatch_wollmux_kill(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleKill();
  }

  public void dispatch_wollmux_about(String arg, PropertyValue[] props)
  {
    String wollMuxBarVersion = null;
    if (arg.length() > 0) wollMuxBarVersion = arg;
    WollMuxEventHandler.handleAbout(wollMuxBarVersion);
  }

  public void dispatch_wollmux_dumpinfo(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleDumpInfo();
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
    FeatureStateEvent fse = new FeatureStateEvent();
    fse.FeatureURL = url;
    fse.IsEnabled = true;
    listener.statusChanged(fse);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#dispatch(com.sun.star.util.URL,
   *      com.sun.star.beans.PropertyValue[])
   */
  public void dispatch(URL url, PropertyValue[] props)
  {
    Logger.debug2(this.getClass().getSimpleName() + ".dispatch('" + url.Complete
      + "')");

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

    String methodName = getMethodName(url);

    try
    {
      Class<? extends Dispatch> myClass = this.getClass();
      Method method =
        myClass.getDeclaredMethod(methodName, String.class, PropertyValue[].class);
      method.invoke(this, arg, props);
    }
    catch (Throwable x)
    {
      Logger.error(x);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.star.frame.XDispatch#addStatusListener(com.sun.star.frame.XStatusListener,
   *      com.sun.star.util.URL)
   */
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
   * @see com.sun.star.frame.XDispatch#removeStatusListener(com.sun.star.frame.XStatusListener,
   *      com.sun.star.util.URL)
   */
  public void removeStatusListener(XStatusListener listener, URL x)
  {
  // Iterator<XStatusListener> iter = statusListener.iterator();
  // while (iter.hasNext())
  // if (UnoRuntime.areSame(UNO.XInterface(iter.next()), listener)) iter.remove();
  }
}
